/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class, Assays.class})
public class ModuleAssayTest extends AbstractAssayTest
{
    private final static String PROJECT_NAME = "ModuleAssayTest";
    private static final String MODULE_NAME = "miniassay";
    private static final String ASSAY_NAME = "My Simple Assay";

    private static final String SAMPLE_SET = "Test Sample Set";
    private static final String SAMPLE_SET_ROWS = "Name\tBarcode\n" +
            "First\t251379110131_A01\n" +
            "Second\t251379110131_A01\n" +
            "Third\t\n" +
            "Fourth\t\n";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteDir(getTestTempDir());
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected void runUITests() throws Exception
    {
        log("Starting ModuleAssayTest");

        checkModuleDeployed();
        setupProject();

        String batchName = "First Batch";
        uploadBatch(batchName, "run01.tsv", "run02.tsv");

        log("Visit batches page");
        clickAndWait(Locator.linkWithText(ASSAY_NAME + " Batches"));
        assertTitleEquals(ASSAY_NAME + " Batches: /" + PROJECT_NAME);

        // Verify file-based view associated with assay design shows up, with expected columns
        _extHelper.clickMenuButton("Views", "AssayDesignBatches");
        _extHelper.clickMenuButton("Views", "AssayDesignChildSchemaBatches");
        assertTextPresent("Modified", "Created By");
        assertTextNotPresent("First Batch", "Target Study", "Target Study", "Run Count");

        // Verify file-based view associated with assay type shows up, with expected columns
        _extHelper.clickMenuButton("Views", "AssayTypeBatches");
        assertTextPresent("First Batch", "Created By");
        assertTextNotPresent("Modified", "Run Count");

        _extHelper.clickMenuButton("Views", "default");

        log("Visit batch details page");
        clickAndWait(Locator.linkWithText("details"));
        assertTitleEquals(batchName + " Details: /" + PROJECT_NAME);
        waitForElement(Locator.id("RunName_0"), WAIT_FOR_JAVASCRIPT);
        assertElementContains(Locator.id("RunName_0"), "run01");
        assertElementContains(Locator.id("SampleId_0_0"), "Monkey 1");
        assertElementContains(Locator.id("DoubleData_0_0"), String.valueOf(3.2));
        assertElementContains(Locator.id("RunName_1"), "run02");
        assertElementContains(Locator.id("SampleId_1_2"), "Monkey 3");
        assertElementContains(Locator.id("DoubleData_1_2"), String.valueOf(1.5));

        log("Visit runs page");
        // back to batches page
        goBack();
        clickAndWait(Locator.linkWithText(batchName));
        assertTitleEquals(ASSAY_NAME + " Runs: /" + PROJECT_NAME);

        // Verify file-based view associated with assay design shows up, with expected columns
        _extHelper.clickMenuButton("Views", "AssayDesignRuns");
        _extHelper.clickMenuButton("Views", "AssayDesignChildSchemaRuns");
        assertTextPresent("Created By", "Modified");
        assertTextNotPresent("Assay Id", "MetaOverride Double Run", "Run Count", "run01.tsv", "run02.tsv");

        // Verify file-based view associated with assay type shows up, with expected columns
        _extHelper.clickMenuButton("Views", "AssayTypeRuns");
        assertTextPresent("Assay Id", "Created By", "MetaOverride Double Run", "run01.tsv", "run02.tsv");
        assertTextNotPresent("Modified", "Run Count");

        _extHelper.clickMenuButton("Views", "default");

        log("Visit run details page");
        setSort("Runs", "Name", SortDirection.ASC);
        clickAndWait(Locator.linkWithText("details"));
        assertTitleEquals("run01.tsv Details: /" + PROJECT_NAME);
        assertElementContains(Locator.id("SampleId_0"), "Monkey 1");
        assertElementContains(Locator.id("DoubleData_0"), String.valueOf(3.2));

        log("Visit results page");
        // back to runs page
        goBack();
        clickAndWait(Locator.linkWithText("run01.tsv"));
        assertTitleEquals(ASSAY_NAME + " Results: /" + PROJECT_NAME);

                // Verify file-based view associated with assay design shows up, with expected columns
        _extHelper.clickMenuButton("Views", "AssayDesignData");
        _extHelper.clickMenuButton("Views", "AssayDesignChildSchemaData");
        assertTextPresent("Created By", "Modified");
        assertTextNotPresent("Sample Id", "Monkey 1", "Monkey 2", "MetaOverride Double Run", "Run Count");

        // Verify file-based view associated with assay type shows up, with expected columns
        _extHelper.clickMenuButton("Views", "AssayTypeData");
        assertTextPresent("Sample Id", "Monkey 1", "Monkey 2", "Time Point", "Double Data", "Assay Id", "run01.tsv");
        assertTextNotPresent("MetaOverride Double Run", "Run Count", "Target Study");

        _extHelper.clickMenuButton("Views", "default");

        log("Visit result details page");
        clickAndWait(Locator.linkWithText("details"));
        assertElementContains(Locator.id("SampleId_div"), "Monkey 1");
        assertElementContains(Locator.id("TimePoint_div"), "2008/11/01 11:22:33");
        assertElementContains(Locator.id("DoubleData_div"), String.valueOf(3.2));
        assertElementContains(Locator.id("HiddenData_div"), "super secret!");


        log("Issue 13404: Test assay query metadata is available when module is active or inactive");
        goBack();
        clickAndWait(Locator.linkContainingText("view runs"));
        assertTextPresent("Simple Assay Button");
        pushLocation();
        _containerHelper.enableModule(PROJECT_NAME, "miniassay");
        popLocation();
        clickButton("Simple Assay Button", 0);
        assertAlert("button clicked");
        assertEquals("Unexpected alerts", 0, dismissAllAlerts());

        // Check that a query scoped to the assay type shows up for this assay design and has the right data
        goToSchemaBrowser();
        selectQuery("assay.simple." + ASSAY_NAME, "AssayProviderScopedRunQuery");
        waitForText("view data");
        clickAndWait(Locator.linkWithText("view data"));
        assertTextPresent("Prefixrun01.tsvSuffix", "Prefixrun02.tsvSuffix");
        // Check that the ${schemaName} substitution works as expected and that query metadata got applied
        clickAndWait(Locator.linkWithText("Prefixrun02.tsvSuffix"));
        assertTextPresent("Monkey 1", "Monkey 2", "Monkey 3");
        assertTextNotPresent("Monkey 4");

        // Check that we're still backwards compatible with the old assay query names
        goToSchemaBrowser();
        selectQuery("assay", "LegacyAssayRunQueryName");
        waitForText("view data");
        clickAndWait(Locator.linkWithText("view data"));
        assertTextPresent("LegacyPrefixrun01.tsvSuffix", "LegacyPrefixrun02.tsvSuffix");

        goToSchemaBrowser();
        selectQuery("assay", "LegacyAssayDataQueryName");
        waitForText("view data");
        clickAndWait(Locator.linkWithText("view data"));
        assertTextPresent("LegacyPrefixsuper secret!Suffix", "LegacyPrefixwakka wakkaSuffix");
    }

    protected void checkModuleDeployed()
    {
        log("Checking miniassay module is deployed");
        goToAdminConsole();
        assertTextPresent(MODULE_NAME);
    }

    protected void setupProject()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.enableModule("miniassay");
        setupPipeline(PROJECT_NAME);
        createAssayDesign();
        createSampleSet();
    }

    protected void createAssayDesign()
    {
        PortalHelper portalHelper = new PortalHelper(this);

        log("Creating assay design");
        clickProject(PROJECT_NAME);

        portalHelper.addWebPart("Assay List");
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", "Noblis Simple"));
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        log("Setting up simple assay");
        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), ASSAY_NAME);
        setFormElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), "My Simple Assay Description");

        sleep(1000);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    protected void createSampleSet()
    {
        PortalHelper portalHelper = new PortalHelper(this);

        log("Creating sample set");
        clickProject(PROJECT_NAME);

        portalHelper.addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement(Locator.name("name"), SAMPLE_SET);
        setFormElement(Locator.name("data"), SAMPLE_SET_ROWS);
        clickButton("Submit");
    }

    protected void uploadBatch(String batchName, String... uploadedFiles)
    {
        File dataRoot = new File(TestFileUtils.getLabKeyRoot(), "/sampledata/miniassay/data");
        assertTrue(dataRoot.isDirectory());

        log("Uploading batch: " + batchName);
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        clickButton("Import Data");
        assertTitleEquals("Data Import: /" + PROJECT_NAME);

        Locator.IdLocator batch_name_input = Locator.id("batch_name_input");
        setFormElement(batch_name_input, batchName);

        Locator.IdLocator batch_comment_input = Locator.id("batch_comment_input");
        setFormElement(batch_comment_input, batchName + " comments...");

        clickButton("Save", 0);
        waitForText("My Simple Assay Description");

        // check name and comments stuck
        assertEquals(batchName, getFormElement(batch_name_input));
        assertEquals(batchName + " comments...", getFormElement(batch_comment_input));

        for (int i = 0; i < uploadedFiles.length; i++)
        {
            String uploadedFile = uploadedFiles[i];
            File file = new File(dataRoot, uploadedFile);
            assertTrue(file.exists());
            setFormElement(Locator.id("upload-run-field-file"), file);
            int count = 5;
            do
            {
                sleep(1500);
                String runCountStr = getText(Locator.id("batch_runCount_div"));
                if (runCountStr != null && !runCountStr.equals("") && Integer.parseInt(runCountStr) == i+1)
                    break;
            } while (--count > 0);
            assertElementContains(Locator.id("batch_runCount_div"), String.valueOf(i+1));
            // file name should appear in runs grid on upload page
            waitForText(uploadedFile, WAIT_FOR_JAVASCRIPT);
        }
    }
}
