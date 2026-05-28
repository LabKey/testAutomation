/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

/**
 * If the current folder has 0 specimen importers (implemented in Professional) enabled in it,
 * but the modules are available on the server, the user will be shown call to action to enable the modules.
 * If 0 importer modules are available, the user will be shown an upsell banner.
 *
 * If the user has >1, this page will show a configuration selection
 *
 * If the user has only 1, this page will show options for configuring it.
 *
 */
public class ConfigureImporterPage extends LabKeyPage<ConfigureImporterPage.ElementCache>
{
    public ConfigureImporterPage(WebDriver driver)
    {
        super(driver);
    }

    public static ConfigureImporterPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study", containerPath, "chooseImporter"));
        return new ConfigureImporterPage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitFor(()-> elementCache().EnableModuleBanner().isPresent() ||
                    elementCache().importOptionPickerBanner().isPresent() ||
                    elementCache().configureQueryBasedConnectionPane().isPresent(),
                "the page did not initialize in time", WAIT_FOR_JAVASCRIPT);
    }

    /**
     * If no importer is enabled in the current folder, a call-to-action enable-module banner should appear.
     * @return true if banner is present
     */
    public boolean isEnableModuleBannerShown()
    {
        return elementCache().EnableModuleBanner().isPresent();
    }

    public boolean isImportOptionPickerShown()
    {
        return elementCache().importOptionPickerBanner().isPresent();
    }

    public boolean isQueryConfigurationShown()
    {
        return elementCache().configureQueryBasedConnectionPane().isPresent();
    }

    /**
     * This action is only available if there are more than 1 options to pick from
     *
     * @param option Use the text label next to the radio button
     * @return The current page
     */
    public ConfigureImporterPage selectSpecimenImportType(String option)
    {
        Locator radioButtonLoc = Locator.tagWithAttribute("input", "value", option);
        RadioButton radioButton = new RadioButton(radioButtonLoc.findElement(getDriver()));
        radioButton.check();

        clickButton("Save");
        return new ConfigureImporterPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        // the enable module banner will be present if the current folder does not have Professional enabled in it,
        // but the Professional module is available within the folder
        Optional<WebElement> EnableModuleBanner()
        {
            return Locator.tagWithClass("div", "alert-warning")
                    .withText("External Specimen Import is not currently available for this folder. To use External import, enable the Professional Module for this folder.")
                    .findOptionalElement(getDriver());
        }

        // the configure import selection form will be present if multiple importers are enabled on the page
        Optional<WebElement> importOptionPickerBanner()
        {
            return Locator.tagWithClass("h4", "labkey-page-section-header")
                    .withText("Configure Specimen Import").findOptionalElement(getDriver());
        }

        Optional<WebElement> configureQueryBasedConnectionPane()
        {
            return Locator.tagWithClass("div", "QBSpecimenImportFormFields")
                    .findOptionalElement(getDriver());
        }

    }
}
