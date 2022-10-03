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

    protected class ElementCache extends PermissionsRowBase<RoleRow>.ElementCache
    {
    }

    public static class RoleRowFinder extends PermissionsRowFinder<RoleRow>
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
