/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

doError = function(param) {
    throw param;
};

var beginTest = function(){
    var coffeeData = [
        {"person":"Alan","time":"9:30","consumedCoffee":"No Coffee","efficiency":65},{"person":"Alan","time":"10:00","consumedCoffee":"Coffee","efficiency":85},
        {"person":"Alan","time":"10:30","consumedCoffee":"No Coffee","efficiency":82},{"person":"Alan","time":"11:00","consumedCoffee":"No Coffee","efficiency":83},
        {"person":"Alan","time":"11:30","consumedCoffee":"No Coffee","efficiency":78},{"person":"Alan","time":"12:00","consumedCoffee":"No Coffee","efficiency":72},
        {"person":"Alan","time":"12:30","consumedCoffee":"No Coffee","efficiency":69},{"person":"Alan","time":"1:00","consumedCoffee":"No Coffee","efficiency":62},
        {"person":"Alan","time":"1:30","consumedCoffee":"Coffee","efficiency":88},{"person":"Alan","time":"2:00","consumedCoffee":"No Coffee","efficiency":85},
        {"person":"Alan","time":"2:30","consumedCoffee":"No Coffee","efficiency":82},{"person":"Alan","time":"3:00","consumedCoffee":"No Coffee","efficiency":84},
        {"person":"Alan","time":"3:30","consumedCoffee":"No Coffee","efficiency":79},{"person":"Alan","time":"4:00","consumedCoffee":"No Coffee","efficiency":78},
        {"person":"Alan","time":"4:30","consumedCoffee":"No Coffee","efficiency":null},{"person":"Alan","time":"5:00","consumedCoffee":"No Coffee","efficiency":76},
        {"person":"Josh","time":"9:30","consumedCoffee":"No Coffee","efficiency":300},{"person":"Josh","time":"10:00","consumedCoffee":"No Coffee","efficiency":300},
        {"person":"Josh","time":"10:30","consumedCoffee":"No Coffee","efficiency":300},{"person":"Josh","time":"11:00","consumedCoffee":"No Coffee","efficiency":299},
        {"person":"Josh","time":"11:30","consumedCoffee":"No Coffee","efficiency":297},{"person":"Josh","time":"12:00","consumedCoffee":"No Coffee","efficiency":300},
        {"person":"Josh","time":"12:30","consumedCoffee":"No Coffee","efficiency":300},{"person":"Josh","time":"1:00","consumedCoffee":"No Coffee","efficiency":296},
        {"person":"Josh","time":"1:30","consumedCoffee":"No Coffee","efficiency":300},{"person":"Josh","time":"2:00","consumedCoffee":"No Coffee","efficiency":300},
        {"person":"Josh","time":"2:30","consumedCoffee":"No Coffee","efficiency":298},{"person":"Josh","time":"3:00","consumedCoffee":"No Coffee","efficiency":295},
        {"person":"Josh","time":"3:30","consumedCoffee":"No Coffee","efficiency":294},{"person":"Josh","time":"4:00","consumedCoffee":"No Coffee","efficiency":295},
        {"person":"Josh","time":"4:30","consumedCoffee":"No Coffee","efficiency":297},{"person":"Josh","time":"5:00","consumedCoffee":"No Coffee","efficiency":296}
    ];

    var boxPlotData = [];

    for(var i = 0; i < 6; i++){
        var group = "Really Long Group Name "+(i+1);
        for(var j = 0; j < 25; j++){
            boxPlotData.push({
                group: group,
                age: parseInt(25+(Math.random()*(55-25))) //Compute a random age between 18 and 65
            });
        }
        for(j = 0; j < 3; j++){
            boxPlotData.push({
                group: group,
                age: parseInt(75+(Math.random()*(95-75))) //Compute a random age between 18 and 65
            });
        }
        for(j = 0; j < 3; j++){
            boxPlotData.push({
                group: group,
                age: parseInt(1+(Math.random()*(16-1))) //Compute a random age between 18 and 65
            });
        }
    }

    var scatterData01 = [];
    for(var i = 0; i < 1000; i++){
        var point = {
            x: i % 9 == 0 ? null : parseInt((Math.random()*(150))),
            y: parseInt((Math.random()*(150)))
        };
        scatterData01.push(point);
    }

    var scatterData02 = [];
    for(var i = 0; i < 1000; i++){
        var point = {
            x: parseInt((Math.random()*(75))),
            y: parseInt((Math.random()*(210)))
        };
        scatterData02.push(point);
    }


    //Div for charts to render to.
    Ext4.DomHelper.insertAfter('testDiv', {tag: 'div', id:'chartArea'});
    var plot = null;

    var loadConfig = function(config){
        if(plot != null){
            plot.clearGrid();
            Ext4.get('chartArea').update('');
            this.plot = null;
        }
        plot = new LABKEY.vis.Plot(config);
        plot.render();

    };

    var configs = [
        {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            labels: {
                main: {value: "Line Plot - no y-scale defined"}
            },
            data: coffeeData,
            layers: [
                new LABKEY.vis.Layer({
                    name: "Efficiency",
                    geom: new LABKEY.vis.Geom.Point(),
                    aes: {
                        color: 'person',
                        shape: 'consumedCoffee',
                        hoverText: function(row){return 'Person: ' + row.person + "\n" + row.consumedCoffee + " Consumed \nEfficiency: " + row.efficiency},
                        pointClickFn: function(mouseEvent, data){Ext4.Msg.alert('Data Point Details', Ext4.JSON.encode(data));}
                    }
                }),
                new LABKEY.vis.Layer({
                    name: "Efficiency",
                    geom: new LABKEY.vis.Geom.Path({color: '#66c2a5'}),
                    aes: {
                        color: 'person',
                        group: 'person'
                    }
                })
            ],
            aes: {
                x: 'time',
                yLeft: 'efficiency'
            },
            scales: {
                x: {
                    scaleType:'discrete'
                }
            }
        }, {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            legendPos: 'none',
            labels: {
                main: {value: "Line Plot - y-scale defined, no legend, no shape aes"},
                y: {value: "Y Axis"},
                x: {value: "X Axis"}
            },
            margins: {
                bottom: 75,
                left: 80
            },
            layers: [
                new LABKEY.vis.Layer({
                    name: "Efficiency",
                    geom: new LABKEY.vis.Geom.Point(),
                    aes: {
                        color: 'person',
                        hoverText: function(row){return 'Person: ' + row.person + "\n" + row.consumedCoffee + " Consumed \nEfficiency: " + row.efficiency}
                    }
                }),
                new LABKEY.vis.Layer({
                    name: "Efficiency",
                    geom: new LABKEY.vis.Geom.Path({color: '#66c2a5'}),
                    aes: {
                        color: 'person',
                        group: 'person'
                    }
                })
            ],
            data: coffeeData,
            aes: {
                x: 'time',
                yLeft: 'efficiency'
            },
            scales: {
                x: {
                    scaleType:'discrete'
                },
                yLeft: {
                    scaleType: 'continuous',
                    trans: 'linear',
                    min: 0
                }
            }
        }, {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            legendPos: 'none',
            labels: {
                main: {value: "Line Plot - No Layer AES, Changed Opacity"},
                y: {value: "Y Axis"},
                x: {
                    value: "There is a Click Handler on this Label",
                    lookClickable: true,
                    listeners: {
                        click: function(){
                            console.log('click!');
                        }
                    }
                }
            },
            layers: [
                new LABKEY.vis.Layer({
                    name: "Efficiency",
                    geom: new LABKEY.vis.Geom.Point({
                        color: '#66c2a5',
                        opacity: .5
                    })
                }),
                new LABKEY.vis.Layer({
                    name: "Efficiency",
                    geom: new LABKEY.vis.Geom.Path({
                        color: '#66c2a5',
                        opacity: .5
                    })
                })
            ],
            data: coffeeData,
            aes: {
                x: 'time',
                yLeft: 'efficiency'
            },
            scales: {
                x: {
                    scaleType:'discrete'
                },
                yLeft: {
                    scaleType: 'continuous',
                    trans: 'linear',
                    min: 0
                }
            }
        }, {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            legendPos: 'none',
            labels: {
                main: {value: "Two Axis Scatter, plot null points"}
            },
            layers: [
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Point({
                        plotNullPoints: true,
                        color: 'teal',
                        size: 3
                    }),
                    data: scatterData01,
                    aes: {
                        x: 'x',
                        y: 'y',
                        pointClickFn: function(mouseEvent, data){Ext4.Msg.alert('Teal Point Details', Ext4.JSON.encode(data));}
                    }
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Point({
                        color: 'maroon',
                        size: 3
                    }),
                    data: scatterData02,
                    aes: {
                        x: 'x',
                        y: 'y',
                        pointClickFn: function(mouseEvent, data){Ext4.Msg.alert('Maroon Point Details', Ext4.JSON.encode(data));}
                    }
                })
            ]
        }, {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            legendPos: 'none',
            labels: {
                main: {value: "Discrete X Scale Scatter No Geom Config"}
            },
            layers: [new LABKEY.vis.Layer({
                geom: new LABKEY.vis.Geom.Point()
            })],
            data: boxPlotData,
            aes: {
                yLeft: 'age',
                x: 'group'
            },
            scales: {
                x: {
                    scaleType: 'discrete'
                },
                yLeft: {
                    scaleType: 'continuous',
                    trans: 'linear'
                }
            }
        }, {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            legendPos: 'none',
            labels: {
                main: {value: "Discrete X Scale Scatter, Log Y"}
            },
            layers: [new LABKEY.vis.Layer({
                geom: new LABKEY.vis.Geom.Point({
                    size: 2,
                    color: 'teal',
                    opacity: .6,
                    position: 'jitter'
                })
            })],
            data: boxPlotData,
            aes: {
                yRight: 'age',
                x: 'group'
            },
            scales: {
                x: {
                    scaleType: 'discrete'
                },
                yLeft: {
                    scaleType: 'continuous',
                    trans: 'log'
                }
            }
        }, {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            legendPos: 'none',
            labels: {
                main: {value: "Boxplot no Geom Config"}
            },
            layers: [new LABKEY.vis.Layer({
                geom: new LABKEY.vis.Geom.Boxplot()
            })],
            data: boxPlotData,
            aes: {
                yLeft: 'age',
                x: 'group',
                pointClickFn: function(mouseEvent, data){Ext4.Msg.alert('Outlier Point Details', Ext4.JSON.encode(data));}
            },
            scales: {
                x: {
                    scaleType: 'discrete'
                },
                yLeft: {
                    scaleType: 'continuous',
                    trans: 'linear'
                }
            }
        }, {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            legendPos: 'none',
            labels: {
                main: {value: "Boxplot No Outliers"}
            },
            layers: [
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Boxplot({
                        showOutliers: false,
                        lineWidth: 3,
                        color: 'red',
                        fill: 'blue'
                    })
                })],
            data: boxPlotData,
            aes: {
                yLeft: 'age',
                x: 'group'
            },
            scales: {
                x: {
                    scaleType: 'discrete'
                },
                yLeft: {
                    scaleType: 'continuous',
                    trans: 'linear'
                }
            }
        }, {
            renderTo: 'chartArea',
            width: 600,
            height: 300,
            legendPos: 'none',
            labels: {
                main: {value: "Boxplot No Outliers, All Points"}
            },
            layers: [
                new LABKEY.vis.Layer({
                            geom: new LABKEY.vis.Geom.Boxplot({
                                showOutliers: false
                            })
                        }
                ),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Point({
                        color: 'teal',
                        size: 3,
                        position: 'jitter'
                    })
                })],
            data: boxPlotData,
            aes: {
                yRight: 'age',
                x: 'group'
            },
            scales: {
                x: {
                    scaleType: 'discrete'
                },
                yRight: {
                    scaleType: 'continuous',
                    trans: 'log'
                }
            }
        }
    ];


    var nextButton = Ext4.create('Ext.button.Button', {
        text: 'Next',
        width: 75,
        margin: '0 0 0 10',
        handler: function(){
            if(testNumberField.getValue() == configs.length){
                // No more tests to load.
            } else {
                testNumberField.setValue(testNumberField.getValue() + 1);
                loadConfig(configs[testNumberField.getValue()-1]);
            }
        },
        scope: this
    });

    var testNumberField = Ext4.create('Ext.form.field.Number', {
        fieldLabel: 'Current Config',
        width: 125,
        hideTrigger: true,
        value: 1
    });

    var totalNumberField = Ext4.create('Ext.form.field.Number', {
        fieldLabel: 'of',
        inputId: 'configCount',
        labelWidth: 25,
        margin: '0 0 0 10',
        width: 50,
        hideTrigger: true,
        value: configs.length
    });

    var getSVGBtn = Ext4.create('Ext.button.Button', {
        text: "Get SVG",
        handler: function(){
            var svg = Ext4.query('svg')[0];
            svgArea.setValue(LABKEY.vis.SVGConverter.svgToStr(svg));
        },
        scope: this
    });

    var svgArea = Ext4.create('Ext.form.field.TextArea', {
        title: 'SVG Text',
        inputId: 'svgtext',
        width: 300,
        height: 100
    });

    Ext4.create('Ext.panel.Panel', {
        renderTo: 'testDiv',
        layout: 'hbox',
        border: false,
        frame: false,
        width: '400',
        height: '100',
        items: [
            testNumberField,
            totalNumberField,
            nextButton,
            getSVGBtn
        ],
        listeners: {
            afterrender: function(){
                loadConfig(configs[testNumberField.getValue()-1]);
            },
            scope: this
        }
    });

    Ext4.create('Ext.panel.Panel', {
        renderTo: 'testDiv',
        layout: 'fit',
        border: false,
        frame: false,
        width: '400',
        height: '100',
        items: [
            svgArea
        ]
    });
};

var loadVis = function(){
    LABKEY.requiresVisualization();
    LABKEY.Utils.onTrue({
        testCallback: function(){
            return LABKEY.vis && LABKEY.vis.Geom && LABKEY.vis.Stat && LABKEY.vis.Layer && LABKEY.vis.Plot;
        },
        successCallback: function(){beginTest.defer(100)},
        errorCallback: doError
    });
};

var loadPatches = function(){
    LABKEY.requiresScript(LABKEY.extJsRoot_42 + "/ext-patches.js", true);
    LABKEY.Utils.onTrue({
        testCallback: function(){
            return Ext4.USE_NATIVE_JSON === true;
        },
        successCallback: function(){
            loadVis();
        },
        errorCallback: doError
    });
};

LABKEY.requiresScript(LABKEY.extJsRoot_42 + "/ext-all-sandbox-debug.js", true);

LABKEY.Utils.onTrue({
    testCallback:function() {
        return ('Ext4' in window) &&  Ext4.isReady;
    },
    successCallback:function () {
//        loadPatches.defer(250);
        loadPatches();
    },
    errorCallback:doError
});
