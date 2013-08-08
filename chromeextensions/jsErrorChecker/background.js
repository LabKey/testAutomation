/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var checkerPaused = false;

chrome.extension.onMessage.addListener(function (pause, sender, sendResponse) {
    if ('get' == pause.command)
    {
        sendResponse({paused: checkerPaused});
    }
    else if ('set' == pause.command)
    {
        checkerPaused = pause.value;
    }
});