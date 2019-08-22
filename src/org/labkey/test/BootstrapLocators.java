package org.labkey.test;

public abstract class BootstrapLocators
{
    static public Locator infoAlert = Locator.tagWithClass("div", "alert-info");
    static public Locator successAlert = Locator.tagWithClass("div", "alert-success");
    static public Locator dangerAlert = Locator.tagWithClass("div", "alert-danger");
    static public Locator warningAlert = Locator.tagWithClass("div", "alert-warning");
}
