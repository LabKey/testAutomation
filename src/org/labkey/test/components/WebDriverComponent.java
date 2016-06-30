package org.labkey.test.components;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wrapper for components that need a WebDriver for full functionality (e.g. page navigation or JavaScript execution)
 */
public abstract class WebDriverComponent extends Component
{
    protected abstract WebDriver getDriver();

    public static abstract class WebDriverComponentFinder<C, F extends WebDriverComponentFinder<C, F>> extends ComponentFinder<SearchContext, C, F>
    {
        WebDriver driver;
        public WebDriverComponentFinder(WebDriver driver)
        {
            this.driver = driver;
        }

        public C findWhenNeeded()
        {
            return super.findWhenNeeded(driver);
        }

        public C find()
        {
            return super.find(driver);
        }

        public C waitFor()
        {
            return super.waitFor(driver);
        }

        @Override
        protected final C construct(WebElement el)
        {
            return construct(el, driver);
        }

        protected abstract C construct(WebElement el, WebDriver driver);
    }
}
