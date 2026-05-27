/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.test.components.ui.files;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ImageFileViewDialog extends ModalDialog
{
    public ImageFileViewDialog(WebDriver driver, String filename)
    {
        super(new ModalDialogFinder(driver).withTitle(filename).waitFor().getComponentElement(), driver);
    }

    public boolean isImageRendered()
    {
        return elementCache().img.isDisplayed();
    }

    @Override
    public ElementCache elementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends ModalDialog.ElementCache
    {
        final WebElement img = Locator.tagWithClass("img", "attachment-card__img_modal").findWhenNeeded(this);
    }
}
