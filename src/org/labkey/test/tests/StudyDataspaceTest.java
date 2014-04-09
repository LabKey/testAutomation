/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Study;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, Study.class})
public class StudyDataspaceTest extends StudyBaseTest
{

    protected final String FOLDER_STUDY1 = "Study 1";
    protected final String FOLDER_STUDY2 = "Study 2";
    protected final String FOLDER_STUDY5 = "Study 5";

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    protected String getProjectName()
    {
        return "DataspaceStudyVerifyProject";
    }

    protected String getStudyLabel()
    {
        return FOLDER_STUDY1;
    }

    protected String getFolderName()
    {
        return FOLDER_STUDY1;
    }

    protected String getDataspaceSampledataPath()
    {
        return getPipelinePath() + "Dataspace";
    }

    @Override
    protected void doCreateSteps()
    {
        doCleanup(false);
        initializeFolder();
        setPipelineRoot(getPipelinePath());
    }

    @Override
    protected void initializeFolder()
    {
        _containerHelper.createProject(getProjectName(), "Dataspace");
        createSubfolder(getProjectName(), getProjectName(), FOLDER_STUDY1, "Study", null, true);
        createSubfolder(getProjectName(), getProjectName(), FOLDER_STUDY2, "Study", null, true);
        createSubfolder(getProjectName(), getProjectName(), FOLDER_STUDY5, "Study", null, true);
    }

    @Override
    protected void doVerifySteps()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        log("Verify project has study, but can't import or export");
        clickFolder(getProjectName());
        clickTab("Manage");
        assertMenuButtonNotPresent("Export Study");
        assertMenuButtonNotPresent("Reload Study");
        clickTab("Overview");
        portalHelper.addQueryWebPart(null, "study", "Product", null);
        assertTextPresent("Insert New", "Import Data");     // Can insert into Product table in project
        String qwpTableId = "dataregion_qwp2";
        int qwpDefaultTableRowCount = 5;

        // Import first study
        log("Import first study and verify");
        clickFolder(FOLDER_STUDY1);
        startImportStudyFromZip(new File(getDataspaceSampledataPath(), "DataspaceStudyTest-Study1B.zip"), false, false);
        waitForPipelineJobsToComplete(1, "Study import", false);
        clickTab("Overview");
        assertTextPresent("tracks data in", "over 97 time points", "Data is present for 8 Participants");

        log("Check dataset 'Lab Results'");
        clickAndWait(Locator.linkContainingText("4 datasets"));
        clickAndWait(Locator.linkContainingText("Lab Results"));
        DataRegionTable labResultsTable = new DataRegionTable("Dataset", this, false);
        assertEquals("Expected cell value.", "Buffalo1", labResultsTable.getDataAsText(0, "Lab"));
        assertEquals("Expected cell value.", "gp145", labResultsTable.getDataAsText(0, "Product"));
        assertEquals("Expected cell value.", "VRC-HIVADV014-00-VP", labResultsTable.getDataAsText(0, "Treatment"));
        assertEquals("Expected cell value.", "543", labResultsTable.getDataAsText(0, "CD4"));
        assertEquals("Expected cell value.", "Frankie Lee", labResultsTable.getDataAsText(0, "Treatment By"));

        clickTab("Manage");
        assertMenuButtonPresent("Export Study");
        assertMenuButtonPresent("Reload Study");

        log("Verify Product rows added");
        clickFolder(getProjectName());
        Assert.assertTrue("Product row count incorrect.", getTableRowCount(qwpTableId) == qwpDefaultTableRowCount + 7);
        List<String> productNames = getTableColumnValues(qwpTableId, 3);
        Assert.assertTrue("Product table should contain these products.",
                productNames.contains("gag") && productNames.contains("gag-pol-nef") && productNames.contains("gp145"));

        // Import second study
        clickFolder(FOLDER_STUDY2);
        startImportStudyFromZip(new File(getDataspaceSampledataPath(), "DataspaceStudyTest-Study2B.zip"), false, false);
        waitForPipelineJobsToComplete(1, "Study import", false);
        clickTab("Overview");
        assertTextPresent("tracks data in", "over 103 time points", "Data is present for 8 Participants");

        log("Check dataset 'Lab Results'");
        clickAndWait(Locator.linkContainingText("2 datasets"));
        clickAndWait(Locator.linkContainingText("Lab Results"));
        DataRegionTable labResultsTable2 = new DataRegionTable("Dataset", this, false);
        assertEquals("Expected cell value.", "Buffalo1", labResultsTable2.getDataAsText(1, "Lab"));
        assertEquals("Expected cell value.", "gakkon", labResultsTable2.getDataAsText(1, "Product"));
        assertEquals("Expected cell value.", "Placebo", labResultsTable2.getDataAsText(1, "Treatment"));
        assertEquals("Expected cell value.", "520", labResultsTable2.getDataAsText(1, "CD4"));
        assertEquals("Expected cell value.", "Frankie Lee", labResultsTable2.getDataAsText(1, "Treatment By"));

        log("Verify Product rows added");
        clickFolder(getProjectName());
        Assert.assertTrue("Product row count incorrect.", getTableRowCount(qwpTableId) == qwpDefaultTableRowCount + 8);
        List<String> productNames2 = getTableColumnValues(qwpTableId, 3);
        Assert.assertTrue("Product table should contain these products.",
                productNames2.contains("gakkon") && productNames2.contains("gag-pol-nef") && productNames2.contains("gp145"));

        // Import third study
        clickFolder(FOLDER_STUDY5);
        startImportStudyFromZip(new File(getDataspaceSampledataPath(), "DataspaceStudyTest-Study5.zip"), false, false);
        waitForPipelineJobsToComplete(1, "Study import", false);

        log("Verify Product rows added");
        clickFolder(getProjectName());
        Assert.assertTrue("Product row count incorrect.", getTableRowCount(qwpTableId) == qwpDefaultTableRowCount + 8);
        List<String> productNames3 = getTableColumnValues(qwpTableId, 3);
        Assert.assertTrue("Product table should contain these products.",
                productNames3.contains("gakkon") && productNames3.contains("gag-pol-nef") && productNames3.contains("gp145"));

        // Export archive without Treatment, Product, etc.
        clickFolder(FOLDER_STUDY5);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        uncheckCheckbox(Locator.checkboxByNameAndValue("types", "Assay Schedule"));
        uncheckCheckbox(Locator.checkboxByNameAndValue("types", "Treatment Data"));
        checkRadioButton(Locator.radioButtonByNameAndValue("location", "0"));
        clickButton("Export");

        // Load study in another folder
        _fileBrowserHelper.selectFileBrowserItem("export/study/study.xml");
        createSubfolder(getProjectName(), getProjectName(), "SubFolder 5", "Collaboration", null, true);
        clickFolder("SubFolder 5");
        setPipelineRoot(getPipelinePath());
        importFolderFromPipeline("/export/folder.xml");

        log("Check dataset 'Lab Results'");
        clickFolder("SubFolder 5");
        clickAndWait(Locator.linkContainingText("1 dataset"));
        clickAndWait(Locator.linkContainingText("Lab Results"));
        DataRegionTable labResultsTable3 = new DataRegionTable("Dataset", this, false);
        assertEquals("Expected cell value.", "Buffalo1", labResultsTable3.getDataAsText(1, "Lab"));
        assertEquals("Expected cell value.", "gakkon", labResultsTable3.getDataAsText(1, "Product"));
        assertEquals("Expected cell value.", "1850", labResultsTable3.getDataAsText(1, "Lymphocytes"));
        assertEquals("Expected cell value.", "LabKeyLab", labResultsTable3.getDataAsText(4, "Lab"));
        assertEquals("Expected cell value.", "gakkon", labResultsTable3.getDataAsText(4, "Product"));
    }

}
