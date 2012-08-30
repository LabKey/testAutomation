/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
console.log('Preparing to be cool...');

function render(queryConfig, div) {
    Ext4.onReady(function() {
        console.log('ready...');
        testfile2(div, 'Less cool than expected.');
    });
    console.log('render called!');
}