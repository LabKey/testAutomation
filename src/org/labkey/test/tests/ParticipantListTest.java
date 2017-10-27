/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.categories.BVT;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({BVT.class})
public class ParticipantListTest extends StudyBaseTest
{
    protected static final String[] BITS = {"ABCD", "EFGH", "IJKL", "MNOP", "QRST", "UVWX"};
    protected static final String[] CATEGORIES = {BITS[0]+BITS[1]+TRICKY_CHARACTERS_NO_QUOTES, BITS[1]+BITS[2]+TRICKY_CHARACTERS_NO_QUOTES,
            BITS[2]+BITS[3]+TRICKY_CHARACTERS_NO_QUOTES, BITS[3]+BITS[4]+TRICKY_CHARACTERS_NO_QUOTES, BITS[4]+BITS[5]+TRICKY_CHARACTERS_NO_QUOTES};
    protected static final String[] someDatasets = {"Data Views","DEM-1: Demographics", "URF-1: Follow-up Urinalysis (Page 1)", CATEGORIES[2], "AE-1:(VTN) AE Log", CATEGORIES[3]};
    protected static final String EDITED_DATASET = "CPS-1: Screening Chemistry Panel";
    private static final String PARTICIPANT_GROUP_ONE = "GROUP 1";
    private static final String PARTICIPANT_GROUP_TWO = "GROUP 2";
    private static final String PARTICIPANT_GROUP_THREE = "ThisIsAGroupThatHasALong'NameWithoutAnySpacesInIt";
    private static final String[] PTIDS_ONE = {"999320016", "999320518", "999320529", "999320533", "999320541", "999320557",
                                               "999320565", "999320576", "999320582", "999320590"};
    private static final String[] PTIDS_TWO = {"999320004", "999320007", "999320010", "999320016", "999320018", "999320021",
                                               "999320029", "999320033", "999320036","999320038", "999321033", "999321029",
                                               "999320981"};
    private static final String[] PTIDS = {"999320518", "999320529", "999320533", "999320541", "999320557",
                                           "999320565", "999320576", "999320582", "999320590", "999320004", "999320007",
                                           "999320010", "999320016", "999320018", "999320021", "999320029", "999320033",
                                           "999320036","999320038", "999321033", "999321029", "999320981"};
    protected static final String REFRESH_DATE = "2012-03-01";

    @Override @LogMethod
    protected void doCreateSteps()
    {
        RReportHelper _reportHelperWD = new RReportHelper(this);
        _reportHelperWD.ensureRConfig();
        importStudy();
        startSpecimenImport(2);

        // wait for study and specimens to finish loading
        waitForSpecimenImport();

        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_ONE, "Mouse", PTIDS_ONE);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_TWO, "Mouse", PTIDS_TWO);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_THREE, "Mouse", PTIDS[0]);
    }

    @Override @LogMethod
    protected void doVerifySteps() throws Exception
    {
        doParticipantListWebPartTest();
    }

    protected void setupDatasetCategories()
    {
        clickAndWait(Locator.linkWithText("Manage"));
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("Change Properties"));

        int dsCount = getElementCount(Locator.xpath("//tr[@data-datasetid]"));
        assertEquals("Unexpected number of Datasets.", 48, dsCount);

        // create new categories, then assign them out
        for (String category : CATEGORIES)
        {
            clickButton("Manage Categories", 0);
            _extHelper.waitForExtDialog("Manage Categories");
            clickButton("New Category", 0);
            WebElement formField = Locator.xpath("//input[contains(@id, 'textfield') and @name='label']").notHidden().waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(formField, category);
            fireEvent(formField, SeleniumEvent.blur);
            waitForElement(Ext4Helper.Locators.window("Manage Categories").append("//div").withText(category));
            clickButton("Done", 0);
            _extHelper.waitForExtDialogToDisappear("Manage Categories");
        }

        // assign them to each dataset
        for (int i = 0; i < dsCount; i++)
        {
            Locator.XPathLocator combo = Locator.xpath("//div[contains(@id, '-viewcategory')]//table").withClass("x4-form-item").index(i);
            scrollIntoView(combo, true);
            _ext4Helper.selectComboBoxItem(combo, CATEGORIES[i / 10]);
        }

        // Set last dataset to not be visible. (ignore the hidden inputs starting with '@')
        uncheckCheckbox(Locator.xpath("//input[not(starts-with(@name, '@')) and contains(@name, 'visible')]").index(dsCount-1));
        clickButton("Save");
    }

    private void doParticipantListWebPartTest()
    {
        log("Participant List Webpart Test");
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText("Overview"));
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Mouse List");
        waitForElement(Locator.css(".lk-filter-panel-label"));
        waitForText("Found 25 enrolled mice of 138."); // Wait for participant list to appear with Not in any cohort deselected by default

        // Deselect All Filter Groups
        _ext4Helper.deselectAllParticipantFilter();
        waitForText("No matching enrolled Mice");

        //Mouse down on GROUP 1
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 10 enrolled mice of 138.");

        //Check if all PTIDs of GROUP 1 are visible.
        assertTextPresent(PTIDS_ONE);

        // intersection of GROUP 1 and GROUP 2
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_TWO, 0);
        waitForText("Found 1 enrolled mouse of 138.");

        //Mouse down on GROUP 1 to remove it.
        _ext4Helper.uncheckGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 13 mice of 138.");

        //Check if all PTIDs of GROUP 2 are visible
        assertTextPresent(PTIDS_TWO);

        //Filter a mouse in GROUP 2
        setFormElement(Locator.xpath("//div[contains(text(), 'Filter')]//input"), "999320038");
        fireEvent(Locator.xpath("//div[contains(text(), 'Filter')]//input"), SeleniumEvent.blur);
        waitForText("Found 1 mouse of 138.");

        //Filter a mouse not in GROUP 2
        setFormElement(Locator.xpath("//div[contains(text(), 'Filter')]//input"), "999320518");
        fireEvent(Locator.xpath("//div[contains(text(), 'Filter')]//input"), SeleniumEvent.blur);
        waitForText("No mouse IDs contain \"999320518\".");

        //Remove filter
        setFormElement(Locator.xpath("//div[contains(text(), 'Filter')]//input"), "");
        fireEvent(Locator.xpath("//div[contains(text(), 'Filter')]//input"), SeleniumEvent.blur);
        waitForText("Found 13 mice of 138.");

        // verify cohort/group panel is resizable and grid cells have the word-wrapping CSS class
        assertElementPresent(Locator.xpath("//div[contains(@class, 'x4-resizable-handle-east')]"), 1);
        assertElementPresent(Locator.xpath("//div[contains(@class, 'x4-resizable-handle-south')]"), 2); //twice, one for south and one for southeast
        assertElementPresent(Locator.xpath("//div[contains(@class, 'x4-resizable-handle-southeast')]"), 1);
        assertElementPresent(Locator.xpath("//div[contains(@class, 'lk-filter-panel-label') and contains(@class, 'group-label')]"), 9); // 3 Cohorts + 3 participant groups and 3 not in any group
        assertElementPresent(Locator.xpath("//div[contains(@class, 'lk-filter-panel-label') and contains(@class, 'category-label')]"), 4); // 4 cohort/ptid group category labels

        // compare the height of a non text-wrapped group grid cell to a wrapped one
        _ext4Helper.checkGridRowCheckbox(PARTICIPANT_GROUP_THREE);
        int group2Height = _extHelper.getExtElementHeight("normalwrap-gridcell", 8);
        int group3Height = _extHelper.getExtElementHeight("normalwrap-gridcell", 11);
        assertTrue("Expected " + PARTICIPANT_GROUP_THREE + " grid cell to wrap text (group3height=" + group3Height + ",group2Height=" + group2Height, group3Height > group2Height);
        // drag the east handle to the right so that the group three doesn't wrap anymore
        dragAndDrop(Locator.xpath("//div[contains(@class, 'x4-resizable-handle-east')]"), 250, 0);
        group2Height = _extHelper.getExtElementHeight("normalwrap-gridcell", 8);
        group3Height = _extHelper.getExtElementHeight("normalwrap-gridcell", 11);
        assertTrue("Expected panel width to allow " + PARTICIPANT_GROUP_THREE + " grid cell on one line", group3Height == group2Height);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
