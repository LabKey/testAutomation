package org.labkey.test.components.bootstrap;


import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;


public class LoggedOutDialog extends ModalDialog
{
    protected LoggedOutDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle("Logged Out")
                .waitFor().getComponentElement(), driver);
    }

    static public LoggedOutDialog waitFor(WebDriver driver)
    {
        return new LoggedOutDialog(driver);
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

    public void clickReloadPage()
    {
        elementCache().reloadPageBtn.click();
    }

    /**
     *
     * @return
     */
    public boolean isContentBlurred()
    {
        Boolean headerBlurred = null;
        var lkHeader = elementCache().lkHeader.findOptionalElement(getDriver());
        if (lkHeader.isPresent())
            headerBlurred = lkHeader.get().getAttribute("class").contains("lk-content-blur");

        Boolean bodyBlurred = null;
        var lkBody = elementCache().lkBody.findOptionalElement(getDriver());
        if (lkBody.isPresent())
            bodyBlurred = lkBody.get().getAttribute("class").contains("lk-content-blur");

        Boolean footerBlurred = null;
        var footerBlock = elementCache().footerBlock.findOptionalElement(getDriver());
        if (footerBlock.isPresent())
            footerBlurred = footerBlock.get().getAttribute("class").contains("lk-content-blur");

        return (headerBlurred != null && headerBlurred.booleanValue()) &&
                (bodyBlurred != null && bodyBlurred.booleanValue()) &&
                (footerBlurred != null && footerBlurred.booleanValue());
    }


    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final WebElement reloadPageBtn = Locator.id("lk-websocket-reload").findWhenNeeded(this);

        Locator modalBackdrop = Locator.tagWithClass("div", "modal-backdrop");

        Locator.XPathLocator body = Locator.tag("html").child("body");
        Locator lkHeader = body.child(Locator.tagWithClass("div", "lk-header-ct"));
        Locator lkBody = body.child(Locator.tagWithClass("div", "lk-body-ct"));
        Locator footerBlock = body.child(Locator.tagWithClass("footer", "footer-block"));
    }

}
