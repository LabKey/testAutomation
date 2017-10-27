/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;

@Category({DailyB.class})
public class DatasetExportTest extends AssayResultsExportTest
{
    @Override
    protected String getProjectName()
    {
        return "Dataset Download Test";
    }

    protected String getFolderName()
    {
        return "SimpleStudy";
    }

    @Override
    protected boolean hasSelectors()
    {
        return true;
    }

    @Override
    protected int getTestColumnIndex()
    {
        return 0;
    }

    protected String getDataRegionColumnName()
    {
        return "ParticipantId";
    }

    @Override
    protected String getDataRegionSchemaName()
    {
        return "study";
    }

    @Override
    protected String getDataRegionQueryName()
    {
        return ASSAY_NAME;
    }

    @Override
    protected String getExportedFilePrefixRegex()
    {
        return ASSAY_NAME;
    }

    @Override
    protected String getDataRegionId()
    {
        return "Dataset";
    }

    @Override
    protected boolean expectSortedExport()
    {
        return false;
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        DatasetExportTest initTest = (DatasetExportTest)getCurrentTest();
        initTest.setupDataset();
    }

    public void setupDataset() throws Exception
    {
        super.setupTestContainers();

        _containerHelper.createSubfolder(getProjectName(), getFolderName(), "Study");
        clickButton("Create Study");
        checkRadioButton(Locator.radioButtonByNameAndValue("timepointType", "DATE"));
        clickButton("Create Study");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithText(ASSAY_RUN_FILE.getName()));

        DataRegionTable assayResults = new DataRegionTable(super.getDataRegionId(), this);
        assayResults.checkAll();
        clickButton("Copy to Study");
        selectOptionByText(Locator.name("targetStudy"), "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)");
        clickButton("Next");
        clickButton("Copy to Study");
    }

    @Override
    protected void goToDataRegionPage()
    {
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText("1 dataset"));
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }
}
