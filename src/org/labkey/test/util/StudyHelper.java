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
public class StudyHelper
{

    public static void createParticipantGroup(BaseSeleniumWebTest test, String projectName, String studyFolder, String groupName, String... ptids)
    {
        createCustomParticipantGroup(test, projectName, studyFolder, groupName, "Participant", ptids);
    }

    public static void createCustomParticipantGroup(BaseSeleniumWebTest test, String projectName, String studyFolder, String groupName, String participantString, String... ptids)
    {
        createCustomParticipantGroup(test, projectName, studyFolder, groupName, participantString, null, ptids);
    }

    public static void createCustomParticipantGroup(BaseSeleniumWebTest test, String projectName, String studyFolder, String groupName, String participantString,
                                                    Boolean shared, String... ptids)
    {
        createCustomParticipantGroup(test, projectName, studyFolder, groupName, participantString, null, false, shared, ptids);
    }
    public static void createCustomParticipantGroup(BaseSeleniumWebTest test, String projectName, String studyFolder, String groupName, String participantString,
                                                    String categoryName, boolean isCategoryNameNew, Boolean shared, String... ptids)
    {
        if( !test.isElementPresent(Locator.xpath("//div[contains(@class, 'labkey-nav-page-header') and text() = 'Manage "+participantString+" Groups']")) )
        {
            test.clickLinkWithText(projectName);
            test.clickLinkWithText(studyFolder);
            test.clickTab("Manage");
            test.clickLinkWithText("Manage " + participantString + " Groups");
            test.waitForText("groups allow");
        }
        test.log("Create "+participantString+" Group: " + groupName);
        test.clickNavButton("Create", 0);
        ExtHelper.waitForExtDialog(test, "Define "+participantString+" Group");
        test.waitForElement(Locator.id("dataregion_demoDataRegion"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        test.setFormElement("groupLabel", groupName);
        if( ptids.length > 0 )
        {
            String csp = ptids[0];
            for( int i = 1; i < ptids.length; i++ )
                csp += ","+ptids[i];
            test.setFormElement("categoryIdentifiers", csp);
        }
        if( categoryName != null )
        {
            if (isCategoryNameNew)
                test.setFormElement("participantCategory", categoryName);
            else
                ExtHelper.selectComboBoxItem(test, participantString + " Category", categoryName);
        }
        if ( shared != null )
        {
            if( shared.booleanValue() && !test.isChecked(Locator.checkboxByName("Shared")))
            {
                test.checkCheckbox("Shared");
            }
            else if (!shared.booleanValue() && test.isChecked(Locator.checkboxByName("Shared")))
            {
                test.uncheckCheckbox("Shared");
            }
        }

        ExtHelper.clickExtButton(test, "Define "+participantString+" Group", "Save", 0);
        test.waitForExtMaskToDisappear();
    }

    public static void editCustomParticipantGroup(BaseSeleniumWebTest test, String groupName, String participantString,
                                                  String categoryName, boolean isCategoryNameNew, Boolean shared, String... newPtids)
    {
        // Caller must already be on Manage <participantString> Groups page
        // And there should be NO DEMOGRAPHICS DATASETS!

        test.log("Edit "+participantString+" Group: " + groupName);

        // Select row
        test.getWrapper().getEval("selenium.selectExtGridItem('label', '"+groupName+"', -1, 'participantCategoriesGrid', null, false)");
        test.click(Locator.xpath("//*[text()='"+groupName+"']"));

        test.clickNavButton("Edit Selected", 0);
        ExtHelper.waitForExtDialog(test, "Define " + participantString + " Group");
//        test.waitForElement(Locator.id("dataregion_demoDataRegion"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        if( newPtids != null && newPtids.length > 0 )
        {
            String csp = newPtids[0];
            for( int i = 1; i < newPtids.length; i++ )
                csp += ","+newPtids[i];
            String currentIds = test.getFormElement("categoryIdentifiers");
            if (currentIds != null && currentIds.length() > 0)
                test.setFormElement("categoryIdentifiers", currentIds + "," + csp);
            else
                test.setFormElement("categoryIdentifiers", csp);
        }
        if( categoryName != null )
        {
            if (isCategoryNameNew)
                test.setFormElement("participantCategory", categoryName);
            else
                ExtHelper.selectComboBoxItem(test, participantString + " Category", categoryName);
        }
        if ( shared != null )
        {
            if( shared.booleanValue() && !test.isChecked(Locator.checkboxByName("Shared")))
            {
                test.checkCheckbox("Shared");
            }
            else if (!shared.booleanValue() && test.isChecked(Locator.checkboxByName("Shared")))
            {
                test.uncheckCheckbox("Shared");
            }
        }
        test.sleep(100);
        ExtHelper.clickExtButton(test, "Define "+participantString+" Group", "Save", 0);
        test.waitForExtMaskToDisappear();
    }

    public static void exportStudy(BaseSeleniumWebTest test, String folder)
    {
        exportStudy(test, folder, true, false);
    }

    public static void exportStudy(BaseSeleniumWebTest test, String folder, boolean useXmlFormat, boolean zipFile)
    {
        test.clickLinkWithText(folder);
        test.clickTab("Manage");
        test.clickNavButton("Export Study");

        test.assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings", "Participant Groups", "Protocol Documents");
        // NOTE: these have moved to the folder archive export: "Queries", "Custom Views", "Reports", "Lists"

        test.checkRadioButton("format", useXmlFormat ? "new" : "old");
        test.checkRadioButton("location", zipFile ? "1" : "0");  // zip file vs. individual files
        test.clickNavButton("Export");
    }

}
