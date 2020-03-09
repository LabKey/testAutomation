package org.labkey.test;

public abstract class BootstrapLocators
{
    public static final Locator.XPathLocator infoBanner = Locator.tagWithClass("div", BannerType.INFO.getCss());
    public static final Locator.XPathLocator successBanner = Locator.tagWithClass("div", BannerType.SUCCESS.getCss());
    public static final Locator.XPathLocator errorBanner = Locator.tagWithClass("div", BannerType.ERROR.getCss());
    public static final Locator.XPathLocator warningBanner = Locator.tagWithClass("div", BannerType.WARNING.getCss());

    public enum BannerType
    {
        SUCCESS("alert-success"),
        INFO("alert-info"),
        WARNING("alert-warning"),
        ERROR("alert-danger");

        private final String _css;

        BannerType(String css)
        {
            _css = css;
        }

        public String getCss()
        {
            return _css;
        }
    }

    public static Locator panel(String panelHeading)
    {
        return Locator.byClass("panel").withChild(Locator.byClass("panel-heading").withText(panelHeading));
    }
}
