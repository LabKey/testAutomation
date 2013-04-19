/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

/**
 * User: elvan
 * Date: 11/21/12
 * Time: 12:52 PM
 */
public class AncillaryStudyFromSpecimenRequestTest extends StudyBaseTest
{

    public static final String DOV_DATASET = "DOV-1:";
    protected String ANCILLARY_STUDY_NAME = "Anc Study" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    protected String ANCILLARY_STUDY_DESC = "Study description";

    @Override
    public void doCreateSteps()
    {
        enableExperimentalFeature("CreateSpecimenStudy");
        importStudy();
        startSpecimenImport(2);
        waitForPipelineJobsToComplete(2, "study import", false);
    }

    public void doVerifySteps()
    {
        log("here");
        setupSpecimenManagement();
        clickTab("Specimen Data");
        clickAndWait(Locator.linkWithText("By Individual Vial"));
        
        selectSpecimens();
        createRequest();
        createStudy();
        verifyAncillaryStudy();
        

    }

    private void verifyAncillaryStudy()
    {
        clickFolder(ANCILLARY_STUDY_NAME);
        assertTextPresent(ANCILLARY_STUDY_DESC);
        clickTab("Mice");
        assertTextPresent("Showing all " + specimensToSelect.length + " mice");
        assertTextPresent(specimensToSelect);

        clickTab("Manage");
        assertTextPresent("This study defines 7 Datasets", "This study defines " + visitCount + " Visits", "This study references 24 locations (labs/sites/repositories)");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        assertTextPresent(DOV_DATASET, "APX-1", "DEM-1", "FPX-1", "TM-1", "IV-1", "EVC-1");
    }


    private void createStudy()
    {
        clickButton("Create Study", WAIT_FOR_EXT_MASK_TO_APPEAR);
        setFormElement("studyName", ANCILLARY_STUDY_NAME);
        setFormElement("studyDescription", ANCILLARY_STUDY_DESC);
        clickButton("Next", WAIT_FOR_EXT_MASK_TO_APPEAR);

        Locator datasetRow = Locator.tagContainingText("div", DOV_DATASET);
        waitForElement(datasetRow);
        clickAt(datasetRow, "1,1");
        clickButton("Finish");
        waitForPipelineJobsToFinish(3);


    }

    private void createRequest()
    {
        clickMenuButton("Request Options", "Create New Request");

        selectOptionByText("destinationLocation", "Aurum Health KOSH Lab, Orkney, South Africa (Endpoint Lab, Repository)");
        setFormElement("input0", "Assay Plan");
        setFormElement("input2", "Comments");
        setFormElement("input1", "Shipping");
        clickButton("Create and View Details");
    }


    protected String[] specimensToSelect = {"999320812", "999320396", "999320885", "999320746", "999320190", "999320466"};
    private void selectSpecimens()
    {
        for(String specimen : specimensToSelect)
        {
            checkCheckboxByNameInDataRegion(specimen);
        }
    }
}
