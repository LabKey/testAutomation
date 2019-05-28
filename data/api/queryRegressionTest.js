/*
 * Copyright (c) 2013-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var testNumber = arguments[0];

var success = function(index, msg) {
    return '' + index + ')SUCCESS: ' + msg + '\n';
};
var failure = function(index, msg) {
    return '' + index + ')FAILURE: ' + msg + '\n';
};

var testResult;
var testFunctions = [
        function() // testNumber = 0
        {
            LABKEY.Query.executeSql({
                schemaName : 'lists',
                sql        : 'SELECT * FROM NewPeople',
                success    : successHandler,
                failure    : failureHandler
            });
        },

        function() // testNumber = 1
        {
            LABKEY.Query.executeSql({
                schemaName : 'lists',
                sql        : 'SELECT * FROM NewPeople',
                maxRows    : 93,
                success    : successHandler,
                failure    : failureHandler
            });
        },

        function() // testNumber = 2
        {
            LABKEY.Query.executeSql({
                schemaName : 'lists',
                requiredVersion : '9.1',
                sql        : 'SELECT * FROM NewPeople WHERE FirstName LIKE \'%A%\' LIMIT 10',
                success    : successHandler,
                failure    : failureHandler
            });
        },

        // TODO: 35526: Ext4.Ajax.request doesn't trigger callbacks when invoked by Geckodriver
        function() // testNumber = 3
        {
            Ext4.Ajax.request({
                url     : LABKEY.ActionURL.buildURL('query', 'selectRows'),
                method  : 'POST',
                success : function(json) {
                    successHandler.call(this, Ext4.decode(json.responseText));
                },
                failure : failureHandler,
                params  : {
                    'schemaName' : 'lists',
                    'query.queryName' : 'NewPeople',

                    // labkey sort
                    'query.sort' : '-Age',

                    // ext sort
                    'sort' : 'Age',
                    'dir'  : 'DESC',

                    // filter
                    'query.Age~gt' : 25
                }
            });
        },

        // last function returns results
        function()
        {
            var html = '';
            switch (testNumber) {
                case 0:
                    if (testResult.rowCount !== undefined && testResult.rowCount == 125)
                        html += success(0, 'executeSql 0 returned 125 rows');
                    else
                        html += failure(0, 'executeSql 0 returned ' + testResult.rowCount + ' rows, expected 125.  Error value = ' + testResult.exception);
                    break;
                case 1:
                    if (testResult.rows !== undefined && testResult.rows.length == 93)
                        html += success(1, 'executeSql 1 returned 93 rows');
                    else
                        html += failure(1, 'executeSql 1 failed to return 93 rows.  Error value = ' + testResult.exception);
                    break;
                case 2:
                    if (testResult.rows !== undefined && testResult.rows.length == 10)
                        html += success(2, 'executeSql 2 returned 10 rows');
                    else
                        html += failure(2, 'executeSql 2 returned ' + testResult.rowCount + ' rows, expected 10.  Error value = ' + testResult.exception);
                    if (testResult.formatVersion && testResult.formatVersion == 9.1)
                        html += success(2, 'executeSql 2 returned with requested v9.1');
                    else
                        html += failure(2, 'executeSql 2 failed to return v9.1. Version value = ' + testResult.formatVersion);
                    break;
                case 3: // TODO: Not working through Geckodriver
                    if (testResult.rows !== undefined && testResult.rows.length == 96)
                        html += success(3, "selectRows 3 returned 96 rows with mixed sort parameters");
                    else
                        html += failure(3, "selectRows 3 failed with mixed sort parameters. Error value = '" + testResult.exception);
                    break;
                default:
                    throw "No such test, select [0-3]: " + testNumber;
            }

            callback(html);
        }
];

var executeNext = function()
{
    var currentFn;
    if (testResult) { // Validate result if present
        currentFn = testFunctions[testFunctions.length - 1];
        window.console.log("Starting test #" + testNumber);
    }
    else{
        currentFn = testFunctions[testNumber];
        window.console.log("Test #" + testNumber + " finished.");
    }


    try
    {
        currentFn();
    }
    catch (e)
    {
        callback('ERROR: ' + e.message);
    }
};

var failureHandler = function(errorInfo, responseObj, options)
{
    testResult = errorInfo;
    executeNext();
};

var successHandler = function(data, responseObj, options)
{
    testResult = data;
    executeNext();
};

LABKEY.requiresExt4ClientAPI(executeNext);
