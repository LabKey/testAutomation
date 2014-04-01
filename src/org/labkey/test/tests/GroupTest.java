/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.NoSuchElementException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

@Category(BVT.class)
public class GroupTest extends BaseWebDriverTest
{
    protected static final String SIMPLE_GROUP = "group1";
    protected static final String COMPOUND_GROUP = "group2";
    protected static final String BAD_GROUP = "group3";
    protected static final String CHILD_GROUP = "group4";
    protected static final String[] TEST_USERS_FOR_GROUP = {"user1_grouptest@" + SIMPLE_GROUP + ".group.test", "user2_grouptest@" + SIMPLE_GROUP + ".group.test", "user3_grouptest@" + COMPOUND_GROUP + ".group.test"};
    protected static final String[] TEST_DISPLAY_NAMES_FOR_GROUP = {"user1 grouptest", "user2 grouptest", "user3 grouptest"};
    protected static final String WIKITEST_NAME = "GroupSecurityApiTest";
    protected static final String GROUP_SECURITY_API_FILE = "groupSecurityTest.html";

    @Override
    protected String getProjectName()
    {
        return "Group Verify Test Project";
    }

    protected String getProject2Name()
    {
        return getProjectName() + "2";
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteGroup(COMPOUND_GROUP, afterTest);
        deleteGroup(SIMPLE_GROUP, afterTest);
        deleteGroup(BAD_GROUP, afterTest);
        deleteGroup(CHILD_GROUP, afterTest);
        deleteUsers(afterTest, TEST_USERS_FOR_GROUP);
        deleteProject(getProjectName(), afterTest);
        deleteProject(getProject2Name(), afterTest);
    }

    @LogMethod protected void init()
    {
        for(String group : TEST_USERS_FOR_GROUP)
        {
            createUser(group, null);
        }

        _containerHelper.createProject(getProjectName(), "Collaboration");
    }

    @Test
    public void testSteps()
    {
        init();

        //double check that user can't see the project yet- otherwise our later check will be invalid

        impersonate(TEST_USERS_FOR_GROUP[0]);
        hoverProjectBar();
        assertElementNotPresent(Locator.linkWithText(getProjectName()));
        stopImpersonating();
        //create users

        createGlobalPermissionsGroup(SIMPLE_GROUP, TEST_USERS_FOR_GROUP[0], TEST_USERS_FOR_GROUP[1]);
        createGlobalPermissionsGroup(COMPOUND_GROUP, SIMPLE_GROUP,  TEST_USERS_FOR_GROUP[2]);

        verifyExportFunction();

        verifyRedundantUserWarnings();

        //add read permissions to group2
        goToHome();
        clickProject(getProjectName());
        enterPermissionsUI();
        waitForText("Author");

        _securityHelper.setSiteGroupPermissions(COMPOUND_GROUP, "Author");
        _securityHelper.setSiteGroupPermissions(COMPOUND_GROUP, "Reader");
        _securityHelper.setSiteGroupPermissions(SIMPLE_GROUP, "Editor");
        clickButton("Save and Finish");
        assertUserCanSeeProject(TEST_USERS_FOR_GROUP[0], getProjectName());
        //can't add built in group to regular group
        log("Verify you can copy perms even with a default");

        //give a system group permissions, so that we can verify copying them doesn't cause a problem
        clickProject(getProjectName());
        enterPermissionsUI();
        waitForText("Author");
        _securityHelper.setSiteGroupPermissions("All Site Users", "Author");

        permissionsReportTest();

        goToProjectHome();

        createProjectCopyPerms();

        verifyImpersonate();

        verifyCantAddSystemGroupToUserGroup();

        groupSecurityApiTest();
    }

    @LogMethod
    private void permissionsReportTest()
    {
        clickAndWait(Locator.linkWithText("view permissions report"));
        DataRegionTable drt = new DataRegionTable("access", this, false, false);

        waitForText("Access Modification History For This Folder");
        assertTextPresent( "Folder Access Details");

        //this table isn't quite a real Labkey Table Region, so we can't use column names
        int userColumn = 1;
        int accessColumn = 2;

        int rowIndex = drt.getRow(userColumn, displayNameFromEmail(TEST_USERS_FOR_GROUP[0]));
        List<String> expectedGroups = Arrays.asList("Author", "Reader", "Editor");
        List<String> groupsForUser = Arrays.asList(drt.getDataAsText(rowIndex, accessColumn).replace(" ", "").split(","));

        //confirm correct perms
        assertEquals("Unexpected groups", new HashSet<>(expectedGroups), new HashSet<>(groupsForUser));


        //exapnd plus  to check specific groups
        click(Locator.imageWithSrc("/labkey/_images/plus.gif", true).index(rowIndex));
//        assertTrue(StringHelper.stringArraysAreEquivalentTrimmed(("Reader, Author RoleGroup(s) ReaderSite: " + GROUP2 + "AuthorSite: " + GROUP2 + ", Site: Users").split(" "),
//                drt.getDataAsText(rowIndex, accessColumn).split(" "))); //TODO: Fix

        //confirm hover over produces list of groups
//        Locator groupSpecification = Locator.tagContainingText("span", "Site: " + COMPOUND_GROUP);
//        String groupHierarchy = getAttribute(groupSpecification, "ext:qtip");
//        String[] expectedMessagesInHierarchy = new String[] {
//                displayNameFromEmail(TEST_USERS_FOR_GROUP[0]) + " is a member of <strong>" + SIMPLE_GROUP + "</strong>",
//                "Which is a member of <strong>" + COMPOUND_GROUP + "</strong><BR/>",
//                "Which is assigned the Author role",
//                displayNameFromEmail(TEST_USERS_FOR_GROUP[0]) + " is a member of <strong>" + COMPOUND_GROUP + "</strong>",
//                "Which is assigned the Author role"};
//        for (String msg : expectedMessagesInHierarchy)
//        {
//                assertTrue("Expected group hover over: " + msg, groupHierarchy.contains(msg));
//        }

        //confirm details link leads to right user, page
        clickAndWait(Locator.linkContainingText("details", rowIndex));
        assertTextPresent(TEST_USERS_FOR_GROUP[0]);
        assertTrue("details link for user did not lead to folder access page", getURL().getFile().contains("folderAccess.view"));
        goBack();

        //confirm username link leads to right user, page
        clickAndWait(Locator.linkWithText(displayNameFromEmail(TEST_USERS_FOR_GROUP[0])));
        assertTextPresent("User Access Details: "  + TEST_USERS_FOR_GROUP[0]);
        goBack();

    }

    @LogMethod
    private void verifyImpersonate()
    {
        //set simple group as editor
        _securityHelper.setSiteGroupPermissions(SIMPLE_GROUP, "Editor");

        //impersonate user 1, make several wiki edits
        impersonate(TEST_USERS_FOR_GROUP[0]);
        clickProject(getProjectName());
        String[][] nameTitleBody = {{"Name1", "Title1", "Body1"}, {"Name2", "Title2", "Body2"}};

        for(String[] wikiValues : nameTitleBody)
        {
            createNewWikiPage();
            setWikiValuesAndSave(wikiValues[0], wikiValues[1], wikiValues[2]);
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

//        Locator unavailableEditorChoice = Locator.xpath("//li[contains(@class, 'x-item-disabled')]/a/span[text()='Editor']");
        impersonateRoles("Author");
//        assertElementNotPresent(unavailableEditorChoice);
        verifyAuthorPermission(nameTitleBody);
        stopImpersonatingRole();

        impersonateRoles("Editor");
//        clickUserMenuItem(false, true, "Impersonate", "Role", "Editor");
//        waitForElement(unavailableEditorChoice);
//        assertElementPresent(Locator.xpath("//li[contains(@class, 'x-item-disabled')]/a/span[text()='Author']"));
        verifyEditorPermission(nameTitleBody);
        stopImpersonatingRole();

        //Issue 13802: add child group to SIMPLE_GROUP, child group should also have access to pages
        createGlobalPermissionsGroup(CHILD_GROUP, "");
        addUserToSiteGroup(CHILD_GROUP, SIMPLE_GROUP);
        clickProject(getProjectName());
        impersonateGroup(CHILD_GROUP, true);
        verifyEditorPermission(nameTitleBody);
        stopImpersonatingGroup();
    }

    private void verifyAuthorPermission(String[][] nameTitleBody)
    {
        clickProject(getProjectName());
        assertTrue("could not see wiki pages when impersonating " + SIMPLE_GROUP,canSeePages(nameTitleBody));
        assertFalse("Was able to edit wiki page when impersonating group without privileges", canEditPages(nameTitleBody));
        sleep(500);
    }

    private void verifyEditorPermission(String[][] nameTitleBody)
    {
        clickProject(getProjectName());
        assertTrue("could not see wiki pages when impersonating " + SIMPLE_GROUP, canSeePages(nameTitleBody));
        assertTrue("could not edit wiki pages when impersonating " + SIMPLE_GROUP, canEditPages(nameTitleBody));
        sleep(500);
    }

    private boolean canEditPages(String[][] nameTitleBody)
    {
        for(String[] wikiValues : nameTitleBody)
        {
            waitAndClick(WAIT_FOR_JAVASCRIPT, Locator.linkWithText(wikiValues[1]), WAIT_FOR_PAGE);
            waitForText(wikiValues[2]);
            if(!isElementPresent(Locator.linkWithText("Edit")))
                return false;
            goBack();
        }
        return true;
    }

    private boolean canSeePages(String[][] nameTitleBody)
    {
        for(String[] wikiValues : nameTitleBody)
        {
            if(!isElementPresent(Locator.linkWithText(wikiValues[1])))
                return false;
        }
        return true;
    }

    //should be at manage group page of COMPOUND_GROUP already
    //verify attempting add a user and a group containing that user to another group results in a warning
    @LogMethod private void verifyRedundantUserWarnings()
    {
        setFormElement(Locator.name("names"), TEST_USERS_FOR_GROUP[0]); //this user is in group1 and so is already in group 2
        clickButton("Update Group Membership");
        assertTextPresent("* These group members already appear in other included member groups and can be safely removed.");
        assertTrue("Missing or badly formatted group redundancy warning", getBodyText().contains(TEST_DISPLAY_NAMES_FOR_GROUP[0] + ") *"));
//        expect warning
    }

    @LogMethod private void verifyExportFunction()
    {
        selectGroup(COMPOUND_GROUP, true);
        clickAndWait(Locator.linkWithText("manage group"));
        waitForElement(Locator.name("names"));
        //Selenium can't handle file exports, so there's nothing to be done here.
        assertElementPresent(getButtonLocatorContainingText("Export All to Excel"));

    }

    @LogMethod
    private void verifyCantAddSystemGroupToUserGroup()
    {
        startCreateGlobalPermissionsGroup(BAD_GROUP, true);
        _ext4Helper.selectComboBoxItem(Locator.xpath(_extHelper.getExtDialogXPath(BAD_GROUP + " Information") + "//table[contains(@id, 'labkey-principalcombo')]"), "Site: All Site Users");

        waitForText("Can't add a system group to another group");
        clickButton("OK", 0);
        clickButton("Done", 0);
        clickButton("Save and Finish");
    }

    @LogMethod
    protected void createProjectCopyPerms()
    {
        String projectName = getProject2Name();

        ensureAdminMode();
        log("Creating project with name " + projectName);
        goToCreateProject();
        waitForElement(Locator.name("name"));
        setFormElement(Locator.name("name"), projectName);

        click(Locator.xpath("//td[./label[text()='Custom']]/input"));

        waitAndClickButton("Next");

        //second page of the wizard
        waitAndClick(Locator.xpath("//td[./label[text()='Copy From Existing Project']]/input"));
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            { // Workaround: erratic combo-box behavior
                try{_ext4Helper.selectComboBoxItem(Locator.xpath("//table[@id='targetProject']"), getProjectName());}
                catch (NoSuchElementException recheck) {return false;}

                if (!getFormElement(Locator.css("#targetProject input")).equals(getProjectName()))
                {
                    click(Locator.xpath("//table[@id='targetProject']"));
                    return false;
                }
                else
                {
                    return true;
                }
            }
        }, "Failed to select project", WAIT_FOR_JAVASCRIPT);

        waitAndClickButton("Next");

        //third page of wizard
        waitAndClickButton("Finish");

        assertUserCanSeeProject(TEST_USERS_FOR_GROUP[1], getProject2Name());

        _containerHelper.addCreatedProject(projectName);
    }


    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    protected void assertUserCanSeeProject(String user, String project)
    {
        impersonate(user);
        hoverProjectBar();
        assertElementPresent(Locator.linkWithText(project));
        stopImpersonating();
    }

    @LogMethod
    protected void groupSecurityApiTest()
    {
        // Initialize the Wiki
        clickProject(getProjectName());
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Wiki");

        createNewWikiPage();
        setFormElement(Locator.name("name"), WIKITEST_NAME);
        setFormElement(Locator.name("title"), WIKITEST_NAME);
        setWikiBody("Placeholder text.");
        saveWikiPage();

        setSourceFromFile(GROUP_SECURITY_API_FILE, WIKITEST_NAME);

        // Run the Test Script
        clickButton("Start Test", 0);
        waitForText("Done!", defaultWaitForPage);
        assertFalse("Security API error.", Locator.id("log-info").findElement(getDriver()).getText().contains("Error"));
    }

    @Override protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
