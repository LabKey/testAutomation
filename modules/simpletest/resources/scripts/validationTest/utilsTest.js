/*
 * Copyright (c) 2011-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");
var Ext = require("Ext").Ext;

function doTest()
{
    var errors = [];
    var result;

    // uses document.createTextNode which is inaccessible server-side
    // LABKEY.Utils.encodeHtml()
//    var result = LABKEY.Utils.encodeHtml("<html>test</html>");
//    if( result.contains('<') )
//        errors[errors.length] = new Error("Utils.encodeHtml() = "+Ext.util.JSON.encode(result));

    // LABKEY.Utils.endsWith()
    result = LABKEY.Utils.endsWith("<html>test</html>", "html>");
    if( !result )
        errors[errors.length] = new Error("Utils.endsWith() = "+Ext.util.JSON.encode(result));

    // LABKEY.Utils.generateUUID()
    result = LABKEY.Utils.generateUUID();
    if(  !/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/.test(result) )
        errors[errors.length] = new Error("Utils.generateUUID() = "+Ext.util.JSON.encode(result));

    // LABKEY.Utils.merge()
    var A = {y: 0, z: 0};
    var B = {y: 2};
    var C = {y: 4, z: 5};
    result = LABKEY.Utils.merge(A, B, C);
    if( !(A.y == 4 && A.z == 5 && B.y == 2) )
        errors[errors.length] = new Error("Utils.merge(): A = " + Ext.util.JSON.encode(A) + ", B = " + Ext.util.JSON.encode(B) + ", C = " + Ext.util.JSON.encode(C));

    // LABKEY.Utils.mergeIf()
    A = {y: 0, z: 0};
    B = {y: 2};
    C = {y: 4, z: 5};
    result = LABKEY.Utils.mergeIf(A, B, C);
    if( !(A.y == 0 && A.z == 0 && B.y == 2 && result.y == 4 && result.z == 5 ) )
        errors[errors.length] = new Error("Utils.mergeIf(): A = " + Ext.util.JSON.encode(A) + ", B = " + Ext.util.JSON.encode(B) + ", C = " + Ext.util.JSON.encode(C) + ", result = " + Ext.util.JSON.encode(result));

    // LABKEY.Utils.textLink()
    result = LABKEY.Utils.textLink({
        href:"www.labkey.com",
        style:"awesome",
        text:"LabKey"
    });
    if( result != "<a href='www.labkey.com' style='awesome'>LabKey</a>" )
        errors[errors.length] = new Error("Utils.testLink() = "+Ext.util.JSON.encode(result));

    doAsyncTests();
}

var i = 0;
var results = [];
function tester()
{ return (++i > 10); }
function badTester()
{ return (++i > 101); }
function doneChecking(msg, callback)
{
    if( msg != "Expected!" )
        errors[errors.length] = new Error("Utils.onTrue(): "+callback+" expected.");
    doAsyncTests();
}

var aTests = [
        // LABKEY.Utils.onTrue() success
        function()
        {
            i = 0;
            results[results.length] = LABKEY.Utils.onTrue({
                testCallback: tester,
                success: doneChecking,
                successArguments: ['Expected!', "success"],
                error: doneChecking,
                maxTests: 100
            })
        },
        // LABKEY.Utils.onTrue() success
        function()
        {
            i = 0;
            results[results.length] = LABKEY.Utils.onTrue({
                testCallback: badTester,
                success: doneChecking,
                error: doneChecking,
                errorArguments: ['Expected!', "error"],
                maxTests: 100
            })
        },

        function()
        {
            if( errors.length > 0 )
                throw errors;
        }
];

function doAsyncTests()
{
    var currentTest = aTests[results.length];
    currentTest();
}

