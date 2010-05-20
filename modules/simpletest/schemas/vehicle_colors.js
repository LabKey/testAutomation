/*
 * Copyright (c) 2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// ================================================

print("** evaluating: " + this['javax.script.filename']);

var Debug = {
    addBefore : function (obj, fname, before) {
        var oldFn = obj[fname];
        var wrappedFn = function () {
            return oldFn.apply(scope, before(arguments, oldFn, this));
        }
        wrappedFn.oldFn = oldFn;
        obj[fname] = wrappedFn;
    },

    restore : function (obj, fname) {
        obj[fname] = obj[fname].oldFn;
    }
};

function trace(args, oldFn, thiz)
{
    var msg = oldFn.name + "(";
    for (var i = 0; i < args.length; i++)
        msg += (i > 0 ? ", " : "") + args[i];
    msg += ")";

    // return arguments needed by oldFn
    return args;
}

// ================================================

var hexRe = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;

var rows = [];

// called once before insert/update/delete
function init(event, errors) {
}

function beforeInsert(row, errors) {
    // Throwing a script exception will bubble all the way up and cancel the insert immediately.
    if (row.Hex && row.Hex[0] != "#")
        throw new Error("color value must start with '#'");

    // Any errors added to the error map will cancel the insert
    // and appear in the html or Ext form next to the field with the error.
    if (row.Hex && !hexRe.test(row.Hex))
        errors.Hex = "color value must be of the form #abc or #aabbcc";

    // Field error value as a single string.
    if (row.Name == "TestFieldErrorMessage")
        errors.Hex = "single message";

    // Field error value as an array of error messages.
    if (row.Name == "TestFieldErrorArray")
    {
        errors.Name = [ "one error message"];
        errors.Name.push("two error message!");
        errors.Name.push("ha ha ha!");
        errors.Hex = "also an error here";
    }

    // Adding a generic error not associated with a field
    if (row.Name == "TestRowError")
        errors[null] = "boring error message";

    // Returning false will cancel the insert with a
    // generic error message for the row.
    if (row.Name == "TestReturnFalse")
        return false;

    if (row.Name == "TestErrorInComplete")
        errors.Hex = "TestErrorInComplete error one";

    // Values can be transformed during insert and update
    row.Name = row.Name + "!";
    rows.push(row);
}

function beforeUpdate(row, oldRow, errors) {
    // Woah, scary! Even the pk 'Name' property can be changed during update.
    if (row.Name[row.Name.length - 1] == "!")
        row.Name = row.Name.substring(0, row.Name.length-1) + "?";

    if (oldRow.Hex != row.Hex)
        errors.Hex = "once set, cannot be changed";
}

function afterUpdate(row, oldRow, errors) {
    if (row.Name[row.Name.length - 1] != "?")
        throw new Error("Expected color name to end in '?'");
}

// called once after insert/update/delete
function complete(event, errors) {
    if (event == "insert") {

        for (var i in errors) {
            var error = errors[i];
            if (error.Hex == "TestErrorInComplete error one")
                error.Hex = [ "TestErrorInComplete error one", "TestErrorInComplete error two" ];
        }

        for (var i in rows) {
            var row = rows[i];

            // The 'errors' object for init/complete is an array of maps
            if (row.Name == "TestErrorInComplete!")
                errors.push({ Name : "TestErrorInComplete error three!", _rowNumber: i });

            if (row.Name == "TestFieldErrorArrayInComplete!")
                errors.push({Name: ["one error message", "two error message"], _rowNumber: i});

            // Returning false will cancel the insert with
            // a generic error message for the entire set of rows.
            if (row.Name == "Zoro!")
                return false;

        }
    }
}

Debug.addBefore(this, 'init', trace);
Debug.addBefore(this, 'beforeInsert', trace);
Debug.addBefore(this, 'beforeUpdate', trace);
Debug.addBefore(this, 'complete', trace);

