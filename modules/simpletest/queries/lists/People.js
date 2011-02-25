/*
 * Copyright (c) 2010-2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);

function beforeInsert(row, errors)
{
    console.log("beforeInsert: " + row);

    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("beforeInsert row properties must be case-insensitive.");
}

function afterInsert(row, errors)
{
    console.log("afterInsert: " + row);

    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("afterInsert row properties must be case-insensitive.");
}

function beforeUpdate(row, oldRow, errors)
{
    console.log("beforeUpdate: " + row);

    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("beforeUpdate row properties must be case-insensitive.");

    // Test oldRow map is case-insensitive
    if (oldRow.Name != oldRow.nAmE)
        throw new Error("beforeUpdate oldRow properties must be case-insensitive.");
}

function afterUpdate(row, oldRow, errors)
{
    console.log("afterUpdate: " + row);

    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("afterUpdate row properties must be case-insensitive.");

    // Test oldRow map is case-insensitive
    if (oldRow.Name != oldRow.nAmE)
        throw new Error("afterUpdate oldRow properties must be case-insensitive.");
}

function beforeDelete(row, errors)
{
    console.log("beforeDelete: " + row);

    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("beforeDelete row properties must be case-insensitive.");
}

function afterDelete(row, errors)
{
    console.log("afterDelete: " + row);

    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("afterDelete row properties must be case-insensitive.");
}

var {Debug, trace} = require("simpletest/Debug");
init         = Debug.addBefore(init, trace);
beforeInsert = Debug.addBefore(beforeInsert, trace);
afterInsert  = Debug.addBefore(afterInsert, trace);
beforeUpdate = Debug.addBefore(beforeUpdate, trace);
afterUpdate  = Debug.addBefore(afterUpdate, trace);
beforeDelete = Debug.addBefore(beforeDelete, trace);
afterDelete  = Debug.addBefore(afterDelete, trace);

