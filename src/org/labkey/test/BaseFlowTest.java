/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract public class BaseFlowTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "Flow Verify Project";
    protected static final String PIPELINE_PATH = "/sampledata/flow";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/flow";
    }

    //need not fill all three, but must be the same length.  If you wish to skip a field, set it to an empty string,
    // or the default in the case of op
    public void setFlowFilter(String[] fields, String[] ops, String[] values)
    {
        goToFlowDashboard();
        clickLinkWithText("Other settings");
        clickLinkWithText("Edit FCS Analysis Filter");

        for(int i=0; i<fields.length; i++)
        {
            selectOptionByValue(Locator.xpath("//select[@name='ff_field']").index(i),  fields[i]);
            selectOptionByValue(Locator.xpath("//select[@name='ff_op']").index(i), ops[i]);
            setFormElement(Locator.xpath("//input[@name='ff_value']").index(i), values[i]);
        }
        submit();
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected String getFolderName()
    {
        return getClass().getSimpleName();
    }

    private String _containerPath = null;

    protected String getContainerPath()
    {
        if (_containerPath == null)
            _containerPath = "/" + getProjectName() + "/" + getFolderName();
        return _containerPath;
    }


    protected void setFlowPipelineRoot(String rootPath)
    {
        setPipelineRoot(rootPath);
    }

    protected File getPipelineWorkDirectory()
    {
        return new File(getLabKeyRoot() + "/sampledata/flow/work");
    }

    protected void deletePipelineWorkDirectory()
    {
        File dir = getPipelineWorkDirectory();
        if (dir.exists())
        {
            try
            {
                log("Deleting pipeline work directory: " + dir);
                FileUtils.deleteDirectory(dir);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    protected void waitForPipeline(String containerPath)
    {
        pushLocation();
        beginAt("/Flow" + containerPath + "/showJobs.view");

        long startTime = System.currentTimeMillis();
        do
        {
            log("Waiting for flow pipeline jobs to complete...");
            sleep(1500);
            refresh();
        }
        while (!isTextPresent("There are no running or pending flow jobs") && System.currentTimeMillis() - startTime < 300000);

        popLocation(longWaitForPage);
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(getProjectName()); } catch (Throwable t) {}
        deletePipelineWorkDirectory();
        try
        {
            beginAt("/admin/begin.view");
            clickLinkWithText("flow cytometry");
            setFormElement("workingDirectory", "");
            clickButton("update");
        }
        catch (Throwable t) {}
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        init();
        _doTestSteps();
        after();
    }

    protected abstract void _doTestSteps() throws Exception;

    protected boolean requiresNormalization()
    {
        return false;
    }

    protected void init()
    {
        beginAt("/admin/begin.view");
        clickLinkWithText("flow cytometry");
        deletePipelineWorkDirectory();
        setFormElement("workingDirectory", getPipelineWorkDirectory().toString());
        clickButton("update");
        assertTextPresent("Path does not exist");
        getPipelineWorkDirectory().mkdir();
        setFormElement("workingDirectory", getPipelineWorkDirectory().toString());

        boolean normalizationEnabled = requiresNormalization();
        if (normalizationEnabled)
            checkCheckbox(Locator.id("normalizationEnabled"));
        else
            uncheckCheckbox(Locator.id("normalizationEnabled"));

        clickButton("update");
        assertTextNotPresent("Path does not exist");
        if (normalizationEnabled)
        {
            assertTextNotPresent("The R script engine is not available.");
            assertTextNotPresent("Please install the flowWorkspace R library");
        }

        _containerHelper.createProject(getProjectName(), null);
        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Flow", null);

        setFlowPipelineRoot(getLabKeyRoot() + PIPELINE_PATH);
    }

    protected void after() throws Exception
    {
        if (!skipCleanup())
            deleteAllRuns();
    }

    //Issue 12597: Need to delete exp.data objects when deleting a flow run
    protected void deleteAllRuns() throws Exception
    {
        if (!isLinkPresentWithText(getProjectName()))
            goToHome();
        if (!isLinkPresentWithText(getProjectName()))
            return;

        clickLinkWithText(getProjectName());
        if (!isLinkPresentWithText(getFolderName()))
            return;

        clickLinkWithText(getFolderName());

        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=Runs");
        DataRegionTable table = new DataRegionTable("query", this);
        if (table.getDataRowCount() > 0)
        {
            // Delete all runs
            table.checkAllOnPage();
            selenium.chooseOkOnNextConfirmation();
            clickButton("Delete", 0);
            Assert.assertTrue(selenium.getConfirmation().contains("Are you sure you want to delete the selected row"));
            waitForPageToLoad();
            Assert.assertEquals("Expected all experiment Runs to be deleted", 0, table.getDataRowCount());

            // Check all DataInputs were deleted
            beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=DataInputs");
            Assert.assertEquals("Expected all experiment DataInputs to be deleted", 0, table.getDataRowCount());

            // Check all Datas were deleted except for flow analysis scripts (FlowDataType.Script)
            beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=Datas&query.LSID~doesnotcontain=Flow-AnalysisScript");
            Assert.assertEquals("Expected all experiment Datas to be deleted", 0, table.getDataRowCount());
        }
    }

    // if we aren't already on the Flow Dashboard, try to get there.
    protected void goToFlowDashboard()
    {
        String title = selenium.getTitle();
        if (!title.startsWith("Flow Dashboard: "))
        {
            // All flow pages have a link back to the Flow Dashboard
            if (isLinkPresentWithText("Flow Dashboard"))
            {
                clickLinkWithText("Flow Dashboard");
            }
            else
            {
                // If we are elsewhere, get back to the current test folder
                goToFolder();
            }
        }
    }

    protected void goToFolder()
    {
        goToFolder(getProjectName(), getFolderName());
    }

    protected void goToFolder(String... folderPath)
    {
        for (String folderName : folderPath)
            clickLinkWithText(folderName);
    }


    protected void createQuery(String container, String name, String sql, String xml, boolean inheritable)
    {
        super.createQuery(container, name, "flow", sql, xml, inheritable);
        goToSchemaBrowser();
    }

    protected void uploadSampleDescriptions(String sampleFilePath, String [] idCols, String[] keywordCols)
    {
        log("** Uploading sample set");
        goToFlowDashboard();
        clickLinkWithText("Upload Sample Descriptions");
        setFormElement("data", getFileContents(sampleFilePath));
        for (int i = 0; i < idCols.length; i++)
            selectOptionByText("idColumn" + (i+1), idCols[i]);
        submit();

        log("** Join sample set with FCSFile keywords");
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Define sample description join fields");
        for (int i = 0; i < idCols.length; i++)
            selectOptionByText(Locator.name("ff_samplePropertyURI", i), idCols[i]);
        for (int i = 0; i < keywordCols.length; i++)
            selectOptionByText(Locator.name("ff_dataField", i), keywordCols[i]);
        submit();
    }

    protected void setProtocolMetadata(String participantColumn, String dateColumn, String visitColumn, boolean setBackground)
    {
        log("** Specify ICS metadata");
        goToFlowDashboard();
        clickLinkWithText("Other settings");
        clickLinkWithText("Edit ICS Metadata");

        // specify PTID and Visit/Date columns
        selectOptionByText("ff_participantColumn", participantColumn);
        if (dateColumn != null)
            selectOptionByText("ff_dateColumn", dateColumn);
        if (visitColumn != null)
            selectOptionByText("ff_visitColumn", visitColumn);

        if (setBackground)
        {
            // specify forground-background match columns
            assertFormElementEquals(Locator.name("ff_matchColumn", 0), "Run");
            selectOptionByText(Locator.name("ff_matchColumn", 1), "Sample Sample Order");

            // specify background values
            selectOptionByText(Locator.name("ff_backgroundFilterField", 0), "Sample Stim");
            assertFormElementEquals(Locator.name("ff_backgroundFilterOp", 0), "eq");
            setFormElement(Locator.name("ff_backgroundFilterValue", 0), "Neg Cont");
        }

        submit();
    }

    protected void importExternalAnalysis(String containerPath, String analysisZipPath)
    {
        goToFlowDashboard();
        clickLinkContainingText("FCS files to be imported");
        selectPipelineFileAndImportAction(analysisZipPath, "Import External Analysis");

        waitForPipeline(containerPath);
    }

    protected void importAnalysis(String containerPath, String workspacePath, String fcsPath, boolean existingKeywordRun, String analysisName, boolean existingAnalysisFolder, boolean viaPipeline)
    {
        ImportAnalysisOptions options = new ImportAnalysisOptions(containerPath, workspacePath, fcsPath, existingKeywordRun, analysisName, existingAnalysisFolder, viaPipeline);
        importAnalysis(options);
    }

    protected void importAnalysis(ImportAnalysisOptions options)
    {
        if (options.isViaPipeline())
        {
            importAnalysis_viaPipeline(options.getWorkspacePath());
        }
        else
        {
            importAnalysis_begin(options.getContainerPath());
            importAnalysis_uploadWorkspace(options.getContainerPath(), options.getWorkspacePath());
        }
        importAnalysis_FCSFiles(options.getContainerPath(), options.getFcsPath(), options.isExistingKeywordRun());
        if (options.getFcsPath() == null)
        {
            assertFormElementEquals(Locator.name("existingKeywordRunId"), String.valueOf(0));
            assertFormElementEquals(Locator.name("runFilePathRoot"), "");
        }
        else
        {
            if (options.isExistingKeywordRun())
                assertFormElementNotEquals(Locator.name("existingKeywordRunId"), String.valueOf(0));
            else
                assertFormElementEquals(Locator.name("existingKeywordRunId"), String.valueOf(0));
            assertFormElementNotEquals(Locator.name("runFilePathRoot"), "");
        }

        // Analysis engine can only be selected when no FCS files are associated with the run
        // or if the workspace is not a PC workspace (.wsp)
        if ((options.getFcsPath() != null || options.isExistingKeywordRun()) && !options.getWorkspacePath().endsWith(".wsp"))
            importAnalysis_analysisEngine(options.getContainerPath(), options.getAnalysisEngine());

        importAnalysis_analysisOptions(options.getContainerPath(), options.getImportGroupNames(), options.isREngineNormalization(), options.getREngineNormalizationReference(), options.getREngineNormalizationSubsets(), options.getREngineNormalizationParameters());

        importAnalysis_analysisFolder(options.getContainerPath(), options.getAnalysisName(), options.isExistingAnalysisFolder());

        importAnalysis_confirm(
                options.getContainerPath(),
                options.getWorkspacePath(),
                options.getFcsPath(),
                options.isExistingKeywordRun(),
                options.getAnalysisEngine(),
                options.getImportGroupNames(),
                options.isREngineNormalization(),
                options.getREngineNormalizationReference(),
                options.getREngineNormalizationSubsets(),
                options.getREngineNormalizationParameters(),
                options.getAnalysisName(),
                options.isExistingAnalysisFolder());

        importAnalysis_checkErrors(options.getExpectedErrors());
    }

    protected void importAnalysis_viaPipeline(String workspacePath)
    {
        log("browse pipeline to begin import analysis wizard");
        goToFlowDashboard();
        clickLinkContainingText("FCS files to be imported");
        _extHelper.selectFileBrowserItem(workspacePath);

        selectImportDataAction("Import FlowJo Workspace");
    }

    protected void importAnalysis_begin(String containerPath)
    {
        log("begin import analysis wizard");
        goToFlowDashboard();
        clickLinkWithText("Import FlowJo Workspace Analysis");
        assertTitleEquals("Import Analysis: Select Workspace: " + containerPath);
    }

    protected void importAnalysis_uploadWorkspace(String containerPath, String workspacePath)
    {
        assertTitleEquals("Import Analysis: Select Workspace: " + containerPath);
        _extHelper.selectFileBrowserItem(workspacePath);
        clickButton("Next");
    }

    protected void importAnalysis_FCSFiles(String containerPath, String fcsPath, boolean existingRun)
    {
        sleep(100); // Avoid race condition for form
        assertTitleEquals("Import Analysis: Select FCS Files: " + containerPath);
        _extHelper.waitForFileGridReady();
        if (existingRun)
        {
            selectOptionByText("existingKeywordRunId", fcsPath);
            waitFor(new Checker()
            {
                public boolean check()
                {
                    return isChecked(Locator.id("previousFCSFiles"));
                }
            }, "Previous FCS files radio button should be selected.", WAIT_FOR_JAVASCRIPT);
        }
        else if (fcsPath != null)
        {
            _extHelper.selectFileBrowserItem(fcsPath);
            waitFor(new Checker()
            {
                public boolean check()
                {
                    return isChecked(Locator.id("browseFCSFiles"));
                }
            }, "Browse for FCS files radio button should be selected.", WAIT_FOR_JAVASCRIPT);
        }
        else
        {
            setFormElement(Locator.name("runFilePathRoot"), "");
            clickRadioButtonById("noFCSFiles");
        }
        clickButton("Next");
    }

    protected void importAnalysis_analysisEngine(String containerPath, String engineId)
    {
        assertTitleEquals("Import Analysis: Analysis Engine: " + containerPath);
        waitForElement(Locator.id(engineId), defaultWaitForPage);
        clickRadioButtonById(engineId);
        clickButton("Next");
    }

    protected void importAnalysis_analysisOptions(String containerPath, List<String> groupNames, boolean rEngineNormalization, String rEngineNormalizationReference, List<String> rEngineNormalizationSubsets, List<String> rEngineNormalizationParameters)
    {
        assertTitleEquals("Import Analysis: Analysis Options: " + containerPath);
        if (groupNames != null && groupNames.size() > 0)
        {
            setFormElement(Locator.id("importGroupNames"), StringUtils.join(groupNames, ","));
        }

        // R normalization options only present if rEngine as selected
        if (isElementPresent(Locator.id("rEngineNormalization")))
        {
            log("Setting normalization options");
            assertTextNotPresent("Normalization is current disabled");
            if (rEngineNormalization)
            {
                checkCheckbox("rEngineNormalization");
                if (rEngineNormalizationReference != null)
                {
                    selectOptionByText("rEngineNormalizationReference", rEngineNormalizationReference);
                    Assert.assertEquals(rEngineNormalizationReference, getFormElement("rEngineNormalizationReference"));
                }

                if (rEngineNormalizationSubsets != null)
                    setFormElement("rEngineNormalizationSubsets", StringUtils.join(rEngineNormalizationSubsets, ImportAnalysisOptions.PARAMETER_SEPARATOR));

                if (rEngineNormalizationParameters != null)
                    setFormElement("rEngineNormalizationParameters", StringUtils.join(rEngineNormalizationParameters, ImportAnalysisOptions.PARAMETER_SEPARATOR));
            }
            else
            {
                uncheckCheckbox("rEngineNormalization");
            }
        }
        else
        {
            if (rEngineNormalization)
                Assert.fail("Expected to find R normalization options");
            log("Not setting normalization options");
        }
        clickButton("Next");
    }

    protected void importAnalysis_analysisFolder(String containerPath, String analysisName, boolean existing)
    {
        assertTitleEquals("Import Analysis: Analysis Folder: " + containerPath);
        if (existing)
        {
            selectOptionByText("existingAnalysisId", analysisName);
        }
        else
        {
            setFormElement("newAnalysisName", analysisName);
        }
        clickButton("Next");
    }

    protected void importAnalysis_confirm(String containerPath, String workspacePath,
                                          String fcsPath, boolean existingKeywordRun,
                                          String analysisFolder, boolean existingAnalysisFolder)
    {
        importAnalysis_confirm(containerPath, workspacePath, fcsPath, existingKeywordRun, fcsPath,
                Arrays.asList("All Samples"), false, null, null, null,
                analysisFolder, existingAnalysisFolder);
    }

    protected void importAnalysis_confirm(String containerPath, String workspacePath,
                                          String fcsPath, boolean existingKeywordRun,
                                          String analysisEngine,
                                          List<String> importGroupNames,
                                          boolean rEngineNormalization,
                                          String rEngineNormalizationReference,
                                          List<String> rEngineNormalizationSubsets,
                                          List<String> rEngineNormalizationParameters,
                                          String analysisFolder,
                                          boolean existingAnalysisFolder)
    {
        assertTitleEquals("Import Analysis: Confirm: " + containerPath);

        if (analysisEngine.equals("noEngine"))
            assertTextPresent("Analysis Engine: No analysis engine selected");
        else if (analysisEngine.equals("rEngine"))
            assertTextPresent("Analysis Engine: External R analysis engine");

        if (importGroupNames == null)
            assertTextPresent("Import Groups: All Samples");
        else
            assertTextPresent("Import Groups: " + StringUtils.join(importGroupNames, ","));

        if (rEngineNormalization)
        {
            assertTextPresent("Reference Sample: " + rEngineNormalizationReference);
            assertTextPresent("Normalize Subsets: " + (rEngineNormalizationSubsets == null ? "All subsets" : StringUtils.join(rEngineNormalizationSubsets, ", ")));
            assertTextPresent("Normalize Parameters: " + (rEngineNormalizationParameters == null ? "All parameters" : StringUtils.join(rEngineNormalizationParameters, ", ")));
        }

        if (existingAnalysisFolder)
            assertTextPresent("Existing Analysis Folder: " + analysisFolder);
        else
            assertTextPresent("New Analysis Folder: " + analysisFolder);

        if (existingKeywordRun)
            assertTextNotPresent("Existing FCS File run: none set");

        // XXX: assert fcsPath is present: need to normalize windows path backslashes
        if (fcsPath == null)
            assertTextPresent("FCS File Path: none set");

        assertTextPresent("Workspace: " + workspacePath);

        clickButton("Finish");
        waitForPipeline(containerPath);
        log("finished import analysis wizard");
    }

    protected void importAnalysis_checkErrors(List<String> expectedErrors)
    {
        log("Checking for errors after importing");
        pushLocation();
        if (expectedErrors == null || expectedErrors.isEmpty())
        {
            checkErrors();
        }
        else
        {
            goToFlowDashboard();
            clickLinkContainingText("Show Jobs");
            clickLinkWithText("ERROR");

            for (String errorText : expectedErrors)
                assertTextPresent(errorText);

            int errorCount = countText("ERROR");
            checkExpectedErrors(errorCount);
        }
        popLocation();
    }

    protected static class ImportAnalysisOptions
    {
        public static final String PARAMETER_SEPARATOR = "\ufe50";

        private final String _containerPath;
        private final String _workspacePath;
        private final String _fcsPath;
        private final boolean _existingKeywordRun;
        private final String _analysisEngine;
        private final List<String> _importGroupNames;
        private final boolean _rEngineNormalization;
        private final String _rEngineNormalizationReference;
        private final List<String> _rEngineNormalizationSubsets;
        private final List<String> _rEngineNormalizationParameters;
        private final String _analysisName;
        private final boolean _existingAnalysisFolder;
        private final boolean _viaPipeline;
        private final List<String> _expectedErrors;


        public ImportAnalysisOptions(
                String containerPath,
                String workspacePath,
                String fcsPath,
                boolean existingKeywordRun,
                String analysisName,
                boolean existingAnalysisFolder,
                boolean viaPipeline)
        {
            _containerPath = containerPath;
            _workspacePath = workspacePath;
            _fcsPath = fcsPath;
            _existingKeywordRun = existingKeywordRun;
            _analysisEngine = "noEngine";
            _importGroupNames = Arrays.asList("All Samples");
            _rEngineNormalization = false;
            _rEngineNormalizationReference = null;
            _rEngineNormalizationSubsets = null;
            _rEngineNormalizationParameters = null;
            _analysisName = analysisName;
            _existingAnalysisFolder = existingAnalysisFolder;
            _viaPipeline = viaPipeline;
            _expectedErrors = new ArrayList<String>();
        }

        public ImportAnalysisOptions(
                String containerPath,
                String workspacePath,
                String fcsPath,
                boolean existingKeywordRun,
                String analysisEngine,
                List<String> importGroupNames,
                boolean rEngineNormalization,
                String rEngineNormalizationReference,
                List<String> rEngineNormalizationSubsets,
                List<String> rEngineNormalizationParameters,
                String analysisName,
                boolean existingAnalysisFolder,
                boolean viaPipeline,
                List<String> expectedErrors)
        {
            _containerPath = containerPath;
            _workspacePath = workspacePath;
            _fcsPath = fcsPath;
            _existingKeywordRun = existingKeywordRun;
            _analysisEngine = analysisEngine;
            _importGroupNames = importGroupNames;
            _rEngineNormalization = rEngineNormalization;
            _rEngineNormalizationReference = rEngineNormalizationReference;
            _rEngineNormalizationSubsets = rEngineNormalizationSubsets;
            _rEngineNormalizationParameters = rEngineNormalizationParameters;
            _analysisName = analysisName;
            _existingAnalysisFolder = existingAnalysisFolder;
            _viaPipeline = viaPipeline;
            _expectedErrors = expectedErrors;
        }

        public String getContainerPath()
        {
            return _containerPath;
        }

        public String getWorkspacePath()
        {
            return _workspacePath;
        }

        public String getFcsPath()
        {
            return _fcsPath;
        }

        public boolean isExistingKeywordRun()
        {
            return _existingKeywordRun;
        }

        public String getAnalysisEngine()
        {
            return _analysisEngine;
        }

        public List<String> getImportGroupNames()
        {
            return _importGroupNames;
        }

        public boolean isREngineNormalization()
        {
            return _rEngineNormalization;
        }

        public String getREngineNormalizationReference()
        {
            return _rEngineNormalizationReference;
        }

        public List<String> getREngineNormalizationSubsets()
        {
            return _rEngineNormalizationSubsets;
        }

        public List<String> getREngineNormalizationParameters()
        {
            return _rEngineNormalizationParameters;
        }

        public String getAnalysisName()
        {
            return _analysisName;
        }

        public boolean isExistingAnalysisFolder()
        {
            return _existingAnalysisFolder;
        }

        public boolean isViaPipeline()
        {
            return _viaPipeline;
        }

        public List<String> getExpectedErrors()
        {
            return _expectedErrors;
        }
    }
}
