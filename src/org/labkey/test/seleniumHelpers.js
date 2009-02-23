/*
 * Copyright (c) 2007-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/*
 * This file contains a set of helper functions that are injected into
 * the selenium frame at test startup. These make it somewhat easier
 * to run old LabKey tests and write new ones.
 */

/**
 * Return hrefs for all links on the current page as \n delimited string
 */

selenium.getLinkAddresses = function () {
        var links = selenium.browserbot.getCurrentWindow().document.links;
        var addresses = new Array();
        for (var i = 0; i < links.length; i++)
          addresses[i] = links[i].getAttribute('href');
        return addresses.join('\\n');
};

selenium.countLinksWithText = function (txt) {
    var doc = selenium.browserbot.getCurrentWindow().document;
    var count = 0;
    for (var i = 0; i < doc.links.length; i++) {
        if (doc.links[i].innerHTML && doc.links[i].innerHTML.indexOf(txt) >= 0)
            count++;
    }
    return count;
};

selenium.appendToFormField = function (fieldName, txt) {
    var doc = selenium.browserbot.getCurrentWindow().document;
    doc.forms[0][fieldName].value = doc.forms[0][fieldName].value + txt;
    return "OK";
};

selenium.clickExtComponent = function (id) {
    var ext = selenium.browserbot.getCurrentWindow().Ext;
    //if (!ext) return false;
    var cmp = ext.getCmp(id);
    //if (!cmp) return false;
    if (cmp.handler)
        return cmp.fireEvent("click");
    else if (cmp.href)
    {
        //window.location = cmp.href;
        cmp.show();
        return true;
    }
    return false;
};

selenium.getExtElementId = function (id) {
    var ext = selenium.browserbot.getCurrentWindow().Ext;
    //if (!ext) return false;
    var cmp = ext.getCmp(id);
    //if (!cmp) return false;

    return cmp.getEl().id;
};

// firefox error console listener
// http://sejq.blogspot.com/2008/12/can-selenium-detect-if-page-has.html
// https://developer.mozilla.org/en/Console_service
if (browserVersion.isChrome) {
    var consoleListener = {
        installed: false,
        observe: function( msg ) {
//            LOG.info("JsErrorChecker: " + msg.message);
            try {
                dump("Log : " + msg.message);
                if (msg.message != null &&
                    msg.message.indexOf("[JavaScript Error:") == 0 &&
                    msg.message.indexOf("{file: \"chrome://") == -1)
                {
                    LOG.error("JsErrorChecker: " + msg.message);

                    var result = {};
                    result.failed = true;
                    result.failureMessage = "JsErrorChecker: " + msg.message;
                    currentTest.result = result;
                    currentTest.commandComplete(this.result);
                 }
            }
            catch (e) {
                LOG.error("JsErrorChecker observe error: " + e.message);
            }
        },
        QueryInterface: function (iid) {
             if (!iid.equals(Components.interfaces.nsIConsoleListener) &&
                   !iid.equals(Components.interfaces.nsISupports)) {
                 throw Components.results.NS_ERROR_NO_INTERFACE;
             }
             return this;
        }
   };
 }

selenium.doBeginJsErrorChecker = function() {
    try {
        if (browserVersion.isChrome) {// firefox
            if (!consoleListener.installed)
            {
                var consoleService = Components.classes["@mozilla.org/consoleservice;1"].getService(Components.interfaces.nsIConsoleService);
                consoleService.registerListener(consoleListener);
                consoleService.reset();
                consoleListener.installed = true;
                LOG.info("console listener registered");
            }
            else {
                LOG.warn("console listener already registered");
            }
        } else {
            throw new Error("TODO: Non-FF browser...");
        }
    } catch (e) {
        throw new Error("doBeginJsErrorChecker() threw an exception: " + e.message);
    }
};

selenium.doEndJsErrorChecker = function() {
    try {
        if (browserVersion.isChrome) {// firefox
            if (consoleListener.installed) {
                var consoleService = Components.classes["@mozilla.org/consoleservice;1"].getService(Components.interfaces.nsIConsoleService);
                consoleService.unregisterListener(consoleListener);
                consoleService.reset();
                consoleListener.installed = false;
                LOG.info("console listener unregistered");
            }
            else {
                LOG.warn("console listener not previously registered");
            }
        } else {
            throw new SeleniumError("TODO: Non-FF browser...");
        }
    } catch (e) {
        throw new SeleniumError("doEndJsErrorChecker() threw an exception: " + e.message);
    }
};

"OK";

