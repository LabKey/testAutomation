var sampleVar = "value";
function sampleFunc(arg)
{
    return arg;
}

var hiddenVar = "hidden";
function hiddenFunc(arg)
{
    throw new Error("Function shouldn't be exposed");
}

exports.sampleFunc = sampleFunc;
exports.sampleVar = sampleVar;
