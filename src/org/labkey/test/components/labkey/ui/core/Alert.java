package org.labkey.test.components.labkey.ui.core;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
/**
 * Component for bootstrap alert defined in 'labkey-ui-components/packages/components/src/components/base/Alert.tsx'
 */
public class Alert extends WebDriverComponent<Component<?>.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected Alert(WebElement element, WebDriver driver)
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

    public String getMessage()
    {
        return getComponentElement().getText();
    }

    public BootstrapLocators.BannerType getType()
    {
        String classAttribute = getComponentElement().getAttribute("class");
        List<String> cssClasses = Arrays.asList(classAttribute.trim().split("\\s+"));
        for (BootstrapLocators.BannerType type : BootstrapLocators.BannerType.values())
        {
            if (cssClasses.contains(type.getCss()))
            {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown alert type: 'class=\"" + classAttribute + "\"'");
    }

    public static class AlertFinder extends WebDriverComponentFinder<Alert, AlertFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "alert")
            .withAttribute("role", "alert");
        private Locator.XPathLocator _locator = _baseLocator.withAttributeContaining("class", "alert-");

        public AlertFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected Alert construct(WebElement el, WebDriver driver)
        {
            return new Alert(el, driver);
        }

        public AlertFinder success()
        {
            _locator = _baseLocator.withClass(BootstrapLocators.BannerType.SUCCESS.getCss());
            return this;
        }

        public AlertFinder info()
        {
            _locator = _baseLocator.withClass(BootstrapLocators.BannerType.INFO.getCss());
            return this;
        }

        public AlertFinder warning()
        {
            _locator = _baseLocator.withClass(BootstrapLocators.BannerType.WARNING.getCss());
            return this;
        }

        public AlertFinder error()
        {
            _locator = _baseLocator.withClass(BootstrapLocators.BannerType.ERROR.getCss());
            return this;
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
