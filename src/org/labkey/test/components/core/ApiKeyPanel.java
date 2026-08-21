/*
 * Copyright (c) 2024-2026 LabKey Corporation
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

    public String generateApiKey(@Nullable String description, @Nullable String restrictionRole)
    {
        ApiKeyDialog apiKeyDialog = clickGenerateApiKey();
        if (description != null)
        {
            apiKeyDialog.setDescription(description);
        }
        if (restrictionRole != null)
        {
            apiKeyDialog.setRestrictionRole(restrictionRole);
        }
        apiKeyDialog.generateApiKey();
        Assert.assertEquals("API Key description", description == null ? "" : description, apiKeyDialog.getDescription());
        String inputFieldValue = apiKeyDialog.getInputFieldValue();
        apiKeyDialog.clickDone();
        return inputFieldValue;
    }

    public String generateApiKey()
    {
        return generateApiKey(null, null);
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
