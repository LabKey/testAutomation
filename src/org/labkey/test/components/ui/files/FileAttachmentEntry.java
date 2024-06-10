package org.labkey.test.components.ui.files;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/*
    Wraps FileAttachmentEntry.tsx, packages/components/src/internal/components/files/FileAttachmentEntry.tsx
 */
public class FileAttachmentEntry extends WebDriverComponent<FileAttachmentEntry.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected FileAttachmentEntry(WebElement element, WebDriver driver)
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

    /*
        checks to see if the entry has a red-x-circle remove icon
     */
    public boolean allowsRemove()
    {
        return elementCache().removeIconLoc.existsIn(this);
    }

    public void remove()
    {
        var removeBtn = elementCache().removeIcon;
        removeBtn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(removeBtn));
    }

    public String getFileName()
    {
        return getComponentElement().getText();
    }

    public WebElement getIcon()
    {
        return elementCache().fileIcon;
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

    protected class ElementCache extends Component<?>.ElementCache
    {
        public Locator removeIconLoc = Locator.tagWithClass("span", "file-upload__remove--icon");
        public WebElement removeIcon = removeIconLoc.findWhenNeeded(this);
        public WebElement fileIcon = Locator.tagWithClass("span", "attached-file--icon").findWhenNeeded(this);
    }


    public static class FileAttachmentEntryFinder extends WebDriverComponentFinder<FileAttachmentEntry, FileAttachmentEntryFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.XPathLocator.union(
                Locator.tagWithClass("div", "attached-file--container"),
                Locator.tagWithClass("div", "attached-file--inline-container"));
        private String _title = null;

        public FileAttachmentEntryFinder(WebDriver driver)
        {
            super(driver);
        }

        public FileAttachmentEntryFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected FileAttachmentEntry construct(WebElement el, WebDriver driver)
        {
            return new FileAttachmentEntry(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withText(_title);
            else
                return _baseLocator;
        }
    }
}
