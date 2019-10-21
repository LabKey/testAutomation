/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

public class QueryGrid extends WebDriverComponent
{

    final private WebDriver _driver;
    final private WebElement _queryGridPanel;
    private Optional<GridBar> _gridBar;
    private Optional<ResponsiveGrid> _responsiveGrid;

    protected QueryGrid(WebElement element, WebDriver driver)
    {
        _queryGridPanel = element;
        _driver = driver;

        _responsiveGrid = new ResponsiveGrid.ResponsiveGridFinder(_driver).findOptional(_queryGridPanel);

        if (_responsiveGrid.isPresent())
        {
            _gridBar = new GridBar.GridBarFinder(_driver, _responsiveGrid.get()).withQueryGrid().findOptional(_queryGridPanel);
        }
        else
        {
            _gridBar = Optional.empty();
        }

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
        _responsiveGrid = new ResponsiveGrid.ResponsiveGridFinder(_driver).findOptional(_queryGridPanel);
        return _responsiveGrid.orElseThrow();
    }

    public boolean hasGridBar()
    {
        return _gridBar.isPresent();
    }

    public boolean hasGrid()
    {
        return _responsiveGrid.isPresent();
    }

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
        return _gridBar.orElseThrow();
    }

    public int getRecordCount()
    {

        if(_gridBar.isPresent())
            return _gridBar.orElseThrow().getRecordCount();
        else
            return _responsiveGrid.orElseThrow().getRows().size();

    }

    public static class QueryGridFinder extends WebDriverComponentFinder<QueryGrid, QueryGridFinder>
    {
        private Locator _locator;

        public QueryGridFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locator.xpath("//div[contains(@class,'panel ')]//div[@class='table-responsive']/ancestor::div[@class='panel-body']");
        }

        public QueryGridFinder withGridId(String gridId)
        {
            _locator= Locator.xpath("//div[contains(@class, 'table-responsive')][@data-gridid='" + gridId + "']/ancestor::div[@class='panel-body']");
            return this;
        }

        public QueryGridFinder withPanelHeader(String panelHeader)
        {
            _locator= Locator.xpath("//div[@class='panel-heading'][text()='" + panelHeader + "']/parent::div[contains(@class,'panel')]//div[@class='panel-body']");
            return this;
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
