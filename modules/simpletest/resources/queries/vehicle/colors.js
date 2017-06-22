/*
 * Copyright (c) 2010-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// ================================================

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);

// ================================================

var hexRe = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;

var rows = [];

// called once before insert/update/delete
function init(event, errors) {
}

function beforeInsert(row, errors) {
    // Test row map is case-insensitive
    if (row.Name != row.nAmE && row.Hex != row.hex)
        throw new Error("row properties must be case-insensitive.");

    // Throwing a script exception will bubble all the way up and cancel the insert immediately.
    if (row.Hex && row.Hex[0] != "#") {
        errors[null] = "color value must start with '#'";
        return;
    }

    // Any errors added to the error map will cancel the insert
    // and appear in the html or Ext form next to the field with the error.
    if (row.Hex && !hexRe.test(row.Hex))
        errors.Hex = "color value must be of the form #abc or #aabbcc";

    // Field error value as a single string.
    if (row.Name == "TestFieldErrorMessage")
    {
        errors.Hex = "**expect to overwrite this**";
        errors.Hex = "single message";
    }

    // Field error value as an array of error messages.
    if (row.Name == "TestFieldErrorArray")
    {
        errors.Name = "**expect to overwrite this**";
        errors.Name = [ "one error message"];
        errors.Name.push("two error message!");
        errors.Name.push("ha ha ha!");
        errors.Hex = "also an error here";
    }

    // Adding a generic error not associated with a field
    if (row.Name == "TestRowError")
    {
        errors[null] = "boring error message";
        // Test setting a schema/query/row number
        errors._rowNumber = 1000;
        errors._schemaName = "blarg";
        errors._queryName = "zorg";
    }

    // Returning false will cancel the insert with a
    // generic error message for the row.
    if (row.Name == "TestReturnFalse")
        return false;

    if (row.Name == "TestErrorInComplete")
        errorsInComplete = true;

    // Values can be transformed during insert and update
    row.Name = row.Name + "!";
    rows.push(row);

    // this is a field set to userEditable=false, but has a value provided by the trigger script
    row.TriggerScriptProperty = 'set';
}

function afterInsert(row, errors) {
    // Test row map is case-insensitive
    if (row.Name != row.nAmE && row.Hex != row.hex)
        throw new Error("afterInsert row properties must be case-insensitive.");
}

function beforeUpdate(row, oldRow, errors) {
    // Test row map is case-insensitive
    if (row.Name != row.nAmE && row.Hex != row.hex)
        throw new Error("beforeUpdate row properties must be case-insensitive.");

    // Test oldRow map is case-insensitive
    if (oldRow.Name != oldRow.nAmE && oldRow.Hex != oldRow.hex)
        throw new Error("beforeUpdate oldRow properties must be case-insensitive.");

    // Adding a generic error not associated with a field
    if (row.Name == "TestRowErrorBeforeUpdate!")
        errors[null] = "boring error message beforeUpdate";

    // Woah, scary! Even the pk 'Name' property can be changed during update.
    if (row.Name[row.Name.length - 1] == "!")
        row.Name = row.Name.substring(0, row.Name.length-1) + "?";

    if (oldRow.Hex != row.Hex)
        errors.Hex = "once set, cannot be changed";
}

function afterUpdate(row, oldRow, errors) {
    // Test row map is case-insensitive
    if (row.Name != row.nAmE && row.Hex != row.hex)
        throw new Error("afterUpdate row properties must be case-insensitive.");

    // Test oldRow map is case-insensitive
    if (oldRow.Name != oldRow.nAmE && oldRow.Hex != oldRow.hex)
        throw new Error("afterUpdate oldRow properties must be case-insensitive.");

    if (row.Name[row.Name.length - 1] != "?")
        errors[null] = "Expected color name to end in '?'";
}

function beforeDelete(row, errors) {
    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("beforeDelete row properties must be case-insensitive.");

    // Adding a generic error not associated with a field
    if (row.Name == "TestRowErrorBeforeDelete!")
        errors[null] = "boring error message beforeDelete";
}

function afterDelete(row, errors) {
    // Test row map is case-insensitive
    if (row.Name != row.nAmE)
        throw new Error("afterDelete row properties must be case-insensitive.");
}

// called once after insert/update/delete
function complete(event, errors) {
    if (event == "insert") {

        for (var i in rows) {
            var row = rows[i];

            // The 'errors' object for init/complete is an array of maps
            if (row.Name == "TestErrorInComplete!")
                errors.push({ Name : "TestErrorInComplete error global four!", _rowNumber: i, _row: {a: "A", b: "B"} });

            if (row.Name == "TestFieldErrorArrayInComplete!")
            {
                errors.push({Name: ["one error message", "two error message"], Hex: ["three error message"], _rowNumber: i});
                errors.push({Hex: "four error message", _rowNumber: i});
            }

            // Returning false will cancel the insert with
            // a generic error message for the entire set of rows.
            if (row.Name == "Zoro!")
                return false;

        }
    }
}

var {Debug, trace} = require("simpletest/Debug");
init         = Debug.addBefore(init, trace);
beforeInsert = Debug.addBefore(beforeInsert, trace);
afterInsert  = Debug.addBefore(afterInsert, trace);
beforeUpdate = Debug.addBefore(beforeUpdate, trace);
afterUpdate  = Debug.addBefore(afterUpdate, trace);
beforeDelete = Debug.addBefore(beforeDelete, trace);
afterDelete  = Debug.addBefore(afterDelete, trace);
complete     = Debug.addBefore(complete, trace);

