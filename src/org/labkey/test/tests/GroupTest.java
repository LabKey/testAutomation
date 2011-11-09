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

    protected static final String[] TEST_USERS_FOR_GROUP = {"user1@group1.com", "user2@group1.com", "user3@group2.com"};
    protected static final String SIMPLE_GROUP = "group1";
    protected static final String COMPOUND_GROUP = "group2";
    protected static final String BAD_GROUP = "group3";

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
        deleteGroup(SIMPLE_GROUP, false);
        deleteGroup(COMPOUND_GROUP, false);
        deleteGroup(BAD_GROUP, false);
        deleteProject(getProjectName());
        deleteProject(getProject2Name());
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

        log("TODO");

        //add read permissions to group2
        goToHome();
        clickLinkWithText(getProjectName());
        clickLinkWithText("Permissions");
        waitForPageToLoad();

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
        verifyCantAddSystemGroupToUserGroup();

    }

    private void verifyCantAddSystemGroupToUserGroup()
    {
        startcreateGlobalPermissionsGroup(BAD_GROUP);
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
        ExtHelper.clickExt4DropDownMenu(this, Locator.name("targetProject"), getProjectName());
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
}
