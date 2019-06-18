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
package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ManageStudyPage extends LabKeyPage<ManageStudyPage.ElementCache>
{
    public ManageStudyPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageStudyPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageStudyPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study", containerPath, "manageStudy"));
        return new ManageStudyPage(driver.getDriver());
    }

    public StudySecurityPage manageSecurity()
    {
        clickAndWait(elementCache().manageSecurity);
        return new StudySecurityPage(getDriver());
    }

    public ManageDatasetsPage manageDatasets()
    {
        clickAndWait(elementCache().manageDatasets);
        return new ManageDatasetsPage(getDriver());
    }

    public ManageDatasetQCStatesPage manageDatasetQCStates()
    {
        clickAndWait(elementCache().manageQCStates);
        return new ManageDatasetQCStatesPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement manageDatasets = Locator.linkWithText("Manage Datasets").findWhenNeeded(this);
        WebElement manageSecurity = Locator.linkWithText("Manage Security").findWhenNeeded(this);
        WebElement manageQCStates = Locator.linkWithText("Manage Dataset QC States").findWhenNeeded(this);
    }
}