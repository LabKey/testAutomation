var shared = require("TriggerTestModule/EmployeeLib");
var console = require("console");
   
function init(event, errors) {
	console.log("init got triggered with event: " + event);
    console.log(shared.sampleFunc("this is from the shared function"));
}

function beforeInsert(row, errors) {
	console.log("beforeInsert got triggered");
	console.log("row is: " + row);
	if(row.Comments == "Individual Test ") {
		row.Country = "Inserting Single";
		row.Comments = "BeforeDelete"; //set this for next test step
	}
	else if(row.Comments == "Import Test ")
		row.Country = "Importing TSV";
	else if(row.Comments == "API Test ")
		row.Country = "API BeforeInsert";
	console.log("edited row is: " + row);
	console.log(shared.sampleFunc("this is from the shared function"));
}

function beforeUpdate(row, oldRow, errors) {
	console.log("beforeUpdate got triggered");
	console.log("row is: " + row);
	if(row.Comments == "BeforeUpdate")
		row.Country = "Before Update changed me";
	console.log("old row is: " + oldRow);
	console.log(shared.sampleFunc("this is from the shared function"));
}

function beforeDelete(row, errors) {
	console.log("beforeDelete got triggered");
	console.log("row is: " + row);
	if(row.Comments == "BeforeDelete")
		errors[null] = "This is the Before Delete Error";

	console.log(shared.sampleFunc("this is from the shared function"));
}

function afterInsert(row, errors) {
	console.log("afterInsert got triggered");
	console.log("row is: " + row);

	if(row.Comments == "AfterInsert")
		errors[null] = "This is the After Insert Error";

	console.log(shared.sampleFunc("this is from the shared function"));
}

function afterUpdate(row, oldRow, errors) {
	console.log("afterUpdate got triggered");
	console.log("row is: " + row);

	if(row.Comments == "AfterUpdate")
		errors[null] = "This is the After Update Error";

	console.log("old row is: " +oldRow);
	console.log(shared.sampleFunc("this is from the shared function"));
}

function afterDelete(row, errors) {
	console.log("afterDelete got triggered");
	console.log("row is: " + row);

	if(row.Country == "Before Update changed me")
		errors[null] = "This is the After Delete Error";

	console.log(shared.sampleFunc("this is from the shared function"));
}


function complete(event, errors) {
	console.log("complete got triggered with event: " + event);
    console.log(shared.sampleFunc("this is from the shared function"));
}