package org.labkey.test.components;

import org.labkey.test.pages.issues.DeleteIssueListPage;
import org.labkey.test.pages.issues.InsertIssueDefPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class IssueListDefDataRegion extends DataRegionTable
{
    public static final String NAME_IN_WEBPART = "IssueListDef";
    public static final String NAME_IN_QUERY = "query";

    public IssueListDefDataRegion(String regionName, WebDriver driver)
    {
        super(regionName, driver);
    }

    public static IssueListDefDataRegion fromWebPart(WebDriver driver)
    {
        return new IssueListDefDataRegion(NAME_IN_WEBPART, driver);
    }

    public static IssueListDefDataRegion fromExecuteQuery(WebDriver driver)
    {
        return new IssueListDefDataRegion(NAME_IN_QUERY, driver);
    }

    public InsertIssueDefPage clickInsert()
    {
        clickHeaderButtonByText("Insert New");
        return new InsertIssueDefPage(getDriver(), this);
    }

    public DeleteIssueListPage clickDelete()
    {
        clickHeaderButtonByText("Delete");
        return new DeleteIssueListPage(getDriver());
    }

    public IssueListDefDataRegion createIssuesListDefinition(String name)
    {
        return startCreateIssuesListDefinition(name).clickYes();
    }

    public InsertIssueDefPage.CreateListDefConfirmation startCreateIssuesListDefinition(String name)
    {
        InsertIssueDefPage insertIssueDefPage = clickInsert();
        insertIssueDefPage.setLabel(name);
        insertIssueDefPage.selectKind("General Issue Tracker");
        return insertIssueDefPage.clickSubmit();
    }

    public void selectIssues(String... names)
    {
        for (String name : names)
        {
            checkCheckbox(getRowIndex("Label", name));
        }
    }

    public IssueListDefDataRegion deleteListDefs(String... names)
    {
        selectIssues(names);
        return clickDelete().confirmDelete();
    }
}
