/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var chartConfig = {
    queryName: 'People',
    schemaName: 'lists',
    chartType: LABKEY.Chart.XY,
    columnXName: 'Key',
    columnYName: ['Age'],
    renderTo: 'testDiv'
};
var chart = new LABKEY.Chart(chartConfig);
chart.render();