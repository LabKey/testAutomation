/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

package org.labkey.test.tests.flow;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.categories.Flow;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, Flow.class, FileBrowser.class})
public class FlowImportTest extends BaseFlowTest
{
    @Test
    public void importTest()
    {
        String workspacePath = "/flowjoquery/microFCS/microFCS.xml";
        List<String> keywordDirs = Arrays.asList("/flowjoquery/microFCS");
        String analysisFolder = "FlowJoAnalysis";

        log("** import FlowJo workspace, without FCS file path");
        // import FlowJo workspace
        // don't select file path
        // place in FlowJoAnalysis_1 folder
        // assert only one analysis run created
        importAnalysis(getContainerPath(), workspacePath, SelectFCSFileOption.None, null, analysisFolder, false, true);
        beginAt(WebTestHelper.getContextPath() + "/query" + getContainerPath() + "/executeQuery.view?query.queryName=Runs&schemaName=flow");
        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals("Expected a single run", table.getDataRowCount(), 1);
        assertEquals("Expected an Analysis run", table.getDataAsText(0, "Protocol Step"), "Analysis");

        log("** import same FlowJo workspace again, with FCS files");
        importAnalysis_begin(getContainerPath());
        importAnalysis_uploadWorkspace(getContainerPath(), workspacePath);
        // assert analysis run doesn't show up in list of keyword runs
        assertTextNotPresent("Previously imported FCS file run");
        // assert microFCS directory is selected in the pipeline tree browser since it contains the .fcs files used by the workspace
        //assertEquals("/flowjoquery/microFCS", getTreeSelection("tree"));
        importAnalysis_selectFCSFiles(getContainerPath(), SelectFCSFileOption.Browse, keywordDirs);
        log("** Check only samples that exist in keywordDirs are selected");
        assertNotChecked(Locator.name("selectedSamples.rows[285].selected"));
        assertChecked(Locator.name("selectedSamples.rows[295].selected"));
        assertChecked(Locator.name("selectedSamples.rows[296].selected"));
        importAnalysis_reviewSamples(getContainerPath(), false, null, null);
        importAnalysis_analysisEngine(getContainerPath(), AnalysisEngine.FlowJoWorkspace);
        // assert previous analysis folder is available in drop down
        assertTextPresent("Choose an analysis folder to put the results into");
        importAnalysis_analysisFolder(getContainerPath(), analysisFolder, true);
        importAnalysis_confirm(getContainerPath(), workspacePath, SelectFCSFileOption.Browse, keywordDirs, AnalysisEngine.FlowJoWorkspace, analysisFolder, true);
        importAnalysis_checkErrors(null);
        // assert one keyword run created, one additional analysis run created
        beginAt(WebTestHelper.getContextPath() + "/query" + getContainerPath() + "/executeQuery.view?query.queryName=Runs&schemaName=flow");
        table = new DataRegionTable("query", this);
        assertEquals("Expected three runs", table.getDataRowCount(), 3);
        table.setSort("ProtocolStep", SortDirection.DESC);
        assertEquals("Expected a Keywords run", table.getDataAsText(0, "Protocol Step"), "Keywords");
        assertEquals("Expected an Analysis run", table.getDataAsText(1, "Protocol Step"), "Analysis");
        assertEquals("Expected an Analysis run", table.getDataAsText(2, "Protocol Step"), "Analysis");
        // UNDONE: Check Runs.Workspace column

        // UNDONE: Check '118795.fcs' FCSAnalysis well has a fake FCSFile that has an original FCSFile data input.
        // UNDONE: Check FCSFiles.Original column
        //assertEquals(

        log("** import same FlowJo workspace again");
        importAnalysis_begin(getContainerPath());
        importAnalysis_uploadWorkspace(getContainerPath(), workspacePath);
        assertTextPresent("Previously imported FCS files.");
        // assert keyword run shows up in list of keyword runs
        importAnalysis_selectFCSFiles(getContainerPath(), SelectFCSFileOption.Previous, Arrays.asList("microFCS"));
        log("** Check only samples that have been previously imported are selected and are matched");
        assertNotChecked(Locator.name("selectedSamples.rows[285].selected"));
        assertOptionEquals(Locator.name("selectedSamples.rows[285].matchedFile"), "");
        assertChecked(Locator.name("selectedSamples.rows[295].selected"));
        assertOptionEquals(Locator.name("selectedSamples.rows[295].matchedFile"), "118795.fcs (microFCS)");
        assertChecked(Locator.name("selectedSamples.rows[296].selected"));
        assertOptionEquals(Locator.name("selectedSamples.rows[296].matchedFile"), "118797.fcs (microFCS)");
        importAnalysis_reviewSamples(getContainerPath(), false, Arrays.asList("All Samples"), null);
        assertElementPresent(Locator.css(".labkey-error").withText("All selected rows must be matched to a previously imported FCS file."));
        importAnalysis_reviewSamples(getContainerPath(), false, Arrays.asList("labkey-demo-samples"), null);
        importAnalysis_analysisEngine(getContainerPath(), AnalysisEngine.FlowJoWorkspace);
        // assert FlowJoAnalysis analysis folder doesn't show up in list of folders
        assertTextNotPresent("Choose an analysis folder to put the results into");
        importAnalysis_analysisFolder(getContainerPath(), analysisFolder + "_1", false);
        importAnalysis_confirm(getContainerPath(), workspacePath, SelectFCSFileOption.Previous, keywordDirs, AnalysisEngine.FlowJoWorkspace, analysisFolder + "_1", false);
        importAnalysis_checkErrors(null);

        beginAt(WebTestHelper.getContextPath() + "/query" + getContainerPath() + "/executeQuery.view?query.queryName=Runs&schemaName=flow");
        table = new DataRegionTable("query", this);
        assertEquals("Expected four runs", table.getDataRowCount(), 4);

    }
}
