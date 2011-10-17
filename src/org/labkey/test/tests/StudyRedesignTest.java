/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 8/16/11
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class StudyRedesignTest extends StudyBaseTest
{

    private static final String DATA_BROWSE_TABLE_NAME = "";
    private static final String[] BITS = {"ABCD", "EFGH", "IJKL", "MNOP", "QRST", "UVWX"};
    private static final String[] CATEGORIES = {BITS[0]+BITS[1]+TRICKY_CHARACTERS_NO_QUOTES, BITS[1]+BITS[2]+TRICKY_CHARACTERS_NO_QUOTES,
            BITS[2]+BITS[3]+TRICKY_CHARACTERS_NO_QUOTES, BITS[3]+BITS[4]+TRICKY_CHARACTERS_NO_QUOTES, BITS[4]+BITS[5]+TRICKY_CHARACTERS_NO_QUOTES};
    private static final String[] someDataSets = {"Data Views","DEM-1: Demographics", "URF-1: Follow-up Urinalysis (Page 1)", "Category: " + CATEGORIES[3], "AE-1:(VTN) AE Log"};

    @Override
    protected void doCreateSteps()
    {

        importStudy();
        startSpecimenImport(2);

        // wait for study and specimens to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);
        waitForSpecimenImport();
        setStudyRedesign();
        setupDatasetCategories();
    }

    @Override
    protected void doVerifySteps()
    {
        datasetBrowserWebPartTest();
    }

    private void datasetBrowserWebPartTest()
    {
        log("Data Views Test");
        clickLinkContainingText("Data Analysis");
        waitForText(someDataSets[3]);
        assertTextPresent("Data Views", "Name", "Type", "Access");

        assertDataDisplayedAlphabetically();

        //TODO:  waiting on hypermove fix
//        datasetBrowseClickDataTest();

        log("Verify dataset category sorting.");
        setDataBrowseSearch(BITS[4]);
        waitForTextToDisappear(BITS[2]);
        assertTextPresent("Category: ", 2); // Two categories contain text.
        // 10 datasets(CATEGORIES[3]) + 7 datasets(CATEGORIES[4]) - 1 hidden dataset
        assertEquals("Incorrect number of datasets after filter", 17, getXpathCount(Locator.xpath("//tr[contains(@class, 'x4-grid-row')]")));
        collapseCategory(CATEGORIES[3]);
        assertEquals("Incorrect number of datasets after filter", 7, getXpathCount(Locator.xpath("//tr[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and contains(@class, 'x4-grid-row')]")));
    }

    private void clickExt4HeaderMenu(String title, String selection)
    {
        click(Locator.xpath("//div[./span[@class='x4-column-header-text' and text() = '"+title+"']]/div[@class='x4-column-header-trigger']"));
        click(Locator.xpath("//div[@role='menuitem']/a/span[text()='"+selection+"']"));
    }

    private void setDataBrowseSearch(String value)
    {
        setFormElement(Locator.xpath("//div[contains(@class, 'dataset-search')]//input"), value);
    }

    private void collapseCategory(String category)
    {
        log("Collapse category: " + category);
        assertElementPresent(Locator.xpath("//div[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and @class='x4-grid-group-title' and text()='Category: " + category + "']"));
        click(Locator.xpath("//div[@class='x4-grid-group-title' and text()='Category: " + category + "']"));
        waitForElement(Locator.xpath("//tr[contains(@class, 'collapsed')]//div[@class='x4-grid-group-title' and text()='Category: " + category + "']"), WAIT_FOR_JAVASCRIPT);
    }

    private void expandCategory(String category)
    {
        log("Expand category: " + category);
        assertElementPresent(Locator.xpath("//div[ancestor-or-self::tr[contains(@class, 'collapsed')] and @class='x4-grid-group-title' and text()='Category: " + category + "']"));
        click(Locator.xpath("//div[@class='x4-grid-group-title' and text()='Category: " + category + "']"));
        waitForElement(Locator.xpath("//div[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and @class='x4-grid-group-title' and text()='Category: " + category + "']"), WAIT_FOR_JAVASCRIPT);
    }

    private void datasetBrowseClickDataTest()
    {

        log("Test click behavior for datasets");

        Object[][] vals = getDPDataOnClick();

        for(Object[] val : vals)
        {
            clickSingleDataSet((String) val[0], (String) val[1], (String) val[2], true);
        }
    }

    private Object[][] getDPDataOnClick()
    {
        //TODO when hypermove fixed
        Object[][] ret = {
                {"AE-1:(VTN) AE Log", "", ""},
        };
        return ret;  //To change body of created methods use File | Settings | File Templates.
    }

    private void setupDatasetCategories()
    {
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Change Properties");

        int dsCount = getXpathCount(Locator.xpath("//input[@name='extraData']"));
        assertEquals("Unexpected number of Datasets.", 47, dsCount);
        int i;
        for (i = 0; i < dsCount; i++)
        {
            setFormElement(Locator.name("extraData", i), CATEGORIES[i/10]);
        }
        uncheckCheckbox("visible", dsCount - 1); // Set last dataset to not be visible.
        clickNavButton("Save");
    }

    private void clickSingleDataSet(String title, String source, String type, boolean testDelete)
    {
        Locator l = Locator.tagWithText("div", title);
        click(l);
        assertTextPresentInThisOrder(title, "Source: " + source, "Type: " + type);
        clickButtonContainingText("View", 0);
        assertTextPresent(title);
        selenium.goBack();
        if(testDelete)
        {
            //this feature is scheduled for removal
        }
    }

     private void clickSingleDataSet(String title, String source, String type)
    {
       clickSingleDataSet(title, source, type, false);
    }

    //Issue 12914: dataset browse web part not displaying data sets alphabetically
    private void assertDataDisplayedAlphabetically()
    {
      //ideally we'd grab the test names from the normal dataset part, but that would take a lot of time to write
        assertTextPresentInThisOrder(someDataSets);
    }
}
