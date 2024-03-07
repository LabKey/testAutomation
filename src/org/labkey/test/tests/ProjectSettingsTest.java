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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.openqa.selenium.WebElement;

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

    private static final String DEFAULT_DATE_DISPLAY = "yyyy-MM-dd";
    private static final String DEFAULT_DATE_TIME_DISPLAY = "yyyy-MM-dd HH:mm";
    private static final String DEFAULT_TIME_DISPLAY = "HH:mm:ss";

    private static final String INJECT_CHARS = "<script>alert(\"8(\");</script>";
    private static final String DATE_TIME_FORMAT_INJECTION = DEFAULT_DATE_TIME_DISPLAY + "'" + INJECT_CHARS + "'";

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
                                        String dateFormat, String dateTimeFormat, String timeFormat)
    {

        if(helpMenu)
        {
            checker().verifyTrue("Help menu should be checked.",
                    settingsPage.getHelpMenu());
        }
        else
        {
            checker().verifyFalse("Help menu should not be checked.",
                    settingsPage.getHelpMenu());
        }

        if(supportLink.isEmpty())
        {
            checker().verifyTrue("Support link should be empty.",
                    settingsPage.getSupportLink().isEmpty());
        }
        else
        {
            checker().verifyEquals("Support link value not as expected.",
                    supportLink, settingsPage.getSupportLink());
        }

        if(dateFormat.isEmpty())
        {
            checker().verifyTrue("'Default Date Display' should be empty.",
                    settingsPage.getDefaultDateDisplay().isEmpty());
        }
        else
        {
            checker().verifyEquals("'Default Date Display'  not as expected.",
                    dateFormat, settingsPage.getDefaultDateDisplay());
        }

        if(dateTimeFormat.isEmpty())
        {
            checker().verifyTrue("'Default Time Display' should be empty.",
                    settingsPage.getDefaultDateTimeDisplay().isEmpty());
        }
        else
        {
            checker().verifyEquals("'Default Date Display'  not as expected.",
                    dateTimeFormat, settingsPage.getDefaultDateTimeDisplay());
        }

        if(timeFormat.isEmpty())
        {
            checker().verifyTrue("'Default Time Display' should be empty.",
                    settingsPage.getDefaultTimeDisplay().isEmpty());
        }
        else
        {
            checker().verifyEquals("'Default Time Display'  not as expected.",
                    timeFormat, settingsPage.getDefaultTimeDisplay());
        }

    }

    private void resetSiteSettings()
    {
        LookAndFeelSettingsPage settingsPage = LookAndFeelSettingsPage.beginAt(this);
        settingsPage.reset();
    }

    private void resetProjectSettings()
    {
        ProjectSettingsPage settingsPage = ProjectSettingsPage.beginAt(this, PROJ_CHANGE);
        settingsPage.reset();
    }

    @BeforeClass
    public static void setupProject()
    {
        ProjectSettingsTest init = (ProjectSettingsTest)getCurrentTest();
        init.setUpTest();
    }

    protected void setUpTest()
    {
        _containerHelper.deleteProject(PROJ_CHANGE, false);
        _containerHelper.deleteProject(PROJ_BASE, false);

        // Make sure the site settings are reset.
        resetSiteSettings();
        _containerHelper.createProject(PROJ_CHANGE, null);
        _containerHelper.createProject(PROJ_BASE, null);
    }

    private static final String DT_LIST_NAME = "Date_And_Time";
    private static final String DT_LIST_ID_COL = "id";
    private static final String DT_LIST_DATE_COL = "Date";
    private static final String DT_LIST_TIME_COL = "Time";
    private static final String DT_LIST_DATETIME_COL = "DateTime";

    private void createDateAndTimeList(String project, List<Map<String, String>> listData)
    {
        _listHelper.createList(project, DT_LIST_NAME, ListHelper.ListColumnType.AutoInteger, DT_LIST_ID_COL,
                new FieldDefinition(DT_LIST_DATE_COL, FieldDefinition.ColumnType.Date),
                new FieldDefinition(DT_LIST_TIME_COL, FieldDefinition.ColumnType.Time),
                new FieldDefinition(DT_LIST_DATETIME_COL, FieldDefinition.ColumnType.DateAndTime));


        // Use the default format to initially populate the lists.
        StringBuilder bulkImportData = new StringBuilder();
        bulkImportData.append(String.format("%s\t%s\t%s\n", DT_LIST_DATE_COL, DT_LIST_TIME_COL, DT_LIST_DATETIME_COL));

        for(Map<String, String> listRow : listData)
        {
            bulkImportData.append(String.format("%s\t%s\t%s\n",
                    listRow.get(DT_LIST_DATE_COL),
                    listRow.get(DT_LIST_TIME_COL),
                    listRow.get(DT_LIST_DATETIME_COL))
            );
        }

        _listHelper.bulkImportData(bulkImportData.toString());

    }

    @Test
    public void testSiteSettingOverride()
    {

        String siteDateDisplay = "MMMM dd, yyyy";
        String siteTimeDisplay = "hh:mm a";
        String siteDateTimeDisplay = "hh:mm a MMMM, dd yyyy";
        boolean siteHelpMenuState = false;
        String siteSupportLink = "";

        SimpleDateFormat defaultDateFormat = new SimpleDateFormat(DEFAULT_DATE_DISPLAY);
        SimpleDateFormat defaultTimeFormat = new SimpleDateFormat(DEFAULT_TIME_DISPLAY);
        SimpleDateFormat defaultDateTimeFormat = new SimpleDateFormat(DEFAULT_DATE_TIME_DISPLAY);

        SimpleDateFormat updatedDateFormat = new SimpleDateFormat(siteDateDisplay);
        SimpleDateFormat updateTimeFormat = new SimpleDateFormat(siteTimeDisplay);
        SimpleDateFormat updateDateTimeFormat = new SimpleDateFormat(siteDateTimeDisplay);

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

        log("Change global settings to exclude some menu links and change the format of date and time fields.");
        LookAndFeelSettingsPage settingsPage = goToSiteLookAndFeel();
        settingsPage.setHelpMenu(siteHelpMenuState);
        settingsPage.setSupportLink(siteSupportLink);
        settingsPage.setDefaultDateDisplay(siteDateDisplay);
        settingsPage.setDefaultTimeDisplay(siteTimeDisplay);
        settingsPage.setDefaultDateTimeDisplay(siteDateTimeDisplay);
        settingsPage.save();

        log("Go to the project settings page and validate that various settings match the site settings.");
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this, PROJ_CHANGE);
        checkSettingPageValues(projectSettingsPage, siteHelpMenuState, siteSupportLink,
                siteDateDisplay, siteDateTimeDisplay, siteTimeDisplay);

        log("Validate help and report links are missing from the menu in the project.");
        checkHelpLinks(PROJ_CHANGE, false, false);

        log("Validate format of the data in the list.");
        checkDataInList(PROJ_CHANGE, DT_LIST_NAME, datesUpdatedFormat);

        log("Validate help and report links are missing from the menu in the root project.");
        checkHelpLinks(null, false, false);

        log("Change settings in folder/project re-enable help and report options.");

        String supportLink = "${contextPath}/home/support/project-begin.view";

        projectSettingsPage = ProjectSettingsPage.beginAt(this, PROJ_CHANGE);
        projectSettingsPage.setHelpMenu(true);
        projectSettingsPage.setSupportLink(supportLink);
        projectSettingsPage.setDefaultDateDisplay(DEFAULT_DATE_DISPLAY);
        projectSettingsPage.setDefaultTimeDisplay(DEFAULT_TIME_DISPLAY);
        projectSettingsPage.setDefaultDateTimeDisplay(DEFAULT_DATE_TIME_DISPLAY);
        projectSettingsPage.save();

        log("Check 'help' and 'report' links are present in the menu in the project.");
        checkHelpLinks(PROJ_CHANGE, true, true);

        log(String.format("In '%s' validate format of data is back to site settings.", PROJ_CHANGE));
        checkDataInList(PROJ_CHANGE, DT_LIST_NAME, datesDefaultFormat);

        log("Check the settings and links in the second project.");
        projectSettingsPage = ProjectSettingsPage.beginAt(this, PROJ_BASE);

        checkSettingPageValues(projectSettingsPage, siteHelpMenuState, siteSupportLink,
                siteDateDisplay, siteDateTimeDisplay, siteTimeDisplay);

        log(String.format("In '%s' validate format of data is defined by the settings in the folder.", PROJ_BASE));
        checkDataInList(PROJ_BASE, DT_LIST_NAME, datesUpdatedFormat);

        checkHelpLinks(PROJ_BASE, false, false);

        resetSiteSettings();
        resetProjectSettings();
    }

    @Test
    public void testInjection()
    {
        resetSiteSettings();
        resetProjectSettings();

        var projectSettingPage = ProjectSettingsPage.beginAt(this, PROJ_CHANGE);
        projectSettingPage.setDefaultDateTimeDisplay(DATE_TIME_FORMAT_INJECTION);
        projectSettingPage.save();

        _listHelper.createList(getProjectName(), "IceCream", ListHelper.ListColumnType.AutoInteger, "IceCreamID",
                new ListHelper.ListColumn("IceCreamDate", "", ListHelper.ListColumnType.DateAndTime, ""));
        goToProjectHome();
        clickAndWait(Locator.linkWithText("IceCream"));
        Map<String, String> testRow = new HashMap<>();
        String testDate = "1800-05-10 10:32";
        testRow.put("IceCreamDate", testDate);
        _listHelper.insertNewRow(testRow);
        DataRegionTable list = new DataRegionTable("query", getDriver());
        String attemptedInjection = list.getDataAsText(0, 0);
        assertEquals("Wrong list data from injection attempt", testDate + INJECT_CHARS, attemptedInjection);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        resetSiteSettings();
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
