/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.labkey.api.data.ColumnHeaderType;
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
    protected abstract String getExportedXlsTestColumnHeader(ColumnHeaderType exportType); // tsv column headers might be field name, rather than label
    protected abstract String getExportedTsvTestColumnHeader(ColumnHeaderType exportType); // tsv column headers might be field name, rather than label
    protected abstract String getDataRegionColumnName();
    protected abstract String getDataRegionSchemaName();
    protected abstract String getDataRegionQueryName();
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

        dataRegion = new DataRegionTable(getDataRegionId(), this);
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

        for (ColumnHeaderType exportHeaderType : getExportHeaderTypes())
        {
            File exportedFile = exportHelper.exportText(exportHeaderType, DataRegionExportHelper.TextSeparator.TAB, DataRegionExportHelper.TextQuote.DOUBLE, true);
            assertExportExists(exportedFile, DataRegionExportHelper.TextSeparator.TAB.getFileExtension());
            assertTextExportContents(exportHeaderType, exportedFile, rowCount);
        }
    }

    @Test
    public final void testExportIgnoreSelectedTSV()
    {
        Assume.assumeTrue("Skipping test for grid that doesn't support selecting rows", hasSelectors());

        int rowCount = 3;
        checkFirstNRows(rowCount);

        for (ColumnHeaderType exportHeaderType : getExportHeaderTypes())
        {
            File exportedFile = exportHelper.exportText(exportHeaderType, DataRegionExportHelper.TextSeparator.TAB, DataRegionExportHelper.TextQuote.DOUBLE, false);
            assertExportExists(exportedFile, DataRegionExportHelper.TextSeparator.TAB.getFileExtension());
            assertTextExportContents(exportHeaderType, exportedFile, dataRegion.getDataRowCount());
        }
    }

    @Test
    public final void testExportAllTSV()
    {
        File exportedFile = exportHelper.exportText(ColumnHeaderType.Caption, DataRegionExportHelper.TextSeparator.TAB);
        assertExportExists(exportedFile, DataRegionExportHelper.TextSeparator.TAB.getFileExtension());
        assertTextExportContents(ColumnHeaderType.Caption, exportedFile, dataRegion.getDataRowCount());
    }

    @Test
    public final void testExportSelectedExcel()
    {
        Assume.assumeTrue("Skipping test for grid that doesn't support selecting rows", hasSelectors());

        int rowCount = 4;
        checkFirstNRows(rowCount);

        for (ColumnHeaderType exportHeaderType : getExportHeaderTypes())
        {
            File exportedFile = exportHelper.exportExcel(exportHeaderType, DataRegionExportHelper.ExcelFileType.XLSX, true);
            assertExportExists(exportedFile, DataRegionExportHelper.ExcelFileType.XLSX.getFileExtension());
            assertExcelExportContents(exportHeaderType, exportedFile, rowCount);
        }
    }

    @Test
    public final void testExportIgnoreSelectedExcel()
    {
        Assume.assumeTrue("Skipping test for grid that doesn't support selecting rows", hasSelectors());

        int rowCount = 5;
        checkFirstNRows(rowCount);

        for (ColumnHeaderType exportHeaderType : getExportHeaderTypes())
        {
            File exportedFile = exportHelper.exportExcel(exportHeaderType, DataRegionExportHelper.ExcelFileType.XLSX, false);
            assertExportExists(exportedFile, DataRegionExportHelper.ExcelFileType.XLSX.getFileExtension());
            assertExcelExportContents(exportHeaderType, exportedFile, dataRegion.getDataRowCount());
        }
    }

    @Test
    public final void testExportAllExcel()
    {
        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX);
        assertExportExists(exportedFile, DataRegionExportHelper.ExcelFileType.XLSX.getFileExtension());
        assertExcelExportContents(ColumnHeaderType.Caption, exportedFile, dataRegion.getDataRowCount());
    }

    @Test
    public final void testExportAllPagedExcel()
    {
        int allRows = dataRegion.getDataRowCount();

        List<String> expectedExportColumn = new ArrayList<>();
        expectedExportColumn.add(getExportedXlsTestColumnHeader(ColumnHeaderType.Caption));
        expectedExportColumn.addAll(dataRegion.getColumnDataAsText(getTestColumnTitle()));

        assertEquals(allRows, expectedExportColumn.size()-1);

        // Issue 19854: Check that all rows will be exported when nothing is selected and page size is less than grid row count.
        dataRegion.setMaxRows(2);
        assertEquals(2, dataRegion.getDataRowCount());
        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX);
        assertExcelExportContents(ColumnHeaderType.Caption, exportedFile, allRows, expectedExportColumn);
    }

    @Test
    public final void testCreatePythonScriptNoFilter()
    {
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.PYTHON, pythonScript ->
        {
            assertScriptContentLineCount(pythonScript, 10);
            assertPythonScriptContents(pythonScript, null);
        });
    }

    @Test
    public final void testCreatePythonScriptWithFilter()
    {
        String testColumn = getDataRegionColumnName();
        dataRegion.setFilter(testColumn, "Equals", "foo");
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.PYTHON, pythonScript ->
        {
            assertScriptContentLineCount(pythonScript, 13);
            assertPythonScriptContents(pythonScript, testColumn);
        });
    }

    @Test
    public final void testCreateRScriptNoFilter()
    {
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.R, rScript ->
        {
            assertScriptContentLineCount(rScript, 20);
            assertRScriptContents(rScript, null);
        });
    }

    @Test
    public final void testCreateRScriptWithFilter()
    {
        String testColumn = getDataRegionColumnName();
        dataRegion.setFilter(testColumn, "Equals", "foo");
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.R, rScript ->
        {
            assertScriptContentLineCount(rScript, 20);
            assertRScriptContents(rScript, testColumn);
        });
    }

    @Test
    public final void testCreateJavaScriptNoFilter()
    {
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.JAVA, javaScript ->
        {
            assertScriptContentLineCount(javaScript, 20);
            assertJavaScriptContents(javaScript, null);
        });
    }

    @Test
    public final void testCreateJavaScriptWithFilter()
    {
        String testColumn = getDataRegionColumnName();
        dataRegion.setFilter(testColumn, "Equals", "foo");
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.JAVA, javaScript ->
        {
            assertScriptContentLineCount(javaScript, 21);
            assertJavaScriptContents(javaScript, testColumn);
        });
    }

    @Test
    public final void testCreateJavaScriptScriptNoFilter()
    {
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.JAVASCRIPT, javaScriptScript ->
        {
            assertScriptContentLineCount(javaScriptScript, 35);
            assertJavaScriptScriptContents(javaScriptScript, null);
        });
    }

    @Test
    public final void testCreateJavaScriptScriptWithFilter()
    {
        String testColumn = getDataRegionColumnName();
        dataRegion.setFilter(testColumn, "Equals", "foo");
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.JAVASCRIPT, javaScriptScript ->
        {
            assertScriptContentLineCount(javaScriptScript, 35);
            assertJavaScriptScriptContents(javaScriptScript, testColumn);
        });
    }

    @Test
    public final void testCreateSASScriptNoFilter()
    {
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.SAS, sasScript ->
        {
            assertScriptContentLineCount(sasScript, 15);
            assertSASScriptContents(sasScript, null);
        });
    }

    @Test
    public final void testCreateSASScriptWithFilter()
    {
        String testColumn = getDataRegionColumnName();
        dataRegion.setFilter(testColumn, "Equals", "foo");
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.SAS, sasScript ->
        {
            assertScriptContentLineCount(sasScript, 16);
            assertSASScriptContents(sasScript, testColumn);
        });
    }

    @Test
    public final void testCreatePerlScriptNoFilter()
    {
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.PERL, perlScript ->
        {
            assertScriptContentLineCount(perlScript, 30);
            assertPerlScriptContents(perlScript, null);
        });
    }

    @Test
    public final void testCreatePerlScriptWithFilter()
    {
        String testColumn = getDataRegionColumnName();
        dataRegion.setFilter(testColumn, "Equals", "foo");
        exportHelper.exportAndVerifyScript(DataRegionExportHelper.ScriptExportType.PERL, perlScript ->
        {
            assertScriptContentLineCount(perlScript, 33);
            assertPerlScriptContents(perlScript, testColumn);
        });
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

    protected final void assertTextExportContents(ColumnHeaderType exportHeaderType, File exportedFile, int expectedDataRowCount)
    {
        String fileContents = TestFileUtils.getFileContents(exportedFile);
        String[] exportedRows = fileContents.split("\n");
        List<String> expectedExportColumn = new ArrayList<>();
        int expectedFileRowCount = expectedDataRowCount;

        if (!exportHeaderType.equals(ColumnHeaderType.None))
        {
            expectedFileRowCount++;     // need to account for the column header
            expectedExportColumn.add(getExportedTsvTestColumnHeader(exportHeaderType));
        }

        assertEquals("Wrong number of rows exported to " + exportedFile.getName(), expectedFileRowCount, exportedRows.length);
        expectedExportColumn.addAll(dataRegion.getColumnDataAsText(getTestColumnTitle()).subList(0, expectedDataRowCount));

        List<String> exportedColumn = new ArrayList<>();
        for (String row : exportedRows)
        {
            exportedColumn.add(row.split("\t")[getTestColumnIndex()]);
        }

        assertColumnContentsEqual(expectedExportColumn, exportedColumn);
    }

    protected final void assertExcelExportContents(ColumnHeaderType exportHeaderType, File exportedFile, int expectedDataRowCount)
    {
        List<String> expectedExportColumn = new ArrayList<>();
        if (!exportHeaderType.equals(ColumnHeaderType.None))
            expectedExportColumn.add(getExportedXlsTestColumnHeader(exportHeaderType));

        expectedExportColumn.addAll(dataRegion.getColumnDataAsText(getTestColumnTitle()).subList(0, expectedDataRowCount));
        assertExcelExportContents(exportHeaderType, exportedFile, expectedDataRowCount, expectedExportColumn);
    }

    protected final void assertExcelExportContents(ColumnHeaderType exportHeaderType, File exportedFile, int expectedDataRowCount, List<String> expectedExportColumn)
    {
        try
        {
            Workbook workbook = ExcelHelper.create(exportedFile);
            Sheet sheet = workbook.getSheetAt(0);
            int expectedFileRowCount = expectedDataRowCount;
            if (exportHeaderType.equals(ColumnHeaderType.None))
            {
                expectedFileRowCount--;
            }

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), expectedFileRowCount, sheet.getLastRowNum());

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

    protected void assertScriptContentLineCount(String script, int expectedLineCount)
    {
        String[] linesInScript = script.split("\n");
        assertTrue("Wrong number of lines in script [" + linesInScript.length + "]. Expected >" + expectedLineCount, expectedLineCount <= linesInScript.length);
    }

    protected final void assertPythonScriptContents(String pythonScript, String filterColumn)
    {
        assertTrue("Script is missing labkey library", pythonScript.contains("import labkey"));
        assertTrue("Script is missing labkey.query.select_rows call", pythonScript.contains("labkey.query.select_rows("));
        assertTrue("Script is missing schema_name property", pythonScript.contains("schema_name='" + getDataRegionSchemaName() + "'"));
        assertTrue("Script is missing query_name property", pythonScript.contains("query_name='" + getDataRegionQueryName() + "'"));
        if (null != filterColumn)
            assertTrue("Script is missing filter for column '" + filterColumn + "'", pythonScript.contains(filterColumn));
    }

    protected final void assertRScriptContents(String rScript, String filterColumn)
    {
        assertTrue("Script is missing Rlabkey library", rScript.contains("library(Rlabkey)"));
        assertTrue("Script is missing labkey.selectRows call", rScript.contains("labkey.selectRows("));
        assertTrue("Script is missing schemaName property", rScript.contains("schemaName=\"" + getDataRegionSchemaName() + "\""));
        assertTrue("Script is missing queryName property", rScript.contains("queryName=\"" + getDataRegionQueryName() + "\""));
        if (null != filterColumn)
            assertTrue("Script is missing colFilter property", rScript.contains("makeFilter(c(\"" + filterColumn + "\", \"EQUAL\", \"foo\""));
    }

    protected final void assertJavaScriptContents(String javaScript, String filterColumn)
    {
        assertTrue("Script is missing SelectRowsCommand", javaScript.contains("SelectRowsCommand cmd = new SelectRowsCommand(\"" + getDataRegionSchemaName() + "\", \"" + getDataRegionQueryName() + "\");"));
        if (null != filterColumn)
            assertTrue("Script is missing addFilter()", javaScript.contains("cmd.addFilter(\"" + filterColumn + "\", \"foo\", Filter.Operator.EQUAL);"));
    }

    protected final void assertJavaScriptScriptContents(String javaScriptScript, String filterColumn)
    {
        assertTrue("Script is missing LABKEY.Query.selectRows call", javaScriptScript.contains("LABKEY.Query.selectRows({"));
        assertTrue("Script is missing schemaName property", javaScriptScript.contains("schemaName: '" + getDataRegionSchemaName() + "'"));
        assertTrue("Script is missing queryName property", javaScriptScript.contains("queryName: '" + getDataRegionQueryName() + "'"));
        if (null != filterColumn)
            assertTrue("Script is missing filterArray property", javaScriptScript.contains("LABKEY.Filter.create('" + filterColumn + "', 'foo', LABKEY.Filter.Types.EQUAL)"));
    }

    protected final void assertSASScriptContents(String sasScript, String filterColumn)
    {
        assertTrue("Script is missing %labkeySelectRows call", sasScript.contains("%labkeySelectRows("));
        assertTrue("Script is missing schemaName property", sasScript.contains("schemaName=\"" + getDataRegionSchemaName() + "\""));
        assertTrue("Script is missing queryName property", sasScript.contains("queryName=\"" + getDataRegionQueryName() + "\""));
        if (null != filterColumn)
        {
            assertTrue("Script is missing filter property", sasScript.contains("filter=%labkeyMakeFilter("));
            assertTrue("Script is missing filter property", sasScript.contains("\"" + filterColumn + "\",\"EQUAL\",\"foo\""));
        }
    }

    protected final void assertPerlScriptContents(String perlScript, String filterColumn)
    {
        // some browsers return script with ">" and "<" and some with "&gt;" and "&lt;"
        perlScript = perlScript.replaceAll("&gt;", ">");
        perlScript = perlScript.replaceAll("&lt;", "<");

        assertTrue("Script is missing labkey library", perlScript.contains("use LabKey::Query;"));
        assertTrue("Script is missing LabKey::Query::selectRows call", perlScript.contains("LabKey::Query::selectRows("));
        assertTrue("Script is missing schemaName property", perlScript.contains("-schemaName => '" + getDataRegionSchemaName() + "'"));
        assertTrue("Script is missing queryName property", perlScript.contains("-queryName => '" + getDataRegionQueryName() + "'"));
        if (null != filterColumn)
            assertTrue("Script is missing filterArray property", perlScript.contains("['" + filterColumn + "', eq, ''foo'']"));
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

    public ColumnHeaderType[] getExportHeaderTypes()
    {
        return new ColumnHeaderType[]{ColumnHeaderType.Caption};
    }
}
