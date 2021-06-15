package org.labkey.test.components.bootstrap;


import org.labkey.test.Locator;
import org.labkey.test.util.PasswordUtil;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().reloadPageBtn));
        elementCache().reloadPageBtn.click();
    }

    public void reloadPageAndConfirmPostLoginRedirect(String expectedURL)
    {
        clickReloadPage();  // dismisses the dialog

        // at this point, we're either at a login page already, or at a public page showing a link to one
        // when we add biologics to this, there will likely be a third case to handle here
        WebElement el = Locator.waitForAnyElement(new FluentWait<SearchContext>(getDriver()).withTimeout(Duration.ofMillis(WAIT_FOR_JAVASCRIPT)),
                org.labkey.test.Locators.signInLink,
                Locator.tagWithName("form", "login"));

        if (el.getAttribute("class").equals("header-link"))
            getWrapper().clickAndWait(el);

        // We should now be on the regular login page, without the beginAt action simpleSignIn does
        getWrapper().assertElementPresent(Locator.tagWithName("form", "login"));
        getWrapper().setFormElement(Locator.name("email"), PasswordUtil.getUsername());
        getWrapper().setFormElement(Locator.name("password"), PasswordUtil.getPassword());
        getWrapper().clickButton("Sign In", 0);

        var returnUrl = getWrapper().getCurrentRelativeURL();
        assertThat("Signing back in did not take us back to the expected page.", returnUrl, is(expectedURL));
    }

    /**
     *  Checks the background/masked elements behind the dialog to ensure that the expected content
     *  areas are blurred/styled as expected.
     *  Because not all of these page sections are present in all situations (notably, in apps like
     *  SM, FM, Biologics) this checks for the ones present, and if any of them are both present and
     *  not styled as expected, will return false.
     * @return True if all page sections that are present are styled with 'lk-content-blur', otherwise false
     */
    public boolean isContentBlurred()
    {
        List<Boolean> factors = new ArrayList<>();
        var lkHeader = elementCache().lkHeader.findOptionalElement(getDriver());
        if (lkHeader.isPresent())
            factors.add(lkHeader.get().getAttribute("class").contains("lk-content-blur"));

        var lkBody = elementCache().lkBody.findOptionalElement(getDriver());
        if (lkBody.isPresent())
            factors.add(lkBody.get().getAttribute("class").contains("lk-content-blur"));

        var footerBlock = elementCache().footerBlock.findOptionalElement(getDriver());
        if (footerBlock.isPresent())
            factors.add(footerBlock.get().getAttribute("class").contains("lk-content-blur"));

        // if any of these bodyParts are present and don't have the lk-content-blur style applied, return false
        // otherwise, true
        for(Boolean fact : factors)
        {
            if(!fact)
                return false;
        }
        return true;
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
