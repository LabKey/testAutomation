/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.params.list.VarListDefinition;
import org.labkey.test.tests.issues.IssuesTest;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.ReportDataRegion;
import org.labkey.test.util.TestDataGenerator;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.params.FieldDefinition.ColumnType;
import static org.labkey.test.params.FieldDefinition.LookupInfo;
import static org.labkey.test.util.PermissionsHelper.MemberType;

@Category({Daily.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 18)
public class FilterTest extends BaseWebDriverTest
{
    protected final static String EDITOR1 = "filter_editor1@filter.test";
    protected final static String EDITOR2 = "filter_editor2@filter.test";
    protected final static String R_VIEW = TRICKY_CHARACTERS + "R report";
    protected final static String FACET_TEST_LIST = "FacetList";

    protected final static String LIST_NAME_COLORS = TRICKY_CHARACTERS_NO_QUOTES + "Colors";
    protected final static String LIST_KEY_NAME2 = "Color";

    protected final FieldDefinition _listColKey = new FieldDefinition("Color", ColumnType.String);
    protected final FieldDefinition _listCol1 = new FieldDefinition("Desc", ColumnType.String).setLabel("Description").setDescription("What the color is like");
    protected final FieldDefinition _listCol2 = new FieldDefinition("Month", ColumnType.DateAndTime).setLabel("Month to Wear").setDescription("When to wear the color").setFormat("M");
    protected final FieldDefinition _listCol3 = new FieldDefinition("JewelTone", ColumnType.Boolean).setLabel("Jewel Tone").setDescription("Am I a jewel tone?");
    protected final FieldDefinition _listCol4 = new FieldDefinition("Good", ColumnType.Integer).setLabel("Quality").setDescription("How nice the color is");
    protected final FieldDefinition _listCol5 = new FieldDefinition("HiddenColumn", ColumnType.String).setLabel("CantSeeMe").setDescription("I should be hidden!");
    protected final FieldDefinition _listCol6 = new FieldDefinition("Aliased,Column", ColumnType.String).setLabel("Element").setDescription("I show aliased data.");
    protected final static String[][] TEST_DATA = {
            { "Blue", "Green", "Red", "Yellow" },
            { "Light", "Mellow", "Robust", "ZanzibarMasinginiTanzaniaAfrica" },
            { "true", "false", "true", "false"},
            { "1", "4", "3", "2" },
            { "10", "9", "8", "7"},
            { "Water", "Earth", "Fire", "Air"}};
    protected final static String[] CONVERTED_MONTHS = { "2000-01-01", "2000-04-04", "2000-03-03", "2000-02-02" };

    protected final FieldDefinition _list2ColKey = new FieldDefinition("Car", ColumnType.String);
    protected final FieldDefinition _list2Col1 = new FieldDefinition(LIST_KEY_NAME2, new LookupInfo(null, "lists", LIST_NAME_COLORS).setTableType(ColumnType.String)).setDescription("The color of the car");
    protected final FieldDefinition _list2ColYear = new FieldDefinition("year", ColumnType.Integer).setLabel("year");

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "FilterVerifyProject";
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(false, EDITOR1, EDITOR2);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        FilterTest init = (FilterTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        new RReportHelper(this).ensureRConfig();
        _containerHelper.createProject(getProjectName(), null);
        _userHelper.createUser(EDITOR1);
        _userHelper.createUser(EDITOR2);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(EDITOR1, "Editor", MemberType.user, getProjectName());
        apiPermissionsHelper.addMemberToRole(EDITOR2, "Editor", MemberType.user, getProjectName());

        createList();
        createList2();
    }

    @LogMethod
    void createList() throws Exception
    {
        Connection connection = createDefaultConnection();

        log("Add list -- " + LIST_NAME_COLORS);
        ListDefinition listDefinition = new VarListDefinition(LIST_NAME_COLORS)
                .setFields(List.of(_listColKey, _listCol1, _listCol2, _listCol3, _listCol4, _listCol5, _listCol6))
                .setTitleColumn(_listCol1.getName());
        TestDataGenerator dataGenerator = listDefinition.create(connection, getProjectName());

        log("Import data to the list");
        for (int i=0 ; i<TEST_DATA[0].length ; i++)
        {
            dataGenerator.addCustomRow(Map.of(
                    _listColKey.getName(), TEST_DATA[0][i],
                    _listCol1.getName(), TEST_DATA[1][i],
                    _listCol2.getName(), CONVERTED_MONTHS[i],
                    _listCol3.getName(), TEST_DATA[2][i],
                    _listCol4.getName(), TEST_DATA[4][i],
                    _listCol6.getName(), (i==TEST_DATA[0].length-1) ? "" : TEST_DATA[5][i] // NOTE make last row blank
            ));
        }
        dataGenerator.insertRows(connection);

        _listHelper.goToList(LIST_NAME_COLORS);
        new RReportHelper(this).createRReport(R_VIEW);
        _listHelper.goToManageLists();
        clickAndWait(Locator.linkWithText(LIST_NAME_COLORS));
    }

    @LogMethod
    protected void createList2() throws Exception
    {
        ListDefinition list2 = new VarListDefinition(FACET_TEST_LIST)
                .setFields(List.of(_list2ColKey, _list2Col1, _list2ColYear));
        Connection connection = createDefaultConnection();
        TestDataGenerator dataGenerator = list2.create(connection, getProjectName());

        dataGenerator.addCustomRow(Map.of("Car", 1, "Color", "Blue", "Year", 1980));
        dataGenerator.addCustomRow(Map.of("Car", 2, "Color", "Red", "Year", 1970));
        dataGenerator.addCustomRow(Map.of("Car", 3, "Color", "Yellow", "Year", 1990));
        dataGenerator.insertRows(connection);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testFacetedFilter()
    {
        _listHelper.goToList(FACET_TEST_LIST);
        DataRegionTable region = new DataRegionTable("query", this);
        verifyColumnValues(region, "Color", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");

        log("Verifying expected faceted filter elements present");
        verifyFacetOptions(region, "Color", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");

        region.setFacetedFilter("Color", "ZanzibarMasinginiTanzaniaAfrica");
        verifyColumnValues(region, "Color", "ZanzibarMasinginiTanzaniaAfrica");

        region.setUpFacetedFilter("Color", "Robust");
        _extHelper.clickExtTab("Choose Filters");
        //NOTE: the filter will optimize this to EQUALS, since there is 1 value
        waitForFormElementToEqual(Locator.name("filterType_1"), "Equals");
        assertEquals("Faceted -> logical filter conversion failure", "Robust", getFormElement(Locator.name("value_1")));
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtButton("OK");
        verifyColumnValues(region, "Color", "Robust");

        // Issue 14710: Switching between faceted and logical filters breaks dialog
        region.setUpFacetedFilter("Color", "Robust", "Light");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Does Not Equal");
        assertEquals("Faceted -> logical filter conversion failure", "ZanzibarMasinginiTanzaniaAfrica", getFormElement(Locator.name("value_1")));
        _extHelper.selectComboBoxItem("Filter Type:", "Is Blank");
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Is Blank");
        clickButton("Clear Filter");
        //the change above would result in filters being dropped.
        verifyColumnValues(region, "Color", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");

        //now repeat with a filter that should be translated
        region.setUpFacetedFilter("Color", "Light");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Equals");
        assertEquals("Faceted -> logical filter conversion failure", "Light", getFormElement(Locator.name("value_1")));

        setFormElement(Locator.name("value_1"), "Light;Robust");

        _extHelper.clickExtTab("Choose Values"); //we should get no alerts
        clickButton("Clear Filter");
        region.setFacetedFilter("Color", "Light", "Robust");
        verifyColumnValues(region, "Color", "Light", "Robust");
        verifyColumnValues(region, "year", "1980", "1970");

        region.setFacetedFilter("Color");
        verifyColumnValues(region, "Color", "Light", "Robust", "ZanzibarMasinginiTanzaniaAfrica");

        log("Verifying faceted filter on non-lookup column");

        verifyColumnValues(region, "year", "1980", "1970", "1990");
        verifyFacetOptions(region, "year", "1970", "1980", "1990");

        region.setFacetedFilter("year", "1980");
        verifyColumnValues(region, "year", "1980");

        region.setUpFacetedFilter("year", "1990");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Equals");
        assertEquals("Faceted -> logical filter conversion failure", "1990", getFormElement(Locator.name("value_1")));
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtButton("OK");
        verifyColumnValues(region, "year", "1990");

        region.setUpFacetedFilter("year", "1990", "1980");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.name("filterType_1"), "Does Not Equal");
        assertEquals("Faceted -> logical filter conversion failure", "1970", getFormElement(Locator.name("value_1")));
        _extHelper.selectComboBoxItem("Filter Type:", "Is Blank");
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtButton("OK");
        verifyColumnValues(region, "year", "1980", "1970", "1990");

        region.setFacetedFilter("year");
        verifyColumnValues(region, "year", "1980", "1970", "1990");
    }

    @Test
    public void testMaskedFacet()
    {
        _listHelper.goToList(FACET_TEST_LIST);
        DataRegionTable region = new DataRegionTable("query", this);
        region.setFacetedFilter("year", "1980", "1990");
        verifyFacetOptions(region, "Car", "1", "3");

        region.setFacetedFilter("Car", "1");
        verifyFacetOptions(region, "Color", "Light");

        region.clearAllFilters("Car");
        region.setFilter("year", "Is Greater Than", "1980");
        verifyFacetOptions(region, "Car", "3");
    }

    @Test
    public void testContainerFilterFacet() throws Exception
    {
        IssuesHelper issuesHelper = new IssuesHelper(this);
        issuesHelper.createNewIssuesList("issues", getContainerHelper());
        goToModule("Issues");
        issuesHelper.goToAdmin()
                .setAssignedTo("Site: Administrators")
                .clickSave();
        IssuesTest.addLookupValues(getProjectName(), "issues", "Type", Arrays.asList("typea", "typeb"));

        HashMap<String, String> projectIssue = new HashMap<>();
        projectIssue.put("title", "project issue1");
        projectIssue.put("assignedTo", getDisplayName());
        projectIssue.put("type", "typea");
        projectIssue.put("priority", "1");
        issuesHelper.addIssue(projectIssue);
        HashMap<String, String> projectIssue2 = new HashMap<>();
        projectIssue2.put("title", "project issue2");
        projectIssue2.put("assignedTo", getDisplayName());
        projectIssue2.put("type", "typeb");
        projectIssue2.put("priority", "2");
        issuesHelper.addIssue(projectIssue2);

        _containerHelper.createSubfolder(getProjectName(), "subfolder");
        navigateToFolder(getProjectName(), "subfolder");

        issuesHelper.createNewIssuesList("issues", getContainerHelper());
        goToModule("Issues");
        issuesHelper.goToAdmin();
        issuesHelper.setIssueAssignmentList("Site: Administrators");
        clickButton("Save");

        IssuesTest.addLookupValues(getProjectName(), "issues", "Type", Arrays.asList("typed", "typee"));

        navigateToFolder(getProjectName(), "subfolder");

        HashMap<String, String> subfolderIssue = new HashMap<>();
        subfolderIssue.put("title", "subfolder issue1");
        subfolderIssue.put("assignedTo", getDisplayName());
        subfolderIssue.put("type", "typed");
        subfolderIssue.put("priority", "3");
        issuesHelper.addIssue(subfolderIssue);
        HashMap<String, String> subfolderIssue2 = new HashMap<>();
        subfolderIssue2.put("title", "subfolder issue2");
        subfolderIssue2.put("assignedTo", getDisplayName());
        subfolderIssue2.put("type", "typee");
        subfolderIssue2.put("priority", "4");
        issuesHelper.addIssue(subfolderIssue2);

        goToProjectHome();
        goToModule("Issues");

        assertElementPresent(Locator.linkWithText(projectIssue.get("title")));
        assertElementPresent(Locator.linkWithText(projectIssue2.get("title")));
        assertElementNotPresent(Locator.linkWithText(subfolderIssue.get("title")));
        assertElementNotPresent(Locator.linkWithText(subfolderIssue2.get("title")));

        DataRegionTable region = new DataRegionTable("issues-issues", this);
        region.setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS);
        assertElementPresent(Locator.linkWithText(projectIssue.get("title")));
        assertElementPresent(Locator.linkWithText(projectIssue2.get("title")));
        assertElementPresent(Locator.linkWithText(subfolderIssue.get("title")));
        assertElementPresent(Locator.linkWithText(subfolderIssue2.get("title")));

        verifyFacetOptions(region, "Type",
                projectIssue.get("type"),
                projectIssue2.get("type"),
                subfolderIssue.get("type"),
                subfolderIssue2.get("type"));

        verifyFacetOptions(region, "Priority",
                projectIssue.get("priority"),
                projectIssue2.get("priority"),
                subfolderIssue.get("priority"),
                subfolderIssue2.get("priority"));

        region.setFacetedFilter("Priority", projectIssue2.get("priority"), subfolderIssue.get("priority"));
        assertElementNotPresent(Locator.linkWithText(projectIssue.get("title")));
        assertElementPresent(Locator.linkWithText(projectIssue2.get("title")));
        assertElementPresent(Locator.linkWithText(subfolderIssue.get("title")));
        assertElementNotPresent(Locator.linkWithText(subfolderIssue2.get("title")));

        verifyFacetOptions(region, "Type",
                projectIssue2.get("type"),
                subfolderIssue.get("type"));
    }

    private void verifyFacetOptions(DataRegionTable dataRegion, String column, String... options)
    {
        dataRegion.openFilterDialog(column);
        verifyOptionsInFilterDialog(options);
        _extHelper.clickExtButton("Cancel", 0);
    }

    private void verifyColumnValues(DataRegionTable dataRegion, String columnName, String... expectedValues)
    {
        List<String> expectedList = Arrays.asList(expectedValues);
        assertEquals(expectedList, dataRegion.getColumnDataAsText(columnName));
    }

    private void verifyOptionsInFilterDialog(String... expectedOptions)
    {
        final Locator.CssLocator filterDialogFacetPanel = Locator.css(".labkey-filter-dialog .x-grid3-body");
        waitForElement(filterDialogFacetPanel.containing(expectedOptions[0]));
        List<String> actualOptions = Arrays.asList(getText(filterDialogFacetPanel).replaceAll(" ", "").split("\n"));

        assertEquals("Unexpected filter options", Arrays.asList(expectedOptions), actualOptions);
    }

    @Test
    public void testFilters()
    {
        _listHelper.goToList(LIST_NAME_COLORS);

        validFiltersGenerateCorrectResultsTest();

        invalidFiltersGenerateCorrectErrorTest();

        filterCancelButtonWorksTest();
    }

    /**
     * Issue 40362: Automated Regression Coverage for ~me~ filter value
     */
    @Test
    public void testUserMeFilter()
    {
        _listHelper.goToList(LIST_NAME_COLORS);

        final DataRegionTable list = new DataRegionTable(TABLE_NAME, this);

        // Modify a row as EDITOR1
        doAsUser(EDITOR1, () -> list.updateRow(1, Map.of("HiddenColumn", "edit1")));

        // Modify a row as EDITOR2
        doAsUser(EDITOR2, () -> list.updateRow(2, Map.of("HiddenColumn", "edit2")));

        list.ensureColumnPresent("ModifiedBy");
        int userColumn = list.getColumnIndex("ModifiedBy");
        String name0 = getDisplayName();
        String name1 = _userHelper.getDisplayNameForEmail(EDITOR1);
        String name2 = _userHelper.getDisplayNameForEmail(EDITOR2);

        list.setFilter("ModifiedBy", "Equals", "~me~");
        assertEquals("ModifiedBy: Equals '~me~'",
                List.of(name0, name0), list.getColumnDataAsText(userColumn));

        list.setFilter("ModifiedBy", "Contains", "~me~");
        assertEquals("ModifiedBy: Contains '~me~'",
                List.of(name0, name0), list.getColumnDataAsText(userColumn));

        list.setFilter("ModifiedBy", "Does Not Equal Any Of", "~me~");
        assertEquals("ModifiedBy: Does Not Equal Any Of (~me~)",
                List.of(name1, name2), list.getColumnDataAsText(userColumn));

        list.setFilter("ModifiedBy", "Equals One Of", "~me~;" + name1);
        assertEquals("ModifiedBy: Equals One Of (~me~;editor1)",
                List.of(name0, name1, name0), list.getColumnDataAsText(userColumn));
    }

    @LogMethod
    private void invalidFiltersGenerateCorrectErrorTest()
    {
        for (String[] argSet : generateInvalidFilterTestArgs())
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
        log("attempt to set filter column: " + columnName + ". With filter type: " + filterType + ".  And filter value: " + filterValue);
        DataRegionTable region = new DataRegionTable(regionName, this);
        region.setUpFilter(columnName, filterType, filterValue);
        sleep(300);
        clickButton("OK", 0);
        assertElementPresent(Locator.extButton("OK"));
        assertTextPresent(expectedError);

        clickButton("Cancel", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

    }

    public static final String TABLE_NAME = "query";
    public static final String EMPTY_FILTER_VAL_ERROR_MSG = "You must enter a value";

    private String[][] generateInvalidFilterTestArgs()
    {

        return new String[][]{
                {TABLE_NAME, getBooleanColumnName(), "Equals", "foo", "foo is not a valid boolean"},
                {TABLE_NAME, getStringColumnName(), "Equals", "", EMPTY_FILTER_VAL_ERROR_MSG},
                {TABLE_NAME, getDateColumnName(), "Equals", TRICKY_CHARACTERS, TRICKY_CHARACTERS + " might not be a valid date"},
                {TABLE_NAME, getIntColumnName(), "Equals", "ab123", "Invalid value: ab123"},
                {TABLE_NAME, getIntColumnName(), "Equals", "", EMPTY_FILTER_VAL_ERROR_MSG},
        };
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
        generateValidFilterArgsAndResponses().forEach(this::validFilterGeneratesCorrectResultsTest);
    }

    private static class FilterArgs
    {
        public final FieldDefinition columnDef;
        public final String filter1Type;
        public final String filter1Value;
        public final String filter2Type;
        public final String filter2Value;

        public final String[] present;
        public final String[] notPresent;

        public FilterArgs(FieldDefinition columnDef,
                          String filter1Type, @Nullable String filter1Value,
                          @Nullable String filter2Type, @Nullable String filter2Value,
                          String[] present, String[] notPresent)
        {
            this.columnDef = columnDef;
            this.filter1Type = filter1Type;
            this.filter1Value = filter1Value;
            this.filter2Type = filter2Type;
            this.filter2Value = filter2Value;
            this.present = present;
            this.notPresent = notPresent;
        }
    }

    public static FilterArgs createFilterArgs(FieldDefinition columnDef,
                                              String filter1Type, @Nullable String filter1Value,
                                              @Nullable String filter2Type, @Nullable String filter2Value,
                                              String[] present, String[] notPresent)
    {
        return new FilterArgs(columnDef, filter1Type, filter1Value, filter2Type, filter2Value, present, notPresent);
    }

    private List<FilterArgs> generateValidFilterArgsAndResponses()
        {
            return Arrays.asList(
                    //String columnName, String filter1Type, String filter1, String filter2Type, String filter2, String[] textPresentAfterFilter, String[] textNotPresentAfterFilter,
                    //Issue 12197
                    createFilterArgs(_listCol4, "Equals One Of", TEST_DATA[4][3] + ";" + TEST_DATA[4][2], null, null, new String[]{TEST_DATA[1][2], TEST_DATA[1][3]}, new String[]{TEST_DATA[1][0], TEST_DATA[1][1]}),
                    createFilterArgs(_listCol1, "Equals", TEST_DATA[1][0], null, null, new String[]{TEST_DATA[1][0]}, new String[]{TEST_DATA[1][2], TEST_DATA[1][1], TEST_DATA[1][3]}),
                    createFilterArgs(_listCol1, "Starts With", "Z", null, null, new String[]{TEST_DATA[1][3]}, new String[]{TEST_DATA[1][0], TEST_DATA[1][1], TEST_DATA[1][2]}),
                    createFilterArgs(_listCol1, "Does Not Start With", "Z", null, null, new String[]{TEST_DATA[1][2], TEST_DATA[1][1], TEST_DATA[1][0]}, new String[]{TEST_DATA[1][3]}),
                    //can't check for the absence of thing you're excluding, since it will be present in the filter text
                    createFilterArgs(_listCol1, "Does Not Equal", TEST_DATA[1][0], null, null, new String[]{TEST_DATA[1][2], TEST_DATA[1][1], TEST_DATA[1][3]}, new String[]{TEST_DATA[5][0]}),
                    createFilterArgs(_listCol1, "Does Not Equal Any Of", TEST_DATA[1][0] + ";" + TEST_DATA[1][1], null, null, new String[]{TEST_DATA[1][2], TEST_DATA[1][3]}, new String[]{TEST_DATA[5][0], TEST_DATA[5][1]}),
                    createFilterArgs(_listCol3, "Equals", "true", null, null, new String[]{TEST_DATA[1][0], TEST_DATA[1][2]}, new String[]{TEST_DATA[1][1], TEST_DATA[1][3]}),
                    createFilterArgs(_listCol3, "Does Not Equal", "false", null, null, new String[]{TEST_DATA[1][0], TEST_DATA[1][2]}, new String[]{TEST_DATA[1][1], TEST_DATA[1][3]}),
                    //filter is case insensitive
                    createFilterArgs(_listCol6, "Contains", "e", "Contains", "r", new String[]{TEST_DATA[5][2], TEST_DATA[5][0], TEST_DATA[5][1]}, new String[]{TEST_DATA[1][3]}),
                    createFilterArgs(_listCol4, "Is Greater Than or Equal To", "9", null, null, new String[]{TEST_DATA[1][0], TEST_DATA[1][1]}, new String[]{TEST_DATA[1][2], TEST_DATA[1][3]}),
                    createFilterArgs(_listCol4, "Is Greater Than", "9", null, null, new String[]{TEST_DATA[1][0]}, new String[]{TEST_DATA[1][2], TEST_DATA[1][3], TEST_DATA[1][1]}),
                    createFilterArgs(_listCol4, "Is Blank", null, null, null, new String[]{}, new String[]{TEST_DATA[1][2], TEST_DATA[1][3], TEST_DATA[1][1], TEST_DATA[1][0]}),
                    //new filters for faceted filtering
                    createFilterArgs(_listCol6, "Contains One Of", TEST_DATA[5][1] + ";" + TEST_DATA[5][3], null, null, new String[]{TEST_DATA[5][1]}, new String[]{TEST_DATA[1][0], TEST_DATA[1][2]}),
                    createFilterArgs(_listCol1, "Does Not Contain Any Of", TEST_DATA[1][3] + ";" + TEST_DATA[1][1], null, null, new String[]{TEST_DATA[1][0], TEST_DATA[1][2]}, new String[]{TEST_DATA[0][1], TEST_DATA[0][3]}),
                    createFilterArgs(_listCol6, "Is Blank", null, null, null, new String[]{TEST_DATA[1][3]}, new String[]{TEST_DATA[1][1], TEST_DATA[1][2], TEST_DATA[1][0]}),
                    createFilterArgs(_listCol6, "Is Not Blank", null, null, null, new String[]{TEST_DATA[1][1], TEST_DATA[1][2], TEST_DATA[1][0]}, new String[]{TEST_DATA[1][3]})
            );
        }

    //Issue 12787: Canceling filter dialog requires two clicks
    @LogMethod
    private void filterCancelButtonWorksTest()
    {
        DataRegionTable region = new DataRegionTable(TABLE_NAME, this);
        region.openFilterDialog(_listCol4.getName());
        clickButton("Cancel", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Show Rows Where");
    }

    private void validFilterGeneratesCorrectResultsTest(FilterArgs a)
    {
            validFilterGeneratesCorrectResultsTest(
                    a.columnDef,
                    a.filter1Type, a.filter1Value,
                    a.filter2Type, a.filter2Value,
                    a.present, a.notPresent.clone());
    }

    @LogMethod
    private void validFilterGeneratesCorrectResultsTest(FieldDefinition columnDef, String filter1Type, String filter1, String filter2Type, String filter2,
            String[] textPresentAfterFilter, String[] textNotPresentAfterFilter)
    {
        String fieldKey = EscapeUtil.fieldKeyEncodePart(columnDef.getName());
        {
            log("** Filtering " + columnDef.getName() + " with filter type: " + filter1Type + ", value: " + filter1);
            if (null != filter2Type)
                log("** Second filter: " + filter2Type + ".  value:" + filter2);
            DataRegionTable region = new DataRegionTable(TABLE_NAME, this);
            region.setFilter(fieldKey, filter1Type, filter1, filter2Type, filter2);

            checkFilterWasApplied(textPresentAfterFilter, textNotPresentAfterFilter, columnDef.getLabel(), filter1Type, filter1, filter2Type, filter2);

            log("** Checking filter present in R view");
            region.goToReport(R_VIEW);
            Locator.tagWithClass("table", "labkey-r-tsvout").waitForElement(getDriver(), 10000);
            checkFilterWasApplied(textPresentAfterFilter, textNotPresentAfterFilter, columnDef.getName(), filter1Type, filter1, filter2Type, filter2);
            new ReportDataRegion(TABLE_NAME, this).goToView("Default");

            log("** Checking filter values in filter dialog");
            region = new DataRegionTable(TABLE_NAME, this);
            region.openFilterDialog(fieldKey);
            _extHelper.clickExtTab("Choose Filters");
            String id = (filter1Type.matches("Contains One Of|Does Not Contain Any Of|Equals One Of|Does Not Equal Any Of")) ? "value_1-1" : "value_1";
            shortWait().until(ExpectedConditions.visibilityOf(Locator.id(id).findElement(getDriver())));

            if (filter1 != null)
            {
                // When we first load the filter panel, we convert single-value filters into a multi-value filter if possible,
                // then we invert negative filters ("Does Not Equal" becomes "In") and invert the values.
                // When switching to the 'Choose Filters' tab, we may invert again (if more than half of the values are selected)
                // and we may change a multi-value filter into a singular filter if only one value is selected.

                if (filter1Type.equals("Does Not Equal") && "Light".equals(filter1))
                {
                    // In this test case, "Does Not Equal" and "Light" are the initial filter type and value.
                    // When showing the dialog, "Does Not Equal" is first converted into "Not In" since "Does Not Equal" is a single value filter.
                    // Next, it is inverted from "Not In" to "In" and "Mellow;Robust;ZanzibarMasinginiTanzaniaAfrica" are selected.
                    // When switching tabs, the number of selected values (1) is less than half of the available values (4),
                    // so the filter is inverted again from "Not In" to "Does Not Equal Any Of" and "Light" is selected.
                    waitForFormElementToEqual(Locator.name("value_1"), "Light");
                }
                else if (filter1Type.equals("Does Not Equal Any Of") && "Light;Mellow".equals(filter1))
                {
                    // In this test case, "Does Not Equal Any Of" and "Light;Mellow" are the initial filter type and value.
                    // When showing the dialog, "Does Not Equal Any Of" is inverted to "In" and "Robust;ZanzibarMasinginiTanzaniaAfrica" are selected.
                    // When switching tabs, nothing changes.
                    waitForFormElementToEqual(Locator.name("value_1-1"), "Light\nMellow");
                }
                else if (filter1Type.equals("Does Not Equal") && "false".equals(filter1))
                {
                    // In this test case, "Does Not Equal" and "false" are the initial filter type and value.
                    // When showing the dialog "Does Not Equal" is inverted as "In" and "true" is selected.
                    // When switching tabs, the filter is simplified from "In" to "Equal" because only a single value, "true", is selected.
                    if (getFormElement(Locator.name("filterType_1")).equals("Equals"))
                        assertEquals("true", getFormElement(Locator.id("value_1")));
                    else
                        assertEquals(filter1, getFormElement(Locator.id("value_1")));
                }
                else
                {
                    assertEquals(filter1.replaceAll(";", "\n"), getFormElement(Locator.id(id)));
                }
            }

            if (filter2 != null)
                assertEquals(filter2, getFormElement(Locator.id("value_2")));
            else
                assertEquals("No Other Filter", getFormElement(Locator.name("filterType_2")));

            clickButtonContainingText("Cancel", 0);
        }

        DataRegionTable table = new DataRegionTable(TABLE_NAME, this.getDriver());
        table.clearAllFilters();
        assertElementNotPresent("Expected message to disappear", Locator.css(".labkey-dataregion-msg"));
    }

    @Test
    public void testSearchFilter()
    {
        _listHelper.goToList(LIST_NAME_COLORS);

        DataRegionTable region = new DataRegionTable(TABLE_NAME, this.getDriver());

        // Add search filter for 'ellow' matching Mellow / Yellow
        region.api().expectingRefresh().executeScript("addFilter(" + createSearchFilterJS("ellow") + ")");

        assertEquals("Should have 2 rows present after adding search filter", 2, region.getDataRowCount());

        // Replace search filter 'Robust'
        region.api().expectingRefresh().executeScript("replaceFilter(" + createSearchFilterJS("Robust") + ")");
        assertEquals("Should have 1 row present after replacing search filter", 1, region.getDataRowCount());

        // Replace search filter with INJECT_CHARS_1
        region.api().expectingRefresh().executeScript("replaceFilter(" + createSearchFilterJS(INJECT_CHARS_1) + ")");
        assertEquals("Should have 0 row present after replacing search filter", 0, region.getDataRowCount());

        // Clear all filters
        region.api().expectingRefresh().executeScript("clearAllFilters();");
        assertEquals("Should have 4 rows present after clearing search filters", 4, region.getDataRowCount());

        // Set a filter, then add a case-insensitive "water" search filter to match "Water"
        region.api().expectingRefresh().executeScript("addFilter(LABKEY.Filter.create('JewelTone', true))");
        region.api().expectingRefresh().executeScript("addFilter(" + createSearchFilterJS("water") + ")");
        assertEquals("Should have 1 row present after adding case-insensitive search filter", 1, region.getDataRowCount());

        // Remove just the search filter
        region.api().expectingRefresh().executeScript("removeFilter(" + createSearchFilterJS("Should not appear") + ")");
        assertEquals("Should have 2 rows present after removing only search filter", 2, region.getDataRowCount());
    }

    private String createSearchFilterJS(Object value)
    {
        return "LABKEY.Filter.create('*', " + EscapeUtil.jsString(value.toString()) + ", LABKEY.Filter.Types.Q)";
    }

    @LogMethod
    protected void checkFilterWasApplied(String[] textPresentAfterFilter, String[] textNotPresentAfterFilter, String columnName, String filter1Type, String filter1, String filter2Type, String filter2 )
    {
        assertTextPresent(textPresentAfterFilter);
        assertTextNotPresent(textNotPresentAfterFilter);
        //make sure we show user a description of what's going on.  See 11.2-3_make_filters_work.docx
        assertFilterTextPresent(columnName, filter1Type, filter1);
        if (filter2Type != null)
        {
            assertFilterTextPresent(columnName, filter2Type, filter2);
        }
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }
}
