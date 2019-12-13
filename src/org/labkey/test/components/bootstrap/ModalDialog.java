/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.components.bootstrap;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class ModalDialog extends WebDriverComponent<ModalDialog.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    protected ModalDialog(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
        waitForReady();
    }

    public ModalDialog(ModalDialogFinder finder)
    {
        this(finder.waitFor().getComponentElement(), finder.getDriver());
    }

    /**
     * @deprecated Inline me. Use {@link ModalDialog.ModalDialogFinder}
     */
    @Deprecated
    static public ModalDialogFinder finder(WebDriver driver)
    {
        return new ModalDialogFinder(driver);
    }

    public void waitForReady()
    {
        elementCache().body.isDisplayed(); // Make sure timeout doesn't get used up by waiting for the dialog to appear
        WebDriverWrapper.waitFor(() -> elementCache().body.getText().length() > 0, "Modal dialog not ready", 2000);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public String getTitle()
    {
        return elementCache().title.getText();
    }

    public String getBodyText()
    {
        return elementCache().body.getText();
    }

    public void close()
    {
        elementCache().closeButton.click();
        waitForClose();
    }

    public void dismiss()
    {
        Locator.tagWithClass("button", "close").findElement(getComponentElement()).click();
        waitForClose();
    }

    public void dismiss(String buttonText)
    {
        dismiss(buttonText, 10);
    }

    /* for synchronous operations that take time, the caller can specify how long to wait */
    public void dismiss(String buttonText, Integer waitSeconds)
    {
        Locators.dismissButton(buttonText).findElement(getComponentElement()).click();
        waitForClose(waitSeconds);
    }

    protected void waitForClose()
    {
        waitForClose(10);
    }

    protected void waitForClose(Integer waitSeconds)
    {
        List<WebElement> elements = new ArrayList<>();
        elements.add(getComponentElement());
        WebElement modalBackdrop = Locator.byClass("modal-backdrop").findElementOrNull(getDriver());
        if (modalBackdrop != null)
            elements.add(modalBackdrop);
        new WebDriverWait(getDriver(), waitSeconds)
                .until(ExpectedConditions.invisibilityOfAllElements(elements));
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        WebElement title = Locators.title.findWhenNeeded(getComponentElement());
        WebElement closeButton = Locator.tagWithClass("button", "close")
                .withAttribute("data-dismiss", "modal")
                .findWhenNeeded(getComponentElement());
        WebElement body = Locators.body
                .findWhenNeeded(getComponentElement()).withTimeout(WAIT_FOR_JAVASCRIPT);
    }

    public static class Locators
    {
        static public Locator.XPathLocator dialog = Locator.tagWithClassContaining("div", "modal-dialog");
        static public Locator.XPathLocator title = Locator.tagWithClass("*", "modal-title");
        static public Locator.XPathLocator body = Locator.tagWithClass("div","modal-body");
        static public Locator.XPathLocator dismissButton(String text)
        {
            return Locator.button(text);
        }
    }

    public static class ModalDialogFinder extends WebDriverComponent.WebDriverComponentFinder<ModalDialog, ModalDialogFinder>
    {
        private Locator _locator;

        public ModalDialogFinder(WebDriver driver)
        {
            super(driver);
            _locator=Locators.dialog;
        }

        public ModalDialogFinder withTitle(String title)
        {
            _locator = Locators.dialog.withDescendant(Locator.tagWithClass("h4","modal-title")).containing(title);
            return this;
        }

        public ModalDialogFinder withBodyTextContaining(String text)
        {
            _locator = Locators.dialog.withDescendant(Locators.body).containing(text);
            return this;
        }

        @Override
        protected ModalDialog construct(WebElement el, WebDriver driver)
        {
            return new ModalDialog(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}