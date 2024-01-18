package org.labkey.test.components.ui.grids;


import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;




public class LineageGridRow extends GridRow
{
    private static final Locator seedLocator = Locator.tagWithClass("span", "show-on-hover").withText("Seed");
    private static final Locator dupeLocator = Locator.tagWithClass("span", "label-warning").withText("Duplicate");
    private static final Locator firstParentLoc = Locator.tagWithClass("span", "label-info").withText("1st parent");
    private static final Locator secondParentLoc = Locator.tagWithClass("span", "label-primary").withText("2nd parent");
    private String _lineageName;

    protected LineageGridRow(ResponsiveGrid<?> grid, WebElement el)
    {
        super(grid, el);
    }

    public String getLineageName()
    {
        if (null == _lineageName)
            _lineageName = Locator.tag("a").findElement(elementCache().lineageNameElement).getText();
        return _lineageName;
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
