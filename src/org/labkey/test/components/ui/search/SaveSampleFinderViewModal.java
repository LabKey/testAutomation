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
package org.labkey.test.components.ui.search;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SaveSampleFinderViewModal extends ModalDialog
{
    public SaveSampleFinderViewModal(WebDriver driver)
    {
        this("Save Custom Search", driver);
    }

    protected SaveSampleFinderViewModal(String title, WebDriver driver)
    {
        super(new ModalDialog.ModalDialogFinder(driver).withTitle(title));
    }

    public String getName()
    {
        return Locator.tag("input").findElement(getComponentElement()).getAttribute("value");
    }

    public SaveSampleFinderViewModal setName(String name)
    {
        WebElement input = elementCache().nameInput;
        input.clear();
        if (name != null)
            input.sendKeys(name);
        return this;
    }

    public void clickSave()
    {
        dismiss("Save");
    }

    public String clickSaveExpectingError()
    {
        elementCache().saveBtn.click();
        return getErrorMsg();
    }

    public String getErrorMsg()
    {
        return elementCache().errorMsg.getText();
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

    protected class ElementCache extends ModalDialog.ElementCache
    {
        WebElement errorMsg = Locator.tagWithClassContaining("div", "alert-danger").findWhenNeeded(getComponentElement());

        WebElement nameInput = Locator.tag("input").findWhenNeeded(getComponentElement());

        WebElement saveBtn = Locator.tagWithClassContaining("button", "btn-success")
                .withText("Save")
                .findWhenNeeded(getComponentElement());
        WebElement cancelButton = Locator.tagWithClassContaining("button", "btn-default")
                .withText("Cancel")
                .findWhenNeeded(getComponentElement());
    }

}
