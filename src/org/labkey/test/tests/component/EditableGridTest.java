package org.labkey.test.tests.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.FieldDefinition.IntLookup;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.EscapeUtil;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.labkey.test.util.TestDataGenerator.randomDomainName;

@Category({Daily.class})
public class EditableGridTest extends BaseWebDriverTest
{
    private static final int MAX_LENGTH = 32; // Avoid excessively long field names to avoid blocking clicks
    private static final String EXTRAPOLATING_SAMPLE_TYPE = randomDomainName("ExtrapolatingSampleType");
    private static final FieldInfo ASC_STRING = FieldInfo.random("Ascending String", ColumnType.String, MAX_LENGTH);
    private static final FieldInfo DESC_STRING = FieldInfo.random("Descending String", ColumnType.String, MAX_LENGTH);
    private static final FieldInfo ASC_INT = FieldInfo.random("Ascending Int", ColumnType.Integer, MAX_LENGTH);
    private static final FieldInfo DESC_INT = FieldInfo.random("Descending Int", ColumnType.Integer, MAX_LENGTH);
    private static final FieldInfo ASC_DATE = FieldInfo.random("Ascending Date", ColumnType.DateAndTime, MAX_LENGTH);
    private static final FieldInfo DESC_DATE = FieldInfo.random("Descending Date", ColumnType.DateAndTime, MAX_LENGTH);

    private static final String FILLING_SAMPLE_TYPE = randomDomainName("FillingSampleType");
    private static final FieldInfo FILL_STRING = FieldInfo.random("Filling String", ColumnType.String, MAX_LENGTH);
    private static final FieldInfo FILL_MULTI_LINE = FieldInfo.random("Filling Multi Line", ColumnType.MultiLine, MAX_LENGTH);
    private static final FieldInfo FILL_INT = FieldInfo.random("Filling Int", ColumnType.Integer, MAX_LENGTH);
    private static final FieldInfo FILL_DATE = FieldInfo.random("Filling Date", ColumnType.DateAndTime, MAX_LENGTH);

    private static final String PASTING_SAMPLE_TYPE = randomDomainName("PastingSampleType");
    private static final FieldInfo PASTE_1 = FieldInfo.random("Paste Column 1", ColumnType.String, MAX_LENGTH);
    private static final FieldInfo PASTE_2 = FieldInfo.random("Paste Column 2", ColumnType.String, MAX_LENGTH);
    private static final FieldInfo PASTE_3 = FieldInfo.random("Paste Column 3", ColumnType.String, MAX_LENGTH);
    private static final FieldInfo PASTE_4 = FieldInfo.random("Paste Column 4", ColumnType.String, MAX_LENGTH);
    private static final FieldInfo PASTE_5 = FieldInfo.random("Paste Column 5", ColumnType.String, MAX_LENGTH);
    private static final FieldInfo PASTE_ML = FieldInfo.random("Paste Multi Line", ColumnType.MultiLine, MAX_LENGTH);

    private static final List<String> TEXT_CHOICES = Arrays.asList("red", "Orange", "YELLOW");
    private static final String LOOKUP_LIST = randomDomainName("Fruits");
    private static final List<String> LOOKUP_CHOICES = Arrays.asList("apple", "Orange", "kiwi");

    private static final String ALL_TYPE_SAMPLE_TYPE = randomDomainName("AllFieldsSampleType");

    private static final FieldInfo STR_FIELD = FieldInfo.random("strCol", ColumnType.String, MAX_LENGTH)
        .customizeFieldDefinition(fd -> fd.setScale(10));
    private static final FieldInfo REQ_STR_FIELD = FieldInfo.random("strColReq", ColumnType.String, MAX_LENGTH)
        .customizeFieldDefinition(fd -> fd.setScale(10).setRequired(true));
    private static final FieldInfo INT_FIELD = FieldInfo.random("intCol", ColumnType.Integer, MAX_LENGTH);
    private static final FieldInfo REQ_INT_FIELD = FieldInfo.random("intColReq", ColumnType.Integer, MAX_LENGTH)
        .customizeFieldDefinition(fd -> fd.setRequired(true));
    private static final FieldInfo DATE_FIELD = FieldInfo.random("dateCol", ColumnType.Date, MAX_LENGTH);
    private static final FieldInfo REQ_DATETIME_FIELD = FieldInfo.random("datetimeColReq", ColumnType.DateAndTime, MAX_LENGTH)
        .customizeFieldDefinition(fd -> fd.setRequired(true));
    private static final FieldInfo TIME_FIELD = FieldInfo.random("timeCol", ColumnType.Time, MAX_LENGTH);
    private static final FieldInfo REQ_TIME_FIELD = FieldInfo.random("timeColReq", ColumnType.Time, MAX_LENGTH)
        .customizeFieldDefinition(fd -> fd.setRequired(true));
    private static final FieldInfo BOOL_FIELD = FieldInfo.random("boolCol", ColumnType.Boolean, MAX_LENGTH);
    private static final FieldInfo FLOAT_FIELD = FieldInfo.random("floatCol", ColumnType.Decimal, MAX_LENGTH);
    private static final FieldInfo TEXTCHOICE_FIELD = FieldInfo.random("textchoiceCol", ColumnType.TextChoice, MAX_LENGTH)
        .customizeFieldDefinition(fd -> fd.setTextChoiceValues(TEXT_CHOICES));
    private static final FieldInfo REQ_TEXTCHOICE_FIELD = FieldInfo.random("textchoiceColReq", ColumnType.TextChoice, MAX_LENGTH)
        .customizeFieldDefinition(fd -> fd.setRequired(true).setTextChoiceValues(TEXT_CHOICES));
    private static final FieldInfo LOOKUP_FIELD = FieldInfo.random("lookupCol", new IntLookup(null, "lists", LOOKUP_LIST));
    private static final FieldInfo REQ_LOOKUP_FIELD = FieldInfo.random("lookupColReq", new IntLookup(null, "lists", LOOKUP_LIST))
        .customizeFieldDefinition(fd -> fd.setRequired(true));

    final List<FieldInfo> ALL_FIELDS = Arrays.asList(STR_FIELD, REQ_STR_FIELD, INT_FIELD, REQ_INT_FIELD,
        DATE_FIELD, REQ_DATETIME_FIELD, TIME_FIELD, REQ_TIME_FIELD,
        BOOL_FIELD, FLOAT_FIELD, TEXTCHOICE_FIELD, REQ_TEXTCHOICE_FIELD, LOOKUP_FIELD, REQ_LOOKUP_FIELD);

    @BeforeClass
    public static void setupProject() throws Exception
    {
        ((EditableGridTest) getCurrentTest()).doSetup();
    }

    private void createLookupList(Connection connection) throws IOException, CommandException
    {
        ListDefinition listDef = new IntListDefinition(LOOKUP_LIST, "Key");
        listDef.addField(new FieldDefinition("Name", ColumnType.String));
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
                                ASC_STRING.getFieldDefinition(),
                                DESC_STRING.getFieldDefinition(),
                                ASC_INT.getFieldDefinition(),
                                DESC_INT.getFieldDefinition(),
                                ASC_DATE.getFieldDefinition(),
                                DESC_DATE.getFieldDefinition()
                        ))
                .create(connection, getProjectName());
        new SampleTypeDefinition(FILLING_SAMPLE_TYPE)
                .setFields(
                        List.of(
                                FILL_STRING.getFieldDefinition(),
                                FILL_MULTI_LINE.getFieldDefinition(),
                                FILL_INT.getFieldDefinition(),
                                FILL_DATE.getFieldDefinition()
                        ))
                .create(connection, getProjectName());
        new SampleTypeDefinition(PASTING_SAMPLE_TYPE)
                .setFields(
                        List.of(
                                PASTE_1.getFieldDefinition(),
                                PASTE_2.getFieldDefinition(),
                                PASTE_3.getFieldDefinition(),
                                PASTE_4.getFieldDefinition(),
                                PASTE_5.getFieldDefinition(),
                                PASTE_ML.getFieldDefinition()
                        ))
                .create(connection, getProjectName());

        new SampleTypeDefinition(ALL_TYPE_SAMPLE_TYPE)
                .setFields(
                    ALL_FIELDS.stream().map(FieldInfo::getFieldDefinition).toList())
                .create(connection, getProjectName());

        for (String sampleType : List.of(EXTRAPOLATING_SAMPLE_TYPE, FILLING_SAMPLE_TYPE, PASTING_SAMPLE_TYPE, ALL_TYPE_SAMPLE_TYPE))
        {
            // Hide columns from editable grid so that test columns are in view
            String metadataXml = """
                <tables xmlns="http://labkey.org/data/xml">
                  <table tableName="%s" tableDbType="NOT_IN_DB">
                    <columns>
                      <column columnName="Name">
                        <shownInInsertView>false</shownInInsertView>
                      </column>
                      <column columnName="Alias">
                        <shownInInsertView>false</shownInInsertView>
                      </column>
                      <column columnName="MaterialExpDate">
                        <shownInInsertView>false</shownInInsertView>
                      </column>
                      <column columnName="Flag">
                        <shownInInsertView>false</shownInInsertView>
                      </column>
                      <column columnName="SampleState">
                        <shownInInsertView>false</shownInInsertView>
                      </column>
                    </columns>
                  </table>
                </tables>
                """.formatted(EscapeUtil.getMarkupEscapedValue(sampleType));

            SimplePostCommand postCommand = new SimplePostCommand("query", "saveSourceQuery");
            postCommand.setJsonObject(new JSONObject());
            postCommand.getJsonObject().put("ff_metadataText", metadataXml);
            postCommand.getJsonObject().put("schemaName", "samples");
            postCommand.getJsonObject().put("queryName", sampleType);
            postCommand.execute(connection, getProjectName());
        }
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
    @Ignore  // Test disabled until Issue 52226, Issue 51927 are resolved
    public void testDragFillSingleRow()
    {
        final LocalDateTime now = LocalDate.of(2019, 1, 30).atTime(16, 30);

        EditableGrid testGrid = goToEditableGrid(FILLING_SAMPLE_TYPE);

        String stringValue = "ABC-1";
        String multiLineValue = "Line 1\nLine 2";
        String intValue = "1";

        testGrid.addRows(4);

        // Get the various row heights before adding a value to the multiLine field.
        WebElement gridRow = Locator.tag("tr").findElements(testGrid).get(1);
        int rowHeightBefore = gridRow.getSize().height;
        var totalHeightBefore = new MutableInt(0);
        Locator.tag("tr").findElements(testGrid).forEach(gr -> totalHeightBefore.add(gr.getSize().height));
        WebElement topLeft = testGrid.setCellValue(0, FILL_STRING, stringValue);

        testGrid.setCellValue(0, FILL_INT, intValue);
        testGrid.setMultiLineCellValue(0, FILL_MULTI_LINE, multiLineValue);

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
        var totalHeightAfter = new MutableInt(0);
        Locator.tag("tr").findElements(testGrid).forEach(gr -> totalHeightAfter.add(gr.getSize().height));

        checker().withScreenshot()
                .verifyTrue("The total height of all the rows should have increases after the paste.",
                        totalHeightBefore.intValue() + (3 * rowHeightBefore) >= totalHeightAfter.intValue());
    }

    @Test
    @Ignore  // Test disabled until Issue 52226, Issue 51927 are resolved
    public void testDragFillMultipleRows()
    {
        final LocalDateTime now = LocalDate.of(2019, 1, 30).atTime(14, 30);

        EditableGrid testGrid = goToEditableGrid(FILLING_SAMPLE_TYPE);

        String mlRow1 = "Line 1\nLine 2";
        String mlRow2 = "Line 3\nLine 4";

        testGrid.addRows(7);
        WebElement topLeft = setCellValues(testGrid, FILL_STRING, "QWE", "ASD", "ZXC").get(0);

        // Just for fun put an empty row between.
        testGrid.setMultiLineCellValue(0, FILL_MULTI_LINE, mlRow1);
        testGrid.setMultiLineCellValue(2, FILL_MULTI_LINE, mlRow2);

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
        scrollIntoView(checkBoxes.get(2), false);
        checkBoxes.get(2).click();

        shiftClickCheckbox(checkBoxes.get(5));

        shiftClickCheckbox(checkBoxes.get(7));

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
        testGrid.clickDelete();
        checkBoxes = boxes.findElements(testGrid);

        // verify shift-select to another row does not select the range from the now-removed row

        shiftClickCheckbox(checkBoxes.get(7));

        for (int i=2; i<6; i++)
        {
            checker().verifyFalse(String.format("row %d should not be checked", i), testGrid.isRowSelected(i));
        }
        checker().verifyTrue(String.format("row %d should be checked", 7), testGrid.isRowSelected(7));
        checker().screenShotIfNewError("unexpected_selection_range");
    }

    private void shiftClickCheckbox(WebElement el)
    {
        scrollIntoView(el, false);
        new Actions(getDriver())
            .keyDown(Keys.SHIFT)
            .click(el)
            .keyUp(Keys.SHIFT)
            .perform();
    }

    @Test
    public void testExpandedPaste()
    {
        final List<List<String>> clipRows = List.of(
                List.of("A", "B"),
                List.of("C", "D"));

        EditableGrid testGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        testGrid.addRows(5);

        scrollIntoView(testGrid.getCell(3, PASTE_4), false); // Get target area into view
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

    /**
     * <p>
     *     Validate the pasting of text into the multi-line cell edit.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Double click to edit the multi-line cell and then paste text with line breaks into it.</li>
     *         <li>Single click the multi-line cell and validate pasting multiple lines creates new grid rows.</li>
     *     </ul>
     * </p>
     */
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
                sbPasteString.append("\n");
            }

        }

        log("Test double clicking the MultiLine cell and pasting in a multi-line string.");
        WebElement gridCell = editableGrid.activateCellUsingDoubleClick(0, PASTE_ML);
        actionPaste(gridCell, sbPasteString.toString());

        checker().verifyEquals("All lines should have gone into one cell.",
                1, editableGrid.getRowCount());

        // Using a waitFor because there is a slight pause before the cell actually has the value.
        checker().verifyTrue(String.format("Value in the cell is not as expected. Expecting '%s' but found '%s'.",
                        sbPasteString, editableGrid.getCellValue(0, PASTE_ML)),
                waitFor(()->editableGrid.getCellValue(0, PASTE_ML).contentEquals(sbPasteString), 1_000));

        checker().screenShotIfNewError("Paste_Into_One_Cell_Error");

        log("Reset the grid.");
        editableGrid.selectAll(true);
        editableGrid.clickDelete();

        log("Paste in a multi-line string without putting the cell into edit mode.");
        editableGrid.addRows(1);
        editableGrid.pasteFromCell(0, PASTE_ML, sbPasteString.toString());

        checker().verifyTrue("Each line should have created a new row.",
                waitFor(()->expectedValues.size() == editableGrid.getRowCount(), 1_000));

        checker().verifyEquals(String.format("Values in column '%s' not as expected.", PASTE_ML),
                expectedValues, editableGrid.getColumnData(PASTE_ML));

        checker().screenShotIfNewError("Paste_Into_Multiple_Cells_Error");

    }

    /**
     * <p>
     *     Test the sizing of the multi-line edit cell in an editable grid.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Many long lines will size the textArea to the maximum allowed size.</li>
     *         <li>Hitting enter removes focus and the updated text is saved.</li>
     *         <li>One long line (no new line) expands only the width and does not change the heigth.</li>
     *         <li>Clicking outside of the textArea save the changes.</li>
     *         <li>Many shot lines expands only the height and not the width.</li>
     *         <li>Hitting the 'ESC' key closes the textArea and does not save the changes.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testMultiLineCellSizing()
    {

        EditableGrid editableGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        editableGrid.addRows(1);

        // Scroll the last column into view it will make any failure screenshots more useful.
        String lastColumn = editableGrid.getColumnLabels().get(editableGrid.getColumnLabels().size() - 1);
        scrollIntoView(editableGrid.getCell(0, lastColumn));

        int emptyWidth = editableGrid.getCell(0, PASTE_ML).getSize().getWidth();
        int emptyHeight = editableGrid.getCell(0, PASTE_ML).getSize().getHeight();

        int maxWidth = 600;
        int maxHeight = 205;

        log("Test entering several long lines.");

        StringBuilder longLine01 = new StringBuilder();
        StringBuilder longLine02 = new StringBuilder();
        StringBuilder longLine03 = new StringBuilder();

        for(int i = 0; i < 250; i++)
        {
            longLine01.append("a");
            longLine02.append("b");
            longLine03.append("c");
        }

        WebElement gridCell = editableGrid.getCell(0, PASTE_ML);
        log("Cell initial Width & Height: " + gridCell.getSize());
        log("Empty width: " + emptyWidth + " empty height: " + emptyHeight);

        WebElement editCell = editableGrid.activateCellUsingDoubleClick(0, PASTE_ML);

        Actions actions;

        // Add each of the 3 long lines 10 times to the textArea.
        for(int rows = 0; rows < 10; rows++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys(longLine01)
                    .keyDown(Keys.SHIFT)
                    .sendKeys(Keys.ENTER)
                    .keyUp(Keys.SHIFT)
                    .sendKeys(longLine02)
                    .keyDown(Keys.SHIFT)
                    .sendKeys(Keys.ENTER)
                    .keyUp(Keys.SHIFT)
                    .sendKeys(longLine03)
                    .keyDown(Keys.SHIFT)
                    .sendKeys(Keys.ENTER)
                    .keyUp(Keys.SHIFT)
                    .build()
                    .perform();
        }

        checker().verifyTrue(String.format("Height of editable cell, %d, is not as expected %d (+/- 5).",
                        editCell.getSize().height, emptyHeight),
                Math.abs(editCell.getSize().height - emptyHeight) <= 5);

        checker().verifyTrue(String.format("Width of editable cell, %d, is not as expected %d (+/- 10).",
                        editCell.getSize().width, emptyWidth),
                Math.abs(editCell.getSize().width - emptyWidth) <= 10);

        checker().screenShotIfNewError("Editable_Cell_Size_Error");

        actions = new Actions(getDriver());
        actions.pause(500)
                .sendKeys(Keys.ENTER)
                .build()
                .perform();

        scrollIntoView(editableGrid.getCell(0, lastColumn));

        checker().verifyTrue("TextArea should have gone away after hitting <Enter>.",
                shortWait().until(ExpectedConditions.stalenessOf(editCell)).booleanValue());

        WebElement updatedGridCell01 = editableGrid.getCell(0, PASTE_ML);
        checker().verifyTrue("Cell not updated with many long lines.",
                waitFor(()->!updatedGridCell01.getText().isEmpty(), 1_000));

        log("Multi line updated cell Width & Height: " + updatedGridCell01.getSize());

        checker().verifyTrue(String.format("Height of updated cell, %d, is not as expected %d (+/- 5).",
                        updatedGridCell01.getSize().height, maxHeight),
                Math.abs(updatedGridCell01.getSize().height - maxHeight) <= 5);

        checker().verifyTrue(String.format("Width of updated cell, %d, is not as expected %d (+/- 10).",
                        updatedGridCell01.getSize().width, maxWidth),
                Math.abs(updatedGridCell01.getSize().width - maxWidth) <= 10);

        checker().screenShotIfNewError("Many_Long_Lines_Size_Error");

        log("Reset the grid.");
        editableGrid.selectAll(true);
        editableGrid.clickDelete();

        log("Enter one long line.");
        editableGrid.addRows(1);
        scrollIntoView(editableGrid.getCell(0, lastColumn));

        editCell = editableGrid.activateCellUsingDoubleClick(0, PASTE_ML);

        actions = new Actions(getDriver());
        actions.sendKeys(longLine01)
                .build()
                .perform();

        // This should scroll the last cell into view.
        editableGrid.getCell(0, lastColumn).click();

        checker().verifyTrue("TextArea should have gone away after clicking out of the edit cell.",
                shortWait().until(ExpectedConditions.stalenessOf(editCell)).booleanValue());

        WebElement updatedGridCell02 = editableGrid.getCell(0, PASTE_ML);
        checker().verifyTrue("Cell not updated with one long line.",
                waitFor(()->!updatedGridCell02.getText().isEmpty(), 1_000));

        log("Single line updated cell Width & Height: " + updatedGridCell02.getSize());

        int emptyHeightWithScroll = emptyHeight + 15;
        checker().verifyTrue(String.format("Height of updated cell with one line, %d, is not as expected %d (+/- 5).",
                        updatedGridCell02.getSize().height, emptyHeightWithScroll),
                Math.abs(updatedGridCell02.getSize().height - emptyHeightWithScroll) <= 5);

        checker().verifyTrue(String.format("Width of updated cell with one line, %d, is not as expected %d (+/- 10).",
                        updatedGridCell02.getSize().width, maxWidth),
                Math.abs(updatedGridCell02.getSize().width - maxWidth) <= 10);

        checker().screenShotIfNewError("Single_Long_Line_Size_Error");

        log("Reset the grid again.");
        editableGrid.selectAll(true);
        editableGrid.clickDelete();

        log("Enter many short lines.");
        editableGrid.addRows(1);
        scrollIntoView(editableGrid.getCell(0, lastColumn));

        editCell = editableGrid.activateCellUsingDoubleClick(0, PASTE_ML);

        for(int rows = 0; rows < 75; rows++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys("y")
                    .keyDown(Keys.SHIFT)
                    .sendKeys(Keys.ENTER)
                    .keyUp(Keys.SHIFT)
                    .build()
                    .perform();
        }

        actions = new Actions(getDriver());
        actions.pause(500)
                .sendKeys(Keys.ENTER)
                .build()
                .perform();

        checker().verifyTrue("TextArea should have gone away after hitting <Enter>.",
                shortWait().until(ExpectedConditions.stalenessOf(editCell)).booleanValue());

        scrollIntoView(editableGrid.getCell(0, lastColumn));

        WebElement updatedGridCell03 = editableGrid.getCell(0, PASTE_ML);
        checker().verifyTrue("Cell not updated with many short lines.",
                waitFor(()->!updatedGridCell03.getText().isEmpty(), 1_000));

        log("Single line updated cell Width & Height: " + updatedGridCell03.getSize());

        checker().verifyTrue(String.format("Height of updated cell with many short lines, %d, is not as expected %d (+/- 5).",
                        updatedGridCell03.getSize().height, maxHeight),
                Math.abs(updatedGridCell03.getSize().height - maxHeight) <= 5);

        checker().verifyTrue(String.format("Width of updated cell many short lines, %d, is not as expected %d (+/- 10).",
                        updatedGridCell03.getSize().width, emptyWidth),
                Math.abs(updatedGridCell03.getSize().width - emptyWidth) <= 10);

        checker().screenShotIfNewError("Many_Short_Line_Size_Error");

        log("Reset the grid for the last time.");
        editableGrid.selectAll(true);
        editableGrid.clickDelete();

        log("Validate <esc> exits edit mode and does not save.");
        editableGrid.addRows(1);
        scrollIntoView(editableGrid.getCell(0, lastColumn));

        editCell = editableGrid.activateCellUsingDoubleClick(0, PASTE_ML);

        for(int rows = 0; rows < 5; rows++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys("z")
                    .keyDown(Keys.SHIFT)
                    .sendKeys(Keys.ENTER)
                    .keyUp(Keys.SHIFT)
                    .build()
                    .perform();
        }

        actions = new Actions(getDriver());
        actions.pause(500)
                .sendKeys(Keys.ESCAPE)
                .build()
                .perform();

        checker().verifyTrue("TextArea should have gone away after hitting <Esc>.",
                shortWait().until(ExpectedConditions.stalenessOf(editCell)).booleanValue());

        scrollIntoView(editableGrid.getCell(0, lastColumn));

        WebElement updatedGridCell04 = editableGrid.getCell(0, PASTE_ML);
        checker().verifyTrue("Cell should not be updated after hitting <esc>.",
                waitFor(()->updatedGridCell04.getText().isEmpty(), 1_000));

        checker().screenShotIfNewError("Exit_With_Escape_Error");

    }

    /**
     * <p>
     *     Test for <a href=https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=49953>Issue 49953: Shift up-arrow does not undo cell selection in editable grid.</a>
     * </p>
     * <p>
     *     Validates shift arrow selection in a column.
     * </p>
     * <p>
     *     <ul>
     *         <li>Click a cell then use shift down arrow.</li>
     *         <li>Use shift up arrow once.</li>
     *         <li>Use shift up arrow to go above the starting cell.</li>
     *         <li>Release shift key and use arrow key to validate selection is removed.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testShiftArrowSelectVertical()
    {

        EditableGrid editableGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        editableGrid.addRows(10);

        checker().fatal()
                .verifyEquals("There should be no grid cells already selected. Fatal error.",
                        0, editableGrid.getSelectedCells().size());

        List<String> columns = editableGrid.getColumnLabels();
        int column = columns.size() / 2;

        int startRow = 4;
        editableGrid.getCell(startRow, columns.get(column)).click();

        log("Select a few vertical cells in the grid.");
        Actions actions = new Actions(getDriver());
        actions.keyDown(Keys.SHIFT)
                .build()
                .perform();

        // Expected count is the number of down arrows plus the original cell selected.
        int expectedSelectedCount = 5;
        for(int count = 1; count <= 4; count++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys(Keys.ARROW_DOWN)
                    .build()
                    .perform();
        }

        int endRow = startRow + expectedSelectedCount - 1;
        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                column,
                startRow,
                column,
                endRow);

        checker().screenShotIfNewError("SHIFT_DOWN_ARROW_ERROR");

        log("Go up one row.");
        actions = new Actions(getDriver());
        actions.sendKeys(Keys.ARROW_UP)
                .build()
                .perform();

        expectedSelectedCount = expectedSelectedCount - 1;
        endRow = endRow - 1;

        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                column,
                startRow,
                column,
                endRow);

        checker().screenShotIfNewError("SHIFT_UP_ARROW_ERROR");

        log("Go up multiple times, past the original start row.");

        for(int count = 1; count <= 5; count++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys(Keys.ARROW_UP)
                    .build()
                    .perform();
        }

        expectedSelectedCount = 3;
        endRow = startRow;
        startRow = startRow - expectedSelectedCount + 1;
        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                column,
                startRow,
                column,
                endRow);

        checker().screenShotIfNewError("SHIFT_UP_ARROW_ABOVE_START_ERROR");

        log("Validate that releasing the <shift> key and using an <arrow> key removes the selection.");
        actions = new Actions(getDriver());
        actions.keyUp(Keys.SHIFT)
                .sendKeys(Keys.ARROW_DOWN)
                .build()
                .perform();

        checker().withScreenshot()
                .verifyEquals("There should be no grid cells selected after releasing <shift> key and using an <arrow> key.",
                        0, editableGrid.getSelectedCells().size());

    }

    /**
     * <p>
     *     Test for <a href=https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=49953>Issue 49953: Shift up-arrow does not undo cell selection in editable grid.</a>
     * </p>
     * <p>
     *     Validates shift arrow selection in a row.
     * </p>
     * <p>
     *     <ul>
     *         <li>Click a cell then use shift left arrow.</li>
     *         <li>Use shift right arrow once.</li>
     *         <li>Use shift right arrow to go to the right of the starting cell.</li>
     *         <li>Release shift key and hit enter key and validate starter cell is 'activated'.</li>
     *         <li>Hit tab key navigates to new cell and removes selection.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testShiftArrowSelectHorizontal()
    {

        EditableGrid editableGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        editableGrid.addRows(10);

        checker().fatal()
                .verifyEquals("There should be no grid cells already selected. Fatal error.",
                        0, editableGrid.getSelectedCells().size());

        int startColumn = editableGrid.getColumnIndex(PASTE_3);

        int gridRow = 4;
        WebElement startCell = editableGrid.getCell(gridRow, PASTE_3);
        startCell.click();

        log("Select a few horizontal cells the the left in the grid.");
        Actions actions = new Actions(getDriver());
        actions.keyDown(Keys.SHIFT)
                .build()
                .perform();

        // Expected count is the number of left arrows plus the original cell selected.
        int expectedSelectedCount = 4;
        int endColumn = startColumn - 3;
        for(int count = 1; count <= 3; count++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys(Keys.ARROW_LEFT)
                    .build()
                    .perform();
        }

        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                endColumn,
                gridRow,
                startColumn,
                gridRow);

        checker().screenShotIfNewError("SHIFT_LEFT_ARROW_ERROR");

        log("Go to the right one column.");
        actions = new Actions(getDriver());
        actions.sendKeys(Keys.ARROW_RIGHT)
                .build()
                .perform();

        expectedSelectedCount = expectedSelectedCount - 1;
        endColumn = endColumn + 1;

        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                endColumn,
                gridRow,
                startColumn,
                gridRow);

        checker().screenShotIfNewError("SHIFT_RIGHT_ARROW_ERROR");

        log("Go to the right multiple times, past the original start column.");

        for(int count = 1; count <= 4; count++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys(Keys.ARROW_RIGHT)
                    .build()
                    .perform();
        }

        expectedSelectedCount = 3;
        endColumn = startColumn + 2;
        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                startColumn,
                gridRow,
                endColumn,
                gridRow);

        checker().screenShotIfNewError("SHIFT_RIGHT_ARROW_BEFORE_START_ERROR");

        log("Validate release <shift> key and hitting <enter> leaves the current selection but 'activates' the cell where the selection started.");
        actions = new Actions(getDriver());
        actions.keyUp(Keys.SHIFT)
                .sendKeys(Keys.ENTER)
                .build()
                .perform();

        // The cell with the textArea does not have the 'cell-selection' style so expectedSelectedCount is off by one.
        checker().verifyEquals("The selected cells should have remained after hitting <enter>.",
                expectedSelectedCount - 1, editableGrid.getSelectedCells().size());

        WebElement textArea = Locator.tag("textarea").findWhenNeeded(startCell);

        checker().verifyTrue(String.format("Cell on row %d column %s should be active.",
                        gridRow, PASTE_1),
                waitFor(textArea::isDisplayed, 1_000));

        checker().screenShotIfNewError("ENTER_WITH_SELECTION_ERROR");

        log("Finally validate that hitting <tab> removes selection and moves the active cell.");

        actions = new Actions(getDriver());
        actions.sendKeys(Keys.TAB)
                .build()
                .perform();

        checker().verifyEquals("Hitting <tab> should have removed the selection.",
                0, editableGrid.getSelectedCells().size());

        WebElement endCell = Locator.tag("div").findWhenNeeded(editableGrid.getCell(gridRow, PASTE_4));

        checker().verifyTrue(String.format("The expected cell on row %d and column %s is not selected after hitting <tab>.",
                        gridRow, PASTE_4),
                Objects.requireNonNullElse(endCell.getAttribute("class"), "").toLowerCase().contains("cell-selected"));

        checker().screenShotIfNewError("TAB_ERROR");
    }

    /**
     * <p>
     *     Test for <a href=https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=49953>Issue 49953: Shift up-arrow does not undo cell selection in editable grid.</a>
     * </p>
     * <p>
     *     Validates shift arrow selection in 2 dimensions.
     * </p>
     * <p>
     *     <ul>
     *         <li>Click a cell then use shift left and down arrow.</li>
     *         <li>Use shift up arrow to go above starting row.</li>
     *         <li>Use shift left arrow to go to the left of the starting column.</li>
     *         <li>Validate that clicking the starting cell leaves the selection in place.</li>
     *         <li>Validate clicking any other cell removes selection.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testShiftArrowSelect2D()
    {

        EditableGrid editableGrid = goToEditableGrid(PASTING_SAMPLE_TYPE);
        editableGrid.addRows(10);

        checker().fatal()
                .verifyEquals("There should be no grid cells already selected. Fatal error.",
                        0, editableGrid.getSelectedCells().size());

        List<String> columns = editableGrid.getColumnLabels();
        int startColumn = 4;

        int startRow = 5;
        WebElement startCell = editableGrid.getCell(startRow, columns.get(startColumn));

        startCell.click();

        log("Select a few cells in different rows and columns.");
        Actions actions = new Actions(getDriver());
        actions.keyDown(Keys.SHIFT)
                .build()
                .perform();

        int expectedSelectedCount = 9;
        int endColumn = startColumn + 2;
        int endRow = startRow + 2;
        for(int count = 1; count <= 2; count++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys(Keys.ARROW_RIGHT)
                    .sendKeys(Keys.ARROW_DOWN)
                    .build()
                    .perform();
        }

        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                startColumn,
                startRow,
                endColumn,
                endRow);

        checker().screenShotIfNewError("2D_SELECTION_ERROR");

        log("Gow up four rows (above original start column).");
        endRow = startRow - 2;
        for(int count = 1; count <= 4; count++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys(Keys.ARROW_UP)
                    .build()
                    .perform();
        }

        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                startColumn,
                startRow,
                endColumn,
                endRow);

        checker().screenShotIfNewError("2D_SELECTION_ABOVE_START_ERROR");

        log("Gow left four columns (to the left of the original start).");
        endColumn = startColumn - 2;
        for(int count = 1; count <= 4; count++)
        {
            actions = new Actions(getDriver());
            actions.sendKeys(Keys.ARROW_LEFT)
                    .build()
                    .perform();
        }

        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                startColumn,
                startRow,
                endColumn,
                endRow);

        checker().screenShotIfNewError("2D_SELECTION_BEFORE_START_ERROR");

        log("Validate that clicking the starting cell leaves the selection as is.");
        startCell.click();

        checkSelectedStyle(editableGrid,
                expectedSelectedCount,
                startColumn,
                startRow,
                endColumn,
                endRow);

        checker().screenShotIfNewError("2D_CLICK_START_CELL_ERROR");

        log("Validate that clicking any cell other than the origin cell removes the selection.");
        editableGrid.getCell(9, columns.get(startColumn)).click();

        checker().verifyEquals("There should be no grid cells selected after clicking some other cell.",
                        0, editableGrid.getSelectedCells().size());

    }

    private void checkSelectedStyle(EditableGrid editableGrid,
                                    int expectedSelectedCount,
                                    int startCol,
                                    int startRow,
                                    int endCol,
                                    int endRow)
    {

        int selectedSizeAfter = editableGrid.getSelectedCells().size();

        checker().fatal()
                .verifyTrue("No cells are selected. Fatal error.",
                        !editableGrid.getSelectedCells().isEmpty());

        checker().verifyEquals("Number of cells selected not as expected.",
                expectedSelectedCount, selectedSizeAfter);

        List<String> columnNames = editableGrid.getColumnLabels();

        for(int colIndex = startCol; colIndex <= endCol; colIndex++)
        {
            for(int rowIndex = startRow; rowIndex <= endRow; rowIndex++)
            {
                WebElement gridCell = Locator.tag("div").findElement(editableGrid.getCell(rowIndex, columnNames.get(colIndex)));
                checker().verifyTrue(String.format("Cell (%s, %d) is not selected.", columnNames.get(colIndex), rowIndex),
                        Objects.requireNonNullElse(gridCell.getAttribute("class"), "").toLowerCase().contains("cell-selection"));
            }
        }

    }

    private String getActualPaste(EditableGrid testGrid)
    {
        List<List<String>> rows = testGrid.getGridData(PASTE_1, PASTE_2, PASTE_3, PASTE_4, PASTE_5);
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

    private static List<WebElement> setCellValues(EditableGrid testGrid, CharSequence columnIdentifier, Object... values)
    {
        List<WebElement> cells = new ArrayList<>();
        List.of(values).forEach(value -> cells.add(testGrid.setCellValue(cells.size(), columnIdentifier, value)));
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

        log("Verify no cell errors when page first load");
        checker().verifyEquals("Cell error should be absent when a row is added on page load", 0, testGrid.getCellErrors().size());

        log("Input empty string for required field should trigger cell warning.");
        testGrid.setCellValue(1, REQ_STR_FIELD, " ");
        checker().verifyEquals("Cell warning status not as expected at row " + 1 + " for col " + REQ_STR_FIELD.getLabel(), true, testGrid.hasCellError(1, REQ_STR_FIELD));
        checker().verifyEquals("Cell warning msg not as expected at row " + 1 + " for col " + REQ_STR_FIELD.getLabel(), REQ_STR_FIELD.getUiLabel() + " is required.", testGrid.getCellPopoverText(1, REQ_STR_FIELD));
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(1, REQ_INT_FIELD, " ");
        checker().verifyEquals("Cell warning status not as expected at row " + 1 + " for col " + REQ_INT_FIELD.getLabel(), true, testGrid.hasCellError(1, REQ_INT_FIELD));
        checker().verifyEquals("Cell warning msg not as expected at row " + 1 + " for col " + REQ_INT_FIELD.getLabel(), REQ_INT_FIELD.getUiLabel() + " is required.", testGrid.getCellPopoverText(1, REQ_INT_FIELD));

        log("Correct values should remove cell warning, keep entering wrong values should update warning");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, STR_FIELD, "");
        mouseOver(testGrid.getCell(0, "Row"));
        testGrid.setCellValue(1, STR_FIELD, "This value is too long");
        mouseOver(testGrid.getCell(0, "Row"));
        testGrid.setCellValue(0, REQ_STR_FIELD, "good");
        mouseOver(testGrid.getCell(0, "Row"));
        testGrid.setCellValue(1, REQ_STR_FIELD, "This value is too long");
        checker().verifyEquals("Cell warning status not as expected at row " + 0 + " for col " + STR_FIELD.getLabel(), false, testGrid.hasCellError(0, STR_FIELD));
        checker().verifyEquals("Cell warning msg not as expected at row " + 1 + " for col " + STR_FIELD.getLabel(), "22/10 characters", testGrid.getCellPopoverText(1, STR_FIELD));
        checker().verifyEquals("Cell warning status not as expected at row " + 0 + " for col " + REQ_STR_FIELD.getLabel(), false, testGrid.hasCellError(0, REQ_STR_FIELD));
        checker().verifyEquals("Cell warning msg not as expected at row " + 1 + " for col " + REQ_STR_FIELD.getLabel(), "22/10 characters", testGrid.getCellPopoverText(1, REQ_STR_FIELD));

        log("Input invalid data type value should trigger cell warnings.");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, INT_FIELD, "1.23");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, FLOAT_FIELD, "not float");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, BOOL_FIELD, "not bool");
        checker().verifyEquals("Cell warning msg not as expected at row " + 0 + " for col " + INT_FIELD.getLabel(), "Invalid integer", testGrid.getCellPopoverText(0, INT_FIELD));
        checker().verifyEquals("Cell warning msg not as expected at row " + 0 + " for col " + FLOAT_FIELD.getLabel(), "Invalid decimal", testGrid.getCellPopoverText(0, FLOAT_FIELD));
        checker().verifyEquals("Cell warning msg not as expected at row " + 0 + " for col " + BOOL_FIELD.getLabel(), "Invalid boolean", testGrid.getCellPopoverText(0, BOOL_FIELD));

        log("Correct values should remove data type warning.");
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.setCellValue(0, INT_FIELD, "123");
        checker().verifyFalse("Cell warning should disappear after correcting value", testGrid.hasCellError(0, INT_FIELD));

        log("Required value warning should be absent before the cell is acted on");
        checker().verifyFalse("Required value warning should not be present on page init", testGrid.hasCellError(0, REQ_STR_FIELD));
        mouseOver(testGrid.getCell(0, "Row")); // dismiss warning popup
        testGrid.clearCellValue(1, REQ_STR_FIELD);
        checker().verifyTrue("Required value warning should show up after removing a value from cell", testGrid.hasCellError(1, REQ_STR_FIELD));
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
        testGrid.pasteFromCell(0, STR_FIELD, rowsToString(clipRows), false);

        List<List<String>> expectedCellWarnings = List.of(
                Arrays.asList(null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                Arrays.asList(null, REQ_STR_FIELD.getLabel() + " is required.", null, REQ_INT_FIELD.getLabel() + " is required.", null, REQ_DATETIME_FIELD.getLabel() + " is required.", null, REQ_TIME_FIELD.getLabel() + " is required.", null, null, null, REQ_TEXTCHOICE_FIELD.getLabel() + " is required.", null, REQ_LOOKUP_FIELD.getLabel() + " is required."),
                Arrays.asList("22/10 characters", "22/10 characters", "Invalid integer", "Invalid integer", "Invalid date, use format yyyy-MM-dd", "Invalid date time, use format yyyy-MM-dd HH:mm", "Invalid time", "Invalid time", "Invalid boolean", "Invalid decimal", "'wrong text choice' is not a valid choice", "'bad choice' is not a valid choice", "Could not find \"bad lookup\"", "Could not find \"bad lookup\"")
        );

        log("Verify pasted values triggers cell warnings");
        for (int row = 0; row < expectedCellWarnings.size(); row++)
        {
            List<String> expectedWarnings = expectedCellWarnings.get(row);
            verifyCellWarning(testGrid, expectedWarnings, row);
        }

        // Dismiss warning popup by moving off the grid.
        mouseOver(Locator.tagWithClass("a", "brand-logo").findWhenNeeded(getDriver()));

        WebDriverWrapper.waitFor(()->!Locator.tagWithClass("div", "popover").findWhenNeeded(getDriver()).isDisplayed(),
                "Bad value popup did not go away.", 500);

        log("Correct missing required fields should remove corresponding cell warnings");
        testGrid.setCellValue(1, REQ_STR_FIELD, " ");
        checker().verifyTrue("Cell warning should be present after setting another invalid value", testGrid.hasCellError(1, REQ_STR_FIELD));
        mouseOver(testGrid.getCell(0, STR_FIELD)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_INT_FIELD, "2");
        mouseOver(testGrid.getCell(0, STR_FIELD)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_TEXTCHOICE_FIELD, List.of("Orange"));
        mouseOver(testGrid.getCell(0, STR_FIELD)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_LOOKUP_FIELD, List.of("Orange"));
        mouseOver(testGrid.getCell(0, STR_FIELD)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_STR_FIELD, "not empty");
        mouseOver(testGrid.getCell(0, STR_FIELD)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_DATETIME_FIELD, LocalDateTime.of(2024, 7, 7, 10, 30));
        mouseOver(testGrid.getCell(0, STR_FIELD)); // dismiss warning popup
        testGrid.setCellValue(1, REQ_TIME_FIELD, LocalTime.of(2, 30));

        for (FieldInfo field : ALL_FIELDS)
        {
            checker().verifyFalse("Cell warning be absent after required values are provided: " + field.getLabel(), testGrid.hasCellError(1, field));
        }

        log("Enter another bad value should retain cell warning");
        testGrid.setCellValue(2, INT_FIELD, "bad");
        checker().verifyTrue("Cell warning should be present after setting another invalid value", testGrid.hasCellError(2, INT_FIELD));
        checker().screenShotIfNewError("after required value correction error");

        log("Correct bad data type values should remove paste data warnings");
        testGrid.setCellValue(2, STR_FIELD, "good");
        testGrid.setCellValue(2, REQ_STR_FIELD, "good");
        testGrid.setCellValue(2, INT_FIELD, "1");
        testGrid.setCellValue(2, REQ_INT_FIELD, "134");
        testGrid.setCellValue(2, BOOL_FIELD, "on");
        testGrid.setCellValue(2, FLOAT_FIELD, "1.23");
        testGrid.setCellValue(2, TEXTCHOICE_FIELD, List.of("red"));
        testGrid.setCellValue(2, REQ_TEXTCHOICE_FIELD, List.of("red"));
        testGrid.setCellValue(2, LOOKUP_FIELD, List.of("kiwi"));
        testGrid.setCellValue(2, REQ_LOOKUP_FIELD, List.of("kiwi"));
        testGrid.setCellValue(2, DATE_FIELD, LocalDate.of(2024, 7, 7));
        testGrid.setCellValue(2, TIME_FIELD, LocalTime.of(2, 30));
        testGrid.setCellValue(2, REQ_DATETIME_FIELD, LocalDateTime.of(2024, 7, 7, 10, 30));
        testGrid.setCellValue(2, REQ_TIME_FIELD, LocalTime.of(2, 30));

        for (FieldInfo field : ALL_FIELDS)
        {
            checker().verifyFalse("Cell warning should be absent after correct values are provided: " + field.getLabel(), testGrid.hasCellError(2, field));
        }
        checker().screenShotIfNewError("after data correction error");

        log("Issue 46767: start date before 1000-01-01");
        for (FieldInfo field : ALL_FIELDS)
        {
            // start date before year 1000 shouldn't trigger warning
            checker().verifyFalse("Cell warning should not be present for: " + field.getLabel(), testGrid.hasCellError(0, field));
        }

        log("Verify UI is interactable with values before 1000-01-01");
        testGrid.setCellValue(3, DATE_FIELD, LocalDate.of(2024, 7, 7));
        testGrid.setCellValue(3, TIME_FIELD, LocalTime.of(2, 30));
        testGrid.setCellValue(3, REQ_DATETIME_FIELD, LocalDate.of(2024, 7, 7));
        testGrid.setCellValue(3, REQ_TIME_FIELD, LocalTime.of(2, 30));

        checker().screenShotIfNewError("Issue 46767");

        testGrid.clearAllCells();

    }

    private void verifyCellWarning(EditableGrid testGrid, List<String> expectedWarnings, int rowId)
    {
        for (int col = 0; col < ALL_FIELDS.size(); col++)
        {
            String expectedWarning = expectedWarnings.get(col);
            FieldInfo field = ALL_FIELDS.get(col);

            checker().verifyEquals("Cell warning status not as expected at row " + rowId + " for col " + field.getLabel(), !StringUtils.isEmpty(expectedWarning), testGrid.hasCellError(rowId, field));
            if (!StringUtils.isEmpty(expectedWarning))
                checker().verifyEqualsIgnoreWhiteSpaces("Cell warning msg not as expected at row " + rowId + " for col " + field.getLabel(), expectedWarning, testGrid.getCellPopoverText(rowId, field));
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
        testGrid.pasteFromCell(0, STR_FIELD, rowsToString(clipRows), false);

        // Scroll one column to the right into view, this will help ensure the REQ_LOOKUP_FIELD is within the viewport.
        var index = testGrid.getColumnLabels().indexOf(REQ_LOOKUP_FIELD.getLabel() + " *") + 1;
        scrollIntoView(testGrid.getCell(0, testGrid.getColumnLabels().get(index)));

        WebElement fillFrom = testGrid.getCell(0, REQ_LOOKUP_FIELD);
        WebElement fillTo = testGrid.getCell(2, REQ_LOOKUP_FIELD);
        testGrid.dragFill(fillFrom, fillTo);

        List<String> expectedWarnings = Arrays.asList("22/10 characters", REQ_STR_FIELD.getLabel() + " is required.", "Invalid integer", "Invalid integer",
                "Invalid date, use format yyyy-MM-dd", REQ_DATETIME_FIELD.getLabel() + " is required.", "Invalid time", REQ_TIME_FIELD.getLabel() + " is required.",
                "Invalid boolean", "Invalid decimal", "'wrong text choice' is not a valid choice", "'bad choice' is not a valid choice", "Could not find \"bad lookup\"", REQ_LOOKUP_FIELD.getLabel() + " is required.");

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
