package org.labkey.test.tests.list;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.core.admin.BaseSettingsPage.DATE_FORMAT;
import org.labkey.test.pages.core.admin.BaseSettingsPage.TIME_FORMAT;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Category({Daily.class, Data.class, Hosting.class})
public class ListDateAndTimeTest extends BaseWebDriverTest
{

    private static final String PROJECT_NAME = "List Date And Time Test";
    private static SimpleDateFormat _defaultDateFormat = null;
    private static SimpleDateFormat _defaultTimeFormat = null;
    private static SimpleDateFormat _defaultDateTimeFormat = null;

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        ListDateAndTimeTest init = (ListDateAndTimeTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        resetSiteSettings();
        _containerHelper.createProject(PROJECT_NAME, null);
        goToProjectHome();
        LookAndFeelSettingsPage settingsPage = LookAndFeelSettingsPage.beginAt(this);

        // Use java's Date object and SimpleDateFormatter (not strings) to enter the data.
        _defaultDateFormat = new SimpleDateFormat(settingsPage.getDefaultDateDisplay());
        _defaultTimeFormat = new SimpleDateFormat(settingsPage.getDefaultTimeDisplay());
        _defaultDateTimeFormat = new SimpleDateFormat(String.format("%s %s",
                settingsPage.getDefaultDateTimeDateDisplay(), settingsPage.getDefaultDateTimeTimeDisplay()));
    }

    @AfterClass
    public static void afterClass() throws IOException, CommandException
    {
        ((ListDateAndTimeTest) getCurrentTest()).resetSiteSettings();
    }

    private void resetSiteSettings() throws IOException, CommandException
    {
        log("Reset site settings.");
        BaseSettingsPage.resetSettings(createDefaultConnection(), "/");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    private void validateListDataInUI(DataRegionTable table, List<Map<String, String>> expectedData)
    {
        checker().verifyEquals("Number of rows in the UI list not equal to number of expected rows.",
                expectedData.size(), table.getDataRowCount());

        int rowIndex = 0;
        for(Map<String, String> expectedRowMap : expectedData)
        {
            // Protect against the table having fewer rows than expected (index out of bounds error).
            if(rowIndex < table.getDataRowCount())
            {
                Map<String, String> tableRowMap = table.getRowDataAsMap(rowIndex);

                for(Map.Entry<String, String> expectedEntry : expectedRowMap.entrySet())
                {
                    if(checker().verifyTrue(String.format("Could not find column '%s' in rowmap for row %d", expectedEntry.getKey(), rowIndex),
                            tableRowMap.containsKey(expectedEntry.getKey())))
                    {
                        checker().verifyEquals(String.format("For row %d column %s not as expected.", rowIndex, expectedEntry.getKey()),
                                expectedEntry.getValue(), tableRowMap.get(expectedEntry.getKey()));
                    }
                }

            }
            rowIndex++;
        }
    }

    /**
     * <p>
     *     Primarily testing the import of values into a date-only and time-only field in a list.
     * </p>
     * <p>
     *     Test covers:
     *     <ul>
     *         <li>Import date-only, time-only and dateTime values by bulk.</li>
     *         <li>Import date-only, time-only and dateTime values by xlsx. The xlsx file has columns formatted for date and time.</li>
     *         <li>Add a row through the UI.</li>
     *         <li>Import a time value into a date-only field and a date value into a time-only field.</li>
     *         <li>The test also uses different formats and validates the list displays in default site formats.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testDateAndTimeColumnsInsert() throws IOException, CommandException
    {

        SimpleDateFormat variantDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        SimpleDateFormat variantTimeFormat = new SimpleDateFormat("hh:mm:ss aa");
        SimpleDateFormat variantDateTimeFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm aa");

        String listName = "Date and Time Insert List";
        String dateCol = "Date";
        String timeCol = "Time";
        String dateTimeCol = "DateTime";

        log(String.format("Create a list named '%s' with date-only, time-only and dateTime fields.", listName));

        _listHelper.createList(PROJECT_NAME, listName, "key",
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime).setLabel(dateTimeCol)
        );

        log("Validate adding entries in bulk. Use a different format for the date and time values in the second row.");

        List<Map<String, String>> expectedData = new ArrayList<>();

        Date testDate01 = new Calendar.Builder()
                .setDate(2023, 1, 17)
                .setTimeOfDay(11, 12, 03)
                .build().getTime();

        expectedData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        // Use a separate variable for this date. It will be formatted differently from the default when entered.
        Date testDate02 = new Calendar.Builder()
                .setDate(2022, 6, 10)
                .setTimeOfDay(20, 45, 15)
                .build().getTime();

        expectedData.add(Map.of(dateCol, _defaultDateFormat.format(testDate02),
                timeCol, _defaultTimeFormat.format(testDate02),
                dateTimeCol, _defaultDateTimeFormat.format(testDate02)));

        testDate01 = new Calendar.Builder()
                .setDate(1994, 11, 21)
                .setTimeOfDay(3, 23, 0)
                .build().getTime();

        expectedData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        String bulkImportText = String.format("%s\t%s\t%s\n", dateCol, timeCol, dateTimeCol) +
                String.format("%s\t%s\t%s\n",
                        expectedData.get(0).get(dateCol), expectedData.get(0).get(timeCol), expectedData.get(0).get(dateTimeCol)) +
                String.format("%s\t%s\t%s\n",
                        variantDateFormat.format(testDate02), variantTimeFormat.format(testDate02), _defaultDateTimeFormat.format(testDate02)) +
                String.format("%s\t%s\t%s\n",
                        expectedData.get(2).get(dateCol), expectedData.get(2).get(timeCol), expectedData.get(2).get(dateTimeCol));

        _listHelper.bulkImportData(bulkImportText);

        log("Validate adding entries by xlsx import. Note the xlsx file has columns formatted as time and date specific.");
        File excelDateTimeFile = TestFileUtils.getSampleData("lists/Date_And_Time_Format.xlsx");

        _listHelper.importDataFromFile(excelDateTimeFile);

        // Note, the month is zero based. The value of the month here is one less than what is seen in the file.
        // Also of note the xlsx used has a column formatted as time and another as date. Excel does not have a DateTime specific format.
        testDate01 = new Calendar.Builder()
                .setDate(2024, 1, 29)
                .setTimeOfDay(11, 28, 54)
                .build().getTime();

        expectedData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        testDate01 = new Calendar.Builder()
                .setDate(1998, 2, 10)
                .setTimeOfDay(18, 12, 0)
                .build().getTime();

        expectedData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        testDate01 = new Calendar.Builder()
                .setDate(2002, 9, 31)
                .setTimeOfDay(22, 20, 0)
                .build().getTime();

        expectedData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        log("Validate adding an entry through the UI. Use a different format for the date and dateTime field.");

        testDate01 = new Calendar.Builder()
                .setDate(2024, 2, 14)
                .setTimeOfDay(12, 0, 45)
                .build().getTime();

        expectedData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        Map<String, String> uiDateFormat = Map.of(
                dateCol, variantDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, variantDateTimeFormat.format(testDate01));

        _listHelper.insertNewRow(uiDateFormat, false);

        log("Bulk import a time for the date field and a date for the time field.");

        testDate01 = new Calendar.Builder()
                .setDate(1992, 8, 4)
                .setTimeOfDay(19, 30, 32)
                .build().getTime();

        // If a time is given for a date-only field the date will default to 1-1-1970
        // If a date is given for a time-only field the time will default to 0:00:00
        Date dateMissing = new Calendar.Builder()
                .setDate(1970, 0, 1)
                .setTimeOfDay(0, 0, 0)
                .build().getTime();

        expectedData.add(Map.of(dateCol, _defaultDateFormat.format(dateMissing),
                timeCol, _defaultTimeFormat.format(dateMissing),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        // Making sure the time format and date formats are in the wrong column.
        bulkImportText = String.format("%s\t%s\t%s\n", dateCol, timeCol, dateTimeCol) +
                String.format("%s\t%s\t%s\n",
                        _defaultTimeFormat.format(testDate01), _defaultDateFormat.format(testDate01), _defaultDateTimeFormat.format(testDate01));

        _listHelper.bulkImportData(bulkImportText);

        DataRegionTable table = new DataRegionTable("query", getDriver());

        validateListDataInUI(table, expectedData);

    }

    @Test
    public void testExcelImportExport() throws IOException, CommandException
    {

        String listName = "Date and Time Export List";
        String dateCol = "Date";
        String timeCol = "Time";
        String dateTimeCol = "DateTime";

        log(String.format("Create a list named '%s' with date-only, time-only and dateTime fields.", listName));

        _listHelper.createList(PROJECT_NAME, listName, "key",
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime).setLabel(dateTimeCol)
        );

        File excelDateTimeFile = TestFileUtils.getSampleData("lists/Date_And_Time_Format.xlsx");

        _listHelper.importDataFromFile(excelDateTimeFile);

        DataRegionTable table = new DataRegionTable("query", getDriver());

        List<Map<String, String>> expectedUIData = new ArrayList<>();

        // Note, the month is zero based. The value of the month here is one less than what is seen in the file.
        // Also of note the xlsx used has a column formatted as time and another as date. Excel does not have a DateTime specific format.
        Date testDate01 = new Calendar.Builder()
                .setDate(2024, 1, 29)
                .setTimeOfDay(11, 28, 54)
                .build().getTime();

        expectedUIData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        // The expected data in the exported Excel sheet.
        List<List<Date>> expectedExportedData = new ArrayList<>();

        // In Excel:
        // The date-only column will have a time of 0:00
        // The time-only column will have a date of 1/1/70
        // The date-time column should be the same as the date object.
        expectedExportedData.add(List.of(
                new Calendar.Builder()
                        .setDate(2024, 1, 29)
                        .setTimeOfDay(0, 0, 0)
                        .build().getTime(),
                new Calendar.Builder()
                        .setDate(1970, 0, 1)
                        .setTimeOfDay(11, 28, 54)
                        .build().getTime(),
                testDate01
        ));

        testDate01 = new Calendar.Builder()
                .setDate(1998, 2, 10)
                .setTimeOfDay(18, 12, 0)
                .build().getTime();

        expectedUIData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        expectedExportedData.add(List.of(
                new Calendar.Builder()
                        .setDate(1998, 2, 10)
                        .setTimeOfDay(0, 0, 0)
                        .build().getTime(),
                new Calendar.Builder()
                        .setDate(1970, 0, 1)
                        .setTimeOfDay(18, 12, 0)
                        .build().getTime(),
                testDate01
        ));

        testDate01 = new Calendar.Builder()
                .setDate(2002, 9, 31)
                .setTimeOfDay(22, 20, 0)
                .build().getTime();

        expectedUIData.add(Map.of(dateCol, _defaultDateFormat.format(testDate01),
                timeCol, _defaultTimeFormat.format(testDate01),
                dateTimeCol, _defaultDateTimeFormat.format(testDate01)));

        expectedExportedData.add(List.of(
                new Calendar.Builder()
                        .setDate(2002, 9, 31)
                        .setTimeOfDay(0, 0, 0)
                        .build().getTime(),
                new Calendar.Builder()
                        .setDate(1970, 0, 1)
                        .setTimeOfDay(22, 20, 0)
                        .build().getTime(),
                testDate01
        ));

        validateListDataInUI(table, expectedUIData);

        log("Now export the list to excel and validate.");
        DataRegionExportHelper exportHelper = new DataRegionExportHelper(table);

        table.checkAllOnPage();

        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX);

        try(Workbook workbook = ExcelHelper.create(exportedFile))
        {
            Sheet sheet = workbook.getSheetAt(workbook.getActiveSheetIndex());

            int row = 1;
            for(List<Date> expectedDate : expectedExportedData)
            {

                for (int column = 0; column < 3; column++)
                {
                    checker().verifyEquals(String.format("For row %d column %d the exported value is not as expected.", row, column),
                            expectedDate.get(column), sheet.getRow(row).getCell(column).getDateCellValue());
                }

                row++;
            }
        }

    }

    // The order of the list returned is important. The data will be added to the list in the same order as the list.
    // If changes are made to this data corresponding changes will need to be made to the sorting and filtering tests.
    private List<Date> createDateAndTimeTestData()
    {
        List<Date> dates = new ArrayList<>();

        Date date = new Calendar.Builder()
                .setDate(1950, 9, 12)
                .setTimeOfDay(8, 0, 1)
                .build().getTime();

        dates.add(date);

        // Add a date in the future.
        Calendar calToday = Calendar.getInstance();
        calToday.set(Calendar.HOUR_OF_DAY, 14);
        calToday.set(Calendar.MINUTE, 23);
        calToday.set(Calendar.SECOND, 54);
        calToday.add(Calendar.MONTH, 2);
        date = calToday.getTime();
        dates.add(date);

        date = new Calendar.Builder()
                .setDate(2024, 0, 1)
                .setTimeOfDay(0, 0, 0)
                .build().getTime();

        dates.add(date);

        date = new Calendar.Builder()
                .setDate(1992, 2, 3)
                .setTimeOfDay(10, 10, 10)
                .build().getTime();

        dates.add(date);

        // Have two entries with the same values.
        dates.add(date);

        date = new Calendar.Builder()
                .setDate(1992, 2, 3)
                .setTimeOfDay(10, 11, 34)
                .build().getTime();

        dates.add(date);

        date = new Calendar.Builder()
                .setDate(1995, 2, 3)
                .setTimeOfDay(9, 10, 10)
                .build().getTime();

        dates.add(date);

        // Add leap day to the mix
        date = new Calendar.Builder()
                .setDate(2024, 1, 29)
                .setTimeOfDay(18, 32, 0)
                .build().getTime();

        dates.add(date);

        date = new Calendar.Builder()
                .setDate(2002, 8, 15)
                .setTimeOfDay(17, 45, 20)
                .build().getTime();

        dates.add(date);

        // Add date only
        date = new Calendar.Builder()
                .setDate(1989, 7, 12)
                .setTimeOfDay(0, 0, 0)
                .build().getTime();

        dates.add(date);

        // Add time only
        date = new Calendar.Builder()
                .setDate(1970, 0, 1)
                .setTimeOfDay(14, 59, 25)
                .build().getTime();

        dates.add(date);

        return dates;
    }

    /**
     * <p>
     *     Test sorting date-only and time-only fields.
     * </p>
     * <p>
     *     Test will sort different values, that include duplicates, blanks and dates in the future.
     * </p>
     * @throws IOException Can be thrown by helper that checks if the list already exists.
     * @throws CommandException Can be thrown by helper that checks if the list already exists.
     */
    @Test
    public void testDateAndTimeColumnSorting() throws IOException, CommandException
    {

        String listName = "Date and Time Sort List";
        String dateCol = "Date";
        String timeCol = "Time";
        String keyCol = "Key";

        log(String.format("Create a list named '%s' with date-only and time-only fields.", listName));

        _listHelper.createList(PROJECT_NAME, listName, keyCol,
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time)
        );

        List<Date> dates = createDateAndTimeTestData();

        // The method createDateAndTimeTestData should create a list of Date object in the order below.
        // They will be inserted into the list in this order, the Key column will be used to validate the sort order.
        // Key 1: 1950-10-12 08:00:01
        // Key 2: (some future date) 14:23:54
        // Key 3:  2024-01-01 00:00:00
        // Key 4: 1992-03-03 10:10:10
        // Key 5: 1992-03-03 10:10:10
        // Key 6: 1992-03-03 10:11:34
        // Key 7: 1995-03-03 09:10:10
        // Key 8: 2024-02-29 18:32:00
        // Key 9: 2002-09-15 17:45:20
        // Key 10: 1989-08-12 (empty)
        // Key 11: (empty) 14:59:25

        // These two are special. One will only supply a date value the other will only supply a time value.
        Date dateUseDateOnly = dates.get(9);
        Date dateUseTimeOnly = dates.get(10);

        StringBuilder bulkInsertText = new StringBuilder();
        bulkInsertText.append(String.format("%s\t%s\n", dateCol, timeCol));

        for(Date date : dates)
        {

            if(date.equals(dateUseDateOnly))
            {
                // Add a line with only a date.
                bulkInsertText.append(String.format("%s\t\n", _defaultDateFormat.format(date)));
            }
            else if(date.equals(dateUseTimeOnly))
            {
                // Add a line with only a time.
                bulkInsertText.append(String.format("\t%s\n", _defaultTimeFormat.format(date)));
            }
            else
            {
                bulkInsertText.append(String.format("%s\t%s\n",
                        _defaultDateFormat.format(date), _defaultTimeFormat.format(date)));
            }

        }

        log("Use bulk import to populate the list with values to sort.");
        _listHelper.bulkImportData(bulkInsertText.toString());

        DataRegionTable table = new DataRegionTable("query", getDriver());

        // Show the 'Key' column in the table. It will be used to validate sort order.
        CustomizeView customizeView = table.openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn(keyCol);
        customizeView.saveDefaultView(true);

        log("Sort the date-only field in ascending order.");
        List<String> expectedKeyColOrder = new ArrayList<>();

        // In MSSQL the "empty" value is at the top.
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
        {
            expectedKeyColOrder.add("11"); // (empty) 14:59:25
        }

        expectedKeyColOrder.add("1"); // 1950-10-12 08:00:01
        expectedKeyColOrder.add("10"); // 1989-08-12 (empty)
        expectedKeyColOrder.add("4"); // 1992-03-03 10:10:10
        expectedKeyColOrder.add("5"); // 1992-03-03 10:10:10
        expectedKeyColOrder.add("6"); // 1992-03-03 10:11:34
        expectedKeyColOrder.add("7"); // 1995-03-03 09:10:10
        expectedKeyColOrder.add("9"); // 2002-09-15 17:45:20
        expectedKeyColOrder.add("3"); // 2024-01-01 00:00:00
        expectedKeyColOrder.add("8"); // 2024-02-29 18:32:00
        expectedKeyColOrder.add("2"); // (some future date) 14:23:54

        // In postgres the "empty" value is at the bottom.
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL)
        {
            expectedKeyColOrder.add("11"); // (empty) 14:59:25
        }

        table.setSort(dateCol, SortDirection.ASC);
        List<String> actualKeyColOrder = table.getColumnDataAsText(keyCol);

        checker().withScreenshot("Error_Sort_Date_ASC")
                .verifyEquals("Sort date-only column in ascending order not as expected.",
                        expectedKeyColOrder, actualKeyColOrder);

        log("Sort the date-only field in descending order.");
        expectedKeyColOrder = new ArrayList<>();

        // Empty is sorted differently between postgres and MSSQL.
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL)
        {
            expectedKeyColOrder.add("11"); // (empty) 14:59:25
        }

        expectedKeyColOrder.add("2");
        expectedKeyColOrder.add("8");
        expectedKeyColOrder.add("3");
        expectedKeyColOrder.add("9");
        expectedKeyColOrder.add("7");
        expectedKeyColOrder.add("4");
        expectedKeyColOrder.add("5");
        expectedKeyColOrder.add("6");
        expectedKeyColOrder.add("10");
        expectedKeyColOrder.add("1");

        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
        {
            expectedKeyColOrder.add("11"); // (empty) 14:59:25
        }

        table.setSort(dateCol, SortDirection.DESC);
        actualKeyColOrder = table.getColumnDataAsText(keyCol);

        checker().withScreenshot("Error_Sort_Date_DESC")
                .verifyEquals("Sort date-only column in descending order not as expected.",
                        expectedKeyColOrder, actualKeyColOrder);

        log("Clear the sort from the date-only column.");
        table.clearSort(dateCol);

        log("Sort the time-only field in ascending order.");
        expectedKeyColOrder = new ArrayList<>();

        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
        {
            expectedKeyColOrder.add("10"); // 1989-08-12 (empty)
        }

        expectedKeyColOrder.add("3"); // 2024-01-01 00:00:00
        expectedKeyColOrder.add("1"); // 1950-10-12 08:00:01
        expectedKeyColOrder.add("7"); // 1995-03-03 09:10:10
        expectedKeyColOrder.add("4"); // 1992-03-03 10:10:10
        expectedKeyColOrder.add("5"); // 1992-03-03 10:10:10
        expectedKeyColOrder.add("6"); // 1992-03-03 10:11:34
        expectedKeyColOrder.add("2"); // (some future date) 14:23:54
        expectedKeyColOrder.add("11"); // (empty) 14:59:25
        expectedKeyColOrder.add("9"); // 2002-09-15 17:45:20
        expectedKeyColOrder.add("8"); // 2024-02-29 18:32:00

        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL)
        {
            expectedKeyColOrder.add("10"); // 1989-08-12 (empty)
        }

        table.setSort(timeCol, SortDirection.ASC);
        actualKeyColOrder = table.getColumnDataAsText(keyCol);

        checker().withScreenshot("Error_Sort_Time_ASC")
                .verifyEquals("Sort time-only column in ascending order not as expected.",
                        expectedKeyColOrder, actualKeyColOrder);

        log("Sort the time-only field in descending order.");
        expectedKeyColOrder = new ArrayList<>();

        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL)
        {
            expectedKeyColOrder.add("10"); // 1989-08-12 (empty)
        }

        expectedKeyColOrder.add("8");
        expectedKeyColOrder.add("9");
        expectedKeyColOrder.add("11");
        expectedKeyColOrder.add("2");
        expectedKeyColOrder.add("6");
        expectedKeyColOrder.add("4");
        expectedKeyColOrder.add("5");
        expectedKeyColOrder.add("7");
        expectedKeyColOrder.add("1");
        expectedKeyColOrder.add("3");

        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
        {
            expectedKeyColOrder.add("10"); // 1989-08-12 (empty)
        }

        table.setSort(timeCol, SortDirection.DESC);
        actualKeyColOrder = table.getColumnDataAsText(keyCol);

        checker().withScreenshot("Error_Sort_Time_DESC")
                .verifyEquals("Sort time-only column in descending order not as expected.",
                        expectedKeyColOrder, actualKeyColOrder);

        log("Clear the sort from the time-only column.");
        table.clearSort(timeCol);

    }

    private void testFiltering(String column, String keyCol, List<String> expectedKeyCol,
                               String filterType01, @Nullable String filter01,
                               @Nullable String filterType02, @Nullable String filter02,
                               String errorMsg, String screenshotName)
    {
        DataRegionTable table = new DataRegionTable("query", getDriver());

        if(null == filter01)
        {
            table.setFilter(column, filterType01);
        }
        else
        {
            table.setFilter(column, filterType01, filter01, filterType02, filter02);
        }


        List<String> actualKeyCol = table.getColumnDataAsText(keyCol);

        checker().withScreenshot(screenshotName)
                .verifyEquals(errorMsg,
                        expectedKeyCol, actualKeyCol);

        table.clearFilter(column);

    }

    /**
     * <p>
     *     Test for filtering date-only and time-only column.
     * </p>
     * <p>
     *     Test will filter for:
     *     <ul>
     *         <li>Equals</li>
     *         <li>Is Greater Than or Equal To</li>
     *         <li>Is Greater Than or Equal To and Is Less Than</li>
     *         <li>Is blank</li>
     *         <li>Filtering time-only column also validates when the value given as the filter has only hh:mm (no seconds)</li>
     *     </ul>
     * </p>
     * @throws IOException Can be thrown by helper that checks if the list already exists.
     * @throws CommandException Can be thrown by helper that checks if the list already exists.
     */
    @Test
    public void testDateAndTimeColumnFiltering() throws IOException, CommandException
    {

        String listName = "Date and Time Filter List";
        String dateCol = "Date";
        String timeCol = "Time";
        String keyCol = "Key";

        log(String.format("Create a list named '%s' with date-only and time-only fields.", listName));

        _listHelper.createList(PROJECT_NAME, listName, keyCol,
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time)
        );

        List<Date> dates = createDateAndTimeTestData();

        // These times will be used in filters.
        Date dateKey01 = dates.get(0); // 1950-10-12 08:00:01 Key: 1
        Date dateDuplicate = dates.get(3); // 1992-03-03 10:10:10 Keys: 4 & 5
        Date dateKey07 = dates.get(6); // 1995-03-03 09:10:10 Key: 7
        Date dateKey08 = dates.get(7); // 2024-02-29 18:32:00 Key: 8
        Date dateUseDateOnlyKey10 = dates.get(9); // 1989-08-12 (empty) Key: 10
        Date dateUseTimeOnlyKey11 = dates.get(10); // (empty) 14:59:25 Key: 11

        StringBuilder bulkInsertText = new StringBuilder();
        bulkInsertText.append(String.format("%s\t%s\n", dateCol, timeCol));

        for(Date date : dates)
        {

            if(date.equals(dateUseDateOnlyKey10))
            {
                // Add a line with only a date.
                bulkInsertText.append(String.format("%s\t\n", _defaultDateFormat.format(date)));
            }
            else if(date.equals(dateUseTimeOnlyKey11))
            {
                // Add a line with only a time.
                bulkInsertText.append(String.format("\t%s\n", _defaultTimeFormat.format(date)));
            }
            else
            {
                bulkInsertText.append(String.format("%s\t%s\n",
                        _defaultDateFormat.format(date), _defaultTimeFormat.format(date)));
            }

        }

        log("Use bulk insert to populate the list.");
        _listHelper.bulkImportData(bulkInsertText.toString());

        DataRegionTable table = new DataRegionTable("query", getDriver());

        // Show the 'Key' column in the table. It will be used to validate the filter.
        CustomizeView customizeView = table.openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn(keyCol);
        customizeView.saveDefaultView(true);

        log("Start by filtering the date-only column.");

        String filterValue01 = _defaultDateFormat.format(dateKey01);
        log(String.format("Filter the date-only field equals '%s'.", filterValue01));
        List<String> expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("1"); // 1950-10-12 08:00:01

        testFiltering(dateCol, keyCol, expectedKeyCol,
                "Equals", filterValue01,
                null, null,
                String.format("Filtering date-only column to equal '%s' not as expected.", filterValue01),
                "Error_Date_Filter_Equals");


        filterValue01 = _defaultDateFormat.format(dateDuplicate);
        log(String.format("Filter the date-only field equals '%s' which has duplicates.", filterValue01));
        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("4"); // 1992-03-03 10:10:10
        expectedKeyCol.add("5"); // 1992-03-03 10:10:10
        expectedKeyCol.add("6"); // 1992-03-03 10:11:34

        testFiltering(dateCol, keyCol, expectedKeyCol,
                "Equals", filterValue01,
                null, null,
                String.format("Filtering date-only column to equal '%s' (duplicate values) not as expected.", filterValue01),
                "Error_Date_Filter_Equals_Duplicates");


        Date filterDate01 = new Calendar.Builder()
                .setDate(2010, 1, 1)
                .build().getTime();
        filterValue01 = _defaultDateFormat.format(filterDate01);
        log(String.format("Filter the date-only field greater than '%s'.", filterValue01));
        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("2"); // (some future date) 14:23:54
        expectedKeyCol.add("3"); // 2024-01-01 00:00:00
        expectedKeyCol.add("8"); // 2024-02-29 18:32:00

        testFiltering(dateCol, keyCol, expectedKeyCol,
                "Is Greater Than or Equal To", filterValue01,
                null, null,
                String.format("Filtering date-only column greater than or equal to '%s' not as expected.", filterValue01),
                "Error_Date_Filter_Greater");


        // Filter between two dates.
        filterDate01 = new Calendar.Builder()
                .setDate(2001, 1, 1)
                .build().getTime();

        Date filterDate02 = new Calendar.Builder()
                .setDate(2010, 1, 1)
                .build().getTime();

        filterValue01 = _defaultDateFormat.format(filterDate01);
        String filterValue02 = _defaultDateFormat.format(filterDate02);

        log(String.format("Filter the date-only field greater than or equal to '%s' and less than '%s'.",
                filterValue01, filterValue02));

        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("9"); // 2002-09-15 17:45:20

        testFiltering(dateCol, keyCol, expectedKeyCol,
                "Is Greater Than or Equal To", filterValue01,
                "Is Less Than", filterValue02,
                String.format("Filtering date-only column between '%s' and '%s' not as expected.", filterValue01, filterValue02),
                "Error_Date_Filter_Between");


        log("Filter the date-only is blank.");

        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("11"); // (empty) 14:59:25

        testFiltering(dateCol, keyCol, expectedKeyCol,
                "Is Blank", null,
                null, null,
                "Filtering date-only to 'Is Blank' not as expected.",
                "Error_Date_Filter_Blank");


        log("Now filter the time-only field.");

        filterValue01 = _defaultTimeFormat.format(dateKey07);
        log(String.format("Filter the time-only field equals '%s'.", filterValue01));
        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("7"); // 1995-03-03 09:10:10

        testFiltering(timeCol, keyCol, expectedKeyCol,
                "Equals", filterValue01,
                null, null,
                String.format("Filtering time-only column to equal '%s' not as expected.", filterValue01),
                "Error_Time_Filter_Equals");


        filterValue01 = new SimpleDateFormat("HH:mm").format(dateKey08);
        log(String.format("Filter the time-only field equals '%s' and treats missing seconds as zero.", filterValue01));

        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("8"); // 2024-02-29 18:32:00 (Entering no seconds is treated as 00 seconds.)

        testFiltering(timeCol, keyCol, expectedKeyCol,
                "Equals", filterValue01,
                null, null,
                String.format("Filtering time-only column to equal '%s' (no seconds) not as expected.", filterValue01),
                "Error_Time_Filter_No_Seconds");


        filterValue01 = _defaultTimeFormat.format(dateDuplicate);
        log(String.format("Filter the time-only field equals '%s' which has duplicates.", filterValue01));
        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("4"); // 1992-03-03 10:10:10
        expectedKeyCol.add("5"); // 1992-03-03 10:10:10

        testFiltering(timeCol, keyCol, expectedKeyCol,
                "Equals", filterValue01,
                null, null,
                String.format("Filtering time-only column to equal '%s' (duplicate values) not as expected.", filterValue01),
                "Error_Time_Filter_Equals_Duplicates");


        filterDate01 = new Calendar.Builder()
                .setTimeOfDay(14, 0, 0)
                .build().getTime();
        filterValue01 = _defaultTimeFormat.format(filterDate01);
        log(String.format("Filter the time-only field greater than '%s'.", filterValue01));

        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("2"); // (some future date) 14:23:54
        expectedKeyCol.add("8"); // 2024-02-29 18:32:00
        expectedKeyCol.add("9"); // 2002-09-15 17:45:20
        expectedKeyCol.add("11"); // (empty) 14:59:25

        testFiltering(timeCol, keyCol, expectedKeyCol,
                "Is Greater Than or Equal To", filterValue01,
                null, null,
                String.format("Filtering time-only column greater than or equal to '%s' not as expected.", filterValue01),
                "Error_Time_Filter_Greater");


        filterDate01 = new Calendar.Builder()
                .setTimeOfDay(9, 0, 0)
                .build().getTime();
        filterValue01 = _defaultTimeFormat.format(filterDate01);

        filterDate02 = new Calendar.Builder()
                .setTimeOfDay(15, 0, 0)
                .build().getTime();
        filterValue02 = _defaultTimeFormat.format(filterDate02);

        log(String.format("Filter the time-only field greater than or equal to '%s' and less than '%s'.",
                filterValue01, filterValue02));

        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("2"); // (some future date) 14:23:54
        expectedKeyCol.add("4"); // 1992-03-03 10:10:10
        expectedKeyCol.add("5"); // 1992-03-03 10:10:10
        expectedKeyCol.add("6"); // 1992-03-03 10:11:34
        expectedKeyCol.add("7"); // 1995-03-03 09:10:10
        expectedKeyCol.add("11"); // (empty) 14:59:25

        testFiltering(timeCol, keyCol, expectedKeyCol,
                "Is Greater Than or Equal To", filterValue01,
                "Is Less Than", filterValue02,
                String.format("Filtering time-only column between '%s' and '%s' not as expected.", filterValue01, filterValue02),
                "Error_Time_Filter_Between");


        log("Filter the time-only is blank.");
        expectedKeyCol = new ArrayList<>();
        expectedKeyCol.add("10"); // 1989-08-12 (empty)

        testFiltering(timeCol, keyCol, expectedKeyCol,
                "Is Blank", null,
                null, null,
                "Filtering time-only to 'Is Blank' not as expected.",
                "Error_Time_Filter_Blank");

    }

    /**
     * <p>
     *     Validate error messages are expected for invalid values in date-only, time-only and DateTime fields.
     *     Validates bulk import, file (xlsx) and UI.
     * </p>
     * @throws IOException Can be thrown by helper that checks if the list already exists.
     * @throws CommandException Can be thrown by helper that checks if the list already exists.
     */
    @Test
    public void testInvalidDateAndTimeInsert() throws IOException, CommandException
    {

        String listName = "Date and Time Invalid Insert List";
        String dateCol = "Date";
        String timeCol = "Time";
        String dateTimeCol = "DateTime";

        log(String.format("Create a list named '%s' with date-only, time-only and dateTime fields.", listName));

        _listHelper.createList(PROJECT_NAME, listName, "Key",
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime).setLabel(dateTimeCol)
        );

        log("Validate adding entries in bulk will give a meaningful error with a bad format.");

        String expectedErrorMsgFormat = "Could not convert value '%s' (String) for %s field '%s'";
        String badDate = "45/93/2001";
        String nonLeapDay = "2/29/2023";
        String badTime = "26:abc:604";
        String badDateTime = "1/1/22 4:55 pm And Some Text";

        ImportDataPage importPage = _listHelper.clickImportData();


        String bulkImportText = String.format("%s\t\n%s", dateCol, badDate);
        importPage.setText(bulkImportText);
        String actualErrorMsg = importPage.submitExpectingError();

        checker().withScreenshot()
                .verifyEquals("Error message for a bad date value not as expected.",
                        String.format(expectedErrorMsgFormat, badDate, "Date", dateCol), actualErrorMsg);

        // Not a leap year.
        bulkImportText = String.format("%s\t\n%s", dateCol, nonLeapDay);
        importPage.setText(bulkImportText);
        actualErrorMsg = importPage.submitExpectingError();

        checker().withScreenshot()
                .verifyEquals("Error message for invalid leap day not as expected.",
                        String.format(expectedErrorMsgFormat, nonLeapDay, "Date", dateCol), actualErrorMsg);

        bulkImportText = String.format("%s\t\n%s", timeCol, badTime);
        importPage.setText(bulkImportText);
        actualErrorMsg = importPage.submitExpectingError();

        checker().withScreenshot()
                .verifyEquals("Error message for a bad time value not as expected.",
                        String.format(expectedErrorMsgFormat, badTime, "Time", timeCol), actualErrorMsg);

        bulkImportText = String.format("%s\t\n%s", dateTimeCol, badDateTime);
        importPage.setText(bulkImportText);
        actualErrorMsg = importPage.submitExpectingError();

        checker().withScreenshot()
                .verifyEquals("Error message for a bad DateTime value not as expected.",
                        String.format(expectedErrorMsgFormat, badDateTime, "Timestamp", dateTimeCol), actualErrorMsg);

        File excelDateTimeFile = TestFileUtils.getSampleData("lists/Bad_Date_And_Time_Values.xlsx");
        importPage = importPage.selectUpload();
        importPage.setFile(excelDateTimeFile);
        actualErrorMsg = importPage.submitExpectingError();

        checker().withScreenshot()
                .verifyTrue("Error message for bad file import not as expected.",
                        actualErrorMsg.contains(String.format(expectedErrorMsgFormat, badTime, "Time", timeCol)) &&
                                actualErrorMsg.contains(String.format(expectedErrorMsgFormat, nonLeapDay, "Date", dateCol)) &&
                                actualErrorMsg.contains(String.format(expectedErrorMsgFormat, badDateTime, "Timestamp", dateTimeCol))
                );

        _listHelper.beginAtList(getProjectName(), listName);

        DataRegionTable regionTable = new DataRegionTable("query", getDriver());
        regionTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_" + timeCol), badTime);
        setFormElement(Locator.name("quf_" + dateCol), badDate);
        setFormElement(Locator.name("quf_" + dateTimeCol), badDateTime);
        clickButton("Submit");

        checker().verifyTrue("Bad value error message in UI not as expected.",
                isTextPresent(String.format("Could not convert value: %s", badTime),
                        String.format("Could not convert value: %s", badDate),
                        String.format("Could not convert value: %s", badDateTime)));

    }

    /**
     * <p>
     *     Test setting the format property on a date-only, time-only and DateTime field.
     * </p>
     * @throws IOException Can be thrown by helper that checks if the list already exists.
     * @throws CommandException Can be thrown by helper that checks if the list already exists.
     */
    @Test
    public void testDateAndTimeFormat() throws IOException, CommandException
    {
        DATE_FORMAT dateFormat01 = DATE_FORMAT.Default;
        TIME_FORMAT timeFormat01 = TIME_FORMAT.hh_mm_a;
        String dateTimeFormat01 = String.format("%s %s",
                DATE_FORMAT.Default, TIME_FORMAT.hh_mm_a);

        SimpleDateFormat formatterDate = new SimpleDateFormat(dateFormat01.toString());
        SimpleDateFormat formatterTime = new SimpleDateFormat(timeFormat01.toString());
        SimpleDateFormat formatterDateTime = new SimpleDateFormat(dateTimeFormat01);

        String listName = "Date and Time Format List";
        String dateCol = "Date";
        String timeCol = "Time";
        String dateTimeCol = "DateTime";

        log(String.format("Create a list named '%s' with date-only, time-only and dateTime fields.", listName));

        _listHelper.createList(PROJECT_NAME, listName, "key",
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date).setFormat(dateFormat01.toString()),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time).setFormat(timeFormat01.toString()),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime).setFormat(dateTimeFormat01).setLabel(dateTimeCol)
        );

        List<Date> dates = new ArrayList<>();

        dates.add(new Calendar.Builder()
                .setDate(1955, 0, 1)
                .setTimeOfDay(0, 0, 0)
                .build().getTime());

        dates.add(new Calendar.Builder()
                .setDate(2023, 11, 31)
                .setTimeOfDay(23, 59, 59)
                .build().getTime());

        dates.add(new Calendar.Builder()
                .setDate(2022, 5, 1)
                .setTimeOfDay(12, 1, 23)
                .build().getTime());

        dates.add(new Calendar.Builder()
                .setDate(2023, 6, 4)
                .setTimeOfDay(8, 12, 35)
                .build().getTime());

        // Milliseconds should default to 0 if not given.
        dates.add(new Calendar.Builder()
                .setDate(2024, 1, 29)
                .setTimeOfDay(11, 11, 11)
                .build().getTime());

        StringBuilder bulkImportText = new StringBuilder();

        bulkImportText.append(String.format("%s\t%s\t%s\n", dateCol, timeCol, dateTimeCol));

        // Use a different format when writing the data for bulk import.
        SimpleDateFormat inputDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat inputTimeFormatter = new SimpleDateFormat("hh:mm:ss aa");
        SimpleDateFormat inputDateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for(Date date : dates)
        {
            bulkImportText.append(String.format("%s\t%s\t%s\n",
                    inputDateFormatter.format(date), inputTimeFormatter.format(date), inputDateTimeFormatter.format(date)
            ));
        }

        _listHelper.bulkImportData(bulkImportText.toString());

        DataRegionTable table = new DataRegionTable("query", getDriver());

        List<Map<String, String>> expectedData = new ArrayList<>();

        for(Date date : dates)
        {
            expectedData.add(
                    Map.of(
                            dateCol, formatterDate.format(date),
                            timeCol, formatterTime.format(date),
                            dateTimeCol, formatterDateTime.format(date)
                    ));
        }

        validateListDataInUI(table, expectedData);
        checker().screenShotIfNewError("Format01_Error");

        DATE_FORMAT dateFormat02 = DATE_FORMAT.ddMMMyy;
        TIME_FORMAT timeFormat02 = TIME_FORMAT.HH_mm_ss;
        DATE_FORMAT dateTimeDateFormat02 = DATE_FORMAT.dd_MMM_yyyy;
        TIME_FORMAT dateTimeTimeFormat02 = TIME_FORMAT.HH_mm_ss;

        formatterDate = new SimpleDateFormat(dateFormat02.toString());
        formatterTime = new SimpleDateFormat(timeFormat02.toString());
        formatterDateTime = new SimpleDateFormat(String.format("%s %s", dateTimeDateFormat02, dateTimeTimeFormat02));

        clickAndWait(table.getHeaderButton("Design"));
        EditListDefinitionPage listDefinitionPage = new EditListDefinitionPage(getDriver());

        DomainFormPanel domainEditor = listDefinitionPage.getFieldsPanel();
        domainEditor.getField(timeCol).setTimeFormat(timeFormat02);
        domainEditor.getField(dateCol).setDateFormat(dateFormat02);
        domainEditor.getField(dateTimeCol).setDateTimeFormat(dateTimeDateFormat02, dateTimeTimeFormat02);

        listDefinitionPage.clickSave();

        expectedData = new ArrayList<>();

        for(Date date : dates)
        {
            expectedData.add(
                    Map.of(
                            dateCol, formatterDate.format(date),
                            timeCol, formatterTime.format(date),
                            dateTimeCol, formatterDateTime.format(date)
                    ));
        }

        validateListDataInUI(table, expectedData);
        checker().screenShotIfNewError("Format02_Error");
    }

    /**
     * <p>
     *     Test converting a DateTime filed to a date-only and time-only field. Test also validates that a date-only
     *     field can be converted to a DateTime field, but a time-only field can not.
     * </p>
     * @throws IOException Can be thrown by helper that checks if the list already exists.
     * @throws CommandException Can be thrown by helper that checks if the list already exists.
     */
    @Test
    public void testConvertDateTimeField() throws IOException, CommandException
    {

        String listName = "Convert DateTime Field List";
        String dateTimeToTimeCol = "DT_To_Time";
        String dateTimeToDateCol = "DT_To_Date";

        log(String.format("Create a list named '%s' with two DateTime fields that will be converted to date-only and time-only fields.", listName));

        String dtFormatDate = String.format("%s %s",
                DATE_FORMAT.yyyy_MMM_dd, TIME_FORMAT.HH_mm);
        String dtFormatTime = String.format("%s %s",
                DATE_FORMAT.yyyy_MMM_dd, TIME_FORMAT.HH_mm_ss);

        SimpleDateFormat formatterFormatTime = new SimpleDateFormat(dtFormatTime);

        log(String.format("Set the format for one DateTime field to '%s'.", dtFormatDate));
        log(String.format("Set the format for the other DateTime field to '%s'. This field will be converted to a tine-only field.", dtFormatTime));

        _listHelper.createList(PROJECT_NAME, listName, "Key",
                new FieldDefinition(dateTimeToDateCol, FieldDefinition.ColumnType.DateAndTime).setFormat(dtFormatDate).setLabel(dateTimeToDateCol),
                new FieldDefinition(dateTimeToTimeCol, FieldDefinition.ColumnType.DateAndTime).setFormat(dtFormatTime).setLabel(dateTimeToTimeCol)
        );

        List<Date> dates = new ArrayList<>();

        dates.add(new Calendar.Builder()
                .setDate(1962, 0, 1)
                .setTimeOfDay(0, 0, 0)
                .build().getTime());

        dates.add(new Calendar.Builder()
                .setDate(2023, 11, 31)
                .setTimeOfDay(23, 59, 59)
                .build().getTime());

        dates.add(new Calendar.Builder()
                .setDate(2024, 1, 29)
                .setTimeOfDay(11, 11, 11)
                .build().getTime());

        dates.add(new Calendar.Builder()
                .setDate(2022, 5, 6)
                .setTimeOfDay(13, 12, 11)
                .build().getTime());

        dates.add(new Calendar.Builder()
                .setDate(1999, 7, 18)
                .setTimeOfDay(15, 0, 12)
                .build().getTime());

        dates.add(new Calendar.Builder()
                .setDate(2005, 3, 20)
                .setTimeOfDay(4, 54, 33)
                .build().getTime());

        StringBuilder bulkImportText = new StringBuilder();

        bulkImportText.append(String.format("%s\t%s\n", dateTimeToDateCol, dateTimeToTimeCol));

        for(Date date : dates)
        {
            bulkImportText.append(String.format("%s\t%s\n",
                    _defaultDateTimeFormat.format(date), formatterFormatTime.format(date)
            ));
        }

        _listHelper.bulkImportData(bulkImportText.toString());

        DataRegionTable table = new DataRegionTable("query", getDriver());

        clickAndWait(table.getHeaderButton("Design"));
        EditListDefinitionPage listDefinitionPage = new EditListDefinitionPage(getDriver());

        log(String.format("Change field '%s' to be a date-only field.", dateTimeToDateCol));

        DomainFormPanel domainEditor = listDefinitionPage.getFieldsPanel();
        ModalDialog confirmDialog = domainEditor.getField(dateTimeToDateCol).setTypeWithDialog(FieldDefinition.ColumnType.Date);

        String expectedMsg = "This change will convert the values in the field from DateTime to Date. This will cause the Time portion of the value to be removed.";
        String actualMsg = confirmDialog.getBodyText();

        checker().withScreenshot("Convert_To_Date_Msg_Error")
                .verifyTrue("Confirmation dialog for converting DateTime to Date does not have expected message.",
                        actualMsg.contains(expectedMsg));

        confirmDialog.dismiss("Yes, Change Data Type");

        log(String.format("Change field '%s' to be a time-only field.", dateTimeToTimeCol));

        confirmDialog = domainEditor.getField(dateTimeToTimeCol).setTypeWithDialog(FieldDefinition.ColumnType.Time);
        expectedMsg = "This change will convert the values in the field from DateTime to Time. This will cause the Date portion of the value to be removed. Once you save your changes, you will not be able to change it back to DateTime.";
        actualMsg = confirmDialog.getBodyText();

        checker().withScreenshot("Convert_To_Time_Msg_Error")
                .verifyTrue("Confirmation dialog for converting DateTime to Time does not have expected message.",
                        actualMsg.contains(expectedMsg));

        confirmDialog.dismiss("Yes, Change Data Type");

        listDefinitionPage.clickSave();

        // Update default format after changing the types.
        DATE_FORMAT dateFormat = DATE_FORMAT.Default;
        TIME_FORMAT timeFormat = TIME_FORMAT.Default;

        SimpleDateFormat formatterDate = new SimpleDateFormat(dateFormat.toString());
        SimpleDateFormat formatterTime = new SimpleDateFormat(timeFormat.toString());

        List<Map<String, String>> expectedData = new ArrayList<>();

        for(Date date : dates)
        {
            expectedData.add(
                    Map.of(
                            dateTimeToDateCol, formatterDate.format(date),
                            dateTimeToTimeCol, formatterTime.format(date)
                    ));
        }

        log("Validate the data in the list.");

        validateListDataInUI(table, expectedData);
        checker().screenShotIfNewError("Convert_From_DateTime_Error");

        table = new DataRegionTable("query", getDriver());

        clickAndWait(table.getHeaderButton("Design"));
        listDefinitionPage = new EditListDefinitionPage(getDriver());

        domainEditor = listDefinitionPage.getFieldsPanel();

        log(String.format("Validate that field '%s' cannot be converted back to a DateTime field.", dateTimeToTimeCol));

        List<String> expectedList = List.of(FieldDefinition.ColumnType.MultiLine.getLabel(),
                FieldDefinition.ColumnType.String.getLabel(),
                FieldDefinition.ColumnType.Time.getLabel());
        List<String> actualList = domainEditor.getField(dateTimeToTimeCol).getTypeOptions();

        checker().verifyEquals("Listed types Time field can convert to are not as expected.",
                expectedList, actualList);

        expectedList = List.of(FieldDefinition.ColumnType.Date.getLabel(),
                FieldDefinition.ColumnType.DateAndTime.getLabel(),
                FieldDefinition.ColumnType.MultiLine.getLabel(),
                FieldDefinition.ColumnType.String.getLabel());
        actualList = domainEditor.getField(dateTimeToDateCol).getTypeOptions();

        checker().verifyEquals("Listed types Date field can convert to are not as expected.",
                expectedList, actualList);

        log(String.format("Change field '%s' back to a DateTime field.", dateTimeToDateCol));

        domainEditor.getField(dateTimeToDateCol).setType(FieldDefinition.ColumnType.DateAndTime, true);

        listDefinitionPage.clickSave();

        expectedData = new ArrayList<>();

        for(Date date : dates)
        {
            expectedData.add(
                    Map.of(
                            dateTimeToDateCol, String.format("%s 00:00", formatterDate.format(date)),
                            dateTimeToTimeCol, formatterTime.format(date)
                    ));
        }

        log("Again validate the data in the list.");

        validateListDataInUI(table, expectedData);
        checker().screenShotIfNewError("Convert_To_DateTime_Error");

    }

}
