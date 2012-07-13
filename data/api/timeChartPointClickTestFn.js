/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function (data, columnMap, measureInfo, clickEvent) {
   var ptidHref = LABKEY.ActionURL.buildURL('study', 'participant', LABKEY.container.path,
                      {participantId: data[columnMap["participant"]].value});
   window.location = ptidHref;
}