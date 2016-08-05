/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.Component;
import org.labkey.test.components.FloatingComponent;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class Window extends FloatingComponent<Window.Elements>
{
    WebElement _window;
    WebDriverWrapper _driver;

    public Window(String windowTitle, WebDriver driver)
    {
        this(Window().withTitle(windowTitle), driver);
    }

    protected Window(WindowFinder finder, WebDriver driver)
    {
        this(finder.waitFor(driver).getComponentElement(), driver);
    }

    public Window(WebElement window, WebDriver driver)
    {
        _window = window;
        _driver = new WebDriverWrapperImpl(driver);
    }

    public static WindowFinder Window()
    {
        return new WindowFinder();
    }

    protected WebDriverWrapper getWrapper()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _window;
    }

    public void clickButton(String buttonText)
    {
        getWrapper().clickAndWait(elements().findButton(buttonText));
    }

    public void clickButton(String buttonText, int msWait)
    {
        getWrapper().clickAndWait(elements().findButton(buttonText), msWait);
    }

    public void clickButton(String buttonText, boolean expectClose)
    {
        clickButton(buttonText, 0);
        if (expectClose)
            waitForClose();
    }

    public String getTitle()
    {
        return elements().title.getText();
    }

    public String getBody()
    {
        return elements().body.getText();
    }

    public void close()
    {
        elements().closeButton.click();
        waitForClose();
    }

    public void waitForClose()
    {
        waitForClose(5000);
    }

    public void waitForClose(int msWait)
    {
        getWrapper().waitFor(() -> {
            try
            {
                return !_window.isDisplayed();
            }
            catch (StaleElementReferenceException gone)
            {
                return true;
            }
        }, "Window did not close", msWait);
    }

    protected Elements elements()
    {
        return super.elementCache();
    }

    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends Component.ElementCache
    {
        WebElement title = new LazyWebElement(Locator.css(".x4-window-header-text"), this);
        WebElement body = new LazyWebElement(Locator.css(".x4-window-body"), this);
        WebElement closeButton = new LazyWebElement(Locator.css(".x4-window-header .x4-tool-close"), this);
        WebElement findButton(String buttonText)
        {
            return Ext4Helper.Locators.ext4Button(buttonText).findElement(this);
        }
    }

    public static class WindowFinder extends WebDriverComponentFinder<Window, WindowFinder>
    {
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
