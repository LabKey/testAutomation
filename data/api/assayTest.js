/*
 * Copyright (c) 2011-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function renderer(assayArray)
{
    var html = '';
    for (var defIndex = 0; defIndex  < assayArray.length; defIndex ++)
    {
	var definition = assayArray[defIndex ];
	html += '<b>' + definition.type + '</b>: ' + definition.name + '<br>';
        for (var domain in definition.domains)
        {
            html += '&nbsp;&nbsp;&nbsp;' + domain + '<br>';
            var properties = definition.domains[domain];
            for (var propertyIndex = 0; propertyIndex < properties.length; propertyIndex++)
            {
                var property = properties[propertyIndex];
                html += '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                         property.name + ' - ' + property.typeName + '<br>';
            }
        }
    }
    document.getElementById('testDiv').innerHTML = html;
}

function errorHandler(error)
{
    alert('An error occurred retrieving data.');
}

LABKEY.Assay.getAll(renderer, errorHandler);