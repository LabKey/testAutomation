package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;


public class CompactFileUploadField extends WebDriverComponent<CompactFileUploadField.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public CompactFileUploadField(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public CompactFileUploadField setFile(File file)
    {
        if (hasAttachedFile())
        {
            removeFile();
        }
        elementCache().logoFileInput.sendKeys(file.getAbsolutePath());
        WebDriverWrapper.waitFor(()-> hasAttachedFile() && getAttachedFileName().equals(file.getName()),
                "the file did not become attached", 2000);
        return this;
    }

    public CompactFileUploadField removeFile()
    {
        elementCache().removeBtn.click();
        WebDriverWrapper.waitFor(()-> !hasAttachedFile(),
                "the file was not removed", 2000);
        return this;
    }

    public boolean hasAttachedFile()
    {
        return Locator.tagWithClass("div", "attached-file--container")
                .existsIn(this);
    }

    public String getAttachedFileName()
    {
        if (hasAttachedFile())
        {
            return Locator.tagWithClass("div", "attached-file--container")
                    .waitForElement(this, 2000).getText();
        }
        else
            return "";
    }

    public boolean hasFieldError()
    {
        return Locator.tagWithClassContaining("div", "modal__tiny-error")
                .existsIn(this);
    }

    public String getFieldErrorText()
    {
        if (hasFieldError())
        {
            return Locator.tagWithClassContaining("div", "modal__tiny-error")
                    .findElement(this).getText();
        }
        else
            return "";
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

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {

        WebElement logoFileInput = Locator.tagWithClass("input", "file-upload--input")
                .refindWhenNeeded(this).withTimeout(4000);

        WebElement removeBtn = Locator.tagWithClass("span", "file-upload__remove--icon")
                .refindWhenNeeded(this).withTimeout(4000);

        Locator attachedFile(File file)
        {
            return Locator.tagWithClass("div", "attached-file--container").containing(file.getName());
        }
    }


    public static class CompactFileUploadFieldFinder extends WebDriverComponentFinder<CompactFileUploadField, CompactFileUploadFieldFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "file-upload-field");
        private String _label = null;
        private String _inputId = null;

        public CompactFileUploadFieldFinder(WebDriver driver)
        {
            super(driver);
        }

        public CompactFileUploadFieldFinder withLabel(String label)
        {
            _label = label;
            return this;
        }

        public CompactFileUploadFieldFinder withInputId(String inputId)
        {
            _inputId = inputId;
            return this;
        }

        @Override
        protected CompactFileUploadField construct(WebElement el, WebDriver driver)
        {
            return new CompactFileUploadField(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_label != null)
                return _baseLocator.withChild(Locator.tagWithClass("span", "modal__field-label").containing( _label));
            else if (_inputId != null)
                return _baseLocator.withDescendant(Locator.id(_inputId));
            else
                return _baseLocator;
        }
    }
}
