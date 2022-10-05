package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.components.react.ReactSelect;
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

    protected class ElementCache extends PermissionsRowBase<RoleRow>.ElementCache
    {
        public ElementCache()
        {
            super(null, // group options show the raw group name
                    ReactSelect.Locators.option::startsWith // users options may end with display name
            );
        }
    }

    public static class RoleRowFinder extends PermissionsRowFinder<RoleRow, RoleRowFinder>
    {

        public RoleRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public RoleRowFinder forRole(String roleTitle)
        {
            super.withTitle(roleTitle);
            return this;
        }

        @Override
        protected RoleRow construct(WebElement el, WebDriver driver)
        {
            return new RoleRow(el, driver);
        }
    }
}
