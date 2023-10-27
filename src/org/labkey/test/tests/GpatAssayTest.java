/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayBeginPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({Assays.class, BVT.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class GpatAssayTest extends BaseWebDriverTest
{
    private static final File GPAT_ASSAY_XLS = TestFileUtils.getSampleData("GPAT/trial01.xls");
    private static final File GPAT_ASSAY_XLSX = TestFileUtils.getSampleData("GPAT/trial01a.xlsx");
    private static final File GPAT_ASSAY_TSV = TestFileUtils.getSampleData("GPAT/trial02.tsv");
    private static final File ALIASED_ASSAY_1 = TestFileUtils.getSampleData("GPAT/trial01columns1.tsv");
    private static final File ALIASED_ASSAY_2 = TestFileUtils.getSampleData("GPAT/trial01columns2.tsv");
    private static final File ALIASED_ASSAY_3 = TestFileUtils.getSampleData("GPAT/trial01columns3.tsv");
    private static final File ALIASED_ASSAY_4 = TestFileUtils.getSampleData("GPAT/trial01columns4.tsv");
    private static final File GPAT_ASSAY_FNA_1 = TestFileUtils.getSampleData("GPAT/trial03.fna");
    private static final File GPAT_ASSAY_FNA_2 = TestFileUtils.getSampleData("GPAT/trial04.fna");
    private static final File GPAT_ASSAY_FNA_3 = TestFileUtils.getSampleData("GPAT/trial05.fna");
    private static final String ASSAY_NAME_XLS = "XLS Assay " + TRICKY_CHARACTERS;
    private static final String ASSAY_NAME_XLSX = "XLSX Assay";
    private static final String ASSAY_NAME_TSV = "TSV Assay";
    private static final String ASSAY_NAME_FNA = "FASTA Assay";
    private static final String ASSAY_NAME_FNA_MULTIPLE = "FASTA Assay - Multiple file upload";
    private static final String ASSAY_NAME_FNA_MULTIPLE_SINGLE_INPUT = "FASTA Assay - Multiple file single input upload";

    @BeforeClass
    public static void doSetup()
    {
        GpatAssayTest init = (GpatAssayTest) getCurrentTest();
        init._containerHelper.createProject(init.getProjectName(), "Assay");
        init.goToProjectHome();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return "GpatAssayTest Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {
        startCreateGpatAssay(GPAT_ASSAY_XLS, ASSAY_NAME_XLS);
        assertEquals("SpecimenID", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("VisitID", getFormElement(Locator.name("VisitID")));
        assertEquals("DrawDt", getFormElement(Locator.name("Date")));
        setAssayResultsProperties();
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLS.getName()));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.tagWithClass("*", "labkey-column-header").withText("Role")); // excluded column

        try
        {
            // Issue 36077: SelectRows: SchemaKey decoding of public schema name causes request failure
            Connection cn = createDefaultConnection();
            SelectRowsCommand selectCmd = new SelectRowsCommand("assay.General." + ASSAY_NAME_XLS, "Runs");
            selectCmd.setRequiredVersion(17.1);
            SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
            assertEquals(1, selectResp.getRowCount().intValue());
        }
        catch (CommandException e)
        {
            throw new RuntimeException(e.getResponseText());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        log("Import XLSX GPAT assay");
        startAssayCreationAndVerifyFields(GPAT_ASSAY_XLSX, ASSAY_NAME_XLSX);
        setAssayResultsProperties();
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLSX.getName()));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Import TSV GPAT assay");
        clickProject(getProjectName());
        startAssayCreationAndVerifyFields(GPAT_ASSAY_TSV, ASSAY_NAME_TSV);

        clickButton("Show Assay Designer");
        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());
        DomainFormPanel results = assayDesignerPage.expandFieldsPanel("Results");
        results.getField(4) // field name = Primary
                .setLabel("Blank")
                .setMissingValuesEnabled(true);
        results.getField(7) // field name = Score
                .setName("Result")
                .setLabel("Result")
                .setImportAliases("Score")
                .setRequiredField(true);
        assayDesignerPage.clickFinish();

        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_TSV.getName()));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Verify standard column aliases");
        startCreateGpatAssay(ALIASED_ASSAY_1, null);
        assertEquals("specId", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ParticipantID", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitNo", getFormElement(Locator.name("VisitID")));
        assertEquals("draw_date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        startCreateGpatAssay(ALIASED_ASSAY_2, null);
        assertEquals("vialId1", getFormElement(Locator.name("SpecimenID")));
        assertEquals(null, "ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visit_no", getFormElement(Locator.name("VisitID")));
        assertEquals("drawDate", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        startCreateGpatAssay(ALIASED_ASSAY_3, null);
        assertEquals("vialId", getFormElement(Locator.name("SpecimenID")));
        assertEquals(null, "ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitId", getFormElement(Locator.name("VisitID")));
        assertEquals("date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        startCreateGpatAssay(ALIASED_ASSAY_4, null);
        assertEquals("guspec", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitId", getFormElement(Locator.name("VisitID")));
        assertEquals("date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");

        importFastaGpatAssay(GPAT_ASSAY_FNA_1, ASSAY_NAME_FNA);
        log("Verify data after the GPAT assay upload");
        clickAndWait(Locator.linkWithText(GPAT_ASSAY_FNA_1.getName()));
        waitForText("Sequence");
        assertTextPresent(
                "Header", "HCJDRSZ07IVO6P", "HCJDRSZ07IL1GX", "HCJDRSZ07H5SPZ",
                "CACCAGACAGGTGTTATGGTGTGTGCCTGTAATCCCAGCTACTTGGGAGGGAGCTCAGGT");
    }

    private void setAssayResultsProperties()
    {
        clickButton("Show Assay Designer");
        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());
        DomainFormPanel results = assayDesignerPage.expandFieldsPanel("Results");
        results.getField("Score").setRequiredField(true);
        results.getField("Primary").setMissingValuesEnabled(true);
        assayDesignerPage.clickFinish();
    }

    @LogMethod
    private void importFastaGpatAssay(File fnaFile, String assayName)
    {
        startCreateGpatAssay(fnaFile, assayName);

        clickButton("Begin import");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
    }

    @LogMethod
    private void startCreateGpatAssay(File dataFile, @LoggedParam String assayName)
    {
        log("Create GPAT assay from " + dataFile.getName());
        new WebDavUploadHelper(getProjectName()).uploadFile(dataFile);
        beginAt(WebTestHelper.buildURL("pipeline", getProjectName(), "browse"));
        _fileBrowserHelper.importFile(dataFile.getName(), "Create New Standard Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        if (assayName != null)
        {
            setFormElement(Locator.name("AssayDesignerName"), assayName);
            fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        }
    }

    private void startAssayCreationAndVerifyFields(File dataFile, String assayName)
    {
        startCreateGpatAssay(dataFile, assayName);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        assertEquals("SpecimenID", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("VisitID", getFormElement(Locator.name("VisitID")));
        assertEquals("DrawDt", getFormElement(Locator.name("Date")));
    }

    @Test
    public void testMultipleFileUploadInAssayRun()
    {
        File file1 = TestFileUtils.getSampleData("GPAT/trial01a.xlsx");
        File file2 = TestFileUtils.getSampleData("GPAT/trial01b.xlsx");
        File file3 = TestFileUtils.getSampleData("GPAT/trial01c.xlsx");
        String fileName = "trial01a";

        importFastaGpatAssay(GPAT_ASSAY_FNA_2, ASSAY_NAME_FNA_MULTIPLE);
        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME_FNA_MULTIPLE));
        clickButton("Import Data");
        clickButton("Next");

        log("Check radio button for multiple upload");
        checkRadioButton(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));

        uploadAssayFile(file1, 0);
        addNewFile();
        uploadAssayFile(file2, 1);
        addNewFile();
        uploadAssayFile(file3, 2);

        clickButton("Save and Finish");

        log("Verifying the upload");
        clickAndWait(Locator.linkContainingText(fileName));
        DataRegionTable table = new DataRegionTable("Data", getDriver());
        table.assertPaginationText(1, 100, 603);

    }

    // this test only works in Chrome
    @Test
    public void testMultipleFileUploadSingleRowInAssayRun()
    {
        List<File> files = new ArrayList<>();
        files.add(TestFileUtils.getSampleData("GPAT/trial01b.xlsx"));
        files.add(TestFileUtils.getSampleData("GPAT/trial01a.xlsx"));
        files.add(TestFileUtils.getSampleData("GPAT/trial01c.xlsx"));

        String fileName = "trial01b";

        importFastaGpatAssay(GPAT_ASSAY_FNA_3, ASSAY_NAME_FNA_MULTIPLE_SINGLE_INPUT);
        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME_FNA_MULTIPLE_SINGLE_INPUT));
        clickButton("Import Data");
        clickButton("Next");

        log("Check radio button for multiple upload");
        checkRadioButton(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));

        uploadAssayFiles(files);  // write all paths to the same input field

        clickButton("Save and Finish");

        log("Verifying the upload");
        clickAndWait(Locator.linkContainingText(fileName));
        DataRegionTable table = new DataRegionTable("Data", getDriver());
        table.assertPaginationText(1, 100, 603);

    }

    @Test
    public void testRenameAssayDesign()
    {
        String originalAssayName = "A Assay Name";
        log(String.format("Create an assay named '%s'.", originalAssayName));
        startCreateGpatAssay(GPAT_ASSAY_XLS, originalAssayName);
        setAssayResultsProperties();

        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);

        String newAssayName = "Updated Assay Name";
        log(String.format("Edit the assay design and rename it to '%s'.", newAssayName));


        ReactAssayDesignerPage assayDesignerPage = _assayHelper.clickEditAssayDesign(false);
        checker().fatal()
                .verifyTrue("The 'Name' field should be enabled and editable. Fatal error.",
                        assayDesignerPage.isNameEnabled());

        assayDesignerPage.setName(newAssayName);
        assayDesignerPage.clickFinish();

        log("Validate the new name is shown in the header.");
        WebElement header = Locator.tagWithText("h3", String.format("%s Runs", newAssayName)).refindWhenNeeded(getDriver());
        checker().withScreenshot()
                .verifyTrue(String.format("New name '%s' is not shown on the assays runs page.", newAssayName),
                        waitFor(header::isDisplayed, 5_000));

        AssayBeginPage assayBeginPage = AssayBeginPage.beginAt(this);

        log("Validate the new name is shown in the lists of assays.");
        List<String> actualValues = assayBeginPage.getAssayList().getColumnDataAsText("Name");

        checker().fatal()
                .verifyTrue(String.format("New assay name '%s' is not in the assay list. Fatal error.", newAssayName),
                        actualValues.contains(newAssayName));

        log("Validate that clicking the assay name navigates as expected.");
        assayBeginPage.clickAssay(newAssayName);

        checker().withScreenshot()
                .verifyTrue(String.format("Clicking assay name '%s' did not navigate as expected.", newAssayName),
                        waitFor(header::isDisplayed, 5_000));

    }

    @Test
    public void testRenameSharedAssayDesign()
    {
        String otherProject = "Testing_Shared_Assay_Rename";

        _containerHelper.deleteProject(otherProject, false);

        log(String.format("Create another project named '%s'.", otherProject));

        _containerHelper.createProject(otherProject, "Assay");

        goToProjectHome();

        String originalAssayName = "A Shared Assay Name";
        log(String.format("Create an assay named '%s' and put it in the shared folder.", originalAssayName));
        startCreateGpatAssay(GPAT_ASSAY_XLS, originalAssayName);

        Locator gwtSelectLocator = Locator.xpath("//h3[@title='Assay Properties']/ancestor::div[contains(@class,'panel-portal')]//select[@class='gwt-ListBox']");
        Select location = SelectWrapper.Select(gwtSelectLocator).findWhenNeeded(getDriver());
        location.selectByVisibleText("Shared Folder");

        setAssayResultsProperties();

        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);

        log("Validate assay is visible in other project.");
        goToProjectHome(otherProject);
        AssayBeginPage assayBeginPage = AssayBeginPage.beginAt(this, otherProject);

        List<String> actualValues = assayBeginPage.getAssayList().getColumnDataAsText("Name");

        checker().fatal()
                .verifyTrue(String.format("Original assay name '%s' is not visible in the project '%s'. Fatal error.", originalAssayName, otherProject),
                        actualValues.contains(originalAssayName));

        log("Go back to the original project and rename the assay.");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(originalAssayName));
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.clickEditAssayDesign(true);
        checker().fatal()
                .verifyTrue("The 'Name' field should be enabled and editable. Fatal error.",
                        assayDesignerPage.isNameEnabled());

        String newAssayName = "Updated Shared Assay Name";
        assayDesignerPage.setName(newAssayName);
        assayDesignerPage.clickFinish();

        WebElement header = Locator.tagWithText("h3", String.format("%s Runs", newAssayName)).refindWhenNeeded(getDriver());
        checker().withScreenshot()
                .verifyTrue(String.format("New name '%s' is not shown on the assays runs page.", newAssayName),
                        waitFor(header::isDisplayed, 5_000));

        log(String.format("Go to project '%s' and validate the new assay name.", otherProject));
        assayBeginPage = AssayBeginPage.beginAt(this, otherProject);

        actualValues = assayBeginPage.getAssayList().getColumnDataAsText("Name");

        checker().fatal()
                .verifyTrue(String.format("New assay name '%s' is not in the assay list in project '%s'. Fatal error.", newAssayName, otherProject),
                        actualValues.contains(newAssayName));

        assayBeginPage.clickAssay(newAssayName);

        checker().withScreenshot()
                .verifyTrue(String.format("Clicking assay name '%s' did not navigate as expected.", newAssayName),
                        waitFor(header::isDisplayed, 5_000));

        // Be a good corporate citizen and clean up after the test.
        log("Delete the extra project and the shared assay.");
        _assayHelper.deleteAssayDesign();
        _containerHelper.deleteProject(otherProject, false);

    }

    private void uploadAssayFile(File guavaFile, int fileNumber)
    {
        String fileLoc = "__primaryFile__";
        if (fileNumber > 0)
            fileLoc += fileNumber;
        setFormElement(Locator.name(fileLoc), guavaFile);
    }

    @LogMethod
    private void uploadAssayFiles(List<File> guavaFiles)
    {
        setInput(Locator.name("__primaryFile__"), guavaFiles);
    }

    private void addNewFile()
    {
        log("Clicking +  to add new file");
        click(Locator.byClass("labkey-file-add-icon-enabled"));
    }
}
