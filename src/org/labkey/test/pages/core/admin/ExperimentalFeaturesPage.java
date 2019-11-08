package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ExperimentalFeaturesPage extends LabKeyPage<ExperimentalFeaturesPage.ElementCache>
{
    public ExperimentalFeaturesPage(WebDriver driver)
    {
        super(driver);
    }

    public static ExperimentalFeaturesPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", "experimentalFeatures"));
        return new ExperimentalFeaturesPage(driver.getDriver());
    }

    public ExperimentalFeaturesPage enableResolveLookupsByValue()
    {
        log("Enabling the experimental feature");
        if (elementCache().resolveLookupsByValueLink.getText().equalsIgnoreCase("Enable"))
        {
            elementCache().resolveLookupsByValueLink.click();
        }
        return this;
    }

    public ExperimentalFeaturesPage disableResolveLookupsByValue()
    {
        log("Disabling the experimental feature");
        if (elementCache().resolveLookupsByValueLink.getText().equalsIgnoreCase("Disable"))
        {
            elementCache().resolveLookupsByValueLink.click();
        }
        return this;
    }

    public ExperimentalFeaturesPage enableExperimentalNotificationMenu()
    {
        log("Enabling the experimental feature");
        if (elementCache().experimentalNotificationMenuLink.getText().equalsIgnoreCase("Enable"))
        {
            elementCache().experimentalNotificationMenuLink.click();
        }
        return this;
    }

    public ExperimentalFeaturesPage disableExperimentalNotificationMenu()
    {
        log("Enabling the experimental feature");
        if (elementCache().experimentalNotificationMenuLink.getText().equalsIgnoreCase("Disable"))
        {
            elementCache().experimentalNotificationMenuLink.click();
        }
        return this;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {

        protected WebElement experimentalNotificationMenuLink = Locator.tagWithAttribute("a", "data-exp-flag", "experimental-notificationmenu").findWhenNeeded(this);
        protected WebElement resolveLookupsByValueLink = Locator.tagWithAttribute("a", "data-exp-flag", "resolve-lookups-by-value").findWhenNeeded(this);
        protected WebElement genericDetailsUrlLink = Locator.tagWithAttribute("a", "data-exp-flag", "generic-details-url").findWhenNeeded(this);
        protected WebElement legacyLineageLink = Locator.tagWithAttribute("a", "data-exp-flag", "legacy-lineage").findWhenNeeded(this);
        protected WebElement resolvePropertyUriColumnsLink = Locator.tagWithAttribute("a", "data-exp-flag", "resolve-property-uri-columns").findWhenNeeded(this);
        protected WebElement clientSideExceptionLoogingtoMothershipLink = Locator.tagWithAttribute("a", "data-exp-flag", "javascriptMothership").findWhenNeeded(this);
        protected WebElement clientSideExceptionLoogingtoServerLink = Locator.tagWithAttribute("a", "data-exp-flag", "javascriptErrorServerLogging").findWhenNeeded(this);
        protected WebElement userFoldersLink = Locator.tagWithAttribute("a", "data-exp-flag", "userFolders").findWhenNeeded(this);
        protected WebElement disableGuestAccountLink = Locator.tagWithAttribute("a", "data-exp-flag", "disableGuestAccount").findWhenNeeded(this);
        protected WebElement blockMaliciousClientsLink = Locator.tagWithAttribute("a", "data-exp-flag", "blockMaliciousClients").findWhenNeeded(this);
        protected WebElement strictReturnUrlLink = Locator.tagWithAttribute("a", "data-exp-flag", "strictReturnUrl").findWhenNeeded(this);
        protected WebElement experimentalUxassaydataimportLink = Locator.tagWithAttribute("a", "data-exp-flag", "experimental-uxassaydataimport").findWhenNeeded(this);
        protected WebElement CreateSpecimenStudyLink = Locator.tagWithAttribute("a", "data-exp-flag", "CreateSpecimenStudy").findWhenNeeded(this);
        protected WebElement StudySubSchemasLink = Locator.tagWithAttribute("a", "data-exp-flag", "StudySubSchemas").findWhenNeeded(this);

    }
}
