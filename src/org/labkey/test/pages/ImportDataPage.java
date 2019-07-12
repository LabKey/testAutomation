/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;

public class ImportDataPage extends LabKeyPage<ImportDataPage.ElementCache>
{
    public ImportDataPage(WebDriver driver)
    {
        super(driver);
    }

    public void setText(String text)
    {
        setFormElement(elementCache().pasteDataTextArea, text);
    }

    public void setFormat(Format format)
    {
        _ext4Helper.selectComboBoxItem("Format:", format.format);
    }

    public void setImportLookupByAlternateKey(boolean useAltKey)
    {
        elementCache().getAltKeyCheckbox().set(useAltKey);
    }

    public void selectUpload()
    {
        if (!elementCache().uploadFileDiv.isDisplayed())
        {
            expandUpload();
        }
    }

    public void selectCopyPaste()
    {
        if (!elementCache().copyPasteDiv.isDisplayed())
        {
            expandCopyPaste();
        }
    }

    public void expandUpload()
    {
        elementCache().uploadExpando.click();
    }

    public void expandCopyPaste()
    {
        elementCache().copyPasteExpando.click();
    }

    public void setUploadFileLocation(File file)
    {
        setFormElement(elementCache().uploadFileFilePath, file);
    }

    public void uploadData(File file, String error)
    {
        selectUpload();
        setUploadFileLocation(file);
        submit();
        assertError(error);
    }

    public void pasteData(String tsv, String error)
    {
        selectCopyPaste();
        setText(tsv);
        submit();
        assertError(error);
    }

    private void assertError(String error)
    {
        if (error != null)
        {
            assertElementPresent(Locators.labkeyError.withText(error));
        }
    }

    public void submit()
    {
        clickAndWait(elementCache().getSubmitButton());
        clearCache();
    }

    public void cancel()
    {
        clickAndWait(elementCache().getCancelButton());
        clearCache();
    }

    public enum Format
    {
        TSV("Tab-separated text (tsv)"), CSV("Comma-separated text (csv)");

        private String format;

        public String getFormat()
        {
            return this.format;
        }

        Format(String format)
        {
            this.format = format;
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        // Upload file
        WebElement uploadFileDiv = Locator.id("uploadFileDiv2").findWhenNeeded(this);
        WebElement uploadExpando = Locator.id("uploadFileDiv2Expando").findWhenNeeded(this);
        WebElement uploadFileFilePath = Locator.xpath("//div[@id='uploadFileDiv2']/descendant::input[@name='file']").findElement(this);

        // Paste data
        WebElement copyPasteDiv = Locator.id("copypasteDiv1").findWhenNeeded(this);
        WebElement copyPasteExpando = Locator.id("copyPasteDiv1Expando").findWhenNeeded(this);
        WebElement pasteDataTextArea = Locator.xpath("//div[@id='copypasteDiv1']/descendant::textarea").findWhenNeeded(this);
        ComboBox formatCombo = new ComboBox.ComboBoxFinder(getDriver()).withLabel("Format:").findWhenNeeded(this);

        WebElement getExpandedPanel()
        {
            if (copyPasteDiv.isDisplayed())
                return copyPasteDiv;
            else if (uploadFileDiv.isDisplayed())
                return uploadFileDiv;
            else
                throw new IllegalStateException("Unable to determine expanded panel");
        }

        WebElement getSubmitButton()
        {
            return Ext4Helper.Locators.ext4Button("Submit").findElement(getExpandedPanel());
        }

        WebElement getCancelButton()
        {
            return Ext4Helper.Locators.ext4Button("Cancel").findElement(getExpandedPanel());
        }

        Checkbox getAltKeyCheckbox()
        {
            return Ext4Checkbox().withLabel("Import Lookups by Alternate Key").find(getExpandedPanel());
        }
    }
}
