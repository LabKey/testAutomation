package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.pages.ldap.LdapConfigurePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.InvocationTargetException;

public class LoginConfigurePage extends LabKeyPage<LoginConfigurePage.ElementCache>
{
    public LoginConfigurePage(WebDriver driver)
    {
        super(driver);
    }

    public static LoginConfigurePage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("login",  "configure"));
        return new LoginConfigurePage(webDriverWrapper.getDriver());
    }

    public <P extends PrimaryAuthenticationProviderConfigurationPage> P addConfiguration(Class<P> authType)
    {
        P page;
        try
        {
            page = authType.getDeclaredConstructor(WebDriver.class).newInstance(getDriver());
        }
        catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
        {
            throw new RuntimeException("Unable to instantiate page class: " + authType.getName(), e);
        }

        elementCache().addMenu.
                clickSubMenu(true, page.getProviderName() + " - " + page.getProviderDescription());

        return page;
    }

    public LoginConfigurePage removeConfiguration(String description)
    {
        // the html table class does not support finding a row's index correctly; doing it ourself here
        WebElement row = Locator.xpath("//tbody/tr").containing(description).findElement(elementCache().tableElement);
        Locator.tagWithClass("a", "labkey-text-link").withText("delete").findElement(row).click();
        // handle dialog
        acceptAlert();
        // pause, because that works
        sleep(1000);
        return this;
    }

    public LdapConfigurePage clickEditConfiguration(String description)
    {
        WebElement row = Locator.xpath("//tbody/tr").containing(description).findElement(elementCache().tableElement);
        clickAndWait(Locator.tagWithClass("a", "labkey-text-link").withText("delete").findElement(row));

        return new LdapConfigurePage(getDriver());
    }

    public ShowAdminPage clickDone()
    {
        clickAndWait(elementCache().doneBtn());
        return new ShowAdminPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement primaryConfigsPanel()
        {
            return Locator.tagWithClass("div", "panel-default")
                    .withDescendant(Locator.tagWithClass("h3", "panel-title").withText("Primary authentication configurations"))
                    .waitForElement(this, WAIT_FOR_JAVASCRIPT);
        }

        BootstrapMenu addMenu = new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("div", "lk-menu-drop")
                .withChild(Locator.tagWithAttribute("a", "data-toggle", "dropdown").withText("Add..."))
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));

        WebElement addAuthButton = Locator.linkWithSpan("Add...").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        WebElement tableElement = Locator.tagWithClass("table", "labkey-data-region-legacy")
                .findElement(primaryConfigsPanel());

        WebElement doneBtn()
        {
            return Locator.lkButton("Done").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }
    }
}
