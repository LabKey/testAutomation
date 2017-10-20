/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

abstract public class BaseFlowTest extends BaseWebDriverTest
{
    protected static final File PIPELINE_PATH = TestFileUtils.getSampleData("flow");

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("flow");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "Flow Verify Project";
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

    @BeforeClass
    public static void setupProject()
    {
        BaseFlowTest initTest = (BaseFlowTest)getCurrentTest();
        initTest.init();
    }

    private void init()
    {
        beginAt("/admin/begin.view");
        goToAdminConsole().goToAdminConsoleLinksSection();
        clickAndWait(Locator.linkWithText("flow cytometry"));
        getPipelineWorkDirectory().mkdir();
        setFormElement(Locator.id("workingDirectory"), getPipelineWorkDirectory().toString());

        boolean normalizationEnabled = requiresNormalization();
        if (normalizationEnabled)
            checkCheckbox(Locator.id("normalizationEnabled"));
        else
            uncheckCheckbox(Locator.id("normalizationEnabled"));

        clickButton("update");
        assertTextNotPresent("Path does not exist");
        if (normalizationEnabled)
        {
            assertTextNotPresent("The R script engine is not available.", "Please install the flowWorkspace R library");
        }

        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Flow", null);

        setPipelineRoot(PIPELINE_PATH.getAbsolutePath());
    }

    //need not fill all three, but must be the same length.  If you wish to skip a field, set it to an empty string,
    // or the default in the case of op
    public void setFlowFilter(String[] fields, String[] ops, String[] values)
    {
        goToFlowDashboard();
        waitForElement(Locator.linkWithText("Other settings"));
        clickAndWait(Locator.linkWithText("Other settings"));
        clickAndWait(Locator.linkWithText("Edit FCS Analysis Filter"));

        for(int i=0; i<fields.length; i++)
        {
            selectOptionByValue(Locator.xpath("//select[@name='ff_field']").index(i),  fields[i]);
            selectOptionByValue(Locator.xpath("//select[@name='ff_op']").index(i), ops[i]);
            setFormElement(Locator.xpath("//input[@name='ff_value']").index(i), values[i]);
        }
        clickButton("Set filter");
    }

    protected File getPipelineWorkDirectory()
    {
        return new File(TestFileUtils.getLabKeyRoot() + "/sampledata/flow/work");
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

        // Only show running jobs (not complete, cancelled, or error)
        beginAt(containerPath + "/pipeline-status-showList.view?StatusFiles.Status~notin=COMPLETE%3BCANCELLED%3BERROR");
        waitForRunningPipelineJobs(MAX_WAIT_SECONDS * 1000);

        popLocation(longWaitForPage);
    }

    protected void doCleanup(boolean afterTest)
    {
        deleteAllRuns();
        _containerHelper.deleteProject(getProjectName(), afterTest);
        try
        {
            beginAt("/admin/begin.view");
            clickAndWait(Locator.linkWithText("flow cytometry"));
            setFormElement(Locator.id("workingDirectory"), "");
            clickButton("update");
        }
        catch (WebDriverException ignored) {}
        deletePipelineWorkDirectory();
    }

    protected boolean requiresNormalization()
    {
        return false;
    }

    //Issue 12597: Need to delete exp.data objects when deleting a flow run
    protected void deleteAllRuns()
    {
        if (!isElementPresent(Locator.linkWithText(getProjectName())))
            goToHome();
        if (!isElementPresent(Locator.linkWithText(getProjectName())))
            return;

        clickProject(getProjectName());
        if (!isElementPresent(Locator.linkWithText(getFolderName())))
            return;

        clickFolder(getFolderName());

        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=Runs");
        DataRegionTable table = new DataRegionTable("query", this);
        if (table.getDataRowCount() > 0)
        {
            // Delete all runs
            table.checkAllOnPage();
            doAndWaitForPageToLoad(() ->
            {
                clickButton("Delete", 0);
                assertAlertContains("Are you sure you want to delete the selected row");
            });
            assertEquals("Expected all experiment Runs to be deleted", 0, table.getDataRowCount());

            // Check all DataInputs were deleted
            beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=DataInputs");
            assertEquals("Expected all experiment DataInputs to be deleted", 0, table.getDataRowCount());

            // Check all Datas were deleted except for flow analysis scripts (FlowDataType.Script)
            beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=Datas&query.LSID~doesnotcontain=Flow-AnalysisScript");
            assertEquals("Expected all experiment Datas to be deleted", 0, table.getDataRowCount());
        }
    }

    // if we aren't already on the Flow Dashboard, try to get there.
    protected void goToFlowDashboard()
    {
        String title = getDriver().getTitle();
        if (!title.startsWith("Flow Dashboard: "))
        {
            // All flow pages have a link back to the Flow Dashboard
            if (isElementPresent(Locator.linkWithText("Flow Dashboard")))
            {
                clickAndWait(Locator.linkWithText("Flow Dashboard"));
            }
            else
            {
                navigateToFolder(getProjectName(), getFolderName());
            }
        }
        BodyWebPart.find(getDriver(), "Flow Experiment Management", 0);
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
        clickAndWait(Locator.linkWithText("Upload Sample Descriptions"));
        setFormElement(Locator.name("data"), TestFileUtils.getFileContents(sampleFilePath));
        click(Locator.name("idColumn1")); //need to trigger an event to populate the columns
        for (int i = 0; i < idCols.length; i++)
            selectOptionByText(Locator.name("idColumn" + (i+1)), idCols[i]);
        clickButton("Submit");

        log("** Join sample set with FCSFile keywords");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Define sample description join fields"));
        for (int i = 0; i < idCols.length; i++)
            selectOptionByText(Locator.name("ff_samplePropertyURI").index(i), idCols[i]);
        for (int i = 0; i < keywordCols.length; i++)
            selectOptionByText(Locator.name("ff_dataField").index(i), keywordCols[i]);
        clickButton("update");
    }

    protected void setProtocolMetadata(String specimenIdColumn, String participantColumn, String dateColumn, String visitColumn, boolean setBackground)
    {
        log("** Specify ICS metadata");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Other settings"));
        clickAndWait(Locator.linkWithText("Edit ICS Metadata"));

        // specify PTID and Visit/Date columns
        if (specimenIdColumn != null)
            selectOptionByText(Locator.name("ff_specimenIdColumn"), specimenIdColumn);
        if (participantColumn != null)
            selectOptionByText(Locator.name("ff_participantColumn"), participantColumn);
        if (dateColumn != null)
            selectOptionByText(Locator.name("ff_dateColumn"), dateColumn);
        if (visitColumn != null)
            selectOptionByText(Locator.name("ff_visitColumn"), visitColumn);

        if (setBackground)
        {
            // specify forground-background match columns
            assertFormElementEquals(Locator.name("ff_matchColumn").index(0), "Run");
            selectOptionByText(Locator.name("ff_matchColumn").index(1), "Sample Sample Order");

            // specify background values
            selectOptionByText(Locator.name("ff_backgroundFilterField").index(0), "Sample Stim");
            assertFormElementEquals(Locator.name("ff_backgroundFilterOp").index(0), "eq");
            setFormElement(Locator.name("ff_backgroundFilterValue").index(0), "Neg Cont");
        }

        clickButton("Set ICS Metadata");
    }


    protected void importFCSFiles()
    {
        waitAndClickAndWait(Locator.linkWithText("Browse for FCS files to be imported"));

        _fileBrowserHelper.selectFileBrowserItem("flowjoquery/microFCS");
        _fileBrowserHelper.selectImportDataAction("Import Directory of FCS Files");
        clickButton("Import Selected Runs", defaultWaitForPage * 2);
        waitForPipeline(getContainerPath());
    }

    protected void importExternalAnalysis(String containerPath, String analysisZipPath)
    {
        goToFlowDashboard();
        clickAndWait(Locator.linkContainingText("FCS files to be imported"));
        _fileBrowserHelper.importFile(analysisZipPath, "Import External Analysis");

        importAnalysis_selectFCSFiles(containerPath, SelectFCSFileOption.None, null);
        importAnalysis_reviewSamples(containerPath, false, null, null);
        String analysisFolder = new File(analysisZipPath).getName();
        importAnalysis_analysisFolder(containerPath, analysisFolder, false);
        // UNDONE: use importAnalysis_confim step
        clickButton("Finish");

        waitForPipeline(containerPath);
    }

    protected void importAnalysis(String containerPath, String workspacePath, SelectFCSFileOption selectFCSFilesOption, List<String> keywordDirs, String analysisName, boolean existingAnalysisFolder, boolean viaPipeline)
    {
        ImportAnalysisOptions options = new ImportAnalysisOptions(containerPath, workspacePath, selectFCSFilesOption, keywordDirs, analysisName, existingAnalysisFolder, viaPipeline);
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
        importAnalysis_selectFCSFiles(options.getContainerPath(), options.getSelectFCSFilesOption(), options.getKeywordDirs());
        assertFormElementEquals(Locator.name("selectFCSFilesOption"), options.getSelectFCSFilesOption().name());

        boolean resolving = options.getSelectFCSFilesOption() == SelectFCSFileOption.Previous;
        importAnalysis_reviewSamples(options.getContainerPath(), resolving, options.getSelectedGroupNames(), options.getSelectedSampleIds());

        // R Analysis engine can only be selected when using Mac FlowJo workspaces
        if (!options.getWorkspacePath().endsWith(".wsp"))
            importAnalysis_analysisEngine(options.getContainerPath(), options.getAnalysisEngine());

        // Analysis option page only shown when using R normalization.
        if (options.getAnalysisEngine() == AnalysisEngine.R && options.isREngineNormalization())
            importAnalysis_analysisOptions(options.getContainerPath(), options.isREngineNormalization(), options.getREngineNormalizationReference(), options.getREngineNormalizationSubsets(), options.getREngineNormalizationParameters());

        importAnalysis_analysisFolder(options.getContainerPath(), options.getAnalysisName(), options.isExistingAnalysisFolder());

        importAnalysis_confirm(
                options.getContainerPath(),
                options.getWorkspacePath(),
                options.getSelectFCSFilesOption(),
                options.getKeywordDirs(),
                options.getSelectedGroupNames(),
                options.getSelectedSampleIds(),
                options.getAnalysisEngine(),
                options.isREngineNormalization(),
                options.getREngineNormalizationReference(),
                options.getREngineNormalizationSubsets(),
                options.getREngineNormalizationParameters(),
                options.getAnalysisName(),
                options.isExistingAnalysisFolder());

        importAnalysis_checkErrors(options.getExpectedErrors());
    }

    @LogMethod
    protected void importAnalysis_viaPipeline(String workspacePath)
    {
        log("browse pipeline to begin import analysis wizard");
        goToFlowDashboard();
        clickAndWait(Locator.linkContainingText("FCS files to be imported"));
        _fileBrowserHelper.selectFileBrowserItem(workspacePath);

        _fileBrowserHelper.selectImportDataAction("Import FlowJo Workspace");
    }

    @LogMethod
    protected void importAnalysis_begin(String containerPath)
    {
        log("begin import analysis wizard");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Import FlowJo Workspace Analysis"));
        assertTitleEquals("Import Analysis: Select Analysis: " + containerPath);
    }

    @LogMethod
    protected void importAnalysis_uploadWorkspace(String containerPath, String workspacePath)
    {
        assertTitleEquals("Import Analysis: Select Analysis: " + containerPath);
        _fileBrowserHelper.selectFileBrowserItem(workspacePath);
        clickButton("Next");
    }

    @LogMethod
    protected void importAnalysis_selectFCSFiles(String containerPath, final SelectFCSFileOption selectFCSFilesOption, List<String> keywordDirs)
    {
        _ext4Helper.waitForOnReady();
        if (isChecked(Locator.id(SelectFCSFileOption.Browse.name())))
            _fileBrowserHelper.waitForFileGridReady();

        assertTitleEquals("Import Analysis: Select FCS Files: " + containerPath);
        switch (selectFCSFilesOption)
        {
            case None:
                checkRadioButton(Locator.radioButtonById("None"));
                break;

            case Included:
                throw new UnsupportedOperationException("FCS files 'Included' option not yet implemented");
                //clickRadioButtonById("Included");

            case Previous:
                checkRadioButton(Locator.radioButtonById("Previous"));
                break;

            case Browse:
                checkRadioButton(Locator.radioButtonById("Browse"));
                // UNDONE: Currently, only one file path supported
                _fileBrowserHelper.selectFileBrowserItem(keywordDirs.get(0));
                break;

            default:
                throw new IllegalArgumentException("Unknown method for selecting FCS files: " + selectFCSFilesOption);
        }
        waitFor(() -> isChecked(Locator.id(selectFCSFilesOption.name())), "selectFCSFilesOption", 2000);
        clickButton("Next");
    }

    @LogMethod
    protected void importAnalysis_reviewSamples(String containerPath, boolean resolving, List<String> selectedGroupNames, List<String> selectedSampleIds)
    {
        assertTitleEquals("Import Analysis: Review Samples: " + containerPath);

        if (resolving)
        {
            // UNDONE: Test resolving files
            assertTextPresent("Matched");
        }

        if (selectedGroupNames != null && selectedGroupNames.size() > 0)
        {
            selectOptionByValue(Locator.id("importGroupNames"), StringUtils.join(selectedGroupNames, ","));
            fireEvent(Locator.id("importGroupNames"), SeleniumEvent.change); // TODO: Workaround for reselection not changing checkboxes
        }
        else if (selectedSampleIds != null && selectedSampleIds.size() > 0)
        {
            // UNDONE: Select individual rows for import
        }

        clickButton("Next");
    }

    @LogMethod
    protected void importAnalysis_analysisEngine(String containerPath, AnalysisEngine engine)
    {
        assertTitleEquals("Import Analysis: Analysis Engine: " + containerPath);
        waitForElement(Locator.id(engine.name()), defaultWaitForPage);
        click(Locator.radioButtonById(engine.name()));
        clickButton("Next");
    }

    @LogMethod
    protected void importAnalysis_analysisOptions(String containerPath, boolean rEngineNormalization, String rEngineNormalizationReference, List<String> rEngineNormalizationSubsets, List<String> rEngineNormalizationParameters)
    {
        assertTitleEquals("Import Analysis: Analysis Options: " + containerPath);

        // R normalization options only present if rEngine as selected
        if (isElementPresent(Locator.id("rEngineNormalization")))
        {
            log("Setting normalization options");
            assertTextNotPresent("Normalization is current disabled");
            if (rEngineNormalization)
            {
                checkCheckbox(Locator.checkboxByName("rEngineNormalization"));
                if (rEngineNormalizationReference != null)
                {
                    selectOptionByText(Locator.id("rEngineNormalizationReference"), rEngineNormalizationReference);
                    String formValue = getFormElement(Locator.id("rEngineNormalizationReference"));
                    assertEquals(rEngineNormalizationReference, getText(Locator.xpath("id('rEngineNormalizationReference')/option[@value='" + formValue + "']")));
                }

                if (rEngineNormalizationSubsets != null)
                    setFormElement(Locator.id("rEngineNormalizationSubsets"), StringUtils.join(rEngineNormalizationSubsets, ImportAnalysisOptions.PARAMETER_SEPARATOR));

                if (rEngineNormalizationParameters != null)
                    setFormElement(Locator.id("rEngineNormalizationParameters"), StringUtils.join(rEngineNormalizationParameters, ImportAnalysisOptions.PARAMETER_SEPARATOR));
            }
            else
            {
                uncheckCheckbox(Locator.checkboxByName("rEngineNormalization"));
            }
        }
        else
        {
            if (rEngineNormalization)
                fail("Expected to find R normalization options");
            log("Not setting normalization options");
        }
        clickButton("Next");
    }

    @LogMethod
    protected void importAnalysis_analysisFolder(String containerPath, String analysisName, boolean existing)
    {
        assertTitleEquals("Import Analysis: Analysis Folder: " + containerPath);
        if (existing)
        {
            selectOptionByText(Locator.name("existingAnalysisId"), analysisName);
        }
        else
        {
            setFormElement(Locator.name("newAnalysisName"), analysisName);
        }
        clickButton("Next");
    }

    protected void importAnalysis_confirm(String containerPath, String workspacePath,
                                          SelectFCSFileOption selectFCSFilesOption, List<String> keywordDirs,
                                          AnalysisEngine analysisEngine,
                                          String analysisFolder, boolean existingAnalysisFolder)
    {
        importAnalysis_confirm(containerPath, workspacePath,
                selectFCSFilesOption, keywordDirs,
                Arrays.asList("All Samples"),
                null,
                analysisEngine,
                false, null, null, null,
                analysisFolder, existingAnalysisFolder);
    }

    @LogMethod
    protected void importAnalysis_confirm(String containerPath, String workspacePath,
                                          SelectFCSFileOption selectFCSFilesOption,
                                          List<String> keywordDirs,
                                          List<String> selectedGroupNames,
                                          List<String> selectedSampleIds,
                                          AnalysisEngine analysisEngine,
                                          boolean rEngineNormalization,
                                          String rEngineNormalizationReference,
                                          List<String> rEngineNormalizationSubsets,
                                          List<String> rEngineNormalizationParameters,
                                          String analysisFolder,
                                          boolean existingAnalysisFolder)
    {
        assertTitleEquals("Import Analysis: Confirm: " + containerPath);

        assertElementPresent(Locator.tag("li").startsWith("FlowJo ").containing("Workspace: " + workspacePath));

        switch (analysisEngine)
        {
            case FlowJoWorkspace:
                assertElementPresent(Locator.tag("li").withText("Analysis Engine: No analysis engine selected"));
                break;
            case R:
                assertElementPresent(Locator.tag("li").withText("Analysis Engine: External R analysis engine with normalization"));
                break;
        }

        if (rEngineNormalization)
        {
            WebElement normalizationOptions = Locator.tag("li").startsWith("Normalization Options:").findElement(getDriver());
            String normalizationOptionsText = normalizationOptions.getText();
            assertTrue("Wrong Refernce Sample", normalizationOptionsText.contains("Reference Sample: " + rEngineNormalizationReference));
            assertTrue("Wrong Normalize Subsets", normalizationOptionsText.contains("Normalize Subsets: " + (rEngineNormalizationSubsets == null ? "All subsets" : StringUtils.join(rEngineNormalizationSubsets, ", "))));
            assertTrue("Wrong Normalize Parameters", normalizationOptionsText.contains("Normalize Parameters: " + (rEngineNormalizationParameters == null ? "All parameters" : StringUtils.join(rEngineNormalizationParameters, ", "))));
        }

        if (existingAnalysisFolder)
            assertElementPresent(Locator.tag("li").withText("Existing Analysis Folder: " + analysisFolder));
        else
            assertElementPresent(Locator.tag("li").withText("New Analysis Folder: " + analysisFolder));

        // XXX: assert fcsPath is present: need to normalize windows path backslashes
        if (selectFCSFilesOption == SelectFCSFileOption.Browse && keywordDirs == null)
            assertElementPresent(Locator.tag("li").withText("FCS File Path: none set"));

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
            clickAndWait(Locator.linkContainingText("Show Jobs"));
            clickAndWait(Locator.linkWithText("ERROR"));

            for (String errorText : expectedErrors)
                assertTextPresent(errorText);

            int errorCount = countText("ERROR");
            checkExpectedErrors(errorCount);
        }
        popLocation();
    }

    protected enum SelectFCSFileOption { None, Included, Previous, Browse }
    protected enum AnalysisEngine { FlowJoWorkspace, R }

    protected static class ImportAnalysisOptions
    {
        public static final String PARAMETER_SEPARATOR = "\ufe50";

        private final String _containerPath;
        private final String _workspacePath;
        private SelectFCSFileOption _selectFCSFilesOption;
        private final List<String> _keywordDirs;
        private final List<String> _selectedGroupNames;
        private final List<String> _selectedSampleIds;
        private final AnalysisEngine _analysisEngine;
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
                SelectFCSFileOption selectFCSFilesOption,
                List<String> keywordDirs,
                String analysisName,
                boolean existingAnalysisFolder,
                boolean viaPipeline)
        {
            _containerPath = containerPath;
            _workspacePath = workspacePath;
            _selectFCSFilesOption = selectFCSFilesOption;
            _keywordDirs = keywordDirs;
            _selectedGroupNames = Collections.emptyList();
            _selectedSampleIds = null;
            _analysisEngine = AnalysisEngine.FlowJoWorkspace;
            _rEngineNormalization = false;
            _rEngineNormalizationReference = null;
            _rEngineNormalizationSubsets = null;
            _rEngineNormalizationParameters = null;
            _analysisName = analysisName;
            _existingAnalysisFolder = existingAnalysisFolder;
            _viaPipeline = viaPipeline;
            _expectedErrors = new ArrayList<>();
        }

        public ImportAnalysisOptions(
                String containerPath,
                String workspacePath,
                SelectFCSFileOption selectFCSFilesOption,
                List<String> keywordDirs,
                List<String> selectGroupNames,
                List<String> selectSampleIds,
                AnalysisEngine analysisEngine,
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
            _selectFCSFilesOption = selectFCSFilesOption;
            _keywordDirs = keywordDirs;
            _selectedGroupNames = selectGroupNames;
            _selectedSampleIds = selectSampleIds;
            _analysisEngine = analysisEngine;
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

        public SelectFCSFileOption getSelectFCSFilesOption()
        {
            return _selectFCSFilesOption;
        }
        public List<String> getKeywordDirs()
        {
            return _keywordDirs;
        }

        public List<String> getSelectedGroupNames()
        {
            return _selectedGroupNames;
        }

        public List<String> getSelectedSampleIds()
        {
            return _selectedSampleIds;
        }

        public AnalysisEngine getAnalysisEngine()
        {
            return _analysisEngine;
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
