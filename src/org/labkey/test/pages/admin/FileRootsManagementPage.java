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
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.RadioButton.RadioButton;

public class FileRootsManagementPage extends FolderManagementPage
{
    public FileRootsManagementPage(WebDriver driver)
    {
        super(driver);
    }

    public static FileRootsManagementPage beginAt(WebDriverWrapper wrapper)
    {
        return beginAt(wrapper, wrapper.getCurrentContainerPath());
    }

    public static FileRootsManagementPage beginAt(WebDriverWrapper wrapper, String containerPath)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", containerPath, "fileRoots", Maps.of("tabId", "files")));
        return new FileRootsManagementPage(wrapper.getDriver());
    }

    public FileRootsManagementPage setFileRoot(FileRootOption fileRootOption)
    {
        elementCache().findFileRootOptionRadio(fileRootOption.name()).check();
        return this;
    }

    public FileRootsManagementPage useCloudBasedStorage(String cloudRootName)
    {
        elementCache().findFileRootOptionRadio("cloudRoot").check();
        selectOptionByValue(Locator.id("cloudRootName"), cloudRootName);
        return this;
    }

    public FileRootsManagementPage setCloudStoreEnabled(String name, Boolean enabled)
    {
        elementCache().findCloudStoreCheckbox(name).set(enabled);
        return this;
    }

    public FileRootsManagementPage clickSave()
    {
        clickAndWait(elementCache().saveButton);
        clearCache();
        return this;
    }

    public String getRootPath()
    {
        return getFormElement(elementCache().rootPath);
    }

    public String getCloudRootName()
    {
        return getSelectedOptionValue(elementCache().cloudRootName);
    }

    public String getSelectedFileRootOption()
    {
        return elementCache().fileRootOptionLoc.withAttribute("checked").findElement(getDriver())
                .getAttribute("value");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends FolderManagementPage.ElementCache
    {
        WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected final Locator.XPathLocator fileRootOptionLoc = Locator.radioButtonByName("fileRootOption");
        RadioButton findFileRootOptionRadio(String value)
        {
            return RadioButton(fileRootOptionLoc.withAttribute("value", value)).find(this);
        }
        WebElement rootPath = Locator.id("rootPath").findWhenNeeded(this);
        WebElement cloudRootName = Locator.id("cloudRootName").findWhenNeeded(this);

        Checkbox findCloudStoreCheckbox(String name)
        {
            return new Checkbox(Locator.input("enabledCloudStore").withAttribute("value", name).findElement(this));
        }
    }

    public enum FileRootOption
    {
        disable,
        siteDefault,
        folderOverride,
        cloudRoot
    }
}
