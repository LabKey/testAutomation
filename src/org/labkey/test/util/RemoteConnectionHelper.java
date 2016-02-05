/*
 * Copyright (c) 2013-2016 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.tests.RemoteConnectionTest;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RemoteConnectionHelper
{
    protected BaseWebDriverTest _test;

    // If true then this helper won't navigate back and forth between the
    // remote connection management page and the project folder.  Right now
    // only used for the RemoteConnectionTest
    private boolean _navToProjectFolder = true;

    public RemoteConnectionHelper(BaseWebDriverTest test)
    {
        _test = test;
        if (test instanceof RemoteConnectionTest)
            _navToProjectFolder = false;
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
        boolean success = false;
        _goToManageRemoteConnections();
        WebElement target = findConnection(name, "test");
        if (null != target)
        {
            _test.clickAndWait(target, _test.getDefaultWaitForPage());
            success = _test.isTextPresent("not successful") ? false: true;
            _test.clickAndWait(Locator.linkWithText("Manage Remote Connections"));
        }
        _goToProjectHome();
        return success;
    }

    public void createConnection(String name, String url, String container, String user, String password, String expectedError)
    {
        _goToManageRemoteConnections();
        _test.clickAndWait(Locator.linkWithText("Create New Connection"));
        setConnectionProperties(name, url, container, user, password);
        _test.clickButton("save");
        verifyExpectedError(expectedError);
        _goToProjectHome();
    }
    public void editConnection(String name, String newName, String newUrl, String newContainer, String newUser, String newPassword)
    {
        editConnection(name, newName, newUrl, newContainer, newUser, newPassword, null);
    }

    // edit properties of the connection.  if the parameter passed in is null then the old value is used
    public void editConnection(String name, String newName, String newUrl, String newContainer, String newUser, String newPassword, String expectedError)
    {
        _goToManageRemoteConnections();
        WebElement target = findConnection(name, "edit");
        if (null != target)
        {
            _test.clickAndWait(target, _test.getDefaultWaitForPage());
            setConnectionProperties(newName, newUrl, newContainer, newUser, newPassword);
            _test.clickButton("save");
            verifyExpectedError(expectedError);
        }
        _goToProjectHome();
    }

    public void deleteConnection(String name)
    {
        _goToManageRemoteConnections();
        WebElement target = findConnection(name, "delete");
        if (null != target)
        {
            _test.clickAndWait(target, _test.getDefaultWaitForPage());
            _test.assertTextPresent(name);
            _test.clickButton("delete");
        }
        _goToProjectHome();
    }

    public void goToManageRemoteConnections()
    {
        _test.goToSchemaBrowser();
        _test.click(Ext4Helper.Locators.ext4ButtonContainingText("Manage Remote Connections"));
    }

    public int getNumConnections()
    {
        _goToManageRemoteConnections();
        List<WebElement> items = Locator.xpath("//a[contains(text(), 'edit')]").findElements(_test.getDriver());
        _goToProjectHome();
        return items.size();
    }

    public WebElement findConnection(String name)
    {
        _goToManageRemoteConnections();
        WebElement elt =  findConnection(name, "edit");
        _goToProjectHome();
        return elt;
    }

    private void _goToManageRemoteConnections()
    {
        if (_navToProjectFolder)
            goToManageRemoteConnections();
    }

    private void _goToProjectHome()
    {
        if (_navToProjectFolder)
            _test.goToProjectHome();
    }

    private void setConnectionProperties(String name, String url, String container, String user, String password)
    {
        if (null != name)
        _test.setFormElement(Locator.name("newConnectionName"), name);

        if (null != url)
        _test.setFormElement(Locator.name("url"), url);

        if (null != user)
        _test.setFormElement(Locator.name("user"), user);

        if (null != password)
        _test.setFormElement(Locator.name("password"), password);

        if (null != container)
        _test.setFormElement(Locator.name("container"), container);
    }

    private void verifyExpectedError(String expectedError)
    {
        if (null != expectedError)
        {
            _test.assertTextPresent(expectedError);
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
