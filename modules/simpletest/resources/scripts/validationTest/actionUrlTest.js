/*
 * Copyright (c) 2013-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");
var Ext = require("Ext").Ext;

function doTest()
{
    var errors = [];

    var contextPath = LABKEY.contextPath;
    var controller = "wiki";
    var action = "editWiki";
    var containerPath = "Shared/_junit";
    var urlParameters = {x:'fred', y: 'barney'};

    if (contextPath.length > 0) {
        var baseUrl = LABKEY.ActionURL.getBaseURL();
        if (baseUrl.indexOf("labkey/") != baseUrl.length - 7)
            errors[errors.length] = new Error("ActionURL.getBaseURL() = " + baseUrl);
    }

    var queryString = LABKEY.ActionURL.queryString(urlParameters);
    if( queryString != 'x=fred&y=barney' )
        errors[errors.length] = new Error("ActionURL.queryString = " + queryString);

    var url = LABKEY.ActionURL.buildURL(controller, action, containerPath, urlParameters);
    if( url != contextPath + "/" + controller + "/" + containerPath + "/" + action + ".view?" + queryString  &&
        url != contextPath + "/" + containerPath + "/" + controller + "-" + action + ".view?" + queryString)
        errors[errors.length] = new Error("ActionURL.buildUrl() = " + url);

    var parameters = LABKEY.ActionURL.getParameters(url);
    if( Ext.util.JSON.encode(parameters) != Ext.util.JSON.encode(urlParameters) )
        errors[errors.length] = new Error("ActionURL.getParameters() = " + Ext.util.JSON.encode(parameters));

    if( errors.length > 0 )
        throw errors;
}
