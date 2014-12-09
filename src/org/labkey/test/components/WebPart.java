package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.util.List;

public class WebPart
{
    protected BaseWebDriverTest _test;
    protected String _title;
    protected String _id;

    public WebPart(BaseWebDriverTest test, String title, int index)
    {
        _test = test;
        _title = title;
        List<WebElement> webparts = PortalHelper.Locators.webPart(title).findElements(test.getDriver());
        _id = webparts.get(index).getAttribute("id");
        waitForReady();
    }

    public WebPart(BaseWebDriverTest test, String title)
    {
        this(test, title, 0);
    }

    public WebPart(BaseWebDriverTest test, int index)
    {
        _test = test;
        List<WebElement> webparts = PortalHelper.Locators.webPart.findElements(test.getDriver());
        _id = webparts.get(index).getAttribute("id");
        getTitle();
        waitForReady();
    }

    protected void waitForReady() {}

    private void clearCachedTitle()
    {
        _title = null;
    }

    public String getCurrentTitle()
    {
        clearCachedTitle();
        return getTitle();
    }

    public String getTitle()
    {
        if (_title == null)
            _title = elements().webPartTitle.findElement(_test.getDriver()).getAttribute("title");
        return _title;
    }

    protected Elements elements()
    {
        return new Elements();
    }

    public void delete()
    {
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.removeWebPart(getTitle());
    }

    public void moveUp()
    {
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.moveWebPart(getTitle(), PortalHelper.Direction.UP);
    }

    public void moveDown()
    {
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.moveWebPart(getTitle(), PortalHelper.Direction.DOWN);
    }

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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebPart webPart = (WebPart) o;

        if (!_id.equals(webPart._id)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        return _id.hashCode();
    }

    protected class Elements
    {
        public Locator.XPathLocator webPart = Locator.tagWithId("table", _id);
        public Locator.XPathLocator webPartTitle = webPart.append(Locator.xpath("tbody/tr/th"));
    }
}
