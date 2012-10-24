/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

/**
 * User: elvan
 * Date: 8/7/11
 * Time: 3:58 PM
 */
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.RReportHelper;

import java.util.Arrays;

/**conceptually filter and list are separate, but
 * it was convenient to use the list test helpers for filter
 */
public class FilterTest extends ListTest
{
    protected final static String PROJECT_NAME = "FilterVerifyProject";
    protected final static String R_VIEW = TRICKY_CHARACTERS + "R view";
    protected final static String FACET_TEST_LIST = "FacetList";

    protected void createList2()
    {
        ListHelper.ListColumn yearColumn = new ListHelper.ListColumn("year", "year", ListHelper.ListColumnType.Integer, "");
        _listHelper.createList(PROJECT_NAME, FACET_TEST_LIST, LIST2_KEY_TYPE, LIST2_KEY_NAME, _list2Col1, yearColumn);
        clickButton("Import Data");
        setFormElement(Locator.name("text"),"Car\tColor\tyear\n" +
                "1\tBlue\t1980\n" +
                "2\tRed\t1970\n" +
                "3\tYellow\t1990\n" );

        clickButton("Submit", 0);
        waitForElement(Locator.tagWithText("button", "OK"), WAIT_FOR_JAVASCRIPT);
        _extHelper.clickExtButton("OK");
        waitForPageToLoad();
    }


    void setUpList()
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

        log("Setup project and list module");
        _containerHelper.createProject(PROJECT_NAME, null);
        log("Add list -- " + LIST_NAME_COLORS);
        _listHelper.createList(PROJECT_NAME, LIST_NAME_COLORS, LIST_KEY_TYPE, LIST_KEY_NAME2, _listCol1, _listCol2, _listCol3, _listCol4, _listCol5, _listCol6);
        log("Set title field of 'Colors' to 'Desc'");
        clickEditDesign();
        selectOptionByText("ff_titleColumn", "Desc");
        clickDone();
        clickLinkWithText(LIST_NAME_COLORS);
        clickImportData();
        setFormElement("text", testDataFull.toString());
        submitImportTsv();
    }


    public void doTestSteps()
    {
        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();
        setUpList();
        _customizeViewsHelper.createRView(null, R_VIEW);
        filterTest();
        facetedFilterTest();
    }

    private void startFilter(String column)
    {
        click(Locator.tagWithText("div", column));
        click(Locator.tagWithText("span", "Filter..."));
        _extHelper.waitForExtDialog("Show Rows Where "+column+"...");
        waitForText("[All]");
        sleep(400);
    }

    private void facetedFilterTest()
    {
        createList2();
        assertTextPresent("Light", "Robust", "Zany");
        startFilter("Color");

        log("Verifying expected faceted filter elements present");
        assertTextPresent("Choose Filters", "Choose Values");

        assertTextPresent("Light", 2);
        assertTextPresent("Robust", 2);
        assertTextPresent("Zany", 2);
        _extHelper.clickExtButton("OK");
        assertTextPresent("Light", "Robust", "Zany");

        setFacetedFilter("query", "Color", "Light");
        assertTextPresent("Light");
        assertTextNotPresent("Robust", "Zany");

        setUpFacetedFilter("query", "Color", "Robust");
        _extHelper.clickExtTab("Choose Filters");
        //NOTE: the filter will optimize this to EQUALS, since there is 1 value
        waitForFormElementToEqual(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='Filter Type:']]/div/div/input"), "Equals");
        Assert.assertEquals("Faceted -> logical filter conversion failure", "Robust", getFormElement("value_1"));
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtButton("OK");
        assertTextNotPresent("Light", "Zany");
        assertTextPresent("Robust");

        // Issue 14710: Switching between faceted and logical filters breaks dialog
        setUpFacetedFilter("query", "Color", "Robust", "Light");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='Filter Type:']]/div/div/input"), "Does Not Equal Any Of (e.g. \"a;b;c\")");
        Assert.assertEquals("Faceted -> logical filter conversion failure", "Zany", getFormElement("value_1"));
        _extHelper.selectComboBoxItem("Filter Type:", "Is Blank");
        _extHelper.clickExtTab("Choose Values");
        _extHelper.waitForExtDialog("Confirm change");
        _extHelper.clickExtButton("Confirm change", "Yes", 0);
        _extHelper.clickExtButton("OK");
        //the change above would result in filters being dropped.  because we did not select new filters, we expect this to return an unfiltered grid
        assertTextPresent("Light", "Robust", "Zany");

        //now repeat with a filter that should be translated
        setUpFacetedFilter("query", "Color", "Light");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='Filter Type:']]/div/div/input"), "Equals");
        Assert.assertEquals("Faceted -> logical filter conversion failure", "Light", getFormElement("value_1"));

        setFormElement("value_1", "Light;Robust");

        _extHelper.clickExtTab("Choose Values"); //we should get no alerts, and Light + Robust should be checked
        _extHelper.clickExtButton("OK");
        assertTextPresent("Light", "Robust");
        //assertTextNotPresent("Zany");  //NOTE: the filter message is 'Is not any of (Zany)', so this will fail.  test for '1990' instead.
        assertTextNotPresent("1990");

        setFacetedFilter("query", "Color");
        assertTextPresent("Light", "Robust", "Zany");
        
        log("Verifying faceted filter on non-lookup column");
        startFilter("year");
        assertTextPresent("Choose Filters", "Choose Values");

        assertTextPresent("1980", 2);
        assertTextPresent("1990", 2);
        assertTextPresent("1970", 2);

        _extHelper.clickExtButton("OK");
        assertTextPresent("1980", "1990", "1970");

        setFacetedFilter("query", "year", "1980");
        assertTextPresent("1980");
        assertTextNotPresent("1990", "1970");

        setUpFacetedFilter("query", "year", "1990");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='Filter Type:']]/div/div/input"), "Equals");
        Assert.assertEquals("Faceted -> logical filter conversion failure", "1990", getFormElement("value_1"));
        _extHelper.clickExtTab("Choose Values");
        _extHelper.clickExtButton("OK");
        assertTextNotPresent("1980", "1970");
        assertTextPresent("1990");

        setUpFacetedFilter("query", "year", "1990", "1980");
        _extHelper.clickExtTab("Choose Filters");
        waitForFormElementToEqual(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='Filter Type:']]/div/div/input"), "Does Not Equal Any Of (e.g. \"a;b;c\")");
        Assert.assertEquals("Faceted -> logical filter conversion failure", "1970", getFormElement("value_1"));
        _extHelper.selectComboBoxItem("Filter Type:", "Is Blank");
        _extHelper.clickExtTab("Choose Values");
        _extHelper.waitForExtDialog("Confirm change");
        _extHelper.clickExtButton("Confirm change", "Yes", 0);
        _extHelper.clickExtButton("OK");
        assertTextPresent("1980", "1990");
        assertLinkNotPresentWithText("1970");

        // TODO: Blocked: 14710: Switching between faceted and logical filters breaks dialog
//        setFacetedFilter("query", "year");
//        assertTextPresent("1980", "1990", "1970");
    }

    protected void filterTest()
    {
        log("Filter Test");

        validFiltersGenerateCorrectResultsTest();

        invalidFiltersGenerateCorrectErrorTest();

        filterCancelButtonWorksTest();
    }


    private void invalidFiltersGenerateCorrectErrorTest()
    {
        String[][] testArgs = generateInvalidFilterTestArgs();

        for(String[] argSet : testArgs)
        {
            invalidFiltersGenerateCorrectErrorTest(argSet[0], argSet[1],
                argSet[2], argSet[3], argSet[4]);
        }

    }


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
        assert(!isElementPresent(Locator.extButtonEnabled("OK")));
        assertTextPresent(expectedError);

        clickButton("CANCEL", 0);
        waitForExtMaskToDisappear();

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

    public void validFiltersGenerateCorrectResultsTest()
    {
        Object[][] testArgs = generateValidFilterArgsAndResponses();

        for(Object[] args : testArgs)
        {
            validFilterGeneratesCorrectResultsTest((String) args[0], (String) args[1], (String) args[2], (String) args[3], (String) args[4], (String[]) args[5], (String[]) args[6]);
        }
    }

    private Object[][] generateValidFilterArgsAndResponses()
    {
        String escapedName = _listCol6.getName().replace(",", "$C");

        Object[][] ret = {
                //String columnName, String filter1Type, String filter1, String filter2Type, String filter2, String[] textPresentAfterFilter, String[] textNotPresentAfterFilter,
                //Issue 12197
                {_listCol4.getName(), "Equals One Of (e.g. \"a;b;c\")", TEST_DATA[4][3] + ";" + TEST_DATA[4][2], null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][3]}, new String[] {TEST_DATA[1][0],TEST_DATA[1][1]}},
                {_listCol1.getName(), "Equals", TEST_DATA[1][0], null, null, new String[] {TEST_DATA[1][0]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][3]}},
                {_listCol1.getName(), "Starts With", "Z", null, null, new String[] {TEST_DATA[1][3]}, new String[] {TEST_DATA[1][0],TEST_DATA[1][1],TEST_DATA[1][2]}},
                {_listCol1.getName(), "Does Not Start With", "Z", null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][0]}, new String[] {TEST_DATA[1][3]}},
                //can't check for the absence of thing you're excluding, since it will be present in the filter text
                {_listCol1.getName(), "Does Not Equal", TEST_DATA[1][0], null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][3]}, new String[] {TEST_DATA[5][0]}},
                {_listCol3.getName(), "Equals", "true", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][2]}, new String[] {TEST_DATA[1][1],TEST_DATA[1][3]}},
                {_listCol3.getName(), "Does Not Equal", "false", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][2]}, new String[] {TEST_DATA[1][1],TEST_DATA[1][3]}},
                //filter is case insensitive
                {_listCol6.getName(), "Contains", "e", "Contains", "r", new String[] {TEST_DATA[5][2],TEST_DATA[5][0], TEST_DATA[5][1]}, new String[] {TEST_DATA[1][3]}},
//                {_listCol2.getName(), "Is Greater Than", "2", "Is Less Than or Equal To", "4", new String[] {TEST_DATA[1][2]}, new String[] {TEST_DATA[1][0],TEST_DATA[1][1],TEST_DATA[1][3]}},
                {_listCol4.getName(), "Is Greater Than or Equal To", "9", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][1]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3]}},
                {_listCol4.getName(), "Is Greater Than", "9", null, null, new String[] {TEST_DATA[1][0]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3],TEST_DATA[1][1]}},
                {_listCol4.getName(), "Is Blank", "", null, null, new String[] {}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3],TEST_DATA[1][1], TEST_DATA[1][0]}},
                //new filters for faceted filtering
                {_listCol6.getName(),  "Contains One Of (e.g. \"a;b;c\")", TEST_DATA[5][1] + ";" + TEST_DATA[5][3], null, null, new String[] {TEST_DATA[5][1]}, new String[] {TEST_DATA[1][0], TEST_DATA[1][2]}},
                {_listCol1.getName(), "Does Not Contain Any Of (e.g. \"a;b;c\")", TEST_DATA[1][3] + ";" + TEST_DATA[1][1], null, null, new String[] {TEST_DATA[1][0], TEST_DATA[1][2]}, new String[] {TEST_DATA[0][1] , TEST_DATA[0][3]}},
                {_listCol6.getName(), "Is Blank", "", null, null, new String[] {TEST_DATA[1][3]}, new String[] {TEST_DATA[1][1] ,TEST_DATA[1][2], TEST_DATA[1][0]}},
                {_listCol6.getName(), "Is Not Blank", "", null, null, new String[] {TEST_DATA[1][1] ,TEST_DATA[1][2], TEST_DATA[1][0]}, new String[] {TEST_DATA[1][3]}}
        };

        return ret;
    }

    //Issue 12787: Canceling filter dialog requires two clicks
    private void filterCancelButtonWorksTest()
    {
        String id = EscapeUtil.filter(TABLE_NAME + ":" + _listCol4.getName() + ":filter");
        runMenuItemHandler(id);

        clickButton("CANCEL",0);
        waitForExtMaskToDisappear();

        assertTextNotPresent("Show Rows Where");
    }

    private void validFilterGeneratesCorrectResultsTest(String columnName, String filter1Type, String filter1, String filter2Type, String filter2,
            String[] textPresentAfterFilter, String[] textNotPresentAfterFilter)
    {
        String fieldKey = EscapeUtil.fieldKeyEncodePart(columnName);

        log("Filtering " + columnName + " with filter type: " + filter1Type + ".  value: " + filter1.getClass());
        if(filter2Type!=null)
            log("Second filter: " + filter2Type + ".  value:" + filter2);
        setFilter(TABLE_NAME, fieldKey, filter1Type, filter1, filter2Type, filter2);

        checkFilterWasApplied(textPresentAfterFilter, textNotPresentAfterFilter, columnName, filter1Type, filter1, filter2Type, filter2);


        log("checking filter present in R view");
        clickMenuButton("Views", R_VIEW);
        sleep(1000);
        checkFilterWasApplied(textPresentAfterFilter, textNotPresentAfterFilter, columnName, filter1Type, filter1, filter2Type, filter2);

        clickMenuButton("Views", "default");

        //open filter
        runMenuItemHandler(TABLE_NAME + ":" + fieldKey + ":filter");
        waitForTextToDisappear("Loading...");
        _extHelper.clickExtTab("Choose Filters");

        if(filter1!=null)
            waitForFormElementToEqual(Locator.name("value_1"), filter1);

        if(filter2!=null)
            waitForFormElementToEqual(Locator.name("value_2"), filter2);

        clickButtonContainingText("CANCEL", 0);

        clickButton("Clear All");
    }

    protected void checkFilterWasApplied(String[] textPresentAfterFilter, String[] textNotPresentAfterFilter, String columnName, String filter1Type, String filter1, String filter2Type, String filter2 )
    {
        waitForTextToDisappear("Loading", defaultWaitForPage);
        assertTextPresent(textPresentAfterFilter);
        assertTextNotPresent(textNotPresentAfterFilter);
        //make sure we show user a description of what's going on.  See 11.2-3_make_filters_work.docx
        assertFilterTextPresent(columnName, filter1Type, filter1);
        if(filter2Type!=null)
        {
        assertFilterTextPresent(columnName, filter2Type, filter2);

        }

    }


    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected String getProjectName()
    {
        return PROJECT_NAME;
    }
}
