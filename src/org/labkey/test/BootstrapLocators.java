package org.labkey.test;

public abstract class BootstrapLocators
{
    static public Locator infoAlertLoc = Locator.tagWithClass("div", "alert-info");
    static public Locator successAlertLoc = Locator.tagWithClass("div", "alert-success");
    static public Locator dangerAlertLoc = Locator.tagWithClass("div", "alert-danger");
    static public Locator warningAlertLoc = Locator.tagWithClass("div", "alert-warning");
}
