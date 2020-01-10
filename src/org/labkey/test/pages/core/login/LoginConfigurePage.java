package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.glassLibrary.components.MultiMenu;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class LoginConfigurePage extends LabKeyPage<LoginConfigurePage.ElementCache>
{
    public LoginConfigurePage(WebDriver driver)
    {
        super(driver);
    }

    public static LoginConfigurePage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("login", "configure"));
        return new LoginConfigurePage(webDriverWrapper.getDriver());
    }

    public <D extends AuthDialogBase> D addConfiguration(AuthenticationProvider<D> authenticationProvider)
    {
        elementCache().addPrimaryMenu.
                clickSubMenu(false, authenticationProvider.getProviderName() + " : " + authenticationProvider.getProviderDescription());

        return authenticationProvider.getNewDialog(getDriver());
    }

    public LoginConfigurePage togglePrimaryConfiguration()
    {
        if (!elementCache().panelTab1.getAttribute("aria-selected").equals("true"))
            elementCache().panelTab1.click();
        waitFor(()-> elementCache().panelTab1.getAttribute("aria-selected").equals("true"), 1000);
        return this;
    }

    public LoginConfigRow getPrimaryConfigurationRow(String description)
    {
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description).waitFor();
    }

    public List<LoginConfigRow> getConfigurations()
    {
        togglePrimaryConfiguration();
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).findAll();
    }

    public LoginConfigurePage toggleSecondaryConfiguration()
    {
        if (!elementCache().panelTab2.getAttribute("aria-selected").equals("true"))
            elementCache().panelTab2.click();
        waitFor(()-> elementCache().panelTab2.getAttribute("aria-selected").equals("true"), 1000);
        return this;
    }

    public LoginConfigurePage removeConfiguration(String description)      // assumes for now we're doing primary only
    {
        new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description).waitFor()
                .clickDelete();

        // pause, because that works?
        sleep(1000);
        return this;
    }

    public <D extends AuthDialogBase> D clickEditConfiguration(String description, AuthenticationProvider<D> authenticationProvider)
    {
        LoginConfigRow row = new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description).waitFor(getDriver());
        return row.clickEdit(authenticationProvider);
    }

    public ShowAdminPage clickSaveAndFinish()
    {
        clickAndWait(elementCache().saveAndFinishBtn());
        return new ShowAdminPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement globalSettingsPanel()
        {
            return Locator.tagWithClass("div", "panel-default")
                    .withDescendant(Locator.tagWithClass("div", "panel-heading").withChild(Locator.tag("span").withText("Global Settings")))
                    .waitForElement(this, WAIT_FOR_JAVASCRIPT);
        }

        Locator checkBoxLoc(String label)
        {
            return Locator.tagWithClass("div", "bottom-margin").withChild(Locator.tagWithText("span", label))
                    .child(Locator.tagWithClassContaining("svg", "svg-inline--fa"));
        }

        WebElement configurationsPanel()
        {
            return Locator.tagWithClass("div", "panel-default")
                    .withDescendant(Locator.tagWithClass("div", "panel-heading").withChild(Locator.tag("span").withText("Configurations")))
                    .waitForElement(this, WAIT_FOR_JAVASCRIPT);
        }

        WebElement tabPanel = Locator.id("tab-panel").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement panelTab1 = Locator.id("tab-panel-tab-1").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement panelTab2 = Locator.id("tab-panel-tab-2").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);

        BootstrapMenu addPrimaryMenu = new MultiMenu.MultiMenuFinder(getDriver()).withText("Add New Primary Configuration").timeout(WAIT_FOR_JAVASCRIPT)
                .findWhenNeeded(this);


        WebElement saveAndFinishBtn()
        {
            return Locator.tagWithClass("button", "labkey-button")
                .withText("Save and Finish").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        }
    }
}
