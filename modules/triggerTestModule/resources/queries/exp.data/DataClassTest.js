/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var shared = require("TriggerTestModule/EmployeeLib");
var console = require("console");

function init(event, errors) {
    console.log("init got triggered with event: " + event);
    console.log(shared.sampleFunc("this is from the shared function"));
}

function beforeInsert(row, errors) {
    console.log("exp.data: beforeInsert: row is: " + row);
    if(row.Comments) {
        if (row.Comments == "Individual Test") {
            row.Country = "Inserting Single";
            row.Comments = "BeforeDelete"; //set this for next test step
        }
        else if (row.Comments == "Import Test")
            row.Country = "Importing TSV";
        else if (row.Comments == "API Test")
            row.Country = "API BeforeInsert";
    } else {
        if (row.comments == "Individual Test") {
            row.country = "Inserting Single";
            row.comments = "BeforeDelete"; //set this for next test step
        }
        else if (row.comments == "Import Test")
            row.country = "Importing TSV";
        else if (row.comments == "API Test")
            row.country = "API BeforeInsert";
    }

    console.log("exp.data: edited row is: " + row);
    console.log(shared.sampleFunc("exp.data: this is from the shared function"));
}

function beforeUpdate(row, oldRow, errors) {
    console.log("exp.data: beforeUpdate: row is: " + row);
    if(row.Comments == "BeforeUpdate")
        row.Country = "Before Update changed me";
    else if(row.comments == "BeforeUpdate")
        row.country = "Before Update changed me";
    console.log("exp.data: old row is: " + oldRow);
    console.log(shared.sampleFunc("exp.data: this is from the shared function"));
}

function beforeDelete(row, errors) {
    console.log("exp.data: beforeDelete: row is: " + row);
    if(row.Comments == "BeforeDelete" || row.comments == "BeforeDelete")
        errors[null] = "This is the Before Delete Error";

    console.log(shared.sampleFunc("exp.data: this is from the shared function"));
}

function afterInsert(row, errors) {
    console.log("exp.data: afterInsert: row is: " + row);

    if(row.Comments == "AfterInsert" || row.comments == "AfterInsert")
        errors[null] = "This is the After Insert Error";

    console.log(shared.sampleFunc("exp.data: this is from the shared function"));
}

function afterUpdate(row, oldRow, errors) {
    console.log("exp.data: afterUpdate: row is: " + row);

    if(row.Comments == "AfterUpdate" || row.comments == "AfterUpdate")
        errors[null] = "This is the After Update Error";

    console.log("exp.data: old row is: " +oldRow);
    console.log(shared.sampleFunc("exp.data: this is from the shared function"));
}

function afterDelete(row, errors) {
    console.log("exp.data: afterDelete: row is: " + row);

    if(row.Country == "Before Update changed me" || row.country == "Before Update changed me")
        errors[null] = "This is the After Delete Error";

    console.log(shared.sampleFunc("exp.data: this is from the shared function"));
}

function complete(event, errors) {
    console.log("exp.data: complete got triggered with event: " + event);
    console.log(shared.sampleFunc("exp.data: this is from the shared function"));
}