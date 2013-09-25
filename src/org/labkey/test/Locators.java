package org.labkey.test;

/**
 * User: tchadick
 * Date: 9/24/13
 */
public class Locators
{
    public static final Locator.XPathLocator ADMIN_MENU = Locator.xpath("id('adminMenuPopupLink')[@onclick]");
    public static final Locator.IdLocator USER_MENU = Locator.id("userMenuPopupLink");
}
