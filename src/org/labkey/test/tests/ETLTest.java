package org.labkey.test.tests;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.PortalHelper;

/**
 * Created with IntelliJ IDEA.
 * User: Rylan
 * Date: 3/26/13
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class ETLTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ETLTestProject";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        //Setup Steps
        runInitialSetup();
        //Confirm that if no changes were made, we don't initialize a pipeline job.
        checkRun(1);
        //Add an issue to detect and check that it creates a job
        addIssue("Issue #1", "issueuser", "The first issue.");
        checkRun(2);
    }

    protected void runInitialSetup()
    {
        //This is here to prevent an email send error at the end of the test
        enableEmailRecorder();
        _containerHelper.createProject(PROJECT_NAME, null);
        createUser("issueuser@testing.test", null, false);
        createProjectGroup(PROJECT_NAME, "IssueGroup");
        goToProjectHome();
        sleep(200);
        addUserToProjGroup("issueuser@testing.test", PROJECT_NAME, "IssueGroup");
        goToProjectHome();
        sleep(200);
        new PortalHelper(this).addWebPart("Issues List");
        addModule("DataIntegration");
        //Turn on the checker service (should cause a job to appear at the first pipeline check for the user we made)
        goToModule("DataIntegration");
        waitForElement(Locator.xpath("//tr[@transformid='etls/DemoETL/config.xml']/td/input"));
        click(Locator.xpath("//tr[@transformid='etls/DemoETL/config.xml']/td/input"));
    }

    protected void checkRun(int amount)
    {
        goToModule("DataIntegration");
        waitForElement(Locator.xpath("//tr[@transformid='etls/IssuesETL/config.xml']/td/a"));
        click(Locator.xpath("//tr[@transformid='etls/IssuesETL/config.xml']/td/a"));
        goToProjectHome();
        sleep(500);
        click(Locator.xpath("//span[@id='adminMenuPopupText']"));
        mouseOver(Locator.xpath("//span[text()='Go To Module']"));
        waitForElement(Locator.xpath("//span[text()='Pipeline']"));
        click(Locator.xpath("//span[text()='Pipeline']"));
        //There are two instances of the text "COMPLETE" on the page, so we compensate them out.
        assertTextPresent("COMPLETE", amount+2);
    }

    protected void addIssue(String issueName, String assignedTo, String comment)
    {
        goToProjectHome();
        waitForElement(Locator.xpath("//span[text()='New Issue']"));
        clickButton("New Issue");
        waitForElement(Locator.xpath("//input[@name='title']"));
        setFormElement(Locator.xpath("//input[@name='title']"), issueName);
        setFormElement(Locator.xpath("//select[@name='assignedTo']"), assignedTo);
        setFormElement(Locator.xpath("//textarea[@id='comment']"), comment);
        clickButton("Save");
        goToProjectHome();
    }

    protected void createProjectGroup(String projectName, String groupName)
    {
        if (isElementPresent(Locator.permissionRendered()))
        {
            exitPermissionsUI();
            clickAndWait(Locator.linkWithText(projectName));
        }
        enterPermissionsUI();
        click(Locator.xpath("//span[text()='Project Groups']"));
        waitForElement(Locator.xpath("//input[@name='projectgroupsname']"));
        setFormElement(Locator.xpath("//input[@name='projectgroupsname']"), groupName);
        click(Locator.xpath("//span[text()='Create New Group']"));
        waitForElement(Locator.xpath("//span[text()='Done']"));
        click(Locator.xpath("//span[text()='Done']"));
        waitForElement(Locator.xpath("//span[text()='Save and Finish']"));
        click(Locator.xpath("//span[text()='Save and Finish']"));
    }

    protected void addModule(String moduleName)
    {
        //Add a module to find later
        goToFolderManagement();
        waitForElement(Locator.xpath("//a[text()='Folder Type']"));
        click(Locator.xpath("//a[text()='Folder Type']"));
        waitForElement(Locator.xpath("//input[@value='"+moduleName+"']"));
        checkCheckbox(Locator.xpath("//input[@value='"+moduleName+"']"));
        clickButton("Update Folder");
    }

    @Override
    protected void doCleanup(boolean aftertest) throws TestTimeoutException
    {
        if(aftertest)
        {
            deleteUser("issueuser@testing.test");
        }

        super.doCleanup(aftertest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
