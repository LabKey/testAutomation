/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.experiment.CreateSampleTypePage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriverException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

abstract public class BaseFlowTest extends BaseWebDriverTest
{
    protected static final File PIPELINE_PATH = TestFileUtils.getSampleData("flow");

    @Override
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
        goToAdminConsole().goToSettingsSection();
        clickAndWait(Locator.linkWithText("flow cytometry"));
        getPipelineWorkDirectory().mkdir();
        setFormElement(Locator.id("workingDirectory"), getPipelineWorkDirectory().toString());

        clickButton("update");
        assertTextNotPresent("Path does not exist");

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
        return new File(TestFileUtils.getSampleData("flow"), "work");
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

    protected void waitForPipelineError(List<String> expectedErrors)
    {
        new PipelineStatusDetailsPage(getDriver()).waitForError(expectedErrors);
    }

    protected void waitForPipelineComplete()
    {
        new PipelineStatusDetailsPage(getDriver()).waitForComplete();

        log("Checking for errors after importing");
        checkErrors();
    }

    @Override
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

    //Issue 12597: Need to delete exp.data objects when deleting a flow run
    protected void deleteAllRuns()
    {
        if (!_containerHelper.doesContainerExist(getContainerPath()))
        {
            return;
        }

        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=exp&query.queryName=Runs");
        DataRegionTable table = new DataRegionTable("query", this);
        if (table.getDataRowCount() > 0)
        {
            // Delete all runs
            table.checkAllOnPage();
            table.deleteSelectedRows();
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

    protected void uploadSampleDescriptions(File sampleFile, Map<String, FieldDefinition.ColumnType> fields, String[] idCols, String[] keywordCols)
    {
        log("** Uploading sample type");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Upload Sample Descriptions"));
        CreateSampleTypePage createPage = new CreateSampleTypePage(getDriver());
        // NOTE: name for the sample type is coming from URL param - 'experiment-createSampleSet.view?name=Samples&nameReadOnly=true'
        StringBuilder nameExpression = new StringBuilder();
        for (String idCol : idCols)
        {
            nameExpression.append("-${").append(idCol).append("}");
        }
        createPage.setNameExpression(nameExpression.toString().substring(1));
        List<FieldDefinition> fieldDefinitions = fields.entrySet().stream()
                    .map(entry -> new FieldDefinition(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

        createPage.addFields(fieldDefinitions);
        createPage.clickSave();

        clickAndWait(Locator.linkWithText("Samples"));
        clickAndWait(Locator.linkWithText("Import More Samples"));
        new ImportDataPage(getDriver())
                .setFile(sampleFile)
                .submit();

        log("** Join sample type with FCSFile keywords");
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
        log("** Specify metadata");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Other settings"));
        clickAndWait(Locator.linkWithText("Edit Metadata"));

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

        clickButton("Set Metadata");
    }


    protected void importFCSFiles()
    {
        waitAndClickAndWait(Locator.linkWithText("Browse for FCS files to be imported"));

        _fileBrowserHelper.selectFileBrowserItem("flowjoquery/microFCS");
        _fileBrowserHelper.selectImportDataAction("Import Directory of FCS Files");
        clickButton("Import Selected Runs", defaultWaitForPage * 2);
        waitForPipelineComplete();
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

        waitForPipelineComplete();
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

        importAnalysis_analysisFolder(options.getContainerPath(), options.getAnalysisName(), options.isExistingAnalysisFolder());

        importAnalysis_confirm(
                options.getContainerPath(),
                options.getWorkspacePath(),
                options.getSelectFCSFilesOption(),
                options.getKeywordDirs(),
                options.getSelectedGroupNames(),
                options.getSelectedSampleIds(),
                options.getAnalysisName(),
                options.isExistingAnalysisFolder(),
                options.getExpectedErrors());
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
    protected void analysisFolder_viewFiles(String containerPath)
    {
        log("examine imported FCS files in container "+ containerPath);
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("2 FCS files"));
        assertTitleEquals("FCSFiles: /Flow Verify Project/FlowImportTest");
    }

    @LogMethod
    protected void importAnalysis_begin(String containerPath)
    {
        log("begin import analysis wizard");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Import FlowJo Workspace Analysis"));
        assertTitleEquals("Import Analysis: Select Workspace: " + containerPath);
    }

    @LogMethod
    protected void importAnalysis_uploadWorkspace(String containerPath, String workspacePath)
    {
        assertTitleEquals("Import Analysis: Select Workspace: " + containerPath);
        _fileBrowserHelper.selectFileBrowserItem(workspacePath);
        clickButton("Next");
    }

    @LogMethod
    protected void importAnalysis_selectFCSFiles(String containerPath, final SelectFCSFileOption selectFCSFilesOption, List<String> keywordDirs)
    {
        _ext4Helper.waitForOnReady();
        if (Locator.id(SelectFCSFileOption.Browse.name()).waitForElement(getDriver(), 5_000).isSelected())
        {
            _fileBrowserHelper.waitForFileGridReady();
        }

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
                                          String analysisFolder, boolean existingAnalysisFolder)
    {
        importAnalysis_confirm(containerPath, workspacePath,
                selectFCSFilesOption, keywordDirs,
                Arrays.asList("All Samples"),
                null,
                analysisFolder, existingAnalysisFolder,
                null);
    }

    @LogMethod
    protected void importAnalysis_confirm(String containerPath, String workspacePath,
                                          SelectFCSFileOption selectFCSFilesOption,
                                          List<String> keywordDirs,
                                          List<String> selectedGroupNames,
                                          List<String> selectedSampleIds,
                                          String analysisFolder,
                                          boolean existingAnalysisFolder,
                                          List<String> expectedErrors)
    {
        assertTitleEquals("Import Analysis: Confirm: " + containerPath);

        assertElementPresent(Locator.tag("li").startsWith("FlowJo ").containing("Workspace: " + workspacePath));

        if (existingAnalysisFolder)
            assertElementPresent(Locator.tag("li").withText("Existing Analysis Folder: " + analysisFolder));
        else
            assertElementPresent(Locator.tag("li").withText("New Analysis Folder: " + analysisFolder));

        // XXX: assert fcsPath is present: need to normalize windows path backslashes
        if (selectFCSFilesOption == SelectFCSFileOption.Browse && keywordDirs == null)
            assertElementPresent(Locator.tag("li").withText("FCS File Path: none set"));

        clickButton("Finish");
        if (expectedErrors == null || expectedErrors.isEmpty())
            waitForPipelineComplete();
        else
            waitForPipelineError(expectedErrors);
        log("finished import analysis wizard");
    }


    protected enum SelectFCSFileOption { None, Included, Previous, Browse }

    protected static class ImportAnalysisOptions
    {
        private final String _containerPath;
        private final String _workspacePath;
        private SelectFCSFileOption _selectFCSFilesOption;
        private final List<String> _keywordDirs;
        private final List<String> _selectedGroupNames;
        private final List<String> _selectedSampleIds;
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
