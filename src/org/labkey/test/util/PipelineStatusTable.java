/*
 * Copyright (c) 2008-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.Assert.assertTrue;

public class PipelineStatusTable extends DataRegionTable
{
    public static final String REGION_NAME = "StatusFiles";
    private final Map<String, String> _mapDescriptionStatus = new LinkedHashMap<>();

    public PipelineStatusTable(WebDriverWrapper test)
    {
        super(REGION_NAME, test);
    }

    public PipelineStatusTable(WebDriver driver)
    {
        super(REGION_NAME, driver);
    }

    private PipelineStatusTable(WebElement el, WebDriver driver)
    {
        super(el, driver);
    }

    public static WebDriverComponentFinder<DataRegionTable, ?> finder(WebDriver driver)
    {
        return new DataRegionFinder(driver).withName(REGION_NAME)
                .wrap((BiFunction<WebElement, WebDriver, DataRegionTable>) PipelineStatusTable::new);
    }

    public static PipelineStatusTable viewJobsForContainer(WebDriverWrapper driverWrapper, String containerPath)
    {
        driverWrapper.beginAt(WebTestHelper.buildURL("pipeline-status", containerPath, "showList"));
        return new PipelineStatusTable(driverWrapper);
    }

    public static PipelineStatusTable goToAllJobsPage(WebDriverWrapper driverWrapper)
    {
        driverWrapper.beginAt(WebTestHelper.buildURL("pipeline-status", "home", "showList", Maps.of(REGION_NAME + ".containerFilterName", "AllFolders")));
        return new PipelineStatusTable(driverWrapper);
    }

    @Override
    protected void clearCache()
    {
        super.clearCache();
        _mapDescriptionStatus.clear();
    }

    @Override
    protected Elements newElementCache()
    {
        disablePipelineRefresh();
        return super.newElementCache();
    }

    @Override
    public String getDataRegionName()
    {
        return REGION_NAME;
    }

    public FileBrowserHelper clickProcessData()
    {
        clickHeaderButtonAndWait("Process and Import Data");
        return new FileBrowserHelper(getWrapper());
    }

    private void disablePipelineRefresh()
    {
        getWrapper().executeScript("LABKEY.disablePipelineRefresh = true;");
    }

    private int getStatusColumnIndex()
    {
        return getColumnIndex("Status");
    }

    private int getDescriptionColumnIndex()
    {
        return getColumnIndex("Description");
    }

    public boolean hasJob(String description)
    {
        return getJobStatus(description) != null;
    }

    public String getJobStatus(String description)
    {
        return getMapDescriptionStatus().get(description);
    }

    private Map<String, String> getMapDescriptionStatus()
    {
        if (_mapDescriptionStatus.isEmpty())
        {
            int rows = getDataRowCount();
            int colStatus = getStatusColumnIndex();
            int colDescripton = getDescriptionColumnIndex();

            for (int i = 0; i < rows; i++)
            {
                _mapDescriptionStatus.put(getDataAsText(i, colDescripton),
                        getDataAsText(i, colStatus));
            }
        }

        return _mapDescriptionStatus;
    }

    public int getJobRow(String description)
    {
        return getJobRow(description, false);
    }

    public int getJobRow(String description, boolean descriptionStartsWith)
    {
        int i = 0;
        for (String actualDescription : getMapDescriptionStatus().keySet())
        {
            if (description.equals(actualDescription) || (descriptionStartsWith && actualDescription.startsWith(description)))
                return i;
            i++;
        }

        return -1;
    }

    public String getJobDescription(int row)
    {
        return getDataAsText(row, getDescriptionColumnIndex());
    }

    public PipelineStatusDetailsPage clickStatusLink(String description)
    {
        return clickStatusLink(getExpectedJobRow(description));
    }

    public PipelineStatusDetailsPage clickStatusLink(int row)
    {
        return clickStatusLink(row, getWrapper().getDefaultWaitForPage());
    }

    public PipelineStatusDetailsPage clickStatusLink(int row, int waitTimeout)
    {
        getWrapper().clickAndWait(link(row, getStatusColumnIndex()), waitTimeout);
        return new PipelineStatusDetailsPage(getDriver());
    }

    public LabKeyPage clickSetup()
    {
        clickHeaderButtonAndWait("Setup");
        return null; // TODO: Create pipeline setup page
    }

    private int getExpectedJobRow(String description)
    {
        int row = getJobRow(description);
        assertTrue("Job not found " + description, row != -1);
        return row;
    }

    public void deleteAllPipelineJobs()
    {
        if (getDataRowCount() > 0)
        {
            checkAllOnPage();
            deleteSelectedRows();
        }
    }

    @Override
    public void deleteSelectedRows()
    {
        clickHeaderButtonAndWait("Delete");
        if (getWrapper().isElementPresent(Locator.id("deleteRuns")))
            getWrapper().checkCheckbox(Locator.id("deleteRuns"));
        getWrapper().clickButton("Confirm Delete");
    }
}
