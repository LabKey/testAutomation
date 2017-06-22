/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages.announcements;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ThreadPage extends LabKeyPage<ThreadPage.ElementCache>
{
    public ThreadPage(WebDriver driver)
    {
        super(driver);
    }

    public static ThreadPage beginAt(WebDriverWrapper driver, String entityId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), entityId);
    }

    public static ThreadPage beginAt(WebDriverWrapper driver, String containerPath, String entityId)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "thread", Maps.of("entityId", entityId)));
        return new ThreadPage(driver.getDriver());
    }

    public RespondPage clickRespond()
    {
        clickAndWait(elementCache().respondButton);
        return new RespondPage(getDriver());
    }

    public UpdatePage clickEdit()
    {
        clickAndWait(elementCache().editLink);
        return new UpdatePage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement respondButton = Locator.lkButton("Respond").findWhenNeeded(this);
        WebElement editLink = Locator.linkWithText("edit").findWhenNeeded(this);
    }
}