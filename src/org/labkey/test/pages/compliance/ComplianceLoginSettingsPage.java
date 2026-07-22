/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.pages.compliance;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ComplianceLoginSettingsPage extends BaseComplianceSettingsPage<ComplianceLoginSettingsPage.ElementCache>
{
    public ComplianceLoginSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceLoginSettingsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        BaseComplianceSettingsPage.beginAt(webDriverWrapper, SettingsTab.Login);
        return new ComplianceLoginSettingsPage(webDriverWrapper.getDriver());
    }

    public void enableFicamProviders()
    {
        checkCheckbox(elementCache().acceptOnlyFicamChk);
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().ficamProvidersDiv));
    }

    public boolean isFicamProvidersChecked()
    {
        return elementCache().acceptOnlyFicamChk.isSelected();
    }

    public boolean isFicamProvidersDivDisplayed()
    {
        return elementCache().ficamProvidersDiv.isDisplayed();
    }

    public void disableFicamProviders()
    {
        uncheckCheckbox(elementCache().acceptOnlyFicamChk);
    }

    public List<String> getFicamProviersList()
    {
        return getTexts(Locator.tag("li").findElements(elementCache().ficamProvidersDiv));
    }

    public void clickSaveExpectingAlert(String expectedAlert)
    {
        elementCache().saveButton.click();
        assertAlert(expectedAlert);
    }

    public static void setFicamRestriction(Connection connection, boolean restrict)
    {
        SimplePostCommand command = new SimplePostCommand("compliance", "complianceSettings");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tab", "login");
        if (restrict)
            jsonObject.put("acceptOnlyFICAMProviders", "on");
        command.setJsonObject(jsonObject);

        try
        {
            command.execute(connection, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseComplianceSettingsPage<ElementCache>.ElementCache
    {
        final WebElement acceptOnlyFicamChk = Locator.checkboxById("acceptOnlyFICAMProviders").findWhenNeeded(this);
        final WebElement ficamProvidersDiv = Locator.tagWithId("div", "FICAMProviders").findWhenNeeded(this);
    }
}
