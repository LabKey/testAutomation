"""Analysis of PostgreSQL snapshots returned by the server's `admin-postgresSnapshot.view` action.

Nothing here connects to a database. The perf harness has no database access to the servers under test; it asks the
server for snapshots as JSON files and attaches them to a run.

Datadog Database Monitoring is not enabled for these instances, so these snapshots are the only source of per-statement
evidence. When they are unavailable a run still measures client latency and, via APM, per-query latency -- but not
cache behavior, temp spill, or whether the configuration under test was actually in effect.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

# Settings worth surfacing in a report. The snapshot always contains the full pg_settings dump.
NOTABLE_SETTINGS = [
    "server_version",
    "shared_buffers",
    "effective_cache_size",
    "work_mem",
    "maintenance_work_mem",
    "max_connections",
    "random_page_cost",
    "seq_page_cost",
    "effective_io_concurrency",
    "max_parallel_workers_per_gather",
    "max_parallel_workers",
    "max_worker_processes",
    "jit",
    "checkpoint_completion_target",
    "max_wal_size",
    "min_wal_size",
    "wal_buffers",
    "default_statistics_target",
    "autovacuum",
    "autovacuum_max_workers",
    "autovacuum_vacuum_cost_limit",
    "track_io_timing",
]

# Not counters, so differencing them is meaningless
_NON_COUNTER_KEYS = {"query", "queryid", "datid", "datname", "stats_reset", "userid", "dbid", "toplevel"}


@dataclass
class PgSnapshot:
    """One capture of cumulative counters and configuration, as returned by admin-postgresSnapshot.view."""

    taken_at: str
    server_version_num: int
    statements: dict[str, dict[str, Any]]
    database: dict[str, Any]
    checkpointer: dict[str, Any]
    settings: dict[str, dict[str, Any]]

    @classmethod
    def read(cls, path: Path) -> PgSnapshot:
        data = json.loads(path.read_text(encoding="utf-8"))
        missing = {"takenAt", "serverVersionNum", "settings", "statements"} - set(data)
        if missing:
            raise ValueError(
                f"{path} is not a postgres snapshot (missing {', '.join(sorted(missing))})"
            )
        return cls(
            taken_at=data["takenAt"],
            server_version_num=data["serverVersionNum"],
            statements=data.get("statements") or {},
            database=data.get("database") or {},
            checkpointer=data.get("checkpointer") or {},
            settings=data.get("settings") or {},
        )

    def pending_restart(self) -> list[str]:
        """Settings changed but not yet in effect. Non-empty means the run measured the old configuration."""
        return sorted(name for name, row in self.settings.items() if row.get("pending_restart"))

    def notable_settings(self) -> dict[str, str]:
        return {name: self.settings[name]["value"] for name in NOTABLE_SETTINGS if name in self.settings}

    def has_statements(self) -> bool:
        return bool(self.statements)


def _delta(after: dict[str, Any], before: dict[str, Any]) -> dict[str, float]:
    """Difference every numeric field the two rows share. Field names differ across major versions, so they are
    discovered rather than listed."""
    out: dict[str, float] = {}
    for key, a in after.items():
        if key in _NON_COUNTER_KEYS or not isinstance(a, (int, float)) or isinstance(a, bool):
            continue
        b = before.get(key)
        # A counter that went backwards means statistics were reset mid-run; keep the raw value over a negative
        out[key] = a - b if isinstance(b, (int, float)) and a >= b else a
    return out


def _first_present(row: dict[str, Any], *names: str) -> float | None:
    for name in names:
        if isinstance(row.get(name), (int, float)):
            return row[name]
    return None


def _cache_hit_ratio(row: dict[str, Any], hit_key: str, read_key: str) -> float | None:
    hit, read = row.get(hit_key), row.get(read_key)
    if not isinstance(hit, (int, float)) or not isinstance(read, (int, float)) or (hit + read) == 0:
        return None
    return hit / (hit + read)


def diff(before: PgSnapshot, after: PgSnapshot) -> dict[str, Any]:
    """Work performed between two snapshots, per statement and per database."""
    if before.server_version_num != after.server_version_num:
        raise ValueError(
            f"Snapshots are from different server versions ({before.server_version_num} vs "
            f"{after.server_version_num}); they cannot be differenced"
        )

    statements = []
    for queryid, after_row in after.statements.items():
        delta = _delta(after_row, before.statements.get(queryid, {}))
        if not delta.get("calls"):
            continue
        delta["queryid"] = queryid
        delta["query"] = after_row.get("query", "")
        total = _first_present(delta, "total_exec_time", "total_time")
        if total is not None:
            delta["mean_exec_time"] = total / delta["calls"]
            delta["total_exec_time"] = total
        delta["cache_hit_ratio"] = _cache_hit_ratio(delta, "shared_blks_hit", "shared_blks_read")
        statements.append(delta)

    statements.sort(key=lambda s: s.get("total_exec_time", 0), reverse=True)

    database = _delta(after.database, before.database)
    database["cache_hit_ratio"] = _cache_hit_ratio(database, "blks_hit", "blks_read")

    changed = sorted(
        name
        for name, row in after.settings.items()
        if before.settings.get(name, {}).get("value") != row["value"]
    )

    return {
        "window": {"from": before.taken_at, "to": after.taken_at},
        "serverVersionNum": after.server_version_num,
        "statements": statements,
        "database": database,
        "checkpointer": _delta(after.checkpointer, before.checkpointer),
        "settingsChangedDuringRun": changed,
        "pendingRestart": after.pending_restart(),
        "notableSettings": after.notable_settings(),
    }
