window.addEventListener('error', function(text, url, line)
{
    var excludedErrors = [];

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
    
    var error = text + "\n" + url + "\n" + line;
	
    for (var i = excludedErrors.length - 1; i >= 0; i--) {
		if (text.indexOf(excludedErrors[i]) != -1) {return;}
    }

    chrome.runtime.sendMessage({command: 'get'}, function(response){
        if (!response.paused)
            alert(error);
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