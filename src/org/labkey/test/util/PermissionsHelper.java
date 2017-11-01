/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.apache.commons.lang3.ObjectUtils;
import org.labkey.api.security.PrincipalType;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.GetRolesCommand;
import org.labkey.remoteapi.security.GetRolesResponse;
import org.labkey.test.WebDriverWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * org.labkey.core.security.SecurityController#PermissionsAction
 */
public abstract class PermissionsHelper
{
    private static final String permissionClassPkg = "org.labkey.api.security.roles.";
    protected WebDriverWrapper _driver;

    public PermissionsHelper(WebDriverWrapper driver)
    {
        _driver = driver;
    }

    public String toRole(final String perm)
    {
        if (perm.contains("."))
            return perm;

        Map<String, String> specialRoleClasses = new HashMap<>();
        specialRoleClasses.put("See Audit Log Events", permissionClassPkg + "CanSeeAuditLogRole");

        String roleClassName = perm.replaceAll("[- ]", "").replace("Administrator", "Admin") + "Role";

        return ObjectUtils.firstNonNull(
                specialRoleClasses.get(perm),
                getRoles().get(perm),
                getRoles().get(roleClassName),
                permissionClassPkg + roleClassName);
    }

    private Map<String, String> _roles;
    private Map<String, String> getRoles()
    {
        if (_roles == null)
        {
            _roles = new HashMap<>();
            GetRolesCommand command = new GetRolesCommand();
            Connection connection = _driver.createDefaultConnection(false);

            try
            {
                GetRolesResponse response = command.execute(connection, "/");
                List<GetRolesResponse.Role> roles = response.getRoles();
                for (GetRolesResponse.Role role : roles)
                {
                    String[] roleParts = role.getUniqueName().split("\\.");
                    _roles.put(roleParts[roleParts.length - 1], role.getUniqueName());
                    _roles.put(role.getName(), role.getUniqueName());
                }
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException(e);
            }
        }
        return _roles;
    }

    public abstract Integer createGlobalPermissionsGroup(String groupName, String... users);
    public abstract Integer createPermissionsGroup(String groupName);
    public abstract Integer createPermissionsGroup(String groupName, String... memberNames);
    public abstract void assertNoPermission(String userOrGroupName, String permissionSetting);
    public abstract void assertPermissionSetting(String userOrGroupName, String permissionSetting);
    public abstract void checkInheritedPermissions();
    public abstract void uncheckInheritedPermissions();
    public abstract boolean isPermissionsInherited();

    public void assertPermissionsInherited()
    {
        assertTrue("Permissions not inherited for folder: " + _driver.getCurrentContainerPath(), isPermissionsInherited());
    }

    public enum MemberType
    {user, group, siteGroup}

    @LogMethod
    public void setPermissions(@LoggedParam String groupName, @LoggedParam String roleClass)
    {
        addMemberToRole(groupName, roleClass, MemberType.group);
    }

    @LogMethod
    public void setSiteGroupPermissions(@LoggedParam String groupName, @LoggedParam String roleClass)
    {
        addMemberToRole(groupName, roleClass, MemberType.siteGroup);
    }

    @LogMethod
    public void setUserPermissions(@LoggedParam String userName, @LoggedParam String roleClass)
    {
        addMemberToRole(userName, roleClass, MemberType.user);
    }

    @LogMethod
    public abstract void setSiteAdminRoleUserPermissions(@LoggedParam String userName, @LoggedParam String permissionString);
    protected abstract void addMemberToRole(String userOrGroupName, String permissionString, MemberType memberType);

    public void removeSiteGroupPermission(String groupName, String permissionString)
    {
        removeRoleAssignment(groupName, permissionString, MemberType.siteGroup);
    }

    public void removePermission(String groupName, String permissionString)
    {
        removeRoleAssignment(groupName, permissionString, MemberType.group);
    }

    public void removeUserRole(String groupName, String permissionString)
    {
        removeRoleAssignment(groupName, permissionString, MemberType.user);
    }

    protected abstract void removeRoleAssignment(String groupName, String permissionString, MemberType memberType);
    public abstract void addUserToSiteGroup(String userName, String groupName);

    /**
     * Adds a new or existing user to an existing group within an existing project
     * @param userName new or existing user name
     * @param projectName existing project name
     * @param groupName existing group within the project to which we should add the user
     */
    public abstract void addUserToProjGroup(String userName, String projectName, String groupName);

    public void deleteGroup(String groupName)
    {
        deleteGroup(groupName, false);
    }

    @LogMethod(quiet = true)
    public abstract void deleteGroup(@LoggedParam String groupName, boolean failIfNotFound);

    public abstract void removeUserFromGroup(String groupName, String userName);
    public abstract void removeUserFromSiteGroup(String groupName, String userName);

    public abstract boolean doesGroupExist(String groupName, String projectName);

    public void assertGroupExists(String groupName, String projectName)
    {
        _driver.log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (!doesGroupExist(groupName, projectName))
            fail("group " + groupName + " does not exist in project " + projectName);
    }

    public void assertGroupDoesNotExist(String groupName, String projectName)
    {
        _driver.log("asserting that group " + groupName + " does not exist in project " + projectName + "...");
        if (doesGroupExist(groupName, projectName))
            fail("group " + groupName + " exists in project " + projectName);
    }

    public abstract boolean isUserInGroup(String user, String groupName, String projectName, PrincipalType principalType);

    public void assertUserInGroup(String member, String groupName, String projectName, PrincipalType principalType)
    {
        _driver.log("asserting that member " + member + " is in group " + projectName + "/" + groupName + "...");
        if (!isUserInGroup(member, groupName, projectName, principalType))
            fail("member " + member + " was not in group " + projectName + "/" + groupName);
    }

    public void assertUserNotInGroup(String member, String groupName, String projectName, PrincipalType principalType)
    {
        _driver.log("asserting that member " + member + " is not in group " + projectName + "/" + groupName + "...");
        if (isUserInGroup(member, groupName, projectName, principalType))
            fail("member " + member + " was found in group " + projectName + "/" + groupName);
    }
}
