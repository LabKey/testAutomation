/*
 * Copyright (c) 2021-2026 LabKey Corporation
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

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class ManageStudyNotificationPage extends LabKeyPage<ManageStudyNotificationPage.ElementCache>
{
    public ManageStudyNotificationPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageStudyNotificationPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageStudyNotificationPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("reports", containerPath, "manageNotifications"));
        return new ManageStudyNotificationPage(driver.getDriver());
    }

    public ManageStudyNotificationPage selectNone()
    {
        elementCache().none.check();
        return this;
    }

    public ManageStudyNotificationPage selectAll()
    {
        elementCache().all.check();
        return this;
    }

    public ManageStudyNotificationPage selectByCategory(String name)
    {
        elementCache().byCategory.check();
        _ext4Helper.checkGridCellCheckbox(name,0);
        return this;
    }

    public ManageStudyNotificationPage selectByDataset(String name)
    {
        elementCache().byDataset.check();
        _ext4Helper.checkGridCellCheckbox(name,0);
        return this;
    }

    public ManageStudyNotificationPage save()
    {
        clickButton("Save");
        return this;
    }

    public ManageStudyNotificationPage cancel()
    {
        clickButton("Cancel");
        return this;
    }

    @Override
    protected void waitForPage()
    {
        waitForText("Manage Study Notifications");
    }

    @Override
    protected ManageStudyNotificationPage.ElementCache newElementCache()
    {
        return new ManageStudyNotificationPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        RadioButton none = new RadioButton.RadioButtonFinder().withLabelContaining("None.").findWhenNeeded(this);
        RadioButton all = new RadioButton.RadioButtonFinder().withLabel("All. Your daily digest will list changes and additions to all reports and datasets.")
                .findWhenNeeded(this);
        RadioButton byCategory = new RadioButton.RadioButtonFinder().withLabel("By category. Your daily digest will list changes and additions to reports and datasets in the subscribed categories.")
                .findWhenNeeded(this);
        RadioButton byDataset = new RadioButton.RadioButtonFinder().withLabel("By dataset. Your daily digest will list changes and additions to subscribed datasets.")
                .findWhenNeeded(this);
    }
}
