package org.labkey.test.tests;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.Test;
import org.labkey.test.BaseWebDriverMultipleTest;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Inheritors should provide the needed info for checking the exported file.
 * goToDataRegionPage will be called before each test and should leave the browser at the test target
 * The DataRegion to be checked should have 5+ rows of data
 */
public abstract class AbstractExportTest extends BaseWebDriverMultipleTest
{
    protected DataRegionTable dataRegion;
    protected DataRegionExportHelper exportHelper;

    protected abstract String getTestColumnTitle();
    protected abstract int getTestColumnIndex();
    protected abstract String getExportedTsvTestColumnHeader(); // tsv column headers might be field name, rather than label
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
        exportHelper = new DataRegionExportHelper(this, dataRegion);

        dataRegion.uncheckAll();
    }

    @Test
    public final void testExportSelectedTSV()
    {
        int rowCount = 2;
        checkFirstNRows(rowCount);

        File exportedFile = exportHelper.exportText(DataRegionExportHelper.TextSeparator.TAB, DataRegionExportHelper.TextQuote.DOUBLE, true);
        assertExportExists(exportedFile, DataRegionExportHelper.TextSeparator.TAB.getFileExtension());

        assertTextExportContents(exportedFile, rowCount);
    }

    @Test
    public final void testExportIgnoreSelectedTSV()
    {
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
        int rowCount = 4;
        checkFirstNRows(rowCount);

        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX, true);
        assertExportExists(exportedFile, DataRegionExportHelper.ExcelFileType.XLSX.getFileExtension());

        assertExcelExportContents(exportedFile, rowCount);
    }

    @Test
    public final void testExportIgnoreSelectedExcel()
    {
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

        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return exportedFile.length() > 0;
            }
        }, "Exported file is empty",  WAIT_FOR_JAVASCRIPT);
    }

    protected final void assertTextExportContents(File exportedFile, int expectedDataRowCount)
    {
        String fileContents = getFileContents(exportedFile);
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

        assertEquals("Wrong rows exported", expectedExportColumn, exportedColumn);
    }

    protected final void assertExcelExportContents(File exportedFile, int expectedDataRowCount)
    {
        try
        {
            Workbook workbook = ExcelHelper.create(exportedFile);
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), expectedDataRowCount, sheet.getLastRowNum());

            List<String> expectedExportColumn = new ArrayList<>();
            expectedExportColumn.add(getTestColumnTitle());
            expectedExportColumn.addAll(dataRegion.getColumnDataAsText(getTestColumnTitle()).subList(0, expectedDataRowCount));

            assertEquals("Wrong rows exported", expectedExportColumn, ExcelHelper.getColumnData(sheet, getTestColumnIndex()));
        }
        catch (IOException | InvalidFormatException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
