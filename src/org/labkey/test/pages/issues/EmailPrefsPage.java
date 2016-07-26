package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Checkbox.Checkbox;

public class EmailPrefsPage extends LabKeyPage<EmailPrefsPage.ElementCache>
{
    public EmailPrefsPage(WebDriver driver)
    {
        super(driver);
    }

    public static EmailPrefsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static EmailPrefsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "emailPrefs"));
        return new EmailPrefsPage(driver.getDriver());
    }

    public Checkbox notifyOnOpenedToMe()
    {
        return elementCache().notifyOpenedToMe;
    }

    public Checkbox notifyOnAssignedIsModified()
    {
        return elementCache().notifyAssignedIsModified;
    }

    public Checkbox notifyOnOpenedByMeModified()
    {
        return elementCache().notifyOpenedByMeModified;
    }

    public Checkbox notifyAnyIssue()
    {
        return elementCache().notifyAnyIssue;
    }

    public Checkbox notifyOnMyChanges()
    {
        return elementCache().notifyMyChanges;
    }

    public EmailPrefsPage clickUpdate()
    {
        clickAndWait(elementCache().updateButton);
        return new EmailPrefsPage(getDriver());
    }

    public ListPage clickViewGrid()
    {
        clickAndWait(elementCache().viewGridButton);
        return new ListPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Checkbox notifyOpenedToMe = Checkbox(Locator.tagWithName("input", "emailPreference").withAttribute("value", "1")).findWhenNeeded(this);
        Checkbox notifyAssignedIsModified = Checkbox(Locator.tagWithName("input", "emailPreference").withAttribute("value", "2")).findWhenNeeded(this);
        Checkbox notifyOpenedByMeModified = Checkbox(Locator.tagWithName("input", "emailPreference").withAttribute("value", "4")).findWhenNeeded(this);
        Checkbox notifyAnyIssue = Checkbox(Locator.tagWithName("input", "emailPreference").withAttribute("value", "16")).findWhenNeeded(this);
        Checkbox notifyMyChanges = Checkbox(Locator.tagWithName("input", "emailPreference").withAttribute("value", "8")).findWhenNeeded(this);
        WebElement updateButton = Locator.lkButton("Update").findWhenNeeded(this);
        WebElement viewGridButton = Locator.lkButton("View Grid").findWhenNeeded(this);
    }
}