package org.labkey.test;

public abstract class BootstrapLocators
{

    // '@labkey/components/base/LoadingSpinner.tsx'
    public static final Locator.XPathLocator loadingSpinner = Locator.tag("span").withChild(Locator.tagWithClass("i", "fa-spinner"));

    // '@labkey/components/base/ContentGroup.tsx'
    public static Locator.XPathLocator contentGroup(String label)
    {
        return Locator.byClass("content-group").withChild(Locator.byClass("content-group-label").withText(label));
    }

    public static Locator.XPathLocator button()
    {
        return Locator.tagWithClass("a", "btn");
    }

    public static Locator.XPathLocator button(String text)
    {
        return button().withText(text);
    }

    // '@labkey/components/base/Alert.tsx'
    public static final Locator infoBanner = Locator.tagWithClass("div", BannerType.INFO.getCss());
    public static final Locator successBanner = Locator.tagWithClass("div", BannerType.SUCCESS.getCss());
    public static final Locator errorBanner = Locator.tagWithClass("div", BannerType.ERROR.getCss());
    public static final Locator warningBanner = Locator.tagWithClass("div", BannerType.WARNING.getCss());

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
