/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.tests.pipeline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineAnalysisHelper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
public class PipelineProtocolArchiveTest extends BaseWebDriverTest
{
    public static final String WEB_PART_NAME_PIPELINE_PROTOCOLS = "Pipeline Protocols";
    public static final String PROTOCOL_NAME_B_R = "brainRadio";
    public static final String PROTOCOL_NAME_A_M = "toBeArchived";
    public static final String PROTOCOL_NAME_NEW_PROTOCOL = "<New Protocol>";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PIPELINE = "pipeline";
    public static final String COLUMN_ARCHIVED = "Archived";
    public static final String CHECKMARK = "\u2714";

    private static final String ETL_MODULE = "ETLtest";
    private static final String PIPELINE_MODULE = "Pipeline";

    private static final File SAMPLE_INPUT_FILE1 = TestFileUtils.getSampleData("pipeline/sample1.testIn.tsv");
    private static final File SAMPLE_INPUT_FILE2 = TestFileUtils.getSampleData("pipeline/sample2.testIn.tsv");
    private static final File SAMPLE_INPUT_FILE3 = TestFileUtils.getSampleData("pipeline/sample3.testIn.tsv");
    private static final File SAMPLE_INPUT_FILE4 = TestFileUtils.getSampleData("pipeline/sample4.testIn.tsv");
    private static final File SAMPLE_INPUT_FILE5 = TestFileUtils.getSampleData("pipeline/sample5.testIn.tsv");

    @Nullable
    protected String getProjectName()
    {
        return "Protocol Archive Test";
    }

    @BeforeClass
    public static void setupProject()
    {
        PipelineProtocolArchiveTest init = (PipelineProtocolArchiveTest) getCurrentTest();
        init.doSetup();
    }

    public void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        _containerHelper.enableModules(Arrays.asList(ETL_MODULE));

        PortalHelper _portalHelper = new PortalHelper(this);
        _portalHelper.addWebPart("Data Pipeline");
        _portalHelper.addWebPart(WEB_PART_NAME_PIPELINE_PROTOCOLS);
    }

    @Test
    public void  testArchiveProtocol() throws Exception
    {
        goToProjectHome();

        String PROTOCOL_DEF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bioml>\n" +
                "<note label=\"diseaseGroup\" type=\"input\">brain</note>\n" +
                "<note label=\"docType\" type=\"input\">not_a_value_in_the_xml</note>  \n" +
                "</bioml>";

        DataRegionTable protocols = DataRegionTable.findDataRegionWithinWebpart(this, WEB_PART_NAME_PIPELINE_PROTOCOLS);
        assertEquals("Expecting empty protocol list",0,protocols.getDataRowCount());

        assertTextPresent("No data to show.");

        runPipeline(SAMPLE_INPUT_FILE1, PROTOCOL_NAME_B_R, PROTOCOL_DEF, false);

        clickProject(getProjectName());

        String protocol1_Name = protocols.getDataAsText(0,COLUMN_NAME);
        assertEquals("Wrong protocol name", PROTOCOL_NAME_B_R,protocol1_Name);
        String protocol1_Pipeline = protocols.getDataAsText(0, COLUMN_PIPELINE);
        assertEquals("Wrong pipeline name", getClientPipelineLabel(),protocol1_Pipeline);
        String protocol1_Archived = protocols.getDataAsText(0, COLUMN_ARCHIVED);
        assertEquals("Wrong archived indication", "",protocol1_Archived);


        runPipeline(SAMPLE_INPUT_FILE2, PROTOCOL_NAME_A_M, PROTOCOL_DEF, false);

        clickProject(getProjectName());

        String protocol2_Name = protocols.getDataAsText(1,COLUMN_NAME);
        assertEquals("Wrong protocol name", PROTOCOL_NAME_A_M,protocol2_Name);
        String protocol2_Archived = protocols.getDataAsText(1,COLUMN_ARCHIVED);
        assertEquals("Wrong archived indication", "",protocol2_Archived);

        List<String> expectedOptions = new ArrayList<>(Arrays.asList(
                PROTOCOL_NAME_NEW_PROTOCOL,
                PROTOCOL_NAME_B_R,
                PROTOCOL_NAME_A_M));

        confirmProtocolSelectList(expectedOptions, SAMPLE_INPUT_FILE3);

        clickProject(getProjectName());
        protocols.checkCheckbox(1);
        archiveSelected(protocols);
        protocol2_Archived = protocols.getDataAsText(1,COLUMN_ARCHIVED);
        assertEquals("Wrong archived indication", CHECKMARK, protocol2_Archived);

        expectedOptions.remove(2);

        confirmProtocolSelectList(expectedOptions, SAMPLE_INPUT_FILE4);

        clickProject(getProjectName());
        protocols.checkCheckbox(1);
        unarchiveSelected(protocols);
        protocol2_Archived = protocols.getDataAsText(1,COLUMN_ARCHIVED);
        assertEquals("Wrong archived indication", "",protocol2_Archived);

        expectedOptions.add(2,PROTOCOL_NAME_A_M);

        confirmProtocolSelectList(expectedOptions, SAMPLE_INPUT_FILE5);

    }

    private void confirmProtocolSelectList(List<String> expectedOptions, File file)
    {
        final PipelineAnalysisHelper pipelineAnalysisHelper = prepareToRunPipeline(file);

        final Select protocolSelect = pipelineAnalysisHelper.waitForProtocolSelect();
        List<String> protocolOptions = getTexts(protocolSelect.getOptions());
        assertEquals("Expected protocols" , expectedOptions, protocolOptions);
    }

    private void archiveSelected(DataRegionTable protocols)
    {
        doAndWaitForPageToLoad(()->
        {
            protocols.clickHeaderButtonByText("archive");
            assertAlert("Are you sure you want to archive the selected protocol?");
        });
    }

    private void unarchiveSelected(DataRegionTable protocols)
    {
        doAndWaitForPageToLoad(()->
        {
            protocols.clickHeaderButtonByText("unarchive");
            assertAlert("Are you sure you want to unarchive the selected protocol?");
        });
    }

    protected void runPipeline(@NotNull File file, @NotNull String protocolName, @Nullable String protocolDef, boolean expectError)
    {
        PipelineAnalysisHelper _pipelineHelper = prepareToRunPipeline(file);

        _pipelineHelper.runProtocol(protocolName, protocolDef, false);

        goToModule("Pipeline");
        assertTrue("Import did not complete.", getPipelineStatusValues().contains(expectError ? "ERROR" : "COMPLETE"));

        checkExpectedErrors(expectError ? 1 : 0);
    }

    @NotNull
    private PipelineAnalysisHelper prepareToRunPipeline(File testFile)
    {
        goToModule("Pipeline"); // this seems more robust than using the button on the home page.
        clickButton("Process and Import Data");
        _fileBrowserHelper.uploadFile(testFile);
        _fileBrowserHelper.importFile(testFile.getName(), getClientPipelineLabel());
        return new PipelineAnalysisHelper(this);
    }

    protected String getClientPipelineLabel()
    {
        return "etlTest tail";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList(PIPELINE_MODULE);
    }
}
