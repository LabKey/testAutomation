/*
 * Copyright (c) 2011-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// TODO: Query APIs added to this script should be matched by updates to test/modules/simpletest/resources/scripts/validationTest/queryTest.js

// Designed to be run by JavaScriptExecutor#executeAsyncScript
var callback = arguments[arguments.length - 1]; // See WebDriver documentation

var schemaName = 'lists';
var queryName = 'People';
var viewName = 'QueryTestView';

var testResults = [];
var testFunctions = [
    function() //testResults[0]
    {
        LABKEY.Query.selectRows(schemaName, queryName, successHandler, failureHandler);
    },

    function() //testResults[1]
    {
        LABKEY.Query.selectRows(schemaName, queryName, successHandler, failureHandler, [ LABKEY.Filter.create('FirstName', 'Norbert') ]);
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
        LABKEY.Query.updateRows(schemaName, queryName, [ rowCopy ], successHandler, failureHandler);        
    },

    function() //testResults[3]
    {
        // get the result from the single-row select call:
        var prevRowset = testResults[1].rows;
        var rowCopy = {};
        for (var prop in prevRowset[0])
            rowCopy[prop] = prevRowset[0][prop];
        rowCopy.Age = 99;
        LABKEY.Query.updateRows(schemaName, queryName, [ rowCopy ], successHandler, failureHandler);        
    },

    function() //testResults[4]
    {
        // get the result from the single-row select call:
        var prevRowset = testResults[1].rows;
        LABKEY.Query.deleteRows(schemaName, queryName, prevRowset, successHandler, failureHandler);        
    },

    function() //testResults[5]
    {
        // get the result from the single-row select call:
        var prevRowset = testResults[1].rows;
        LABKEY.Query.insertRows(schemaName, queryName, prevRowset, successHandler, failureHandler);        
    },

    function() //testResults[6]
    {
        // get the result from the single-row select call:
        var missingLastName = [ { FirstName: 'Herbert', Age: 100 } ];
        LABKEY.Query.insertRows(schemaName, queryName, missingLastName, successHandler, failureHandler);
    },

    function() //testResults[7]
    {
        LABKEY.Query.selectRows(schemaName + '-badname', queryName, successHandler, failureHandler);
    },

    function() //testResults[8]
    {
        LABKEY.Query.executeSql({schemaName: 'lists', sort: 'Age', sql: 'select People.age from People', saveInSession: true, successCallback: successHandler, errorCallback: failureHandler});
    },

    function() //testResults[9]
    {
        LABKEY.Query.executeSql({
            schemaName:'lists',
            sql: 'select subfolderList.FirstName from Project."api folder/subfolder/".lists.subfolderList',
            successCallback: successHandler,
            errorCallback: failureHandler
        });
    },

    function() //testResults[10]
    {
        LABKEY.Query.executeSql({
            schemaName:'lists',
            sql: 'select otherProjectList.FirstName from "/OtherClientAPITestProject".lists.otherProjectList',
            successCallback: successHandler,
            errorCallback: failureHandler
        });
    },

    // Test QUERY.saveRows (transacted)
    function() //testResults[11]
    {
        try
        {
            var peopleRowset = testResults[0].rows;
            peopleRowset[0].Age = -1;
            peopleRowset[1].Age = -1;
            LABKEY.Query.saveRows({
                commands:[
                    {schemaName:schemaName, queryName:queryName, command:'insert', rows:[peopleRowset[0]]},
                    {schemaName:schemaName, queryName:queryName, command:'update', rows:[peopleRowset[1]]},
                    {schemaName:schemaName, queryName:queryName, command:'delete', rows:[peopleRowset[2]]},
                    {schemaName:schemaName, queryName:'noSuchQuery', command:'insert', rows:[peopleRowset[0]]}
                ],
                successCallback: successHandler,
                errorCallback: failureHandler
            });
        }
        catch (err)
        {
            failureHandler(err, null, null);
        }
    },

    // Test QUERY.saveRows (not transacted)
    function() //testResults[12]
    {
        try
        {
            var peopleRowset = testResults[0].rows;
            peopleRowset[3].Age = 101;
            peopleRowset[5].Age = 101;
            peopleRowset[6].Age = -1;
            LABKEY.Query.saveRows({
                commands:[
                    {schemaName:schemaName, queryName:queryName, command:'insert', rows:[peopleRowset[3]]},
                    {schemaName:schemaName, queryName:queryName, command:'update', rows:[peopleRowset[5]]},
                    {schemaName:schemaName, queryName:queryName, command:'delete', rows:[peopleRowset[6]]},
                    {schemaName:'noSuchSchema', queryName:queryName, command:'insert', rows:[peopleRowset[6]]},
                    {schemaName:schemaName, queryName:queryName, command:'insert', rows:[peopleRowset[6]]}
                ],
                successCallback: successHandler,
                errorCallback: failureHandler,
                transacted: false
            });
        }
        catch (err)
        {
            failureHandler(err, null, null);
        }
    },

// Verify QUERY.saveRows operations
    function() //testResults[13]
    { // Check that successful inserts/updates occurred (Age: 101) and unsuccessful deletes did not (Age: 17)
        LABKEY.Query.selectRows(schemaName, queryName, successHandler, failureHandler, [ LABKEY.Filter.create('Age', '101;17', LABKEY.Filter.Types.EQUALS_ONE_OF) ]);
    },

// Verify QUERY.saveRows operations
    function() //testResults[14]
    { // Check that failed inserts/updates did not occur (Age: -1) and successful deletes did (Age: 88)
        LABKEY.Query.selectRows(schemaName, queryName, successHandler, failureHandler, [ LABKEY.Filter.create('Age', '-1;88', LABKEY.Filter.Types.EQUALS_ONE_OF) ]);
    },

// Verify QUERY.selectRows handles QueryParseException
    function() //testResults[15]
    {
        LABKEY.Query.executeSql({schemaName: schemaName, sql: 'Bad Query', successCallback: successHandler, errorCallback: failureHandler});
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

    // last function returns results.
    function()
    {
        var html = '';
        if (testResults[0].rowCount !== undefined && testResults[0].rowCount == 7)
            html += '0)SUCCESS: Select 1 returned 7 rows\n';
        else
            html += '0)FAILURE: Select 1 returned ' + testResults[0].rowCount + ' rows, expected 7.  Error value = ' + testResults[0].exception + '\n';

        if (testResults[1].rowCount !== undefined && testResults[1].rowCount == 1)
            html += '1)SUCCESS: Select 2 returned 1 rows\n';
        else
            html += '1)FAILURE: Select 2 returned ' + testResults[1].rowCount + ' rows, expected 1.  Error value = ' + testResults[1].exception + '\n';

        if (testResults[2].exception !== undefined && testResults[2].exception)
            html += '2)SUCCESS: Bad update generated exception: ' + testResults[2].exception + '\n';
        else
            html += '2)FAILURE: Bad update did not generate expected exception.\n';

        if (testResults[3].rowsAffected !== undefined && testResults[3].rowsAffected == 1)
            html += '3)SUCCESS: Update affected 1 rows\n';
        else
            html += '3)FAILURE: Update affected ' + testResults[2].rowCount + ' rows, expected 1.  Error value = ' + testResults[3].exception + '\n';

        if (testResults[4].rowsAffected !== undefined && testResults[4].rowsAffected == 1)
            html += '4)SUCCESS: Delete affected 1 rows\n';
        else
            html += '4)FAILURE: Delete affected ' + testResults[4].rowCount + ' rows, expected 1.  Error value = ' + testResults[4].exception + '\n';

        if (testResults[5].rowsAffected!== undefined && testResults[5].rowsAffected == 1)
            html += '5)SUCCESS: Insert created 1 rows\n';
        else
            html += '5)FAILURE: Insert created ' + testResults[5].rowCount + ' rows, expected 1.  Error value = ' + testResults[5].exception + '\n';

        if (testResults[6].exception !== undefined && testResults[6].exception)
            html += '6)SUCCESS: Bad insert generated exception: ' + testResults[6].exception + '\n';
        else
            html += '6)FAILURE: Bad insert did not generate expected exception.\n';

        if (testResults[7].exception !== undefined && testResults[7].exception)
            html += '7)SUCCESS: Bad query generated exception: ' + testResults[7].exception + '\n';
        else
            html += '7)FAILURE: Bad query did not generate expected exception.\n';

        if (testResults[8].rowCount !== undefined && testResults[8].rowCount == 7)
            html += '8a)SUCCESS: executeSql returned 7 rows\n';
        else
            html += '8a)FAILURE: executeSql returned ' + testResults[8].rowCount + ' rows, expected 7. Error value = ' + testResults[8].exception + '\n';

        if (testResults[8].rows !== undefined && testResults[8].rows.length >= 3 && testResults[8].rows[1].age < testResults[8].rows[2].age)
            html += '8b)SUCCESS: executeSql returned properly sorted\n';
        else
            html += '8b)FAILURE: executeSql returned unsorted data: ' + testResults[8].rows[1].age + ' before ' + testResults[8].rows[1].age + '\n';

        if (testResults[8].queryName && testResults[8].queryName.indexOf('lists_temp') > -1)
            html += '8c)SUCCESS: executeSql returned a session-based query\n';
        else
            html += '8c)FAILURE: executeSql returned \'' + testResults[8].queryName + '\'. Was expecting a session-based query to be returned.';

        if (testResults[9].rowCount !== undefined && testResults[9].rowCount == 7)
            html += '9)SUCCESS: cross-folder executeSql succeeded\n';
        else
            html += '9)FAILURE: executeSql returned ' + testResults[9].rowCount + ' rows, expected 7.  Error value = ' + testResults[9].exception + '\n';

        if (testResults[10].rowCount !== undefined && testResults[10].rowCount == 7)
            html += '10)SUCCESS: cross-project executeSql succeeded\n';
        else
            html += '10)FAILURE: executeSql returned ' + testResults[10].rowCount + ' rows, expected 7.  Error value = ' + testResults[10].exception + '\n';

        if (testResults[11].exception)
            html += '11)SUCCESS: Bad saveRows exception: ' + testResults[11].exception + '\n';
        else
            html += '11)FAILURE: Bad saveRows did not generate an exception.\n';

        if (testResults[12].exception)
            html += '12)SUCCESS: Bad saveRows exception: ' + testResults[12].exception + '\n';
        else
            html += '12)FAILURE: Bad saveRows did not generate an exception.\n';

        if (testResults[13].rowCount !== undefined && testResults[13].rowCount == 3)
            html += '13)SUCCESS: Non-transacted bad saveRows modified rows.\n';
        else
            html += '13)FAILURE: Non-transacted bad saveRows returned ' + testResults[13].rowCount + ' rows, expected 3.  Error value = ' + testResults[13].exception + '\n';

        if (testResults[14].rowCount !== undefined && testResults[14].rowCount == 0)
            html += '14)SUCCESS: Transacted bad saveRows did not modify rows rows.\n';
        else
            html += '14)FAILURE: Non-transacted bad saveRows returned ' + testResults[14].rowCount + ' rows, expected 0.  Error value = ' + testResults[14].exception + '\n';

        if (testResults[15].exception)
            if (testResults[11].exceptionClass !== undefined && testResults[15].exceptionClass == "org.labkey.api.query.QueryParseException")
                html += '15)SUCCESS: Bad query exception: ' + testResults[15].exceptionClass + '\n';
            else
                html += '15)FAILURE: Bad query generated wrong exception: ' + testResults[15].exceptionClass + '\n';
        else
            html += '15)FAILURE: Bad query did not generate an exception.\n';

        if (testResults[16].exception)
            html += '16)FAILURE: Failed to create custom view for list\n';
        else
            html += '16)SUCCESS: Created Custom View: \'' + viewName + '\' for list\n';

        if (testResults[17].values && testResults[17].values.length == 6)
            html += '17)SUCCESS: SelectDistinctRows returned correct result set\n';
        else
            html += '17)FAILURE: SelectDistinctRows failed to return expected result of 6 values\n';

        if (testResults[18].values && testResults[18].values.length == 2)
            html += '18)SUCCESS: SelectDistinctRows returned correct custom view filtered result set\n';
        else
            html += '18)FAILURE: SelectDistinctRows failed to return expected result of 2 values\n';

        if (testResults[19].rowCount !== undefined && testResults[19].rowCount == 2)
            html += '19)SUCCESS: SelectRows returned correct expected result when using a search filter\n';
        else
            html += '19)FAILURE: SelectRows failed to return expected result of 2 rows. Result had ' + testResults[19].rowCount + ' rows\n';

        callback(html);
    }
];

function executeNext()
{
    var currentFn = testFunctions[testResults.length];
    currentFn();
}

function failureHandler(errorInfo, responseObj, options)
{
    testResults[testResults.length] = errorInfo;
    executeNext();
}

function successHandler(data, responseObj, options)
{
    testResults[testResults.length] = data;
    executeNext();
}

executeNext();
