/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class AbstractUserHelper
{
    private WebDriverWrapper _driverWrapper;
    protected static final Map<String, String> usersAndDisplayNames = new HashMap<>();

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
        usersAndDisplayNames.put(wrapper.getCurrentUser(), wrapper.getCurrentUserName());
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
    public void setDisplayName(@LoggedParam String email, @LoggedParam String newDisplayName)
    {
        String previousDisplayName = usersAndDisplayNames.get(email);

        if (!newDisplayName.equals(previousDisplayName))
        {
            usersAndDisplayNames.remove(email); // Forget cached display name in case something goes wrong
            getWrapper().goToSiteUsers();

            DataRegionTable users = new DataRegionTable("Users", getWrapper().getDriver());
            users.setFilter("Email", "Equals", email);
            int userRow = users.getRowIndex("Email", email);
            assertFalse("No such user: " + email, userRow == -1);
            getWrapper().clickAndWait(users.detailsLink(userRow));

            getWrapper().clickButton("Edit");
            assertEquals("Editing details for wrong user.",
                    email, Locator.tagWithClass("ol", "breadcrumb").parent().childTag("h3").findElement(getWrapper().getDriver()).getText());
            getWrapper().setFormElement(Locator.name("quf_DisplayName"), newDisplayName);
            getWrapper().clickButton("Submit");
            usersAndDisplayNames.put(email, newDisplayName);
        }
    }

    public AbstractUserHelper(WebDriverWrapper driverWrapper)
    {
        _driverWrapper = driverWrapper;
    }

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

    public abstract CreateUserResponse createUser(String userName, boolean sendEmail, boolean verifySuccess);
    protected abstract void _deleteUser(String userEmail);
    protected abstract void _deleteUsers(boolean failIfNotFound, String... userEmails);
}
