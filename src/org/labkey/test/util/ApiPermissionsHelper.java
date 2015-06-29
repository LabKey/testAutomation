package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.api.security.PrincipalType;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.GetGroupPermsCommand;
import org.labkey.remoteapi.security.GetGroupPermsResponse;
import org.labkey.test.BaseWebDriverTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiPermissionsHelper extends PermissionsHelper
{
    public ApiPermissionsHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    public void assertNoPermission(String userOrGroupName, String permissionSetting)
    {
        String container = _test.getCurrentContainerPath();
        List<String> roles = new ArrayList<>();
        if (userOrGroupName.contains("@"))
            roles.addAll(getUserRoles(container, userOrGroupName));
        else
            roles.addAll(getGroupRoles(container, userOrGroupName));

        Assert.assertFalse(String.format("%s had role: %s\nFound: %s", userOrGroupName, permissionSetting, String.join("\n", roles)),
                roles.contains(toRole(permissionSetting)));
    }

    @Override
    public void assertPermissionSetting(String userOrGroupName, String permissionSetting)
    {
        String container = _test.getCurrentContainerPath();
        String expectedRole = toRole(permissionSetting);
        List<String> roles = new ArrayList<>();
        if (userOrGroupName.contains("@"))
            roles.addAll(getUserRoles(container, userOrGroupName));
        else
            roles.addAll(getGroupRoles(container, userOrGroupName));

        Assert.assertTrue(String.format("%s did not have role: %s\nFound: %s", userOrGroupName, permissionSetting, String.join("\n", roles)),
                roles.contains(expectedRole) || "No Permissions".equals(permissionSetting) && roles.size() > 0);
    }

    public List<String> getGroupRoles(String container, String groupName)
    {
        List<Map<String, Object>> groups = getGroups(container);
        List<String> roles = new ArrayList<>();

        for (Map<String, Object> group : groups)
        {
            if (group.get("name").equals(groupName))
                roles.addAll((List<String>) group.get("roles"));
        }

        return roles;
    }

    @Override
    public boolean doesGroupExist(String groupName, String container)
    {
        List<Map<String, Object>> groups = getGroups(container);

        for (Map<String, Object> group : groups)
        {
            if (groupName.equals(group.get("name")))
                return true;
        }

        return false;
    }

    @Override
    public boolean isUserInGroup(String userOrGroup, String groupName, String container, PrincipalType principalType)
    {
        List<Map<String, Object>> groups = new ArrayList<>();
        if (principalType == PrincipalType.USER)
        {
            try
            {
                groups = getUserGroups(container, userOrGroup);
            }
            catch (CommandException ignore)
            {
                // User not found
            }
        }
        else if (principalType == PrincipalType.GROUP)
        {
            for(Map<String, Object> group : getGroups(container))
            {
                if (groupName.equals(group.get("name")))
                {
                    groups = (List)group.get("groups");
                    break;
                }
            }
        }
        else
            throw new IllegalArgumentException(principalType.toString() + " not supported");

        for (Map<String, Object> group : groups)
        {
            if (groupName.equals(group.get("name")))
                return true;
        }

        return false;
//        List<String> groupMembers = getGroupMembers(container, groupName);
//
//        return groupMembers.contains(user);
    }

    private List<Map<String, Object>> getGroups(String container)
    {
        Connection connection = _test.createDefaultConnection(false);
        GetGroupPermsCommand command = new GetGroupPermsCommand();
        GetGroupPermsResponse response;
        try
        {
            response = command.execute(connection, container);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        return (List)((Map)response.getParsedData().get("container")).get("groups");
    }

    private List<String> getGroupMembers(String container, String groupName)
    {
        Long groupId = null;
        List<Map<String, Object>> groups = getGroups(container);
        for (Map<String, Object> group : groups)
        {
            if (groupName.equals(group.get("name")))
            {
                groupId = (Long)group.get("id");
                break;
            }
        }

        Connection connection = _test.createDefaultConnection(false);
        Command command = new Command("security", "getGroupMembers");
        command.setParameters(new HashMap<String, Object>(Maps.of("groupId", groupId)));

        CommandResponse response;
        try
        {
            response = command.execute(connection, container);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        return (List)response.getParsedData().get("groupMembers");
    }

    private List<Map<String, Object>> getUserGroups(String container, String user) throws CommandException
    {
        return (List)getUserPerms(container, user).get("groups");
    }

    private List<String> getUserRoles(String container, String user)
    {
        try
        {
            return (List<String>)getUserPerms(container, user).get("roles");
        }
        catch (CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> getUserPerms(String container, String user) throws CommandException
    {
        Connection connection = _test.createDefaultConnection(false);
        Command command = new Command("security", "getUserPerms");
        command.setParameters(new HashMap<String, Object>(Maps.of("userEmail", user)));

        CommandResponse response;
        try
        {
            response = command.execute(connection, container);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return (Map) response.getParsedData().get("container");
    }
}
