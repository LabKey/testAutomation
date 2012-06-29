//module.xml should require this module's context to be present
Ext.onReady(function(){
    if(LABKEY.getModuleContext('simpletest') === null)
        alert('Simpletest module context not present');

    if(LABKEY.getModuleContext('core') === null)
        alert('Core module context not present');

});
