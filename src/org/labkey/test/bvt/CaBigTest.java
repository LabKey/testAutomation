package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: tamram
 * Date: May 15, 2006
 */
public class CaBigTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "CaBigVerifyProject";
    protected static final String FOLDER_NAME = "CaBigFolder";

    public String getAssociatedModuleDirectory()
    {
        return "cabig";
    }

    protected void doCleanup()
    {
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {/* */}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {/* */}
    }

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME);

        boolean caBigEnabled = isTextPresent("Publish to caBIG");

        if (!caBigEnabled)
            setCaBigSiteSetting(true);

        // Test publish/unpublish on the project
        assertTextPresent("This folder is not published to the caBIG");
        clickNavButton("Publish");
        assertTextPresent("This folder is published to the caBIG");
        clickNavButton("Unpublish");
        assertTextPresent("This folder is not published to the caBIG");

        // Create a subfolder
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[0]);
        clickLinkWithText("Permissions");
        clickNavButton("Publish");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Permissions");

        // Test caBIG admin page
        clickNavButton("Admin");
        assertNavButtonPresent("Publish");
        assertNavButtonPresent("Unpublish");
        clickNavButton("Publish All");
        assertNavButtonNotPresent("Publish");
        clickNavButton("Unpublish All");
        assertNavButtonNotPresent("Unpublish");

        clickNavButton("Done");

        // Should be on the project permissions page
        assertTextPresent("Permissions for /" + PROJECT_NAME);
        assertTextPresent("This folder is not published to the caBIG");

        // Turn off caBIG if it was originally off
        if (!caBigEnabled)
            setCaBigSiteSetting(false);
    }

    private void setCaBigSiteSetting(boolean enable)
    {
        pushLocation();

        clickLinkWithText("Admin Console");
        clickLinkWithText("site settings");

        if (enable)
            checkCheckbox("caBIGEnabled");
        else
            uncheckCheckbox("caBIGEnabled");

        clickNavButton("Save");

        popLocation();
    }
}