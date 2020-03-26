package org.labkey.test.components.labkey.ui.grid;

import org.labkey.test.components.glassLibrary.grids.ResponsiveGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/base/Grid.tsx
 */
public class Grid extends ResponsiveGrid
{
    protected Grid(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public static class GridFinder extends ResponsiveGridFinder
    {
        public GridFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected GridFinder getThis()
        {
            return this;
        }

        @Override
        protected Grid construct(WebElement el, WebDriver driver)
        {
            return new Grid(el, driver);
        }
    }

    protected class ElementCache extends ResponsiveGrid.ElementCache
    {
    }
}
