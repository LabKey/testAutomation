/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DevModeOnlyTest;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 11/14/12
 */
public class HiddenEmailTest extends BaseWebDriverTest implements DevModeOnlyTest
{
    private static final String TEST_GROUP = "HiddenEmail Test group";
    private static final String ADMIN_USER = "experimental_admin@experimental.test";
    private static final String IMPERSONATED_USER = "experimental_user@experimental.test";
    private static final String CHECKED_USER = "experimental_user2@experimental.test";
    private static final String EMAIL_TEST_LIST = "My Users";
    private static final String EMAIL_VIEW = "emailView";


    @Override
    protected String getProjectName()
    {
        return "Hidden Email Test";
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);

        deleteUsers(afterTest, IMPERSONATED_USER, CHECKED_USER, ADMIN_USER);
        deleteGroup(TEST_GROUP, afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupHiddenEmailTest();
        verifyHiddenEmailTest();
    }

    @LogMethod
    private void setupHiddenEmailTest()
    {
        createUsersAndGroups();
        createHiddenEmailList();
        createQueryWebpart();
        setHiddenEmailPermissions();
    }

    @LogMethod
    private void createUsersAndGroups()
    {
        // Create users and groups
        createUser(ADMIN_USER, null);
        addUserToGroup("Site Administrators", ADMIN_USER);
        impersonate(ADMIN_USER); // Use created user to ensure we have a known 'Modified by' column for created users
        createGlobalPermissionsGroup(TEST_GROUP, IMPERSONATED_USER, CHECKED_USER);
        _containerHelper.createProject(getProjectName(), null);
        setSiteGroupPermissions(TEST_GROUP, "Reader");
        clickButton("Save and Finish");
        stopImpersonating();
        impersonate(CHECKED_USER);
        goToMyAccount();
        clickButton("Edit");
        setFormElement(Locator.name("quf_FirstName"), displayNameFromEmail(CHECKED_USER));
        clickButton("Submit");
        stopImpersonating();
    }

    @LogMethod
    private void createHiddenEmailList()
    {
        // Create list
        impersonate(ADMIN_USER);
        ListHelper.ListColumn userColumn = new ListHelper.ListColumn("user", "user", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(getProjectName(), "core", "Users"));
        _listHelper.createList(getProjectName(), EMAIL_TEST_LIST, ListHelper.ListColumnType.AutoInteger, "Key", userColumn);
        clickButton("Done");
        clickLinkWithText(EMAIL_TEST_LIST);
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_user"), displayNameFromEmail(CHECKED_USER));
        clickButton("Submit");
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_user"), displayNameFromEmail(ADMIN_USER));
        clickButton("Submit");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("user/Email", "Email");
        _customizeViewsHelper.addCustomizeViewColumn("user/ModifiedBy/Email", "Email");
        _customizeViewsHelper.addCustomizeViewColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(EMAIL_VIEW, true);
        stopImpersonating();
    }

    @LogMethod
    private void createQueryWebpart()
    {
        // Create query webpart
        clickFolder(getProjectName());
        addWebPart("Query");
        selectOptionByValue(Locator.name("schemaName"), "core");
        clickRadioButtonById("selectQueryContents");
        selectOptionByValue(Locator.name("queryName"), "Users");
        submit();
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(EMAIL_VIEW, true);
    }

    @LogMethod
    private void setHiddenEmailPermissions()
    {
        // Set user permissions for hidden emails
        goToSiteGroups();
        _ext4Helper.clickExt4Tab("Permissions");
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        removeSiteGroupPermission("All Site Users", "See Email Addresses");
        assertElementNotPresent(Locator.permissionButton(TEST_GROUP, "See Email Addresses"));
        assertElementNotPresent(Locator.permissionButton(IMPERSONATED_USER, "See Email Addresses"));
        clickButton("Save and Finish");
    }

    @LogMethod
    private void verifyHiddenEmailTest()
    {
        impersonate(IMPERSONATED_USER);
        clickFolder(getProjectName());

        log("Verify that emails cannot be seen in query webpart");
        clickMenuButton("Views", EMAIL_VIEW);
        assertTextNotPresent(CHECKED_USER, ADMIN_USER);

        log("Verify that emails cannot be seen in list via lookup");
        clickLinkWithText(EMAIL_TEST_LIST);
        clickMenuButton("Views", EMAIL_VIEW);
        assertTextNotPresent(CHECKED_USER, ADMIN_USER);

        stopImpersonating();
    }
}
