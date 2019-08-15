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
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CreateProjectPage extends LabKeyPage<CreateProjectPage.ElementCache>
{
    public CreateProjectPage(WebDriver driver)
    {
        super(driver);
    }

    public static CreateProjectPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static CreateProjectPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("controller", containerPath, "action"));
        return new CreateProjectPage(driver.getDriver());
    }

    public CreateProjectPage setProjectName(String projectName)
    {
        log("Creating project with name " + projectName);
        setFormElement(elementCache().nameInput, projectName);
        return this;
    }

    public CreateProjectPage setUseNameAsDisplayTitle(boolean set)
    {
        new Checkbox(elementCache().useNameAsDisplayTitleCheckBox).set(set);
        return this;
    }

    public CreateProjectPage setTitle(String title)
    {
        setFormElement(elementCache().titleInput, title);
        return this;
    }

    public CreateProjectPage setFolderType(String folderType)
    {
        WebElement btn = Locator.xpath("//td[./label[text()='"+folderType+"']]/input")
                .findWhenNeeded(getDriver()).withTimeout(4000);
        btn.click();
        waitFor(()-> btn.getAttribute("class").contains("x4-form-radio-focus"),
                "folder type [" +folderType+ "] did not become selected",2000);
        return this;
    }

    public SetFolderPermissionsPage clickNext()
    {
        clickButton("Next");
        return new SetFolderPermissionsPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement nameInput = Locator.input("name").findWhenNeeded(this).withTimeout(4000);;
        WebElement titleInput = Locator.input("title").findWhenNeeded(this).withTimeout(4000);;
        WebElement useNameAsDisplayTitleCheckBox = Locator.checkboxByLabel("Use name as display title", false)
                .findWhenNeeded(this).withTimeout(4000);
    }
}