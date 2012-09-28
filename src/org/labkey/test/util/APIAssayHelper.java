/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.assay.AssayListCommand;
import org.labkey.remoteapi.assay.AssayListResponse;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.test.BaseSeleniumWebTest;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/14/12
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */

public class APIAssayHelper extends AbstractAssayHelper
{

    public APIAssayHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    public void importAssay(int assayID, String file, String projectPath) throws CommandException, IOException
    {
        ImportRunCommand  irc = new ImportRunCommand(assayID, new File(file));
        irc.execute(_test.getDefaultConnection(), "/" + projectPath);
    }

    public void importAssay(String assayName, String file, String projectPath) throws CommandException, IOException
    {
        importAssay(getIdFromAssayName(assayName, projectPath), file, projectPath);
    }

    private int getIdFromAssayName(String assayName, String projectPath)
    {
        AssayListCommand alc = new AssayListCommand();
        alc.setName(assayName);
        AssayListResponse alr = null;
        try
        {
            alr = alc.execute(_test.getDefaultConnection(), "/" + projectPath);
        }
        catch (Exception e)
        {
            if(e.getMessage().contains("Not Found"))
                Assert.fail("Assay or project not found");
        }

        if(alr.getDefinition(assayName)==null)
            Assert.fail("Assay not found");
        return ((Long) alr.getDefinition(assayName).get("id")).intValue();

    }
}
