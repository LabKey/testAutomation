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
 * User: elvan
 * Date: 11/4/11
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class GroupTest extends BaseSeleniumWebTest
{

    protected static final String[] TEST_USERS_FOR_GROUP = {"user1@group1.group.test", "user2@group1.group.test", "user3@group2.group.test"};
    protected static final String SIMPLE_GROUP = "group1";
    protected static final String COMPOUND_GROUP = "group2";
    protected static final String BAD_GROUP = "group3";
    protected static final String WIKITEST_NAME = "GroupSecurityApiTest";
    protected static final String GROUP_SECURITY_API_FILE = "groupSecurityTest.html";
    protected static final String API_SITE_GROUP = "API Site Group";

    @Override
    protected String getProjectName()
    {
        return "Group Verify Test Project";  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected String getProject2Name()
    {
        return getProjectName() + "2";
    }

    protected void doCleanup()
    {
        for(String user : TEST_USERS_FOR_GROUP)
        {
            deleteUser(user);
        }
        try{deleteGroup(SIMPLE_GROUP);}catch(Throwable t){/*ignore*/}
        try{deleteGroup(COMPOUND_GROUP);}catch(Throwable t){/*ignore*/}
        try{deleteGroup(BAD_GROUP);}catch(Throwable t){/*ignore*/}
        try{deleteGroup(API_SITE_GROUP);}catch(Throwable t){/*ignore*/}
        try{deleteProject(getProjectName());}catch(Throwable t){/*ignore*/}
        try{deleteProject(getProject2Name());}catch(Throwable t){/*ignore*/}
    }

    protected void init()
    {


        for(int i=0; i<TEST_USERS_FOR_GROUP.length; i++)
        {
            createUser(TEST_USERS_FOR_GROUP[i], null);
        }

        createProject(getProjectName());
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        init();

        //double check that user can't see the project yet- otherwise our later check will be invalid

        impersonate(TEST_USERS_FOR_GROUP[0]);
        assertLinkNotPresentWithText(getProjectName());
        stopImpersonating();
        //create users


        createGlobalPermissionsGroup(SIMPLE_GROUP,  TEST_USERS_FOR_GROUP[0], TEST_USERS_FOR_GROUP[1]);
        createGlobalPermissionsGroup(COMPOUND_GROUP, SIMPLE_GROUP,  TEST_USERS_FOR_GROUP[2]);


        verifyExportFunction();

        verifyRedundantUserWarnings();

        //add read permissions to group2
        goToHome();
        clickLinkWithText(getProjectName());
        clickLinkWithText("Permissions");
        waitForPageToLoad();

        waitForElement(Locator.id("$add$org.labkey.api.security.roles.EditorRole"), defaultWaitForPage);
        ExtHelper.clickExtDropDownMenu(this, "$add$org.labkey.api.security.roles.EditorRole", COMPOUND_GROUP);
        clickButton("Save and Finish");
        assertUserCanSeeFolder(TEST_USERS_FOR_GROUP[0], getProjectName());
        //can't add built in group to regular group
        log("Verify you can copy perms even with a default");

        clickLinkWithText(getProjectName());
        clickLinkWithText("Permissions");
        waitForPageToLoad();
        ExtHelper.clickExtDropDownMenu(this, "$add$org.labkey.api.security.roles.AuthorRole", "All Site Users");
        clickButton("Save and Finish");

        createProjectCopyPerms();

        //TODO: Blocked: 13299: Various IllegalStateExceptions related to users/groups written to log file
        //verifyCantAddSystemGroupToUserGroup();

        //TODO: Blocked: 13299: Various IllegalStateExceptions related to users/groups written to log file
        //groupSecurityApiTest(); // todo: talk to Li about where to put this call in the test
    }

    //should be at manage group page of COMPOUND_GROUP already
    //verify attempting add a user and a group containing that user to another group results in a warning
    private void verifyRedundantUserWarnings()
    {
        setFormElement("names", TEST_USERS_FOR_GROUP[0]); //this user is in group1 and so is already in group 2
        clickButton("Update Group Membership");
        assertTextPresent(TEST_USERS_FOR_GROUP[0] + "*", "* These group members already appear in other included member groups and can be safely removed.");
//        expect warning
    }

    private void verifyExportFunction()
    {
        selectGroup(COMPOUND_GROUP);
        clickLinkWithText("manage group");
        //Selenium can't handle file exports, so there's nothing to be done here.
        assertElementPresent(getButtonLocatorContainingText("Export All to Excel"));

    }

    private void verifyCantAddSystemGroupToUserGroup()
    {
        startCreateGlobalPermissionsGroup(BAD_GROUP);
        setFormElement("Users_dropdownMenu", "All Site Users");

        ExtHelper.clickExtDropDownMenu(this, Locator.xpath("//input[@id='Users_dropdownMenu']/../img"), "All Site Users");
        waitForText("Can't add a system group to another group");
        clickButton("OK", 0);
        clickButton("Done");
    }

    protected void createProjectCopyPerms()
    {
        String projectName = getProject2Name();
        String folderType = null;

        ensureAdminMode();
        log("Creating project with name " + projectName);
        if (isLinkPresentWithText(projectName))
            fail("Cannot create project; A link with text " + projectName + " already exists.  " +
                    "This project may already exist, or its name appears elsewhere in the UI.");
        clickLinkWithText("Create Project");
        waitForElement(Locator.name("name"), 100*WAIT_FOR_JAVASCRIPT);
        setText("name", projectName);

        if (null != folderType && !folderType.equals("None"))
            click(Locator.xpath("//div[./label[text()='"+folderType+"']]/input"));
        else
            click(Locator.xpath("//div[./label[text()='Custom']]/input"));

        waitAndClick(Locator.xpath("//button[./span[text()='Next']]"));
        waitForPageToLoad();

        //second page of the wizard
        click(Locator.xpath("//label[contains(text(), 'Copy From Existing Project')]/../input"));
        ExtHelper.clickExt4DropDownMenu(this, Locator.xpath("//div[@id='targetProject-bodyEl']/input"), getProjectName());
        waitAndClick(Locator.xpath("//button[./span[text()='Next']]"));
        waitForPageToLoad();

        //third page of wizard
        waitAndClick(Locator.xpath("//button[./span[text()='Finish']]"));
        waitForPageToLoad();

        assertUserCanSeeFolder(TEST_USERS_FOR_GROUP[1], getProject2Name());

//        _createdProjects.add(projectName);
    }


    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void assertUserCanSeeFolder(String user, String folder)
    {

        impersonate(user);
        assertLinkPresentWithText(folder);
        stopImpersonating();
    }

    protected void groupSecurityApiTest()
    {
        // Initialize the Wiki
        clickLinkWithText(getProjectName());
        addWebPart("Wiki");

        createNewWikiPage();
        setFormElement("name", WIKITEST_NAME);
        setFormElement("title", WIKITEST_NAME);
        setWikiBody("Placeholder text.");
        saveWikiPage();

        setSourceFromFile(GROUP_SECURITY_API_FILE, WIKITEST_NAME);

        // Run the Test Script
        clickButton("Start Test", 0);
        waitForText("Done!", defaultWaitForPage);
        assertTextNotPresent("Error");
    }
}
