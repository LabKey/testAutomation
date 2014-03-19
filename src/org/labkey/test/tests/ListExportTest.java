/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverMultipleTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.labkey.test.util.DataRegionExportHelper.*;

@Category({DailyB.class})
public class ListExportTest extends BaseWebDriverMultipleTest
{
    private static final File LIST_ARCHIVE = new File(getSampledataPath(), "lists/ListDemo.lists.zip");
    private static final String LIST_NAME = "NIMHDemographics";
    private static final int LIST_ROW_COUNT = 15;
    private static final String TEST_COLUMN_HEADER = "Name";
    private static final int TEST_COLUMN_INDEX = 1;

    private DataRegionTable listDataRegion;
    private DataRegionExportHelper exportHelper;

    @Override
    protected String getProjectName()
    {
        return "List Download Test";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        ListExportTest initTest = new ListExportTest();
        initTest.doCleanup(false);

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest._listHelper.importListArchive(initTest.getProjectName(), LIST_ARCHIVE);

        currentTest = initTest;
    }

    @Before
    public void preTest()
    {
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(LIST_NAME));

        listDataRegion = new DataRegionTable("query", this);
        exportHelper = new DataRegionExportHelper(this, listDataRegion);

        listDataRegion.uncheckAll();
    }

    @Test
    public void testExportSelectedTSV()
    {
        int rowCount = 3;
        checkFirstNRows(rowCount);

        File exportedList = exportHelper.exportText(TextSeparator.TAB, TextQuote.DOUBLE, true);
        assertExportExists(exportedList, TextSeparator.TAB.getFileExtension());

        assertTextExportContents(exportedList, rowCount);
    }

    @Test
    public void testExportIgnoreSelectedTSV()
    {
        int rowCount = 4;
        checkFirstNRows(rowCount);

        File exportedList = exportHelper.exportText(TextSeparator.TAB, TextQuote.DOUBLE, false);
        assertExportExists(exportedList, TextSeparator.TAB.getFileExtension());

        assertTextExportContents(exportedList, LIST_ROW_COUNT);
    }

    @Test
    public void testExportAllTSV()
    {
        File exportedList = exportHelper.exportText(TextSeparator.TAB);
        assertExportExists(exportedList, TextSeparator.TAB.getFileExtension());

        assertTextExportContents(exportedList, LIST_ROW_COUNT);
    }

    @Test
    public void testExportSelectedExcel()
    {
        int rowCount = 5;
        checkFirstNRows(rowCount);

        File exportedList = exportHelper.exportExcel(ExcelFileType.XLSX, true);
        assertExportExists(exportedList, ExcelFileType.XLSX.getFileExtension());

        assertExcelExportContents(exportedList, rowCount);
    }

    @Test
    public void testExportIgnoreSelectedExcel()
    {
        int rowCount = 6;
        checkFirstNRows(rowCount);

        File exportedList = exportHelper.exportExcel(ExcelFileType.XLSX, false);
        assertExportExists(exportedList, ExcelFileType.XLSX.getFileExtension());

        assertExcelExportContents(exportedList, LIST_ROW_COUNT);
    }

    @Test
    public void testExportAllExcel()
    {
        File exportedList = exportHelper.exportExcel(ExcelFileType.XLSX);
        assertExportExists(exportedList, ExcelFileType.XLSX.getFileExtension());

        assertExcelExportContents(exportedList, LIST_ROW_COUNT);
    }

    private List<String> checkFirstNRows(int n)
    {
        List<String> rowIds = new ArrayList<>();
        for (int i = 0; i < n; i++)
        {
            listDataRegion.checkCheckbox(i);
            rowIds.add(listDataRegion.getDataAsText(i, "Name"));
        }

        return rowIds;
    }

    private void assertExportExists(File exportedList, String expectedExtension)
    {
        assertTrue("Exported list does not exist: " + exportedList.getAbsolutePath(), exportedList.exists());

        final String listExportRegex = LIST_NAME + "_[0-9_-]*\\." + expectedExtension;
        assertTrue("Exported list did not have expected name: " + exportedList.getName(), exportedList.getName().matches(listExportRegex));

        assertTrue("Exported file is empty", exportedList.length() > 0);
    }

    private void assertTextExportContents(File exportedList, int expectedDataRowCount)
    {
        String fileContents = getFileContents(exportedList);
        String[] exportedRows = fileContents.split("\n");

        assertEquals("Wrong number of rows exported to " + exportedList.getName(), expectedDataRowCount + 1, exportedRows.length);

        List<String> expectedExportColumn = new ArrayList<>();
        expectedExportColumn.add(TEST_COLUMN_HEADER.toLowerCase()); // header is lower case after export to text
        expectedExportColumn.addAll(listDataRegion.getColumnDataAsText(TEST_COLUMN_HEADER).subList(0, expectedDataRowCount));

        List<String> exportedColumn = new ArrayList<>();
        for (String row : exportedRows)
        {
            exportedColumn.add(row.split("\t")[TEST_COLUMN_INDEX]);
        }

        assertEquals("Wrong rows exported", expectedExportColumn, exportedColumn);
    }

    private void assertExcelExportContents(File exportedList, int expectedDataRowCount)
    {
        try
        {
            Workbook workbook = ExcelHelper.create(exportedList);
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedList.getName(), expectedDataRowCount, sheet.getLastRowNum());

            List<String> expectedExportColumn = new ArrayList<>();
            expectedExportColumn.add(TEST_COLUMN_HEADER);
            expectedExportColumn.addAll(listDataRegion.getColumnDataAsText(TEST_COLUMN_HEADER).subList(0, expectedDataRowCount));

            assertEquals("Wrong rows exported", expectedExportColumn, ExcelHelper.getColumnData(sheet, TEST_COLUMN_INDEX));
        }
        catch (IOException | InvalidFormatException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
       deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/list";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
