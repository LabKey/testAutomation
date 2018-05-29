/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.pages.assay.ExclusionConfirmationPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

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
    private static final String GPAT_ASSAY_FNA = "trial03.fna";
    private static final String ASSAY_NAME_XLS = "XLS Assay";
    private static final String ASSAY_NAME_XLSX = "XLSX Assay";
    private static final String ASSAY_NAME_TSV = "TSV Assay";
    private static final String ASSAY_NAME_FNA = "FASTA Assay";


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
        _containerHelper.createProject(getProjectName(), "Assay");
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Pipeline Files");
        setPipelineRoot(TestFileUtils.getLabKeyRoot() + "/sampledata/GPAT");
        clickProject(getProjectName());

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

        verifyAssayDataExclusions();

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

        log("Import FASTA GPAT assay");
        clickProject(getProjectName());
        _fileBrowserHelper.importFile(GPAT_ASSAY_FNA, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_FNA);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);

        clickButton("Begin import");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_FNA));

        waitForText("Sequence");
        assertTextPresent(
                "Header", "HCJDRSZ07IVO6P", "HCJDRSZ07IL1GX", "HCJDRSZ07H5SPZ",
                "CACCAGACAGGTGTTATGGTGTGTGCCTGTAATCCCAGCTACTTGGGAGGGAGCTCAGGT");
    }

    private void verifyAssayDataExclusions()
    {
        log("Verify assay data exclusion");
        DataRegionTable assayResultsTable = goToAssayResults(ASSAY_NAME_XLSX, GPAT_ASSAY_XLSX);
        log("Verify 'Exclude' button is disabled until row selection is made");
        WebElement excludeButton = assayResultsTable.getHeaderButton("Exclude");
        Assert.assertTrue("Exclude button not initially disabled", excludeButton.getAttribute("class").contains("disabled"));
        int rowsToExclude = 10;
        log("Exclude the first " + rowsToExclude + " data rows from for assay run " + ASSAY_NAME_XLSX + " " + GPAT_ASSAY_XLSX);
        for (int i = 0 ; i < rowsToExclude; i++)
            assayResultsTable.checkCheckbox(i);
        assayResultsTable.clickHeaderButton("Exclude");
        ExclusionConfirmationPage confirmPage = new ExclusionConfirmationPage(getDriver());
        String comment1 = "first exclusion of " + rowsToExclude + " rows";
        confirmPage.verifyCountAndSave(rowsToExclude, comment1);

        assertEquals("Exclusion redirected to wrong page", ASSAY_NAME_XLSX + " Results", getText(Locators.bodyTitle()));

        log("Verify exclusion highlight");
        Assert.assertEquals("Number of rows highlighted as excluded is not as expected",rowsToExclude, Locator.css(".labkey-error-row").findElements(getDriver()).size());

        int startInd = 5;
        int endInd = 15;
        log("Create another exclusion event with overlapping data rows. Exclude data row " + (startInd + 1) + " through row " + endInd);
        for (int i = startInd ; i < endInd; i++)
            assayResultsTable.checkCheckbox(i);
        assayResultsTable.clickHeaderButton("Exclude");
        String comment2 = "second exclusion of " + (endInd - startInd) + " rows";
        confirmPage.verifyCountAndSave(endInd - startInd, comment2);

        Locator.XPathLocator runFilterLoc = DataRegionTable.Locators.filterContextAction();
        String runFilter = runFilterLoc.findElement(getDriver()).getText();
        Assert.assertTrue("Filter context message is not as expected", runFilter.contains("Run "));

        Locator viewExclusionReportHeaderBtn = Locator.linkWithText("view excluded data");

        log("Go to Exclusion Report");
        clickAndWait(viewExclusionReportHeaderBtn);
        DataRegionTable exclusionReportTable = new DataRegionTable.DataRegionFinder(getDriver()).find();

        log("Verify Exclusion Report is filtered down to Run that the link is coming from");
        assertElementPresent(runFilterLoc.withText(runFilter));

        log("Verify Exclusion Report columns and content");
        Assert.assertEquals("Exclusion report data row count is not as expected", endInd, exclusionReportTable.getDataRowCount());
        List<String> expectedColumns = Arrays.asList("Additive", "Run", "Excluded By", "Excluded At", "Exclusion Comment", "Row Id");
        List<String> columns = exclusionReportTable.getColumnLabels();
        for (String expected : expectedColumns)
            Assert.assertTrue("Column is missing: " + expected, columns.contains(expected));
        List<String> rowContent = exclusionReportTable.getRowDataAsText(0, "Additive", "Run", "Exclusion Comment", "Row Id");
        Assert.assertEquals("Row 1 content not as expected", Arrays.asList("ACD", GPAT_ASSAY_XLSX, comment1, "1"), rowContent);
        rowContent = exclusionReportTable.getRowDataAsText(startInd, "Additive", "Run", "Exclusion Comment", "Row Id");
        Assert.assertEquals("Row " + (startInd + 1) + " content not as expected", Arrays.asList("ACD", GPAT_ASSAY_XLSX, comment2, String.valueOf(startInd + 1)), rowContent);

        log("Verify Exclusion Report link from runs view");
        goToAssayHome(ASSAY_NAME_XLSX);
        clickAndWait(Locator.linkWithText("view runs"));
        clickAndWait(viewExclusionReportHeaderBtn);
        assertElementNotPresent(runFilterLoc.withText(runFilter));

        log("Verify Exclusions Warning present on re import run with exclusions present");
        Locator exclusionWarningHeader = Locators.panelWebpartTitle.withText("Exclusions Warning");
        goToAssayResults(ASSAY_NAME_XLSX, GPAT_ASSAY_XLSX);
        assayResultsTable.clickHeaderButton("Re-import run");
        clickButton("Next");
        assertElementPresent(exclusionWarningHeader);
        assertTextPresent("The run you are replacing has " + endInd + " exclusions");

        log("Verify Exclusion Report is filtered down to Run from re-import run page");
        clickAndWait(Locator.tagWithClass("a", "labkey-text-link").withText("exclusions report"));
        assertElementPresent(runFilterLoc.withText(runFilter));

        log("Remove all exclusions");
        DataRegionTable exclusionTable = goToExclusionsTable();
        Assert.assertEquals("Exclusion record count is not as expected", 2, exclusionTable.getDataRowCount());
        exclusionTable.checkAll();
        exclusionTable.clickHeaderButton("Delete");
        assertAlert("Are you sure you want to delete the selected rows?");

        log("Verify ExclusionMaps are deleted with Exclusion");
        DataRegionTable exclusionMapTable = goToExclusionMapsTable();
        Assert.assertEquals("Exclusion map should have been cleared on exclusion deletion", 0, exclusionMapTable.getDataRowCount());
        Locator deleteBtn = Locator.tagWithAttribute("a", "data-original-title","Delete");
        assertElementNotPresent(deleteBtn);

        log("Verify Exclusions Warning is not present on re import run with 0 exclusions");
        goToAssayResults(ASSAY_NAME_XLSX, GPAT_ASSAY_XLSX);
        assayResultsTable.clickHeaderButton("Re-import run");
        clickButton("Next");
        assertElementNotPresent(exclusionWarningHeader);

        goToAdminConsole().clickAuditLog();
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("view"), "Assay/Experiment events"));
        DataRegionTable auditTable =  new DataRegionTable("query", getDriver());

        log("Verify audit log for adding and deleting exclusions");
        String[][] columnAndValues = new String[][] {
                    {"Created By", getDisplayName()},
                    {"Assay/Protocol", ASSAY_NAME_XLSX}, {"Run", GPAT_ASSAY_XLSX},
                    {"Comment", "10 data rows have been marked for exclusion for run '" + GPAT_ASSAY_XLSX +
                            "' with comment '" + comment1 + "'. DataRowId: 1,2"},
                };
        verifyAuditLog(auditTable, columnAndValues, 3);

        columnAndValues = new String[][] {
                {"Created By", getDisplayName()},
                {"Assay/Protocol", ASSAY_NAME_XLSX}, {"Run", GPAT_ASSAY_XLSX},
                {"Comment", "Exclusion event has been deleted from run 'trial01a.xlsx'"},
        };
        verifyAuditLog(auditTable, columnAndValues, 0);
    }

    private void verifyAuditLog(DataRegionTable auditTable, String[][] columnAndValues, int rowId)
    {
        for (String[] columnAndValue : columnAndValues)
        {
            Assert.assertTrue("Audit log is not as expected for " + columnAndValue[0],
                    auditTable.getDataAsText(rowId, columnAndValue[0]).contains(columnAndValue[1]));
        }
    }

    private DataRegionTable goToExclusionsTable()
    {
        beginAt(getProjectName() + "/query-executeQuery.view?schemaName=exp&query.queryName=Exclusions");
        return new DataRegionTable.DataRegionFinder(getDriver()).find();
    }

    private DataRegionTable goToExclusionMapsTable()
    {
        beginAt(getProjectName() + "/query-executeQuery.view?schemaName=exp&query.queryName=ExclusionMaps");
        return new DataRegionTable.DataRegionFinder(getDriver()).find();
    }

    private DataRegionTable goToAssayResults(String assayName, String runName)
    {
        goToAssayHome(assayName);
        clickAndWait(Locator.linkWithText("view runs"));
        waitAndClickAndWait(Locator.tagWithText("a", runName));
        return new DataRegionTable.DataRegionFinder(getDriver()).find();
    }

    private void goToAssayHome(String assayName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(assayName));
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

    /**
     * @deprecated Use {@link org.labkey.test.components.PropertiesEditor} from _assayHelper or _listHelper
     */
    @Deprecated
    public String getPropertyXPath(String propertyHeading)
    {
        return "//h3[text() = '" + propertyHeading + "']/../..";
    }
}
