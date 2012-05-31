/*
 * Copyright (c) 2011-2012 LabKey Corporation
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
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.StringHelper;

/**
 * User: elvan
 * Date: 11/4/11
 * Time: 2:08 PM
 */
public class GroupTest extends BaseSeleniumWebTest
{
    protected static final String[] TEST_USERS_FOR_GROUP = {"user1@group1.group.test", "user2@group1.group.test", "user3@group2.group.test"};
    protected static final String SIMPLE_GROUP = "group1";
    protected static final String COMPOUND_GROUP = "group2";
    protected static final String BAD_GROUP = "group3";
    protected static final String CHILD_GROUP = "group4";
    protected static final String WIKITEST_NAME = "GroupSecurityApiTest";
    protected static final String GROUP_SECURITY_API_FILE = "groupSecurityTest.html";
    protected static final String API_SITE_GROUP = "API Site Group";

    @Override
    protected String getProjectName()
    {
        return "Group Verify Test Project";
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
        try{deleteGroup(CHILD_GROUP);}catch(Throwable t){}
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

        createProject(getProjectName(), "Collaboration");
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
        enterPermissionsUI();
        waitForText("Author");
        setSiteGroupPermissions(COMPOUND_GROUP, "Author");
        setSiteGroupPermissions(COMPOUND_GROUP, "Reader");
        setSiteGroupPermissions(SIMPLE_GROUP, "Editor");
        clickButton("Save and Finish");
        assertUserCanSeeFolder(TEST_USERS_FOR_GROUP[0], getProjectName());
        //can't add built in group to regular group
        log("Verify you can copy perms even with a default");

        //give a system group permissions, so that we can verify copying them doesn't cause a problem
        clickLinkWithText(getProjectName());
        enterPermissionsUI();
        waitForText("Author");
        ExtHelper.clickExtDropDownMenu(this, "$add$org.labkey.api.security.roles.AuthorRole", "Site: All Site Users");

        clickButton("Save", 0);
        waitForText("save successful");
        waitForExtMaskToDisappear();

        permissionsReportTest();

        goToProjectHome();

        createProjectCopyPerms();

        verifyImpersonateGroup();

        verifyCantAddSystemGroupToUserGroup();

        groupSecurityApiTest();
    }
    private void permissionsReportTest()
    {
        clickLinkWithText("view permissions report");
        DataRegionTable drt = new DataRegionTable("access", this, false, false);

        waitForText("Access Modification History For This Folder");
        assertTextPresent( "Folder Access Details");

        //this table isn't quite a real Labkey Table Region, so we can't use column names
        int detailsColumn = 0;
        int userColumn = 1;
        int accessColumn = 2;

        int rowIndex = drt.getRow(userColumn, TEST_USERS_FOR_GROUP[0]);

        if(getBrowser().startsWith(FIREFOX_BROWSER))
        //IE displays correctly but selenium retrieves the data differently
        {
            //confirm correct perms
            assertTrue(StringHelper.stringArraysAreEquivalent(new String[] {"Reader", "Author"},
                    drt.getDataAsText(rowIndex, 2).split(",")));
        }
        else
        {
            assertTrue(StringHelper.stringArraysAreEquivalent("Reader, Author RoleGroup(s) ReaderSite: group2AuthorSite: group2, Site: Users".split(" "),
                    drt.getDataAsText(rowIndex, 2).split(" ")));
        }


        //exapnd plus  to check specific groups
        clickAt(Locator.imageWithSrc("/labkey/_images/plus.gif", true).index(rowIndex+1), "1,1");
        assertTrue(StringHelper.stringArraysAreEquivalent("Reader, Author RoleGroup(s) ReaderSite: group2AuthorSite: group2, Site: Users".split(" "),
                drt.getDataAsText(rowIndex, 2).split(" ")));

        //confirm hover over produces list of broups
        Locator groupSpecification = Locator.tagContainingText("span","Site: group2");
        String groupHierarchy = getAttribute(groupSpecification, "ext:qtip");
        String[] expectedMessagesInHierarchy = new String[] {"user1@group1.group.test is a member of <strong>group1</strong>",
                "Which is a member of <strong>group2</strong><BR/>",
                "Which is assigned the Author role",
                "user1@group1.group.test is a member of <strong>group2</strong>",
                "Which is assigned the Author role"};
        for(String msg : expectedMessagesInHierarchy)
        {
                assertTrue("Expected group hover over: " + msg, groupHierarchy.contains(msg));
        }
//        assertTrue(groupHierarchy.contains());

        //confirm details link leads to right user, page
        clickLinkContainingText("details", rowIndex);
        assertTextPresent(TEST_USERS_FOR_GROUP[0]);
        assertTrue("details link for user did not lead to folder access page", getURL().getFile().contains("folderAccess.view"));
        goBack();

        //confirm username link leads to right user, page
        clickLinkContainingText(TEST_USERS_FOR_GROUP[0]);
        assertTextPresent("User Access Details: "  + TEST_USERS_FOR_GROUP[0]);
        goBack();

    }

    private void verifyImpersonateGroup()
    {
        //set simple group as editor
        setSiteGroupPermissions(SIMPLE_GROUP, "Editor");

        //impersonate user 1, make several wiki edits
        impersonate(TEST_USERS_FOR_GROUP[0]);
        clickLinkWithText(getProjectName());
        String[][] nameTitleBody = {{"Name1", "Title1", "Body1"}, {"Name2", "Title2", "Body2"}};

        for(int i=0; i<nameTitleBody.length; i++)
        {
            createNewWikiPage();
            setWikiValuesAndSave(nameTitleBody[i][0], nameTitleBody[i][1], nameTitleBody[i][2]);
        }
        stopImpersonating();

        //impersonate simple group, they should have full editor permissions
        impersonateGroup(SIMPLE_GROUP, true);
        verifyEditorPermission(nameTitleBody);
        stopImpersonatingGroup();

        //impersonate compound group, should only have author permissions
        impersonateGroup(COMPOUND_GROUP, true);
        verifyAuthorPermission(nameTitleBody);
        stopImpersonatingGroup();


        Locator unavailableEditorChoice = Locator.xpath("//li[contains(@class,'disabled')]//span[text()='Editor']");
        impersonateRole("Author");
        clickUserMenuItem(false, "Impersonate", "Role", "Author");
        assertElementNotPresent(unavailableEditorChoice);
        verifyAuthorPermission(nameTitleBody);
        impersonateRole("Editor");
        clickUserMenuItem(false, "Impersonate", "Role", "Author");
        assertElementPresent(unavailableEditorChoice);
        assertElementPresent(Locator.xpath("//li[contains(@class,'disabled')]//span[text()='Author']"));
        verifyEditorPermission(nameTitleBody);
        stopImpersonatingRole();

        //Issue 13802: add child group to SIMPLE_GROUP, child group should also have access to pages
        createGlobalPermissionsGroup(CHILD_GROUP, "");
        addUserToSiteGroup(CHILD_GROUP, SIMPLE_GROUP);
        impersonateGroup(CHILD_GROUP, true);
        clickLinkContainingText(getProjectName());
        verifyEditorPermission(nameTitleBody);
        stopImpersonatingGroup();
    }

    private void verifyAuthorPermission(String[][] nameTitleBody)
    {
        clickLinkContainingText(getProjectName());
        assertTrue("could not see wiki pages when impersonating " + SIMPLE_GROUP,canSeePages(nameTitleBody));
        assertFalse("Was able to edit wiki page when impersonating group without privileges", canEditPages(nameTitleBody));
        sleep(500);
    }

    private void verifyEditorPermission(String[][] nameTitleBody)
    {
        clickLinkContainingText(getProjectName());
        assertTrue("could not see wiki pages when impersonating " + SIMPLE_GROUP, canSeePages(nameTitleBody));
        assertTrue("could not edit wiki pages when impersonating " + SIMPLE_GROUP, canEditPages(nameTitleBody));
        sleep(500);
    }

    private boolean canEditPages(String[][] nameTitleBody)
    {
        for(int i=0; i<nameTitleBody.length; i++)
        {
            waitForElement(Locator.linkContainingText(nameTitleBody[i][1]), defaultWaitForPage);
            sleep(1000);
            clickLinkWithText(nameTitleBody[i][1]);
            if(!isTextPresent("Edit"))
                return false;
            selenium.goBack();
        }
        return true;
    }

    private boolean canSeePages(String[][] nameTitleBody)
    {
        for(int i=0; i<nameTitleBody.length; i++)
        {
            if(!isTextPresent(nameTitleBody[i][1]))
                return false;
        }
        return true;
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
        startCreateGlobalPermissionsGroup(BAD_GROUP, true);
        setFormElement("Users_dropdownMenu", "All Site Users");

        ExtHelper.clickExtDropDownMenu(this, Locator.xpath("//input[@id='Users_dropdownMenu']/../img"), "Site: All Site Users");
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
        goToCreateProject();
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
        ExtHelper.clickExtDropDownMenu(this, Locator.xpath("//div[@id='targetProject-bodyEl']/input"), getProjectName());
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
        return null;
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
