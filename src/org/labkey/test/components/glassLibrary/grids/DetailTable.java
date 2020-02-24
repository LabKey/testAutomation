package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a 'special' table that has only two columns, and no header. An example of this table can be seen in the
 * Sample Detail page. The first column contains the list of attributes for a given sample, and the second column
 * contains the values of the attributes.
 */
public class DetailTable extends WebDriverComponent
{
    private final WebElement _tableElement;
    private final WebDriver _driver;

    protected DetailTable(WebElement tableElement, WebDriver driver)
    {
        _tableElement = tableElement;
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _tableElement;
    }

    public Boolean isLoaded()
    {
        // Need to wrap the checks in a try / catch for a stale element exception. This can happen because the "this"
        // reference can go stale after editing a sample and reloading the grid happening.
        try
        {
            return !Locators.loadingGrid.existsIn(this) &&
                    !Locators.spinner.existsIn(this) &&
                    Locator.tag("td").existsIn(this);
        }
        catch(StaleElementReferenceException stale)
        {
            return false;
        }

    }

    // TODO Not sure if the get & click methods are correct (or appropriate?), for a @glass component.
    //  It may be appropriate to have these interfaces but maybe the way the cell is identified should be different.

    /**
     * Return the value of a cell identified by the text in the left most column.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return A value of the cell as a string.
     **/
    public String getFieldValue(String fieldCaption)
    {
        return Locators.fieldValue(fieldCaption).findElement(getDriver()).getText();
    }

    /**
     * Click on a cell in a grid.
     *
     * @param fieldCaption The caption/label of the field to click.
     **/
    public void clickField(String fieldCaption)
    {
        Locators.fieldValue(fieldCaption).findElement(getDriver()).click();
    }

    /**
     * Returns a list of a list of the values in the grid. All values are treated a string.
     *
     * @return A list of list of strings.
     **/
    public List<List<String>> getTableData()
    {
        List<List<String>> tableData = new ArrayList<>();

        for(WebElement tableRow : getComponentElement().findElements(By.cssSelector("tr")))
        {
            List<WebElement> tds = tableRow.findElements(By.tagName("td"));
            List<String> rowData = new ArrayList<>();

            for(WebElement td : tds)
            {
                rowData.add(td.getText());
            }

            tableData.add(rowData);
        }

        return tableData;
    }

    protected static abstract class Locators
    {
        static final Locator detailTable = Locator.css("table.detail-component--table__fixed");

        static Locator fieldValue(String caption)
        {
            return Locator.tagWithAttribute("td", "data-caption", caption);
        }

        static final Locator loadingGrid = Locator.css("tbody tr.grid-loading");
        static final Locator emptyGrid = Locator.css("tbody tr.grid-empty");
        static final Locator spinner = Locator.css("span i.fa-spinner");

    }

    public static class DetailTableFinder extends WebDriverComponent.WebDriverComponentFinder<DetailTable, DetailTableFinder>
    {
        private Locator _locator;

        public DetailTableFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locators.detailTable;
        }

        @Override
        protected DetailTable construct(WebElement el, WebDriver driver)
        {
            return new DetailTable(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
