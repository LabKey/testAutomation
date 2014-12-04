/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.*;

//TODO: Add to DailyA suite: 20994: DataViewsTest is unreliable on TeamCity
@Category({InDevelopment.class})
public class DataViewsTest extends ParticipantListTest
{
    private static final String REPORT_NAME = "TestReport";
    private static final String RENAMED_WEBPART_TITLE = "TestDataViews";
    private static final String ORIGINAL_WEBPART_TITLE = "Data Views";
    private static final String REPORT_TO_DELETE = "Scatter: Systolic vs Diastolic";
    private static final String NEW_CATEGORY = "A New Category";
    private static final String NEW_DESCRIPTION = "Description set in data views webpart";
    private static final String[][] datasets = {
            {"CPS-1: Screening Chemistry Panel", "Unlocked"},
            {"ECI-1: Eligibility Criteria", "Draft"},
            {"MV-1: Missed Visit", "Final"},
            {"PT-1: Participant Transfer", "Locked"}
    };

    private final PortalHelper _portalHelper = new PortalHelper(this);

    @Override @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        RReportHelper reportHelper = new RReportHelper(this);
        reportHelper.ensureRConfig();

        importStudy();
        startSpecimenImport(2);

        // wait for study and specimens to finish loading
        waitForSpecimenImport();
        setStudyRedesign();
        setupDatasetCategories();
        log("Create report for data view webpart test.");
        goToModule("StudyRedesign");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "R View");
        clickButton("Save", "Please enter a view name:");

        Locator locator = Ext4Helper.Locators.window("Save View").append(Locator.xpath("//input[contains(@class, 'x4-form-field')]"));
        if (isElementPresent(locator))
        {
            setFormElement(locator, REPORT_NAME);
            _ext4Helper.clickWindowButton("Save View", "OK", WAIT_FOR_JAVASCRIPT, 0);
        }
    }

    @Override
    protected void doVerifySteps()
    {   
        subcategoryTest();
        basicTest();
        datasetStatusTest();
        refreshDateTest();
        exportImportTest();
    }
    
    @LogMethod
    public void basicTest()
    {
        log("Data Views Test");
        clickAndWait(Locator.linkContainingText("Data & Reports"));
        waitForText(someDataSets[3]);
        assertTextPresent("Data Views", "Name", "Type", "Access");

        assertDataDisplayedAlphabetically();

        //TODO:  waiting on hypermove fix
//        datasetBrowseClickDataTest();

        log("Verify dataset category filtering.");
        setDataBrowseSearch(BITS[3]);
        waitForElementToDisappear(Locator.tag("tr").withClass("x4-grid-row").containing(BITS[1]).notHidden());
        assertEquals("Incorrect number of dataset categories visible.", 4, getElementCount(Locator.xpath("//td").withClass("dvcategory").notHidden())); // Two categories contain filter text.
        assertEquals("Incorrect number of datasets after filter", 22, getElementCount(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden()));
        collapseCategory("Subcategory1-" + CATEGORIES[2]);
        assertEquals("Incorrect number of datasets after collapsing subcategory.", 21, getElementCount(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden()));
        assertEquals("Incorrect number of dataset categories visible after collapsing subcategory.", 4, getElementCount(Locator.xpath("//td").withClass("dvcategory").notHidden()));
        collapseCategory(CATEGORIES[2]);
        assertEquals("Incorrect number of datasets after collapsing category.", 10, getElementCount(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden()));
        assertEquals("Incorrect number of dataset categories visible after collapsing category.", 2, getElementCount(Locator.xpath("//td").withClass("dvcategory").notHidden()));
        openCustomizePanel(ORIGINAL_WEBPART_TITLE);
        _ext4Helper.uncheckCheckbox("datasets");
        setFormElement(Locator.name("webpart.title"), RENAMED_WEBPART_TITLE);
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear();
        setDataBrowseSearch("");
        waitForElement(Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden());
        waitForElement(Locator.linkWithText(REPORT_NAME), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(PortalHelper.Locators.webPartTitle(RENAMED_WEBPART_TITLE));
        final Locator displayedDataViewsRow = Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden();
        waitForElement(displayedDataViewsRow);
        assertEquals("Incorrect number of datasets after filter", 9, getElementCount(displayedDataViewsRow));

        log("Verify cancel button");
        openCustomizePanel(ORIGINAL_WEBPART_TITLE);
        _ext4Helper.checkCheckbox("datasets");
        _ext4Helper.uncheckCheckbox("reports");
        setFormElement(Locator.name("webpart.title"), "nothing");
        clickButton("Cancel", 0);
        sleep(500);               //TODO: \
        refresh();                //TODO:  |remove: 13265: Data views webpart admin cancel button doesn't reset form
        waitForText(REPORT_NAME); //TODO: /
        assertTextNotPresent("nothing");
        assertTextPresent(RENAMED_WEBPART_TITLE);
        waitForElement(displayedDataViewsRow);
        assertEquals("Incorrect number of datasets after filter", 9, getElementCount(displayedDataViewsRow));

        log("Verify category management: delete");
        openCustomizePanel(RENAMED_WEBPART_TITLE);
        _ext4Helper.checkCheckbox("datasets");
        clickButton("Manage Categories", 0);
        _extHelper.waitForExtDialog("Manage Categories");
        waitAndClick(Locator.xpath("//img[@data-qtip='Delete']"));
        _extHelper.waitForExtDialog("Delete Category");
        clickButton("OK", 0);
        _extHelper.waitForExtDialogToDisappear("Delete Category");
        waitForElementToDisappear(Locator.xpath("(//input[contains(@class, 'form-field') and @type='text'])[" + CATEGORIES.length + "]"), WAIT_FOR_JAVASCRIPT);
        clickButton("Done", 0);
        clickButton("Save", 0);

        log("Verify category management: create");
        refresh();
        openCustomizePanel(RENAMED_WEBPART_TITLE);
        clickButton("Manage Categories", 0);
        _extHelper.waitForExtDialog("Manage Categories");
        clickButton("New Category", 0);
        WebElement newCategoryField = Locator.xpath("//input[contains(@id, 'textfield') and @name='label']").notHidden().waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        setFormElement(newCategoryField, NEW_CATEGORY);
        fireEvent(newCategoryField, SeleniumEvent.blur);
        waitForElement(Ext4Helper.Locators.window("Manage Categories").append("//div").withText(NEW_CATEGORY));
        clickButton("Done", 0);
        _extHelper.waitForExtDialogToDisappear("Manage Categories");
        clickButton("Save", 0);
        waitForText(CATEGORIES[1], WAIT_FOR_JAVASCRIPT);
        refresh(); // Deleted category is still present, but hidden.  Refresh to clear page.
        waitForText(CATEGORIES[1], WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent(CATEGORIES[0]);
        assertTextPresentInThisOrder(CATEGORIES[2], CATEGORIES[3], "Uncategorized", REPORT_NAME, "APX-1");

        log("Verify modify dataset");
        enableEditMode();
        openEditPanel(EDITED_DATASET);
        setFormElement(Locator.name("description"), NEW_DESCRIPTION);
        saveDatasetProperties(EDITED_DATASET);
        mouseOver(Locator.linkWithText(EDITED_DATASET));
        waitForElement(Locator.css(".data-views-tip-content"));
//        assertEquals("Dataset hover tip not as expected", EDITED_DATASET_TOOLTIP, getText(Locator.css(".data-views-tip-content")));
        clickAndWait(Locator.linkWithText(EDITED_DATASET));
        assertTextPresent(NEW_DESCRIPTION);

        log("Verify report deletion");
        clickAndWait(Locator.linkContainingText("Data & Reports"));
        waitForElement(Locator.linkContainingText(REPORT_TO_DELETE));
        enableEditMode();
        openEditPanel(REPORT_TO_DELETE);
        waitForElement(Ext4Helper.Locators.ext4Button("Delete View"));
        clickButton("Delete View", 0);
        waitForText("Delete View?");
        clickButton("Yes", 0);
        waitForElementToDisappear(Locator.linkContainingText(REPORT_TO_DELETE));
    }
    private final static String EDITED_DATASET_TOOLTIP = "Source:Subcategory1-EFGHIJKL></% 1Type:DatasetStatus:NoneDescription:Description set in data views webpart";

    @LogMethod
    public void datasetStatusTest()
    {
        log("Testing status settings for datasets");
        clickAndWait(Locator.linkContainingText("Data & Reports"));
        waitForText(someDataSets[3]);
        assertTextPresent("Data Views", "Name", "Type", "Access");

        openCustomizePanel(RENAMED_WEBPART_TITLE);
        _ext4Helper.checkCheckbox("Status");
        clickButton("Save", 0);
        clickAndWait(Locator.linkContainingText("Data & Reports"));

        for (String[] entry : datasets)
        {
            enableEditMode();
            openEditPanel(entry[0]);

            _ext4Helper.selectComboBoxItem("Status", entry[1]);

            clickButton("Save", 0);

            Locator statusLink = Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//a[contains(text(), '" + entry[0] + "')]/../../../../..//div//img[@alt='" + entry[1] + "']");
            waitForElement(statusLink, WAIT_FOR_JAVASCRIPT);

            // visit the dataset page and make sure we inject the correct class onto the page
            log("Verify dataset view has the watermark class");
            click(Locator.xpath("//a[contains(text(), '" + entry[0] + "')]"));

            waitForElement(Locator.xpath("//table[contains(@class, 'labkey-proj') and contains(@class, 'labkey-dataset-status-" + entry[1].toLowerCase() + "')]"), WAIT_FOR_JAVASCRIPT);
            clickAndWait(Locator.linkContainingText("Data & Reports"));
        }
    }

    private void openCustomizePanel(String title)
    {
        _portalHelper.clickWebpartMenuItem(title, false, "Customize");
        waitForElement(Ext4Helper.Locators.ext4Button("Manage Categories"), WAIT_FOR_JAVASCRIPT);
    }

    private void closeCustomizePanel(String title)
    {
        _portalHelper.clickWebpartMenuItem(title, false, "Customize");
        waitForElementToDisappear(Ext4Helper.Locators.ext4Button("Manage Categories"), WAIT_FOR_JAVASCRIPT);
    }

    private void enableEditMode()
    {
        waitAndClick(Locator.css("a>img[title=Edit]"));
        waitForElement(Locator.css("span[class~=edit-views-link]"), WAIT_FOR_JAVASCRIPT);
    }

    public static void clickCustomizeView(String viewName, BaseWebDriverTest test)
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
        assertTextPresentInThisOrder(someDataSets);
    }

    private void setDataBrowseSearch(String value)
    {
        setFormElement(Locator.xpath("//table[contains(@class, 'dataset-search')]//input"), value);
    }

    @LogMethod
    private void collapseCategory(@LoggedParam String category)
    {
        Locator.XPathLocator dataViewRow = Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden();
        int dataViewCount = getElementCount(dataViewRow);
        assertElementPresent(Locator.xpath("//tr").withClass("x4-grid-tree-node-expanded").append("/td/div").withText(category));
        click(Locator.xpath("//div").withText(category).append("/img").withClass("x4-tree-expander"));
        waitForElementToDisappear(dataViewRow.index(dataViewCount - 1), WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    private void expandCategory(@LoggedParam String category)
    {
        Locator.XPathLocator dataViewRow = Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden();
        int dataViewCount = getElementCount(dataViewRow);
        assertElementNotPresent(Locator.xpath("//tr").withClass("x4-grid-tree-node-expanded").append("/td/div").withText(category));
        click(Locator.xpath("//div").withText(category).append("/img").withClass("x4-tree-expander"));
        waitForElement(dataViewRow.index(dataViewCount));
    }

    @LogMethod
    public void refreshDateTest()
    {
        log("Verify refresh date");
        String refreshDate = "2012-03-01";
        clickAndWait(Locator.linkContainingText("Data & Reports"));
        waitForText(someDataSets[3]);
        // Refresh date not present when not set.
        mouseOver(Locator.linkWithText(EDITED_DATASET));
        waitForText("Type:");
        assertTextNotPresent("Data Cut Date:");
//        mouseOut(Locator.linkWithText(EDITED_DATASET)); // Dismiss hover box
//        waitForTextToDisappear("Type:");
        openCustomizePanel(RENAMED_WEBPART_TITLE);
        _ext4Helper.checkCheckbox("Modified");
        _ext4Helper.checkCheckbox("Data Cut Date");
        Locator manageButton = getButtonLocator("Manage Categories");
        clickButton("Save", 0);
        waitForElementToDisappear(manageButton, WAIT_FOR_JAVASCRIPT);
        waitForText("Data Cut Date");
        waitForText("Modified");
        enableEditMode();
        openEditPanel(EDITED_DATASET);
        _extHelper.waitForExtDialog(EDITED_DATASET);
        setFormElement(Locator.name("refreshDate"), refreshDate);
        clickButton("Save", 0);
        waitForText(refreshDate, 1, WAIT_FOR_JAVASCRIPT);
        // check hover box
        mouseOver(Locator.linkWithText(EDITED_DATASET));
        waitForText("Data Cut Date:");
        assertTextPresent("2012-03-01");
        clickAndWait(Locator.linkWithText(EDITED_DATASET));
        assertTextPresent("2012-03-01");
    }

    private static final String CATEGORY_LIST =
            CATEGORIES[0]+ "\n" +
            CATEGORIES[1]+ "\n" +
                "Subcategory1-"+CATEGORIES[1]+ "\n" +
                "Subcategory2-"+CATEGORIES[1]+ "\n" +
            CATEGORIES[2]+ "\n" +
                "Subcategory1-"+CATEGORIES[2]+ "\n" +
                "Subcategory2-"+CATEGORIES[2]+ "\n" +
            CATEGORIES[3]+ "\n" +
            CATEGORIES[4];

    @LogMethod
    public void subcategoryTest()
    {
        clickAndWait(Locator.linkContainingText("Data & Reports"));
        openCustomizePanel(ORIGINAL_WEBPART_TITLE);
        clickButton("Manage Categories", 0);
        _extHelper.waitForExtDialog("Manage Categories");

        waitForElement(Ext4Helper.Locators.window("Manage Categories").append(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(CATEGORIES[1])));
        click(Ext4Helper.Locators.window("Manage Categories").append(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(CATEGORIES[1])));
        _extHelper.waitForExtDialog("Subcategories");
        assertElementPresent(Ext4Helper.Locators.window("Manage Categories").append("//tr").withClass("x4-grid-row-selected").withText(CATEGORIES[1]));

        addSubCategory("Subcategory1-" + CATEGORIES[1]);
        addSubCategory("Subcategory2-" + CATEGORIES[1]);

        click(Ext4Helper.Locators.window("Subcategories").append("//img").withClass("x4-tool-close").notHidden());
        waitForTextToDisappear("Subcategory1-" + CATEGORIES[1]);
        click(Ext4Helper.Locators.window("Manage Categories").append(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(CATEGORIES[2])));
        waitForElement(Ext4Helper.Locators.window("Manage Categories").append("//tr").withClass("x4-grid-row-selected").withText(CATEGORIES[2]));
        assertTextNotPresent("Subcategory1-" + CATEGORIES[1], "Subcategory2-" + CATEGORIES[1]);

        addSubCategory("Subcategory1-" + CATEGORIES[2]);
        addSubCategory("Subcategory2-" + CATEGORIES[2]);

        click(Ext4Helper.Locators.window("Subcategories").append("//img").withClass("x4-tool-close").notHidden());
        waitForTextToDisappear("Subcategory1-" + CATEGORIES[2]);
        click(Ext4Helper.Locators.window("Manage Categories").append(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(CATEGORIES[3])));
        waitForElement(Ext4Helper.Locators.window("Manage Categories").append("//tr").withClass("x4-grid-row-selected").withText(CATEGORIES[3]));
        assertTextNotPresent("Subcategory1-" + CATEGORIES[1], "Subcategory2-" + CATEGORIES[1], "Subcategory1-" + CATEGORIES[2], "Subcategory2-" + CATEGORIES[2]);

        click(Ext4Helper.Locators.ext4Button("Done"));
        _extHelper.waitForExtDialogToDisappear("Manage Categories");
        closeCustomizePanel(ORIGINAL_WEBPART_TITLE);
        enableEditMode();
        openEditPanel("DEM-1: Demographics");
        click(Locator.xpath("//tr[./td/input[@name='category']]/td/div").withClass("x4-form-arrow-trigger"));
        waitForElement(Locator.xpath("//li//span").withText(CATEGORIES[1]));
        assertEquals("Available categories are not as expected", CATEGORY_LIST, getText(Locator.css(".x4-boundlist")));
        click(Locator.xpath("//tr[./td/input[@name='category']]/td/div").withClass("x4-form-arrow-trigger"));
        saveDatasetProperties("DEM-1: Demographics");

        openEditPanel(datasets[0][0]);
        _ext4Helper.selectComboBoxItem("Category", "Subcategory1-" + CATEGORIES[1]);
        saveDatasetProperties(datasets[0][0]);

        openEditPanel(datasets[1][0]);
        _ext4Helper.selectComboBoxItem("Category", "Subcategory2-" + CATEGORIES[1]);
        saveDatasetProperties(datasets[1][0]);

        openEditPanel(datasets[2][0]);
        _ext4Helper.selectComboBoxItem("Category", "Subcategory1-" + CATEGORIES[2]);
        saveDatasetProperties(datasets[2][0]);

        openEditPanel(datasets[3][0]);
        _ext4Helper.selectComboBoxItem("Category", "Subcategory2-" + CATEGORIES[2]);
        saveDatasetProperties(datasets[3][0]);

        assertTextPresentInThisOrder(
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

    private void exportImportTest()
    {
        log("Verify roundtripping of study redesign features");
        exportStudy(false);
        deleteStudy();

        clickButton("Import Study");
        clickButton("Import Study Using Pipeline");
        _fileBrowserHelper.importFile("export/study/study.xml", "Import Study");
        waitForText("Import Study from Pipeline");
        clickButton("Start Import");

        waitForPipelineJobsToComplete(3, "Study import", false);

        clickAndWait(Locator.linkWithText("Data & Reports"));

        log("Verify export-import of refresh date settings");
        //why should a date appear somewhere on the page at this point???
        waitForText(REFRESH_DATE, 1, WAIT_FOR_JAVASCRIPT);
        // check hover box
        mouseOver(Locator.linkWithText(EDITED_DATASET));
        waitForText("Data Cut Date:");
        assertTextPresent(REFRESH_DATE);
        clickAndWait(Locator.linkWithText(EDITED_DATASET));
        assertTextPresent(REFRESH_DATE);
    }

    /**
     * Add a sub-category to the selected dataset category
     */
    private void addSubCategory(String subCategoryName)
    {
        clickButton("New Subcategory", 0);
        WebElement subCategoryField = Locator.xpath("//input[@name='label']").notHidden().waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        setFormElement(subCategoryField, subCategoryName);
        fireEvent(subCategoryField, SeleniumEvent.blur);
        waitForElement(Locator.xpath("//div").withClass("x4-grid-cell-inner").withText(subCategoryName));
    }

    @LogMethod
    public void categoryReorderTest()
    {
        throw new IllegalStateException("Not yet implemented");
    }

    private void openEditPanel(String itemName)
    {
        waitAndClick(Locators.editViewsLink(itemName));
        waitForElement(Ext4Helper.Locators.window(itemName));
        waitForElementToDisappear(Locator.tagWithClass("input", "x4-form-invalid-field"));
    }

    public void saveDatasetProperties(String dataset)
    {
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.css(".x4-grid-row-selected"));
        waitForElement(Locator.css(".x4-grid-row").containing(dataset));
    }

    public static class Locators
    {
        public static Locator.XPathLocator editViewsLink(String dataset)
        {
            return Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").withDescendant(Locator.xpath("td/div/a[normalize-space()="+Locator.xq(dataset)+"]")).append("//span").withClass("edit-views-link");
        }
    }
}
