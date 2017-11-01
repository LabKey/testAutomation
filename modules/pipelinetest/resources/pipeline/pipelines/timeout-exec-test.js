/*
 * Copyright (c) 2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
console.log('hello node timeout world!');
setTimeout(function () {
    console.log('goodbye node timeout world!');
}, 10*1000);
