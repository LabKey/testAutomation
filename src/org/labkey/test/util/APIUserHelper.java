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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.security.CreateUserCommand;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.remoteapi.security.GetUsersCommand;
import org.labkey.remoteapi.security.GetUsersResponse;
import org.labkey.remoteapi.security.GetUsersResponse.UserInfo;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.query.QueryApiHelper;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class APIUserHelper extends AbstractUserHelper
{
    private final Supplier<Connection> connectionSupplier;

    public APIUserHelper(Supplier<Connection> connectionSupplier)
    {
        super(null);
        this.connectionSupplier = connectionSupplier;
    }

    public APIUserHelper(WebDriverWrapper driver)
    {
        super(driver);
        connectionSupplier = driver::createDefaultConnection;
    }

    @Override
    public String getDisplayNameForEmail(@NotNull String email)
    {
        Map<String, String> displayNames = getDisplayNames();
        if (displayNames.containsKey(email))
        {
            return displayNames.get(email);
        }
        else
        {
            return super.getDisplayNameForEmail(email);
        }
    }

    public Map<String, String> getDisplayNames()
    {
        GetUsersResponse users = getUsers(true);
        return users.getUsersInfo().stream().collect(Collectors.toMap(UserInfo::getEmail, UserInfo::getDisplayName));
    }

    @Override
    public void _setDisplayName(String email, String newDisplayName)
    {
        final Integer userId = getUserId(email);

        if (userId == null)
        {
            throw new IllegalArgumentException("No such user: " + email);
        }

        try
        {
            new QueryApiHelper(connectionSupplier.get(), "/", "core", "siteusers")
                    .updateRows(List.of(Maps.of("userId", userId, "DisplayName", newDisplayName)));
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to set display name for user:" + email, e);
        }
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
        Connection connection = connectionSupplier.get();
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
                assertNotNull("Invalid userId", response.getUserId());
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

    public GetUsersResponse getUsers(boolean includeInactive)
    {
        GetUsersCommand command = new GetUsersCommand();
        command.setIncludeInactive(includeInactive);
        Connection connection = connectionSupplier.get();
        if (getWrapper() != null && getWrapper().isImpersonating())
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

    public Map<String, Integer> getUserIds(List<String> userEmails, boolean includeInactive)
    {
        Map<String, Integer> userIds = new HashMap<>();
        List<UserInfo> usersInfo = getUsers(includeInactive).getUsersInfo();
        for (UserInfo userInfo : usersInfo)
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

    public int getUserIdStrict(String userEmail)
    {
        Integer userId = getUserId(userEmail);
        if (userId == null)
            throw new IllegalStateException("No user with email " + userEmail + " found.");
        return userId;
    }

    @Override
    protected void _deleteUser(String userEmail)
    {
        deleteUsers(false, userEmail);
    }

    @Override
    protected void _deleteUsers(boolean failIfNotFound, String... userEmails)
    {
        Map<String, Integer> userIds = getUserIds(Arrays.asList(userEmails), true);
        List<Integer> validUserIds = new ArrayList<>();
        for (String userEmail : new HashSet<>(Arrays.asList(userEmails)))
        {
            Integer userId = userIds.get(userEmail);
            if (failIfNotFound)
                assertNotNull(userEmail + " was not present", userId);
            if (userId != null)
                validUserIds.add(userId);
        }
        if (!validUserIds.isEmpty())
            deleteUsers(ArrayUtils.toPrimitive(validUserIds.toArray(new Integer[0])));
    }

    private static final Pattern regEmailVerification = Pattern.compile("verification=([A-Za-z0-9]+)");

    public String setInitialPassword(String email)
    {
        return setInitialPassword(getUserIdStrict(email));
    }

    @Override
    public String setInitialPassword(int userId)
    {
        String verification;

        try
        {
            SimpleGetCommand getRegEmailCommand = new SimpleGetCommand("security", "showRegistrationEmail");
            getRegEmailCommand.setParameters(Map.of("userId", userId));

            String responseText = getRegEmailCommand.execute(connectionSupplier.get(), null).getText();
            Matcher matcher = regEmailVerification.matcher(responseText);

            if (matcher.find())
            {
                verification = matcher.group(1);
            }
            else
            {
                throw new IllegalStateException("No verification code found:\n" + responseText);
            }
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to fetch new user registration email.", e);
        }

        try
        {
            String password = PasswordUtil.getPassword();
            new SetPasswordCommand(password, verification).execute(connectionSupplier.get(), null);
            return password;
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void deleteUsers(int... userIds)
    {
        updateUsersState(false, true, userIds);
    }

    public void deactivateUsers(int... userIds)
    {
        updateUsersState(false, false, userIds);
    }

    public void activateUsers(int... userIds)
    {
        updateUsersState(true, false, userIds);
    }

    private void updateUsersState(boolean activate, boolean delete, int... userIds)
    {
        SimplePostCommand updateUsers = new SimplePostCommand("user", "updateUsersStateApi");
        updateUsers.setJsonObject(new JSONObject(Map.of("userId", userIds, "activate", activate, "delete", delete)));
        try
        {
            updateUsers.execute(connectionSupplier.get(), null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }
}

class SetPasswordCommand extends PostCommand<CommandResponse>
{
    private final String _password;
    private final String _verification;

    public SetPasswordCommand(String password, String verification)
    {
        super("login", "setPassword");
        _password = password;
        _verification = verification;
    }

    protected List<BasicNameValuePair> getPostData()
    {
        List<BasicNameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("password", _password));
        postData.add(new BasicNameValuePair("password2", _password));
        postData.add(new BasicNameValuePair("verification", _verification));

        return postData;
    }

    @Override
    protected HttpPost createRequest(URI uri)
    {
        // SetPasswordAction is not a real API action, so we POST form data instead of JSON
        HttpPost request = new HttpPost(uri);
        request.setEntity(new UrlEncodedFormEntity(getPostData()));
        return request;
    }
}
