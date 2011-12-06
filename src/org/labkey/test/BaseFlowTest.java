/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
import org.apache.commons.lang.StringUtils;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;

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

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
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
        while(!isTextPresent("There are no running or pending flow jobs") && System.currentTimeMillis() - startTime < 300000)
        {
            sleep(2000);
            refresh();
        }
        popLocation(longWaitForPage);
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        deletePipelineWorkDirectory();
        try
        {
            beginAt("/admin/begin.view");
            clickLinkWithText("flow cytometry");
            setFormElement("workingDirectory", "");
            clickNavButton("update");
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
        clickNavButton("update");
        assertTextPresent("Path does not exist");
        getPipelineWorkDirectory().mkdir();
        setFormElement("workingDirectory", getPipelineWorkDirectory().toString());

        boolean normalizationEnabled = requiresNormalization();
        if (normalizationEnabled)
            checkCheckbox(Locator.id("normalizationEnabled"));
        else
            uncheckCheckbox(Locator.id("normalizationEnabled"));

        clickNavButton("update");
        assertTextNotPresent("Path does not exist");
        if (normalizationEnabled)
        {
            assertTextNotPresent("The R script engine is not available.");
            assertTextNotPresent("Please install the flowWorkspace R library");
        }

        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, getFolderName(), "Flow", null);

        setFlowPipelineRoot(getLabKeyRoot() + PIPELINE_PATH);
    }

    protected void after() throws Exception
    {
        deleteAllRuns();
    }

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
            table.checkAllOnPage();
            selenium.chooseOkOnNextConfirmation();
            clickButton("Delete", 0);
            assertEquals(selenium.getConfirmation(), "Are you sure you want to delete the selected rows?");
            waitForPageToLoad();
            assertEquals("Expected all experiment Runs to be deleted", 0, table.getDataRowCount());

            beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=DataInputs");
            assertEquals("Expected all experiment DataInputs to be deleted", 0, table.getDataRowCount());

            beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=Datas");
            assertEquals("Expected all experiment Datas to be deleted", 0, table.getDataRowCount());
        }
    }

    protected String getFolderName()
    {
        return getClass().getSimpleName();
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
                clickLinkWithText(getProjectName());
                clickLinkWithText(getFolderName());
            }
        }
    }


    protected void gotoProjectQuery()
    {
        beginAt("/query/" + PROJECT_NAME + "/begin.view?schemaName=flow");
    }

    protected void createQuery(String container, String name, String sql, String xml, boolean inheritable)
    {
        String queryURL = "query/" + container + "/begin.view?schemaName=flow";
        beginAt(queryURL);
        createNewQuery("flow");
        setFormElement("ff_newQueryName", name);
        clickNavButton("Create and Edit Source");
//        toggleSQLQueryEditor();
        setQueryEditorValue("queryText", sql);
//        setFormElement("queryText", sql);
        ExtHelper.clickExtTab(this, "XML Metadata");
        setQueryEditorValue("metadataText", xml);
//        toggleMetadataQueryEditor();
//        setFormElement("metadataText", xml);
        clickButton("Save", 0);
        waitForText("Saved", WAIT_FOR_JAVASCRIPT);
        if (inheritable)
        {
            beginAt(queryURL);
            editQueryProperties("flow", name);
            selectOptionByValue("inheritable", "true");
            submit();
        }
        beginAt(queryURL);
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
        if (options.getFcsPath() != null || options.isExistingKeywordRun())
            importAnalysis_analysisEngine(options.getContainerPath(), options.getAnalysisEngine());

        importAnalysis_analysisOptions(options.getContainerPath(), options.getImportGroupNames(), options.isREngineNormalization(), options.getREngineNormalizationReference(), options.getREngineNormalizationParameters());

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

        if (workspacePath.startsWith("/"))
            workspacePath = workspacePath.substring(1);
        String[] parts = workspacePath.split("/");

        for (int i = 0; i < parts.length; i++)
        {
            if (i == parts.length - 1)
            {
                // workaround for import button enable/disable state bug in pipeline browser
                ExtHelper.clickFileBrowserFileCheckbox(this, parts[i]);
                ExtHelper.clickFileBrowserFileCheckbox(this, parts[i]);
                ExtHelper.clickFileBrowserFileCheckbox(this, parts[i]);
            }
            else
                waitAndClick(Locator.fileTreeByName(parts[i]));
        }

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
        ExtHelper.selectFileBrowserItem(this, workspacePath);
        clickNavButton("Next");
    }

    protected void importAnalysis_FCSFiles(String containerPath, String fcsPath, boolean existingRun)
    {
        assertTitleEquals("Import Analysis: Select FCS Files: " + containerPath);
        if (existingRun)
        {
            selectOptionByText("existingKeywordRunId", fcsPath);
        }
        else if (fcsPath != null)
        {
            ExtHelper.selectFileBrowserItem(this, fcsPath);
        }
        else
        {
            // XXX: clicking the radio button doesn't clear selection for some reason
            setFormElement(Locator.name("runFilePathRoot"), "");
            clickRadioButtonById("noFCSFiles");
        }
        clickNavButton("Next");
    }

    protected void importAnalysis_analysisEngine(String containerPath, String engineId)
    {
        assertTitleEquals("Import Analysis: Analysis Engine: " + containerPath);
        clickRadioButtonById(engineId);
        clickNavButton("Next");
    }

    protected void importAnalysis_analysisOptions(String containerPath, List<String> groupNames, boolean rEngineNormalization, String rEngineNormalizationReference, String rEngineNormalizationParameters)
    {
        assertTitleEquals("Import Analysis: Analysis Options: " + containerPath);
        if (groupNames != null && groupNames.size() > 0)
        {
            setFormElement("importGroupNames", StringUtils.join(groupNames, ","));
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
                    assertEquals(rEngineNormalizationReference, getFormElement("rEngineNormalizationReference"));
                }

                if (rEngineNormalizationParameters != null)
                    setFormElement("rEngineNormalizationParameters", rEngineNormalizationParameters);
            }
            else
            {
                uncheckCheckbox("rEngineNormalization");
            }
        }
        else
        {
            if (rEngineNormalization)
                fail("Expected to find R normalization options");
            log("Not setting normalization options");
        }
        clickNavButton("Next");
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
        clickNavButton("Next");
    }

    protected void importAnalysis_confirm(String containerPath, String workspacePath,
                                          String fcsPath, boolean existingKeywordRun,
                                          String analysisFolder, boolean existingAnalysisFolder)
    {
        importAnalysis_confirm(containerPath, workspacePath, fcsPath, existingKeywordRun, fcsPath,
                Arrays.asList("All Samples"), false, null, null,
                analysisFolder, existingAnalysisFolder);
    }

    protected void importAnalysis_confirm(String containerPath, String workspacePath,
                                          String fcsPath, boolean existingKeywordRun,
                                          String analysisEngine,
                                          List<String> importGroupNames,
                                          boolean rEngineNormalization,
                                          String rEngineNormalizationReference,
                                          String rEngineNormalizationParameters,
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
            assertTextPresent("Normalize Parameters: " + (rEngineNormalizationParameters == null ? "All parameters" : rEngineNormalizationParameters));
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

        clickNavButton("Finish");
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
        private final String _containerPath;
        private final String _workspacePath;
        private final String _fcsPath;
        private final boolean _existingKeywordRun;
        private final String _analysisEngine;
        private final List<String> _importGroupNames;
        private final boolean _rEngineNormalization;
        private final String _rEngineNormalizationReference;
        private final String _rEngineNormalizationParameters;
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
                String rEngineNormalizationParameters,
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

        public String getREngineNormalizationParameters()
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
