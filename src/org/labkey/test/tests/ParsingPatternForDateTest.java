package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class ParsingPatternForDateTest extends BaseWebDriverTest
{
    private String dateList = "Sample List with Date column";

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        ParsingPatternForDateTest init = (ParsingPatternForDateTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), "Study");
        _studyHelper.startCreateStudy()
                .setTimepointType(StudyHelper.TimepointType.DATE)
                .createStudy();

        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", dateList);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("dateTimeCol", FieldDefinition.ColumnType.DateAndTime),
                        new FieldDefinition("dateCol", FieldDefinition.ColumnType.Date),
                        new FieldDefinition("timeCol", FieldDefinition.ColumnType.Time)));
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        dgen.addCustomRow(Map.of("name", "First", "dateTimeCol", "05/10/2020", "date", "02/05/2024", "time", "16:43:32"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());
    }

    @AfterClass
    public static void resetLookAndFeel()
    {
        ParsingPatternForDateTest init = (ParsingPatternForDateTest) getCurrentTest();
        LookAndFeelSettingsPage settingsPage = LookAndFeelSettingsPage.beginAt(init);
        settingsPage.reset();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "Parsing Pattern For Date And Date Time Test Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return new ArrayList<>();
    }

    @Test
    public void testAdditionalParsingPatternDateAndTime()
    {
        log("Setting the additional parsing patterns.");
        String dateTimePattern = "ddMMMyyyy:HH:mm:ss";
        String datePattern = "mm/dd/yy";
        String timePattern = "hh:mm a";
        setAdditionalParsingPatterns(dateTimePattern, datePattern, timePattern);

        List<Map<String, String>> expectedTableValues = new ArrayList<>();

        log("Update a row with values in the additional parsing format.");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(dateList));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());
        listTable.clickEditRow(0);

        Map<String, String> setValuesMap = new HashMap<>();
        setValuesMap.put("dateTimeCol", "2020-11-26 00:23");
        setValuesMap.put("dateCol", "2020-11-25");
        setValuesMap.put("timeCol", "00:23:00");

        for(var entry : setValuesMap.entrySet())
        {
            setFormElement(Locator.name(String.format("quf_%s", entry.getKey())), entry.getValue());
        }
        clickButton("Submit");

        // Add the name to the values expected in the grid.
        setValuesMap.put("name", "First");

        expectedTableValues.add(setValuesMap);

        log("Insert a row with a data in the additional parsing format.");

        listTable.clickInsertNewRow();

        setValuesMap = Map.of(
                "name", "Second",
                "dateTimeCol", "2020-11-23 12:23",
                "dateCol", "2020-11-23",
                "timeCol", "12:23:00"
        );

        expectedTableValues.add(setValuesMap);

        for(var entry : setValuesMap.entrySet())
        {
            setFormElement(Locator.name(String.format("quf_%s", entry.getKey())), entry.getValue());
        }
        clickButton("Submit");

        log("Bulk import with values in additional parsing patterns.");
        listTable.clearAllFilters();
        listTable.clickImportBulkData();
        click(Locator.tagContainingText("h3", "Upload file"));

        // Note: the date and time columns in this xlsx are formatted as date and time types. Excel does not have a date-time format, that is a string.
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("DateParsing/BulkImportDateParsing.xlsx"));
        clickButton("Submit");

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dateList));
        listTable = new DataRegionTable("query", getDriver());

        expectedTableValues.add(Map.of(
                "name", "Third",
                "dateTimeCol", "2020-12-23 12:20",
                "dateCol", "2020-12-23",
                "timeCol", "12:20:34")
        );

        expectedTableValues.add(Map.of(
                "name", "Fourth",
                "dateTimeCol", "2020-01-23 12:24",
                "dateCol", "2020-01-23",
                "timeCol", "00:24:35")
        );

        expectedTableValues.add(Map.of(
                "name", "Fifth",
                "dateTimeCol", "2020-03-23 12:23",
                "dateCol", "2020-03-23",
                "timeCol", "12:23:36")
        );

        expectedTableValues.add(Map.of(
                "name", "Sixth",
                "dateTimeCol", "2020-06-23 12:29",
                "dateCol", "2020-06-23",
                "timeCol", "11:29:37")
        );

        int i = 0;
        for(Map<String, String> expectedRowMap : expectedTableValues)
        {
            Map<String, String> actualRowMap = listTable.getRowDataAsMap(i);
            checker().verifyEquals(String.format("Incorrect parsed data for row %d '%s'.", i, expectedRowMap.get("name")),
                    expectedRowMap, actualRowMap);
            i++;
        }

    }

    @Test
    public void testAdditionalParsingPatternForPipelineJobs()
    {
        log("Setting the parsing pattern");
        String dateTimePattern = "ddMMMyyyy:HH:mm:ss";
        String datePattern = "dd/mm/yy";
        String timePattern = "hh:mm a";
        setAdditionalParsingPatterns(dateTimePattern, datePattern, timePattern);

        log("Importing a study where a dataset has some dates in the non-standard format");
        goToProjectHome();
        importFolderFromZip(TestFileUtils.getSampleData("DateParsing/StudyForDateParsing.zip"), false, 1);

        goToProjectHome();
        clickAndWait(Locator.linkContainingText("dataset"));
        clickAndWait(Locator.linkContainingText("Dataset1")); // dataset with non standard date format.

        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        checker().verifyEquals("Incorrect date-time parsed while importing", Arrays.asList("2020-11-29 00:23", "2020-11-28 00:23", "2024-02-05 16:36")
                , table.getColumnDataAsText("dateTimeCol"));

        checker().verifyEquals("Incorrect date parsed while importing", Arrays.asList("2020-11-29", "2020-11-28", "2024-02-05")
                , table.getColumnDataAsText("dateCol"));

        checker().verifyEquals("Incorrect time parsed while importing", Arrays.asList("00:23:00", "00:23:00", "16:36:00")
                , table.getColumnDataAsText("timeCol"));
    }

    private void setAdditionalParsingPatterns(String dateTimePattern, String datePattern, String timePattern)
    {
        LookAndFeelSettingsPage lookAndFeelSettingsPage = LookAndFeelSettingsPage.beginAt(this);

        if(null != dateTimePattern && !dateTimePattern.isEmpty())
        {
            lookAndFeelSettingsPage.setAdditionalParsingPatternDateAndTime(dateTimePattern);
        }

        if(null != datePattern && !datePattern.isEmpty())
        {
            lookAndFeelSettingsPage.setAdditionalParsingPatternDates(datePattern);
        }

        if(null != timePattern && !timePattern.isEmpty())
        {
            lookAndFeelSettingsPage.setAdditionalParsingPatternTimes(timePattern);
        }

        lookAndFeelSettingsPage.save();
    }

}
