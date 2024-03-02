package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
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
public class ParsingPatternForDateTest extends BaseWebDriverTest
{
    private static final String TEST_LIST = "Input Format List";

    private static final String COL_NAME = "name";
    private static final String COL_DATETIME = "dateTimeCol";
    private static final String COL_DATE = "dateCol";
    private static final String COL_TIME = "timeCol";

    private static int completedPipelineJobs = 0;

    @BeforeClass
    public static void setupProject()
    {
        ParsingPatternForDateTest init = (ParsingPatternForDateTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        _studyHelper.startCreateStudy()
                .setTimepointType(StudyHelper.TimepointType.DATE)
                .createStudy();

        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");
    }

    @AfterClass
    public static void resetAfterClass()
    {
        ParsingPatternForDateTest init = (ParsingPatternForDateTest) getCurrentTest();
        init.resetSiteSettings();
    }

    @Before
    public void resetForTest()
    {
        resetSiteSettings();
        resetProjectSettings();
    }

    public void resetSiteSettings()
    {
        log("Reset site settings.");
        LookAndFeelSettingsPage.beginAt(this).reset();
    }

    private void resetProjectSettings()
    {
        log("Reset project / folder settings.");
        goToProjectHome();
        ProjectSettingsPage.beginAt(this).reset();
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
    public void testSiteAdditionalParsingPatternDateAndTime() throws IOException, CommandException
    {
        createList();
        log("Setting the additional parsing patterns for the site.");
        testParsingPatternsList(true);
    }

    @Test
    public void testProjectAdditionalParsingPatternDateAndTime() throws IOException, CommandException
    {
        createList();
        log("Set the additional parsing patterns for the project.");
        testParsingPatternsList(false);
    }

    private void createList() throws IOException, CommandException
    {
        // Delete the list if it already exists.
        goToProjectHome();
        if(goToManageLists().getGrid().getListNames().contains(TEST_LIST))
        {
            _listHelper.goToList(TEST_LIST);
            _listHelper.deleteList();
        }

        TestDataGenerator dgen = new IntListDefinition(TEST_LIST, "id")
                .setFields(List.of(
                        new FieldDefinition(COL_NAME, FieldDefinition.ColumnType.String),
                        new FieldDefinition(COL_DATETIME, FieldDefinition.ColumnType.DateAndTime),
                        new FieldDefinition(COL_DATE, FieldDefinition.ColumnType.Date),
                        new FieldDefinition(COL_TIME, FieldDefinition.ColumnType.Time)))
                .create(createDefaultConnection(), getProjectName());

        // Prepopulate the list with one item.
        dgen.addCustomRow(Map.of(COL_NAME, "First", COL_DATETIME, "05/10/2020", "date", "02/05/2024", "time", "16:43:32"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());
    }

    private void testParsingPatternsList(boolean changeSiteSettings)
    {

        String dateTimePattern = "ddMMMyyyy:HH:mm:ss";
        String datePattern = "mm/dd/yy";
        String timePattern = "hh:mm a";

        if(changeSiteSettings)
        {
            setSiteAdditionalParsingPatterns(dateTimePattern, datePattern, timePattern);
        }
        else
        {
            goToProjectHome();
            setProjectAdditionalParsingPatterns(dateTimePattern, datePattern, timePattern);
        }

        List<Map<String, String>> expectedTableValues = new ArrayList<>();

        log("Update a row with values in the additional parsing format.");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_LIST));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());
        listTable.clickEditRow(0);

        Map<String, String> setValuesMap = new HashMap<>();
        setValuesMap.put(COL_DATETIME, "2020-11-26 00:23");
        setValuesMap.put(COL_DATE, "2020-11-25");
        setValuesMap.put(COL_TIME, "00:23:00");

        for(var entry : setValuesMap.entrySet())
        {
            setFormElement(Locator.name(String.format("quf_%s", entry.getKey())), entry.getValue());
        }
        clickButton("Submit");

        // Add the name to the values expected in the grid.
        setValuesMap.put(COL_NAME, "First");

        expectedTableValues.add(setValuesMap);

        log("Insert a row with a data in the additional parsing format.");

        listTable.clickInsertNewRow();

        setValuesMap = Map.of(
                COL_NAME, "Second",
                COL_DATETIME, "2020-11-23 12:23",
                COL_DATE, "2020-11-23",
                COL_TIME, "12:23:00"
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

        log("Import a xlsx file where date & time values are numbers.");
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("DateParsing/BulkImportDateParsing.xlsx"));
        clickButton("Submit");

        listTable.clickImportBulkData();
        click(Locator.tagContainingText("h3", "Upload file"));

        log("Import a xlsx file where date & time values are text.");
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("DateParsing/BulkImportDateParsing_Text.xlsx"));
        clickButton("Submit");

        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_LIST));
        listTable = new DataRegionTable("query", getDriver());

        expectedTableValues.add(Map.of(
                COL_NAME, "Third",
                COL_DATETIME, "2020-12-23 12:20",
                COL_DATE, "2020-12-23",
                COL_TIME, "12:20:34")
        );

        expectedTableValues.add(Map.of(
                COL_NAME, "Fourth",
                COL_DATETIME, "2020-01-23 12:24",
                COL_DATE, "2020-01-23",
                COL_TIME, "00:24:35")
        );

        expectedTableValues.add(Map.of(
                COL_NAME, "Fifth",
                COL_DATETIME, "2020-03-23 12:23",
                COL_DATE, "2020-03-23",
                COL_TIME, "12:23:36")
        );

        expectedTableValues.add(Map.of(
                COL_NAME, "Sixth",
                COL_DATETIME, "2020-06-23 12:29",
                COL_DATE, "2020-06-23",
                COL_TIME, "11:29:37")
        );

        int i = 0;
        for(Map<String, String> expectedRowMap : expectedTableValues)
        {
            Map<String, String> actualRowMap = listTable.getRowDataAsMap(i);
            checker().verifyEquals(String.format("Incorrect parsed data for row %d '%s'.", i, expectedRowMap.get(COL_NAME)),
                    expectedRowMap, actualRowMap);
            i++;
        }

    }

    @Test
    public void testSiteAdditionalParsingPatternForPipelineJobs()
    {
        testParsingPatternsPipelineJobs(true);
    }

    @Test
    public void testProjectAdditionalParsingPatternForPipelineJobs()
    {
        testParsingPatternsPipelineJobs(false);
    }

    private void testParsingPatternsPipelineJobs(boolean changeSiteSettings)
    {
        log("Setting the parsing pattern");
        String dateTimePattern = "ddMMMyyyy:HH:mm:ss";
        String datePattern = "dd/mm/yy";
        String timePattern = "hh:mm a";

        if(changeSiteSettings)
        {
            setSiteAdditionalParsingPatterns(dateTimePattern, datePattern, timePattern);
        }
        else
        {
            goToProjectHome();
            setProjectAdditionalParsingPatterns(dateTimePattern, datePattern, timePattern);
        }

        log("Importing a study where a dataset has some dates in the non-standard format");
        goToProjectHome();
        completedPipelineJobs = completedPipelineJobs + 1;
        importFolderFromZip(TestFileUtils.getSampleData("DateParsing/StudyForDateParsing.zip"), false, completedPipelineJobs);

        goToProjectHome();
        clickAndWait(Locator.linkContainingText("dataset"));
        clickAndWait(Locator.linkContainingText("Dataset1")); // dataset with non standard date format.

        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        checker().verifyEquals("Incorrect date-time parsed while importing", Arrays.asList("2020-11-29 00:23", "2020-11-28 00:23", "2024-02-05 16:36")
                , table.getColumnDataAsText(COL_DATETIME));

        checker().verifyEquals("Incorrect date parsed while importing", Arrays.asList("2020-11-29", "2020-11-28", "2024-02-05")
                , table.getColumnDataAsText(COL_DATE));

        checker().verifyEquals("Incorrect time parsed while importing", Arrays.asList("00:23:00", "00:23:00", "16:36:00")
                , table.getColumnDataAsText(COL_TIME));
    }

    private void setSiteAdditionalParsingPatterns(String dateTimePattern, String datePattern, String timePattern)
    {
        LookAndFeelSettingsPage lookAndFeelSettingsPage = LookAndFeelSettingsPage.beginAt(this);
        setAdditionalParsingPatterns(lookAndFeelSettingsPage, dateTimePattern, datePattern, timePattern);
    }

    private void setProjectAdditionalParsingPatterns(String dateTimePattern, String datePattern, String timePattern)
    {
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this);
        setAdditionalParsingPatterns(projectSettingsPage, dateTimePattern, datePattern, timePattern);
    }

    private void setAdditionalParsingPatterns(BaseSettingsPage settingsPage, String dateTimePattern, String datePattern, String timePattern)
    {
        if(null != dateTimePattern && !dateTimePattern.isEmpty())
        {
            settingsPage.setAdditionalParsingPatternDateAndTime(dateTimePattern);
        }

        if(null != datePattern && !datePattern.isEmpty())
        {
            settingsPage.setAdditionalParsingPatternDates(datePattern);
        }

        if(null != timePattern && !timePattern.isEmpty())
        {
            settingsPage.setAdditionalParsingPatternTimes(timePattern);
        }

        settingsPage.save();
    }

}
