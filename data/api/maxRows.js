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
