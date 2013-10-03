/*
 * Copyright (c) 2011-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");
var Ext = require("Ext").Ext;

var schemaName = "auditLog";
var queryName = "Client API Actions";

var startTime = new Date();
var month = startTime.getMonth() + 1;
var day = startTime.getDate();
var year = startTime.getFullYear();
var hour = startTime.getHours() + 1;
var minute = startTime.getMinutes() + 1;
var startTimeString = year + "-" + month + "-" + day + " " + hour + ":" + minute;

var comment = "Server side script testing ("+startTimeString+"): ";

var filters = [ LABKEY.Filter.create("Comment", startTimeString, LABKEY.Filter.Types.CONTAINS) ];

// Only testing select and insert on audit log until there are APIs to create a table to test more thoroughly on.
var errors = [];
var testResults = [];
var testFunctions = [
    function() //testResults[0]
    {
        var newRow = [ { Comment: comment+"insertRows" } ];
        testResults[testResults.length] = LABKEY.Query.insertRows(schemaName, queryName, newRow);
        executeNext();
    },

    function() //testResults[1]
    {
        testResults[testResults.length] = LABKEY.Query.selectRows({schemaName:schemaName, queryName:queryName, sort: "-Date", filterArray: filters});
        executeNext();
    },

    function() //testResults[2]
    {
        testResults[testResults.length] = LABKEY.Query.selectRows(schemaName+"-badname", queryName);
        executeNext();
    },

    function() //testResults[3]
    {
        testResults[testResults.length] = LABKEY.Query.executeSql({schemaName: schemaName, sort: "Date", sql: "select audit.Date from audit"});
        executeNext();
    },

// Test QUERY.saveRows (transacted)
    function() //testResults[4]
    {
        testResults[testResults.length] = LABKEY.Query.saveRows({
            commands:[
                {schemaName:schemaName, queryName:queryName, command:'insert', rows:[{ Comment: comment+'saveRows ERROR' }]} ,
                {schemaName:'noSuchSchema', queryName:queryName, command:'insert', rows:[{ Comment: comment+'saveRows ERROR' }]},
                {schemaName:schemaName, queryName:queryName, command:'insert', rows:[{ Comment: comment+'saveRows ERROR' }]}
            ]
        });
        executeNext();
    },

// Test QUERY.saveRows (not transacted)
    function() //testResults[5]
    {
        testResults[testResults.length] = LABKEY.Query.saveRows({
            commands:[
                {schemaName:schemaName, queryName:queryName, command:'insert', rows:[{ Comment: comment+'saveRows success' }]} ,
                {schemaName:'noSuchSchema', queryName:queryName, command:'insert', rows:[{ Comment: comment+'saveRows ERROR' }]},
                {schemaName:schemaName, queryName:queryName, command:'insert', rows:[{ Comment: comment+'saveRows ERROR' }]}
            ],
            transacted: false
        });
        executeNext();
    },

// Verify QUERY.saveRows operations
    function() //testResults[6]
    { // Check for inserted rows.
        testResults[testResults.length] = LABKEY.Query.selectRows({schemaName:schemaName, queryName:queryName, filterArray: filters});
        executeNext();
    },

// Verify QUERY.saveRows operations
    function() //testResults[7]
    { // Check that improper inserts did not occur (Comment contains ERROR)
        testResults[testResults.length] = LABKEY.Query.selectRows({schemaName:schemaName, queryName:queryName, filterArray: [ LABKEY.Filter.create("Comment", startTimeString, LABKEY.Filter.Types.CONTAINS), LABKEY.Filter.create('Comment', 'ERROR', LABKEY.Filter.Types.CONTAINS) ]});
        executeNext();
    },

    // last function checks all results.
    function()
    {
        if (!testResults[0].rowsAffected || testResults[0].rowsAffected != 1)
            errors[errors.length] = new Error("Query.insertRows() = "+Ext.util.JSON.encode(testResults[0]));

        if (!testResults[1].rows || testResults[1].rows.length  != 1)
            errors[errors.length] = new Error("Query.selectRows() = "+Ext.util.JSON.encode(testResults[1]));

        if (!testResults[2].exception)
            errors[errors.length] = new Error("Query.selectRows(badSchemaName) = "+Ext.util.JSON.encode(testResults[2]));

        if (!testResults[3].rows)
            errors[errors.length] = new Error("Query.executeSql() = "+Ext.util.JSON.encode(testResults[3]));

        if (!testResults[4].exception)
            errors[errors.length] = new Error("Query.saveRows(transacted) = "+Ext.util.JSON.encode(testResults[4]));

        if (!testResults[5].exception)
            errors[errors.length] = new Error("Query.saveRows(non-transacted) = "+Ext.util.JSON.encode(testResults[5]));

        if (!testResults[6].rows || testResults[6].rows.length != 2)
            errors[errors.length] = new Error("Verify inserted rows = "+Ext.util.JSON.encode(testResults[6]));

        if (!testResults[7].rows || testResults[7].rows.length != 0)
            errors[errors.length] = new Error("Verify failed inserts = "+Ext.util.JSON.encode(testResults[7]));

        if( errors.length > 0 )
            throw errors;
    }
];

function executeNext()
{
    var currentFn = testFunctions[testResults.length];
    currentFn();
}

function doTest()
{
    executeNext();
}
