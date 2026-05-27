/*
 * Copyright (c) 2025-2026 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

public class FolderAliasesPage extends LabKeyPage<FolderAliasesPage.ElementCache>
{
    public FolderAliasesPage(WebDriver driver)
    {
        super(driver);
    }

    public List<String> getAliases()
    {
        String aliases = getFormElement(elementCache().aliases);
        return Arrays.asList(aliases.split("\\R"));
    }

    public FolderAliasesPage setAliases(List<String> aliases)
    {
        setFormElement(elementCache().aliases, StringUtils.join(aliases, "\n"));
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

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement aliases = Locator.textarea("aliases").findWhenNeeded(this);

        WebElement saveBtn = Locator.lkButton("Save Aliases").refindWhenNeeded(this);
        WebElement cancelBtn = Locator.button("Cancel").refindWhenNeeded(this);
    }
}