package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class LoginConfigurePage extends LabKeyPage<LoginConfigurePage.ElementCache>
{
    public LoginConfigurePage(WebDriver driver)
    {
        super(driver);
        waitForPage();
    }

    public static LoginConfigurePage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("login", "configure"));
        return new LoginConfigurePage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        Locator.waitForAnyElement(shortWait(), Locator.button("Done"), Locator.button("Cancel"));
    }

    public <D extends AuthDialogBase> D addConfiguration(AuthenticationProvider<D> authenticationProvider)
    {
        togglePrimaryConfiguration();
        elementCache().addPrimaryMenu.
                clickSubMenu(false, authenticationProvider.getProviderName() + " : " + authenticationProvider.getProviderDescription());

        return authenticationProvider.getNewDialog(getDriver());
    }

    public <D extends AuthDialogBase> D editConfiguration(AuthenticationProvider<D> authenticationProvider, String description)
    {
        return clickEditConfiguration(description, authenticationProvider);
    }

    public boolean canAddConfiguration()
    {
        togglePrimaryConfiguration();
        return elementCache().primaryMenuFinder.findOptional(getDriver()).isPresent();
    }

    public <D extends AuthDialogBase> D addSecondaryConfiguration(AuthenticationProvider<D> authenticationProvider)
    {
        toggleSecondaryConfiguration();
        WebDriverWrapper.waitFor(() -> elementCache().addSecondaryMenu.getComponentElement().isDisplayed(), 2000);
        elementCache().addSecondaryMenu.
                clickSubMenu(false, authenticationProvider.getProviderName() + " : " + authenticationProvider.getProviderDescription());

        return authenticationProvider.getNewDialog(getDriver());
    }

    public boolean canAddSecondaryConfiguration()
    {
        toggleSecondaryConfiguration();
        return elementCache().secondaryMenuFinder.findOptional(getDriver()).isPresent();
    }

    private boolean isPrimarySelected()
    {
        return elementCache().panelTab1.getAttribute("aria-selected").equals("true");
    }

    private boolean isSecondarySelected()
    {
        return elementCache().panelTab2.getAttribute("aria-selected").equals("true");
    }

    // global settings
    public LoginConfigurePage setSelfSignup(boolean enable)
    {
        elementCache().selfSignupCheckBox.set(enable);
        return this;
    }

    public boolean getSelfSignupEnabled()
    {
        return elementCache().selfSignupCheckBox.get();
    }

    public boolean getAllowEditEmail()
    {
        return elementCache().allowUserEmailEditCheckbox.get();
    }

    public LoginConfigurePage setAllowEditEmail(boolean enable)
    {
        elementCache().allowUserEmailEditCheckbox.set(enable);
        return this;
    }

    public LoginConfigurePage setAutoCreate(boolean enable)
    {
        elementCache().autoCreateCheckBox.set(enable);
        return this;
    }

    public boolean getAutoCreateEnabled()
    {
        return elementCache().autoCreateCheckBox.get();
    }

    public LoginConfigurePage togglePrimaryConfiguration()
    {
        if (!isPrimarySelected())
            elementCache().panelTab1.click();
        waitFor(() -> isPrimarySelected(), 1000);
        return this;
    }

    public LoginConfigRow getPrimaryConfigurationRow(String description)
    {
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description)
                .waitFor(elementCache().tabPane1);
    }

    public List<LoginConfigRow> getPrimaryConfigurations()
    {
        togglePrimaryConfiguration();
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).findAll(elementCache().tabPane1);
    }

    public LoginConfigurePage toggleSecondaryConfiguration()
    {
        if (!isSecondarySelected())
            elementCache().panelTab2.click();
        waitFor(() -> isSecondarySelected(), 1000);
        return this;
    }

    public List<LoginConfigRow> getSecondaryConfigurations()
    {
        toggleSecondaryConfiguration();
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).findAll(elementCache().tabPane2);
    }

    public LoginConfigRow getSecondaryConfigurationRow(String description)
    {
        toggleSecondaryConfiguration();
        return new LoginConfigRow.LoginConfigRowFinder(getDriver())
                .withDescription(description).waitFor(elementCache().tabPane2);
    }

    public LoginConfigurePage removeConfiguration(String description)      // assumes for now we're doing primary only
    {
        new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description).waitFor()
                .clickDelete();
        return this;
    }

    public <D extends AuthDialogBase> D clickEditConfiguration(String description, AuthenticationProvider<D> authenticationProvider)
    {
        LoginConfigRow row = new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description).waitFor(getDriver());
        return row.clickEdit(authenticationProvider);
    }

    public ShowAdminPage clickSaveAndFinish()
    {
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().saveAndFinishBtn()));
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
        Checkbox selfSignupCheckBox = new Checkbox(this, "Allow self sign up");
        Checkbox allowUserEmailEditCheckbox = new Checkbox(this, "Allow users to edit their own email addresses");
        Checkbox autoCreateCheckBox = new Checkbox(this,"Auto-create authenticated users");
        WebElement tabPanel = Locator.id("tab-panel").refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement panelTab1 = Locator.id("tab-panel-tab-1").refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement tabPane1 = Locator.id("tab-panel-pane-1").refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement panelTab2 = Locator.id("tab-panel-tab-2").refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement tabPane2 = Locator.id("tab-panel-pane-2").refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        MultiMenu.MultiMenuFinder primaryMenuFinder = new MultiMenu.MultiMenuFinder(getDriver())
                .withText("Add New Primary Configuration").timeout(WAIT_FOR_JAVASCRIPT);
        BootstrapMenu addPrimaryMenu = primaryMenuFinder.findWhenNeeded(this);
        MultiMenu.MultiMenuFinder secondaryMenuFinder = new MultiMenu.MultiMenuFinder(getDriver())
                .withText("Add New Secondary Configuration").timeout(WAIT_FOR_JAVASCRIPT);
        BootstrapMenu addSecondaryMenu = secondaryMenuFinder.findWhenNeeded(this);

        WebElement globalSettingsPanel()
        {
            return Locator.tagWithClass("div", "panel-default")
                    .withChild(Locator.tagWithClass("div", "panel-heading")
                            .withChild(Locator.tag("span").withText("Global Settings")))
                    .waitForElement(this, WAIT_FOR_JAVASCRIPT);
        }

        WebElement configurationsPanel()
        {
            return Locator.tagWithClass("div", "panel-default")
                    .withDescendant(Locator.tagWithClass("div", "panel-heading").withChild(Locator.tag("span").withText("Configurations")))
                    .waitForElement(this, WAIT_FOR_JAVASCRIPT);
        }

        WebElement saveAndFinishBtn()
        {
            return Locator.tagWithClass("button", "labkey-button")
                    .withText("Save and Finish").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        }
    }
}
