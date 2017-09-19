package org.labkey.test.components.bootstrap;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
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

    public void waitForReady()
    {
        WebDriverWrapper.waitFor(() -> elementCache().title.getText().length() > 0, "Modal dialog not ready", 2000);
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

    protected class ElementCache extends Component.ElementCache
    {
        WebElement dialog = Locator.tagWithClass("div", "modal-dialog").findWhenNeeded(this);
        WebElement header = Locator.tagWithClass("div","modal-header").findWhenNeeded(dialog).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement title = Locator.tagWithClass("*", "modal-title").findWhenNeeded(header);
        WebElement closeButton = Locator.tagWithClass("button", "close").withAttribute("data-dismiss", "modal").findWhenNeeded(header);
        WebElement body = Locator.tagWithClass("div","modal-body").findWhenNeeded(dialog).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}