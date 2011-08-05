<div id='queryTestDiv1'/>
<script type="text/javascript">
var myFilters = [
		LABKEY.Filter.create('Weight_kg', '75', LABKEY.Filter.Types.LESS_THAN),
		LABKEY.Filter.create('Temp_C',
		LABKEY.ActionURL.getParameter('Temp_C'),LABKEY.Filter.Types.LESS_THAN)
	]

var qwp1 = new LABKEY.QueryWebPart({
	renderTo: 'queryTestDiv1',
	title: 'My Query Web Part',
	schemaName: 'core',
	queryName: 'SiteUsers',
});

 //note that you may also register for the 'render' event
 //instead of using the success config property.
 //registering for events is done using Ext event registration.
 //Example:
 qwp1.on("render", onRender);
 function onRender()
 {
    //...do something after the part has rendered...
 }

 ///////////////////////////////////////
 // Custom Button Bar Example

function onTestHandler(dataRegion)
{
    alert("onTestHandler called!");
    return false;
}

function onItem1Handler(dataRegion)
{
    alert("onItem1Handler called!");
}

function onItem2Handler(dataRegion)
{
    alert("onItem2Handler called!");
}
