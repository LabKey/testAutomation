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
package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.admin.CreateSubFolderPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_PAGE;

public class SubfoldersWebPart extends BodyWebPart<SubfoldersWebPart.ElementCache>
{
    public SubfoldersWebPart(WebDriver driver)
    {
        super(driver, "Subfolders");
    }

    static public SubfoldersWebPart getWebPart(WebDriver driver)
    {
        return new SubfoldersWebPart(driver);
    }

    public List<String> GetSubfolderNames()
    {
        List<WebElement> subFolderElements = elementCache().visibleSubfolders();
        return getWrapper().getTexts(subFolderElements);
    }

    public WebElement getSubfolderElement(String folderName)
    {
        return elementCache().subFolder(folderName);
    }

    public LabKeyPage goToSubfolder(String folderName)
    {
        getWrapper().clickAndWait(getSubfolderElement(folderName));
        return new LabKeyPage(getDriver());
    }

    public CreateSubFolderPage clickCreateSubfolder()
    {
        getWrapper().clickButton("Create New Subfolder", WAIT_FOR_PAGE);

        return new CreateSubFolderPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BodyWebPart.ElementCache
    {
        List<WebElement> visibleSubfolders()
        {
            return Locators.folder.withDescendant(Locators.folderLabel).findElements(this);
        }
        WebElement subFolder(String folderName)
        {
            return Locators.folder.withDescendant(Locators.folderLabel.withText(folderName))
                    .waitForElement(this, WAIT_FOR_JAVASCRIPT);
        }
    }

    public static class Locators
    {
        public static Locator.XPathLocator folder = Locator.tagWithClass("div", "tool-icon");
        public static Locator.XPathLocator folderLabel = Locator.tagWithClass("span", "thumb-label-bottom");
    }
}