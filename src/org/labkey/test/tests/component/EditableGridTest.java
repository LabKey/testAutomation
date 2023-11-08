package org.labkey.test.tests.component;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@Category({Daily.class})
public class EditableGridTest extends BaseWebDriverTest
{
    private static final String EXTRAPOLATING_SAMPLE_TYPE = "ExtrapolatingSampleType";
    private static final String ASC_STRING = "Ascending String";
    private static final String DESC_STRING = "Descending String";
    private static final String ASC_INT = "Ascending Int";
    private static final String DESC_INT = "Descending Int";
    private static final String ASC_DATE = "Ascending Date";
    private static final String DESC_DATE = "Descending Date";

    private static final String FILLING_SAMPLE_TYPE = "FillingSampleType";
    private static final String FILL_STRING = "Filling String";
    private static final String FILL_INT = "Filling Int";
    private static final String FILL_DATE = "Filling Date";

    private static final String PASTING_SAMPLE_TYPE = "PastingSampleType";
    private static final String PASTE_1 = "Paste Column 1";
    private static final String PASTE_2 = "Paste Column 2";
    private static final String PASTE_3 = "Paste Column 3";
    private static final String PASTE_4 = "Paste Column 4";
    private static final String PASTE_5 = "Paste Column 5";

    @BeforeClass
    public static void setupProject() throws Exception
    {
        ((EditableGridTest) getCurrentTest()).doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        Connection connection = createDefaultConnection();
        new SampleTypeDefinition(EXTRAPOLATING_SAMPLE_TYPE)
                .setFields(
                        List.of(
                                new FieldDefinition(ASC_STRING, FieldDefinition.ColumnType.String),
                                new FieldDefinition(DESC_STRING, FieldDefinition.ColumnType.String),
                                new FieldDefinition(ASC_INT, FieldDefinition.ColumnType.Integer),
                                new FieldDefinition(DESC_INT, FieldDefinition.ColumnType.Integer),
                                new FieldDefinition(ASC_DATE, FieldDefinition.ColumnType.DateAndTime),
                                new FieldDefinition(DESC_DATE, FieldDefinition.ColumnType.DateAndTime)
                        ))
                .create(connection, getProjectName());
        new SampleTypeDefinition(FILLING_SAMPLE_TYPE)
                .setFields(
                        List.of(
                                new FieldDefinition(FILL_STRING, FieldDefinition.ColumnType.String),
                                new FieldDefinition(FILL_INT, FieldDefinition.ColumnType.Integer),
                                new FieldDefinition(FILL_DATE, FieldDefinition.ColumnType.DateAndTime)
                        ))
                .create(connection, getProjectName());
        new SampleTypeDefinition(PASTING_SAMPLE_TYPE)
                .setFields(
                        List.of(
                                new FieldDefinition(PASTE_1, FieldDefinition.ColumnType.String),
                                new FieldDefinition(PASTE_2, FieldDefinition.ColumnType.String),
                                new FieldDefinition(PASTE_3, FieldDefinition.ColumnType.String),
                                new FieldDefinition(PASTE_4, FieldDefinition.ColumnType.String),
                                new FieldDefinition(PASTE_5, FieldDefinition.ColumnType.String)
                        ))
                .create(connection, getProjectName());
    }

    @Test
    public void testTooWideErrorCase() throws Exception
    {
        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        EditableGrid testGrid = testPage.getEditableGrid("exp", "Data");
        String wideShape = "Too wide\tACME\tthing\tanother\televen\toff the map\tMoar columns\n";

        testGrid.addRows(1);
        testGrid.pasteFromCell(0, "Description", wideShape);
        assertEquals("Expect cell error to explain that paste cannot add columns",
                "Unable to paste. Cannot paste columns beyond the columns found in the grid.",
                testGrid.getCellPopoverText(0, "Description"));
        assertThat("Expect failed paste to leave data unchanged",
                testGrid.getColumnData("Name"), everyItem(is("")));
    }

    @Test
    public void testCanAddRowsWithTallShape()
    {
        EditableGrid testGrid = goToEditableGrid(EXTRAPOLATING_SAMPLE_TYPE);
        String tallShape = """
                42
                41
                40
                39
                38""";

        assertEquals("Initial editable grid row count", 0, testGrid.getRowCount());
        testGrid.addRows(1);
        testGrid.pasteFromCell(0, DESC_STRING, tallShape);
        List<String> pastedColData = testGrid.getColumnData(DESC_STRING);
        List<String> unpastedColData = testGrid.getColumnData(ASC_STRING);

        assertEquals("Didn't get correct values", List.of("42", "41", "40", "39", "38"), pastedColData);
        assertThat("expect other column to remain empty",
                unpastedColData, everyItem(is("")));
    }

    @Test
    public void testDragFillExtrapolatingIntegers()
    {
        EditableGrid testGrid = goToEditableGrid(EXTRAPOLATING_SAMPLE_TYPE);

        testGrid.addRows(6);
        WebElement topLeft = setCellValues(testGrid, ASC_STRING, "2", "4").get(0);
        setCellValues(testGrid, DESC_STRING, "4", "2");
        setCellValues(testGrid, ASC_INT, "2", "4");
        WebElement bottomRight = setCellValues(testGrid, DESC_INT, "4", "2").get(1);
        WebElement fillTo = testGrid.getCell(4, DESC_INT);

        testGrid.selectCellRange(topLeft, bottomRight);
        testGrid.dragFill(bottomRight, fillTo);

        List<String> expectedIncreasing = List.of("2", "4", "6", "8", "10", "");
        List<String> expectedDecreasing = List.of("4", "2", "0", "-2", "-4", "");
        assertEquals("Drag-fill should have extrapolated " + ASC_STRING,
                expectedIncreasing,
                testGrid.getColumnData(ASC_STRING));
        assertEquals("Drag-fill should have extrapolated " + DESC_STRING,
                expectedDecreasing,
                testGrid.getColumnData(DESC_STRING));
        assertEquals("Drag-fill should have extrapolated " + ASC_INT,
                expectedIncreasing,
                testGrid.getColumnData(ASC_INT));
        assertEquals("Drag-fill should have extrapolated " + DESC_INT,
                expectedDecreasing,
                testGrid.getColumnData(DESC_INT));
    }

    @Test
    public void testDragFillExtrapolatingIntegersWithPrefix()
    {
        EditableGrid testGrid = goToEditableGrid(EXTRAPOLATING_SAMPLE_TYPE);

        testGrid.addRows(6);
        WebElement topLeft = setCellValues(testGrid, ASC_STRING, "ABC-2", "ABC-4").get(0);
        WebElement bottomRight = setCellValues(testGrid, DESC_STRING, "ABC-4", "ABC-2").get(1);
        WebElement fillTo = testGrid.getCell(4, DESC_STRING);

        testGrid.selectCellRange(topLeft, bottomRight);
        testGrid.dragFill(bottomRight, fillTo);

        List<String> expectedIncreasing = List.of("ABC-2", "ABC-4", "ABC-6", "ABC-8", "ABC-10", "");
        List<String> expectedDecreasing = List.of("ABC-4", "ABC-2", "ABC-0", "ABC--2", "ABC--4", "");
        assertEquals("Drag-fill should have extrapolated " + ASC_STRING,
                expectedIncreasing,
                testGrid.getColumnData(ASC_STRING));
        assertEquals("Drag-fill should have extrapolated " + DESC_STRING,
                expectedDecreasing,
                testGrid.getColumnData(DESC_STRING));
    }

    @Test
    public void testDragFillSingleRow()
    {
        final LocalDateTime now = LocalDate.of(2019, 1, 30).atTime(16, 30);

        EditableGrid testGrid = goToEditableGrid(FILLING_SAMPLE_TYPE);

        testGrid.addRows(4);
        WebElement topLeft = testGrid.setCellValue(0, FILL_STRING, "ABC-1");
        testGrid.setCellValue(0, FILL_INT, "1");
        WebElement bottomRight = testGrid.setCellValue(0, FILL_DATE, now);
        WebElement fillTo = testGrid.getCell(2, FILL_DATE);

        testGrid.selectCellRange(topLeft, bottomRight);
        testGrid.dragFill(bottomRight, fillTo);

        assertEquals("Drag-fill should have filled " + FILL_STRING,
                List.of("ABC-1", "ABC-1", "ABC-1", ""),
                testGrid.getColumnData(FILL_STRING));
        assertEquals("Drag-fill should have filled " + FILL_INT,
                List.of("1", "1", "1", ""),
                testGrid.getColumnData(FILL_INT));
        assertEquals("Drag-fill should have filled " + FILL_DATE,
                List.of(EditableGrid.DATE_FORMAT.format(now),
                        EditableGrid.DATE_FORMAT.format(now.plusDays(1)),
                        EditableGrid.DATE_FORMAT.format(now.plusDays(2)),
                        ""),
                testGrid.getColumnData(FILL_DATE));
    }

    @Test
    public void testDragFillMultipleRows()
    {
        final LocalDateTime now = LocalDate.of(2019, 1, 30).atTime(14, 30);

        EditableGrid testGrid = goToEditableGrid(FILLING_SAMPLE_TYPE);

        testGrid.addRows(7);
        WebElement topLeft = setCellValues(testGrid, FILL_STRING, "QWE", "ASD", "ZXC").get(0);
        WebElement bottomRight = setCellValues(testGrid, FILL_DATE, now, now.plusDays(3), now.plusDays(1)).get(2);
        WebElement fillTo = testGrid.getCell(5, FILL_DATE);

        testGrid.selectCellRange(topLeft, bottomRight);
        testGrid.dragFill(bottomRight, fillTo);

        assertEquals("Drag-fill should have filled " + FILL_STRING,
                List.of("QWE", "ASD", "ZXC", "QWE", "ASD", "ZXC", ""),
                testGrid.getColumnData(FILL_STRING));
        assertEquals("Drag-fill should have filled " + FILL_DATE,
                List.of(EditableGrid.DATE_FORMAT.format(now),
                        EditableGrid.DATE_FORMAT.format(now.plusDays(3)),
                        EditableGrid.DATE_FORMAT.format(now.plusDays(1)),
                        EditableGrid.DATE_FORMAT.format(now),
                        EditableGrid.DATE_FORMAT.format(now.plusDays(3)),
                        EditableGrid.DATE_FORMAT.format(now.plusDays(1)),
                        ""),
                testGrid.getColumnData(FILL_DATE));
    }

    @Test
    public void testExpandedPaste() throws Exception
    {
        final List<List<String>> clipRows = List.of(
                List.of("A", "B"),
                List.of("C", "D"));

        EditableGrid testGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        testGrid.addRows(5);

        log("Test wide");
        testGrid.selectCellRange(testGrid.getCell(0, PASTE_1), testGrid.getCell(1, PASTE_4));
        actionPaste(null, rowsToString(clipRows));
        assertEquals("Paste should expand to fill selection", getExpectedPaste(2, 1, clipRows), getActualPaste(testGrid));
        testGrid.clearAllCells();

        log("Test tall");
        testGrid.selectCellRange(testGrid.getCell(0, PASTE_1), testGrid.getCell(3, PASTE_2));
        actionPaste(null, rowsToString(clipRows));
        assertEquals("Paste should expand to fill selection", getExpectedPaste(1, 2, clipRows), getActualPaste(testGrid));
        testGrid.clearAllCells();

        log("Test wide and tall");
        testGrid.selectCellRange(testGrid.getCell(0, PASTE_1), testGrid.getCell(3, PASTE_4));
        actionPaste(null, rowsToString(clipRows));
        assertEquals("Paste should expand to fill selection", getExpectedPaste(2, 2, clipRows), getActualPaste(testGrid));
        testGrid.clearAllCells();
    }

    @Test
    public void testInvalidExpandedPaste() throws Exception
    {
        final List<List<String>> clipRows = List.of(
                List.of("A", "B"),
                List.of("C", "D"));

        EditableGrid testGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        testGrid.addRows(5);

        log("Test invalid selection width");
        testGrid.selectCellRange(testGrid.getCell(0, PASTE_1), testGrid.getCell(1, PASTE_5));
        actionPaste(null, rowsToString(clipRows));
        assertEquals("Paste should expand to fill selection", getExpectedPaste(1, 1, clipRows), getActualPaste(testGrid));
        testGrid.clearAllCells();

        log("Test invalid selection height");
        testGrid.selectCellRange(testGrid.getCell(0, PASTE_1), testGrid.getCell(4, PASTE_2));
        actionPaste(null, rowsToString(clipRows));
        assertEquals("Paste should expand to fill selection", getExpectedPaste(1, 1, clipRows), getActualPaste(testGrid));
        testGrid.clearAllCells();

        log("Test invalid width and height");
        testGrid.selectCellRange(testGrid.getCell(0, PASTE_1), testGrid.getCell(4, PASTE_5));
        actionPaste(null, rowsToString(clipRows));
        assertEquals("Paste should expand to fill selection", getExpectedPaste(1, 1, clipRows), getActualPaste(testGrid));
    }

    @Test
    public void testExpandedPasteIntoSkinnySelection() throws Exception
    {
        final List<List<String>> clipRows = List.of(
                List.of("A", "B"),
                List.of("C", "D"),
                List.of("E", "F"));

        EditableGrid testGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        testGrid.addRows(7);
        Dimension size = new Dimension(5, 7);

        log("Test expand right");
        testGrid.selectCellRange(testGrid.getCell(0, PASTE_1), testGrid.getCell(1, PASTE_4));
        actionPaste(null, rowsToString(clipRows));
        assertEquals("Paste should expand to fill selection", getExpectedPaste(2, 1, size, clipRows), getActualPaste(testGrid));
        testGrid.clearAllCells();

        log("Test expand down");
        testGrid.selectCellRange(testGrid.getCell(0, PASTE_1), testGrid.getCell(5, PASTE_1));
        actionPaste(null, rowsToString(clipRows));
        assertEquals("Paste should expand to fill selection", getExpectedPaste(1, 2, size, clipRows), getActualPaste(testGrid));
    }

    private String getActualPaste(EditableGrid testGrid)
    {
        List<Map<String, String>> gridData = testGrid.getGridData(PASTE_1, PASTE_2, PASTE_3, PASTE_4, PASTE_5);
        List<List<String>> rows = gridData.stream().map(r -> List.of(r.get(PASTE_1), r.get(PASTE_2), r.get(PASTE_3), r.get(PASTE_4), r.get(PASTE_5))).toList();
        return rowsToString(rows);
    }

    private static String getExpectedPaste(int colMultiplier, int rowMultiplier, List<List<String>> rows)
    {
        return getExpectedPaste(colMultiplier, rowMultiplier, new Dimension(5, 5), rows);
    }

    private static String getExpectedPaste(int colMultiplier, int rowMultiplier, Dimension size, List<List<String>> clipRows)
    {
        List<List<String>> wideRows = new ArrayList<>();
        for (List<String> row : clipRows)
        {
            List<String> wideRow = new ArrayList<>();
            for (int i = 0; i < colMultiplier; i++)
            {
                wideRow.addAll(row);
            }

            while (wideRow.size() < size.getWidth())
            {
                wideRow.add(""); // add padding
            }
            wideRows.add(wideRow);
        }

        List<List<String>> tallRows = new ArrayList<>();
        for (int i = 0; i < rowMultiplier; i++)
        {
            tallRows.addAll(wideRows);
        }
        if (tallRows.size() < size.getHeight())
        {
            List<String> paddingRow = new ArrayList<>();
            for (int i = 0; i < size.getWidth(); i++)
            {
                paddingRow.add("");
            }
            while (tallRows.size() < size.getHeight())
            {
                tallRows.add(paddingRow);
            }
        }

        return rowsToString(tallRows);
    }

    private static String rowsToString(List<List<String>> rows)
    {
        return rows.stream()
                .map(row -> String.join("\t", row))
                .collect(Collectors.joining("\n"));
    }

    private static void duplicateRow(StringBuilder sb, String row, int count)
    {
        for (int i = 0; i < count - 1; i++)
        {
            sb.append(row).append("\t");
        }
        sb.append(row);
    }

    private static List<WebElement> setCellValues(EditableGrid testGrid, String ascString, Object... values)
    {
        List<WebElement> cells = new ArrayList<>();
        List.of(values).forEach(value -> cells.add(testGrid.setCellValue(cells.size(), ascString, value)));
        return cells;
    }

    private EditableGrid goToEditableGrid(String sampleType)
    {
        return CoreComponentsTestPage.beginAt(this, getProjectName())
                .getEditableGrid("Samples", sampleType);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "EditableGridTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
