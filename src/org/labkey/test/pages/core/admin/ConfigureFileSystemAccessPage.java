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
package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ConfigureFileSystemAccessPage extends LabKeyPage<ConfigureFileSystemAccessPage.ElementCache>
{
    public ConfigureFileSystemAccessPage(WebDriver driver)
    {
        super(driver);
    }

    public static ConfigureFileSystemAccessPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", "filesSiteSettings"));
        return new ConfigureFileSystemAccessPage(driver.getDriver());
    }

    public ShowAdminPage save()
    {
        clickAndWait(elementCache().saveButton);

        return new ShowAdminPage(getDriver());
    }

    public ShowAdminPage cancel()
    {
        clickAndWait(elementCache().cancelButton);

        return new ShowAdminPage(getDriver());
    }

    public ConfigureFileSystemAccessPage setSiteLevelFileRoot(String value)
    {
        elementCache().siteLevelFileRoot.set(value);
        return this;
    }

    public String getSiteLevelFileRoot()
    {
        return elementCache().siteLevelFileRoot.get();
    }

    public ConfigureFileSystemAccessPage setEnableWebFiles(boolean set)
    {
        new Checkbox(elementCache().enableWebFilesCheckbox).set(set);
        return this;
    }

    public ConfigureFileSystemAccessPage setHomeDirectoryFileRoot(String value)
    {
        elementCache().homeDirectoryFileRoot.set(value);
        return this;
    }

    public String getHomeDirectoryFileRoot()
    {
        return elementCache().homeDirectoryFileRoot.get();
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected final WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);

        protected final Input siteLevelFileRoot = Input.Input(Locator.name("rootPath"), getDriver()).findWhenNeeded(this);
        protected final Input homeDirectoryFileRoot = Input.Input(Locator.name("userRootPath"), getDriver()).findWhenNeeded(this);

        public WebElement enableWebFilesCheckbox = Locator.input("webfilesEnabled")
                .findWhenNeeded(this).withTimeout(4000);
        // TODO need to add an element and support methods for the summary grid.
    }

}
