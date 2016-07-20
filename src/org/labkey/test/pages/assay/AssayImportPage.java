package org.labkey.test.pages.assay;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;

import java.io.File;


public class AssayImportPage extends LabKeyPage<AssayImportPage.Elements>
{
    public AssayImportPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public void setNamedFieldText(String name, String text)
    {
        WebElement input = Locator.input(name).findElement(getDriver());
        new Input(input, getDriver()).setValue(text);
    }

    public void setDataText(String text)
    {
        selectTSVRadioButton();
        setTextInputField(text);
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
        return new AssayImportPage(BaseWebDriverTest.getCurrentTest());
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
