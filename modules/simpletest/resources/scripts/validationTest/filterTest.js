/*
 * Copyright (c) 2013-2019 LabKey Corporation
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

    {
        var url = "list/home/grid.view?listId=1&query.Key~startswith=test";
        var desc = LABKEY.Filter.getFilterDescription(url, "query", "Key");
        if (desc != "Starts With test")
            errors[errors.length] = new Error("LABKEY.Filter.getFilterDescription() = " + desc);
    }

    {
        var url = '/some.view?q.RowId~eq=3&q.Name~in=a;b&q.Score~neq=5&q.Score~neq=6';
        var filters = LABKEY.Filter.getFiltersFromUrl(url, 'q');
        if (filters.length !== 4)
            errors.push(new Error("Expected to parse 4 filters from URL, got = " + filters.length));

        if (filters[0].getColumnName() !== 'RowId')
            errors.push(new Error("Expected filter[0].getColumnName() == 'RowId', got = " + filters[0].getColumnName()));
        if (filters[0].getURLParameterName('q') !== 'q.RowId~eq')
            errors.push(new Error("Expected filter[0].getURLParameterName('q') == 'q.RowId~eq', got = " + filters[0].getURLParameterName()));
        if (filters[0].getValue() !== '3')
            errors.push(new Error("Expected filter[0].getValue() == 3, got = " + filters[0].getValue()));

        if (filters[1].getColumnName() !== 'Name')
            errors.push(new Error("Expected filter[1].getColumnName() == 'Name', got = " + filters[1].getColumnName()));
        if (filters[1].getURLParameterName('q') !== 'q.Name~in')
            errors.push(new Error("Expected filter[1].getURLParameterName('q') == 'q.Name~in', got = " + filters[1].getURLParameterName()));
        if (filters[1].getValue().length !== 2 && filters[1].getValue().join('|') !== 'a|b')
            errors.push(new Error("Expected filter[1].getValue() == [a,b], got = " + filters[1].getValue()));

        if (filters[2].getColumnName() !== 'Score')
            errors.push(new Error("Expected filter[2].getColumnName() == 'Score', got = " + filters[2].getColumnName()));
        if (filters[2].getURLParameterName('q') !== 'q.Score~neq')
            errors.push(new Error("Expected filter[2].getURLParameterName('q') == 'q.Score~neq', got = " + filters[2].getURLParameterName()));
        if (filters[2].getValue() !== '5')
            errors.push(new Error("Expected filter[2].getValue() == 3, got = " + filters[2].getValue()));
    }


    // create multi-valued filters
    {
        console.log("single string value should");
        var f = LABKEY.Filter.create('q', 'abc', LABKEY.Filter.Types.IN);
        var value = f.getValue();
        if (value.length !== 1 && value[0] !== 'abc')
            errors.push(new Error("Expected a single value array for IN, got = " + value));
        if (f.getURLParameterValue() !== 'abc')
            errors.push(new Error("Expected a 'abc' URL parameter for IN, got = " + f.getURLParameterValue()));

        console.log("semi-colon separated values");
        var f = LABKEY.Filter.create('q', 'a;b;c', LABKEY.Filter.Types.IN);
        var value = f.getValue();
        if (value.length !== 3 && value.join('|') !== 'a|b|c')
            errors.push(new Error("Expected array of [a,c,b] for IN, got = " + value));
        if (f.getURLParameterValue() !== 'a;b;c')
            errors.push(new Error("Expected a 'a;b;c' URL parameter for IN, got = " + f.getURLParameterValue()));

        console.log("semi-colon separated values");
        var f = LABKEY.Filter.create('q', ['a', 'b', 'c'], LABKEY.Filter.Types.IN);
        var value = f.getValue();
        if (value.length !== 3 && value.join('|') !== 'a|b|c')
            errors.push(new Error("Expected array of [a,c,b] for IN, got = " + value));
        if (f.getURLParameterValue() !== 'a;b;c')
            errors.push(new Error("Expected a 'a;b;c' URL parameter for IN, got = " + f.getURLParameterValue()));

        console.log("json encoded array of values");
        var f = LABKEY.Filter.create('q', '{json:["a", "b", "c"]}', LABKEY.Filter.Types.IN);
        var value = f.getValue();
        if (value.length !== 3 && value.join('|') !== 'a|b|c')
            errors.push(new Error("Expected array of [a,c,b] for IN, got = " + value));
        if (f.getURLParameterValue() !== 'a;b;c')
            errors.push(new Error("Expected a 'a;b;c' URL parameter for IN, got = " + f.getURLParameterValue()));

        console.log("json encoded array of values with semi-colon");
        var f = LABKEY.Filter.create('q', '{json:["a", "b;c"]}', LABKEY.Filter.Types.IN);
        var value = f.getValue();
        if (value.length !== 3 && value.join('|') !== 'a|b;c')
            errors.push(new Error("Expected array of [a,c,b] for IN, got = " + value));
        if (f.getURLParameterValue() !== '{json:["a","b;c"]}')
            errors.push(new Error("Expected a '{json:[\"a\",\"b;c\"]}' URL parameter for IN, got = " + f.getURLParameterValue()));
    }

    // LABKEY.Filter.create() tested in queryTest.js

    if( errors.length > 0 )
        throw errors;
}