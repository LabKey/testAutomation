package org.labkey.test;

public abstract class AppLocators
{
    private AppLocators() {}

    public static final Locator.XPathLocator detailHeaderName = Locator.tagWithClass("h1", "detail__header--name");
}
