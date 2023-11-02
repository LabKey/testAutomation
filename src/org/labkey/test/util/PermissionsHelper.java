/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.GetRolesCommand;
import org.labkey.remoteapi.security.GetRolesResponse;
import org.labkey.test.WebTestHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * org.labkey.core.security.SecurityController#PermissionsAction
 */
public abstract class PermissionsHelper
{
    public static final String SITE_ADMIN_ROLE = "Site Administrator";
    public static final String APP_ADMIN_ROLE = "Application Admin";
    public static final String DEVELOPER_ROLE = "Platform Developer";
    public static final String IMP_TROUBLESHOOTER_ROLE = "Impersonating Troubleshooter";

    public static String toRole(final String name)
    {
        if (name.contains("."))
            return name;

        String roleClassName = name.replaceAll("[- ]", "").replace("Administrator", "Admin") + "Role";

        String role = ObjectUtils.firstNonNull(
                getRoles().get(name),
                getRoles().get(roleClassName));
        if (role == null)
            throw new RuntimeException("No role found matching '" + name + "'");

        return role;
    }

    private static Map<String, String> _roles;
    private static Map<String, String> getRoles()
    {
        if (_roles == null)
        {
            _roles = new HashMap<>();
            GetRolesCommand command = new GetRolesCommand();
            Connection connection = WebTestHelper.getRemoteApiConnection();

            try
            {
                GetRolesResponse response = command.execute(connection, "/");
                List<GetRolesResponse.Role> roles = response.getRoles();
                for (GetRolesResponse.Role role : roles)
                {
                    String uniqueName = role.getUniqueName();
                    String simpleRoleClassName = uniqueName.substring(uniqueName.lastIndexOf(".") + 1);
                    _roles.put(simpleRoleClassName, uniqueName);
                    _roles.put(role.getName(), uniqueName);
                }
                _roles.put("NoPermissionsRole", "org.labkey.api.security.roles.NoPermissionsRole");
                _roles.put("No Permissions", "org.labkey.api.security.roles.NoPermissionsRole");
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
    protected abstract Connection getConnection();

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
        TestLogger.log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (!doesGroupExist(groupName, projectName))
            fail("group " + groupName + " does not exist in project " + projectName);
    }

    public void assertGroupDoesNotExist(String groupName, String projectName)
    {
        TestLogger.log("asserting that group " + groupName + " does not exist in project " + projectName + "...");
        if (doesGroupExist(groupName, projectName))
            fail("group " + groupName + " exists in project " + projectName);
    }

    public abstract boolean isUserInGroup(String user, String groupName, String projectName, PrincipalType principalType);

    public void assertUserInGroup(String member, String groupName, String projectName, PrincipalType principalType)
    {
        TestLogger.log("asserting that member " + member + " is in group " + projectName + "/" + groupName + "...");
        if (!isUserInGroup(member, groupName, projectName, principalType))
            fail("member " + member + " was not in group " + projectName + "/" + groupName);
    }

    public void assertUserNotInGroup(String member, String groupName, String projectName, PrincipalType principalType)
    {
        TestLogger.log("asserting that member " + member + " is not in group " + projectName + "/" + groupName + "...");
        if (isUserInGroup(member, groupName, projectName, principalType))
            fail("member " + member + " was found in group " + projectName + "/" + groupName);
    }

    /**
     * Duplicated from org.labkey.api.security.PrincipalType
     */
    public enum PrincipalType
    {
        USER('u', "User"),
        GROUP('g', "Group"),
        ROLE('r', "Role"),
        MODULE('m', "Module Group"),
        SERVICE('s', "Service");

        private final char _typeChar;
        private final String _description;

        PrincipalType(char type, String description)
        {
            _typeChar = type;
            _description = description;
        }

        public char getTypeChar()
        {
            return _typeChar;
        }

        public String getDescription()
        {
            return _description;
        }

        public static PrincipalType forChar(char type)
        {
            switch (type)
            {
                case 'u': return USER;
                case 'g': return GROUP;
                case 'r': return ROLE;
                case 'm': return MODULE;
                case 's': return SERVICE;
                default : return null;
            }
        }
    }
}
