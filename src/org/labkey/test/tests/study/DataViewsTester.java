/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.test.tests.study;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.tests.StudyBaseTest;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Feb 29, 2012
 */
public class DataViewsTester
{
    private static final String REPORT_NAME = "TestReport";
    private static final String WEBPART_TITLE = "TestDataViews";
    private static final String NEW_CATEGORY = "A New Category";
    private static final String EDITED_DATASET = "CPS-1: Screening Chemistry Panel";
    private static final String NEW_DESCRIPTION = "Description set in data views webpart";
    private static final String[] BITS = {"ABCD", "EFGH", "IJKL", "MNOP", "QRST", "UVWX"};
    private static final String[] CATEGORIES = {
            BITS[0]+BITS[1]+ BaseSeleniumWebTest.TRICKY_CHARACTERS_NO_QUOTES,
            BITS[1]+BITS[2]+BaseSeleniumWebTest.TRICKY_CHARACTERS_NO_QUOTES,
            BITS[2]+BITS[3]+BaseSeleniumWebTest.TRICKY_CHARACTERS_NO_QUOTES,
            BITS[3]+BITS[4]+BaseSeleniumWebTest.TRICKY_CHARACTERS_NO_QUOTES,
            BITS[4]+BITS[5]+BaseSeleniumWebTest.TRICKY_CHARACTERS_NO_QUOTES
    };
    private static final String[] someDataSets = {
            "Data Views",
            "DEM-1: Demographics",
            "URF-1: Follow-up Urinalysis (Page 1)",
            CATEGORIES[3],
            "AE-1:(VTN) AE Log"
    };
    private static final String[][] datasets = {
            {"CPS-1: Screening Chemistry Panel", "Unlocked"},
            {"ECI-1: Eligibility Criteria", "Draft"},
            {"MV-1: Missed Visit", "Final"},
            {"PT-1: Participant Transfer", "Locked"}
    };

    private StudyBaseTest _test;
    private String _folderName;

    public DataViewsTester(StudyBaseTest test, String folderName)
    {
        _test = test;
        _folderName = folderName;
    }

    public void basicTest()
    {
        _test.log("Data Views Test");
        _test.clickLinkContainingText("Data & Reports");
        _test.waitForText(someDataSets[3]);
        _test.assertTextPresent("Data Views", "Name", "Type", "Access");

        assertDataDisplayedAlphabetically();

        //TODO:  waiting on hypermove fix
//        datasetBrowseClickDataTest();

        _test.log("Verify dataset category sorting.");
        setDataBrowseSearch(BITS[4]);
        _test.waitForTextToDisappear(BITS[2]);
        _test.assertTextNotPresent(CATEGORIES[0]);
        _test.assertTextNotPresent(CATEGORIES[1]);
        _test.assertTextNotPresent(CATEGORIES[2]);
        _test.assertEquals("Incorrect number of dataset categories visible.", 2, _test.getXpathCount(Locator.xpath("//div[contains(@class, 'x4-grid-group-title')]"))); // Two categories contain filter text.
        // 10 datasets(CATEGORIES[3]) + 7 datasets(CATEGORIES[4]) - 1 hidden dataset == 16?
        _test.assertEquals("Incorrect number of datasets after filter", 16, _test.getXpathCount(Locator.xpath("//tr[contains(@class, 'x4-grid-row')]")));
        collapseCategory(CATEGORIES[3]);
        _test.assertEquals("Incorrect number of datasets after collapsing category.", 6, _test.getXpathCount(Locator.xpath("//tr[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and contains(@class, 'x4-grid-row')]")));
        _test.clickWebpartMenuItem("Data Views", false, "Customize");
        _test.waitForElement(Locator.button("Manage Categories"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        ExtHelper.uncheckCheckbox(_test, "datasets");
        _test.setFormElement(Locator.name("webpart.title"), WEBPART_TITLE);
        _test.clickButton("Save", 0);
        setDataBrowseSearch("");
        _test.waitForElement(Locator.linkWithText(REPORT_NAME), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.assertTextPresent(WEBPART_TITLE);
        _test.assertEquals("Incorrect number of datasets after filter", 1, _test.getXpathCount(Locator.xpath("//tr[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and contains(@class, 'x4-grid-row')]")));

        _test.log("Verify cancel button");
        _test.clickWebpartMenuItem("Data Views", false, "Customize");
        _test.waitForElement(Locator.button("Manage Categories"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        ExtHelper.checkCheckbox(_test, "datasets");
        ExtHelper.uncheckCheckbox(_test, "reports");
        _test.setFormElement(Locator.name("webpart.title"), "nothing");
        _test.clickButton("Cancel", 0);
        _test.sleep(500);               //TODO: \
        _test.refresh();                //TODO:  |remove: 13265: Data views webpart admin cancel button doesn't reset form
        _test.waitForText(REPORT_NAME); //TODO: /
        _test.assertTextNotPresent("nothing");
        _test.assertTextPresent(WEBPART_TITLE);
        _test.assertEquals("Incorrect number of datasets after filter", 1, _test.getXpathCount(Locator.xpath("//tr[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and contains(@class, 'x4-grid-row')]")));

        _test.log("Verify category management");
        _test.clickWebpartMenuItem(WEBPART_TITLE, false, "Customize");
        _test.waitForElement(Locator.button("Manage Categories"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        ExtHelper.checkCheckbox(_test, "datasets");
        _test.clickNavButton("Manage Categories", 0);
        ExtHelper.waitForExtDialog(_test, "Manage Categories");
        _test.waitAndClick(Locator.xpath("//img[@data-qtip='Delete']"));
        ExtHelper.waitForExtDialog(_test, "Delete Category");
        _test.clickNavButton("OK", 0);
        _test.clickNavButton("New Category", 0);
        _test.setFormElement(Locator.xpath("(//input[contains(@class, 'form-field') and @type='text'])[5]"), "testcategory"); // TODO: need a better xpath
        _test.clickNavButton("Done", 0);
        _test.clickNavButton("Save", 0);
        _test.waitForText(CATEGORIES[1], BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.refresh(); // Deleted category is still present, but hidden.  Refresh to clear page.
        _test.waitForText(CATEGORIES[1], BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.assertTextNotPresent(CATEGORIES[0]);
        _test.assertTextPresentInThisOrder(CATEGORIES[2], CATEGORIES[3], "Uncategorized", "APX-1", REPORT_NAME);

        _test.log("Verify modify dataset");
        _test.click(Locator.xpath("//span[contains(@class, 'edit-views-link')]"));
        ExtHelper.waitForExtDialog(_test, EDITED_DATASET);
        _test.setFormElement(Locator.xpath("//label[text() = 'Category']/../..//input"), NEW_CATEGORY);
        _test.setFormElement(Locator.name("description"), NEW_DESCRIPTION);
        ExtHelper.clickExtButton(_test, EDITED_DATASET, "Save", 0);
        _test.waitForText(NEW_CATEGORY);
        _test.clickLinkWithText(EDITED_DATASET);
        _test.assertTextPresent(NEW_DESCRIPTION);
    }

    public void datasetStatusTest()
    {
        _test.log("Testing status settings for datasets");
        _test.clickLinkContainingText("Data & Reports");
        _test.waitForText(someDataSets[3]);
        _test.assertTextPresent("Data Views", "Name", "Type", "Access");

        _test.clickWebpartMenuItem(WEBPART_TITLE, false, "Customize");
        _test.waitForElement(Locator.button("Manage Categories"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        ExtHelper.checkCheckbox(_test, "Status");
        _test.clickNavButton("Save", 0);
        _test.clickLinkContainingText("Data & Reports");

        for (String[] entry : datasets)
        {
            clickCustomizeView(entry[0]);

//            Locator.XPathLocator comboParent = Locator.xpath("//label[contains(text(), 'Status')]/../..");
            Ext4Helper.selectComboBoxItem(_test, "Status", entry[1]);

            _test.clickNavButton("Save", 0);

            Locator statusLink = Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//a[contains(text(), '" + entry[0] + "')]/../../../..//img[@alt='" + entry[1] + "']");
            _test.waitForElement(statusLink, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

            // visit the dataset page and make sure we inject the correct class onto the page
            _test.log("Verify dataset view has the watermark class");
            _test.click(Locator.xpath("//a[contains(text(), '" + entry[0] + "')]"));

            _test.waitForElement(Locator.xpath("//td[contains(@class, 'labkey-proj') and contains(@class, 'labkey-dataset-status-" + entry[1].toLowerCase() + "')]"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            _test.clickLinkContainingText("Data & Reports");
        }
    }

    private void clickCustomizeView(String viewName)
    {
        Locator editLink = Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//a[contains(text(), '" + viewName + "')]/../../../..//span[contains(@class, 'edit-views-link')]");
        _test.waitForElement(editLink, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.click(editLink);
        
        ExtHelper.waitForExtDialog(_test, viewName);
    }

    //Issue 12914: dataset browse web part not displaying data sets alphabetically
    private void assertDataDisplayedAlphabetically()
    {
        //ideally we'd grab the test names from the normal dataset part, but that would take a lot of time to write
        _test.assertTextPresentInThisOrder(someDataSets);
    }

    private void setDataBrowseSearch(String value)
    {
        _test.setFormElement(Locator.xpath("//table[contains(@class, 'dataset-search')]//input"), value);
    }

    private void collapseCategory(String category)
    {
        _test.log("Collapse category: " + category);
        _test.assertElementPresent(Locator.xpath("//div[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and @class='x4-grid-group-title' and contains(text(), '" + category + "')]"));
        _test.click(Locator.xpath("//div[@class='x4-grid-group-title' and contains(text(), '" + category + "')]"));
        _test.waitForElement(Locator.xpath("//tr[contains(@class, 'collapsed')]//div[@class='x4-grid-group-title' and contains(text(), '" + category + "')]"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    private void expandCategory(String category)
    {
        _test.log("Expand category: " + category);
        _test.assertElementPresent(Locator.xpath("//div[ancestor-or-self::tr[contains(@class, 'collapsed')] and @class='x4-grid-group-title' and text()='" + category + "']"));
        _test.click(Locator.xpath("//div[@class='x4-grid-group-title' and text()='" + category + "']"));
        _test.waitForElement(Locator.xpath("//div[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and @class='x4-grid-group-title' and text()='" + category + "']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public void refreshDateTest()
    {
        _test.log("Verify refresh date");
        String refreshDate = "2012-03-01";
        _test.clickLinkContainingText("Data & Reports");
        _test.waitForText(someDataSets[3]);
        // Refresh date not present when not set.
        _test.mouseOver(Locator.linkWithText(EDITED_DATASET));
        _test.waitForText("Type:");
        _test.assertTextNotPresent("Data Cut Date:");
        _test.clickWebpartMenuItem(WEBPART_TITLE, false, "Customize");
        _test.waitForElement(Locator.button("Manage Categories"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        ExtHelper.checkCheckbox(_test, "Modified");
        ExtHelper.checkCheckbox(_test, "Data Cut Date");
        Locator manageButton = _test.getButtonLocator("Manage Categories");
        _test.clickButton("Save", 0);
        _test.waitForElementToDisappear(manageButton, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForText("Data Cut Date");
        _test.waitForText("Modified");
        _test.click(Locator.xpath("//span[contains(@class, 'edit-views-link')]"));
        ExtHelper.waitForExtDialog(_test, EDITED_DATASET);
        _test.setFormElement("refreshDate", refreshDate);
        ExtHelper.clickExtButton(_test, EDITED_DATASET, "Save", 0);
        _test.waitForText(refreshDate, 1, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        // check hover box
        _test.mouseOver(Locator.linkWithText(EDITED_DATASET));
        _test.waitForText("Data Cut Date:");
        _test.assertTextPresent("2012-03-01");
        _test.clickLinkWithText(EDITED_DATASET);
        _test.assertTextPresent("2012-03-01");
    }
}
