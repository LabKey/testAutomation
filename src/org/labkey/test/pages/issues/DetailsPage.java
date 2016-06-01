package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DetailsPage extends BaseIssuePage
{
    public DetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public InsertPage clickNewIssue()
    {
        clickAndWait(elements().newIssueLink);
        return new InsertPage(getDriver());
    }

    public LabKeyPage clickReturnToGrid()
    {
        clickAndWait(elements().returnLink);
        return new LabKeyPage(getDriver());
    }

    public UpdatePage clickUpdate()
    {
        clickAndWait(elements().updateLink);
        return new UpdatePage(getDriver());
    }

    public UpdatePage clickResolve()
    {
        clickAndWait(elements().resolveLink);
        return new UpdatePage(getDriver());
    }

    public UpdatePage clickClose()
    {
        clickAndWait(elements().closeLink);
        return new UpdatePage(getDriver());
    }

    public UpdatePage clickReOpen()
    {
        clickAndWait(elements().reopenLink);
        return new UpdatePage(getDriver());
    }

    public LabKeyPage clickPrint()
    {
        clickAndWait(elements().printLink);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage clickEmailPrefs()
    {
        clickAndWait(elements().emailPrefsLink);
        return new LabKeyPage(getDriver());
    }

    protected Elements elements()
    {
        return (Elements) super.elements();
    }

    protected Elements newElements()
    {
        return new Elements();
    }

    protected class Elements extends BaseIssuePage.Elements
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
