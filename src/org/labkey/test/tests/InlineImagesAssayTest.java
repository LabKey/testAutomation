/*
 * Copyright (c) 2011-2015 LabKey Corporation
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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDomainEditor;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(DailyB.class)
public class InlineImagesAssayTest extends BaseWebDriverTest
{
    protected DataRegionTable dataRegion;

    protected final String SAMPLE_DATA_LOC =  "/sampledata/InlineImages/";
    protected final String XLS_FILE = "foo.xls";
    protected final String PNG01_FILE = "crest.png";
    protected final String LRG_PNG_FILE = "screenshot.png";
    protected final String JPG01_FILE = "help.jpg";

    protected DataRegionExportHelper exportHelper;


    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return "InlineImagesAssayTestProject";
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

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @BeforeClass
    public static void initTest()
    {
        InlineImagesAssayTest init = (InlineImagesAssayTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        _containerHelper.createProject(getProjectName(), "Assay");
    }

    @Test
    public final void AssayTest()
    {

        String assayName = "InlineImageTest";
        String runName = "inlineImageRun01";
        String importData = "Specimen ID\tParticipant ID\tVisit ID\tDate\tData File Field\n" +
                "100\t1A2B\t3\t\t" + LRG_PNG_FILE + "\n" +
                "101\t2A2B\t3\n" +
                "102\t3A2B\t3";

        DataRegionTable list;
        DataRegionExportHelper exportHelper;
        File exportedFile;
        Workbook workbook;
        Sheet sheet;
        List<String> exportedColumn;

        String filePath;

        log("Create an Assay.");

        AssayDomainEditor assayDesigner = _assayHelper.createAssayAndEdit("General", assayName);

        log("Mark the assay as editable.");
        assayDesigner.setEditableRuns(true);
        assayDesigner.setEditableResults(true);

        log("Create a 'File' column for the assay batch.");
        assayDesigner.addBatchField("BatchFileField", "Batch File Field", "File");

        log("Create a 'File' column for the assay run.");
        assayDesigner.addRunField("RunFileField", "Run File Field", "File");

        log("Create a 'File' column for the assay data.");
        assayDesigner.addDataField("DataFileField", "Data File Field", "File");

        log("Save the changes.");
        assayDesigner.save();
        assayDesigner.saveAndClose();
        sleep(1000);

        log("Populate the assay with data.");
        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");

        log("Set the 'File' column on the batch grid.");

        filePath = TestFileUtils.getLabKeyRoot() + SAMPLE_DATA_LOC + XLS_FILE;
        assertFileExist(filePath);
        log("Going to attach file: " + filePath);

        setFormElement(Locator.css("#ExperimentRun > table > tbody > tr:nth-child(3) > td:nth-child(2) > input[type=\"file\"]"), filePath);
        clickButton("Next", 5000);
        waitForText("Run Data *");  // Make sure the page has loaded completely.

        log("Paste tab separated values for the assay data.");
        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);

        clickButton("Save and Finish");

        // Note the translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'... converts the attribute value to lower case.
        // This is just to make the test more robust when checking values that contain file names.
        log("Verify link to attached file and icon is present as expected.");
        assertElementPresent("Did not find link to file " + XLS_FILE + " in grid.", Locator.xpath("//a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + XLS_FILE + "')]"), 1);
        assertElementPresent("Did not find expected file icon in grid.", Locator.xpath("//a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'foo.xls')]//img[contains(translate(@src, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'xls.gif')]"), 1);

        log("Set the 'File' column on the runs.");

        filePath = TestFileUtils.getLabKeyRoot() + SAMPLE_DATA_LOC + PNG01_FILE;
        assertFileExist(filePath);
        log("Going to attach file: " + filePath);

        click(Locator.linkWithText("view runs"));
        click(Locator.linkWithText("edit"));

        setFormElement(Locator.css("#Runs > table > tbody > tr:nth-child(4) > td:nth-child(2) > input[type=\"file\"]"), filePath);
        clickButton("Submit", 5000);
        waitForElement(Locator.linkWithText("edit")); // Wait to make sure the grid has been renedered.

        log("Verify inline image is present as expected.");
        assertElementPresent("Did not find expected inline image for "+ PNG01_FILE + " in grid.", Locator.xpath("//img[contains(translate(@title, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + PNG01_FILE + "')]"), 1);

        log("Hover over the thumbnail and make sure the pop-up is as expected.");
        mouseOver(Locator.xpath("//img[contains(translate(@title, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + PNG01_FILE + "')]"));
        sleep(5000);
        assertElementVisible(Locator.css("#helpDiv"));
        assertElementPresent("Download image is not as expected.", Locator.xpath("//div[@id='helpDiv']//img[contains(@src, '" + WebTestHelper.buildURL("core", getProjectName(), "downloadFileLink") + "')]"), 1);

        // Not going to try and download the file as part of the automaiton, although that could be added if wanted int he future.

        log("View the results grid.");
        click(Locator.linkWithText("view results"));

        log("Verify that the correct number of file fields are populated as expected.");
        assertElementPresent("Did not find the expected number of links for the file " + XLS_FILE, Locator.xpath("//a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + XLS_FILE + "')]"), 3);
        assertElementPresent("Did not find the expected number of icons for images for " + PNG01_FILE + " from the runs.", Locator.xpath("//img[contains(translate(@title, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + PNG01_FILE + "')]"), 3);

        log("Verify that the reference to the png that was in the data used for populating is listed as unavailable.");
        assertTextPresent(LRG_PNG_FILE + " (unavailable)");

        log("Add the image to one of the result's 'File' column.");

        filePath = TestFileUtils.getLabKeyRoot() + SAMPLE_DATA_LOC + LRG_PNG_FILE;
        assertFileExist(filePath);
        log("Going to attach file: " + filePath);

        List<WebElement> editLinks = Locator.linkWithText("edit").findElements(getDriver());
        editLinks.get(2).click();

        setFormElement(Locator.css("#Data > table > tbody > tr:nth-child(5) > td:nth-child(2) > input[type=\"file\"]"), filePath);
        clickButton("Submit", 5000);
        waitForElement(Locator.linkWithText("edit")); // Wait to make sure the grid has been renedered.

        log("Validate that two links to this image file are now present.");
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE + " from the runs.", Locator.xpath("//img[contains(translate(@title, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + LRG_PNG_FILE + "')]"), 2);

        log("Export the grid to excel.");
        list = new DataRegionTable("Data", this);
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        try{

            workbook = ExcelHelper.create(exportedFile);
            sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), 3, sheet.getLastRowNum());

            log("Validate that the value for the file columns is as expected.");
            exportedColumn = ExcelHelper.getColumnData(sheet, 4);
            assertTrue("Value of 'File' column for the first row in the data grid not exported as expected.", exportedColumn.get(1).trim().toLowerCase().contains(LRG_PNG_FILE));
            assertTrue("Value of 'File' column for the second row in the data grid not exported as expected.", exportedColumn.get(2).trim().equals(""));
            assertTrue("Value of 'File' column for the last row in the data grid not exported as expected.", exportedColumn.get(3).trim().toLowerCase().contains(LRG_PNG_FILE));

            exportedColumn = ExcelHelper.getColumnData(sheet, 5);
            assertTrue("Value of 'File' column for the first row in the run grid not exported as expected.", exportedColumn.get(1).trim().toLowerCase().contains(PNG01_FILE));
            assertTrue("Value of 'File' column for the second row in the run grid not exported as expected.", exportedColumn.get(2).trim().toLowerCase().contains(PNG01_FILE));
            assertTrue("Value of 'File' column for the last row in the run grid not exported as expected.", exportedColumn.get(3).trim().toLowerCase().contains(PNG01_FILE));

            exportedColumn = ExcelHelper.getColumnData(sheet, 7);
            assertTrue("Value of 'File' column for the first row in the batch grid not exported as expected.", exportedColumn.get(1).trim().toLowerCase().contains(XLS_FILE));
            assertTrue("Value of 'File' column for the second row in the batch grid not exported as expected.", exportedColumn.get(2).trim().toLowerCase().contains(XLS_FILE));
            assertTrue("Value of 'File' column for the last row in the batch grid not exported as expected.", exportedColumn.get(3).trim().toLowerCase().contains(XLS_FILE));

        }
        catch (IOException | InvalidFormatException e)
        {
            throw new RuntimeException(e);
        }

        log("Remove the 'File' column from the batch and see that things still work.");

        assayDesigner = _assayHelper.clickEditAssayDesign();
        assayDesigner.removeBatchField("BatchFileField");
        assayDesigner.saveAndClose();
        waitForElement(Locator.linkWithText("view results")); // Make sure page has loaded completely.
        click(Locator.linkWithText("view results"));

        log("Verify that the file fields from the batch are no longer present.");
        assertElementPresent("Found a link to file " + XLS_FILE + " in grid, it should not be there.", Locator.xpath("//a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + XLS_FILE + "')]"), 0);
        assertElementPresent("Found a file icon in grid, it should not be there.", Locator.xpath("//a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'foo.xls')]//img[contains(translate(@src, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'xls.gif')]"), 0);

        log("Verify that the other 'File' fields are not affected.");
        assertElementPresent("Did not find the expected number of icons for images for " + PNG01_FILE + " from the runs.", Locator.xpath("//img[contains(translate(@title, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + PNG01_FILE + "')]"), 3);
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE + " from the runs.", Locator.xpath("//img[contains(translate(@title, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + LRG_PNG_FILE + "')]"), 2);


        log("Export the grid to excel again and make sure that everything is as expected.");
        list = new DataRegionTable("Data", this);
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        try{

            workbook = ExcelHelper.create(exportedFile);
            sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), 3, sheet.getLastRowNum());

            log("Validate that the value for the file columns is as expected.");
            exportedColumn = ExcelHelper.getColumnData(sheet, 4);
            assertTrue("Value of 'File' column for the first row in the data grid not exported as expected.", exportedColumn.get(1).trim().toLowerCase().contains(LRG_PNG_FILE));
            assertTrue("Value of 'File' column for the second row in the data grid not exported as expected.", exportedColumn.get(2).trim().equals(""));
            assertTrue("Value of 'File' column for the last row in the data grid not exported as expected.", exportedColumn.get(3).trim().toLowerCase().contains(LRG_PNG_FILE));

            exportedColumn = ExcelHelper.getColumnData(sheet, 5);
            assertTrue("Value of 'File' column for the first row in the run grid not exported as expected.", exportedColumn.get(1).trim().toLowerCase().contains(PNG01_FILE));
            assertTrue("Value of 'File' column for the second row in the run grid not exported as expected.", exportedColumn.get(2).trim().toLowerCase().contains(PNG01_FILE));
            assertTrue("Value of 'File' column for the last row in the run grid not exported as expected.", exportedColumn.get(3).trim().toLowerCase().contains(PNG01_FILE));

            exportedColumn = ExcelHelper.getColumnData(sheet, 7);
            assertTrue("Value of the removed 'File' column for the first row in the batch grid not exported as expected.", exportedColumn.get(1).trim().equals(""));
            assertTrue("Value of the removed 'File' column for the second row in the batch grid not exported as expected.", exportedColumn.get(2).trim().equals(""));
            assertTrue("Value of the removed 'File' column for the last row in the batch grid not exported as expected.", exportedColumn.get(3).trim().equals(""));

        }
        catch (IOException | InvalidFormatException e)
        {
            throw new RuntimeException(e);
        }

    }

    private void assertFileExist(String filePath)
    {
        File imgFile;
        imgFile = new File(filePath);
        Assert.assertTrue("File " + filePath + " can not be found.", imgFile.exists());
    }
}
