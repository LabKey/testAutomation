/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.MultiMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Wrapper for UI component defined in 'packages/components/src/internal/components/gridbar/PageSizeSelector.tsx'
 * Or maybe 'packages/components/src/internal/components/pagination/PageSizeMenu.tsx'
 */
public class Pager extends WebDriverComponent<Pager.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;
    private final UpdatingComponent _pagedComponent;

    protected Pager(WebElement element, UpdatingComponent component, WebDriver driver)
    {
        _el = element;
        _driver = driver;
        _pagedComponent = component;
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

    public Pager jumpToPage(String jumpTo)      // only works on GridPanel
    {
        _pagedComponent.doAndWaitForUpdate(()->
                elementCache().jumpToDropdown.clickSubMenu(false, jumpTo));

        return this;
    }

    public MultiMenu getDropDownMenu()
    {
        elementCache().jumpToDropdown.expand();
        return elementCache().jumpToDropdown;
    }

    public int getCurrentPage()                 // only works on GridPanel
    {
        return Integer.parseInt(elementCache().currentPageButton.getText());
    }

    public Pager selectPageSize(String pageSize)    // only works on GridPanel
    {
        int currentPageSize = getPageSize();
        if(currentPageSize != Integer.parseInt(pageSize))
        {
            _pagedComponent.doAndWaitForUpdate(() -> elementCache().jumpToDropdown.clickSubMenu(false, pageSize));
        }
        return this;
    }

    public int getPageSize()                // only works on GridPanel
    {
        // Changing the jumpToDropdown button from the deprecated DropdownButtonGroup class to a MultiMenu type has changed
        // the way that various text from the control is gathered. Getting the current page size now requires that the dropdown
        // be expanded and the selected page size found in the list.

        elementCache().jumpToDropdown.expand();

        // Find the selected li element in the page size list (//div[@class='grid-panel__button-bar']//ul[contains(@aria-labelledby,'current-page-drop')]//li[@class='active'])
        WebElement activeLi = Locator.tagWithAttributeContaining("ul", "aria-labelledby", "current-page-drop").childTag("li").withAttribute("class", "active").findElement(this);

        int size = Integer.parseInt(activeLi.getText());
        elementCache().jumpToDropdown.collapse();

        return size;
    }

    /**
     * Helper to see if the paging menu is visible.
     * Basically this is to check issue 45451.
     *
     * @return True if the dropdown page menu is visible, false otherwise.
     */
    public boolean isPagingMenuVisible()
    {
        Locator dropMenuLocator = Locator.tagWithClass("ul", "dropdown-menu");
        WebElement gridPanel = Locator.tagWithClass("div", "grid-panel__body").findElement(getDriver());
        WebElement dropDownMenu = dropMenuLocator.findWhenNeeded(gridPanel);
        return dropDownMenu.isDisplayed();
    }

    public boolean hasPaginationControls()
    {
        return Locator.tagWithClass("div", "pagination-button-group")
                .findWhenNeeded(getComponentElement()).isDisplayed();
    }

    public Pager clickPrevious()
    {
        WebElement button = elementCache().prevButton();
        _pagedComponent.doAndWaitForUpdate(() ->
        {
            getWrapper().scrollIntoView(button);
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(button));
            button.click();
        });
        return this;
    }

    public boolean isPreviousEnabled()
    {
        WebElement button = elementCache().prevButton();
        return button.isDisplayed() && isButtonEnabled(button);
    }

    public Pager clickNext()
    {
        WebElement button = elementCache().nextButton();
        _pagedComponent.doAndWaitForUpdate(() ->
        {
            getWrapper().scrollIntoView(button);
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(button));
            button.click();
        });
        return this;
    }

    public boolean isNextEnabled()
    {
        WebElement button = elementCache().nextButton();
        return button.isDisplayed() && isButtonEnabled(button);
    }

    public int start()
    {
        // note: the 'old' element here and in end() and total() are here to support the pager used in
        // biologicsReportTest.testReportListPaging.  Hopefully, we can consolidate dom between that pager
        // and the ones we now have in QueryGrid

        WebElement oldStart = Locator.tagWithClass("span", "pagination-info__start")
                .findElementOrNull(this);
        if (oldStart != null)
            return Integer.parseInt(oldStart.getText());
        else
        {
            String value = elementCache().counts.getAttribute("data-min");
            return Integer.parseInt(value);
        }
    }

    public int end()
    {
        WebElement oldEnd = Locator.tagWithClass("span", "pagination-info__end")
                .findElementOrNull(this);
        if (oldEnd != null)
            return Integer.parseInt(oldEnd.getText());
        else
        {
            String value = elementCache().counts.getAttribute("data-max");
            return Integer.parseInt(value);
        }
    }

    public int total()
    {
        WebElement oldTotal = Locator.tagWithClass("span", "pagination-info__total")
                .findElementOrNull(this);
        if (oldTotal != null)
            return Integer.parseInt(oldTotal.getText());
        else
        {
            String value = elementCache().counts.getAttribute("data-total");
            return Integer.parseInt(value);
        }
    }

    /**
     * if the pager does not show a total, it indicates that there are no more to the set than are shown on the current page.
     * use this to understand whether or not there are more pages of information (which
     * could be the whole set, or a filtered range of it, or both).
     * @return <code>true</code> if pager shows a "total"
     */
    public boolean hasTotal()
    {
        return elementCache().counts.getAttribute("data-total") != null;
    }

    public String summary()
    {
        return elementCache().counts.getText();
    }

    private boolean isButtonEnabled(WebElement btn)
    {
        return btn.isEnabled() && !btn.getAttribute("class").contains("disabled-button");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        // Was previously a DropdownButtonGroup type, which is a deprecated class.
        MultiMenu jumpToDropdown = new MultiMenu.MultiMenuFinder(getDriver())
                .withButtonClass("current-page-dropdown").findWhenNeeded(this);

        WebElement currentPageButton = Locator.tagWithClass("button", "current-page-dropdown").refindWhenNeeded(this);

        final Locator.XPathLocator pagingCountsSpan = Locator.tagWithClass("span", "pagination-info");

        WebElement prevButton()
        {
            return Locator.XPathLocator.union(
                    Locator.tagWithClass("button", "pagination-button--previous"),     // used in GridPanel
                    Locator.tagWithClass("button", "pagination-buttons__prev"))     // used in ReportList
                    .findWhenNeeded(this);
        }

        WebElement nextButton()
        {
            return Locator.XPathLocator.union(
                    Locator.tagWithClass("button", "pagination-button--next"), // used in GridPanel
                    Locator.tagWithClass("button", "pagination-buttons__next")) // used in ReportList
                    .findWhenNeeded(this);
        }

        public WebElement counts = pagingCountsSpan
                .refindWhenNeeded(getComponentElement()).withTimeout(4000);
    }

    public static class PagerFinder extends WebDriverComponentFinder<Pager, PagerFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.XPathLocator.union(
                Locator.tagWithClass("div", "pagination-buttons"),  // used in biologics report list
                Locator.tagWithClass("div", "lk-pagination"));      // used in GridPanel
        private final UpdatingComponent _pagedComponent;

        public PagerFinder(WebDriver driver, UpdatingComponent pagedComponent)
        {
            super(driver);
            _pagedComponent = pagedComponent;
        }

        @Override
        protected Pager construct(WebElement el, WebDriver driver)
        {
            return new Pager(el, _pagedComponent, getDriver());
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
