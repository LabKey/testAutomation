package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.user.ClonePermissionsPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PermissionsHelper;

import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class UserClonePermissionTest extends BaseWebDriverTest
{
    private static final String CLONED_SOURCE_SITE_USER = "sourcesiteuser@clonepermission.test";
    private static final String CLONED_SOURCE_APP_USER = "sourceappuser@clonepermission.test";
    private static final String CLONED_TARGET_SITE_USER = "targetsiteuser@clonepermission.test";
    private static final String CLONED_TARGET_APP_USER = "targetappuser@clonepermission.test";
    private static final String CLONE_GROUP = "Test clone group";

    ApiPermissionsHelper _permissionsHelper;

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    @BeforeClass
    public static void setup()
    {
        UserClonePermissionTest initTest = (UserClonePermissionTest) getCurrentTest();
        initTest.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName());
        _permissionsHelper = new ApiPermissionsHelper(this);

        _userHelper.createUser(CLONED_SOURCE_SITE_USER);
        _permissionsHelper.addUserToSiteGroup(CLONED_SOURCE_SITE_USER, "Site Administrators");

        _userHelper.createUser(CLONED_SOURCE_APP_USER);
        _permissionsHelper.addUserAsAppAdmin(CLONED_SOURCE_APP_USER);

        goToProjectHome();
        _permissionsHelper.createPermissionsGroup(CLONE_GROUP);
        _permissionsHelper.setPermissions(CLONE_GROUP, "Editor");
        _permissionsHelper.setPermissions(CLONE_GROUP, "Author");
        _permissionsHelper.addUserToProjGroup(CLONED_SOURCE_SITE_USER, getProjectName(), CLONE_GROUP);
        _permissionsHelper.addMemberToRole(CLONED_SOURCE_SITE_USER, "Author", PermissionsHelper.MemberType.user);
        _permissionsHelper.addUserToProjGroup(CLONED_SOURCE_APP_USER, getProjectName(), CLONE_GROUP);

        _permissionsHelper.addUserToSiteGroup(CLONED_SOURCE_SITE_USER, "Developers");

        _userHelper.createUser(CLONED_TARGET_SITE_USER);
        _permissionsHelper.addMemberToRole(CLONED_TARGET_SITE_USER, "Submitter", PermissionsHelper.MemberType.user);

        _userHelper.createUser(CLONED_TARGET_APP_USER);
    }

    @Test
    public void testSiteUserClonePermission()
    {
        goToSiteUsers();
        clickAndWait(Locator.linkWithText(_userHelper.getDisplayNameForEmail(CLONED_TARGET_SITE_USER)));
        clickAndWait(Locator.linkWithText("Clone Permissions"));
        ClonePermissionsPage clonePermissionsPage = new ClonePermissionsPage(getDriver());

        Assert.assertEquals("Incorrect warning message",
                "Warning! Cloning permissions will delete all group memberships and direct role assignments for " + _userHelper.getDisplayNameForEmail(CLONED_TARGET_SITE_USER) + " (" +
                        CLONED_TARGET_SITE_USER + ") and replace them with the group memberships and direct role assignments of the user selected below.",
                clonePermissionsPage.getWarningMessage());

        log("Validating Clone permission page");
        clonePermissionsPage.setCloneUser("");
        Assert.assertEquals("Invalid error message for blank user ", "Clone user is required", clonePermissionsPage.clonePermissionExpectingError());

        clonePermissionsPage = new ClonePermissionsPage(getDriver());
        clonePermissionsPage.setCloneUser("xxx@junkemail.com");
        Assert.assertEquals("Invalid error message for invalid user ", "Unknown clone user", clonePermissionsPage.clonePermissionExpectingError());

        clonePermissionsPage = new ClonePermissionsPage(getDriver());
        clonePermissionsPage.setCloneUser(CLONED_SOURCE_SITE_USER + " (" + _userHelper.getDisplayNameForEmail(CLONED_SOURCE_SITE_USER) + ")");
        clonePermissionsPage.clonePermission();

        log("Verifying project level permission");
        _permissionsHelper = new ApiPermissionsHelper(getProjectName());
        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_SITE_USER, "Author");
        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_SITE_USER, "Editor");
        _permissionsHelper.assertNoPermission(CLONED_TARGET_SITE_USER, "Submitter");

        log("Verifying site level permission");
        _permissionsHelper = new ApiPermissionsHelper("");
        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_SITE_USER, "PlatformDeveloper");
        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_SITE_USER, "SiteAdmin");

        log("Verify audit log entry");
        DataRegionTable table = goToAdminConsole().clickAuditLog()
                .selectView("Group and role events")
                .getLogTable();
        table.setFilter("user", "Contains", _userHelper.getDisplayNameForEmail(CLONED_TARGET_SITE_USER));

        Assert.assertEquals("Incorrect audit rows for " + _userHelper.getDisplayNameForEmail(CLONED_TARGET_SITE_USER), 7, table.getDataRowCount());
        Assert.assertEquals("Incorrect audit log messages for " + _userHelper.getDisplayNameForEmail(CLONED_TARGET_SITE_USER), Arrays.asList("The user " + CLONED_TARGET_SITE_USER + " was assigned to the security role Author.",
                        "User: " + CLONED_TARGET_SITE_USER + " was added as a member to Group: Developers",
                        "User: " + CLONED_TARGET_SITE_USER + " was added as a member to Group: Test clone group",
                        "User: " + CLONED_TARGET_SITE_USER + " was added as a member to Group: Administrators",
                        "The user " + CLONED_TARGET_SITE_USER + " was removed from the security role Submitter.",
                        "The user " + CLONED_TARGET_SITE_USER + " had their group memberships and role assignments deleted and replaced with those of user sourcesiteuser@clonepermission.test",
                        "The user " + CLONED_TARGET_SITE_USER + " was assigned to the security role Submitter."),
                table.getColumnDataAsText("comment"));
    }

    @Test
    public void testAppAdminClonePermission()
    {
        goToSiteUsers();
        impersonateRole("Application Admin");
        clickAndWait(Locator.linkWithText(_userHelper.getDisplayNameForEmail(CLONED_TARGET_APP_USER)));
        clickAndWait(Locator.linkWithText("Clone Permissions"));
        ClonePermissionsPage clonePermissionsPage = new ClonePermissionsPage(getDriver());

        Assert.assertEquals("Incorrect warning message",
                "Warning! Cloning permissions will delete all group memberships and direct role assignments for " + _userHelper.getDisplayNameForEmail(CLONED_TARGET_APP_USER) + " (" +
                        CLONED_TARGET_APP_USER + ") and replace them with the group memberships and direct role assignments of the user selected below.",
                clonePermissionsPage.getWarningMessage());

        clonePermissionsPage.setCloneUser(CLONED_SOURCE_SITE_USER);
        Assert.assertEquals("App admin should not be able to clone from site admin",
                "Only site administrators can clone from users with site administration permissions",
                clonePermissionsPage.clonePermissionExpectingError());

        clonePermissionsPage = new ClonePermissionsPage(getDriver());
        clonePermissionsPage.setCloneUser(CLONED_SOURCE_APP_USER);
        clonePermissionsPage.clonePermission();
        stopImpersonating();

        log("Verifying project level permission");
        _permissionsHelper = new ApiPermissionsHelper(getProjectName());
        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_APP_USER, "Author");
        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_APP_USER, "Editor");

        log("Verifying site level permission");
        _permissionsHelper = new ApiPermissionsHelper("");
        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_APP_USER, "ApplicationAdmin");

        log("Verify audit logs");
        DataRegionTable table = goToAdminConsole().clickAuditLog()
                .selectView("Group and role events")
                .getLogTable();
        table.setFilter("user", "Contains", _userHelper.getDisplayNameForEmail(CLONED_TARGET_APP_USER));

        Assert.assertEquals("Incorrect audit rows for " + _userHelper.getDisplayNameForEmail(CLONED_TARGET_APP_USER), 3, table.getDataRowCount());
        Assert.assertEquals("Incorrect audit log messages for " + _userHelper.getDisplayNameForEmail(CLONED_TARGET_APP_USER), Arrays.asList("The user " + CLONED_TARGET_APP_USER + " was assigned to the security role Application Admin.",
                        "User: " + CLONED_TARGET_APP_USER + " was added as a member to Group: Test clone group",
                        "The user " + CLONED_TARGET_APP_USER + " had their group memberships and role assignments deleted and replaced with those of user sourceappuser@clonepermission.test"),
                table.getColumnDataAsText("comment"));
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, CLONED_SOURCE_SITE_USER, CLONED_SOURCE_APP_USER, CLONED_TARGET_APP_USER, CLONED_TARGET_SITE_USER);
    }
}
