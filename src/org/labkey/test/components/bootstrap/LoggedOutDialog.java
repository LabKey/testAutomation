package org.labkey.test.components.bootstrap;


import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.PasswordUtil;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;


public class LoggedOutDialog extends ModalDialog
{
    static public List<Locator> LKS_TOP_LEVEL_ELEMENTS = List.of(org.labkey.test.Locators.headerContainer(),
            org.labkey.test.Locators.bodyPanel(), org.labkey.test.Locators.footerPanel());
    static public List<Locator> APP_TOP_LEVEL_ELEMENTS = List.of(org.labkey.test.Locators.bodyPanel());

    protected LoggedOutDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle("Session Expired")
                .waitFor().getComponentElement(), driver);
    }

    static public LoggedOutDialog waitFor(WebDriver driver)
    {
        return new LoggedOutDialog(driver);
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
        getWrapper().clickButton("Sign In");

        var returnUrl = getWrapper().getCurrentRelativeURL();
        assertThat("Signing back in did not take us back to the expected page.", returnUrl, startsWith(expectedURL));
    }

    /**
     *  Checks the background/masked elements behind the dialog to ensure that the expected content
     *  areas are blurred/styled as expected.
     *  If any of them are not present or not styled as expected, will return false.
     * @return True if all page sections that are present are styled with 'lk-content-blur', otherwise false
     */
    public boolean isContentBlurred(List<Locator> elementsToBeBlurred)
    {
        // if any of these bodyParts are present and don't have the lk-content-blur style applied, return false
        // otherwise, true
        for(Locator loc : elementsToBeBlurred)
        {
            if(!loc.findElement(getDriver()).getAttribute("class").contains("lk-content-blur"))
                return false;
        }
        return true;
    }

    /**
     * Use this to check expected top-level elements' presence in tests before subsequently calling isContentBlurred
     * to measure whether they are blurred or not
     * @param test
     * @param expectedElements  Locators for the top-level elements we expect; either for LKS or for apps
     */
    static public void verifyExpectedElements(WebDriverWrapper test, List<Locator> expectedElements)
    {
        for(Locator loc : expectedElements)
        {
            var elem = loc.findElement(test.getDriver()); // if it's not there, fail and tell us
            assertThat("expect the element to be present and to not have the content-blur style applied",
                    elem.getAttribute("class"), not(containsString("lk-content-blur")));
        }
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final WebElement reloadPageBtn = Locator.id("lk-websocket-reload").findWhenNeeded(this);
    }

}
