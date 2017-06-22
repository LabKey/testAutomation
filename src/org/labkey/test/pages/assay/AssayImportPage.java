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
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;


public class AssayImportPage extends LabKeyPage<AssayImportPage.Elements>
{
    public AssayImportPage(WebDriver driver)
    {
        super(driver);
    }

    public AssayImportPage setNamedInputText(String name, String text)
    {
        WebElement input = Locator.input(name).findElement(getDriver());
        new Input(input, getDriver()).set(text);
        return this;
    }

    public AssayImportPage selectNamedFieldOption(String name, String text)
    {
        WebElement input = Locator.xpath("//select[@name='" + name +"']").findElement(getDriver());
        new OptionSelect(input).set(text);
        return this;
    }

    public AssayImportPage setNamedTextAreaValue(String name, String text)
    {
        WebElement input = Locator.xpath("//textarea[@name='" + name +"']").findElement(getDriver());
        setFormElement(input, text);
        return this;
    }

    public AssayImportPage setDataText(String text)
    {
        selectTSVRadioButton();
        setTextInputField(text);
        return this;
    }

    private void selectTSVRadioButton()
    {
        elementCache().pasteTSVButton.check();
    }

    private void setTextInputField(String text)
    {
        elementCache().inputRunDataField.setValue(text);
    }

    public void setDataFile(File uploadFile)
    {
        selectUploadFileRadioButton();
        setFormElement(Locator.name("__primaryFile__"), uploadFile);
    }

    private void selectUploadFileRadioButton()
    {
        elementCache().uploadFileButton.check();
    }


    /* button actions */
    public void clickSaveAndFinish()
    {
        doAndWaitForPageToLoad(()-> elementCache().saveAndFinishButton.click());
    }

    public AssayImportPage clickSaveAndImportAnother()
    {
        doAndWaitForPageToLoad(()-> elementCache().saveAndImportAnotherButton.click());
        return new AssayImportPage(getDriver());
    }

    public void clickResetDefaults()
    {
        doAndWaitForPageToLoad(()-> elementCache().resetDefaultValuesButton.click());
    }

    public void clickCancel()
    {
        doAndWaitForPageToLoad(()-> elementCache().cancelButton.click());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends LabKeyPage.ElementCache
    {
        final RadioButton pasteTSVButton = new RadioButton(
                new LazyWebElement(Locator.radioButtonById("textAreaDataProvider"), this));
        final RadioButton uploadFileButton = new RadioButton(
                new LazyWebElement(Locator.radioButtonById("FileUpload"), this));
        final Input inputRunDataField = new Input(
                new LazyWebElement(Locator.xpath(".//textarea[@id='TextAreaDataCollector.textArea']"), this),
                getDriver());

        final WebElement saveAndFinishButton = new LazyWebElement(Locator.lkButton("Save and Finish"), this);
        final WebElement saveAndImportAnotherButton = new LazyWebElement(Locator.lkButton("Save and Import Another Run"), this);
        final WebElement resetDefaultValuesButton = new LazyWebElement(Locator.lkButton("Reset Default Values"), this);
        final WebElement cancelButton = new LazyWebElement(Locator.lkButton("Cancel"), this);
    }
}
