/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var LABKEY = require("labkey");

function beforeInsert(row, errors) {
    row.TriggerScriptContainer = LABKEY.Security.currentContainer.id;
}

function beforeUpdate(row, oldRow, errors) {
    row.TriggerScriptContainer = LABKEY.Security.currentContainer.id;
}