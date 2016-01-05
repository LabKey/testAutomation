/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.pages.AssayDomainEditor;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public abstract class AbstractAssayHelper extends AbstractHelper
{
    public AbstractAssayHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    public abstract void importAssay(String assayName, File file, String projectPath) throws CommandException, IOException;

    protected abstract void goToUploadXarPage();

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
        goToUploadXarPage();
        _test.setFormElement(Locator.name("uploadFile"), file);
        _test.clickAndWait(Locator.lkButton("Upload"));
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
        createAssayAndEdit(type, name).saveAndClose();

        _test.waitForElement(Locator.id("AssayList"));
    }

    public AssayDomainEditor createAssayAndEdit(String type, String name)
    {
        _test.clickButton("New Assay Design");
        _test.checkRadioButton(Locator.radioButtonByNameAndValue("providerName", type));
        _test.clickButton("Next");

        AssayDomainEditor assayDesigner = new AssayDomainEditor(_test);
        assayDesigner.setName(name);
        assayDesigner.save();

        return assayDesigner;
    }

    public AssayDomainEditor clickEditAssayDesign()
    {
        return clickEditAssayDesign(false);
    }

    public AssayDomainEditor clickEditAssayDesign(boolean confirmEditInOtherContainer)
    {
        _test.prepForPageLoad();
        _test._ext4Helper.clickExt4MenuButton(false, Locator.linkWithText("MANAGE ASSAY DESIGN"), false, "edit assay design");
        if (confirmEditInOtherContainer)
        {
            String alertText = _test.acceptAlert();
            assertTrue("Alert did not contain expected text\nExpected: This assay is defined in the\nActual: " + alertText,
                    alertText.contains("This assay is defined in the"));
            assertTrue("Alert did not contain expected text\nExpected: Would you still like to edit it?\nActual: " + alertText,
                    alertText.contains("Would you still like to edit it?"));
        }
        _test.waitForPageToLoad(BaseWebDriverTest.WAIT_FOR_PAGE);
        _test.waitForElement(Locator.id("AssayDesignerDescription"));

        return new AssayDomainEditor(_test);
    }

    public AssayDomainEditor copyAssayDesign()
    {
        return copyAssayDesign(null);
    }

    public AssayDomainEditor copyAssayDesign(@Nullable String destinationFolder)
    {
        _test._ext4Helper.clickExt4MenuButton(true, Locator.linkWithText("MANAGE ASSAY DESIGN"), false, "copy assay design");

        if (destinationFolder == null)
            _test.clickButton("Copy to Current Folder");
        else
            _test.clickAndWait(Locator.tag("tr").append(Locator.linkWithText(destinationFolder)));

        return new AssayDomainEditor(_test);
    }

    public void deleteAssayDesign()
    {
        _test._ext4Helper.clickExt4MenuButton(true, Locator.linkWithText("MANAGE ASSAY DESIGN"), false, "delete assay design");
        _test.clickButton("Confirm Delete");
    }

    public File exportAssayDesign()
    {
        return _test.doAndWaitForDownload(() ->
                _test._ext4Helper.clickExt4MenuButton(true, Locator.linkWithText("MANAGE ASSAY DESIGN"), false, "export assay design"));
    }

    public void setDefaultValues(final String assayName, final AssayDefaultAreas defaults)
    {
        _test._ext4Helper.clickExt4MenuButton(true, Locator.linkWithText("MANAGE ASSAY DESIGN"), false, "set default values", defaults.getMenuText(assayName));
    }

    public static enum AssayDefaultAreas
    {
        BATCH_FIELDS("Batch Fields"),
        DATA_FIELDS("Data Fields"),
        ANALYTE_PROPERTIES("Analyte Properties"),
        RUN_FIELDS("Run Fields"),
        EXCEL_FILE_RUN_PROPERTIES("Excel File Run Properties");

        private String menuSuffix;

        private AssayDefaultAreas(String menuSuffix)
        {
            this.menuSuffix = menuSuffix;
        }

        private String getMenuText(String assayName)
        {
            return assayName + " " + menuSuffix;
        }
    }
}
