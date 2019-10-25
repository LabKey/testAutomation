/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.ReactAssayDesignerPage;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public abstract class AbstractAssayHelper
{
    private static final String MANAGE_LINK_TEXT = "Manage assay design";
    
    protected BaseWebDriverTest _test;

    public AbstractAssayHelper(BaseWebDriverTest test)
    {
        _test = test;
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

    public abstract void importAssay(String assayName, File file, String projectPath, Map<String, Object> batchProperties) throws CommandException, IOException;

    public void createAssayWithDefaults(String provider, String name)
    {
        fail("Please change your test to use the new assay designer by using createAssayDesignWithDefaults().");
    }

    @LogMethod
    public void createAssayDesignWithDefaults(String type, String name)
    {
        createAssayDesign(type, name).clickFinish();

        // TODO add a check that the new assay name is in list?
        DataRegionTable.DataRegion(_test.getDriver()).withName("AssayList").waitFor();
    }

    @LogMethod
    public ReactAssayDesignerPage createAssayDesign(String type, String name)
    {
        _test.enableUxDomainDesigner();
        _test.clickButton("New Assay Design");
        _test.checkRadioButton(Locator.radioButtonByNameAndValue("providerName", type));
        _test.clickButton("Next");
        _test.disableUxDomainDesigner();

        ReactAssayDesignerPage assayDesigner = new ReactAssayDesignerPage(_test.getDriver());
        assayDesigner.setName(name);
        return assayDesigner;
    }

    public ReactAssayDesignerPage clickEditAssayDesign()
    {
        return clickEditAssayDesign(false);
    }

    public ReactAssayDesignerPage clickEditAssayDesign(boolean confirmEditInOtherContainer)
    {
        _test.enableUxDomainDesigner();
        _test.doAndWaitForPageToLoad(() ->
        {
            clickManageOption(false, "Edit assay design");
            if (confirmEditInOtherContainer)
            {
                String alertText = _test.acceptAlert();
                assertTrue("Alert did not contain expected text\nExpected: This assay is defined in the\nActual: " + alertText,
                        alertText.contains("This assay is defined in the"));
                assertTrue("Alert did not contain expected text\nExpected: Would you still like to edit it?\nActual: " + alertText,
                        alertText.contains("Would you still like to edit it?"));
            }
        });
        // use the assayDesignerPage to synchronize
        ReactAssayDesignerPage page = new ReactAssayDesignerPage(_test.getDriver());
        _test.disableUxDomainDesigner();
        return page;
    }

    public ReactAssayDesignerPage copyAssayDesign()
    {
        return copyAssayDesign(null);
    }

    public ReactAssayDesignerPage copyAssayDesign(@Nullable String destinationFolder)
    {
        _test.enableUxDomainDesigner();
        clickManageOption(true, "Copy assay design");
        if (destinationFolder == null)
            _test.clickButton("Copy to Current Folder");
        else
            _test.clickAndWait(Locator.tag("tr").append(Locator.linkWithText(destinationFolder)));
        _test.disableUxDomainDesigner();

        return new ReactAssayDesignerPage(_test.getDriver());
    }

    public void deleteAssayDesign()
    {
        clickManageOption(true, "Delete assay design");
        _test.clickButton("Confirm Delete");
    }

    public File exportAssayDesign()
    {
        return _test.doAndWaitForDownload(() -> clickManageOption(true, "Export assay design"));
    }

    public void setDefaultValues(String assayName, AssayDefaultAreas defaults)
    {
        clickManageOption(true, "Set default values", defaults.getMenuText(assayName));
    }

    public void clickManageOption(boolean wait, String ... subMenuLabels)
    {
        new DataRegionTable.DataRegionFinder(_test.getDriver()).waitFor(); // Just to ensure that the page has loaded
        BootstrapMenu.finder(_test.getDriver()).timeout(WAIT_FOR_JAVASCRIPT)
                .withButtonText(MANAGE_LINK_TEXT).find().withExpandRetries(3)
                .clickSubMenu(wait, subMenuLabels);
    }

    public enum AssayDefaultAreas
    {
        BATCH_FIELDS("Batch Fields"),
        DATA_FIELDS("Data Fields"),
        ANALYTE_PROPERTIES("Analyte Properties"),
        RUN_FIELDS("Run Fields"),
        EXCEL_FILE_RUN_PROPERTIES("Excel File Run Properties");

        private String menuSuffix;

        AssayDefaultAreas(String menuSuffix)
        {
            this.menuSuffix = menuSuffix;
        }

        private String getMenuText(String assayName)
        {
            return assayName + " " + menuSuffix;
        }
    }
}
