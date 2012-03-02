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

import org.labkey.test.Locator;
import org.labkey.test.tests.study.StudyScheduleTester;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.StudyHelper;

/**
 * User: elvan
 * Date: 8/16/11
 * Time: 3:22 PM
 */
public class StudyRedesignTest extends StudyBaseTest
{

    private static final String DATA_BROWSE_TABLE_NAME = "";
    private static final String[] BITS = {"ABCD", "EFGH", "IJKL", "MNOP", "QRST", "UVWX"};
    private static final String[] CATEGORIES = {BITS[0]+BITS[1]+TRICKY_CHARACTERS_NO_QUOTES, BITS[1]+BITS[2]+TRICKY_CHARACTERS_NO_QUOTES,
            BITS[2]+BITS[3]+TRICKY_CHARACTERS_NO_QUOTES, BITS[3]+BITS[4]+TRICKY_CHARACTERS_NO_QUOTES, BITS[4]+BITS[5]+TRICKY_CHARACTERS_NO_QUOTES};
    private static final String[] someDataSets = {"Data Views","DEM-1: Demographics", "URF-1: Follow-up Urinalysis (Page 1)", CATEGORIES[3], "AE-1:(VTN) AE Log"};
    private static final String REPORT_NAME = "TestReport";
    private static final String WEBPART_TITLE = "TestDataViews";
    private static final String EDITED_DATASET = "CPS-1: Screening Chemistry Panel";
    private static final String NEW_CATEGORY = "A New Category"; // + TRICKY_CHARACTERS_NO_QUOTES; // TODO: Add tricky characters to regress 14230
    private static final String NEW_DESCRIPTION = "Description set in data views webpart";
    private static final String PARTICIPANT_GROUP_ONE = "GROUP 1";
    private static final String PARTICIPANT_GROUP_TWO = "GROUP 2";
    private static final String[] PTIDS_ONE = {"999320016", "999320518", "999320529", "999320533", "999320541", "999320557",
                                               "999320565", "999320576", "999320582", "999320590"};
    private static final String[] PTIDS_TWO = {"999320004", "999320007", "999320010", "999320016", "999320018", "999320021",
                                               "999320029", "999320033", "999320036","999320038", "999321033", "999321029",
                                               "999320981"};
    private static final String[] PTIDS = {"999320518", "999320529", "999320533", "999320541", "999320557",
                                           "999320565", "999320576", "999320582", "999320590", "999320004", "999320007",
                                           "999320010", "999320016", "999320018", "999320021", "999320029", "999320033",
                                           "999320036","999320038", "999321033", "999321029", "999320981"};
    private static final String REFRESH_DATE = "03/01/2012";

    @Override
    protected void doCreateSteps()
    {
        RReportHelper.ensureRConfig(this);
        importStudy();
        startSpecimenImport(2);

        // wait for study and specimens to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);
        waitForSpecimenImport();
        setStudyRedesign();
        setupDatasetCategories();
        log("Create report for data view webpart test.");
        goToModule("Study");
        clickTab("Manage");
        clickLinkWithText("Manage Views");
        clickMenuButton("Create", "R View");
        clickNavButton("Save", 0);
        waitForText("Please enter a view name:");
        setFormElement(Locator.xpath("//div[./span[.='Please enter a view name:']]/div/input"), REPORT_NAME);
        clickNavButton("Save");

        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), PARTICIPANT_GROUP_ONE, "Mouse", PTIDS_ONE);
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), PARTICIPANT_GROUP_TWO, "Mouse", PTIDS_TWO);

//        log("Create query for data view webpart.");
//        goToSchemaBrowser();
//        createNewQuery("study");
//        setFormElement("ff_newQueryName", "testquery");
//        clickNavButton("Create and Edit Source");
//        clickNavButton("Save & Finish");
    }

    @Override
    protected void doVerifySteps()
    {
        dataViewsWebpartTest();
        scheduleWebpartTest();
        participantListWebpartTest();
        exportImportTest();
    }

    private void dataViewsWebpartTest()
    {
        log("Data Views Test");
        clickLinkContainingText("Data & Reports");
        waitForText(someDataSets[3]);
        assertTextPresent("Data Views", "Name", "Type", "Access");

        assertDataDisplayedAlphabetically();

        //TODO:  waiting on hypermove fix
//        datasetBrowseClickDataTest();

        log("Verify dataset category sorting.");
        setDataBrowseSearch(BITS[4]);
        waitForTextToDisappear(BITS[2]);
        assertTextNotPresent(CATEGORIES[0]);
        assertTextNotPresent(CATEGORIES[1]);
        assertTextNotPresent(CATEGORIES[2]);
        assertEquals("Incorrect number of dataset categories visible.", 2, getXpathCount(Locator.xpath("//div[contains(@class, 'x4-grid-group-title')]"))); // Two categories contain filter text.
        // 10 datasets(CATEGORIES[3]) + 7 datasets(CATEGORIES[4]) - 1 hidden dataset == 16?
        assertEquals("Incorrect number of datasets after filter", 16, getXpathCount(Locator.xpath("//tr[contains(@class, 'x4-grid-row')]")));
        collapseCategory(CATEGORIES[3]);
        assertEquals("Incorrect number of datasets after collapsing category.", 6, getXpathCount(Locator.xpath("//tr[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and contains(@class, 'x4-grid-row')]")));
        clickWebpartMenuItem("Data Views", false, "Customize");
        waitForElement(Locator.button("Manage Categories"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.uncheckCheckbox(this, "datasets");
        setFormElement(Locator.name("webpart.title"), WEBPART_TITLE);
        clickButton("Save", 0);
        setDataBrowseSearch("");
        waitForElement(Locator.linkWithText(REPORT_NAME), WAIT_FOR_JAVASCRIPT);
        assertTextPresent(WEBPART_TITLE);
        assertEquals("Incorrect number of datasets after filter", 1, getXpathCount(Locator.xpath("//tr[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and contains(@class, 'x4-grid-row')]")));

        log("Verify cancel button");
        clickWebpartMenuItem("Data Views", false, "Customize");
        waitForElement(Locator.button("Manage Categories"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.checkCheckbox(this, "datasets");
        ExtHelper.uncheckCheckbox(this, "reports");
        setFormElement(Locator.name("webpart.title"), "nothing");
        clickButton("Cancel", 0);
        sleep(500);               //TODO: \
        refresh();                //TODO:  |remove: 13265: Data views webpart admin cancel button doesn't reset form
        waitForText(REPORT_NAME); //TODO: /
        assertTextNotPresent("nothing");
        assertTextPresent(WEBPART_TITLE);
        assertEquals("Incorrect number of datasets after filter", 1, getXpathCount(Locator.xpath("//tr[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and contains(@class, 'x4-grid-row')]")));

        log("Verify category management");
        clickWebpartMenuItem(WEBPART_TITLE, false, "Customize");
        waitForElement(Locator.button("Manage Categories"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.checkCheckbox(this, "datasets");
        clickNavButton("Manage Categories", 0);
        ExtHelper.waitForExtDialog(this, "Manage Categories");
        waitAndClick(Locator.xpath("//img[@data-qtip='Delete']"));
        ExtHelper.waitForExtDialog(this, "Delete Category");
        clickNavButton("OK", 0);
        clickNavButton("New Category", 0);
        setFormElement(Locator.xpath("(//input[contains(@class, 'form-field') and @type='text'])[5]"), "testcategory"); // TODO: need a better xpath
        clickNavButton("Done", 0);
        clickNavButton("Save", 0);
        waitForText(CATEGORIES[1], WAIT_FOR_JAVASCRIPT);
        refresh(); // Deleted category is still present, but hidden.  Refresh to clear page.
        waitForText(CATEGORIES[1], WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent(CATEGORIES[0]);
        assertTextPresentInThisOrder(CATEGORIES[2], CATEGORIES[3], "Uncategorized", "APX-1", REPORT_NAME);

        log("Verify modify dataset");
        click(Locator.xpath("//span[contains(@class, 'edit-views-link')]"));
        ExtHelper.waitForExtDialog(this, EDITED_DATASET);
        setFormElement(Locator.xpath("//label[text() = 'Category']/..//input"), NEW_CATEGORY);
        setFormElement(Locator.name("description"), NEW_DESCRIPTION);
        ExtHelper.clickExtButton(this, EDITED_DATASET, "Save", 0);
        waitForText(NEW_CATEGORY);
        clickLinkWithText(EDITED_DATASET);
        assertTextPresent(NEW_DESCRIPTION);

        log("Verify refresh date");
        String refreshDate = "03/01/2012";
        clickLinkContainingText("Data & Reports");
        waitForText(someDataSets[3]);
        // Refresh date not present when not set.
        mouseOver(Locator.linkWithText(EDITED_DATASET));
        waitForText("Type:");
        assertTextNotPresent("Refresh Date:");
        clickWebpartMenuItem(WEBPART_TITLE, false, "Customize");
        waitForElement(Locator.button("Manage Categories"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.checkCheckbox(this, "Modified");
        ExtHelper.checkCheckbox(this, "Refresh Date");
        Locator manageButton = getButtonLocator("Manage Categories");
        clickButton("Save", 0);
        waitForElementToDisappear(manageButton, WAIT_FOR_JAVASCRIPT);
        waitForText("Refresh Date");
        waitForText("Modified");
        click(Locator.xpath("//span[contains(@class, 'edit-views-link')]"));
        ExtHelper.waitForExtDialog(this, EDITED_DATASET);
        setFormElement("refreshDate", refreshDate);
        ExtHelper.clickExtButton(this, EDITED_DATASET, "Save", 0);
        waitForText(refreshDate, 1, WAIT_FOR_JAVASCRIPT);
        // check hover box
        mouseOver(Locator.linkWithText(EDITED_DATASET));
        waitForText("Refresh Date:");
        assertTextPresent("Thu Mar 01 2012");
        clickLinkWithText(EDITED_DATASET);
        assertTextPresent("Refresh Date: Thu Mar 01 00:00:00 PST 2012");
    }

    private void scheduleWebpartTest()
    {
        StudyScheduleTester tester = new StudyScheduleTester(this, getFolderName(), getSampleDataPath());

        tester.basicTest();
        tester.linkDatasetTest();
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
        assertElementPresent(Locator.xpath("//div[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and @class='x4-grid-group-title' and contains(text(), '" + category + "')]"));
        click(Locator.xpath("//div[@class='x4-grid-group-title' and contains(text(), '" + category + "')]"));
        waitForElement(Locator.xpath("//tr[contains(@class, 'collapsed')]//div[@class='x4-grid-group-title' and contains(text(), '" + category + "')]"), WAIT_FOR_JAVASCRIPT);
    }

    private void expandCategory(String category)
    {
        log("Expand category: " + category);
        assertElementPresent(Locator.xpath("//div[ancestor-or-self::tr[contains(@class, 'collapsed')] and @class='x4-grid-group-title' and text()='" + category + "']"));
        click(Locator.xpath("//div[@class='x4-grid-group-title' and text()='" + category + "']"));
        waitForElement(Locator.xpath("//div[not(ancestor-or-self::tr[contains(@class, 'collapsed')]) and @class='x4-grid-group-title' and text()='" + category + "']"), WAIT_FOR_JAVASCRIPT);
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
        return ret;
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

    private void participantListWebpartTest()
    {
        log("Participant List Webpart Test");
        clickLinkWithText("Overview");
        addWebPart("Mouse List");
        waitForText("Filter"); // Wait for participant list to appear.

        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), 'All')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("No matching Mice");

        //Mouse down on GROUP 1
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), 'GROUP 1')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Found 10 mice of 138.");

        //Check if all PTIDs of GROUP 1 are visible.
        for(String ptid : PTIDS_ONE)
        {
            assertTextPresent(ptid);
        }

        //Mouse down GROUP 2
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), 'GROUP 2')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Found 22 mice of 138.");
        //Check that all PTIDs from GROUP 1 and GROUP 2 are present at the same time.
        for(String ptid : PTIDS)
        {
            assertTextPresent(ptid);
        }

        //Mouse down on GROUP 1 to remove it.
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), 'GROUP 1')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Found 13 mice of 138.");
        
        //Check if all PTIDs of GROUP 2 are visible
        for(String ptid : PTIDS_TWO)
        {
            assertTextPresent(ptid);
        }

        //Filter a mouse in GROUP 2
        setFormElement("//div[contains(text(), 'Filter')]//input", "999320038");
        fireEvent(Locator.xpath("//div[contains(text(), 'Filter')]//input"), SeleniumEvent.blur);
        waitForText("Found 1 mouse of 138.");

        //Filter a mouse not in GROUP 2
        setFormElement("//div[contains(text(), 'Filter')]//input", "999320518");
        fireEvent(Locator.xpath("//div[contains(text(), 'Filter')]//input"), SeleniumEvent.blur);
        waitForText("No mouse IDs contain \"999320518\".");

        //Remove filter
        setFormElement("//div[contains(text(), 'Filter')]//input", "");
        fireEvent(Locator.xpath("//div[contains(text(), 'Filter')]//input"), SeleniumEvent.blur);
        waitForText("Found 13 mice of 138.");
    }

    private void exportImportTest()
    {
        log("Verify roundtripping of study redesign features");
        exportStudy(true, false);
        deleteStudy(getStudyLabel());

        clickNavButton("Import Study");
        clickNavButton("Import Study Using Pipeline");
        ExtHelper.selectFileBrowserItem(this, "export/study.xml");
        selectImportDataAction("Import Study");

        waitForPipelineJobsToComplete(3, "Study import", false);

        clickLinkWithText("Data & Reports");

        log("Verify export-import of refresh date settings");
        waitForText(REFRESH_DATE, 1, WAIT_FOR_JAVASCRIPT);
        // check hover box
        mouseOver(Locator.linkWithText(EDITED_DATASET));
        waitForText("Refresh Date:");
        assertTextPresent("Thu Mar 01 2012");
        clickLinkWithText(EDITED_DATASET);
        assertTextPresent("Refresh Date: Thu Mar 01 00:00:00 PST 2012");
    }
}
