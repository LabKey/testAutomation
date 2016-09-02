package org.labkey.test.components;

import org.labkey.test.pages.issues.DeleteIssueListPage;
import org.labkey.test.pages.issues.InsertIssueDefPage;
import org.labkey.test.pages.issues.ListPage;
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
        clickInsertNewRowButton();
        return new InsertIssueDefPage(getDriver(), this);
    }

    public DeleteIssueListPage clickDelete()
    {
        clickHeaderButtonAndWait("Delete");
        return new DeleteIssueListPage(getDriver());
    }

    public ListPage createIssuesListDefinition(String name)
    {
        return startCreateIssuesListDefinition(name, false).clickYes();
    }

    public ListPage goToIssueList(String name)
    {
        link(getRowIndex("Label", name), "Label").click();
        return new ListPage(getDriver());
    }

    public InsertIssueDefPage.CreateListDefConfirmation startCreateIssuesListDefinition(String name, boolean errorExpected)
    {
        InsertIssueDefPage insertIssueDefPage = clickInsert();
        insertIssueDefPage.setLabel(name);
        if (!errorExpected)
            insertIssueDefPage.selectKind("General Issue Tracker");
        return insertIssueDefPage.clickSubmit(errorExpected);
    }

    public void selectIssueDefs(String... names)
    {
        for (String name : names)
        {
            checkCheckbox(getRowIndex("Label", name));
        }
    }

    public IssueListDefDataRegion deleteListDefs(String... names)
    {
        selectIssueDefs(names);
        return clickDelete().confirmDelete();
    }
}
