/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

@Category({DailyA.class, Assays.class, FileBrowser.class})
public class AffymetrixAssayTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "AffymetrixAssayVerifyProject";
    private static final String PIPELINE_ROOT = "/sampledata/Affymetrix";
    private static final String ASSAY_NAME = "Affy Test Assay";
    private static final String EXTRA_RUN_DATA_FIELD_NAME = "ExtraField";
    private static final String EXTRA_RUN_DATA_FIELD_LABEL = "Extra Field";
    private static final String SAMPLE_SET_NAME = "AffyTestSampleSet";
    private static final String EXCEL_FILE_NAME = "test_affymetrix_run.xlsx";
    public static final String CEL_FILE_NAME = "sample_file_1.CEL";

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("microarray");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Test
    public void testSteps()
    {
        doCreateSteps();
        doVerifySteps();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @LogMethod
    protected void doCreateSteps()
    {
        log("Create Project");

        _containerHelper.createProject(getProjectName(), "Assay");

        log("Setup the pipeline");
        setPipelineRoot(TestFileUtils.getLabKeyRoot() + PIPELINE_ROOT);
        assertTextPresent("The pipeline root was set to");
        _containerHelper.enableModule("Microarray");

        log("Create Affymetrix Assay");
        goToManageAssays();
        clickButton("New Assay Design");
        checkCheckbox(Locator.tagWithId("input", "providerName_Affymetrix"));
        clickButton("Next");
        Locator nameLocator = Locator.tagWithId("input", "AssayDesignerName");
        waitForElement(nameLocator);
        setFormElement(nameLocator, ASSAY_NAME);
        _listHelper.addField("Data Fields", EXTRA_RUN_DATA_FIELD_NAME, EXTRA_RUN_DATA_FIELD_LABEL, ListHelper.ListColumnType.Integer);
        clickButton("Save & Close");

        PortalHelper portalHelper = new PortalHelper(this);

        log("Create Sample Set");
        String sampleData = "hyb_name\n";

        for (int i = 1; i <= 96; i++)
        {
            sampleData += "Sample" + i + "\n";
        }

        goToProjectHome();
        portalHelper.addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement(Locator.name("name"), SAMPLE_SET_NAME);
        setFormElement(Locator.name("data"), sampleData);
        clickButton("Submit");
    }

    @LogMethod
    protected void doVerifySteps()
    {
        importRun();
        verifyResults();
    }

    @LogMethod
    private void importRun()
    {
        goToModule("Pipeline");
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile(EXCEL_FILE_NAME, "Use " + ASSAY_NAME);
        clickButton("Save and Finish");
    }

    @LogMethod
    private void verifyResults()
    {
        assertTextPresent(EXCEL_FILE_NAME);
        click(Locator.linkContainingText(EXCEL_FILE_NAME));
        waitForText(EXTRA_RUN_DATA_FIELD_LABEL);
        pushLocation();
        click(Locator.linkContainingText("Sample1"));
        waitForElement(Locator.linkWithText(EXCEL_FILE_NAME));
        popLocation();
        click(Locator.linkContainingText(CEL_FILE_NAME));
        waitForElement(Locator.linkWithText(EXCEL_FILE_NAME));
    }
}
