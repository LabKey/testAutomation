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
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;

public class ImportDataPage extends LabKeyPage<ImportDataPage.ElementCache>
{
    public ImportDataPage(WebDriver driver)
    {
        super(driver);
    }

    public void setText(String text)
    {
        setFormElement(elementCache().pasteDataTextArea,text);
    }

    public void setFormat(Format format)
    {
        _extHelper.selectComboBoxItem("Format:", format.format);
    }

    public void setImportLookupByAlternateKey(boolean useAltKey)
    {
        // Find the checkbox for the currently expanded section
        String checkboxLabel = "Import Lookups by Alternate Key";
        Optional<Checkbox> altKeyCheckbox = Ext4Checkbox().withLabel(checkboxLabel)
                .findAll(getDriver()).stream().filter(Checkbox::isDisplayed).findAny();

        altKeyCheckbox.orElseThrow(() -> new AssertionError("Failed to find checkbox for: " + checkboxLabel))
                .set(useAltKey);
    }

    public void selectUpload()
    {
        if(!elementCache().uploadFileDiv.isDisplayed()){expandUpload();}
    }

    public void selectCopyPaste()
    {
        if(!elementCache().copyPasteDiv.isDisplayed()){expandCopyPaste();}
    }

    public void expandUpload()
    {
        elementCache().uploadExpando.click();
    }

    public void expandCopyPaste()
    {
        elementCache().copyPasteExpando.click();
    }

    public void setUploadFileLocation(String path)
    {
        setFormElement(elementCache().uploadFileFilePath,path);
    }

    public void uploadData(String filePath, boolean expectSuccess, String error)
    {
        selectUpload();
        setUploadFileLocation(filePath);
        submit();
    }

    public void pasteData(String tsv,boolean expectSuccess, String error)
    {
        selectCopyPaste();
        setText(tsv);
        submit();
    }

    public void submit()
    {
        clickAndWait(elementCache().submitBtn);
    }

    public void cancel()
    {
        clickAndWait(elementCache().cancelBtn);
    }

    public enum Format{
        TSV("Text (String)"), CSV("Multi-Line Text");

        private String format;

        public String getFormat(){
            return this.format;
        }
        Format(String format){
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
        WebElement pasteDataTextArea = Locator.xpath("//div[@id='copypasteDiv1']/descendant::textarea").findWhenNeeded(this);
        WebElement submitBtn = Ext4Helper.Locators.ext4Button("Submit").findWhenNeeded(this);
        WebElement cancelBtn = Ext4Helper.Locators.ext4Button("Cancel").findWhenNeeded(this);
        WebElement uploadExpando = Locator.id("uploadFileDiv2Expando").findWhenNeeded(this);
        WebElement copyPasteExpando = Locator.id("copyPasteDiv1Expando").findWhenNeeded(this);
        WebElement copyPasteDiv = Locator.id("copypasteDiv1").findElement(this);
        WebElement uploadFileDiv = Locator.id("uploadFileDiv2").findElement(this);
        WebElement uploadFileFilePath = Locator.xpath("//div[@id='uploadFileDiv2']/descendant::input[@name='file']").findElement(this);
    }
}
