/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import org.apache.http.HttpStatus;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(DailyB.class)
public class InlineImagesAssayTest extends BaseWebDriverTest
{
    protected final static File XLS_FILE = TestFileUtils.getSampleData("InlineImages/foo.xls");
    protected final static File PNG01_FILE =  TestFileUtils.getSampleData("InlineImages/crest.png");
    protected final static File LRG_PNG_FILE = TestFileUtils.getSampleData("InlineImages/screenshot.png");
    protected final static File HELP_JPG_FILE = TestFileUtils.getSampleData("InlineImages/help.jpg");

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
    public final void testAssayInlineImages() throws Exception
    {
        String assayName = "InlineImageTest";
        String runName = "inlineImageRun01";
        String importData = 
                "Specimen ID\tParticipant ID\tVisit ID\tDate\tData File Field\n" +
                "100\t1A2B\t3\t\t" + LRG_PNG_FILE.getName() + "\n" +
                "101\t2A2B\t3\n" +
                "102\t3A2B\t3";

        log("Create an Assay.");
        AssayDesignerPage assayDesigner = _assayHelper.createAssayAndEdit("General", assayName);

        log("Mark the assay as editable.");
        assayDesigner.setEditableRuns(true);
        assayDesigner.setEditableResults(true);
        assayDesigner.addBatchField("BatchFileField", "Batch File Field", FieldDefinition.ColumnType.File);
        assayDesigner.addRunField("RunFileField", "Run File Field", FieldDefinition.ColumnType.File);
        assayDesigner.addDataField("DataFileField", "Data File Field", FieldDefinition.ColumnType.File);
        assayDesigner.saveAndClose();

        log("upload inline files to the pipeline root");
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(LRG_PNG_FILE);

        goToManageAssays();
        log("Populate the assay with data.");
        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        setFormElement(Locator.name("batchFileField"), XLS_FILE);
        clickButton("Next");
        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);

        clickButton("Save and Finish");

        log("Verify link to attached file and icon is present as expected.");
        assertElementPresent("Did not find link to file " + XLS_FILE.getName() + " in grid.", Locator.xpath("//a[contains(text(), '" + XLS_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find expected file icon in grid.", Locator.xpath("//a[contains(text(), 'foo.xls')]//img[contains(@src, 'xls.gif')]"), 1);

        log("Set the 'File' column on the runs.");

        clickAndWait(Locator.linkWithText("view runs"));
        new DataRegionTable("Runs", getDriver()).clickEditRow(0);

        setFormElement(Locator.name("quf_RunFileField"), PNG01_FILE);
        clickButton("Submit");
        waitForElement(DataRegionTable.updateLinkLocator()); // Wait to make sure the grid has been renedered.

        log("Verify inline image is present as expected.");
        assertElementPresent("Did not find expected inline image for " + PNG01_FILE.getName() + " in grid.", Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"), 1);

        log("Hover over the thumbnail and make sure the pop-up is as expected.");
        mouseOver(Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"));
        shortWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#helpDiv")));
        String src = Locator.xpath("//div[@id='helpDiv']//img[contains(@src, 'downloadFileLink')]").findElement(getDriver()).getAttribute("src");
        assertEquals("Bad response from run field pop-up", HttpStatus.SC_OK, WebTestHelper.getHttpResponse(src).getResponseCode());

        // Not going to try and download the file as part of the automation, although that could be added if wanted int he future.

        log("View the results grid.");
        clickAndWait(Locator.linkWithText("view results"));

        log("Verify that the correct number of file fields are populated as expected.");
        assertElementPresent("Did not find the expected number of links for the file " + XLS_FILE.getName(), Locator.xpath("//a[contains(text(), '" + XLS_FILE.getName() + "')]"), 3);
        assertElementPresent("Did not find the expected number of icons for images for " + PNG01_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"), 3);
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"), 1);

        log("Add the image to one of the result's 'File' column.");
        List<WebElement> editLinks = DataRegionTable.updateLinkLocator().findElements(getDriver());
        clickAndWait(editLinks.get(2));

        setFormElement(Locator.name("quf_DataFileField"), HELP_JPG_FILE);
        clickButton("Submit");
        waitForElement(DataRegionTable.updateLinkLocator()); // Wait to make sure the grid has been rendered.

        log("Validate that two links to this image file are now present.");
        assertElementPresent("Did not find the expected number of icons for images for " + PNG01_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"), 3);
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + HELP_JPG_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + HELP_JPG_FILE.getName() + "')]"), 1);

        log("Export the grid to excel.");
        File exportedFile;
        Workbook workbook;
        DataRegionTable list;
        DataRegionExportHelper exportHelper;

        list = new DataRegionTable("Data", getDriver());
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        workbook = ExcelHelper.create(exportedFile);

        validateExcelExport(exportedFile, workbook);

        log("Validate that the 'File' (last) column is as expected.");
        assertEquals("Values in 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("Batch File Field", "assaydata" + File.separator + XLS_FILE.getName(), "assaydata" + File.separator + XLS_FILE.getName(), "assaydata" + File.separator + XLS_FILE.getName()),
                ExcelHelper.getColumnData(workbook.getSheetAt(workbook.getActiveSheetIndex()), 7));


        log("Remove the 'File' (last) column from the batch and see that things still work.");

        assayDesigner = _assayHelper.clickEditAssayDesign();
        assayDesigner.batchFields().selectField("BatchFileField").markForDeletion();
        assayDesigner.saveAndClose();
        waitAndClickAndWait(Locator.linkWithText("view results"));

        log("Verify that the file fields from the batch are no longer present.");
        assertElementPresent("Found a link to file " + XLS_FILE.getName() + " in grid, it should not be there.", Locator.xpath("//a[contains(text(), '" + XLS_FILE.getName() + "')]"), 0);
        assertElementPresent("Found a file icon in grid, it should not be there.", Locator.xpath("//a[contains(text(), 'foo.xls')]//img[contains(@src, 'xls.gif')]"), 0);

        log("Verify that the other 'File' fields are not affected.");
        assertElementPresent("Did not find the expected number of icons for images for " + PNG01_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"), 3);
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + HELP_JPG_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + HELP_JPG_FILE.getName() + "')]"), 1);


        log("Export the grid to excel again and make sure that everything is as expected.");
        list = new DataRegionTable("Data", getDriver());
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        workbook = ExcelHelper.create(exportedFile);

        validateExcelExport(exportedFile, workbook);

        log("Validate that the removed column no longer shows up in the export.");
        List<String> exportedHeaders = ExcelHelper.getRowData(workbook.getSheetAt(workbook.getActiveSheetIndex()), 0);
        assertFalse("Value of removed 'File' column not exported as expected [" + exportedFile.getName() + "]",
                exportedHeaders.contains("Batch File Field"));
    }

    private void validateExcelExport(File exportedFile, Workbook workbook)
    {
        Sheet sheet = workbook.getSheetAt(workbook.getActiveSheetIndex());

        log("Validate number of rows exported");
        assertEquals("Wrong number of rows exported to " + exportedFile.getName(), 3, sheet.getLastRowNum());

        log("Validate that the 'Pictures' collection in excel has the correct number of images/pictures.");
        assertEquals("Number of pictures in excel collection not as expected.", 5, workbook.getAllPictures().size());

        final int COLUMN_WIDTH_LARGE_LBOUND = 11100;
        final int COLUMN_WIDTH_LARGE_UBOUND = 11500;
        final int COLUMN_WIDTH_SMALL_LBOUND = 3700;
        final int COLUMN_WIDTH_SMALL_UBOUND = 3940;
        final int IMAGE_COL_01 = 4;
        final int IMAGE_COL_02 = 5;

        log("Look at the cell sizes and use that as an indication the image is rendered on the sheet.");
        assertTrue("Column '" + sheet.getRow(0).getCell(IMAGE_COL_01).getStringCellValue() + "' width not in expected range (" + COLUMN_WIDTH_LARGE_LBOUND + " to " + COLUMN_WIDTH_LARGE_UBOUND + "). Actual width: " + sheet.getColumnWidth(IMAGE_COL_01), (sheet.getColumnWidth(IMAGE_COL_01) > COLUMN_WIDTH_LARGE_LBOUND) && (sheet.getColumnWidth(IMAGE_COL_01) < COLUMN_WIDTH_LARGE_UBOUND));
        assertTrue("Column '" + sheet.getRow(0).getCell(IMAGE_COL_02).getStringCellValue() + "' width not in expected range (" + COLUMN_WIDTH_SMALL_LBOUND + " to " + COLUMN_WIDTH_SMALL_UBOUND + "). Actual width: " + sheet.getColumnWidth(IMAGE_COL_02), (sheet.getColumnWidth(IMAGE_COL_02) > COLUMN_WIDTH_SMALL_LBOUND) && (sheet.getColumnWidth(IMAGE_COL_02) < COLUMN_WIDTH_SMALL_UBOUND));

        for (int j=0; j <= sheet.getLastRowNum(); j++)
        {
            log("Row " + j + " height: " + sheet.getRow(j).getHeight());
        }

        final int ROW_HEIGHT_LARGE_LBOUND = 6500;
        final int ROW_HEIGHT_LARGE_UBOUND = 6800;
        final int ROW_HEIGHT_SMALL_LBOUND = 800;
        final int ROW_HEIGHT_SMALL_UBOUND = 1000;
        assertTrue("Height of row 1 not in expected range (" + ROW_HEIGHT_LARGE_LBOUND + " to " + ROW_HEIGHT_LARGE_UBOUND + "). Actual height: " + sheet.getRow(1).getHeight(), (sheet.getRow(1).getHeight() > ROW_HEIGHT_LARGE_LBOUND) && (sheet.getRow(1).getHeight() < ROW_HEIGHT_LARGE_UBOUND));
        assertTrue("Height of row 2 not in expected range (" + ROW_HEIGHT_SMALL_LBOUND + " to " + ROW_HEIGHT_SMALL_UBOUND + "). Actual height: " + sheet.getRow(2).getHeight(), (sheet.getRow(2).getHeight() > ROW_HEIGHT_SMALL_LBOUND) && (sheet.getRow(2).getHeight() < ROW_HEIGHT_SMALL_UBOUND));
        assertTrue("Height of row 3 not in expected range (" + ROW_HEIGHT_SMALL_LBOUND + " to " + ROW_HEIGHT_SMALL_UBOUND + "). Actual height: " + sheet.getRow(3).getHeight(), (sheet.getRow(3).getHeight() > ROW_HEIGHT_SMALL_LBOUND) && (sheet.getRow(3).getHeight() < ROW_HEIGHT_SMALL_UBOUND));

        log("Validate that the value for the file columns is as expected.");
        List<String> exportedColumn = ExcelHelper.getColumnData(sheet, 4);
        assertEquals("Values in 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("Data File Field", LRG_PNG_FILE.getName(), "", HELP_JPG_FILE.getName()),
                exportedColumn);

        exportedColumn = ExcelHelper.getColumnData(sheet, 5);
        assertEquals("Values in 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("Run File Field", "assaydata" + File.separator + PNG01_FILE.getName(), "assaydata" + File.separator + PNG01_FILE.getName(), "assaydata" + File.separator + PNG01_FILE.getName()),
                exportedColumn);

    }
}
