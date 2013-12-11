/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename'] + ", schema=" + schemaName + ", table=" + tableName);

var LABKEY = require("labkey");

function beforeInsert(row, errors)
{
	// Initial caps
	if (row.TextField)
		row.TextField = row.TextField.substring(0,1).toUpperCase() + row.TextField.substring(1);
	// ALL CAPS
	if (row.SampleId)	
		row.SampleId = row.SampleId.toUpperCase();
}

function afterInsert(row, errors)
{
}

function beforeUpdate(row, oldRow, errors)
{
}

function afterUpdate(row, oldRow, errors)
{
}

function beforeDelete(row, errors)
{
}

function afterDelete(row, errors)
{
}

