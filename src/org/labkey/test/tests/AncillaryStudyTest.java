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
package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Sep 8, 2011
 * Time: 1:55:05 PM
 */
public class AncillaryStudyTest extends StudyBaseTest
{
    private static final String PROJECT_NAME = "AncillaryStudyTest Project";
    private static final String STUDY_NAME = "Ancillary Study";
    private static final String PARTICIPANT_GROUP = "Ancillary Group";
    private static final String[] DATASETS = {"AE-1:(VTN) AE Log","APX-1: Abbreviated Physical Exam","BRA-1: Behavioral Risk Assessment (Page 1)","BRA-2: Behavioral Risk Assessment (Page 2)","CM-1:(Ph I/II) Concomitant Medications Log","CPF-1: Follow-up Chemistry Panel","CPS-1: Screening Chemistry Panel","DEM-1: Demographics","DOV-1: Discontinuation of Vaccination","ECI-1: Eligibility Criteria"};

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/...";
    }

    @Override
    public void doCleanup()
    {
        // Delete any containers and users created by the test.
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void doCreateSteps()
    {
        importStudy();
        startSpecimenImport(2);
        waitForPipelineJobsToComplete(2, "study import", false);
    }

    @Override
    public void doVerifySteps()
    {
        clickLinkWithText("My Study");
        clickLinkWithText("Manage Study");

        log("Create Special Emphasis Study.");
        clickNavButton("Create New Study", 0);
        
        //Wizard page 1 - location
        ExtHelper.waitForExtDialog(this, "Create New Study");
        setFormElement("studyFolder", "/"+PROJECT_NAME+"/"+STUDY_NAME);
        clickNavButton("Next", 0);

        //Wizard page 2 - participant group
        waitForElement(Locator.radioButtonByName("renderType"), WAIT_FOR_JAVASCRIPT);
        assertWizardError("Next", "You must select an existing group or create a new one.");
        checkRadioButton("renderType", "all");

        // kbl: commented out current wizard only allows existing participant groups or all participants (although this could change)
/*
        checkRadioButton("renderType", "new");
        waitForElement(Locator.xpath("//table[@id='dataregion_demoDataRegion']"), WAIT_FOR_JAVASCRIPT);
        assertWizardError("Next", "Mouse Category Label required.");
        setFormElement("categoryLabel", PARTICIPANT_GROUP);
        assertWizardError("Next", "One or more Mouse Identifiers required.");
        waitForElement(Locator.name(".toggle"), WAIT_FOR_JAVASCRIPT);
        sleep(100); // wait for specimen grid to be ready.
        checkAllOnPage("demoDataRegion");
        uncheckDataRegionCheckbox("demoDataRegion", 0);
        uncheckDataRegionCheckbox("demoDataRegion", 1);
        clickNavButton("Add Selected", 0);
        sleep(1000); // wait for specimen Ids to appear in form.
*/
        clickNavButton("Next", 0);

        //Wizard page 3 - select datasets
        waitForElement(Locator.xpath("//div[contains(@class, 'studyWizardDatasetList')]"), WAIT_FOR_JAVASCRIPT);
        assertWizardError("Finish", "You must select at least one dataset to create the new study from.");
        for(int i = 0; i < DATASETS.length; i++)
        {
            selenium.getEval("selenium.selectExtGridItem('Label', '"+DATASETS[i]+"', null, 'studyWizardDatasetList', true)");
        }
        assertWizardError("Finish", "An error occurred trying to load: A study already exists in the destination folder.");

        clickNavButton("Previous", 0);
        clickNavButton("Previous", 0);
        setFormElement("studyName", STUDY_NAME);
        setFormElement(Locator.name("studyFolder"), "/"+PROJECT_NAME+"/"+STUDY_NAME);
        clickNavButton("Next", 0);
        clickNavButton("Next", 0);
        clickNavButton("Finish", 0);
        waitForExtMaskToDisappear();
    }

    private void assertWizardError(String button, String error)
    {
        clickNavButton(button, 0);
        waitForText(error);
        clickNavButton("OK", 0);
        waitForTextToDisappear(error);
    }

    public String getProjectName()
    {
        return PROJECT_NAME;
    }
}
