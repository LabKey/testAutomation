package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Wraps basic open/close functionality of 'Dropdown' and 'DropdownButton' components from '@types/react-bootstrap'
 */
public abstract class BaseBootstrapMenu extends WebDriverComponent<BaseBootstrapMenu.ElementCache>
{
    private final WebElement _componentElement;
    private final WebDriver _driver;
    private int _expandRetryCount = 1;

    /* componentElement should contain the toggle anchor *and* the UL containing list items */
    public BaseBootstrapMenu(WebElement componentElement, WebDriver driver)
    {
        _componentElement = componentElement;
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

    /* Sometimes the menu doesn't expand on the first try.
     * Sets the number of attempts it will make to expand the menu before failing/giving up. */
    protected BaseBootstrapMenu withExpandRetries(int retries)
    {
        _expandRetryCount = retries;
        return this;
    }

    protected boolean isExpanded()
    {
        return "true".equals(elementCache().toggleAnchor.getAttribute("aria-expanded"));
    }

    public void expand()
    {
        if (!isExpanded())
        {
            getWrapper().scrollIntoView(elementCache().toggleAnchor);
            new WebDriverWait(getDriver(), Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(elementCache().toggleAnchor));
            for (int retry = 0; retry < _expandRetryCount; retry++)
            {
                elementCache().toggleAnchor.click();
                if (WebDriverWrapper.waitFor(this::isExpanded, 1000))
                {
                    if (retry > 0)
                    {
                        TestLogger.log("Menu expanded after attempt #" + (retry + 1));
                    }
                    return;
                }
            }
            WebDriverWrapper.waitFor(this::isExpanded, "Menu did not expand as expected", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
    }

    public void collapse()
    {
        if (isExpanded())
        {
            new WebDriverWait(getDriver(), Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(elementCache().toggleAnchor));
            elementCache().toggleAnchor.click();
        }
        WebDriverWrapper.waitFor(()-> !isExpanded(), "Menu did not collapse as expected", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    protected abstract Locator getToggleLocator();

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public final WebElement toggleAnchor = getToggleLocator().findWhenNeeded(getComponentElement());

        public WebElement findOpenMenu()
        {
            WebElement insideContainerList = Locator.tagWithClassContaining("ul", "dropdown-menu")
                    .findElementOrNull(getComponentElement());
            if (insideContainerList != null)
                return insideContainerList;

            // outside the container, require it to be block-display,
            // as is the case with dataRegion header menus.
            return Locator.tagWithClassContaining("ul", "dropdown-menu")
                    .notHidden()
                    .withAttributeContaining("style", "display: block")
                    .findElement(getDriver());
        }
    }
}
