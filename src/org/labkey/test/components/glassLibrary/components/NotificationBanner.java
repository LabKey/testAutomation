package org.labkey.test.components.glassLibrary.components;

import org.labkey.test.BootstrapLocators.BannerType;
import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.List;

public class NotificationBanner<P extends LabKeyPage<?>> extends WebDriverComponent<WebDriverComponent<?>.ElementCache>
{
    private final WebElement _el;
    private final P _page;

    protected NotificationBanner(WebElement element, P page)
    {
        _el = element;
        _page = page;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _page.getDriver();
    }

    public String getMessage()
    {
        return getComponentElement().getText();
    }

    public P dismiss()
    {
        Locator.byClass("fa-times-circle").findElement(getDriver()).click();
        _page.shortWait().until(ExpectedConditions.invisibilityOf(getComponentElement()));
        return _page;
    }

    public BannerType getType()
    {
        List<String> css = Arrays.asList(getComponentElement().getAttribute("class").trim().split("\\w+"));
        for (BannerType type : BannerType.values())
        {
            if (css.contains(type.getCss()))
            {
                return type;
            }
        }
        return null;
    }

    public static class NotificationBannerFinder<P extends LabKeyPage<?>> extends WebDriverComponentFinder<NotificationBanner<P>, NotificationBannerFinder<P>>
    {
        private final P _page;
        private final Locator.XPathLocator _notificationLocator = Locator.tagWithClass("div", "notification-container");
        private Locator _locator = _notificationLocator;

        public NotificationBannerFinder(P page)
        {
            super(page.getDriver());
            _page = page;
        }

        @Override
        protected NotificationBanner<P> construct(WebElement el, WebDriver driver)
        {
            return new NotificationBanner<>(el, _page);
        }

        public NotificationBannerFinder<P> success()
        {
            _locator = _notificationLocator.withClass(BannerType.SUCCESS.getCss());
            return this;
        }

        public NotificationBannerFinder<P> info()
        {
            _locator = _notificationLocator.withClass(BannerType.INFO.getCss());
            return this;
        }

        public NotificationBannerFinder<P> warning()
        {
            _locator = _notificationLocator.withClass(BannerType.WARNING.getCss());
            return this;
        }

        public NotificationBannerFinder<P> error()
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
