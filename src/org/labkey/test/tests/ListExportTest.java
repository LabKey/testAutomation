/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.ListHelperWD;

import java.io.File;

import static org.junit.Assert.*;

@Category({DailyB.class})
public class ListExportTest extends BaseWebDriverTest
{
    private static final String LIST_NAME = "DownloadList";
    private static final String LIST_KEY = "TestKey";

    @Override
    protected String getProjectName()
    {
        return "List Download Test";
    }

    @Override
    protected void doTestSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        _listHelper.createList(getProjectName(), LIST_NAME, ListHelperWD.ListColumnType.AutoInteger, LIST_KEY);
        clickButton("Done");
        clickAndWait(Locator.linkWithText("View Data"));

        File exportedList = exportList();

        assertTrue("Exported list does not exist: " + exportedList.getAbsolutePath(), exportedList.exists());

        final String listExportRegex = LIST_NAME + "_[0-9_-]*\\.xlsx";
        assertTrue("Exported list did not have expected name: " + exportedList.getName(), exportedList.getName().matches(listExportRegex));

        assertTrue("Exported file is empty", exportedList.length() > 0);
    }

    private File exportList()
    {
        clickButton("Export", 0);
        shortWait().until(LabKeyExpectedConditions.dataRegionPanelIsExpanded(Locator.id("query")));
        click(Locator.name("excelExportType").index(0));
        File[] downloadedFiles = clickAndWaitForDownload(Locator.navButton("Export to Excel"), 1);

        assertEquals("Too many files downloaded", 1, downloadedFiles.length);

        return downloadedFiles[0];
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
       deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/list";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
