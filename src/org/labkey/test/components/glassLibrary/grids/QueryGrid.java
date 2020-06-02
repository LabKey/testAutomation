/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
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
public class QueryGrid extends WebDriverComponent
{

    final private WebDriver _driver;
    final private WebElement _queryGridPanel;
    private GridBar _gridBar;
    private ResponsiveGrid _responsiveGrid;
    private Optional<GridTabBar> _gridTabBar;

    protected QueryGrid(WebElement element, WebDriver driver)
    {
        _queryGridPanel = element;
        _driver = driver;

        _responsiveGrid = new ResponsiveGrid.ResponsiveGridFinder(_driver).find(_queryGridPanel);
        _gridBar = new GridBar.GridBarFinder(_driver, _queryGridPanel, _responsiveGrid).find();
        _gridTabBar = new GridTabBar.GridTabBarFinder(_driver, _responsiveGrid).findOptional(_queryGridPanel);
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

    public Map<String, String> getRowMap(int rowIndex)
    {
        return getGrid().getRowMap(rowIndex);
    }

    public Map<String, String> getRowMap(String containsText)
    {
        GridRow row = getGrid().getRow(containsText).orElseThrow(()->
                new NotFoundException("No row was found with value ["+ containsText +"]"));
        return row.getRowMap();
    }

    public Map<String, String> getRowMap(String containsText, String column)
    {
        GridRow row = getGrid().getRow(containsText, column).orElseThrow(()->
                new NotFoundException("No row was found with value ["+ containsText +"] in column ["+ column +"]"));
        return row.getRowMap();
    }

    public Map<String, String> getRowMap(Locator.XPathLocator containing)
    {
        return getGrid().getRow(containing).orElseThrow(()->
                new NotFoundException("No row was found with  ["+ containing +"]")).getRowMap();
    }

    public List<Map<String, String>> getRowMaps()
    {
        return getGrid().getRowMaps();
    }

    // row selection

    public QueryGrid selectRow(int index, boolean checked)
    {
        getGrid().getRows().get(index).select(checked);
        return this;
    }

    public QueryGrid selectRow(String column, String containsText, boolean checked)
    {
        getGrid().getRow(containsText, column).orElseThrow(()->
                new NotFoundException("No row was found with value ["+ containsText +"] in column ["+ column +"]"))
                .select(checked);
        return this;
    }


    public List<String> getColumnNames()
    {
        return getGrid().getColumnNames();
    }

    public boolean hasTabs() { return _gridTabBar.isPresent(); }

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

    public ResponsiveGrid getGrid()
    {
        _responsiveGrid = new ResponsiveGrid.ResponsiveGridFinder(_driver).find(_queryGridPanel);
        return _responsiveGrid;
    }

    public GridBar getGridBar()
    {
        return _gridBar;
    }

    public GridTabBar getGridTabBar()
    {
        return _gridTabBar.orElseThrow();
    }

    // record count

    public int getRecordCount()
    {
        return _gridBar.getRecordCount();
    }

    public QueryGrid waitForRecordCount(int expectedCount)
    {
        WebDriverWrapper.waitFor(()-> getRecordCount() == expectedCount,
                "did not get to the expected record count ["+expectedCount+"] in time",  WAIT_FOR_JAVASCRIPT);
        return this;
    }

    // search, sort and filter methods

    public QueryGrid search(String searchTerm)
    {
        getGrid().doAndWaitForUpdate(()->
                getGridBar().getOmniBox().setSearch(searchTerm));
        return this;
    }

    public QueryGrid sortOn(String column, SortDirection direction)
    {
        getGrid().doAndWaitForUpdate(()->
                getGridBar().getOmniBox().setSort(column, direction));
        return this;
    }

    public QueryGrid filterOn(String columnName, String operator, String value)
    {
        getGrid().doAndWaitForUpdate(()->
                getGridBar().getOmniBox().setFilter(columnName, operator, value));
        return this;
    }

    public QueryGrid clearSortsAndFilters()
    {
        getGrid().doAndWaitForUpdate(()->
                getGridBar().getOmniBox().clearAll());
        return this;
    }

    // select view
    public QueryGrid selectView(String viewName)
    {
        getGrid().doAndWaitForUpdate(()->
                getGridBar().doMenuAction("Grid Views", Arrays.asList(viewName)));
        return this;
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
            _locator= Locator.tagWithClass("div", "panel-body");
        }

        public QueryGridFinder inPanelWithHeaderText(String panelHeading)
        {
            _locator = Locator.tagWithClass("div", "panel")
                    .withChild(Locator.tagWithClass("div", "panel-heading").withText(panelHeading))
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
