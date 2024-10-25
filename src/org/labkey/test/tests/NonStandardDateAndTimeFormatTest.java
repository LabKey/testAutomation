package org.labkey.test.tests;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.core.admin.BaseSettingsPage.TIME_FORMAT;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
public class NonStandardDateAndTimeFormatTest extends BaseWebDriverTest
{

    private static final String PROJECT_NAME = "Non-Standard Date And Time Formats Test";

    private static final String PROJECT_DATE_FORMAT = "dd/MM/yy";
    private static final String PROJECT_TIME_FORMAT = "K:m:s a Z";
    private static final String PROJECT_DATETIME_FORMAT = "EEEE MMMM dd D yyyy k:mm:s X";

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
        NonStandardDateAndTimeFormatTest init = (NonStandardDateAndTimeFormatTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        resetSiteSettings();
        _containerHelper.createProject(PROJECT_NAME, null);
        goToProjectHome();

        new APIContainerHelper(this)
                .setDateAndTimeFormats(createDefaultConnection(), PROJECT_NAME,
                        PROJECT_DATE_FORMAT, PROJECT_TIME_FORMAT, PROJECT_DATETIME_FORMAT);
    }

    @AfterClass
    public static void afterClass() throws IOException, CommandException
    {
        ((NonStandardDateAndTimeFormatTest) getCurrentTest()).resetSiteSettings();
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

    @Test
    public void testProjectSettingsPage()
    {
        goToProjectHome();
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this);

        log("Check that the Date, Time and DateTime format fields are not inherited.");

        checker().verifyFalse("Date format field should not be shown as inherited.",
                projectSettingsPage.getDefaultDateDisplayInherited());

        checker().verifyFalse("Time format field should not be shown as inherited.",
                projectSettingsPage.getDefaultTimeDisplayInherited());

        checker().verifyFalse("DateTime format field should not be shown as inherited.",
                projectSettingsPage.getDefaultDateDisplayInherited());

        log("Check that the values in the fields are as expected.");

        checker().verifyEquals("Format of Default Date is not as expected.",
                PROJECT_DATE_FORMAT, projectSettingsPage.getDefaultDateDisplay());

        checker().verifyTrue("Default Date field should have a non-standard warning, it does not.",
                projectSettingsPage.defaultDateDisplayWarning());

        checker().verifyEquals("Format of Default Time is not as expected.",
                PROJECT_TIME_FORMAT, projectSettingsPage.getDefaultTimeDisplay());

        checker().verifyTrue("Default Time field should have a non-standard warning, it does not.",
                projectSettingsPage.defaultTimeDisplayWarning());

        checker().verifyEquals("Format of Default DateTime (Date) is not as expected.",
                PROJECT_DATETIME_FORMAT, projectSettingsPage.getDefaultDateTimeDateDisplay());

        checker().verifyTrue("Default DateTime Date field should have a non-standard warning, it does not.",
                projectSettingsPage.defaultDateTimeDateDisplayWarning());

        checker().verifyEquals("Format of Default DateTime (Time) is not as expected.",
                "", projectSettingsPage.getDefaultDateTimeTimeDisplay());

        checker().verifyFalse("Default DateTime Time field should not have a non-standard warning, it does.",
                projectSettingsPage.defaultDateTimeTimeDisplayWarning());

    }

    @Test
    public void testLists() throws IOException, CommandException
    {

        String listFormat = "Non-Standard Formats";
        String listInherit = "Inherit Project Formats";
        String dateCol01 = "Date01";
        String dateCol02 = "Date02";
        String timeCol01 = "Time01";
        String timeCol02 = "Time02";
        String dateTimeCol01 = "DateTime01";
        String dateTimeCol02 = "DateTime02";

        log(String.format("Create a list named '%s' with various Date, Time and DateTime columns.", listFormat));

        String nsDateFormat01 = "MMMM dd, yyyy";
        String nsDateFormat02 = "yyyy-w G";
        String nsTimeFormat01 = "hh:mm:ss.ss a";
        String nsTimeFormat02 = "K:mm a z";

        List<FieldDefinition> listFields = List.of(
                new FieldDefinition(dateCol01, FieldDefinition.ColumnType.Date).setFormat(nsDateFormat01),
                new FieldDefinition(dateCol02, FieldDefinition.ColumnType.Date).setFormat(nsDateFormat02),
                new FieldDefinition(timeCol01, FieldDefinition.ColumnType.Time).setFormat(nsTimeFormat01),
                new FieldDefinition(timeCol02, FieldDefinition.ColumnType.Time).setFormat(nsTimeFormat02),
                new FieldDefinition(dateTimeCol01, FieldDefinition.ColumnType.DateAndTime).setFormat(String.format("%s %s", nsDateFormat01, nsTimeFormat01)),
                new FieldDefinition(dateTimeCol02, FieldDefinition.ColumnType.DateAndTime).setFormat(String.format("%s %s", nsTimeFormat02, nsDateFormat02))
        );

        createListByAPI(listFormat, listFields);

        log(String.format("Create a second list named '%s' that inherits formats from the project.", listInherit));

        listFields = List.of(
                new FieldDefinition(dateCol01, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol01, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol01, FieldDefinition.ColumnType.DateAndTime)
        );

        createListByAPI(listInherit, listFields);

        log(String.format("Validate the design and data of list '%s' (does not inherit formats).", listFormat));
        goToProjectHome();
        _listHelper.goToList(listFormat);
        _listHelper.insertNewRow(Map.of(dateCol01, "4/1/21",
                dateCol02, "12/25/19",
                timeCol01, "12:32 am",
                timeCol02, "14:42",
                dateTimeCol01, "May 1, 2005 6:32 pm",
                dateTimeCol02, "6/1/24 10:00"), false);

        Map<String, String> expectedRowValues = Map.of(dateCol01, "April 01, 2021",
                dateCol02, "2019-52 AD",
                timeCol01, "12:32:00.00 AM",
                timeCol02, "2:42 PM PST",
                dateTimeCol01, "May 01, 2005 06:32:00.00 PM",
                dateTimeCol02, "10:00 AM PDT 2024-22 AD");

        DataRegionTable table = new DataRegionTable("query", getDriver());

        clickAndWait(table.getHeaderButton("Design"));
        EditListDefinitionPage listDefinitionPage = new EditListDefinitionPage(getDriver());

        checkField(listDefinitionPage, dateCol01, false, FieldDefinition.ColumnType.Date,
                nsDateFormat01, "Non-standard date format.");
        checkField(listDefinitionPage, dateCol02, false, FieldDefinition.ColumnType.Date,
                nsDateFormat02, "Non-standard date format.");
        checkField(listDefinitionPage, timeCol01, false, FieldDefinition.ColumnType.Time,
                nsTimeFormat01, "Non-standard time format.");
        checkField(listDefinitionPage, timeCol02, false, FieldDefinition.ColumnType.Time,
                nsTimeFormat02, "Non-standard time format.");
        checkField(listDefinitionPage, dateTimeCol01, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsDateFormat01, nsTimeFormat01), "Non-standard date-time format.");
        checkField(listDefinitionPage, dateTimeCol02, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsTimeFormat02, nsDateFormat02), "Non-standard date-time format.");

        listDefinitionPage.clickCancel();
        table = new DataRegionTable("query", getDriver());
        Map<String, String> actualRowValues = table.getRowDataAsMap(0);

        checker().verifyEquals("Data in grid is not formatted as expected.",
                expectedRowValues, actualRowValues);

        log(String.format("Validate the design and data of list '%s' (does inherit formats).", listInherit));
        goToProjectHome();
        _listHelper.goToList(listInherit);
        _listHelper.insertNewRow(Map.of(dateCol01, "4/1/21",
                timeCol01, "12:32 am",
                dateTimeCol01, "May 1, 2005 6:32 pm"), false);

        expectedRowValues = Map.of(dateCol01, "01/04/21",
                timeCol01, "0:32:0 AM -0800",
                dateTimeCol01, "Sunday May 01 121 2005 18:32:0 -07");

        table = new DataRegionTable("query", getDriver());

        clickAndWait(table.getHeaderButton("Design"));
        listDefinitionPage = new EditListDefinitionPage(getDriver());

        checkField(listDefinitionPage, dateCol01, true, FieldDefinition.ColumnType.Date,
                PROJECT_DATE_FORMAT, "Non-standard date format.");
        checkField(listDefinitionPage, timeCol01, true, FieldDefinition.ColumnType.Time,
                PROJECT_TIME_FORMAT, "Non-standard time format.");
        checkField(listDefinitionPage, dateTimeCol01, true, FieldDefinition.ColumnType.DateAndTime,
                PROJECT_DATETIME_FORMAT, "Non-standard date-time format.");

        listDefinitionPage.clickCancel();
        table = new DataRegionTable("query", getDriver());
        actualRowValues = table.getRowDataAsMap(0);

        checker().verifyEquals("Data in grid is not formatted as expected.",
                expectedRowValues, actualRowValues);
    }

    private void createListByAPI(String listName, List<FieldDefinition> fields) throws IOException, CommandException
    {
        Connection connection = createDefaultConnection();
        ListDefinition listDef = new IntListDefinition(listName, "Key");
        for(FieldDefinition field : fields)
        {
            listDef.addField(field);
        }

        listDef.create(connection, getProjectName());
    }

    private void checkField(EditListDefinitionPage listDefinitionPage, String fieldName,
                            boolean isInherited, FieldDefinition.ColumnType columnType,
                            String expectedFormat, String expectedToolTipText)
    {
        DomainFormPanel domainEditor = listDefinitionPage.getFieldsPanel();
        DomainFieldRow fieldRow = domainEditor.getField(fieldName);
        fieldRow.expand();

        String actualFormat;

        if (FieldDefinition.ColumnType.Date.equals(columnType))
        {
            if(isInherited)
            {
                checker().verifyTrue(String.format("Field '%s' should show as inherited, it does not.", fieldName),
                        fieldRow.isDateInherited());

                checker().verifyFalse(String.format("Field '%s' should not be enabled.", fieldName),
                        fieldRow.isDateFormatEnabled());
            }

            actualFormat = fieldRow.getDateFormat();
            checker().verifyEquals(String.format("Date format for field '%s' not as expected.", fieldName),
                    expectedFormat, actualFormat);
        }
        else if (FieldDefinition.ColumnType.Time.equals(columnType))
        {

            if(isInherited)
            {
                checker().verifyTrue(String.format("Field '%s' should show as inherited, it does not.", fieldName),
                        fieldRow.isTimeInherited());

                checker().verifyFalse(String.format("Field '%s' should not be enabled.", fieldName),
                        fieldRow.isTimeFormatEnabled());
            }

            actualFormat = fieldRow.getTimeFormat();
            checker().verifyEquals(String.format("Time format for field '%s' not as expected.", fieldName),
                    expectedFormat, actualFormat);
        }
        else
        {

            if(isInherited)
            {
                checker().verifyTrue(String.format("Field '%s' should show as inherited, it does not.", fieldName),
                        fieldRow.isDateTimeInherited());

                checker().verifyFalse(String.format("Date part of DateTime field '%s' should not be enabled.", fieldName),
                        fieldRow.isDateTimeFormatDateEnabled());

                checker().verifyFalse(String.format("Time part of DateTime field '%s' should not be enabled.", fieldName),
                        fieldRow.isDateTimeFormatTimeEnabled());
            }

            actualFormat = fieldRow.getDateTimeFormatDate();
            checker().verifyEquals(String.format("DateTime Date format for field '%s' not as expected.", fieldName),
                    expectedFormat, actualFormat);

            actualFormat = fieldRow.getDateTimeFormatTime();
            checker().verifyEquals(String.format("DateTime Time format for field '%s' not as expected.", fieldName),
                    TIME_FORMAT.none.toString(), actualFormat);
        }

        if(checker().verifyTrue("No warning icon present for field with non-standard date-time format.",
                fieldRow.hasDomainWarningIcon()))
        {
            WebElement icon = fieldRow.getDomainWarningIcon();
            mouseOver(icon);
            WebElement toolTip = Locator.tagWithClass("div", "tooltip-inner")
                    .withText(expectedToolTipText)
                    .findWhenNeeded(getDriver());

            checker().verifyTrue("Tooltip not present or text not as expected.",
                    waitFor(toolTip::isDisplayed, 1_000));
        }

        checker().screenShotIfNewError(String.format("Non_Standard_Field_%s_Error", fieldName));

    }

}
