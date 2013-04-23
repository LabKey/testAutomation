/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");

function doTest()
{
    // LABKEY.Report.execute()
    var result = LABKEY.Report.execute({
        reportId: "module:simpletest/reports/schemas/schemaFolderReport.r",
        inputParams: {
            greeter: "Bob"
        }
    });
    console.log("LABKEY.Report.execute response: ", result);

    if (result.errors.length != 0)
        throw new Error("Expected empty errors array, but got: " + result.errors.join());

    if (!result.console[0] || result.console[0].indexOf("Hello, Bob!") == -1)
        throw new Error("Expected console to contain 'Hello, Bob!', got: " + result.console[0]);

    var expectedOutputParam = {
        name: "hello.json",
        value: {a: [1,2,3], Bob: "Hello"},
        type: "json"
    };
    var jsonOutputParam = result.outputParams[0];
    if (jsonOutputParam.name != expectedOutputParam.name ||
        jsonOutputParam.type != expectedOutputParam.type ||
        jsonOutputParam.value.a[0] != expectedOutputParam.value.a[0] ||
        jsonOutputParam.value.Bob != expectedOutputParam.value.Bob)
    {
        throw new Error("OutputParam mismatch.  Expected '" + JSON.stringify(expectedOutputParam) + "', got '" + JSON.stringify(jsonOutputParam) + "'");
    }
}
