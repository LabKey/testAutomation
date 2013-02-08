/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.tests.StudyBaseTest;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;

/**
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

    @LogMethod
    public void basicTest()
    {
        _test.log("Data Views Test");
        _test.clickAndWait(Locator.linkContainingText("Data & Reports"));
        _test.waitForText(someDataSets[3]);
        _test.assertTextPresent("Data Views", "Name", "Type", "Access");

        assertDataDisplayedAlphabetically();

        //TODO:  waiting on hypermove fix
//        datasetBrowseClickDataTest();

        _test.log("Verify dataset category filtering.");
        setDataBrowseSearch(BITS[3]);
        _test.waitForTextToDisappear(BITS[1]);
        _test.assertTextNotPresent(CATEGORIES[0]);
        _test.assertTextNotPresent(CATEGORIES[1]);
        _test.assertTextNotPresent(CATEGORIES[4]);
        Assert.assertEquals("Incorrect number of dataset categories visible.", 4, _test.getXpathCount(Locator.xpath("//td").withClass("dvcategory").notHidden())); // Two categories contain filter text.
        Assert.assertEquals("Incorrect number of datasets after filter", 22, _test.getXpathCount(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden()));
        collapseCategory("Subcategory1-" + CATEGORIES[2]);
        Assert.assertEquals("Incorrect number of datasets after collapsing subcategory.", 21, _test.getXpathCount(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden()));
        Assert.assertEquals("Incorrect number of dataset categories visible after collapsing subcategory.", 4, _test.getXpathCount(Locator.xpath("//td").withClass("dvcategory").notHidden()));
        collapseCategory(CATEGORIES[2]);
        Assert.assertEquals("Incorrect number of datasets after collapsing category.", 10, _test.getXpathCount(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden()));
        Assert.assertEquals("Incorrect number of dataset categories visible after collapsing category.", 2, _test.getXpathCount(Locator.xpath("//td").withClass("dvcategory").notHidden()));
        openCustomizePanel();
        _test._extHelper.uncheckCheckbox("datasets");
        _test.setFormElement(Locator.name("webpart.title"), WEBPART_TITLE);
        _test.clickButton("Save", 0);
        _test._extHelper.waitForLoadingMaskToDisappear(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        setDataBrowseSearch("");
        _test.waitForElement(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden());
        _test.waitForElement(Locator.linkWithText(REPORT_NAME), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.assertTextPresent(WEBPART_TITLE);
        Assert.assertEquals("Incorrect number of datasets after filter", 1, _test.getXpathCount(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden()));

        _test.log("Verify cancel button");
        openCustomizePanel();
        _test._extHelper.checkCheckbox("datasets");
        _test._extHelper.uncheckCheckbox("reports");
        _test.setFormElement(Locator.name("webpart.title"), "nothing");
        _test.clickButton("Cancel", 0);
        _test.sleep(500);               //TODO: \
        _test.refresh();                //TODO:  |remove: 13265: Data views webpart admin cancel button doesn't reset form
        _test.waitForText(REPORT_NAME); //TODO: /
        _test.assertTextNotPresent("nothing");
        _test.assertTextPresent(WEBPART_TITLE);
        Assert.assertEquals("Incorrect number of datasets after filter", 1, _test.getXpathCount(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden()));

        _test.log("Verify category management");
        openCustomizePanel();
        _test._extHelper.checkCheckbox("datasets");
        _test.clickButton("Manage Categories", 0);
        _test._extHelper.waitForExtDialog("Manage Categories");
        _test.waitAndClick(Locator.xpath("//img[@data-qtip='Delete']"));
        _test._extHelper.waitForExtDialog("Delete Category");
        _test.clickButton("OK", 0);
        _test._extHelper.waitForExtDialogToDisappear("Delete Category");
        _test.waitForElementToDisappear(Locator.xpath("(//input[contains(@class, 'form-field') and @type='text'])[" + CATEGORIES.length + "]"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.clickButton("New Category", 0);
        _test.waitForElement(Locator.xpath("(//input[contains(@class, 'form-field') and @type='text'])[" + CATEGORIES.length + "]"));
        _test.setFormElement(Locator.xpath("(//input[contains(@class, 'form-field') and @type='text'])[" + CATEGORIES.length + "]"), NEW_CATEGORY);
        _test.clickButton("Done", 0);
        _test.clickButton("Save", 0);
        _test.waitForText(CATEGORIES[1], BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.refresh(); // Deleted category is still present, but hidden.  Refresh to clear page.
        _test.waitForText(CATEGORIES[1], BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.assertTextNotPresent(CATEGORIES[0]);
        _test.assertTextPresentInThisOrder(CATEGORIES[2], CATEGORIES[3], "Uncategorized", REPORT_NAME, "APX-1");

        _test.log("Verify modify dataset");
        openCustomizePanel();
        editDatasetProperties(EDITED_DATASET);
        _test.setFormElement(Locator.name("description"), NEW_DESCRIPTION);
        saveDatasetProperties(EDITED_DATASET);
        _test.mouseOver(Locator.linkWithText(EDITED_DATASET));
        _test.waitForElement(Locator.css(".data-views-tip-content"));
        Assert.assertEquals("Dataset hover tip not as expected", EDITED_DATASET_TOOLTIP, _test.getText(Locator.css(".data-views-tip-content")));
        _test.clickAndWait(Locator.linkWithText(EDITED_DATASET));
        _test.assertTextPresent(NEW_DESCRIPTION);
    }
    private final static String EDITED_DATASET_TOOLTIP = "Source:Subcategory1-EFGHIJKL></% 1Type:DatasetDescription:Description set in data views webpart";

    @LogMethod
    public void datasetStatusTest()
    {
        _test.log("Testing status settings for datasets");
        _test.clickAndWait(Locator.linkContainingText("Data & Reports"));
        _test.waitForText(someDataSets[3]);
        _test.assertTextPresent("Data Views", "Name", "Type", "Access");

        openCustomizePanel();
        _test._extHelper.checkCheckbox("Status");
        _test.clickButton("Save", 0);
        _test.clickAndWait(Locator.linkContainingText("Data & Reports"));

        for (String[] entry : datasets)
        {
            openCustomizePanel();
            editDatasetProperties(entry[0]);

            _test._ext4Helper.selectComboBoxItem("Status", entry[1]);

            _test._extHelper.clickExtButton(entry[0], "Save", 0);

            Locator statusLink = Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//a[contains(text(), '" + entry[0] + "')]/../../../../..//div//img[@alt='" + entry[1] + "']");
            _test.waitForElement(statusLink, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

            // visit the dataset page and make sure we inject the correct class onto the page
            _test.log("Verify dataset view has the watermark class");
            _test.click(Locator.xpath("//a[contains(text(), '" + entry[0] + "')]"));

            _test.waitForElement(Locator.xpath("//td[contains(@class, 'labkey-proj') and contains(@class, 'labkey-dataset-status-" + entry[1].toLowerCase() + "')]"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            _test.clickAndWait(Locator.linkContainingText("Data & Reports"));
        }
    }

    private void openCustomizePanel()
    {
        _test.waitAndClick(Locator.css("a>img[title=Edit]"));
        _test.waitForElement(Locator.button("Manage Categories"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void clickCustomizeView(String viewName, BaseSeleniumWebTest test)
    {
        Locator editLink = Locators.editViewsLink(viewName);
        test.waitAndClick(editLink);
        
        test._extHelper.waitForExtDialog(viewName);
    }

    public static Locator getEditLinkLocator(String viewName)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//a[contains(text(),'" + viewName + "')]/../../../../..//td//div//span[contains(@class, 'edit-views-link')]");
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

    @LogMethod
    private void collapseCategory(@LoggedParam String category)
    {
        Locator.XPathLocator dataViewRow = Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden();
        int dataViewCount = _test.getXpathCount(dataViewRow);
        _test.assertElementPresent(Locator.xpath("//tr").withClass("x4-grid-tree-node-expanded").append("/td/div").withText(category));
        _test.click(Locator.xpath("//div").withText(category).append("/img").withClass("x4-tree-expander"));
        _test.waitForElementToDisappear(dataViewRow.index(dataViewCount - 1), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    private void expandCategory(@LoggedParam String category)
    {
        Locator.XPathLocator dataViewRow = Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden();
        int dataViewCount = _test.getXpathCount(dataViewRow);
        _test.assertElementNotPresent(Locator.xpath("//tr").withClass("x4-grid-tree-node-expanded").append("/td/div").withText(category));
        _test.click(Locator.xpath("//div").withText(category).append("/img").withClass("x4-tree-expander"));
        _test.waitForElement(dataViewRow.index(dataViewCount));
    }

    @LogMethod
    public void refreshDateTest()
    {
        _test.log("Verify refresh date");
        String refreshDate = "2012-03-01";
        _test.clickAndWait(Locator.linkContainingText("Data & Reports"));
        _test.waitForText(someDataSets[3]);
        // Refresh date not present when not set.
        _test.mouseOver(Locator.linkWithText(EDITED_DATASET));
        _test.waitForText("Type:");
        _test.assertTextNotPresent("Data Cut Date:");
        _test.mouseOut(Locator.linkWithText(EDITED_DATASET)); // Dismiss hover box
        _test.waitForTextToDisappear("Type:");
        openCustomizePanel();
        _test._extHelper.checkCheckbox("Modified");
        _test._extHelper.checkCheckbox("Data Cut Date");
        Locator manageButton = _test.getButtonLocator("Manage Categories");
        _test.clickButton("Save", 0);
        _test.waitForElementToDisappear(manageButton, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForText("Data Cut Date");
        _test.waitForText("Modified");
        openCustomizePanel();
        editDatasetProperties(EDITED_DATASET);
        _test._extHelper.waitForExtDialog(EDITED_DATASET);
        _test.setFormElement("refreshDate", refreshDate);
        _test._extHelper.clickExtButton(EDITED_DATASET, "Save", 0);
        _test.waitForText(refreshDate, 1, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        // check hover box
        _test.mouseOver(Locator.linkWithText(EDITED_DATASET));
        _test.waitForText("Data Cut Date:");
        _test.assertTextPresent("2012-03-01");
        _test.clickAndWait(Locator.linkWithText(EDITED_DATASET));
        _test.assertTextPresent("2012-03-01");
    }

    private static final String CATEGORY_LIST =
            CATEGORIES[0]+
            CATEGORIES[1]+
                "Subcategory1-"+CATEGORIES[1]+
                "Subcategory2-"+CATEGORIES[1]+
            CATEGORIES[2]+
                "Subcategory1-"+CATEGORIES[2]+
                "Subcategory2-"+CATEGORIES[2]+
            CATEGORIES[3]+
            CATEGORIES[4];

    @LogMethod
    public void subcategoryTest()
    {
        _test.clickAndWait(Locator.linkContainingText("Data & Reports"));
        openCustomizePanel();
        _test.clickButton("Manage Categories", 0);
        _test._extHelper.waitForExtDialog("Manage Categories");

        _test.waitForElement(Ext4HelperWD.Locators.window("Manage Categories").append(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(CATEGORIES[1])));
        _test.mouseDown(Ext4HelperWD.Locators.window("Manage Categories").append(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(CATEGORIES[1])));
        _test._extHelper.waitForExtDialog("Subcategories");
        _test.assertElementPresent(Ext4HelperWD.Locators.window("Manage Categories").append("//tr").withClass("x4-grid-row-selected").withText(CATEGORIES[1]));

        _test.clickButton("New Subcategory", 0);
        _test.waitForElement(Locator.xpath("//input[@name='label']").notHidden());
        _test.setFormElement(Locator.name("label"), "Subcategory1-" + CATEGORIES[1]);
        _test.pressEnter(Locator.name("label"));
        _test.waitForElementToDisappear(Locator.xpath("//input[@name='label']").notHidden(), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.clickButton("New Subcategory", 0);
        _test.waitForElement(Locator.xpath("//input[@name='label']").notHidden());
        _test.setFormElement(Locator.name("label"), "Subcategory2-" + CATEGORIES[1]);
        _test.pressEnter(Locator.name("label"));
        _test.waitForElementToDisappear(Locator.xpath("//input[@name='label']").notHidden(), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        _test.mouseDown(Ext4HelperWD.Locators.window("Manage Categories").append(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(CATEGORIES[2])));
        _test.waitForElement(Ext4HelperWD.Locators.window("Manage Categories").append("//tr").withClass("x4-grid-row-selected").withText(CATEGORIES[2]));
        _test.assertTextNotPresent("Subcategory1-" + CATEGORIES[1], "Subcategory2-" + CATEGORIES[1]);

        _test.clickButton("New Subcategory", 0);
        _test.waitForElement(Locator.xpath("//input[@name='label']").notHidden());
        _test.setFormElement(Locator.name("label"), "Subcategory1-" + CATEGORIES[2]);
        _test.pressEnter(Locator.name("label"));
        _test.waitForElementToDisappear(Locator.xpath("//input[@name='label']").notHidden(), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.clickButton("New Subcategory", 0);
        _test.waitForElement(Locator.xpath("//input[@name='label']").notHidden());
        _test.setFormElement(Locator.name("label"), "Subcategory2-" + CATEGORIES[2]);
        _test.pressEnter(Locator.name("label"));
        _test.waitForElementToDisappear(Locator.xpath("//input[@name='label']").notHidden(), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        _test.mouseDown(Ext4HelperWD.Locators.window("Manage Categories").append(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(CATEGORIES[3])));
        _test.waitForElement(Ext4HelperWD.Locators.window("Manage Categories").append("//tr").withClass("x4-grid-row-selected").withText(CATEGORIES[3]));
        _test.assertTextNotPresent("Subcategory1-" + CATEGORIES[1], "Subcategory2-" + CATEGORIES[1], "Subcategory1-" + CATEGORIES[2], "Subcategory2-" + CATEGORIES[2]);

        _test._extHelper.clickExtButton("Manage Categories", "Done", 0);
        _test._extHelper.waitForExtDialogToDisappear("Manage Categories");
        editDatasetProperties("DEM-1: Demographics");
        _test.click(Locator.xpath("//tr[./td/input[@name='category']]/td/div").withClass("x4-form-arrow-trigger"));
        Assert.assertEquals("Available categories are not as expected", CATEGORY_LIST, _test.getText(Locator.css(".x4-boundlist")));
        saveDatasetProperties("DEM-1: Demographics");

        editDatasetProperties(datasets[0][0]);
        _test._ext4Helper.selectComboBoxItem("Category", "Subcategory1-" + CATEGORIES[1]);
        saveDatasetProperties(datasets[0][0]);

        editDatasetProperties(datasets[1][0]);
        _test._ext4Helper.selectComboBoxItem("Category", "Subcategory2-" + CATEGORIES[1]);
        saveDatasetProperties(datasets[1][0]);

        editDatasetProperties(datasets[2][0]);
        _test._ext4Helper.selectComboBoxItem("Category", "Subcategory1-" + CATEGORIES[2]);
        saveDatasetProperties(datasets[2][0]);

        editDatasetProperties(datasets[3][0]);
        _test._ext4Helper.selectComboBoxItem("Category", "Subcategory2-" + CATEGORIES[2]);
        saveDatasetProperties(datasets[3][0]);

        _test.assertTextPresentInThisOrder(
            CATEGORIES[0],
            CATEGORIES[1],
                "Subcategory1-"+CATEGORIES[1],
                datasets[0][0],
                "Subcategory2-"+CATEGORIES[1],
                datasets[1][0],
            CATEGORIES[2],
                "Subcategory1-"+CATEGORIES[2],
                datasets[2][0],
                "Subcategory2-"+CATEGORIES[2],
                datasets[3][0],
            CATEGORIES[3],
            CATEGORIES[4]);
    }

    @LogMethod
    public void categoryReorderTest()
    {
        throw new IllegalStateException("Not yet implemented");
//        _test.clickAndWait(Locator.linkContainingText("Data & Reports"));
//        openCustomizePanel();
//        _test.clickButton("Manage Categories", 0);
//        _test._extHelper.waitForExtDialog("Manage Categories");
    }

    private void editDatasetProperties(String dataset)
    {
        _test.waitAndClick(Locators.editViewsLink(dataset));
        _test._extHelper.waitForExtDialog(dataset);
        _test.waitForElement(Locator.xpath("//table").withClass("category-loaded-marker").notHidden());
    }

    public void saveDatasetProperties(String dataset)
    {
        _test._extHelper.clickExtButton(dataset, "Save", 0);
        _test._extHelper.waitForExtDialogToDisappear(dataset);
        _test._ext4Helper.waitForMaskToDisappear(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.css("table[name='data-browser-table'] .x4-grid-row"));
    }

    public static class Locators
    {
        public static Locator.XPathLocator editViewsLink(String dataset)
        {
            return Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").withDescendant(Locator.xpath("td/div/a[normalize-space()="+Locator.xq(dataset)+"]")).append("//span").withClass("edit-views-link");
        }
    }
}
