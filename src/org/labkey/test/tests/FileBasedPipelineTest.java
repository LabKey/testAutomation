/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.FileBrowserHelper;
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

import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
public class FileBasedPipelineTest extends BaseWebDriverTest
{
    private static final String PIPELINETEST_MODULE = "pipelinetest";
    private static final File SAMPLE_FILE = TestFileUtils.getSampleData("fileTypes/sample.txt");
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
        goToProjectHome();
    }

    @Test
    public void testRCopyPipeline()
    {
        final String folderName = "rCopy";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(containerPath);
        final String pipelineName = "r-copy";
        final String importAction = "Use R to duplicate a file";
        final String protocolName = "RCopy";
        final String description = "testRCopyPipeline";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
            "protocolName", protocolName,
            "protocolDescription", description);

        final Map<String, Set<String>> outputFiles = Maps.of(
            "r-copy.xml", Collections.emptySet(),
            "sample.log", Collections.emptySet(),
            "sample-taskInfo.tsv", Collections.emptySet(),
            "sample.xxx", Collections.emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, null, null, fileRoot, outputFiles);
    }

    @Test
    public void testRCopyInlinePipeline()
    {
        final String folderName = "rCopyInline";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(containerPath);
        final String pipelineName = "r-copy-inline";
        final String importAction = "Use R to duplicate a file and generate xar exp run (r-copy-inline)";
        final String protocolName = "InlineRCopy";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "skipLines", "5");

        final Map<String, Set<String>> outputFiles = new HashMap<>();
        outputFiles.put("r-copy-inline.xml", Collections.emptySet());
        outputFiles.put("sample-taskInfo.tsv", Collections.emptySet());
        outputFiles.put("sample.pipe.xar.xml", Collections.emptySet());
        outputFiles.put("sample.log", Collections.emptySet());
        outputFiles.put("sample.xxx", Collections.emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        final String jobDescription = "@files/sample (InlineRCopy)";

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties, "Duplicate File(s)", true);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, jobDescription, null, fileRoot, outputFiles);

        // Running same protocol again is an error
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties, "Duplicate File(s)", false);
        assertExtMsgBox("Error", "Cannot redefine an existing protocol", "OK");

        // Delete the job, including any referenced runs
        deletePipelineJob(jobDescription, true);

        // Verify the analysis dir was deleted
        verifyPipelineAnalysisDeleted(pipelineName, protocolName);

        // Running the same protocol again should now be a-ok.
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties, "Duplicate File(s)", true);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, jobDescription, null, fileRoot, outputFiles);
    }

    @Test
    public void testRAssayImport()
    {
        final String folderName = "rAssayImport";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(containerPath);
        final String pipelineName = "r-localtask-assayimport";
        final String importAction = "Use R to create tsv file using locally defined task and import into 'myassay' (r-localtask-assayimport)";
        final String protocolName = "assay_import";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName);

        final Map<String, Set<String>> outputFiles = new HashMap<>();
        outputFiles.put("r-localtask-assayimport.xml", Collections.emptySet());
        outputFiles.put("sample-taskInfo.tsv", Collections.emptySet());
        outputFiles.put("sample.log", Collections.emptySet());
        outputFiles.put("sample.tsv", Collections.emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName);

        // Create a target assay
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Assay List");
        clickButton("Manage Assays");

        AssayDesignerPage assayDesignerPage = _assayHelper.createAssayAndEdit("General", "myassay");
        assayDesignerPage.dataFields().selectField(0).markForDeletion(); // SpecimenID
        assayDesignerPage.dataFields().selectField(1).markForDeletion(); // ParticipantID
        assayDesignerPage.dataFields().selectField(2).markForDeletion(); // VisitID
        assayDesignerPage.dataFields().selectField(3).markForDeletion(); // Date
        assayDesignerPage.addDataField("Name", "Name", FieldDefinition.ColumnType.String);
        assayDesignerPage.addDataField("Age", "Age", FieldDefinition.ColumnType.Integer);
        assayDesignerPage.save();

        navigateToFolder(getProjectName(), folderName);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, null, null, fileRoot, outputFiles);
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

        _containerHelper.createSubfolder(getProjectName(), folderName);
        _containerHelper.disableModules("pipelinetest");

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);
    }

    @Test
    public void testWithOutputLocation()
    {
        final String folderName = "withOutputLocation";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(containerPath);
        final String pipelineName = "with-output-location";
        final String importAction = "Test output location attribute";
        final String protocolName = "with_output_location";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName);
        final Map<String, Set<String>> outputFiles = new HashMap<>();
        outputFiles.put("sample-taskInfo.tsv", Collections.emptySet());
        outputFiles.put("sample.log", Collections.emptySet());
        outputFiles.put("with-output-location.xml", Collections.emptySet());
        outputFiles.put("relative-to-analysis/sample.xxx", Collections.emptySet());
        outputFiles.put("/relative-to-root/sample.xxx", Collections.emptySet());
        outputFiles.put("/sample.xxx", Collections.emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        final String jobDescription = "@files/sample (with_output_location)";

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, null, jobDescription, fileRoot, outputFiles);

        // Delete the job, including any referenced runs
        deletePipelineJob(jobDescription, true);

        // Verify the analysis dir was deleted
        verifyPipelineAnalysisDeleted(pipelineName, protocolName);

        // Issue 22587: Verify output files outside of the analysis directory were also deleted
        _fileBrowserHelper.selectFileBrowserRoot();
        assertElementNotPresent(FileBrowserHelper.Locators.gridRowWithNodeId("sample.xxx"));
        _fileBrowserHelper.selectFileBrowserItem("/relative-to-root");
        assertElementNotPresent(FileBrowserHelper.Locators.gridRowWithNodeId("relative-to-root/sample.xxx"));

    }

    @Test
    public void testScriptTimeout()
    {
        final String folderName = "testScriptTimeout";

        final String importAction = "timeout script test";
        final String protocolName = "timeout_script_test";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName);

        _containerHelper.createSubfolder(getProjectName(), folderName);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        final String jobDescription = "@files/sample (timeout_script_test)";

        checkErrors();

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        goToModule("Pipeline");
        waitForPipelineJobsToComplete(1, jobDescription, true);
        resetErrors();

        clickAndWait(Locator.linkWithText("ERROR"));
        assertTextPresent(
                "INFO : hello script timeout world!",
                "Process killed after exceeding timeout of 1 seconds");
        assertTextNotPresent("goodbye script timeout world!");

    }

    @Test
    public void testExecTimeout()
    {
        final String folderName = "testExecTimeout";

        final String importAction = "timeout exec test";
        final String protocolName = "timeout_exec_test";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName);

        _containerHelper.createSubfolder(getProjectName(), folderName);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        final String jobDescription = "@files/sample (timeout_exec_test)";

        checkErrors();

        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        goToModule("Pipeline");
        waitForPipelineJobsToComplete(1, jobDescription, true);
        resetErrors();

        clickAndWait(Locator.linkWithText("ERROR"));
        assertTextPresent(
                "INFO : hello node timeout world!",
                "Process killed after exceeding timeout of 1 seconds");
        assertTextNotPresent("goodbye node timeout world!");
    }


    @LogMethod
    private void verifyPipelineAnalysisDeleted(@LoggedParam String pipelineName, String protocolName)
    {
        goToModule("FileContent");

        // If there had been only one job when it was deleted, the folder for the pipeline will have been deleted as well.
        if (isElementPresent(FileBrowserHelper.Locators.gridRowWithNodeId(pipelineName)))
        {
            _fileBrowserHelper.selectFileBrowserItem("/" + pipelineName);
            assertElementNotPresent(FileBrowserHelper.Locators.gridRowWithNodeId(protocolName));
        }
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
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("pipeline");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
