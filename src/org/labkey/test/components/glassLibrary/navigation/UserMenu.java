package org.labkey.test.components.glassLibrary.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.glassLibrary.components.MultiMenu;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.BiFunction;

/**
 * Base class for app user menus implemented using @labkey-components/UserMenu
 */
public abstract class UserMenu extends BootstrapMenu
{
    private static final String MENU_ID = "user-menu-dropdown";

    protected UserMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    protected static <U extends UserMenu> SimpleWebDriverComponentFinder<U> baseMenuFinder(WebDriver driver, BiFunction<WebElement, WebDriver, U> factory)
    {
        return new MultiMenu.MultiMenuFinder(driver).withButtonId(MENU_ID).wrap(factory);
    }

    // TODO: Placeholder for product update
    protected LabKeyPage<?> login()
    {
        clickSubMenu(true, "Sign In");
        return new LabKeyPage<>(getDriver());
    }

    // TODO: Placeholder for product update
    protected LabKeyPage<?> logout()
    {
        clickSubMenu(true, "Sign Out");
        return new LabKeyPage<>(getDriver());
    }

    @Override
    protected Locator getToggleLocator()
    {
        return Locator.id(MENU_ID);
    }
}
