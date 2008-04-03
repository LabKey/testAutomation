package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: ulberge
 * Date: Aug 7, 2007
 * Time: 3:41:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ModuleTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "ModuleVerifyProject";
    private static final String TEST_MODULE_TEMPLATE_FOLDER_NAME = "testmodule";

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME);
        log("Test module created from moduleTemplate");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Customize Folder");
        checkCheckbox(Locator.raw("//input[@value='" + TEST_MODULE_TEMPLATE_FOLDER_NAME + "']"));
        clickNavButton("Update Folder");
        clickTab(TEST_MODULE_TEMPLATE_FOLDER_NAME);
        assertTextPresent("Hello, and welcome to the " + TEST_MODULE_TEMPLATE_FOLDER_NAME + " module.");
    }
    
    public String getAssociatedModuleDirectory()
    {
        return "none";
    }

}
