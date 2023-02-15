#!/usr/bin/env bash

set -eu

export PGHOST=${PGHOST:-localhost}
export PGPORT=${PGPORT:-5432}
export PGUSER=${PGUSER:-postgres}
export PGDATABASE=${PGDATABASE:-labkey_restored}

BASE_SERVER_URL=${BASE_SERVER_URL:-http://localhost:8080}
LABKEY_FILES=${LABKEY_FILES:-$LABKEY_HOME/build/deploy/files}
LABKEY_PIPELINE_TOOLS=${LABKEY_PIPELINE_TOOLS:-$LABKEY_HOME/build/deploy/bin}

## Check that we can connect to DB
psql -ew -c "SELECT 1" &> /dev/null || (
 echo "Unable to connect to database." &&
 echo "How to define connection variables: https://www.postgresql.org/docs/current/libpq-envars.html" &&
 echo "How to authenticate: https://www.postgresql.org/docs/current/libpq-pgpass.html" &&
 exit 1)

## Update site config settings
# Update base server URL so that various redirects and notifications are correct
psql -ew -c "UPDATE prop.properties p1 SET value = '${BASE_SERVER_URL}'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'baseServerURL' AND p2.category = 'SiteConfig';"
# Update site file root
psql -ew -c "UPDATE prop.properties p1 SET value = '${LABKEY_FILES}'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'siteFileRoot' AND p2.category = 'SiteConfig';"
# Update pipeline tools directory
psql -ew -c "UPDATE prop.properties p1 SET value = '${LABKEY_PIPELINE_TOOLS}'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'pipelineToolsDirectory' AND p2.category = 'SiteConfig';"
# Disable SSL requirement
psql -ew -c "UPDATE prop.properties p1 SET value = 'FALSE'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'sslRequired' AND p2.category = 'SiteConfig';"
# Disable reporting exceptions to mothership
psql -ew -c "UPDATE prop.properties p1 SET value = 'NONE'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'exceptionReportingLevel' AND p2.category = 'SiteConfig';"
# Disable reporting usage to mothership
psql -ew -c "UPDATE prop.properties p1 SET value = 'NONE'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'usageReportingLevel' AND p2.category = 'SiteConfig';"

## Update authentication settings
# Disable reporting usage to mothership
psql -ew -c "UPDATE prop.properties p1 SET value = 'localhost'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'DefaultDomain' AND p2.category = 'Authentication';"
# Disable autoredirect for all authentication configs (usually just CAS)
psql -ew -c "UPDATE core.authenticationconfigurations SET autoredirect=FALSE;"

## Update look and feel settings
psql -ew -c "UPDATE prop.properties p1 SET value = 'LabKey Server'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'systemShortName' AND p2.category = 'LookAndFeel';"
psql -ew -c "UPDATE prop.properties p1 SET value = 'LabKey Server'
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'systemDescription' AND p2.category = 'LookAndFeel';"

## Remove analytics tracking script
psql -ew -c "UPDATE prop.properties p1 SET value = ''
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'accountId' AND p2.category = 'analytics';"
psql -ew -c "UPDATE prop.properties p1 SET value = ''
    FROM prop.propertysets p2 WHERE p1.set = p2.set
    AND p1.name = 'trackingScript' AND p2.category = 'analytics';"

## Clear trial server properties
psql -ew -c "UPDATE prop.properties p1 SET value = ''
    WHERE p1.name = 'EvaluationContent Settings/manageHostedSiteUrl';"
psql -ew -c "UPDATE prop.properties p1 SET value = ''
    WHERE p1.name = 'EvaluationContent Settings/trialEndDate';"
