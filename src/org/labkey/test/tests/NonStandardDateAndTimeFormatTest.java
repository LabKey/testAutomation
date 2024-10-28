package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
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
import org.labkey.test.pages.core.admin.BaseSettingsPage.DATE_FORMAT;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.pages.experiment.CreateDataClassPage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.URLBuilder;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
public class NonStandardDateAndTimeFormatTest extends BaseWebDriverTest
{

    private static final String PROJECT_NAME = "Non-Standard Date And Time Formats Test";

    // No standard Date, Time and DateTime formats set at the project level.
    private static final String PROJECT_DATE_FORMAT = "dd/MM/yy";
    private static final String PROJECT_TIME_FORMAT = "K:m:s a Z";
    private static final String PROJECT_DATETIME_FORMAT = "EEEE MMMM dd D yyyy k:mm:s X";

    // Warning shown on the Validation page for project settings.
    private static final List<String> PROJECT_WARNINGS = List.of(String.format("Project default display format for Dates: %s", PROJECT_DATE_FORMAT),
            String.format("Project default display format for DateTimes: %s", PROJECT_DATETIME_FORMAT),
            String.format("Project default display format for Times: %s", PROJECT_TIME_FORMAT));

    // Tooltips shown on the various designer pages for non-standard formats.
    private static final String TT_NS_DATE = "Non-standard date format.";
    private static final String TT_NS_TIME = "Non-standard time format.";
    private static final String TT_NS_DATETIME = "Non-standard date-time format.";

    private final PortalHelper _portalHelper = new PortalHelper(this);

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

        // Use the API to set non-standard formats for the project.
        new APIContainerHelper(this)
                .setDateAndTimeFormats(createDefaultConnection(), PROJECT_NAME,
                        PROJECT_DATE_FORMAT, PROJECT_TIME_FORMAT, PROJECT_DATETIME_FORMAT);

        _portalHelper.addWebPart("Lists");
        _portalHelper.addWebPart("Data Classes");
        _portalHelper.addWebPart("Assay List");

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

    /**
     * <p>
     *     Test non-standard Date, Time and DateTime format settings at the project level. The non-standard formats are
     *     set during project creation.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate that the Date, Time and DateTime fields are not inherited.</li>
     *         <li>Validate the formats are as expected (non-standard)</li>
     *         <li>Validate the tooltip message.</li>
     *         <li>Validate the Site Validate report show the project settings.</li>
     *     </ul>
     * </p>
     */
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

        log("Check that Site Validation includes warnings from the project settings.");
        checkSiteValidation(PROJECT_WARNINGS, true);

    }

    /**
     * <p>
     *     Test non-standard Date, Time and DateTime formats with lists.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a list that has non-standard formats set for the fields.</li>
     *         <li>Create a second list that has fields that inherit from the project settings.</li>
     *         <li>Import data into the list to validate non-standard formats are used when display the data.</li>
     *         <li>Validate the designer for fields that do not inherit from the project.</li>
     *         <li>Validate the designer for fields that are inherited from the project (should see no warnings)</li>
     *         <li>Check Site Validation report calls out the fields not inherited, but not the inherited fields.</li>
     *     </ul>
     * </p>
     *
     * @throws IOException Can be thrown by the API creation calls.
     * @throws CommandException Can be thrown by the API creation calls.
     */
    @Test
    public void testLists() throws IOException, CommandException
    {

        String listFormat = "List Non-Standard Formats";
        String listInherit = "List Inherit Project Formats";
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

        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listFormat);
        DomainFormPanel domainEditor = listDefinitionPage.getFieldsPanel();

        checkField(domainEditor, dateCol01, false, FieldDefinition.ColumnType.Date,
                nsDateFormat01, TT_NS_DATE);
        checkField(domainEditor, dateCol02, false, FieldDefinition.ColumnType.Date,
                nsDateFormat02, TT_NS_DATE);
        checkField(domainEditor, timeCol01, false, FieldDefinition.ColumnType.Time,
                nsTimeFormat01, TT_NS_TIME);
        checkField(domainEditor, timeCol02, false, FieldDefinition.ColumnType.Time,
                nsTimeFormat02, TT_NS_TIME);
        checkField(domainEditor, dateTimeCol01, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsDateFormat01, nsTimeFormat01), TT_NS_DATETIME);
        checkField(domainEditor, dateTimeCol02, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsTimeFormat02, nsDateFormat02), TT_NS_DATETIME);

        listDefinitionPage.clickCancel();
        DataRegionTable table = new DataRegionTable("query", getDriver());
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

        listDefinitionPage =  _listHelper.goToEditDesign(listInherit);
        domainEditor = listDefinitionPage.getFieldsPanel();

        checkField(domainEditor, dateCol01, true, FieldDefinition.ColumnType.Date,
                PROJECT_DATE_FORMAT, TT_NS_DATE);
        checkField(domainEditor, timeCol01, true, FieldDefinition.ColumnType.Time,
                PROJECT_TIME_FORMAT, TT_NS_TIME);
        checkField(domainEditor, dateTimeCol01, true, FieldDefinition.ColumnType.DateAndTime,
                PROJECT_DATETIME_FORMAT, TT_NS_DATETIME);

        listDefinitionPage.clickCancel();
        table = new DataRegionTable("query", getDriver());
        actualRowValues = table.getRowDataAsMap(0);

        checker().verifyEquals("Data in grid is not formatted as expected.",
                expectedRowValues, actualRowValues);

        log("Validate that the Site Validation contains the expected warnings.");

        List<String> expectedWarnings = List.of(String.format("Date property \"lists.%s.%s\": %s",
                        listFormat, dateCol01, nsDateFormat01),
                String.format("Date property \"lists.%s.%s\": %s",
                        listFormat, dateCol02, nsDateFormat02),
                String.format("Time property \"lists.%s.%s\": %s",
                        listFormat, timeCol01, nsTimeFormat01),
                String.format("Time property \"lists.%s.%s\": %s",
                        listFormat, timeCol02, nsTimeFormat02),
                String.format("DateTime property \"lists.%s.%s\": %s %s",
                        listFormat, dateTimeCol01, nsDateFormat01, nsTimeFormat01),
                String.format("DateTime property \"lists.%s.%s\": %s %s",
                        listFormat, dateTimeCol02, nsTimeFormat02, nsDateFormat02)
        );

        checkSiteValidation(expectedWarnings, true);

        log("Validate that Site Validation does not contain warnings about inherited fields.");

        expectedWarnings = List.of(String.format("Date property \"lists.%s.%s\": %s",
                        listInherit, dateCol01, nsDateFormat01),
                String.format("Time property \"lists.%s.%s\": %s",
                        listInherit, timeCol01, nsTimeFormat01),
                String.format("DateTime property \"lists.%s.%s\": %s %s",
                        listInherit, dateTimeCol01, nsDateFormat01, nsTimeFormat01)
        );

        checkSiteValidation(expectedWarnings, false);

    }

    /**
     * <p>
     *     Validate editing Date, Time and DateTime fields with non-standard formats.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a list with a non-standard format.</li>
     *         <li>Validate the list and fields are called out in the site validation.</li>
     *         <li>Edit the various fields and change the formats to a standard format.</li>
     *         <li>Cancel the edit and validate the at the non-standard format is retained.</li>
     *         <li>Edit again and change the fields to a standard format.</li>
     *         <li>Validate the list and fields are no longer called out in the Site Validation report.</li>
     *     </ul>
     * </p>
     *
     * @throws IOException Can be thrown by the API creation calls.
     * @throws CommandException Can be thrown by the API creation calls.
     */
    @Test
    public void testEdit() throws IOException, CommandException
    {
        String listEdit = "List Edit Non-Standard Formats";
        String dateCol = "Date";
        String timeCol = "Time";
        String dateTimeCol = "DateTime";

        log(String.format("Create a list named '%s' with various Date, Time and DateTime columns.", listEdit));

        String nsDateFormat = "MMMM dd, yyyy";
        String nsTimeFormat = "hh:mm:ss.ss a";

        List<FieldDefinition> listFields = List.of(
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date).setFormat(nsDateFormat),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time).setFormat(nsTimeFormat),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime).setFormat(String.format("%s %s", nsDateFormat, nsTimeFormat))
        );

        createListByAPI(listEdit, listFields);

        log("Check that Site Validation tags the fields in the list.");

        List<String> expectedWarnings = List.of(String.format("Date property \"lists.%s.%s\": %s",
                        listEdit, dateCol, nsDateFormat),
                String.format("Time property \"lists.%s.%s\": %s",
                        listEdit, timeCol, nsTimeFormat),
                String.format("DateTime property \"lists.%s.%s\": %s %s",
                        listEdit, dateTimeCol, nsDateFormat, nsTimeFormat));

        checkSiteValidation(expectedWarnings, true);

        goToProjectHome();
        _listHelper.goToList(listEdit);
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listEdit);

        log("Edit the Date, Time and DateTime fields but then cancel out of the edit.");

        DomainFormPanel domainEditor = listDefinitionPage.getFieldsPanel();
        DomainFieldRow fieldRow = domainEditor.getField(dateCol);
        fieldRow.expand();
        fieldRow.setDateFormat(DATE_FORMAT.yyyy_MM_dd);
        checker().verifyFalse(String.format("Changing '%s' to a standard format should remove the warning icon.", dateCol),
                fieldRow.hasDomainWarningIcon());

        fieldRow = domainEditor.getField(timeCol);
        fieldRow.expand();
        fieldRow.setTimeFormat(TIME_FORMAT.HH_mm_ss);
        checker().verifyFalse(String.format("Changing '%s' to a standard format should remove the warning icon.", timeCol),
                fieldRow.hasDomainWarningIcon());

        fieldRow = domainEditor.getField(dateTimeCol);
        fieldRow.expand();
        fieldRow.setDateTimeFormat(DATE_FORMAT.yyyy_MM_dd, TIME_FORMAT.hh_mm_a);
        checker().verifyFalse(String.format("Changing '%s' to a standard format should remove the warning icon.", dateTimeCol),
                fieldRow.hasDomainWarningIcon());

        log("Cancel out of the edit.");
        listDefinitionPage.clickCancel();

        log("Go back to the design page and validate that the non-standard formats are still there.");
        listDefinitionPage = _listHelper.goToEditDesign(listEdit);
        domainEditor = listDefinitionPage.getFieldsPanel();

        checkField(domainEditor, dateCol, false, FieldDefinition.ColumnType.Date,
                nsDateFormat, TT_NS_DATE);
        checkField(domainEditor, timeCol, false, FieldDefinition.ColumnType.Time,
                nsTimeFormat, TT_NS_TIME);
        checkField(domainEditor, dateTimeCol, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsDateFormat, nsTimeFormat), TT_NS_DATETIME);

        listDefinitionPage.clickCancel();

        log("Now edit the field to a standard format.");
        listDefinitionPage = _listHelper.goToEditDesign(listEdit);

        domainEditor = listDefinitionPage.getFieldsPanel();
        fieldRow = domainEditor.getField(dateCol);
        fieldRow.expand();
        fieldRow.setDateFormat(DATE_FORMAT.yyyy_MM_dd);
        fieldRow = domainEditor.getField(timeCol);
        fieldRow.expand();
        fieldRow.setTimeFormat(TIME_FORMAT.HH_mm_ss);
        fieldRow = domainEditor.getField(dateTimeCol);
        fieldRow.expand();
        fieldRow.setDateTimeFormat(DATE_FORMAT.yyyy_MM_dd, TIME_FORMAT.hh_mm_a);

        listDefinitionPage.clickSave();

        listDefinitionPage = _listHelper.goToEditDesign(listEdit);
        domainEditor = listDefinitionPage.getFieldsPanel();

        checkField(domainEditor, dateCol, false, FieldDefinition.ColumnType.Date,
                DATE_FORMAT.yyyy_MM_dd.toString(), null);
        checkField(domainEditor, timeCol, false, FieldDefinition.ColumnType.Time,
                TIME_FORMAT.HH_mm_ss.toString(), null);
        checkField(domainEditor, dateTimeCol, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", DATE_FORMAT.yyyy_MM_dd, TIME_FORMAT.hh_mm_a), null);

        log("Check that Site Validation Does not tag the fields in the list after they have been edited.");

        checkSiteValidation(expectedWarnings, false);

    }

    // Private helper that will create a list through the APIs. This allows the list to have non-standard formats.
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

    /**
     * <p>
     *     Validate non-standard Date, Time and DateTime formats in a DataClass
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a DataClass with non-standard formats.</li>
     *         <li>Create a DataClass that inherits non-standard formats from the project settings.</li>
     *         <li>Add some data to both as a sanity validation that non-standard formats are used.</li>
     *         <li>Validate the designer scenarios for both DataClasses.</li>
     *     </ul>
     * </p>
     *
     * @throws IOException Can be thrown by the API creation calls.
     * @throws CommandException Can be thrown by the API creation calls.
     */
    @Test
    public void testDataClass() throws IOException, CommandException
    {
        String dcFormat = "DC Non-Standard Formats";
        String dcInherit = "DC Inherit Project Formats";
        String dateCol = "Date";
        String timeCol = "Time";
        String dateTimeCol = "DateTime";

        String nsDateFormat = "MMMM dd, yyyy";
        String nsTimeFormat = "hh:mm:ss.ss a";

        List<FieldDefinition> fields = List.of(
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date).setFormat(nsDateFormat),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time).setFormat(nsTimeFormat),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime).setFormat(String.format("%s %s", nsDateFormat, nsTimeFormat))
        );

        log(String.format("Create a Data Class named '%s' with non-standard format fields.", dcFormat));

        DataClassDefinition dataClass = new DataClassDefinition(dcFormat);
        for(FieldDefinition field : fields)
        {
            dataClass.addField(field);
        }

        dataClass.create(createDefaultConnection(), getProjectName());

        fields = List.of(
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime)
        );

        log(String.format("Create a Data Class named '%s' that inherits non-standard format fields from the project.", dcFormat));

        dataClass = new DataClassDefinition(dcInherit);
        for(FieldDefinition field : fields)
        {
            dataClass.addField(field);
        }

        dataClass.create(createDefaultConnection(), getProjectName());

        log("Add some data to both data classes as a sanity validation of the formats.");

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dcFormat));

        String bulkData = String.format("%s\t%s\t%s\t%s\n", "Name", dateTimeCol, dateCol, timeCol)
                + "A\t12/23/24 14:45\t12/23/24\t14:45\n";

        Map<String, String> expectedFormatData = Map.of("Name", "A",
                dateTimeCol, "December 23, 2024 02:45:00.00 PM",
                dateCol, "December 23, 2024",
                timeCol, "02:45:00.00 PM", "Flag", "");

        Map<String, String> expectedInheritedData = Map.of("Name", "A",
                dateTimeCol, "Monday December 23 358 2024 14:45:0 -08",
                dateCol, "23/12/24",
                timeCol, "2:45:0 PM -0800", "Flag", "");

        DataRegionTable dataTable = new DataRegionTable("query", getDriver());
        dataTable.clickImportBulkData()
                .setText(bulkData);
        clickButton("Submit");

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dcInherit));

        dataTable = new DataRegionTable("query", getDriver());
        dataTable.clickImportBulkData()
                .setText(bulkData);
        clickButton("Submit");

        log(String.format("Validate domain designer feedback for '%s'.", dcFormat));

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dcFormat));
        clickAndWait(Locator.lkButton("Edit Data Class"));
        CreateDataClassPage editPage = new CreateDataClassPage(getDriver());
        DomainFormPanel domainEditor = editPage.getDomainEditor();

        checkField(domainEditor, dateCol,
        false, FieldDefinition.ColumnType.Date,
                nsDateFormat, TT_NS_DATE);

        checkField(domainEditor, timeCol,
                false, FieldDefinition.ColumnType.Time,
                nsTimeFormat, TT_NS_TIME);

        checkField(domainEditor, dateTimeCol,
                false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsDateFormat, nsTimeFormat), TT_NS_DATETIME);

        editPage.clickCancel();

        log("Validate the data (make sure we still respect non-standard formats).");

        dataTable = new DataRegionTable("query", getDriver());
        Map<String, String> actualData = dataTable.getRowDataAsMap(0);
        checker().verifyEquals("Data with formatted fields is not as expected.",
                expectedFormatData, actualData);

        log("Validate that data sets are reported in Site Validation.");
        List<String> expectedWarnings = List.of(String.format("Date property \"exp.data.%s.%s\": %s",
                        dcFormat, dateCol, nsDateFormat),
                String.format("Time property \"exp.data.%s.%s\": %s",
                        dcFormat, timeCol, nsTimeFormat),
                String.format("DateTime property \"exp.data.%s.%s\": %s %s",
                        dcFormat, dateTimeCol, nsDateFormat, nsTimeFormat));

        checkSiteValidation(expectedWarnings, true);

        log(String.format("Validate domain designer feedback for '%s'.", dcInherit));

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dcInherit));
        clickAndWait(Locator.lkButton("Edit Data Class"));
        editPage = new CreateDataClassPage(getDriver());
        domainEditor = editPage.getDomainEditor();

        checkField(domainEditor, dateCol,
                false, FieldDefinition.ColumnType.Date,
                PROJECT_DATE_FORMAT, TT_NS_DATE);

        checkField(domainEditor, timeCol,
                false, FieldDefinition.ColumnType.Time,
                PROJECT_TIME_FORMAT, TT_NS_TIME);

        checkField(domainEditor, dateTimeCol,
                false, FieldDefinition.ColumnType.DateAndTime,
                PROJECT_DATETIME_FORMAT, TT_NS_DATETIME);

        editPage.clickCancel();

        log("Validate the data (make sure we still respect non-standard formats).");
        dataTable = new DataRegionTable("query", getDriver());
        actualData = dataTable.getRowDataAsMap(0);
        checker().verifyEquals("Data with inherited fields is not as expected.",
                expectedInheritedData, actualData);

    }

    // Private helper that validates Date, Time and DateTime fields in a domain designer.
    // isInherited: Used to check the enabled / editable state of the field.
    // columnType: Used to identify the expected field options and messages.
    // expectedToolTipText: Used as a check for the warning icon. If null no warning icon is expected.
    private void checkField(DomainFormPanel domainEditor, String fieldName,
                            boolean isInherited, FieldDefinition.ColumnType columnType,
                            String expectedFormat, @Nullable String expectedToolTipText)
    {
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

            String expectedDateFormat;
            String expectedTimeFormat;

            // If expected tooltip text is not null treat the field as a non-standard format field.
            if (null != expectedToolTipText)
            {
                expectedDateFormat = expectedFormat;
                expectedTimeFormat = TIME_FORMAT.none.toString();
            }
            else
            {
                // None of the standard dates have a space. The first space will be between the date format and the time format.
                int index = expectedFormat.indexOf(" ");

                expectedDateFormat = expectedFormat.substring(0, index);
                expectedTimeFormat = expectedFormat.substring(index+1);
            }

            actualFormat = fieldRow.getDateTimeFormatDate();
            checker().verifyEquals(String.format("DateTime Date format for field '%s' not as expected.", fieldName),
                    expectedDateFormat, actualFormat);

            actualFormat = fieldRow.getDateTimeFormatTime();
            checker().verifyEquals(String.format("DateTime Time format for field '%s' not as expected.", fieldName),
                    expectedTimeFormat, actualFormat);
        }

        // If expected tooltip text is not null treat the field as a non-standard format field.
        if (null != expectedToolTipText)
        {
            if (checker().verifyTrue("No warning icon present for field with non-standard date-time format.",
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
        }
        else
        {
            checker().verifyFalse(String.format("Field '%s' should not have a warning icon present, but one is there.", fieldName),
                    fieldRow.hasDomainWarningIcon());
        }
        checker().screenShotIfNewError(String.format("Non_Standard_Field_%s_Error", fieldName));

    }

    // Private helper to check the Site Validation report. Checks to see if the report should, or should not, contain
    // the list of warnings.
    private void checkSiteValidation(List<String> expectedWarnings, boolean shouldContain)
    {
        List<String> actualWarnings = getProjectValidationWarnings();

        log("Found Warnings: " + actualWarnings);

        if (shouldContain)
        {
            log("Should Contain Warnings: " + expectedWarnings);

            if (!actualWarnings.isEmpty())
            {
                checker().verifyTrue("Did not find the expected warnings.",
                        actualWarnings.containsAll(expectedWarnings));

            }
            else
            {
                log(String.format("No warnings found for project '%s'.", getProjectName()));
            }
        }
        else
        {
            log("Should Not Contain Warnings: " + expectedWarnings);

            boolean shouldBeFalse = actualWarnings.stream()
                    .anyMatch(expectedWarnings::contains);

            checker().verifyFalse("Found warning that should not be there.",
                    shouldBeFalse);
        }
    }

    private List<String> getProjectValidationWarnings()
    {
        List<String> warnings = new ArrayList<>();

        URLBuilder urlBuilder = new URLBuilder("admin", "configureSiteValidation");
        String url = urlBuilder.buildRelativeURL();
        beginAt(url);

        waitForElementToBeVisible(Locator.lkButton("Validate"));

        // Disable all validators.
        Locator.tagWithAttribute("input", "type", "checkbox")
                .findElements(getDriver()).forEach(this::uncheckCheckbox);

        // Enable Display Format validator.
        WebElement formatValidation = Locator.checkboxByNameAndValue("providers", "Display Format Validator").findWhenNeeded(getDriver());
        checkCheckbox(formatValidation);

        // Validate projects and sub-folders.
        checkRadioButton(Locator.radioButtonByNameAndValue("includeSubfolders", "true"));

        // Don't run in the background.
        uncheckCheckbox(Locator.id("background"));

        clickAndWait(Locator.lkButton("Validate"));

        waitForText("Folder Validation Results");

        String xpath = String.format("//li[contains(text(),'Project: %s')]//li[contains(text(),'Warnings:')]//ul", getProjectName());
        WebElement ul = Locator.xpath(xpath).findWhenNeeded(getDriver());

        int linkTextLength = " more info".length();

        if (ul.isDisplayed())
        {
            List<String> actualWarnings = ul.findElements(Locator.tag("li")).stream().map(WebElement::getText).toList();

            for(String warning : actualWarnings)
            {
                // Trim off the MORE INFO link text.
                String temp = warning.trim().substring(0, warning.length() - linkTextLength);
                warnings.add(temp);
            }
        }

        return warnings;
    }

}
