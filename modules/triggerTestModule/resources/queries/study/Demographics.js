var shared = require("TriggerTestModule/SharedTriggerLib");
var console = require("console");
   
function managedColumns() {
    return shared.managedColumns();
}

function init(event, errors) {
	console.log("init got triggered with event: " + event);
}

function beforeInsert(row, errors) {
	console.log("study: beforeInsert: row is: " + row);
    shared.beforeInsert(row, errors);
	console.log("study: edited row is: " + row);
}

function beforeUpdate(row, oldRow, errors) {
	console.log("study: beforeUpdate: row is: " + row);
    shared.beforeUpdate(row, oldRow, errors);
	console.log("study: old row is: " + oldRow);
}

function beforeDelete(row, errors) {
	console.log("study: beforeDelete: row is: " + row);
    shared.beforeDelete(row, errors);
}

function afterInsert(row, errors) {
	console.log("study: afterInsert: row is: " + row);
    shared.afterInsert(row, errors);
}

function afterUpdate(row, oldRow, errors) {
	console.log("study: afterUpdate: row is: " + row);
    shared.afterUpdate(row, oldRow, errors);
	console.log("study: old row is: " +oldRow);
}

function afterDelete(row, errors) {
	console.log("study: afterDelete: row is: " + row);
    shared.afterDelete(row, errors);
}

function complete(event, errors) {
	console.log("study: complete got triggered with event: " + event);
}