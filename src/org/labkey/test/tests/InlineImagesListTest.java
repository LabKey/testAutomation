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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.ListHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(DailyB.class)
public class InlineImagesListTest extends BaseWebDriverTest
{
    protected final static String LIST_NAME = "InlineImagesList";
    protected final static String LIST_KEY_NAME = "Key";
    protected final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.Integer;

    protected final static String LIST_ATTACHMENT01_NAME = "Attachment01";
    protected final static String LIST_ATTACHMENT01_LABEL = "Attachment Column 01";
    protected final static String LIST_ATTACHMENT01_DESC = "An 1st attachment column.";

    protected final static String LIST_ATTACHMENT02_NAME = "Attachment02";
    protected final static String LIST_ATTACHMENT02_LABEL = "Attachment Column 02";
    protected final static String LIST_ATTACHMENT02_DESC = "An 2nd attachment column.";

    protected final static ListHelper.ListColumnType LIST_ATTACHMENT_TYPE = ListHelper.ListColumnType.Attachment;

    protected final static String LIST_DESC_COL_NAME = "Description";
    protected final static String LIST_DESC_COL_LABEL = "Description";
    protected final static String LIST_DESC_COL_DESC = "A simple description(text) field.";
    protected final static ListHelper.ListColumnType LIST_DESC_COL_TYPE = ListHelper.ListColumnType.String;

    protected final ListHelper.ListColumn _listColAttachment01 = new ListHelper.ListColumn(LIST_ATTACHMENT01_NAME, LIST_ATTACHMENT01_LABEL, LIST_ATTACHMENT_TYPE, LIST_ATTACHMENT01_DESC);
    protected final ListHelper.ListColumn _listColAttachment02 = new ListHelper.ListColumn(LIST_ATTACHMENT02_NAME, LIST_ATTACHMENT02_LABEL, LIST_ATTACHMENT_TYPE, LIST_ATTACHMENT02_DESC);
    protected final ListHelper.ListColumn _listColDescription = new ListHelper.ListColumn(LIST_DESC_COL_NAME, LIST_DESC_COL_LABEL, LIST_DESC_COL_TYPE, LIST_DESC_COL_DESC);

    protected final static File LRG_PNG_FILE = TestFileUtils.getSampleData("InlineImages/screenshot.png");
    protected final static File JPG01_FILE = TestFileUtils.getSampleData("InlineImages/help.jpg");
    protected final static File PDF_FILE =  TestFileUtils.getSampleData("InlineImages/agraph.pdf");

    protected final static int COLUMN_WIDTH_LARGE_LBOUND = 11100;
    protected final static int COLUMN_WIDTH_LARGE_UBOUND = 11500;
    protected final static int COLUMN_WIDTH_SMALL_LBOUND = 1900;
    protected final static int COLUMN_WIDTH_SMALL_UBOUND = 2200;
    protected final static int ROW_HEIGHT_LARGE_LBOUND = 6500;
    protected final static int ROW_HEIGHT_LARGE_UBOUND = 6800;
    protected final static int ROW_HEIGHT_SMALL_LBOUND = 450;
    protected final static int ROW_HEIGHT_SMALL_UBOUND = 510;
    protected final static int ROW_HEIGHT_TEXT_LBOUND = 200;
    protected final static int ROW_HEIGHT_TEXT_UBOUND = 300;

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @Override
    protected String getProjectName()
    {
        return "InlineImagesListTestProject";
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
        InlineImagesListTest init = (InlineImagesListTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Test
    public final void ListTest() throws Exception
    {
        DataRegionTable list;
        DataRegionExportHelper exportHelper;
        File exportedFile;
        Workbook workbook;
        Sheet sheet;
        List<String> exportedColumn;

        log("Create a list named: " + LIST_NAME);
        _listHelper.createList(getProjectName(), LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, _listColDescription, _listColAttachment01);
        clickAndWait(Locator.lkButton("Done"));

        Map<String, String> newValues = new HashMap<>();

        list=new DataRegionTable("query", getDriver());
        clickAndWait(Locator.linkWithText(LIST_NAME).waitForElement(list.getComponentElement(), WAIT_FOR_JAVASCRIPT));

        log("Add a \"large\" png as an attachment.");
        newValues.put(LIST_KEY_NAME, "1");
        newValues.put(LIST_DESC_COL_NAME, "Here is a simple png attachment.");
        newValues.put(LIST_ATTACHMENT01_NAME, LRG_PNG_FILE.getAbsolutePath());
        _listHelper.insertNewRow(newValues, false);

        newValues = new HashMap<>();

        log("Add a jpg as an attachment.");
        newValues.put(LIST_KEY_NAME, "2");
        newValues.put(LIST_DESC_COL_NAME, "Here is a simple jpg attachment.");
        newValues.put(LIST_ATTACHMENT01_NAME, JPG01_FILE.getAbsolutePath());
        _listHelper.insertNewRow(newValues, false);

        log("Add a pdf as an attachment.");
        newValues.put(LIST_KEY_NAME, "3");
        newValues.put(LIST_DESC_COL_NAME, "Here is a simple pdf attachment.");
        newValues.put(LIST_ATTACHMENT01_NAME, PDF_FILE.getAbsolutePath());
        _listHelper.insertNewRow(newValues, false);

        log("Validate that the correct number of images are present.");
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE.getName(), Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + JPG01_FILE.getName(), Locator.xpath("//img[contains(@title, '" + JPG01_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + PDF_FILE.getName(), Locator.xpath("//img[contains(@src, 'pdf.gif')]"), 1);
        assertElementPresent("Did not find the expected text for " + PDF_FILE.getName(), Locator.xpath("//a[contains(text(), '" + PDF_FILE.getName() + "')]"), 1);

        log("Add another attachment field.");
        clickAndWait(Locator.linkWithText("Design"));
        _listHelper.clickEditDesign();
        _listHelper.addField(_listColAttachment02);
        _listHelper.clickSave();
        clickAndWait(Locator.lkButton("Done"));

        log("Insert images and files into the new attachment rows.");
        newValues = new HashMap<>();
        newValues.put(LIST_ATTACHMENT02_NAME, PDF_FILE.getAbsolutePath());
        _listHelper.updateRow(1, newValues, false);

        newValues = new HashMap<>();
        newValues.put(LIST_ATTACHMENT02_NAME, LRG_PNG_FILE.getAbsolutePath());
        _listHelper.updateRow(2, newValues, false);

        newValues = new HashMap<>();
        newValues.put(LIST_ATTACHMENT02_NAME, JPG01_FILE.getAbsolutePath());
        _listHelper.updateRow(3, newValues, false);

        log("Validate that the correct updated number of images are present.");
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE.getName(), Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"), 2);
        assertElementPresent("Did not find the expected number of icons for images for " + JPG01_FILE.getName(), Locator.xpath("//img[contains(@title, '" + JPG01_FILE.getName() + "')]"), 2);
        assertElementPresent("Did not find the expected number of icons for images for " + PDF_FILE.getName(), Locator.xpath("//img[contains(@src, 'pdf.gif')]"), 2);
        assertElementPresent("Did not find the expected text for " + PDF_FILE.getName(), Locator.xpath("//a[contains(text(), '" + PDF_FILE.getName() + "')]"), 2);

        log("Hover over the thumbnail for the png and make sure the pop-up is as expected.");
        mouseOver(Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"));
        shortWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#helpDiv")));
        String src = Locator.xpath("//div[@id='helpDiv']//img[contains(@src, 'download')]").findElement(getDriver()).getAttribute("src");
        assertTrue("Wrong image in popup: " + src, src.contains(LRG_PNG_FILE.getName()));
        assertEquals("Bad response from image pop-up", HttpStatus.SC_OK, WebTestHelper.getHttpResponse(src).getResponseCode());

        // Commenting out for now. There is a random behavior where sometimes the thumbnail image will not show up when you move from one cell to another.
        /*
        // Need to explicitly delete the thumbnail so the download image can disappear and allow hover to work over the images below it.
        click(Locator.xpath("//img[contains(@src, 'partdelete.png')]"));
        // Also need to move the mouse, and bring it back, otherwise we just get the tool-tip and not the thumb nail.
        mouseOver(Locator.xpath("//img[contains(@src, 'logo.image')]"));

        log("Hover over the other thumbnail for the jpg and make sure the pop-up is as expected.");
        mouseOver(Locator.xpath("//img[contains(@title, '" + JPG01_FILE + "')]"));
        shortWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#helpDiv")));
        assertElementPresent("Download image is not as expected.", Locator.xpath("//div[@id='helpDiv']//img[contains(@src, '" + JPG01_FILE + "')]"), 1);

        // PDF as attachments are curerntly broken and do not show an expected thumbnail.
        */
        log("Export the grid to excel.");
        list = new DataRegionTable("query", this);
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        try{

            workbook = ExcelHelper.create(exportedFile);
            sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), 3, sheet.getLastRowNum());

            log("Validate that the value for the first attachment columns is as expected.");
            exportedColumn = ExcelHelper.getColumnData(sheet, 2);
            assertEquals("Files in '" + LIST_ATTACHMENT01_LABEL + "' column not exported as expected [" + exportedFile.getName() + "]",
                    Arrays.asList(LIST_ATTACHMENT01_LABEL, LRG_PNG_FILE.getName(), JPG01_FILE.getName(), PDF_FILE.getName()), exportedColumn);

            log("Validate that the value for the second attachment columns is as expected.");
            exportedColumn = ExcelHelper.getColumnData(sheet, 3);
            assertEquals("Files in '" + LIST_ATTACHMENT02_LABEL + "' column not exported as expected [" + exportedFile.getName() + "]",
                    Arrays.asList(LIST_ATTACHMENT02_LABEL, PDF_FILE.getName(), LRG_PNG_FILE.getName(), JPG01_FILE.getName()), exportedColumn);

            for(int i=0; i < 6; i++)
            {
                log("Column " + i + " width: " + sheet.getColumnWidth(i));
            }

            assertTrue("Column '" + LIST_ATTACHMENT01_LABEL + "' width not in expected range (" + COLUMN_WIDTH_LARGE_LBOUND + " to " + COLUMN_WIDTH_LARGE_UBOUND + "). Actual width: " + sheet.getColumnWidth(2), (sheet.getColumnWidth(2) > COLUMN_WIDTH_LARGE_LBOUND) && (sheet.getColumnWidth(2) < COLUMN_WIDTH_LARGE_UBOUND));
            assertTrue("Column '" + LIST_ATTACHMENT02_LABEL + "' width not in expected range (" + COLUMN_WIDTH_LARGE_LBOUND + " to " + COLUMN_WIDTH_LARGE_UBOUND + "). Actual width: " + sheet.getColumnWidth(3), (sheet.getColumnWidth(3) > COLUMN_WIDTH_LARGE_LBOUND) && (sheet.getColumnWidth(3) < COLUMN_WIDTH_LARGE_UBOUND));

            for(int j=0; j <= sheet.getLastRowNum(); j++)
            {
                log("Row " + j + " height: " + sheet.getRow(j).getHeight());
            }

            assertTrue("Height of row 1 not in expected range (" + ROW_HEIGHT_LARGE_LBOUND + " to " + ROW_HEIGHT_LARGE_UBOUND + "). Actual height: " + sheet.getRow(1).getHeight(), (sheet.getRow(1).getHeight() > ROW_HEIGHT_LARGE_LBOUND) && (sheet.getRow(1).getHeight() < ROW_HEIGHT_LARGE_UBOUND));
            assertTrue("Height of row 2 not in expected range (" + ROW_HEIGHT_LARGE_LBOUND + " to " + ROW_HEIGHT_LARGE_UBOUND + "). Actual height: " + sheet.getRow(2).getHeight(), (sheet.getRow(2).getHeight() > ROW_HEIGHT_LARGE_LBOUND) && (sheet.getRow(2).getHeight() < ROW_HEIGHT_LARGE_UBOUND));
            assertTrue("Height of row 3 not in expected range (" + ROW_HEIGHT_SMALL_LBOUND + " to " + ROW_HEIGHT_SMALL_UBOUND + "). Actual height: " + sheet.getRow(3).getHeight(), (sheet.getRow(3).getHeight() > ROW_HEIGHT_SMALL_LBOUND) && (sheet.getRow(3).getHeight() < ROW_HEIGHT_SMALL_UBOUND));

        }
        catch (IOException | InvalidFormatException e)
        {
            throw new RuntimeException(e);
        }

        log("Remove one of the attachment columns and validate that everything still works.");
        clickAndWait(Locator.linkWithText("Design"));
        _listHelper.clickEditDesign();
        _listHelper.deleteField("List Fields", 3);
        _listHelper.clickSave();
        clickAndWait(Locator.lkButton("Done"));

        log("Validate that the correct number of images are present.");
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE.getName(), Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + JPG01_FILE.getName(), Locator.xpath("//img[contains(@title, '" + JPG01_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + PDF_FILE.getName(), Locator.xpath("//img[contains(@src, 'pdf.gif')]"), 1);
        assertElementPresent("Did not find the expected text for " + PDF_FILE.getName(), Locator.xpath("//a[contains(text(), '" + PDF_FILE.getName() + "')]"), 1);

        log("Hover over the thumbnail and make sure the pop-up is as expected.");
        mouseOver(Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"));
        shortWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#helpDiv")));
        src = Locator.xpath("//div[@id='helpDiv']//img[contains(@src, 'download')]").findElement(getDriver()).getAttribute("src");
        assertTrue("Wrong image in popup: " + src, src.contains(LRG_PNG_FILE.getName()));
        assertEquals("Bad response from image pop-up", HttpStatus.SC_OK, WebTestHelper.getHttpResponse(src).getResponseCode());

        log("Export the grid to excel.");
        list = new DataRegionTable("query", this);
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        workbook = ExcelHelper.create(exportedFile);
        sheet = workbook.getSheetAt(0);

        assertEquals("Wrong number of rows exported to " + exportedFile.getName(), 3, sheet.getLastRowNum());

        log("Validate that the value for the first attachment column is as expected.");
        exportedColumn = ExcelHelper.getColumnData(sheet, 2);
        assertEquals("Files in '" + LIST_ATTACHMENT01_LABEL + "' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList(LIST_ATTACHMENT01_LABEL, LRG_PNG_FILE.getName(), JPG01_FILE.getName(), PDF_FILE.getName()), exportedColumn);

        for(int i=0; i < 6; i++)
        {
            log("Column " + i + " width: " + sheet.getColumnWidth(i));
        }

        log("Validate that the column widths are as expected.");
        assertTrue("Column '" + LIST_ATTACHMENT01_LABEL + "' width not in expected range (" + COLUMN_WIDTH_LARGE_LBOUND + " to " + COLUMN_WIDTH_LARGE_UBOUND + "). Actual width: " + sheet.getColumnWidth(2), (sheet.getColumnWidth(2) > COLUMN_WIDTH_LARGE_LBOUND) && (sheet.getColumnWidth(2) < COLUMN_WIDTH_LARGE_UBOUND));
        assertTrue("Column '" + LIST_ATTACHMENT02_LABEL + "' width not in expected range (" + COLUMN_WIDTH_SMALL_LBOUND + " to " + COLUMN_WIDTH_SMALL_UBOUND + "). Actual width: " + sheet.getColumnWidth(3), (sheet.getColumnWidth(3) > COLUMN_WIDTH_SMALL_LBOUND) && (sheet.getColumnWidth(3) < COLUMN_WIDTH_SMALL_UBOUND));

        for(int j=0; j <= sheet.getLastRowNum(); j++)
        {
            log("Row " + j + " height: " + sheet.getRow(j).getHeight());
        }

        assertTrue("Height of row 1 not in expected range (" + ROW_HEIGHT_LARGE_LBOUND + " to " + ROW_HEIGHT_LARGE_UBOUND + "). Actual height: " + sheet.getRow(1).getHeight(), (sheet.getRow(1).getHeight() > ROW_HEIGHT_LARGE_LBOUND) && (sheet.getRow(1).getHeight() < ROW_HEIGHT_LARGE_UBOUND));
        assertTrue("Height of row 2 not in expected range (" + ROW_HEIGHT_SMALL_LBOUND + " to " + ROW_HEIGHT_SMALL_UBOUND + "). Actual height: " + sheet.getRow(2).getHeight(), (sheet.getRow(2).getHeight() > ROW_HEIGHT_SMALL_LBOUND) && (sheet.getRow(2).getHeight() < ROW_HEIGHT_SMALL_UBOUND));
        assertTrue("Height of row 3 not in expected range (" + ROW_HEIGHT_TEXT_LBOUND + " to " + ROW_HEIGHT_TEXT_UBOUND + "). Actual height: " + sheet.getRow(3).getHeight(), (sheet.getRow(3).getHeight() > ROW_HEIGHT_TEXT_LBOUND) && (sheet.getRow(3).getHeight() < ROW_HEIGHT_TEXT_UBOUND));
    }
}
