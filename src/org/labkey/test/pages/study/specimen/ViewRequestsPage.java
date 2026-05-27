/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.pages.study.specimen;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ViewRequestsPage extends LabKeyPage<ViewRequestsPage.ElementCache>
{
    public ViewRequestsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ViewRequestsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ViewRequestsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("specimen", containerPath, "viewRequests"));
        return new ViewRequestsPage(webDriverWrapper.getDriver());
    }

    // TODO: Add methods for other actions on this page
    public LabKeyPage clickButton()
    {
        clickAndWait(elementCache().example);

        // TODO: Methods that navigate should return an appropriate page object
        return new LabKeyPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        // TODO: Add other elements that are on the page
        WebElement example = Locator.css("button").findWhenNeeded(this);
    }
}
