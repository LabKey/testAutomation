/*
 * Copyright (c) 2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// Designed to be run by JavaScriptExecutor#executeAsyncScript
var callback = arguments[arguments.length - 1]; // See WebDriver documentation

function start()
{
    var n = 6;  // 7 = ~750 folders.
    var fibArray = calculateFibonacci(n);
    var startTime = (new Date()).getTime();

    function createContainers(arr, path)
    {
        var chars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
        var folders = [];

        function createHelper(arr, path, delta)
        {
            for (var j = 0; j < arr[(arr.length-1)]; j++)
            {
                var nextDelta = delta + chars[j];
                var folderName = '[' + nextDelta + ']';
                folders.push({
                    name          : folderName,
                    containerPath : path });
                var nextPath = (path === undefined ? '/' : path + "/") + folderName;
                createHelper(arr.slice(0, (arr.length-1)), nextPath, nextDelta);
            }
        }

        createHelper(arr, path, '');

        function createSyncronous(folders)
        {
            if (folders.length > 0)
            {
                LABKEY.Security.createContainer({
                    name          : folders[0].name,
                    containerPath : folders[0].containerPath,
                    success       : folders.length > 1 ? function(ci) {
                            createSyncronous(folders.slice(1, folders.length));} : callback,
                    failure       : function(xhr) {
                        console.error(xhr);
                        alert("Error trying to create container." + folders[0].name + " -- " + folders[0].containerPath);
                    }
                });
            }
        }

        createSyncronous(folders);
    }

    createContainers(fibArray, LABKEY.container.path);
}

/**
 * Returns an array of length n of Fibonacci values. For example,
 * if you gave n=3 it would return [0,1,1].
 * @param n
 */
function calculateFibonacci(n) {

    n = n-1; // since we are returning a 0-based array

    if (n == 0)
        return [0];
    if (n == 1)
        return [0,1];

    var ret = [0,1];
    for (var x = 2; x <= n; x++) { ret.push(ret[x-1] + ret[x-2]); }
    return ret;
}

start();