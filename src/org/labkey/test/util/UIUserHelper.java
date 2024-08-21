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

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.core.login.SetPasswordForm;
import org.labkey.test.pages.security.AddUsersPage;
import org.labkey.test.pages.user.ShowUsersPage;
import org.labkey.test.pages.user.UpdateUserDetailsPage;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UIUserHelper extends AbstractUserHelper
{
    public UIUserHelper(WebDriverWrapper driverWrapper)
    {
        super(driverWrapper);
    }

    @LogMethod
    public CreateUserResponse cloneUser(@LoggedParam String userName, String cloneUserName, boolean sendEmail, boolean verifySuccess)
    {
        createUsers(Arrays.asList(userName), cloneUserName, sendEmail, getWrapper().goToSiteUsers());

        if (verifySuccess)
        {
            assertTrue("Failed to add user " + userName,
                    Locator.byClass("labkey-message").containing(userName).existsIn(getWrapper().getDriver()));
        }

        WebElement resultEl = Locator.css(".labkey-error, .labkey-message").findElement(getWrapper().getDriver());
        String message = resultEl.getText();

        String email;
        Integer userId;
        List<WebElement> userInfo = Locator.css("meta").findElements(resultEl);
        if (userInfo.size() > 0)
        {
            email = userInfo.get(0).getAttribute("email");
            String userIdStr = userInfo.get(0).getAttribute("userId");
            userId = userIdStr == null ? null : Integer.parseInt(userIdStr);
        }
        else
        {
            email = null;
            userId = null;
        }

        return new CreateUserResponse(null, 200, null, null){
            @Override
            public Integer getUserId()
            {
                return userId;
            }

            @Override
            public String getEmail()
            {
                return email;
            }

            @Override
            public String getMessage()
            {
                return message;
            }
        };
    }

    @LogMethod
    private AddUsersPage createUsers(@LoggedParam List<String> userNames, @Nullable String cloneUserName, boolean sendEmail, ShowUsersPage showUsersPage)
    {
        return showUsersPage
                .clickAddUsers()
                .setNewUsers(userNames)
                .setSendNotification(sendEmail)
                .setClonedUser(cloneUserName)
                .clickAddUsers();
    }

    public CreateUserResponse cloneUser(String userName, String cloneUserName)
    {
        return cloneUser(userName, cloneUserName, true, true);
    }

    @Override
    public void ensureUsersExist(List<String> userEmails)
    {
        ShowUsersPage showUsersPage = ShowUsersPage.beginAt(getWrapper(), true);
        DataRegionTable usersTable = showUsersPage.getUsersTable();

        List<String> existingUsers = usersTable.getColumnDataAsText("Email");
        List<String> usersToCreate = userEmails.stream().filter(o -> !existingUsers.contains(o)).collect(Collectors.toList());
        createUsers(usersToCreate, null, false, showUsersPage);

        getWrapper().assertTextPresent(usersToCreate);
        getWrapper().assertElementNotPresent(Locators.labkeyError);
    }

    @Override
    public CreateUserResponse createUser(String userName, boolean sendEmail, boolean verifySuccess)
    {
        return cloneUser(userName, null, sendEmail, verifySuccess);
    }

    @Override
    protected void _setDisplayName(String email, String newDisplayName)
    {
        DataRegionTable users = getWrapper().goToSiteUsers().getUsersTable();

        users.setFilter("Email", "Equals", email);
        int userRow = users.getRowIndex("Email", email);
        assertFalse("No such user: " + email, userRow == -1);
        getWrapper().clickAndWait(users.detailsLink(userRow));

        getWrapper().clickButton("Edit");
        assertEquals("Editing details for wrong user.",
                email, Locator.tagWithClass("ol", "breadcrumb").parent().childTag("h3").findElement(getWrapper().getDriver()).getText());

        final UpdateUserDetailsPage updateUserDetailsPage = new UpdateUserDetailsPage(getWrapper().getDriver());
        updateUserDetailsPage.setDisplayName(newDisplayName);
        updateUserDetailsPage.clickSubmit();
    }

    @Override
    protected void _deleteUser(String userEmail)
    {
        _deleteUsers(false, userEmail);
    }

    @Override
    protected void _deleteUsers(boolean failIfNotFound, String... userEmails)
    {
        int checked = 0;
        List<String> displayNames = new ArrayList<>();
        DataRegionTable usersTable = ShowUsersPage.beginAt(getWrapper(), true).getUsersTable();

        for(String userEmail : userEmails)
        {
            int row = usersTable.getRowIndex("Email", userEmail);

            boolean isPresent = row != -1;

            if (failIfNotFound)
                assertTrue(userEmail + " was not present", isPresent);
            else if (!isPresent)
                TestLogger.log("Unable to delete non-existent user: " + userEmail);

            if (isPresent)
            {
                usersTable.checkCheckbox(row);
                checked++;
                displayNames.add(usersTable.getDataAsText(row, "Display Name"));
            }
        }

        if(checked > 0)
        {
            getWrapper().clickButton("Delete");
            getWrapper().assertTextPresent(displayNames);
            getWrapper().clickButton("Permanently Delete");
            getWrapper().assertTextNotPresent(userEmails);
        }
    }

    public void deactivateUser(String userEmail)
    {
        DataRegionTable usersTable = getWrapper().goToSiteUsers().getUsersTable();
        usersTable.setFilter("Email", "Equals", userEmail);
        int row = usersTable.getRowIndex("Email", userEmail);
        usersTable.checkCheckbox(row);
        getWrapper().clickButton("Deactivate");
        getWrapper().clickButton("Deactivate");
        getWrapper().assertTextNotPresent(userEmail);
    }

    @Override
    public String setInitialPassword(String user)
    {
        String password = PasswordUtil.getPassword();
        SetPasswordForm.goToInitialPasswordForUser(getWrapper(), user)
                .setNewPassword(password)
                .clickSubmit();

        return password;
    }
}
