package org.labkey.test.components.core;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.Panel;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ApiKeyPanel extends Panel<ApiKeyPanel.ElementCache>
{
    protected ApiKeyPanel(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public static SimpleWebDriverComponentFinder<ApiKeyPanel> panelFinder(WebDriver driver)
    {
        return new Panel.PanelFinder(driver).withTitle("API Keys").wrap(ApiKeyPanel::new);
    }

    public String generateApiKey(@Nullable String description)
    {
        ApiKeyDialog apiKeyDialog = clickGenerateApiKey();
        if (description != null)
        {
            apiKeyDialog.setDescription(description);
        }
        apiKeyDialog.generateApiKey();
        Assert.assertEquals("API Key discription", description == null ? "" : description, apiKeyDialog.getDescription());
        String inputFieldValue = apiKeyDialog.getInputFieldValue();
        apiKeyDialog.clickDone();
        return inputFieldValue;
    }

    public String generateApiKey()
    {
        return generateApiKey(null);
    }
    public ApiKeyDialog clickGenerateApiKey()
    {
        elementCache().generateApiKeyButton.click();
        return new ApiKeyDialog(getDriver(), ApiKeyDialog.API_KEY_TITLE);
    }

    public String generateSessionKey()
    {
        ApiKeyDialog apiKeyDialog = clickGenerateSessionKey();
        String inputFieldValue = apiKeyDialog.getInputFieldValue();
        apiKeyDialog.clickDone();
        return inputFieldValue;
    }

    public ApiKeyDialog clickGenerateSessionKey()
    {
        elementCache().generateSessionKeyButton.click();
        return new ApiKeyDialog(getDriver(), ApiKeyDialog.SESSION_KEY_TITLE);
    }

    public QueryGrid getGrid()
    {
        return new QueryGrid.QueryGridFinder(getDriver()).findWhenNeeded();
    }


    public boolean isGenerateApiKeyButtonEnabled()
    {
        return elementCache().generateApiKeyButton.isEnabled();
    }

    public boolean isGenerateApiKeyButtonDisplayed()
    {
        return elementCache().generateApiKeyButton.isDisplayed();
    }

    public boolean hasDisabledMessage()
    {
        return BootstrapLocators.warningBanner.containing("not enabled").existsIn(this);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Panel<ElementCache>.ElementCache
    {
        WebElement generateApiKeyButton = Locator.tagWithText("button", "Generate API Key").findWhenNeeded(this);
        WebElement generateSessionKeyButton = Locator.tagWithText("button", "Generate Session Key").findWhenNeeded(this);
    }
}
