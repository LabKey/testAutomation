/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
LABKEY.requiresVisualization();
//Since we are in a single script block (within a nested function created by test framework, no less)
//we need to make sure that everything is loaded including visualization libraries

var baseConfig =  {
    width:800,
    height:600,
    title:"Scatter Chart",
    series:[{
        xProperty:"x",
        yProperty:"y",
        data:[{x:0,y:1}, {x:500,y:1000}, {x:1234, y:3300}]
    }],
    axes:{bottom:{caption:"Bottom Axis"}}
};

var baseSeries =  {
    xProperty:"x",
    yProperty:"y",
    data:[{x:0,y:1}, {x:500,y:1000}, {x:1234, y:3300}]
};

function applyConfig(cfg)
{
    var base = Ext.apply({}, baseConfig);
    if (cfg.axes)
    {
        for (var x in base.axes)
            if (x in cfg.axes)
                Ext.apply(base.axes[x], cfg.axes[x]);

        for(var y in cfg.axes)
            if(!base.axes[y]){
                base.axes[y] = {};
                Ext.apply(base.axes[y], cfg.axes[y]);
            }
    }

    base.title = cfg.title || baseConfig.title;
    if (cfg.series)
    {
        for (var i = 0; i < cfg.series.length; i++)
        {
            var ser = Ext.apply({}, baseSeries);
            ser = Ext.apply(ser, cfg.series[i]);
            ser.caption = ser.caption || "Series" + (i + 1);
            base.series[i] = ser;
        }
    }

    return base;
}

var configs = [
{
    title:"Test Scatter Chart - multi-series",
    series:[
        {data:[{x:1, y:1},{x:2, y:2},{x:3, y:3},{x:4, y:4},{x:5, y:5}]},
        {data:[{x:1.5, y:6},{x:2, y:8},{x:3, y:2},{x:4.64, y:3.25},{x:5, y:-1}]}
    ],
    axes:{left:{caption:"Left Y Axis"}}
},
{
    title:"Log Scale on both Left and Bottom (and null values)",
    series:[
        {data:[{x:1, y:1},{x:2, y:2},{x:3, y:3},{x:4, y:4},{x:5, y:5}]},
        {data:[{x:1.5, y:6},{x:200, y:800345},{x:3000, y:null}, {x:null, y:1000}, {x:46400, y:3250},{x:50000, y:1}]}
    ],
    axes:{
        bottom:{caption:"Bottom Axis (log)", scale: "log"},
        left:{caption:"Left Y Axis (log)", scale: "log"}
    }
},
{
    title:"Log Scale, small values, and illegal values (pinned to axis)",
    series:[
        {data:[{x:0.00001, y:0.001},{x:0.0023, y:1},{x:-3, y:0.1},{x:0.01, y:0},{x:0.1, y:0.0005}]}
    ],
    axes:{
        bottom:{caption:"Bottom Axis (log)", scale: "log"},
        left:{caption:"Left Y Axis (log)", scale: "log"}
    }
},
{
    title:"No data",
    series:[
        {data:[]}
    ],
    axes:{
        left:{caption:"Left Y Axis"}
    }
},
{
    title:"One data point",
    series:[
        {data:[{x:3, y:3}]}
    ],
    axes:{left:{caption:"Left Y Axis"}}
},
{
    title:"Explicit Min and Max Range",
    series:[
        {data:[{x:1, y:1},{x:2, y:2},{x:3, y:3},{x:4, y:4},{x:5, y:5}]},
        {data:[{x:1.5, y:6},{x:200, y:800345}, {x:46400, y:3250},{x:50000, y:1}]}
    ],
    axes:{
        bottom:{caption:"Bottom Axis", min:0, max:6},
        left:{caption:"Left Y Axis (log)", scale: "log", min:0, max:6}
    }
},
{
    title:"Bad min and max - no data visible",
    series:[
        {data:[{x:1, y:1},{x:2, y:2},{x:3, y:3},{x:4, y:4},{x:5, y:5}]},
        {data:[{x:1.5, y:6},{x:200, y:800345}, {x:46400, y:3250},{x:50000, y:1}]}
    ],
    axes:{
        bottom:{caption:"Bottom Axis", min:-200, max:-10},
        left:{caption:"Left Y Axis (log)", scale: "log"}
    }
},
{
    title:"Left: min range, bottom: max range",
    series:[
        {data:[{x:1, y:1},{x:2, y:2},{x:3, y:3},{x:4, y:4},{x:5, y:5}]},
        {data:[{x:1.5, y:6},{x:9, y:8345}]}
    ],
    axes:{
        bottom:{caption:"Bottom Axis", max:10},
        left:{caption:"Left Y Axis (log)", scale: "log", min:0.001}
    }
},
{
    title:"Multi y-axes",
    series:[
        {data:[{x:1, y:1},{x:2, y:2},{x:3, y:3},{x:4, y:4},{x:5, y:5}], axis: "left"},
        {data:[{x:1.5, y:3}, {x:1.7, y:2.9}, {x:1.6, y:3.1}, {x:1.8, y:3.3}], axis: "left"},
        {data:[{x:1, y:5},{x:2, y:4},{x:3, y:3},{x:4, y:2},{x:5, y:1}], axis: "right"},
        {data:[{x:4.2, y:3.1}, {x:4.4, y:2.9}, {x:4.3, y:3.1}, {x:4.1, y:3.2}], axis: "right"}
    ],
    axes:{
        bottom:{caption:"X Axis"},
        left:{caption:"Left Y Axis"},
        right:{caption:"Right Y Axis"}
    }
},
{
    title:"Multi y-axes with log scale on right and bottom",
    series:[
        {data:[{x:1, y:1},{x:2, y:2},{x:3, y:3},{x:4, y:4},{x:5, y:5}], axis: "left"},
        {data:[{x:0.001, y:6},{x:200, y:800345}, {x:46400, y:3250},{x:50000, y:0.001}], axis: "right"}
    ],
    axes:{
        bottom:{caption:"Bottom Axis", scale: "log"},
        left:{caption:"Left Y Axis (log)", min: 0, max: 8},
        right:{caption:"Right Y Axis (log)", scale: "log"}
    }
},
{
    title:"Right Axis Only",
    series:[
        {data:[{x:1, y:1},{x:2, y:2},{x:3, y:3},{x:4, y:4},{x:5, y:5}], axis: "right"},
        {data:[{x:1.5, y:6},{x:2, y:8},{x:3, y:2},{x:4.64, y:3.25},{x:5, y:-1}], axis: "right"}
    ],
    axes:{right:{caption:"Right Y Axis"}}
}];


//Lots of async script loading. Make sure everything is loaded before starting
LABKEY.Utils.onTrue({
    testCallback:function() {return Ext.isReady && LABKEY.vis && window.pv},
    //Calling createGraph directly causes errors to get swallowed by onTrue. We defer one more time
    successCallback:function () {createGraph.defer(100)},
    errorCallback:doError
});

function doError(param) {
    throw param;
}

function createGraph()
{
    var param = LABKEY.ActionURL.getParameter("config");
    var index = 0;
    if (null != param)
    {
        index = (+param); //Unary plus to convert to integer
        if (index < 0 || index >= configs.length)
            throw "Bad configuration index: " + index + " only " + configs.length + " configs available to test."
    }
    
    var config = applyConfig(configs[index]);
    var chart = new LABKEY.vis.ScatterChart(config);
    new Ext.Panel({
        tbar: [{text:"Get SVG",
                handler:function() {Ext.getCmp("svgtext").setValue(chart.getSerializedXML())}}],
        items:[{html:"<form>Config Count:<input id='configCount' value='" + configs.length + "'><br>Show Config<input type='text' name='config' value='" + index + "'><input type='submit' name='submit' value='Submit'><\\/form>"},
            chart, {xtype:"textarea", height:300, width:400, id:"svgtext"}],
        renderTo:"testDiv"
    });
}
