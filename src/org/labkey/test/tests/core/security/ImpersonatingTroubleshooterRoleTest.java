package org.labkey.test.tests.core.security;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.user.ImpersonateRolesCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.labkey.test.util.PermissionsHelper.IMP_TROUBLESHOOTER_ROLE;
import static org.labkey.test.util.PermissionsHelper.toRole;

@Category({Git.class})
public class ImpersonatingTroubleshooterRoleTest extends TroubleshooterRoleTest
{
    private static final String USER = "user@imptrouble.test";

    private final ApiPermissionsHelper _apiPermissionsHelper = new ApiPermissionsHelper(this);

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, USER);
    }

    @Override
    protected void doSetup()
    {
        super.doSetup();
        setInitialPassword(TROUBLESHOOTER);
    }

    @Override
    protected String getRole()
    {
        return IMP_TROUBLESHOOTER_ROLE;
    }

    /**
     * "Impersonating Troubleshooter" should not be able to modify permissions for privileged roles (e.g. Site Admin)
     * They should be able to do so when impersonating a Site Admin.
     */
    @Test
    public void testModifyPrivilegedPermission() throws Exception
    {
        _userHelper.createUser(USER);
        Assertions.assertThatThrownBy(() -> apiAsTroubleshooter().addMemberToRole(USER, "Site Admin", PermissionsHelper.MemberType.user, "/"))
                .as("Impersonating Troubleshooter assigning Site Admin over API").cause()
                .isInstanceOf(CommandException.class)
                .hasMessage("User does not have permission to perform this operation.");

        apiAsImpersonatingSiteAdmin().addMemberToRole(USER, "Site Admin", PermissionsHelper.MemberType.user, "/");
        Assertions.assertThat(_apiPermissionsHelper.getUserRoles("/", USER)).contains(PermissionsHelper.toRole("Site Administrator"));
    }

    @Override
    @Test
    public void testAdminConsoleVisibility()
    {
        signOut();
        signIn(TROUBLESHOOTER);
        log("Verify permissions from troubleshooter");
        verifySitePermissionSetting(false);

        impersonateRole("Site Administrator");
        log("Verify the permissions while impersonating admin");
        verifySitePermissionSetting(true);
    }

    private ApiPermissionsHelper apiAsTroubleshooter()
    {
        return new ApiPermissionsHelper(this, () -> new Connection(WebTestHelper.getBaseURL(), TROUBLESHOOTER, PasswordUtil.getPassword()));
    }

    private ApiPermissionsHelper apiAsImpersonatingSiteAdmin() throws IOException, CommandException
    {
        Connection connection = new Connection(WebTestHelper.getBaseURL(), TROUBLESHOOTER, PasswordUtil.getPassword());
        new ImpersonateRolesCommand(toRole("Site Administrator")).execute(connection, "/");
        return new ApiPermissionsHelper(this, () -> connection);
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
