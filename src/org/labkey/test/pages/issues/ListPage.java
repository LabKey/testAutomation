package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Input.Input;

public class ListPage extends LabKeyPage<ListPage.ElementCache>
{
    public ListPage(WebDriver driver)
    {
        super(driver);
    }

    public static ListPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), null);
    }

    public static ListPage beginAt(WebDriverWrapper driver, String issueDefName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueDefName);
    }

    public static ListPage beginAt(WebDriverWrapper driver, String containerPath, String issueDefName)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "list", Maps.of("issueDefName", issueDefName.toLowerCase())));
        return new ListPage(driver.getDriver());
    }

    public DetailsPage jumpToIssue(String issueId)
    {
        elementCache().issueJumpInput.set(issueId);
        clickAndWait(elementCache().issueJumpButton);
        return new DetailsPage(getDriver());
    }

    public SearchResultsPage searchIssues(String search)
    {
        elementCache().searchInput.set(search);
        clickAndWait(elementCache().searchButton);
        return new SearchResultsPage(getDriver());
    }

    public InsertPage clickNewIssue()
    {
        clickAndWait(elementCache().newIssueButton);
        return new InsertPage(getDriver());
    }

    public AdminPage clickAdmin()
    {
        elementCache().issuesList.clickHeaderButtonAndWait("Admin");
        return new AdminPage(getDriver());
    }

    public EmailPrefsPage clickEmailPreferences()
    {
        elementCache().issuesList.clickHeaderButtonAndWait("Email Preferences");
        return new EmailPrefsPage(getDriver());
    }

    public DataRegionTable dataRegion()
    {
        return elementCache().issuesList;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement jumpToForm = Locator.tagWithName("form", "jumpToIssue").findWhenNeeded(this);
        WebElement newIssueButton = Locator.lkButton().startsWith("New ").findWhenNeeded(jumpToForm);
        Input issueJumpInput = Input(Locator.name("issueId"), getDriver()).findWhenNeeded(jumpToForm);
        WebElement issueJumpButton = Locator.lkButton().startsWith("Jump to ").findWhenNeeded(jumpToForm);

        Input searchInput = Input(Locator.name("q"), getDriver()).findWhenNeeded(bodyBlock);
        WebElement searchButton = Locator.lkButton("Search").findWhenNeeded(bodyBlock);
        DataRegionTable issuesList = DataRegionTable.findDataRegion(ListPage.this);
    }
}