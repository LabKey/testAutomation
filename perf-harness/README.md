# perf-harness

Generates reproducible load against a deployed LabKey server and measures what it costs, so that database
configuration — RDS instance class, Postgres parameters, storage type — can be A/B tested.

The load generator is the Java stress harness in `../src/org/labkey/test/stress/`.
This directory adds the run lifecycle around it: preconditions, result summarization, and comparison.

## Why it is built this way

Because the variable under test is the database rather than the application, the hard part is not generating load but
keeping everything except the database identical between runs. Most of what follows exists for that reason.

**This harness has no database credentials**, which shapes the measurement stack. What it can do is ask the server,
which has them: `admin-postgresSnapshot.view` returns `pg_stat_statements`, the `pg_settings` dump and the database
counters as one JSON document, for anyone holding Troubleshooter in the root container.

| Evidence | Source | Needs |
|---|---|---|
| Client wall-clock per request | harness TSV | nothing |
| Server-side duration per request | LabKey mini-profiler | Troubleshooter, profiler enabled |
| SQL attributed to an individual request | Datadog APM `type:sql`, matched on `@thread.name` | nothing |
| Latency per LabKey query | Datadog APM `AsyncRequest` spans, `@labkey.query` | nothing |
| Latency per SQL statement | Datadog APM `type:sql` spans | nothing (trace-sampled) |
| DB saturation, CPU, IO latency | CloudWatch `aws.rds.*` | nothing |
| Per-statement cache behavior, temp spill, settings in effect | `admin-postgresSnapshot.view` | Troubleshooter |

Where Datadog Database Monitoring is not enabled there is no per-statement database telemetry in Datadog. That gap
is filled by the snapshots described under [Database evidence](#database-evidence).

Where Performance Insights is off, `aws.rds.dbload*` returns nulls rather than an error -- read a quiet result there
as "not collected", never as "not loaded". Saturation has to be read from the plain CloudWatch metrics
instead; see [Reading saturation](#reading-saturation).

## Setup

`activities/` and all of `runs/` except the `example-*` configs are gitignored: recordings and run configs name a
deployment's containers, sample types and data, and this repository is public. Copy the examples to start a series.

```bash
just init
export LABKEY_STRESS_APIKEY=<API key for the target server>
```

The credential is read only from the environment, never from a config file, and never written to any output.

Enable the profiler at Admin Console → Mini Profiler on the target server. Reading it needs only the Troubleshooter
permission, not site admin.

## Impersonation

Skip this when the credential's user can see the data directly. A troubleshooter-style credential can authenticate and reach the API but sees none of the data. The run config takes
an `impersonate` block, and every simulation's connection adopts those permissions before issuing any request:

```json
"impersonate": { "container": "/Home", "group": "Administrators" }
```

The group is resolved against the groups the credential is *actually* permitted to impersonate, so a name that cannot
be impersonated fails with the list of ones that can. Use `"groupId"` instead to skip the lookup. Preflight then
asserts the session really is impersonating, rather than trusting that the call succeeded.

Data visibility depends on it: without impersonation requests return empty results rather than errors, so a run looks
successful and measures nothing.

## Building activity files

An *activity* is one user action, expressed as the set of API requests the browser actually makes. Datadog shows which
endpoints matter but never their POST bodies, and for `query-getQuery.api` the body is the entire experiment — schema,
query, view, filters, sort, container filter, column list. So activities are recorded from a real browser session
rather than written by hand.

1. Set the app up so the workflow is reproducible on the target.
2. Open DevTools, clear the network log, perform the single action, and export the log as HAR.
3. Convert it:

```bash
just convert-har ~/Downloads/recording.har activities/some-action.xml
```

The converter reads the server root off the recording itself, keeps GET and POST requests to LabKey actions, drops
static resources, and replaces container paths with `@@CONTAINER@@` placeholders. It prints the mapping it chose; put
those values in the run config's `replacements`. `@@USERID@@` is filled in automatically from the credential's user.

Pass `-PharBaseUrl=` to the gradle task if the deployment has a context path, which the origin alone won't capture.

Record one HAR per action, not one long session; each session runs the activity files in the order the run config lists them.

## Running

One variant's whole measurement set is a single command, which reads `runs/<variant>-{materialization,s2,s4}.json`:

```bash
just sequence example
```

It gates every config before generating any load, samples the database to confirm nobody else is using the
deployment, then runs the materialization probe, the 2-session run and the 4-session run in that order, capturing and
attaching Postgres snapshots around each. [PLAYBOOK.md](PLAYBOOK.md) covers the series these fit into.

The commands below are those same steps, run individually.

### Background load

To keep a deployment realistically busy rather than to measure it:

```bash
just soak            # 4 sessions, 8 hours
just soak 2 0.5      # 2 sessions, 30 minutes
```

Derived from `runs/example-s4.json` by default (`--config` picks another): the same activities as the A/B scenarios, but `runOnce` off so sessions cycle the
activities continuously, `durationSeconds` as the run length rather than a cap, no cache clearing, and no early stop.
Results stream to `results/soak/` so hours of request rows cannot be confused with the comparison runs. Interrupting
it is safe -- the per-request TSV is written as requests complete -- but no run manifest is produced, so an
interrupted soak cannot be fed to `compare`.

A closed-loop model: 4 sessions means up to 24 requests in flight (`sessions` x `maxActivityThreads`), and the
sessions wait rather than piling on when the server slows, so it cannot overload a deployment into errors the way a
fixed arrival rate would.

```bash
just preflight runs/example-s2.json   # check without generating load
just run       runs/example-s2.json
```

`preflight` checks the config locally, then hands off to the Java runner for anything needing a server session --
authenticating, impersonating, probing the mini-profiler, and verifying any configured cache names. It refuses to
proceed if `configUnderTest` is unfilled or if impersonation was requested but did not take effect. `--force`
overrides the local gates.

**`run` does not check whether anyone else is using the server** (`sequence` does). Background traffic during a run is indistinguishable
from the effect of whatever is being varied, so confirm the deployment is idle before starting -- and remember that
your own browsing counts, even though a page or two of admin console is negligible load next to the workload.

Each run writes, keyed by run UUID:

| File | Contents |
|---|---|
| `*-run.json` | the manifest: workload parameters, activity file digests, `configUnderTest`, measured window |
| `*-client.tsv` | one row per request, client-observed latency and status |
| `*.tsv` | mini-profiler detail — server-side duration and SQL count per request |

## Database evidence

`just sequence` captures a snapshot before and after every run and attaches it, so none of this is needed by hand.
What follows is for one-off runs.

```bash
just snapshot runs/example-s2.json pg-before.json
```

That GETs `admin-postgresSnapshot.view` with the harness's own API key. It is read-only, works across Postgres major
versions, needs Troubleshooter in the root container, and exists only where the primary database is Postgres. It
fails loudly on a server that predates the action rather than writing a login page to the file.

The snapshot runs as the LabKey database role, which usually lacks `pg_read_all_stats`, so expect the per-statement
section to be empty unless that role has been granted it; everything else is complete. `snapshot` says so when it is.
That section matters because `shared_blks_read` per statement is what tells you *which* queries are driving the IO,
and it is how the playbook picks the Postgres parameters to test.

A Troubleshooter can also download the same document from the Admin Console -- **postgres snapshot (download)** under
Diagnostics.

`sequence` warns if a snapshot was taken outside the measured window, and surfaces the one thing nothing else can:
whether any setting was still `pending_restart`, meaning the run measured the *old* configuration.

## Sample type materialization

A sample type's data is served from a materialized temp table. The table is built after a server restart, or after the
cache is cleared manually or ages out (8 days). The next request for that sample type's data triggers the build on a
background thread, and every request until it finishes is served by a **live query** against the unmaterialized data.
So there are two performance regimes, and a run that straddles them is measuring both at once.

This matters for A/B work beyond the cost of the build itself: **changing a Postgres parameter requires a reboot**,
which discards the materialized table, so every post-reboot run starts in the live-query regime whether or not that
was intended.

Measure the build on its own, before the main workload:

```bash
just preflight runs/example-materialization.json
just run       runs/example-materialization.json
just materialization <run-uuid>
```

The run clears the cache itself, via `clearCachesBeforeRun`, so the cold state and the load that measures it are one
step and nothing else can touch the sample type in between:

```json
"clearCachesBeforeRun": ["materialized sample types"]
```

Each name is POSTed to `admin-clearCaches` as a `debugName`, which clears that one named cache and leaves every other
cache, the Introspector caches and the search queues alone. `"materialized sample types"` is the `BlockingCache` of
`_MaterializedQueryHelper` keyed by sample type LSID -- the same cache the 8-day expiry acts on -- so dropping it makes
the next read of any sample type rebuild its temp table.

Three things to know, each of which has already bitten:

- **`debugName` binds from form-encoded parameters, not from a JSON body.** Post it the way the Caches page does,
  `debugName=materialized%20sample%20types` as `application/x-www-form-urlencoded`. With a JSON body the field stays
  null, and the action then falls through every branch and returns `{"success": true}` having cleared nothing. This is
  why the harness uses `ApiTestCommand` with `post_form` rather than `SimplePostCommand`.
- **Success does not mean it cleared.** The action reports success whether or not the name matched anything.
  Preflight checks each configured name against the Cache Statistics page, but only warns: caches are created on
  first use, so one that nothing has touched since the last restart is legitimately absent from that page and will
  exist by the time the workload asks for it. A warning therefore means "unverifiable", not "wrong" -- if a run that
  should have started cold looks warm, a typo'd `debugName` is the first thing to check.
- **It may not be permitted.** `admin-clearCaches` is an `@AdminConsoleAction`: the container must be root, a GET needs
  only Troubleshooter, but a POST needs AdminPermission there. If it returns 401/403 the run stops with that message,
  and an admin has to clear the cache by hand (Admin Console -> Caches) immediately before the run, or restart.

Only the probe config sets this. The sweep and baseline configs deliberately do not: they measure steady-state warm
performance, and starting them cold would mix the two regimes back together.

The probe is one session polling one request -- `activities/materialization-probe.xml`, a recorded total-count query
against the sample type. `materialization` reads the per-request time series out of the client TSV and splits it into a
slow prefix and a fast suffix, which gives the live-query cost, the materialized cost, and an upper bound on when the
build finished.

`warmupSeconds` must stay 0: a warm-up would trigger the build during the discarded phase and there would be nothing
left to observe. `delayBetweenActivitiesMillis` is deliberately long (10s) because each poll runs a full live query
during the cold regime, and on a 2-vCPU instance that competes with the build it is trying to time.

Once the table is built the probe is polling a flat line, so `durationSeconds` is a cap and `stopWhenStable` ends the
run when the line goes flat:

```json
"durationSeconds": 2400,
"stopWhenStable": { "samples": 20, "maxRatioToPeak": 0.1 }
```

A request counts as fast at or under `maxRatioToPeak` of the slowest request seen so far -- relative to the observed
peak, because the fast-path latency differs per instance class and is the thing being measured. Twenty in a row is
what distinguishes the end from a dip: an observed build dropped to 1.4s for three polls before its slowest phase, so
a shorter run would have ended the probe mid-build. Against that recording, `samples: 20` ends the run at t+506s
instead of t+2400s.

Only the probe sets this. Never set it on a run whose numbers are aggregates over the window -- a flat tail is the
normal state of a healthy load run, so it would end those runs early and silently shorten the measured window.

### In APM

The build is instrumented directly, so its duration needs no database access:

```
operation_name:MaterializeAsync host:<host>        # one trace per build; @duration is the total
operation_name:labkey.materialize host:<host>      # group by resource_name for the phases
```

`labkey.materialize` resource names are `full` (SELECT INTO plus eager indexes), `full.deferredIndexes` (the expensive
indexes, built after readers are already being served from the table) and `incremental.*` (later top-ups, not part of
a cold build). It carries `DDTags.MEASURED`, so trace metrics exist for it.

Two things to know before trusting these:

- **`@labkey.materialized_view` does not identify the sample type.** Sample types build the helper with an empty
  prefix, so the tag and the resource name are both the class name for every one of them. Use
  `@labkey.triggering_trace_id`, which links back to the request that kicked the build off -- and that request carries
  the harness's `_test=<request name>` parameter.
- **Zero rows may mean the build predates the instrumentation** rather than that no materialization happened. Confirm
  the emitting build is deployed before reading an empty result as a negative.

## Comparing

```bash
just compare <baseline-uuid> <candidate-uuid>
```

Compares client-observed p95 per request, database totals, and which Postgres settings actually differed between the
two captures. It warns loudly when the two manifests disagree about anything other than `configUnderTest` — different
session counts or activity digests mean the runs are not comparable — and when *no* settings differ, which
usually means the change never took effect.

## Keeping runs comparable

The manifest pins the workload; these are the things it cannot pin.

- **Verifying the change is live.** Without database access nothing here can confirm the configuration under test is
  actually in effect. Confirm in the RDS console that the change applied *and* that the instance rebooted if the
  parameter required it. `sequence` catches this after the fact from the snapshots it attaches.
- **Data drift.** Runs that write (imports, inserts, deletes, workflow actions) leave the database different from how
  they found it, and that difference is confounded with the configuration change. Restore from an RDS snapshot between
  runs in a series.
- **Cold cache.** Changing an instance class or a non-dynamic parameter reboots the instance, emptying both
  `shared_buffers` and the page cache. Use `warmupSeconds`; its results are discarded.
- **CPU burst credits.** A burstable instance class (`db.t4g.*`) spends credits above its baseline utilization, so a
  run's result depends on what ran before it. A storage-bound instance barely spends any, since it waits on IO rather
  than burning CPU; re-check `aws.rds.cpucredit_balance` once storage stops being the constraint.
- **EBS burst credits.** `aws.rds.ebsiobalance` and `aws.rds.ebsbyte_balance` track the burst *balance*, not the
  baseline: a volume can be pinned at its IOPS ceiling all day with the balance
  untouched, so a healthy balance is not evidence of healthy storage.
- **Autovacuum.** Heavy insert/delete churn leaves vacuum work outstanding that may land during the next run.
- **Leftover queries from an aborted run.** Dropping the client does not stop Postgres; an aborted run can keep the
  database busy for minutes after the harness exits, with the app server idle. Wait for
  `aws.rds.read_iops` to fall to zero before starting the next run -- "the harness exited" is not "the server is
  idle".

## Correlating with Datadog

The run summary prints the measured window. Every LabKey deployment reports `env:prod` and `service:tomcat_lk`, so
`application` or `host` is what distinguishes them.

Long-running reads run on a background thread that emits a dedicated span, which is the closest thing to per-query
timing available without database access:

```
operation_name:AsyncRequest host:<host>          # group by @labkey.query for per-query p50/p95/max
service:tomcat_lk application:<application> type:web    # request latency by resource
host:<host> type:sql                             # group by @thread.name, or SUM @duration for total DB time
```

### Reading saturation

`aws.rds.dbload_relative_to_num_vcpus` needs Performance Insights, so without it it reports nothing. Use
these four together -- no one of them identifies the bottleneck alone:

```
aws.rds.read_iops                 # against the volume's ceiling: gp3 under 400 GiB is capped at 3,000
aws.rds.disk_queue_depth          # sustained above ~5 means requests are waiting on storage
aws.rds.cpuutilization            # of the whole instance, so 50% on 2 vCPUs can mean one core pinned
aws.rds.read_throughput           # divide by read_iops for average read size
```

Two traps this combination avoids:

- **Low CPU does not mean idle.** A database pinned at its IOPS ceiling can sit under 40% CPU. CPU only measures the bottleneck
  when the bottleneck is CPU.
- **A high per-statement duration does not locate the cost.** Individual `type:sql` spans ran 240-413 seconds while
  the instance did no more work than a laptop; the time was queueing, not execution.

**Do not use the mini-profiler's `queryCount` as a measure of database work.** It counts only SQL issued on the
request's own thread, and LabKey runs the actual query on an `AsyncQueryRequest` background thread. A validation run
recorded 13 statements over 45 requests where APM saw 132 sql spans; every count after the first pass was zero, which
looks exactly like result caching but is not.

Those background threads are named `AsyncQueryRequest: <thread> for <url>`, and the harness appends a `_test=<request
name>` parameter to every URL it issues. Grouping sql spans by `@thread.name` therefore attributes database work to
individual activity requests — the per-request DB attribution the mini-profiler cannot give.

Sizing a run: sum `@duration` over `type:sql` for a run's window to get total database time, and divide by wall-clock
seconds and session count to get DB-seconds per second per session. An activity that produces very little of it cannot
differentiate database configurations no matter how carefully the run is controlled.

Span durations are in nanoseconds.
