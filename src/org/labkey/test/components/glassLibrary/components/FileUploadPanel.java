package org.labkey.test.components.glassLibrary.components;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.labkey.test.components.html.Input.Input;

public class FileUploadPanel extends WebDriverComponent<FileUploadPanel.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public FileUploadPanel(WebElement element, WebDriver driver)
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

    public FileUploadPanel uploadFile(File file)
    {
        try
        {
            elementCache().input().set(file.getAbsolutePath());
        }catch(ElementNotInteractableException nie)
        {
            elementCache().input().set(file.getAbsolutePath());   // retry
        }
        WebDriverWrapper.waitFor(()-> hasFile(file.getName()),
                "the file ["+ file.getName() +"] was not added", 2000);
        return this;
    }

    public FileUploadPanel removeFile(String file)
    {
        WebDriverWrapper.waitFor(()-> hasFile(file),
                "the file ["+ file +"] was not present to be removed", 2000);
        elementCache().removeBtn(file).click();
        WebDriverWrapper.waitFor(()-> !hasFile(file),
                "the file was not removed", 2000);
        return this;
    }

    public List<String> attachedFiles()
    {
        return elementCache().attachedFileContainer.findElements(this)
                .stream().map(WebElement::getText).collect(Collectors.toList());
    }

    private boolean hasFile(String fileName)
    {
        return elementCache().attachedFileContainer(fileName).existsIn(this);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        Input input()
        {
            return Input(Locator.id("fileUpload"), getDriver()).waitFor(this);
        }

        Locator.XPathLocator attachedFileContainer = Locator.tagWithClass("div", "attached-file--container")
                .withChild(Locator.tagWithClass("span", "fa-times-circle"));

        Locator attachedFileContainer(String fileName)
        {
            return attachedFileContainer.withText(fileName);
        }

        WebElement removeBtn(String fileName)
        {
            return Locator.tagWithAttribute("span", "title", "Remove file")
                    .findElement(attachedFileContainer(fileName).findElement(this));
        }
    }


    public static class FileUploadPanelFinder extends WebDriverComponentFinder<FileUploadPanel, FileUploadPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "file-upload--container").parent();

        public FileUploadPanelFinder(WebDriver driver)
        {
            super(driver);
        }


        @Override
        protected FileUploadPanel construct(WebElement el, WebDriver driver)
        {
            return new FileUploadPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
