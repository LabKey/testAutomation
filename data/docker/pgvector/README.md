Disposable PostgreSQL database with the [pgvector](https://github.com/pgvector/pgvector) extension for LabKey test
runs, primarily on TeamCity. Use it for tests that need the assistant module's vector store; otherwise use the standard
PostgreSQL app in [`../postgres`](../postgres).

Setup, TeamCity configuration, and environment variables are the same as the standard app; follow
[`../postgres/README.md`](../postgres/README.md), using this directory's `docker-compose.yml`. The only difference
is the image: `PG_VERSION` selects the `pgvector/pgvector` tag (`pg18` by default) instead of the `postgres` tag.
