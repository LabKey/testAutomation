/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Griffith
 * Date: Jan 18, 2006
 * To change this template use File | Settings | File Templates.
 */
public class UserPermissionsTest extends BaseSeleniumWebTest
{
    protected static final String PERM_PROJECT_NAME = "PermissionCheckProject";
    protected static final String GAMMA_SUB_FOLDER_NAME = "GammaFolder";
    protected static final String GAMMA_EDITOR_GROUP_NAME = "GammaEditor";
    protected static final String GAMMA_AUTHOR_GROUP_NAME = "GammaAuthor";
    protected static final String GAMMA_READER_GROUP_NAME = "GammaReader";
    protected static final String GAMMA_RESTRICTED_READER_GROUP_NAME = "GammaRestrictedReader";
    protected static final String GAMMA_SUBMITTER_GROUP_NAME = "GammaSubmitter";
    protected static final String GAMMA_ADMIN_GROUP_NAME = "GammaAdmin";
    //permissions
    //editor, author, reader, restricted_reader, submitter, Admin
    protected static final String GAMMA_EDITOR_USER = "gammaeditor@security.text";
    protected static final String GAMMA_EDITOR_PAGE_TITLE = "This is a Test Message from : " + GAMMA_EDITOR_USER;
    protected static final String GAMMA_AUTHOR_USER = "gammauthor@security.test";
    protected static final String GAMMA_AUTHOR_PAGE_TITLE = "This is a Test Message from : " + GAMMA_AUTHOR_USER;
    protected static final String GAMMA_READER_USER = "gammareader@security.test";
    //I can't really find any docs on what this is exactly?
    protected static final String GAMMA_RESTRICTED_READER_USER = "gammarestricted@security.test";
    protected static final String GAMMA_SUBMITTER_USER = "gammasubmitter@security.test";
    protected static final String GAMMA_ADMIN_USER = "gammaadmin@security.test";

    public String getAssociatedModuleDirectory()
    {
        return "core";
    }

    protected void doCleanup()
    {
        log(this.getClass().getName() + " Cleaning UP");
        if (isLinkPresentContainingText(PERM_PROJECT_NAME))
        {
            try {deleteProject(PERM_PROJECT_NAME); } catch (Throwable t) { t.printStackTrace();}
        }

        deleteUser(GAMMA_EDITOR_USER);
        deleteUser(GAMMA_AUTHOR_USER);
        deleteUser(GAMMA_READER_USER);
        deleteUser(GAMMA_SUBMITTER_USER);
    }

    protected void doTestSteps()
    {
        userPermissionRightsTest();
    }

    /**
     * Create some projects, create some groups, permissions for those groups
     * Create some users, assign to groups and validate the permissions by
     * impersonating the user.
     */
    private void userPermissionRightsTest(){
        createProject(PERM_PROJECT_NAME);
        createPermissionsGroup(GAMMA_EDITOR_GROUP_NAME);
        assertPermissionSetting(GAMMA_EDITOR_GROUP_NAME, "No Permissions");
        setPermissions(GAMMA_EDITOR_GROUP_NAME, "Editor");
        createUserInProjectForGroup(GAMMA_EDITOR_USER, PERM_PROJECT_NAME, GAMMA_EDITOR_GROUP_NAME);

        createSubfolder(PERM_PROJECT_NAME, PERM_PROJECT_NAME, GAMMA_SUB_FOLDER_NAME, "None", new String[] {"Messages", "Wiki"}, true);
        addWebPart("Messages");
        assertLinkPresentWithText("Messages");
        addWebPart("Wiki");
        assertTextPresent("Wiki");
        assertLinkPresentWithText("Create a new wiki page");
        addWebPart("Wiki TOC");

        //Create Reader User
        clickLinkWithText(PERM_PROJECT_NAME);
        clickLinkWithText("Permissions");
        createPermissionsGroup(GAMMA_READER_GROUP_NAME);
        assertPermissionSetting(GAMMA_READER_GROUP_NAME, "No Permissions");
        setPermissions(GAMMA_READER_GROUP_NAME, "Reader");
        createUserInProjectForGroup(GAMMA_READER_USER, PERM_PROJECT_NAME, GAMMA_READER_GROUP_NAME);
        //Create Author User
        clickLinkWithText(PERM_PROJECT_NAME);
        clickLinkWithText("Permissions");
        createPermissionsGroup(GAMMA_AUTHOR_GROUP_NAME);
        assertPermissionSetting(GAMMA_AUTHOR_GROUP_NAME, "No Permissions");
        setPermissions(GAMMA_AUTHOR_GROUP_NAME, "Author");
        createUserInProjectForGroup(GAMMA_AUTHOR_USER, PERM_PROJECT_NAME, GAMMA_AUTHOR_GROUP_NAME);
        //Create the Submitter User
        clickLinkWithText(PERM_PROJECT_NAME);
        clickLinkWithText("Permissions");
        createPermissionsGroup(GAMMA_SUBMITTER_GROUP_NAME);
        assertPermissionSetting(GAMMA_SUBMITTER_GROUP_NAME, "No Permissions");
        setPermissions(GAMMA_SUBMITTER_GROUP_NAME, "Submitter");
        createUserInProjectForGroup(GAMMA_AUTHOR_USER, PERM_PROJECT_NAME, GAMMA_AUTHOR_GROUP_NAME);
        /*
         * I need a way to test submitter, I can't even view a folder where submitter has permissions when
         * impersonating on my local labkey, so may require special page?
         */

        //Make sure the Editor can edit
        impersonate(GAMMA_EDITOR_USER);
        clickLinkWithText(PERM_PROJECT_NAME);
        clickLinkWithText(GAMMA_SUB_FOLDER_NAME);
        clickLinkWithText("email preferences");
        checkRadioButton("emailPreference", "0");
        clickNavButton("Update");
        clickLinkWithText(GAMMA_SUB_FOLDER_NAME);

        clickLinkWithText("new message");
        setFormElement("title", GAMMA_EDITOR_PAGE_TITLE);
        setFormElement("body", "This is a secret message that was generated by " + GAMMA_EDITOR_USER);
        selectOptionByValue("rendererType", "RADEOX");
        clickNavButton("Submit");
        stopImpersonating();

        //Make sure that the Author can read as well, edit his own but not edit the Edtiors
        impersonate(GAMMA_AUTHOR_USER);
        clickLinkWithText(PERM_PROJECT_NAME);
        clickLinkWithText(GAMMA_SUB_FOLDER_NAME);
        clickLinkWithText("email preferences");
        checkRadioButton("emailPreference", "0");
        clickNavButton("Update");
        clickLinkWithText(GAMMA_SUB_FOLDER_NAME);

        clickLinkWithText("new message");
        setFormElement("title", GAMMA_AUTHOR_PAGE_TITLE);
        setFormElement("body", "This is a secret message that was generated by " + GAMMA_AUTHOR_USER);
        selectOptionByValue("rendererType", "RADEOX");
        clickNavButton("Submit");
        //Can't edit the editor's
        clickLinkWithText(GAMMA_SUB_FOLDER_NAME);
        clickLinkWithText("view message", 1);
        assertTextPresent(GAMMA_EDITOR_PAGE_TITLE);
        assertLinkNotPresentWithText("Edit");
        stopImpersonating();

        //Make sure that the Reader can read but not edit
        impersonate(GAMMA_READER_USER);
        clickLinkWithText(PERM_PROJECT_NAME);
        clickLinkWithText(GAMMA_SUB_FOLDER_NAME);

        clickLinkWithText("view message", 0);
        assertTextPresent(GAMMA_AUTHOR_PAGE_TITLE);
        assertLinkNotPresentWithText("Edit");

        clickLinkWithText(GAMMA_SUB_FOLDER_NAME);
        clickLinkWithText("view message", 1);
        assertTextPresent(GAMMA_EDITOR_PAGE_TITLE);
        assertLinkNotPresentWithText("Edit");
        stopImpersonating();

        //switch back to Editor and edit
        impersonate(GAMMA_EDITOR_USER);
        clickLinkWithText(PERM_PROJECT_NAME);
        clickLinkWithText(GAMMA_SUB_FOLDER_NAME);
        //Go back and Edit
        clickLinkWithText("view message", 1);
        assertTextPresent(GAMMA_EDITOR_PAGE_TITLE);
        clickLinkWithText("edit");
        setFormElement("body", "This is a secret message that was generated by " + GAMMA_EDITOR_USER + "\nAnd I have edited it");

        //Reset ourselves to the global user so we can do cleanup
        stopImpersonating();
    }

    /**
     * Create a User in a Project for a Specific group
     *
     * @param userName
     * @param projectName
     * @param groupName
     */
    private void createUserInProjectForGroup(String userName, String projectName, String groupName){
        clickLinkWithText(projectName);
        clickLinkWithText("Permissions");
        clickLink("managegroup/" + projectName + "/" + groupName);
        
        setFormElement("names", userName );
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
    }
}
