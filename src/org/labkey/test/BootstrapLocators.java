package org.labkey.test;

public abstract class BootstrapLocators
{
    static public Locator infoBanner = Locator.tagWithClass("div", "alert-info");
    static public Locator successBanner = Locator.tagWithClass("div", "alert-success");
    static public Locator dangerBanner = Locator.tagWithClass("div", "alert-danger");
    static public Locator warningBanner = Locator.tagWithClass("div", "alert-warning");
}
