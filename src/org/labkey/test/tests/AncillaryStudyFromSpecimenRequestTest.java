/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.test.categories.DailyC;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExperimentalFeaturesHelper;

@Category({DailyC.class})
public class AncillaryStudyFromSpecimenRequestTest extends StudyBaseTest
{
    public static final String DOV_DATASET = "DOV-1:";
    protected String ANCILLARY_STUDY_NAME = "Anc Study" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    protected String ANCILLARY_STUDY_DESC = "Study description";
    protected String REPUBLISH_STUDY_NAME = "Republish Study" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    protected String REPUBLISH_STUDY_DESC = "Study description (republished)";

    @Override
    public void doCreateSteps()
    {
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(true), "CreateSpecimenStudy");
        importStudy();
        startSpecimenImport(2);
        waitForPipelineJobsToComplete(2, "study import", false);
    }

    public void doVerifySteps()
    {
        setupRequestStatuses();
        selectSpecimens();
        createRequest();
        createStudy();
        verifyStudy(ANCILLARY_STUDY_NAME, ANCILLARY_STUDY_DESC);
        republishStudy();
        verifyStudy(REPUBLISH_STUDY_NAME, REPUBLISH_STUDY_DESC);
    }

    private void verifyStudy(String name, String description)
    {
        navigateToFolder(getProjectName(), name);
        //clickFolder(name);
        assertTextPresent(description);
        clickTab("Mice");
        waitForText("Found " + specimensToSelect.length + " mice of " + specimensToSelect.length);
        assertTextPresent(specimensToSelect);

        clickTab("Manage");
        assertTextPresent("This study defines 7 datasets", "This study defines " + visitCount + " visits", "This study references 24 locations (labs/sites/repositories)");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        assertTextPresent(DOV_DATASET, "APX-1", "DEM-1", "FPX-1", "TM-1", "IV-1", "EVC-1");
    }

    private void createStudy()
    {
        clickButton("Create Study", WAIT_FOR_EXT_MASK_TO_APPEAR);
        setFormElement(Locator.name("studyName"), ANCILLARY_STUDY_NAME);
        setFormElement(Locator.name("studyDescription"), ANCILLARY_STUDY_DESC);
        clickButton("Next", WAIT_FOR_EXT_MASK_TO_APPEAR);

        Locator datasetRow = Locator.tagContainingText("div", DOV_DATASET);
        waitForElement(datasetRow);
        click(datasetRow);
        clickButton("Finish");
        waitForPipelineJobsToFinish(3);
    }

    private void republishStudy()
    {
        clickFolder(getFolderName());
        goToSchemaBrowser();
        viewQueryData("study", "StudySnapshot");
        waitForText("\"specimenRequestId\"", 1, WAIT_FOR_PAGE);
        assertElementPresent(Locator.linkWithText("Republish"), 1);
        clickAndWait(Locator.linkWithText("Republish"), WAIT_FOR_EXT_MASK_TO_APPEAR);
        setFormElement(Locator.name("studyName"), REPUBLISH_STUDY_NAME);
        setFormElement(Locator.name("studyDescription"), REPUBLISH_STUDY_DESC);
        clickButton("Next", WAIT_FOR_EXT_MASK_TO_APPEAR);
        clickButton("Finish");
        waitForPipelineJobsToFinish(4);
    }

    private void createRequest()
    {
        new BootstrapMenu(getDriver(), Locator.tagWithClass("div", "lk-menu-drop")
                    .withDescendant(Locator.tag("span").withText("Request Options")).findElement(getDriver())
            ).clickSubMenu(true, "Create New Request");

        selectOptionByText(Locator.name("destinationLocation"), "Aurum Health KOSH Lab, Orkney, South Africa (Endpoint Lab, Repository)");
        setFormElement(Locator.id("input0"), "Assay Plan");
        setFormElement(Locator.id("input2"), "Comments");
        setFormElement(Locator.id("input1"), "Shipping");
        clickButton("Create and View Details");
    }

    protected String[] specimensToSelect = {"999320812", "999320396", "999320885", "999320746", "999320190", "999320466"};
    private void selectSpecimens()
    {
        goToSpecimenData();
        waitAndClickAndWait(Locator.linkWithText("By Individual Vial"));

        DataRegionTable specimenDetail = new DataRegionTable("SpecimenDetail", this);
        for(String specimen : specimensToSelect)
        {
            specimenDetail.checkCheckbox(specimenDetail.getRowIndex("MouseId", specimen));
        }
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}