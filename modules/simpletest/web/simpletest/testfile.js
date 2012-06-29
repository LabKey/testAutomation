//module.xml should require this module's context to be present
//if not, the alert should fail the test
Ext.onReady(function(){
    if(LABKEY.getModuleContext('simpletest') === null)
        alert('Simpletest module context not present');

    if(LABKEY.getModuleContext('core') === null)
        alert('Core module context not present');

    //we also test for this value
    LABKEY.moduleContext.simpletest.scriptLoaded = true;
});