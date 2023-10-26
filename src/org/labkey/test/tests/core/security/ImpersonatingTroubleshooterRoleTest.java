package org.labkey.test.tests.core.security;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PermissionsHelper;

import java.util.Arrays;
import java.util.List;

@Category({})
public class ImpersonatingTroubleshooterRoleTest extends BaseWebDriverTest
{
    private static final String IMP_T = "impersonating_troubleshooter@imptrouble.test";
    private static final String USER = "user@imptrouble.test";
    private static final String ADMIN_GROUP = "Custom Admin Group";
    private static final String DEV_GROUP = "Custom Developer Group";
    private static final String IT_GROUP = "Custom IT Group";
    private static final String SITE_GROUP = "Custom Site Group";

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, USER, IMP_T);
    }

    @BeforeClass
    public static void setupProject()
    {
        ImpersonatingTroubleshooterRoleTest init = (ImpersonatingTroubleshooterRoleTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _userHelper.createUser(USER);
        _userHelper.createUser(IMP_T);
        setInitialPassword(IMP_T);
        new ApiPermissionsHelper(this).addMemberToRole(IMP_T, "Impersonating Troubleshooter", PermissionsHelper.MemberType.user, "/");
    }

    private void deleteSiteGroups(ApiPermissionsHelper apiPermissionsHelper)
    {
        apiPermissionsHelper.deleteGroup(ADMIN_GROUP, "/", false);
        apiPermissionsHelper.deleteGroup(SITE_GROUP, "/", false);
        apiPermissionsHelper.deleteGroup(DEV_GROUP, "/", false);
        apiPermissionsHelper.deleteGroup(IT_GROUP, "/", false);
    }

    @LogMethod
    private void recreateSiteGroups(ApiPermissionsHelper apiPermissionsHelper)
    {
        deleteSiteGroups(apiPermissionsHelper);
        apiPermissionsHelper.createGlobalPermissionsGroup(SITE_GROUP);
        apiPermissionsHelper.createGlobalPermissionsGroup(ADMIN_GROUP);
        apiPermissionsHelper.addMemberToRole(ADMIN_GROUP, "Site Administrator", PermissionsHelper.MemberType.group, "/");
        apiPermissionsHelper.createGlobalPermissionsGroup(DEV_GROUP);
        apiPermissionsHelper.addMemberToRole(DEV_GROUP, "Platform Developer", PermissionsHelper.MemberType.group, "/");
        apiPermissionsHelper.createGlobalPermissionsGroup(IT_GROUP);
        apiPermissionsHelper.addMemberToRole(IT_GROUP, "Impersonating Troubleshooter", PermissionsHelper.MemberType.group, "/");
    }

    @Test
    public void testSomething()
    {
        Assert.fail("Write some tests");
    }

    @Override
    protected String getProjectName()
    {
        return "ImpersonatingTroubleshooterRoleTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
