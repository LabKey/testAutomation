package org.labkey.test.module;

import org.labkey.test.TestTimeoutException;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 1/25/13
 * Time: 4:31 PM
 */
public class ONPRC_EHRTest extends AbstractEHRTest
{
    protected String PROJECT_NAME = "ONPRC_EHR_TestProject";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public String getContainerPath()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void goToEHRFolder()
    {
        clickFolder(getProjectName());
    }

    public void runUITests() throws Exception
    {
        setupTest();

        // for now, this test will import the ONPRC reference study
        // and run query validation.  most test coverage is part of main EHR tests,
        // but this does provide some valuable additional coverage
    }

    public void setupTest() throws Exception
    {
        _containerHelper.createProject(PROJECT_NAME, "ONPRC EHR");

        setEHRModuleProperties();

        //note: we create the users prior to study import, b/c that user is used by TableCustomizers
        createUsersandPermissions();

        File path = new File(getLabKeyRoot());
        path = new File(path, "/server/customModules/onprc_ehr/resources/referenceStudy");
        setPipelineRoot(path.getPath());
        importStudy();

        defineQCStates();
    }

    protected void importStudy()
    {
        goToModule("Pipeline");
        waitAndClickButton("Process and Import Data");

        _extHelper.selectFileBrowserRoot();
        _extHelper.clickFileBrowserFileCheckbox("study.xml");

        if (isTextPresent("Reload Study"))
            selectImportDataAction("Reload Study");
        else
            selectImportDataAction("Import Study");
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }
}
