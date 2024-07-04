package org.labkey.test.tests.component;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
    private static final String FILL_MULTI_LINE = "Filling Multi Line";
    private static final String FILL_INT = "Filling Int";
    private static final String FILL_DATE = "Filling Date";

    private static final String PASTING_SAMPLE_TYPE = "PastingSampleType";
    private static final String PASTE_1 = "Paste Column 1";
    private static final String PASTE_2 = "Paste Column 2";
    private static final String PASTE_3 = "Paste Column 3";
    private static final String PASTE_4 = "Paste Column 4";
    private static final String PASTE_5 = "Paste Column 5";
    private static final String PASTE_ML = "Paste Multi Line";

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
                                new FieldDefinition(FILL_MULTI_LINE, FieldDefinition.ColumnType.MultiLine),
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
                                new FieldDefinition(PASTE_5, FieldDefinition.ColumnType.String),
                                new FieldDefinition(PASTE_ML, FieldDefinition.ColumnType.MultiLine)
                        ))
                .create(connection, getProjectName());
    }

    @Test
    public void testTooWideErrorCase()
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

        String stringValue = "ABC-1";
        String multiLineValue = "Line 1" + System.lineSeparator() + "Line 2";
        String intValue = "1";

        testGrid.addRows(4);

        // Get the various row heights before adding a value to the multiLine field.
        WebElement gridRow = Locator.tag("tr").findElements(testGrid).get(1);
        int rowHeightBefore = gridRow.getSize().height;
        var totalHeightBefore = new Object(){int size = 0; };
        Locator.tag("tr").findElements(testGrid).forEach(gr -> totalHeightBefore.size = totalHeightBefore.size + gr.getSize().height);
        WebElement topLeft = testGrid.setCellValue(0, FILL_STRING, stringValue);

        testGrid.setCellValue(0, FILL_INT, intValue);
        testGrid.setCellValue(0, FILL_MULTI_LINE, multiLineValue);

        int rowHeightAfter = gridRow.getSize().height;

        // Only going to check that the row height got bigger after adding text.
        checker().withScreenshot()
                .verifyTrue("Row height should have increased after putting multiple lines into the MultiLine field.",
                        rowHeightAfter > rowHeightBefore);

        WebElement bottomRight = testGrid.setCellValue(0, FILL_DATE, now);
        WebElement fillTo = testGrid.getCell(2, FILL_DATE);

        testGrid.selectCellRange(topLeft, bottomRight);
        testGrid.dragFill(bottomRight, fillTo);

        checker().verifyEquals("Drag-fill should have filled " + FILL_STRING,
                List.of(stringValue, stringValue, stringValue, ""),
                testGrid.getColumnData(FILL_STRING));
        checker().verifyEquals("Drag-fill should have filled " + FILL_MULTI_LINE,
                List.of(multiLineValue, multiLineValue, multiLineValue, ""),
                testGrid.getColumnData(FILL_MULTI_LINE));
        checker().verifyEquals("Drag-fill should have filled " + FILL_INT,
                List.of(intValue, intValue, intValue, ""),
                testGrid.getColumnData(FILL_INT));
        checker().verifyEquals("Drag-fill should have filled " + FILL_DATE,
                List.of(EditableGrid.DATE_FORMAT.format(now),
                        EditableGrid.DATE_FORMAT.format(now.plusDays(1)),
                        EditableGrid.DATE_FORMAT.format(now.plusDays(2)),
                        ""),
                testGrid.getColumnData(FILL_DATE));

        // Check that pasting increased the size of all the rows.
        var totalHeightAfter = new Object(){int size = 0; };
        Locator.tag("tr").findElements(testGrid).forEach(gr -> totalHeightAfter.size = totalHeightAfter.size + gr.getSize().height);

        checker().withScreenshot()
                .verifyTrue("The total height of all the rows should have increases after the paste.",
                        totalHeightBefore.size + (3 * rowHeightBefore) >= totalHeightAfter.size);
    }

    @Test
    public void testDragFillMultipleRows()
    {
        final LocalDateTime now = LocalDate.of(2019, 1, 30).atTime(14, 30);

        EditableGrid testGrid = goToEditableGrid(FILLING_SAMPLE_TYPE);

        String mlRow1 = "Line 1" + System.lineSeparator() + "Line 2";
        String mlRow2 = "Line 3" + System.lineSeparator() + "Line 4";

        testGrid.addRows(7);
        WebElement topLeft = setCellValues(testGrid, FILL_STRING, "QWE", "ASD", "ZXC").get(0);

        // Just for fun put an empty row between.
        testGrid.setCellValue(0, FILL_MULTI_LINE, mlRow1);
        testGrid.setCellValue(2, FILL_MULTI_LINE, mlRow2);

        WebElement bottomRight = setCellValues(testGrid, FILL_DATE, now, now.plusDays(3), now.plusDays(1)).get(2);
        WebElement fillTo = testGrid.getCell(5, FILL_DATE);

        testGrid.selectCellRange(topLeft, bottomRight);
        testGrid.dragFill(bottomRight, fillTo);

        checker().verifyEquals("Drag-fill should have filled " + FILL_STRING,
                List.of("QWE", "ASD", "ZXC", "QWE", "ASD", "ZXC", ""),
                testGrid.getColumnData(FILL_STRING));
        checker().verifyEquals("Drag-fill should have filled " + FILL_MULTI_LINE,
                List.of(mlRow1, "", mlRow2, mlRow1, "", mlRow2, ""),
                testGrid.getColumnData(FILL_MULTI_LINE));
        checker().verifyEquals("Drag-fill should have filled " + FILL_DATE,
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
    public void testShiftClick()
    {
        EditableGrid testGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        testGrid.addRows(15);
        testGrid.shiftSelectRange(2, 7);

        // select a range
        checker().verifyFalse(String.format("row %d should not be checked", 1), testGrid.isRowSelected(1));
        for (int i=2; i<7; i++)
        {
            checker().verifyTrue(String.format("row %d should be checked", i), testGrid.isRowSelected(i));
        }
        checker().verifyFalse(String.format("row %d should not be checked", 8), testGrid.isRowSelected(8));
        checker().screenShotIfNewError("unexpected selection range");

        // select a non-adjacent range
        testGrid.shiftSelectRange(10, 13);
        checker().verifyFalse(String.format("row %d should not be checked", 9), testGrid.isRowSelected(9));
        for (int i=10; i<13; i++)
        {
            checker().verifyTrue(String.format("row %d should be checked", i), testGrid.isRowSelected(i));
        }
        checker().verifyFalse(String.format("row %d should not be checked", 14), testGrid.isRowSelected(14));
        // ensure the first range is still selected
        for (int i=2; i<7; i++)
        {
            checker().verifyTrue(String.format("row %d should be checked", i), testGrid.isRowSelected(i));
        }
        checker().screenShotIfNewError("unexpected selections1");

        // now de-select cells 6 to 3
        testGrid.shiftSelectRange(6, 3);
        // ensure they are deselected
        for (int i=3; i<6; i++)
        {
            checker().verifyFalse(String.format("row %d should not be checked", i), testGrid.isRowSelected(i));
        }
        // make sure 2 and 7 are still selected
        checker().verifyTrue(String.format("row %d should be checked", 2), testGrid.isRowSelected(2));
        checker().verifyTrue(String.format("row %d should be checked", 7), testGrid.isRowSelected(7));
        // make sure 10-13 are still selected
        for (int i=10; i<13; i++)
        {
            checker().verifyTrue(String.format("row %d should be checked", i), testGrid.isRowSelected(i));
        }
        checker().screenShotIfNewError("unexpected selections2");

        // now select 0-14
        testGrid.shiftSelectRange(0, 14);
        checker().withScreenshot("all_rows_not_selected")
                .verifyTrue("not all rows are selected",
                testGrid.areAllRowsSelected());
    }

    /*
        Tests the scenario where a row is selected, then another, and another are shift-selected
        expects the range-bump to redefine the selected range
     */
    @Test public void testShiftSelect_bumpSelect()
    {
        EditableGrid testGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        testGrid.addRows(15);

        Locator boxes = Locator.tag("tr").child("td")
                .child(Locator.tagWithAttribute("input", "type", "checkbox"));
        var checkBoxes = boxes.findElements(testGrid);
        scrollIntoView(checkBoxes.get(0), true); // bring as much of the grid into view as possible

        new Actions(getDriver())
                .click(checkBoxes.get(2))
                .keyDown(Keys.SHIFT)
                .click(checkBoxes.get(5))
                .click(checkBoxes.get(7))
                .keyUp(Keys.SHIFT)
                .perform();

        // make sure 2-7 are still selected
        for (int i=2; i<7; i++)
        {
            checker().verifyTrue(String.format("row %d should be checked", i), testGrid.isRowSelected(i));
        }
        checker().screenShotIfNewError("unexpected_selection_range");

        // clear all selections
        testGrid.selectAll(false);

        // now select a row and remove it
        new Actions(getDriver())
                .click(checkBoxes.get(2))
                .perform();
        testGrid.clickRemove();
        checkBoxes = boxes.findElements(testGrid);

        // verify shift-select to another row does not select the range from the now-removed row
        new Actions(getDriver())
                .keyDown(Keys.SHIFT)
                .click(checkBoxes.get(7))
                .keyUp(Keys.SHIFT)
                .perform();

        for (int i=2; i<6; i++)
        {
            checker().verifyFalse(String.format("row %d should not be checked", i), testGrid.isRowSelected(i));
        }
        checker().verifyTrue(String.format("row %d should be checked", 7), testGrid.isRowSelected(7));
        checker().screenShotIfNewError("unexpected_selection_range");
    }

    @Test
    public void testExpandedPaste()
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
    public void testInvalidExpandedPaste()
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
    public void testExpandedPasteIntoSkinnySelection()
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

    @Test
    public void testPasteIntoMultiLine()
    {

        EditableGrid editableGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        editableGrid.addRows(1);

        List<String> expectedValues = List.of("Line 1",
                "Line 2",
                "Line 3",
                "Line 4",
                "Line 5"
                );

        StringBuilder sbPasteString = new StringBuilder();
        Iterator<String> iStr = expectedValues.iterator();
        while (iStr.hasNext())
        {
            sbPasteString.append(iStr.next());

            if (iStr.hasNext())
            {
                sbPasteString.append(System.lineSeparator());
            }

        }

        log("Test double clicking the MultiLine cell and pasting in a multi-line string.");
        WebElement gridCell = editableGrid.getCell(0, PASTE_ML);
        doubleClick(gridCell);
        WebElement textArea = Locator.tag("textarea").findElement(gridCell);
        actionPaste(textArea, sbPasteString.toString());

        // Exit "edit" mode.
        textArea.sendKeys(Keys.ENTER);

        waitFor(()->shortWait().until(ExpectedConditions.stalenessOf(textArea)),
                "TextArea did not go away.", 500);

        checker().verifyEquals("All lines should have gone into one cell.",
                1, editableGrid.getRowCount());

        // Using a waitFor because there is a slight pause before the cell actually has the value.
        checker().verifyTrue("Value in the cell is not as expected.",
                waitFor(()->editableGrid.getCellValue(0, PASTE_ML).contentEquals(sbPasteString), 1_000));

        checker().screenShotIfNewError("Paste_Into_One_Cell_Error");

        log("Reset the grid.");
        editableGrid.selectAll(true);
        editableGrid.clickRemove();

        log("Paste in a multi-line string without putting the cell into edit mode.");
        editableGrid.addRows(1);
        editableGrid.pasteFromCell(0, PASTE_ML, sbPasteString.toString());

        checker().verifyEquals("Each line should have created a new row.",
                expectedValues.size(), editableGrid.getRowCount());

        checker().verifyEquals(String.format("Values in column '%s' not as expected.", PASTE_ML),
                expectedValues, editableGrid.getColumnData(PASTE_ML));

        checker().screenShotIfNewError("Paste_Into_Multiple_Cells_Error");

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
