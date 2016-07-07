package org.labkey.test.components;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wrapper for components that should be found at global scope (e.g. an Ext dialog)
 */
public abstract class FloatingComponent<EC extends Component.ElementCache> extends Component<EC>
{
    public static abstract class WebDriverComponentFinder<C extends FloatingComponent, F extends WebDriverComponentFinder<C, F>> extends ComponentFinder<WebDriver, C, F>
    {
        WebDriver driver;

        @Override
        public C find(WebDriver context)
        {
            driver = context;
            return super.find(context);
        }

        @Override
        public C waitFor(WebDriver context)
        {
            driver = context;
            return super.waitFor(context);
        }

        @Override
        public C findWhenNeeded(WebDriver context)
        {
            driver = context;
            return super.findWhenNeeded(context);
        }

        @Override
        protected C construct(WebElement el)
        {
            return construct(el, driver);
        }

        protected abstract C construct(WebElement el, WebDriver driver);
    }
}
