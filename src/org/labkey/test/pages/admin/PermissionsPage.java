package org.labkey.test.pages.admin;

import org.labkey.api.security.PrincipalType;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.PermissionsEditor;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.net.URL;
import java.util.Date;
import java.util.List;

import static org.labkey.test.WebTestHelper.buildURL;

public class PermissionsPage extends LabKeyPage<PermissionsPage.ElementCache>
{
    private static final String READY_SIGNAL = "policyRendered";
    private static final Locator SIGNAL_LOC = Locators.pageSignal(READY_SIGNAL);

    public PermissionsPage(WebDriver driver)
    {
        super(driver);
    }

    public static PermissionsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static PermissionsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("security", containerPath, "permissions"));
        return new PermissionsPage(driver.getDriver());
    }

    public void clickSaveAndFinish()
    {
        clickAndWait(elementCache().saveAndFinishButton);
    }

    public PermissionsPage selectFolder(String folderName)
    {
        clickAndWait(Locator.linkWithText(folderName).withClass("x4-tree-node-text"));
        return new PermissionsPage(getDriver());
    }

    public PermissionsPage createPermissionsGroup(String groupName)
    {
        log("Creating permissions group " + groupName);
        _ext4Helper.clickTabContainingText("Project Groups");
        setFormElement(Locator.xpath("//input[contains(@name, 'projectgroupsname')]"), groupName);
        clickButton("Create New Group", 0);
        _extHelper.waitForExtDialog(groupName + " Information");
        assertTextPresent("Group " + groupName);
        click(Ext4Helper.Locators.ext4Button("Done"));
        waitForElement(Locator.css(".groupPicker .x4-grid-cell-inner").withText(groupName), WAIT_FOR_JAVASCRIPT);
        return this;
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

    public PermissionsPage checkInheritedPermissions()
    {
        checkCheckbox(elementCache().inheritedCheckbox);
        return this;
    }

    public PermissionsPage uncheckInheritedPermissions()
    {
        uncheckCheckbox(elementCache().inheritedCheckbox);
        return this;
    }

    public void savePermissions()
    {
        doAndWaitForPageSignal(() -> clickButton("Save", 0), READY_SIGNAL);
        _ext4Helper.waitForMaskToDisappear();
    }

    @LogMethod
    public PermissionsPage setPermissions(@LoggedParam String groupName, @LoggedParam String... permissionStrings)
    {
        log(new Date().toString());
        for(String permissionString : permissionStrings)
        {
            _setPermissions(groupName, permissionString, "pGroup");
        }
        log(new Date().toString());
        savePermissions();
        return this;
    }

    @LogMethod
    public void setSiteGroupPermissions(@LoggedParam String groupName, @LoggedParam String... permissionStrings)
    {
        log(new Date().toString());
        for(String permissionString : permissionStrings)
        {
            _setPermissions(groupName, permissionString, "pSite");
        }
        log(new Date().toString());
        savePermissions();
    }

    @LogMethod
    public void setUserPermissions(@LoggedParam String userName, @LoggedParam String... permissionsStrings)
    {
        log(new Date().toString());
        for(String permissionString : permissionsStrings)
        {
            _setPermissions(userName, permissionString, "pUser");
        }
        log(new Date().toString());
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
            log("Setting permissions for group " + userOrGroupName + " to " + role);
            _ext4Helper.clickTabContainingText("Permissions");

            String group = userOrGroupName;
            if (className.equals("pSite"))
                group = "Site: " + group;
            _selectPermission(userOrGroupName, group, permissionString);
        }
    }

    private PermissionsPage _selectPermission(String userOrGroupName, String group, String permissionString)
    {
        Locator.XPathLocator roleCombo = Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + permissionString + "']]");
        waitForElement(roleCombo);
        scrollIntoView(roleCombo);
        _ext4Helper.selectComboBoxItem(roleCombo, Ext4Helper.TextMatchTechnique.STARTS_WITH, group);
        waitForElement(Locator.permissionButton(userOrGroupName, permissionString));
        return this;
    }

    public PermissionsPage removeSiteGroupPermission(String groupName, String permissionString)
    {
        return _removePermission(groupName, permissionString, "pSite");
    }

    public PermissionsPage removePermission(String groupName, String permissionString)
    {
        return _removePermission(groupName, permissionString, "pGroup");
    }

    public PermissionsPage _removePermission(String groupName, String permissionString, String className)
    {
        Locator close = Locator.closePermissionButton(groupName, permissionString);
        if (isElementPresent(close))
        {
            click(close);
            waitForElementToDisappear(close);
            savePermissions();
        }
        return this;
    }

    /**
     * Adds a new or existing user to an existing group within an the current project
     *
     * @param userName    new or existing user name
     * @param groupName   existing group within the project to which we should add the user
     */
    public PermissionsPage addUserToProjGroup(String userName, String groupName)
    {
        addUsersToGroup(groupName, userName);
        return this;
    }

    public PermissionsPage addUserToProjGroup(String userName, String projectName, String groupName)
    {
        if (!getCurrentContainerPath().startsWith("/" + projectName))
        {
            beginAt(buildURL("project", projectName, "begin"));
        }
        new SiteNavBar(getDriver()).goToPermissionsPage()
            .clickManageGroup(groupName)
            .addUserToGroupFromGroupScreen(userName);
        return this;
    }


    public PermissionsPage deleteGroup(String groupName)
    {
        deleteGroup(groupName, false);
        return this;
    }

    public void deleteAllUsersFromGroup()
    {
        Locator.XPathLocator l = Locator.xpath("//td/a/span[text()='remove']");

        while (isElementPresent(l))
        {
            int i = getElementCount(l) - 1;
            click(l);
            waitForElementToDisappear(l.index(i), WAIT_FOR_JAVASCRIPT);
        }
    }

    @LogMethod(quiet = true)
    public PermissionsPage deleteGroup(@LoggedParam String groupName, boolean failIfNotFound)
    {
        log("Attempting to delete group: " + groupName);
        if (selectGroup(groupName, failIfNotFound))
        {
            assertElementPresent(Locator.css(".x4-grid-cell-first").withText(groupName));
            deleteAllUsersFromGroup();
            click(Locator.xpath("//td/a/span[text()='Delete Empty Group']"));
            waitForElementToDisappear(Locator.css(".x4-grid-cell-first").withText(groupName), WAIT_FOR_JAVASCRIPT);
        }
        return this;
    }

    public PermissionsPage removeUserFromGroup(String groupName, String userName)
    {
        if (!isTextPresent("Group " + groupName))
            selectGroup(groupName);

        Locator l = Locator.xpath("//td[text()='" + userName + "']/..//td/a/span[text()='remove']");
        click(l);
        return this;
    }

    public PermissionsPage addUserToGroup(String groupName, String userName)
    {
        if (!isTextPresent("Group " + groupName))
            selectGroup(groupName);
        String dialogTitle = groupName + " Information";

        _ext4Helper.selectComboBoxItem(Locator.xpath(_extHelper.getExtDialogXPath(dialogTitle) + "//table[contains(@id, 'labkey-principalcombo')]"), userName);
        Locator.css(".userinfo td").withText(userName).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        _extHelper.clickExtButton(dialogTitle, "Done", 0);
        _extHelper.waitForExtDialogToDisappear(dialogTitle);

        clickButton("Done");
        return this;
    }

    public boolean selectGroup(String groupName, boolean failIfNotFound)
    {
        if (!isElementPresent(Locator.css(".x4-tab-active").withText("Site Groups")))
            goToSiteGroups();

        waitForElement(Locator.css(".groupPicker .x4-grid-body"), WAIT_FOR_JAVASCRIPT);
        if (isElementPresent(Locator.xpath("//div[text()='" + groupName + "']")))
        {
            click(Locator.xpath("//div[text()='" + groupName + "']"));
            _extHelper.waitForExtDialog(groupName + " Information");
            return true;
        }
        else if (failIfNotFound)
            throw new NoSuchElementException("Group not found:" + groupName);

        return false;
    }

    public PermissionsPage openGroupPermissionsDisplay(String groupName)
    {
        _ext4Helper.clickTabContainingText("Project Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRef> refs = _ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long) ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        ref.eval("getSelectionModel().select(" + idx + ")");
        return this;
    }

    public PermissionsPage clickManageGroup(String groupName)
    {
        openGroupPermissionsDisplay(groupName);
        waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        waitForElement(Locator.name("names"));
        return this;
    }

    public PermissionsPage clickManageSiteGroup(String groupName)
    {
        _ext4Helper.clickTabContainingText("Site Groups");
        // warning Administrators can appear multiple times
        List<Ext4CmpRef> refs = _ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long) ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        ref.eval("getSelectionModel().select(" + idx + ")");
        waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        waitForElement(Locator.name("names"));
        return this;
    }

    @LogMethod
    public PermissionsPage createPermissionsGroup(@LoggedParam String groupName, String... memberNames)
    {
        createPermissionsGroup(groupName);
        addUsersToGroup(groupName, memberNames);
        return this;
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

        waitForElementToDisappear(Locator.permissionButton(group, srcRole), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.permissionButton(group, destRole));
    }

    public boolean isUserInGroup(String user, String groupName, PrincipalType principalType)
    {
        _ext4Helper.clickTabContainingText("Project Groups");
        waitForElement(Locator.css(".groupPicker"), WAIT_FOR_JAVASCRIPT);
        waitAndClick(Locator.xpath("//div[text()='" + groupName + "']"));
        _extHelper.waitForExtDialog(groupName + " Information");
        boolean ret;
        if (principalType == PrincipalType.USER)
            ret = isElementPresent(Locator.xpath("//table[contains(@class, 'userinfo')]//td[starts-with(text(), '" + user + "')]"));
        else
            ret = isElementPresent(Locator.linkContainingText(user));
        clickButton("Done", 0);
        _extHelper.waitForExtDialogToDisappear(groupName + " Information");
        return ret;
    }

    public void selectGroup(String groupName)
    {
        selectGroup(groupName, true);
    }

    public boolean doesGroupExist(String groupName)
    {
        _ext4Helper.clickTabContainingText("Project Groups");
        waitForText("Member Groups");
        List<Ext4CmpRef> refs = _ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long) ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        return (idx >= 0);
    }

    public boolean doesPermissionExist(String groupName, String permissionSetting)
    {
        waitForReady();
        return waitForElement(Locator.permissionButton(groupName, permissionSetting), WAIT_FOR_JAVASCRIPT, false);
    }

    private PermissionsPage waitForReady()
    {
        waitForElement(Locators.pageSignal(READY_SIGNAL), WAIT_FOR_JAVASCRIPT);
        return this;
    }

    @LogMethod
    private PermissionsPage addUsersToGroup(String groupName, @LoggedParam String... userNames)
    {
        clickManageGroup(groupName);

        setFormElement(Locator.name("names"), String.join("\n", userNames));
        uncheckCheckbox(Locator.name("sendEmail"));
        clickButton("Update Group Membership");
        return this;
    }

    public PermissionsPage addUserToGroupFromGroupScreen(String userName)
    {
        waitForElement(Locator.name("names"));
        setFormElement(Locator.name("names"), userName);
        uncheckCheckbox(Locator.name("sendEmail"));
        clickButton("Update Group Membership");
        return this;
    }

    public PermissionsPage removeUserFromGroupFromGroupScreen(String userName)
    {
        checkCheckbox(Locator.checkboxByNameAndValue("delete", userName));
        doAndWaitForPageToLoad(() -> {
            clickButton("Update Group Membership", 0);
            assertAlert("Permanently remove selected users from this group?");
        }, WAIT_FOR_PAGE);
        return this;
    }

    public PermissionsPage assertNoPermission(String userOrGroupName, String permissionSetting)
    {
        waitForElementToDisappear(Locator.permissionButton(userOrGroupName, permissionSetting), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public PermissionsPage assertPermissionSetting(String userOrGroupName, String permissionSetting)
    {
        String role = toRole(permissionSetting);

        log("Checking permission setting for group " + userOrGroupName + " equals " + role);
        _ext4Helper.clickTabContainingText("Permissions");

        if (role.endsWith("security.roles.NoPermissionsRole"))
        {
            assertNoPermission(userOrGroupName, "Reader");
            assertNoPermission(userOrGroupName, "Editor");
            assertNoPermission(userOrGroupName, "Project Administrator");
            return this;
        }
        waitForElement(Locator.permissionRendered(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.permissionButton(userOrGroupName, permissionSetting));
        return this;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {

        WebElement saveAndFinishButton = Locator.tagWithText("span", "Save and Finish")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement saveButton = Locator.tagWithText("span", "Save")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement cancelButton = Locator.tagWithText("span", "Cancel")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        WebElement inheritedCheckbox = Locator.inputById("inheritedCheckbox-inputEl")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}