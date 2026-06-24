/*
 * Copyright (c) 2018-2026 LabKey Corporation
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
package org.labkey.test.pages.test;

import org.apache.hc.core5.http.HttpStatus;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestReauthPage extends LabKeyPage<TestReauthPage.ElementCache>
{
    public TestReauthPage(WebDriver driver)
    {
        super(driver);
    }

    public static TestReauthPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("test", "home", "testReauth"));
        return new TestReauthPage(webDriverWrapper.getDriver());
    }

    public static TestReauthPage beginAt(WebDriverWrapper webDriverWrapper, String reauthToken)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("test", "home", "testReauth", Map.of("reauthToken", reauthToken)));
        return new TestReauthPage(webDriverWrapper.getDriver());
    }

    public String getDescription()
    {
        return elementCache().description.getText();
    }

    public void clickReauth()
    {
        clickAndWait(elementCache().reauthLink);
        clearCache();
    }

    public String getReauthToken()
    {
        return elementCache().reauthTokenInput()
                .map(el -> el.getDomProperty("value")).orElse(null);
    }

    public void validateToken()
    {
        clickAndWait(elementCache().validateButton);
        clearCache();
        assertNoLabKeyErrors();
        assertEquals("Response code", HttpStatus.SC_OK, getResponseCode());
    }

    public void validateTokenExpectingError()
    {
        clickAndWait(elementCache().validateButton);
        clearCache();
        assertNotEquals("Response code", HttpStatus.SC_OK, getResponseCode());
    }

    public String getReauthError()
    {
        if (elementCache().reauthError.isDisplayed())
        {
            return elementCache().reauthError.getText();
        }
        else
        {
            return "";
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        public ElementCache()
        {
            waitFor(description::isDisplayed, "Page failed to load", 5_000);
        }

        final WebElement description = Locator.id("description").findWhenNeeded(this);
        final WebElement reauthLink = Locator.id("link").findWhenNeeded(this);
        final Optional<WebElement> reauthTokenInput()
        {
            return Locator.name("reauthToken").findOptionalElement(this);
        }
        final WebElement reauthError = Locators.labkeyError.findWhenNeeded(this);
        final WebElement validateButton = Locator.tagWithAttribute("input", "value", "Sign!").findWhenNeeded(this);
    }
}
