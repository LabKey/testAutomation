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

import java.awt.image.LookupTable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

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

    public void addAliasedFieldToMetadata(String tableName, String aliasedColumn, String columnName, ListHelper.LookupInfo lookupInfo)
    {
        //go to schema browser
        _test.goToSchemaBrowser();

        //go to assay
        _test.selectQuery("assay", tableName);

        //edit metadata
        _test.waitForText("edit metadata");
        _test.clickLinkWithText("edit metadata");
        _test.sleep(5000); //TODO;
        _test.clickButton("Alias Field", "Choose a field");

        Locator l = Locator.name("sourceColumn");
        _test.setFormElement(l, aliasedColumn);
        // may need to try all lower case for form element selection
        if (!_test.getFormElement(l).equals(aliasedColumn))
            _test.setFormElement(l, aliasedColumn.toLowerCase());
        _test.clickButton("OK", _test.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        //set name
        //TODO:  better locator
        int fieldCount = getLastPropertyFieldNumber();
        _test.setFormElement(Locator.name("ff_name" + fieldCount), columnName);
        _test._listHelper.setColumnType(fieldCount, lookupInfo);
        //set lookup
        //todo
    }

    //TODO:  best location for this?
    private int getLastPropertyFieldNumber()
    {
        int count = _test.getXpathCount(Locator.xpath("//input[contains(@name, 'ff_name')]"));
        Locator l = Locator.xpath("(//input[contains(@name, 'ff_name')])["+count + "]");
        _test.isElementPresent(l);
        String name = _test.getAttribute(l,  "name");
        return new Integer(name.substring(7)).intValue();
    }


    public abstract void importAssay(String assayName, String file, String projectPath, Map<String, Object> batchProperties) throws CommandException, IOException;
}
