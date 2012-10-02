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
package org.labkey.test.util;

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
    public void createParticipantGroup(String projectName, String studyFolder, String groupName, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, "Participant", ptids);
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
                                                    String categoryName, boolean isCategoryNameNew, Boolean shared, String... ptids)
    {
        if( !_test.isElementPresent(Locator.xpath("//div[contains(@class, 'labkey-nav-page-header') and text() = 'Manage "+participantString+" Groups']")) )
        {
            _test.clickLinkWithText(projectName);
            _test.clickLinkWithText(studyFolder);
            _test.clickTab("Manage");
            _test.clickLinkWithText("Manage " + participantString + " Groups");
            _test.waitForText("groups allow");
        }
        _test.log("Create "+participantString+" Group: " + groupName);
        _test.clickButton("Create", 0);
        _test._extHelper.waitForExtDialog("Define "+participantString+" Group");
        _test.waitForElement(Locator.id("dataregion_demoDataRegion"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement("groupLabel", groupName);
        if( ptids.length > 0 )
        {
            String csp = ptids[0];
            for( int i = 1; i < ptids.length; i++ )
                csp += ","+ptids[i];
            _test.setFormElement("categoryIdentifiers", csp);
        }
        if( categoryName != null )
        {
            if (isCategoryNameNew)
                _test.setFormElement("participantCategory", categoryName);
            else
                _test._extHelper.selectComboBoxItem(participantString + " Category", categoryName);
        }
        if ( shared != null )
        {
            _test.sleep(100);
            if( shared )
            {
                _test.checkCheckbox(Locator.checkboxByName("Shared"));
            }
            else
            {
                _test.uncheckCheckbox(Locator.checkboxByName("Shared"));
            }
        }

        _test._extHelper.clickExtButton("Define " + participantString + " Group", "Save", 0);
        _test.waitForExtMaskToDisappear();
    }

    @LogMethod
    public void editCustomParticipantGroup(String groupName, String participantString,
                                                  String categoryName, boolean isCategoryNameNew, Boolean shared, String... newPtids)
    {
        // Caller must already be on Manage <participantString> Groups page
        // And there should be NO DEMOGRAPHICS DATASETS!

        _test.log("Edit " + participantString + " Group: " + groupName);

        // Select row
        _test.getWrapper().getEval("selenium.selectExtGridItem('label', '"+groupName+"', -1, 'participantCategoriesGrid', null, false)");
        _test.click(Locator.xpath("//*[text()='" + groupName + "']"));

        _test.clickButton("Edit Selected", 0);
        _test._extHelper.waitForExtDialog("Define " + participantString + " Group");
        _test.waitForElement(Locator.css(".doneLoadingTestMarker"));

        if( newPtids != null && newPtids.length > 0 )
        {
            String csp = newPtids[0];
            for( int i = 1; i < newPtids.length; i++ )
                csp += ","+newPtids[i];
            String currentIds = _test.getFormElement("categoryIdentifiers");
            if (currentIds != null && currentIds.length() > 0)
                _test.setFormElement("categoryIdentifiers", currentIds + "," + csp);
            else
                _test.setFormElement("categoryIdentifiers", csp);
        }
        if( categoryName != null )
        {
            _test.sleep(100);
            if (isCategoryNameNew)
                _test.setFormElement("participantCategory", categoryName);
            else
                _test._extHelper.selectComboBoxItem(participantString + " Category", categoryName);
        }
        if ( shared != null )
        {
            _test.sleep(100);
            if( shared )
            {
                _test.checkCheckbox("Shared");
            }
            else
            {
                _test.uncheckCheckbox("Shared");
            }
        }
        _test.sleep(100);
        _test._extHelper.clickExtButton("Define "+participantString+" Group", "Save", 0);
        _test.waitForExtMaskToDisappear();
    }

    public void exportStudy(String folder)
    {
        exportStudy(folder, true, false);
    }

    @LogMethod
    public void exportStudy(String folder, boolean useXmlFormat, boolean zipFile)
    {
        _test.clickLinkWithText(folder);
        _test.clickTab("Manage");
        _test.clickButton("Export Study");

        _test.assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings", "Participant Groups", "Protocol Documents");
        // NOTE: these have moved to the folder archive export: "Queries", "Custom Views", "Reports", "Lists"

        _test.checkRadioButton("format", useXmlFormat ? "new" : "old");
        _test.checkRadioButton("location", zipFile ? "1" : "0");  // zip file vs. individual files
        _test.clickButton("Export");
    }

}
