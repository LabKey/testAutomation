/*
 * Copyright (c) 2013 LabKey Corporation
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
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.io.File;

/**
 * User: davebradlee
 * Date: 8/27/13
 * Time: 3:22 PM
 */
@Category({CustomModules.class})
public class NpodDonorSamplesTest extends StudyBaseTestWD
{
    protected static final String NPOD_STUDYARCHIVE = "NpodTestStudy.zip";  // Includes NPOD Tools web part and Editable Specimens
    protected static final String PARTICIPANTTAB_NAME = "nPOD CaseIDs";
    protected PortalHelper _portalHelper;

    @Override
    protected String getProjectName()
    {
        return "NPOD Project";
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/npod";
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        _portalHelper = new PortalHelper(this);
        initializeFolder();
        setPipelineRoot(getPipelinePath());
        importStudy();

        setupRequestabilityRules();
        setupRequestStatuses();
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        addNpodModule();
        clickFolder(getFolderName());
        clickTab(PARTICIPANTTAB_NAME);

        testManageWizard();
        createDonorWizardView();
        testForm();
    }

    @LogMethod
    protected void setupRequestabilityRules()
    {
        // Create custom query to test requestability rules.
        clickFolder(getFolderName());
        waitAndClick(Locator.linkWithText("Manage Study"));
        waitAndClick(Locator.linkWithText("Manage Requestability Rules"));
        waitForElement(Locator.xpath("//div[contains(@class, 'x-grid3-row')]//div[text()='Locked In Request Check']"));

        clickButton("Add Rule", 0);
        click(Locator.menuItem("Locked While Processing Check"));

        // Remove Locked In Request
        waitForElement(Locator.xpath("//div[contains(@class, 'x-grid3-row')]//div[text()='Locked In Request Check']"));
        click(Locator.xpath("//div[contains(@class, 'x-grid3-row')]//div[text()='Locked In Request Check']"));
        clickButton("Remove Rule", 0);

        clickButton("Save");
    }

    protected void importStudy()
    {
        clickFolder(getFolderName());
        importStudyFromZip(new File(getPipelinePath() + NPOD_STUDYARCHIVE));
    }

    private void addNpodModule()
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("nPOD"));
        clickButton("Update Folder");
    }

    private void testManageWizard()
    {
        clickFolder(getFolderName());
        clickTab(PARTICIPANTTAB_NAME);
        clickButton("Manage Form", "Select one or more datasets");
        assertTextPresent("Select one or more priority sample types.");

        // test some stuff
        clickButton("Manage Form", 0);      // Close panel
    }

    private void createDonorWizardView()
    {
        clickFolder(getFolderName());
        clickTab(PARTICIPANTTAB_NAME);
        clickButton("Manage Sample Columns");
        assertTextPresent("SpecimenDetail");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.clearCustomizeViewColumns();
        _customizeViewsHelper.addCustomizeViewColumn("VolumeUnits");
        _customizeViewsHelper.addCustomizeViewColumn("ProtocolNumber");
        _customizeViewsHelper.addCustomizeViewColumn("requestable", "Requestable");
        _customizeViewsHelper.addCustomizeViewColumn("Freezer");
        _customizeViewsHelper.addCustomizeViewColumn("LatestComments");
        _customizeViewsHelper.addCustomizeViewColumn("PrimaryType");
        _customizeViewsHelper.addCustomizeViewColumn("DerivativeType");
        _customizeViewsHelper.saveCustomView("DonorWizardView", false);

    }
    private void testForm()
    {
        clickFolder(getFolderName());
        clickTab(PARTICIPANTTAB_NAME);
        clickButton("New nPOD CaseID", "UNOS IDs");

        // TODO: needs work
//        setFormElement(Locator.elementByLabel("N PODCase ID*", 0, "textfield", 0), "224433");
//        setFormElement(Locator.elementByLabel("UNOS ID", 0, "textfield", 0), "224433U");

        clickButton("Next", "Demographics");
        clickButton("Next", "Lifestyle");
        clickButton("Next", "Diabetes Information");
        clickButton("Next", "Samples");
        clickButton("Next", "Cancel");
        clickButton("Cancel");

        clickTab(PARTICIPANTTAB_NAME);
    }
}
