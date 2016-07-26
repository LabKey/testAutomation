package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.issues.DeleteIssueListPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.ext4.Window.Window;

public class IssueListDefDataRegion extends DataRegionTable
{
    protected IssueListDefDataRegion(String regionName, WebDriver driver)
    {
        super(regionName, driver);
    }

    public static IssueListDefDataRegion fromWebPart(WebDriver driver)
    {
        return new IssueListDefDataRegion("IssueListDef", driver);
    }

    public static IssueListDefDataRegion fromExecuteQuery(WebDriver driver)
    {
        return new IssueListDefDataRegion("query", driver);
    }

    public void createIssuesListDefinition(String name)
    {
        startCreateIssuesListDefinition(name).clickButton("Yes");
    }

    public Window startCreateIssuesListDefinition(String name)
    {
        clickHeaderButtonByText("Insert New");
        getWrapper().setFormElement(Locator.input("quf_Label"), name);
        getWrapper().click(Locator.linkWithText("Submit"));
        return Window().withTitle("Create Issue List Definition?").waitFor(getDriver());
    }

    public void selectIssues(String... names)
    {
        for (String name : names)
        {
            checkCheckbox(getRowIndex("Label", name));
        }
    }

    public DeleteIssueListPage clickDelete()
    {
        clickHeaderButtonByText("Delete");
        return new DeleteIssueListPage(getDriver());
    }

    public IssueListDefDataRegion deleteListDefs(String... names)
    {
        selectIssues(names);
        return clickDelete().confirmDelete();
    }
}
