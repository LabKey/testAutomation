/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category({DailyA.class})
public class UserPermissionsTest extends BaseWebDriverTest
{
    PortalHelper portalHelper = new PortalHelper(this);
    protected static final String PERM_PROJECT_NAME = "PermissionCheckProject";
    protected static final String DENIED_SUB_FOLDER_NAME = "UnlinkedFolder";
    protected static final String GAMMA_SUB_FOLDER_NAME = "GammaFolder";
    protected static final String GAMMA_EDITOR_GROUP_NAME = "GammaEditor";
    protected static final String GAMMA_AUTHOR_GROUP_NAME = "GammaAuthor";
    protected static final String GAMMA_READER_GROUP_NAME = "GammaReader";
//    protected static final String GAMMA_RESTRICTED_READER_GROUP_NAME = "GammaRestrictedReader";
    protected static final String GAMMA_SUBMITTER_GROUP_NAME = "GammaSubmitter";
    protected static final String GAMMA_ADMIN_GROUP_NAME = "GammaAdmin";
    //permissions
    //editor, author, reader, restricted_reader, submitter, Admin
    protected static final String GAMMA_EDITOR_USER = "gammaeditor@security.test";
    protected static final String GAMMA_EDITOR_PAGE_TITLE = "This is a Test Message from : " + GAMMA_EDITOR_USER;
    protected static final String GAMMA_AUTHOR_USER = "gammaauthor@security.test";
    protected static final String GAMMA_AUTHOR_PAGE_TITLE = "This is a Test Message from : " + GAMMA_AUTHOR_USER;
    protected static final String GAMMA_READER_USER = "gammareader@security.test";
    protected static final String GAMMA_PROJECT_ADMIN_USER = "gammaadmin@security.test";

    //I can't really find any docs on what this is exactly?
//    protected static final String GAMMA_RESTRICTED_READER_USER = "gammarestricted@security.test";
//    protected static final String GAMMA_SUBMITTER_USER = "gammasubmitter@security.test";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    @Override
    protected String getProjectName()
    {
        return PERM_PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        log(this.getClass().getName() + " Cleaning Up");
        _containerHelper.deleteProject(PERM_PROJECT_NAME, afterTest);

        deleteUsersIfPresent(GAMMA_EDITOR_USER, GAMMA_AUTHOR_USER, GAMMA_READER_USER, GAMMA_PROJECT_ADMIN_USER);
    }

    @Test
    public void testSteps()
    {
        enableEmailRecorder();
        userPermissionRightsTest();
    }

    /**
     * Create some projects, create some groups, permissions for those groups
     * Create some users, assign to groups and validate the permissions by
     * impersonating the user.
     */
    @LogMethod
    private void userPermissionRightsTest()
    {
        _containerHelper.createProject(PERM_PROJECT_NAME, null);
        _permissionsHelper.createPermissionsGroup(GAMMA_EDITOR_GROUP_NAME);
        _permissionsHelper.assertPermissionSetting(GAMMA_EDITOR_GROUP_NAME, "No Permissions");
        _permissionsHelper.setPermissions(GAMMA_EDITOR_GROUP_NAME, "Editor");
        createUserInProjectForGroup(GAMMA_EDITOR_USER, PERM_PROJECT_NAME, GAMMA_EDITOR_GROUP_NAME, false);

        _containerHelper.createSubfolder(PERM_PROJECT_NAME, PERM_PROJECT_NAME, DENIED_SUB_FOLDER_NAME, "None", new String[] {"Messages", "Wiki"}, true);
        _containerHelper.createSubfolder(PERM_PROJECT_NAME, DENIED_SUB_FOLDER_NAME, GAMMA_SUB_FOLDER_NAME, "None", new String[] {"Messages", "Wiki"}, true);
        portalHelper.addWebPart("Messages");
        assertElementPresent(Locator.linkWithText("Messages"));
        portalHelper.addWebPart("Wiki");
        assertTextPresent("Wiki");
        assertElementPresent(Locator.linkWithText("Create a new wiki page"));
        portalHelper.addWebPart("Wiki Table of Contents");

        //Create Reader User
        clickProject(PERM_PROJECT_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.createPermissionsGroup(GAMMA_READER_GROUP_NAME);
        _permissionsHelper.assertPermissionSetting(GAMMA_READER_GROUP_NAME, "No Permissions");
        _permissionsHelper.setPermissions(GAMMA_READER_GROUP_NAME, "Reader");
        createUserInProjectForGroup(GAMMA_READER_USER, PERM_PROJECT_NAME, GAMMA_READER_GROUP_NAME, false);
        //Create Author User
        clickProject(PERM_PROJECT_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.createPermissionsGroup(GAMMA_AUTHOR_GROUP_NAME);
        _permissionsHelper.assertPermissionSetting(GAMMA_AUTHOR_GROUP_NAME, "No Permissions");
        _permissionsHelper.setPermissions(GAMMA_AUTHOR_GROUP_NAME, "Author");
        createUserInProjectForGroup(GAMMA_AUTHOR_USER, PERM_PROJECT_NAME, GAMMA_AUTHOR_GROUP_NAME, false);
        //Create the Submitter User
        clickProject(PERM_PROJECT_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.createPermissionsGroup(GAMMA_SUBMITTER_GROUP_NAME);
        _permissionsHelper.assertPermissionSetting(GAMMA_SUBMITTER_GROUP_NAME, "No Permissions");
        _permissionsHelper.setPermissions(GAMMA_SUBMITTER_GROUP_NAME, "Submitter");
        // TODO: Add submitter to a group
        /*
         * I need a way to test submitter, I can't even view a folder where submitter has permissions when
         * impersonating on my local labkey, so may require special page?
         */

        //Make sure the Editor can edit
        impersonate(GAMMA_EDITOR_USER);
        navigateToFolder(PERM_PROJECT_NAME, GAMMA_SUB_FOLDER_NAME);
        portalHelper.clickWebpartMenuItem("Messages", true, "Email", "Preferences");
        checkCheckbox(Locator.radioButtonByNameAndValue("emailPreference", "0"));
        clickButton("Update");
        clickFolder(GAMMA_SUB_FOLDER_NAME);

        portalHelper.clickWebpartMenuItem("Messages", true, "New");
        setFormElement(Locator.name("title"), GAMMA_EDITOR_PAGE_TITLE);
        setFormElement(Locator.id("body"), "This is a secret message that was generated by " + GAMMA_EDITOR_USER);
        selectOptionByValue(Locator.name("rendererType"), "RADEOX");
        clickButton("Submit");
        stopImpersonating();

        //Make sure that the Author can read as well, edit his own but not edit the Edtiors
        impersonate(GAMMA_AUTHOR_USER);
        navigateToFolder(PERM_PROJECT_NAME, GAMMA_SUB_FOLDER_NAME);
        portalHelper.clickWebpartMenuItem("Messages", true, "Email", "Preferences");
        checkCheckbox(Locator.radioButtonByNameAndValue("emailPreference", "0"));
        clickButton("Update");
        clickFolder(GAMMA_SUB_FOLDER_NAME);

        portalHelper.clickWebpartMenuItem("Messages", true, "New");
        setFormElement(Locator.name("title"), GAMMA_AUTHOR_PAGE_TITLE);
        setFormElement(Locator.id("body"), "This is a secret message that was generated by " + GAMMA_AUTHOR_USER);
        selectOptionByValue(Locator.name("rendererType"), "RADEOX");
        clickButton("Submit");
        //Can't edit the editor's
        clickFolder(GAMMA_SUB_FOLDER_NAME);
        clickAndWait(Locator.linkContainingText("view message").index(1));
        assertTextPresent(GAMMA_EDITOR_PAGE_TITLE);
        assertElementNotPresent(Locator.linkWithText("Edit"));
        stopImpersonating();

        //Make sure that the Reader can read but not edit
        impersonate(GAMMA_READER_USER);
        navigateToFolder(PERM_PROJECT_NAME, GAMMA_SUB_FOLDER_NAME);

        clickAndWait(Locator.linkContainingText("view message").index(0));
        assertTextPresent(GAMMA_AUTHOR_PAGE_TITLE);
        assertElementNotPresent(Locator.linkWithText("Edit"));

        clickFolder(GAMMA_SUB_FOLDER_NAME);
        clickAndWait(Locator.linkContainingText("view message").index(1));
        assertTextPresent(GAMMA_EDITOR_PAGE_TITLE);
        assertElementNotPresent(Locator.linkWithText("Edit"));
        stopImpersonating();

        //switch back to Editor and edit
        impersonate(GAMMA_EDITOR_USER);
        navigateToFolder(PERM_PROJECT_NAME, GAMMA_SUB_FOLDER_NAME);
        //Go back and Edit
        clickAndWait(Locator.linkContainingText("view message").index(1));
        assertTextPresent(GAMMA_EDITOR_PAGE_TITLE);
        clickAndWait(Locator.linkWithText("edit"));
        setFormElement(Locator.id("body"), "This is a secret message that was generated by " + GAMMA_EDITOR_USER + "\nAnd I have edited it");

        //Remove permission from folder to verify unviewability
        log("Check for disallowed folder links");
        stopImpersonating();
        navigateToFolder(PERM_PROJECT_NAME, GAMMA_SUB_FOLDER_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.uncheckInheritedPermissions();
        clickButton("Save and Finish", defaultWaitForPage);
        clickFolder(DENIED_SUB_FOLDER_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.uncheckInheritedPermissions();
        _permissionsHelper.removePermission(GAMMA_READER_GROUP_NAME, "Reader");
        clickButton("Save and Finish");

        // Test that a project admin is confined to a single project when impersonating a project user. Site admins
        // are not restricted in this way, so we need to create and login as a new user with project admin permissions.
        clickProject(PERM_PROJECT_NAME);
        _permissionsHelper.createPermissionsGroup(GAMMA_ADMIN_GROUP_NAME);
        _permissionsHelper.setPermissions(GAMMA_ADMIN_GROUP_NAME, "Project Administrator");
        createUserInProjectForGroup(GAMMA_PROJECT_ADMIN_USER, PERM_PROJECT_NAME, GAMMA_ADMIN_GROUP_NAME, true);
        clickLinkWithTextNoTarget("here");
        clickAndWait(Locator.linkContainingText("setPassword.view"));
        setFormElement(Locator.id("password"), PasswordUtil.getPassword());
        setFormElement(Locator.id("password2"), PasswordUtil.getPassword());
        clickButton("Set Password");
        signOut();
        signIn(GAMMA_PROJECT_ADMIN_USER, PasswordUtil.getPassword());
        clickProject(PERM_PROJECT_NAME);
        impersonate(GAMMA_READER_USER);
        WebElement projectTree = projectMenu().expandProjectFully(PERM_PROJECT_NAME);
        assertNotNull("No link to subfolder: /" + PERM_PROJECT_NAME + "/" + GAMMA_SUB_FOLDER_NAME, Locator.linkWithText(GAMMA_SUB_FOLDER_NAME).findElementOrNull(projectTree));
        assertNotNull("Link found to inaccessable subfolder: /" + PERM_PROJECT_NAME + "/" + GAMMA_SUB_FOLDER_NAME, Locator.linkWithText(GAMMA_SUB_FOLDER_NAME).findElementOrNull(projectTree)); // it will appear as a span, no link
        // Ensure only one project visible during project impersonation. Regression test 13346
        assertEquals("Only one project should be visible while impersonating", Arrays.asList(PERM_PROJECT_NAME), getTexts(projectMenu().projectMenuLinks()));

        //Reset ourselves to the global user so we can do cleanup
        stopImpersonating();
        signOut();
        signIn();
    }

    private void clickLinkWithTextNoTarget(String text)
    {
        String href = getAttribute(Locator.linkWithText(text), "href");
        beginAt(href);
    }

    /**
     * Create a User in a Project for a Specific group
     */
    @LogMethod
    private void createUserInProjectForGroup(String userName, String projectName, String groupName, boolean sendEmail)
    {
        if (isElementPresent(Locator.permissionRendered()))
        {
            _permissionsHelper.exitPermissionsUI();
            clickProject(projectName);
        }
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup(groupName);
        setFormElement(Locator.name("names"), userName);
        if (!sendEmail)
            uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
    }
}
