/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.InferDomainCommand;
import org.labkey.remoteapi.domain.InferDomainResponse;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PerlHelper;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.AbstractDataRegionExportOrSignHelper.XarLsidOutputType.*;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class AssayExportImportTest extends BaseWebDriverTest
{
    private final String ASSAY_PROJECT_FOR_EXPORT_01 = "Assay_Project_For_Export_ByFilesWebPart";
    private final String ASSAY_PROJECT_FOR_IMPORT_01 = "Assay_Project_For_Import_ByFilesWebPart";
    private final String ASSAY_PROJECT_FOR_EXPORT_02 = "Assay_Project_For_Export_ByFile";
    private final String ASSAY_PROJECT_FOR_IMPORT_02 = "Assay_Project_For_Import_ByFile";
    private final String ASSAY_PROJECT_FOR_EXPORT_03 = "Assay_Project_For_Export_ByApi";
    private final String ASSAY_PROJECT_FOR_IMPORT_03 = "Assay_Project_For_Import_ByApi";
    private final String ASSAY_PROJECT_FOR_EXPORT_04 = "Assay_Project_For_Export_Xar";
    private final String ASSAY_PROJECT_FOR_IMPORT_04 = "Assay_Project_For_Import_Xar";

    private final String SIMPLE_ASSAY_FOR_EXPORT = "AssayForExport";

    private static final File SAMPLE_DATA_LOCATION = TestFileUtils.getSampleData("AssayImportExport");

    private final File RUN01_FILE = new File(SAMPLE_DATA_LOCATION, "GenericAssay_Run1.xls");
    private final File RUN02_FILE = new File(SAMPLE_DATA_LOCATION, "GenericAssay_Run2.xls");
    private final File RUN03_FILE = new File(SAMPLE_DATA_LOCATION, "GenericAssay_Run3.xls");
    private final File RUN04_FILE = new File(SAMPLE_DATA_LOCATION, "GenericAssay_Run4.xls");

    private final String RUN01_NAME = "Run01";
    private final String RUN02_NAME = "Run02";
    private final String RUN03_NAME = "Run03";
    private final String RUN04_NAME = "Run04";

    @Override
    protected String getProjectName()
    {
        return "AssayExportImportTest";
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(ASSAY_PROJECT_FOR_EXPORT_01, afterTest);
        _containerHelper.deleteProject(ASSAY_PROJECT_FOR_IMPORT_01, afterTest);
        _containerHelper.deleteProject(ASSAY_PROJECT_FOR_EXPORT_02, afterTest);
        _containerHelper.deleteProject(ASSAY_PROJECT_FOR_IMPORT_02, afterTest);
        _containerHelper.deleteProject(ASSAY_PROJECT_FOR_EXPORT_03, afterTest);
        _containerHelper.deleteProject(ASSAY_PROJECT_FOR_IMPORT_03, afterTest);
        _containerHelper.deleteProject(ASSAY_PROJECT_FOR_EXPORT_04, false);
        _containerHelper.deleteProject(ASSAY_PROJECT_FOR_IMPORT_04, false);
    }

    @BeforeClass
    public static void setupProject()
    {
        AssayExportImportTest init = (AssayExportImportTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        log("Setup project and list module");
        PerlHelper perlHelper = new PerlHelper(this);
        perlHelper.ensurePerlConfig();

    }

    @LogMethod
    private Long createSimpleProjectAndAssay(String projectName, String assayName) throws IOException, CommandException
    {
        final String PERL_SCRIPT = "modifyColumnInAssayRun.pl";

        log("Create a project named: '" + projectName + "' with an assay named: '" + assayName + "'.");

        _containerHelper.createProject(projectName, "Assay");
        goToProjectHome(projectName);

        Connection cn = createDefaultConnection();
        Protocol protocol = new GetProtocolCommand("General").execute(cn, projectName).getProtocol();
        protocol.setName(assayName);
        protocol.setProtocolTransformScripts(List.of(new File(SAMPLE_DATA_LOCATION, PERL_SCRIPT).getAbsolutePath()));
        protocol.setSaveScriptFiles(true);
        protocol.setEditableResults(true);
        protocol.setEditableRuns(true);

        Map<String, Domain> domains = new HashMap<>();
        protocol.getDomains().forEach(domain -> domains.put(domain.getName(), domain));

        Domain batchDomain = domains.get("Batch Fields");
        batchDomain.getFields().add(new FieldDefinition("operatorEmail", ColumnType.String));
        batchDomain.getFields().add(new FieldDefinition("instrument", ColumnType.String)
                .setDescription("The diagnostic test instrument."));

        Domain runDomain = domains.get("Run Fields");
        runDomain.getFields().add(new FieldDefinition("instrumentSetting", ColumnType.Integer)
                .setDescription("The configuration setting on the instrument."));

        InferDomainCommand inferDomainCommand = new InferDomainCommand(RUN01_FILE, "Assay");
        InferDomainResponse inferDomainResponse = inferDomainCommand.execute(cn, projectName);
        List<PropertyDescriptor> inferredResultsFields = inferDomainResponse.getFields().stream()
                .filter(f -> !f.getName().startsWith("column")).toList();

        List<PropertyDescriptor> resultsFields = new ArrayList<>(inferredResultsFields);
        resultsFields.add(new FieldDefinition("adjustedM1", FieldDefinition.ColumnType.Integer));
        domains.get("Data Fields").setFields(resultsFields);

        return new SaveProtocolCommand(protocol).execute(cn, projectName).getProtocol().getProtocolId();
    }

    public void addNewField(String projectName, String assayName, FieldDefinition newField)
    {
        log("Modify the assay design to include a new field named '" + newField.getName() + "'.");

        goToProjectHome(projectName);
        clickAndWait(Locator.linkWithText(assayName));
        click(Locator.linkWithText("Manage assay design"));
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.clickEditAssayDesign();

        assayDesignerPage.expandFieldsPanel("Results").addField(newField);

        assayDesignerPage.clickFinish();
    }

    public void populateAssay(String projectName, String assayName, boolean useFilesWebPart, List<File> runFiles, @Nullable Map<String, String> batchProperties, @Nullable List<Map<String, String>> runProperties)
    {

        goToProjectHome(projectName);

        if(useFilesWebPart)
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());

            portalHelper.addWebPart("Files");

            runFiles.forEach(file -> _fileBrowserHelper.uploadFile(file));

            runFiles.forEach(file -> _fileBrowserHelper.selectFileBrowserItem(file.getName()));

            _fileBrowserHelper.selectImportDataAction(assayName);

        }
        else
        {
            clickAndWait(Locator.linkWithText(assayName));
            waitForElement(Locator.lkButton("Import Data"));
            clickAndWait(Locator.lkButton("Import Data"));
        }

        waitForElement(Locator.tagWithName("select", "targetStudy"));

        if(null != batchProperties)
        {
            batchProperties.keySet().forEach((propertyName)->setFormElement(Locator.input(propertyName), batchProperties.get(propertyName)));
        }

        clickAndWait(Locator.lkButton("Next"));
        waitForElement(Locator.lkButton("Save and Finish"));

        int fileIndex = 0;
        for(Map<String, String> runProperty : runProperties)
        {
            runProperty.keySet().forEach((property)->setFormElement(Locator.name(property), runProperty.get(property)));

            if(!useFilesWebPart)
            {
                click(Locator.radioButtonById("Fileupload"));
                waitForElementToBeVisible(Locator.tagWithAttribute("input", "type", "file"));
                setFormElement(Locator.tagWithAttribute("input", "type", "file"), runFiles.get(fileIndex++));

                if (fileIndex < runProperties.size())
                {
                    clickAndWait(Locator.lkButton("Save and Import Another Run"));
                    waitForElement(Locator.tagWithName("input", "instrumentSetting"));
                }

            }
            else
            {
                if (isElementPresent(Locator.lkButton("Save and Import Another Run")))
                {
                    clickAndWait(Locator.lkButton("Save and Import Another Run"));
                    waitForElement(Locator.tagWithName("input", "instrumentSetting"));
                }
            }
        }

        clickAndWait(Locator.lkButton("Save and Finish"));

        // make sure we end up on the assay runs grid with the expected number of runs
        assertTitleContains(assayName + " Runs");
        DataRegionTable runs = new DataRegionTable("Runs", this.getDriver());
        Assert.assertEquals("Unexpected number of assay runs", runFiles.size(), runs.getDataRowCount());
    }

    private void setFieldValues(String projectName, String assayName, String runId, Map<Integer, Map<String, String>> fieldValues)
    {
        goToProjectHome(projectName);
        clickAndWait(Locator.linkWithText(assayName));
        waitForElement(Locator.linkWithText(runId));
        clickAndWait(Locator.linkWithText(runId));

        DataRegionTable drt;

        for(int rowId : fieldValues.keySet())
        {
            drt = new DataRegionTable("Data", getDriver());
            drt.clickEditRow(rowId);
            waitForElement(Locator.lkButton("Submit"));

            Map<String, String> values = fieldValues.get(rowId);

            for(String fieldName : values.keySet())
            {
                if(fieldName.equals("quf_missingValueMVIndicator"))
                    selectOptionByValue(Locator.tagWithName("select", "quf_missingValueMVIndicator"), values.get(fieldName));
                else
                    setFormElement(Locator.tagWithName("input", fieldName), values.get(fieldName));
            }
            clickAndWait(Locator.lkButton("Submit"));
        }

    }

    private Map<String, List<String>> getRunColumnData(String projectName, String assayName, String runId, List<String> columns)
    {
        Map<String, List<String>> columnValues = new HashMap<>();

        log("Going to assay '" + assayName + "' in project '" + projectName + "' and getting column data for run '" + runId + "'.");
        goToProjectHome(projectName);
        clickAndWait(Locator.linkWithText(assayName));
        waitForElement(Locator.linkWithText(runId));

        clickAndWait(Locator.linkWithText(runId));

        DataRegionTable drt = new DataRegionTable("Data", getDriver());

        columns.forEach((s)->{
            log("Getting the data for column '" + s + "'.");
            columnValues.put(s, drt.getColumnDataAsText(s));
        });

        goToProjectHome(projectName);

        return columnValues;
    }

    private void compareRunColumnsWithExpected(String projectName, String assayName, String runId, Map<String, List<String>> expectedColumns)
    {
        log("Going to assay '" + assayName + "' in project '" + projectName + "' and going to compare values in run '" + runId + "'.");
        goToProjectHome(projectName);
        clickAndWait(Locator.linkWithText(assayName));
        waitForElement(Locator.linkWithText(runId));

        clickAndWait(Locator.linkWithText(runId));

        DataRegionTable drt = new DataRegionTable("Data", getDriver());

        for(String columnName : expectedColumns.keySet())
        {
            log("Getting the data for column '" + columnName + "'.");
            List<String> currentColumn = drt.getColumnDataAsText(columnName);
            checker().verifyEquals("Wrong imported data in column " + columnName, expectedColumns.get(columnName), currentColumn);
        }

        checker().screenShotIfNewError("FailedImportData_" + runId);

        goToProjectHome(projectName);
    }

    @Test
    public void validateImportingFileUsingFilesWebPart() throws Exception
    {
        final String OPERATOR_EMAIL_01 = "john.doe@AssayExportImport.test";
        final String INSTRUMENT_NAME_01 = "ABC Reader";
        final String INSTRUMENT_SETTING_01 = "456";
        final String COMMENT_BASIC_01 = "This is a comment for run where the data was imported by the FileWeb Part. This is for run: ";

        createSimpleProjectAndAssay(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT);

        List<File> runFiles = Arrays.asList(
                RUN01_FILE,
                RUN02_FILE,
                RUN03_FILE,
                RUN04_FILE);

        Map<String, String> batchProperties = new HashMap<>();
        batchProperties.put("operatorEmail", OPERATOR_EMAIL_01);
        batchProperties.put("instrument", INSTRUMENT_NAME_01);

        List<Map<String, String>> runProperties = new ArrayList<>();
        runProperties.add(Maps.of("name", RUN01_NAME, "comments", COMMENT_BASIC_01 + RUN01_NAME, "instrumentSetting", INSTRUMENT_SETTING_01));
        runProperties.add(Maps.of("name", RUN02_NAME, "comments", COMMENT_BASIC_01 + RUN02_NAME, "instrumentSetting", INSTRUMENT_SETTING_01));
        runProperties.add(Maps.of("name", RUN03_NAME, "comments", COMMENT_BASIC_01 + RUN03_NAME, "instrumentSetting", INSTRUMENT_SETTING_01));
        runProperties.add(Maps.of("name", RUN04_NAME, "comments", COMMENT_BASIC_01 + RUN04_NAME, "instrumentSetting", INSTRUMENT_SETTING_01));

        log("Populate the assay '" + SIMPLE_ASSAY_FOR_EXPORT + "' by using files in the Files WebPart.");
        populateAssay(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT, true, runFiles, batchProperties, runProperties);

        log("Add a new field that has a missing value indicator.");
        addNewField(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT, new FieldDefinition("missingValue", FieldDefinition.ColumnType.String).setMvEnabled(true));

        log("Set some of the missing value indicators in run '" + RUN01_NAME + "'.");
        Map<Integer, Map<String, String>> rowFieldValues = new HashMap<>();
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "Q");
        rowFieldValues.put(0, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "N");
        rowFieldValues.put(2, fieldValues);
        setFieldValues(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN01_NAME, rowFieldValues);

        log("Record various column values for run '" + RUN01_NAME + "'.");
        List<String> runColumns = Arrays.asList("missingValue", "adjustedM1", "M2");
        Map<String, List<String>> run01ColumnData = getRunColumnData(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN01_NAME, runColumns);

        log("Go to run '" + RUN04_NAME + "' and update various value in some of it's fields.");
        rowFieldValues = new HashMap<>();
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "N");
        rowFieldValues.put(1, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValue", "This value is here.");
        rowFieldValues.put(3, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "Q");
        fieldValues.put("quf_M2", "12345");
        rowFieldValues.put(4, fieldValues);
        setFieldValues(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN04_NAME, rowFieldValues);

        log("Record various column values for run '" + RUN04_NAME + "'.");
        runColumns = Arrays.asList("missingValue", "adjustedM1", "M1");
        Map<String, List<String>> run04ColumnData = getRunColumnData(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN04_NAME, runColumns);

        log("Now export the run.");

        goToProjectHome(ASSAY_PROJECT_FOR_EXPORT_01);

        goToFolderManagement().goToExportTab();
        new Checkbox(Locator.tagWithText("label", ExportFolderPage.EXPERIMENTS_AND_RUNS).precedingSibling("input")
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)).check();
        new Checkbox(Locator.tagWithText("label", "Files").precedingSibling("input").findElement(getDriver())).check();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        log("Create a simple Assay project as the import target.");
        _containerHelper.createProject(ASSAY_PROJECT_FOR_IMPORT_01, "Assay");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_01);

        log("Import the folder.");
        importFolderFromZip(exportedFolderFile);

        log("Validate that the data has been imported as expected.");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_01);
        clickAndWait(Locator.linkWithText(SIMPLE_ASSAY_FOR_EXPORT));
        waitForElement(Locator.linkWithText(RUN01_NAME));

        compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN01_NAME, run01ColumnData);
        compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN04_NAME, run04ColumnData);

    }

    @Test
    public void validateImportingFileUsingRunProperties() throws Exception
    {
        final String OPERATOR_EMAIL_02 = "jane.doe@AssayExportImport.test";
        final String INSTRUMENT_NAME_02 = "XYZ Reader";
        final String INSTRUMENT_SETTING_02 = "890";
        final String COMMENT_BASIC_02 = "This is a comment for run where the data was imported in the Run Details. This is for run: ";

        createSimpleProjectAndAssay(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT);

        List<File> runFiles = Arrays.asList(
                RUN01_FILE,
                RUN02_FILE,
                RUN03_FILE,
                RUN04_FILE);

        Map<String, String> batchProperties = new HashMap<>();
        batchProperties.put("operatorEmail", OPERATOR_EMAIL_02);
        batchProperties.put("instrument", INSTRUMENT_NAME_02);

        List<Map<String, String>> runProperties = new ArrayList<>();
        runProperties.add(Maps.of("name", RUN01_NAME, "comments", COMMENT_BASIC_02 + RUN01_NAME, "instrumentSetting", INSTRUMENT_SETTING_02));
        runProperties.add(Maps.of("name", RUN02_NAME, "comments", COMMENT_BASIC_02 + RUN02_NAME, "instrumentSetting", INSTRUMENT_SETTING_02));
        runProperties.add(Maps.of("name", RUN03_NAME, "comments", COMMENT_BASIC_02 + RUN03_NAME, "instrumentSetting", INSTRUMENT_SETTING_02));
        runProperties.add(Maps.of("name", RUN04_NAME, "comments", COMMENT_BASIC_02 + RUN04_NAME, "instrumentSetting", INSTRUMENT_SETTING_02));

        log("Populate the assay '" + SIMPLE_ASSAY_FOR_EXPORT + "' by importing the file through the 'Run Properties'.");
        populateAssay(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT, false, runFiles, batchProperties, runProperties);

        log("Add a new field that has a missing value indicator.");
        addNewField(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT, new FieldDefinition("missingValue", FieldDefinition.ColumnType.String).setMvEnabled(true));

        log("Set some of the missing value indicators in run '" + RUN02_NAME + "'.");
        Map<Integer, Map<String, String>> rowFieldValues = new HashMap<>();
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "Q");
        rowFieldValues.put(5, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValue", "This value is not missing.");
        rowFieldValues.put(7, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "N");
        rowFieldValues.put(8, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValue", "Neither is this value missing.");
        rowFieldValues.put(9, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "N");
        rowFieldValues.put(10, fieldValues);
        setFieldValues(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN02_NAME, rowFieldValues);

        log("Record various column values for run '" + RUN02_NAME + "'.");
        List<String> runColumns = Arrays.asList("missingValue", "adjustedM1", "M2");
        Map<String, List<String>> run02ColumnData = getRunColumnData(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN02_NAME, runColumns);

        log("Go to run '" + RUN03_NAME + "' and update various value in some of it's fields.");
        rowFieldValues = new HashMap<>();
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "N");
        fieldValues.put("quf_M1", "1010101");
        fieldValues.put("quf_M3", "77.7");
        rowFieldValues.put(2, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValue", "Hello world: " + TRICKY_CHARACTERS);
        rowFieldValues.put(3, fieldValues);
        fieldValues = new HashMap<>();
        fieldValues.put("quf_missingValueMVIndicator", "Q");
        fieldValues.put("quf_M2", "12345");
        rowFieldValues.put(4, fieldValues);
        setFieldValues(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN03_NAME, rowFieldValues);

        log("Record various column values for run '" + RUN03_NAME + "'.");
        runColumns = Arrays.asList("missingValue", "adjustedM1", "M1", "M3");
        Map<String, List<String>> run03ColumnData = getRunColumnData(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN03_NAME, runColumns);

        log("Now export the run.");

        goToProjectHome(ASSAY_PROJECT_FOR_EXPORT_02);

        goToFolderManagement().goToExportTab();
        new Checkbox(Locator.tagWithText("label", ExportFolderPage.EXPERIMENTS_AND_RUNS).precedingSibling("input").findElement(getDriver())).check();
        new Checkbox(Locator.tagWithText("label", "Files").precedingSibling("input").findElement(getDriver())).check();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        log("Create a simple Assay project as the import target.");
        _containerHelper.createProject(ASSAY_PROJECT_FOR_IMPORT_02, "Assay");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_02);

        log("Import the folder.");
        importFolderFromZip(exportedFolderFile);

        log("Validate that the data has been imported as expected.");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_02);
        clickAndWait(Locator.linkWithText(SIMPLE_ASSAY_FOR_EXPORT));
        waitForElement(Locator.linkWithText(RUN01_NAME));

        compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN02_NAME, run02ColumnData);
        compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN03_NAME, run03ColumnData);

    }

    private void createGeneralAssayWithoutTransform(String assayName)
    {
        log("Create an Assay with no transform, no run properties.");

        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", assayName);

        log("Remove the batch fields we don't care about.");
        assayDesignerPage.goToBatchFields().removeField("ParticipantVisitResolver")
                .removeField("TargetStudy");

        assayDesignerPage.goToResultsFields()
                .removeAllFields(false)
                .setInferFieldFile(RUN01_FILE);
        assayDesignerPage.clickFinish();
    }

    @Test
    public void testExportImportWithClientAPI()
    {
        String runName = "api run";
        log("Create an Assay project with no transform, no run properties.");
        _containerHelper.createProject(ASSAY_PROJECT_FOR_EXPORT_03, "Assay");
        goToProjectHome(ASSAY_PROJECT_FOR_EXPORT_03);
        createGeneralAssayWithoutTransform(SIMPLE_ASSAY_FOR_EXPORT);
        SelectRowsResponse response = executeSelectRowCommand("assay", "AssayList", ContainerFilter.Current, "/" + ASSAY_PROJECT_FOR_EXPORT_03, null);

        log("Import a run with a JavaScript API call");
        List<String> runColumns = Arrays.asList("participantid", "Date", "M1", "M2", "M3");
        executeScript(
            "LABKEY.Assay.importRun({" +
                    "assayId: " + response.getRows().get(0).get("RowId") + "," +
                    "name: \"" + runName + "\"," +
                    "dataRows: [{" +
                        "ParticipantId: 10," +
                        "Date: \"2017-01-01\"," +
                        "M1: 42," +
                        "M2: 43," +
                        "M3: 666," +
                    "}]" +
            "});"
        );

        Map<String, List<String>> runColumnData = getRunColumnData(ASSAY_PROJECT_FOR_EXPORT_03, SIMPLE_ASSAY_FOR_EXPORT, runName, runColumns);


        log("Now export the run.");

        goToProjectHome(ASSAY_PROJECT_FOR_EXPORT_03);

        ExportFolderPage exportFolderPage = goToFolderManagement().goToExportTab();
        exportFolderPage.includeExperimentsAndRuns(true);
        exportFolderPage.includeFiles(true);
        File exportedFolderFile = exportFolderPage.exportToBrowserAsZipFile();

        log("Create a simple Assay project as the import target.");
        _containerHelper.createProject(ASSAY_PROJECT_FOR_IMPORT_03, "Assay");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_03);

        log("Import the folder.");
        importFolderFromZip(exportedFolderFile);

        log("Validate that the data has been imported as expected.");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_03);
        clickAndWait(Locator.linkWithText(SIMPLE_ASSAY_FOR_EXPORT));
        waitForElement(Locator.linkWithText(runName));

        compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_03, SIMPLE_ASSAY_FOR_EXPORT, runName, runColumnData);

    }

    /**
     * Issue 43802: Export runs to XAR via "Write to exportedXARs directory in pipeline" fails with error
     */
    @Test
    public void testExportXarToPipeline() throws Exception
    {
        final String exportProject = ASSAY_PROJECT_FOR_EXPORT_04;
        final String importProject = ASSAY_PROJECT_FOR_IMPORT_04;
        final String assayName = SIMPLE_ASSAY_FOR_EXPORT;
        final String instrumentSetting = "456";
        final String commentPrefix = "This is a comment for run to be exported via XAR. This is for run: ";

        int assayId = createSimpleProjectAndAssay(exportProject, assayName).intValue();

        Connection cn = createDefaultConnection();

        ImportRunCommand run1 = new ImportRunCommand(assayId, RUN01_FILE);
        run1.setName(RUN01_NAME);
        run1.setComment(commentPrefix + RUN01_NAME);
        run1.setProperties(Maps.of("instrumentSetting", instrumentSetting));
        run1.execute(cn, exportProject);

        ImportRunCommand run2 = new ImportRunCommand(assayId, RUN02_FILE);
        run2.setName(RUN02_NAME);
        run2.setComment(commentPrefix + RUN02_NAME);
        run2.setProperties(Maps.of("instrumentSetting", instrumentSetting));
        run2.execute(cn, exportProject);

        ImportRunCommand run3 = new ImportRunCommand(assayId, RUN03_FILE);
        run3.setName(RUN03_NAME);
        run3.setComment(commentPrefix + RUN03_NAME);
        run3.setProperties(Maps.of("instrumentSetting", instrumentSetting));
        run3.execute(cn, exportProject);

        ImportRunCommand run4 = new ImportRunCommand(assayId, RUN04_FILE);
        run4.setName(RUN04_NAME);
        run4.setComment(commentPrefix + RUN04_NAME);
        run4.setProperties(Maps.of("instrumentSetting", instrumentSetting));
        run4.execute(cn, exportProject);

        List<String> runColumns = Arrays.asList("adjustedM1", "M2");
        Map<String, List<String>> run01ColumnData = getRunColumnData(ASSAY_PROJECT_FOR_EXPORT_04, SIMPLE_ASSAY_FOR_EXPORT, RUN01_NAME, runColumns);
        runColumns = Arrays.asList("adjustedM1", "M1");
        Map<String, List<String>> run04ColumnData = getRunColumnData(ASSAY_PROJECT_FOR_EXPORT_04, SIMPLE_ASSAY_FOR_EXPORT, RUN04_NAME, runColumns);
        goToManageAssays();

        log("Now export the run.");
        final String toPipelineXarName = "toPipeline.xar";

        clickAndWait(Locator.linkWithText(assayName));
        DataRegionTable runsTable = DataRegionTable.DataRegion(getDriver()).find();
        runsTable.checkAllOnPage();
        DataRegionExportHelper exportPanel = runsTable.expandExportPanel();
        File absoluteXarFile = exportPanel.exportXar(ABSOLUTE, "absolute.xar");
        File folderRelativeXarFile = exportPanel.exportXar(FOLDER_RELATIVE, "folderRelative.xar");
        File partialRelativeXarFile = exportPanel.exportXar(PARTIAL_FOLDER_RELATIVE, "partialRelative.xar");
        exportPanel.exportXarToPipeline(FOLDER_RELATIVE, toPipelineXarName)
                .waitForComplete();

        log("Delete runs.");
        AssayRunsPage assayRunsPage = goToManageAssays().clickAssay(assayName);
        DataRegionTable runsGrid = assayRunsPage.getTable();
        runsGrid.checkAllOnPage();
        runsGrid.deleteSelectedRows();

        log("Reimport runs into same project.");
        goToModule("FileContent");
        _fileBrowserHelper.importFile("/exportedXars/" + toPipelineXarName, "Import Experiment");
        goToDataPipeline();
        waitForPipelineJobsToComplete(2, true);
        checkExpectedErrors(2);

        // Issue 45830: Malformed XAR when exporting assay runs with "PARTIAL_FOLDER_RELATIVE" LSIDs
        log("Import runs (partial relative LSID) into a new project");
        _containerHelper.createProject(importProject, "Assay");
        goToProjectHome(importProject);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(partialRelativeXarFile);
        _fileBrowserHelper.importFile(partialRelativeXarFile.getName(), "Import Experiment");
        PipelineStatusTable pipelineStatusTable = goToDataPipeline();
        waitForPipelineJobsToComplete(1, true);
        checkExpectedErrors(2);
        pipelineStatusTable.deleteAllPipelineJobs();

        log("Import runs (folder relative LSID) into a new project");
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(folderRelativeXarFile);
        _fileBrowserHelper.importFile(folderRelativeXarFile.getName(), "Import Experiment");
        goToDataPipeline();
        waitForPipelineJobsToComplete(1, false);
        goToManageAssays()
                .clickAssay(assayName)
                .clickAssayIdLink(RUN01_NAME);

        compareRunColumnsWithExpected(importProject, assayName, RUN01_NAME, run01ColumnData);
        compareRunColumnsWithExpected(importProject, assayName, RUN04_NAME, run04ColumnData);
    }

}
