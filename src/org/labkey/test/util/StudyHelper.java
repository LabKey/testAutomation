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
        if( !test.isElementPresent(Locator.xpath("//div[contains(@class, 'labkey-nav-page-header') and text() = 'Manage "+participantString+" Groups']")) )
        {
            test.clickLinkWithText(projectName);
            test.clickLinkWithText(studyFolder);
            test.clickLinkWithText("Manage Study");
            test.clickLinkWithText("Manage "+participantString+" Groups");
        }
        test.log("Create "+participantString+" Group: " + groupName);
        test.clickNavButton("Create", 0);
        ExtHelper.waitForExtDialog(test, "Define "+participantString+" Group");
        test.waitForElement(Locator.id("dataregion_demoDataRegion"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        test.setFormElement("categoryLabel", groupName);
        if( ptids.length > 0 )
        {
            String csp = ptids[0];
            for( int i = 1; i < ptids.length; i++ )
                csp += ","+ptids[i];
            test.setFormElement("categoryIdentifiers", csp);
        }
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
        test.clickLinkWithText("Manage Study");
        test.clickNavButton("Export Study");

        test.assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings", "Participant Groups", "Protocol Documents", "Queries", "Custom Views", "Reports", "Lists");

        test.checkRadioButton("format", useXmlFormat ? "new" : "old");
        test.checkRadioButton("location", zipFile ? "1" : "0");  // zip file vs. individual files
        test.clickNavButton("Export");
    }

}
