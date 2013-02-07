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
package org.labkey.test.util;

import junit.framework.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Aug 16, 2011
 * Time: 3:45:02 PM
 */
public class StudyHelper extends AbstractHelper
{
    public StudyHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, participantString, null, ptids);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString,
                                                    Boolean shared, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, participantString, null, false, shared, ptids);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString,
                                                    Boolean shared, Boolean demographicsPresent, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, participantString, null, false, shared, demographicsPresent, ptids);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString,
                                                    String categoryName, boolean isCategoryNameNew, Boolean shared, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, participantString, categoryName, isCategoryNameNew, shared, true, ptids);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString,
                                                    String categoryName, boolean isCategoryNameNew, Boolean shared, Boolean demographicsPresent, String... ptids)
    {
        if (_test.isElementPresent(ExtHelperWD.Locators.extDialog("Define "+participantString+" Group")))
            throw new IllegalStateException("Already in the middle of Creating a participant group");
        if( !_test.isElementPresent(Locator.xpath("id('labkey-nav-trail-current-page')[text() = 'Manage "+participantString+" Groups']")) )
        {
            _test.clickAndWait(Locator.linkWithText(projectName));
            _test.clickAndWait(Locator.linkWithText(studyFolder));
            _test.clickTab("Manage");
            _test.clickAndWait(Locator.linkWithText("Manage " + participantString + " Groups"));
            _test.waitForText("groups allow");
        }
        _test.log("Create "+participantString+" Group: " + groupName);
        _test.clickButton("Create", 0);
        _test._extHelper.waitForExtDialog("Define "+participantString+" Group");
        _test.waitForElement(Locator.css(".doneLoadingTestMarker"));
        if (demographicsPresent)
            _test.waitForElement(Locator.id("dataregion_demoDataRegion"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.xpath("//input[@name='groupLabel']"), groupName);
        if( ptids.length > 0 )
        {
            String csp = ptids[0];
            for( int i = 1; i < ptids.length; i++ )
                csp = csp.concat("," + ptids[i]);
            _test.setFormElement(Locator.xpath("//textarea[@name='participantIdentifiers']"), csp);
        }
        if( categoryName != null )
        {
            if (isCategoryNameNew)
                _test.setFormElement(Locator.name("participantCategory"), categoryName);
            else
                _test._ext4Helper.selectComboBoxItem(participantString + " Category:", categoryName);
            _test.pressTab(Locator.name("participantCategory"));
            _test.waitForElementToDisappear(Locator.css(".x-form-focus"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        }
        if ( shared != null )
        {
            _test.waitForElement(Locator.css(".share-group-rendered"));
            if( shared )
            {
                _test._ext4Helper.checkCheckbox("Share Category?");
            }
            else
            {
                _test._ext4Helper.uncheckCheckbox("Share Category?");
            }
        }

        _test._extHelper.clickExtButton("Define "+participantString+" Group", "Save", 0);
        _test._ext4Helper.waitForMaskToDisappear(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    public void editCustomParticipantGroup(String groupName, String participantString, String categoryName,
                                           Boolean isCategoryNameNew, Boolean shared, String... newPtids)
    {
        editCustomParticipantGroup(groupName, participantString, categoryName, isCategoryNameNew, shared, false, false, newPtids);
    }

    @LogMethod
    public void editCustomParticipantGroup(String groupName, String participantString,
                                                  String categoryName, Boolean isCategoryNameNew, Boolean shared, Boolean demographicsPresent,
                                                  Boolean replaceExistingPtids, String... newPtids)
    {
        // Caller must already be on Manage <participantString> Groups page
        // And there should be NO DEMOGRAPHICS DATASETS!

        _test.log("Edit " + participantString + " Group: " + groupName);

        // Select row
        selectParticipantCategoriesGridRow(groupName);

        _test.clickButton("Edit Selected", 0);
        _test._extHelper.waitForExtDialog("Define " + participantString + " Group");
        _test.waitForElement(Locator.css(".doneLoadingTestMarker"));
        if (demographicsPresent)
            _test.waitForElement(Locator.id("dataregion_demoDataRegion"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        if( newPtids != null && newPtids.length > 0 )
        {
            StringBuilder csp = new StringBuilder(newPtids[0]);
            for( int i = 1; i < newPtids.length; i++ )
                csp.append(",").append(newPtids[i]);

            if (replaceExistingPtids)
            {
                _test.setFormElement(Locator.xpath("//textarea[@name='participantIdentifiers']"), csp.toString());
            }
            else
            {
                String currentIds = _test.getFormElement(Locator.xpath("//textarea[@name='participantIdentifiers']"));
                if (currentIds != null && currentIds.length() > 0)
                    _test.setFormElement(Locator.xpath("//textarea[@name='participantIdentifiers']"), currentIds + "," + csp.toString());
            }
        }
        if( categoryName != null )
        {
            if (isCategoryNameNew)
                _test.setFormElement(Locator.xpath("//input[@name='participantCategory']"), categoryName);
            else
                _test._ext4Helper.selectComboBoxItem(participantString + " Category:", categoryName);
            _test.pressTab(Locator.xpath("//input[@name='participantCategory']"));
            _test.waitForElementToDisappear(Locator.css(".x-form-focus"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            Assert.assertEquals("Mouse category not set", categoryName, _test.getFormElement(Ext4HelperWD.Locators.formItemWithLabel(participantString + " Category:").append("//input")));
        }
        if ( shared != null )
        {
            _test.waitForElement(Locator.css(".share-group-rendered"));
            if( shared )
            {
                _test._ext4Helper.checkCheckbox("Share Category?");
            }
            else
            {
                _test._ext4Helper.uncheckCheckbox("Share Category?");
            }
        }
        _test._extHelper.clickExtButton("Define "+participantString+" Group", "Save", 0);
        _test._ext4Helper.waitForMaskToDisappear(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    public void deleteCustomParticipantGroup(String groupName, String participantString)
    {
        // Caller must already be on Manage <participantString> Groups page
        _test.log("Delete " + participantString + " Group: " + groupName);
        selectParticipantCategoriesGridRow(groupName);
        _test.clickButton("Delete Selected", 0);
        _test._extHelper.waitForExtDialog("Delete Group");
        _test.sleep(100);
        _test._extHelper.clickExtButton("Delete Group", "Yes", 0);
    }

    public void selectParticipantCategoriesGridRow(final String groupName)
    {
        _test.waitFor(new BaseSeleniumWebTest.Checker(){
            @Override
            public boolean check()
            {
                return _test.isElementPresent(Locator.xpath("//div[contains(@class,'x4-grid-cell-inner') and contains(text(),'" + groupName + "')]"));
            }
        }, "could not group: " + groupName, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.getWrapper().getEval("selenium.selectExt4GridItem('label', '"+groupName+"', -1, 'participantCategoriesGrid', null, false)");
        _test.click(Locator.xpath("//*[text()='" + groupName + "']")); // Ext.select doesn't trigger click events
    }


    public void exportStudy(String folder)
    {
        exportStudy(folder, true, false);
    }


    @LogMethod
    public void exportStudy(String folder, boolean useXmlFormat, boolean zipFile)
    {
        _test.clickAndWait(Locator.linkWithText(folder));
        _test.clickTab("Manage");
        _test.clickButton("Export Study");

        _test.assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings", "Participant Groups", "Protocol Documents");
        // NOTE: these have moved to the folder archive export: "Queries", "Custom Views", "Reports", "Lists"

        _test.checkRadioButton("format", useXmlFormat ? "new" : "old");
        _test.checkRadioButton("location", zipFile ? "1" : "0");  // zip file vs. individual files
        _test.clickButton("Export");
    }

}
