package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineStatusTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class AssayUploadJobsPage extends LabKeyPage<AssayUploadJobsPage.ElementCache>
{
    public AssayUploadJobsPage(WebDriver driver)
    {
        super(driver);
    }

    public static AssayUploadJobsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "showUploadJobs"));
        return new AssayUploadJobsPage(webDriverWrapper.getDriver());
    }

    public static AssayUploadJobsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, int protocolId)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "showUploadJobs", Map.of("rowId", protocolId)));
        return new AssayUploadJobsPage(webDriverWrapper.getDriver());
    }

    public PipelineStatusTable getDataTable()
    {
        return (PipelineStatusTable) PipelineStatusTable.finder(getDriver()).findWhenNeeded(getDriver());
    }

    public PipelineStatusDetailsPage clickJobStatus(String jobDescription)
    {
        int jobRow = getDataTable().getJobRow(jobDescription);
        return getDataTable().clickStatusLink(jobRow);
    }

    public AssayDataPage clickViewResults()
    {
        clickAndWait(Locator.linkWithText("view results"));
        return new AssayDataPage(getDriver());
    }

    public AssayRunsPage clickViewRuns()
    {
        clickAndWait(Locator.linkWithText("view runs"));
        return new AssayRunsPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {

    }
}
