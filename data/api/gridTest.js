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
