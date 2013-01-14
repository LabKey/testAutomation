package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

/**
 * User: tchadick
 * Date: 1/11/13
 * Time: 2:38 PM
 */
public class PortalHelper extends AbstractHelper
{
    public PortalHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    @LogMethod(quiet = true)
    private void clickTabMenuItem(@LoggedParam String tabText, boolean wait, @LoggedParam String... items)
    {
        Locator tabMenuXPath = Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']/following-sibling::span//a");
        _test.waitForElement(tabMenuXPath);
        _test._extHelper.clickExtMenuButton(wait, tabMenuXPath, items);
    }

    @LogMethod(quiet = true)
    public void moveTab(@LoggedParam String tabText, @LoggedParam Direction direction)
    {
        if (direction.isVertical())
            throw new IllegalArgumentException("Can't move folder tabs vertically.");

        String tabId = tabText + "Tab";
        int tabCount = _test.getXpathCount(Locator.xpath("//li[contains(@class, 'labkey-app-bar-tab')]"));
        int startIndex = _test.getElementIndex(Locator.xpath("//li[contains(@class, 'labkey-app-bar-tab')][./a[@id="+Locator.xq(tabId)+"]]"));
        clickTabMenuItem(tabText, false, "Move", direction.toString());
        int expectedEndIndex = startIndex;

        if (direction == Direction.LEFT && startIndex > 0 || direction == Direction.RIGHT && startIndex < (tabCount - 2))
        {
            switch (direction)
            {
                case LEFT:
                    expectedEndIndex = startIndex - 1;
                    break;
                case RIGHT:
                    expectedEndIndex = startIndex + 1;
                    break;
            }
        }

        _test.waitForElement(Locator.xpath("//li[contains(@class, 'labkey-app-bar-tab')]["+(expectedEndIndex+1)+"][./a[@id="+Locator.xq(tabId)+"]]"));
    }

    @LogMethod(quiet = true)
    public void removeTab(@LoggedParam String tabText)
    {
        clickTabMenuItem(tabText, true, "Remove");
        _test.assertElementNotPresent(Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']"));
    }

    @LogMethod(quiet = true)
    public void addTab(@LoggedParam String tabName)
    {
        addTab(tabName, null);
    }

    @LogMethod(quiet = true)
    public void addTab(@LoggedParam String tabName, @Nullable @LoggedParam String expectedError)
    {
        _test.clickAndWait(Locator.linkWithText("+"));
        _test.waitForText("Add Tab");
        _test.setFormElement(Locator.input("tabName"), tabName);
        _test.clickButton("save");
        if (expectedError != null)
            Assert.assertEquals("Did not find expected error", expectedError, _test.getText(Locator.id("errors")));
        else
            _test.waitForElement(Locator.folderTab(tabName));
    }

    @LogMethod(quiet = true)
    public void renameTab(@LoggedParam String tabText, @LoggedParam String newName)
    {
        renameTab(tabText, newName, null);
    }

    @LogMethod(quiet = true)
    public void renameTab(@LoggedParam String tabText, @LoggedParam String newName, @Nullable @LoggedParam String expectedError)
    {
        clickTabMenuItem(tabText, true, "Rename");
        _test.setFormElement(Locator.name("tabName"), newName);
        _test.clickButton("save");
        if (expectedError != null)
            Assert.assertEquals("Did not find expected error", expectedError, _test.getText(Locator.id("errors")));
        else
        {
            _test.waitForElement(Locator.folderTab(newName));
            _test.assertElementNotPresent(Locator.folderTab(tabText));
        }
    }

    /**
     * Allows test code to navigate to a Webpart Ext-based navigation menu.
     * @param webPartTitle title (not name) of webpart to be clicked.  Multiple web parts with the same title not supported.
     * @param items
     */
    public void clickWebpartMenuItem(String webPartTitle, String... items)
    {
        clickWebpartMenuItem(webPartTitle, true, items);
    }

    public void clickWebpartMenuItem(String webPartTitle, boolean wait, String... items)
    {
        _test._extHelper.clickExtMenuButton(wait, Locator.xpath("//img[@id='more-" + webPartTitle.toLowerCase() + "']"), items);
    }

    /**
     * Works with {@link BaseWebDriverTest} only
     */
    public void addWebPart(String webPartName)
    {
        Locator.css("option").withText(webPartName).waitForElmement(((BaseWebDriverTest)_test)._driver, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        Locator.XPathLocator form = Locator.xpath("//form[contains(@action,'addWebPart.view')][.//option[text()='"+webPartName+"']]");
        _test.selectOptionByText(form.append("//select"), webPartName);
        _test.submit(form);
    }

    public void removeWebPart(String webPartTitle)
    {
        Locator.XPathLocator removeButton = Locator.xpath("//tr[th[@title='"+webPartTitle+"']]//a[img[@title='Remove From Page']]");
        int startCount = _test.getXpathCount(removeButton);
        _test.click(removeButton);
        _test.waitForElementToDisappear(removeButton.index(startCount), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Works with {@link BaseWebDriverTest} only
     */
    public void moveWebPart(String webPartTitle, Direction direction)
    {
        if (direction.isHorizontal())
            throw new IllegalArgumentException("Can't move webpart horizontally.");

        Locator.XPathLocator webPart = Locator.xpath("//table[@name='webpart'][.//span[contains(@class, 'labkey-wp-title-text') and text()="+Locator.xq(webPartTitle)+"]]");

        int initialIndex = (_test.getElementIndex(webPart) / 2);

        Locator.XPathLocator portalPanel = Locator.xpath("//td[./table[@name='webpart']//span[contains(@class, 'labkey-wp-title-text') and text()="+Locator.xq(webPartTitle)+"]]");
        String panelClass = portalPanel.findElement(((BaseWebDriverTest)_test).getDriver()).getAttribute("class");
        if (panelClass.contains("labkey-body-panel"))
        {
            _test.click(webPart.append("//img[@title='Move "+direction+"']"));
        }
        else if (panelClass.contains("labkey-side-panel"))
        {
            clickWebpartMenuItem(webPartTitle, false, "Move " + direction.toString());
        }
        else
        {
            Assert.fail("Unable to analyze webpart type. PortalHelper.java needs updating.");
        }

        // TODO: Check final webpart index

        _test._ext4Helper.waitForMaskToDisappear();
    }

    public static enum Direction
    {
        UP("Up", Axis.VERTICAL),
        DOWN("Down", Axis.VERTICAL),
        LEFT("Left", Axis.HORIZONTAL),
        RIGHT("Right", Axis.HORIZONTAL);

        private String _dir;
        private Axis _axis;

        private Direction (String dir, Axis axis)
        {
            _dir = dir;
            _axis = axis;
        }

        public String toString()
        {
            return _dir;
        }

        public Boolean isHorizontal()
        {
            return _axis == Axis.HORIZONTAL;
        }

        public Boolean isVertical()
        {
            return _axis == Axis.VERTICAL;
        }

        public static enum Axis
        {
            HORIZONTAL,
            VERTICAL
        }
    }
}
