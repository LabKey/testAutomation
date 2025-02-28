package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;

public class TemplateDownloadButton extends MultiMenu
{
    private static final String DEFAULT_TEMPLATE_NAME = "Default Template";
    private static final String BUTTON_CLASS = "template-download-button";
    public static final Locator LOCATOR = Locator.tagWithClass("button", BUTTON_CLASS);

    public TemplateDownloadButton(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    private File download(String templateName)
    {
        return getWrapper().doAndWaitForDownload(() -> {
            clickAndWaitForLoaded();

            boolean hasTemplates = hasCustomTemplates();
            boolean isDefaultDownload = DEFAULT_TEMPLATE_NAME.equals(templateName);

            if (hasTemplates)
            {
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

    // This will error if there are no custom templates.
    public MultiMenu expandDropdown()
    {
        if (!isDropdown())
            throw new Error("Custom Templates are not available.");

        return this;
    }

    public boolean isDropdown()
    {
        if (!isLoaded())
            clickAndWaitForLoaded();
        return hasCustomTemplates();
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
        getComponentElement().click();
        if (!wasLoaded)
            waitFor(this::isLoaded, "Template download button failed to load in time", WAIT_FOR_JAVASCRIPT);
    }

    private boolean hasButtonClass(String cls)
    {
        return Locator.tagWithClass("button", cls).existsIn(getComponentElement());
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
