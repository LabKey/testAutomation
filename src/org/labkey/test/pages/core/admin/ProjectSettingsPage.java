/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ProjectSettingsPage extends LabKeyPage<ProjectSettingsPage.ElementCache>
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

    public void save()
    {
        elementCache().saveButton.click();
    }

    public Checkbox getEnableDiscussionCheckbox()
    {
        return elementCache().enableDiscussion;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final Checkbox enableDiscussion = Checkbox.Checkbox(Locator.name("enableDiscussion")).findWhenNeeded(this);
        protected final WebElement saveButton = findButton("Save");
    }
}