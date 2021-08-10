package org.labkey.test.components.ui.files;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.MultiMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

// AttachmentCard.tsx
public class FileUploadField extends WebDriverComponent<FileUploadField.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public FileUploadField(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public FileUploadField setFile(File file)
    {
        if (hasAttachedFile())
        {
            removeFile();
        }
        if (hasTempAttachedFile())
        {
            removeTempFile();
        }
        elementCache().fileInput.sendKeys(file.getAbsolutePath());
        WebDriverWrapper.waitFor(()-> hasFile() && getAttachedFileName().equals(file.getName()),
                "the file did not get attached", 4000);
        return this;
    }

    public ImageFileViewDialog viewImgFile()
    {
        String filename = getAttachedFileName();
        elementCache().fileContent.click();
        return new ImageFileViewDialog(getDriver(), filename);
    }

    public File clickOnNonImgFile()
    {
        return getWrapper()
                .doAndWaitForDownload(() -> elementCache().fileContent.click(), 1)[0];
    }
    
    public FileUploadField removeFile(String fileNoun)
    {
        getActionMenu().doMenuAction("Remove " + fileNoun);
        WebDriverWrapper.waitFor(()-> !hasAttachedFile(),
                "the file was not removed", 2000);
        return this;
    }

    public FileUploadField removeFile()
    {
        return removeFile("file");
    }

    public FileUploadField removeTempFile()
    {
        elementCache().removeBtn.click();
        WebDriverWrapper.waitFor(()-> !hasAttachedFile(),
                "the file was not removed", 2000);
        return this;
    }

    public boolean hasFile()
    {
        return hasAttachedFile() || hasTempAttachedFile();
    }

    public boolean hasAttachedFile()
    {
        return elementCache().fileNameLoc.existsIn(this);
    }

    public boolean hasTempAttachedFile()
    {
        return elementCache().tempFileLoc.existsIn(this);
    }

    public String getAttachedFileName()
    {
        if (hasFile())
        {
            return (hasAttachedFile() ? elementCache().fileNameLoc : elementCache().tempFileLoc)
                    .waitForElement(this, 2000).getText();
        }
        else
            return "";
    }

    public MultiMenu getActionMenu()
    {
        return elementCache().actionMenu;
    }

    public File download()
    {
        return getWrapper()
                .doAndWaitForDownload(() -> getActionMenu().doMenuAction("Download"), 1)[0];
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

        WebElement fileInput = Locator.tagWithClass("input", "file-upload--input")
                .refindWhenNeeded(this).withTimeout(4000);

        Locator menuBtnLoc = Locator.tagWithId("div", "attachment-card__menu");

        Locator fileNameLoc = Locator.tagWithClass("div", "attachment-card__name");

        Locator tempFileLoc = Locator.tagWithClass("div", "attached-file--inline-container");

        WebElement fileContent = Locator.tagWithClass("div", "attachment-card__content")
                .refindWhenNeeded(this).withTimeout(4000);

        WebElement menuBtn = menuBtnLoc
                .refindWhenNeeded(this).withTimeout(4000);

        WebElement removeBtn = Locator.tagWithClass("span", "file-upload__remove--icon")
                .refindWhenNeeded(this).withTimeout(4000);

        MultiMenu actionMenu = new MultiMenu.MultiMenuFinder(getDriver()).withButtonId("attachment-card__menu").findWhenNeeded(this);
    }


    public static class FileUploadFieldFinder extends WebDriverComponent.WebDriverComponentFinder<FileUploadField, FileUploadFieldFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tag("tr");
        private String _label = null;

        public FileUploadFieldFinder(WebDriver driver)
        {
            super(driver);
        }

        public FileUploadFieldFinder withLabel(String label)
        {
            _label = label;
            return this;
        }

        @Override
        protected FileUploadField construct(WebElement el, WebDriver driver)
        {
            return new FileUploadField(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_label != null)
                return _baseLocator.withChild(Locator.tag("td").withChild(Locator.tagContainingText("span", _label)));
            else
                return _baseLocator;
        }
    }
}
