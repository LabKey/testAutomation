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
package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * wraps the 'confirm delete' page for assay run(s)
 */
public class DeleteSelectedRunsPage extends LabKeyPage<DeleteSelectedRunsPage.Elements>
{
    public DeleteSelectedRunsPage(WebDriver driver)
    {
        super(driver);
    }

    public void clickOK()
    {
        doAndWaitForPageToLoad(()-> newElementCache().okButton.click());
    }

    public void clickConfirmDelete()
    {
        doAndWaitForPageToLoad(()-> newElementCache().confirmDeleteButton.click());
    }

    public void clickCancel()
    {
        doAndWaitForPageToLoad(()-> newElementCache().cancelButton.click());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends LabKeyPage.ElementCache
    {
        final WebElement okButton = new LazyWebElement(Locator.lkButton("OK"), this);
        final WebElement confirmDeleteButton = new LazyWebElement(Locator.lkButton("Confirm Delete"), this);
        final WebElement cancelButton = new LazyWebElement(Locator.lkButton("Cancel"), this);
    }
}
