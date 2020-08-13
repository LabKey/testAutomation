/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.DropdownButtonGroup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class Pager extends WebDriverComponent<Pager.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;
    //private final ResponsiveGrid _grid;

    protected Pager(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public Pager jumpToPage(String jumpTo)
    {
        elementCache().jumpToDropdown.clickSubMenu(jumpTo);
        return this;
    }
    public int getCurrentPage()
    {
        return Integer.parseInt(elementCache().jumpToDropdown.getButtonText());
    }

    public Pager selectPageSize(String pageSize)
    {
        elementCache().pageSizeDropdown.clickSubMenu(pageSize);
        return this;
    }

    public int getPageSize()
    {
        return Integer.parseInt(elementCache().pageSizeDropdown.getButtonText());
    }

    public Pager clickPrevious()
    {
        elementCache().prevButton.click();
        return this;
    }

    public boolean isPreviousEnabled()
    {
        return !elementCache().prevButton.getAttribute("disabled").equals("true");
    }

    public Pager clickNextButton()
    {
        elementCache().nextButton.click();
        return this;
    }

    public boolean isNextEnabled()
    {
        return !elementCache().nextButton.getAttribute("disabled").equals("true");
    }

    public int start()
    {
        String value = elementCache().counts.getAttribute("data-min");
        return Integer.parseInt(value);
    }

    public int end()
    {
        String value = elementCache().counts.getAttribute("data-max");
        return Integer.parseInt(value);
    }

    public int total()
    {
        String value = elementCache().counts.getAttribute("data-total");
        return Integer.parseInt(value);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        DropdownButtonGroup jumpToDropdown = new DropdownButtonGroup.DropdownButtonGroupFinder(getDriver())
                .withButtonId("current-page-drop-model").findWhenNeeded(this);
        DropdownButtonGroup pageSizeDropdown = new DropdownButtonGroup.DropdownButtonGroupFinder(getDriver())
                .withButtonId("page-size-drop-model").findWhenNeeded(this);

        final Locator.XPathLocator queryGridModelPagingCounts = Locator.tag("span").withAttribute("data-min");
        final Locator.XPathLocator queryModelPagingCounts = Locator.tagWithClass("span", "pagination-info");
        final Locator pagingCountsSpan = Locator.XPathLocator.union(queryGridModelPagingCounts, queryModelPagingCounts);

        final WebElement prevButton = Locator.XPathLocator.union(
                Locator.tagWithClass("button", "pagination-buttons__prev"),     // used in gridPanel
                Locator.tag("button").withChild(Locator.tagWithClass("i", "fa fa-chevron-left"))) // used in QueryGridPanel, here for back-support
                .findWhenNeeded(this).withTimeout(4000);
        final WebElement nextButton = Locator.XPathLocator.union(
                Locator.tagWithClass("button", "pagination-buttons__next"),
                Locator.tag("button").withChild(Locator.tagWithClass("i", "fa fa-chevron-right")))
                .findWhenNeeded(this).withTimeout(4000);

        public WebElement counts = pagingCountsSpan
                .refindWhenNeeded(getComponentElement()).withTimeout(4000);
    }

    public static class PagerFinder extends WebDriverComponentFinder<Pager, PagerFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.XPathLocator.union(
                Locator.tagWithClass("span", "paging"),             // used in QueryGridPanel, here for backwards-support
                Locator.tagWithClass("div", "lk-pagination"));      // used in GridPanel

        public PagerFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected Pager construct(WebElement el, WebDriver driver)
        {
            return new Pager(el, getDriver());
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
