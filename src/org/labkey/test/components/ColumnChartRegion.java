package org.labkey.test.components;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ColumnChartRegion//<EC extends Component.ElementCache> extends Component<EC>
{
    WebDriverWrapper _driver;
    DataRegionTable _dataRegionTable;

    public ColumnChartRegion(WebDriverWrapper driver, DataRegionTable dataRegionTable)
    {
        _driver = driver;
        _dataRegionTable = dataRegionTable;
    }

    public WebElement getComponentElement()
    {
        WebElement webElement;
        try
        {
            webElement = _dataRegionTable.findElement(By.cssSelector(" div.labkey-dataregion-msg-part-plotanalyticsprovider"));
        }
        catch(org.openqa.selenium.NoSuchElementException nse)
        {
            _driver.log("*** Couldn't find the column plot region. ***");
            webElement = null;
        }
        return webElement;
    }

    public List<WebElement> getPlots()
    {
        return getComponentElement().findElements(By.cssSelector(" div.labkey-dataregion-msg-plot-analytic"));
    }

    // Use getPlots to get the list of plot element, then pass one of those to this function.
    public ColumnChartComponent getColumnPlotWrapper(WebElement plotElement)
    {
        return new ColumnChartComponent(_driver, plotElement);
    }

    public boolean isRegionVisible()
    {
        if(null != getComponentElement())
            return getComponentElement().isDisplayed();
        else
            return false;
    }

    public void toggleRegion()
    {
        _dataRegionTable.findElement(By.cssSelector(" span.labkey-dataregion-msg-toggle")).click();
    }

    public void revertView()
    {
        WebElement revertButton = _dataRegionTable.findElement(By.cssSelector(" span.unsavedview-revert"));
        if(!revertButton.isDisplayed())
        {
            _driver.mouseOver(_dataRegionTable.findElement(By.cssSelector(" div.labkey-dataregion-msg-part-customizeview")));
        }
        revertButton.click();
    }

    public void saveView(boolean makeDefault, @Nullable String name, boolean availableToAll)
    {

        final String SAVE_VIEW_DIALOG_TITLE = "Save Custom Grid View";
        String dialogXpath;

        WebElement saveButton = _dataRegionTable.findElement(By.cssSelector(" span.unsavedview-save"));

        if(!saveButton.isDisplayed())
        {
            _driver.mouseOver(_dataRegionTable.findElement(By.cssSelector(" div.labkey-dataregion-msg-part-customizeview")));
        }

        saveButton.click();

        _driver._extHelper.waitForExtDialog(SAVE_VIEW_DIALOG_TITLE);

        dialogXpath = _driver._extHelper.getExtDialogXPath(SAVE_VIEW_DIALOG_TITLE);

        if(makeDefault)
            _driver.click(Locator.xpath(dialogXpath + "//label[contains(@class, 'x4-form-cb-label')][contains(text(), 'Default grid view')]/preceding-sibling::input"));
        else
        {
            _driver.click(Locator.xpath(dialogXpath + "//label[contains(@class, 'x4-form-cb-label')][contains(text(), 'Named')]/preceding-sibling::input"));
            _driver.setFormElement(Locator.xpath(dialogXpath + "//input[@name='saveCustomView_name']"), name);
        }

        if(availableToAll)
        {
            _driver.click(Locator.xpath(dialogXpath + "//label[contains(@class, 'x4-form-cb-label')][contains(text(), 'Make this grid view available')]/preceding-sibling::input"));
        }

        _driver.clickButton("Save");

    }

}
