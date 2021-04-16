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
 * If the current folder has 0 specimen importers (implemented in Professional, and FreezerPro) enabled in it,
 * but the modules are available on the server, the user will be shown call to action to enable the modules.
 * If 0 importer modules are available, the user will be shown an upsell banner.
 *
 * If the user has >1, this page will show a configuration selection (pick which one- QueryBased, FreezerPro)
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
                    elementCache().configureQueryBasedConnectionPane().isPresent() ||
                    elementCache().freezerProBanner().isPresent(),
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
     * this action is only available if there are more than 1 options to pick from- querybased,  freezerpro (or others?)
     * If either Professional(query-based) or Freezerpro are enabled on the folder, this page will not be shown
     * at the end of the "configure specimen import" link on the Manage Study page, instead you will be directed to views
     * from those modules so you can configure their importers accordingly
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

        Optional<WebElement> freezerProBanner()
        {
            return Locator.tagWithText("h3", "FreezerPro Configuration").findOptionalElement(getDriver());
        }
    }
}
