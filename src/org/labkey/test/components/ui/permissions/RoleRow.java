package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RoleRow extends PermissionsRowBase<RoleRow>
{
    protected RoleRow(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    protected WebElement getAssignmentContainer()
    {
        return Locator.byClass("permissions-assignments-row").waitForElement(getComponentElement(), 2000);
    }

    @Override
    protected RoleRow getThis()
    {
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends PermissionsRowBase.ElementCache
    {
    }

    public static class RoleRowFinder extends WebDriverComponentFinder<RoleRow, RoleRow.RoleRowFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "container-expandable")
                .withChild(Locator.tagWithClass("div", "container-expandable-grey"));
        private String _roleTitle = null;

        public RoleRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public RoleRow.RoleRowFinder forRole(String roleTitle)
        {
            _roleTitle = roleTitle;
            return this;
        }

        @Override
        protected RoleRow construct(WebElement el, WebDriver driver)
        {
            return new RoleRow(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_roleTitle != null)
                return _baseLocator.withDescendant(Locator.byClass("permissions-title").withText(_roleTitle));
            else
                return _baseLocator;
        }
    }
}
