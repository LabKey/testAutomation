/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.html.Table;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.openqa.selenium.NoSuchElementException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(BVT.class)
public class GroupTest extends BaseWebDriverTest
{
    protected static final String SIMPLE_GROUP = "group1";
    protected static final String COMPOUND_GROUP = "group2";
    protected static final String BAD_GROUP = "group3";
    protected static final String CHILD_GROUP = "group4";
    protected static final String[] TEST_USERS_FOR_GROUP = {"user1_grouptest@" + SIMPLE_GROUP + ".group.test", "user2_grouptest@" + SIMPLE_GROUP + ".group.test", "user3_grouptest@" + COMPOUND_GROUP + ".group.test"};
    protected static final String[] TEST_DISPLAY_NAMES_FOR_GROUP = {"user1 grouptest", "user2 grouptest", "user3 grouptest"};
    protected static final String SITE_USER_IN_GROUP = "useringroup";
    protected static final String SITE_USER_NOT_IN_GROUP = "usernotingroup";
    protected static final String SITE_USER_GROUP = "SiteUsersGroup";
    protected static final String[] SITE_USER_EMAILS = {SITE_USER_IN_GROUP + "@group.test", SITE_USER_NOT_IN_GROUP + "@group.test"};
    protected static final String API_SITE_GROUP = "API Site Group";
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
        _permissionsHelper.deleteGroup(COMPOUND_GROUP);
        _permissionsHelper.deleteGroup(SIMPLE_GROUP);
        _permissionsHelper.deleteGroup(BAD_GROUP);
        _permissionsHelper.deleteGroup(CHILD_GROUP);
        _permissionsHelper.deleteGroup(SITE_USER_GROUP);
        _permissionsHelper.deleteGroup(API_SITE_GROUP);
        _userHelper.deleteUsers(false, TEST_USERS_FOR_GROUP);
        _userHelper.deleteUsers(false, SITE_USER_EMAILS);
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(getProject2Name(), afterTest);
    }

    @BeforeClass
    public static void setup()
    {
        GroupTest init = new GroupTest();
        init._containerHelper.createProject(init.getProjectName(), "Collaboration");
    }

    @Test
    public void testSteps()
    {
        for (String user : TEST_USERS_FOR_GROUP)
        {
            _userHelper.createUser(user);
        }

        //double check that user can't see the project yet- otherwise our later check will be invalid

        impersonate(TEST_USERS_FOR_GROUP[0]);
        openProjectMenu();
        assertElementNotPresent(Locator.linkWithText(getProjectName()));
        stopImpersonating();
        //create users

        _permissionsHelper.createGlobalPermissionsGroup(SIMPLE_GROUP, TEST_USERS_FOR_GROUP[0], TEST_USERS_FOR_GROUP[1]);
        _permissionsHelper.createGlobalPermissionsGroup(COMPOUND_GROUP, SIMPLE_GROUP,  TEST_USERS_FOR_GROUP[2]);

        verifyExportFunction();

        verifyRedundantUserWarnings();

        //add read permissions to group2
        goToHome();
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
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
        _permissionsHelper.enterPermissionsUI();
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
        Table access = new Table(this, DataRegionTable.Locators.table("access").findElement(getDriver()));

        waitForText("Access Modification History For This Folder");
        assertTextPresent("Folder Access Details");

        int userColumn = 2;
        int accessColumn = 3;
        int headerIndex = 2;

        String displayName = _userHelper.getDisplayNameForEmail(TEST_USERS_FOR_GROUP[0]);
        int rowIndex = access.getRowIndex(userColumn, displayName) - headerIndex;
        assertTrue("Unable to find user: " + displayName, rowIndex >= 0);
        List<String> expectedGroups = Arrays.asList("Author", "Reader", "Editor");
        List<String> groupsForUser = Arrays.asList(access.getDataAsText(rowIndex + headerIndex, accessColumn).replace(" ", "").split(","));

        //confirm correct perms
        assertEquals("Unexpected groups", new HashSet<>(expectedGroups), new HashSet<>(groupsForUser));

        //expand plus to check specific groups
        click(Locator.tag("img").withAttributeContaining("src", "/labkey/_images/plus.gif").index(rowIndex));

        //confirm details link leads to right user, page
        clickAndWait(Locator.linkContainingText("details").index(rowIndex - 1)); // TODO: Table component uses one-based indexing
        assertTextPresent(TEST_USERS_FOR_GROUP[0]);
        assertTrue("details link for user did not lead to folder access page", getURL().getFile().contains("folderAccess.view"));
        goBack();

        //confirm username link leads to right user, page
        clickAndWait(Locator.linkWithText(displayName));
        assertTextPresent("User Access Details: "  + TEST_USERS_FOR_GROUP[0]);
        goBack();
    }

    @LogMethod
    private void verifyImpersonate()
    {
        WikiHelper wikiHelper = new WikiHelper(this);

        //set simple group as editor
        _securityHelper.setSiteGroupPermissions(SIMPLE_GROUP, "Editor");

        //impersonate user 1, make several wiki edits
        impersonate(TEST_USERS_FOR_GROUP[0]);
        clickProject(getProjectName());
        String[][] nameTitleBody = {{"Name1", "Title1", "Body1"}, {"Name2", "Title2", "Body2"}};

        for (String[] wikiValues : nameTitleBody)
        {
            wikiHelper.createNewWikiPage();
            wikiHelper.setWikiValuesAndSave(wikiValues[0], wikiValues[1], wikiValues[2]);
        }
        stopImpersonating();

        //impersonate simple group, they should have full editor permissions
        impersonateGroup(SIMPLE_GROUP, true);
        verifyEditorPermission(nameTitleBody);
        stopImpersonating();

        //impersonate compound group, should only have author permissions
        impersonateGroup(COMPOUND_GROUP, true);
        verifyAuthorPermission(nameTitleBody);
        stopImpersonating();

        impersonateRoles("Author");
        verifyAuthorPermission(nameTitleBody);
        stopImpersonating();

        impersonateRoles("Editor");
        verifyEditorPermission(nameTitleBody);
        stopImpersonating();

        //Issue 13802: add child group to SIMPLE_GROUP, child group should also have access to pages
        _permissionsHelper.createGlobalPermissionsGroup(CHILD_GROUP, "");
        _permissionsHelper.addUserToSiteGroup(CHILD_GROUP, SIMPLE_GROUP);
        clickProject(getProjectName());
        impersonateGroup(CHILD_GROUP, true);
        verifyEditorPermission(nameTitleBody);
        stopImpersonating();
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
        for (String[] wikiValues : nameTitleBody)
        {
            waitAndClick(WAIT_FOR_JAVASCRIPT, Locator.linkWithText(wikiValues[1]), WAIT_FOR_PAGE);
            waitForText(wikiValues[2]);
            if (!isElementPresent(Locator.linkWithText("Edit")))
                return false;
            goBack();
        }
        return true;
    }

    private boolean canSeePages(String[][] nameTitleBody)
    {
        for (String[] wikiValues : nameTitleBody)
        {
            if (!isElementPresent(Locator.linkWithText(wikiValues[1])))
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
        _permissionsHelper.selectGroup(COMPOUND_GROUP, true);
        clickAndWait(Locator.linkWithText("manage group"));
        waitForElement(Locator.name("names"));
        //Selenium can't handle file exports, so there's nothing to be done here.
        assertElementPresent(Locator.lkButton("Export All to Excel"));

    }

    @LogMethod
    private void verifyCantAddSystemGroupToUserGroup()
    {
        _permissionsHelper.startCreateGlobalPermissionsGroup(BAD_GROUP, true);
        _ext4Helper.selectComboBoxItem(Ext4Helper.Locators.window(BAD_GROUP + " Information").append("//table[contains(@id, 'labkey-principalcombo')]"), "Site: All Site Users");
        assertExt4MsgBox("Can't add a system group to another group", "OK");
        clickButton("Done", 0);
        clickButton("Save and Finish");
    }

    @LogMethod
    protected void createProjectCopyPerms()
    {
        String projectName = getProject2Name();

        ensureAdminMode();
        navBar()
                .goToCreateProjectPage()
                .setProjectName(projectName)
                .setFolderType("Custom")
                .clickNext()
                .setCopyFromExistingProject(getProjectName())
                .clickNext()
                .clickFinish();

        assertUserCanSeeProject(TEST_USERS_FOR_GROUP[1], getProject2Name());
        _containerHelper.addCreatedProject(projectName);
    }


    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    protected void assertUserCanSeeProject(String user, String project)
    {
        impersonate(user);
        assertTrue(projectMenu().projectLinkExists(project));
        stopImpersonating();
    }

    @LogMethod
    protected void groupSecurityApiTest()
    {
        WikiHelper wikiHelper = new WikiHelper(this);
        PortalHelper portalHelper = new PortalHelper(this);

        // Initialize the Wiki
        clickProject(getProjectName());
        portalHelper.addWebPart("Wiki");

        wikiHelper.createNewWikiPage();
        setFormElement(Locator.name("name"), WIKITEST_NAME);
        setFormElement(Locator.name("title"), WIKITEST_NAME);
        wikiHelper.setWikiBody("Placeholder text.");
        wikiHelper.saveWikiPage();

        wikiHelper.setSourceFromFile(GROUP_SECURITY_API_FILE, WIKITEST_NAME);

        // Run the Test Script
        click(Locator.button("Start Test"));
        waitForElement(Locator.tagWithText("span", "Done!"), defaultWaitForPage);
        assertFalse("Security API error.", Locator.id("log-info").findElement(getDriver()).getText().contains("Error"));
    }

    @Test
    public void testSiteUserGroupFilters()
    {
        APIUserHelper apiUserHelper = new APIUserHelper(this);
        for (String user : SITE_USER_EMAILS)
        {
            apiUserHelper.createUser(user);
        }
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.createGlobalPermissionsGroup(SITE_USER_GROUP, SITE_USER_EMAILS[0]);

        goToSiteUsers();
        DataRegionTable table = new DataRegionTable("Users", getDriver());

        int initialRowCount = table.getDataRowCount();

        /* TODO: Unable to multi-value display column through Customize Grid (See Issue 26100)
        CustomizeView helper = table.getCustomizeView();
        helper.openCustomizeViewPanel();
        helper.addFilter("Groups", "Is Not Blank");
        helper.applyCustomView();

        Assert.assertNotEquals("Filtered number of users should not be the same as the initial count", initialRowCount, table.getDataRowCount());
        Assert.assertEquals("User not in a group should not be in filtered list", -1, table.getRowIndex("Display Name", SITE_USER_NOT_IN_GROUP));
        Assert.assertNotEquals("User in group should be in filtered list", -1, table.getRowIndex("Display Name", SITE_USER_IN_GROUP));

        // now try the same filtering with a column filter
        helper.revertUnsavedViewGridClosed();
        Assert.assertEquals("After removing filters, should have the original number of users", initialRowCount, table.getDataRowCount());
        */

        table.setFilter("Groups", "Is Not Blank");
        Assert.assertNotEquals("Filtered number of users should not be the same as the initial count", initialRowCount, table.getDataRowCount());
        Assert.assertEquals("User not in a group should not be in filtered list", -1, table.getRowIndex("Display Name", SITE_USER_NOT_IN_GROUP));
        Assert.assertNotEquals("User in group should be in filtered list", -1, table.getRowIndex("Display Name", SITE_USER_IN_GROUP));
    }

    @Override protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
