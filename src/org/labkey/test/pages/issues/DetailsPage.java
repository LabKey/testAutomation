package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DetailsPage extends BaseIssuePage<DetailsPage.ElementCache>
{
    public DetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public InsertPage clickNewIssue()
    {
        clickAndWait(elementCache().newIssueLink);
        return new InsertPage(getDriver());
    }

    public LabKeyPage clickReturnToGrid()
    {
        clickAndWait(elementCache().returnLink);
        return new LabKeyPage(getDriver());
    }

    public UpdatePage clickUpdate()
    {
        clickAndWait(elementCache().updateLink);
        return new UpdatePage(getDriver());
    }

    public UpdatePage clickResolve()
    {
        clickAndWait(elementCache().resolveLink);
        return new UpdatePage(getDriver());
    }

    public UpdatePage clickClose()
    {
        clickAndWait(elementCache().closeLink);
        return new UpdatePage(getDriver());
    }

    public UpdatePage clickReOpen()
    {
        clickAndWait(elementCache().reopenLink);
        return new UpdatePage(getDriver());
    }

    public LabKeyPage clickPrint()
    {
        clickAndWait(elementCache().printLink);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage clickEmailPrefs()
    {
        clickAndWait(elementCache().emailPrefsLink);
        return new LabKeyPage(getDriver());
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
