#!/usr/bin/env python3
"""Orchestrates one perf run against a deployed LabKey server, and compares runs against each other.

The intended use is A/B-ing database configuration -- RDS instance class, Postgres parameters -- so the job here is
mostly about making runs comparable rather than about generating load. Load generation is delegated to the Java
harness (`org.labkey.test.stress.SimulationRunner`), which pins the workload and records what it ran.

    just preflight runs/baseline.json      # is the server reachable, profiled, and idle?
    just run       runs/baseline.json      # generate load, report client and server-side latency
    just compare   <uuid-a> <uuid-b>       # side-by-side

This harness needs no database credentials. Database evidence comes from the server's own postgresSnapshot action,
which needs only Troubleshooter. The LabKey credential
comes from LABKEY_STRESS_APIKEY and is never written to any output.
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from collections import defaultdict
from pathlib import Path
from typing import Any

# perf-harness sits in server/testAutomation, three levels below the LabKey repo root
LABKEY_REPO = Path(__file__).resolve().parents[3]

import tempfile

import pg_stats

CREDENTIAL_ENV = "LABKEY_STRESS_APIKEY"


def _fail(message: str) -> None:
    print(f"\033[31mERROR\033[0m {message}", file=sys.stderr)
    sys.exit(1)


def _warn(message: str) -> None:
    print(f"\033[33mWARN\033[0m  {message}")


def _info(message: str) -> None:
    print(f"\033[36m>\033[0m {message}")


def load_config(config_path: Path) -> dict[str, Any]:
    if not config_path.is_file():
        _fail(f"No such config file: {config_path}")
    config = json.loads(config_path.read_text(encoding="utf-8"))
    for required in ("scenarioName", "baseUrl", "durationSeconds", "activities"):
        if required not in config:
            _fail(f"{config_path} is missing required field '{required}'")
    return config


def credential() -> str:
    value = os.environ.get(CREDENTIAL_ENV)
    if not value:
        _fail(f"Set {CREDENTIAL_ENV} to a LabKey API key for the target server")
    return value


# Needs Troubleshooter in the root container, not DB access.
PG_SNAPSHOT_ACTION = "admin-postgresSnapshot.view"


def capture_pg_snapshot(base_url: str, out_path: Path) -> Path:
    """Fetches a Postgres snapshot from the server itself, so a run no longer waits on someone with a psql session."""
    url = base_url.rstrip("/") + "/" + PG_SNAPSHOT_ACTION
    request = urllib.request.Request(url, headers={"apikey": credential()})
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        detail = {401: "the credential is not authenticated", 403: "the user lacks Troubleshooter in the root container",
                  404: "the server predates PostgresSnapshotAction, or is not running Postgres"}.get(e.code, "")
        _fail(f"Could not capture a Postgres snapshot from {url}: HTTP {e.code}{' -- ' + detail if detail else ''}")
    except urllib.error.URLError as e:
        _fail(f"Could not reach {url}: {e.reason}")

    # Parse before writing so a login redirect returning HTML fails here rather than at attach time
    try:
        snapshot = json.loads(body)
    except ValueError:
        _fail(f"{url} did not return JSON. A redirect to a login page looks like this; check the credential.")
    if "takenAt" not in snapshot:
        _fail(f"{url} returned JSON that is not a snapshot (no 'takenAt'). Got keys: {sorted(snapshot)[:8]}")

    out_path.write_text(body, encoding="utf-8")
    return out_path


def database_read_rate(base_url: str, seconds: int) -> float:
    """Physical blocks read per second, from two snapshots taken `seconds` apart.

    The one precondition nothing else can check without Datadog: whether anyone else is using the deployment. An
    import job running alongside a run both competes for the IOPS ceiling and changes the data being measured.
    """
    with tempfile.TemporaryDirectory() as tmp:
        first = pg_stats.PgSnapshot.read(capture_pg_snapshot(base_url, Path(tmp) / "idle-a.json"))
        time.sleep(seconds)
        second = pg_stats.PgSnapshot.read(capture_pg_snapshot(base_url, Path(tmp) / "idle-b.json"))
    blocks = pg_stats._delta(second.database, first.database).get("blks_read", 0)
    return blocks / seconds


def results_dir_for(config_path: Path, config: dict[str, Any]) -> Path:
    path = (config_path.parent / config.get("resultsDir", "results")).resolve()
    path.mkdir(parents=True, exist_ok=True)
    return path


def local_checks(config: dict[str, Any], *, force: bool = False) -> None:
    """Checks that need no server round-trip, so obvious mistakes fail in a second rather than after a gradle start."""
    credential()

    declared = config.get("configUnderTest", {})
    if declared and not any(str(v).startswith("CHANGEME") for v in declared.values()):
        print("  Config under test, as declared:")
        for key, value in declared.items():
            print(f"    {key:34} {value}")
    else:
        message = (
            "'configUnderTest' is missing or still has CHANGEME placeholders. Results will not record what was "
            "being tested, which makes the comparison unattributable."
        )
        if force:
            _warn(message)
        else:
            _fail(message + "\n        Fill it in, or pass --force.")

    activities = config.get("activities", [])
    if config.get("runOnce"):
        _info(
            "runOnce: each of the %d session(s) runs all %d activities once, in order. 'durationSeconds' is a cap."
            % (config.get("sessions", 10), len(activities))
        )

    caches = config.get("clearCachesBeforeRun") or []
    if caches:
        _warn(
            "This run will CLEAR server caches before generating load: %s. That affects everyone on the deployment -- "
            "the next real page load rebuilds them. It needs AdminPermission in the root container; preflight probes "
            "that without clearing anything." % ", ".join(caches)
        )

    if not config.get("impersonate"):
        _warn(
            "No 'impersonate' block. If the credential's user needs to impersonate to see data, every request will "
            "return empty results, so the run will look successful and measure nothing."
        )

    _warn(
        "This harness cannot verify the database configuration is actually in effect -- it has no DB access. "
        "Confirm in the RDS console that the change applied, and that the instance rebooted if the parameter needed it."
    )


def preflight_via_runner(config_path: Path, config: dict[str, Any]) -> int:
    """Runs the Java preflight: authenticate, impersonate, probe the mini-profiler, and check the server is idle.

    These need the LabKey client's session, CSRF and impersonation handling, so they live in the Java runner rather
    than being reimplemented here.
    """
    preflight_config = dict(config)
    preflight_config["preflightOnly"] = True
    temp_path = config_path.parent / f".preflight-{config_path.name}"
    temp_path.write_text(json.dumps(preflight_config, indent=2), encoding="utf-8")
    try:
        return run_simulation(temp_path)
    finally:
        temp_path.unlink(missing_ok=True)


def run_simulation(config_path: Path) -> int:
    gradlew = LABKEY_REPO / "gradlew"
    if not gradlew.is_file():
        _fail(f"No gradlew at {gradlew}")

    command = [
        str(gradlew),
        ":server:testAutomation:runStressSimulation",
        f"-PsimulationConfig={config_path}",
        "--console=plain",
    ]
    _info(f"Running: {' '.join(command)}")
    return subprocess.run(command, cwd=LABKEY_REPO).returncode


def manifest_from_run(results_dir: Path, scenario_name: str, started: float) -> Path | None:
    """The manifest a just-finished run wrote, or None if it wrote none.

    Requiring it to be newer than the run keeps an earlier run of the same scenario from being summarized as if it
    were this one, which is the failure mode when a run dies before writing its manifest.
    """
    matches = [p for p in results_dir.glob(f"{scenario_name}-*-run.json") if p.stat().st_mtime >= started - 1]
    return max(matches, key=lambda p: p.stat().st_mtime) if matches else None


def find_manifest(results_dir: Path, run_ref: str) -> Path:
    """Locates a run manifest by UUID fragment, or the newest for a scenario name."""
    matches = [p for p in results_dir.glob("*-run.json") if run_ref in p.name]
    if not matches:
        _fail(f"No run manifest matching '{run_ref}' in {results_dir}")
    return max(matches, key=lambda p: p.stat().st_mtime)


def command_run(args: argparse.Namespace) -> int:
    config_path = Path(args.config).resolve()
    config = load_config(config_path)
    results_dir = results_dir_for(config_path, config)

    local_checks(config, force=args.force)

    print("\n  Starting. Confirm separately that the RDS configuration under test is applied and the instance is "
          "fully available -- nothing here can see that.")
    print("  This run takes no database snapshots; use 'just sequence' when the comparison needs them.")

    started = time.time()
    exit_code = run_simulation(config_path)

    manifest_path = manifest_from_run(results_dir, config["scenarioName"], started)
    if manifest_path is None:
        # Without this the missing manifest is reported instead, hiding whatever the runner actually said
        _fail(
            f"Load generation failed (gradle exit {exit_code}); see the runner output above. No results were written."
        )
    if exit_code != 0:
        _warn(
            "The runner exited non-zero, so this run is flagged incomplete -- see its output above. What it did "
            "write is summarized below and holds every request that finished."
        )

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    summarize_run(manifest, results_dir)
    return exit_code


def _percentiles(values: list[float]) -> dict[str, float]:
    ordered = sorted(values)

    def pct(p: float) -> float:
        if not ordered:
            return 0.0
        return ordered[min(len(ordered) - 1, int(round(p / 100 * (len(ordered) - 1))))]

    return {"n": len(ordered), "p50": pct(50), "p95": pct(95), "p99": pct(99), "max": pct(100)}


def read_client_results(path: Path) -> dict[str, dict[str, float]]:
    """Latency percentiles per request name, from the client-side TSV."""
    durations: dict[str, list[float]] = defaultdict(list)
    errors: dict[str, int] = defaultdict(int)
    with path.open(encoding="utf-8") as handle:
        header = handle.readline().rstrip("\n").split("\t")
        name_idx = header.index("requestName")
        status_idx = header.index("statusCode")
        duration_idx = header.index("durationMillis")
        for line in handle:
            fields = line.rstrip("\n").split("\t")
            if len(fields) <= duration_idx:
                continue
            name = fields[name_idx].strip('"')
            durations[name].append(float(fields[duration_idx]))
            if fields[status_idx] not in ("200", "302"):
                errors[name] += 1

    return {name: {**_percentiles(values), "errors": errors[name]} for name, values in durations.items()}


def load_pg_diff(results_dir: Path, manifest: dict[str, Any]) -> dict[str, Any] | None:
    path = results_dir / f"{manifest['scenarioName']}-{manifest['runUuid']}-pg-diff.json"
    return json.loads(path.read_text(encoding="utf-8")) if path.is_file() else None


def summarize_run(manifest: dict[str, Any], results_dir: Path) -> None:
    print("\n" + "=" * 100)
    print(f"  RUN {manifest['runUuid']}  ({manifest['scenarioName']})")
    print("=" * 100)
    print(f"  Window      {manifest['measuredStart']} -> {manifest['measuredEnd']}")
    print(
        f"  Load        {manifest['sessions']} sessions, {manifest['durationSeconds']}s, "
        f"{manifest['warmupSeconds']}s warm-up"
    )
    for key, value in manifest.get("configUnderTest", {}).items():
        print(f"  {key:<11} {value}")

    if not manifest["completed"]:
        _warn("Run did not complete cleanly -- treat these numbers with suspicion")
    if not manifest["miniProfilerAvailable"]:
        _warn("Mini-profiler was unavailable; no server-side timings or SQL counts for this run")

    client_path = results_dir / manifest["clientResultsFile"]
    if client_path.is_file():
        results = read_client_results(client_path)
        print("\n  Client-observed latency (ms), slowest first:")
        print(f"    {'request':<44} {'n':>6} {'p50':>8} {'p95':>8} {'p99':>8} {'err':>5}")
        for name, stats in sorted(results.items(), key=lambda kv: kv[1]["p95"], reverse=True)[:25]:
            print(
                f"    {name[:44]:<44} {stats['n']:>6} {stats['p50']:>8.0f} {stats['p95']:>8.0f} "
                f"{stats['p99']:>8.0f} {stats['errors']:>5}"
            )

    pg_diff = load_pg_diff(results_dir, manifest)
    if pg_diff:
        summarize_pg(pg_diff)
    else:
        print("\n  No database snapshots attached yet.")

    print("\n  Datadog, for this window:")
    print(f"    from={manifest['measuredStartEpochMillis']}  to={manifest['measuredEndEpochMillis']}")
    print("    per-query latency:  operation_name:AsyncRequest host:<host>  group by @labkey.query")
    print("    request latency:    service:tomcat_lk application:<application> type:web")
    print("    db saturation:      aws.rds.read_iops (gp3 <400GiB caps at 3000), aws.rds.disk_queue_depth")
    print("                        dbload_relative_to_num_vcpus needs Performance Insights")
    print("=" * 100)


def summarize_pg(pg_diff: dict[str, Any]) -> None:
    if pg_diff.get("pendingRestart"):
        _warn(
            "Settings were pending a restart at capture time, so this run measured the OLD configuration: "
            + ", ".join(pg_diff["pendingRestart"])
        )
    if pg_diff.get("settingsChangedDuringRun"):
        _warn("Settings changed mid-run: " + ", ".join(pg_diff["settingsChangedDuringRun"]))

    database = pg_diff["database"]
    ratio = database.get("cache_hit_ratio")
    active = database.get("active_time", 0) / 1000
    reading = database.get("blk_read_time", 0) / 1000
    print("\n  Database totals for the run:")
    # Held to 4.5% across two identical runs where client p95 moved 39%, so this is the comparable number
    print(f"    active time        {active:>14,.0f}s")
    print(f"    waiting on reads   {reading:>14,.0f}s ({100 * reading / active if active else 0:.0f}% of active)")
    print(f"    read from disk     {database.get('blks_read', 0) * 8192 / 1e9:>14,.1f} GB")
    print(f"    commits            {database.get('xact_commit', 0):>14,.0f}")
    print(f"    buffer cache hit   {ratio * 100 if ratio is not None else 0:>13.2f}%")
    print(
        f"    temp files         {database.get('temp_files', 0):>14,.0f} "
        f"({database.get('temp_bytes', 0) / 1e6:,.0f} MB spilled)"
    )
    print(f"    deadlocks          {database.get('deadlocks', 0):>14,.0f}")

    if pg_diff["statements"]:
        print("\n  Top statements by total execution time:")
        print(f"    {'calls':>8} {'total s':>10} {'mean ms':>10} {'hit%':>7}  query")
        for statement in pg_diff["statements"][:15]:
            hit = statement.get("cache_hit_ratio")
            query = " ".join(statement["query"].split())[:78]
            print(
                f"    {statement['calls']:>8,.0f} {statement.get('total_exec_time', 0) / 1000:>10.1f} "
                f"{statement.get('mean_exec_time', 0):>10.1f} "
                f"{hit * 100 if hit is not None else 0:>6.1f}%  {query}"
            )


def command_preflight(args: argparse.Namespace) -> int:
    config_path = Path(args.config).resolve()
    config = load_config(config_path)

    local_checks(config, force=args.force)
    exit_code = preflight_via_runner(config_path, config)
    print("\n  Ready." if exit_code == 0 else "\n  Preflight failed.")
    return exit_code


def attach_snapshots(results_dir: Path, manifest: dict[str, Any], before_path: Path, after_path: Path) -> None:
    before = pg_stats.PgSnapshot.read(before_path)
    after = pg_stats.PgSnapshot.read(after_path)

    if not before.has_statements() or not after.has_statements():
        _warn(
            "A snapshot has no pg_stat_statements rows. The capturing role likely lacks pg_read_all_stats, "
            "so only its own queries were visible."
        )

    if before.taken_at > manifest["measuredStart"]:
        _warn(f"BEFORE snapshot ({before.taken_at}) was taken after the run started ({manifest['measuredStart']})")
    if after.taken_at < manifest["measuredEnd"]:
        _warn(f"AFTER snapshot ({after.taken_at}) was taken before the run ended ({manifest['measuredEnd']})")

    pg_diff = pg_stats.diff(before, after)
    out = results_dir / f"{manifest['scenarioName']}-{manifest['runUuid']}-pg-diff.json"
    out.write_text(json.dumps(pg_diff, indent=2), encoding="utf-8")
    _info(f"Wrote {out.name}")

    summarize_pg(pg_diff)


def read_client_series(path: Path, request_name: str | None = None) -> list[tuple[str, str, float, str]]:
    """Every client-side request row in file order: (requestName, startTime, durationMillis, statusCode)."""
    rows: list[tuple[str, str, float, str]] = []
    with path.open(encoding="utf-8") as handle:
        header = handle.readline().rstrip("\n").split("\t")
        idx = {field: header.index(field) for field in ("requestName", "startTime", "durationMillis", "statusCode")}
        for line in handle:
            fields = line.rstrip("\n").split("\t")
            if len(fields) <= max(idx.values()):
                continue
            name = fields[idx["requestName"]].strip('"')
            if request_name and name != request_name:
                continue
            rows.append(
                (
                    name,
                    fields[idx["startTime"]].strip('"'),
                    float(fields[idx["durationMillis"]]),
                    fields[idx["statusCode"]].strip('"'),
                )
            )
    rows.sort(key=lambda r: r[1])
    return rows


def best_step(values: list[float]) -> tuple[int, float, float] | None:
    """Split a latency series into a slow prefix and a fast suffix.

    Chooses the split minimizing within-group squared deviation, which is a 1-D two-means fit. Returns
    (index of first fast sample, slow mean, fast mean), or None when no split beats treating the series as one
    group -- the case when the view was already materialized, or never finished materializing during the run.
    """
    n = len(values)
    if n < 6:
        return None

    def sse(group: list[float]) -> float:
        mean = sum(group) / len(group)
        return sum((v - mean) ** 2 for v in group)

    whole = sse(values)
    best = None
    # Require a few samples on each side so a single outlier cannot define the step.
    for k in range(3, n - 2):
        cost = sse(values[:k]) + sse(values[k:])
        if best is None or cost < best[0]:
            best = (cost, k)
    if best is None or best[0] >= whole:
        return None

    k = best[1]
    slow = sum(values[:k]) / k
    fast = sum(values[k:]) / (n - k)
    # A step, not drift: the fast group has to be clearly faster than the slow one.
    if slow <= fast * 1.5:
        return None
    return k, slow, fast


def command_materialization(args: argparse.Namespace) -> int:
    results_dir = Path(args.results_dir).resolve()
    manifest = json.loads(find_manifest(results_dir, args.run).read_text(encoding="utf-8"))
    return report_materialization(manifest, results_dir, args.request)


def report_materialization(manifest: dict[str, Any], results_dir: Path, request: str | None = None) -> int:
    client = results_dir / manifest["clientResultsFile"]
    if not client.is_file():
        _fail(f"client results not found: {client}")

    rows = read_client_series(client, request)
    if not rows:
        _fail(f"no rows for request {request!r} in {client.name}")

    names = {r[0] for r in rows}
    if len(names) > 1:
        _fail(f"{client.name} holds {len(names)} request names; pass --request to pick one: {sorted(names)}")

    name = rows[0][0]
    values = [r[2] for r in rows]
    errors = [r for r in rows if r[3] not in ("200", "302")]

    print(f"  Run           {manifest['runUuid']}  ({manifest['scenarioName']})")
    print(f"  Request       {name}")
    print(f"  Samples       {len(rows)} over {manifest['measuredStart'][11:19]}-{manifest['measuredEnd'][11:19]}")
    print(f"  Latency       min {min(values):.0f}ms  max {max(values):.0f}ms")
    if errors:
        _warn(f"{len(errors)} non-2xx response(s); a timeout records as -1 and would distort the step")

    step = best_step(values)
    if step is None:
        print()
        _warn(
            "No latency step found. Either the sample type was already materialized when the probe started "
            "(so every sample is the fast path), or materialization never finished inside the run (every sample "
            "is the live query). Check the APM 'MaterializeAsync' span for the run window to tell which."
        )
        return 0

    k, slow, fast = step
    elapsed = _seconds_between(rows[0][1], rows[k][1])
    interval = elapsed / k if k else 0
    print()
    print(f"  {'Live query (pre-materialization)':<38} {slow:>9.0f}ms  over {k} sample(s)")
    print(f"  {'Materialized':<38} {fast:>9.0f}ms  over {len(rows) - k} sample(s)")
    print(f"  {'Speed-up':<38} {slow / fast:>9.1f}x")
    print()
    print(f"  Materialization completed within {elapsed:.0f}s of the first probe request")
    print(f"    (+/- one poll interval, about {interval:.0f}s -- the probe cannot see it finish any sooner)")
    print()
    print("  This is an upper bound and it is measured under the probe's own load. For the authoritative")
    print("  duration and its breakdown, read the APM spans for this window:")
    print("    operation_name:MaterializeAsync                 total, one trace per materialization")
    print("    operation_name:labkey.materialize               group by resource_name for the phases:")
    print("      full                 SELECT INTO plus the eager indexes")
    print("      full.deferredIndexes the expensive indexes, built after readers are already served")
    print("      incremental.*        later top-ups, not part of the cold build")
    return 0


def _seconds_between(a: str, b: str) -> float:
    """Seconds between two client-TSV startTime stamps."""
    import re as _re

    def to_seconds(stamp: str) -> float:
        m = _re.search(r"(\d\d):(\d\d):(\d\d)(?:[.,](\d+))?", stamp or "")
        if not m:
            return 0.0
        h, mi, sec, frac = m.groups()
        return int(h) * 3600 + int(mi) * 60 + int(sec) + (float("0." + frac) if frac else 0.0)

    delta = to_seconds(b) - to_seconds(a)
    return delta + 86400 if delta < -1 else delta


def _load_run(results_dir: Path, run_ref: str) -> tuple[dict[str, Any], dict[str, Any] | None, Path]:
    manifest = json.loads(find_manifest(results_dir, run_ref).read_text(encoding="utf-8"))
    return manifest, load_pg_diff(results_dir, manifest), results_dir / manifest["clientResultsFile"]


SCENARIOS = ("materialization", "s2", "s4")


def command_sequence(args: argparse.Namespace) -> int:
    """Runs one variant's whole measurement set: cold-build probe, 2 sessions, 4 sessions, each with its own
    Postgres snapshots attached. One command per RDS configuration, so the procedure cannot drift between variants."""
    runs_dir = Path(args.runs_dir).resolve()
    names = [n for n in (args.only.split(",") if args.only else SCENARIOS)]
    configs = []
    for scenario in names:
        path = runs_dir / f"{args.variant}-{scenario}.json"
        if not path.is_file():
            _fail(f"No config at {path}. Expected {args.variant}-{{{','.join(SCENARIOS)}}}.json in {runs_dir}")
        configs.append((scenario, path, load_config(path)))

    base_urls = {c["baseUrl"] for _, _, c in configs}
    if len(base_urls) > 1:
        _fail(f"The configs disagree about baseUrl: {sorted(base_urls)}")
    base_url = base_urls.pop()
    results_dir = results_dir_for(configs[0][1], configs[0][2])

    print(f"\n  Sequence {args.variant}: {', '.join(n for n, _, _ in configs)}")
    # Every config is gated before anything runs, so a CHANGEME in the 4-session config fails now rather than an hour in
    for scenario, path, config in configs:
        print(f"\n  --- {path.name}")
        local_checks(config, force=args.force)

    if not args.skip_idle_check:
        print(f"\n  Checking the deployment is idle ({args.idle_seconds}s sample)...")
        rate = database_read_rate(base_url, args.idle_seconds)
        print(f"    {rate:,.0f} blocks/s read from disk")
        if rate > args.idle_threshold:
            message = (
                f"The database is reading {rate:,.0f} blocks/s, over the {args.idle_threshold:,.0f} threshold. "
                "Someone else is using this deployment. Their load competes for the same IO ceiling, and if they "
                "are writing, the data under test changes mid-series."
            )
            if args.force:
                _warn(message)
            else:
                _fail(message + "\n        Wait for it to stop, or pass --force.")

    print(f"\n  Generating load against {base_url}; up to "
          f"{sum(c.get('durationSeconds', 0) for _, _, c in configs) // 60} minutes (caps, not fixed costs).")

    completed = []
    for scenario, path, config in configs:
        print("\n" + "=" * 100)
        print(f"  {args.variant} / {scenario}")
        print("=" * 100)

        stamp = time.strftime("%Y%m%d%H%M%S")
        before = capture_pg_snapshot(base_url, results_dir / f"{config['scenarioName']}-{stamp}-pg-before.json")
        started = time.time()
        exit_code = run_simulation(path)
        manifest_path = manifest_from_run(results_dir, config["scenarioName"], started)
        if manifest_path is None:
            _fail(f"{scenario} produced no manifest (gradle exit {exit_code}); the sequence stops here so the "
                  f"remaining runs are not made against an unknown state.")
        after = capture_pg_snapshot(base_url, results_dir / f"{config['scenarioName']}-{stamp}-pg-after.json")

        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if exit_code != 0:
            _warn(f"{scenario} is flagged incomplete -- see the runner output above.")
        # Attach before summarizing so the run summary picks up the pg section instead of "none attached yet"
        attach_snapshots(results_dir, manifest, before, after)
        summarize_run(manifest, results_dir)
        completed.append((scenario, manifest))

        if scenario == "materialization":
            print()
            report_materialization(manifest, results_dir)

    index = {
        "variant": args.variant,
        "baseUrl": base_url,
        "configUnderTest": completed[0][1]["configUnderTest"],
        "runs": {scenario: manifest["runUuid"] for scenario, manifest in completed},
    }
    index_path = results_dir / f"{args.variant}-sequence.json"
    index_path.write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")

    print("\n" + "=" * 100)
    print(f"  SEQUENCE COMPLETE: {args.variant}")
    print("=" * 100)
    for scenario, manifest in completed:
        print(f"  {scenario:16} {manifest['runUuid']}  {manifest['measuredStart'][11:19]}-{manifest['measuredEnd'][11:19]}")
    print(f"\n  Index: {index_path.name}")
    elapsed = {s: (m["measuredEndEpochMillis"] - m["measuredStartEpochMillis"]) / 1000 for s, m in completed}
    truncated = [s for s, m in completed if m.get("runOnce") and not m.get("completed")]
    for scenario, manifest in completed:
        if manifest.get("runOnce"):
            print(f"  {scenario} completed {client_row_count(results_dir / manifest['clientResultsFile'])} "
                  f"requests in {elapsed[scenario]:,.0f}s")
    if "s2" in elapsed and "s4" in elapsed and elapsed["s2"]:
        # Both passes are fixed work, and s4 is exactly twice s2's, so the ratio isolates whether the extra
        # concurrency bought throughput: 1.0 is perfect scaling, 2.0 is none at all
        print(f"\n  Pass time    2 sessions {elapsed['s2']:,.0f}s   4 sessions {elapsed['s4']:,.0f}s   "
              f"ratio {elapsed['s4'] / elapsed['s2']:.2f}")
        print("    s4 does exactly twice s2's work. Ratio 1.0 = the extra sessions were free; 2.0 = no scaling.")
    if truncated:
        _warn(f"Truncated pass in: {', '.join(truncated)}. Raise 'durationSeconds' -- a partial pass is not "
              f"comparable to a complete one.")
    print("\n  Compare against another variant's runs:")
    print(f"    just compare <other-variant-s2-uuid> {dict(completed).get('s2', {}).get('runUuid', '<s2-uuid>')}")
    return 0


def client_row_count(path: Path) -> int:
    if not path.is_file():
        return 0
    with path.open(encoding="utf-8") as handle:
        return max(0, sum(1 for _ in handle) - 1)


def command_soak(args: argparse.Namespace) -> int:
    """Continuous background load. Unlike the A/B scenarios this has no end condition but the clock, captures no
    snapshots and touches no caches -- it exists to keep a deployment realistically busy, not to measure it."""
    config_path = Path(args.config).resolve()
    config = load_config(config_path)
    config["sessions"] = args.sessions
    config["durationSeconds"] = int(args.hours * 3600)
    config["runOnce"] = False
    config["scenarioName"] = f"soak-s{args.sessions}"
    for key in ("stopWhenStable", "clearCachesBeforeRun"):
        config.pop(key, None)

    results_dir = results_dir_for(config_path, config)
    local_checks(config, force=args.force)

    finish = time.strftime("%H:%M on %a %d %b", time.localtime(time.time() + config["durationSeconds"]))
    print(f"\n  Soak: {args.sessions} sessions against {config['baseUrl']} for {args.hours:g}h, ending about {finish}.")
    print(f"  Results stream to {results_dir} as requests complete.")
    print("  Interrupting is safe: the per-request TSV is already on disk, though no run manifest is written.")

    started = time.time()
    # Write the derived config next to the original so its relative activity paths still resolve
    temp_path = config_path.parent / f".soak-{config_path.name}"
    temp_path.write_text(json.dumps(config, indent=2), encoding="utf-8")
    try:
        exit_code = run_simulation(temp_path)
    finally:
        temp_path.unlink(missing_ok=True)

    manifest_path = manifest_from_run(results_dir, config["scenarioName"], started)
    if manifest_path is not None:
        summarize_run(json.loads(manifest_path.read_text(encoding="utf-8")), results_dir)
    return exit_code


def command_snapshot(args: argparse.Namespace) -> int:
    config = load_config(Path(args.config).resolve())
    path = capture_pg_snapshot(config["baseUrl"], Path(args.out))
    snapshot = pg_stats.PgSnapshot.read(path)
    print(f"  Wrote {path} ({snapshot.taken_at}, PG {snapshot.server_version_num}, "
          f"{len(snapshot.statements)} statement(s))")
    if not snapshot.has_statements():
        _warn("No pg_stat_statements rows. Either the extension is not installed, or the LabKey role lacks "
              "pg_read_all_stats -- in which case only its own statements would be visible, which is still the "
              "workload under test here.")
    return 0


def command_compare(args: argparse.Namespace) -> int:
    results_dir = Path(args.results_dir).resolve()
    baseline, baseline_pg, baseline_client = _load_run(results_dir, args.baseline)
    candidate, candidate_pg, candidate_client = _load_run(results_dir, args.candidate)

    print("=" * 104)
    print(f"  {'':<40} {'BASELINE':>28} {'CANDIDATE':>28}")
    print("=" * 104)

    divergent = [
        f"{f}: {baseline.get(f)} vs {candidate.get(f)}"
        for f in (
            "sessions",
            "durationSeconds",
            "delayBetweenActivitiesMillis",
            "maxActivityThreads",
            "baseUrl",
            "requestTimeoutSeconds",
            "runOnce",
        )
        if baseline.get(f) != candidate.get(f)
    ]
    if {a["file"]: a["sha256"] for a in baseline["activities"]} != {
        a["file"]: a["sha256"] for a in candidate["activities"]
    }:
        divergent.append("activity files differ")
    if divergent:
        _warn("These runs are NOT directly comparable: " + "; ".join(divergent))
        print()

    for label, manifest in (("baseline", baseline), ("candidate", candidate)):
        if manifest.get("runOnce"):
            elapsed = (manifest["measuredEndEpochMillis"] - manifest["measuredStartEpochMillis"]) / 1000
            print(f"  {label + ' pass':<40} {elapsed:>12,.0f}s"
                  f"{'  (TRUNCATED -- hit the cap)' if not manifest.get('completed') else ''}")
    print("  Config under test:")
    for key in sorted(set(baseline["configUnderTest"]) | set(candidate["configUnderTest"])):
        a = baseline["configUnderTest"].get(key, "-")
        b = candidate["configUnderTest"].get(key, "-")
        print(f"  {' ' if a == b else '*'} {key:<38} {a:>28} {b:>28}")

    if baseline_client.is_file() and candidate_client.is_file():
        a_results, b_results = read_client_results(baseline_client), read_client_results(candidate_client)
        print("\n  Client-observed p95 (ms), worst regression first:")
        print(f"    {'request':<44} {'baseline':>12} {'candidate':>12} {'change':>12}")
        rows = []
        for name in sorted(set(a_results) & set(b_results)):
            a, b = a_results[name]["p95"], b_results[name]["p95"]
            rows.append((name, a, b, (b - a) / a * 100 if a else 0))
        for name, a, b, change in sorted(rows, key=lambda r: r[3], reverse=True):
            print(f"    {name[:44]:<44} {a:>12.0f} {b:>12.0f} {change:>11.1f}%")

    if baseline_pg and candidate_pg:
        print("\n  Database totals:")
        print(f"    {'':<40} {'baseline':>12}   {'candidate':>12} {'change':>10}")
        for key, label, scale, suffix in [
            # active_time and blk_read_time lead because they held to 4.5% across two identical runs where
            # client p95 moved 39%; the rest are context
            ("active_time", "active time (s)", 1e-3, ""),
            ("blk_read_time", "waiting on reads (s)", 1e-3, ""),
            ("blks_read", "read from disk (GB)", 8192 / 1e9, ""),
            ("temp_bytes", "temp spilled (GB)", 1e-9, ""),
            ("cache_hit_ratio", "buffer cache hit", 100, "%"),
            ("xact_commit", "commits", 1, ""),
            ("deadlocks", "deadlocks", 1, ""),
        ]:
            a = (baseline_pg["database"].get(key) or 0) * scale
            b = (candidate_pg["database"].get(key) or 0) * scale
            change = f"{(b - a) / a * 100:>9.1f}%" if a else "        -"
            print(f"    {label:<40} {a:>12,.2f}{suffix:<2} {b:>12,.2f}{suffix} {change}")

        a_settings, b_settings = baseline_pg["notableSettings"], candidate_pg["notableSettings"]
        differing = [k for k in sorted(set(a_settings) | set(b_settings)) if a_settings.get(k) != b_settings.get(k)]
        print("\n  Settings that differ between the two captures:")
        for key in differing:
            print(f"    {key:<40} {a_settings.get(key, '-'):>12} {b_settings.get(key, '-'):>26}")
        if not differing:
            _warn("No Postgres settings differ between these runs -- was the configuration change actually applied?")
    else:
        print("\n  No database snapshots attached; comparison covers client-observed latency only.")

    print("=" * 104)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    subparsers = parser.add_subparsers(dest="command", required=True)

    p = subparsers.add_parser("preflight", help="Check the target without generating load")
    p.add_argument("config")
    p.add_argument("--force", action="store_true", help="Continue despite failed gates")
    p.set_defaults(func=command_preflight)

    p = subparsers.add_parser("run", help="Generate load and report")
    p.add_argument("config")
    p.add_argument("--force", action="store_true", help="Continue despite failed gates")
    p.set_defaults(func=command_run)

    p = subparsers.add_parser("materialization", help="Find the live-query-to-materialized latency step in a probe run")
    p.add_argument("run", help="Run UUID or scenario name prefix")
    p.add_argument("--request", help="Request name to analyze, when the run has more than one")
    p.add_argument("--results-dir", default="results")
    p.set_defaults(func=command_materialization)

    p = subparsers.add_parser("sequence", help="Run a variant's whole set: materialization, 2 sessions, 4 sessions")
    p.add_argument("variant", help="Config prefix, e.g. 'v1-baseline' for runs/v1-baseline-*.json")
    p.add_argument("--runs-dir", default="runs")
    p.add_argument("--only", help="Comma-separated scenario names, default " + ",".join(SCENARIOS) + ". Any name works if runs/<variant>-<name>.json exists, e.g. s6,s8")
    p.add_argument("--force", action="store_true", help="Continue despite failed gates, including a busy database")
    p.add_argument("--skip-idle-check", action="store_true", help="Do not sample the database for other activity")
    p.add_argument("--idle-seconds", type=int, default=20, help="How long to sample for the idle check")
    p.add_argument("--idle-threshold", type=float, default=50.0, help="Blocks/s read above which the server is busy")
    p.set_defaults(func=command_sequence)

    p = subparsers.add_parser("soak", help="Run continuous background load until the clock runs out")
    p.add_argument("--config", default="runs/example-s4.json", help="Base config to derive from")
    p.add_argument("--sessions", type=int, default=4)
    p.add_argument("--hours", type=float, default=8.0)
    p.add_argument("--force", action="store_true", help="Continue despite failed gates")
    p.set_defaults(func=command_soak)

    p = subparsers.add_parser("snapshot", help="Capture a Postgres snapshot from the server over HTTP")
    p.add_argument("config", help="Run config, for its baseUrl")
    p.add_argument("out", help="File to write")
    p.set_defaults(func=command_snapshot)

    p = subparsers.add_parser("compare", help="Compare two completed runs")
    p.add_argument("baseline", help="Run UUID, or a fragment of it")
    p.add_argument("candidate", help="Run UUID, or a fragment of it")
    p.add_argument("--results-dir", default="results")
    p.set_defaults(func=command_compare)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
