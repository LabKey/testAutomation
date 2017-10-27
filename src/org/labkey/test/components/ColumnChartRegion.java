/*
 * Copyright (c) 2016 LabKey Corporation
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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT;

public class ColumnChartRegion
{
    WebDriverWrapper _driver;
    DataRegionTable _dataRegionTable;

    public ColumnChartRegion(WebDriverWrapper driver, DataRegionTable dataRegionTable)
    {
        _driver = driver;
        _dataRegionTable = dataRegionTable;
    }

    public WebElement getComponentElement()
    {
        WebElement webElement;
        try
        {
            webElement = _dataRegionTable.findElement(By.cssSelector(" div.lk-region-bar"));
        }
        catch(org.openqa.selenium.NoSuchElementException nse)
        {
            _driver.log("*** Couldn't find the column plot region. ***");
            webElement = null;
        }
        return webElement;
    }

    public List<WebElement> getPlots()
    {
        return _dataRegionTable.findElements(By.cssSelector(" div.labkey-dataregion-msg-plot-analytic"));
    }

    // Use getPlots to get the list of plot element, then pass one of those to this function.
    public ColumnChartComponent getColumnPlotWrapper(WebElement plotElement)
    {
        return new ColumnChartComponent(_driver, plotElement);
    }

    public boolean isRegionVisible()
    {
        return null != getComponentElement() && getComponentElement().isDisplayed();
    }

    public boolean isViewModified()
    {
        return _dataRegionTable.findElements(Locator.tagWithText("span", "This grid view has been modified.")).size() > 0;
    }

    public void revertView()
    {
        _dataRegionTable.openCustomizeGrid().revertUnsavedView();
    }
}
