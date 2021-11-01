package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
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
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class ParsingPatternForDateAndDateTimeTest extends BaseWebDriverTest
{
    private String dateList = "Sample List with Date column";

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        ParsingPatternForDateAndDateTimeTest init = (ParsingPatternForDateAndDateTimeTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), null);
        goToProjectHome();

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", dateList);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("dateCol", FieldDefinition.ColumnType.DateAndTime)));
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        dgen.addCustomRow(Map.of("name", "First", "dateCol", "05/10/2020"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "Parsing Pattern For Date And Date Time Test Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Test
    public void testAdditionalParsingPatternDateAndTime()
    {
        log("Setting the parsing pattern");
        String pattern = "ddMMMyyyy:HH:mm:ss";
        goToAdminConsole().clickLookAndFeelSettings();
        LookAndFeelSettingsPage lookAndFeelSettingsPage = new LookAndFeelSettingsPage(getDriver());
        lookAndFeelSettingsPage.setAdditionalParsingPatternDates(pattern);
        lookAndFeelSettingsPage.setAdditionalParingPatternDateAndTime(pattern);
        lookAndFeelSettingsPage.save();

        log("Update a row with a date in a non-standard format");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(dateList));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());
        listTable.clickEditRow(0);
        setFormElement(Locator.name("quf_dateCol"), "25Nov2020:24:23:00");
        clickButton("Submit");

        checker().verifyEquals("Incorrect date parsed while editing the row", "2020-11-26 00:23", listTable.getDataAsText(0, "dateCol"));

        log("Insert a row with a date in a non-standard format");
        listTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_name"), "Second");
        setFormElement(Locator.name("quf_dateCol"), "23Nov2020:12:23:34");
        clickButton("Submit");

        listTable.setFilter("name", "Equals", "Second");
        checker().verifyEquals("Incorrect date parsed while inserting row", "2020-11-23 12:23", listTable.getDataAsText(0, "dateCol"));

        log("Bulk import with some dates in non-standard format");
        listTable.clearAllFilters();
        listTable.clickImportBulkData();
        click(Locator.tagWithText("h3", "Upload file (.xlsx, .xls, .csv, .txt)"));
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("DateParsing/BulkImportDateParsing.xlsx"));
        clickButton("Submit");

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dateList));
        listTable = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Incorrect number of ros after bulk import", 6, listTable.getDataRowCount());
    }

}
