/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverMultipleTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PipelineAnalysisHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.NoSuchElementException;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
public class FileBasedPipelineTest extends BaseWebDriverMultipleTest
{
    private static final String PIPELINETEST_MODULE = "pipelinetest";
    private static final File SAMPLE_FILE = new File(getSampledataPath(), "fileTypes/sample.txt");
    private final PipelineAnalysisHelper pipelineAnalysis = new PipelineAnalysisHelper(this);

    @BeforeClass
    public static void doSetup() throws Exception
    {
        FileBasedPipelineTest initTest = (FileBasedPipelineTest)getCurrentTest();

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest._containerHelper.enableModules(Arrays.asList(PIPELINETEST_MODULE, "Pipeline"));

        RReportHelper rReportHelper = new RReportHelper(initTest);
        rReportHelper.ensureRConfig();
    }

    @Before
    public void startTest()
    {
        clickProject(getProjectName());
    }

    @Test
    public void testRCopyPipeline()
    {
        final String folderName = "rCopy";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = getDefaultFileRoot(containerPath);
        final String pipelineName = "r-copy";
        final String importAction = "Use R to duplicate a file";
        final String protocolName = "RCopy";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
            "protocolName", protocolName,
            "protocolDescription", "");

        final Map<String, Set<String>> outputFiles = Maps.of(
            "r-copy.xml", Collections.<String>emptySet(),
            "sample.log", Collections.<String>emptySet(),
            "sample-taskInfo.tsv", Collections.<String>emptySet(),
            "sample.xxx", Collections.<String>emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName, null);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);
    }

    @Test
    public void testRCopyInlinePipeline()
    {
        final String folderName = "rCopyInline";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = getDefaultFileRoot(containerPath);
        final String pipelineName = "r-copy-inline";
        final String importAction = "Use R to duplicate a file and generate xar exp run (r-copy-inline)";
        final String protocolName = "InlineRCopy";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "skipLines", "5");

        final Map<String, Set<String>> outputFiles = new HashMap<>();
        outputFiles.put("r-copy-inline.xml", Collections.<String>emptySet());
        outputFiles.put("sample-taskInfo.tsv", Collections.<String>emptySet());
        outputFiles.put("sample.pipe.xar.xml", Collections.<String>emptySet());
        outputFiles.put("sample.log", Collections.<String>emptySet());
        outputFiles.put("sample.xxx", Collections.<String>emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName, null);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties, "Duplicate File(s)", true);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);

        // Running same protocol again is an error
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties, "Duplicate File(s)", false);
        waitForElement(Ext4Helper.Locators.window("Error"));
        assertEquals("Cannot redefine an existing protocol", getText(Ext4Helper.Locators.windowBody("Error")));
        clickButton("OK", 0);

        // Delete the job, including any refernced runs
        String jobDescription = "@files/sample (InlineRCopy)";
        deletePipelineJob(jobDescription, true);

        // Verify the analysis dir was deleted
        verifyPipelineAnalysisDeleted(pipelineName, protocolName);

        // Running the same protocol again should now be a-ok.
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties, "Duplicate File(s)", true);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);
    }

    @Test
    public void testRAssayImport()
    {
        final String folderName = "rAssayImport";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = getDefaultFileRoot(containerPath);
        final String pipelineName = "r-localtask-assayimport";
        final String importAction = "Use R to create tsv file using locally defined task and import into 'myassay' (r-localtask-assayimport)";
        final String protocolName = "assay_import";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
            "protocolName", protocolName);

        final Map<String, Set<String>> outputFiles = new HashMap<>();
        outputFiles.put("r-localtask-assayimport.xml", Collections.<String>emptySet());
        outputFiles.put("sample-taskInfo.tsv", Collections.<String>emptySet());
        outputFiles.put("sample.log", Collections.<String>emptySet());
        outputFiles.put("sample.tsv", Collections.<String>emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName, null);

        // Create a target assay
        createAssay("General", "myassay");

        clickProject(getProjectName());
        clickFolder(folderName);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);
        verifyAssayImport("myassay");
    }

    @Test (expected = NoSuchElementException.class)
    public void testRPipelineWithModuleDisabled()
    {
        final String folderName = "rPipelineDisabled";
        final String importAction = "Use R to duplicate a file";
        final String protocolName = "Inline R Copy";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "protocolDescription", "");

        _containerHelper.createSubfolder(getProjectName(), folderName, null);
        _containerHelper.disableModules("pipelinetest");

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);
    }

    @LogMethod
    private void verifyPipelineAnalysisDeleted(@LoggedParam String pipelineName, String protocolName)
    {
        goToModule("FileContent");

        _fileBrowserHelper.selectFileBrowserItem("/" + pipelineName);
        assertElementNotPresent(FileBrowserHelper.Locators.gridRowWithNodeId(protocolName));
    }

    // Create an assay with 'Name' and 'Age' columns.
    @LogMethod
    private void createAssay(String providerName, String protocolName)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Assay List");

        clickButton("Manage Assays");
        clickButton("New Assay Design");

        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", providerName));
        clickButton("Next", 0);

        waitForElement(Locator.id("AssayDesignerName"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("AssayDesignerName"), protocolName);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.change); // GWT compensation

        _listHelper.deleteField("Data Fields", 0); // SpecimenID
        _listHelper.deleteField("Data Fields", 0); // ParticipantID
        _listHelper.deleteField("Data Fields", 0); // VisitID
        _listHelper.deleteField("Data Fields", 0); // Date
        _listHelper.addField("Data Fields", 0, "Name", "Name", ListHelper.ListColumnType.String);
        _listHelper.addField("Data Fields", 1, "Age", "Age", ListHelper.ListColumnType.Integer);

        clickButton("Save", 0);
        waitForText("Save successful.", WAIT_FOR_JAVASCRIPT);
        waitForText("Save successful.", 20000);
    }

    @LogMethod
    private void verifyAssayImport(String protocolName)
    {
        goToManageAssays();
        clickAndWait(Locator.linkWithText(protocolName));

        clickAndWait(Locator.linkWithText("view results"));
        DataRegionTable table = new DataRegionTable("Data", this);
        List<String> names = table.getColumnDataAsText("Name");
        assertTrue("Expected 'Bob' and 'Sally' in names column; got '" + names + "' instead",
                names.contains("Bob") && names.contains("Sally"));

        // UNDONE: Verify 'r-localtask-assayimport.xml' and 'sample.txt' are data inputs to the assay exp.run
        // UNDONE: and 'sample.tsv' is a data output of the assay exp.run.
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        // Issue 19545: R pipeline scripts don't support spaces
        //return this.getClass().getSimpleName() + " Project";
        return this.getClass().getSimpleName() + "Project";
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/pipeline";
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
