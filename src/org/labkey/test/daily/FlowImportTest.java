/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.test.daily;

import org.labkey.test.BaseFlowTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.SortDirection;
import org.labkey.test.util.DataRegionTable;

/**
 * User: kevink
 * Date: Mar 31, 2009
 */
public class FlowImportTest extends BaseFlowTest
{
    protected void doTestSteps() throws Exception
    {
        init();

        // set pipeline root
        clickLinkWithText("Set pipeline root");
        setFormElement("path", getLabKeyRoot() + PIPELINE_PATH);
        submit();
        clickLinkWithText("Flow Dashboard");

        String containerPath = "/" + PROJECT_NAME + "/" + getFolderName();
        String workspacePath = "/flowjoquery/microFCS/microFCS.xml";
        String fcsFilePath = "/flowjoquery/microFCS";
        String analysisFolder = "FlowJoAnalysis";

        log("** import FlowJo workspace, without FCS file path");
        // import FlowJo workspace
        // don't select file path
        // place in FlowJoAnalysis_1 folder
        // assert only one analysis run created
        importAnalysis(containerPath, workspacePath, null, analysisFolder);
        beginAt(WebTestHelper.getContextPath() + "/query/" + PROJECT_NAME + "/" + getFolderName() + "/executeQuery.view?query.queryName=Runs&schemaName=flow");
        DataRegionTable table = new DataRegionTable("query", this, true);
        assertEquals("Expected a single run", table.getDataRowCount(), 1);
        assertEquals("Expected an Analysis run", table.getDataAsText(0, "Protocol Step"), "Analysis");

        log("** import same FlowJo workspace again, with FCS files");
        importAnalysis_begin(containerPath);
        importAnalysis_uploadWorkspace(containerPath, workspacePath);
        // assert analysis run doesn't show up in list of keyword runs
        assertTextNotPresent("Option 2: Choose previously uploaded directory of FCS files:");
        // assert microFCS directory is selected in the pipeline tree browser since it contains the .fcs files used by the workspace
        //assertEquals("/flowjoquery/microFCS", getTreeSelection("tree"));
        importAnalysis_FCSFiles(containerPath, fcsFilePath, false);
        // assert previous analysis folder is available in drop down
        assertTextPresent("Choose an analysis folder to put the results into");
        importAnalysis_analysisFolder(containerPath, analysisFolder, true);
        importAnalysis_confirm(containerPath, workspacePath, fcsFilePath, false, analysisFolder, true);
        // assert one keyword run created, one additional analysis run created
        beginAt(WebTestHelper.getContextPath() + "/query/" + PROJECT_NAME + "/" + getFolderName() + "/executeQuery.view?query.queryName=Runs&schemaName=flow");
        table = new DataRegionTable("query", this, true);
        assertEquals("Expected three runs", table.getDataRowCount(), 3);
        table.setSort("ProtocolStep", SortDirection.DESC);
        assertEquals("Expected a Keywords run", table.getDataAsText(0, "Protocol Step"), "Keywords");
        assertEquals("Expected an Analysis run", table.getDataAsText(1, "Protocol Step"), "Analysis");
        assertEquals("Expected an Analysis run", table.getDataAsText(2, "Protocol Step"), "Analysis");

        log("** import same FlowJo workspace again");
        importAnalysis_begin(containerPath);
        importAnalysis_uploadWorkspace(containerPath, workspacePath);
        assertTextPresent("Option 2: Choose previously uploaded directory of FCS files:");
        // assert keyword run shows up in list of keyword runs
        importAnalysis_FCSFiles(containerPath, "microFCS", true);
        // assert FlowJoAnalysis analysis folder doesn't show up in list of folders
        assertTextNotPresent("Choose an analysis folder to put the results into");
        importAnalysis_analysisFolder(containerPath, analysisFolder + "_1", false);
        importAnalysis_confirm(containerPath, workspacePath, fcsFilePath, true, analysisFolder + "_1", false);

        beginAt(WebTestHelper.getContextPath() + "/query/" + PROJECT_NAME + "/" + getFolderName() + "/executeQuery.view?query.queryName=Runs&schemaName=flow");
        table = new DataRegionTable("query", this, true);
        assertEquals("Expected four runs", table.getDataRowCount(), 4);

        log("** import same FlowJo workspace again");
    }
}
