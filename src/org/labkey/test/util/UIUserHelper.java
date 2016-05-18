/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

    @Override
    public CreateUserResponse createUser(String userName, boolean sendEmail, boolean verifySuccess)
    {
        _driver.goToSiteUsers();
        _driver.clickButton("Add Users");

        _driver.setFormElement(Locator.name("newUsers"), userName);
        _driver.setCheckbox(Locator.checkboxByName("sendMail").findElement(_driver.getDriver()), sendEmail);
        //            if (cloneUserName != null)
//            {
//                checkCheckbox("cloneUserCheck");
//                setFormElement("cloneUser", cloneUserName);
//            }
        _driver.clickButton("Add Users");

        if (verifySuccess)
            assertTrue("Failed to add user " + userName, _driver.isTextPresent(userName + " added as a new user to the system"));

        WebElement resultEl = Locator.css(".labkey-error, .labkey-message").findElement(_driver.getDriver());
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

    @Override
    public void deleteUser(String userEmail)
    {
        deleteUsers(false, userEmail);
    }

    @LogMethod
    public void deleteUsers(boolean failIfNotFound, @LoggedParam String... userEmails)
    {
        int checked = 0;
        List<String> displayNames = new ArrayList<>();
        _driver.beginAt("user/showUsers.view?inactive=true&Users.showRows=all");

        DataRegionTable usersTable = new DataRegionTable("Users", _driver.getDriver());

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
            _driver.clickButton("Delete");
            _driver.assertTextPresent(displayNames);
            _driver.clickButton("Permanently Delete");
            _driver.assertTextNotPresent(userEmails);
        }
    }
}
