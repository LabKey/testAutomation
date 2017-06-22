/*
 * Copyright (c) 2017 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Created by RyanS on 5/18/2017.
 */
public class ImportDataPage extends LabKeyPage<ImportDataPage.ElementCache>
{
    public ImportDataPage(WebDriver driver)
    {
        super(driver);
        //waitFor(()-> {return elementCache().pasteDataTextArea.isDisplayed();});
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
        List<WebElement> els = elementCache().importAltKeyChk.findElements(this.elementCache());
        WebElement input = null;
        for (WebElement el : els)
        {
            if (!el.isDisplayed())
                continue;
            input = el;
            break;
        }

        if (input == null)
            Assert.fail("Failed to find checkbox: " + elementCache().importAltKeyChk.toString());

        if (useAltKey)
            checkCheckbox(input);
        else
            uncheckCheckbox(input);
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
        click(elementCache().uploadExpando);
    }

    public void expandCopyPaste()
    {
        click(elementCache().copyPasteExpando);
    }

    public void setUploadFileLocation(String path)
    {
        setFormElement(elementCache().uploadFileFilePath,path);
    }

    public void uploadData(String filePath, boolean expectSuccess, String error)
    {
        selectUpload();
        setUploadFileLocation(filePath);
        click(elementCache().submitBtn);
    }

    public void pasteData(String tsv,boolean expectSuccess, String error)
    {
        selectCopyPaste();
        setText(tsv);
        click(elementCache().submitBtn);
    }

    public void submit()
    {
        clickAndWait(elementCache().submitBtn);
    }

    public void cancel()
    {
        click(elementCache().cancelBtn);
    }

    enum Format{
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
        Locator importAltKeyChk = Locator.input("importLookupByAlternateKey");//.findWhenNeeded(this);
        WebElement submitBtn = Locator.button("Submit").findWhenNeeded(this);
        WebElement cancelBtn = Locator.button("Cancel").findWhenNeeded(this);
        WebElement uploadExpando = Locator.id("uploadFileDiv2Expando").findWhenNeeded(this);
        WebElement copyPasteExpando = Locator.id("copyPasteDiv1Expando").findWhenNeeded(this);
        WebElement copyPasteDiv = Locator.id("copypasteDiv1").findElement(this);
        WebElement uploadFileDiv = Locator.id("uploadFileDiv2").findElement(this);
        WebElement uploadFileFilePath = Locator.xpath("//div[@id='uploadFileDiv2']/descendant::input[@name='file']").findElement(this);
    }
}
