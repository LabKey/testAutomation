### Dumping trial db
Pre-dump preparation
- Create site admin user admin@local.host
  - Set password and record it for use by others

### Restoring trial db
After restoring the database dump, before starting LabKey, run the included script ([cleanPgDump.sh](./cleanPgDump.sh)).
You can also run the individual queries listed below.
- Update baseServerURL (usually http://localhost:8080) so redirects for changing passwords don't go to the trial instance
  - `UPDATE prop.properties SET value = 'http://localhost:8080' WHERE name = 'baseServerURL';`
- Disable 'autoredirect' for SSO providers
  - `UPDATE core.authenticationconfigurations SET autoredirect = 'FALSE' WHERE TRUE;`
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

Things that you might want to update:
- search index file path
- systemShortName
- systemDescription
- serverGUID
- DefaultDomain
- remove email template overrides
