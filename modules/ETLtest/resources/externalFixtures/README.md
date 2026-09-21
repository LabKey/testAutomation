# External SQL Server fixtures

SQL Server is no longer supported as LabKey's primary database, but it **is** still supported as an external data source, including running stored procedures through the DataIntegration `StoredProcedureStep`. These scripts are the SQL Server dialect fixtures for that feature, preserved when the primary-DB SQL Server dbscripts were removed.

They are deliberately **not** under `schemas/dbscripts/`. The module upgrade scanner only reads `schemas/dbscripts/<dialect>/`, so nothing here can ever execute against the primary database. Apply them by hand to an external SQL Server database.

| Script | Contents |
|---|---|
| `sqlserver/etltest-procs.sql` | `etltest` schema, the `source` table, and the `etlTest` / `etlTestResultSet` procedures covering the nine test modes (return codes, raised errors, in/out parameter persistence, run and modified-since filter strategies, result sets) |
| `sqlserver/etltest-specialchars.sql` | `"etl test!schema"."etl""test proc!"` — identifier quoting/escaping for schema and procedure names containing spaces, `!`, and an embedded double-quote. Driven by `../ETLs/SProcSpecialCharacters.xml` |

## Porting notes

These were lifted from the deleted `schemas/dbscripts/sqlserver/` scripts and adjusted for a database that is not a LabKey primary DB:

- `entityid` is a LabKey alias type defined by `core`; replaced with `UNIQUEIDENTIFIER`.
- The `container` foreign key to `core.containers` was dropped — that table does not exist in an external database.
- `EXEC core.fn_dropifexists` calls were dropped for the same reason. Scripts assume a clean schema.
- The procedures are the final state, not the `CREATE` plus `ALTER` chain the versioned dbscripts carried.

The trap worth knowing before editing `etltest-specialchars.sql`: LabKey's `SqlScanner` does not understand `[bracket]` quoting and will misread a double-quote inside brackets as the start of a string literal. Use `"..."` with the interior quote doubled as `""`, and keep `SET QUOTED_IDENTIFIER ON`.

## Not wired up in TeamCity

There is no automated coverage for this path yet, and `test.properties.template` has no external-datasource keys at all — that absence, not a missing fixture, is the gap. Wiring it up needs, at minimum:

- external SQL Server datasource properties in `test.properties.template` and the TeamCity build configuration
- a way to apply these scripts to that database during test setup
- a skip gate for "an external SQL Server datasource is configured". Note this is **not** `SqlserverOnlyTest`, which means "the primary database is SQL Server" and is now permanently false.
