    LABKEY.requiresVisualization();
//Since we are in a single script block (within a nested function created by test framework, no less)
//we need to make sure that everything is loaded including visualization libraries
LABKEY.Utils.onTrue({
    testCallback:function() {return Ext.isReady && LABKEY.vis;},
    successCallback:createGraph
});

function createGraph()
{
    var chartConfig = {
        width:800,
        height:600,
        title:"Test Chart",
        series:[{
            caption:"Series1",
            xProperty:"x",
            yProperty:"y",
            data:[{x:1, y:1},{x:2, y:1},{x:3, y:2},{x:4, y:3},{x:5, y:4} ]
        },
                {
            caption:"Series2",
            xProperty:"x",
            yProperty:"y",
            data:[{x:1, y:6},{x:2, y:8},{x:3, y:2},{x:4, y:3},{x:5, y:1} ]
        }
        ],
        axes:{x:{caption:"X Axis"}, y:{caption:"Y Axis"}}
    };

    var chart = new LABKEY.vis.LineChart(chartConfig);
    var panel = new Ext.Panel({
        tbar: [{text:"Get SVG",
                handler:function(btn) {Ext.getCmp("svgtext").setValue(chart.getSerializedXML())}}],
        items:[chart, {xtype:"textarea", height:300, width:400, id:"svgtext"}],
        renderTo:"testDiv"
    });
}