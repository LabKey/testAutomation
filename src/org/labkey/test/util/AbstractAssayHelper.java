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
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/14/12
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractAssayHelper extends AbstractHelper
{
    public AbstractAssayHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

//    public abstract void importAssay(int assayID, String file, String projectPath) throws CommandException, IOException;
    public abstract void importAssay(String assayName, String file, String projectPath) throws CommandException, IOException;



    public void uploadXarFileAsAssayDesign(String path, int pipelineCount, String name)
    {
        uploadXarFileAsAssayDesign(new File(path), pipelineCount, name);
    }

    /**
     * Upload a xar file as an assay configuration
     *
     * There's no API version of this, so it can go in the absract helper for now.
     * Preconditions:  on a page with an assay web part
     * @param file   file to upload
     * @param pipelineCount  expected count of succesful pipeline jobs including thise one
     * @param name  name of assay file (rest of path removed)
     */
    public void uploadXarFileAsAssayDesign(File file, int pipelineCount, String name)
    {
        Assert.assertTrue("XAR file does not exist: " + file.toString(), file.exists());
        //create a new luminex assay
        _test.clickButton("Manage Assays");
        _test.clickButton("New Assay Design");

        _test.clickLinkWithText("upload the XAR file directly");
        _test.setFormElement(Locator.name("uploadFile"), file);
        _test.click(Locator.xpath("//input[contains(@type, 'SUBMIT') and contains(@value, 'Upload')]"));
        _test.waitForPageToLoad();
        _test.waitForPipelineJobsToComplete(pipelineCount, "Uploaded file - " + name, false);
    }

}
