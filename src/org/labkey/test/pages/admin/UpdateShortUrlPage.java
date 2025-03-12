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
