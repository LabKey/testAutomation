/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Inheritors should provide the needed info for checking the exported file.
 * goToDataRegionPage will be called before each test and should leave the browser at the test target
 * The DataRegion to be checked should have 5+ rows of data
 */
public abstract class AbstractExportTest extends BaseWebDriverTest
{
    protected DataRegionTable dataRegion;
    protected DataRegionExportHelper exportHelper;

    /** Return true if the rows can be selected in the grid. */
    protected abstract boolean hasSelectors();
    protected abstract String getTestColumnTitle();
    protected abstract int getTestColumnIndex();
    protected abstract String getExportedTsvTestColumnHeader(); // tsv column headers might be field name, rather than label
    protected abstract String getDataRegionColumnName();
    protected abstract String getExportedFilePrefixRegex();
    protected abstract String getDataRegionId();
    protected abstract void goToDataRegionPage();

    @Before
    public void preTest()
    {
        if (getSavedLocation() == null)
        {
            goToDataRegionPage();
            saveLocation();
        }
        else
        {
            recallLocation();
        }

        dataRegion = new DataRegionTable(getDataRegionId(), this, hasSelectors());
        exportHelper = new DataRegionExportHelper(dataRegion);

        if (hasSelectors())
            dataRegion.uncheckAll();
    }

    @Test
    public final void testExportSelectedTSV()
    {
        Assume.assumeTrue("Skipping test for grid that doesn't support selecting rows", hasSelectors());

        int rowCount = 2;
        checkFirstNRows(rowCount);

        File exportedFile = exportHelper.exportText(DataRegionExportHelper.TextSeparator.TAB, DataRegionExportHelper.TextQuote.DOUBLE, true);
        assertExportExists(exportedFile, DataRegionExportHelper.TextSeparator.TAB.getFileExtension());

        assertTextExportContents(exportedFile, rowCount);
    }

    @Test
    public final void testExportIgnoreSelectedTSV()
    {
        Assume.assumeTrue("Skipping test for grid that doesn't support selecting rows", hasSelectors());

        int rowCount = 3;
        checkFirstNRows(rowCount);

        File exportedFile = exportHelper.exportText(DataRegionExportHelper.TextSeparator.TAB, DataRegionExportHelper.TextQuote.DOUBLE, false);
        assertExportExists(exportedFile, DataRegionExportHelper.TextSeparator.TAB.getFileExtension());

        assertTextExportContents(exportedFile, dataRegion.getDataRowCount());
    }

    @Test
    public final void testExportAllTSV()
    {
        File exportedFile = exportHelper.exportText(DataRegionExportHelper.TextSeparator.TAB);
        assertExportExists(exportedFile, DataRegionExportHelper.TextSeparator.TAB.getFileExtension());

        assertTextExportContents(exportedFile, dataRegion.getDataRowCount());
    }

    @Test
    public final void testExportSelectedExcel()
    {
        Assume.assumeTrue("Skipping test for grid that doesn't support selecting rows", hasSelectors());

        int rowCount = 4;
        checkFirstNRows(rowCount);

        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX, true);
        assertExportExists(exportedFile, DataRegionExportHelper.ExcelFileType.XLSX.getFileExtension());

        assertExcelExportContents(exportedFile, rowCount);
    }

    @Test
    public final void testExportIgnoreSelectedExcel()
    {
        Assume.assumeTrue("Skipping test for grid that doesn't support selecting rows", hasSelectors());

        int rowCount = 5;
        checkFirstNRows(rowCount);

        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX, false);
        assertExportExists(exportedFile, DataRegionExportHelper.ExcelFileType.XLSX.getFileExtension());

        assertExcelExportContents(exportedFile, dataRegion.getDataRowCount());
    }

    @Test
    public final void testExportAllExcel()
    {
        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX);
        assertExportExists(exportedFile, DataRegionExportHelper.ExcelFileType.XLSX.getFileExtension());

        assertExcelExportContents(exportedFile, dataRegion.getDataRowCount());
    }

    @Test
    public final void testExportAllPagedExcel()
    {
        int allRows = dataRegion.getDataRowCount();

        List<String> expectedExportColumn = new ArrayList<>();
        expectedExportColumn.add(getTestColumnTitle());
        expectedExportColumn.addAll(dataRegion.getColumnDataAsText(getTestColumnTitle()));

        assertEquals(allRows, expectedExportColumn.size()-1);

        // Issue 19854: Check that all rows will be exported when nothing is selected and page size is less than grid row count.
        dataRegion.setMaxRows(2);
        assertEquals(2, dataRegion.getDataRowCount());
        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX);
        assertExcelExportContents(exportedFile, allRows, expectedExportColumn);
    }

    @Test
    public final void testCreatePythonScriptNoFilter()
    {
        String pythonScript = exportHelper.exportScript(DataRegionExportHelper.ScriptExportType.PYTHON);
        int expectedLineCountNoFilters = 10;
        assertPythonScriptContents(pythonScript, expectedLineCountNoFilters, null);
    }

    @Test
    public final void testCreatePythonScriptWithFilter()
    {
        String testColumn = getDataRegionColumnName();
        dataRegion.setFilter(testColumn, "Equals", "foo");
        String pythonScript = exportHelper.exportScript(DataRegionExportHelper.ScriptExportType.PYTHON);
        int expectedLineCountWithFilters = 13;
        assertPythonScriptContents(pythonScript, expectedLineCountWithFilters, testColumn);
    }

    protected final List<String> checkFirstNRows(int n)
    {
        List<String> rowIds = new ArrayList<>();
        for (int i = 0; i < n; i++)
        {
            dataRegion.checkCheckbox(i);
            rowIds.add(dataRegion.getDataAsText(i, getTestColumnTitle()));
        }

        return rowIds;
    }

    protected final void assertExportExists(final File exportedFile, String expectedExtension)
    {
        assertTrue("Exported file does not exist: " + exportedFile.getAbsolutePath(), exportedFile.exists());

        final String fileExportRegex = getExportedFilePrefixRegex() + "_[0-9_-]*\\." + expectedExtension;
        assertTrue("Exported file has wrong name:\n expected: " + fileExportRegex + "\n actual: " + exportedFile.getName(), exportedFile.getName().matches(fileExportRegex));

        waitFor(() -> exportedFile.length() > 0, "Exported file is empty", WAIT_FOR_JAVASCRIPT);
    }

    protected final void assertTextExportContents(File exportedFile, int expectedDataRowCount)
    {
        String fileContents = TestFileUtils.getFileContents(exportedFile);
        String[] exportedRows = fileContents.split("\n");

        assertEquals("Wrong number of rows exported to " + exportedFile.getName(), expectedDataRowCount + 1, exportedRows.length);

        List<String> expectedExportColumn = new ArrayList<>();
        expectedExportColumn.add(getExportedTsvTestColumnHeader());
        expectedExportColumn.addAll(dataRegion.getColumnDataAsText(getTestColumnTitle()).subList(0, expectedDataRowCount));

        List<String> exportedColumn = new ArrayList<>();
        for (String row : exportedRows)
        {
            exportedColumn.add(row.split("\t")[getTestColumnIndex()]);
        }

        assertColumnContentsEqual(expectedExportColumn, exportedColumn);
    }

    protected final void assertExcelExportContents(File exportedFile, int expectedDataRowCount)
    {
        List<String> expectedExportColumn = new ArrayList<>();
        expectedExportColumn.add(getTestColumnTitle());
        expectedExportColumn.addAll(dataRegion.getColumnDataAsText(getTestColumnTitle()).subList(0, expectedDataRowCount));

        assertExcelExportContents(exportedFile, expectedDataRowCount, expectedExportColumn);
    }

    protected final void assertExcelExportContents(File exportedFile, int expectedDataRowCount, List<String> expectedExportColumn)
    {
        try
        {
            Workbook workbook = ExcelHelper.create(exportedFile);
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), expectedDataRowCount, sheet.getLastRowNum());

            List<String> exportedColumn = ExcelHelper.getColumnData(sheet, getTestColumnIndex());
            assertColumnContentsEqual(expectedExportColumn, exportedColumn);
        }
        catch (IOException | InvalidFormatException e)
        {
            throw new RuntimeException(e);
        }
    }

    private final void assertColumnContentsEqual(List<String> expectedColumnContents, List<String> actualColumnContents)
    {
        if (expectSortedExport())
        {
            assertEquals("Wrong rows exported", expectedColumnContents, actualColumnContents);
        }
        else
        {
            assertEquals("Wrong column header", expectedColumnContents.get(0), actualColumnContents.get(0));
            assertEquals("Wrong rows exported",
                    new HashSet<>(expectedColumnContents.subList(1, expectedColumnContents.size())),
                    new HashSet<>(actualColumnContents.subList(1, actualColumnContents.size())));
        }
    }

    protected final void assertPythonScriptContents(String pythonScript, int expectedLineCount, String testColumn)
    {
        String[] linesInScript = pythonScript.split("\n");
        assertTrue("Wrong number of lines in script [" + linesInScript.length + "]. Expected >" + expectedLineCount, expectedLineCount <= linesInScript.length);
        if (null != testColumn)
        {
            assertTrue("Script is missing filter for column '" + testColumn + "'", pythonScript.contains(testColumn));
        }
    }

    protected boolean expectSortedExport()
    {
        return true;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
