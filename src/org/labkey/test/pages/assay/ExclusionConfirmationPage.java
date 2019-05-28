/*
 * Copyright (c) 2018 LabKey Corporation
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
package org.labkey.test.pages.assay;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ExclusionConfirmationPage extends LabKeyPage
{
    private DataRegionTable excludedRowsDataGrid = null;

    public ExclusionConfirmationPage(WebDriver driver)
    {
        super(driver);
        this.excludedRowsDataGrid = new DataRegionTable.DataRegionFinder(getDriver()).find();

    }

    public ExclusionConfirmationPage setComment(String comment)
    {
        WebElement input = Locator.css("textarea[name=comment]").findElement(getDriver());
        setFormElement(input, comment);
        return this;
    }

    public DataRegionTable getTrackingDataGrid()
    {
        if (excludedRowsDataGrid == null)
            excludedRowsDataGrid = new DataRegionTable.DataRegionFinder(getDriver()).find();
        return excludedRowsDataGrid;
    }

    public void verifyCountAndSave(int exclusionCount, String comment)
    {
        Assert.assertEquals("Exclusion confirmation data row count is not as expected", exclusionCount, getTrackingDataGrid().getDataRowCount());
        setComment(comment).save();
    }

    public void save()
    {
        clickButton("Confirm");
    }
}
