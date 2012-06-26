function (data, columnMap, measureInfo, clickEvent) {
   var ptidHref = LABKEY.ActionURL.buildURL('study', 'participant', LABKEY.container.path,
                      {participantId: data[columnMap["participant"]].value});
   window.location = ptidHref;
}