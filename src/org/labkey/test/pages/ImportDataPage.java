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

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;

public class ImportDataPage extends LabKeyPage<ImportDataPage.ElementCache>
{
    public ImportDataPage(WebDriver driver)
    {
        super(driver);
    }

    public ImportDataPage setText(String text)
    {
        selectCopyPaste();
        setFormElement(elementCache().pasteDataTextArea, text);
        return this;
    }

    public ImportDataPage setFormat(Format format)
    {
        elementCache().formatCombo.selectComboBoxItem(format.format);
        return this;
    }

    public ImportDataPage setImportLookupByAlternateKey(boolean useAltKey)
    {
        elementCache().getAltKeyCheckbox().set(useAltKey);
        return this;
    }

    public ImportDataPage selectUpload()
    {
        if (!elementCache().uploadFileDiv.isDisplayed())
        {
            elementCache().uploadExpando.click();
        }
        return this;
    }

    public ImportDataPage selectCopyPaste()
    {
        if (!elementCache().copyPasteDiv.isDisplayed())
        {
            elementCache().copyPasteExpando.click();
        }
        return this;
    }

    public ImportDataPage setFile(File file)
    {
        selectUpload();
        setFormElement(elementCache().uploadFileFilePath, file);
        return this;
    }

    public ImportDataPage submit()
    {
        clickAndWait(elementCache().getSubmitButton());
        clearCache();
        return this;
    }

    public ImportDataPage submitExpectingError(String error)
    {
        String actualError = submitExpectingError();
        assertEquals(error, actualError);
        return this;
    }

    public String submitExpectingError()
    {
        elementCache().getSubmitButton().click();
        clearCache();
        return waitForErrors();
    }

    private String waitForErrors()
    {
        Mutable<String> error = new MutableObject<>();
        shortWait().until(wd ->
        {
            error.setValue(String.join("\n", getTexts(Locators.labkeyError.withText().findElements(getDriver()))));
            return !error.getValue().isBlank();
        });
        return error.getValue();
    }

    public void cancel()
    {
        clickAndWait(elementCache().getCancelButton());
        clearCache();
    }

    public File downloadTemplate()
    {
        return doAndWaitForDownload(()->elementCache().getDownloadTemplateButton().click());
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

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        // Upload file
        WebElement uploadPanel = Locator.tagWithAttribute("div", "data-panel-name", "uploadFilePanel").findWhenNeeded(this);
        WebElement uploadFileDiv = Locator.byClass("panel-body").findWhenNeeded(uploadPanel);
        WebElement uploadExpando = Locator.byClass("lk-import-expando").findWhenNeeded(uploadPanel);
        WebElement uploadFileFilePath = Locator.input("file").findWhenNeeded(uploadFileDiv);

        // Paste data
        WebElement copyPastePanel = Locator.tagWithAttribute("div", "data-panel-name", "uploadFilePanel").findWhenNeeded(this);
        WebElement copyPasteDiv = Locator.byClass("panel-body").findWhenNeeded(copyPastePanel);
        WebElement copyPasteExpando = Locator.byClass("lk-import-expando").findWhenNeeded(copyPastePanel);
        WebElement pasteDataTextArea = Locator.tag("textarea").findWhenNeeded(copyPasteDiv);
        ComboBox formatCombo = new ComboBox.ComboBoxFinder(getDriver()).withLabel("Format:").findWhenNeeded(copyPastePanel);

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

        WebElement getDownloadTemplateButton()
        {
            return Locator.lkButton("Download Template").findElement(this);
        }

        Checkbox getAltKeyCheckbox()
        {
            return Ext4Checkbox().withLabel("Import Lookups by Alternate Key").find(getExpandedPanel());
        }
    }
}
