/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.pages.user;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.security.AddUsersPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ShowUsersPage extends LabKeyPage<ShowUsersPage.ElementCache>
{
    private static final Locator hideInactiveUsersLoc = Locator.linkWithText("hide inactive users");
    private static final Locator showInactiveUsersLoc = Locator.linkWithText("include inactive users");

    public ShowUsersPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowUsersPage beginAt(WebDriverWrapper driver, boolean showAll)
    {
        Map<String, String> params = new HashMap<>();
        if (showAll)
        {
            params.put("inactive", "true");
            params.put("Users.showRows", "all");
        }
        driver.beginAt(WebTestHelper.buildURL("user", "showUsers", params));
        return new ShowUsersPage(driver.getDriver());
    }

    public DataRegionTable getUsersTable()
    {
        return elementCache().usersTable;
    }

    public DomainDesignerPage clickChangeUserProperties()
    {
        getUsersTable().clickHeaderButtonAndWait( "Change User Properties");
        return new DomainDesignerPage(getDriver());
    }

    public AddUsersPage clickAddUsers()
    {
        getUsersTable().clickHeaderButtonAndWait( "Add Users");
        return new AddUsersPage(getDriver());
    }

    public ShowUsersPage includeInactiveUsers()
    {
        Optional<WebElement> link = showInactiveUsersLoc.findOptionalElement(getDriver());
        if (link.isPresent())
        {
            clickAndWait(link.get());
            return new ShowUsersPage(getDriver());
        }
        else
        {
            return this;
        }
    }

    public ShowUsersPage hideInctiveUsers()
    {
        Optional<WebElement> link = hideInactiveUsersLoc.findOptionalElement(getDriver());
        if (link.isPresent())
        {
            clickAndWait(link.get());
            return new ShowUsersPage(getDriver());
        }
        else
        {
            return this;
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        final DataRegionTable usersTable = DataRegionTable.DataRegion(getDriver()).withName("Users").findWhenNeeded(getDriver());
    }
}