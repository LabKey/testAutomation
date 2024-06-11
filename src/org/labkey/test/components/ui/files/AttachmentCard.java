package org.labkey.test.components.ui.files;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.List;
import java.util.Optional;

/*
 * Test wrapper for AttachmentCard in '@labkey/components'
 */
public class AttachmentCard extends WebDriverComponent<AttachmentCard.ElementCache>
{
    private static final String REMOVE_ATTACHMENT = "Remove attachment";
    private static final String DOWNLOAD_ATTACHMENT = "Download";

    private final WebElement _el;
    private final WebDriver _driver;

    protected AttachmentCard(WebElement element, WebDriver driver)
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

    public String getFileName()
    {
        String title = getComponentElement().getAttribute("title");
        String[] filePath = title.split("[/\\\\]"); // '\' for Windows. '/' for MacOS and Linux
        return filePath[filePath.length - 1];
    }

    public String getSize()
    {
        return elementCache().fileSize.getText();
    }

    public String getIconSrc()
    {
        return elementCache().icon.getAttribute("src");
    }

    public ImageFileViewDialog viewImgFile()
    {
        String filename = getFileName();
        elementCache().fileContent.click();
        return new ImageFileViewDialog(getDriver(), filename);
    }

    public File clickOnNonImgFile()
    {
        return getWrapper()
                .doAndWaitForDownload(() -> elementCache().fileContent.click(), 1)[0];
    }

    public boolean canDownload()
    {
        if (!elementCache().menu.isPresent())
            return false;
        elementCache().menu.get().openMenuTo();
        List<String> menuOptions = getWrapper().getTexts(elementCache().menu.get().findVisibleMenuItems());
        elementCache().menu.get().collapse();
        return menuOptions.contains(DOWNLOAD_ATTACHMENT);
    }

    public File clickDownload()
    {
        if (!elementCache().menu.isPresent())
            throw new IllegalStateException("Unable to download attachment/file");;
        return getWrapper().doAndWaitForDownload(()->
                elementCache().menu.get().clickSubMenu(false, DOWNLOAD_ATTACHMENT));
    }

    public boolean isFileUnavailable()
    {
        return elementCache().unavailableWarning.isPresent();
    }

    public boolean canRemove()
    {
        return getRemoveOption() != null;
    }

    public void clickRemove()
    {
        if (!elementCache().menu.isPresent())
            throw new IllegalStateException("Unable to remove attachment/file");
        String remove = getRemoveOption();
        if (remove == null)
        {
            throw new IllegalStateException("Unable to remove attachment/file");
        }
        elementCache().menu.get().clickSubMenu(false, remove);
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
    }

    private String getRemoveOption()
    {
        if (!elementCache().menu.isPresent())
            return null;

        elementCache().menu.get().openMenuTo();
        List<String> menuOptions = getWrapper().getTexts(elementCache().menu.get().findVisibleMenuItems());
        return menuOptions.stream().filter(item -> item.startsWith("Remove ")).findFirst().orElse(null);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement fileSize = Locator.byClass("attachment-card__size").findWhenNeeded(this);
        final WebElement icon = Locator.byClass("attachment-card__icon_img").findWhenNeeded(this);
        Optional<BootstrapMenu> menu = BootstrapMenu.finder(getDriver())
                .locatedBy(Locator.tagWithClass("div", "attachment-card__menu"))
                .findOptional(this);
        WebElement fileContent = Locator.tagWithClass("div", "attachment-card__content")
                .refindWhenNeeded(this).withTimeout(4000);
        Optional<WebElement> unavailableWarning = Locator.tagWithClass("i", "fa-exclamation-triangle").findOptionalElement(this);
    }


    public static class FileAttachmentCardFinder extends WebDriverComponentFinder<AttachmentCard, FileAttachmentCardFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "attachment-card");
        private String _fileName = null;
        private String _title = null;

        public FileAttachmentCardFinder(WebDriver driver)
        {
            super(driver);
        }

        /*
            Finds the card by the componentElement's title attribute
            This is often the file name but can include more information, such as its location
         */
        public FileAttachmentCardFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        /*
            Finds the card by the text of its attachment-card__name element
         */
        public FileAttachmentCardFinder withFileName(String name)
        {
            _fileName = name;
            return this;
        }

        @Override
        protected AttachmentCard construct(WebElement el, WebDriver driver)
        {
            return new AttachmentCard(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else if (_fileName != null)
                return _baseLocator.withDescendant(
                        Locator.tagWithClass("div", "attachment-card__name").withText(_fileName));
            else
                return _baseLocator;
        }
    }
}
