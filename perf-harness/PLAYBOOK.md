# A/B playbook: database configuration

A fixed procedure for measuring several database configurations against the same workload. The point of the fixed
procedure is that the only thing differing between two runs is the thing named in `configUnderTest`.

Read [README.md](README.md) first for what each command does. This document is the order to do them in.

## Choosing the variants

Measure the baseline first and let it say what binds, using the four CloudWatch metrics under
[Reading saturation](README.md#reading-saturation). A series then walks from "change nothing" to "remove the
constraint and then some", one dimension at a time. A typical shape:

| # | Variant | Changes vs. previous | Hypothesis |
|---|---|---|---|
| 1 | Baseline | — | reference; also the noise floor |
| 2 | Baseline + Postgres parameters | parameter group | fewer/cheaper reads for the same hardware |
| 3 | Larger gp3 volume | storage size | a higher IOPS ceiling, when the baseline is storage-bound |
| 4 | Larger instance class | instance class | more RAM caches the working set, more vCPU for whatever binds after storage |

When an instance class is in doubt, the snapshot settles it: `shared_buffers` is the RDS formula
`DBInstanceClassMemory/32768`, so multiplying it by 32768 recovers the memory the engine was actually given.

### Why a 400 GiB gp3 volume gives more IOPS

Not for the reason it would on gp2 (where IOPS scaled with size). On **RDS**, gp3 has a size threshold:

| Volume size | Baseline IOPS | Baseline throughput | Provisioned IOPS |
|---|---|---|---|
| < 400 GiB | 3,000 | 125 MiB/s | not available |
| ≥ 400 GiB | 12,000 | 500 MiB/s | up to 64,000 (PostgreSQL) |

Confirm the current numbers in the RDS console before relying on them.

Three properties of a resize shape the ordering: it applies **without a reboot**, it is **one-way** (RDS volumes grow,
never shrink), and storage can only be modified **once every 6 hours**. Every variant after a resize stays at the new
size.

## Choosing the Postgres parameters from evidence, not from a list

Do not guess at the parameter set. Run the baseline first and let its snapshot choose:

| What the snapshot shows | Parameter | Reboot? |
|---|---|---|
| low `shared_blks_hit` ratio on the top statements | `shared_buffers` (RDS default is 25% of RAM) | yes |
| `temp_blks_read`/`temp_blks_written` above zero | `work_mem` (default 4 MB) | no |
| seq scans where an index exists | `random_page_cost` 4 → 1.1, since gp3 is SSD and the default assumes a spindle | no |
| bitmap heap scans dominating | `effective_io_concurrency` 1 → 200, so one query keeps many reads in flight | no |
| planner underestimating cache | `effective_cache_size` (should be ~50–75% of RAM) | no |

For a workload pinned at an IOPS ceiling, `effective_io_concurrency` and `random_page_cost` are the two most likely to
matter: the first uses the ceiling better, the second stops the planner from choosing plans that need it.

**A variant that changes the instance class changes the RAM, so memory-derived parameters cannot be copied forward
literally.** If an earlier variant sets `shared_buffers` or `effective_cache_size` to an absolute value, recompute it
for the new instance size and say so in `configUnderTest.notes` — otherwise the variant silently tests "more RAM that
Postgres was told not to use".

## Per-variant run set

Three runs per variant, in this order. The order is not arbitrary.

| Order | Run | Why here |
|---|---|---|
| 1 | materialization probe | needs a cold cache, and clears it itself. Must precede the others |
| 2 | 2 sessions | the primary comparison point — at or below the deployment's limit |
| 3 | 4 sessions | the scaling test — does the limit move? |

Add 6- and 8-session configs (`runs/<variant>-s6.json`, `-s8.json`, run with `--only s6,s8`) once a variant scales
well at 4, since a 2-session comparison understates a CPU or memory increase on a deployment that is not saturated.

### The session runs are boxed by work, not by the clock

`runOnce` is set, so each session executes every activity exactly once and the run ends when the last one finishes.
`durationSeconds` is only a cap; a run that hits it is reported as truncated and is not comparable to a complete
pass.

This is not a detail. With **time-boxed** runs, two identical 2-session runs differed by 35–43% in requests
completed, p50, p95 and GB read, while database active time differed by 4.5%. When a few long statements dominate
database time, whether the window happens to fit five or seven calls of one moves every client-side aggregate more
than a configuration change plausibly will, because the clock cuts the same activity sequence at a different point
each time. Fixing the work removes that; the outcome becomes how long identical work took.

The probe's `durationSeconds` is also a cap: `stopWhenStable` ends the run once latency has been flat for 20 polls. A
probe that runs to the cap means the build never finished — check `operation_name:MaterializeAsync` before treating
that run as a result.

Running the materialization probe first leaves the sample type materialized, so the session runs are all in the warm
regime and comparable to every other variant's. Any other order mixes the two regimes. A variant that reboots the
instance discards the materialized table anyway, so the same order is also the only correct one there.

## Configuration files

Each variant is one file per run, named `runs/<variant>-{materialization,s2,s4}.json`; copy the `example-*` configs
to start. Across a series the following must be **byte-identical**, which is what lets `compare` treat the runs as
comparable:

- `sessions`, `durationSeconds`, `warmupSeconds`, `delayBetweenActivitiesMillis`
- `maxActivityThreads`, `requestTimeoutSeconds`, `runOnce`, `baseUrl`
- the `activities` list and the activity files themselves — the manifest records a SHA-256 of each, so editing an
  activity file mid-series invalidates every comparison that spans the edit

Only `scenarioName` and `configUnderTest` change between variants. Fill in `configUnderTest` completely:

```json
"configUnderTest": {
  "variant": "v3 gp3-400",
  "instanceClass": "db.t4g.large",
  "vcpus": 2,
  "memoryGb": 8,
  "burstable": true,
  "storageType": "gp3",
  "allocatedStorageGb": 400,
  "storageIops": 12000,
  "storageThroughputMbps": 500,
  "parameterGroup": "custom",
  "notes": "storage resized 100 -> 400 GiB, no reboot; parameters carried forward from variant 2 unchanged"
}
```

Keep the key set identical across a series so `compare` lines the values up row by row; add a key to one variant and
you must add it to all of them. `compare` prints them side by side and marks every key that differs, which is the
record of what a delta is attributable to.

`configUnderTest` is a human-written claim, so nothing depends on it being right: the authority on which settings a
run actually measured is the `pg_settings` dump in the snapshot attached to that run. `compare` diffs those and says
so when two runs turn out to have been identically configured.

## Procedure per variant

**1. Apply the change.** In the RDS console. Wait for status `Available`, and for a storage change also wait out the
`Optimizing` period — a resize that is still optimizing is not the configuration you meant to test.

**2. Confirm it took effect.** The harness cannot see this. For a parameter change, check for `pending_restart` in
the console; the snapshots attached to each run also surface it after the fact. For a reboot-requiring change,
confirm the instance actually rebooted.

**3. Run the whole set.**

```bash
just sequence <variant>
```

That is the entire measurement procedure. In order, it:

- loads all three configs and runs the local gates on each **before generating any load**, so a `CHANGEME` left in
  the 4-session config fails in a second rather than 20 minutes in;
- samples the database for 20 seconds and refuses to start if it is reading more than 50 blocks/s, because someone
  else's job both competes for the IO ceiling and changes the data under test;
- asks for confirmation once, not three times;
- for each of materialization, s2, s4: captures a Postgres snapshot, runs, captures another, summarizes, and attaches
  the pair;
- reports the materialization step for the probe run;
- writes `results/<variant>-sequence.json` holding the three run UUIDs.

Useful flags: `--only s2` to redo one scenario, `--skip-idle-check` when you know the other traffic is yours,
`--force` to proceed past a gate anyway.

If a run produces no manifest the sequence stops rather than continuing, since the runs after it would be measuring
an unknown state.

**4. Record the four Datadog numbers for each run's window.** The run summaries print the window as `from=`/`to=`
epoch milliseconds. Per [Reading saturation](README.md#reading-saturation), collect `read_iops` (avg and peak),
`disk_queue_depth` (avg and peak), `cpuutilization` (avg and peak), and `read_throughput`. These are what say
*whether the constraint moved*, which the client-side numbers alone cannot. This is the one step still done by hand.

## Establish the noise floor first

Before the second variant, **run the baseline's 2-session config a second time, changing nothing**, and compare the
two:

```bash
just sequence <baseline> --only s2
just compare <baseline-s2-run-a> <baseline-s2-run-b>
```

`compare` warns when no settings differ, which is expected here — this run exists to measure how much two identical
runs differ. Any later variant's improvement smaller than that spread is not a result. On a burstable instance
sharing a tenant that spread can be large.

## What to compare, and in what order of importance

```bash
just compare <baseline-s2-uuid> <variant-s2-uuid>
```

**Primary — how long did identical work take?** Pass elapsed time at 2 sessions, and database active time for the
same run. Database active time is the more trustworthy of the two when a few statements dominate.

**Also primary — did concurrency start paying?** The ratio of the 4-session pass time to the 2-session pass time. s4
does exactly twice the work of s2, so 2.0 means the extra sessions bought nothing and 1.0 means they were free.

**Secondary — is it faster for one user?** Client p50 and p95 at 2 sessions. Treat a change inside the noise floor as
unproven: per-request percentiles rest on a handful of samples per request name.

**Tertiary — where did the time go?** Read IOPS and disk queue depth. If queue depth stays high, storage is still the
limit whatever else changed. If queue depth collapses and CPU rises toward 100%, the bottleneck has moved to CPU.

**Materialization, separately.** `just materialization <uuid>` per variant gives the live-query cost, the
materialized cost, and a bound on the build time. Compare build times across variants; this is the one measurement
that is not affected by the session-count question at all.

## Time budget

Per variant, allow for the probe (until `stopWhenStable` ends it), one 2-session pass, one 4-session pass (about twice
as long), a minute of snapshots and idle check, and 10–30 minutes of RDS changes and settling. The 4-session passes
are the first thing to drop if time is short — the 2-session pass answers "is it faster", and only the ratio between
the two answers "does it scale".

## Recording results

Keep one table, appended as you go, so a variant that was run days apart from its comparison is still readable:

| Variant | run UUID (s2) | pass @2 | pass @4 | ratio | p50 @2 | p95 @2 | IOPS avg/peak | queue avg | CPU avg | build time |
|---|---|---|---|---|---|---|---|---|---|---|

The run UUID is the join key back to the manifest, both TSVs, and the `pg-diff.json`, all of which stay in `results/`.
