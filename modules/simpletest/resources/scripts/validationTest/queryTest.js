/*
 * Copyright (c) 2011-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");

//==================================================================
// TODO: Query APIs added to this script should be matched by updates to test/data/api/queryTest.js

var schemaName = 'vehicle';
var queryName = 'Vehicles';
var viewName = 'QueryTestView';


var testResults = [];
var testFunctions = [
    function() //testResults[0]
    {
        testResults[testResults.length] = LABKEY.Query.selectRows(schemaName, queryName);
        executeNext();
    },

    function() //testResults[1]
    {
        testResults[testResults.length] = LABKEY.Query.selectRows({schemaName:schemaName, queryName:queryName, filter: [ LABKEY.Filter.create('FirstName', 'Norbert') ]});
        executeNext();
    },

    function() //testResults[2]
    {
        // get the result from the single-row select call:
        var prevRowset = testResults[1].rows;
        var rowCopy = {};
        for (var prop in prevRowset[0])
            rowCopy[prop] = prevRowset[0][prop];
        rowCopy.LastName = null;
        rowCopy.Age = 99;
        testResults[testResults.length] = LABKEY.Query.updateRows(schemaName, queryName, [ rowCopy ]);
        executeNext();
    },

    function() //testResults[3]
    {
        // get the result from the single-row select call:
        var prevRowset = testResults[1].rows;
        var rowCopy = {};
        for (var prop in prevRowset[0])
            rowCopy[prop] = prevRowset[0][prop];
        rowCopy.Age = 99;
        testResults[testResults.length] = LABKEY.Query.updateRows(schemaName, queryName, [ rowCopy ]);
        executeNext();
    },

    function() //testResults[4]
    {
        // get the result from the single-row select call:
        var prevRowset = testResults[1].rows;
        testResults[testResults.length] = LABKEY.Query.deleteRows(schemaName, queryName, prevRowset);
        executeNext();
    },

    function() //testResults[5]
    {
        // get the result from the single-row select call:
        var prevRowset = testResults[1].rows;
        testResults[testResults.length] = LABKEY.Query.insertRows(schemaName, queryName, prevRowset);
        executeNext();
    },

    function() //testResults[6]
    {
        // get the result from the single-row select call:
        var missingLastName = [ { FirstName: 'Herbert', Age: 100 } ];
        testResults[testResults.length] = LABKEY.Query.insertRows(schemaName, queryName, missingLastName);
        executeNext();
    },

    function() //testResults[7]
    {
        testResults[testResults.length] = LABKEY.Query.selectRows(schemaName + '-badname', queryName);
        executeNext();
    },

    function() //testResults[8]
    {
        testResults[testResults.length] = LABKEY.Query.executeSql({schemaName: 'lists', sort: 'Age', sql: 'select People.age from People', saveInSession: true});
        executeNext();
    },

    function() //testResults[9]
    {
        testResults[testResults.length] = LABKEY.Query.executeSql({
            schemaName:'lists',
            sql: 'select subfolderList.FirstName from Project."api folder/subfolder/".lists.subfolderList'
        });
        executeNext();
    },

    function() //testResults[10]
    {
        testResults[testResults.length] = LABKEY.Query.executeSql({
            schemaName:'lists',
            sql: 'select otherProjectList.FirstName from "/OtherClientAPITestProject".lists.otherProjectList'
        });
        executeNext();
    },

// Test QUERY.saveRows (transacted)
    function() //testResults[11]
    {
        var peopleRowset = testResults[0].rows;
        peopleRowset[0].Age = -1;
        peopleRowset[0].Key = null;
        peopleRowset[1].Age = -1;
        peopleRowset[1].Key = null;
        testResults[testResults.length] = LABKEY.Query.saveRows({
            commands:[
                {schemaName:schemaName, queryName:queryName, command:'insert', rows:[peopleRowset[0]]},
                {schemaName:schemaName, queryName:queryName, command:'update', rows:[peopleRowset[1]]},
                {schemaName:schemaName, queryName:queryName, command:'delete', rows:[peopleRowset[2]]},
                {schemaName:schemaName, queryName:'noSuchQuery', command:'insert', rows:[peopleRowset[0]]}
            ]
        });
        executeNext();
    },

// Test QUERY.saveRows (not transacted)
    function() //testResults[12]
    {
        var peopleRowset = testResults[0].rows;
        peopleRowset[3].Age = 101;
        peopleRowset[3].Key = null;
        peopleRowset[5].Age = 101;
        peopleRowset[5].Key = null;
        peopleRowset[6].Age = -1;
        peopleRowset[6].Key = null;
        testResults[testResults.length] = LABKEY.Query.saveRows({
            commands:[
                {schemaName:schemaName, queryName:queryName, command:'insert', rows:[peopleRowset[3]]},
                {schemaName:schemaName, queryName:queryName, command:'update', rows:[peopleRowset[5]]},
                {schemaName:schemaName, queryName:queryName, command:'delete', rows:[peopleRowset[6]]},
                {schemaName:'noSuchSchema', queryName:queryName, command:'insert', rows:[peopleRowset[6]]},
                {schemaName:schemaName, queryName:queryName, command:'insert', rows:[peopleRowset[6]]}
            ],
            transacted: false
        });
        executeNext();
    },

// Verify QUERY.saveRows operations
    function() //testResults[13]
    { // Check that successful inserts/updates occurred (Age: 101) and unsuccessful deletes did not (Age: 17)
        testResults[testResults.length] = LABKEY.Query.selectRows({schemaName:schemaName, queryName:queryName, filter:[ LABKEY.Filter.create('Age', '101;17', LABKEY.Filter.Types.EQUALS_ONE_OF) ]});
        executeNext();
    },

// Verify QUERY.saveRows operations
    function() //testResults[14]
    { // Check that failed inserts/updates did not occur (Age: -1) and successful deletes did (Age: 88)
        testResults[testResults.length] = LABKEY.Query.selectRows({schemaName:schemaName, queryName:queryName, filter: [ LABKEY.Filter.create('Age', '-1;88', LABKEY.Filter.Types.EQUALS_ONE_OF) ]});
        executeNext();
    },

// Verify QUERY.selectRows handles QueryParseException
    function() //testResults[15]
    {
        LABKEY.Query.executeSql({schemaName: 'lists', sql: 'Bad Query', successCallback: successHandler, errorCallback: failureHandler});
    },

// Create a CustomView with a filter applied of Age > 35
    function() //testResults[16]
    {
        LABKEY.Query.getQueryViews({
            schemaName: schemaName, queryName: queryName,
            success: function(query) {

                var view = query.views[3];
                var filter = new LABKEY.Query.Filter('Age', 35, LABKEY.Filter.Types.GREATER_THAN);
                view.name = viewName;
                view.default = false;
                view.filter = [{
                    fieldKey: filter.getColumnName(),
                    op: filter.getFilterType().getURLSuffix(),
                    value: filter.getValue().toString()
                }];
                LABKEY.Query.saveQueryViews({ schemaName: schemaName, queryName: queryName,
                    views: [view],
                    success: successHandler, failure: failureHandler });

            },
            failure: failureHandler
        });
    },

    function() //testResults[17]
    {
        LABKEY.Query.selectDistinctRows({ schemaName: schemaName, queryName: queryName, column: 'Age', success: successHandler, failure: failureHandler });
    },

    function() //testResults[18]
    {
        LABKEY.Query.selectDistinctRows({ schemaName: schemaName, queryName: queryName, column: 'Age', viewName: viewName, success: successHandler, failure: failureHandler });
    },

    function() //testResults[19]
    {
        // Filter for Jane Janeson, John Johnson
        LABKEY.Query.selectRows({
            schemaName: schemaName,
            queryName: queryName,
            filterArray: [ LABKEY.Filter.create('*', 'J', LABKEY.Filter.Types.Q) ],
            success: successHandler,
            failure: failureHandler
        });
    },

    // last function sets the contents of the results div.
    function()
    {
        var html = '';
        if (testResults[0].rowCount == 7)
            html += 'SUCCESS: Select 1 returned 7 rows<br>';
        else
            html += 'FAILURE: Select 1 returned ' + testResults[0].rowCount + ' rows, expected 7.  Error value = ' + testResults[0].exception + '<br>';

        if (testResults[1].rowCount == 1)
            html += 'SUCCESS: Select 2 returned 1 rows<br>';
        else
            html += 'FAILURE: Select 2 returned ' + testResults[1].rowCount + ' rows, expected 1.  Error value = ' + testResults[1].exception + '<br>';

        if (testResults[2].exception)
            html += 'SUCCESS: Bad update generated exception: ' + testResults[2].exception + '<br>';
        else
            html += 'FAILURE: Bad update did not generate expected exception.<br>';

        if (testResults[3].rowsAffected == 1)
            html += 'SUCCESS: Update affected 1 rows<br>';
        else
            html += 'FAILURE: Update affected ' + testResults[2].rowCount + ' rows, expected 1.  Error value = ' + testResults[3].exception + '<br>';

        if (testResults[4].rowsAffected == 1)
            html += 'SUCCESS: Delete affected 1 rows<br>';
        else
            html += 'FAILURE: Delete affected ' + testResults[4].rowCount + ' rows, expected 1.  Error value = ' + testResults[4].exception + '<br>';

        if (testResults[5].rowsAffected == 1)
            html += 'SUCCESS: Insert created 1 rows<br>';
        else
            html += 'FAILURE: Insert created ' + testResults[5].rowCount + ' rows, expected 1.  Error value = ' + testResults[5].exception + '<br>';

        if (testResults[6].exception)
            html += 'SUCCESS: Bad insert generated exception: ' + testResults[6].exception + '<br>';
        else
            html += 'FAILURE: Bad insert did not generate expected exception.<br>';

        if (testResults[7].exception)
            html += 'SUCCESS: Bad query generated exception: ' + testResults[7].exception + '<br>';
        else
            html += 'FAILURE: Bad query did not generate expected exception.<br>';

        if (testResults[8].rowCount == 7)
            html += 'SUCCESS: executeSql returned 7 rows<br>';
        else
            html += 'FAILURE: executeSql returned ' + testResults[8].rowCount + ' rows, expected 7. Error value = ' + testResults[8].exception + '<br>';

        if (testResults[8].queryName && testResults[8].queryName.indexOf('lists_temp') > -1)
            html += 'SUCCESS: executeSql returned a session-based query<br>';
        else
            html += 'FAILURE: executeSql returned \'' + testResults[8].queryName + '\'. Was expecting a session-based query to be returned.';

        if (testResults[8].rows[1].age < testResults[8].rows[2].age)
            html += 'SUCCESS: executeSql returned properly sorted<br>';
        else
            html += 'FAILURE: executeSql returned unsorted data: ' + testResults[8].rows[1].age + ' before ' + testResults[8].rows[1].age + '<br>';

        if (testResults[9].rowCount == 7)
            html += 'SUCCESS: cross-folder executeSql succeeded<br>';
        else
            html += 'FAILURE: executeSql returned ' + testResults[9].rowCount + ' rows, expected 7.  Error value = ' + testResults[9].exception + '<br>';

        if (testResults[10].rowCount == 7)
            html += 'SUCCESS: cross-project executeSql succeeded<br>';
        else
            html += 'FAILURE: executeSql returned ' + testResults[10].rowCount + ' rows, expected 7.  Error value = ' + testResults[10].exception + '<br>';

        if (testResults[11].exception)
            html += 'SUCCESS: Bad saveRows exception: ' + testResults[11].exception + '<br>';
        else
            html += 'FAILURE: Bad saveRows did not generate an exception.<br>';

        if (testResults[12].exception)
            html += 'SUCCESS: Bad saveRows exception: ' + testResults[12].exception + '<br>';
        else
            html += 'FAILURE: Bad saveRows did not generate an exception.<br>';

        if (testResults[13].rowCount == 3)
            html += 'SUCCESS: Non-transacted bad saveRows modified rows.<br>';
        else
            html += 'FAILURE: Non-transacted bad saveRows returned ' + testResults[13].rowCount + ' rows, expected 3.  Error value = ' + testResults[13].exception + '<br>';

        if (testResults[14].rowCount == 0)
            html += 'SUCCESS: Transacted bad saveRows did not modify rows rows.<br>';
        else
            html += 'FAILURE: Non-transacted bad saveRows returned ' + testResults[14].rowCount + ' rows, expected 0.  Error value = ' + testResults[14].exception + '<br>';

        if (testResults[15].exception)
            if (testResults[15].exceptionClass == "org.labkey.api.query.QueryParseException")
                html += 'SUCCESS: Bad query exception: ' + testResults[15].exceptionClass + '<br>';
            else
                html += 'FAILURE: Bad query generated wrong exception: ' + testResults[15].exceptionClass + '<br>';
        else
            html += 'FAILURE: Bad query did not generate an exception.<br>';

        if (testResults[16].exception)
            html += '16)FAILURE: Failed to create custom view for list<br>';
        else
            html += '16)SUCCESS: Created Custom View: \'' + viewName + '\' for list<br>';

        if (testResults[17].values && testResults[17].values.length == 6)
            html += '17)SUCCESS: SelectDistinctRows returned correct result set<br>';
        else
            html += '17)FAILURE: SelectDistinctRows failed to return expected result of 6 values<br>';

        if (testResults[18].values && testResults[18].values.length == 2)
            html += '18)SUCCESS: SelectDistinctRows returned correct custom view filtered result set<br>';
        else
            html += '18)FAILURE: SelectDistinctRows failed to return expected result of 2 values<br>';

        if (testResults[19].rowCount == 2)
            html += '19)SUCCESS: SelectRows returned correct expected result when using a search filter<br>';
        else
            html += '19)FAILURE: SelectRows failed to return expected result of 2 rows. Result had ' + testResults[19].rowCount + ' rows<br>';

        if (html.contains("FAILURE"))
            throw new Error(html);
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
