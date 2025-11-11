/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.components.assay.AssayConstants;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.labkey.test.util.EscapeUtil.getTableViewFormFieldName;

public class AssayImportPage extends LabKeyPage<AssayImportPage.Elements>
{
    public AssayImportPage(WebDriver driver)
    {
        super(driver);
    }

    public AssayImportPage setNamedInputText(String name, String text)
    {
        WebElement input = Locator.input(getTableViewFormFieldName(name)).findElement(getDriver());
        new Input(input, getDriver()).set(text);
        return this;
    }

    public AssayImportPage selectNamedFieldOption(String name, String text)
    {
        WebElement input = Locator.tagWithName("select", getTableViewFormFieldName(name)).findElement(getDriver());
        new OptionSelect(input).set(text);
        return this;
    }

    public AssayImportPage setNamedTextAreaValue(String name, String text)
    {
        WebElement input = Locator.textarea(getTableViewFormFieldName(name)).findElement(getDriver());
        setFormElement(input, text);
        return this;
    }

    public AssayImportPage setFileField(String name, File file)
    {
        setFormElement(Locator.input(getTableViewFormFieldName(name)), file);
        return this;
    }

    public AssayImportPage setDataText(String text)
    {
        selectTSVRadioButton();
        return setTextInputField(text);
    }

    private void selectTSVRadioButton()
    {
        elementCache().pasteTSVButton.check();
    }

    private AssayImportPage setTextInputField(String text)
    {
        elementCache().inputRunDataField.setValue(text);
        return this;
    }

    public AssayImportPage setDataFile(File uploadFile)
    {
        selectUploadFileRadioButton();
        setFormElement(Locator.name("__primaryFile__"), uploadFile);
        return this;
    }

    public AssayImportPage selectUploadFileRadioButton()
    {
        elementCache().uploadFileButton.check();
        return this;
    }

    /**
     * Retrieves the file name for a field if it has been previously uploaded. In this case the server
     * will display a file name with an icon and a "[remove]" link. If a value has not been previously uploaded,
     * then this will look for a file input and return the value of that field.
     *
     * @param fieldName the name of the field for which the value should be retrieved
     * @return the value associated with the specified file field, or null if no value is found
     */
    public @Nullable String getFileFieldValue(String fieldName)
    {
        var removeFileLink = Locator.tagWithClass("div", "lk-remove-file")
                .withAttribute("data-fieldname", fieldName);

        if (isElementPresent(removeFileLink))
        {
            var text = removeFileLink.findElement(getDriver()).getText();
            return StringUtils.trimToNull(text.replace("[remove]", ""));
        }

        var fileInput = Locator.input(getTableViewFormFieldName(fieldName));
        if (isElementPresent(fileInput))
            return getFormElement(fileInput);

        return null;
    }

    /* button actions */
    public AssayImportPage clickNext()
    {
        doAndWaitForPageToLoad(()-> elementCache().nextButton.click());
        return new AssayImportPage(getDriver());
    }

    public void clickSaveAndFinish()
    {
        scrollIntoView(elementCache().saveAndFinishButton, true);
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
                new LazyWebElement<>(Locator.radioButtonById("textAreaDataProvider"), this));
        final RadioButton uploadFileButton = new RadioButton(
                new LazyWebElement<>(Locator.radioButtonById("Fileupload"), this));
        final Input inputRunDataField = new Input(
                new LazyWebElement<>(AssayConstants.TEXT_AREA_DATA_COLLECTOR_LOCATOR, this),
                getDriver());

        final WebElement nextButton = new LazyWebElement<>(Locator.lkButton("Next"), this);
        final WebElement saveAndFinishButton = new LazyWebElement<>(Locator.lkButton("Save and Finish"), this);
        final WebElement saveAndImportAnotherButton = new LazyWebElement<>(Locator.lkButton("Save and Import Another Run"), this);
        final WebElement resetDefaultValuesButton = new LazyWebElement<>(Locator.lkButton("Reset Default Values"), this);
        final WebElement cancelButton = new LazyWebElement<>(Locator.lkButton("Cancel"), this);
    }
}
