
package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;

/**
 * This component is redundant (I think)
 * @deprecated Use {@link org.labkey.test.components.html.BootstrapMenu} or {@link MultiMenu}
 */
@Deprecated
public class DropdownButtonGroup extends WebDriverComponent<DropdownButtonGroup.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public DropdownButtonGroup(WebElement el, WebDriver driver)
    {
        _el = el;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public boolean isExpanded()
    {
        return "true".equals(elementCache().toggleAnchor.getAttribute("aria-expanded"));
    }

    public void expand()
    {
        if (!isExpanded())
        {
            getWrapper().scrollIntoView(elementCache().toggleAnchor);
            elementCache().toggleAnchor.click();
        }
        WebDriverWrapper.waitFor(this::isExpanded, "Menu did not expand as expected", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public DropdownButtonGroup collapse()
    {
        if (isExpanded())
            elementCache().toggleAnchor.click();
        WebDriverWrapper.waitFor(()-> !isExpanded(), "Menu did not collapse as expected", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return  this;
    }

    public String getButtonText()
    {
        return elementCache().toggleAnchor.getText();
    }

    public DropdownButtonGroup closeVisibleSubmenus()
    {
        getWrapper().waitFor(()-> {
            WebElement openToggle = Locators.openMenuCaret.findElementOrNull(getComponentElement());
            if (openToggle == null)
                return true;
            else
                openToggle.click();
            return false;
        }, 1500);
        return this;
    }

    private String componentId()
    {
        return elementCache().toggleAnchor.getAttribute("id");
    }

    @LogMethod(quiet = true)
    public WebElement openMenuTo(@LoggedParam String ... subMenuLabels)
    {
        expand();

        if (subMenuLabels.length == 0)
            return null;

        WebElement context = getRootMenu();
        WebElement item = null;
        for (int i = 0; i < subMenuLabels.length -1; i++)
        {
            context = openSubmenu(subMenuLabels[i], context);
        }
        item = findNextMenuItem(subMenuLabels[subMenuLabels.length - 1], context);

        getWrapper().fireEvent(item, WebDriverWrapper.SeleniumEvent.mouseover); /* mouseOver causes selenium to attempt
                                to scroll the item into view, and if that can't be done we'll fail here.  See if firing
                                the event is less flaky */
        return item;
    }

    @LogMethod(quiet = true)
    public List<WebElement> getMenuItemsAt(@LoggedParam String ... subMenuLabels)
    {
        expand();
        sleep(500);
        WebElement context = getRootMenu();

        for (int i = 0; i < subMenuLabels.length; i++)
        {
            context = openSubmenu(subMenuLabels[i], context);
        }

        return Locators.menuItem().findElements(context);
    }

    public List<String> getMenuItemTexts(@LoggedParam String ... subMenuLabels)
    {
        return getWrapper().getTexts(getMenuItemsAt(subMenuLabels));
    }

    private WebElement getRootMenu()
    {
        WebElement rootMenu = Locator.tagWithClass("ul", "dropdown-menu")
                .withAttribute("aria-labelledby", componentId()).findElement(getComponentElement());
        WebDriverWrapper.waitFor(()-> Locators.menuItem().findElements(rootMenu).size() > 0,
                "the root menu did not have items in time", WAIT_FOR_JAVASCRIPT);
        return rootMenu;
    }

    // returns the item to be clicked.
    @LogMethod
    private WebElement findNextMenuItem(@LoggedParam String itemText, WebElement context)
    {
        WebElement item = Locators.menuItem(itemText)
                .waitForElement(context, 2000);
        return item;
    }

    // finds and opens the next submenu item.  Returns the child UL of the specified
    private WebElement openSubmenu(@LoggedParam String itemText, WebElement context)
    {
        WebElement item = Locators.subMenuItem(itemText).waitForElement(context, 4000);

        if (!menuItemIsExpanded(item))
        {
            getWrapper().log("attempting to expand menu item [" + itemText + "]");
            item.click();
        }

        getWrapper().waitFor(()-> menuItemIsExpanded(item), "menu item ["+itemText+"] failed to open", 2000);

        return Locators.childMenuLoc().waitForElement(item, 2000);  // It's the child <ul> of the current submenu item
    }

    // itemcontext is the li containing the link and toggle, unless it is top-level (in which case context is the entire component).
    // itemtext is the text of the menu toggle to be expanded
    // nextItemText is the text of the item the opened child menu should contain
    private boolean menuItemIsExpanded(WebElement itemContext)
    {
        WebElement openCaret = Locators.openMenuCaret.findElementOrNull(itemContext);
        WebElement closedCaret = Locators.closedMenuCaret.findElementOrNull(itemContext);
        WebElement childMenu = Locators.childMenuLoc().findElementOrNull(itemContext);
        boolean isExpanded =  openCaret != null &&      // chevron-up signals current submenu/context is open
                closedCaret == null &&                  // chevron-down signals current submenu is closed.
                childMenu != null;                      // child ul element is created when the menu item is opened
        return isExpanded;
    }

    @LogMethod(quiet = true)
    public void clickSubMenu(@LoggedParam String ... subMenuLabels)
    {
        if (subMenuLabels.length < 1)
            throw new IllegalArgumentException("Specify menu item(s)");

        WebElement item = openMenuTo(subMenuLabels);
        getWrapper().waitFor(()-> item.isEnabled(),
                "expect menu item to be enabled before clicking", 2000);
        item.click();
    }

    public void clickMainButton()
    {
        getComponentElement().click();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        public WebElement toggleAnchor = Locators.toggleAnchor().refindWhenNeeded(getComponentElement());
    }

    static public class Locators
    {
        public static Locator.XPathLocator toggleAnchor()
        {
            return Locator.tagWithAttributeContaining("button", "class", "dropdown-toggle");
        }

        public static Locator.XPathLocator menuItem(String text)
        {
            return menuItem().withChild(menuAnchor(text)).notHidden();
        }
        public static Locator.XPathLocator menuAnchor(String text)
        {
            return Locator.tagWithAttribute("a", "role", "menuitem").withText(text);
        }

        public static Locator.XPathLocator subMenuItem()
        {
            return Locator.tagWithClass("li", "dropdown-submenu");
        }

        public static Locator.XPathLocator subMenuItem(String text)
        {
            return subMenuItem().withChild(menuAnchor(text));
        }

        public static Locator.XPathLocator menuItem()
        {
            return Locator.tagWithAttribute("li", "role", "presentation").withChild(
                    Locator.tagWithAttribute("a", "role", "menuitem"));
        }

        public static Locator.XPathLocator openMenuCaret = Locator.tagWithClass("i", "fa-chevron-up");
        public static Locator.XPathLocator closedMenuCaret = Locator.tagWithClass("i", "fa-chevron-down");

        static public Locator.XPathLocator childMenuLoc()
        {
            return Locator.tagWithClass("ul", "well");
        }

        static public Locator.XPathLocator buttonGroupWithId(String Id)
        {
            return Locator.tagWithClass("div", "dropdown").withChild(Locators.toggleAnchor().withAttribute("id", Id));
        }

        static public Locator.XPathLocator buttonGroupWithClass(String cls)
        {
            return Locator.tagWithClass("div", "dropdown").withClass(cls);
        }
    }

    public static class DropdownButtonGroupFinder extends WebDriverComponentFinder<DropdownButtonGroup, DropdownButtonGroupFinder>
    {
        private Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "dropdown")
                .withChild(Locators.toggleAnchor());

        public DropdownButtonGroupFinder(WebDriver driver)
        {
            super(driver);
        }

        public DropdownButtonGroupFinder withButtonText(String text)
        {
            _baseLocator = Locator.tagWithClass("div", "dropdown").withChild(Locators.toggleAnchor().withText(text));
            return this;
        }

        public DropdownButtonGroupFinder withButtonSpanText(String text)
        {
            _baseLocator = Locator.tagWithClass("div", "dropdown")
                    .withChild(Locators.toggleAnchor().withChild(Locator.tag("span").withText(text)));
            return this;
        }

        public DropdownButtonGroupFinder withButtonId(String Id)
        {
            _baseLocator = Locators.buttonGroupWithId(Id);
            return this;
        }

        public DropdownButtonGroupFinder withButtonClass(String cls)
        {
            _baseLocator = Locators.buttonGroupWithClass(cls);
            return this;
        }

        @Override
        protected DropdownButtonGroup construct(WebElement el, WebDriver driver)
        {
            return new DropdownButtonGroup(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
