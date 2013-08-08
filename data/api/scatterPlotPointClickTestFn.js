/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function (data, measureInfo, clickEvent) {
   var queryHref = LABKEY.ActionURL.buildURL("query", "executeQuery", LABKEY.container.path,
                      {schemaName: measureInfo["schemaName"], "query.queryName": measureInfo["queryName"]});
   window.location = queryHref;
}