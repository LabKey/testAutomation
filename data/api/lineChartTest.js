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
    title:"Test Chart",
    series:[{
        xProperty:"x",
        yProperty:"y",
        data:[{x:0,y:1}, {x:1,y:1000}, {x:2, y:3300}]
    }
    ],
    axes:{x:{caption:"X Axis"}, y:{caption:"Y Axis"}}
};

var baseSeries =  {
        xProperty:"x",
        yProperty:"y",
        data:[{x:0,y:1}, {x:1,y:1000}, {x:2, y:3300}]
};

function applyConfig(cfg)
{
    var base = Ext.apply({}, baseConfig);
    if (cfg.axes)
    {
        for (var x in base.axes)
            if (x in cfg.axes)
                Ext.apply(base.axes[x], cfg.axes[x]);
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
    title:"Test Chart",
        series:[{
            caption:"Series1",
            data:[{x:1, y:1},{x:2, y:1},{x:3, y:2},{x:4, y:3},{x:5, y:4} ]
        },
                {
            caption:"Series2",
            data:[{x:1, y:6},{x:2, y:8},{x:3, y:2},{x:4, y:3},{x:5, y:1} ]
        }
    ]
    },
    {    //Log scale
        title:"Log Scale",
        series:[{
            data:[{x:0,y:1}, {x:1,y:1000}, {x:2, y:3300}]
        }],
        axes:{y:{scale:"log"}}
    },
    {
        title: "Log scale with small values",
        series:[
            {data:[{x:0,y:.0001}, {x:1,y:.014}, {x:2, y:.4}, {x:3, y:11}]}],
        axes:{y:{scale:"log"}}
    },
    {    title: "Log scale with illegal values (should be pinned to minimal value",
        series:[
            {data:[{x:0,y:.0001}, {x:1,y:.014}, {x:2, y:-3}, {x:3, y:-0}]}],
        axes:{y:{scale:"log"}}
    },
    {
        title:"No data",
        series:[
            {data:[]}]
    },
    {
        title:"One data point",
        series:[
            {data:[{x:3, y:3}]}]
    },
    {
        title:"Null data points (removed)",
        series:[
            {data:[{x:0, y:0}, {x:0, y:null}, {x:null, y:0}, {x:3, y:2}]}]
    },
    {
        title:"Multi-series",
        series:[
        {data:[{x:0, y:.0001}, {x:3, y:.15}]},
        {data:[{x:0, y:0}, {x:1, y:202894}, {x:2, y:3}, {x:1, y:209980}]}],
        axes:{y:{scale:"log"}}
    },
    { //Explicitly specify min & max on scale
        title:"Explicitly specify min & max on scale, but pass illegal values",
        series:[
        {data:[{x:0, y:.0001}, {x:3, y:.15}]},
        {data:[{x:0, y:0}, {x:1, y:202894}, {x:2, y:3}, {x:1, y:209980}]}],
        axes:{y:{scale:"log", min:1, max:300000}}
    },
    {
        title:"Just specify min on scale",
        series:[
        {data:[{x:0, y:.0001}, {x:3, y:.15}]},
        {data:[{x:0, y:0}, {x:1, y:202894}, {x:2, y:3}, {x:1, y:209980}]}],
        axes:{y:{scale:"log", min:1}}
    },
    {
        title:"Just specify max on scale",
        series:[
        {data:[{x:0, y:.0001}, {x:3, y:.15}]},
        {data:[{x:0, y:0}, {x:1, y:202894}, {x:2, y:3}, {x:1, y:209980}]}],
        axes:{y:{scale:"log", max:10000000}}
    },
    {
        title:"Explicitly specify min & max on scale, but bad range",
        series:[
        {data:[{x:0, y:.0001}, {x:3, y:.15}]},
        {data:[{x:0, y:0}, {x:1, y:3700}, {x:2, y:3}, {x:3, y:5000}]}],
        axes:{y:{min:6, max:3000}}
    },
    {
        title:"Really bad range  -- should see nothing",
        series:[
        {data:[{x:0, y:.0001}, {x:3, y:.15}]},
        {data:[{x:0, y:0}, {x:1, y:3700}, {x:2, y:3}, {x:3, y:5000}]}],
        axes:{y:{min:-60, max:-10}}
    }
    ];


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
    var chart = new LABKEY.vis.LineChart(config);
    var panel = new Ext.Panel({
        tbar: [{text:"Get SVG",
                handler:function(btn) {Ext.getCmp("svgtext").setValue(chart.getSerializedXML())}}],
        items:[{html:"<form>Config Count:<input id='configCount' value='" + configs.length + "'><br>Show Config<input type='text' name='config' value='" + index + "'><input type='submit' name='submit' value='Submit'><\\/form>"},
            chart, {xtype:"textarea", height:300, width:400, id:"svgtext"}],
        renderTo:"testDiv"
    });
}
