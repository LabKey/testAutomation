/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// create new grid over a list named 'People'
window.gridView = new LABKEY.ext.EditorGridPanel({
    store: new LABKEY.ext.Store({
       schemaName : 'lists',
       queryName : 'People',
       sort: 'LastName'}),
    renderTo : 'testDiv',
    editable : true,
    enableFilters : true,
    title :'ClientAPITest Grid Title',
    autoHeight : true,
    width: 800,
    pageSize: 4
});
