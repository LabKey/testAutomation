/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;

/**
 * This is a test component for the <TemplateDownloadButton/> React component found in the @labkey/components package.
 * This is not a typical dropdown menu (a.k.a. MultiMenu) in that it has a couple of unique behaviors:
 * 1. It always renders as a dropdown toggle menu button.
 * 2. When the button is clicked it will fetch/resolve custom templates for the related entity.
 * 3. If there are custom templates, then the button will display a dropdown menu from which custom templates can
 * be downloaded.
 * 4. If there are no custom templates, then the default template file will be downloaded immediately without a menu
 * being displayed.
 */
public class TemplateDownloadButton extends MultiMenu
{
    public static final String DEFAULT_TEMPLATE_NAME = "Default Template";
    private static final String BUTTON_CLASS = "template-download-button";
    public static final Locator LOCATOR = Locator.tagWithClass("button", BUTTON_CLASS);

    public TemplateDownloadButton(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    private File download(String templateName)
    {
        return getWrapper().doAndWaitForDownload(() -> {
            clickAndWaitForLoaded(); // Will trigger download if there are no custom templates

            boolean hasTemplates = hasCustomTemplates();
            boolean isDefaultDownload = DEFAULT_TEMPLATE_NAME.equals(templateName);

            if (hasTemplates)
            {
                // When custom templates are available the default template download will appear as a menu item
                if (isDefaultDownload)
                    doMenuAction(DEFAULT_TEMPLATE_NAME);
                else
                    doMenuAction(templateName);
            }
            else if (!isDefaultDownload)
                throw new Error("Custom Templates are not available.");
            // else the act of clicking the button will download the file
        });
    }

    @Override
    public void expand()
    {
        if (!isExpanded())
        {
            if (!isLoaded())
                clickAndWaitForLoaded();

            if (!isDropdown())
                return; // Not a dropdown, do not attempt to expand
        }

        super.expand();
    }

    public boolean isDropdown()
    {
        return isLoaded() && hasCustomTemplates();
    }

    public File downloadDefaultTemplate()
    {
        return download(DEFAULT_TEMPLATE_NAME);
    }

    public File downloadCustomTemplate(String templateName)
    {
        return download(templateName);
    }

    // This will induce a file download of the default template if there are no custom templates configured.
    private void clickAndWaitForLoaded()
    {
        boolean wasLoaded = isLoaded();
        elementCache().toggleAnchor.click();
        if (!wasLoaded)
            waitFor(this::isLoaded, "Template download button failed to load in time", WAIT_FOR_JAVASCRIPT);
    }

    private boolean hasButtonClass(String cls)
    {
        String cssClass = elementCache().toggleAnchor.getDomAttribute("class");
        return cssClass != null && cssClass.contains(cls);
    }

    private boolean hasCustomTemplates()
    {
        return isLoaded() && hasButtonClass("has-templates");
    }

    private boolean isLoaded()
    {
        return hasButtonClass("is-loaded");
    }

    public static class Finder extends WebDriverComponentFinder<TemplateDownloadButton, Finder>
    {
        private final Locator _locator;

        public Finder(WebDriver driver)
        {
            super(driver);
            _locator = new MultiMenuFinder(driver).withButtonClass(BUTTON_CLASS).locator();
        }

        @Override
        protected TemplateDownloadButton construct(WebElement el, WebDriver driver)
        {
            return new TemplateDownloadButton(el, driver);
        }

        @Override
        protected Finder getThis()
        {
            return this;
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
