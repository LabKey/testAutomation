package org.labkey.test.components.ui.grids;


import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;




public class LineageGridRow extends GridRow
{
    private Locator seedLocator = Locator.tagWithClass("span", "show-on-hover").withText("Seed");
    private Locator dupeLocator = Locator.tagWithClass("span", "label-warning").withText("Duplicate");
    private Locator firstParentLoc = Locator.tagWithClass("span", "label-info").withText("1st parent");
    private Locator secondParentLoc = Locator.tagWithClass("span", "label-primary").withText("2nd parent");

    protected LineageGridRow(ResponsiveGrid grid, WebElement el, WebDriver driver)
    {
        super(grid, el, driver);
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

    public String getLineageName()
    {
        return Locator.tag("a").findElement(elementCache().lineageNameElement).getText();
    }

    public String getLineageNameTitle()
    {
        return elementCache().lineageNameElement.getAttribute("title");
    }

    public boolean isSeed()
    {
        return seedLocator.existsIn(elementCache().lineageNameElement);
    }

    public boolean isDuplicate()
    {
        return dupeLocator.existsIn(elementCache().lineageNameElement);
    }

    public boolean isFirstParent()
    {
        return firstParentLoc.existsIn(elementCache().lineageNameElement);
    }

    public boolean isSecondParent()
    {
        return secondParentLoc.existsIn(elementCache().lineageNameElement);
    }

    public void changeSeed(SeedDirection direction)
    {
        WebElement cell = getCell("Change Seed");

        _grid.doAndWaitForUpdate(()->
                elementCache().lineageBtnSeed(getLineageName(), direction).findElement(cell).click());
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

    protected class ElementCache extends GridRow.ElementCache
    {
        public WebElement lineageNameElement = Locator.tagWithClass("div", "lineage-name").findWhenNeeded(this);

        Locator lineageBtnSeed(String lineageName, SeedDirection direction)
        {
            String title = direction.equals(SeedDirection.PARENT) ? "Parent for " + lineageName : "Children for " + lineageName;
            return Locator.tagWithClass("a", "lineage-btn-seed").withAttribute("title", title);
        }
    }

    public enum SeedDirection
    {
        PARENT,
        CHILD
    }
}
