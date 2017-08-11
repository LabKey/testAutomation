package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class ModalDialog extends WebDriverComponent<ModalDialog.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public ModalDialog(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
        waitForReady();
    }

    public static ModalDialog find(WebDriver driver)
    {
        return new ModalDialog(Locators.component.waitForElement(driver, WAIT_FOR_JAVASCRIPT), driver);
    }

    public void waitForReady()
    {
        getWrapper().waitFor(()-> Locators.title.findElementOrNull(this ) != null &&
                newElementCache().title.getText().length() > 0, 2000);
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
        return newElementCache().title.getText();
    }

    public String getBodyText()
    {
        return newElementCache().body.getText();
    }

    public void close()
    {
        newElementCache().closeButton.click();
        getWrapper().waitFor(()-> !Locators.component.findElementOrNull(getDriver()).isDisplayed(), 2000);
        getWrapper().waitFor(()-> Locators.modalFade.findElementOrNull(getDriver())==null, 2000);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        WebElement header = Locators.header.findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement title = Locators.title.findWhenNeeded(header).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement body = Locators.body.findWhenNeeded(getComponentElement()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement closeButton = Locators.closeBtn.findWhenNeeded(header);
    }

    public static class Locators
    {
        public static final Locator modalFade = Locator.tagWithClass("div", "modal fade")
                .withAttribute("style", "display:block;");
        public static final Locator component = Locator.tagWithClass("div", "modal-dialog");
        public static final Locator contents = Locator.tagWithClass("div","modal-content");
        public static final Locator header = Locator.tagWithClass("div","modal-header");
        public static final Locator body = Locator.tagWithClass("div","modal-body");
        public static final Locator title = Locator.tagWithClass("*", "modal-title");
        public static final Locator closeBtn = Locator.tagWithClass("button", "close").withAttribute("data-dismiss", "modal");
    }
}