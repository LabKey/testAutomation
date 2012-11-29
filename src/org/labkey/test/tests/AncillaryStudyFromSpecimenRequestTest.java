package org.labkey.test.tests;

import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 11/21/12
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
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
        clickLinkWithText("By Individual Vial");
        
        selectSpecimens();
        createRequest();
        createStudy();
        verifyAncillaryStudy();
        

    }

    private void verifyAncillaryStudy()
    {
        clickLinkWithText(ANCILLARY_STUDY_NAME);
        assertTextPresent(ANCILLARY_STUDY_DESC);
        clickTab("Mice");
        assertTextPresent("Showing all " + specimensToSelect.length + " mice");
        assertTextPresent(specimensToSelect);

        clickTab("Manage");
        assertTextPresent("This study defines 7 Datasets", "This study defines 64 Visits", "This study references 24 labs/sites/repositories");
        clickLinkWithText("Manage Datasets");
        assertTextPresent(DOV_DATASET, "APX-1", "DEM-1", "FPX-1", "TM-1", "IV-1", "EVC-1");
    }


    private void createStudy()
    {
        clickButton("Create Study", WAIT_FOR_EXT_MASK_TO_APPEAR);
        setFormElement("studyName", ANCILLARY_STUDY_NAME);
        setFormElement("studyDescription", ANCILLARY_STUDY_DESC);
        clickButton("Next", WAIT_FOR_EXT_MASK_TO_APPEAR);

        clickAt(Locator.tagContainingText("div", DOV_DATASET), "1,1");
        clickButton("Finish");
        waitForPipelineJobsToFinish(3);


    }

    private void createRequest()
    {
        clickMenuButton("Request Options", "Create New Request");

        selectOptionByText("destinationSite", "Aurum Health KOSH Lab, Orkney, South Africa (Repository)");
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
