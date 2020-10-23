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
 * the user will be shown an upsell banner
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
        waitFor(()-> elementCache().upsellBanner().isPresent() ||
                    elementCache().importOptionPickerBanner().isPresent() ||
                    elementCache().configureQueryBasedConnectionPane().isPresent() ||
                    elementCache().freezerProBanner().isPresent(),
                "the page did not initialize in time", WAIT_FOR_JAVASCRIPT);
    }

    /**
     * If no importer is enabled in the current folder, an upsell banner should appear.
     * @return
     */
    public boolean isUpsellBannerShown()
    {
        return elementCache().upsellBanner().isPresent();
    }

    public boolean isImportOptionPickerShown()
    {
        return elementCache().importOptionPickerBanner().isPresent();
    }

    /**
     * this action is only available if there are more than 1 options to pick from- querybased,  freezerpro (or others?)
     * If either Professional(query-based) or Freezerpro are enabled on the folder, this page will not be shown
     * at the end of the "configure specimen import" link on the Manage Study page, instead you will be directed to views
     * from those modules so you can configure their importers accordingly
     *
     * @param option
     * @return
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
        // the upsell banner will be present if the current folder does not have Professional enabled in it
        Optional<WebElement> upsellBanner()
        {
            return Locator.tagWithClass("div", "alert-info")
                    .withDescendant(Locator.tag("h3").withText("Specimen Import is not available with your current edition of LabKey Server."))
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
