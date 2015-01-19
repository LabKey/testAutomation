package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

public abstract class WebPart
{
    protected BaseWebDriverTest _test;
    protected WebElement _componentElement;
    protected String _title;
    protected String _id;

    protected abstract void waitForReady();

    private void clearCachedTitle()
    {
        _title = null;
    }

    public String getCurrentTitle()
    {
        clearCachedTitle();
        return getTitle();
    }

    public abstract String getTitle();

    protected Elements elements()
    {
        return new Elements();
    }

    public abstract void delete();

    public abstract void moveUp();

    public abstract void moveDown();

    public void goToPermissions()
    {
        clickMenuItem("Permissions");
    }

    public void clickMenuItem(String... items)
    {
        clickMenuItem(true, items);
    }

    public void clickMenuItem(boolean wait, String... items)
    {
        _test._extHelper.clickExtMenuButton(wait, Locator.xpath("//img[@id='more-" + _title.toLowerCase() + "']"), items);
    }

    protected class Elements
    {
        public Locator.XPathLocator webPart = Locator.tagWithId("table", _id);
        public Locator.XPathLocator webPartTitle = webPart.append(Locator.xpath("/tbody/tr/th"));
    }
}
