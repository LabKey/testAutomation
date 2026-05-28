/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var console = require("console");

function managedColumns() {
    return false;
}

function beforeInsert(row, errors) {
    console.log("list: beforeInsert: row is: " + row);

    // Flower is not marked as a managed column, but managed columns are disabled
    if (row.type === 'A') {
        row.flower = 'Rose';
    }
}

function beforeUpdate(row, oldRow, errors) {
    console.log("list: beforeUpdate: row is: " + row);

    if (row.type === 'A') {
        row.hemisphere = 'Northern';
    }
}