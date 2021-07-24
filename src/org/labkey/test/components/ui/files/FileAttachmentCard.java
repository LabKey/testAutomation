package org.labkey.test.components.ui.files;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.DropdownButtonGroup;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

import static org.labkey.test.components.html.Input.Input;

/*
    Wraps /src/client/eln/AttachmentCard.tsx
    Used to wrap file attachments in eln, sm/bio workflow
 */
public class FileAttachmentCard extends WebDriverComponent<FileAttachmentCard.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected FileAttachmentCard(WebElement element, WebDriver driver)
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

    public String getTitle()
    {
        return getComponentElement().getAttribute("title");
    }

    public File clickDownload()
    {
        return getWrapper().doAndWaitForDownload(()->
                elementCache().menu.clickSubMenu(false, "Download"));
    }

    public void clickRemove()
    {
        elementCache().menu.clickSubMenu(false, "Remove attachment");
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement menuElement = Locator.tagWithClass("div", "attachment-card__menu")
                .findWhenNeeded(this);
        BootstrapMenu menu = new BootstrapMenu(getDriver(), menuElement)
                .setToggleLocator(Locator.id("attachment-card__menu"));
    }


    public static class FileAttachmentCardFinder extends WebDriverComponentFinder<FileAttachmentCard, FileAttachmentCardFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "attachment-card");
        private String _title = null;

        public FileAttachmentCardFinder(WebDriver driver)
        {
            super(driver);
        }

        public FileAttachmentCardFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected FileAttachmentCard construct(WebElement el, WebDriver driver)
        {
            return new FileAttachmentCard(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else
                return _baseLocator;
        }
    }
}
