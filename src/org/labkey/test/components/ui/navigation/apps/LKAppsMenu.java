package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.components.html.BaseBootstrapMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wraps the expand/collapse toggle to show/hide the Apps-navigation menu in LabKey pages
 * In SM or Biologics views, AppsMenu is the analog of this component
 */
public class LKAppsMenu extends BaseBootstrapMenu
{
    protected LKAppsMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    public ProductsNavContainer showProductsPanel()
    {
        expand();
        return new ProductsNavContainer.ProductNavContainerFinder(getDriver()).withTitle("Applications")
                .waitFor();
    }

    public void navigateTo(ProductsNavContainer.Product product, String project, String node)
    {
        showProductsPanel()
                .clickProduct(product)
                .clickProject(project)
                .clickItem(node);
    }

    public void navigateToLabkey(String project)
    {
        showProductsPanel()
                .clickLabkey()
                .clickProject(project);
    }

    @Override
    protected Locator getToggleLocator()
    {
        return Locator.tagWithAttribute("a", "data-toggle", "dropdown");
    }

    public static class LKAppsMenuFinder extends WebDriverComponentFinder<LKAppsMenu, LKAppsMenuFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.id("headerProductDropdown");

        public LKAppsMenuFinder(WebDriver driver)
        {
            super(driver);
        }


        @Override
        protected LKAppsMenu construct(WebElement el, WebDriver driver)
        {
            return new LKAppsMenu(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
