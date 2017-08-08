package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.html.BootstrapMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PagingWidget extends WebDriverComponent<PagingWidget.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public PagingWidget(WebElement element, WebDriver driver)
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

    public PagingWidget collapseMenu()
    {
        if (elementCache().paginationMenu.isExpanded())
           elementCache().paginationMenu.collapse();
        return this;
    }

    public PagingWidget clickPreviousPage()
    {
        collapseMenu();
        elementCache().previousPageButton.click();
        return this;
    }

    public PagingWidget clickNextPage()
    {
        collapseMenu();
        elementCache().nextPageButton.click();
        return this;
    }

    public PagingWidget clickGoToFirst()
    {
        elementCache().paginationMenu.clickSubMenu(false,  "Show first");
        return this;
    }

    public PagingWidget clickGoToLast()
    {
        elementCache().paginationMenu.clickSubMenu(false,  "Show last");
        return this;
    }

    public PagingWidget setPageSize(int pageSize, boolean wait)
    {
        String menuText = String.valueOf(pageSize) + " per page";
        elementCache().paginationMenu.clickSubMenu(wait, "Paging", menuText);
        return this;
    }

    public PagingWidget viewPagingOptions()
    {
        elementCache().paginationMenu.openMenuTo( "Paging");
        return this;
    }

    public boolean menuOptionEnabled(String menuItemText, String... options)
    {
        elementCache().paginationMenu.openMenuTo( options);
        boolean result = !Locator.linkContainingText(menuItemText)
                .parent().findElement(this)
                .getAttribute("class").contains("disabled");
        collapseMenu();
        return result;
    }

    public boolean pagingButtonEnabled(boolean isPrevious)
    {
        boolean result;

        if (isPrevious)
            result = !elementCache().previousPageButton.getAttribute("class").contains("disabled");
        else
            result = !elementCache().nextPageButton.getAttribute("class").contains("disabled");

        collapseMenu();
        return result;
    }


    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        BootstrapMenu paginationMenu = new BootstrapMenu(getDriver(), getComponentElement());
        WebElement nextPageButton = Locator.xpath("//button[ ./i[@class='fa fa-chevron-right']]")
                .findWhenNeeded(getComponentElement());
        WebElement previousPageButton = Locator.xpath("//button[ ./i[@class='fa fa-chevron-left']]")
                .findWhenNeeded(getComponentElement());
    }
}