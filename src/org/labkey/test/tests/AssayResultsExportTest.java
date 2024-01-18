/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper.ColumnHeaderType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public class AssayResultsExportTest extends AbstractExportTest
{
    private static final File ASSAY_DESIGN_FILE = TestFileUtils.getSampleData("studyextra/TestAssay1.xar");
    protected static final String ASSAY_NAME = "TestAssay1";
    protected static final File ASSAY_RUN_FILE = TestFileUtils.getSampleData("studyextra/TestAssayRun1.tsv");

    @Override
    protected String getProjectName()
    {
        return "Assay Download Test";
    }

    @Override
    protected boolean hasSelectors()
    {
        return true;
    }

    @Override
    protected String getTestColumnTitle()
    {
        return "Participant ID";
    }

    @Override
    protected String getTestLookUpColumnHeader()
    {
        return null;
    }

    @Override
    protected int getTestLookUpColumnIndex()
    {
        return 0;
    }

    @Override
    protected int getTestColumnIndex()
    {
        return 1;
    }

    @Override
    protected String getExportedXlsTestColumnHeader(ColumnHeaderType exportType)
    {
        switch (exportType)
        {
            case FieldKey:
                return "ParticipantId";
            default:
                return "Participant ID";
        }
    }

    @Override
    protected String getExportedTsvTestColumnHeader(ColumnHeaderType exportType)
    {
        switch (exportType)
        {
            case FieldKey:
                return "ParticipantId";
            default:
                return "Participant ID";
        }
    }

    @Override
    protected String getDataRegionColumnName()
    {
        return "ParticipantID";
    }

    @Override
    protected String getDataRegionSchemaName()
    {
        return "assay.General." + ASSAY_NAME;
    }

    @Override
    protected String getDataRegionQueryName()
    {
        return "Data";
    }

    @Override
    protected String getExportedFilePrefixRegex()
    {
        return "[Dd]ata";
    }

    @Override
    protected String getDataRegionId()
    {
        return "Data";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        AssayResultsExportTest initTest = (AssayResultsExportTest) getCurrentTest();
        initTest.setupTestContainers();
    }

    protected void setupTestContainers() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Assay");

        _assayHelper.uploadXarFileAsAssayDesign(ASSAY_DESIGN_FILE, 1);
        _assayHelper.importAssay(ASSAY_NAME, ASSAY_RUN_FILE, getProjectName());
    }

    @Override
    protected void goToDataRegionPage()
    {
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        waitAndClickAndWait(Locator.linkWithText(ASSAY_RUN_FILE.getName()));
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }
}

