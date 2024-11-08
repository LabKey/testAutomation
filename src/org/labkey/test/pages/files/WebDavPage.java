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
import org.labkey.test.util.FileBrowserHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WebDavPage extends LabKeyPage<WebDavPage.ElementCache>
{
    private final FileBrowserHelper _fileBrowserHelper;
    public WebDavPage(WebDriver driver)
    {
        super(driver);
        _fileBrowserHelper = new FileBrowserHelper(driver);
    }

    public static WebDavPage beginAt(WebDriverWrapper driver, String path)
    {
        driver.beginAt(WebTestHelper.getBaseURL() + "/_webdav/" + path);
        return new WebDavPage(driver.getDriver());
    }

    public FileBrowserHelper getFileBrowserHelper()
    {
        return _fileBrowserHelper;
    }

    public WebElement getWebDavUrl()
    {
        return elementCache().webDavUrlElement;
    }

    public String getAbsolutePath()
    {
        return elementCache().absolutePathElement.getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        WebElement htmlViewButton = Locator.button("HTML View").findWhenNeeded(this);

        WebElement fbDetailsTable = Locator.tagWithClass("table", "fb-details").findWhenNeeded(this);
        WebElement webDavUrlElement = Locator.tagWithText("th", "WebDav URL:").followingSibling("td").childTag("a")
                .findWhenNeeded(fbDetailsTable);
        WebElement absolutePathElement = Locator.tagWithText("th", "Absolute Path:").followingSibling("td")
            .findWhenNeeded(fbDetailsTable);
    }
}