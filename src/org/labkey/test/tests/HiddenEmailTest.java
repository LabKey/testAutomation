/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DevModeOnlyTest;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.util.List;

@Category({DailyB.class})
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
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        deleteUsersIfPresent(IMPERSONATED_USER, CHECKED_USER, ADMIN_USER);
        _permissionsHelper.deleteGroup(TEST_GROUP, afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Test
    public void testSteps()
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
        _userHelper.createUser(ADMIN_USER);

        pushLocation();
        goToSiteAdmins();
        setFormElement(Locator.xpath("//textarea[@name='names']"), ADMIN_USER);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
        assertTextPresent(ADMIN_USER);
        clickButton("Update Group Membership");
        popLocation();

        impersonate(ADMIN_USER); // Use created user to ensure we have a known 'Modified by' column for created users
        _permissionsHelper.createGlobalPermissionsGroup(TEST_GROUP, IMPERSONATED_USER, CHECKED_USER);
        _containerHelper.createProject(getProjectName(), null);
        _permissionsHelper.setSiteGroupPermissions(TEST_GROUP, "Reader");
        clickButton("Save and Finish");
        stopImpersonating();
        impersonate(CHECKED_USER);
        goToMyAccount();
        clickButton("Edit");
        setFormElement(Locator.name("quf_FirstName"), _userHelper.getDisplayNameForEmail(CHECKED_USER));
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
        clickAndWait(Locator.linkWithText(EMAIL_TEST_LIST));
        DataRegionTable.findDataRegion(this).clickInsertNewRow();
        selectOptionByText(Locator.name("quf_user"), _userHelper.getDisplayNameForEmail(CHECKED_USER));
        clickButton("Submit");
        DataRegionTable.findDataRegion(this).clickInsertNewRow();
        selectOptionByText(Locator.name("quf_user"), _userHelper.getDisplayNameForEmail(ADMIN_USER));
        clickButton("Submit");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("user/Email", "Email");
        _customizeViewsHelper.addColumn("user/ModifiedBy/Email", "Email");
        _customizeViewsHelper.addColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(EMAIL_VIEW, true);
        stopImpersonating();
    }

    @LogMethod
    private void createQueryWebpart()
    {
        // Create query webpart
        clickProject(getProjectName());
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addQueryWebPart(null, "core", "Users", null);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(EMAIL_VIEW, true);
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

    @LogMethod
    private void verifyHiddenEmailTest()
    {
        impersonate(IMPERSONATED_USER);
        clickProject(getProjectName());

        log("Verify that emails cannot be seen in query webpart");
        DataRegionTable.findDataRegion(this).goToView( EMAIL_VIEW);
        assertTextNotPresent(CHECKED_USER, ADMIN_USER);

        log("Verify that emails cannot be seen in list via lookup");
        clickAndWait(Locator.linkWithText(EMAIL_TEST_LIST));
        DataRegionTable.findDataRegion(this).goToView( EMAIL_VIEW);
        assertTextNotPresent(CHECKED_USER, ADMIN_USER);

        stopImpersonating();
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
