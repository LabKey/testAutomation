/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.components.ext4;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class Window<EC extends Window.ElementCache> extends WebDriverComponent<EC>
{
    private WebElement _window;
    private final WebDriver _driver;

    public Window(String windowTitle, WebDriver driver)
    {
        this(Window(driver).withTitle(windowTitle));
    }

    protected Window(WindowFinder finder)
    {
        this(finder.waitFor().getComponentElement(), finder.getDriver());
    }

    public Window(WebElement window, WebDriver driver)
    {
        _window = window;
        _driver = driver;
    }

    public static WindowFinder Window(WebDriver driver)
    {
        return new WindowFinder(driver);
    }

    /**
     * @deprecated Use {@link Window#Window(WebDriver)}
     */
    @Deprecated
    public static WindowFinder Window()
    {
        return new WindowFinder();
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public final WebElement getComponentElement()
    {
        return _window;
    }

    public void clickButton(String buttonText)
    {
        getWrapper().clickAndWait(elementCache().findButton(buttonText));
    }

    public void clickButton(String buttonText, int msWait)
    {
        getWrapper().clickAndWait(elementCache().findButton(buttonText), msWait);
    }

    public void clickButton(String buttonText, boolean expectClose)
    {
        clickButton(buttonText, 0);
        if (expectClose)
            waitForClose();
    }

    public String getTitle()
    {
        return elementCache().title.getText();
    }

    public String getBody()
    {
        return elementCache().body.getText();
    }

    public boolean isClosed()
    {
        try
        {
            return !getComponentElement().isDisplayed();
        }
        catch (StaleElementReferenceException gone)
        {
            return true;
        }
    }

    public void close()
    {
        elementCache().closeButton.click();
        waitForClose();
    }

    public void waitForClose()
    {
        waitForClose(5000);
    }

    public void waitForClose(int msWait)
    {
        getWrapper().waitFor(this::isClosed, "Window did not close", msWait);

        // Ext4 can reuse the Window element. Hide that from the tests to avoid surprising behavior.
        _window = null;
        clearElementCache();
    }

    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    public class ElementCache extends WebDriverComponent.ElementCache
    {
        protected ElementCache() { }

        protected WebElement title = new LazyWebElement(Locator.css(".x4-window-header-text"), this);
        protected WebElement body = new LazyWebElement(Locator.css(".x4-window-body"), this);
        WebElement closeButton = new LazyWebElement(Locator.css(".x4-window-header .x4-tool-close"), this);
        WebElement findButton(String buttonText)
        {
            return Ext4Helper.Locators.ext4Button(buttonText).findElement(this);
        }
    }

    /**
     * @deprecated Renamed to {@link ElementCache} for consistency
     */
    @Deprecated
    public class Elements extends ElementCache
    {
    }

    public static class WindowFinder extends WebDriverComponentFinder<Window, WindowFinder>
    {
        public WindowFinder(WebDriver driver)
        {
            super(driver);
        }

        /**
         * Bridge transition from redundant FloatingComponent
         */
        private WebDriver _driver;
        @Deprecated
        public WindowFinder()
        {
            this(null);
        }
        @Deprecated
        public Window find(WebDriver driver)
        {
            _driver = driver;
            return super.find();
        }
        @Deprecated
        public Window waitFor(WebDriver driver)
        {
            _driver = driver;
            return super.waitFor();
        }
        @Deprecated
        public Window findWhenNeeded(WebDriver driver)
        {
            _driver = driver;
            return super.findWhenNeeded();
        }
        @Override
        protected WebDriver getDriver()
        {
            if (_driver != null)
                return _driver;
            return super.getDriver();
        }

        private String titleText = "";
        private boolean partialText = true;

        public WindowFinder withTitle(@NotNull String text)
        {
            this.titleText = text;
            partialText = false;
            return this;
        }

        public WindowFinder withTitleContaining(@NotNull String text)
        {
            this.titleText = text;
            partialText = true;
            return this;
        }

        @Override
        protected Window construct(WebElement el, WebDriver driver)
        {
            return new Window(el, driver);
        }

        @Override
        protected Locator locator()
        {
            Locator.XPathLocator loc = windowLoc;
            if (!partialText)
                loc = loc.withPredicate(titleLoc.withText(titleText));
            else if (!titleText.isEmpty())
                loc = loc.withPredicate(titleLoc.containing(titleText));
            return loc;
        }

        static final Locator.XPathLocator windowLoc = Locator.tagWithClass("div", Ext4Helper.getCssPrefix() + "window").withoutClass(Ext4Helper.getCssPrefix() + "window-ghost").notHidden();
        static final Locator.XPathLocator titleLoc = Locator.tagWithClass("span", Ext4Helper.getCssPrefix() + "window-header-text");
    }

    /**
     * @deprecated Use {@link Window}
     */
    @Deprecated
    public static class Locators
    {
        public static Locator.XPathLocator window()
        {
            return WindowFinder.windowLoc;
        }
        public static Locator.XPathLocator title()
        {
            return WindowFinder.titleLoc;
        }
    }
}
