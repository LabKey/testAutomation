/*
 * Copyright (c) 2014-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

// Designed to be run by JavaScriptExecutor#executeAsyncScript
var callback = arguments[arguments.length - 1]; // See WebDriver documentation

LABKEY.requiresExt3ClientAPI(function()
{
    window.result = '';

    function logMsg(msg){
        var date = new Date();
        window.result += date.getHours() + ':' + date.getMinutes() + ':' + date.getSeconds() + '\t' + msg + '\n';
        if (msg.contains('ERROR'))
            callback(window.result);
    }

    logMsg('Test Started');

    //use the current container
    var fileSystem = new LABKEY.FileSystem.WebdavFileSystem();

    fileSystem.on('ready', function(fileSystem){
        logMsg("Filesystem ready");
        doTest();
    }, this);

    var num = 0;
    //who loves ajax...
    function doTest(){
        switch (num){
            //create files we need.  always delete first just in case a file is present from a failed test
            case 0:
                logMsg('Creating files and directories');
                doDeleteFile('newFile2.txt', onSuccess);
                break;
            case 1:
                createFile('newFile2.txt', onSuccess);
                break;
            case 2:
                verifyFileExists('/', 'newFile2.txt', onSuccess);
                break;
            case 3:
                doDeleteFile('newFolder', onSuccess);
                break;
            case 4:
                doCreateDirectory('newFolder/', onSuccess);
                break;
            case 5:
                verifyFileExists('/', 'newFolder/', onSuccess);
                break;
            case 6:
                doCreateDirectory('newFolder/subFolder/', onSuccess);
                break;
            case 7:
                createFile('/newFolder/subFolder/fileInSubfolder.txt', onSuccess);
                break;
            case 8:
                verifyFileExists('/newFolder/subFolder', 'fileInSubfolder.txt', onSuccess);
                break;

            //test move
            case 9:
                logMsg('Testing move');
                doMoveFiles('/newFolder/subFolder/fileInSubfolder.txt', '/newFolder/fileInSubfolder.txt', onSuccess);
                break;
            case 10:
                verifyFileExists('/newFolder/', 'fileInSubfolder.txt', onSuccess);
                break;
            case 11:
                verifyFileDoesNotExist('/newFolder/subFolder/', 'fileInSubfolder.txt', onSuccess);
                break;

            //test rename w/ full path
            case 12:
                logMsg('Testing rename w/ full path');
                doRename('/newFolder/fileInSubfolder.txt', '/newFolder/renamedFile.txt', onSuccess);
                break;
            case 13:
                verifyFileExists('/newFolder/', 'renamedFile.txt', onSuccess);
                break;
            case 14:
                verifyFileDoesNotExist('/newFolder/', 'fileInSubfolder.txt', onSuccess);
                break;

            //test rename w/o full path
            case 15:
                logMsg('Testing rename w/o full path');
                doRename('/newFolder/renamedFile.txt', 'renamedAgain.txt', onSuccess);
                break;
            case 16:
                verifyFileExists('/newFolder/', 'renamedAgain.txt', onSuccess);
                break;
            case 17:
                verifyFileDoesNotExist('/newFolder/', 'renamedFile.txt', onSuccess);
                break;

            //test get history
            case 18:
                logMsg('Testing get history');
                getHistory('/newFolder/renamedAgain.txt', onSuccess);
                break;
            case 19:
                verifyFileExists('/newFolder/', 'renamedAgain.txt', onSuccess);
                break;
            case 20:
                verifyFileDoesNotExist('/newFolder/', 'renamedFile.txt', onSuccess);
                break;

            //cleanup
            case 21:
                logMsg('Starting cleanup');
                doDeleteFile('newFile2.txt', onSuccess);
                break;
            case 22:
                doDeleteFile('newFolder/' ,onSuccess);
                break;
            case 23:
                doDeleteFile('newFolder2/' ,onSuccess);
                break;
            case 24:
                doListFiles('/', 0, onSuccess);
                break;
            case 25:
                logMsg('Test Complete');
                callback(window.result);
                break;
        }
    }

    function doDeleteFile(path, success){
        fileSystem.deletePath({
            path: path,
            success: function(){
                success("Deleted file: " + path);
            },
            failure: function(e){
                logMsg('ERROR: unable to delete file ' + path);
            },
            scope: this
        }, this);
    }

    function doListFiles(path, expected, success){
        fileSystem.listFiles({
            path: path,
            success: function(fileSystem, path, records){
                if(records.length == expected){
                    success('Loaded directory with ' + records.length + ' files');
                }
                else
                    logMsg('ERROR: wrong number of files found.  Expected:  ' + expected + ', Found: ' + records.length);
            },
            failure: function(e){
                logMsg('ERROR: unable to load files');
            },
            scope: this
        }, this);
    }

    function doCreateDirectory(name, success){
        fileSystem.createDirectory({
            path: name,
            success: function(){
                success('Created folder: ' + name);
            },
            failure: function(response, options){
                logMsg("ERROR: unable to creating directory: " + name)
            },
            scope: this
        });
    }

    function doMoveFiles(source, dest, success){
        fileSystem.movePath({
            source: source,
            destination: dest,
            isFile: true,
            success: function(fileSystem, path, records){
                success("Moved files from: " + source + ' to ' + dest);
            },
            failure: function(response, options){
                logMsg('ERROR: unable to perform move');
            },
            scope: this
        });
    }

    function doRename(source, dest, success){
        fileSystem.renamePath({
            source: source,
            destination: dest,
            isFile: true,
            success: function(fileSystem, path, records){
                success("Renamed file from: " + source + ' to ' + dest);
            },
            failure: function(response, options){
                logMsg('ERROR: unable to perform rename');
            },
            scope: this
        });
    }

    function getHistory(path, success){
        fileSystem.getHistory({
            path: path,
            success: function(fileSystem, path, records){
                if(records.length > 0)
                    success("Obtained history for: " + path);
                else
                    logMsg('ERROR: problem getting history for file: ' + path);
            },
            failure: function(response, options){
                logMsg('ERROR getting history for file: ' + path);
            },
            scope: this
        });
    }

    function createFile(name, success){
        var client = new XMLHttpRequest();
        client.onreadystatechange = function()
        {
            if (client.readyState == 4)
            {
                if (200 == client.status || 201 == client.status)   // OK, CREATED
                    success("File created: " + name);
                else
                {
                    logMsg('ERROR: unable to create file');
                    console.log(client)
                }
            }
        };
        var resourcePath = LABKEY.ActionURL.getContextPath() + '/_webdav' + LABKEY.ActionURL.getContainer() + '/@files/' + name;
        client.open("PUT", resourcePath);
        client.setRequestHeader("method", "PUT");
        client.setRequestHeader("Translate", "f");
        client.setRequestHeader("Content-Type", "text/plain");
        client.send('abcd');
    }

    function verifyFile(path, name, status, success)
    {
        var filepath = fileSystem.concatPaths(path, name);
        fileSystem.listFiles({
            path: path,
            forceReload: true,
            success: function(fileSystem, path, records){
                var record = fileSystem.recordFromCache(filepath);
                if(status){
                    if(record)
                        success('Verified file exists: ' + filepath);
                    else
                        logMsg('ERROR: file does not exist: ' + filepath);
                }
                else {
                    if(!record)
                        success('Verified file does not exist: ' + filepath);
                    else
                        logMsg('ERROR: file exists: ' + filepath);
                }
            },
            failure: function(e){
                logMsg('ERROR: unable to load files: ' + path);
            },
            scope: this
        }, this);
    }

    function verifyFileExists(path, name, success)
    {
        verifyFile(path, name, true, success);
    }

    function verifyFileDoesNotExist(path, name, success)
    {
        verifyFile(path, name, false, success)
    }

});