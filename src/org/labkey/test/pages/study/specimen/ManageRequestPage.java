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
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study-samples", containerPath, "manageRequest", Maps.of("id", String.valueOf(id))));
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
