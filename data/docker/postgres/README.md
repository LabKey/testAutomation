Disposable PostgreSQL database for LabKey test runs, primarily on TeamCity. For tests that need the assistant module's
vector store, use the [pgvector](https://github.com/pgvector/pgvector) app in [`../pgvector`](../pgvector) instead.

## TeamCity

Point `PG_DATA_DIR` at a directory on the agent's EC2 instance store (ephemeral) drive; it's much faster than EBS, and
nothing in it needs to survive.

1. Add a **Command Line** step that clears the data directory:

   ```sh
   rm -rf "$PG_DATA_DIR" && mkdir -p "$PG_DATA_DIR"
   ```

2. Add a **Docker Compose** step that uses this directory's `docker-compose.yml`. It waits for the database's health
   check before the build continues.

3. Set `env.PG_DATA_DIR` as a build parameter, plus `env.PG_VERSION` or `env.PG_PORT` if needed. The agents already
   provide `env.UID`.

At the end of the build, TeamCity runs `docker compose down -v`, which stops the database but leaves the files in
`PG_DATA_DIR`. The cleanup belongs at the start of the build, not the end: the database is still running until every
step finishes, and starting with an empty directory also covers builds that died before cleaning up.

The container runs as the agent's uid, so the agent can delete the files itself. Without that, the files would belong
to the image's `postgres` user (uid 999) and only root could remove them.

The server runs with `synchronous_commit=off`, so commits don't wait for the disk. A crash can lose the last few
commits but can't corrupt the database, and query results and transaction visibility are unchanged.

## Running locally

```
mkdir -p /tmp/pgdata
export UID
PG_DATA_DIR=/tmp/pgdata docker compose up -d
```

Shut down with `docker compose down`. The container only creates a new database when `PG_DATA_DIR` is empty, so delete
the directory to start fresh.

## Environment variables

| Variable      | Default    | Purpose                                               |
|---------------|------------|-------------------------------------------------------|
| `PG_DATA_DIR` | (required) | Host directory mounted as the database data directory |
| `UID`         | (required) | User ID the database runs as; must own `PG_DATA_DIR`  |
| `PG_VERSION`  | `18`       | PostgreSQL major version (`postgres` image tag)       |
| `PG_PORT`     | `5432`     | Host port the database listens on                     |
| `PG_USER`     | `postgres` | Database superuser name                               |
| `PG_PASSWORD` | `sasa`     | Database superuser password                           |
