package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BaseBootstrapMenu;
import org.openqa.selenium.NoSuchElementException;
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
     * @return The menu panel that wraps product selection
     */
    public ProductsNavContainer showProductsPanel()
    {
        expand();
        return new ProductsNavContainer.ProductNavContainerFinder(getDriver()).withTitle("Applications")
                .waitFor();
    }

    /**
     *  Navigates to the specified location.
     * @param product   The product- SampleManager, or Biologics.  (If LabKey, use navigateToLabKey instead)
     * @param node      The product section in the target project
     */
    public void navigateTo(ProductsNavContainer.Product product, String node)
    {
        var productsPanel = showProductsPanel();
        productsPanel
                .clickProduct(product)
                .clickItem(node);
    }

    public void navigateToLabKey(String project)
    {
        showProductsPanel()
                .clickLabkey()
                .clickProject(project);
    }


    @Override
    protected Locator getToggleLocator()
    {
        // the toggle appears differently in-app (LKSM, Biologics) than it does in LKS.
        return Locator.XPathLocator.union(
                Locator.tagWithAttribute("button", "id", "product-navigation-button"),  // lksm/bio
                Locator.tagWithAttribute("a", "data-toggle", "dropdown"));              // lks
    }


    public static class AppsMenuFinder extends WebDriverComponent.WebDriverComponentFinder<AppsMenu, AppsMenuFinder>
    {
        private Locator _locator = Locator.XPathLocator.union(
                Locator.tagWithClass("div", "navbar-item-product-navigation")   //lksm/bio
                .child(Locator.tagWithClass("div", "dropdown"))
                .withChild(Locator.id("product-navigation-button")),
                Locator.id("headerProductDropdown"));                                       //lks

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
