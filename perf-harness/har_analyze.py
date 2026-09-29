"""Inspect browser HAR recordings and the activity files derived from them.

Answers the questions that come up between recording a HAR and wiring it into a run config: is any of it already
covered by an existing activity, and what load would the resulting run produce.

Request durations come from the recordings rather than the activity files, so `project` needs the HARs kept
around. When several recordings contain the same request it reports the spread, which is the only measure of
run-to-run noise available without doing a run.
"""

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import parse_qsl, urlparse

STATIC_SUFFIXES = (
    ".js",
    ".css",
    ".svg",
    ".png",
    ".woff2",
    ".woff",
    ".ttf",
    ".gif",
    ".ico",
    ".jpg",
    ".jpeg",
    ".image",
)

RESET, BOLD, DIM, RED, YELLOW, GREEN, CYAN = (
    "\033[0m",
    "\033[1m",
    "\033[2m",
    "\033[31m",
    "\033[33m",
    "\033[32m",
    "\033[36m",
)


def _fail(message):
    print(f"{RED}ERROR{RESET} {message}", file=sys.stderr)
    sys.exit(1)


# ---------------------------------------------------------------------------
# Request signatures
#
# A signature has to match a HAR entry against a <test> in an activity file, where the container path has been
# replaced by a placeholder and JSON bodies have been pretty-printed. So it keys on the action name plus the
# normalized body, and never on the path prefix.
# ---------------------------------------------------------------------------


def _normalize_body(text):
    text = (text or "").strip()
    if not text:
        return ""
    if text.startswith("{") or text.startswith("["):
        try:
            return json.dumps(json.loads(text), sort_keys=True, separators=(",", ":"))
        except ValueError:
            return re.sub(r"\s+", "", text)
    return "&".join(sorted(f"{k}={v}" for k, v in parse_qsl(text, keep_blank_values=True)))


def _normalize_query(query):
    if not query:
        return ""
    return "&".join(sorted(f"{k}={v}" for k, v in parse_qsl(query, keep_blank_values=True)))


def signature(action, query, body):
    return f"{action}?{_normalize_query(query)}|{_normalize_body(body)}"


def describe(action, query, body):
    """A short human label: the LabKey query being run, plus whichever filter parameters distinguish it."""
    action = action.replace("query-", "")
    params = (
        dict(parse_qsl(body or "", keep_blank_values=True))
        if "=" in (body or "") and not (body or "").strip().startswith("{")
        else {}
    )
    if params:
        schema = params.get("schemaName", "")
        name = params.get("query.queryName") or params.get("queryName") or ""
        label = f"{schema}.{name}" if name else action
        interesting = [
            f"{k.replace('query.', '')}={v[:30]}"
            for k, v in sorted(params.items())
            if k.startswith("query.") and k not in ("query.queryName", "query.columns")
        ]
        return f"{action} {label} {' '.join(interesting)}".strip()
    if (body or "").strip().startswith("{"):
        try:
            payload = json.loads(body)
            name = payload.get("queryName") or payload.get("schemaName") or ""
            return f"{action} {name}".strip()
        except ValueError:
            pass
    return f"{action} {_normalize_query(query)[:60]}".strip()


# ---------------------------------------------------------------------------
# Loading
# ---------------------------------------------------------------------------


class Request:
    __slots__ = ("index", "started", "action", "query", "body", "millis", "status", "path", "host", "size")

    def __init__(self, index, entry):
        request = entry["request"]
        url = urlparse(request["url"])
        self.index = index
        self.started = entry.get("startedDateTime", "")
        self.path = url.path
        self.host = url.netloc
        self.action = url.path.rstrip("/").split("/")[-1]
        self.query = url.query
        self.body = request.get("postData", {}).get("text", "")
        self.millis = entry.get("time", 0.0)
        self.status = entry.get("response", {}).get("status", 0)
        self.size = entry.get("response", {}).get("content", {}).get("size", 0)

    @property
    def seconds(self):
        return self.millis / 1000.0

    @property
    def signature(self):
        return signature(self.action, self.query, self.body)

    @property
    def label(self):
        return describe(self.action, self.query, self.body)


def load_har(path, primary_host=None):
    """Returns (api_requests, dropped_counter). Static assets and third-party hosts are dropped, as the
    converter drops them too, so what is left is what an activity file could contain."""
    try:
        log = json.loads(Path(path).read_text())["log"]
    except (OSError, ValueError, KeyError) as e:
        _fail(f"could not read HAR {path}: {e}")

    entries = log["entries"]
    if primary_host is None:
        hosts = Counter(urlparse(e["request"]["url"]).netloc for e in entries)
        primary_host = hosts.most_common(1)[0][0] if hosts else ""

    requests, dropped = [], Counter()
    for i, entry in enumerate(entries):
        request = Request(i, entry)
        if request.host != primary_host:
            dropped["third-party host"] += 1
        elif any(request.path.endswith(s) for s in STATIC_SUFFIXES):
            dropped["static asset"] += 1
        elif request.path.endswith(".view") or request.action == "notifications":
            dropped["page/websocket"] += 1
        else:
            requests.append(request)
    return requests, dropped, primary_host


def load_activity(path):
    """Returns [(test name, signature, label)] for one activity XML file."""
    text = Path(path).read_text()
    out = []
    for match in re.finditer(
        r'<test\s+name="([^"]*)"[^>]*>\s*<url>(.*?)</url>(?:\s*<formData>(.*?)</formData>)?',
        text,
        re.DOTALL,
    ):
        name = match.group(1)
        url = re.sub(r"<!\[CDATA\[|\]\]>", "", match.group(2)).strip()
        body = re.sub(r"<!\[CDATA\[|\]\]>", "", match.group(3) or "").strip()
        parsed = urlparse(url)
        action = parsed.path.rstrip("/").split("/")[-1]
        out.append((name, signature(action, parsed.query, body), describe(action, parsed.query, body)))
    return out


def activity_files(directory):
    return sorted(Path(directory).glob("*.xml"))


# ---------------------------------------------------------------------------
# diff
# ---------------------------------------------------------------------------


def command_diff(args):
    requests, _, _ = load_har(args.har)
    covered = {}
    for path in activity_files(args.activities):
        for name, sig, _ in load_activity(path):
            covered.setdefault(sig, []).append(f"{path.name}:{name}")

    known = [r for r in requests if r.signature in covered]
    novel = [r for r in requests if r.signature not in covered]

    print(f"{BOLD}{args.har}{RESET} vs activity files in {args.activities}")
    print(
        f"  {len(requests)} API requests: {GREEN}{len(known)} already covered{RESET}, " f"{BOLD}{len(novel)} new{RESET}"
    )

    if known:
        by_file = Counter(covered[r.signature][0].split(":")[0] for r in known)
        print(f"\n  {BOLD}already covered by{RESET}")
        for name, count in by_file.most_common():
            print(f"    {count:3}  {name}")

    if novel:
        print(f"\n  {BOLD}new requests{RESET}")
        for r in novel:
            flag = f" {RED}[{r.seconds:.0f}s]{RESET}" if r.seconds >= args.slow else ""
            print(f"    har[{r.index:3}] {r.seconds:7.2f}s  {r.label[:78]}{flag}")
        print(f"\n  {DIM}Sum of new request time: {sum(r.seconds for r in novel):.1f}s{RESET}")
    else:
        print(f"\n  {YELLOW}Nothing new.{RESET} This recording adds no coverage -- do not wire it.")


# ---------------------------------------------------------------------------
# project
# ---------------------------------------------------------------------------


def command_project(args):
    config_path = Path(args.config)
    try:
        config = json.loads(config_path.read_text())
    except (OSError, ValueError) as e:
        _fail(f"could not read config {config_path}: {e}")

    # Durations live in the recordings, not the activity files, so recover them by signature.
    samples = defaultdict(list)
    labels = {}
    hars = sorted(Path(args.hars).glob("*.har"))
    if not hars:
        _fail(f"no .har files in {args.hars}; project needs the recordings for request durations")
    for har in hars:
        requests, _, _ = load_har(har)
        for r in requests:
            samples[r.signature].append((har.name, r.seconds))
            labels[r.signature] = r.label

    delay = config.get("delayBetweenActivitiesMillis", 5000) / 1000.0
    threads = config.get("maxActivityThreads", 6)
    sessions = config.get("sessions", 10)
    duration = config.get("durationSeconds", 0)
    timeout = config.get("requestTimeoutSeconds")

    print(f"{BOLD}{config_path}{RESET}")
    print(
        f"  {sessions} sessions x {threads} activity threads = up to {BOLD}{sessions * threads} "
        f"concurrent requests{RESET}"
    )
    print(
        f"  {duration}s measured, {config.get('warmupSeconds', 0)}s warmup, "
        f"{delay:.0f}s between activities, timeout {timeout}s"
    )
    print(f"  durations recovered from {len(hars)} recording(s) in {args.hars}\n")

    print(
        f"  {'activity':26} {'reqs':>5} {'miss':>5} {'sum':>8} {'longest':>8} " f"{'iter':>7} {'req-s/s':>8}"
    )
    unmatched_total = 0
    rows = []
    for entry in config.get("activities", []):
        path = (config_path.parent / entry["file"]).resolve()
        if not path.exists():
            _fail(f"activity file missing: {path}")
        durations, unmatched = [], 0
        for name, sig, label in load_activity(path):
            if sig in samples:
                durations.append((label, max(s for _, s in samples[sig])))
            else:
                unmatched += 1
        if not durations:
            print(f"  {path.name:26} {RED}no requests matched any recording{RESET}")
            continue
        total = sum(d for _, d in durations)
        longest = max(d for _, d in durations)
        iteration = longest + delay
        unmatched_total += unmatched
        miss = f"{unmatched}" if unmatched else "-"
        rows.append((path.name, total, longest, iteration))
        print(
            f"  {path.name:26} {len(durations):>5} {miss:>5} {total:>7.1f}s "
            f"{longest:>7.1f}s {iteration:>6.1f}s {total / iteration:>8.2f}"
        )

    if not rows:
        _fail("nothing to project")

    # Each session runs every activity in order, so a pass is the sum of the iterations
    pass_work = sum(total for _, total, _, _ in rows)
    pass_wall = sum(iteration for _, _, _, iteration in rows)
    print(f"\n  {BOLD}pass{RESET}  every activity once per session, in order")
    print(f"  one pass  {pass_wall / 60:.1f} min wall clock, {pass_work:.0f} request-sec of work per session")
    print(
        f"  {BOLD}rate{RESET}  {pass_work / pass_wall:.2f} request-sec/sec/session, "
        f"{pass_work / pass_wall * sessions:.0f} aggregate at {sessions} session(s)"
    )
    if not config.get("runOnce"):
        print(
            f"\n  {YELLOW}'runOnce' is not set{RESET} -- sessions loop until durationSeconds ({duration}s) cuts one "
            f"mid-pass, so the amount of work differs between runs."
        )
    elif duration < pass_wall * 1.5:
        print(
            f"\n  {YELLOW}durationSeconds ({duration}s) leaves little headroom over a {pass_wall:.0f}s "
            f"pass{RESET} -- it is a cap here, and a truncated pass is not comparable to a complete one."
        )

    print(f"\n  {DIM}request-sec is an upper bound on DB-sec: it includes app-server time, and for light{RESET}")
    print(f"  {DIM}activities has overstated measured DB time by roughly 6x. Only an APM `type:sql`{RESET}")
    print(f"  {DIM}duration sum over a real run's window gives DB-seconds.{RESET}")

    if unmatched_total:
        print(
            f"\n  {YELLOW}{unmatched_total} request(s) across all activities matched no recording{RESET} "
            f"-- their cost is missing from the totals above."
        )

    spread = [(sig, s) for sig, s in samples.items() if len({n for n, _ in s}) >= 2]
    if spread:
        print(f"\n  {BOLD}run-to-run spread{RESET} (requests captured in more than one recording)")
        ranked = []
        for sig, s in spread:
            values = [v for _, v in s]
            if max(values) >= 1.0:
                ranked.append((max(values) / max(min(values), 0.001), min(values), max(values), len(values), sig))
        for ratio, lo, hi, n, sig in sorted(ranked, reverse=True)[:10]:
            print(f"    {ratio:5.2f}x  {lo:6.2f}s -> {hi:6.2f}s  (n={n})  {labels.get(sig, sig)[:52]}")
        if ranked:
            print(f"\n  {DIM}These are the same request with no configuration change between recordings.{RESET}")
            print(f"  {DIM}Any A/B difference smaller than this spread needs a measured noise floor first:{RESET}")
            print(f"  {DIM}run the baseline twice unchanged and compare those two runs.{RESET}")


# ---------------------------------------------------------------------------


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("diff", help="how much of a recording existing activities already cover")
    p.add_argument("har")
    p.add_argument("--activities", default="activities")
    p.add_argument("--slow", type=float, default=60.0)
    p.set_defaults(func=command_diff)

    p = sub.add_parser("project", help="load a run config's activities would generate")
    p.add_argument("config")
    p.add_argument("--hars", default=str(Path.home() / "Downloads"), help="directory holding the recordings")
    p.set_defaults(func=command_project)

    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == "__main__":
    sys.exit(main())
