package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginConfigRow extends WebDriverComponent<LoginConfigRow.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    protected LoginConfigRow(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public String getDescription()
    {
        return elementCache().description.getText();
    }

    public String getDetail()
    {
        return elementCache().details.getText();
    }

    public String getProvider()
    {
        return elementCache().provider.getText();
    }

    public LoginConfigurePage clickDelete()
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().deleteButton));
        elementCache().deleteButton.click();
        new ModalDialog.ModalDialogFinder(getDriver()).withBodyTextContaining("Deletion cannot be undone.")
                .waitFor().dismiss("Yes, delete");
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
        return new LoginConfigurePage(getDriver());
    }

    public <P extends AuthDialogBase> P clickEdit(AuthenticationProvider<P> authenticationProvider)
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().editButton));
        elementCache().editButton.click();
        return authenticationProvider.getEditDialog(this);
    }

    public boolean canEdit()
    {
        return elementCache().editButtonLoc.existsIn(this);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        final WebElement baseFieldsElement = Locator.tagWithClass("div", "domain-row-base-fields").findWhenNeeded(this);
        final WebElement description = Locator.tagWithClass("div", "description").findWhenNeeded(baseFieldsElement);
        final WebElement details = Locator.tagWithClass("div", "details").findWhenNeeded(baseFieldsElement);
        final WebElement provider = Locator.tagWithClass("div", "provider").findWhenNeeded(baseFieldsElement);

        WebElement statusElement = Locator.tagWithClass("div", "col-xs-7")
                .withChild(Locator.tagWithClass("span", "fa-circle")).findWhenNeeded(this);

        final WebElement deleteButton = Locator.tagWithClass("span", "fa-times-circle")
                .findWhenNeeded(this).withTimeout(2000);
        Locator editButtonLoc = Locator.tagWithClass("span", "fa-pencil");
        final WebElement editButton = editButtonLoc.findWhenNeeded(this).withTimeout(2000);
    }


    public static class LoginConfigRowFinder extends WebDriverComponentFinder<LoginConfigRow, LoginConfigRowFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "auth-row").notHidden();
        private String _description = null;

        public LoginConfigRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public LoginConfigRowFinder withDescription(String description)
        {
            _description = description;
            return this;
        }

        @Override
        protected LoginConfigRow construct(WebElement el, WebDriver driver)
        {
            return new LoginConfigRow(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_description != null)
                return _baseLocator.withDescendant(Locator.tagWithClass("div", "description").withText( _description));
            else
                return _baseLocator;
        }
    }
}
