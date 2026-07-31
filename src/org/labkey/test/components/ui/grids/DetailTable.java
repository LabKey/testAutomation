/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.params.FieldKey;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

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
        getWrapper().shortWait().withMessage("waiting for detailTable to load").until(_ -> isLoaded());
    }

    public Boolean isLoaded()
    {
        return !Locators.loadingGrid.existsIn(this) &&
                !Locators.spinner.existsIn(this) &&
                Locator.tag("td").existsIn(this);
    }

    /**
     * Rather than add yet another method to get a field value, do a 'best guess' to find the appropriate field. This
     * will return the first field that meets the criteria.
     *
     * @param identifier fieldKey, name, or label
     * @return A web element resolved by fieldKey, name, or label; or, for tables without those data attributes,
     * a value cell found by its sibling label text.
     */
    private WebElement getField(CharSequence identifier)
    {
        FieldReferenceManager.FieldReference fieldReference = elementCache().getFieldManager().findFieldReferenceOrNull(identifier);
        if (fieldReference != null)
        {
            return fieldReference.getElement();
        }
        else if (elementCache().siblingField(identifier).isDisplayed())
        {
            // Track down where/if this is still needed
            TestLogger.info("sibling field " + identifier + " found");
            return elementCache().siblingField(identifier);
        }
        else
        {
            throw new NoSuchElementException(String.format("Could not find field '%s'.", identifier));
        }
    }

    /**
     * @param identifier fieldKey, name, or label
     * @return True if the field is present, false otherwise.
     */
    public boolean hasField(CharSequence identifier)
    {
        try
        {
            getField(identifier);
            return true;
        }
        catch (NoSuchElementException nse)
        {
            return false;
        }
    }

    /**
     * @param identifier fieldKey, name, or label
     * @return True if the field's value has a conditional-format status pill applied.
     */
    public boolean fieldHasFormatPill(CharSequence identifier)
    {
        return Locator.tagWithClass("*", "status-pill").existsIn(getField(identifier));
    }

    /**
     * Return the value of a field cell, resolved by fieldKey, name, or label.
     *
     * @param identifier fieldKey, name, or label
     * @return The value of the cell as a string.
     **/
    public String getFieldValue(CharSequence identifier)
    {
        return getField(identifier).getText();
    }

    /**
     * Gets the value of a cell identified by its data-fieldKey attribute
     * @param identifier value of the data-fieldKey attribute on the intended element
     * @return  Text value of the specified element
     * @deprecated Use {@link #getFieldValue(CharSequence)} instead; it now resolves fieldKey, name, and label alike.
     */
    @Deprecated (since = "26.8")
    public String getFieldValueByKey(CharSequence identifier)
    {
        return getFieldValue(identifier);
    }

    /**
     * Click on a cell in a grid.
     *
     * @param identifier fieldKey, name, or label
     **/
    public void clickField(CharSequence identifier)
    {
        String urlBefore = getWrapper().getCurrentRelativeURL().toLowerCase();

        // Should not click the container, it could be a td which would miss the clickable element.
        // Maybe this shouldn't assume an anchor but should be a generic(*)?
        Locator.tag("a").waitForElement(getField(identifier), _queryWaitMsec).click();

        WebDriverWrapper.waitFor(()->!urlBefore.equals(getWrapper().getCurrentRelativeURL().toLowerCase()),
                String.format("Clicking field (link) '%s' did not navigate.", identifier), 500);

    }

    /**
     * Returns a map of the values in the grid, keyed by each field's label.
     *
     * @return A map with string values.
     **/
    public Map<String, String> getTableDataByLabel()
    {
        Map<String, String> tableData = new LinkedHashMap<>();

        for (FieldReferenceManager.FieldReference fieldReference : elementCache().getFieldManager().getColumnHeaders())
        {
            tableData.put(fieldReference.getLabel(), fieldReference.getElement().getText());
        }

        return tableData;
    }

    /**
     * Returns a map of the values in the grid, keyed by each field's FieldKey.
     *
     * @return A map with string values.
     **/
    public Map<FieldKey, String> getTableDataByFieldKey()
    {
        Map<FieldKey, String> tableData = new LinkedHashMap<>();

        for (FieldReferenceManager.FieldReference fieldReference : elementCache().getFieldManager().getColumnHeaders())
        {
            tableData.put(fieldReference.getFieldKey(), fieldReference.getElement().getText());
        }

        return tableData;
    }

    /**
     * Returns a map of the values in the grid, keyed by each field's name.
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
        static final Locator spinner = Locator.css("span i.fa-spinner");

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<ElementCache>.ElementCache
    {
        // Some tables will show a value in a td with no attributes, use the td that has the text (label) to find the value.
        public final WebElement siblingField(CharSequence identifier)
        {
            return Locator.tagContainingText("td", identifier.toString()).followingSibling("td").findWhenNeeded(this);
        }

        private FieldReferenceManager _fieldReferenceManager;

        @LogMethod
        private FieldReferenceManager getFieldManager()
        {
            if (_fieldReferenceManager == null)
            {
                List<DetailTableFieldReference> columnHeaders = new ArrayList<>();

                List<WebElement> valueCells = Locator.tagWithAttribute("td", "data-fieldkey").findElements(this);
                // Use JavaScript to get fieldKeys and captions in one operation, rather than making 2N calls to 'WebElement.getDomAttribute'
                List<List<String>> captionsAndKeys = getWrapper().executeScript(
                    """
                    var cells = arguments[0];
                    var captions = [];
                    var fieldkeys = [];
                    for (var i = 0; i < cells.length; i++)
                    {
                        captions.push(cells[i].dataset.caption);
                        fieldkeys.push(cells[i].dataset.fieldkey);
                    }
                    return [captions, fieldkeys];
                    """, List.class,
                valueCells);
                List<String> captions = captionsAndKeys.get(0);
                List<String> fieldkeys = captionsAndKeys.get(1);
                for (int i = 0; i < valueCells.size(); i++)
                {
                    columnHeaders.add(new DetailTableFieldReference(valueCells.get(i), i, fieldkeys.get(i), captions.get(i)));
                }

                _fieldReferenceManager = new FieldReferenceManager(columnHeaders);
            }

            return _fieldReferenceManager;
        }
    }

    private static class DetailTableFieldReference extends FieldReferenceManager.FieldReference
    {
        private final FieldKey _fieldKey;
        private final String _label;

        public DetailTableFieldReference(WebElement element, int domIndex, String fieldKey, String label)
        {
            super(element, domIndex);
            _fieldKey = FieldKey.fromFieldKey(fieldKey);
            _label = label;
        }

        @Override
        public FieldKey getFieldKey()
        {
            return _fieldKey;
        }

        @Override
        public String getLabel()
        {
            return _label;
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
                    .withChild(Locator.byClass("panel-heading").startsWith(title))
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
