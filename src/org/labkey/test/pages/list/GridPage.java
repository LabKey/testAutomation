/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages.list;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class GridPage extends LabKeyPage<GridPage.ElementCache>
{
    public GridPage(WebDriver driver)
    {
        super(driver);
    }

    public static GridPage beginAt(WebDriverWrapper driver, int listId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), listId);
    }

    public static GridPage beginAt(WebDriverWrapper driver, String containerPath, int listId)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "grid",  Maps.of("listId", String.valueOf(listId))));
        return new GridPage(driver.getDriver());
    }

    public static GridPage beginAt(WebDriverWrapper driver, String name)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), name);
    }

    public static GridPage beginAt(WebDriverWrapper driver, String containerPath, String name)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "grid",  Maps.of("name", name)));
        return new GridPage(driver.getDriver());
    }

    public DataRegionTable getGrid()
    {
        return elementCache().table;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final DataRegionTable table = new DataRegionTable("query", getDriver());
    }
}