var renderer = new LABKEY.WebPart({partName: 'query',
renderTo: 'testDiv',
partConfig: {
        title: 'Webpart Title',
        schemaName: 'lists',
        queryName: 'People'
    }});
renderer.render();