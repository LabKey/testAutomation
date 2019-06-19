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
package org.labkey.test.pages.announcements;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class InsertPage extends BaseUpdatePage<InsertPage>
{
    public InsertPage(WebDriver driver)
    {
        super(driver);
    }

    public static InsertPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static InsertPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "insert"));
        return new InsertPage(driver.getDriver());
    }

    public InsertPage selectPreviewTab()
    {
        elementCache().previewTab.click();
        return this;
    }

    public InsertPage selectSourceTab()
    {
        elementCache().sourceTab.click();
        return this;
    }

    @Override
    protected InsertPage getThis()
    {
        return this;
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseUpdatePage<InsertPage>.ElementCache
    {
        WebElement sourceTab = Locator.tagWithClassContaining("li", "nav-item")
                .withChild(Locator.tagWithClass("a", "nav-link").withText("Source"))
                .findWhenNeeded(this);
        WebElement previewTab = Locator.tagWithClassContaining("li", "nav-item")
                .withChild(Locator.tagWithClass("a", "nav-link").withText("Preview"))
                .findWhenNeeded(this);
    }
}