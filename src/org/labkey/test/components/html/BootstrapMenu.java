package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.Component;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class BootstrapMenu extends Component
{
    protected WebDriverWrapper _driver;
    protected WebElement _componentElement;
    protected BootstrapMenu.Elements _elements;

    /* componentElement should contain the toggle anchor *and* the UL containing list items */
    public BootstrapMenu(WebDriver driver, WebElement componentElement)
    {
        this(new WebDriverWrapperImpl(driver), componentElement);
    }

    public BootstrapMenu(WebDriverWrapper driver, WebElement componentElement)
    {
        _componentElement = componentElement;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    public boolean isExpanded()
    {
        String expandedAttribute = elements().toggleAnchor.getAttribute("aria-expanded");
        return expandedAttribute != null && expandedAttribute.equals("true");
    }

    @LogMethod(quiet = true)
    public WebElement clickMenuButton(boolean wait, boolean onlyOpen, @LoggedParam String ... subMenuLabels)
    {
        if (!isExpanded())
        getComponentElement().click();
        _driver.waitFor(()-> isExpanded(), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        if (onlyOpen && subMenuLabels.length == 0)
            return null;

        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            WebElement subMenuItem = Locators.bootstrapMenuItem(subMenuLabels[i])
                    .waitForElement(elements().findMenuList(), 2000);
            _driver.clickAndWait(subMenuItem, 0);
        }
        WebElement item = Locators.bootstrapMenuItem(subMenuLabels[subMenuLabels.length - 1])
                .notHidden()
                .waitForElement(elements().findMenuList(), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        if (onlyOpen)
        {
            _driver.mouseOver(item);
            return item;
        }

        if (wait)
            _driver.clickAndWait(item);
        else
            _driver.clickAndWait(item, 0);
        return null;
    }

    protected Elements elements()
    {
        return new Elements();
    }

    protected class Elements extends ElementCache
    {
        public WebElement toggleAnchor = Locator.xpath("//*[@data-toggle='dropdown']")
                .findWhenNeeded(getComponentElement());

        public WebElement findMenuList()
        {
            WebElement insideContainerList = Locator.tagWithClassContaining("ul", "dropdown-menu")
                    .findElementOrNull(getComponentElement());
            if (insideContainerList!=null)
                return insideContainerList;

            // outside the container, require it to be block-display,
            // as is the case with dataRegion header menus.
            return Locator.tagWithClassContaining("ul", "dropdown-menu")
                    .notHidden()
                    .withAttributeContaining("style", "display: block")
                    .findElement(_driver.getDriver());
        }
    }

    static public class Locators
    {
        public static Locator.XPathLocator bootstrapMenuItem(String text)
        {
            return Locator.xpath("//li/a[normalize-space()='"+text+"']").notHidden();
        }
    }
}
