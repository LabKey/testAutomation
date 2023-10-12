/*
 * Copyright (c) 2016-2018 LabKey Corporation
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
package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ColumnChartRegion extends WebDriverComponent<Component<?>.ElementCache>
{
    private final DataRegionTable _dataRegionTable;
    private final WebElement _el;

    public ColumnChartRegion(DataRegionTable dataRegionTable)
    {
        _dataRegionTable = dataRegionTable;
        _el = Locator.css("div.lk-region-section.north").refindWhenNeeded(_dataRegionTable);
    }

    @Override
    protected WebDriver getDriver()
    {
        return _dataRegionTable.getDriver();
    }

    @Override
    public WebDriverWrapper getWrapper()
    {
        return _dataRegionTable.getWrapper();
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public List<WebElement> getPlots(int expectedPlotCount)
    {
        return getWrapper().shortWait().withMessage("Didn't find %s plots".formatted(expectedPlotCount)).until(wd -> {
            List<WebElement> plots = getPlots();
            if (plots.size() == expectedPlotCount)
                return plots;
            return null;
        });
    }

    public List<WebElement> getPlots()
    {
        return Locator.tagWithClass("div", "labkey-dataregion-msg-plot-analytic").waitForElements(this, 10_000);
    }

    // Use getPlots to get the list of plot element, then pass one of those to this function.
    public ColumnChartComponent getColumnPlotWrapper(WebElement plotElement)
    {
        return new ColumnChartComponent(getWrapper(), plotElement);
    }

    public boolean isRegionVisible()
    {
        return getComponentElement().isDisplayed();
    }

    public void revertView()
    {
        _dataRegionTable.openCustomizeGrid().revertUnsavedView();
    }
}
