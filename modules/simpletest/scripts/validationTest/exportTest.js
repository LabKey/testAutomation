/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");

var exported = require("simpletest/scriptvalidationexports");

testFunc = exported.sampleFunc;
testVar = exported.sampleVar;

function testHiddenFunc()
{
    if( !(exported.hiddenFunc === undefined ))
        throw new Error("A function was unexpectedly exported.")
}

function testHiddenVar()
{
    if( !(exported.hiddenVar === undefined ))
        throw new Error("A var was unexpectedly exported.")
}

function doTest()
{
    testHiddenFunc();
    testHiddenVar();
    exported.sampleFunc(exported.sampleVar);
}