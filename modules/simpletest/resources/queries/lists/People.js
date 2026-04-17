/*
 * Copyright (c) 2013-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename'] + ", schema=" + schemaName + ", table=" + tableName);
if (extraContext)
    console.log("extraContext:", extraContext);

var LABKEY = require("labkey");

// Issue 52098 - do custom parsing to validate trigger script gets a chance to do type conversion
function stripPrefix(row)
{
    if (row.Age && row.Age.toString().indexOf("RemoveMe") === 0)
    {
        row.Age = row.Age.substring("RemoveMe".length);
    }
    if (row.FavoriteDateTime && row.FavoriteDateTime.toString().indexOf("RemoveMe") === 0)
    {
        row.FavoriteDateTime = row.FavoriteDateTime.substring("RemoveMe".length);
    }
}

// Disable managed columns from a script
function managedColumns() {
    return false;
}

function beforeInsert(row, errors)
{
    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("beforeInsert row properties must be case-insensitive.");

    stripPrefix(row);

    // Test disabling managed columns from a script by placing an invalid key/value on the row.
    // If this script managed columns, then this would fail.
    row.ImNotManagedInsert = "I'm not managed insert";
}

function afterInsert(row, errors)
{
    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("afterInsert row properties must be case-insensitive.");
}

function beforeUpdate(row, oldRow, errors)
{
    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("beforeUpdate row properties must be case-insensitive.");

    // Test oldRow map is case-insensitive
    if (oldRow.Name != oldRow.nAmE)
        throw new Error("beforeUpdate oldRow properties must be case-insensitive.");

    stripPrefix(row);

    // Test disabling managed columns from a script by placing an invalid key/value on the row.
    // If this script managed columns, then this would fail.
    row.ImNotManagedUpdate = "I'm not managed update";
}

function afterUpdate(row, oldRow, errors)
{
    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("afterUpdate row properties must be case-insensitive.");

    // Test oldRow map is case-insensitive
    if (oldRow.Name != oldRow.nAmE)
        throw new Error("afterUpdate oldRow properties must be case-insensitive.");
}

function beforeDelete(row, errors)
{
    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("beforeDelete row properties must be case-insensitive.");
}

function afterDelete(row, errors)
{
    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("afterDelete row properties must be case-insensitive.");
}

var {Debug, trace} = require("simpletest/Debug");
beforeInsert = Debug.addBefore(beforeInsert, trace);
afterInsert  = Debug.addBefore(afterInsert, trace);
beforeUpdate = Debug.addBefore(beforeUpdate, trace);
afterUpdate  = Debug.addBefore(afterUpdate, trace);
beforeDelete = Debug.addBefore(beforeDelete, trace);
afterDelete  = Debug.addBefore(afterDelete, trace);

