package org.labkey.test.pages.assay;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;


public class DataImportPage extends LabKeyPage<DataImportPage.Elements>
{
    public DataImportPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public void insertTsvData(String data)
    {
        setFormElement(newElementCache().inputTsvField, data);
        clickButton("Submit");
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends LabKeyPage.ElementCache
    {

        final RadioButton uploadFileButton = new RadioButton(
                new LazyWebElement(Locator.radioButtonById("FileUpload"), this));
        final WebElement inputTsvField = new LazyWebElement(Locator.xpath(".//textarea[@id='tsv3']"), this);

        //final WebElement formatSelect = new LazyWebElement(Locator.xpath("//[]"))
        final WebElement importByAltKeyCheckbox = new LazyWebElement(Locator.checkboxByName("importLookupByAlternateKey"), this);
        final WebElement downloadTemplateButton = new LazyWebElement(Locator.lkButton("Download Template"), this);
        final WebElement submitButton = new LazyWebElement(Locator.lkButton("Submit"), this);
        final WebElement cancelButton = new LazyWebElement(Locator.lkButton("Cancel"), this);
    }
}
