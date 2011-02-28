/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var console = require("console");

var Debug = {
    addBefore : function (oldFn, before) {
        return function () {
            var me = this,
                args = arguments;
            return oldFn.apply(me, before(args, oldFn, me));
        };
    }
};

function trace(args, oldFn, thiz)
{
    var msg = oldFn.name + "(";
    for (var i = 0; i < args.length; i++)
        msg += (i > 0 ? ", " : "") + args[i];
    msg += ")";
    console.log("** trace: " + msg);

    // return arguments needed by oldFn
    return args;
}

exports.Debug = Debug;
exports.trace = trace;
