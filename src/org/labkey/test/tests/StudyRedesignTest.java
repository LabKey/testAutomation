/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.tests.study.DataViewsTester;
import org.labkey.test.tests.study.StudyScheduleTester;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;

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
    private static final String NEW_CATEGORY = "A New Category" + TRICKY_CHARACTERS_NO_QUOTES;
    private static final String NEW_DESCRIPTION = "Description set in data views webpart";
    private static final String PARTICIPANT_GROUP_ONE = "GROUP 1";
    private static final String PARTICIPANT_GROUP_TWO = "GROUP 2";
    private static final String PARTICIPANT_GROUP_THREE = "ThisIsAGroupThatHasALongNameWithoutAnySpacesInIt";
    private static final String[] PTIDS_ONE = {"999320016", "999320518", "999320529", "999320533", "999320541", "999320557",
                                               "999320565", "999320576", "999320582", "999320590"};
    private static final String[] PTIDS_TWO = {"999320004", "999320007", "999320010", "999320016", "999320018", "999320021",
                                               "999320029", "999320033", "999320036","999320038", "999321033", "999321029",
                                               "999320981"};
    private static final String[] PTIDS = {"999320518", "999320529", "999320533", "999320541", "999320557",
                                           "999320565", "999320576", "999320582", "999320590", "999320004", "999320007",
                                           "999320010", "999320016", "999320018", "999320021", "999320029", "999320033",
                                           "999320036","999320038", "999321033", "999321029", "999320981"};
    private static final String REFRESH_DATE = "2012-03-01";

    @Override @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();
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
        clickMenuButton("Create", "R View");
        clickButton("Save", "Please enter a view name:");
        setFormElement(Locator.xpath("//div[./span[.='Please enter a view name:']]/div/input"), REPORT_NAME);
        _extHelper.clickExtButton("Save");

        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_ONE, "Mouse", PTIDS_ONE);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_TWO, "Mouse", PTIDS_TWO);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_THREE, "Mouse", PTIDS[0]);

//        log("Create query for data view webpart.");
//        goToSchemaBrowser();
//        createNewQuery("study");
//        setFormElement("ff_newQueryName", "testquery");
//        clickButton("Create and Edit Source");
//        clickButton("Save & Finish");
    }

    @Override @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        customizeTabsTest();
        dataViewsWebpartTest();
        scheduleWebpartTest();
        participantListWebpartTest();
        exportImportTest();
    }

    @LogMethod
    private void customizeTabsTest()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        // Move tabs
        portalHelper.moveTab("Overview", PortalHelper.Direction.LEFT); // Nothing should happen.
        portalHelper.moveTab("Overview", PortalHelper.Direction.RIGHT);
        Assert.assertTrue("Mice".equals(getText(Locator.xpath("//div[@class='labkey-app-bar']//ul//li[1]//a[1]")))); // Verify Mice is in the first position.
        Assert.assertTrue("Overview".equals(getText(Locator.xpath("//div[@class='labkey-app-bar']//ul//li[2]//a[1]")))); // Verify Overview is in the second.
        portalHelper.moveTab("Manage", PortalHelper.Direction.RIGHT); // Nothing should happen.
        Assert.assertTrue("Manage".equals(getText(Locator.xpath("//div[@class='labkey-app-bar']//ul//li[5]//a[1]")))); // Verify Manage did not swap with +

        // Remove tab
        portalHelper.removeTab("Specimens");

        // Add tab
        portalHelper.addTab("TEST TAB 1");
        clickAndWait(Locator.linkWithText("TEST TAB 1"));
        addWebPart("Wiki");

        // Rename tabs
        portalHelper.renameTab("TEST TAB 1", "Specimens", "You cannot change a tab's name to another tab's original name even if the original name is not visible.");
        portalHelper.renameTab("Overview", "TEST TAB 1", "A tab with the same name already exists in this folder.");
        portalHelper.renameTab("Overview", "test tab 1", "A tab with the same name already exists in this folder.");
        portalHelper.renameTab("TEST TAB 1", "RENAMED TAB 1");
        clickAndWait(Locator.linkWithText("RENAMED TAB 1"));
        Assert.assertEquals("Wiki not present after tab rename", "Wiki", getText(Locator.css(".labkey-wp-title-text")));

        // TODO: Test import/export of renamed tabs if applicable
        // See Issue 16929: Folder tab order & names aren't retained through folder export/import

        portalHelper.removeTab("RENAMED TAB 1");
    }

    private void dataViewsWebpartTest()
    {
        DataViewsTester test = new DataViewsTester(this, getFolderName());

        //TODO: enable once data views tree is enabled
        test.subcategoryTest();
        test.basicTest();
        test.datasetStatusTest();
        test.refreshDateTest();
    }

    private void scheduleWebpartTest()
    {
        StudyScheduleTester tester = new StudyScheduleTester(this, getFolderName(), getStudySampleDataPath());

        tester.basicTest();
        tester.datasetStatusTest();
        tester.linkDatasetTest();
        tester.linkFromDatasetDetailsTest();
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
        clickAndWait(Locator.linkWithText("Manage"));
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("Change Properties"));

        int dsCount = getXpathCount(Locator.xpath("//input[@name='extraData']"));
        Assert.assertEquals("Unexpected number of Datasets.", datasetCount, dsCount);

        // create new categories, then assign them out
        for (String category : CATEGORIES)
        {
            clickButton("Manage Categories", 0);
            _extHelper.waitForExtDialog("Manage Categories");
            clickButton("New Category", 0);
            waitForElement(Locator.xpath("//input[contains(@id, 'textfield') and @name='label']").notHidden());
            setFormElement(Locator.xpath("//input[contains(@id, 'textfield') and @name='label']").notHidden(), category);
            waitForElement(Ext4HelperWD.Locators.window("Manage Categories").append("//div").withText(category));

            clickButton("Done", 0);
            _extHelper.waitForExtDialogToDisappear("Manage Categories");
        }

        // assign them to each dataset
        for (int i = 0; i < dsCount; i++)
        {
            Locator.XPathLocator combo = Locator.xpath("//div[contains(@id, '-viewcategory')]//table").withClass("x4-form-item").index(i);
            _ext4Helper.selectComboBoxItem(combo, CATEGORIES[i / 10]);
        }
        uncheckCheckbox("visible", dsCount - 1); // Set last dataset to not be visible.
        clickButton("Save");
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
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Overview"));
        addWebPart("Mouse List");
        waitForElement(Locator.css(".participant-filter-panel"));
        waitForText("Showing all 138 mice."); // Wait for participant list to appear.

        deselectAllFilterGroups();

        waitForText("No matching Mice");

        //Mouse down on GROUP 1
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 10 mice of 138.");

        //Check if all PTIDs of GROUP 1 are visible.
        for(String ptid : PTIDS_ONE)
        {
            assertTextPresent(ptid);
        }

        //Mouse down GROUP 2
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_TWO, 0);
        waitForText("Found 1 mouse of 138.");

        //Mouse down on GROUP 1 to remove it.
        _ext4Helper.uncheckGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0);
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

        // verify cohort/group panel is resizable and grid cells have the word-wrapping CSS class
        assertElementPresent(Locator.xpath("//div[contains(@class, 'x4-resizable-handle-east')]"), 1);
        assertElementPresent(Locator.xpath("//div[contains(@class, 'x4-resizable-handle-south')]"), 2); //twice, one for south and one for southeast
        assertElementPresent(Locator.xpath("//div[contains(@class, 'x4-resizable-handle-southeast')]"), 1);
        assertElementPresent(Locator.xpath("//div[contains(@class, 'participant-filter-panel')]//tr[contains(@class, 'x4-grid-row')]"), 10); // All + 3 Cohorts + 3 participant groups and 3 not in any group
        assertElementPresent(Locator.xpath("//div[contains(@class, 'participant-filter-panel')]//tr[contains(@class, 'x4-grid-group-hd')]"), 4); // 4 cohort/ptid group category labels

        // compare the height of a non text-wrapped group grid cell to a wrapped one
        _ext4Helper.checkGridRowCheckbox(PARTICIPANT_GROUP_THREE);
        int group2Height = Integer.parseInt(this.getWrapper().getEval("selenium.getExtElementHeight('normalwrap-gridcell', 8)"));
        int group3Height = Integer.parseInt(this.getWrapper().getEval("selenium.getExtElementHeight('normalwrap-gridcell', 11)"));
        Assert.assertTrue("Expected " + PARTICIPANT_GROUP_THREE + " grid cell to wrap text", group3Height > group2Height);
        // drag the east handle to the right so that the group three doesn't wrap anymore
        dragAndDrop(Locator.xpath("//div[contains(@class, 'x4-resizable-handle-east')]"), 250, 0);
        group2Height = Integer.parseInt(this.getWrapper().getEval("selenium.getExtElementHeight('normalwrap-gridcell', 8)"));
        group3Height = Integer.parseInt(this.getWrapper().getEval("selenium.getExtElementHeight('normalwrap-gridcell', 11)"));
        Assert.assertTrue("Expected panel width to allow " + PARTICIPANT_GROUP_THREE + " grid cell on one line", group3Height == group2Height);
    }

    private void deselectAllFilterGroups()
    {
        Locator all = Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//b[contains(@class, 'filter-description') and contains(text(), 'All')]/../../../..//div[contains(@class, 'x4-grid-row-checker')]");
        waitForElement(all);
        mouseDown(all);
    }

    private void exportImportTest()
    {
        log("Verify roundtripping of study redesign features");
        exportStudy(true, false);
        deleteStudy(getStudyLabel());

        clickButton("Import Study");
        clickButton("Import Study Using Pipeline");
        _extHelper.selectFileBrowserItem("export/study/study.xml");
        selectImportDataAction("Import Study");

        waitForPipelineJobsToComplete(3, "Study import", false);

        clickAndWait(Locator.linkWithText("Data & Reports"));

        log("Verify export-import of refresh date settings");
        waitForText(REFRESH_DATE, 1, WAIT_FOR_JAVASCRIPT);
        // check hover box
        mouseOver(Locator.linkWithText(EDITED_DATASET));
        waitForText("Data Cut Date:");
        assertTextPresent(REFRESH_DATE);
        clickAndWait(Locator.linkWithText(EDITED_DATASET));
        assertTextPresent(REFRESH_DATE);
    }
}
