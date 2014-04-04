/*
 * Copyright (c) 2011-2014 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Data;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**conceptually filter and list are separate, but
 * it was convenient to use the list test helpers for filter
 */
@Category({BVT.class, Data.class})
public class FilterTest extends ListTest
{
    protected final static String PROJECT_NAME = "FilterVerifyProject";
    protected final static String R_VIEW = TRICKY_CHARACTERS + "R view";
    protected final static String FACET_TEST_LIST = "FacetList";
    protected String listUrl = "";

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {
        setupProject();

        createList();
        filterTest();

        createList2();
        facetedFilterTest();
        maskedFacetTest();
        containerFilterFacetTest();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProject()
    {
        RReportHelper _RReportHelper = new RReportHelper(this);
        _RReportHelper.ensureRConfig();

        _containerHelper.createProject(PROJECT_NAME, null);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    void createList()
    {
        StringBuilder testDataFull = new StringBuilder();
        testDataFull.append(StringUtils.join(Arrays.asList(LIST_KEY_NAME2, _listCol1.getName(), _listCol2.getName(), _listCol3.getName(), _listCol4.getName(), _listCol6.getName()), "\t"));
        testDataFull.append("\n");
        for (int i=0 ; i<TEST_DATA[0].length ; i++)
        {
            testDataFull.append(StringUtils.join(Arrays.asList(
                    TEST_DATA[0][i],
                    TEST_DATA[1][i],
                    CONVERTED_MONTHS[i],
                    TEST_DATA[2][i],
                    TEST_DATA[4][i],
                    (i==TEST_DATA[0].length-1) ? "" : TEST_DATA[5][i] // NOTE make last row blank
                    ),"\t"));
            testDataFull.append("\n");
        }

        log("Add list -- " + LIST_NAME_COLORS);
        _listHelper.createList(PROJECT_NAME, LIST_NAME_COLORS, LIST_KEY_TYPE, LIST_KEY_NAME2, _listCol1, _listCol2, _listCol3, _listCol4, _listCol5, _listCol6);
        log("Set title field of 'Colors' to 'Desc'");
        clickEditDesign();
        selectOptionByText(Locator.id("ff_titleColumn"), "Desc");
        clickDone();
        clickAndWait(Locator.linkWithText(LIST_NAME_COLORS));
        listUrl = getCurrentRelativeURL();
        clickImportData();
        setFormElement(Locator.name("text"), testDataFull.toString());
        submitImportTsv();

        _customizeViewsHelper.createRView(null, R_VIEW);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void createList2()
    {
        ListHelper.ListColumn yearColumn = new ListHelper.ListColumn("year", "year", ListHelper.ListColumnType.Integer, "");
        _listHelper.createList(PROJECT_NAME, FACET_TEST_LIST, LIST2_KEY_TYPE, LIST2_KEY_NAME, _list2Col1, yearColumn);
        clickButton("Import Data");
        setFormElement(Locator.name("text"), "Car\tColor\tyear\n" +
                "1\tBlue\t1980\n" +
                "2\tRed\t1970\n" +
                "3\tYellow\t1990\n");

        clickButton("Submit", 0);
        waitForElement(Locator.id("labkey-nav-trail-current-page").withText(FACET_TEST_LIST));
    }

    /**
     * Issue 16821:  Create additional tests for behavior of URL filters w/ empty strings
     * https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=16281
     * Filters are represented in the page URL.  Because users can save these URLs for reference, it is
     * important they be consistent.  To that end, this test will hit various URLs directly
     * and virify the appropriate data is displayed
     */
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void directUrlTest()
    {
        List<FilterArgs> args = generateValidFilterByUrlArgsAndResponses();
//        FilterArgs  a = args.get(0);
//        beginAt(listUrl + "&query.Good~in=7%3B8");
        int count = 0;
        for(FilterArgs a : args)
        {
            log("Loop count: " + count++);
            validFilterGeneratesCorrectResultsTest(a);
        }


    }

    protected void startFilter(String column)
    {
        click(Locator.tagWithText("div", column));
        waitAndClick(Locator.tagWithText("span", "Filter...").notHidden());
        _extHelper.waitForExtDialog("Show Rows Where " + column + "...");
        waitForElement(Locator.tag("div").withClass("labkey-filter-dialog").append("//tr").withClass("x-grid3-row-table").withPredicate(Locator.tag("a").withText("[All]")).append("//div").withClass("x-grid3-hd-checker-on"));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void facetedFilterTest()
    {
        verifyColumnValues("query", "Color", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");
        startFilter("Color");

        log("Verifying expected faceted filter elements present");
        verifyTextPresentInFilterDialog("Choose Filters", "Choose Values", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");

        _extHelper.clickExtButton("OK");
        verifyColumnValues("query", "Color", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");

        setFacetedFilter("query", "Color", "ZanzibarMasinginiTanzaniaAfrica");
        verifyColumnValues("query", "Color", "ZanzibarMasinginiTanzaniaAfrica");

        setUpFacetedFilter("query", "Color", "Robust");
        _extHelper.clickExtTab("Choose Filters");
        //NOTE: the filter will optimize this to EQUALS, since there is 1 value
        waitForFormElementToEqual(Locator.name("filterType_1"), "Equals");
        assertEquals("Faceted -> logical filter conversion failure", "Robust", getFormElement(Locator.name("value_1")));
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtButton("OK");
        verifyColumnValues("query", "Color", "Robust");

        // Issue 14710: Switching between faceted and logical filters breaks dialog
        setUpFacetedFilter("query", "Color", "Robust", "Light");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Does Not Equal");
        assertEquals("Faceted -> logical filter conversion failure", "ZanzibarMasinginiTanzaniaAfrica", getFormElement(Locator.name("value_1")));
        _extHelper.selectComboBoxItem("Filter Type:", "Is Blank");
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Is Blank");
        clickButton("Clear Filter");
        //the change above would result in filters being dropped.
        verifyColumnValues("query", "Color", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");

        //now repeat with a filter that should be translated
        setUpFacetedFilter("query", "Color", "Light");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Equals");
        assertEquals("Faceted -> logical filter conversion failure", "Light", getFormElement(Locator.name("value_1")));

        setFormElement(Locator.name("value_1"), "Light;Robust");

        _extHelper.clickExtTab("Choose Values"); //we should get no alerts
        clickButton("Clear Filter");
        setUpFacetedFilter("query", "Color", "Light", "Robust");
        _extHelper.clickExtButton("OK");
        verifyColumnValues("query", "Color", "Light", "Robust");
        verifyColumnValues("query", "year", "1980", "1970");

        setFacetedFilter("query", "Color");
        verifyColumnValues("query", "Color", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");

        log("Verifying faceted filter on non-lookup column");
        startFilter("year");

        verifyTextPresentInFilterDialog("Choose Filters", "Choose Values", "1980", "1990", "1970");

        _extHelper.clickExtButton("OK");
        verifyColumnValues("query", "year", "1980", "1970", "1990");

        setFacetedFilter("query", "year", "1980");
        verifyColumnValues("query", "year", "1980");

        setUpFacetedFilter("query", "year", "1990");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Equals");
        assertEquals("Faceted -> logical filter conversion failure", "1990", getFormElement(Locator.name("value_1")));
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtButton("OK");
        verifyColumnValues("query", "year", "1990");

        setUpFacetedFilter("query", "year", "1990", "1980");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Does Not Equal");
        assertEquals("Faceted -> logical filter conversion failure", "1970", getFormElement(Locator.name("value_1")));
        _extHelper.selectComboBoxItem("Filter Type:", "Is Blank");
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtButton("OK");
        verifyColumnValues("query", "year", "1980", "1970", "1990");

        setFacetedFilter("query", "year");
        verifyColumnValues("query", "year", "1980", "1970", "1990");
    }

    @LogMethod
    private void maskedFacetTest()
    {
        setFacetedFilter("query", "year", "1980", "1990");
        verifyFacetOptions("Car", "1", "3");

        setFacetedFilter("query", "Car", "1");
        verifyFacetOptions("Color", "Light");

        clearAllFilters("query", "Car");
        setFilter("query", "year", "Is Greater Than", "1980");
        verifyFacetOptions("Car", "3");
    }

    @LogMethod
    private void containerFilterFacetTest()
    {
        IssuesHelper issuesHelper = new IssuesHelper(this);
        goToModule("Issues");
        issuesHelper.goToAdmin();
        issuesHelper.setIssueAssignmentList("Site:Administrators");
        issuesHelper.addPickListOption("Type", "typea");
        issuesHelper.addPickListOption("Type", "typeb");

        HashMap<String, String> projectIssue = new HashMap<>();
        projectIssue.put("title", "project issue1");
        projectIssue.put("assignedTo", displayNameFromEmail(PasswordUtil.getUsername()));
        projectIssue.put("type", "typea");
        projectIssue.put("priority", "1");
        issuesHelper.addIssue(projectIssue);
        HashMap<String, String> projectIssue2 = new HashMap<>();
        projectIssue2.put("title", "project issue2");
        projectIssue2.put("assignedTo", displayNameFromEmail(PasswordUtil.getUsername()));
        projectIssue2.put("type", "typeb");
        projectIssue2.put("priority", "2");
        issuesHelper.addIssue(projectIssue2);

        _containerHelper.createSubfolder(getProjectName(), "subfolder", null);
        clickProject(getProjectName());
        clickFolder("subfolder");

        goToModule("Issues");
        issuesHelper.goToAdmin();
        issuesHelper.setIssueAssignmentList("Site:Administrators");
        issuesHelper.addPickListOption("Type", "typed");
        issuesHelper.addPickListOption("Type", "typee");

        HashMap<String, String> subfolderIssue = new HashMap<>();
        subfolderIssue.put("title", "subfolder issue1");
        subfolderIssue.put("assignedTo", displayNameFromEmail(PasswordUtil.getUsername()));
        subfolderIssue.put("type", "typed");
        subfolderIssue.put("priority", "3");
        issuesHelper.addIssue(subfolderIssue);
        HashMap<String, String> subfolderIssue2 = new HashMap<>();
        subfolderIssue2.put("title", "subfolder issue2");
        subfolderIssue2.put("assignedTo", displayNameFromEmail(PasswordUtil.getUsername()));
        subfolderIssue2.put("type", "typee");
        subfolderIssue2.put("priority", "4");
        issuesHelper.addIssue(subfolderIssue2);

        clickProject(getProjectName());
        goToModule("Issues");

        assertElementPresent(Locator.linkWithText(projectIssue.get("title")));
        assertElementPresent(Locator.linkWithText(projectIssue2.get("title")));
        assertElementNotPresent(Locator.linkWithText(subfolderIssue.get("title")));
        assertElementNotPresent(Locator.linkWithText(subfolderIssue2.get("title")));

        _extHelper.clickMenuButton("Views", "Folder Filter", "Current folder and subfolders");
        assertElementPresent(Locator.linkWithText(projectIssue.get("title")));
        assertElementPresent(Locator.linkWithText(projectIssue2.get("title")));
        assertElementPresent(Locator.linkWithText(subfolderIssue.get("title")));
        assertElementPresent(Locator.linkWithText(subfolderIssue2.get("title")));

        verifyFacetOptions("Type",
                projectIssue.get("type"),
                projectIssue2.get("type"),
                subfolderIssue.get("type"),
                subfolderIssue2.get("type"));

        verifyFacetOptions("Pri",
                projectIssue.get("priority"),
                projectIssue2.get("priority"),
                subfolderIssue.get("priority"),
                subfolderIssue2.get("priority"));

        setFacetedFilter("Issues", "Priority", projectIssue2.get("priority"), subfolderIssue.get("priority"));
        assertElementNotPresent(Locator.linkWithText(projectIssue.get("title")));
        assertElementPresent(Locator.linkWithText(projectIssue2.get("title")));
        assertElementPresent(Locator.linkWithText(subfolderIssue.get("title")));
        assertElementNotPresent(Locator.linkWithText(subfolderIssue2.get("title")));

        verifyFacetOptions("Type",
                projectIssue2.get("type"),
                subfolderIssue.get("type"));
    }

    private void verifyFacetOptions(String column, String... options)
    {
        startFilter(column);
        verifyOptionsInFilterDialog(options);
        _extHelper.clickExtButton("CANCEL", 0);}

    private void verifyColumnValues(String dataregion, String columnName, String... expectedValues)
    {
        List<String> expectedList = Arrays.asList(expectedValues);
        assertEquals(expectedList, new DataRegionTable(dataregion, this).getColumnDataAsText(columnName));
    }

    private void verifyTextPresentInFilterDialog(String... texts)
    {
        Locator loc = Locator.xpath("//div[contains(@class, 'labkey-filter-dialog')]");
        String filterDialogText = getText(loc);

        for (String text : texts)
            assertEquals("'" + text + "' not found", 1, StringUtils.countMatches(filterDialogText, text));
    }

    private void verifyOptionsInFilterDialog(String... options)
    {
        String expectedOptions = StringUtils.join(options, "\n");
        String actualOptions = getText(Locator.css(".labkey-filter-dialog .x-grid3-body")).replaceAll(" ", "");

        assertEquals("Unexpected filter options", expectedOptions, actualOptions);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void filterTest()
    {
        validFiltersGenerateCorrectResultsTest();

        invalidFiltersGenerateCorrectErrorTest();

        filterCancelButtonWorksTest();
    }

    @LogMethod
    private void invalidFiltersGenerateCorrectErrorTest()
    {
        String[][] testArgs = generateInvalidFilterTestArgs();

        for(String[] argSet : testArgs)
        {
            invalidFiltersGenerateCorrectErrorTest(argSet[0], argSet[1],
                argSet[2], argSet[3], argSet[4]);
        }

    }

    @LogMethod
    private void invalidFiltersGenerateCorrectErrorTest(String
                        regionName, String columnName, String filterType,
                        String filterValue, String expectedError)
    {
        log("attempt to set filter column: " + columnName + ". With filter type: " + filterType + ".  And fitler value: " + filterValue);
        if(filterType==null)
            setFilter(regionName, columnName, filterType);
        else
            setUpFilter(regionName, columnName, filterType,
                filterValue);
        sleep(300);
        //pressEnter("//input[@id='value_1']");
        clickButton("OK",0);
        assert(isElementPresent(Locator.extButton("OK")));
//        assert(!isElementPresent(Locator.extButtonEnabled("OK")));
        assertTextPresent(expectedError);

        clickButton("CANCEL", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

    }

    public static final String TABLE_NAME = "query";
    public static final String EMPTY_FILTER_VAL_ERROR_MSG = "You must enter a value";

    private String[][] generateInvalidFilterTestArgs()
    {
        String[][] args = {
                {TABLE_NAME, getBooleanColumnName(), "Equals", "foo", "foo is not a valid boolean"},
                {TABLE_NAME, getStringColumnName(), "Equals", "", EMPTY_FILTER_VAL_ERROR_MSG},
                {TABLE_NAME, getDateColumnName(), "Equals", TRICKY_CHARACTERS, TRICKY_CHARACTERS + " is not a valid date"},
                {TABLE_NAME, getIntColumnName(), "Equals", "ab123", "Invalid value: ab123"},
                {TABLE_NAME, getIntColumnName(), "Equals", "", EMPTY_FILTER_VAL_ERROR_MSG},
        };

        return args;
    }

    private String getStringColumnName()
    {
        return _listCol1.getName();
    }

    private String getBooleanColumnName()
    {
        return _listCol3.getName();
    }


    private String getDateColumnName()
    {
        return _listCol2.getName();
    }

    private String getIntColumnName()
    {
        return _listCol4.getName();
    }

    @LogMethod
    public void validFiltersGenerateCorrectResultsTest()
    {
        List<FilterArgs> testArgs = generateValidFilterArgsAndResponses();

        for (FilterArgs a : testArgs)
        {
            validFilterGeneratesCorrectResultsTest(a);
        }
    }

    private static class FilterArgs
    {
        public String columnName;
        public String filter1Type;
        public String filter1Value;
        public String filter2Type;
        public String filter2Value;
        public String filterUrl;

        public String[] present;
        public String[] notPresent;

//        public FilterArgs(String columnName,
//                          String filter1Type, @Nullable String filter1Value,
//                          @Nullable String filter2Type, @Nullable String filter2Value,
//                          String[] present, String[] notPresent)
//        {
//            this(columnName, filter1Type, filter1Value, filter2Value, filter2Value, present, notPresent, null);
//            this.columnName = columnName;
//            this.filter1Type = filter1Type;
//            this.filter1Value = filter1Value;
//            this.filter2Type = filter2Type;
//            this.filter2Value = filter2Value;
//            this.present = present;
//            this.notPresent = notPresent;
//        }

        public FilterArgs(String columnName,
                          String filter1Type, @Nullable String filter1Value,
                          @Nullable String filter2Type, @Nullable String filter2Value,
                          String[] present, String[] notPresent, String filterUrl)
        {
            this.columnName = columnName;
            this.filter1Type = filter1Type;
            this.filter1Value = filter1Value;
            this.filter2Type = filter2Type;
            this.filter2Value = filter2Value;
            this.present = present;
            this.notPresent = notPresent;
            this.filterUrl = filterUrl;
        }
    }

    public static FilterArgs FilterArgs(String columnName,
                             String filter1Type, @Nullable String filter1Value,
                             @Nullable String filter2Type, @Nullable String filter2Value,
                             String[] present, String[] notPresent)
    {
        return new FilterArgs(columnName, filter1Type, filter1Value, filter2Type, filter2Value, present, notPresent, null);
    }


    public static FilterArgs FilterArgs(String columnName,
                             String filter1Type, @Nullable String filter1Value,
                             @Nullable String filter2Type, @Nullable String filter2Value,
                             String[] present, String[] notPresent, String url)
    {
        return new FilterArgs(columnName, filter1Type, filter1Value, filter2Type, filter2Value, present, notPresent, url);
    }

    private List<FilterArgs> generateValidFilterByUrlArgsAndResponses()
    {
            return Arrays.asList(
                    //String columnName, String filter1Type, String filter1, String filter2Type, String filter2, String[] textPresentAfterFilter, String[] textNotPresentAfterFilter,
                    //Issue 12197
                    new FilterArgs(_listCol6.getName(), "= ", "NULL",  null, null, new String[] {TEST_DATA[1][3]}, new String[] {TEST_DATA[1][0], TEST_DATA[1][1], TEST_DATA[1][2]}, listUrl + "&query.Aliased%24CColumn~eq="),
                    new FilterArgs(_listCol6.getName(), "<> ", "NULL",  null, null, new String[] {TEST_DATA[1][0], TEST_DATA[1][1], TEST_DATA[1][2]}, new String[] {TEST_DATA[1][3]}, listUrl + "&query.Aliased%24CColumn~neq="),
                    new FilterArgs(_listCol6.getName(), "Equals One Of", "BLANK",  null, null, new String[] {TEST_DATA[1][3]}, new String[] {TEST_DATA[1][0], TEST_DATA[1][1], TEST_DATA[1][2]}, listUrl + "&query.Aliased%24CColumn~in=%3B"),
                    new FilterArgs(_listCol6.getName(), "IS NOT ANY OF ", "(BLANK)",  null, null, new String[] {TEST_DATA[1][0], TEST_DATA[1][1], TEST_DATA[1][2]}, new String[] {TEST_DATA[1][3]}, listUrl + "&query.Aliased%24CColumn~notin=%3B"),
                    new FilterArgs(_listCol6.getName(), "IS ", "NULL",  null, null, new String[] {TEST_DATA[1][3]}, new String[] {TEST_DATA[1][0]}, listUrl + "&query.Aliased%24CColumn~isblank"),
                    new FilterArgs(_listCol4.getName(), "IS NOT ", "NULL",  null, null, new String[] {}, TEST_DATA[1], listUrl + "&query.HiddenColumn~isnonblank"),
                    FilterArgs(_listCol4.getName(), "Equals One Of (e.g. \"a;b;c\")", TEST_DATA[4][3] + ";" + TEST_DATA[4][2], null, null, new String[]{TEST_DATA[1][2], TEST_DATA[1][3]}, new String[]{TEST_DATA[1][0], TEST_DATA[1][1]}, listUrl + "&query.Good~in=7%3B8")
            );
    }

    private List<FilterArgs> generateValidFilterArgsAndResponses()
        {
            return Arrays.asList(
                    //String columnName, String filter1Type, String filter1, String filter2Type, String filter2, String[] textPresentAfterFilter, String[] textNotPresentAfterFilter,
                    //Issue 12197
                    FilterArgs(_listCol4.getName(), "Equals One Of (e.g. \"a;b;c\")", TEST_DATA[4][3] + ";" + TEST_DATA[4][2], null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][3]}, new String[] {TEST_DATA[1][0],TEST_DATA[1][1]}),
                    FilterArgs(_listCol1.getName(), "Equals", TEST_DATA[1][0], null, null, new String[] {TEST_DATA[1][0]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][3]}),
                    FilterArgs(_listCol1.getName(), "Starts With", "Z", null, null, new String[] {TEST_DATA[1][3]}, new String[] {TEST_DATA[1][0],TEST_DATA[1][1],TEST_DATA[1][2]}),
                    FilterArgs(_listCol1.getName(), "Does Not Start With", "Z", null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][0]}, new String[] {TEST_DATA[1][3]}),
                    //can't check for the absence of thing you're excluding, since it will be present in the filter text
                    FilterArgs(_listCol1.getName(), "Does Not Equal", TEST_DATA[1][0], null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][3]}, new String[] {TEST_DATA[5][0]}),
                    FilterArgs(_listCol1.getName(), "Does Not Equal Any Of (e.g. \"a;b;c\")", TEST_DATA[1][0] + ";" + TEST_DATA[1][1], null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][3]}, new String[] {TEST_DATA[5][0], TEST_DATA[5][1]}),
                    FilterArgs(_listCol3.getName(), "Equals", "true", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][2]}, new String[] {TEST_DATA[1][1],TEST_DATA[1][3]}),
                    FilterArgs(_listCol3.getName(), "Does Not Equal", "false", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][2]}, new String[] {TEST_DATA[1][1],TEST_DATA[1][3]}),
                    //filter is case insensitive
                    FilterArgs(_listCol6.getName(), "Contains", "e", "Contains", "r", new String[] {TEST_DATA[5][2],TEST_DATA[5][0], TEST_DATA[5][1]}, new String[] {TEST_DATA[1][3]}),
    //                FilterArgs(_listCol2.getName(), "Is Greater Than", "2", "Is Less Than or Equal To", "4", new String[] {TEST_DATA[1][2]}, new String[] {TEST_DATA[1][0],TEST_DATA[1][1],TEST_DATA[1][3]}),
                    FilterArgs(_listCol4.getName(), "Is Greater Than or Equal To", "9", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][1]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3]}),
                    FilterArgs(_listCol4.getName(), "Is Greater Than", "9", null, null, new String[] {TEST_DATA[1][0]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3],TEST_DATA[1][1]}),
                    FilterArgs(_listCol4.getName(), "Is Blank", null, null, null, new String[] {}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3],TEST_DATA[1][1], TEST_DATA[1][0]}),
                    //new filters for faceted filtering
                    FilterArgs(_listCol6.getName(),  "Contains One Of (e.g. \"a;b;c\")", TEST_DATA[5][1] + ";" + TEST_DATA[5][3], null, null, new String[] {TEST_DATA[5][1]}, new String[] {TEST_DATA[1][0], TEST_DATA[1][2]}),
                    FilterArgs(_listCol1.getName(), "Does Not Contain Any Of (e.g. \"a;b;c\")", TEST_DATA[1][3] + ";" + TEST_DATA[1][1], null, null, new String[] {TEST_DATA[1][0], TEST_DATA[1][2]}, new String[] {TEST_DATA[0][1] , TEST_DATA[0][3]}),
                    FilterArgs(_listCol6.getName(), "Is Blank", null, null, null, new String[] {TEST_DATA[1][3]}, new String[] {TEST_DATA[1][1] ,TEST_DATA[1][2], TEST_DATA[1][0]}),
                    FilterArgs(_listCol6.getName(), "Is Not Blank", null, null, null, new String[] {TEST_DATA[1][1] ,TEST_DATA[1][2], TEST_DATA[1][0]}, new String[] {TEST_DATA[1][3]})
            );
        }

    //Issue 12787: Canceling filter dialog requires two clicks
    private void filterCancelButtonWorksTest()
    {
        String id = EscapeUtil.filter(TABLE_NAME + ":" + _listCol4.getName() + ":filter");
        runMenuItemHandler(id);

        clickButton("CANCEL", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

        assertTextNotPresent("Show Rows Where");
    }

    private void validFilterGeneratesCorrectResultsTest(FilterArgs a)
    {
            validFilterGeneratesCorrectResultsTest(
                    a.columnName,
                    a.filter1Type, a.filter1Value,
                    a.filter2Type, a.filter2Value,
                    a.present, a.notPresent.clone(), a.filterUrl);
    }

    @LogMethod
    private void validFilterGeneratesCorrectResultsTest(String columnName, String filter1Type, String filter1, String filter2Type, String filter2,
            String[] textPresentAfterFilter, String[] textNotPresentAfterFilter, String url)
    {
        String fieldKey = EscapeUtil.fieldKeyEncodePart(columnName);
        if (null == url)
        {
            log("** Filtering " + columnName + " with filter type: " + filter1Type + ", value: " + filter1);
            if (null != filter2Type)
                log("** Second filter: " + filter2Type + ".  value:" + filter2);
            setFilter(TABLE_NAME, fieldKey, filter1Type, filter1, filter2Type, filter2);

            _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
            checkFilterWasApplied(textPresentAfterFilter, textNotPresentAfterFilter, columnName, filter1Type, filter1, filter2Type, filter2);

            log("** Checking filter present in R view");
            clickMenuButton("Views", R_VIEW);
        }
        else
            beginAt(url);


        _ext4Helper.waitForMaskToDisappear();
        checkFilterWasApplied(textPresentAfterFilter, textNotPresentAfterFilter, columnName, filter1Type, filter1, filter2Type, filter2);

        if(url==null)
        {

            clickMenuButton("Views", "default");

            //open filter
            log("** Checking filter values in filter dialog");
            runMenuItemHandler(TABLE_NAME + ":" + fieldKey + ":filter");
            _extHelper.waitForExtDialog("Show Rows Where ");
            waitFor(new Checker()
            {
                @Override
                public boolean check()
                {
                    return isElementPresent(Locator.linkWithText("[All]")) ||
                           isElementPresent(Locator.xpath("//li").withClass("x-tab-strip-active").containing("Choose Filters"));
                }
            }, "Filter dialog loading failed", WAIT_FOR_JAVASCRIPT);

            _extHelper.clickExtTab("Choose Filters");
            shortWait().until(ExpectedConditions.visibilityOf(Locator.id("value_1").findElement(getDriver())));

        if (filter1 != null)
        {
            // When we first load the filter panel, we convert single-value filters into a multi-value filter if possible,
            // then we invert negative filters ("Does Not Equal" becomes "In") and invert the values.
            // When switching to the 'Choose Filters' tab, we may invert again (if more than half of the values are selected)
            // and we may change a multi-value filter into a singluar filter if only one value is selected.

            if (filter1Type.equals("Does Not Equal") && "Light".equals(filter1))
            {
                // In this test case, "Does Not Equal" and "Light" are the initial filter type and value.
                // When showing the dialog, "Does Not Equal" is first converted into "Not In" since "Does Not Equal" is a single value filter.
                // Next, it is inverted from "Not In" to "In" and "Mellow;Robust;ZanzibarMasinginiTanzaniaAfrica" are selected.
                // When switching tabs, the number of selected values (1) is less than half of the available values (4),
                // so the filter is inverted again from "Not In" to "Does Not Equal Any Of" and "Light" is selected.
                //waitForFormElementToEqual(Locator.name("filterType_1"), "Does Not Equals Any Of (e.g. \"a;b;c\")");
                waitForFormElementToEqual(Locator.name("value_1"), "Light");
            }
            else if (filter1Type.equals("Does Not Equal Any Of (e.g. \"a;b;c\")") && "Light;Mellow".equals(filter1))
            {
                // In this test case, "Does Not Equal Any Of" and "Light;Mellow" are the initial filter type and value.
                // When showing the dialog, "Does Not Equal Any Of" is inverted to "In" and "Robust;ZanzibarMasinginiTanzaniaAfrica" are selected.
                // When switching tabs, nothing changes.
                //waitForFormElementToEqual(Locator.name("filterType_1"), "Equals One Of (e.g. \"a;b;c\")");
                waitForFormElementToEqual(Locator.name("value_1"), "Light;Mellow");
            }
            else if (filter1Type.equals("Does Not Equal") && "false".equals(filter1))
            {
                // In this test case, "Does Not Equal" and "false" are the initial filter type and value.
                // When showing the dialog "Does Not Equal" is inverted as "In" and "true" is selected.
                // When switching tabs, the filter is simplified from "In" to "Equal" because only a single value, "true", is selected.
                if (getFormElement(Locator.name("filterType_1")).equals("Equals"))
                    assertFormElementEquals(Locator.id("value_1"), "true");
                else
                    assertFormElementEquals(Locator.id("value_1"), filter1);
            }
            else
            {
                assertFormElementEquals(Locator.id("value_1"), filter1);
            }
        }

        if(filter2!=null)
            assertFormElementEquals(Locator.id("value_2"), filter2);
        else
            assertFormElementEquals(Locator.name("filterType_2"), "No Other Filter");

        clickButtonContainingText("CANCEL", 0);
        }

        executeScript("LABKEY.DataRegions['query'].clearAllFilters();");
        waitForElementToDisappear(Locator.css(".labkey-dataregion-msg"), WAIT_FOR_JAVASCRIPT);
        //clickButton("Clear All"); // Can't trigger :hover pseudo-class with webdriver
    }

    @LogMethod
    protected void checkFilterWasApplied(String[] textPresentAfterFilter, String[] textNotPresentAfterFilter, String columnName, String filter1Type, String filter1, String filter2Type, String filter2 )
    {
        assertTextPresent(textPresentAfterFilter);
        assertTextNotPresent(textNotPresentAfterFilter);
        //make sure we show user a description of what's going on.  See 11.2-3_make_filters_work.docx
        assertFilterTextPresent(columnName, filter1Type, filter1);
        if(filter2Type!=null)
        {
            assertFilterTextPresent(columnName, filter2Type, filter2);
        }

    }
}
