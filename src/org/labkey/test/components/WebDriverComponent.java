package org.labkey.test.components;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class WebDriverComponent extends Component
{
    public static abstract class WebDriverComponentFinder<C extends WebDriverComponent, F extends WebDriverComponentFinder<C, F>> extends ComponentFinder<WebDriver, C, F>
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
