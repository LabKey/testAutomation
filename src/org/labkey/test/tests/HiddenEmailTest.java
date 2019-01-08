/*
 * Copyright (c) 2012-2018 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.pages.query.ExecuteQueryPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class HiddenEmailTest extends BaseWebDriverTest
{
    private static final String TEST_GROUP = "HiddenEmail Test group";
    private static final String ADMIN_USER = "admin@usertable.test";
    private static final String IMPERSONATED_USER = "impersonated_user@usertable.test";
    private static final String CHECKED_USER = "checked_user@usertable.test";
    private static final String EMAIL_TEST_LIST = "My Users";
    private static final String CUSTOM_USER_COLUMN = "UserTablePermTest";
    private static final String HIDDEN_COL_VIEW = "hiddenColView";
    private static final String HIDDEN_STRING = "HIDDEN_VALUE";

    @Override
    protected String getProjectName()
    {
        return "Hidden Email Test";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        _userHelper.deleteUsers(false, IMPERSONATED_USER, CHECKED_USER, ADMIN_USER);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @BeforeClass
    public static void setup()
    {
        HiddenEmailTest initTest = (HiddenEmailTest) getCurrentTest();
        initTest.doSetup();
    }

    private void doSetup()
    {
        PropertiesEditor userProperties = goToSiteUsers().clickChangeUserProperties();
        if (!userProperties.getFieldNames().contains(CUSTOM_USER_COLUMN))
        {
            userProperties.addField(new FieldDefinition(CUSTOM_USER_COLUMN));
            clickAndWait(Locator.lkButton("Save"));
        }

        _userHelper.createUser(ADMIN_USER, true, true);
        _userHelper.createUser(IMPERSONATED_USER, true, true);
        _userHelper.createUser(CHECKED_USER, true, true);
        setInitialPassword(ADMIN_USER, PasswordUtil.getPassword());
        setInitialPassword(IMPERSONATED_USER, PasswordUtil.getPassword());

        _containerHelper.createProject(getProjectName(), null);

        new ApiPermissionsHelper(this).addUserToSiteGroup(ADMIN_USER, "Site Administrators");
        // Use created user to ensure we have a known 'Modified by' column for created users
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this,
                () -> new Connection(WebTestHelper.getBaseURL(), ADMIN_USER, PasswordUtil.getPassword()));

        apiPermissionsHelper.createPermissionsGroup(TEST_GROUP, IMPERSONATED_USER, CHECKED_USER);
        apiPermissionsHelper.setPermissions(TEST_GROUP, "Reader");

        impersonate(ADMIN_USER);
        {
            goToMyAccount();
            clickButton("Edit");
            setFormElement(Locator.name("quf_Phone"), HIDDEN_STRING);
            setFormElement(Locator.name("quf_" + CUSTOM_USER_COLUMN), HIDDEN_STRING);
            clickButton("Submit");
        }
        stopImpersonating();
        impersonate(CHECKED_USER);
        {
            goToMyAccount();
            clickButton("Edit");
            setFormElement(Locator.name("quf_Phone"), HIDDEN_STRING);
            setFormElement(Locator.name("quf_" + CUSTOM_USER_COLUMN), HIDDEN_STRING);
            clickButton("Submit");
        }
        stopImpersonating();
        setHiddenEmailPermissions();
    }

    @Test
    public void testUserVisibilityViaLookup()
    {
        createHiddenEmailList();

        impersonate(IMPERSONATED_USER);
        goToProjectHome();

        log("Verify that emails cannot be seen in list via lookup");
        clickAndWait(Locator.linkWithText(EMAIL_TEST_LIST));
        DataRegionTable.findDataRegion(this).goToView(HIDDEN_COL_VIEW);
        assertElementPresent(Locator.linkWithText(_userHelper.getDisplayNameForEmail(CHECKED_USER)));
        assertTextNotPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING);
    }

    @Test
    public void testUserVisibilityViaQuery()
    {
        createUsersTableView();

        impersonate(IMPERSONATED_USER);

        log("Verify that emails cannot be seen in query webpart");
        DataRegionTable.findDataRegion(this).goToView(HIDDEN_COL_VIEW);
        assertElementPresent(Locator.linkWithText(_userHelper.getDisplayNameForEmail(CHECKED_USER)));
        assertTextNotPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING);
    }

    @Test
    public void testUserVisibilityAutoCompleteApi() throws Exception
    {
        List<Map<String, String>> adminResponse = getAutoCompleteResponse(ADMIN_USER, getProjectName());
        assertThat("Sanity check failed for auto-complete API as admin",
                adminResponse.toString(), containsString(CHECKED_USER));
        List<Map<String, String>> maskedResponse = getAutoCompleteResponse(IMPERSONATED_USER, getProjectName());
        assertThat("Auto-complete API should not return email address without permission",
                maskedResponse.toString(), allOf(
                        not(containsString(CHECKED_USER)),
                        containsString(_userHelper.getDisplayNameForEmail(CHECKED_USER))));
    }

    private List<Map<String, String>> getAutoCompleteResponse(String user, String containerPath) throws IOException
    {
        Connection connection = new Connection(WebTestHelper.getBaseURL(), user, PasswordUtil.getPassword());
        Command<CommandResponse> command = new Command<>("security", "CompleteUserRead");

        try
        {
            return command.execute(connection, containerPath).getProperty("completions");
        }
        catch (CommandException e)
        {
            throw new RuntimeException(e.getResponseText());
        }
    }

    @LogMethod
    private void createHiddenEmailList()
    {
        // Create list
        impersonate(ADMIN_USER);
        ListHelper.ListColumn userColumn = new ListHelper.ListColumn("user", "user", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(getProjectName(), "core", "Users"));
        _listHelper.createList(getProjectName(), EMAIL_TEST_LIST, ListHelper.ListColumnType.AutoInteger, "Key", userColumn);
        clickButton("Done");
        clickAndWait(Locator.linkWithText(EMAIL_TEST_LIST));
        DataRegionTable.findDataRegion(this).clickInsertNewRow();
        selectOptionByText(Locator.name("quf_user"), _userHelper.getDisplayNameForEmail(CHECKED_USER));
        clickButton("Submit");
        DataRegionTable.findDataRegion(this).clickInsertNewRow();
        selectOptionByText(Locator.name("quf_user"), _userHelper.getDisplayNameForEmail(ADMIN_USER));
        clickButton("Submit");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("user/Phone", "Phone");
        _customizeViewsHelper.addColumn("user/" + CUSTOM_USER_COLUMN, CUSTOM_USER_COLUMN);
        _customizeViewsHelper.addColumn("user/Email", "Email");
        _customizeViewsHelper.addColumn("user/ModifiedBy/Email", "Email");
        _customizeViewsHelper.addColumn("user/ModifiedBy/ModifiedBy/Email", "Email");
        _customizeViewsHelper.addColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(HIDDEN_COL_VIEW, true);

        assertTextPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING); // Ensure subsequent check is valid
        stopImpersonating();
    }

    @LogMethod
    private void createUsersTableView()
    {
        ExecuteQueryPage.beginAt(this, "core", "Users");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(HIDDEN_COL_VIEW, true);

        assertTextPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING); // Ensure subsequent check is valid
    }

    @LogMethod
    private void setHiddenEmailPermissions()
    {
        // Set user permissions for hidden emails
        goToSiteGroups();
        _ext4Helper.clickExt4Tab("Permissions");
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        _permissionsHelper.removeSiteGroupPermission("All Site Users", "See Email Addresses");
        assertElementNotPresent(Locator.permissionButton(TEST_GROUP, "See Email Addresses"));
        assertElementNotPresent(Locator.permissionButton(IMPERSONATED_USER, "See Email Addresses"));
        clickButton("Save and Finish");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
