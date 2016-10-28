/*
 * Copyright (c) 2013-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
window.addEventListener('error', function(text, url, line)
{
    // NOTE: https://code.google.com/p/chromium/issues/detail?id=424599
    var excludedErrors = ["ext-all-sandbox", "ERR_CACHE_MISS"];

    // Event parsing logic from https://github.com/barbushin/javascript-error-notifier
    if(text.filename) {
        url = text.filename;
    }
    if(text.lineno) {
        line = text.lineno;
    }
    if(text.target.chrome && !url && !line) {
        return; // ignore handling Google Chrome extensions errors
    }
    if(text.message) {
        text = text.message;
    }
    else {
        // draft fix of http://code.google.com/p/chromium/issues/detail?id=8939
        if(text.target && text.target.src) {
            url = window.location.href;
            text = 'File not found: ' + text.target.src;
        }
    }
    text = (typeof(text) != 'string' ? 'Unknown JavaScript error' : text.replace(/^Uncaught /g, ''));
    
    var error = text + "\n" + url + ":line" + line;

    for (var i = excludedErrors.length - 1; i >= 0; i--) {
        if (error.indexOf(excludedErrors[i]) != -1) {return;}
    }

    chrome.runtime.sendMessage({command: 'get'}, function(response){
        if (!response.paused)
            confirm(error);
    });
}, false);

pauseJsErrorChecker = function()
{
    chrome.runtime.sendMessage({command: "set", value: true});
};
window.addEventListener("pauseJsErrorChecker", pauseJsErrorChecker, false);

resumeJsErrorChecker = function()
{
    chrome.runtime.sendMessage({command: "set", value: false});
};
window.addEventListener("resumeJsErrorChecker", resumeJsErrorChecker, false);