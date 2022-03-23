/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;

/**
 * Base class for Portal WebParts (can be moved, renamed, and removed)
 */
public abstract class WebPart<EC extends WebPart.ElementCache> extends WebPartPanel<EC> implements WebDriverWrapper.PageLoadListener
{
    private final WebDriverWrapper _wDriver;

    public WebPart(WebDriver driver, WebElement componentElement)
    {
        this(new WebDriverWrapperImpl(driver), componentElement);
    }

    public WebPart(WebDriverWrapper driverWrapper, WebElement componentElement)
    {
        super(new RefindingWebElement(componentElement, driverWrapper.getDriver()), driverWrapper.getDriver());
        _wDriver = driverWrapper;
        ((RefindingWebElement) getComponentElement()).withRefindListener(e -> clearCache());

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
    public WebDriverWrapper getWrapper()
    {
        return _wDriver;
    }

    private void waitForStale()
    {
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfAllElements(Arrays.asList(getComponentElement())));
        clearCache();
    }

    public void remove()
    {
        new SiteNavBar(getDriver()).doInAdminMode(() ->
        {
            clickMenuItem(false, "Remove From Page");
            waitForStale();
        });
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

        WebDriverWrapper.waitFor(() -> getWebPartIndex() == expectedIndex,
                "Move WebPart failed", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public int getWebPartIndex()
    {
        // Each webpart has an adjacent <br>
        return Locator.xpath(webPartLoc().getLoc().replaceFirst("//", "preceding-sibling::")).findElements(getComponentElement()).size();
    }

    public void goToPermissions()
    {
        clickMenuItem("Permissions");
    }

    public BootstrapMenu getWebParMenu()
    {
        return getTitleMenu();
    }

    public void clickMenuItem(String... items)
    {
        clickMenuItem(true, items);
    }

    public void clickMenuItem(boolean wait, String... items)
    {
        getTitleMenu().clickSubMenu(wait, items);
    }

    public BootstrapMenu getTitleMenu()
    {
        return elementCache().webPartMenu;
    }

    public void openTitleMenu()
    {
        getTitleMenu().expand();
    }

    public void closeTitleMenu()
    {
        getTitleMenu().collapse();
    }

    @Override
    protected abstract EC newElementCache();

    public class ElementCache extends WebPartPanel.ElementCache
    {
        protected final BootstrapMenu webPartMenu = BootstrapMenu.finder(getDriver()).findWhenNeeded(this);
    }
}
