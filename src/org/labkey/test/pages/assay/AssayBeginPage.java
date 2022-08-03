package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.assay.plate.PlateTemplateListPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class AssayBeginPage extends LabKeyPage<AssayBeginPage.ElementCache>
{
    public AssayBeginPage(WebDriver driver)
    {
        super(driver);
    }

    public static AssayBeginPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static AssayBeginPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "begin"));
        return new AssayBeginPage(webDriverWrapper.getDriver());
    }

    public DataRegionTable getAssayList()
    {
        return elementCache().assaysList;
    }

    public AssayRunsPage clickAssay(String assayName)
    {
        clickAndWait(Locator.linkWithText(assayName));
        return new AssayRunsPage(getDriver());
    }

    public ChooseAssayTypePage clickNewAssayDesign()
    {
        elementCache().assaysList.clickHeaderButtonAndWait("New Assay Design");

        return new ChooseAssayTypePage(getDriver());
    }

    public PlateTemplateListPage clickConfigurePlateTemplates()
    {
        elementCache().assaysList.clickHeaderButtonAndWait("Configure Plate Templates");

        return new PlateTemplateListPage(getDriver());
    }

    public long getAssayId(String assayName)
    {
        int rowIndex = elementCache().assaysList.getRowIndex("Name", assayName);
        if (rowIndex < 0)
        {
            throw new NoSuchElementException("No assay named: " + assayName);
        }
        String href = elementCache().assaysList.link(rowIndex, "Name").getAttribute("href");
        try
        {
            Map<String, String> urlQuery = WebTestHelper.parseUrlQuery(new URL(href));
            return Long.parseLong(urlQuery.get("rowId"));
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        public DataRegionTable assaysList = DataRegionTable.DataRegion(getDriver()).withName("AssayList").waitFor();
    }
}
