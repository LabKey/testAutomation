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

import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.remoteapi.security.WhoAmIResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractUserHelper
{
    private WebDriverWrapper _driverWrapper;
    protected static final Map<String, String> usersAndDisplayNames = new HashMap<>();

    protected AbstractUserHelper(WebDriverWrapper driverWrapper)
    {
        _driverWrapper = driverWrapper;
    }

    public WebDriverWrapper getWrapper()
    {
        return _driverWrapper;
    }

    public void saveCurrentDisplayName()
    {
        saveCurrentDisplayName(getWrapper());
    }

    public static void saveCurrentDisplayName(WebDriverWrapper wrapper)
    {
        WhoAmIResponse whoAmIResponse = wrapper.whoAmI();
        usersAndDisplayNames.put(whoAmIResponse.getEmail(), whoAmIResponse.getDisplayName());
    }

    // assumes there are not collisions in the database causing unique numbers to be appended
    public String getDisplayNameForEmail(String email)
    {
        if (usersAndDisplayNames.containsKey(email))
            return usersAndDisplayNames.get(email);
        else
            return getDefaultDisplayName(email);
    }

    public static String getDefaultDisplayName(String email)
    {
        String display = email.contains("@") ? email.substring(0,email.indexOf('@')) : email;
        display = display.replace('_', ' ');
        display = display.replace('.', ' ');
        return display.trim();
    }

    @LogMethod
    public final String setInjectionDisplayName(@LoggedParam String email)
    {
        String newDisplayName = getDefaultDisplayName(email) +
                (WebTestHelper.RANDOM.nextBoolean() ? BaseWebDriverTest.INJECT_CHARS_1 : BaseWebDriverTest.INJECT_CHARS_2);
        setDisplayName(email, newDisplayName);
        return newDisplayName;
    }

    @LogMethod
    public final void setDisplayName(@LoggedParam String email, @LoggedParam String newDisplayName)
    {
        String previousDisplayName = usersAndDisplayNames.get(email);

        if (!newDisplayName.equals(previousDisplayName))
        {
            usersAndDisplayNames.remove(email); // Forget cached display name in case something goes wrong
            _setDisplayName(email, newDisplayName);
            usersAndDisplayNames.put(email, newDisplayName);
        }
    }

    protected abstract void _setDisplayName(String email, String newDisplayName);

    public CreateUserResponse createUser(String userName)
    {
        return createUser(userName, false, true);
    }

    public CreateUserResponse createUser(String userName, boolean verifySuccess)
    {
        return createUser(userName, false, verifySuccess);
    }

    public CreateUserResponse createUserAndNotify(String userName)
    {
        return createUser(userName, true, true);
    }

    public CreateUserResponse createUserAndNotify(String userName, boolean verifySuccess)
    {
        return createUser(userName, true, verifySuccess);
    }

    @LogMethod
    public final void deleteUser(@LoggedParam String userEmail)
    {
        usersAndDisplayNames.remove(userEmail);
        _deleteUser(userEmail);
    }

    @LogMethod
    public final void deleteUsers(boolean failIfNotFound, @LoggedParam String... userEmails)
    {
        for (String userEmail : userEmails)
        {
            usersAndDisplayNames.remove(userEmail);
        }
        _deleteUsers(failIfNotFound, userEmails);
    }

    public final void deleteUsers(boolean failIfNotFound, TestUser... users)
    {
        _deleteUsers(failIfNotFound, Arrays.stream(users).map(TestUser::getEmail).toArray(String[]::new));
    }

    public abstract void ensureUsersExist(List<String> userEmails);
    public abstract CreateUserResponse createUser(String userName, boolean sendEmail, boolean verifySuccess);
    protected abstract void _deleteUser(String userEmail);
    protected abstract void _deleteUsers(boolean failIfNotFound, String... userEmails);
}
