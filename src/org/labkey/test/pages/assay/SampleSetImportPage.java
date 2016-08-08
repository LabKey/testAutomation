package org.labkey.test.pages.assay;


import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;

public class SampleSetImportPage extends LabKeyPage<SampleSetImportPage.Elements>
{
    public SampleSetImportPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public void insertTsvData(String data, String parentColumn)
    {
        setFormElement(newElementCache().inputTsvField, data);
        setFormElement(newElementCache().parentSelect, parentColumn);
        clickButton("Submit");
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends LabKeyPage.ElementCache
    {
        final WebElement inputTsvField = new LazyWebElement(Locator.xpath(".//textarea[@id='textbox']"), this);

        final WebElement parentSelect = new LazyWebElement(Locator.xpath("//select[@id='parentCol']"), this);

        final WebElement downloadTemplateLink = new LazyWebElement(Locator.linkWithText("Download an Excel Template Workbook"), this);
        final WebElement submitButton = new LazyWebElement(Locator.lkButton("Submit"), this);
        final WebElement clearButton = new LazyWebElement(Locator.lkButton("Clear"), this);
    }
}
