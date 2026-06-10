/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Page object for the LabKey "Sign In" page. This object assumes the browser is already on the Sign In page; it does
 * not handle navigating to it (e.g. clicking a "Sign In" link).
 */
public class SignInPage extends LabKeyPage<SignInPage.ElementCache>
{
    public SignInPage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected void waitForPage()
    {
        waitFor(() -> getDriver().getTitle().contains("Sign In") && isElementPresent(Locator.tagWithName("form", "login")),
                "Sign In page did not load in time.", WAIT_FOR_PAGE);
    }

    public void signIn(String userName, String password)
    {
        setFormElement(elementCache().emailInput, userName);
        setFormElement(elementCache().passwordInput, password);
        WebElement signInButton = elementCache().signInButton;
        doAndMaybeWaitForPageToLoad(10_000, () -> {
            signInButton.click();
            shortWait().until(ExpectedConditions.invisibilityOfElementLocated(Locator.byClass("signing-in-msg")));
            shortWait().until(ExpectedConditions.or(
                    ExpectedConditions.stalenessOf(signInButton), // Successful login
                    ExpectedConditions.presenceOfElementLocated(Locators.labkeyError.withText()))); // Error during sign-in
            return ExpectedConditions.stalenessOf(signInButton).apply(null);
        });
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement emailInput = Locator.id("email").findWhenNeeded(getDriver());
        WebElement passwordInput = Locator.id("password").findWhenNeeded(getDriver());
        WebElement signInButton = Locator.byClass("signin-btn").findWhenNeeded(getDriver());
    }
}
