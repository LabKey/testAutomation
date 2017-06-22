/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.components.ext4.widgets;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.html.Input;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.labkey.test.components.ext4.ComboBox.ComboBox;
import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.util.Ext4Helper.TextMatchTechnique.LEADING_NBSP;

public class SearchPanel extends WebDriverComponent<SearchPanel.ElementCache>
{
    private static final String LOAD_SIGNAL = "extSearchPanelLoaded";
    protected static final String DEFAULT_TITLE = "Search Criteria";
    private final WebElement _el;
    private final WebDriver _driver;

    public SearchPanel(WebElement el, WebDriver driver)
    {
        _el = el;
        _driver = driver;
        Locators.pageSignal(LOAD_SIGNAL).waitForElement(driver, 30000);
    }

    protected SearchPanel(String title, String idPrefix, WebDriver driver)
    {
        this(Locator.tag("div").attributeStartsWith("id", idPrefix)
                .withDescendant(Locator.tag("div").attributeStartsWith("id", idPrefix).attributeEndsWith("id", "_header").withText(title))
                .waitForElement(driver, 10000), driver);
    }

    public SearchPanel(String title, WebDriver driver)
    {
        this(title, "labkey-searchpanel-", driver);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public List<String> getAllSearchCriteria()
    {
        return getWrapper().getTexts(rowLabelLoc.findElements(this));
    }

    public void setView(String view)
    {
        elementCache().viewCombo.selectComboBoxItem(view);
    }

    public void setFilter(String fieldLabel, @Nullable String operator, String value)
    {
        if (operator != null)
        {
            elementCache().findFilterRow(fieldLabel)
                    .operator()
                    .selectComboBoxItem(operator);
        }
        elementCache().findFilterRow(fieldLabel)
                .value()
                .set(value);
    }

    public void selectValues(String fieldLabel, String... values)
    {
        elementCache().findFacetedRow(fieldLabel)
                .value()
                .selectComboBoxItem(LEADING_NBSP, values);
    }

    // NOTE: only using this for dates at the moment, though the code is more general
    public void setInput(String fieldName, String value)
    {
        Ext4FieldRef.getForName(getWrapper(), fieldName).setValue(value);
    }

    public DataRegionTable submit()
    {
        getWrapper().clickAndWait(elementCache().submitButton);
        return new DataRegionTable("query", getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        private final Map<String, SearchPanelFilterRow> filterRows = new TreeMap<>();
        protected SearchPanelFilterRow findFilterRow(String label)
        {
            if (!filterRows.containsKey(label))
                filterRows.put(label, new SearchPanelFilterRow(label));
            return filterRows.get(label);
        }

        private final Map<String, SearchPanelFacetedRow> facetedRows = new TreeMap<>();
        protected SearchPanelFacetedRow findFacetedRow(String label)
        {
            if (!facetedRows.containsKey(label))
                facetedRows.put(label, new SearchPanelFacetedRow(label));
            return facetedRows.get(label);
        }

        protected final ComboBox viewCombo = ComboBox(getDriver()).locatedBy(Locator.tag("table").attributeStartsWith("id", "labkey-viewcombo")).findWhenNeeded(this);
        protected final ComboBox containerCombo = null;
        protected final WebElement submitButton = Ext4Helper.Locators.ext4Button("Submit").findWhenNeeded(this);
    }

    private static final Locator.XPathLocator rowLoc = Locator.tagWithClass("div", "search-panel-row");
    private static final Locator.XPathLocator rowLabelLoc = Locator.tagWithClass("div", "search-panel-row-label");
    protected abstract class SearchPanelRow extends Component
    {
        private final WebElement row;
        private final String label;

        protected SearchPanelRow(String rowLabel)
        {
            this.label = rowLabel;
            row = rowLoc.withDescendant(rowLabelLoc.withText(rowLabel + ":")).findElement(SearchPanel.this);
        }

        @Override
        public WebElement getComponentElement()
        {
            return row;
        }

        public String getLabel()
        {
            return label;
        }
    }

    protected class SearchPanelFilterRow extends SearchPanelRow
    {
        private final ComboBox operatorCombo = ComboBox(getDriver())
                .locatedBy(Locator.tagWithClass("table", "search-panel-row-operator")).findWhenNeeded(this);
        private final Input fieldValueInput = Input(Locator.css(".search-panel-row-value input"), getDriver()).findWhenNeeded(this);

        protected SearchPanelFilterRow(String rowLabel)
        {
            super(rowLabel);
        }

        public ComboBox operator()
        {
            return operatorCombo;
        }

        public Input value()
        {
            return fieldValueInput;
        }
    }

    protected class SearchPanelFacetedRow extends SearchPanelRow
    {
        private final ComboBox valueCombo = ComboBox(getDriver())
                .locatedBy(Locator.css(".search-panel-row-value")).findWhenNeeded(this);

        protected SearchPanelFacetedRow(String rowLabel)
        {
            super(rowLabel);
        }

        public ComboBox value()
        {
            return valueCombo;
        }
    }
}
