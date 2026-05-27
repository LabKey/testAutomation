/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.test.pages.wiki;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class ManageWikiConfigurationPage extends LabKeyPage<ManageWikiConfigurationPage.ElementCache>
{
    public ManageWikiConfigurationPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageWikiConfigurationPage beginAt(WebDriverWrapper driver, String containerPath, Map<String, String> values)
    {
        driver.beginAt(WebTestHelper.buildURL("wiki", containerPath, "manage", values));
        return new ManageWikiConfigurationPage(driver.getDriver());
    }

    @Override
    protected ManageWikiConfigurationPage.ElementCache newElementCache()
    {
        return new ManageWikiConfigurationPage.ElementCache();
    }

    public String getNameInput()
    {
        return elementCache().nameInput.get();
    }

    public ManageWikiConfigurationPage setNameInput(String value)
    {
        elementCache().nameInput.set(value);
        return this;
    }

    public ManageWikiConfigurationPage rename(String newName, boolean addAlias)
    {
        elementCache().renameButton.click();
        waitFor(() -> elementCache().addAlias.isDisplayed(), WAIT_FOR_PAGE);
        elementCache().newNameInput.set(newName);
        elementCache().addAlias.set(addAlias);

        return this;
    }

    public String getAliases()
    {
        return elementCache().aliases.getText();
    }

    public ManageWikiConfigurationPage setAliases(String value)
    {
        elementCache().aliases.sendKeys(value);
        return this;
    }

    public ManageWikiConfigurationPage save()
    {
        elementCache().saveButton.click();
        return this;
    }

    public String saveExpectingErrors()
    {
        elementCache().saveButton.click();
        return elementCache().errorMsg.getText();
    }

    public EditPage editContent()
    {
        elementCache().editContentButton.click();
        return new EditPage(getDriver());
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Input nameInput = Input.Input(Locator.name("name"), getDriver()).findWhenNeeded(this);
        WebElement renameButton = Locator.lkButton("Rename").findWhenNeeded(this);
        Input newNameInput = Input.Input(Locator.name("newName"), getDriver()).findWhenNeeded(this);
        Checkbox addAlias = Checkbox.Checkbox(Locator.checkboxByName("addAlias")).findWhenNeeded(this);

        Input titleInput = Input.Input(Locator.name("title"), getDriver()).findWhenNeeded(this);
        Checkbox shouldIndexCheckbox = Checkbox.Checkbox(Locator.checkboxByName("shouldIndex")).findWhenNeeded(this);

        WebElement siblingOrder = Locator.name("siblings").findWhenNeeded(this);
        WebElement moveUpButton = Locator.lkButton("Move Up").findWhenNeeded(this);
        WebElement moveDownButton = Locator.lkButton("Move Down").findWhenNeeded(this);

        WebElement aliases = Locator.textarea("aliases").findWhenNeeded(this);

        WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement deleteButton = Locator.lkButton("Delete").findWhenNeeded(this);
        WebElement editContentButton = Locator.lkButton("Edit Content").findWhenNeeded(this);
        WebElement errorMsg = Locator.tagWithClass("span", "labkey-error").findWhenNeeded(this);
    }

}
