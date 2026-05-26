/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class UpdateShortUrlPage extends LabKeyPage<UpdateShortUrlPage.ElementCache>
{
    public UpdateShortUrlPage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdateShortUrlPage beginAt(WebDriverWrapper webDriverWrapper, String shortUrl)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "updateShortURL", Map.of("shortURL", shortUrl)));
        return new UpdateShortUrlPage(webDriverWrapper.getDriver());
    }

    public String getShortUrl()
    {
        return elementCache().shortUrlDisplay.getText().trim();
    }

    public UpdateShortUrlPage setTargetUrl(String targetUrl)
    {
        elementCache().targetUrlInput.set(targetUrl);
        return this;
    }

    public String getTargetUrl()
    {
        return elementCache().targetUrlInput.get();
    }

    public ShortUrlAdminPage clickUpdate()
    {
        clickAndWait(elementCache().updateButton);

        return new ShortUrlAdminPage(getDriver());
    }

    public ShortUrlAdminPage clickCancel()
    {
        clickAndWait(elementCache().cancelButton);

        return new ShortUrlAdminPage(getDriver());
    }

    public ShortUrlAdminPage clickDeleteAndConfirm()
    {
        doAndWaitForPageToLoad(() -> {
            elementCache().deleteButton.click();
            acceptAlert();
        });

        return new ShortUrlAdminPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        final WebElement shortUrlDisplay = Locator.name("shortURL").parent().findWhenNeeded(this);
        final Input targetUrlInput = Input.Input(Locator.name("fullURL"), getDriver()).findWhenNeeded(this);

        final WebElement updateButton = Locator.lkButton("Update").findWhenNeeded(this);
        final WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
        final WebElement deleteButton = Locator.lkButton("Delete").findWhenNeeded(this);
    }
}
