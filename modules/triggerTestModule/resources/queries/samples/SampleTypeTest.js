/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var shared = require("TriggerTestModule/EmployeeLib");
var console = require("console");

function managedColumns() {
    return {
        insert: ["Country"],
        update: ["Country"],
    };
}

function init(event, errors) {
    console.log("init got triggered with event: " + event);
    console.log(shared.sampleFunc("this is from the shared function"));
}

function beforeInsert(row, errors) {
    console.log("samples: beforeInsert: row is: " + row);
    if (row.name === "Managed Insert") {
        row.Country = "MANAGED-INS";
    } else if (row.name === "Managed Struct") {
        row.Country = "MANAGED-STRUCT";
        row.undeclaredCol = "bad";
    } else if (row.name === "Managed Struct Remove") {
        row.Country = "MANAGED-STRUCT-REM";
        delete row.Comments;
    }

    if (row.name === "Managed Unhandled") {
        delete row.Country;
    } else {
        if (row.Comments) {
            if (row.Comments === "Individual Test") {
                row.Country = "Inserting Single";
                row.Comments = "BeforeDelete";
            }
            else if (row.Comments === "Import Test")
                row.Country = "Importing TSV";
            else if (row.Comments === "API Test")
                row.Country = "API BeforeInsert";
        } else {
            if (row.Comments === "Individual Test") {
                row.country = "Inserting Single";
                row.Comments = "BeforeDelete";
            }
            else if (row.Comments === "Import Test")
                row.country = "Importing TSV";
            else if (row.Comments === "API Test")
                row.country = "API BeforeInsert";
        }
        if (!row.Country)
            row.Country = "ST-DEFAULT";
    }
    console.log("samples: edited row is: " + row);
    console.log(shared.sampleFunc("samples: this is from the shared function"));
}

function beforeUpdate(row, oldRow, errors) {
    console.log("samples: beforeUpdate: row is: " + row);
    if (row.Comments === "Managed Update") {
        row.Country = "MANAGED-UPD";
    } else if (row.Comments === "Managed Struct") {
        row.Country = "MANAGED-STRUCT";
        row.undeclaredCol = "bad";
    } else if (row.Comments === "Managed Struct Remove") {
        row.Country = "MANAGED-STRUCT-REM";
        delete row.Comments;
    }

    if (row.Comments === "Managed Unhandled") {
        delete row.Country;
    } else {
        if (row.Comments === "BeforeUpdate")
            row.Country = "Before Update changed me";
        else if (row.Comments === "BeforeUpdate")
            row.country = "Before Update changed me";
        if (!row.Country) {
            row.Country = "ST-DEFAULT-UPD";
        }
    }

    console.log("samples: old row is: " + oldRow);
    console.log(shared.sampleFunc("samples: this is from the shared function"));
}

function beforeDelete(row, errors) {
    console.log("samples: beforeDelete: row is: " + row);
    if (row.Comments === "BeforeDelete")
        errors[null] = "This is the Before Delete Error";

    console.log(shared.sampleFunc("samples: this is from the shared function"));
}

function afterInsert(row, errors) {
    console.log("samples: afterInsert: row is: " + row);

    if (row.Comments === "AfterInsert")
        errors[null] = "This is the After Insert Error";

    console.log(shared.sampleFunc("samples: this is from the shared function"));
}

function afterUpdate(row, oldRow, errors) {
    console.log("samples: afterUpdate: row is: " + row);

    if (row.Comments === "AfterUpdate")
        errors[null] = "This is the After Update Error";

    console.log("samples: old row is: " +oldRow);
    console.log(shared.sampleFunc("samples: this is from the shared function"));
}

function afterDelete(row, errors) {
    console.log("samples: afterDelete: row is: " + row);

    if (row.country === "Before Update changed me")
        errors[null] = "This is the After Delete Error";

    console.log(shared.sampleFunc("samples: this is from the shared function"));
}

function complete(event, errors) {
    console.log("samples: complete got triggered with event: " + event);
    console.log(shared.sampleFunc("samples: this is from the shared function"));
}