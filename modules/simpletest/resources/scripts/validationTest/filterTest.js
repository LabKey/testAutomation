/*
 * Copyright (c) 2011-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");

function doTest()
{
    var errors = [];

    if (LABKEY.Filter.Types.CONTAINS === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.CONTAINS  was undefined");
    if (LABKEY.Filter.Types.DATE_EQUAL === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.DATE_EQUAL  was undefined");
    if (LABKEY.Filter.Types.DATE_NOT_EQUAL === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.DATE_NOT_EQUAL  was undefined");
    if (LABKEY.Filter.Types.DOES_NOT_CONTAIN === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.DOES_NOT_CONTAIN  was undefined");
    if (LABKEY.Filter.Types.DOES_NOT_START_WITH === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.DOES_NOT_START_WITH  was undefined");
    if (LABKEY.Filter.Types.EQUAL === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.EQUAL  was undefined");
    if (LABKEY.Filter.Types.EQUALS_ONE_OF === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.EQUALS_ONE_OF  was undefined");
    if (LABKEY.Filter.Types.GREATER_THAN === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.GREATER_THAN  was undefined");
    if (LABKEY.Filter.Types.GREATER_THAN_OR_EQUAL === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.GREATER_THAN_OR_EQUAL  was undefined");
    if (LABKEY.Filter.Types.LESS_THAN === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.LESS_THAN  was undefined");
    if (LABKEY.Filter.Types.LESS_THAN_OR_EQUAL === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.LESS_THAN_OR_EQUAL  was undefined");
    if (LABKEY.Filter.Types.MISSING === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.MISSING  was undefined");
    if (LABKEY.Filter.Types.NOT_EQUAL === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.NOT_EQUAL  was undefined");
    if (LABKEY.Filter.Types.NOT_EQUAL_OR_MISSING === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.NOT_EQUAL_OR_MISSING  was undefined");
    if (LABKEY.Filter.Types.NOT_MISSING === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.NOT_MISSING  was undefined");
    if (LABKEY.Filter.Types.STARTS_WITH === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.STARTS_WITH  was undefined");
    if (LABKEY.Filter.Types.Q === undefined)
        errors[errors.length] = new Error("LABKEY.Filter.Types.Q  was undefined");

    var url = "list/home/grid.view?listId=1&query.Key~startswith=test";
    var desc = LABKEY.Filter.getFilterDescription(url, "query", "Key");
    if (desc != "Starts With test")
        errors[errors.length] = new Error("LABKEY.Filter.getFilterDescription() = " + desc);
    
    // LABKEY.Filter.create() tested in queryTest.js

    if( errors.length > 0 )
        throw errors;
}