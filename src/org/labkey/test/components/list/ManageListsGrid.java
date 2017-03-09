package org.labkey.test.components.list;

import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.list.ImportListArchivePage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class ManageListsGrid extends DataRegionTable
{
    public ManageListsGrid(WebDriver driver)
    {
        super("query", driver);
    }

    public ImportListArchivePage clickImportArchive()
    {
        clickHeaderButtonAndWait("Import List Archive");
        return new ImportListArchivePage(getDriver());
    }

    public LabKeyPage clickCreateList()
    {
        clickHeaderButtonAndWait("Create New List");
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage exportSelectedLists()
    {
        clickHeaderButtonAndWait("Export List Archive");
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage deleteSelectedLists()
    {
        getWrapper().doAndWaitForPageToLoad(() ->
        {
            clickHeaderButton("Delete");
            getWrapper().acceptAlert();
        });
        return new LabKeyPage(getDriver());
    }
}
