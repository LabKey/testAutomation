/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
    var i = 0;
    if (doc.getElementById('headerSearchForm'))
        i++;
    var form = doc.forms[i];
    form[fieldName].value = form[fieldName].value + txt;
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
    if (cmp)
    {
        var el = cmp.getEl();
        if (el)
            return el.id;
    }
    return null;
};

selenium.selectFileBrowserCheckbox = function (filename) {
    selenium.selectExtGridItem('name', filename, -1, 'labkey-filecontent-grid', true);
};

selenium.selectExtGridItem = function (columnName, columnVal, idx, markerCls, keepExisting) {
    // find the grid view ext element
    var domQuery = selenium.browserbot.getCurrentWindow().Ext.DomQuery;
    var ext = selenium.browserbot.getCurrentWindow().Ext;

    var el = domQuery.selectNode("div[class*='"+markerCls+"']");
    if (el)
    {
        var grid = ext.getCmp(el.id);
        if (grid)
        {
            if (idx == -2)
            {
                grid.getSelectionModel().selectAll(false);
                return;
            }

            if (idx == -1)
                idx = grid.getStore().find(columnName, columnVal);

            if (idx < grid.getStore().getCount())
            {
                if (idx >= 0)
                {
                    grid.getSelectionModel().selectRow(idx, keepExisting);
                }
                else
                {
                    throw new Error("Unable to locate " + columnName + ": " + columnVal);
                }
            }
            else
            {
                throw new Error("No such row: " + idx);
            }
        }
    }
    else
    {
        throw new Error("Unable to locate grid panel: " + markerCls)
    }
};

selenium.selectExt4GridItem = function (columnName, columnVal, idx, markerCls, keepExisting) {
    // find the grid view ext element
    var domQuery = selenium.browserbot.getCurrentWindow().Ext4.DomQuery;
    var ext = selenium.browserbot.getCurrentWindow().Ext4;

    var el = domQuery.selectNode("div[class*='"+markerCls+"']");
    if (el)
    {
        var grid = ext.getCmp(el.id);
        if (grid)
        {
            if (idx == -1)
                idx = grid.getStore().find(columnName, columnVal);

            if (idx == -1)
                throw new Error("Unable to locate " + columnName + ": " + columnVal);

            if (idx >= grid.getStore().getCount())
                throw new Error("No such row: " + idx);

            grid.getSelectionModel().select(idx, keepExisting);
        }
    }
    else
    {
        throw new Error("Unable to locate grid: " + markerCls)
    }
};

selenium.ext4ComponentQuery = function (selector, parentId) {
    var ext = selenium.browserbot.getCurrentWindow().Ext4;
    var res =  null;
    if (parentId)
        res = ext.getCmp(parentId).query(selector);
    else
        res = ext.ComponentQuery.query(selector);

    return null == res ? null : ext.JSON.encode(ext.Array.pluck(res, "id"));
};

selenium.ext4Down = function (cmpId, selector) {
    var ext = selenium.browserbot.getCurrentWindow().Ext4;
    var cmp = ext.getCmp(cmpId);
    if (null == cmp)
        return null;
    var res = cmp.down(selector);

    return null == res ? null : res.id;
};

selenium.ext4DomQuerySelect = function (root, selector) {
    var ext = selenium.browserbot.getCurrentWindow().Ext4;
    var res = ext.DomQuery.select(root == null ? null : ext.getDom(root), selector);

    return null == res ? null : ext.JSON.encode(ext.Array.pluck(res, "id"));
};

selenium.ext4ComponentEval = function(cmpId, expr) {
    var ext = selenium.browserbot.getCurrentWindow().Ext4;
    var fn = new Function("return " + expr + ";");
    return fn.call(ext.getCmp(cmpId));
};

// Example: _extHelper.selectFolderManagementTreeItem(this, "/home/545dcbbc9f7fa0f85a86190b9acd6381/14/15/3/2", true);
selenium.selectFolderManagementItem = function(path, keepExisting) {
    selenium.selectExtFolderTreeNode(path, 'folder-management-tree', keepExisting);
};

selenium.selectExtFolderTreeNode = function(containerPath, markerCls, keepExisting) {
    var domQuery = selenium.browserbot.getCurrentWindow().Ext.DomQuery;
    var ext = selenium.browserbot.getCurrentWindow().Ext;

    // Get Path Array
    var pathArray = containerPath.split("/");
    if (pathArray.length == 0)
        throw new Error("Unable to parse path: " + containerPath);

    // Remove invalid paths due to parsing
    if (pathArray[0] == "")
        pathArray = pathArray.slice(1);
    if (pathArray[pathArray.length-1] == "")
        pathArray = pathArray.slice(0, pathArray.length-1);
    
    var el = domQuery.selectNode("div[class*='"+markerCls+"']");
    if (el) {
        var tree = ext.getCmp(el.id);
        if (tree) {

            var root = tree.getRootNode();
            if (!root) {
                throw new Error("Unable to find root node.");
            }

            var _path = "";
            var node;

            for (var i=0; i < pathArray.length; i++) {
                _path += '/' + pathArray[i];
                node = root.findChild('containerPath', _path, true);
                if (node) {
                    if (i==(pathArray.length-1)) {
                        var e = {};
                        if (keepExisting) {
                            e.ctrlKey = true;
                        }
                        tree.getSelectionModel().select(node, e, keepExisting);
                    }
                }
                else {
                    throw new Error("Unable to find node: " + _path);
                }
            }
        }
        else {
            throw new Error(el.id + " does not appear to be a valid Ext Component.");
        }
    }
    else {
        throw new Error("Unable to locate tree panel: " + markerCls);
    }
};

selenium.getContainerId = function () {
    var win = selenium.browserbot.getCurrentWindow();
    return win.LABKEY.container.id;
};

selenium.getExtElementHeight = function(className, index) {
    var ext = selenium.browserbot.getCurrentWindow().Ext4;
    return ext.get(ext.query('.' + className)[index]).getHeight();
};

// firefox error console listener
// http://sejq.blogspot.com/2008/12/can-selenium-detect-if-page-has.html
// https://developer.mozilla.org/en/Console_service
if (browserVersion.isFirefox) {
    var consoleListener = {
        installed: false,
        paused: false,
        observe: function( msg ) {
//            LOG.info("JsErrorChecker: " + msg.message);
            if (this.paused) return;
            try {
                dump("Log : " + msg.message);
                if (msg.message != null &&
                    msg.message.indexOf("[JavaScript Error:") == 0 &&
                    msg.message.indexOf("setting a property that has only a getter") == -1 &&
                    msg.message.indexOf("{file: \"chrome://") == -1 &&
                    msg.message.indexOf("ext-all-sandbox-debug.js") == -1 && // Ignore error caused by project webpart
                    msg.message.indexOf("ext-all-sandbox.js") == -1 && // Ignore error that's junking up the weekly
                    msg.message.indexOf("ext-all-sandbox-dev.js") == -1 && // Ignore error that's junking up the weekly
                    msg.message.indexOf("XULElement.selectedIndex") == -1 && // Ignore known Firefox Issue
                    msg.message.indexOf("Failed to decode base64 string!") == -1 && // Firefox issue
                    msg.message.indexOf("xulrunner-1.9.0.14/components/FeedProcessor.js") == -1 && // Firefox problem
                    msg.message.indexOf("{file: \"resource://") == -1 && // Firefox problem
                    msg.message.indexOf("Image corrupt or truncated: <unknown>") == -1)  // Selenium problem with pages that lack a favicon (e.g., errors since reset)
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
        }
   };
 }

selenium.doBeginJsErrorChecker = function() {
    try {
        if (browserVersion.isFirefox) {// firefox
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
            throw new SeleniumError("TODO: Non-FF browser...");
        }
    } catch (e) {
        throw new SeleniumError("doBeginJsErrorChecker() threw an exception: " + e.message);
    }
};

selenium.doEndJsErrorChecker = function() {
    try {
        if (browserVersion.isFirefox) {// firefox
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

selenium.pauseJsErrorChecker = function() {
    consoleListener.paused = true;
};

selenium.resumeJsErrorChecker = function() {
    consoleListener.paused = false;
};

selenium.setCodeMirrorValue = function(id, value) {
    try {
        var win = selenium.browserbot.getCurrentWindow();
        if (win.LABKEY.CodeMirror && win.LABKEY.CodeMirror[id]) {
            var eal = win.LABKEY.CodeMirror[id];
            eal.setValue(value);
        }
        else {
            throw new SeleniumError("Unable to find editCodeMirror.");
        }
    } catch (e) {
        throw new SeleniumError("setCodeMirrorValue() threw an exception: " + e.message);
    }
};

selenium.setEditAreaValue = function(id, value) {
    try {
        var win = selenium.browserbot.getCurrentWindow();
        if (win.editAreaLoader) {
            var eal = win.editAreaLoader;
            eal.setValue(id, value);
        }
        else {
            throw new SeleniumError("Unable to find editAreaLoader.");
        }
    } catch (e) {
        throw new SeleniumError("setEditAreaValue() threw an exception: " + e.message);
    }
};

"OK";
