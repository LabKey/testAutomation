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
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

public class ManageRequestPage extends LabKeyPage<ManageRequestPage.ElementCache>
{
    public ManageRequestPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageRequestPage beginAt(WebDriverWrapper webDriverWrapper, int id)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath(), id);
    }

    public static ManageRequestPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, int id)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("specimen", containerPath, "manageRequest", Maps.of("id", String.valueOf(id))));
        return new ManageRequestPage(webDriverWrapper.getDriver());
    }

    public ManageRequestPage submitRequest()
    {
        WebElement submitButton = elementCache().getSubmitButton()
                .orElseThrow(() -> new IllegalStateException("Submit button not present. Has request already been submitted?"));

        doAndAcceptUnloadAlert(submitButton::click, "Once a request is submitted, its specimen list may no longer be modified.");
        return new ManageRequestPage(getDriver());
    }

    public String getRequestInformation(String label)
    {
        Optional<WebElement> info = Locator.tag("th").withText(label).followingSibling("td").findOptionalElement(elementCache().requestInformationPanel);
        return info.orElseThrow(() -> new IllegalArgumentException("No Request Information with label: \"" + label + "\""))
                .getText();
    }

    public ManageRequestStatusPage clickUpdateRequest()
    {
        clickAndWait(elementCache().updateRequestLink);
        return new ManageRequestStatusPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        // Only on unsubmitted requests
        public Optional<WebElement> getSubmitButton()
        {
            return Locator.lkButton("Submit Request").findOptionalElement(this);
        }

        public Optional<WebElement> getCancelButton()
        {
            return Locator.lkButton("Cancel Request").findOptionalElement(this);
        }

        // Present for all requests
        WebElement requestInformationPanel = Locator.byClass("specimen-request-information").findWhenNeeded(this);
        WebElement updateRequestLink = Locator.linkWithText("Update Request").findWhenNeeded(this);
        WebElement specimenSearchButton = Locator.lkButton("Specimen Search").findWhenNeeded(this);
        WebElement uploadSpecimenIdsButton = Locator.lkButton("Upload Specimen Ids").findWhenNeeded(this);
    }
}
