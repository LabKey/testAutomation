/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
window.gridView = new LABKEY.ext.EditorGridPanel({
    store: new LABKEY.ext.Store({
        schemaName : 'lists',
        sql: 'SELECT People.FirstName, People.LastName, People.Age FROM People',
        maxRows: 2}),
    renderTo : 'testDiv',
    editable : true,
    enableFilters : true,
    title :'maxRows Test',
    autoHeight : true,
    width: 800
});
