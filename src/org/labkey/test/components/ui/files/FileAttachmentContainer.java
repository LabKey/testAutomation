package org.labkey.test.components.ui.files;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.FileInput;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;

/*
    Wraps https://github.com/LabKey/labkey-ui-components/blob/develop/packages/components/src/internal/components/files/FileAttachmentContainer.tsx
 */
public class FileAttachmentContainer extends WebDriverComponent<FileAttachmentContainer.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public FileAttachmentContainer(WebElement element, WebDriver driver)
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


    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    public boolean isMulti()
    {
        return elementCache().fileInput.getComponentElement().getAttribute("multiple") != null;
    }

    /*
        in single-mode, when a file has been attached, the drop target is hidden
        in multi-mode, the drop target is shown at all times
        in compact-mode, drop target is sometimes not present
     */
    public boolean isDropTargetHidden()
    {
        var dropTarget = elementCache().dropTargetLoc.findOptionalElement(this);
        if (dropTarget.isEmpty())
            return true;
        else
            return dropTarget.get().getAttribute("class").contains("hidden");
    }

    /*
        Alerts can be shown when you try to drop multiple files on a single-mode container
        they can also occur when files the container won't accept are uploaded
     */
    public boolean hasAlert()
    {
        return elementCache().uploadAlertLoc.existsIn(this);
    }

    public String getUploadAlert()
    {
        return elementCache().uploadAlertLoc.findElement(this).getText();
    }

    public FileAttachmentContainer attachFile(File file)
    {
        try
        {
            elementCache().fileInput.set(file);
        }catch(ElementNotInteractableException nie)
        {
            WebDriverWrapper.sleep(1000);
            elementCache().fileInput.set(String.valueOf(file));
        }
        WebDriverWrapper.waitFor(()-> hasFile(file.getName()),
                "the file ["+ file.getName() +"] was not added", 2000);

        clearElementCache();
        return this;
    }

    public String attachFileExpectingAlert(File file)
    {
        elementCache().fileInput.set(file);
        return elementCache().uploadAlertLoc.findElement(this).getText();
    }

    /**
     * Returns true if there is a file with that name in the current instance
     */
    private boolean hasFile(String fileName)
    {
        return new FileAttachmentEntry.FileAttachmentEntryFinder(getDriver())
                .withTitle(fileName).findOptional(this).isPresent();
    }

    /*
        Assumes there is an attached file
     */
    public FileAttachmentContainer removeFile()
    {
        getAttachedFileEntries().get(0).remove();
        return this;
    }

    /**
     * removes the specified attachment
     * @param fileName the name of the attached file to detach
     * @return the current instance
     */
    public FileAttachmentContainer removeFile(String fileName)
    {
        getAttachedFileEntry(fileName).remove();
        clearElementCache();
        return this;
    }

    public FileAttachmentContainer removeAllAttachedFiles()
    {
        var entries = getAttachedFileEntries();
        for (FileAttachmentEntry entry : entries)
            entry.remove();
        return this;
    }

    public FileAttachmentEntry getAttachedFileEntry(String fileName)
    {
        return new FileAttachmentEntry.FileAttachmentEntryFinder(getDriver()).withTitle(fileName)
                .waitFor(elementCache().fileEntryList);
    }

    public List<FileAttachmentEntry> getAttachedFileEntries()
    {
        return new FileAttachmentEntry.FileAttachmentEntryFinder(getDriver())
                .findAll(elementCache().fileEntryList);
    }

    /*
        When the input accepts only specific file types, that information will be here
     */
    public String accepts()
    {
        return elementCache().fileInput.getComponentElement().getAttribute("accept");
    }

    public List<String> getAttachedFileNames()
    {
        return getAttachedFileEntries().stream().map(FileAttachmentEntry::getFileName).toList();
    }

    public boolean hasFooterMessage()
    {
        return elementCache().fileUploadScrollFooterLoc.existsIn(this);
    }

    /*
        scroll footer appears when this isMulti- contains message like 'n files will be uploaded' when the form is submitted
     */
    public String getFooterMessage()
    {
        return elementCache().fileUploadScrollFooterLoc.findElement(this).getText();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        public Locator dropTargetLoc = Locator.XPathLocator.union(
                Locator.tagWithClass("div", "file-upload--container"),
                Locator.tagWithClass("div", "file-upload--container--compact"));
        public Locator labelLoc = Locator.XPathLocator.union(
                Locator.tagWithClass("label", "file-upload--label"),
                Locator.tagWithClass("label", "file-upload--label--compact"));
        public FileInput fileInput = Input.FileInput(Locator.tagWithClass("input", "file-upload--input"), getDriver())
                .findWhenNeeded(this);
        public WebElement fileEntryList = Locator.tagWithClass("div", "file-upload--file-entry-listing")
                .findWhenNeeded(this);
        public Locator uploadAlertLoc = Locator.tagWithClass("div", "alert");
        public Locator fileUploadScrollFooterLoc = Locator.tagWithClass("div", "file-upload--scroll-footer");
    }


    public static class FileAttachmentContainerFinder extends WebDriverComponentFinder<FileAttachmentContainer, FileAttachmentContainerFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "file-upload--container").parent("div");
        private String _inputName = null;
        private String _label = null;

        public FileAttachmentContainerFinder(WebDriver driver)
        {
            super(driver);
        }

        public FileAttachmentContainerFinder withNamedInput(String inputName)
        {
            _inputName = inputName;
            return this;
        }

        public FileAttachmentContainerFinder withLabelFor(String label)
        {
            _label = label;
            return this;
        }

        @Override
        protected FileAttachmentContainer construct(WebElement el, WebDriver driver)
        {
            return new FileAttachmentContainer(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_inputName != null)
                return _baseLocator.withDescendant(Locator.tagWithClass("input", "file-upload--input")
                        .withAttribute("name", _inputName));
            else if (_label != null)
                return Locator.tagWithClass("label", "file-upload--label")
                        .withAttributeMatchingOtherElementAttribute("name", Locator.tag("label").withText(_label), "for")
                        .parent("*");
            else
                return _baseLocator;
        }
    }
}
