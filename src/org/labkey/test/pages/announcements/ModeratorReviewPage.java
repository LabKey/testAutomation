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
package org.labkey.test.pages.announcements;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * User: tgaluhn
 * Date: 4/30/2018
 */
public class ModeratorReviewPage extends LabKeyPage<ModeratorReviewPage.ElementCache>
{
    public ModeratorReviewPage(WebDriver driver)
    {
        super(driver);
    }

    public static ModeratorReviewPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ModeratorReviewPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "moderatorReview"));
        return new ModeratorReviewPage(driver.getDriver());
    }

    public ModeratorReviewPage review(String title, boolean approve)
    {
        elementCache()._dataRegionTable.checkCheckbox(elementCache()._dataRegionTable.getRowIndex("Title", title));
        if (approve)
        {
            elementCache().approveButton.click();
            assertAlertContains("approve");
        }
        else
        {
            elementCache().spamButton.click();
            assertAlertContains("spam");
        }

        return new ModeratorReviewPage(getDriver());
    }

    protected ModeratorReviewPage.ElementCache newElementCache()
    {
        return new ModeratorReviewPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DataRegionTable _dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").findWhenNeeded(this);

        protected WebElement spamButton = Locator.lkButton("Mark As Spam").findWhenNeeded(this);
        protected WebElement approveButton = Locator.lkButton("Approve").findWhenNeeded(this);
    }

}
