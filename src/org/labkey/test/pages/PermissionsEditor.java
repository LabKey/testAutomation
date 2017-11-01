/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.api.security.PrincipalType;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Actions;

import java.util.Date;
import java.util.List;

public class PermissionsEditor
{
    private static final String READY_SIGNAL = "policyRendered";
    private static final Locator SIGNAL_LOC = Locators.pageSignal(READY_SIGNAL);
    protected BaseWebDriverTest _test;

    public PermissionsEditor(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void selectFolder(String folderName)
    {
        _test.clickAndWait(Locator.linkWithText(folderName).withClass("x4-tree-node-text"));
    }

    public void createPermissionsGroup(String groupName)
    {
        _test.log("Creating permissions group " + groupName);
        _test._ext4Helper.clickTabContainingText("Project Groups");
        _test.setFormElement(Locator.xpath("//input[contains(@name, 'projectgroupsname')]"), groupName);
        _test.clickButton("Create New Group", 0);
        _test._extHelper.waitForExtDialog(groupName + " Information");
        _test.assertTextPresent("Group " + groupName);
        _test.click(Ext4Helper.Locators.ext4Button("Done"));
        _test.waitForElement(Locator.css(".groupPicker .x4-grid-cell-inner").withText(groupName), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public String toRole(String perm)
    {
        String R = "security.roles.";
        if ("No Permissions".equals(perm))
            return R + "NoPermissionsRole";
        if ("Project Administrator".equals(perm))
            return R + "ProjectAdminRole";
        else if (!perm.contains("."))
            return R + perm + "Role";
        return perm;
    }

    public void checkInheritedPermissions()
    {
        _test._ext4Helper.checkCheckbox("Inherit permissions from parent");
    }

    public void uncheckInheritedPermissions()
    {
        _test._ext4Helper.uncheckCheckbox("Inherit permissions from parent");
    }

    public void savePermissions()
    {
        _test.doAndWaitForPageSignal(() -> _test.clickButton("Save", 0), READY_SIGNAL);
        _test._ext4Helper.waitForMaskToDisappear();
    }

    @LogMethod
    public void setPermissions(@LoggedParam String groupName, @LoggedParam String... permissionStrings)
    {
        _test.log(new Date().toString());
        for(String permissionString : permissionStrings)
        {
            _setPermissions(groupName, permissionString, "pGroup");
        }
        _test.log(new Date().toString());
        savePermissions();
    }

    @LogMethod
    public void setSiteGroupPermissions(@LoggedParam String groupName, @LoggedParam String... permissionStrings)
    {
        _test.log(new Date().toString());
        for(String permissionString : permissionStrings)
        {
            _setPermissions(groupName, permissionString, "pSite");
        }
        _test.log(new Date().toString());
        savePermissions();
    }

    @LogMethod
    public void setUserPermissions(@LoggedParam String userName, @LoggedParam String... permissionsStrings)
    {
        _test.log(new Date().toString());
        for(String permissionString : permissionsStrings)
        {
            _setPermissions(userName, permissionString, "pUser");
        }
        _test.log(new Date().toString());
        savePermissions();
    }

    private void _setPermissions(String userOrGroupName, String permissionString, String className)
    {
        String role = toRole(permissionString);
        if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
        {
            throw new IllegalArgumentException("Can't set NoPermissionRole; call removePermission()");
        }
        else
        {
            _test.log("Setting permissions for group " + userOrGroupName + " to " + role);
            _test._ext4Helper.clickTabContainingText("Permissions");

            String group = userOrGroupName;
            if (className.equals("pSite"))
                group = "Site: " + group;
            _selectPermission(userOrGroupName, group, permissionString);
        }
    }

    private void _selectPermission(String userOrGroupName, String group, String permissionString)
    {
        Locator.XPathLocator roleCombo = Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + permissionString + "']]");
        _test.waitForElement(roleCombo);
        _test.scrollIntoView(roleCombo);
        _test._ext4Helper.selectComboBoxItem(roleCombo, Ext4Helper.TextMatchTechnique.STARTS_WITH, group);
        _test.waitForElement(Locator.permissionButton(userOrGroupName, permissionString));
    }

    public void removeSiteGroupPermission(String groupName, String permissionString)
    {
        _removePermission(groupName, permissionString, "pSite");
    }

    public void removePermission(String groupName, String permissionString)
    {
        _removePermission(groupName, permissionString, "pGroup");
    }

    public void _removePermission(String groupName, String permissionString, String className)
    {
        Locator close = Locator.closePermissionButton(groupName, permissionString);
        if (_test.isElementPresent(close))
        {
            _test.click(close);
            _test.waitForElementToDisappear(close);
            savePermissions();
        }
    }

    /**
     * Adds a new or existing user to an existing group within an the current project
     *
     * @param userName    new or existing user name
     * @param groupName   existing group within the project to which we should add the user
     */
    public void addUserToProjGroup(String userName, String groupName)
    {
        addUsersToGroup(groupName, userName);
    }

    private PermissionsEditor enterPermissionsUI()
    {
        return enterPermissionsUI(_test);
    }

    public static PermissionsEditor enterPermissionsUI(BaseWebDriverTest test)
    {
        if (!test.isElementPresent(SIGNAL_LOC))
        {
            test.clickAdminMenuItem("Folder", "Permissions");
            test.waitForElement(SIGNAL_LOC);
        }
        return new PermissionsEditor(test);
    }

    public void saveAndFinish()
    {
        _test.clickButton("Save and Finish");
    }

    public void deleteGroup(String groupName)
    {
        deleteGroup(groupName, false);
    }

    public void deleteAllUsersFromGroup()
    {
        Locator.XPathLocator l = Locator.xpath("//td/a/span[text()='remove']");

        while (_test.isElementPresent(l))
        {
            int i = _test.getElementCount(l) - 1;
            _test.click(l);
            _test.waitForElementToDisappear(l.index(i), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    @LogMethod(quiet = true)
    public void deleteGroup(@LoggedParam String groupName, boolean failIfNotFound)
    {
        _test.log("Attempting to delete group: " + groupName);
        if (selectGroup(groupName, failIfNotFound))
        {
            _test.assertElementPresent(Locator.css(".x4-grid-cell-first").withText(groupName));
            deleteAllUsersFromGroup();
            _test.click(Locator.xpath("//td/a/span[text()='Delete Empty Group']"));
            _test.waitForElementToDisappear(Locator.css(".x4-grid-cell-first").withText(groupName), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    public void removeUserFromGroup(String groupName, String userName)
    {
        if (!_test.isTextPresent("Group " + groupName))
            selectGroup(groupName);

        Locator l = Locator.xpath("//td[text()='" + userName + "']/..//td/a/span[text()='remove']");
        _test.click(l);
    }

    public void addUserToGroup(String groupName, String userName)
    {
        if (!_test.isTextPresent("Group " + groupName))
            selectGroup(groupName);
        String dialogTitle = groupName + " Information";

        _test._ext4Helper.selectComboBoxItem(Locator.xpath(_test._extHelper.getExtDialogXPath(dialogTitle) + "//table[contains(@id, 'labkey-principalcombo')]"), userName);
        Locator.css(".userinfo td").withText(userName).waitForElement(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test._extHelper.clickExtButton(dialogTitle, "Done", 0);
        _test._extHelper.waitForExtDialogToDisappear(dialogTitle);

        _test.clickButton("Done");
    }

    public boolean selectGroup(String groupName, boolean failIfNotFound)
    {
        if (!_test.isElementPresent(Locator.css(".x4-tab-active").withText("Site Groups")))
            _test.goToSiteGroups();

        _test.waitForElement(Locator.css(".groupPicker .x4-grid-body"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        if (_test.isElementPresent(Locator.xpath("//div[text()='" + groupName + "']")))
        {
            _test.click(Locator.xpath("//div[text()='" + groupName + "']"));
            _test._extHelper.waitForExtDialog(groupName + " Information");
            return true;
        }
        else if (failIfNotFound)
            throw new NoSuchElementException("Group not found:" + groupName);

        return false;
    }

    public void openGroupPermissionsDisplay(String groupName)
    {
        _test._ext4Helper.clickTabContainingText("Project Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRef> refs = _test._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long) ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        ref.eval("getSelectionModel().select(" + idx + ")");
    }

    public void clickManageGroup(String groupName)
    {
        openGroupPermissionsDisplay(groupName);
        _test.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        _test.waitForElement(Locator.name("names"));
    }

    public void clickManageSiteGroup(String groupName)
    {
        _test._ext4Helper.clickTabContainingText("Site Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRef> refs = _test._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long) ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        ref.eval("getSelectionModel().select(" + idx + ")");
        _test.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        _test.waitForElement(Locator.name("names"));
    }

    @LogMethod
    public PermissionsEditor createPermissionsGroup(@LoggedParam String groupName, String... memberNames)
    {
        createPermissionsGroup(groupName);
        addUsersToGroup(groupName, memberNames);
        return enterPermissionsUI();
    }

    @LogMethod(quiet = true)
    public void dragGroupToRole(@LoggedParam String group, @LoggedParam String srcRole, @LoggedParam String destRole, BaseWebDriverTest _test)
    {
        Actions builder = new Actions(_test.getDriver());
        builder
                .clickAndHold(Locator.permissionButton(group, srcRole).findElement(_test.getDriver()))
                .moveToElement(Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + destRole + "']]/div/div").findElement(_test.getDriver()))
                .release()
                .build().perform();

        _test.waitForElementToDisappear(Locator.permissionButton(group, srcRole), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.permissionButton(group, destRole));
    }

    public boolean isUserInGroup(String user, String groupName, PrincipalType principalType)
    {
        _test._ext4Helper.clickTabContainingText("Project Groups");
        _test.waitForElement(Locator.css(".groupPicker"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitAndClick(Locator.xpath("//div[text()='" + groupName + "']"));
        _test._extHelper.waitForExtDialog(groupName + " Information");
        boolean ret;
        if (principalType == PrincipalType.USER)
            ret = _test.isElementPresent(Locator.xpath("//table[contains(@class, 'userinfo')]//td[starts-with(text(), '" + user + "')]"));
        else
            ret = _test.isElementPresent(Locator.linkContainingText(user));
        _test.clickButton("Done", 0);
        _test._extHelper.waitForExtDialogToDisappear(groupName + " Information");
        return ret;
    }

    public void selectGroup(String groupName)
    {
        selectGroup(groupName, true);
    }

    public boolean doesGroupExist(String groupName)
    {
        _test._ext4Helper.clickTabContainingText("Project Groups");
        _test.waitForText("Member Groups");
        List<Ext4CmpRef> refs = _test._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long) ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        return (idx >= 0);
    }

    public boolean doesPermissionExist(String groupName, String permissionSetting)
    {
        waitForReady();
        return _test.waitForElement(Locator.permissionButton(groupName, permissionSetting), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, false);
    }

    private void waitForReady()
    {
        _test.waitForElement(Locators.pageSignal(READY_SIGNAL), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    private void addUsersToGroup(String groupName, @LoggedParam String... userNames)
    {
        clickManageGroup(groupName);

        _test.setFormElement(Locator.name("names"), String.join("\n", userNames));
        _test.uncheckCheckbox(Locator.name("sendEmail"));
        _test.clickButton("Update Group Membership");
    }
}
