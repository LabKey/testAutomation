package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LabkeyErrorPage extends LabKeyPage<LabkeyErrorPage.ElementCache>
{
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
        return Locator.tagWithClass("div"," labkey-error-subdetails").index(1).findElement(getDriver()).getText();
    }

    public String getErrorImage()
    {
        return elementCache().errorImage.getAttribute("src");
    }
    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement errorHeading = Locator.tagWithClass("div", "labkey-error-heading").findWhenNeeded(this);
        WebElement errorSubHeading = Locator.tagWithClass("div", "labkey-error-subheading").findWhenNeeded(this);
        WebElement errorImage = Locator.tagWithAttributeContaining("*","alt","LabKey Error").findWhenNeeded(this);
        WebElement backBtn = Locator.button("Back").findWhenNeeded(this);
        WebElement viewDetails = Locator.button("View Details").findWhenNeeded(this);
    }
}
