package org.labkey.test.pages.announcements;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * User: tgaluhn
 * Date: 4/30/2018
 */
public class AdminPage extends LabKeyPage<AdminPage.ElementCache>
{
    public AdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static AdminPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static AdminPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "customize")); //, Collections.singletonMap("returnUrl", encodeText(driver.getCurrentContainerPath()) + "/project-begin.view")));
        return new AdminPage(driver.getDriver());
    }

    public LabKeyPage save()
    {
        clickAndWait(elementCache().saveButton);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new LabKeyPage(getDriver());
    }

    public AdminPage setModeratorReviewAll()
    {
        elementCache().moderatorReviewAll.click();
        return new AdminPage(getDriver());
    }

    public AdminPage setModeratorReviewInitial()
    {
        elementCache().moderatorReviewInitial.click();
        return new AdminPage(getDriver());
    }

    public AdminPage setModeratorReviewNone()
    {
        elementCache().moderatorReviewNone.click();
        return new AdminPage(getDriver());
    }

    protected AdminPage.ElementCache newElementCache()
    {
        return new AdminPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private Locator.XPathLocator moderatorReview = Locator.radioButtonByName("moderatorReview");
        protected WebElement moderatorReviewAll = moderatorReview.withAttribute("value", "All").findWhenNeeded(this);
        protected WebElement moderatorReviewInitial = moderatorReview.withAttribute("value", "InitialPost").findWhenNeeded(this);
        protected WebElement moderatorReviewNone = moderatorReview.withAttribute("value", "None").findWhenNeeded(this);

        protected WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }

}
