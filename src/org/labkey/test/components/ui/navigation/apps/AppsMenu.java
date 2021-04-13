package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BaseBootstrapMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wraps the expand/collapse toggle piece of the Apps menu in Biologics and SampleManager.
 * In LKS views, LKAppsMenu is the analog of this component
 */
public class AppsMenu extends BaseBootstrapMenu
{
    protected AppsMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    /**
     * use this to get the products listing panel
     * @return
     */
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
        return Locator.tagWithAttribute("button", "id", "product-navigation-button");
    }


    public static class AppsMenuFinder extends WebDriverComponent.WebDriverComponentFinder<AppsMenu, AppsMenuFinder>
    {
        private Locator _locator = Locator.tagWithClass("div", "navbar-item-product-navigation")
                .child(Locator.tagWithClass("div", "dropdown"))
                .withChild(Locator.id("product-navigation-button"));

        public AppsMenuFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }

        @Override
        protected AppsMenu construct(WebElement el, WebDriver driver)
        {
            return new AppsMenu(el, driver);
        }
    }
}
