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
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class ReorderFoldersPage extends LabKeyPage<ReorderFoldersPage.ElementCache>
{
    public ReorderFoldersPage(WebDriver driver)
    {
        super(driver);
    }

    public static ReorderFoldersPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ReorderFoldersPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "reorderFolders"));
        return new ReorderFoldersPage(driver.getDriver());
    }

    public ReorderFoldersPage setSortByAlpha(boolean sortByAlpha)
    {
        if (sortByAlpha)
            elementCache().alphabeticalOrderRadioButton.click();
        else
            elementCache().customOrderRadioButton.click();

        return this;
    }

    public ReorderFoldersPage selectProjectInList(String project)
    {
        selectOptionByText(Locator.name("items"), project);
        return this;
    }
    public int getIndexOfProject(String project)
    {
        return getElementIndex(Locator.xpath("//option[@value='"+ project +"']"));
    }

    public ReorderFoldersPage clickMoveUp()
    {
        elementCache().moveUp.click();
        // todo: wait for changed index
        return this;
    }

    public ReorderFoldersPage clickMoveDown()
    {
        elementCache().moveDown.click();
        // todo: wait for changed index
        return this;
    }

    public FolderManagementPage clickSave()
    {
        elementCache().saveBtn.click();
        return new FolderManagementPage(getDriver());
    }
    public FolderManagementPage clickCancel()
    {
        elementCache().cancelBtn.click();
        return new FolderManagementPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement alphabeticalOrderRadioButton = Locator.radioButtonByNameAndValue("resetToAlphabetical", "true").findWhenNeeded(this);
        WebElement customOrderRadioButton = Locator.radioButtonByNameAndValue("resetToAlphabetical","false").findWhenNeeded(this);

        Select projectSelect = SelectWrapper.Select(Locator.tagWithName("select",  "items")).findWhenNeeded(this);

        WebElement moveUp = Locator.tagWithText("span", "Move Up").refindWhenNeeded(this);
        WebElement moveDown = Locator.tagWithText("span", "Move Down").refindWhenNeeded(this);

        WebElement saveBtn = Locator.tagWithText("span", "Save").refindWhenNeeded(this);
        WebElement cancelBtn = Locator.tagWithText("span", "Cancel").refindWhenNeeded(this);
    }
}