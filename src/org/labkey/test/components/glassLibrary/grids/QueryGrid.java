/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * The 'grid' element that contains two components.
 * <p>The first component is a grid header bar which contains the omni-box, the paging
 * controls, 'Select All' controls etc...
 * </p>
 * <p>The second component is the responsive grid which is the grid data.</p>
 */
public class QueryGrid extends ResponsiveGrid<QueryGrid>
{
    final private WebDriver _driver;
    final private WebElement _queryGridPanel;

    protected QueryGrid(WebElement element, WebDriver driver)
    {
        super(element, driver);
        _queryGridPanel = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _queryGridPanel;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    // get rowMaps

    /**
     * Returns the first row with a column text equivalent to the supplied text
     * @param text
     * @return
     */
    public Map<String, String> getRowMap(String text)
    {
        return getRow(text).getRowMap();
    }

    /**
     * Returns the first row with the supplied text in the specified column
     * @param column    The text in the column header cell
     * @param text
     * @return
     */
    public Map<String, String> getRowMap(String column, String text)
    {
        GridRow row = getRow(column, text);
        return row.getRowMap();
    }

    /**
     * returns the first row with matching text in the specified column(s)
     * @param partialMap Map where keys are columnText, values are full text
     * @return
     */
    public Map<String, String> getRowMap(Map<String, String> partialMap)
    {
        return getRow(partialMap).getRowMap();
    }

    /**
     * returns the first row with a descendant matching the supplied locator
     * @param containing
     * @return
     */
    public Map<String, String> getRowMap(Locator.XPathLocator containing)
    {
        return getRow(containing).getRowMap();
    }

    // row selection

    /**
     * Selects or un-selects the first row with the specified text in the specified column
     * @param column
     * @param text
     * @param checked   whether or not to check the box
     * @return
     */
    public QueryGrid selectRow(String column, String text, boolean checked)
    {
        getRow(column, text).select(checked);
        return this;
    }

    public boolean hasTabs() { return elementCache().gridTabBar().isPresent(); }

    public boolean gridErrorMessagePresent()
    {
        try
        {
            WebElement alert = Locator.tagWithClass("div", "alert").findElement(this);
            return alert.isDisplayed();
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }

    }

    public String getGridErrorMessage()
    {
        String errorMsg = "";

        if(gridErrorMessagePresent())
        {
            errorMsg = Locator.tagWithClass("div", "alert").findElement(this).getText();
        }

        return errorMsg;
    }

    // subcomponent getters

    public GridBar getGridBar()
    {
        return elementCache()._gridBar;
    }

    public GridTabBar getGridTabBar()
    {
        return elementCache().gridTabBar().orElseThrow();
    }

    // record count

    public int getRecordCount()
    {
        return elementCache()._gridBar.getRecordCount();
    }

    public QueryGrid waitForRecordCount(int expectedCount)
    {
        return waitForRecordCount(expectedCount, WAIT_FOR_JAVASCRIPT);
    }

    public QueryGrid waitForRecordCount(int expectedCount, int milliseconds)
    {
        WebDriverWrapper.waitFor(()-> getRecordCount() == expectedCount,
                "did not get to the expected record count ["+expectedCount+"] in time",  milliseconds);
        return this;
    }

    // search, sort and filter methods

    /**
     * searches the grid, from the omnibox and waits for the grid to refresh
     * @param searchTerm
     * @return
     */
    public QueryGrid search(String searchTerm)
    {
        doAndWaitForUpdate(()->
                getGridBar().getOmniBox().setSearch(searchTerm));
        return this;
    }

    /**
     * Applies a sort to the grid via the omnibox and waits for the grid to refresh
     * @param column
     * @param direction
     * @return
     */
    public QueryGrid sortOn(String column, SortDirection direction)
    {
        doAndWaitForUpdate(()->
                getGridBar().getOmniBox().setSort(column, direction));
        return this;
    }

    /**
     * adds a filter expression to the table via the omnibox, and waits for the grid to update
     * @param columnName
     * @param operator
     * @param value
     * @return
     */
    public QueryGrid filterOn(String columnName, String operator, String value)
    {
        doAndWaitForUpdate(()->
                getGridBar().getOmniBox().setFilter(columnName, operator, value));
        return this;
    }

    /**
     * clears search, sort, and filter expressions via the omnibox
     * @return
     */
    public QueryGrid clearSortsAndFilters()
    {
        doAndWaitForUpdate(()->
                getGridBar().getOmniBox().clearAll());
        return this;
    }

    /**
     *
     * @return
     */
    public QueryGrid selectAllInSet()
    {
        doAndWaitForUpdate(()->
                getGridBar().selectAllInSet());
        return this;
    }

    public QueryGrid clearAllSelections()
    {
        doAndWaitForUpdate(()->
                getGridBar().clearAllInSet());
        return this;
    }


    // select view
    public QueryGrid selectView(String viewName)
    {
        doAndWaitForUpdate(()->
                getGridBar().doMenuAction("Grid Views", Arrays.asList(viewName)));
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ResponsiveGrid.ElementCache
    {
        ResponsiveGrid _responsiveGrid = new ResponsiveGrid.ResponsiveGridFinder(_driver).findWhenNeeded(_queryGridPanel);
        GridBar _gridBar = new GridBar.GridBarFinder(_driver, _queryGridPanel, _responsiveGrid).findWhenNeeded();
        Optional<GridTabBar> gridTabBar()
        {
            return new GridTabBar.GridTabBarFinder(_driver, _responsiveGrid).findOptional(_queryGridPanel);
        }
    }

    public static class QueryGridFinder extends WebDriverComponentFinder<QueryGrid, QueryGridFinder>
    {
        private Locator _locator;

        /**
         * Find the first div with a class of panel-body and assume the grid panel is in there.
         *
         * @param driver Reference to a WebDriver.
         */
        public QueryGridFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locator.tagWithClass("div", "panel-body")
                    .withDescendant(ResponsiveGrid.Locators.responsiveGrid());
        }

        public QueryGridFinder inPanelWithHeaderText(String panelHeading)
        {
            _locator = Locator.tagWithClass("div", "panel")
                    .withChild(Locator.tagWithClass("div", "panel-heading").withText(panelHeading))
                    .withDescendant(ResponsiveGrid.Locators.responsiveGrid())
                    .child(Locator.tagWithClass("div", "panel-body"));
            return this;
        }

        /**
         * Given a containing web element find the grid panel in it.
         *
         * @param driver Reference to a WebDriver.
         * @param containingPanel A locator to scope the search for a gridPanel.
         */
        public QueryGridFinder(WebDriver driver, Locator containingPanel)
        {
            super(driver);
            _locator= containingPanel;
        }

        @Override
        protected QueryGrid construct(WebElement el, WebDriver driver)
        {
            return new QueryGrid(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
