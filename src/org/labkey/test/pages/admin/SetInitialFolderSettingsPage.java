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
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SetInitialFolderSettingsPage extends LabKeyPage<SetInitialFolderSettingsPage.ElementCache>
{
    public SetInitialFolderSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static SetInitialFolderSettingsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static SetInitialFolderSettingsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("controller", containerPath, "action"));
        return new SetInitialFolderSettingsPage(driver.getDriver());
    }

    public SetInitialFolderSettingsPage setCustomFileRoot(String fileRoot)
    {
        elementCache().customLocRadioButton.click();
        setFormElement(elementCache().folderRootPathInput, fileRoot);
        return this;
    }

    public SetInitialFolderSettingsPage useDefaultLocation()
    {
        elementCache().useDefaultRadioButton.click();
        return this;
    }



    public LabKeyPage clickFinish()
    {
        clickAndWait(elementCache().finishButton);

        return new LabKeyPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        final WebElement finishButton = Locator.lkButton("Finish").findWhenNeeded(this).withTimeout(4000);
        public WebElement useDefaultRadioButton =  Locator.xpath("//td[./label[text()='Use Default']]/input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        final WebElement customLocRadioButton =  Locator.xpath("//td[./label[text()='Custom Location']]/input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        final WebElement folderRootPathInput = Locator.input("folderRootPath")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}