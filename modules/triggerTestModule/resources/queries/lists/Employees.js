/*
 * Copyright (c) 2016 LabKey Corporation
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
	console.log("list: beforeInsert: row is: " + row);
    if(row.name == "Emp 2")
    	row.company = "Inserting Single";
    else if(row.name == "Emp 5")
        row.company = "Importing TSV";
    else if(row.name == "Emp 6")
        row.company = "API BeforeInsert";
    console.log("list: edited row is: " + row);
    console.log(shared.sampleFunc("list: this is from the shared function"));
}

function beforeUpdate(row, oldRow, errors) {
	console.log("list: beforeUpdate: row is: " + row);
	if(row.name == "Emp 3" || row.name == "Emp 8" )
        row.company = "Before Update changed me";
	console.log("list: old row is: " + oldRow);
    console.log(shared.sampleFunc("list: this is from the shared function"));
}

function beforeDelete(row, errors) {
	console.log("list: beforeDelete: row is: " + row);
    if(row.company == "Inserting Single" || row.company == "DeleteMe")
        errors[null] = "This is the Before Delete Error";

    console.log(shared.sampleFunc("list: this is from the shared function"));
}

function afterInsert(row, errors) {
	console.log("list: afterInsert: row is: " + row);

    if(row.name == "Emp 1")
        errors[null] = "This is the After Insert Error";

    console.log(shared.sampleFunc("list: this is from the shared function"));
}

function afterUpdate(row, oldRow, errors) {
	console.log("list: afterUpdate: row is: " + row);

    if(row.name == "Emp 2" || row.name == "Emp 6" )
        errors[null] = "This is the After Update Error";
    //throw new Error("This is the After Update Error");

	console.log("list: old row is: " +oldRow);
    console.log(shared.sampleFunc("list: this is from the shared function"));
}

function afterDelete(row, errors) {
	console.log("list: afterDelete: row is: " + row);

    if(row.company == "Before Update changed me")
        errors[null] = "This is the After Delete Error";

    console.log(shared.sampleFunc("list: this is from the shared function"));
}

function complete(event, errors) {
	console.log("list: complete got triggered with event: " + event);
    console.log(shared.sampleFunc("list: this is from the shared function"));
}
