/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
print('hello Nashorn timeout world!');
print("arguments.length: " + arguments.length);
var timeout = 10;
for (var i = 0; i < arguments.length; i++) {
    print("  arg[" + i  + "]=" + arguments[i]);
    if (arguments[i] === '-t') {
        timeout = parseInt(arguments[i+1]);
    }
}
print('sleeping for ' + timeout + ' seconds');
java.lang.Thread.sleep(1000 * timeout);
print('goodbye Nashorn timeout world!');
