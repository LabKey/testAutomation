/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;

import static org.labkey.test.components.WebPart.Locators.webPart;

/**
 * Base class for Portal WebParts (can be moved, added, and removed)
 */
public abstract class WebPart<EC extends WebPart.Elements> extends Component<EC> implements WebDriverWrapper.PageLoadListener
{
    @Deprecated // Use #getWrapper()
    protected final WebDriverWrapper _test;
    private final WebDriverWrapper _wDriver;

    protected final WebElement _componentElement;
    protected String _title;

    public WebPart(WebDriver driver, WebElement componentElement)
    {
        this(new WebDriverWrapperImpl(driver), componentElement);
    }

    public WebPart(WebDriverWrapper driverWrapper, WebElement componentElement)
    {
        _componentElement = new RefindingWebElement(Locator.id(componentElement.getAttribute("id")), driverWrapper.getDriver())
                .withRefindListener(e -> clearCache());
        _test = driverWrapper;
        _wDriver = driverWrapper;

        driverWrapper.addPageLoadListener(this);
    }

    @Override
    public void afterPageLoad()
    {
        clearCache();
    }

    protected void clearCache()
    {
        super.clearElementCache();
        _title = null;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    public WebDriver getDriver()
    {
        return getWrapper().getDriver();
    }

    public WebDriverWrapper getWrapper()
    {
        return _wDriver;
    }

    protected abstract void waitForReady();

    private void waitForStale()
    {
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfAllElements(Arrays.asList(getComponentElement())));
        clearCache();
    }

    public String getTitle()
    {
        if (_title == null)
            _title = elementCache().webPartTitle.getAttribute("title");
        return _title;
    }

    public void delete()
    {
        clickMenuItem(false, "Remove From Page");
        waitForStale();
    }

    public void moveUp()
    {
        moveWebPart(false);
    }

    public void moveDown()
    {
        moveWebPart(true);
    }

    @LogMethod(quiet = true)public void moveWebPart(final boolean down)
    {
        final int initialIndex = getWebPartIndex();
        final int expectedIndex = initialIndex + (down ? 1 : -1);

        clickMenuItem(false, down ? "Move Down" : "Move Up");

        getWrapper().waitFor(() -> getWebPartIndex() == expectedIndex,
                "Move WebPart failed", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public int getWebPartIndex()
    {
        // Each webpart has an adjacent <br>
        return Locator.xpath(webPart.getLoc().replaceFirst("//", "preceding-sibling::")).findElements(getComponentElement()).size();
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
        getWrapper()._ext4Helper.clickExt4MenuButton(wait, elementCache().moreMenu, false, items);
    }

    @Deprecated // Use elementCache()
    protected Elements elements()
    {
        return elementCache();
    }

    protected EC newElementCache()
    {
        return (EC)new Elements();
    }

    public class Elements extends Component.ElementCache
    {
        public WebElement webPartTitle = new LazyWebElement(Locators.leftTitle, this);
        public WebElement moreMenu = new LazyWebElement(Locator.css("span[title=More]"), webPartTitle);
    }

    protected static class Locators
    {
        public static final Locator.XPathLocator leftTitle = Locator.tag("tbody/tr/th");
        public static final Locator.XPathLocator webPart = Locator.tag("table").withAttribute("name", "webpart");
    }
}
