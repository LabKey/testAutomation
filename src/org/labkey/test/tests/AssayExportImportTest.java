package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ArtifactCollector;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PerlHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({DailyB.class})
public class AssayExportImportTest extends BaseWebDriverTest
{
    private final String ASSAY_PROJECT_FOR_EXPORT_01 = "Assay_Project_For_Export_ByFilesWebPart";
    private final String ASSAY_PROJECT_FOR_IMPORT_01 = "Assay_Project_For_Import_ByFilesWebPart";
    private final String ASSAY_PROJECT_FOR_EXPORT_02 = "Assay_Project_For_Export_ByFile";
    private final String ASSAY_PROJECT_FOR_IMPORT_02 = "Assay_Project_For_Import_ByFile";
    private final String ASSAY_PROJECT_FOR_EXPORT_03 = "Assay_Project_For_Export_ByApi";
    private final String ASSAY_PROJECT_FOR_IMPORT_03 = "Assay_Project_For_Import_ByApi";

    private final String SIMPLE_ASSAY_FOR_EXPORT = "AssayForExport";

    private final String SAMPLE_DATA_LOCATION = "/sampledata/AssayImportExport";

    private final String RUN01_FILE = "GenericAssay_Run1.xls";
    private final String RUN02_FILE = "GenericAssay_Run2.xls";
    private final String RUN03_FILE = "GenericAssay_Run3.xls";
    private final String RUN04_FILE = "GenericAssay_Run4.xls";

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

    private void createSimpleProjectAndAssay(String projectName, String assayName)
    {
        final String PERL_SCRIPT = "modifyColumnInAssayRun.pl";

        log("Create a simple Assay project.");
        _containerHelper.createProject(projectName, "Assay");
        goToProjectHome(projectName);

        clickAndWait(Locator.lkButton("New Assay Design"));
        click(Locator.radioButtonById("providerName_General"));
        clickAndWait(Locator.lkButton("Next"));

        AssayDesignerPage assayDesignerPage = new AssayDesignerPage(getDriver());
        assayDesignerPage.waitForReady();

        assayDesignerPage.setName(assayName);

        assayDesignerPage.addTransformScript(new File(TestFileUtils.getLabKeyRoot() + SAMPLE_DATA_LOCATION + "/" + PERL_SCRIPT));
        assayDesignerPage.setSaveScriptData(true);

        assayDesignerPage.setEditableResults(true);
        assayDesignerPage.setEditableRuns(true);

        assayDesignerPage.dataFields().findElement(Locator.lkButton("Infer Fields from File")).click();
        waitForElement(Locator.tagWithName("input", "uploadFormElement"));

        setFormElement(Locator.tagWithName("input", "uploadFormElement").findElement(getDriver()),
                new File(TestFileUtils.getLabKeyRoot() + SAMPLE_DATA_LOCATION + "/" + RUN01_FILE));

        click(Locator.lkButton("Submit"));
        waitForElementToDisappear(Locator.lkButton("Submit"));

        assayDesignerPage.dataFields().addField(
                new FieldDefinition("adjustedM1").setType(FieldDefinition.ColumnType.Integer));

        // Working around a bug that is preventing me adding a field and deleting a field in the same region.
        assayDesignerPage = new AssayDesignerPage(getDriver());

        assayDesignerPage.dataFields()
                .selectField("column5").markForDeletion()
                .selectField("column6").markForDeletion();

        assayDesignerPage.batchFields().addField(
                new FieldDefinition("operatorEmail")
                        .setType(FieldDefinition.ColumnType.String));

        assayDesignerPage.batchFields().addField(
                new FieldDefinition("instrument")
                        .setType(FieldDefinition.ColumnType.String)
                        .setDescription("The diagnostic test instrument."));

        assayDesignerPage.runFields().addField(
                new FieldDefinition("instrumentSetting")
                        .setType(FieldDefinition.ColumnType.Integer)
                        .setDescription("The configuration setting on the instrument."));

        assayDesignerPage.saveAndClose();

    }

    public void addNewField(String projectName, String assayName, String runId, FieldDefinition newField)
    {
        log("Modify the assay design to include a new field named '" + newField.getName() + "'.");

        goToProjectHome(projectName);
        clickAndWait(Locator.linkWithText(assayName));
        waitForElement(Locator.linkWithText(runId));

        click(Locator.linkWithText("Manage assay design"));
        waitForElementToBeVisible(Locator.linkWithText("Edit assay design"));
        clickAndWait(Locator.linkWithText("Edit assay design"));

        AssayDesignerPage assayDesignerPage = new AssayDesignerPage(getDriver());
        assayDesignerPage.waitForReady();

        assayDesignerPage.dataFields().addField(newField);

        assayDesignerPage.saveAndClose();

    }

    public void populateAssay(String projectName, String assayName, boolean useFilesWebPart, List<String> runFiles, @Nullable Map<String, String> batchProperties, @Nullable List<Map<String, String>> runProperties)
    {

        goToProjectHome(projectName);

        if(useFilesWebPart)
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());

            portalHelper.addWebPart("Files");

            runFiles.forEach((filePath)->_fileBrowserHelper.uploadFile(new File(TestFileUtils.getLabKeyRoot() + filePath)));

            runFiles.forEach((filePath)->{
                String fileName = filePath.substring(filePath.lastIndexOf("/"));
                _fileBrowserHelper.selectFileBrowserItem(fileName);
            });

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
            runProperty.keySet().forEach((property)->setFormElement(Locator.name(property), runProperty.get(property))
            );

            if(!useFilesWebPart)
            {
                click(Locator.radioButtonById("Fileupload"));
                waitForElementToBeVisible(Locator.tagWithAttribute("input", "type", "file"));
                setFormElement(Locator.tagWithAttribute("input", "type", "file"), new File(TestFileUtils.getLabKeyRoot() + runFiles.get(fileIndex++)));

                if (isElementPresent(Locator.lkButton("Save and Import Another Run")))
                {
                    clickAndWait(Locator.lkButton("Save and Import Another Run"));
                    waitForElement(Locator.tagWithName("input", "instrumentSetting"));
                }

            }
            else
            {
                if (isElementPresent(Locator.lkButton("Save and Import Next File")))
                {
                    clickAndWait(Locator.lkButton("Save and Import Next File"));
                    waitForElement(Locator.tagWithName("input", "instrumentSetting"));
                }
            }
        }

        clickAndWait(Locator.lkButton("Save and Finish"));

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

    private boolean compareRunColumnsWithExpected(String projectName, String assayName, String runId, Map<String, List<String>> expectedColumns)
    {
        boolean pass = true;

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
            if(!currentColumn.equals(expectedColumns.get(columnName)))
            {
                pass = false;
                log("************** The data in column '" + columnName + "' was not as expected. **************");
            }
        }

        // If the data isn't as expected
        if(!pass)
        {
            log("Take a snapshot of the failed run data.");
            ArtifactCollector af = new ArtifactCollector(this);
            af.dumpPageSnapshot("FailedImportData_" + runId, null);
        }

        goToProjectHome(projectName);

        return pass;
    }

    @Test
    public void validateImportingFileUsingFilesWebPart()
    {
        final String OPERATOR_EMAIL_01 = "john.doe@AssayExportImportTest.com";
        final String INSTRUMENT_NAME_01 = "ABC Reader";
        final String INSTRUMENT_SETTING_01 = "456";
        final String COMMENT_BASIC_01 = "This is a comment for run where the data was imported by the FileWeb Part. This is for run: ";

        log("Create a project named: '" + ASSAY_PROJECT_FOR_EXPORT_01 + "' with an assay named: '" + SIMPLE_ASSAY_FOR_EXPORT + "'.");

        createSimpleProjectAndAssay(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT);

        List<String> runFiles = Arrays.asList(
                SAMPLE_DATA_LOCATION + "/" + RUN01_FILE,
                SAMPLE_DATA_LOCATION + "/" + RUN02_FILE,
                SAMPLE_DATA_LOCATION + "/" + RUN03_FILE,
                SAMPLE_DATA_LOCATION + "/" + RUN04_FILE);

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
        addNewField(ASSAY_PROJECT_FOR_EXPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN01_NAME, new FieldDefinition("missingValue").setType(FieldDefinition.ColumnType.String).setMvEnabled(true));

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
        new Checkbox(Locator.tagWithText("label", "Experiments and runs").precedingSibling("input").findElement(getDriver())).check();
        new Checkbox(Locator.tagWithText("label", "Files").precedingSibling("input").findElement(getDriver())).check();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        log("Create a simple Assay project as the import target.");
        _containerHelper.createProject(ASSAY_PROJECT_FOR_IMPORT_01, "Assay");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_01);

        log("Import the folder.");
        goToFolderManagement().goToImportTab();
        setFormElement(Locator.input("folderZip"), exportedFolderFile);
        clickAndWait(Locator.lkButton("Import Folder"));
        waitForPipelineJobsToFinish(1);

        log("Validate that the data has been imported as expected.");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_01);
        clickAndWait(Locator.linkWithText(SIMPLE_ASSAY_FOR_EXPORT));
        waitForElement(Locator.linkWithText(RUN01_NAME));

        boolean pass = compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN01_NAME, run01ColumnData);
        pass = compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_01, SIMPLE_ASSAY_FOR_EXPORT, RUN04_NAME, run04ColumnData) && pass;

        Assert.assertTrue("The imported columns were not as expected. See log for details.", pass);

    }

    @Test
    public void validateImportingFileUsingRunProperties()
    {
        final String OPERATOR_EMAIL_02 = "jane.doe@AssayExportImportTest.com";
        final String INSTRUMENT_NAME_02 = "XYZ Reader";
        final String INSTRUMENT_SETTING_02 = "890";
        final String COMMENT_BASIC_02 = "This is a comment for run where the data was imported in the Run Details. This is for run: ";

        log("Create a project named: '" + ASSAY_PROJECT_FOR_EXPORT_02 + "' with an assay named: '" + SIMPLE_ASSAY_FOR_EXPORT + "'.");

        createSimpleProjectAndAssay(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT);

        List<String> runFiles = Arrays.asList(
                SAMPLE_DATA_LOCATION + "/" + RUN01_FILE,
                SAMPLE_DATA_LOCATION + "/" + RUN02_FILE,
                SAMPLE_DATA_LOCATION + "/" + RUN03_FILE,
                SAMPLE_DATA_LOCATION + "/" + RUN04_FILE);

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
        addNewField(ASSAY_PROJECT_FOR_EXPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN02_NAME, new FieldDefinition("missingValue").setType(FieldDefinition.ColumnType.String).setMvEnabled(true));

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
        new Checkbox(Locator.tagWithText("label", "Experiments and runs").precedingSibling("input").findElement(getDriver())).check();
        new Checkbox(Locator.tagWithText("label", "Files").precedingSibling("input").findElement(getDriver())).check();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        log("Create a simple Assay project as the import target.");
        _containerHelper.createProject(ASSAY_PROJECT_FOR_IMPORT_02, "Assay");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_02);

        log("Import the folder.");
        goToFolderManagement().goToImportTab();
        setFormElement(Locator.input("folderZip"), exportedFolderFile);
        clickAndWait(Locator.lkButton("Import Folder"));
        waitForPipelineJobsToFinish(1);

        log("Validate that the data has been imported as expected.");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_02);
        clickAndWait(Locator.linkWithText(SIMPLE_ASSAY_FOR_EXPORT));
        waitForElement(Locator.linkWithText(RUN01_NAME));

        boolean pass = compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN02_NAME, run02ColumnData);
        pass = compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_02, SIMPLE_ASSAY_FOR_EXPORT, RUN03_NAME, run03ColumnData) && pass;

        Assert.assertTrue("The imported columns were not as expected. See log for details.", pass);
    }

    private void createGeneralAssayWithoutTransform(String assayName)
    {
        log("Create an Assay with no transform, no run properties.");

        clickAndWait(Locator.lkButton("New Assay Design"));
        click(Locator.radioButtonById("providerName_General"));
        clickAndWait(Locator.lkButton("Next"));

        AssayDesignerPage assayDesignerPage = new AssayDesignerPage(getDriver());
        assayDesignerPage.waitForReady();

        assayDesignerPage.setName(assayName);

        assayDesignerPage.dataFields().findElement(Locator.lkButton("Infer Fields from File")).click();
        waitForElement(Locator.tagWithName("input", "uploadFormElement"));

        setFormElement(Locator.tagWithName("input", "uploadFormElement").findElement(getDriver()),
                new File(TestFileUtils.getLabKeyRoot() + SAMPLE_DATA_LOCATION + "/" + RUN01_FILE));

        click(Locator.lkButton("Submit"));
        waitForElementToDisappear(Locator.lkButton("Submit"));

        log("Remove the batch fields we don't care about.");
        assayDesignerPage.batchFields().selectField("ParticipantVisitResolver").markForDeletion();
        assayDesignerPage.batchFields().selectField("TargetStudy").markForDeletion();

        assayDesignerPage.saveAndClose();
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

        goToFolderManagement().goToPane("tabexport");
        new Checkbox(Locator.tagWithText("label", "Experiments and runs").precedingSibling("input").findElement(getDriver())).check();
        new Checkbox(Locator.tagWithText("label", "Files").precedingSibling("input").findElement(getDriver())).check();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        log("Create a simple Assay project as the import target.");
        _containerHelper.createProject(ASSAY_PROJECT_FOR_IMPORT_03, "Assay");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_03);

        log("Import the folder.");
        goToFolderManagement().goToPane("tabimport");
        setFormElement(Locator.input("folderZip"), exportedFolderFile);
        clickAndWait(Locator.lkButton("Import Folder"));
        waitForPipelineJobsToFinish(1);

        log("Validate that the data has been imported as expected.");
        goToProjectHome(ASSAY_PROJECT_FOR_IMPORT_03);
        clickAndWait(Locator.linkWithText(SIMPLE_ASSAY_FOR_EXPORT));
        waitForElement(Locator.linkWithText(runName));

        Assert.assertTrue("The imported columns were not as expected. See log for details.",
                compareRunColumnsWithExpected(ASSAY_PROJECT_FOR_IMPORT_03, SIMPLE_ASSAY_FOR_EXPORT, runName, runColumnData));

    }
}
