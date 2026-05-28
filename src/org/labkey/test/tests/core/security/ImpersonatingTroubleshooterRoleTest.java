/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.tests.core.security;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.user.ImpersonateRolesCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.labkey.test.pages.LabkeyErrorPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;

import java.io.IOException;

import static org.labkey.test.util.PermissionsHelper.IMP_TROUBLESHOOTER_ROLE;
import static org.labkey.test.util.PermissionsHelper.SITE_ADMIN_ROLE;
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
        setInitialPassword(_troubleShooterId);
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
                .hasMessage(LabkeyErrorPage.UNAUTHORIZED_FULL_PAGE_MESSAGE);

        apiAsImpersonatingSiteAdmin().addMemberToRole(USER, "Site Admin", PermissionsHelper.MemberType.user, "/");
        Assertions.assertThat(_apiPermissionsHelper.getUserRoles("/", USER)).contains(PermissionsHelper.toRole(SITE_ADMIN_ROLE));
    }

    @Override
    @Test
    public void testAdminConsoleVisibility()
    {
        signOut();
        signIn(TROUBLESHOOTER_USER);
        log("Verify permissions from troubleshooter");
        verifySitePermissionSetting(false);

        impersonateRole(SITE_ADMIN_ROLE);
        log("Verify the permissions while impersonating admin");
        verifySitePermissionSetting(true);
    }

    private ApiPermissionsHelper apiAsTroubleshooter()
    {
        return new ApiPermissionsHelper(this, () -> new Connection(WebTestHelper.getBaseURL(), TROUBLESHOOTER_USER, PasswordUtil.getPassword()));
    }

    private ApiPermissionsHelper apiAsImpersonatingSiteAdmin() throws IOException, CommandException
    {
        Connection connection = new Connection(WebTestHelper.getBaseURL(), TROUBLESHOOTER_USER, PasswordUtil.getPassword());
        new ImpersonateRolesCommand(toRole(SITE_ADMIN_ROLE)).execute(connection, "/");
        return new ApiPermissionsHelper(this, () -> connection);
    }

    @Override
    protected String getProjectName()
    {
        return "ImpersonatingTroubleshooterRoleTest Project";
    }

}
