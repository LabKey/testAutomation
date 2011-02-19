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