package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.params.FieldKey;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.util.selenium.WebElementUtils.getTextContent;

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
    private Integer _queryWaitMsec = WAIT_FOR_JAVASCRIPT;

    protected DetailTable(WebElement tableElement, WebDriver driver)
    {
        _tableElement = tableElement;
        _driver = driver;
    }

    public DetailTable setQueryWait(int queryWaitMsec)
    {
        _queryWaitMsec = queryWaitMsec;
        return this;
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
        if(elementCache().dataByLabel(identifier).isDisplayed())
        {
            return elementCache().dataByLabel(identifier);
        }
        else if (elementCache().siblingField(identifier).isDisplayed())
        {
            return elementCache().siblingField(identifier);
        }
        else if (elementCache().dataFieldByKey(identifier).isDisplayed())
        {
            return elementCache().dataFieldByKey(identifier);
        }
        else
        {
            throw new NoSuchElementException(String.format("Could not find field '%s'.", identifier));
        }
    }

    public boolean fieldHasFormatPill(String identifier)
    {
        return Locator.tagWithClass("*", "status-pill").existsIn(getField(identifier));
    }

    /**
     * Return the value of a cell identified by the text in the left most column.
     *
     * @param fieldlabel The label of the field to get.
     * @return A value of the cell as a string.
     **/
    public String getFieldValue(String fieldlabel)
    {
        return getField(fieldlabel).getText();
    }

    /**
     * Gets the value of a cell identified by its data-fieldKey attribute
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
     * @param fieldLabel The label of the field to click.
     **/
    public void clickField(String fieldLabel)
    {
        String urlBefore = getWrapper().getCurrentRelativeURL().toLowerCase();

        // Should not click the container, it could be a td which would miss the clickable element.
        // Maybe this shouldn't assume an anchor but should be a generic(*)?
        Locator.tag("a").waitForElement(getField(fieldLabel), _queryWaitMsec).click();

        WebDriverWrapper.waitFor(()->!urlBefore.equals(getWrapper().getCurrentRelativeURL().toLowerCase()),
                String.format("Clicking field (link) '%s' did not navigate.", fieldLabel), 500);

    }

    /**
     * Returns a map of the values in the grid. The key is the first column and the value is the second column. The
     * first column is a property or attribute name or some identifier. The second column is the value of that property.
     *
     * @return A map with string values.
     **/
    public Map<String, String> getTableDataByLabel()
    {
        Map<String, String> tableData = new LinkedHashMap<>();

        for(WebElement tableRow : getComponentElement().findElements(By.cssSelector("tr")))
        {
            List<WebElement> tds = tableRow.findElements(By.tagName("td"));

            tableData.put(getTextContent(tds.get(0)), tds.get(1).getText());
        }

        return tableData;
    }

    /**
     * Returns a map of the values in the grid. Data is keyed by column FieldKeys.
     *
     * @return A map with string values.
     **/
    public Map<FieldKey, String> getTableDataByFieldKey()
    {
        Map<FieldKey, String> tableData = new LinkedHashMap<>();

        for(WebElement tableRow : Locator.tag("tr").findElements(getComponentElement()))
        {
            WebElement dataCell = Locator.tag("td").withAttribute("data-fieldkey").findElement(tableRow);

            tableData.put(FieldKey.fromFieldKey(dataCell.getDomAttribute("data-fieldkey")), dataCell.getText());
        }

        return tableData;
    }

    /**
     * Returns a map of the values in the grid. Data is keyed by column names.
     * Warning: Names are not guaranteed to be unique.
     *
     * @return A map with string values.
     **/
    public Map<String, String> getTableDataByName()
    {
        Map<FieldKey, String> tableDataByFieldKey = getTableDataByFieldKey();
        Map<String, String> tableDataByName = new LinkedHashMap<>();
        Map<String, FieldKey> collisionChecker = new HashMap<>();

        for (Map.Entry<FieldKey, String> entry : tableDataByFieldKey.entrySet())
        {
            String name = entry.getKey().getName();
            if (collisionChecker.containsKey(name))
            {
                throw new IllegalStateException("Ambiguous field name '%s' from FieldKeys '%s' and '%s'."
                    .formatted(name, collisionChecker.get(name), entry.getKey()));
            }
            collisionChecker.put(name, entry.getKey());
            tableDataByName.put(name, entry.getValue());
        }
        return tableDataByName;
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
        public final WebElement dataByLabel(String fieldLabel)
        {
            return Locator.tagWithAttribute("td", "data-caption", fieldLabel).findWhenNeeded(this)
                    .withTimeout(2000);
        }

        public final WebElement dataFieldByKey(String fieldKey)
        {
            return Locator.tagWithAttribute("td", "data-fieldkey", fieldKey)
                    .findWhenNeeded(this).withTimeout(2000);
        }

        // Some tables will show a value in a td with no attributes, use the td that has the text (label) to find the value.
        public final WebElement siblingField(String fieldLabel)
        {
            return Locator.tagContainingText("td", fieldLabel).followingSibling("td")
                    .findWhenNeeded(this).withTimeout(2000);
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
