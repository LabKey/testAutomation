### Dumping trial db
- Prepare system
  - Enable database authentication: `http://<server_name>/login-configure.view`
  - Create site admin user `admin@local.host`
    - Set password and record it for use by others
  - Shut down LabKey
- Dump database in "plain" format (a giant SQL script)
- Compress dump for sharing (e.g. `.zip`)

### Restore db dump
- _Note for docker_: when using a large dataset, some queries require more shared memory than docker makes available by default. Increase by running the postgres container with `--shm-size 1G`
- Restore database dump (after extracting, if necessary)
  - Using PgAdmin
    - Create empty database to restore into
    - Execute dump script on the new db using the pgAdmin "Query Tool" 
  - Command line:
    ```
    # Drop database
    dropdb --no-password --if-exists $PGDATABASE
    # Create database
    createdb --no-password -T template0 $PGDATABASE
    # Restore database
    psql -q -L /dev/null -f dump_file.sql
    ```
- Clean up database (see below)
- Update `pg.properties` and run `./gradlew pickPg`
- Start LabKey

### Clean up restored db
After restoring the database dump, before starting LabKey, run the included script ([cleanPgDump.sh](./cleanPgDump.sh)).
You can also run the individual queries listed below.
- Update baseServerURL (usually http://localhost:8080) so redirects for changing passwords don't go to the trial instance
  - `UPDATE prop.properties SET value = 'http://localhost:8080' WHERE name = 'baseServerURL';`
- Disable 'autoredirect' for SSO providers
  - `UPDATE core.authenticationconfigurations SET autoredirect = FALSE;`
- Disable SSL
  - `UPDATE prop.properties SET value = 'FALSE' WHERE name = 'sslRequired';`
- Update site file root (Use your actual file root location)
  - `UPDATE prop.properties SET value = '<$LABKEY_HOME/build/deploy/files>' WHERE name = 'siteFileRoot';`
- Disable mothership reporting
  - `UPDATE prop.properties SET value = 'NONE' WHERE name = 'exceptionReportingLevel';`
  - `UPDATE prop.properties SET value = 'NONE' WHERE name = 'usageReportingLevel';`
- Disable tracking and analytics
  - `UPDATE prop.properties SET value = '' WHERE name = 'accountId';`
  - `UPDATE prop.properties SET value = '' WHERE name = 'trackingScript';`
- Clear trial server properties (If dump came from a trial instance)
  - `UPDATE prop.properties p1 SET value = '' WHERE p1.name = 'EvaluationContent Settings/manageHostedSiteUrl';`
  - `UPDATE prop.properties p1 SET value = '' WHERE p1.name = 'EvaluationContent Settings/trialEndDate';`

Things that you might want to update (via SQL script or through LabKey UI after starting the server):
- search index file path
- systemShortName
- systemDescription
- serverGUID
- DefaultDomain
- remove email template overrides
- pipeline tools directory
