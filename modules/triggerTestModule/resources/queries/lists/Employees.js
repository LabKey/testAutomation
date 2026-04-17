var console = require("console");

function managedColumns() {
    return {
        insert: ["boomerang", "employeeId"],
        update: ["boomerang", "company", "employeeId"],
        ignored: ["notes"],
    };
}

function init(event, errors) {
	console.log("init got triggered with event: " + event);
}

function beforeInsert(row, errors) {
	console.log("list: beforeInsert: row is: " + row);
    if (row.name === "Emp 2")
    	row.company = "Inserting Single";
    else if (row.name === "Emp 5")
        row.company = "Importing TSV";
    else if (row.name === "Emp 6")
        row.company = "API BeforeInsert";
    else if (row.name === "Managed Insert")
        row.employeeId = "EMP-INS";
    else if (row.name === "Managed Struct") {
        row.employeeId = "EMP-STRUCT";
        row.undeclaredCol = "bad";
    }
    else if (row.name === "Managed Struct Remove") {
        row.employeeId = "EMP-STRUCT-REM";
        delete row.SSN;
    }

    if (!row.employeeId) {
        row.employeeId = "EMP-INS1";
    }

    row.notes = "This is a note";

    if (row.SSN !== "-123") {
        row.boomeRANG = "Back at ya!";
    }
    console.log("list: edited row is: " + row);
}

function beforeUpdate(row, oldRow, errors) {
	console.log("list: beforeUpdate: row is: " + row);
	if (row.company === "Company Up")
        row.company = "Before Update changed me";
    else if (row.name === "Managed Update") {
        row.company = "Managed Co";
        row.employeeId = "EMP-UPD";
    }
    else if (row.name === "Managed Struct") {
        row.company = "Struct Co";
        row.employeeId = "EMP-STRUCT";
        row.undeclaredCol = "bad";
    }
    else if (row.name === "Managed Struct Remove") {
        row.company = "Struct Remove Co";
        row.employeeId = "EMP-STRUCT-REM";
        delete row.SSN;
    }

    if (!row.employeeId) {
        row.employeeId = "EMP-UPD1";
    }

    row.notes = "This is a note";

    if (row.SSN !== "-123") {
        row.boomeRANG = "Back at me!";
    }
	console.log("list: old row is: " + oldRow);
}

function beforeDelete(row, errors) {
	console.log("list: beforeDelete: row is: " + row);
    if (row.company === "Inserting Single" || row.company === "DeleteMe")
        errors[null] = "This is the Before Delete Error";
}

function afterInsert(row, errors) {
	console.log("list: afterInsert: row is: " + row);

    if (row.name === "Emp 1")
        errors[null] = "This is the After Insert Error";
}

function afterUpdate(row, oldRow, errors) {
	console.log("list: afterUpdate: row is: " + row);

    if (row.company === "Company After Update Error")
        errors[null] = "This is the After Update Error";

	console.log("list: old row is: " +oldRow);
}

function afterDelete(row, errors) {
	console.log("list: afterDelete: row is: " + row);

    if (row.company === "Before Update changed me")
        errors[null] = "This is the After Delete Error";
}

function complete(event, errors) {
	console.log("list: complete got triggered with event: " + event);
}
