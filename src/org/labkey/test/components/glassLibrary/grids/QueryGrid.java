/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

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

    public ResponsiveGrid getGrid()
    {

        // Find the grid if needed. If it has been update the reference created in the constructor should go stale.
        // Calling isEnable should be a benign operation.
        // Didn't want to call satelnessOf because it requires a wait. If the grid has been update stalenessOf should
        // return right away. If the grid hasn't been updated the wait time would have to expire.
        try
        {
            _responsiveGrid.getComponentElement().isEnabled();
        }
        catch(StaleElementReferenceException se)
        {
            _responsiveGrid = new ResponsiveGrid.ResponsiveGridFinder(_driver).find(_queryGridPanel);
        }

        return _responsiveGrid;
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

    public GridBar getGridBar()
    {
        return _gridBar;
    }

    public GridTabBar getGridTabBar()
    {
        return _gridTabBar.orElseThrow();
    }

    public int getRecordCount()
    {
        return _gridBar.getRecordCount();
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
