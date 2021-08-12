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
        elementCache().menu.openMenuTo();
        List<String> menuOptions = getWrapper().getTexts(elementCache().menu.findVisibleMenuItems());
        return menuOptions.contains(REMOVE_ATTACHMENT);
    }

    public void clickRemove()
    {
        elementCache().menu.clickSubMenu(false, REMOVE_ATTACHMENT);
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement fileSize = Locator.byClass("attachment-card__size").findWhenNeeded(this);
        BootstrapMenu menu = BootstrapMenu.finder(getDriver())
                .locatedBy(Locator.tagWithClass("div", "attachment-card__menu"))
                .findWhenNeeded(this)
                .setToggleLocator(Locator.byClass("dropdown-toggle"));
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
