/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.FilterStatusValue.FilterStatusValueFinder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class FilterStatus extends WebDriverComponent<FilterStatus.ElementCache>
{
    private final WebElement _filterStatusBoxElement;
    private final WebDriver _driver;

    private FilterStatus(WebElement element, WebDriver driver)
    {
        _filterStatusBoxElement = element;
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
        return _filterStatusBoxElement;
    }

    public List<FilterStatusValue> getValues()
    {
        return new FilterStatusValueFinder(getDriver()).findAll(this);
    }

    public List<FilterStatusValue> getFilterValues()
    {
        return new FilterStatusValueFinder(getDriver()).findAll(this).stream().filter(FilterStatusValue::isFilter).toList();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    { }

    public static class FilterStatusFinder extends WebDriverComponent.WebDriverComponentFinder<FilterStatus, FilterStatusFinder>
    {
        private final Locator _baseLocator = Locator.css("div.grid-panel__filter-status");

        public FilterStatusFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected FilterStatus construct(WebElement el, WebDriver driver)
        {
            return new FilterStatus(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
