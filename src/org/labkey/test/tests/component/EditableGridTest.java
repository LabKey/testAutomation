package org.labkey.test.tests.component;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private static final List<String> TEXT_CHOICES = Arrays.asList("red", "Orange", "YELLOW");
    private static final String LOOKUP_LIST = "Fruits";
    private static final List<String> LOOKUP_CHOICES = Arrays.asList("apple", "Orange", "kiwi");

    private static final String ALL_TYPE_SAMPLE_TYPE = "AllFieldsSampleType";
    private static final String STR_FIELD_NAME = "Str Col";
    private static final String REQ_STR_FIELD_NAME = "Str Col Req";
    private static final String INT_FIELD_NAME = "Int Col";
    private static final String REQ_INT_FIELD_NAME = "Int Col Req";
    private static final String DATE_FIELD_NAME = "Date Col";
    private static final String REQ_DATETIME_FIELD_NAME = "Datetime Col Req";
    private static final String TIME_FIELD_NAME = "Time Col";
    private static final String REQ_TIME_FIELD_NAME = "Time Col Req";
    private static final String FLOAT_FIELD_NAME = "Float Col";
    private static final String BOOL_FIELD_NAME = "Bool Col";
    private static final String TEXTCHOICE_FIELD_NAME = "Textchoice Col";
    private static final String REQ_TEXTCHOICE_FIELD_NAME = "Textchoice Col Req";
    private static final String LOOKUP_FIELD_NAME = "Lookup Col";
    private static final String REQ_LOOKUP_FIELD_NAME = "Lookup Col Req";
    private static final FieldDefinition STR_FIELD;
    private static final FieldDefinition REQ_STR_FIELD;
    private static final FieldDefinition INT_FIELD;
    private static final FieldDefinition REQ_INT_FIELD;
    private static final FieldDefinition DATE_FIELD;
    private static final FieldDefinition REQ_DATETTIME_FIELD;
    private static final FieldDefinition TIME_FIELD;
    private static final FieldDefinition REQ_TIME_FIELD;
    private static final FieldDefinition BOOLEAN_FIELD;
    private static final FieldDefinition FLOAT_FIELD;
    private static final FieldDefinition TEXTCHOICE_FIELD;
    private static final FieldDefinition REQ_TEXTCHOICE_FIELD;
    private static final FieldDefinition LOOKUP_FIELD;
    private static final FieldDefinition REQ_LOOKUP_FIELD;
    // TODO date fields

    final List<String> ALL_FIELD_NAMES = Arrays.asList(STR_FIELD_NAME, REQ_STR_FIELD_NAME, INT_FIELD_NAME, REQ_INT_FIELD_NAME,
            DATE_FIELD_NAME, REQ_DATETIME_FIELD_NAME, TIME_FIELD_NAME, REQ_TIME_FIELD_NAME,
            BOOL_FIELD_NAME, FLOAT_FIELD_NAME, TEXTCHOICE_FIELD_NAME, REQ_TEXTCHOICE_FIELD_NAME, LOOKUP_FIELD_NAME, REQ_LOOKUP_FIELD_NAME);

    static
    {
        STR_FIELD = new FieldDefinition(STR_FIELD_NAME, FieldDefinition.ColumnType.String);
        STR_FIELD.setScale(10);
        REQ_STR_FIELD = new FieldDefinition(REQ_STR_FIELD_NAME, FieldDefinition.ColumnType.String);
        REQ_STR_FIELD.setScale(10);
        REQ_STR_FIELD.setRequired(true);
        INT_FIELD = new FieldDefinition(INT_FIELD_NAME, FieldDefinition.ColumnType.Integer);
        REQ_INT_FIELD = new FieldDefinition(REQ_INT_FIELD_NAME, FieldDefinition.ColumnType.Integer);
        REQ_INT_FIELD.setRequired(true);
        DATE_FIELD = new FieldDefinition(DATE_FIELD_NAME, FieldDefinition.ColumnType.Date);
        REQ_DATETTIME_FIELD = new FieldDefinition(REQ_DATETIME_FIELD_NAME, FieldDefinition.ColumnType.DateAndTime);
        REQ_DATETTIME_FIELD.setRequired(true);
        TIME_FIELD = new FieldDefinition(TIME_FIELD_NAME, FieldDefinition.ColumnType.Time);
        REQ_TIME_FIELD = new FieldDefinition(REQ_TIME_FIELD_NAME, FieldDefinition.ColumnType.Time);
        REQ_TIME_FIELD.setRequired(true);
        FLOAT_FIELD = new FieldDefinition(FLOAT_FIELD_NAME, FieldDefinition.ColumnType.Decimal);
        BOOLEAN_FIELD = new FieldDefinition(BOOL_FIELD_NAME, FieldDefinition.ColumnType.Boolean);
        TEXTCHOICE_FIELD = new FieldDefinition(TEXTCHOICE_FIELD_NAME, FieldDefinition.ColumnType.TextChoice);
        TEXTCHOICE_FIELD.setTextChoiceValues(TEXT_CHOICES);
        REQ_TEXTCHOICE_FIELD = new FieldDefinition(REQ_TEXTCHOICE_FIELD_NAME, FieldDefinition.ColumnType.TextChoice);
        REQ_TEXTCHOICE_FIELD.setTextChoiceValues(TEXT_CHOICES);
        REQ_TEXTCHOICE_FIELD.setRequired(true);
        LOOKUP_FIELD = new FieldDefinition(LOOKUP_FIELD_NAME, new FieldDefinition.IntLookup(null, "lists", LOOKUP_LIST));
        REQ_LOOKUP_FIELD = new FieldDefinition(REQ_LOOKUP_FIELD_NAME, new FieldDefinition.IntLookup(null, "lists", LOOKUP_LIST));
        REQ_LOOKUP_FIELD.setRequired(true);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        ((EditableGridTest) getCurrentTest()).doSetup();
    }

    private void createLookupList(Connection connection) throws IOException, CommandException
    {
        ListDefinition listDef = new IntListDefinition(LOOKUP_LIST, "Key");
        listDef.addField(new FieldDefinition("Name", FieldDefinition.ColumnType.String));
        listDef.create(connection, getProjectName());
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (String value : LOOKUP_CHOICES)
            rows.add(Map.of("Name", value));
        final InsertRowsCommand insertCommand = new InsertRowsCommand("lists", LOOKUP_LIST);
        insertCommand.setRows(rows);
        insertCommand.execute(connection, getProjectName());
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        Connection connection = createDefaultConnection();
        createLookupList(connection);

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

        new SampleTypeDefinition(ALL_TYPE_SAMPLE_TYPE)
                .setFields(
                        List.of(
                                STR_FIELD, REQ_STR_FIELD, INT_FIELD, REQ_INT_FIELD, DATE_FIELD, REQ_DATETTIME_FIELD, TIME_FIELD, REQ_TIME_FIELD,
                                BOOLEAN_FIELD, FLOAT_FIELD, TEXTCHOICE_FIELD, REQ_TEXTCHOICE_FIELD, LOOKUP_FIELD, REQ_LOOKUP_FIELD
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

    @Test
    public void testInputCellValidation()
    {
        EditableGrid testGrid = goToEditableGrid(ALL_TYPE_SAMPLE_TYPE);
        testGrid.addRows(2);

        log("Verify no warnings when page first load");
        checker().verifyEquals("Cell warning should be absent when a row is added on page load",
                0, Locator.tagWithClass("div", "cell-warning").findElements(testGrid).size());

        log("Input empty string for required field should trigger cell warning.");
        testGrid.setCellValue(1, REQ_STR_FIELD_NAME + " *", " ");
        checker().verifyEquals("Cell warning status not as expected at row " + 1 + " for col " + REQ_STR_FIELD_NAME, true, testGrid.hasCellWarning(1, REQ_STR_FIELD_NAME + " *"));
        checker().verifyEquals("Cell warning msg not as expected at row " + 1 + " for col " + REQ_STR_FIELD_NAME, REQ_STR_FIELD_NAME + " is required.", testGrid.getCellPopoverText(1, REQ_STR_FIELD_NAME + " *"));
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(1, REQ_INT_FIELD_NAME + " *", " ");
        checker().verifyEquals("Cell warning status not as expected at row " + 1 + " for col " + REQ_INT_FIELD_NAME, true, testGrid.hasCellWarning(1, REQ_INT_FIELD_NAME + " *"));
        checker().verifyEquals("Cell warning msg not as expected at row " + 1 + " for col " + REQ_INT_FIELD_NAME, "Invalid integer." + REQ_INT_FIELD_NAME + " is required.", testGrid.getCellPopoverText(1, REQ_INT_FIELD_NAME + " *"));

        log("Correct values should remove cell warning, keep entering wrong values should update warning");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, STR_FIELD_NAME, "");
        mouseOver(testGrid.getCell(0, "Row"));
        testGrid.setCellValue(1, STR_FIELD_NAME, "This value is too long");
        mouseOver(testGrid.getCell(0, "Row"));
        testGrid.setCellValue(0, REQ_STR_FIELD_NAME + " *", "good");
        mouseOver(testGrid.getCell(0, "Row"));
        testGrid.setCellValue(1, REQ_STR_FIELD_NAME + " *", "This value is too long");
        checker().verifyEquals("Cell warning status not as expected at row " + 0 + " for col " + STR_FIELD_NAME, false, testGrid.hasCellWarning(0, STR_FIELD_NAME));
        checker().verifyEquals("Cell warning msg not as expected at row " + 1 + " for col " + STR_FIELD_NAME, "22/10 characters", testGrid.getCellPopoverText(1, STR_FIELD_NAME));
        checker().verifyEquals("Cell warning status not as expected at row " + 0 + " for col " + REQ_STR_FIELD_NAME, false, testGrid.hasCellWarning(0, REQ_STR_FIELD_NAME + " *"));
        checker().verifyEquals("Cell warning msg not as expected at row " + 1 + " for col " + REQ_STR_FIELD_NAME, "22/10 characters", testGrid.getCellPopoverText(1, REQ_STR_FIELD_NAME + " *"));

        log("Input invalid data type value should trigger cell warnings.");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, INT_FIELD_NAME, "1.23");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, FLOAT_FIELD_NAME, "not float");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, BOOL_FIELD_NAME, "not bool");
        checker().verifyEquals("Cell warning msg not as expected at row " + 0 + " for col " + INT_FIELD_NAME, "Invalid integer", testGrid.getCellPopoverText(0, INT_FIELD_NAME));
        checker().verifyEquals("Cell warning msg not as expected at row " + 0 + " for col " + FLOAT_FIELD_NAME, "Invalid decimal", testGrid.getCellPopoverText(0, FLOAT_FIELD_NAME));
        checker().verifyEquals("Cell warning msg not as expected at row " + 0 + " for col " + BOOL_FIELD_NAME, "Invalid boolean", testGrid.getCellPopoverText(0, BOOL_FIELD_NAME));

        log("Correct values should remove data type warning.");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, INT_FIELD_NAME, "123");
        checker().verifyFalse("Cell warning should disappear after correcting value", testGrid.hasCellWarning(0, INT_FIELD_NAME));

    }

    @Test
    public void testPasteCellValidation()
    {
        final List<List<String>> clipRows = List.of(
                List.of("A", "B", "1", "2", "2024-07-07", "2024-07-07 11:23", "11:23 PM", "15:30", "Y", "-1.1", "red", "Orange", "Orange", "kiwi"),
                List.of("", "", "", "", "", "", "", "", "", "", "", "", "", ""),
                List.of("This value is too long", "This value is too long", "not a number", "1.234", "not date", "25-25-2025", "not time", "ab c", "not boolean", "not float", "wrong text choice", "bad choice", "bad lookup", "bad lookup"),
                List.of("", "abc", "", "1", "0218-11-18 00:00" /*Issue 46767*/, "0218-11-18 00:00", "0218-11-18 00:00", "0218-11-18 00:00", "", "", "", "Orange", "", "kiwi")
        );

        EditableGrid testGrid = goToEditableGrid(ALL_TYPE_SAMPLE_TYPE);
        testGrid.addRows(3);

        log("Pasting invalid values");
        testGrid.selectCell(0, STR_FIELD_NAME);

        actionPaste(null, rowsToString(clipRows));
        List<List<String>> expectedCellWarnings = List.of(
                Arrays.asList(null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                Arrays.asList(null, REQ_STR_FIELD_NAME + " is required.", null, REQ_INT_FIELD_NAME + " is required.", null, REQ_DATETIME_FIELD_NAME + " is required.", null, REQ_TIME_FIELD_NAME + " is required.", null, null, null, REQ_TEXTCHOICE_FIELD_NAME + " is required.", null, REQ_LOOKUP_FIELD_NAME + " is required."),
                Arrays.asList("22/10 characters", "22/10 characters", "Invalid integer", "Invalid integer", "Invalid date", "Invalid date time", "Invalid time", "Invalid time", "Invalid boolean", "Invalid decimal", "Invalid text choice", "Invalid text choice", "Could not find data for \"bad lookup\"", "Could not find data for \"bad lookup\"")
        );

        log("Verify pasted values triggers cell warnings");
        for (int row = 0; row < expectedCellWarnings.size(); row++)
        {
            List<String> expectedWarnings = expectedCellWarnings.get(row);
            verifyCellWarning(testGrid, expectedWarnings, row);
        }
        mouseOver(testGrid.getCell(0, STR_FIELD_NAME)); // dismiss warning popup

        log("Correct missing required fields should remove corresponding cell warnings");
        testGrid.setCellValue(1, REQ_STR_FIELD_NAME + " *", " ");
        checker().verifyTrue("Cell warning should be present after setting another invalid value", testGrid.hasCellWarning(1, REQ_STR_FIELD_NAME + " *"));
        mouseOver(testGrid.getCell(0, STR_FIELD_NAME)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_INT_FIELD_NAME + " *", "2");
        mouseOver(testGrid.getCell(0, STR_FIELD_NAME)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_TEXTCHOICE_FIELD_NAME + " *", List.of("Orange"));
        mouseOver(testGrid.getCell(0, STR_FIELD_NAME)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_LOOKUP_FIELD_NAME + " *", List.of("Orange"));
        mouseOver(testGrid.getCell(0, STR_FIELD_NAME)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_STR_FIELD_NAME + " *", "not empty");
        mouseOver(testGrid.getCell(0, STR_FIELD_NAME)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_DATETIME_FIELD_NAME + " *", LocalDateTime.of(2024, 7, 7, 10, 30));
        mouseOver(testGrid.getCell(0, STR_FIELD_NAME)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_TIME_FIELD_NAME + " *", LocalTime.of(2, 30));

        for (int col = 0; col < ALL_FIELD_NAMES.size(); col++)
        {
            String colName = ALL_FIELD_NAMES.get(col);
            if (colName.endsWith(" Req"))
                colName += " *";

            checker().verifyFalse("Cell warning be absent after required values are provided: " + colName, testGrid.hasCellWarning(1, colName));
        }

        log("Enter another bad value should retain cell warning");
        testGrid.setCellValue(2, INT_FIELD_NAME, "bad");
        checker().verifyTrue("Cell warning should be present after setting another invalid value", testGrid.hasCellWarning(2, INT_FIELD_NAME));
        checker().screenShotIfNewError("after required value correction error");

        log("Correct bad data type values should remove paste data warnings");
        testGrid.setCellValue(2, STR_FIELD_NAME, "good");
        testGrid.setCellValue(2, REQ_STR_FIELD_NAME + " *", "good");
        testGrid.setCellValue(2, INT_FIELD_NAME, "1");
        testGrid.setCellValue(2, REQ_INT_FIELD_NAME + " *", "134");
        testGrid.setCellValue(2, BOOL_FIELD_NAME, "on");
        testGrid.setCellValue(2, FLOAT_FIELD_NAME, "1.23");
        testGrid.setCellValue(2, TEXTCHOICE_FIELD_NAME, List.of("red"));
        testGrid.setCellValue(2, REQ_TEXTCHOICE_FIELD_NAME + " *", List.of("red"));
        testGrid.setCellValue(2, LOOKUP_FIELD_NAME, List.of("kiwi"));
        testGrid.setCellValue(2, REQ_LOOKUP_FIELD_NAME + " *", List.of("kiwi"));
        testGrid.setCellValue(2, DATE_FIELD_NAME, LocalDate.of(2024, 7, 7));
        testGrid.setCellValue(2, TIME_FIELD_NAME, LocalTime.of(2, 30));
        testGrid.setCellValue(2, REQ_DATETIME_FIELD_NAME + " *", LocalDateTime.of(2024, 7, 7, 10, 30));
        testGrid.setCellValue(2, REQ_TIME_FIELD_NAME + " *", LocalTime.of(2, 30));

        for (int col = 0; col < ALL_FIELD_NAMES.size(); col++)
        {
            String colName = ALL_FIELD_NAMES.get(col);
            if (colName.endsWith(" Req"))
                colName += " *";

            checker().verifyFalse("Cell warning should be absent after correct values are provided: " + colName, testGrid.hasCellWarning(2, colName));
        }
        checker().screenShotIfNewError("after data correction error");

        log("Issue 46767: start date before 1000-01-01");
        for (int col = 0; col < ALL_FIELD_NAMES.size(); col++)
        {
            String colName = ALL_FIELD_NAMES.get(col);
            if (colName.endsWith(" Req"))
                colName += " *";

            // start date before year 1000 shouldn't trigger warning
            checker().verifyFalse("Cell warning should not be present for: " + colName, testGrid.hasCellWarning(0, colName));
        }

        log("Verify UI is interactable with values before 1000-01-01");
        testGrid.setCellValue(3, DATE_FIELD_NAME, LocalDate.of(2024, 7, 7));
        testGrid.setCellValue(3, TIME_FIELD_NAME, LocalTime.of(2, 30));
        testGrid.setCellValue(3, REQ_DATETIME_FIELD_NAME + " *", LocalDate.of(2024, 7, 7));
        testGrid.setCellValue(3, REQ_TIME_FIELD_NAME + " *", LocalTime.of(2, 30));

        checker().screenShotIfNewError("Issue 46767");

        testGrid.clearAllCells();

    }

    private void verifyCellWarning(EditableGrid testGrid, List<String> expectedWarnings, int rowId)
    {
        for (int col = 0; col < ALL_FIELD_NAMES.size(); col++)
        {
            String expectedWarning = expectedWarnings.get(col);
            String colName = ALL_FIELD_NAMES.get(col);
            if (colName.endsWith(" Req"))
                colName += " *";

            checker().verifyEquals("Cell warning status not as expected at row " + rowId + " for col " + colName, !StringUtils.isEmpty(expectedWarning), testGrid.hasCellWarning(rowId, colName));
            if (!StringUtils.isEmpty(expectedWarning))
                checker().verifyEquals("Cell warning msg not as expected at row " + rowId + " for col " + colName, expectedWarning, testGrid.getCellPopoverText(rowId, colName));
        }
    }

    @Test
    public void testFillCellValidation()
    {
        final List<List<String>> clipRows = List.of(
                List.of("This value is too long", "", "not a number", "1.234", "not a date", "", "not a time", "", "not boolean", "not float", "wrong text choice", "bad choice", "bad lookup", "")
        );

        EditableGrid testGrid = goToEditableGrid(ALL_TYPE_SAMPLE_TYPE);
        testGrid.addRows(3);

        log("Start with pasting invalid values, so we can fill down invalid values for dropdowns and data/time inputs");
        testGrid.selectCell(0, STR_FIELD_NAME);
        actionPaste(null, rowsToString(clipRows));

        WebElement fillFrom = testGrid.getCell(0, REQ_LOOKUP_FIELD_NAME + " *");
        scrollIntoView(fillFrom);
        WebElement fillTo = testGrid.getCell(2, REQ_LOOKUP_FIELD_NAME + " *");
        testGrid.dragFill(fillFrom, fillTo);

        List<String> expectedWarnings = Arrays.asList("22/10 characters", REQ_STR_FIELD_NAME + " is required.", "Invalid integer", "Invalid integer",
                "Invalid date", REQ_DATETIME_FIELD_NAME + " is required.", "Invalid time", REQ_TIME_FIELD_NAME + " is required.",
                "Invalid boolean", "Invalid decimal", "Invalid text choice", "Invalid text choice", "Could not find data for \"bad lookup\"", REQ_LOOKUP_FIELD_NAME + " is required.");

        log("Verify filled down cells have warnings");
        for (int i = 0; i < 3; i++)
            verifyCellWarning(testGrid, expectedWarnings, i);
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
