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
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayBeginPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;
import org.openqa.selenium.WebElement;

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
    private static final File GPAT_ASSAY_FNA_1 = TestFileUtils.getSampleData("GPAT/trial03.fna");
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
        ReactAssayDesignerPage assayDesignerPage = startCreateGpatAssay(GPAT_ASSAY_XLS, ASSAY_NAME_XLS);
        DomainFormPanel results = setAssayResultsProperties(assayDesignerPage, 12);
        results.removeField("Role");
        assayDesignerPage.clickFinish();
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLS.getName()));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.tagWithClass("*", "labkey-column-header").containing("Role")); // removed column

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
        assayDesignerPage = startCreateGpatAssay(GPAT_ASSAY_XLSX, ASSAY_NAME_XLSX);
        setAssayResultsProperties(assayDesignerPage, 11);
        assayDesignerPage.clickFinish();
        if (isElementPresent(Locator.tagContainingText("p", "The files listed below have been created by another run")))
            clickButton("OK", defaultWaitForPage);
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLSX.getName()));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementPresent(Locator.css(".labkey-column-header").containing("Role"));

        log("Import TSV GPAT assay");
        clickProject(getProjectName());
        assayDesignerPage = startCreateGpatAssay(GPAT_ASSAY_TSV, ASSAY_NAME_TSV);
        results = assayDesignerPage.expandFieldsPanel("Results");
        results.getField("Primary")
                .setLabel("Blank")
                .setMissingValuesEnabled(true);
        results.getField("Score")
                .setName("Result")
                .setLabel("Result")
                .setImportAliases("Score")
                .setRequiredField(true);

        // Set the date-only field type.
        results.getField("DateOnly")
                .setType(FieldDefinition.ColumnType.Date, false);

        // Using a tsv abd the data-pipeline to define the results fields sets the time-only field to a type of Text.
        // A field of type Text cannot be converted to a Time type. The only way around this is to remove the field and
        // add it back as a time-only type.
        results.removeField("TimeOnly", false);
        results.addField(new FieldDefinition("TimeOnly", FieldDefinition.ColumnType.Time));
        assayDesignerPage.clickFinish();

        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_TSV.getName()));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementPresent(Locator.css(".labkey-column-header").containing("Blank"));
        assertElementPresent(Locator.css(".labkey-column-header").containing("Result"));

        importFastaGpatAssay(GPAT_ASSAY_FNA_1, ASSAY_NAME_FNA);
        log("Verify data after the GPAT assay upload");
        clickAndWait(Locator.linkWithText(GPAT_ASSAY_FNA_1.getName()));
        waitForText("Sequence");
        assertTextPresent(
                "Header", "HCJDRSZ07IVO6P", "HCJDRSZ07IL1GX", "HCJDRSZ07H5SPZ",
                "CACCAGACAGGTGTTATGGTGTGTGCCTGTAATCCCAGCTACTTGGGAGGGAGCTCAGGT");
    }

    private DomainFormPanel setAssayResultsProperties(ReactAssayDesignerPage assayDesignerPage, int fieldCount)
    {
        DomainFormPanel results = assayDesignerPage.expandFieldsPanel("Results");
        checker().fatal().verifyEquals("Results fields count not as expected", fieldCount + " Fields Defined",
                results.getFieldCountMessage());
        results.getField("Score").setRequiredField(true);
        results.getField("Primary").setMissingValuesEnabled(true);

        // Set the date-only and time-only result fields to the proper type.
        if(results.fieldNames().contains("DateOnly") && results.fieldNames().contains("TimeOnly"))
        {
            results.getField("DateOnly").setType(FieldDefinition.ColumnType.Date, false);
            results.getField("TimeOnly").setType(FieldDefinition.ColumnType.Time, false);
        }

        return results;
    }

    @LogMethod
    private void importFastaGpatAssay(File fnaFile, String assayName)
    {
        ReactAssayDesignerPage assayDesignerPage = startCreateGpatAssay(fnaFile, assayName);
        assayDesignerPage.clickFinish();
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
    }

    @LogMethod
    private ReactAssayDesignerPage startCreateGpatAssay(File dataFile, @LoggedParam String assayName)
    {
        log("Create GPAT assay from " + dataFile.getName());
        new WebDavUploadHelper(getProjectName()).uploadFile(dataFile);
        beginAt(WebTestHelper.buildURL("pipeline", getProjectName(), "browse"));
        _fileBrowserHelper.importFile(dataFile.getName(), "Create New Standard Assay Design");

        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());

        if (assayName != null)
            assayDesignerPage.setName(assayName);
        return assayDesignerPage;
    }

    @Test
    public void testMultipleFileUploadInAssayRun()
    {
        File file1 = TestFileUtils.getSampleData("GPAT/trial01a.xlsx");
        File file2 = TestFileUtils.getSampleData("GPAT/trial01b.xlsx");
        File file3 = TestFileUtils.getSampleData("GPAT/trial01c.xlsx");
        String fileName = "trial01b";

        importFastaGpatAssay(file1, ASSAY_NAME_FNA_MULTIPLE);
        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME_FNA_MULTIPLE));
        clickButton("Import Data");
        clickButton("Next");

        log("Check radio button for multiple upload");
        checkRadioButton(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));

        uploadAssayFile(file2, 0);
        addNewFile();
        uploadAssayFile(file3, 1);

        clickButton("Save and Finish");

        log("Verifying the upload");
        clickAndWait(Locator.linkContainingText(fileName));
        DataRegionTable table = new DataRegionTable("Data", getDriver());
        table.assertPaginationText(1, 100, 402);

    }

    // this test only works in Chrome
    @Test
    public void testMultipleFileUploadSingleRowInAssayRun()
    {
        File file1 = TestFileUtils.getSampleData("GPAT/trial01b.xlsx");
        File file2 = TestFileUtils.getSampleData("GPAT/trial01a.xlsx");
        File file3 = TestFileUtils.getSampleData("GPAT/trial01c.xlsx");
        List<File> files = new ArrayList<>();
        files.add(file2);
        files.add(file3);

        String fileName = "trial01a";

        importFastaGpatAssay(file1, ASSAY_NAME_FNA_MULTIPLE_SINGLE_INPUT);
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
        table.assertPaginationText(1, 100, 402);

    }

    @Test
    public void testRenameAssayDesign()
    {
        File trialData = TestFileUtils.getSampleData("GPAT/renameAssayTrial.xls");

        String originalAssayName = "A Assay Name";
        log(String.format("Create an assay named '%s'.", originalAssayName));
        ReactAssayDesignerPage assayDesignerPage = startCreateGpatAssay(trialData, originalAssayName);
        setAssayResultsProperties(assayDesignerPage, 10);
        assayDesignerPage.clickFinish();

        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);

        String newAssayName = "Updated Assay Name";
        log(String.format("Edit the assay design and rename it to '%s'.", newAssayName));


        assayDesignerPage = _assayHelper.clickEditAssayDesign(false);
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
