/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateUserCommand;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.remoteapi.security.DeleteUserCommand;
import org.labkey.remoteapi.security.GetUsersCommand;
import org.labkey.remoteapi.security.GetUsersResponse;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.user.UpdateUserDetailsPage;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class APIUserHelper extends AbstractUserHelper
{
    public APIUserHelper(WebDriverWrapper driver)
    {
        super(driver);
    }

    @Override
    public String getDisplayNameForEmail(@NotNull String email)
    {
        GetUsersResponse users = getUsers(true);
        Optional<GetUsersResponse.UserInfo> user = users.getUsersInfo().stream()
                .filter(userInfo -> email.equals(userInfo.getEmail())).findFirst();
        if (user.isPresent())
        {
            return user.get().getDisplayName();
        }
        else
        {
            return super.getDisplayNameForEmail(email);
        }
    }

    @Override
    public void _setDisplayName(String email, String newDisplayName)
    {
        final Integer userId = getUserId(email);

        if (userId == null)
        {
            throw new IllegalArgumentException("No such user: " + email);
        }

        // TODO: Update via API
        final UpdateUserDetailsPage updateUserDetailsPage = UpdateUserDetailsPage.beginAt(getWrapper(), userId);
        updateUserDetailsPage.setDisplayName(newDisplayName);
        updateUserDetailsPage.clickSubmit();
    }

    @Override
    public void ensureUsersExist(List<String> userEmails)
    {
        Map<String, Integer> existingUsers = getUserIds(userEmails, true);
        for (String email : userEmails)
        {
            if (!existingUsers.containsKey(email))
            {
                createUser(email);
            }
        }
    }

    @Override
    public CreateUserResponse createUser(final String userName, final boolean sendEmail, final boolean verifySuccess)
    {
        CreateUserCommand command = new CreateUserCommand(userName)
        {
            @Override
            public JSONObject getJsonObject()
            {
                JSONObject jsonObject = super.getJsonObject();
                if (!sendEmail)
                {
                    // Make sure new account notification still works without this flag
                    jsonObject.put("skipFirstLogin", true);
                }
                return jsonObject;
            }
        };
        command.setSendEmail(sendEmail);
        Connection connection = getWrapper().createDefaultConnection();
        try
        {
            CreateUserResponse response = command.execute(connection, "");

            if (verifySuccess)
            {
                if(response.getMessage() == null)
                {
                    TestLogger.error(response.getParsedData().get("htmlErrors").toString());
                    Assert.fail("Not able to create the user " + userName + " because " + response.getParsedData().get("htmlErrors").toString());
                }
                assertEquals(userName, response.getEmail());
                assertTrue("Invalid userId", response.getUserId() != null);
            }

            return response;
        }
        catch (CommandException | IOException e)
        {
            if(verifySuccess)
                throw new RuntimeException("Error while creating user", e);
            return null;
        }
    }

    public GetUsersResponse getUsers()
    {
        return getUsers(false);
    }

    public GetUsersResponse getUsers(boolean includeDeactivated)
    {
        GetUsersCommand command = new GetUsersCommand();
        command.setIncludeDeactivated(includeDeactivated);
        Connection connection = getWrapper().createDefaultConnection();
        if (getWrapper().isImpersonating())
        {
            // Don't use browser session. Tests often call 'getDisplayNameForEmail' while impersonating non-admins.
            connection = WebTestHelper.getRemoteApiConnection(false);
        }

        try
        {
            return command.execute(connection, "/");
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> getUserIds(List<String> userEmails)
    {
        return getUserIds(userEmails, true);
    }

    public Map<String, Integer> getUserIds(List<String> userEmails, boolean includeDeactivated)
    {
        Map<String, Integer> userIds = new HashMap<>();
        List<GetUsersResponse.UserInfo> usersInfo = getUsers(includeDeactivated).getUsersInfo();
        for (GetUsersResponse.UserInfo userInfo : usersInfo)
        {
            if (userEmails.contains(userInfo.getEmail()))
                userIds.put(userInfo.getEmail(), userInfo.getUserId());
        }
        return userIds;
    }

    @Nullable
    public Integer getUserId(String userEmail)
    {
        return getUserIds(Arrays.asList(userEmail)).get(userEmail);
    }

    @Override
    protected void _deleteUser(String userEmail)
    {
        deleteUsers(false, userEmail);
    }

    private void deleteUser(@NotNull Integer userId)
    {
        Connection connection = getWrapper().createDefaultConnection();
        DeleteUserCommand command = new DeleteUserCommand(userId);
        try
        {
            command.execute(connection, "/");
        }
        catch (IOException|CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void _deleteUsers(boolean failIfNotFound, String... userEmails)
    {
        Map<String, Integer> userIds = getUserIds(Arrays.asList(userEmails), true);
        for (String userEmail : new HashSet<>(Arrays.asList(userEmails)))
        {
            Integer userId = userIds.get(userEmail);
            if (failIfNotFound)
                assertTrue(userEmail + " was not present", userId != null);
            if (userId != null)
                deleteUser(userId);
        }
    }
}
