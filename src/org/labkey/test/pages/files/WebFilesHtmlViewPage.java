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
package org.labkey.test.pages.files;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WebFilesHtmlViewPage extends LabKeyPage<WebFilesHtmlViewPage.ElementCache>
{
    public WebFilesHtmlViewPage(WebDriver driver)
    {
        super(driver);
    }

    public static WebFilesHtmlViewPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static WebFilesHtmlViewPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.getBaseURL() + "/_webfiles/" + containerPath + "/?listing=html");
        return new WebFilesHtmlViewPage(driver.getDriver());
    }

    public LabKeyPage goToStandardView()
    {
        clickAndWait(elementCache().standardViewButton);

        return new LabKeyPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement standardViewButton = Locator.button("Standard View").findWhenNeeded(this);
    }
}