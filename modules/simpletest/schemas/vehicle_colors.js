// ================================================

print("** evaluating: " + this['javax.script.filename']);;

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

var totalcalls = 0;
function trace(args, oldFn, thiz)
{
    var msg = oldFn.name + "(";
    for (var i = 0; i < args.length; i++)
        msg += (i > 0 ? ", " : "") + args[i];
    msg += ")";

    if (!oldFn.callcount)
        oldFn.callcount = 0;
    var count = ++oldFn.callcount;
    print(msg + " (callcount=" + count + "/" + (++totalcalls) + ")");

    // return arguments needed by oldFn
    return args;
}

// ================================================

var hexRe = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;

function init_insert(rows, errors) {
}

function before_insert(row, errors) {
    // Throwing a script exception will cancel the insert.
    if (row.Hex && !row.Hex[0] == "#")
        throw new Error("Hex color value must start with '#'");

    // Any errors added to the error map will cancel the insert
    // and show up next to the field with the error.
    if (row.Hex && !hexRe.test(row.Hex))
        errors.Hex = "Hex color value must be of the form #abc or #aabbcc";

    // Returning false will cancel the insert with a
    // generic error message for the row.
    if (row.Name == "Muave")
        return false;

    row.Name = row.Name + "!";
}

function after_insert(row, errors) {
}

function complete_insert(rows, errors) {
}

Debug.addBefore(this, 'init_insert', trace);
Debug.addBefore(this, 'before_insert', trace);
Debug.addBefore(this, 'after_insert', trace);
Debug.addBefore(this, 'complete_insert', trace);

