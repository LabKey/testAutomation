/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.api.security.PrincipalType;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.PermissionsEditor;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class UIPermissionsHelper extends PermissionsHelper
{
    protected BaseWebDriverTest _test;

    public UIPermissionsHelper(BaseWebDriverTest test)
    {
        super(test);
        _test = test;
    }

    public Integer createGlobalPermissionsGroup(String groupName, String... users)
    {
        return createGlobalPermissionsGroup(groupName, true, users);
    }

    @LogMethod
    public void startCreateGlobalPermissionsGroup(@LoggedParam String groupName, boolean failIfAlreadyExists)
    {
        _test.goToHome();
        _driver.goToSiteGroups();
        if (_driver.isElementPresent(Locator.tagWithText("div", groupName)))
        {
            if (failIfAlreadyExists)
                throw new IllegalArgumentException("Group already exists: " + groupName);
            else
                return;
        }

        Locator l = Locator.xpath("//input[contains(@name, 'sitegroupsname')]");
        _driver.waitForElement(l, _driver.defaultWaitForPage);

        _driver.setFormElement(l, groupName);
        _driver.pressEnter(l);
        _driver._extHelper.waitForExtDialog(groupName + " Information");
    }

    @LogMethod
    public Integer createGlobalPermissionsGroup(@LoggedParam String groupName, boolean failIfAlreadyExists, @LoggedParam String... users)
    {
        startCreateGlobalPermissionsGroup(groupName, failIfAlreadyExists);
        StringBuilder namesList = new StringBuilder();

        for (String member : users)
        {
            namesList.append(member).append("\n");
        }

        _driver.log("Adding [" + namesList.toString() + "] to group " + groupName + "...");
        _driver.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        _driver.waitForElement(Locator.name("names"));
        _driver.setFormElement(Locator.name("names"), namesList.toString());
        _driver.uncheckCheckbox(Locator.name("sendEmail"));

        Integer groupId = Integer.parseInt(WebTestHelper.parseUrlQuery(_driver.getURL()).get("id"));
        _driver.clickButton("Update Group Membership");
        return groupId;
    }

    public Integer createPermissionsGroup(String groupName)
    {
        _driver.log("Creating permissions group " + groupName);
        enterPermissionsUI();
        _driver._ext4Helper.clickTabContainingText("Project Groups");
        _driver.setFormElement(Locator.xpath("//input[contains(@name, 'projectgroupsname')]"), groupName);
        _driver.clickButton("Create New Group", 0);
        _driver._extHelper.waitForExtDialog(groupName + " Information");
        _driver.assertTextPresent("Group " + groupName);
        _driver.click(Ext4Helper.Locators.ext4Button("Done"));
        WebElement addedGroup = _driver.waitForElement(Locator.css(".groupPicker .x4-grid-cell-inner div").withText(groupName), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        return Integer.parseInt(addedGroup.getAttribute("groupId"));
    }

    public void assertNoPermission(String userOrGroupName, String permissionSetting)
    {
        enterPermissionsUI();
        _driver.waitForElementToDisappear(Locator.permissionButton(userOrGroupName, permissionSetting), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void assertPermissionSetting(String userOrGroupName, String permissionSetting)
    {
        String role = toRole(permissionSetting);

        _driver.log("Checking permission setting for group " + userOrGroupName + " equals " + role);
        enterPermissionsUI();
        _driver._ext4Helper.clickTabContainingText("Permissions");

        if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
        {
            assertNoPermission(userOrGroupName, "Reader");
            assertNoPermission(userOrGroupName, "Editor");
            assertNoPermission(userOrGroupName, "Project Administrator");
            return;
        }
        _driver.waitForElement(Locator.permissionRendered(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _driver.assertElementPresent(Locator.permissionButton(userOrGroupName, permissionSetting));
    }

    public void savePermissions()
    {
        _driver.doAndWaitForPageSignal(() -> _driver.clickButton("Save", 0), "policyRendered");
        _driver._ext4Helper.waitForMaskToDisappear();
    }

    public void saveAndFinish()
    {
        _driver.clickButton("Save and Finish");
    }

    public void checkInheritedPermissions()
    {
        enterPermissionsUI();
        _driver._ext4Helper.checkCheckbox("Inherit permissions from parent");
        saveAndFinish();
    }

    public void uncheckInheritedPermissions()
    {
        enterPermissionsUI();
        _driver._ext4Helper.uncheckCheckbox("Inherit permissions from parent");
        savePermissions();
    }

    public boolean isPermissionsInherited()
    {
        enterPermissionsUI();
        return _driver.isElementPresent(Locator.css("table#inheritedCheckbox.x4-form-cb-checked"));
    }

    @LogMethod
    public void setSiteAdminRoleUserPermissions(@LoggedParam String userName, @LoggedParam String permissionString)
    {
        _driver.log(new Date().toString());
        _driver.goToSiteAdmins();
        _driver.clickAndWait(Locator.tag("ol").append(Locator.linkContainingText("Permissions")));
        _driver._ext4Helper.clickTabContainingText("Permissions");
        _selectPermission(userName, userName, permissionString);
        _driver.log(new Date().toString());
    }

    protected void addMemberToRole(String userOrGroupName, String permissionString, MemberType memberType)
    {
        String role = toRole(permissionString);
        if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
        {
            throw new IllegalArgumentException("Can't set NoPermissionRole; call removePermission()");
        }
        else
        {
            _driver.log("Setting permissions for " + userOrGroupName + " to " + role);

            enterPermissionsUI();
            _driver._ext4Helper.clickTabContainingText("Permissions");

            String group = userOrGroupName;
            if (memberType == MemberType.siteGroup)
                group = "Site: " + group;
            _selectPermission(userOrGroupName, group, permissionString);
        }
    }

    private void _selectPermission(String userOrGroupName, String group, String permissionString)
    {
        Locator.XPathLocator roleCombo = Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + permissionString + "']]");
        _driver.waitForElement(roleCombo);
        _driver.scrollIntoView(roleCombo);
        _driver._ext4Helper.selectComboBoxItem(roleCombo, Ext4Helper.TextMatchTechnique.STARTS_WITH, group);
        _driver.waitForElement(Locator.permissionButton(userOrGroupName, permissionString));
        String oldId = _driver.getAttribute(Locator.permissionButton(userOrGroupName, permissionString), "id");
        savePermissions();
        _driver._ext4Helper.waitForMaskToDisappear();
        _driver.waitForElementToDisappear(Locator.id(oldId), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT); // Elements get new ids after save
        assertPermissionSetting(userOrGroupName, permissionString);
    }

    public void addUserToSiteGroup(String userName, String groupName)
    {
        _test.ensureAdminMode();
        switch (groupName)
        {
            case "Administrators":
                _driver.goToSiteAdmins();
                break;
            case "Developers":
                _driver.goToSiteDevelopers();
                break;
            default:
                _driver.goToSiteGroups();
                Locator.XPathLocator groupLoc = Locator.tagWithText("div", groupName);
                _driver.waitForElement(groupLoc, _driver.defaultWaitForPage);
                _driver.click(groupLoc);
                _driver.clickAndWait(Locator.linkContainingText("manage group"));
        }
        addUserToGroupFromGroupScreen(userName);
    }

    protected void removeRoleAssignment(String groupName, String permissionString, MemberType memberType)
    {
        Locator close = Locator.closePermissionButton(groupName,permissionString);
        if (_driver.isElementPresent(close))
        {
            _driver.click(close);
            _driver.waitForElementToDisappear(close);
            savePermissions();
            assertNoPermission(groupName, permissionString);
        }
    }

    public void addUserToProjGroup(String userName, String projectName, String groupName)
    {
        if (!_driver.getCurrentContainerPath().startsWith("/" + projectName))
        {
            _test.goToProjectHome(projectName);
        }
        enterPermissionsUI();
        clickManageGroup(groupName);
        addUserToGroupFromGroupScreen(userName);
    } //addUserToProjGroup()

    @Deprecated
    public void enterPermissionsUI()
    {
        PermissionsEditor.enterPermissionsUI(_test);
    }

    public void exitPermissionsUI()
    {
        _driver._ext4Helper.clickTabContainingText("Permissions");
        saveAndFinish();
    }

    public void deleteAllUsersFromGroup()
    {
        Locator.XPathLocator l = Locator.xpath("//td/a/span[text()='remove']");

        while(_driver.isElementPresent(l))
        {
            int i = _driver.getElementCount(l) - 1;
            _driver.click(l);
            _driver.waitForElementToDisappear(l.index(i), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    @LogMethod(quiet = true)
    public void deleteGroup(@LoggedParam String groupName, boolean failIfNotFound)
    {
        _driver.log("Attempting to delete group: " + groupName);
        if (selectGroup(groupName, failIfNotFound))
        {
            _driver.assertElementPresent(Locator.css(".x4-grid-cell-first").withText(groupName));
            deleteAllUsersFromGroup();
            _driver.click(Locator.xpath("//td/a/span[text()='Delete Empty Group']"));
            _driver.waitForElementToDisappear(Locator.css(".x4-grid-cell-first").withText(groupName), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    @Override
    public void removeUserFromSiteGroup(String groupName, String userName)
    {
        removeUserFromGroup(groupName, userName);
    }

    public void removeUserFromGroup(String groupName, String userName)
    {
        if (!_driver.isTextPresent("Group " + groupName))
            selectGroup(groupName);

        Locator l = Locator.xpath("//td[text()='" + userName +  "']/..//td/a/span[text()='remove']");
        _driver.click(l);
        _driver.waitForElementToDisappear(l);
    }

    public boolean selectGroup(String groupName, boolean failIfNotFound)
    {
        if (!_driver.isElementPresent(Locator.css(".x4-tab-active").withText("Site Groups")))
            _driver.goToSiteGroups();

        _driver.waitForElement(Locator.css(".groupPicker .x4-grid-body"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        if (_driver.isElementPresent(Locator.xpath("//div[text()='" + groupName + "']")))
        {
            _driver.click(Locator.xpath("//div[text()='" + groupName + "']"));
            _driver._extHelper.waitForExtDialog(groupName + " Information");
            return true;
        }
        else if (failIfNotFound)
            throw new NoSuchElementException("Group not found:" + groupName);

        return false;
    }

    public boolean selectGroup(String groupName)
    {
        return selectGroup(groupName, false);
    }

    public boolean doesGroupExist(String groupName, String projectName)
    {
        _test.ensureAdminMode();
        _test.clickProject(projectName);
        enterPermissionsUI();
        _driver._ext4Helper.clickTabContainingText("Project Groups");
        _driver.waitForText("Member Groups");
        List<Ext4CmpRef> refs = _driver._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        exitPermissionsUI();
        return (idx >= 0);
    }

    public boolean isUserInGroup(String user, String groupName, String projectName, PrincipalType principalType)
    {
        _test.ensureAdminMode();
        _test.clickProject(projectName);
        enterPermissionsUI();
        _driver._ext4Helper.clickTabContainingText("Project Groups");
        _driver.waitForElement(Locator.css(".groupPicker"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _driver.waitAndClick(Locator.xpath("//div[text()='" + groupName + "']"));
        _driver._extHelper.waitForExtDialog(groupName + " Information");
        boolean ret;
        if (principalType == PrincipalType.USER)
            ret = _driver.isElementPresent(Locator.xpath("//table[contains(@class, 'userinfo')]//td[starts-with(text(), '" + user + "')]"));
        else
            ret = _driver.isElementPresent(Locator.linkContainingText(user));
        _driver.clickButton("Done", 0);
        _driver._extHelper.waitForExtDialogToDisappear(groupName + " Information");
        return ret;
    }

    public Integer createPermissionsGroup(String groupName, String... memberNames)
    {
        Integer groupId = createPermissionsGroup(groupName);
        clickManageGroup(groupName);

        StringBuilder namesList = new StringBuilder();
        for (String member : memberNames)
        {
            namesList.append(member).append("\n");
        }

        _driver.log("Adding [" + namesList.toString() + "] to group " + groupName + "...");
        addUserToGroupFromGroupScreen(namesList.toString());

        enterPermissionsUI();
        return groupId;
    }

    public void openGroupPermissionsDisplay(String groupName)
    {
        _driver._ext4Helper.clickTabContainingText("Project Groups");
        // warning Administrators can appear multiple times
        List<Ext4CmpRef> refs = _driver._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("getSelectionModel().select(" + idx + ")");
    }

    public void clickManageGroup(String groupName)
    {
        openGroupPermissionsDisplay(groupName);
        _driver.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        Locator loc = Locator.name("names");
        _driver.waitForElement(loc);
    }

    public void clickManageSiteGroup(String groupName, BaseWebDriverTest _test)
    {
        _driver._ext4Helper.clickTabContainingText("Site Groups");
        // warning Administrators can appear multiple times
        List<Ext4CmpRef> refs = _driver._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("getSelectionModel().select(" + idx + ")");
        _driver.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        _driver.waitForElement(Locator.name("names"));
    }

    @LogMethod(quiet = true)
    public void dragGroupToRole(@LoggedParam String group, @LoggedParam String srcRole, @LoggedParam String destRole, BaseWebDriverTest _test)
    {
        Actions builder = new Actions(_driver.getDriver());
        builder
                .clickAndHold(Locator.permissionButton(group, srcRole).findElement(_driver.getDriver()))
                .moveToElement(Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + destRole + "']]/div/div").findElement(_driver.getDriver()))
                .release()
                .build().perform();

        _driver.waitForElementToDisappear(Locator.permissionButton(group, srcRole), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _driver.waitForElement(Locator.permissionButton(group, destRole));
    }

    public void addUserToGroupFromGroupScreen(String userName)
    {
        _driver.waitForElement(Locator.name("names"));
        _driver.setFormElement(Locator.name("names"), userName);
        _driver.uncheckCheckbox(Locator.name("sendEmail"));
        _driver.clickButton("Update Group Membership");
    }

    public void removeUserFromGroupFromGroupScreen(String userName)
    {
        _driver.checkCheckbox(Locator.checkboxByNameAndValue("delete", userName));
        _driver.doAndWaitForPageToLoad(() -> {
            _driver.clickButton("Update Group Membership", 0);
            _driver.assertAlert("Permanently remove selected users from this group?");
        }, BaseWebDriverTest.WAIT_FOR_PAGE);
    }
}
