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
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;

/**
 * Created by susanh on 9/20/17.
 */
public class FileRootsManagementPage extends FolderManagementPage
{
    public FileRootsManagementPage(WebDriver driver)
    {
        super(driver);
    }


    public FileRootsManagementPage setCloudStoreEnabled(String name, Boolean enabled)
    {
        Checkbox box = new Checkbox(Locators.cloudStoreCheckBox(name).findElement(getDriver()));
        if (enabled)
            box.check();
        else
            box.uncheck();
        return this;
    }

    public FileRootsManagementPage clickSave()
    {
        elementCache().saveButton.click();
        return this;
    }

    public String getRootPath()
    {
        return getFormElement(Locators.rootPath);
    }

    public static class Locators
    {
        public static Locator rootPath = Locator.id("rootPath");

        public static Locator cloudStoreCheckBox(String name)
        {
            return Locator.tagWithAttribute("input", "value", name);
        }
    }


}
