package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.react.Tabs;
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

    public <D extends AuthDialogBase<?>> D addConfiguration(AuthenticationProvider<D> authenticationProvider)
    {
        togglePrimaryConfiguration();
        elementCache().addPrimaryMenu.
                clickSubMenu(false, authenticationProvider.getProviderName() + " : " + authenticationProvider.getProviderDescription());

        return authenticationProvider.getNewDialog(getDriver());
    }

    public boolean canAddConfiguration()
    {
        togglePrimaryConfiguration();
        return elementCache().primaryMenuFinder.findOptional(getDriver()).isPresent();
    }

    public <D extends AuthDialogBase<?>> D addSecondaryConfiguration(AuthenticationProvider<D> authenticationProvider)
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

    public boolean isSecondaryConfOptionEnabled(String option)
    {
        toggleSecondaryConfiguration();
        return !elementCache().addSecondaryMenu.isMenuItemDisabled(option);
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

    private WebElement togglePrimaryConfiguration()
    {
        return elementCache().authTabs.selectTab("Primary");
    }

    public LoginConfigRow getPrimaryConfigurationRow(String description)
    {
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description)
                .waitFor(togglePrimaryConfiguration());
    }

    public List<LoginConfigRow> getPrimaryConfigurations()
    {
        return new LoginConfigRow.LoginConfigRowFinder(getDriver())
                .findAll(togglePrimaryConfiguration());
    }

    private WebElement toggleSecondaryConfiguration()
    {
        return elementCache().authTabs.selectTab("Secondary");
    }

    public List<LoginConfigRow> getSecondaryConfigurations()
    {
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).findAll(toggleSecondaryConfiguration());
    }

    public LoginConfigRow getSecondaryConfigurationRow(String description)
    {
        return new LoginConfigRow.LoginConfigRowFinder(getDriver())
                .withDescription(description).waitFor(toggleSecondaryConfiguration());
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

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        Checkbox selfSignupCheckBox = new Checkbox(this, "Allow self sign up");
        Checkbox allowUserEmailEditCheckbox = new Checkbox(this, "Allow users to edit their own email addresses");
        Checkbox autoCreateCheckBox = new Checkbox(this,"Auto-create authenticated users");
        final Tabs authTabs = new Tabs.TabsFinder(getDriver()).locatedBy(Locator.byClass("lk-tabs")).findWhenNeeded(this);
        MultiMenu.MultiMenuFinder primaryMenuFinder = new MultiMenu.MultiMenuFinder(getDriver())
                .withText("Add New Primary Configuration").timeout(WAIT_FOR_JAVASCRIPT);
        BootstrapMenu addPrimaryMenu = primaryMenuFinder.findWhenNeeded(this);
        MultiMenu.MultiMenuFinder secondaryMenuFinder = new MultiMenu.MultiMenuFinder(getDriver())
                .withText("Add New Secondary Configuration").timeout(WAIT_FOR_JAVASCRIPT);
        MultiMenu addSecondaryMenu = secondaryMenuFinder.findWhenNeeded(this);

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
