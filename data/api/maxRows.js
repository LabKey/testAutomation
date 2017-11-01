/*
 * Copyright (c) 2015-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
window.gridView = new LABKEY.ext.EditorGridPanel({
    renderTo: 'testDiv',
    store: new LABKEY.ext.Store({
        schemaName: 'lists',
        sql: 'PARAMETERS(MinAge INTEGER) SELECT People.FirstName, People.LastName, People.Age FROM People WHERE People.Age > MinAge',
        // 31656: Ensure query parameters are passed along
        parameters: {
            MinAge: 20
        },
        maxRows: 2
    }),
    editable : true,
    enableFilters : true,
    title :'maxRows Test',
    autoHeight : true,
    width: 800
});
