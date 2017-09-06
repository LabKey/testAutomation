package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class PagingWidget extends WebDriverComponent<PagingWidget.ElementCache>
{
    private final WebElement _el;
    private final DataRegionTable _dataRegionTable;

    public PagingWidget(DataRegionTable table)
    {
        _dataRegionTable = table;
        _el = Locator.xpath("//div[contains(@class,'labkey-pagination')]")
                .findWhenNeeded(_dataRegionTable.elements().getButtonBar());
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _dataRegionTable.getDriver();
    }

    public DataRegionTable getDataRegionTable()
    {
        return _dataRegionTable;
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
        getDataRegionTable().doAndWaitForUpdate(() ->
                elementCache().previousPageButton.click());
        return this;
    }

    public PagingWidget clickNextPage()
    {
        collapseMenu();
        getDataRegionTable().doAndWaitForUpdate(() ->
                elementCache().nextPageButton.click());
        return this;
    }

    public PagingWidget clickGoToFirst()
    {
        getDataRegionTable().doAndWaitForUpdate(() ->
                elementCache().paginationMenu.clickSubMenu(false,  "Show first"));
        return this;
    }

    public PagingWidget clickGoToLast()
    {
        getDataRegionTable().doAndWaitForUpdate(() ->
                elementCache().paginationMenu.clickSubMenu(false,  "Show last"));
        return this;
    }

    public PagingWidget setPageSize(int pageSize, boolean wait)
    {
        String menuText = String.valueOf(pageSize) + " per page";
        elementCache().paginationMenu.clickSubMenu(wait, "Paging", menuText);
        return this;
    }

    public List<WebElement> viewPagingOptions()
    {
        elementCache().paginationMenu.openMenuTo( "Paging", "100 per page");
        return elementCache().paginationMenu.findVisibleMenuItems();
    }

    public BootstrapMenu getMenu()
    {
        return elementCache().paginationMenu;
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

    public boolean hasPagingButton(boolean isPrevious)
    {
        if (isPrevious)
            return getWrapper().isElementPresent(elementCache().previousPageLoc);
        else
            return getWrapper().isElementPresent(elementCache().nextPageLoc);
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
        Locator.XPathLocator nextPageLoc = Locator.xpath("//button[ ./i[@class='fa fa-chevron-right']]");
        Locator.XPathLocator previousPageLoc = Locator.xpath("//button[ ./i[@class='fa fa-chevron-left']]");

        BootstrapMenu paginationMenu = new BootstrapMenu(getDriver(), getComponentElement());
        WebElement nextPageButton = nextPageLoc.findWhenNeeded(getComponentElement());
        WebElement previousPageButton = previousPageLoc.findWhenNeeded(getComponentElement());
    }
}