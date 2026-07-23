/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.pages.experiment;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class ShowDataClassPage extends LabKeyPage<ShowDataClassPage.ElementCache>
{
    public ShowDataClassPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowDataClassPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, int rowId)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("experiment", containerPath, "showDataClass", Map.of("rowId", rowId)));
        return new ShowDataClassPage(webDriverWrapper.getDriver());
    }

    public CreateDataClassPage clickEditDataClass()
    {
        clickAndWait(elementCache().editDataClassButton);
        return new CreateDataClassPage(getDriver());
    }

    public DeleteDataClassPage clickDeleteDataClass()
    {
        clickAndWait(elementCache().deleteDataClassButton);
        return new DeleteDataClassPage(getDriver());
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

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        private final WebElement editDataClassButton = Locator.lkButton("Edit Data Class").findWhenNeeded(this);
        private final WebElement deleteDataClassButton = Locator.lkButton("Delete Data Class").findWhenNeeded(this);
        private final DataRegionTable _dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").findWhenNeeded(this);
    }
}
