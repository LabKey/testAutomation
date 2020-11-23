package org.labkey.test.components.glassLibrary.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.DropdownButtonGroup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     This is a navigational component for the breadcrumb.ts element <i>I think</i>.
 * </p>
 * <p>
 *     The breadcrumb navigation component is an ordered list of elements <i>ol / li</i> where some of the elements
 *     may be links and others are just text. It is also possible that some of the elements will have a dropdown menu
 *     associated with them.
 * </p>
 */
public class FMBreadCrumbs extends WebDriverComponent<FMBreadCrumbs.ElementCache>
{
    final WebElement _breadCrumbsElement;
    final WebDriver _driver;

    public FMBreadCrumbs(WebDriver driver)
    {
        this(Locators.component.findElement(driver), driver);
    }

    private FMBreadCrumbs(WebElement element, WebDriver driver)
    {
        _breadCrumbsElement = element;
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
        return _breadCrumbsElement;
    }

    /**
     * Private function that returns all of the li/span elements in this ol element.
     *
     * @return List of span tags in the ordered list.
     */
    private List<WebElement> getBreadCrumbElements()
    {
        return Locators.breadCrumb.findElements(this);
    }

    /**
     * Return the text value of the breadcrumbs.
     *
     * @return List containing the text of the breadcrumbs.
     */
    public List<String> getBreadCrumbs()
    {
        List<String> breadCrumbs = new ArrayList<>();
        for(WebElement crumb : getBreadCrumbElements())
        {
            breadCrumbs.add(crumb.getText());
        }

        return breadCrumbs;
    }

    /**
     * <p>
     *     A bread crumb may or may not be a link. If it is a link use this to click it.
     * </p>
     * <p>
     *     It is possible that clicking a bread link will cause a navigation event. However this method makes no
     *     assumptions as to what the click action will be.
     * </p>
     *
     * @param breadCrumbText The text of the breadcrumb.
     */
    public void clickBreadCrumbLink(String breadCrumbText)
    {
        elementCache().breadCrumbLink(breadCrumbText).click();
    }

    /**
     * Check to see if the given breadcrumb has a dropdown option.
     *
     * @param breadCrumbText The text of the breadcrumb.
     * @return Return true if the dropdown button is visible, false otherwise.
     */
    public boolean breadCrumbHasDropdown(String breadCrumbText)
    {
        // Use a LazyWebElement to find the dropdown. Using a LazyWebElement will return false for isDisplayed if the
        // element is not there, other finders throw a "No Such Element" exception.
        return Locators.dropDownButton.findWhenNeeded(elementCache().breadCrumb(breadCrumbText)).isDisplayed();
    }

    /**
     * Get the text from the breadcrumb's dropdown menu.
     *
     * @param breadCrumbText The text of the breadcrumb.
     * @return A list of strings with the text from the dropdown menu.
     */
    public List<String> getBreadCrumbDropdownText(String breadCrumbText)
    {
        WebElement breadCrumb = elementCache().breadCrumb(breadCrumbText);
        DropdownButtonGroup dropDownMenu = elementCache().dropDownMenu(breadCrumb);
        if(!dropDownMenu.isExpanded())
        {
            dropDownMenu.expand();
        }
        return elementCache().dropDownMenu(breadCrumb).getMenuItemTexts();
    }

    /**
     * <p>
     *     Click a menu item in the dropdown menu.
     * </p>
     * <p>
     *     Clicking the menu item may or may not result in a page navigation. This control makes no assumptions as to
     *     what the click action will do.
     * </p>
     *
     * @param breadCrumbText The text of the breadcrumb.
     * @param menuText The text of the menu item to click.
     */
    public void clickDropdownMenuItem(String breadCrumbText, String menuText)
    {
        WebElement breadCrumb = elementCache().breadCrumb(breadCrumbText);
        DropdownButtonGroup dropDownMenu = elementCache().dropDownMenu(breadCrumb);

        if(!dropDownMenu.isExpanded())
        {
            dropDownMenu.expand();
        }

        elementCache().dropDownMenu(breadCrumb).clickSubMenu(menuText);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    // The breadcrumbs are dynamic. Elements in it can come and go depending upon what click actions are hooked up to
    // the elements. Because of that I didn't want the ElementCache to have any general WebElements. I could have used
    // LazyWebElements and call refindWhenNeeded to address that, but because the breadcrumb elements are very text
    // centric it made more sense to create methods that take the menu text as an argument or in the case of the dropdown
    // menu the parent breadcrumb element.
    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement breadCrumbLink(String breadCrumbText)
        {
            return Locator.tagWithClassContaining("a", "location-breadcrumb-loc-link")
                    .withText(breadCrumbText)
                    .findElement(this);
        }

        WebElement breadCrumb(String breadCrumbText)
        {
            return Locators.breadCrumb.withText(breadCrumbText).findElement(this);
        }

        DropdownButtonGroup dropDownMenu(WebElement breadCrumb)
        {
            return new DropdownButtonGroup.DropdownButtonGroupFinder(getDriver()).find(breadCrumb);
        }
    }

    // Some of the locators are used outside of the ELementCach class.
    protected static class Locators
    {
        static Locator component = Locator.tagWithClass("ol", "location-breadcrumb");
        static Locator breadCrumb = Locator.tag("li").childTag("span");
        static Locator dropDownButton = Locator.tagWithClassContaining("div", "dropdown");
    }

    public static class FMBreadCrumbsFinder extends WebDriverComponentFinder<FMBreadCrumbs, FMBreadCrumbsFinder>
    {
        private Locator _locator;

        public FMBreadCrumbsFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locators.component;
        }

        @Override
        protected FMBreadCrumbs construct(WebElement el, WebDriver driver)
        {
            return new FMBreadCrumbs(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}
