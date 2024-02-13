package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.BaseBootstrapMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

/**
 * Wraps the expand/collapse toggle piece of the Apps menu in Biologics and SampleManager.
 * In LKS views, LKAppsMenu is the analog of this component
 */
public class AppsMenu extends WebDriverComponent<AppsMenu.ElementCache>
{
    private final WebElement _componentElement;
    private final WebDriver _driver;

    protected AppsMenu(WebElement element, WebDriver driver)
    {
        _componentElement = element;
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    protected boolean isExpanded()
    {
        return "true".equals(elementCache().toggle.getAttribute("aria-expanded"));
    }

    protected boolean isLoaded()
    {
        return elementCache().getList().isPresent();
    }

    public void expand()
    {
        if (!isExpanded())
        {
            elementCache().toggle.click();
            WebDriverWrapper.waitFor(this::isLoaded, "AppsMenu did not expand as expected", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
    }

    public void collapse()
    {
        if (isExpanded())
        {
            elementCache().toggle.click();
        }
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
    protected AppsMenu.ElementCache newElementCache()
    {
        return new AppsMenu.ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public final WebElement rootElement = Locator.byClass("product-navigation-menu").findWhenNeeded(getDriver());
        public final WebElement toggle = Locator.byClass("navbar-menu-button").findWhenNeeded(rootElement);

        public Optional<WebElement> getList()
        {
            return Locator.byClass("product-navigation-listing").findOptionalElement(rootElement);
        }
    }


    public static class AppsMenuFinder extends WebDriverComponent.WebDriverComponentFinder<AppsMenu, AppsMenuFinder>
    {
        private Locator _locator = Locator.byClass("product-navigation-menu");

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
