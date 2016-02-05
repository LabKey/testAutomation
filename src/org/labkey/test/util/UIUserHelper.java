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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UIUserHelper extends AbstractUserHelper
{
    public UIUserHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    public CreateUserResponse createUser(String userName, boolean sendEmail, boolean verifySuccess)
    {
        _test.goToSiteUsers();
        _test.clickButton("Add Users");

        _test.setFormElement(Locator.name("newUsers"), userName);
        _test.setCheckbox(Locator.checkboxByName("sendMail").findElement(_test.getDriver()), sendEmail);
        //            if (cloneUserName != null)
//            {
//                checkCheckbox("cloneUserCheck");
//                setFormElement("cloneUser", cloneUserName);
//            }
        _test.clickButton("Add Users");

        if (verifySuccess)
            assertTrue("Failed to add user " + userName, _test.isTextPresent(userName + " added as a new user to the system"));

        WebElement resultEl = Locator.css(".labkey-error, .labkey-message").findElement(_test.getDriver());
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
}
