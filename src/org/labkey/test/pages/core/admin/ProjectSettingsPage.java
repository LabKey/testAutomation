/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;

public class ProjectSettingsPage extends BaseSettingsPage
{
    public ProjectSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ProjectSettingsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ProjectSettingsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "projectSettings"));
        return new ProjectSettingsPage(driver.getDriver());
    }

    public boolean getShouldInherit()
    {
        return elementCache().shouldInherit.isChecked();
    }

    public void setShouldInherit(boolean value)
    {
        if(value)
        {
            elementCache().shouldInherit.check();
        }
        else
        {
            elementCache().shouldInherit.uncheck();
        }
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

    protected class ElementCache extends BaseSettingsPage.ElementCache
    {
        protected final Checkbox shouldInherit = Checkbox.Checkbox(Locator.name("shouldInherit")).findWhenNeeded(this);
    }
}