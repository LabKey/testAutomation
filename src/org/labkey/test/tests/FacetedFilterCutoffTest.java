/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.util.List;

@Category(DailyA.class)
public class FacetedFilterCutoffTest extends BaseWebDriverTest
{
    public static final int MAX_FACETS = 250;
    private static final String LIST_NAME = "LongTestList";
    private static final String OVER_CUTOFF = "TooMany";
    private static final String AT_CUTOFF = "JustEnough";

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        FacetedFilterCutoffTest initTest = (FacetedFilterCutoffTest)getCurrentTest();
        initTest.doSetupSteps();
    }

    @LogMethod
    private void doSetupSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Lists");
        ListHelper.ListColumn col1 = new ListHelper.ListColumn(OVER_CUTOFF, OVER_CUTOFF, ListHelper.ListColumnType.Integer, "");
        ListHelper.ListColumn col2 = new ListHelper.ListColumn(AT_CUTOFF, AT_CUTOFF, ListHelper.ListColumnType.Integer, "");
        _listHelper.createList(getProjectName(), LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", col1, col2);
        _listHelper.clickImportData();
        _listHelper.submitTsvData(getListData());
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(LIST_NAME));
        assertElementPresent(Locator.paginationText(1, 100, MAX_FACETS + 1));
    }

    @Test
    public void testFacetCutoff()
    {
        Locator advancedFilteringLoc = Locator.name("value_1");
        Locator.XPathLocator factedFilterLoc = Locator.linkContainingText("[All]");
        DataRegionTable listDataRegion = new DataRegionTable("query", this);

        listDataRegion.openFilterDialog(AT_CUTOFF);
        assertElementNotVisible(advancedFilteringLoc);
        assertElementVisible(factedFilterLoc);
        click(Locator.button("Cancel"));
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

        listDataRegion.openFilterDialog(OVER_CUTOFF);
        assertElementVisible(advancedFilteringLoc);
        assertElementNotVisible(factedFilterLoc);
        click(Locator.button("Cancel"));
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    @Test
    public void testPreFilteredFacetCutoff()
    {
        Locator advancedFilteringLoc = Locator.name("value_1");
        Locator.XPathLocator factedFilterLoc = Locator.linkContainingText("[All]");
        DataRegionTable listDataRegion = new DataRegionTable("query", this);

        listDataRegion.setFilter(AT_CUTOFF, "Does Not Equal", "2");
        waitForElement(Locator.paginationText(1, 100, MAX_FACETS));

        listDataRegion.openFilterDialog(OVER_CUTOFF);
        assertElementVisible(factedFilterLoc);
        assertElementNotVisible(advancedFilteringLoc);
        click(Locator.button("Cancel"));
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Create tsv for a list with two columns
     * 1st column: MAX_FACETS+1 unique values to force faceted filter panel to default to "Choose Filters" tab
     * 2nd column: MAX_FACETS unique values
     */
    private String getListData()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(OVER_CUTOFF).append("\t").append(AT_CUTOFF).append("\n");
        for (int i = 1; i <= MAX_FACETS; i++)
        {
            sb.append(i).append("\t").append(i).append("\n");
        }
        sb.append(MAX_FACETS + 1).append("\t").append(1);

        return sb.toString();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
