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

import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.RReportHelper;

/**conceptually filter and list are separate, but
 * it was convenient to use the list test helpers for filter
 */
public class FilterTest extends ListTest
{
    protected final static String PROJECT_NAME = "FilterVerifyProject";
    protected String rViewName = TRICKY_CHARACTERS + "R view";

    protected void createList2()
    {

        ListHelper.createList(this, PROJECT_NAME, "list1", LIST2_KEY_TYPE, LIST2_KEY_NAME, _list2Col1);
        clickNavButton("Import Data");
        setFormElement(Locator.name("text"),"Car\tColor\n" +
                "1\tBlue\n" +
                   "2\tRed" );

        clickButton("Submit", 0);
        waitForElement(Locator.tagWithText("button", "OK"), defaultWaitForPage);
        ExtHelper.clickExtButton(this, "OK");
        waitForPageToLoad();
    }

    public void doTestSteps()
    {
        RReportHelper.ensureRConfig(this);
        setUpList(PROJECT_NAME);
        CustomizeViewsHelper.createRView(this, null, rViewName);
        filterTest();
        facetedFilterTest();
    }

    private void startFilter()
    {
        click(Locator.tagWithText("div", "Color"));
        click(Locator.tagWithText("span", "Filter..."));
        ExtHelper.waitForExtDialog(this, "Show Rows Where Color...");
        waitForText("[All]");
        sleep(400);
    }

    private void facetedFilterTest()
    {
        createList2();
        assertTextPresent("Light", "Robust");
        startFilter();

        log("Verifying expected faceted filter elements present");
        assertTextPresent("Choose Filters", "Choose Values");

        assertTextPresent("Light", 2);
        assertTextPresent("Robust", 2);

        ExtHelper.clickExtButton(this, "OK");
        assertTextPresent("Light", "Robust");

        setFacetedFilter("query", "Color", "Light");
        assertTextPresent("Light");
        assertTextNotPresent("Robust");

        setFacetedFilter("query", "Color", "Robust");
        assertTextNotPresent("Light");
        assertTextPresent("Robust");

        setFacetedFilter("query", "Color");
        assertTextPresent("Light");
        assertTextPresent("Robust");
    }


    protected void filterTest()
    {
        log("Filter Test");

        invalidFiltersGenerateCorrectErrorTest();

        validFiltersGenerateCorrectResultsTest();

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
        pressEnter("//input[@id='value_1']");
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
        Object[][] ret = {
                //String columnName, String filter1Type, String filter1, String filter2Type, String filter2, String[] textPresentAfterFilter, String[] textNotPresentAfterFilter,
                //Issue 12197
                {_listCol4.getName(), "Equals One Of (e.g. \"a;b;c\")", "7;9", null, null, new String[] {TEST_DATA[1][3],TEST_DATA[1][1]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][0]}},
                {_listCol1.getName(), "Equals", TEST_DATA[1][0], null, null, new String[] {TEST_DATA[1][0]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][3]}},
                {_listCol1.getName(), "Starts With", "Z", null, null, new String[] {TEST_DATA[1][3]}, new String[] {TEST_DATA[1][0],TEST_DATA[1][1],TEST_DATA[1][2]}},
                {_listCol1.getName(), "Does Not Start With", "Z", null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][0]}, new String[] {TEST_DATA[1][3]}},
                //can't check for the absence of thing you're excluding, since it will be present in the filter text
                {_listCol1.getName(), "Does Not Equal", TEST_DATA[1][0], null, null, new String[] {TEST_DATA[1][2],TEST_DATA[1][1],TEST_DATA[1][3]}, new String[] {TEST_DATA[5][0]}},
                {_listCol3.getName(), "Equals", "true", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][1]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3]}},
                {_listCol3.getName(), "Does Not Equal", "false", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][1]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3]}},
                //filter is case insensitive
                {_listCol6.getName(), "Contains", "e", "Contains", "r", new String[] {TEST_DATA[5][2],TEST_DATA[5][0], TEST_DATA[5][1]}, new String[] {TEST_DATA[1][3]}},
//                {_listCol2.getName(), "Is Greater Than", "2", "Is Less Than or Equal To", "4", new String[] {TEST_DATA[1][2]}, new String[] {TEST_DATA[1][0],TEST_DATA[1][1],TEST_DATA[1][3]}},
                {_listCol4.getName(), "Is Greater Than Or Equal To", "9", null, null, new String[] {TEST_DATA[1][0],TEST_DATA[1][1]}, new String[] {TEST_DATA[1][2],TEST_DATA[1][3]}},
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
        log("Filtering " + columnName + " with filter type: " + filter1Type + ".  value: " + filter1.getClass());
        if(filter2Type!=null)
            log("Second filter: " + filter2Type + ".  value:" + filter2);
        setFilter(TABLE_NAME, columnName, filter1Type, filter1, filter2Type, filter2);

        checkFilterWasApplied(textPresentAfterFilter, textNotPresentAfterFilter, columnName, filter1Type, filter1, filter2Type, filter2);


        log("checking filter present in R view");
        clickMenuButton("Views", rViewName);
        sleep(1000);
        checkFilterWasApplied(textPresentAfterFilter, textNotPresentAfterFilter, columnName, filter1Type, filter1, filter2Type, filter2);

        clickMenuButton("Views", "default");

        //open filter
        runMenuItemHandler(TABLE_NAME + ":" + columnName + ":filter");

        if(filter1!=null)
        {
            assertEquals("Filter 1 value was not populated when reopening.", getFormElement("value_1"), filter1);
        }

        if(filter2!=null)
            assertEquals("Filter 2 value was not populated when reopening.", getFormElement("value_2"), filter2);

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
