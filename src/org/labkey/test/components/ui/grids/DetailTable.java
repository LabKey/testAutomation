package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
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
public class DetailTable extends WebDriverComponent<DetailTable.ElementCache>
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

    @Override
    protected void waitForReady()
    {
        getWrapper().shortWait().withMessage("waiting for detailTable to load").until(wd -> isLoaded());
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
     * Rather than add yet another method to get a field value, do a 'best guess' to find the appropriate field. This
     * will return the first field that meets the criteria.
     *
     * @param identifier Some text string that can identify the field.
     * @return A web element that either had an attribute value equal to the identifier, or had a text in a sibling field (label) with the identifier.
     */
    private WebElement getField(String identifier)
    {
        if(elementCache().dataCaptionField(identifier).isDisplayed())
        {
            return elementCache().dataCaptionField(identifier);
        }
        else if (elementCache().siblingField(identifier).isDisplayed())
        {
            return elementCache().siblingField(identifier);
        }
        else
        {
            throw new NoSuchElementException(String.format("Could not find field '%s'.", identifier));
        }
    }

    /**
     * Return the value of a cell identified by the text in the left most column.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return A value of the cell as a string.
     **/
    public String getFieldValue(String fieldCaption)
    {
        return getField(fieldCaption).getText();
    }

    /**
     * Gets the value of a cell identified by it's data-fieldKey attribute
     * @param fieldKey  value of the data-fieldKey attribute on the intended element
     * @return  Text value of the specified element
     */
    public String getFieldValueByKey(String fieldKey)
    {
        return elementCache().dataFieldByKey(fieldKey).getText();
    }

    /**
     * Click on a cell in a grid.
     *
     * @param fieldCaption The caption/label of the field to click.
     **/
    public void clickField(String fieldCaption)
    {
        String urlBefore = getWrapper().getCurrentRelativeURL().toLowerCase();

        // Should not click the container, it could be a td which would miss the clickable element.
        // Maybe this shouldn't assume an anchor but should be a generic(*)?
        Locator.tag("a").waitForElement(getField(fieldCaption), 1500).click();

        WebDriverWrapper.waitFor(()->!urlBefore.equals(getWrapper().getCurrentRelativeURL().toLowerCase()),
                String.format("Clicking field (link) '%s' did not navigate.", fieldCaption), 500);

    }

    /**
     * Returns a map of the values in the grid. The key is the first column and the value is the second column. The
     * first column is a property or attribute name or some identifier. The second column is the value of that property.
     *
     * @return A map with string values.
     **/
    public Map<String, String> getTableData()
    {
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

        static final Locator loadingGrid = Locator.css("tbody tr.grid-loading");
        static final Locator emptyGrid = Locator.css("tbody tr.grid-empty");
        static final Locator spinner = Locator.css("span i.fa-spinner");

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public final WebElement dataCaptionField(String caption)
        {
            return Locator.tagWithAttribute("td", "data-caption", caption).findWhenNeeded(this);
        }

        public final WebElement dataFieldByKey(String fieldKey)
        {
            return Locator.tagWithAttribute("td", "data-fieldkey", fieldKey).findElement(this);
        }

        // Some tables will show a value in a td with no attributes, use the td that has the text (label) to find the value.
        public final WebElement siblingField(String caption)
        {
            return Locator.tagContainingText("td", caption).followingSibling("td").findWhenNeeded(this);
        }

    }

    public static class DetailTableFinder extends WebDriverComponent.WebDriverComponentFinder<DetailTable, DetailTableFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locators.detailTable;
        private Locator _locator;

        public DetailTableFinder(WebDriver driver)
        {
            super(driver);
            _locator= _baseLocator;
        }

        public DetailTableFinder withTitle(String title)
        {
            _locator = Locator.tagWithClass("div", "panel")
                    .withChild(Locator.tagWithClass("div", "panel-heading").startsWith(title))
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
