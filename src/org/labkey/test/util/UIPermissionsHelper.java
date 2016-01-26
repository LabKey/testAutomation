package org.labkey.test.util;

import org.labkey.api.security.PrincipalType;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.PermissionsEditor;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.net.URL;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class UIPermissionsHelper extends PermissionsHelper
{
    public UIPermissionsHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    public Integer createGlobalPermissionsGroup(String groupName, String... users)
    {
        return createGlobalPermissionsGroup(groupName, true, users);
    }

    @LogMethod
    public void startCreateGlobalPermissionsGroup(@LoggedParam String groupName, boolean failIfAlreadyExists)
    {
        _test.goToHome();
        _test.goToSiteGroups();
        if(_test.isElementPresent(Locator.tagWithText("div", groupName)))
        {
            if(failIfAlreadyExists)
                throw new IllegalArgumentException("Group already exists: " + groupName);
            else
                return;
        }

        Locator l = Locator.xpath("//input[contains(@name, 'sitegroupsname')]");
        _test.waitForElement(l, _test.defaultWaitForPage);

        _test.setFormElement(l, groupName);
        _test.clickButton("Create New Group", 0);
        _test._extHelper.waitForExtDialog(groupName + " Information");
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

        _test.log("Adding [" + namesList.toString() + "] to group " + groupName + "...");
        _test.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        _test.waitForElement(Locator.name("names"));
        _test.setFormElement(Locator.name("names"), namesList.toString());
        _test.uncheckCheckbox(Locator.name("sendEmail"));

        Integer groupId = Integer.parseInt(WebTestHelper.parseUrlQuery(_test.getURL()).get("id"));
        _test.clickButton("Update Group Membership");
        return groupId;
    }

    public Integer createPermissionsGroup(String groupName)
    {
        _test.log("Creating permissions group " + groupName);
        enterPermissionsUI();
        _test._ext4Helper.clickTabContainingText("Project Groups");
        _test.setFormElement(Locator.xpath("//input[contains(@name, 'projectgroupsname')]"), groupName);
        _test.clickButton("Create New Group", 0);
        _test._extHelper.waitForExtDialog(groupName + " Information");
        _test.assertTextPresent("Group " + groupName);
        _test.click(Ext4Helper.Locators.ext4Button("Done"));
        WebElement addedGroup = _test.waitForElement(Locator.css(".groupPicker .x4-grid-cell-inner div").withText(groupName), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        return Integer.parseInt(addedGroup.getAttribute("groupId"));
    }

    public void assertNoPermission(String userOrGroupName, String permissionSetting)
    {
        enterPermissionsUI();
        _test.waitForElementToDisappear(Locator.permissionButton(userOrGroupName, permissionSetting), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void assertPermissionSetting(String userOrGroupName, String permissionSetting)
    {
        String role = toRole(permissionSetting);

        _test.log("Checking permission setting for group " + userOrGroupName + " equals " + role);
        enterPermissionsUI();
        _test._ext4Helper.clickTabContainingText("Permissions");

        if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
        {
            assertNoPermission(userOrGroupName, "Reader");
            assertNoPermission(userOrGroupName, "Editor");
            assertNoPermission(userOrGroupName, "Project Administrator");
            return;
        }
        _test.waitForElement(Locator.permissionRendered(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.assertElementPresent(Locator.permissionButton(userOrGroupName, permissionSetting));
    }

    public void savePermissions()
    {
        _test.doAndWaitForPageSignal(() -> _test.clickButton("Save", 0), "policyRendered");
        _test._ext4Helper.waitForMaskToDisappear();
    }

    public void saveAndFinish()
    {
        _test.clickButton("Save and Finish");
    }

    public void checkInheritedPermissions()
    {
        enterPermissionsUI();
        _test._ext4Helper.checkCheckbox("Inherit permissions from parent");
        saveAndFinish();
    }

    public void uncheckInheritedPermissions()
    {
        enterPermissionsUI();
        _test._ext4Helper.uncheckCheckbox("Inherit permissions from parent");
        savePermissions();
    }

    public boolean isPermissionsInherited()
    {
        enterPermissionsUI();
        return _test.isElementPresent(Locator.css("table#inheritedCheckbox.x4-form-cb-checked"));
    }

    @LogMethod
    public void setSiteAdminRoleUserPermissions(@LoggedParam String userName, @LoggedParam String permissionString)
    {
        _test.log(new Date().toString());
        _test.goToSiteAdmins();
        _test.clickAndWait(Locator.linkContainingText("Permissions"));
        _test._ext4Helper.clickTabContainingText("Permissions");
        _selectPermission(userName, userName, permissionString);
        _test.log(new Date().toString());
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
            _test.log("Setting permissions for " + userOrGroupName + " to " + role);

            enterPermissionsUI();
            _test._ext4Helper.clickTabContainingText("Permissions");

            String group = userOrGroupName;
            if (memberType == MemberType.siteGroup)
                group = "Site: " + group;
            _selectPermission(userOrGroupName, group, permissionString);
        }
    }

    private void _selectPermission(String userOrGroupName, String group, String permissionString)
    {
        Locator.XPathLocator roleCombo = Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + permissionString + "']]");
        _test.waitForElement(roleCombo);
        _test._ext4Helper.selectComboBoxItem(roleCombo, Ext4Helper.TextMatchTechnique.STARTS_WITH, group);
        _test.waitForElement(Locator.permissionButton(userOrGroupName, permissionString));
        String oldId = _test.getAttribute(Locator.permissionButton(userOrGroupName, permissionString), "id");
        savePermissions();
        _test._ext4Helper.waitForMaskToDisappear();
        _test.waitForElementToDisappear(Locator.id(oldId), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT); // Elements get new ids after save
        assertPermissionSetting(userOrGroupName, permissionString);
    }

    public void addUserToSiteGroup(String userName, String groupName)
    {
        _test.ensureAdminMode();
        switch (groupName)
        {
            case "Administrators":
                _test.goToSiteAdmins();
                break;
            case "Developers":
                _test.goToSiteDevelopers();
                break;
            default:
                _test.goToSiteGroups();
                Locator.XPathLocator groupLoc = Locator.tagWithText("div", groupName);
                _test.waitForElement(groupLoc, _test.defaultWaitForPage);
                _test.click(groupLoc);
                _test.clickAndWait(Locator.linkContainingText("manage group"));
        }
        addUserToGroupFromGroupScreen(userName);
    }

    protected void removeRoleAssignment(String groupName, String permissionString, MemberType memberType)
    {
        Locator close = Locator.closePermissionButton(groupName,permissionString);
        if (_test.isElementPresent(close))
        {
            _test.click(close);
            _test.waitForElementToDisappear(close);
            savePermissions();
            assertNoPermission(groupName, permissionString);
        }
    }

    public void addUserToProjGroup(String userName, String projectName, String groupName)
    {
        if (!_test.getCurrentContainerPath().startsWith("/" + projectName + "/"))
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
        new PermissionsEditor(_test).enterPermissionsUI();
    }

    public void exitPermissionsUI()
    {
        _test._ext4Helper.clickTabContainingText("Permissions");
        saveAndFinish();
    }

    public void deleteAllUsersFromGroup()
    {
        Locator.XPathLocator l = Locator.xpath("//td/a/span[text()='remove']");

        while(_test.isElementPresent(l))
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

    @Override
    public void removeUserFromSiteGroup(String groupName, String userName)
    {
        removeUserFromGroup(groupName, userName);
    }

    public void removeUserFromGroup(String groupName, String userName)
    {
        if(!_test.isTextPresent("Group " + groupName))
            selectGroup(groupName);

        Locator l = Locator.xpath("//td[text()='" + userName +  "']/..//td/a/span[text()='remove']");
        _test.click(l);
        _test.waitForElementToDisappear(l);
    }

    public boolean selectGroup(String groupName, boolean failIfNotFound)
    {
        if(!_test.isElementPresent(Locator.css(".x4-tab-active").withText("Site Groups")))
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

    public boolean selectGroup(String groupName)
    {
        return selectGroup(groupName, false);
    }

    public boolean doesGroupExist(String groupName, String projectName)
    {
        _test.ensureAdminMode();
        _test.clickProject(projectName);
        enterPermissionsUI();
        _test._ext4Helper.clickTabContainingText("Project Groups");
        _test.waitForText("Member Groups");
        List<Ext4CmpRef> refs = _test._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
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

    public Integer createPermissionsGroup(String groupName, String... memberNames)
    {
        Integer groupId = createPermissionsGroup(groupName);
        clickManageGroup(groupName);

        StringBuilder namesList = new StringBuilder();
        for(String member : memberNames)
        {
            namesList.append(member).append("\n");
        }

        _test.log("Adding [" + namesList.toString() + "] to group " + groupName + "...");
        addUserToGroupFromGroupScreen(namesList.toString());

        enterPermissionsUI();
        return groupId;
    }

    public void openGroupPermissionsDisplay(String groupName)
    {
        _test._ext4Helper.clickTabContainingText("Project Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRef> refs = _test._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("getSelectionModel().select(" + idx + ")");
    }

    public void clickManageGroup(String groupName)
    {
        openGroupPermissionsDisplay(groupName);
        _test.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        _test.waitForElement(Locator.name("names"));
    }

    public void clickManageSiteGroup(String groupName, BaseWebDriverTest _test)
    {
        _test._ext4Helper.clickTabContainingText("Site Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRef> refs = _test._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("getSelectionModel().select(" + idx + ")");
        _test.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        _test.waitForElement(Locator.name("names"));
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

    public void addUserToGroupFromGroupScreen(String userName)
    {
        _test.waitForElement(Locator.name("names"));
        _test.setFormElement(Locator.name("names"), userName);
        _test.uncheckCheckbox(Locator.name("sendEmail"));
        _test.clickButton("Update Group Membership");
    }

    public void removeUserFromGroupFromGroupScreen(String userName)
    {
        _test.checkCheckbox(Locator.checkboxByNameAndValue("delete", userName));
        _test.doAndWaitForPageToLoad(() -> {
            _test.clickButton("Update Group Membership", 0);
            _test.assertAlert("Permanently remove selected users from this group?");
        }, BaseWebDriverTest.WAIT_FOR_PAGE);
    }
}
