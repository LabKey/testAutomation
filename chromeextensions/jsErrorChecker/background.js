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