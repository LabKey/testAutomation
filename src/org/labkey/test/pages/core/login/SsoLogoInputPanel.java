/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class SsoLogoInputPanel extends WebDriverComponent<SsoLogoInputPanel.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public SsoLogoInputPanel(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
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


    public SsoLogoInputPanel setLogo(File logoFile)
    {
        elementCache().logoFileInput.sendKeys(logoFile.getAbsolutePath());
        WebDriverWrapper.waitFor(()-> elementCache().attachedFile(logoFile)
                        .existsIn(this),
                "expected file did not become attached", 2000);
        return this;
    }

    public SsoLogoInputPanel clearLogo()
    {
        elementCache().logoImageRemoveBtn.click();
        WebDriverWrapper.waitFor(()-> elementCache().fileDropLoc.existsIn(this),
                "did not clear logo icon", 2000);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        WebElement logoFileInputContainer = Locator.tagWithClass("div", "file-upload__container")
                .refindWhenNeeded(this).withTimeout(4000);
        WebElement logoFileInput = Locator.tagWithClass("input", "file-upload__input")
                .refindWhenNeeded(this).withTimeout(4000);
        WebElement logoImageContainer = Locator.byClass("sso-fields__image-holder")
                .refindWhenNeeded(this).withTimeout(4000);
        WebElement logoImageRemoveBtn = Locator.byClass("sso-fields__delete-img")
                .refindWhenNeeded(this).withTimeout(4000);

        Locator attachedFile(File file)
        {
            return Locator.tagWithClass("div", "attached-file__container").containing(file.getName());
        }
        Locator fileDropLoc = Locator.tagWithClass("div", "sso-fields__file-attachment");
    }

    public static class SsoLogoInputPanelFinder extends WebDriverComponentFinder<SsoLogoInputPanel, SsoLogoInputPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "sso-logo-pane-container")
                .withChild(Locator.tagWithClass("div", "sso-fields-label"));
        private String _label = null;
        private String _inputId = null;

        public SsoLogoInputPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public SsoLogoInputPanelFinder withLabel(String label)
        {
            _label = label;
            return this;
        }

        public SsoLogoInputPanelFinder withInputId(String id)
        {
            _inputId = id;
            return this;
        }

        @Override
        protected SsoLogoInputPanel construct(WebElement el, WebDriver driver)
        {
            return new SsoLogoInputPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_label != null)
                return _baseLocator.withChild(Locator.tagWithClass("div", "auth-config-input-row__caption").withText(_label));
            if (_inputId != null)
                return _baseLocator.withDescendant(Locator.id(_inputId));
            else
                return _baseLocator;
        }
    }
}
