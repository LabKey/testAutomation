package org.labkey.test.components.ui;

import org.labkey.test.BootstrapLocators.BannerType;
import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.List;

public class NotificationBanner extends WebDriverComponent<WebDriverComponent<?>.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected NotificationBanner(WebElement element, WebDriver driver)
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

    public void dismiss()
    {
        Locator.byClass("fa-times-circle").findElement(getDriver()).click();
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOf(getComponentElement()));
    }

    public BannerType getType()
    {
        List<String> css = Arrays.asList(getComponentElement().getAttribute("class").trim().split("\\s+"));
        for (BannerType type : BannerType.values())
        {
            if (css.contains(type.getCss()))
            {
                return type;
            }
        }
        return null;
    }

    public static class NotificationBannerFinder extends WebDriverComponentFinder<NotificationBanner, NotificationBannerFinder>
    {
        private final Locator.XPathLocator _notificationLocator = Locator.tagWithClass("div", "notification-container");
        private Locator _locator = _notificationLocator;

        public NotificationBannerFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected NotificationBanner construct(WebElement el, WebDriver driver)
        {
            return new NotificationBanner(el, driver);
        }

        public NotificationBannerFinder success()
        {
            _locator = _notificationLocator.withClass(BannerType.SUCCESS.getCss());
            return this;
        }

        public NotificationBannerFinder info()
        {
            _locator = _notificationLocator.withClass(BannerType.INFO.getCss());
            return this;
        }

        public NotificationBannerFinder warning()
        {
            _locator = _notificationLocator.withClass(BannerType.WARNING.getCss());
            return this;
        }

        public NotificationBannerFinder error()
        {
            _locator = _notificationLocator.withClass(BannerType.ERROR.getCss());
            return this;
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
