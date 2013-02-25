/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var success = function(index, msg) {
    return '' + index + ')SUCCESS: ' + msg + '<br>';
};
var failure = function(index, msg) {
    return '' + index + ')FAILURE: ' + msg + '<br>';
};

var testResults = [];
var testFunctions = [
        function() // testResults[0]
        {
            LABKEY.Query.executeSql({
                schemaName : 'lists',
                sql        : 'SELECT * FROM NewPeople',
                success    : successHandler,
                failure    : failureHandler
            });
        },

        function() // testResults[1]
        {
            LABKEY.Query.executeSql({
                schemaName : 'lists',
                sql        : 'SELECT * FROM NewPeople',
                maxRows    : 93,
                success    : successHandler,
                failure    : failureHandler
            });
        },

        function() // testResults[2]
        {
            LABKEY.Query.executeSql({
                schemaName : 'lists',
                requiredVersion : '9.1',
                sql        : 'SELECT * FROM NewPeople WHERE FirstName LIKE \'%A%\' LIMIT 10',
                success    : successHandler,
                failure    : failureHandler
            });
        },

        // last function sets the contents of the results div.
        function()
        {
            var html = '';
            if (testResults[0].rowCount !== undefined && testResults[0].rowCount == 125)
                html += success(0, 'executeSql 0 returned 125 rows');
            else
                html += failure(0, 'executeSql 0 returned ' + testResults[0].rowCount + ' rows, expected 125.  Error value = ' + testResults[0].exception);

            if (testResults[1].rows !== undefined && testResults[1].rows.length == 93)
                html += success(1, 'executeSql 1 returned 93 rows');
            else
                html += failure(1, 'executeSql 1 failed to return 93 rows.  Error value = ' + testResults[1].exception);

            if (testResults[2].rows !== undefined && testResults[2].rows.length == 10)
                html += success(2, 'executeSql 2 returned 10 rows');
            else
                html += failure(2, 'executeSql 2 returned ' + testResults[2].rowCount + ' rows, expected 10.  Error value = ' + testResults[2].exception);

            if (testResults[2].formatVersion && testResults[2].formatVersion == 9.1)
                html += success(2, 'executeSql 2 returned with requested v9.1');
            else
                html += failure(2, 'executeSql 2 failed to return v9.1. Version value = ' + testResults[3].formatVersion);

            document.getElementById('testDiv').innerHTML = html;
        }
];

var executeNext = function()
{
    var currentFn = testFunctions[testResults.length];
    currentFn();
};

var failureHandler = function(errorInfo, responseObj, options)
{
    testResults[testResults.length] = errorInfo;
    executeNext();
};

var successHandler = function(data, responseObj, options)
{
    testResults[testResults.length] = data;
    executeNext();
};

executeNext();