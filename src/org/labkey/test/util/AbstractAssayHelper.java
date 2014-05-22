/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public abstract class AbstractAssayHelper extends AbstractHelper
{
    public AbstractAssayHelper(BaseWebDriverTest test)
    {
        super(test);
    }

//    public abstract void importAssay(int assayID, String file, String projectPath) throws CommandException, IOException;
    public abstract void importAssay(String assayName, File file, String projectPath) throws CommandException, IOException;



    public void uploadXarFileAsAssayDesign(String path, int pipelineCount)
    {
        uploadXarFileAsAssayDesign(new File(path), pipelineCount);
    }

    /**
     * Upload a xar file as an assay configuration
     *
     * There's no API version of this, so it can go in the absract helper for now.
     * Preconditions:  on a page with an assay web part
     * @param file   file to upload
     * @param pipelineCount  expected count of succesful pipeline jobs including thise one
     */
    @LogMethod
    public void uploadXarFileAsAssayDesign(File file, int pipelineCount)
    {
        assertTrue("XAR file does not exist: " + file.toString(), file.exists());
        //create a new luminex assay
        _test.clickButton("Manage Assays");
        _test.clickButton("New Assay Design");

        _test.clickAndWait(Locator.linkWithText("upload"));
        _test.setFormElement(Locator.name("uploadFile"), file);
        _test.clickAndWait(Locator.navButton("Upload"));
        _test.waitForPipelineJobsToComplete(pipelineCount, "Uploaded file - " + file.getName(), false);
    }

    @LogMethod
    public void addAliasedFieldToMetadata(String schemaName, String tableName, String aliasedColumn, String columnName, ListHelper.LookupInfo lookupInfo)
    {
        //go to schema browser
        _test.goToSchemaBrowser();

        //go to assay
        _test.selectQuery(schemaName, tableName);

        //edit metadata
        _test.waitForText("edit metadata");
        _test.clickAndWait(Locator.linkWithText("edit metadata"));
        _test.sleep(5000); //TODO;
        _test.clickButton("Alias Field", "Choose a field");

        Locator l = Locator.name("sourceColumn");
        _test.selectOptionByText(l, aliasedColumn);
        _test.clickButton("OK", BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

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
        int count = _test.getElementCount(Locator.xpath("//input[contains(@name, 'ff_name')]"));
        Locator l = Locator.xpath("(//input[contains(@name, 'ff_name')])["+count + "]");
        _test.isElementPresent(l);
        String name = _test.getAttribute(l,  "name");
        return Integer.parseInt(name.substring(7));
    }


    public abstract void importAssay(String assayName, File file, String projectPath, Map<String, Object> batchProperties) throws CommandException, IOException;

    @LogMethod
    public void createAssayWithDefaults(String type, String name)
    {
        _test.clickButton("New Assay Design");
        _test.checkRadioButton(Locator.radioButtonByNameAndValue("providerName", type));
        _test.clickButton("Next", 0);

        _test.waitForElement(Locator.id("AssayDesignerName"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.id("AssayDesignerName"), name);
        _test.fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), BaseWebDriverTest.SeleniumEvent.change); // GWT compensation
        _test.clickButton("Save", 0); // GWT compensation
        _test.waitForText("Save successful.", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.clickButton("Save & Close");

        _test.waitForElement(Locator.id("AssayList"));
    }

    @LogMethod
    public void createAssayWithEditableRunFields(String type, String name)
    {
        _test.clickButton("New Assay Design");
        _test.checkRadioButton(Locator.radioButtonByNameAndValue("providerName", type));
        _test.clickButton("Next", 0);

        _test.waitForElement(Locator.xpath("//input[@type='checkbox' and @name='editableRunProperties']"));
        _test.checkCheckbox(Locator.xpath("//input[@type='checkbox' and @name='editableRunProperties']"));
        _test.waitForElement(Locator.id("AssayDesignerName"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.id("AssayDesignerName"), name);
        _test.fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), BaseWebDriverTest.SeleniumEvent.change); // GWT compensation
        _test.clickButton("Save", 0); // GWT compensation
        _test.waitForText("Save successful.", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.clickButton("Save & Close");

        _test.waitForElement(Locator.id("AssayList"));
    }

    public void clickEditAssayDesign()
    {
        _test.click(Locator.linkWithText("MANAGE ASSAY DESIGN"));
        _test.waitAndClickAndWait(Locator.linkWithText("edit assay design"));
    }

    public void addTransformScript(File transformScript)
    {
        if (transformScript.exists())
        {
            _test.waitForElement(Locator.navButton("Add Script"));
            int index = _test.getElementCount(Locator.xpath("//input[starts-with(@id, 'AssayDesignerTransformScript')]"));
            _test.clickButton("Add Script", 0);
            _test.setFormElement(Locator.xpath("//input[@id='AssayDesignerTransformScript" + index + "']"), transformScript.getAbsolutePath());
        }
        else
            fail("Unable to locate the Transform script: " + transformScript.toString());
    }


    public void setTransformScript(File transformScript)
    {
        setTransformScript(transformScript, 0);
    }

    public void setTransformScript(File transformScript, int index)
    {
        if (transformScript.exists())
        {
            _test.waitForElement(Locator.navButton("Add Script"));
            _test.setFormElement(Locator.xpath("//input[@id='AssayDesignerTransformScript" + index + "']"), transformScript.getAbsolutePath());
        }
        else
            fail("Unable to locate the Transform script: " + transformScript.toString());
    }

    public void saveAssayDesign()
    {
        _test.clickButton("Save & Close");
        _test.waitForElement(Locator.id("dataregion_Runs"));
    }
}
