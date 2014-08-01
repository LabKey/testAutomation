/*
 * Copyright (c) 2012-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
//module.xml should require this module's context to be present
//if not, the alert should fail the test
if(LABKEY.getModuleContext('simpletest') === null)
    alert('Simpletest module context not present');

if(LABKEY.getModuleContext('core') === null)
    alert('Core module context not present');

//we also test for this value
LABKEY.moduleContext.simpletest.scriptLoaded = true;