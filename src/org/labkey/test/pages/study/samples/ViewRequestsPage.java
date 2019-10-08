package org.labkey.test.pages.study.samples;

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
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study-samples", containerPath, "viewRequests"));
        return new ViewRequestsPage(webDriverWrapper.getDriver());
    }

    // TODO: Add methods for other actions on this page
    public LabKeyPage clickButton()
    {
        clickAndWait(elementCache().example);

        // TODO: Methods that navigate should return an appropriate page object
        return new LabKeyPage(getDriver());
    }

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
