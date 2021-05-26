package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a 'special' table that has only two columns, and no header. An example of this table can be seen in the
 * Sample Detail page. The first column contains the list of attributes for a given sample, and the second column
 * contains the values of the attributes.
 *
 * The component it automates is implemented in /components/src/public/QueryModel/DetailPanel.tsx
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
        // reference can go stale after editing a sample and reloading the grid with the updated data is happening.
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
     * Gets the value of a cell identified by it's data-fieldKey attribute
     * @param fieldKey  value of the data-fieldKey attribute on the intended element
     * @return  Text value of the specified element
     */
    public String getFieldValueByKey(String fieldKey)
    {
        return Locator.tagWithAttribute("td", "data-fieldkey", fieldKey)
                .findElement(this).getText();
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
     * Returns a map of the values in the grid. The key is the first column and the value is the second column. The
     * first column is a property or attribute name or some identifier. The second column is the value of that property.
     *
     * @return A map with string values.
     **/
    public Map<String, String> getTableData()
    {
        // Explicitly check that the table has been loaded before trying to get the data.
        getWrapper().waitFor(this::isLoaded, "Cannot get the table data because the table is not loaded.", 500);

        Map<String, String> tableData = new HashMap<>();

        for(WebElement tableRow : getComponentElement().findElements(By.cssSelector("tr")))
        {
            List<WebElement> tds = tableRow.findElements(By.tagName("td"));

            tableData.put(tds.get(0).getText(), tds.get(1).getText());
        }

        return tableData;
    }

    protected static abstract class Locators
    {
        static final Locator.XPathLocator detailTable = Locator.tagWithClass("table", "detail-component--table__fixed");

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
        private Locator.XPathLocator _baseLocator = Locators.detailTable;
        private Locator _locator;

        public DetailTableFinder(WebDriver driver)
        {
            super(driver);
            _locator= _baseLocator;
        }

        public DetailTableFinder withTitle(String title)
        {
            _locator = Locator.tagWithClass("div", "panel")
                    .withChild(Locator.tagWithClass("div", "panel-heading").withText(title))
                    .descendant(_baseLocator);
            return this;
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
