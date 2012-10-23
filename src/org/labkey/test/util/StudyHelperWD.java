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

import junit.framework.Assert;
import org.bouncycastle.crypto.digests.LongDigest;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4CmpRefWD;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Aug 16, 2011
 * Time: 3:45:02 PM
 */
public class StudyHelperWD extends AbstractHelperWD
{
    public StudyHelperWD(BaseWebDriverTest test)
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
        if( !_test.isElementPresent(Locator.xpath("//div[contains(@class, 'labkey-nav-page-header') and text() = 'Manage "+participantString+" Groups']")) )
        {
            _test.clickFolder(projectName);
            _test.clickFolder(studyFolder);
            _test.clickTab("Manage");
            _test.clickLinkWithText("Manage " + participantString + " Groups");
            _test.waitForText("groups allow");
        }
        _test.log("Create "+participantString+" Group: " + groupName);
        _test.clickButton("Create", 0);
        _test._extHelper.waitForExtDialog("Define "+participantString+" Group");
        if (demographicsPresent)
            _test.waitForElement(Locator.id("dataregion_demoDataRegion"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.name("groupLabel"), groupName);
        if( ptids.length > 0 )
        {
            String csp = ptids[0];
            for( int i = 1; i < ptids.length; i++ )
                csp += ","+ptids[i];
            _test.setFormElement(Locator.name("participantIdentifiers"), csp);
        }
        if( categoryName != null )
        {
            if (isCategoryNameNew)
                _test.setFormElement(Locator.name("participantCategory"), categoryName);
            else
                _test._ext4Helper.selectComboBoxItem(participantString + " Category", categoryName);
            _test.pressTab(Locator.name("participantCategory"));
            _test.waitForElementToDisappear(Locator.css(".x-form-focus"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
        if ( shared != null )
        {
            if( shared )
            {
                _test._ext4Helper.checkCheckbox("Share Category?");
            }
            else
            {
                _test._ext4Helper.uncheckCheckbox("Share Category?");
            }
        }

        _test._extHelper.clickExtButton("Define " + participantString + " Group", "Save", 0);
        _test._extHelper.waitForExt3MaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    public void editCustomParticipantGroup(String groupName, String participantString, String categoryName,
                                           Boolean isCategoryNameNew, Boolean shared, String... newPtids)
    {
        editCustomParticipantGroup(groupName, participantString, categoryName, isCategoryNameNew, shared, false, newPtids);
    }

    @LogMethod
    public void editCustomParticipantGroup(String groupName, String participantString,
                                                  String categoryName, Boolean isCategoryNameNew, Boolean shared, Boolean containsDemographics, String... newPtids)
    {
        _test.log("Edit " + participantString + " Group: " + groupName);

        // Select row
        selectParticipantCategoriesGridRow(groupName);

        _test.clickButton("Edit Selected", 0);
        _test._extHelper.waitForExtDialog("Define " + participantString + " Group");
        _test.waitForElement(Locator.css(".doneLoadingTestMarker"));

        if( newPtids != null && newPtids.length > 0 )
        {
            String csp = newPtids[0];
            for( int i = 1; i < newPtids.length; i++ )
                csp += ","+newPtids[i];
            String currentIds = _test.getFormElement(Locator.name("participantIdentifiers"));
            if (currentIds != null && currentIds.length() > 0)
                _test.setFormElement(Locator.name("participantIdentifiers"), currentIds + "," + csp);
            else
                _test.setFormElement(Locator.name("participantIdentifiers"), csp);
        }
        if( categoryName != null )
        {
            if (isCategoryNameNew)
                _test.setFormElement(Locator.name("participantCategory"), categoryName);
            else
                _test._ext4Helper.selectComboBoxItem(participantString + " Category", categoryName);
            _test.pressTab(Locator.name("participantCategory"));
            _test.waitForElementToDisappear(Locator.css(".x-form-focus"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
        if ( shared != null )
        {
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
        _test._extHelper.waitForExt3MaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void selectParticipantCategoriesGridRow(String groupName)
    {
        _test._extHelper.selectExt4GridItem("label", groupName, -1, "participantCategoriesGrid", false);
        _test.click(Locator.xpath("//*[text()='" + groupName + "']")); // Ext.select doesn't trigger click events
    }

    public void exportStudy(String folder)
    {
        exportStudy(folder, true, false);
    }

    @LogMethod
    public void exportStudy(String folder, boolean useXmlFormat, boolean zipFile)
    {
        _test.clickFolder(folder);
        _test.clickTab("Manage");
        _test.clickButton("Export Study");

        _test.assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings", "Participant Groups", "Protocol Documents");
        // NOTE: these have moved to the folder archive export: "Queries", "Custom Views", "Reports", "Lists"

        _test.checkRadioButton("format", useXmlFormat ? "new" : "old");
        _test.checkRadioButton("location", zipFile ? "1" : "0");  // zip file vs. individual files
        _test.clickButton("Export");
    }

}
