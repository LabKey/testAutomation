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
        log("Setting the parsing pattern");
        String dateTimePattern = "ddMMMyyyy:HH:mm:ss";
        String datePattern = "mm/dd/yy";
        String timePattern = "hh:mm a";
        setAdditionalParsingPatterns(dateTimePattern, datePattern, timePattern);

        log("Update a row with a date in a non-standard format");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(dateList));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());
        listTable.clickEditRow(0);
        setFormElement(Locator.name("quf_dateTimeCol"), "25Nov2020:24:23:00");
        setFormElement(Locator.name("quf_dateCol"), "11/25/20");
        setFormElement(Locator.name("quf_timeCol"), "12:23 am");
        clickButton("Submit");

        checker().verifyEquals("Incorrect date parsed while editing the row", "2020-11-26 00:23", listTable.getDataAsText(0, "dateTimeCol"));

        log("Insert a row with a date in a non-standard format");
        listTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_name"), "Second");
        setFormElement(Locator.name("quf_dateTimeCol"), "23Nov2020:12:23:34");
        setFormElement(Locator.name("quf_dateCol"), "11/23/20");
        setFormElement(Locator.name("quf_timeCol"), "12:23 pm");
        clickButton("Submit");

        listTable.setFilter("name", "Equals", "Second");
        checker().verifyEquals("Incorrect date parsed while inserting row", "2020-11-23 12:23", listTable.getDataAsText(0, "dateTimeCol"));

        log("Bulk import with some dates in non-standard format");
        listTable.clearAllFilters();
        listTable.clickImportBulkData();
        click(Locator.tagWithText("h3", "Upload file (.xlsx, .xls, .csv, .txt)"));
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("DateParsing/BulkImportDateParsing.xlsx"));
        clickButton("Submit");

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dateList));
        listTable = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Incorrect number of rows after bulk import", 6, listTable.getDataRowCount());
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
        checker().verifyEquals("Incorrect date parsed while importing", Arrays.asList("2020-11-29 00:23", "2020-11-28 00:23", "2024-02-05 16:36")
                , table.getColumnDataAsText("dateTimeCol"));
    }

    private void setAdditionalParsingPatterns(String dateTimePattern, String datePattern, String timePattern)
    {
        LookAndFeelSettingsPage lookAndFeelSettingsPage = goToAdminConsole().clickLookAndFeelSettings();

        if(null != dateTimePattern && !dateTimePattern.isEmpty())
        {
            lookAndFeelSettingsPage.setAdditionalParsingPatternDates(datePattern);
        }

        if(null != datePattern && !datePattern.isEmpty())
        {
            lookAndFeelSettingsPage.setAdditionalParsingPatternDateAndTime(dateTimePattern);
        }

        if(null != timePattern && !timePattern.isEmpty())
        {
            lookAndFeelSettingsPage.setAdditionalParsingPatternTimes(timePattern);
        }

        lookAndFeelSettingsPage.save();
    }

    private void setDisplayFormats(String dateTimeFormat, String dateFormat, String timeFormat)
    {
        LookAndFeelSettingsPage lookAndFeelSettingsPage = goToAdminConsole().clickLookAndFeelSettings();

        if(null != dateTimeFormat && !dateTimeFormat.isEmpty())
        {
            lookAndFeelSettingsPage.setDefaultDateTimeDisplay(dateTimeFormat);
        }

        if(null != dateFormat && !dateFormat.isEmpty())
        {
            lookAndFeelSettingsPage.setDefaultDateDisplay(dateFormat);
        }

        if(null != timeFormat && !timeFormat.isEmpty())
        {
            lookAndFeelSettingsPage.setDefaultTimeDisplay(timeFormat);
        }

        lookAndFeelSettingsPage.save();
    }

}
