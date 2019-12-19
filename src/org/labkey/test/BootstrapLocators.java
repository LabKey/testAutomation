package org.labkey.test;

public abstract class BootstrapLocators
{
    public static final Locator infoAlert = Locator.tagWithClass("div", "alert-info");
    public static final Locator successAlert = Locator.tagWithClass("div", "alert-success");
    public static final Locator dangerAlert = Locator.tagWithClass("div", "alert-danger");
    public static final Locator warningAlert = Locator.tagWithClass("div", "alert-warning");
    
    public static Locator panel(String panelHeading)
    {
        return Locator.byClass("panel").withChild(Locator.byClass("panel-heading").withText(panelHeading));
    }
}
