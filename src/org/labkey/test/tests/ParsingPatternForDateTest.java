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
import org.labkey.test.util.DomainUtils;
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

    private static final String LIST_SCHEMA = "lists";
    private static final String TEST_PARSING = "Additional Parsing Format List";
    private static final String TEST_MODE = "Date Parsing Mode List";

    private static final String COL_NAME = "name";
    private static final String COL_DATETIME = "dateTimeCol";
    private static final String COL_DATE = "dateCol";
    private static final String COL_TIME = "timeCol";

    private static int completedPipelineJobs = 0;

    private static final String DATE_TIME_PATTERN = "ddMMMyyyy:HH:mm:ss";
    private static final String DATE_PATTERN = "dd/mm/yy";
    private static final String TIME_PATTERN = "hh:mm a";

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
    public static void afterClass()
    {
        try
        {
        ((ParsingPatternForDateTest) getCurrentTest()).resetSiteSettings();
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to reset site look and settings after class: " + e);
        }
    }

    @Before
    public void resetForTest()
    {
        try
        {
            resetSiteSettings();
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to reset site look and settings: " + e);
        }

        try
        {
            resetProjectSettings();
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to reset project settings: " + e);
        }

        log("Verify no patters are set for the project.");
        verifyNoPatternsSet(ProjectSettingsPage.beginAt(this, getProjectName()));

        log("Verify no patters are set for the site.");
        verifyNoPatternsSet(LookAndFeelSettingsPage.beginAt(this));

    }

    private void resetSiteSettings() throws IOException, CommandException
    {
        log("Reset site settings.");
        resetSettings("/");
    }

    private void resetProjectSettings() throws IOException, CommandException
    {
        log("Reset project settings.");
        resetSettings(getProjectName());
    }

    private void resetSettings(String path) throws IOException, CommandException
    {
        BaseSettingsPage.resetSettings(createDefaultConnection(), path);
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

        createList(TEST_PARSING);
        log("Setting the additional parsing patterns for the site.");
        testParsingPatternsList(true);

        log("Validate that parsing patterns for the site are shown in the project config.");
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this, getProjectName());

        checker().verifyEquals("Parsing pattern for the DateTime field should show site level value.",
                DATE_TIME_PATTERN, projectSettingsPage.getAdditionalParsingPatternDateAndTime());

        checker().verifyEquals("Parsing pattern for the Date field should show site level value.",
                DATE_PATTERN, projectSettingsPage.getAdditionalParsingPatternDates());

        checker().verifyEquals("Parsing pattern for the Time field should show site level value.",
                TIME_PATTERN, projectSettingsPage.getAdditionalParsingPatternTimes());

    }

    @Test
    public void testProjectAdditionalParsingPatternDateAndTime() throws IOException, CommandException
    {
        createList(TEST_PARSING);
        log("Set the additional parsing patterns for the project.");
        testParsingPatternsList(false);
    }

    private void importBulkNonUSDate(String bulkData)
    {

        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_MODE));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());
        listTable.clickImportBulkData()
                .setText(bulkData);
        clickButton("Submit");

    }

    private void verifyImportedNonUSDate(List<String> expectedDateTimeCol, List<String> expectedDateCol, List<String> expectedTimeCol)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_MODE));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());

        checker().verifyEquals("Values in " + COL_DATETIME + " are not as expected.",
                expectedDateTimeCol, listTable.getColumnDataAsText(COL_DATETIME));

        checker().verifyEquals("Values in " + COL_DATE + " are not as expected.",
                expectedDateCol, listTable.getColumnDataAsText(COL_DATE));

        checker().verifyEquals("Values in " + COL_TIME + " are not as expected.",
                expectedTimeCol, listTable.getColumnDataAsText(COL_TIME));

        checker().screenShotIfNewError("Non_US_Mode_Error");

        listTable.checkAllOnPage();
        listTable.deleteSelectedRows();
    }

    @Test
    public void testNonUSDateParsingMode() throws IOException, CommandException
    {
        final String bulkData = String.format("%s\t%s\t%s\t%s\n", COL_NAME, COL_DATETIME, COL_DATE, COL_TIME)
                + "A\t23/12/24 14:45\t23/12/24\t14:45\n"
                + "B\t19/11/99 9:32:06.001\t19/11/99\t9:32:06.001\n"
                + "C\t2/3/1972 10:45 pm\t2/3/1972\t10:45 pm\n"
                + "D\t3-2-05 00:00\t3-2-05\t00:00\n"
                + "E\t19July1999 19:32:06\t19/07/99\t19:32:06\n";

        createList(TEST_MODE);

        log("Use 'Non-U.S. date parsing (DMY)'.");
        setSiteAdditionalParsingPatterns(null, null, null, false);

        List<String> expectedDateTimeCol = List.of("2024-12-23 14:45", "1999-11-19 09:32", "1972-03-02 22:45", "2005-02-03 00:00", "1999-07-19 19:32");
        List<String> expectedDateCol = List.of("2024-12-23", "1999-11-19", "1972-03-02", "2005-02-03", "1999-07-19");
        List<String> expectedTimeCol = List.of("14:45:00", "09:32:06", "22:45:00", "00:00:00", "19:32:06");

        importBulkNonUSDate(bulkData);
        verifyImportedNonUSDate(expectedDateTimeCol, expectedDateCol, expectedTimeCol);

        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this, getProjectName());
        projectSettingsPage.setDefaultDateDisplay("MM-dd-yyyy");
        projectSettingsPage.save();
        // Issue 50420: LKS/LKSM: Non US parsing doesn't seem to be respected: US parsing setting should be queried at Root folder level
        log("Change Project Settings - Default display format for dates");
        expectedDateCol = List.of("12-23-2024", "11-19-1999", "03-02-1972", "02-03-2005", "07-19-1999");

        importBulkNonUSDate(bulkData);
        verifyImportedNonUSDate(expectedDateTimeCol, expectedDateCol, expectedTimeCol);
    }

    private void createList(String listName) throws IOException, CommandException
    {
        // Delete the list if it already exists.
        if(DomainUtils.doesDomainExist(getProjectName(), LIST_SCHEMA, listName))
        {
            DomainUtils.deleteDomain(getProjectName(), LIST_SCHEMA, listName);
        }

        goToProjectHome();
        new IntListDefinition(listName, "id")
                .setFields(List.of(
                        new FieldDefinition(COL_NAME, FieldDefinition.ColumnType.String),
                        new FieldDefinition(COL_DATETIME, FieldDefinition.ColumnType.DateAndTime),
                        new FieldDefinition(COL_DATE, FieldDefinition.ColumnType.Date),
                        new FieldDefinition(COL_TIME, FieldDefinition.ColumnType.Time)))
                .create(createDefaultConnection(), getProjectName());

    }

    private void verifyNoPatternsSet(BaseSettingsPage settingsPage)
    {
        checker().fatal()
                .verifyTrue("No additional parsing pattern should be set for the DateTime field.",
                        settingsPage.getAdditionalParsingPatternDateAndTime().isEmpty());

        checker().fatal()
                .verifyTrue("No additional parsing pattern should be set for the Date field.",
                        settingsPage.getAdditionalParsingPatternDates().isEmpty());

        checker().fatal()
                .verifyTrue("No additional parsing pattern should be set for the Time field.",
                        settingsPage.getAdditionalParsingPatternTimes().isEmpty());

    }

    private void testParsingPatternsList(boolean changeSiteSettings) throws IOException, CommandException
    {

        // Pre-populate the list with one item that will be updated.
        TestDataGenerator dataGenerator = new TestDataGenerator(LIST_SCHEMA, TEST_PARSING, getProjectName());
        dataGenerator.addCustomRow(Map.of(COL_NAME, "First", COL_DATETIME, "05/10/2020", COL_DATE, "02/05/2024", COL_TIME, "16:43:32"));
        dataGenerator.insertRows(createDefaultConnection(), dataGenerator.getRows());

        if(changeSiteSettings)
        {
            setSiteAdditionalParsingPatterns(DATE_TIME_PATTERN, DATE_PATTERN, TIME_PATTERN, true);
        }
        else
        {
            setProjectAdditionalParsingPatterns(DATE_TIME_PATTERN, DATE_PATTERN, TIME_PATTERN);
        }

        List<Map<String, String>> expectedTableValues = new ArrayList<>();

        log("Update a row with values in the additional parsing format.");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_PARSING));
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
        log("Import a xlsx file where date & time values are numbers.");
        listTable.clickImportBulkData()
                .setFile(TestFileUtils.getSampleData("DateParsing/BulkImportDateParsing.xlsx"));
        clickButton("Submit");

        log("Import a xlsx file where date & time values are text.");
        listTable.clickImportBulkData()
                .setFile(TestFileUtils.getSampleData("DateParsing/BulkImportDateParsing_Text.xlsx"));
        clickButton("Submit");

        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_PARSING));
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

        if(changeSiteSettings)
        {
            setSiteAdditionalParsingPatterns(DATE_TIME_PATTERN, DATE_PATTERN, TIME_PATTERN, true);
        }
        else
        {
            setProjectAdditionalParsingPatterns(DATE_TIME_PATTERN, DATE_PATTERN, TIME_PATTERN);
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

    private void setSiteAdditionalParsingPatterns(String dateTimePattern, String datePattern, String timePattern, boolean useUSMode)
    {
        LookAndFeelSettingsPage lookAndFeelSettingsPage = LookAndFeelSettingsPage.beginAt(this);

        lookAndFeelSettingsPage.setDateParsingMode(useUSMode);

        setAdditionalParsingPatterns(lookAndFeelSettingsPage, dateTimePattern, datePattern, timePattern);
    }

    private void setProjectAdditionalParsingPatterns(String dateTimePattern, String datePattern, String timePattern)
    {
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this, getProjectName());
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
