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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RemoteConnectionHelper
{
    protected BaseWebDriverTest _test;

    public RemoteConnectionHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void createConnection(String name, String url, String container)
    {
        createConnection(name, url, container, PasswordUtil.getUsername(), PasswordUtil.getPassword(), null);
    }

    public void createConnection(String name, String url, String container, String user, String password)
    {
        createConnection(name, url, container, user, password, null);
    }

    public boolean testConnection(String name)
    {
        boolean success;
        WebElement target = findConnectionStrict(name, "test");
        _test.clickAndWait(target, _test.getDefaultWaitForPage());
        success = !_test.isTextPresent("not successful");
        _test.clickAndWait(Locator.linkWithText("Manage Remote Connections"));
        return success;
    }

    public void createConnection(String name, String url, String container, String user, String password, String expectedError)
    {
        _test.clickAndWait(Locator.linkWithText("Create New Connection"));
        setConnectionProperties(name, url, container, user, password);
        _test.clickButton("save");
        verifyExpectedError(expectedError);
    }

    public void editConnection(String name, String newName, String newUrl, String newContainer, String newUser, String newPassword)
    {
        editConnection(name, newName, newUrl, newContainer, newUser, newPassword, null);
    }

    // edit properties of the connection.  if the parameter passed in is null then the old value is used
    public void editConnection(String name, String newName, String newUrl, String newContainer, String newUser, String newPassword, String expectedError)
    {
        WebElement target = findConnectionStrict(name, "edit");

        _test.clickAndWait(target, _test.getDefaultWaitForPage());
        setConnectionProperties(newName, newUrl, newContainer, newUser, newPassword);
        _test.clickButton("save");
        verifyExpectedError(expectedError);
    }

    public void deleteConnection(String name)
    {
        WebElement target = findConnection(name, "delete");
        if (target != null)
        {
            _test.clickAndWait(target, _test.getDefaultWaitForPage());
            _test.assertTextPresent(name);
            _test.clickButton("delete");
        }
    }

    public void goToManageRemoteConnections()
    {
        _test.goToSchemaBrowser();
        _test.click(Ext4Helper.Locators.ext4ButtonContainingText("Manage Remote Connections"));
    }

    public int getNumConnections()
    {
        List<WebElement> items = Locator.xpath("//a[contains(text(), 'edit')]").findElements(_test.getDriver());
        return items.size();
    }

    public WebElement findConnection(String name)
    {
        return findConnection(name, "edit");
    }

    public WebElement findConnectionStrict(String name, String action)
    {
        WebElement link = findConnection(name, action);
        if (link == null)
        {
            throw new NoSuchElementException("Connection not found: " + name);
        }
        return link;
    }

    private void setConnectionProperties(String name, String url, String container, String user, String password)
    {
        if (null != name)
        _test.setFormElement(Locator.name("newConnectionName"), name);

        if (null != url)
        _test.setFormElement(Locator.name("url"), url);

        if (null != user)
        _test.setFormElement(Locator.name("userEmail"), user);

        if (null != password)
        _test.setFormElement(Locator.name("password"), password);

        if (null != container)
        _test.setFormElement(Locator.name("folderPath"), container);
    }

    private void verifyExpectedError(String expectedError)
    {
        if (null != expectedError)
        {
            String error = Locators.labkeyError.findOptionalElement(_test.getDriver()).map(WebElement::getText).orElse(null);
            Assert.assertEquals("Remote connection error.", expectedError, error);
            _test.clickButton("cancel");
        }
    }

    private WebElement findConnection(String name, String action)
    {
        List<WebElement> items = Locator.xpath("//a[contains(text(), '" + action + "')]").findElements(_test.getDriver());
        for (WebElement elt : items)
        {
            if (elt.getAttribute("href").contains(name))
                return elt;
        }

        return null;
    }
}
