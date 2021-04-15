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
     *  Navigates to the specifed location.
     * @param product   The product- SampleManager, or Biologics.  (If LabKey, use navigateToLabKey instead)
     * @param project   The project to navigate to
     * @param node      The product section in the target project
     */
    public void navigateTo(ProductsNavContainer.Product product, String project, String node)
    {
        /*
            If we are navigating to a Biologics or SampleManager project and there is only on project of that type
            on the system, the menu component won't show the projects pane (making the user click once extra is
            redundant).  If clickProduct fails to find a projects panel, attempt to find the leafContainer directly
            and complete the action
         */
        var productsPanel = showProductsPanel();
        ProjectsNavContainer projectsPanel = null;
        try
        {
            projectsPanel = productsPanel.clickProduct(product);
        } catch (NoSuchElementException nse)
        {
            // assume here that the project choice was not presented, attempt to find the leaf-level container
            // and complete the navigation
            new LeafNavContainer.LeafNavContainerFinder(getDriver()).withBackNavTitle(project)
                    .waitFor()
                    .clickItem(node);
        }

        // if we're here, we were shown (and have found) the projects panel. Select the project and finish navigating
        projectsPanel.clickProject(project)
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
