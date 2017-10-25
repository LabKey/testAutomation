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
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

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
        getWrapper().goToSiteUsers();
        getWrapper().clickButton("Add Users");

        getWrapper().setFormElement(Locator.name("newUsers"), userName);
        getWrapper().setCheckbox(Locator.checkboxByName("sendMail").findElement(getWrapper().getDriver()), sendEmail);
        if (cloneUserName != null)
        {
            getWrapper().checkCheckbox(Locator.id("cloneUserCheck"));
            getWrapper().setFormElement(Locator.name("cloneUser"), cloneUserName);
        }
        getWrapper().clickButton("Add Users");

        if (verifySuccess)
            assertTrue("Failed to add user " + userName, getWrapper().isTextPresent(userName + " added as a new user to the system"));

        WebElement resultEl = Locator.css(".labkey-error, .labkey-message").findElement(getWrapper().getDriver());
        String message = resultEl.getText();

        String email;
        Number userId;
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

        CreateUserResponse fakeResponse = new CreateUserResponse(null, 200, null, null, null){
            @Override
            public Number getUserId()
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
        return fakeResponse;
    }

    public CreateUserResponse cloneUser(String userName, String cloneUserName)
    {
        return cloneUser(userName, cloneUserName, true, true);
    }

    @Override
    public CreateUserResponse createUser(String userName, boolean sendEmail, boolean verifySuccess)
    {
        return cloneUser(userName, null, sendEmail, verifySuccess);
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
        getWrapper().beginAt("user/showUsers.view?inactive=true&Users.showRows=all");

        DataRegionTable usersTable = new DataRegionTable("Users", getWrapper().getDriver());

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
}
