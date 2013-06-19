/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
<script type="text/javascript">

var id = 'jsonWrapperTest';
var tableSuccess = function(responseData){
    var table = '<table class="labkey-data-region labkey-show-borders"><tr>';
    var i, j;

    for(i = 0; i < responseData.metaData.fields.length; i++){
        var field = responseData.metaData.fields[i];
        table = table + '<th><span>' + field.caption + '</span></th>';
    }

    table = table + '</tr>';

    for(i = 0; i < responseData.rows.length; i++){
        table = table + '<tr>';
        var row = responseData.rows[i];
        for(j = 0; j < responseData.metaData.fields.length; j++){
            var field = responseData.metaData.fields[j];
            var fieldKey = field.fieldKey.toString();
            table = table + '<td><span>' + row.data[fieldKey].value + '</span></td>';
        }
        table = table + '</tr>';
    }

    table = table + '</table>';
    document.getElementById(id).innerHTML = table;
};
var extGridSuccess = function(responseData){
    fields = responseData.metaData.fields;
    cm = responseData.getColumnModel();
    Ext4.onReady(function(){
        var storeFields = [];

        for(var i = 0; i < responseData.metaData.fields.length; i++){
            var field = responseData.metaData.fields[i];
            storeFields.push({
                name: field.fieldKey.toString(),
                mapping: 'data.' + field.fieldKey.toString() + '.value',
                type: field.jsonType
            });
        }

        Ext4.define('GetDataModel', {
            extend: 'Ext.data.Model',
            fields: storeFields
        });

        var gdStore = Ext4.create('Ext.data.Store', {
            model: 'GetDataModel',
            autoload: false,
            proxy: {type:'memory', reader: {type: 'json'}}
        });

        gdStore.loadRawData(responseData.rows);

        var myGrid = Ext4.create('Ext.grid.Panel', {
            renderTo: id,
            columns: responseData.getColumnModel(),
            store: gdStore,
            width: '100%'
        });
    });
};
var fancyRequest = REPLACEMENT_STRING

var fReq = LABKEY.Query.GetData.RawData(fancyRequest);

</script>

<div id="jsonWrapperTest">