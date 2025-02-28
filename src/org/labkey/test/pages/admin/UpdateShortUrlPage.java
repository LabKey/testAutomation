package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class UpdateShortUrlPage extends LabKeyPage<UpdateShortUrlPage.ElementCache>
{
    public UpdateShortUrlPage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdateShortUrlPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "updateShortURL"));
        return new UpdateShortUrlPage(webDriverWrapper.getDriver());
    }

    public String getShortUrl()
    {
        return elementCache().shortUrlDisplay.getText().trim();
    }

    public String setTargetUrl(String targetUrl)
    {
        return elementCache().targetUrlInput.get();
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
        elementCache().deleteButton.click();
        doAndWaitForPageToLoad(this::acceptAlert);

        return new ShortUrlAdminPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        WebElement shortUrlDisplay = Locator.name("shortURL").parent().findWhenNeeded(this);
        Input targetUrlInput = Input.Input(Locator.name("fullURL"), getDriver()).findWhenNeeded(this);

        WebElement updateButton = Locator.lkButton("Update").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
        WebElement deleteButton = Locator.lkButton("Delete").findWhenNeeded(this);
    }
}
