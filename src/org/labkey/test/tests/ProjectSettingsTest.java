/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.core.admin.BaseSettingsPage.DATE_FORMAT;
import org.labkey.test.pages.core.admin.BaseSettingsPage.TIME_FORMAT;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Category({Daily.class})
public class ProjectSettingsTest extends BaseWebDriverTest
{

    private static final DATE_FORMAT DEFAULT_DATE_FORMAT = DATE_FORMAT.Default;
    private static final TIME_FORMAT DEFAULT_TIME_FORMAT = TIME_FORMAT.Default;

    private static final String INJECT_CHARS = "<script>alert(\"8(\");</script>";

    private static final String PROJ_CHANGE = "Site Settings Test";
    private static final String PROJ_BASE = "Site Settings Base Test";

    @Override
    //this project will remain unaltered and copy every property from the site.
    protected String getProjectName()
    {
        return PROJ_CHANGE;
    }

    private LookAndFeelSettingsPage goToSiteLookAndFeel()
    {
        goToAdminConsole().goToSettingsSection().clickLookAndFeelSettings();
        return new LookAndFeelSettingsPage(getDriver());
    }

    private boolean checkHelpLinks(String projectName, boolean supportLinkPresent, boolean helpLinkPresent)
    {

        if (projectName != null)
        {
            goToProjectHome(projectName);
        }
        else
        {
            goToHome();
        }

        BootstrapMenu menu = new SiteNavBar(getDriver()).userMenu();
        menu.expand();    // the support and help links are now on the user menu
        List<WebElement> visibleLinks = menu.findVisibleMenuItems();
        checker().verifyEquals("Support link state unexpected.",
                supportLinkPresent, visibleLinks.stream().anyMatch((a)-> a.getText().equals("Support")));
        checker().verifyEquals("Help link state unexpected.",
                helpLinkPresent, visibleLinks.stream().anyMatch((a)-> a.getText().equals("LabKey Documentation")));

        // If there were no errors no screenshot will be taken, returning the "not" of the screenshot will return
        // true if there were no errors.
        return !checker().screenShotIfNewError("Menu_Error");
    }

    private boolean checkDataInList(String projectName, String listName, List<Map<String, String>> expectedValues)
    {
        goToProjectHome(projectName);

        Locator listLink = Locator.tagWithId("table", "lists").descendant("a").withText(listName);

        waitForElement(listLink, 10_000, true);
        clickAndWait(listLink);

        DataRegionTable actualValues = new DataRegionTable("query", getDriver());

        int rowIndex = 0;
        for(Map<String, String> listRow : expectedValues)
        {
            checker().verifyEquals(String.format("Row %d in list '%s' is not as expected.", rowIndex, listName),
                    listRow, actualValues.getRowDataAsMap(rowIndex));
            rowIndex++;
        }

        // checker().screenShotIfNewError returns true if it took a screenshot, which means there is an error.
        return !checker().screenShotIfNewError("List_Data_Error");
    }

    private void checkSettingPageValues(BaseSettingsPage settingsPage, boolean helpMenu, String supportLink,
                                        DATE_FORMAT dateFormat, DATE_FORMAT dtDateFormat,
                                        TIME_FORMAT dtTimeFormat, TIME_FORMAT timeFormat)
    {

        checker().verifyEquals("Help menu should be " + (helpMenu ? "checked." : "unchecked."),
                helpMenu, settingsPage.getHelpMenu());

        checker().verifyEquals("Support link value not as expected.",
                supportLink, settingsPage.getSupportLink());

        checker().verifyEquals("'Default Date Display'  not as expected.",
                dateFormat.toString(), settingsPage.getDefaultDateDisplay());

        checker().verifyEquals("'Default DateTime Date Display'  not as expected.",
                dtDateFormat.toString(), settingsPage.getDefaultDateTimeDateDisplay());

        checker().verifyEquals("'Default DateTime Time Display'  not as expected.",
                dtTimeFormat.toString(), settingsPage.getDefaultDateTimeTimeDisplay());

        checker().verifyEquals("'Default Time Display'  not as expected.",
                timeFormat.toString(), settingsPage.getDefaultTimeDisplay());

    }

    private void resetSiteSettings() throws IOException, CommandException
    {
        BaseSettingsPage.resetSettings(createDefaultConnection(), "/");
    }

    private void resetProjectSettings() throws IOException, CommandException
    {
        BaseSettingsPage.resetSettings(createDefaultConnection(), PROJ_CHANGE);
    }

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        ProjectSettingsTest init = (ProjectSettingsTest)getCurrentTest();
        init.setUpTest();
    }

    protected void setUpTest() throws IOException, CommandException
    {
        _containerHelper.deleteProject(PROJ_CHANGE, false);
        _containerHelper.deleteProject(PROJ_BASE, false);

        // Make sure the site settings are reset.
        resetSiteSettings();
        _containerHelper.createProject(PROJ_CHANGE, null);
        _containerHelper.createProject(PROJ_BASE, null);
    }

    @AfterClass
    public static void cleanUpAfter() throws IOException, CommandException
    {
        ((ProjectSettingsTest) getCurrentTest()).resetSiteSettings();
    }

    private static final String DT_LIST_NAME = "Date_And_Time";
    private static final String DT_LIST_ID_COL = "id";
    private static final String DT_LIST_DATE_COL = "Date";
    private static final String DT_LIST_TIME_COL = "Time";
    private static final String DT_LIST_DATETIME_COL = "DateTime";

    private void createDateAndTimeList(String project, List<Map<String, String>> listData) throws IOException, CommandException
    {

        ListDefinition listDef = new IntListDefinition(DT_LIST_NAME, DT_LIST_ID_COL);
        listDef.setFields(List.of(new FieldDefinition(DT_LIST_DATE_COL, ColumnType.Date),
                new FieldDefinition(DT_LIST_TIME_COL, ColumnType.Time),
                new FieldDefinition(DT_LIST_DATETIME_COL, ColumnType.DateAndTime)));

        TestDataGenerator tdg = listDef.create(createDefaultConnection(), project);

        for(Map<String, String> listRow : listData)
        {
            Map<String, Object> tmap = new HashMap<>(listRow);
            tdg.addCustomRow(tmap);
        }

        tdg.insertRows();

        goToProjectHome(project);
        new PortalHelper(getDriver()).addWebPart("Lists");

    }

    @Test
    public void testSiteSettingOverride() throws IOException, CommandException
    {

        DATE_FORMAT dateDisplay = DATE_FORMAT.yyyy_MMM_dd;
        TIME_FORMAT timeDisplay = TIME_FORMAT.hh_mm_a;
        DATE_FORMAT dtDateDisplay = DATE_FORMAT.dd_MMM_yy;
        TIME_FORMAT dtTimeDisplay = TIME_FORMAT.hh_mm_a;
        boolean siteHelpMenuState = false;
        String siteSupportLink = "";

        SimpleDateFormat defaultDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT.toString());
        SimpleDateFormat defaultTimeFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT.toString());
        SimpleDateFormat defaultDateTimeFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT + " " + DEFAULT_TIME_FORMAT);

        SimpleDateFormat updatedDateFormat = new SimpleDateFormat(dateDisplay.toString());
        SimpleDateFormat updateTimeFormat = new SimpleDateFormat(timeDisplay.toString());
        SimpleDateFormat updateDateTimeFormat = new SimpleDateFormat(String.format("%s %s", dtDateDisplay, dtTimeDisplay));

        Date testDate01 = new Calendar.Builder()
                .setDate(2023, 1, 17)
                .setTimeOfDay(11, 12, 03)
                .build().getTime();

        Date testDate02 = new Calendar.Builder()
                .setDate(2022, 6, 10)
                .setTimeOfDay(20, 45, 15)
                .build().getTime();

        List<Map<String, String>> datesDefaultFormat = new ArrayList<>();
        datesDefaultFormat.add(Map.of(
                DT_LIST_DATE_COL, defaultDateFormat.format(testDate01),
                DT_LIST_TIME_COL, defaultTimeFormat.format(testDate01),
                DT_LIST_DATETIME_COL, defaultDateTimeFormat.format(testDate01)
        ));
        datesDefaultFormat.add(Map.of(
                DT_LIST_DATE_COL, defaultDateFormat.format(testDate02),
                DT_LIST_TIME_COL, defaultTimeFormat.format(testDate02),
                DT_LIST_DATETIME_COL, defaultDateTimeFormat.format(testDate02)
        ));

        List<Map<String, String>> datesUpdatedFormat = new ArrayList<>();
        datesUpdatedFormat.add(Map.of(
                DT_LIST_DATE_COL, updatedDateFormat.format(testDate01),
                DT_LIST_TIME_COL, updateTimeFormat.format(testDate01),
                DT_LIST_DATETIME_COL, updateDateTimeFormat.format(testDate01)
        ));
        datesUpdatedFormat.add(Map.of(
                DT_LIST_DATE_COL, updatedDateFormat.format(testDate02),
                DT_LIST_TIME_COL, updateTimeFormat.format(testDate02),
                DT_LIST_DATETIME_COL, updateDateTimeFormat.format(testDate02)
        ));

        createDateAndTimeList(PROJ_CHANGE, datesDefaultFormat);
        createDateAndTimeList(PROJ_BASE, datesDefaultFormat);

        log("Change global settings to exclude some menu links and change the format of DateTime, Date and Time fields.");
        LookAndFeelSettingsPage settingsPage = goToSiteLookAndFeel();
        settingsPage.setHelpMenu(siteHelpMenuState);
        settingsPage.setSupportLink(siteSupportLink);
        settingsPage.setDefaultDateDisplay(dateDisplay);
        settingsPage.setDefaultTimeDisplay(timeDisplay);
        settingsPage.setDefaultDateTimeDisplay(dtDateDisplay, dtTimeDisplay);
        settingsPage.save();

        log("Go to the project settings page and validate that various settings match the site settings.");
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this, PROJ_CHANGE);
        checkSettingPageValues(projectSettingsPage, siteHelpMenuState, siteSupportLink,
                dateDisplay, dtDateDisplay, dtTimeDisplay, timeDisplay);

        log("Validate help and report links are missing from the menu in the project.");
        checkHelpLinks(PROJ_CHANGE, false, false);

        log("Validate format of the data in the list.");
        checkDataInList(PROJ_CHANGE, DT_LIST_NAME, datesUpdatedFormat);

        log("Validate help and report links are missing from the menu in the root project.");
        checkHelpLinks(null, false, false);

        log("Change settings in folder/project re-enable help and report options.");

        String supportLink = "${contextPath}/home/support/project-begin.view";

        projectSettingsPage = ProjectSettingsPage.beginAt(this, PROJ_CHANGE);

        projectSettingsPage.setHelpMenuInherited(false);
        projectSettingsPage.setHelpMenu(true);

        projectSettingsPage.setSupportLinkInherited(false);
        projectSettingsPage.setSupportLink(supportLink);

        projectSettingsPage.setDefaultDateDisplayInherited(false);
        projectSettingsPage.setDefaultDateDisplay(DATE_FORMAT.yyyy_MM_dd);

        projectSettingsPage.setDefaultTimeDisplayInherited(false);
        projectSettingsPage.setDefaultTimeDisplay(DEFAULT_TIME_FORMAT);

        projectSettingsPage.setDefaultDateTimeDisplayInherited(false);
        projectSettingsPage.setDefaultDateTimeDisplay(DEFAULT_DATE_FORMAT, DEFAULT_TIME_FORMAT);

        projectSettingsPage.save();

        log("Check 'help' and 'report' links are present in the menu in the project.");
        checkHelpLinks(PROJ_CHANGE, true, true);

        log(String.format("In '%s' validate format of data is back to site settings.", PROJ_CHANGE));
        checkDataInList(PROJ_CHANGE, DT_LIST_NAME, datesDefaultFormat);

        log("Check the settings and links in the second project.");
        projectSettingsPage = ProjectSettingsPage.beginAt(this, PROJ_BASE);

        checkSettingPageValues(projectSettingsPage, siteHelpMenuState, siteSupportLink,
                dateDisplay, dtDateDisplay, dtTimeDisplay, timeDisplay);

        log(String.format("In '%s' validate format of data is defined by the settings in the folder.", PROJ_BASE));
        checkDataInList(PROJ_BASE, DT_LIST_NAME, datesUpdatedFormat);

        checkHelpLinks(PROJ_BASE, false, false);

        resetSiteSettings();
        resetProjectSettings();
    }

    @Test
    public void testInjection() throws IOException, CommandException
    {
        resetSiteSettings();
        resetProjectSettings();

        String dateFormatInjection = DATE_FORMAT.yyyy_MM_dd.toString() + "'" + INJECT_CHARS + "'";
        String timeFormatInjection = TIME_FORMAT.HH_mm.toString() + "'" + INJECT_CHARS + "'";

        new APIContainerHelper(this).setNonStandardDateAndTimeFormat(createDefaultConnection(), PROJ_CHANGE,
                dateFormatInjection, timeFormatInjection, String.format("%s %s", dateFormatInjection, timeFormatInjection));

        var projectSettingPage = ProjectSettingsPage.beginAt(this, PROJ_CHANGE);
        log("DateTime format: " + projectSettingPage.getDefaultDateTimeDateDisplay() + " " + projectSettingPage.getDefaultDateTimeTimeDisplay());
        log("Date format: " + projectSettingPage.getDefaultDateDisplay());
        log("Time format: " + projectSettingPage.getDefaultTimeDisplay());

        _listHelper.createList(getProjectName(), "IceCream", "IceCreamID",
                new FieldDefinition("IceCreamDateTime", ColumnType.DateAndTime),
                new FieldDefinition("IceCreamDate", ColumnType.Date),
                new FieldDefinition("IceCreamTime", ColumnType.Time));

        goToProjectHome();
        clickAndWait(Locator.linkWithText("IceCream"));
        Map<String, String> testRow = new HashMap<>();
        String testDate = "1800-05-10";
        String testTime = "10:32";
        String testDateTime = String.format("%s %s", testDate, testTime);
        testRow.put("IceCreamDateTime", testDateTime);
        testRow.put("IceCreamDate", testDate);
        testRow.put("IceCreamTime", testTime);
        _listHelper.insertNewRow(testRow);
        DataRegionTable list = new DataRegionTable("query", getDriver());
        String attemptedInjection = list.getDataAsText(0, 0);
        assertEquals("Wrong list data from DateTime injection attempt", testDate + INJECT_CHARS + " " + testTime + INJECT_CHARS, attemptedInjection);
        attemptedInjection = list.getDataAsText(0, 1);
        assertEquals("Wrong list data from Date injection attempt", testDate + INJECT_CHARS, attemptedInjection);
        attemptedInjection = list.getDataAsText(0, 2);
        assertEquals("Wrong list data from Time injection attempt", testTime + INJECT_CHARS, attemptedInjection);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(PROJ_CHANGE, false);
        _containerHelper.deleteProject(PROJ_BASE, false);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
