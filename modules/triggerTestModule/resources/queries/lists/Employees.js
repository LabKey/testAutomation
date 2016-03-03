var shared = require("TriggerTestModule/EmployeeLib");
var console = require("console");
   
function init(event, errors) {
	console.log("init got triggered with event: " + event);
    console.log(shared.sampleFunc("this is from the shared function"));
}

function beforeInsert(row, errors) {
	console.log("beforeInsert got triggered");
	console.log("row is: " + row);
    if(row.Name == "Emp 2")
    	row.Company = "Inserting Single";
    else if(row.Name == "Emp 5")
        row.Company = "Importing TSV";
    else if(row.Name == "Emp 6")
        row.Company = "API BeforeInsert";
    console.log("edited row is: " + row);
    console.log(shared.sampleFunc("this is from the shared function"));
}

function beforeUpdate(row, oldRow, errors) {
	console.log("beforeUpdate got triggered");
	console.log("row is: " + row);
	if(row.Name == "Emp 3" || row.name == "Emp 8" )
        row.Company = "Before Update changed me";
	console.log("old row is: " + oldRow);
    console.log(shared.sampleFunc("this is from the shared function"));
}

function beforeDelete(row, errors) {
	console.log("beforeDelete got triggered");
	console.log("row is: " + row);
    if(row.Company == "Inserting Single" || row.Company == "DeleteMe")
        errors[null] = "This is the Before Delete Error";

    console.log(shared.sampleFunc("this is from the shared function"));
}

function afterInsert(row, errors) {
	console.log("afterInsert got triggered");
	console.log("row is: " + row);

    if(row.name == "Emp 1")
        errors[null] = "This is the After Insert Error";

    console.log(shared.sampleFunc("this is from the shared function"));
}

function afterUpdate(row, oldRow, errors) {
	console.log("afterUpdate got triggered");
	console.log("row is: " + row);

    if(row.name == "Emp 2" || row.name == "Emp 6" )
        errors[null] = "This is the After Update Error";
    //throw new Error("This is the After Update Error");

	console.log("old row is: " +oldRow);
    console.log(shared.sampleFunc("this is from the shared function"));
}

function afterDelete(row, errors) {
	console.log("afterDelete got triggered");
	console.log("row is: " + row);

    if(row.Company == "Before Update changed me")
        errors[null] = "This is the After Delete Error";
    //throw new Error("This is the After Delete Error");

    console.log(shared.sampleFunc("this is from the shared function"));
}


function complete(event, errors) {
	console.log("complete got triggered with event: " + event);
    console.log(shared.sampleFunc("this is from the shared function"));
}
