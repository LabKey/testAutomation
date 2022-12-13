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
        WebDriverWrapper.waitFor(()-> elementCache().nullImageLoc.existsIn(this),
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
        WebElement logoFileInputContainer = Locator.tagWithClass("div", "file-upload--container")
                .refindWhenNeeded(this).withTimeout(4000);
        WebElement logoFileInput = Locator.tagWithClass("input", "file-upload--input")
                .refindWhenNeeded(this).withTimeout(4000);
        WebElement logoImageContainer = Locator.byClass("sso-fields__image-holder")
                .refindWhenNeeded(this).withTimeout(4000);
        WebElement logoImageRemoveBtn = Locator.byClass("sso-fields__delete-img")
                .refindWhenNeeded(this).withTimeout(4000);

        Locator attachedFile(File file)
        {
            return Locator.tagWithClass("div", "attached-file--container").containing(file.getName());
        }
        Locator nullImageLoc = Locator.tagWithClass("div", "sso-fields__null-image");
    }

    public static class SsoLogoInputPanelFinder extends WebDriverComponentFinder<SsoLogoInputPanel, SsoLogoInputPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "sso-logo-pane-container")
                .withChild(Locator.tagWithClass("div", "sso-fields__label"));
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
                return _baseLocator.withChild(Locator.tagWithClass("div", "sso-fields__label").withText(_label));
            if (_inputId != null)
                return _baseLocator.withDescendant(Locator.id(_inputId));
            else
                return _baseLocator;
        }
    }
}
