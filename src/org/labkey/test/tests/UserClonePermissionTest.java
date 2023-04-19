package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.user.ClonePermissionsPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;

import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class UserClonePermissionTest extends BaseWebDriverTest
{
    private static final String CLONED_SOURCE_SITE_USER = "sourcesiteuser@clonepermission.test";
    private static final String CLONED_SOURCE_APP_USER = "sourceappuser@clonepermission.test";
    private static final String CLONED_TARGET_SITE_USER = "targetsiteuser@clonepermission.test";
    private static final String CLONED_TARGET_APP_USER = "targetappuser@clonepermission.test";
    private static final String CLONE_GROUP = "Test clone group";

    ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

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

        _userHelper.createUser(CLONED_SOURCE_SITE_USER);
        _permissionsHelper.addUserToSiteGroup(CLONED_SOURCE_SITE_USER, "Site Administrators");

        _userHelper.createUser(CLONED_SOURCE_APP_USER);
        _permissionsHelper.addUserAsAppAdmin(CLONED_SOURCE_APP_USER);

        goToProjectHome();
        _permissionsHelper.createPermissionsGroup(CLONE_GROUP);
        _permissionsHelper.setPermissions(CLONE_GROUP, "Reader");
        _permissionsHelper.setPermissions(CLONE_GROUP, "Electronic Signer");
        _permissionsHelper.addUserToProjGroup(CLONED_SOURCE_SITE_USER, getProjectName(), CLONE_GROUP);
        _permissionsHelper.addMemberToRole(CLONED_SOURCE_SITE_USER, "Author", PermissionsHelper.MemberType.user);

        _permissionsHelper.addUserToSiteGroup(CLONED_SOURCE_SITE_USER, "Developers");

        _userHelper.createUser(CLONED_TARGET_SITE_USER);
        _permissionsHelper.addUserToSiteGroup(CLONED_TARGET_SITE_USER, "Site Administrators");

        _userHelper.createUser(CLONED_TARGET_APP_USER);
    }

    @Test
    public void testSiteUserClonePermission()
    {
        impersonate(CLONED_TARGET_SITE_USER);
        ClonePermissionsPage clonePermissionsPage = goToMyAccount().clickClonePermission();

        Assert.assertEquals("Incorrect warning message",
                "Warning! Cloning permissions will delete all group memberships and direct role assignments for targetsiteuser (targetsiteuser@clonepermission.test) and replace them with the group memberships and direct role assignments of the user selected below.",
                clonePermissionsPage.getWarningMessage());

        clonePermissionsPage.setCloneUser(CLONED_SOURCE_SITE_USER);
        clonePermissionsPage.clonePermission();
        stopImpersonating();

        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_SITE_USER, "Reader");
        _permissionsHelper.assertPermissionSetting(CLONED_TARGET_SITE_USER, "Developers");

        //verify audit log entry

    }

    @Test
    public void testAppAdminClonePermission()
    {

    }


    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, CLONED_SOURCE_SITE_USER, CLONED_SOURCE_APP_USER, CLONED_TARGET_APP_USER, CLONED_TARGET_SITE_USER);
    }
}
