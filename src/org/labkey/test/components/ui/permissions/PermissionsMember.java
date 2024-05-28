package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class PermissionsMember extends WebDriverComponent<PermissionsMember.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected PermissionsMember(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
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

    public void remove()
    {
        elementCache()._removeButton.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(_el));
    }

    public void select()
    {
        elementCache()._nameButton.click();
        getWrapper().shortWait().until(ExpectedConditions.attributeContains(elementCache()._nameButton, "class", "primary"));
    }

    public String getName()
    {
        return elementCache()._nameButton.getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        private final WebElement _removeButton = Locator.byClass("btn")
                .withChild(Locator.byClass("fa-remove")).findWhenNeeded(this);
        private final WebElement _nameButton = Locator.byClass("permissions-button-display")
                .findWhenNeeded(this).withTimeout(1000);
    }

    public static class PermissionsMemberFinder extends WebDriverComponentFinder<PermissionsMember, PermissionsMemberFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("li","permissions-groups-member-li");
        private String _title = null;

        public PermissionsMemberFinder(WebDriver driver)
        {
            super(driver);
        }

        public PermissionsMemberFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected PermissionsMember construct(WebElement el, WebDriver driver)
        {
            return new PermissionsMember(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withDescendant(
                        Locator.tagWithClass("button", "permissions-button-display").startsWith(_title));
            else
                return _baseLocator;
        }
    }
}
