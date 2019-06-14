/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.Locators;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Created by RyanS on 5/18/2017.
 */
public class ViewDatasetDataPage extends LabKeyPage<ViewDatasetDataPage.ElementCache>
{
    public ViewDatasetDataPage(WebDriver driver)
    {
        super(driver);
        waitForElement(DataRegionTable.Locators.dataRegion(dataRegionName));
        _dataRegionTable = new DataRegionTable(dataRegionName, getDriver());
    }
    private static final String dataRegionName = "Dataset";
    protected DataRegionTable _dataRegionTable;

    public DataRegionTable getDataRegion()
    {
        return _dataRegionTable;
    }

    public DatasetInsertPage insertDatasetRow()
    {
        _dataRegionTable.clickInsertNewRow();
        return new DatasetInsertPage(getDriver());
    }

    public ImportDataPage importBulkData()
    {
        _dataRegionTable.clickImportBulkData();
        return new ImportDataPage(getDriver());
    }

    public DatasetPropertiesPage clickManageDataset()
    {
        getDataRegion().clickHeaderButtonAndWait("Manage");
        return new DatasetPropertiesPage(getDriver());
    }

    public List<String> getColumnData(String columnName)
    {
        return _dataRegionTable.getColumnDataAsText(columnName);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
    }
}
