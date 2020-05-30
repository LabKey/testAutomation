/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.pages.query;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PageFactory;
import org.labkey.test.util.RelativeUrl;
import org.openqa.selenium.WebDriver;

public class ExecuteQueryPage extends LabKeyPage<ExecuteQueryPage.ElementCache>
{
    public ExecuteQueryPage(WebDriver driver)
    {
        super(driver);
    }

    public static ExecuteQueryPage beginAt(WebDriverWrapper driver, String schemaName, String queryName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), schemaName, queryName);
    }

    public static ExecuteQueryPage beginAt(WebDriverWrapper driver, String containerPath, String schemaName, String queryName)
    {
        return getPageFactory(schemaName, queryName).setContainerPath(containerPath).navigate(driver);
    }

    public static PageFactory<ExecuteQueryPage> getPageFactory(String schemaName, String queryName)
    {
        return new RelativeUrl("query", "executeQuery")
                .addParameters(Maps.of("schemaName", schemaName, "query.queryName", queryName))
                .getPageFactory(wd -> new ExecuteQueryPage(wd));
    }

    public DataRegionTable getDataRegion()
    {
        return elementCache()._dataRegionTable;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DataRegionTable _dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").findWhenNeeded(this);
    }
}