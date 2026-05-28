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
package org.labkey.test.pages.list;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class ConfirmDeletePage extends LabKeyPage<ConfirmDeletePage.ElementCache>
{
    private String _deleteBtnText;

    public ConfirmDeletePage(WebDriver driver)
    {
        this(driver, "Confirm Delete");
    }

    public ConfirmDeletePage(WebDriver driver, String deleteBtnText)
    {
        super(driver);
        _deleteBtnText = deleteBtnText;
    }

    public BeginPage confirmDelete()
    {
        clickAndWait(elementCache().deleteButton);
        return new BeginPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement deleteButton = Locator.lkButton(_deleteBtnText == null ? "Confirm Delete" : _deleteBtnText).findWhenNeeded(this);
    }
}
