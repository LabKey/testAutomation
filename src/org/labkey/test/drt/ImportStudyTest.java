package org.labkey.test.drt;

/**
 * User: adam
 * Date: Apr 3, 2009
 * Time: 9:18:32 AM
 */
public class ImportStudyTest extends StudyTest
{
    protected static final String PROJECT_NAME = "ImportStudyVerifyProject";
    protected static final String FOLDER_NAME = "My Import Study";

    @Override
    protected String getProjectName()
    {
        return "ImportStudyVerifyProject";
    }

    @Override
    protected String getFolderName()
    {
        return "My Import Study";
    }

    @Override
    protected String getSampleDataPath()
    {
        return super.getSampleDataPath() + "import/";
    }

    @Override
    protected void createStudy()
    {
        initializeFolder();
        initializePipeline();

        beginAt("study/" + getProjectName() + "/" + getFolderName() + "/importStudy.view");
        clickButtonContainingText("Import Study");

        // TODO: Verify that pipeline is done?
    }

    private void initializePipeline()
    {
        clickLinkWithText("Customize Folder");
        toggleCheckboxByTitle("Pipeline");
        submit();
        addWebPart("Data Pipeline");
        clickNavButton("Setup");
        setFormElement("path", getPipelinePath());
        submit();
    }
}
