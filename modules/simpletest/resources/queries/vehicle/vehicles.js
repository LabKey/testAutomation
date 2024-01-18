var LABKEY = require("labkey");

function beforeInsert(row, errors) {
    row.TriggerScriptContainer = LABKEY.Security.currentContainer.id;
}

function beforeUpdate(row, oldRow, errors) {
    row.TriggerScriptContainer = LABKEY.Security.currentContainer.id;
}