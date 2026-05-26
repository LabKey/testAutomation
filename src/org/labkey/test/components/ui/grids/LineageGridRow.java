/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        return elementCache().lineageNameElement.getDomAttribute("title");
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
