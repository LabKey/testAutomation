package org.labkey.test.components.glassLibrary;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class CollapsableFormPanel extends WebDriverComponent<CollapsableFormPanel.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public CollapsableFormPanel(WebElement element, WebDriver driver)
    {
        _el = element;
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

    public CollapsableFormPanel expand()
    {
        if (!isExpanded())
            elementCache().expandCollapseToggle.click();
        WebDriverWrapper.waitFor(()-> isExpanded(),
                "the panel did not expand", 2000);
        return this;
    }

    public CollapsableFormPanel collapse()
    {
        if (isExpanded())
            elementCache().expandCollapseToggle.click();
        WebDriverWrapper.waitFor(()-> !isExpanded(),
                "the panel did not collapse", 2000);
        return this;
    }

    public boolean isExpanded()
    {
        return elementCache().waitForBody()
                .getAttribute("class").equals("panel-collapse collapse in");
    }

    public WebElement body()
    {
        return elementCache().waitForBody();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement header = Locator.tagWithClass("div", "collapsible-panel-header")
                .findWhenNeeded(this).withTimeout(2000);
        WebElement expandCollapseToggle = Locator.tagWithClass("span", "pull-right")
                .findWhenNeeded(header).withTimeout(2000);
        private Locator.XPathLocator body = Locator.tagWithClass("div", "panel-collapse");

        WebElement waitForBody()
        {
            return body.waitForElement(this, WAIT_FOR_JAVASCRIPT);
        }
    }


    public static class CollapsableFormPanelFinder extends WebDriverComponentFinder<CollapsableFormPanel, CollapsableFormPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "panel-default")
                .withChild(Locator.tagWithClass("div", "collapsible-panel-header"));
        private String _title = null;

        public CollapsableFormPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public CollapsableFormPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected CollapsableFormPanel construct(WebElement el, WebDriver driver)
        {
            return new CollapsableFormPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withChild(Locator.tagWithClass("div", "collapsible-panel-header")
                        .withDescendant(Locator.tagWithText("span", _title)));
            else
                return _baseLocator;
        }
    }
}
