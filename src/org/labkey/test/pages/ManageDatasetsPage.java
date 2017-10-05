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
package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.pages.study.CreateDatasetPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ManageDatasetsPage extends LabKeyPage<ManageDatasetsPage.ElementCache>
{
    public ManageDatasetsPage(WebDriver driver)
    {
        super(driver);
        elementCache();
    }

    public void clickStudySchedule()
    {
        clickAndWait(elementCache().studyScheduleLink);
    }

    public void clickChangeProperties()
    {
        clickAndWait(elementCache().changePropertiesLink);
    }

    public void clickDeleteMultipleDatasets()
    {
        clickAndWait(elementCache().deleteMultipleDatasetsLink);
    }

    public void clickManageDatasetSecurity()
    {
        clickAndWait(elementCache().manageDatasetSecurityLink);
    }

    public CreateDatasetPage clickCreateNewDataset()
    {
        clickAndWait(elementCache().createNewDatasetLink);
        return new CreateDatasetPage(getDriver());
    }

    public void clickProjectSettings()
    {
        clickAndWait(elementCache().projectSettingsLink);
    }

    public void clickFolderSettings()
    {
        clickAndWait(elementCache().folderSettingsLink);
    }

    public void clickChangeDisplayOrder()
    {
        clickAndWait(elementCache().changeDisplayOrderLink);
    }

    public String getDefaultDateTimeFormat()
    {
        return elementCache().dateTimeFormat.getText();
    }

    public String getDefaultNumberFormat()
    {
        return elementCache().numberFormat.getText();
    }

    public DatasetPropertiesPage selectDatasetById(String id)
    {
        clickAndWait(Locator.tag("td").position(1).append(Locator.linkWithText(id)).findElement(elementCache().datasetGrid));
        return new DatasetPropertiesPage(getDriver());
    }

    public DatasetPropertiesPage selectDatasetByName(String name)
    {
        clickAndWait(Locator.tag("td").position(2).append(Locator.linkWithText(name)).findElement(elementCache().datasetGrid));
        return new DatasetPropertiesPage(getDriver());
    }

    public DatasetPropertiesPage selectDatasetByLabel(String label)
    {
        clickAndWait(Locator.tag("td").position(3).append(Locator.linkWithText(label)).findElement(elementCache().datasetGrid));
        return new DatasetPropertiesPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement studyScheduleLink = Locator.linkWithText("Study Schedule").findWhenNeeded(this);
        WebElement changeDisplayOrderLink = Locator.linkWithText("Change Display Order").findWhenNeeded(this);
        WebElement changePropertiesLink = Locator.linkWithText("Change Properties").findWhenNeeded(this);
        WebElement deleteMultipleDatasetsLink = Locator.linkWithText("Delete Multiple Datasets").findWhenNeeded(this);
        WebElement manageDatasetSecurityLink = Locator.linkWithText("Manage Dataset Security").findWhenNeeded(this);
        WebElement createNewDatasetLink = Locator.linkWithText("Create New Dataset").findWhenNeeded(this);
        WebElement folderSettingsLink = Locator.linkWithText("folder settings page").findWhenNeeded(this);
        WebElement projectSettingsLink = Locator.linkWithText("project settings page").findWhenNeeded(this);
        WebElement dateTimeFormat = Locator.xpath("//td[text()='Default date-time format:']/following-sibling::td").findWhenNeeded(this);
        WebElement numberFormat = Locator.xpath("//td[text()='Default number format:']/following-sibling::td").findWhenNeeded(this);
        WebElement datasetGrid = Locator.id("dataregion_datasets").waitForElement(this, 10000);
    }
}
