package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Collections;

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
        WebDriverWrapper.waitFor(()-> Locators.title.findElementOrNull(this ) != null &&
                elementCache().title.getText().length() > 0, "Modal dialog not ready", 2000);
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
        return elementCache().title.getText();
    }

    public String getBodyText()
    {
        return elementCache().body.getText();
    }

    public void close()
    {
        elementCache().closeButton.click();
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfAllElements(Collections.singletonList(getComponentElement())));
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

    private static abstract class Locators
    {
        private static final Locator component = Locator.tagWithClass("div", "modal-dialog");
        private static final Locator contents = Locator.tagWithClass("div","modal-content");
        private static final Locator header = Locator.tagWithClass("div","modal-header");
        private static final Locator body = Locator.tagWithClass("div","modal-body");
        private static final Locator title = Locator.tagWithClass("*", "modal-title");
        private static final Locator closeBtn = Locator.tagWithClass("button", "close").withAttribute("data-dismiss", "modal");
    }
}