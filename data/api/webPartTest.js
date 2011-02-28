/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var renderer = new LABKEY.WebPart({partName: 'query',
renderTo: 'testDiv',
partConfig: {
        title: 'Webpart Title',
        schemaName: 'lists',
        queryName: 'People'
    }});
renderer.render();