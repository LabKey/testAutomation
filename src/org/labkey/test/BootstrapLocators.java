package org.labkey.test;

public abstract class BootstrapLocators
{
    static public Locator infoBanner = Locator.tagWithClass("div", BannerType.INFO.getCss());
    static public Locator successBanner = Locator.tagWithClass("div", BannerType.SUCCESS.getCss());
    static public Locator dangerBanner = Locator.tagWithClass("div", BannerType.ERROR.getCss());
    static public Locator warningBanner = Locator.tagWithClass("div", BannerType.WARNING.getCss());

    public enum BannerType
    {
        SUCCESS("success-alert"),
        INFO("info-alert"),
        WARNING("warning-alert"),
        ERROR("danger-alert");

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
}
