package org.labkey.test.tests;

import junit.framework.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 2/21/13
 * Time: 4:10 PM
 */
public class AdminConsoleTest extends BaseWebDriverTest
{
    public String getProjectName()
    {
        return "AdminConsoleTestProject";
    }

    public void doTestSteps() throws Exception
    {
        testRibbonBar();
    }

    public void testRibbonBar() throws Exception
    {
        goToAdminConsole();
        waitAndClick(Locator.linkContainingText("site settings"));
        waitForElement(Locator.name("showRibbonMessage"));

        WebElement el = Locator.name("ribbonMessageHtml").findElement(_driver);
        el.clear();

        WebElement checkbox = Locator.checkboxByName("showRibbonMessage").findElement(getDriver());

        //only select if not already checked
        if (!("true".equals(checkbox.getAttribute("checked"))))
            clickCheckbox("showRibbonMessage");

        clickButton("Save");

        waitForElement(Locator.xpath("//div[contains(text(), 'Cannot enable the ribbon message with providing a message to show')]"));

        String linkText = "and also click this...";
        String html = "READ ME!!!  <a href='<%=contextPath%>/project/home/begin.view'>" + linkText + "</a>";

        //only check if not already checked
        checkbox = Locator.checkboxByName("showRibbonMessage").findElement(getDriver());
        if (!("true".equals(checkbox.getAttribute("checked"))))
            clickCheckbox("showRibbonMessage");

        setFormElement(Locator.name("ribbonMessageHtml"), html);
        clickButton("Save");

        waitForElement(Locator.linkContainingText("site settings"));

        Locator ribbon = Locator.xpath("//div[contains(@class, 'labkey-warning-messages')]//li[contains(text(), 'READ ME!!!')]");
        Locator ribbonLink = Locator.xpath("//div[contains(@class, 'labkey-warning-messages')]//li[contains(text(), 'READ ME!!!')]//..//a");

        assertElementPresent(ribbon);
        assertElementPresent(ribbonLink);

        el = getDriver().findElement(By.xpath("//div[contains(@class, 'labkey-warning-messages')]//..//a"));
        Assert.assertNotNull("Link not present in ribbon bar", el);

        String href = el.getAttribute("href");
        String expected = getBaseURL() + "/project/home/begin.view";
        Assert.assertEquals("Incorrect URL", expected, href);

        goToHome();
        impersonateRole("Reader");

        assertElementPresent(ribbon);
        assertElementPresent(ribbonLink);

        stopImpersonatingRole();

        goToAdminConsole();
        waitAndClick(Locator.linkContainingText("site settings"));
        waitForElement(Locator.name("showRibbonMessage"));
        clickCheckbox("showRibbonMessage");  //deactivate
        clickButton("Save");
        waitForElement(Locator.linkContainingText("site settings"));
        assertElementNotPresent(ribbon);
        assertElementNotPresent(ribbonLink);
    }

    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    public void checkQueries()
    {

    }

    @Override
    public void checkViews()
    {

    }
}
