/*
 * Copyright (c) 2011-2018 LabKey Corporation
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
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({Assays.class, DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class GpatAssayTest extends BaseWebDriverTest
{
    private static final String GPAT_ASSAY_XLS = "trial01.xls";
    private static final String GPAT_ASSAY_XLSX = "trial01a.xlsx";
    private static final String GPAT_ASSAY_TSV = "trial02.tsv";
    private static final String ALIASED_ASSAY_1 = "trial01columns1.tsv";
    private static final String ALIASED_ASSAY_2 = "trial01columns2.tsv";
    private static final String ALIASED_ASSAY_3 = "trial01columns3.tsv";
    private static final String ALIASED_ASSAY_4 = "trial01columns4.tsv";
    private static final String GPAT_ASSAY_FNA_1 = "trial03.fna";
    private static final String GPAT_ASSAY_FNA_2 = "trial04.fna";
    private static final String GPAT_ASSAY_FNA_3 = "trial05.fna";
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
        PortalHelper portalHelper = new PortalHelper(init.getDriver());
        portalHelper.addWebPart("Pipeline Files");
        init.setPipelineRoot(TestFileUtils.getLabKeyRoot() + "/sampledata/GPAT");
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
        goToProjectHome();
        log("Import XLS GPAT assay");
        _fileBrowserHelper.importFile(GPAT_ASSAY_XLS, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_XLS);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        waitForGwtDialog("Score Column Properties");
        clickGwtTab("Validators");
        checkCheckbox(Locator.checkboxByName("required"));
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        waitForGwtDialog("Primary Column Properties");
        clickGwtTab("Advanced");
        checkCheckbox(Locator.checkboxByName("mvEnabled"));
        clickButton("OK", 0);
        assertEquals("SpecimenID", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("VisitID", getFormElement(Locator.name("VisitID")));
        assertEquals("DrawDt", getFormElement(Locator.name("Date")));
        clickButton("Begin import");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLS));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.tagWithClass("*", "labkey-column-header").withText("Role")); // excluded column

        try
        {
            // Issue 36077: SelectRows: SchemaKey decoding of public schema name causes request failure
            Connection cn = createDefaultConnection(false);
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
        clickProject(getProjectName());
        _fileBrowserHelper.importFile(GPAT_ASSAY_XLSX, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_XLSX);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        waitForGwtDialog("Score Column Properties");
        clickGwtTab("Validators");
        checkCheckbox(Locator.checkboxByName("required"));
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        waitForGwtDialog("Primary Column Properties");
        clickGwtTab("Advanced");
        checkCheckbox(Locator.checkboxByName("mvEnabled"));
        clickButton("OK", 0);
        assertEquals("SpecimenID", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("VisitID", getFormElement(Locator.name("VisitID")));
        assertEquals("DrawDt", getFormElement(Locator.name("Date")));
        clickButton("Begin import");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLSX));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Import TSV GPAT assay");
        clickProject(getProjectName());
        _fileBrowserHelper.importFile(GPAT_ASSAY_TSV, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_TSV);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        waitForGwtDialog("Score Column Properties");
        clickGwtTab("Validators");
        checkCheckbox(Locator.checkboxByName("required"));
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        waitForGwtDialog("Primary Column Properties");
        clickGwtTab("Advanced");
        checkCheckbox(Locator.checkboxByName("mvEnabled"));
        clickButton("OK", 0);
        assertEquals("SpecimenID", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("VisitID", getFormElement(Locator.name("VisitID")));
        assertEquals("DrawDt", getFormElement(Locator.name("Date")));
        clickButton("Show Assay Designer");

        AssayDesignerPage assayDesignerPage = new AssayDesignerPage(getDriver());
        assayDesignerPage.dataFields().selectField(4)
                .setLabel("Blank");
        assayDesignerPage.dataFields().selectField(7)
                .setName("Result")
                .setLabel("Result")
                .properties()
                .selectAdvancedTab()
                .importAliasesInput.set("Score");

        assayDesignerPage.saveAndClose();
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_TSV));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Verify standard column aliases");
        clickProject(getProjectName());
        _fileBrowserHelper.importFile(ALIASED_ASSAY_1, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        assertEquals("specId", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ParticipantID", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitNo", getFormElement(Locator.name("VisitID")));
        assertEquals("draw_date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_2, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        assertEquals("vialId1", getFormElement(Locator.name("SpecimenID")));
        assertEquals(null, "ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visit_no", getFormElement(Locator.name("VisitID")));
        assertEquals("drawDate", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_3, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        assertEquals("vialId", getFormElement(Locator.name("SpecimenID")));
        assertEquals(null, "ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitId", getFormElement(Locator.name("VisitID")));
        assertEquals("date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_4, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        assertEquals("guspec", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitId", getFormElement(Locator.name("VisitID")));
        assertEquals("date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");

        importFastaGpatAssay(GPAT_ASSAY_FNA_1, ASSAY_NAME_FNA);
        log("Verify data after the GPAT assay upload");
        clickAndWait(Locator.linkWithText(GPAT_ASSAY_FNA_1));
        waitForText("Sequence");
        assertTextPresent(
                "Header", "HCJDRSZ07IVO6P", "HCJDRSZ07IL1GX", "HCJDRSZ07H5SPZ",
                "CACCAGACAGGTGTTATGGTGTGTGCCTGTAATCCCAGCTACTTGGGAGGGAGCTCAGGT");
    }

    private void importFastaGpatAssay(String fileName, String assayName)
    {
        log("Import FASTA GPAT assay");
        goToProjectHome();
        _fileBrowserHelper.importFile(fileName, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), assayName);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);

        clickButton("Begin import");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
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

    private void uploadAssayFile(File guavaFile, int fileNumber)
    {
        String fileLoc = "__primaryFile__";
        if (fileNumber > 0)
            fileLoc += fileNumber;
        setFormElement(Locator.name(fileLoc), guavaFile);
    }

    private void uploadAssayFiles(List<File> guavaFiles)
    {
        setInput(Locator.name("__primaryFile__"), guavaFiles);
    }

    private void addNewFile()
    {
        log("Clicking +  to add new file");
        click(Locator.xpath("//a[contains(@class, 'labkey-file-add-icon-enabled')]"));
    }


    private void waitForGwtDialog(String caption)
    {
        waitForElement(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='" + caption + "']"), WAIT_FOR_JAVASCRIPT);
    }

    private void clickGwtTab(String tabName)
    {
        WebElement element = Locator.tagWithClass("div", "gwt-TabBarItem").withChild(Locator.xpath("//div[contains(@class, 'gwt-Label') and text()='" + tabName + "']")).findElement(getDriver());
        element.click();
    }
}
