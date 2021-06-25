/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ui.OmniBox;
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
     * @param columnLabel    The text in the column header cell
     * @param text
     * @return
     */
    public Map<String, String> getRowMap(String columnLabel, String text)
    {
        GridRow row = getRow(columnLabel, text);
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
     * @param columnLabel
     * @param text
     * @param checked   whether or not to check the box
     * @return
     */
    public QueryGrid selectRow(String columnLabel, String text, boolean checked)
    {
        getRow(columnLabel, text).select(checked);
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

    public OmniBox getOmniBox()
    {
        return elementCache().omniBox;
    }

    public GridTabBar getGridTabBar()
    {
        return elementCache().gridTabBar().orElseThrow();
    }

    // record count

    public int getRecordCount()
    {
        return getGridBar().getRecordCount();
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
        doAndWaitForUpdate(()-> getOmniBox().setSearch(searchTerm));
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
        doAndWaitForUpdate(()-> getOmniBox().setSort(column, direction));
        return this;
    }

    /**
     * Adds a filter expression to the table via the omnibox, and waits for the grid to update.
     *
     * @param columnName Name of the column to filter on.
     * @param operator Enum value of the operator {@link OmniBox.FilterOperator}.
     * @param value The value to compare to.
     * @return The QueryGrid after the filter has been applied.
     */
    public QueryGrid filterOn(String columnName, OmniBox.FilterOperator operator, String value)
    {
        return filterOn(columnName, operator.getValue(), value);
    }

    /**
     * @deprecated Use the filterOn method that takes an enum.
     * @see QueryGrid#filterOn(String, OmniBox.FilterOperator, String) 
     *
     * @param columnName
     * @param operator
     * @param value
     * @return
     */
    @Deprecated
    public QueryGrid filterOn(String columnName, String operator, String value)
    {
        doAndWaitForUpdate(()-> getOmniBox().setFilter(columnName, operator, value));
        return this;
    }

    /**
     * clears search, sort, and filter expressions via the omnibox
     * @return
     */
    public QueryGrid clearSortsAndFilters()
    {
        doAndWaitForUpdate(()-> getOmniBox().clearAll());
        return this;
    }

    /**
     *  Selects all rows in the target domain, including those on other pages, if there are any
     * @return
     */
    public QueryGrid selectAllRows()
    {
        if (isGridPanel())
        {
            if (elementCache().selectAllBtnLoc.existsIn(this))
                doAndWaitForUpdate(()->
                        elementCache().selectAllN_Btn().click());
            else
                doAndWaitForUpdate(() ->
                        selectAllOnPage(true, null));
        }
        else
            doAndWaitForUpdate(() ->
                    getGridBar().selectAllRows());

        return this;
    }

    public boolean hasItemsSelected()
    {
        return Locator.tagWithClass("span", "selection-status__count").existsIn(this);
    }

    public String getSelectionStatusCount()
    {   // note: this element is only present when some number of rows in the set are selected
        WebElement selectionStatus = Locator.tagWithClass("span", "selection-status__count")
                .waitForElement(this, 4000);
        return selectionStatus.getText();
    }

    public QueryGrid clearAllSelections()
    {
        if(hasItemsSelected())
        {
            if (isGridPanel())
            {
                if (elementCache().clearBtnLoc.existsIn(this))
                    doAndWaitForUpdate(() ->
                            elementCache().clearAllSelectionStatusBtn().click());
                else
                    doAndWaitForUpdate(() ->
                            selectAllOnPage(false));
            }
            else
                doAndWaitForUpdate(() ->
                        getGridBar().clearAllSelections());
        }

        return this;
    }


    // select view
    public QueryGrid selectView(String viewName)
    {
        doAndWaitForUpdate(()->
                getGridBar().doMenuAction("Grid Views", Arrays.asList(viewName)));
        return this;
    }

    /**
     * possible this is either a GridPanel, or a QueryGridPanel (QGP is to be deprecated).
     * use this to test which one so we can fork behavior until QGP is gone
     * @return
     */
    private boolean isGridPanel()
    {
        return elementCache().selectionStatusContainerLoc.existsIn(this);
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
        OmniBox omniBox = new OmniBox.OmniBoxFinder(_driver).findWhenNeeded(this);
        Optional<GridTabBar> gridTabBar()
        {
            return new GridTabBar.GridTabBarFinder(_driver, _responsiveGrid).findOptional(_queryGridPanel);
        }

        Locator selectionStatusContainerLoc = Locator.tagWithClass("div", "selection-status");
        Locator selectAllBtnLoc = Locator.tagWithClass("span", "selection-status__select-all")
                .child(Locator.buttonContainingText("Select all"));
        Locator clearBtnLoc = Locator.tagWithClass("span", "selection-status__clear-all")
                .child(Locator.tagContainingText("button", "Clear"));

        WebElement selectionStatusContainer()
        {
            return selectionStatusContainerLoc.findElement(this);
        }
        WebElement clearAllSelectionStatusBtn()
        {
            return clearBtnLoc.findElement(selectionStatusContainer());
        }
        WebElement selectAllN_Btn()
        {
            return selectAllBtnLoc.findElement(selectionStatusContainer());
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
