package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DetailsPage extends BaseIssuePage<DetailsPage.ElementCache>
{
    public DetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public static DetailsPage beginAt(WebDriverWrapper driver, String issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static DetailsPage beginAt(WebDriverWrapper driver, String containerPath, String issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "details", Maps.of("issueId", issueId)));
        return new DetailsPage(driver.getDriver());
    }

    public InsertPage clickNewIssue()
    {
        clickAndWait(elementCache().newIssueLink);
        return new InsertPage(getDriver());
    }

    public ListPage clickReturnToGrid()
    {
        clickAndWait(elementCache().returnLink);
        return new ListPage(getDriver());
    }

    public UpdatePage clickUpdate()
    {
        clickAndWait(elementCache().updateLink);
        return new UpdatePage(getDriver());
    }

    public ResolvePage clickResolve()
    {
        clickAndWait(elementCache().resolveLink);
        return new ResolvePage(getDriver());
    }

    public ClosePage clickClose()
    {
        clickAndWait(elementCache().closeLink);
        return new ClosePage(getDriver());
    }

    public ReopenPage clickReOpen()
    {
        clickAndWait(elementCache().reopenLink);
        return new ReopenPage(getDriver());
    }

    public LabKeyPage clickPrint()
    {
        clickAndWait(elementCache().printLink);
        return new LabKeyPage(getDriver());
    }

    public EmailPrefsPage clickEmailPrefs()
    {
        clickAndWait(elementCache().emailPrefsLink);
        return new EmailPrefsPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseIssuePage.ElementCache
    {
        protected WebElement newIssueLink = Locator.linkWithText("new issue").findWhenNeeded(this);
        protected WebElement returnLink = Locator.linkWithText("return to grid").findWhenNeeded(this);
        protected WebElement updateLink = Locator.linkWithText("update").findWhenNeeded(this);
        protected WebElement resolveLink = Locator.linkWithText("resolve").findWhenNeeded(this);
        protected WebElement closeLink = Locator.linkWithText("close").findWhenNeeded(this);
        protected WebElement reopenLink = Locator.linkWithText("reopen").findWhenNeeded(this);
        protected WebElement printLink = Locator.linkWithText("print").findWhenNeeded(this);
        protected WebElement emailPrefsLink = Locator.linkWithText("email prefs").findWhenNeeded(this);
    }
}
