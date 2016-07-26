package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.IssueListDefDataRegion;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DeleteIssueListPage extends LabKeyPage
{
    public DeleteIssueListPage(WebDriver driver)
    {
        super(driver);
    }

    public IssueListDefDataRegion confirmDelete()
    {
        clickAndWait(Locator.lkButton("Delete"));
        return IssueListDefDataRegion.fromExecuteQuery(getDriver());
    }

    public IssueListDefDataRegion cancelDelete()
    {
        clickAndWait(Locator.lkButton("Cancel"));
        return IssueListDefDataRegion.fromExecuteQuery(getDriver());
    }
}