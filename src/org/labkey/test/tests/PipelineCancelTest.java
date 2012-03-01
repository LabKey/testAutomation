package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.pipeline.PipelineWebTestBase;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 2/29/12
 * Time: 6:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class PipelineCancelTest  extends BaseSeleniumWebTest
{
    private static final String STUDY_ZIP = "/sampledata/study/LabkeyDemoStudy.zip";
    @Override
    protected String getProjectName()
    {
        return "Pipeline Cancel Test";
    }

    public void doTestSteps()
    {
        createProject(getProjectName(), "Study");
        startImportStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());

        log("Cancel import");
        clickLinkWithText("LOAD STUDY RUNNING");
        clickButton("Cancel");

        log("Verify cancel succeeded");
        assertTextPresent("CANCELLING", "Attempting to cancel", "Interrupting job by sending interrupt request.");
        waitForTextWithRefresh("CANCELLED", defaultWaitForPage);

        goToProjectHome();
        assertTextPresent("Data is present for 0 Participants"); //part of the import will be done, but it shouldn't have gotten to participants.

    }

    @Override
    protected void doCleanup() throws Exception
    {
       deleteProject(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
