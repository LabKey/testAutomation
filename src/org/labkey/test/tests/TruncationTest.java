/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Specimen;
import org.labkey.test.categories.Study;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;

import java.io.File;

@Category({Study.class, Specimen.class})
public class TruncationTest extends StudyBaseTest
{
    private final File LIST_ARCHIVE = TestFileUtils.getSampleData("lists/searchTest.lists.zip");
    private final String STUDY_NAME = "Study 001";

    @Override
    protected void doCreateSteps()
    {
        importStudy();
        waitForPipelineJobsToComplete(1, "study import", false);
        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");
        _listHelper.importListArchive(getFolderName(), LIST_ARCHIVE);
        truncateList();
        truncateDataset();
        ensureTruncateVisibility();
    }

    protected void truncateList()
    {
        click(Locator.linkContainingText("Delete All Rows"));
        click(Ext4Helper.Locators.ext4Button("Yes"));
        waitForText("2 rows deleted");
        click(Ext4Helper.Locators.ext4Button("OK"));
        click(Locator.linkContainingText("view data"));
        waitForText("No data to show.");
    }

    protected void truncateDataset()
    {
        click(Locator.linkContainingText(STUDY_NAME));
        waitAndClick(Locator.linkContainingText("Manage Datasets"));
        waitForText("Create New Dataset");
        waitAndClick(Locator.linkContainingText("DEM-1"));
        waitAndClick(Locator.linkContainingText("Delete All Rows"));
        click(Ext4Helper.Locators.ext4Button("Yes"));
        waitForText("24 rows deleted");
        click(Ext4Helper.Locators.ext4Button("OK"));
        click(Locator.linkContainingText("View Data"));
        waitForText("No data to show.");
    }

    protected void ensureTruncateVisibility()
    {
        impersonateRole("Editor");
        click(Locator.linkContainingText(STUDY_NAME));
        waitAndClick(Locator.xpath("//span[text()='Lists']"));
        assertTextNotPresent("Delete All Rows");
        stopImpersonatingRole();
    }

    @Override
    protected void doVerifySteps()
    {
        //Do nothing
    }
}