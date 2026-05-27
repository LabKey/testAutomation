/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests;

import org.apache.commons.lang3.SystemUtils;
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.admin.FolderFormatsPage;
import org.labkey.test.pages.admin.FolderManagementPage;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.core.admin.BaseSettingsPage.DATE_FORMAT;
import org.labkey.test.pages.core.admin.BaseSettingsPage.TIME_FORMAT;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
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
import org.labkey.test.util.WebServicesUtil;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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

    private static final TimeZone TIME_ZONE = TimeZone.getDefault();

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
        NonStandardDateAndTimeFormatTest init = getCurrentTest();
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

    }

    @AfterClass
    public static void afterClass() throws IOException, CommandException
    {
        ((NonStandardDateAndTimeFormatTest) getCurrentTest()).resetSiteSettings();
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {

        super.doCleanup(afterTest);

        try
        {
            ((NonStandardDateAndTimeFormatTest) getCurrentTest()).resetSiteSettings();
        }
        catch (IOException | CommandException rethrow)
        {
            throw new RuntimeException(rethrow);
        }
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

        log("Check that the Date, Time and DateTime format fields in the Project Settings page.");

        goToProjectHome();
        ProjectSettingsPage.beginAt(this);
        validateSettingsPage(false, PROJECT_DATE_FORMAT, true,
                false, PROJECT_TIME_FORMAT, true,
                false, PROJECT_DATETIME_FORMAT, "",
                true, false);

        log("Check that Site Validation includes warnings from the project settings.");
        String scope = String.format("Project: %s", getProjectName());
        validateSiteValidationReport(scope, PROJECT_WARNINGS, true);

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

        String nsDateFormat01 = "MMMM dd, yyyy"; // Similar to a standard format, except this has a comma after the dd.
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

        createListByAPI(getProjectName(), listFormat, listFields);

        log(String.format("Create a second list named '%s' that inherits formats from the project.", listInherit));

        listFields = List.of(
                new FieldDefinition(dateCol01, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol01, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol01, FieldDefinition.ColumnType.DateAndTime)
        );

        createListByAPI(getProjectName(), listInherit, listFields);

        log(String.format("Validate the design and data of list '%s' (does not inherit formats).", listFormat));
        goToProjectHome();
        _listHelper.goToList(listFormat);
        _listHelper.insertNewRow(Map.of(dateCol01, "4/1/21",
                dateCol02, "12/25/19",
                timeCol01, "12:32 am",
                timeCol02, "14:42",
                dateTimeCol01, "May 1, 2005 6:32 pm",
                dateTimeCol02, "6/1/24 10:00"), false);

        Calendar calTime02 = Calendar.getInstance();
        calTime02.set(Calendar.HOUR_OF_DAY, 14);
        calTime02.set(Calendar.MINUTE, 42);

        Calendar calDateTime02 = Calendar.getInstance();
        calDateTime02.set(2024, Calendar.JUNE, 1, 10, 0);

        // Time only fields do not reflect daylight saving time.
        // Note: that TIME_ZONE.getDisplayName gets local machine info. It may not be accurate if you are running
        // against a remote server or running your server in a different timezone.
        Map<String, String> expectedRowValues = Map.of(dateCol01, "April 01, 2021",
                dateCol02, "2019-52 AD",
                timeCol01, "12:32:00.00 AM",
                timeCol02, String.format("2:42 PM %s", TIME_ZONE.getDisplayName(false, 0, Locale.getDefault())),
                dateTimeCol01, "May 01, 2005 06:32:00.00 PM",
                dateTimeCol02, String.format("10:00 AM %s 2024-22 AD", getTimezoneDesc(calDateTime02.getTime())));

        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listFormat);
        DomainFormPanel domainEditor = listDefinitionPage.getFieldsPanel();

        validateFieldsInDesigner(domainEditor, dateCol01, false, FieldDefinition.ColumnType.Date,
                nsDateFormat01, TT_NS_DATE);
        validateFieldsInDesigner(domainEditor, dateCol02, false, FieldDefinition.ColumnType.Date,
                nsDateFormat02, TT_NS_DATE);
        validateFieldsInDesigner(domainEditor, timeCol01, false, FieldDefinition.ColumnType.Time,
                nsTimeFormat01, TT_NS_TIME);
        validateFieldsInDesigner(domainEditor, timeCol02, false, FieldDefinition.ColumnType.Time,
                nsTimeFormat02, TT_NS_TIME);
        validateFieldsInDesigner(domainEditor, dateTimeCol01, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsDateFormat01, nsTimeFormat01), TT_NS_DATETIME);
        validateFieldsInDesigner(domainEditor, dateTimeCol02, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsTimeFormat02, nsDateFormat02), TT_NS_DATETIME);

        validateDataIsFormatted(getProjectName(), listFormat, expectedRowValues);

        log(String.format("Validate the design and data of list '%s' (does inherit formats).", listInherit));
        goToProjectHome();
        _listHelper.goToList(listInherit);
        _listHelper.insertNewRow(Map.of(dateCol01, "4/1/21",
                timeCol01, "12:32 am",
                dateTimeCol01, "May 1, 2005 6:32 pm"), false);

        Calendar calTime01 = Calendar.getInstance();
        calTime01.set(Calendar.HOUR_OF_DAY, 0);
        calTime01.set(Calendar.MINUTE, 32);

        Calendar calDateTime01 = Calendar.getInstance();
        calDateTime01.set(2005, Calendar.MAY, 1, 18, 32);

        expectedRowValues = new HashMap<>();
        expectedRowValues.put(dateCol01, "01/04/21");
        expectedRowValues.put(timeCol01, String.format("0:32:0 AM %s00", getTimezoneOffset(TIME_ZONE.getRawOffset())));

        if (SystemUtils.IS_OS_WINDOWS)
        {
            expectedRowValues.put(dateTimeCol01, "Sunday May 01 121 2005 18:32:0 Z");
        }
        else
        {
            expectedRowValues.put(dateTimeCol01,
                    String.format("Sunday May 01 121 2005 18:32:0 %s", getTimezoneOffset(calDateTime01.getTime())));
        }

        listDefinitionPage =  _listHelper.goToEditDesign(listInherit);
        domainEditor = listDefinitionPage.getFieldsPanel();

        validateFieldsInDesigner(domainEditor, dateCol01, true, FieldDefinition.ColumnType.Date,
                PROJECT_DATE_FORMAT, TT_NS_DATE);
        validateFieldsInDesigner(domainEditor, timeCol01, true, FieldDefinition.ColumnType.Time,
                PROJECT_TIME_FORMAT, TT_NS_TIME);
        validateFieldsInDesigner(domainEditor, dateTimeCol01, true, FieldDefinition.ColumnType.DateAndTime,
                PROJECT_DATETIME_FORMAT, TT_NS_DATETIME);

        listDefinitionPage.clickCancel();

        validateDataIsFormatted(getProjectName(), listInherit, expectedRowValues);

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

        String scope = String.format("Project: %s", getProjectName());
        validateSiteValidationReport(scope, expectedWarnings, true);

        log("Validate that Site Validation does not contain warnings about inherited fields.");

        expectedWarnings = List.of(String.format("Date property \"lists.%s.%s\": %s",
                        listInherit, dateCol01, nsDateFormat01),
                String.format("Time property \"lists.%s.%s\": %s",
                        listInherit, timeCol01, nsTimeFormat01),
                String.format("DateTime property \"lists.%s.%s\": %s %s",
                        listInherit, dateTimeCol01, nsDateFormat01, nsTimeFormat01)
        );

        validateSiteValidationReport(scope, expectedWarnings, false);

    }

    /**
     * <p>
     *     Validate editing Date-Time fields with non-standard formats to standard formats.
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
    public void testEditDomain() throws IOException, CommandException
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

        createListByAPI(getProjectName(), listEdit, listFields);

        log("Check that Site Validation tags the fields in the list.");

        List<String> expectedWarnings = List.of(String.format("Date property \"lists.%s.%s\": %s",
                        listEdit, dateCol, nsDateFormat),
                String.format("Time property \"lists.%s.%s\": %s",
                        listEdit, timeCol, nsTimeFormat),
                String.format("DateTime property \"lists.%s.%s\": %s %s",
                        listEdit, dateTimeCol, nsDateFormat, nsTimeFormat));

        String scope = String.format("Project: %s", getProjectName());
        validateSiteValidationReport(scope, expectedWarnings, true);

        goToProjectHome();
        _listHelper.goToList(listEdit);
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listEdit);

        log("Edit the Date, Time and DateTime fields but then cancel out of the edit.");

        DomainFormPanel domainEditor = listDefinitionPage.getFieldsPanel();
        DomainFieldRow fieldRow = domainEditor.getField(dateCol);
        fieldRow.expand();
        fieldRow.setDateFormat(DATE_FORMAT.yyyy_MM_dd);
        checker().withScreenshot()
                .verifyFalse(String.format("Changing '%s' to a standard format should remove the warning icon.", dateCol),
                        fieldRow.hasDomainWarningIcon());

        fieldRow = domainEditor.getField(timeCol);
        fieldRow.expand();
        fieldRow.setTimeFormat(TIME_FORMAT.HH_mm_ss);
        checker().withScreenshot()
                .verifyFalse(String.format("Changing '%s' to a standard format should remove the warning icon.", timeCol),
                        fieldRow.hasDomainWarningIcon());

        fieldRow = domainEditor.getField(dateTimeCol);
        fieldRow.expand();
        fieldRow.setDateTimeFormat(DATE_FORMAT.yyyy_MM_dd, TIME_FORMAT.hh_mm_a);
        checker().withScreenshot()
                .verifyFalse(String.format("Changing '%s' to a standard format should remove the warning icon.", dateTimeCol),
                        fieldRow.hasDomainWarningIcon());

        log("Cancel out of the edit.");
        listDefinitionPage.clickCancel();

        log("Go back to the design page and validate that the non-standard formats are still there.");
        listDefinitionPage = _listHelper.goToEditDesign(listEdit);
        domainEditor = listDefinitionPage.getFieldsPanel();

        validateFieldsInDesigner(domainEditor, dateCol, false, FieldDefinition.ColumnType.Date,
                nsDateFormat, TT_NS_DATE);
        validateFieldsInDesigner(domainEditor, timeCol, false, FieldDefinition.ColumnType.Time,
                nsTimeFormat, TT_NS_TIME);
        validateFieldsInDesigner(domainEditor, dateTimeCol, false, FieldDefinition.ColumnType.DateAndTime,
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

        validateFieldsInDesigner(domainEditor, dateCol, false, FieldDefinition.ColumnType.Date,
                DATE_FORMAT.yyyy_MM_dd.toString(), null);
        validateFieldsInDesigner(domainEditor, timeCol, false, FieldDefinition.ColumnType.Time,
                TIME_FORMAT.HH_mm_ss.toString(), null);
        validateFieldsInDesigner(domainEditor, dateTimeCol, false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", DATE_FORMAT.yyyy_MM_dd, TIME_FORMAT.hh_mm_a), null);

        log("Check that Site Validation Does not tag the fields in the list after they have been edited.");

        validateSiteValidationReport(scope, expectedWarnings, false);

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

        createDataClass(getProjectName(), dcFormat, fields);

        fields = List.of(
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime)
        );

        log(String.format("Create a Data Class named '%s' that inherits non-standard format fields from the project.", dcFormat));

        createDataClass(getProjectName(), dcInherit, fields);

        log("Add some data to both data classes as a sanity validation of the formats.");

        String bulkData = String.format("%s\t%s\t%s\t%s\n", "Name", dateTimeCol, dateCol, timeCol)
                + "A\t12/23/24 14:45\t12/23/24\t14:45\n";

        Map<String, String> expectedFormatData = Map.of("Name", "A",
                dateTimeCol, "December 23, 2024 02:45:00.00 PM",
                dateCol, "December 23, 2024",
                timeCol, "02:45:00.00 PM", "Flag", "");

        Calendar calDateTime = Calendar.getInstance();
        calDateTime.set(2024, Calendar.DECEMBER, 23, 14, 45);

        Calendar calTime = Calendar.getInstance();
        calTime.set(Calendar.HOUR_OF_DAY, 14);
        calTime.set(Calendar.MINUTE, 45);

        Map<String, String> expectedInheritedData = new HashMap<>();
        expectedInheritedData.put("Name", "A");
        expectedInheritedData.put(dateCol, "23/12/24");
        // The offset of Time only fields are not impacted by daylight saving time.
        // Note: that TIME_ZONE.getRawOffset gets local machine info. It may not be accurate if you are running
        // against a remote server or running your server in a different timezone.
        expectedInheritedData.put(timeCol, String.format("2:45:0 PM %s00", getTimezoneOffset(TIME_ZONE.getRawOffset())));
        expectedInheritedData.put("Flag", "");

        if (SystemUtils.IS_OS_WINDOWS)
        {
            expectedInheritedData.put(dateTimeCol, "Monday December 23 358 2024 14:45:0 Z");
        }
        else
        {
            expectedInheritedData.put(dateTimeCol,
                    String.format("Monday December 23 358 2024 14:45:0 %s", getTimezoneOffset(calDateTime.getTime())));
        }

        populateDataClass(getProjectName(), dcFormat, bulkData);
        populateDataClass(getProjectName(), dcInherit, bulkData);

        log(String.format("Validate domain designer feedback for '%s'.", dcFormat));

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dcFormat));
        clickAndWait(Locator.lkButton("Edit Data Class"));
        CreateDataClassPage editPage = new CreateDataClassPage(getDriver());
        DomainFormPanel domainEditor = editPage.getDomainEditor();

        validateFieldsInDesigner(domainEditor, dateCol,
        false, FieldDefinition.ColumnType.Date,
                nsDateFormat, TT_NS_DATE);

        validateFieldsInDesigner(domainEditor, timeCol,
                false, FieldDefinition.ColumnType.Time,
                nsTimeFormat, TT_NS_TIME);

        validateFieldsInDesigner(domainEditor, dateTimeCol,
                false, FieldDefinition.ColumnType.DateAndTime,
                String.format("%s %s", nsDateFormat, nsTimeFormat), TT_NS_DATETIME);

        editPage.clickCancel();

        log("Validate the data (make sure we still respect non-standard formats).");
        validateDataIsFormatted(getProjectName(), dcFormat, expectedFormatData);

        log("Validate that data sets are reported in Site Validation.");
        List<String> expectedWarnings = List.of(String.format("Date property \"exp.data.%s.%s\": %s",
                        dcFormat, dateCol, nsDateFormat),
                String.format("Time property \"exp.data.%s.%s\": %s",
                        dcFormat, timeCol, nsTimeFormat),
                String.format("DateTime property \"exp.data.%s.%s\": %s %s",
                        dcFormat, dateTimeCol, nsDateFormat, nsTimeFormat));

        String scope = String.format("Project: %s", getProjectName());
        validateSiteValidationReport(scope, expectedWarnings, true);

        log(String.format("Validate domain designer feedback for '%s'.", dcInherit));

        goToProjectHome();
        clickAndWait(Locator.linkWithText(dcInherit));
        clickAndWait(Locator.lkButton("Edit Data Class"));
        editPage = new CreateDataClassPage(getDriver());
        domainEditor = editPage.getDomainEditor();

        validateFieldsInDesigner(domainEditor, dateCol,
                false, FieldDefinition.ColumnType.Date,
                PROJECT_DATE_FORMAT, TT_NS_DATE);

        validateFieldsInDesigner(domainEditor, timeCol,
                false, FieldDefinition.ColumnType.Time,
                PROJECT_TIME_FORMAT, TT_NS_TIME);

        validateFieldsInDesigner(domainEditor, dateTimeCol,
                false, FieldDefinition.ColumnType.DateAndTime,
                PROJECT_DATETIME_FORMAT, TT_NS_DATETIME);

        editPage.clickCancel();

        log("Validate the data (make sure we still respect non-standard formats).");
        validateDataIsFormatted(getProjectName(), dcInherit, expectedInheritedData);

    }

    /**
     * <p>
     *     This test will check the scoping of non-standard formats when set at the site and subfolder levels. This test
     *     creates a separate project to avoid the project settings from changed for the default test project from
     *     interfering. This test also uses data classes for validation because a data class is visible in subfolders.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a separate project with a subfolder. Project has standard formats.</li>
     *         <li>Create and populate a data class in the project.</li>
     *         <li>Create and populate a data class in the subfolder.</li>
     *         <li>From the subfolder add a record to the data class in the parent folder.</li>
     *         <li>Change the Date-Time settings at the site level to be non-standard.</li>
     *         <li>Validate the 'Look and Feel' page for the site.</li>
     *         <li>Validate the Site Validation report calls out the site settings.</li>
     *         <li>Change the Date-Time format at the subfolder.</li>
     *         <li>Validate the 'Formats' page for the subfolder.</li>
     *         <li>Validate the Site Validation report.</li>
     *         <li>Validate the data in both data classes is formatted as expected in the subfolder.</li>
     *         <li>Reset the site settings.</li>
     *         <li>Validate the Site Validation does not call out the site settings but still calls out the subfolder.</li>
     *         <li>Validate the 'Look and Feel' page is reset with the default formats.</li>
     *         <li>Validate the subfolder formats are unchanged.</li>
     *     </ul>
     *     Note: there are periodic checks that the data is formatted as expected at the various levels.
     * </p>
     *
     * @throws IOException Can be thrown by the test APIs.
     * @throws CommandException Can be thrown by the test APIs.
     */
    @Test
    public void testScopeFromSiteToSubFolder() throws IOException, CommandException
    {

        String folderProject = "Non-Standard Sub-Folder Test";
        String subFolder = "SubFolder_01";
        String subFolderPath = folderProject + "/" + subFolder;

        String dcInProj = "DC In Project Folder";
        String dcInSub = "DC In Sub-Folder";
        String dateCol = "Date";
        String timeCol = "Time";
        String dateTimeCol = "DateTime";

        log(String.format("Create a project '%s' with standard formatting.", folderProject));

        _containerHelper.deleteProject(folderProject, false);
        _containerHelper.createProject(folderProject, null);

        log(String.format("Create a sub-folder '%s'.", folderProject));
        _containerHelper.createSubfolder(folderProject, subFolder);

        log(String.format("In the parent folder create a DataClass named '%s' with various Date, Time and DateTime columns.", dcInProj));

        goToProjectHome(folderProject);
        _portalHelper.addWebPart("Data Classes");

        List<FieldDefinition> fields = List.of(
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime)
        );

        createDataClass(folderProject, dcInProj, fields);

        log("Add data to use for format validation.");
        String bulkData = String.format("%s\t%s\t%s\t%s\n", "Name", dateTimeCol, dateCol, timeCol)
                + "P1\t12/23/24 14:45\t12/23/24\t14:45\n";

        populateDataClass(folderProject, dcInProj, bulkData);

        log(String.format("In the sub-folder '%s' create a DataClass named '%s' with Date & Time columns.", subFolder, dcInSub));

        navigateToFolder(folderProject, subFolder);
        _portalHelper.addWebPart("Data Classes");

        fields = List.of(
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime)
        );

        createDataClass(subFolderPath, dcInSub, fields);

        log("Add data to DataClass created in the subfolder as validation.");

        bulkData = String.format("%s\t%s\t%s\t%s\n", "Name", dateTimeCol, dateCol, timeCol)
                + "C1\t11/28/24 11:11\t11/28/24\t11:11\n";

        populateDataClass(subFolderPath, dcInSub, bulkData);

        log("In the subfolder add data to DataClass from the parent folder as validation.");

        populateDataClass(subFolderPath, dcInProj, bulkData);

        log("Change the site setting to be non-standard.");

        String nsSiteDateFormat = "MMMM dd, yyyy";
        String nsSiteTimeFormat = "kk:mm z";

        changeSiteDateAndTimeFormats(nsSiteDateFormat, nsSiteTimeFormat);

        goToProjectHome(folderProject);

        log("Validate the site 'Look and Feel' page has the correct values, feedback, etc...");
        LookAndFeelSettingsPage lookAndFeelSettingsPage = goToAdminConsole().clickLookAndFeelSettings();
        validateSettingsPage(null, nsSiteDateFormat, true,
                null, nsSiteTimeFormat, true,
                null, String.format("%s %s", nsSiteDateFormat, nsSiteTimeFormat), "",
                true, false);

        if (checker().withScreenshot()
                .verifyTrue("The warning banner at the top of the Site Setting page does not contain the expected comment.",
                        lookAndFeelSettingsPage.getFormatWarningMessage()
                                .startsWith("Warning: One or more date, time, or date-time display formats are using non-standard patterns.")))
        {
            WebElement link = Locator.linkWithText("Click here").findWhenNeeded(getDriver());
            if (checker().withScreenshot()
                    .verifyTrue("'Click here' link is not visible.", link.isDisplayed()))
            {
                link.click();
                switchToWindow(1);
                WebElement banner = Locator.tagWithText("h3", "Date & Number Display Formats").refindWhenNeeded(getDriver());
                checker().withScreenshot()
                        .verifyTrue("'Click here' link did not navigate as expected.",
                            WebServicesUtil.isLabKeyDotOrgMaintenance(getDriver()) ||
                                waitFor(banner::isDisplayed, 1_000));
                closeExtraWindows();
            }
        }

        log("Check that the Site Validation report calls out the site settings.");

        List<String> siteWarnings = List.of(String.format("Site default display format for Dates: %s", nsSiteDateFormat),
                String.format("Site default display format for Times: %s", nsSiteTimeFormat),
                String.format("Site default display format for DateTimes: %s %s", nsSiteDateFormat, nsSiteTimeFormat));

        String scope = "Root: /";
        validateSiteValidationReport(scope, siteWarnings, true);

        log(String.format("Validate project settings for '%s' inherit the site settings.", folderProject));
        ProjectSettingsPage.beginAt(this, folderProject);
        validateSettingsPage(true, nsSiteDateFormat, false,
                true, nsSiteTimeFormat, false,
                true, String.format("%s %s", nsSiteDateFormat, nsSiteTimeFormat), "",
                false, false);

        log(String.format("Validate subfolder settings for '%s' inherit the site settings.", subFolderPath));
        FolderFormatsPage.beginAt(this, subFolderPath);
        validateSettingsPage(true, nsSiteDateFormat, false,
                true, nsSiteTimeFormat, false,
                true, String.format("%s %s", nsSiteDateFormat, nsSiteTimeFormat), "",
                false, false);

        Calendar calDateTime = Calendar.getInstance();
        calDateTime.set(2024, Calendar.DECEMBER, 23, 14, 25);

        Calendar calTime = Calendar.getInstance();
        calTime.set(Calendar.HOUR_OF_DAY, 14);
        calTime.set(Calendar.MINUTE, 45);

        log("Sanity check that the DataClass data is formatted in the project and subfolder.");

        // Time only fields do not reflect daylight saving time.
        // Note: that TIME_ZONE.getDisplayName gets local machine info. It may not be accurate if you are running
        // against a remote server or running your server in a different timezone.
        Map<String, String> expectedFormatData = Map.of("Name", "P1",
                dateTimeCol, String.format("December 23, 2024 14:45 %s", getTimezoneDesc(calDateTime.getTime())),
                dateCol, "December 23, 2024",
                timeCol, String.format("14:45 %s", TIME_ZONE.getDisplayName(false, 0, Locale.getDefault())),
                "Flag", "");

        validateDataIsFormatted(folderProject, dcInProj, expectedFormatData);

        calDateTime.set(2024, Calendar.NOVEMBER, 28, 11, 11);

        calTime.set(Calendar.HOUR_OF_DAY, 11);
        calTime.set(Calendar.MINUTE, 11);

        expectedFormatData = Map.of("Name", "C1",
                dateTimeCol, String.format("November 28, 2024 11:11 %s", getTimezoneDesc(calDateTime.getTime())),
                dateCol, "November 28, 2024",
                timeCol, String.format("11:11 %s", TIME_ZONE.getDisplayName(false, 0, Locale.getDefault())),
                "Flag", "");
        validateDataIsFormatted(subFolderPath, dcInSub, expectedFormatData);
        validateDataIsFormatted(subFolderPath, dcInProj, expectedFormatData);

        log("Change the formats in the subFolder.");
        String nsSubDateFormat = "EEEE MMMM dd, yyyy";
        String nsSubTimeFormat = "kk:mm (z)";

        new APIContainerHelper(this)
                .setDateAndTimeFormats(createDefaultConnection(), subFolderPath,
                        nsSubDateFormat, nsSubTimeFormat, String.format("%s %s", nsSubDateFormat, nsSubTimeFormat));

        log(String.format("Validate folder settings for '%s' no longer inherit the site settings.", subFolderPath));
        FolderFormatsPage.beginAt(this, subFolderPath);
        validateSettingsPage(false, nsSubDateFormat, true,
                false, nsSubTimeFormat, true,
                false, String.format("%s %s", nsSubDateFormat, nsSubTimeFormat), "",
                true, false);

        log("Check the format in the DataClasses");
        expectedFormatData = Map.of("Name", "C1",
                dateTimeCol, String.format("Thursday November 28, 2024 11:11 (%s)", getTimezoneDesc(calDateTime.getTime())),
                dateCol, "Thursday November 28, 2024",
                timeCol, String.format("11:11 (%s)", TIME_ZONE.getDisplayName(false, 0, Locale.getDefault())),
                "Flag", "");
        validateDataIsFormatted(subFolderPath, dcInSub, expectedFormatData);
        validateDataIsFormatted(subFolderPath, dcInProj, expectedFormatData);

        log("Check that the 'Site Validation' report now includes the subfolder.");
        List<String> folderWarnings = List.of(String.format("Folder default display format for Dates: %s", nsSubDateFormat),
                String.format("Folder default display format for Times: %s", nsSubTimeFormat),
                String.format("Folder default display format for DateTimes: %s %s", nsSubDateFormat, nsSubTimeFormat));

        scope = String.format("Folder: %s", subFolderPath);
        validateSiteValidationReport(scope, folderWarnings, true);

        log("Reset the site settings.");
        resetSiteSettings();

        log("Validate 'Look and Feel' page after reset.");
        goToAdminConsole().clickLookAndFeelSettings();
        validateSettingsPage(null, DATE_FORMAT.Default.toString(), false,
                null, TIME_FORMAT.Default.toString(), false,
                null, DATE_FORMAT.DTDefault.toString(), TIME_FORMAT.DTDefault.toString(),
                false, false);

        log("Validate site is no longer listed in Site Validation report.");
        scope = "Root: /";
        validateSiteValidationReport(scope, siteWarnings, false);

        log(String.format("Validate subfolder settings for '%s' still do not inherit the site settings.", subFolderPath));
        FolderFormatsPage.beginAt(this, subFolderPath);
        validateSettingsPage(false, nsSubDateFormat, true,
                false, nsSubTimeFormat, true,
                false, String.format("%s %s", nsSubDateFormat, nsSubTimeFormat), "",
                true, false);

        log("Validate the subfolder still appears in the Site Validation report.");
        scope = String.format("Folder: %s", subFolderPath);
        validateSiteValidationReport(scope, folderWarnings, true);

    }

    /**
     * <p>
     *     Use the text values 'Date', 'Time' and 'DateTime' as the format values in a domain designer. This will use a
     *     DataClass because its design will be visible in the sub-folder.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a new project and set the Date-Time formats to some standard format.</li>
     *         <li>Create a sub-folder and set the Date-Time formats to some other standard format.</li>
     *         <li>Create a DataClass in parent folder.</li>
     *         <li>Set the date field format to 'Date'.</li>
     *         <li>Set the time field format to 'Time'.</li>
     *         <li>Set the DateTime field format to 'DateTime' and 'none'.</li>
     *         <li>Validate data is formatted as expected in the project.</li>
     *         <li>In the sub-folder validate the data is formatted by the sub-folder settings.</li>
     *         <li>Validate that setting the date part of a DateTime field only allows 'none' for the time format part.</li>
     *     </ul>
     * </p>
     *
     * @throws IOException Can be thrown by helpers that create the folders, etc...
     * @throws CommandException Can be thrown by helpers that create the folders, etc...
     */
    @Test
    public void testTextFormatValueOfDate() throws IOException, CommandException
    {

        String folderProject = "Text_Format_Test";
        String subFolder = "SubFolder_01";
        String subFolderPath = folderProject + "/" + subFolder;

        log(String.format("Create a project '%s' with standard formatting.", folderProject));

        _containerHelper.deleteProject(folderProject, false);
        _containerHelper.createProject(folderProject, null);

        log(String.format("Create a sub-folder '%s'.", folderProject));
        _containerHelper.createSubfolder(folderProject, subFolder);

        goToProjectHome(folderProject);
        _portalHelper.addWebPart("Data Classes");

        goToProjectHome(subFolderPath);
        _portalHelper.addWebPart("Data Classes");

        String dcSetFormats = "DC Use Text Format";
        String dcCheckWarnings = "DC Check Warnings";

        String dateCol = "Date01";
        String timeCol = "Time01";
        String dateTimeCol = "DateTime01";

        log(String.format("Create a DataClass named '%s' with Date, Time and DateTime columns.", dcSetFormats));

        List<FieldDefinition> fields = List.of(
                new FieldDefinition(dateCol, FieldDefinition.ColumnType.Date),
                new FieldDefinition(timeCol, FieldDefinition.ColumnType.Time),
                new FieldDefinition(dateTimeCol, FieldDefinition.ColumnType.DateAndTime)
        );

        createDataClass(folderProject, dcSetFormats, fields);
        createDataClass(folderProject, dcCheckWarnings, fields);

        log("Add data in the parent folder.");
        String bulkData = String.format("%s\t%s\t%s\t%s\n", "Name", dateTimeCol, dateCol, timeCol)
                + "P1\t11/11/11 23:11\t11/11/11\t23:11\n";

        populateDataClass(folderProject, dcSetFormats, bulkData);

        log("Add data to the DataClasses in the subfolder.");

        bulkData = String.format("%s\t%s\t%s\t%s\n", "Name", dateTimeCol, dateCol, timeCol)
                + "C1\t11/11/11 23:11\t11/11/11\t23:11\n";

        populateDataClass(subFolderPath, dcSetFormats, bulkData);

        log("At the project level set the formats to something other than the default values.");
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this, folderProject);
        projectSettingsPage.setDefaultDateDisplayInherited(false);
        projectSettingsPage.setDefaultDateDisplay(DATE_FORMAT.dd_MMM_yyyy);
        projectSettingsPage.setDefaultTimeDisplayInherited(false);
        projectSettingsPage.setDefaultTimeDisplay(TIME_FORMAT.hh_mm_a);
        projectSettingsPage.setDefaultDateTimeDisplayInherited(false);
        projectSettingsPage.setDefaultDateTimeDisplay(DATE_FORMAT.dd_MMM_yyyy, TIME_FORMAT.hh_mm_a);
        projectSettingsPage.save();

        log("At the sub-folder level set the formats to something different as well.");
        FolderFormatsPage folderFormatsPage = FolderManagementPage.beginAt(this, subFolderPath).goToFormatsTab();
        folderFormatsPage.setDefaultDateDisplayInherited(false);
        folderFormatsPage.setDefaultDateDisplay(DATE_FORMAT.ddMMMyy);
        folderFormatsPage.setDefaultTimeDisplayInherited(false);
        folderFormatsPage.setDefaultTimeDisplay(TIME_FORMAT.HH_mm_ss_SSS);
        folderFormatsPage.setDefaultDateTimeDisplayInherited(false);
        folderFormatsPage.setDefaultDateTimeDisplay(DATE_FORMAT.ddMMMyy, TIME_FORMAT.HH_mm_ss_SSS);
        folderFormatsPage.clickSave();

        log("Go to the project and change the formats in the DataClass.");
        goToProjectHome(folderProject);

        log(String.format("For DataClass '%s' in the parent folder, change the formats to use 'Date', 'Time' and 'DateTime'.", dcSetFormats));
        clickAndWait(Locator.linkWithText(dcSetFormats));
        clickAndWait(Locator.lkButton("Edit Data Class"));
        CreateDataClassPage editPage = new CreateDataClassPage(getDriver());
        DomainFormPanel domainEditor = editPage.getDomainEditor();
        DomainFieldRow fieldRow = domainEditor.getField(dateCol);
        fieldRow.setDateInherited(false);
        fieldRow.setDateFormat(DATE_FORMAT.DATE);
        fieldRow = domainEditor.getField(timeCol);
        fieldRow.setTimeInherited(false);
        fieldRow.setTimeFormat(TIME_FORMAT.TIME);
        fieldRow = domainEditor.getField(dateTimeCol);
        fieldRow.setDateTimeInherited(false);
        fieldRow.setDateTimeFormat(DATE_FORMAT.DATETIME, TIME_FORMAT.none);
        editPage.clickSave();

        log("Validate that the values are formatted with the settings in the parent folder.");

        Map<String, String> expectedFormatData = Map.of("Name", "P1",
                dateTimeCol, "11-Nov-2011 11:11 PM",
                dateCol, "11-Nov-2011",
                timeCol, "11:11 PM", "Flag", "");

        validateDataIsFormatted(folderProject, dcSetFormats, expectedFormatData);

        log("Check that using the values DATE, TIME and DATETIME do not show up in the validation report.");
        // Issue 51491
        List<String> siteWarnings = List.of(String.format("Site default display format for Dates: %s", DATE_FORMAT.DATE),
                String.format("Site default display format for Times: %s", TIME_FORMAT.TIME),
                String.format("Site default display format for DateTimes: %s %s", DATE_FORMAT.DATETIME, TIME_FORMAT.none));

        String scope = "Root: /";
        validateSiteValidationReport(scope, siteWarnings, false);

        log(String.format("Go to sub-folder '%s' and validate the data here is formatted according to sub-folder settings.", subFolder));

        expectedFormatData = Map.of("Name", "C1",
                dateTimeCol, "11Nov11 23:11:00.000",
                dateCol, "11Nov11",
                timeCol, "23:11:00.000", "Flag", "");

        validateDataIsFormatted(subFolderPath, dcSetFormats, expectedFormatData);

        log("Go back to parent folder and validate warnings for DateTime fields.");

        goToProjectHome(folderProject);

        clickAndWait(Locator.linkWithText(dcCheckWarnings));
        clickAndWait(Locator.lkButton("Edit Data Class"));
        editPage = new CreateDataClassPage(getDriver());
        domainEditor = editPage.getDomainEditor();
        fieldRow = domainEditor.getField(dateTimeCol);
        fieldRow.setDateTimeInherited(false);
        fieldRow.setDateTimeFormat(DATE_FORMAT.DATETIME, TIME_FORMAT.hh_mm_a);

        checker().verifyTrue(String.format("When setting the date part of a DateTime to '%s', '%s' to should be the only allowed value for the time part.",
                        DATE_FORMAT.DATETIME, TIME_FORMAT.none),
                fieldRow.hasDomainWarningIcon());

        List<String> actualValues = editPage.clickSaveExpectingErrors();

        List<String> expectedValues = List.of(
                String.format("Property %s: %s %s is an illegal format for type DateTime",
                        dateTimeCol, DATE_FORMAT.DATETIME, TIME_FORMAT.hh_mm_a),
                String.format("Please correct errors in %s before saving.",
                        dcCheckWarnings));

        checker().verifyEquals("Error messages are not as expected.",
                expectedValues, actualValues);

        checker().screenShotIfNewError("Warning_Errors");

        log("Cancel out of the editor.");
        editPage.clickCancel();

    }

    // Private helper that will create a list through the APIs. This allows the list to have non-standard formats.
    private void createListByAPI(String path, String listName, List<FieldDefinition> fields) throws IOException, CommandException
    {
        Connection connection = createDefaultConnection();
        ListDefinition listDef = new IntListDefinition(listName, "Key");
        for(FieldDefinition field : fields)
        {
            listDef.addField(field);
        }

        listDef.create(connection, path);
    }

    private void createDataClass(String path, String dcName, List<FieldDefinition> fields) throws IOException, CommandException
    {
        log(String.format("Create a Data Class named '%s' in '%s'.", dcName, path));

        DataClassDefinition dataClass = new DataClassDefinition(dcName);
        for(FieldDefinition field : fields)
        {
            dataClass.addField(field);
        }

        dataClass.create(createDefaultConnection(), path);

    }

    private void populateDataClass(String path, String dcName, String importData)
    {
        goToProjectHome(path);
        clickAndWait(Locator.linkWithText(dcName));

        DataRegionTable dataTable = new DataRegionTable("query", getDriver());
        dataTable.clickImportBulkData()
                .setText(importData);

        clickButton("Submit");

    }

    // Private helper to validate the data in the list or data class is formatted as expected.
    private void validateDataIsFormatted(String projectPath, String domainName, Map<String, String> expectedFormatData)
    {
        goToProjectHome(projectPath);
        clickAndWait(Locator.linkWithText(domainName));
        DataRegionTable dataTable = new DataRegionTable("query", getDriver());
        Map<String, String> actualData = dataTable.getRowDataAsMap(0);
        checker().withScreenshot()
                .verifyEquals(String.format("Data in '%s' is not formatted as expected.", domainName),
                        expectedFormatData, actualData);

    }

    // Private helper to change the Date-Time formats at the site level to non-standard formats.
    // This modifies the html of the page to make an option in each field to be of a non-standard format.
    private void changeSiteDateAndTimeFormats(String dateFormat, String timeFormat)
    {
        LookAndFeelSettingsPage lookAndFeelSettingsPage = goToAdminConsole().clickLookAndFeelSettings();

        log(String.format("Chang date format '%s' to have a value of '%s'.", DATE_FORMAT.ddMMMyy, dateFormat));
        WebElement defaultDate = Locator.id("defaultDateFormat").findElement(getDriver());
        WebElement optionEl = Locator.tagWithAttribute("option", "value", DATE_FORMAT.ddMMMyy.toString()).findElement(defaultDate);
        executeScript("arguments[0].value = arguments[1]", optionEl, dateFormat);
        selectOptionByValue(defaultDate, dateFormat);

        log(String.format("Chang time format '%s' to have a value of '%s'.", TIME_FORMAT.hh_mm_a, timeFormat));
        WebElement defaultTime = Locator.id("defaultTimeFormat").findElement(getDriver());
        optionEl = Locator.tagWithAttribute("option", "value", TIME_FORMAT.hh_mm_a.toString()).findElement(defaultTime);
        executeScript("arguments[0].value = arguments[1]", optionEl, timeFormat);
        selectOptionByValue(defaultTime, timeFormat);

        log(String.format("Chang DateTime date format '%s' to have a value of '%s'.", DATE_FORMAT.ddMMMyy, dateFormat));
        WebElement defaultDTDate = Locator.id("dateSelect").findElement(getDriver());
        optionEl = Locator.tagWithAttribute("option", "value", DATE_FORMAT.ddMMMyy.toString()).findElement(defaultDTDate);
        executeScript("arguments[0].value = arguments[1]", optionEl, dateFormat);
        selectOptionByValue(defaultDTDate, dateFormat);

        log(String.format("Chang DateTime time format '%s' to have a value of '%s'.", TIME_FORMAT.hh_mm_a, timeFormat));
        WebElement defaultDTTime = Locator.id("timeSelect").findElement(getDriver());
        optionEl = Locator.tagWithAttribute("option", "value", TIME_FORMAT.hh_mm_a.toString()).findElement(defaultDTTime);
        executeScript("arguments[0].value = arguments[1]", optionEl, timeFormat);
        selectOptionByValue(defaultDTTime, timeFormat);

        lookAndFeelSettingsPage.save();
    }

    // Private helper that validates Date, Time and DateTime fields in a domain designer.
    // isInherited: Used to check the enabled / editable state of the field.
    // columnType: Used to identify the expected field options and messages.
    // expectedToolTipText: Used as a check for the warning icon. If null no warning icon is expected.
    private void validateFieldsInDesigner(DomainFormPanel domainEditor, String fieldName,
                                          boolean isInherited, FieldDefinition.ColumnType columnType,
                                          String expectedFormat, @Nullable String expectedToolTipText)
    {
        DomainFieldRow fieldRow = domainEditor.getField(fieldName);
        fieldRow.expand();

        if (FieldDefinition.ColumnType.Date.equals(columnType))
        {
            if(isInherited)
            {
                checker().verifyTrue(String.format("Field '%s' should show as inherited, it does not.", fieldName),
                        fieldRow.isDateInherited());

                checker().verifyFalse(String.format("Field '%s' should not be enabled.", fieldName),
                        fieldRow.isDateFormatEnabled());
            }

            checker().verifyEquals(String.format("Date format for field '%s' not as expected.", fieldName),
                    expectedFormat, fieldRow.getDateFormat());
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

            checker().verifyEquals(String.format("Time format for field '%s' not as expected.", fieldName),
                    expectedFormat, fieldRow.getTimeFormat());
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

            checker().verifyEquals(String.format("DateTime Date format for field '%s' not as expected.", fieldName),
                    expectedDateFormat, fieldRow.getDateTimeFormatDate());

            checker().verifyEquals(String.format("DateTime Time format for field '%s' not as expected.", fieldName),
                    expectedTimeFormat, fieldRow.getDateTimeFormatTime());
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

    // These controls are shared across multiple setting pages. It is easier to find them here in the test than to use
    // the page objects.
    private String getFormatFromControl(String fieldId)
    {
        return getSelectedOptionValue(Locator.id(fieldId).findElement(getDriver()));
    }

    private boolean getFieldInherited(String fieldName)
    {
        return Locator.checkboxByName(fieldName).findElement(getDriver()).isSelected();
    }

    private boolean getNonStandardWarning(String fieldId)
    {
        String xpath = String.format("//select[@id='%s']/following-sibling::span[@class='has-warning']", fieldId);
        return Locator.xpath(xpath).findWhenNeeded(getDriver()).isDisplayed();
    }

    // Private helper to check the format settings at the site, project or folder.
    // Site settings do not inherit, if the inherited parameter is null that check is skipped.
    private void validateSettingsPage(@Nullable Boolean dateInherited, String dateFormat, boolean dateWarning,
                                      @Nullable Boolean timeInherited, String timeFormat, boolean timeWarning,
                                      @Nullable Boolean dateTimeInherited, String dtDateFormat, String dtTimeFormat,
                                      boolean dtDateWarning, boolean dtTimeWarning)
    {

        log("Check Date field settings.");

        checker().verifyEquals("Format of Default Date is not as expected.",
                dateFormat, getFormatFromControl("defaultDateFormat"));

        if (null != dateInherited)
        {
            checker().verifyEquals("Inherited value for Date format not as expected.",
                    dateInherited, getFieldInherited("defaultDateFormatInherited"));
        }

        checker().verifyEquals("Non-standard warning for Date field not as expected.",
                dateWarning, getNonStandardWarning("defaultDateFormat"));


        log("Check Time field settings.");

        checker().verifyEquals("Format of Default Time is not as expected.",
                timeFormat, getFormatFromControl("defaultTimeFormat"));

        if (null != timeInherited)
        {
            checker().verifyEquals("Inherited value for Time format not as expected.",
                    timeInherited, getFieldInherited("defaultTimeFormatInherited"));
        }

        checker().verifyEquals("Non-standard warning for Time field not as expected.",
                timeWarning, getNonStandardWarning("defaultTimeFormat"));


        log("Check DateTime field settings.");

        checker().verifyEquals("Format of Default DateTime (Date) is not as expected.",
                dtDateFormat, getFormatFromControl("dateSelect"));

        checker().verifyEquals("Format of Default DateTime (Time) is not as expected.",
                dtTimeFormat, getFormatFromControl("timeSelect"));

        if (null != dateTimeInherited)
        {
            checker().verifyEquals("Inherited value for DateTime format not as expected.",
                    dateTimeInherited, getFieldInherited("defaultDateTimeFormatInherited"));
        }

        checker().verifyEquals("Non-standard warning for DateTime (Date) not as expected.",
                dtDateWarning, getNonStandardWarning("dateSelect"));

        checker().verifyEquals("Non-standard warning for DateTime (Time) not as expected.",
                dtTimeWarning, getNonStandardWarning("timeSelect"));

        checker().screenShotIfNewError("Settings_Page_Error");
    }

    // Private helper to check the Site Validation report. Checks to see if the report should, or should not, contain
    // the list of warnings.
    private void validateSiteValidationReport(String scope, List<String> expectedWarnings, boolean shouldContain)
    {
        List<String> actualWarnings = getProjectValidationWarnings(scope);

        log("Found Warnings: " + actualWarnings);

        if (shouldContain)
        {
            log("Should Contain Warnings: " + expectedWarnings);

            if (checker().withScreenshot()
                    .verifyFalse(String.format("Found no Site Validation warnings under '%s'.", scope),
                            actualWarnings.isEmpty()))
            {
                checker().verifyTrue("Did not find the expected warnings in the 'Site Validation' report.",
                        actualWarnings.containsAll(expectedWarnings));

            }
        }
        else
        {
            log("Should Not Contain Warnings: " + expectedWarnings);

            boolean shouldBeFalse = actualWarnings.stream()
                    .anyMatch(expectedWarnings::contains);

            checker().withScreenshot()
                    .verifyFalse("Found Site Validation warning(s) that should not be there.",
                            shouldBeFalse);
        }

        checker().screenShotIfNewError("Site_Validation_Error");
    }

    // Private helper that gets the data from the Site Validation page, narrowed to the scope provided, and cleans it up a bit.
    private List<String> getProjectValidationWarnings(String scope)
    {

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

        String xpath = String.format("//li[contains(text(),'%s')]//li[contains(text(),'Warnings:')]//ul", scope);
        WebElement ul = Locator.xpath(xpath).findWhenNeeded(getDriver());

        List<String> warnings = new ArrayList<>();
        int linkTextLength = " more info".length();

        if (ul.isDisplayed())
        {
            // Get the text of the warnings and trim off the 'More Info' link.
            warnings = ul.findElements(Locator.tag("li"))
                    .stream()
                    .map(el->el.getText().trim().substring(0, el.getText().length() - linkTextLength)).toList();
        }

        return warnings;
    }

    // Timezone description (PST vs. PDT) is dependent on the date.
    private String getTimezoneDesc(Date date)
    {
        boolean isDT = TIME_ZONE.inDaylightTime(date);
        return TIME_ZONE.getDisplayName(isDT, 0, Locale.getDefault());
    }

    // Timezone offset from GMT dependent on the date (covers daylight saving time).
    private String getTimezoneOffset(Date date)
    {
        return getTimezoneOffset(TIME_ZONE.getOffset(date.getTime()));
    }

    // Timezone offset from GMT using the raw offset
    private String getTimezoneOffset(int rawOffset)
    {
        return String.format("%+03d", rawOffset / 1000 / 60 / 60);
    }

}
