/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);

var sampleVar = "value";
function sampleFunc(arg)
{
    return arg;
}

var hiddenVar = "hidden";
function hiddenFunc(arg)
{
    throw new Error("Function shouldn't be exposed");
}

exports.sampleFunc = sampleFunc;
exports.sampleVar = sampleVar;