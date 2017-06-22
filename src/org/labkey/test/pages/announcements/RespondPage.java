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

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class RespondPage extends BaseUpdatePage<RespondPage>
{
    public RespondPage(WebDriver driver)
    {
        super(driver);
    }

    public static RespondPage beginAt(WebDriverWrapper driver, String parentId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), parentId);
    }

    public static RespondPage beginAt(WebDriverWrapper driver, String containerPath, String parentId)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "respond", Maps.of("parentId", parentId)));
        return new RespondPage(driver.getDriver());
    }

    @Override
    protected RespondPage getThis()
    {
        return this;
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseUpdatePage<RespondPage>.ElementCache
    {
        // TODO: Add edit and delete links for thread embedded on page
    }
}