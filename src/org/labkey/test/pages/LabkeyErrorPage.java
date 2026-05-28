/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.pages;

import org.hamcrest.CoreMatchers;
import org.labkey.test.Locator;
import org.labkey.test.util.DeferredErrorCollector;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LabkeyErrorPage extends LabKeyPage<LabkeyErrorPage.ElementCache>
{
    public static final String UNAUTHORIZED_FULL_PAGE_MESSAGE = "User does not have permission to perform this operation.";
    public static final String IMAGE_TITLE = "permission_error.svg";


    public LabkeyErrorPage(WebDriver driver)
    {
        super(driver);
    }

    public String getErrorHeading()
    {
        return elementCache().errorHeading.getText();
    }

    public String getSubErrorHeading()
    {
        return elementCache().errorSubHeading.getText();
    }

    public String getErrorInstruction()
    {
        return elementCache().errorInstruction.getText();
    }

    public void clickBack()
    {
        elementCache().backBtn.click();
    }

    public void clickViewDetails()
    {
        elementCache().viewDetails.click();
    }

    public String getViewDetailsSubDetails()
    {
        return Locator.tagWithClass("div"," labkey-error-subdetails").findElement(getDriver()).getText();
    }

    public String getErrorImage()
    {
        return elementCache().errorImage.getAttribute("src");
    }

    public boolean isShowDetailsPresent()
    {
        return !Locator.button("View Details").findElements(getDriver()).isEmpty();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public void assertUnauthorized(DeferredErrorCollector checker)
    {
        checker.verifyEquals("Incorrect error heading message", "Oops! An error has occurred.",
                getErrorHeading());
        checker.verifyEquals("Incorrect error sub-heading message", UNAUTHORIZED_FULL_PAGE_MESSAGE,
                getSubErrorHeading());
        checker.verifyThat("Incorrect error image", getErrorImage(), CoreMatchers.containsString(IMAGE_TITLE));
        checker.verifyEquals("Incorrect response code", 403, getResponseCode());
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement errorHeading = Locator.tagWithClass("div", "labkey-error-heading")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement errorSubHeading = Locator.tagWithClass("div", "labkey-error-subheading").findWhenNeeded(this);
        WebElement errorInstruction = Locator.tagWithClass("div", " labkey-error-instruction").index(1).findWhenNeeded(this);
        WebElement errorImage = Locator.tagWithAttributeContaining("*","alt","LabKey Error").findWhenNeeded(this);
        WebElement backBtn = Locator.button("Back").findWhenNeeded(this);
        WebElement viewDetails = Locator.button("View Details").findWhenNeeded(this);
    }
}
