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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Optional;

// File upload field for 'DetailsTableEdit'. Interacts with several possible underlying components
public class FileUploadField extends WebDriverComponent<FileUploadField.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public FileUploadField(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public FileUploadField setFile(File file)
    {
        removeFile();
        elementCache().fileInput.sendKeys(file.getAbsolutePath());
        getWrapper().shortWait().until(ExpectedConditions.textToBePresentInElement(elementCache().tempFileLoc, file.getName()));
        return this;
    }

    public void removeFile()
    {
        if (hasAttachedFile())
        {
            if (elementCache().removeBtn.isDisplayed())
            {
                elementCache().removeBtn.click();
            }
            else
            {
                elementCache().getExistingAttachment().ifPresent(AttachmentCard::clickRemove);
            }
            WebDriverWrapper.waitFor(() -> !hasAttachedFile(),
                    "the file was not removed", 2000);
        }
    }

    public boolean hasAttachedFile()
    {
        return !elementCache().fileInputLabel.isDisplayed();
    }

    public String getExistingFileName()
    {
        if (hasAttachedFile())
        {
            AttachmentCard card = elementCache().getExistingAttachment().get();
            return card.getFileName();
        }
        return null;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {

        // Elements when no attachment is set
        WebElement fileInputLabel = Locator.tagWithClass("label", "file-upload--compact-label")
                .refindWhenNeeded(this);
        WebElement fileInput = Locator.tagWithClass("input", "file-upload__input")
                .refindWhenNeeded(this);

        // Elements for new attachment
        WebElement tempFileLoc = Locator.tagWithClass("div", "attached-file__inline-container")
                .refindWhenNeeded(this);
        WebElement removeBtn = Locator.tagWithClass("span", "attached-file__remove-icon")
                .refindWhenNeeded(this);

        // Component for existing attachment
        Optional<AttachmentCard> getExistingAttachment()
        {
            return new AttachmentCard.FileAttachmentCardFinder(getDriver()).findOptional(this);
        }
    }

}
