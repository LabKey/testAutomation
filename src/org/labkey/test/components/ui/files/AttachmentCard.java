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
        return getComponentElement().getAttribute("title");
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
        elementCache().menu.openMenuTo();
        List<String> menuOptions = getWrapper().getTexts(elementCache().menu.findVisibleMenuItems());
        return menuOptions.contains(DOWNLOAD_ATTACHMENT);
    }

    public File clickDownload()
    {
        return getWrapper().doAndWaitForDownload(()->
                elementCache().menu.clickSubMenu(false, DOWNLOAD_ATTACHMENT));
    }

    public boolean canRemove()
    {
        return getRemoveOption() != null;
    }

    public void clickRemove()
    {
        String remove = getRemoveOption();
        if (remove == null)
        {
            throw new IllegalStateException("Unable to remove attachment/file");
        }
        elementCache().menu.clickSubMenu(false, remove);
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
    }

    private String getRemoveOption()
    {
        elementCache().menu.openMenuTo();
        List<String> menuOptions = getWrapper().getTexts(elementCache().menu.findVisibleMenuItems());
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
        BootstrapMenu menu = BootstrapMenu.finder(getDriver())
                .locatedBy(Locator.tagWithClass("div", "attachment-card__menu"))
                .findWhenNeeded(this);
        WebElement fileContent = Locator.tagWithClass("div", "attachment-card__content")
                .refindWhenNeeded(this).withTimeout(4000);

    }


    public static class FileAttachmentCardFinder extends WebDriverComponentFinder<AttachmentCard, FileAttachmentCardFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "attachment-card");
        private String _fileName = null;

        public FileAttachmentCardFinder(WebDriver driver)
        {
            super(driver);
        }

        public FileAttachmentCardFinder withFile(String fileName)
        {
            _fileName = fileName;
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
            if (_fileName != null)
                return _baseLocator.withAttribute("title", _fileName);
            else
                return _baseLocator;
        }
    }
}
