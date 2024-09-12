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
package org.labkey.test.tests;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Path;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.pages.files.FileContentPage;
import org.labkey.test.pages.study.CreateStudyPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.TestDataUtils;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 12)
public class FileAttachmentColumnTest extends BaseWebDriverTest
{
    private final String PROJECT_NAME = "FileAndAttachmentColumns Project";
    private final String IMPORT_PROJECT_NAME = "FileAndAttachmentColumns Import Project";
    private final String EXPORT_FOLDER_NAME = "ExportFolder";
    private final String EXPORT_FOLDER_PATH = String.format("%s/%s", PROJECT_NAME, EXPORT_FOLDER_NAME);
    private final String SUBFOLDER_A = "SubFolderA";
    private final String SUBFOLDER_A_PATH = String.format("%s/%s", PROJECT_NAME, SUBFOLDER_A);
    private final String SUB_A_ASSAY = "Sub_A_Assay";
    private final String LIST_NAME = "TestList";
    private final String EXPORT_SAMPLETYPE_NAME = "ExportSamples";
    private final String SUBA_SAMPLETYPE_NAME = "SubASamples";
    private final String EXPORT_ASSAY_NAME = "Export Assay";
    private final File DATAFILE_DIRECTORY = TestFileUtils.getSampleData("fileTypes");
    private final File SAMPLE_CSV = new File(DATAFILE_DIRECTORY, "csv_sample.csv");
    private final File SAMPLE_JPG = new File(DATAFILE_DIRECTORY, "jpg_sample.jpg");
    private final File SAMPLE_TRICKY_PDF = new File(DATAFILE_DIRECTORY, "pdf_sample_with+%$@+%%+#-+=.pdf");
    private final File SAMPLE_PDF = new File(DATAFILE_DIRECTORY, "pdf_sample.pdf");
    private final File SAMPLE_TIF = new File(DATAFILE_DIRECTORY, "tif_sample.tif");
    private final File SAMPLE_ZIP = new File(DATAFILE_DIRECTORY, "zip_sample.zip");
    private final File SAMPLE_DOC = new File(DATAFILE_DIRECTORY, "doc_sample.doc");
    private final File SAMPLE_7z = new File(DATAFILE_DIRECTORY, "7z_sample.7z");
    private final File SAMPLE_JAVA = new File(DATAFILE_DIRECTORY, "java_sample.java");
    private final File SAMPLE_JAR = new File(DATAFILE_DIRECTORY, "jar_sample.jar");
    private final List<File> SAMPLE_FILES = List.of(SAMPLE_CSV, SAMPLE_JPG, SAMPLE_TRICKY_PDF, SAMPLE_PDF, SAMPLE_TIF, SAMPLE_ZIP);
    private final List<File> OTHER_SAMPLE_FILES = List.of(SAMPLE_DOC, SAMPLE_7z, SAMPLE_JAVA, SAMPLE_JAR);
    private final String RUN_TXT_COL = "runTxt";
    private final String RUN_FILE_COL = "runFile";
    private final String RESULT_TXT_COL = "resultTxt";
    private final String RESULT_FILE_COL = "resultFile";
    private final String OTHER_RESULT_FILE_COL = "otherResultFile";
    private final String STUDY_DATASET_NAME = "ogreSpiteLevels";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(IMPORT_PROJECT_NAME, afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        FileAttachmentColumnTest init = (FileAttachmentColumnTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(IMPORT_PROJECT_NAME);
        _containerHelper.enableModules(Arrays.asList("Experiment", "Pipeline", "Portal", "Study"));
        _containerHelper.createProject(getProjectName(), "Custom");
        _containerHelper.createSubfolder(getProjectName(), EXPORT_FOLDER_NAME);
        _containerHelper.enableModules(Arrays.asList("Experiment", "Pipeline", "Portal", "Study"));
        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_A);
        _containerHelper.enableModules(Arrays.asList("Experiment", "Pipeline", "Portal"));

        //create list with attachment columns
        createListWithData(EXPORT_FOLDER_PATH);

        // upload all files that may be used in export domains so importing them to file-fields works
        WebDavUploadHelper uploadHelper = new WebDavUploadHelper(EXPORT_FOLDER_PATH);
        for (File file : SAMPLE_FILES)
        {
            uploadHelper.uploadFile(file);
        }
        for (File file : OTHER_SAMPLE_FILES)
        {
            uploadHelper.uploadFile(file);
        }

        //create sample types with file columns, add some files
        SampleTypeDefinition exportSampleType = new SampleTypeDefinition(EXPORT_SAMPLETYPE_NAME)
                .setFields(List.of(new FieldDefinition("color", ColumnType.String),
                        new FieldDefinition("file", ColumnType.File)));
        SampleTypeAPIHelper.createEmptySampleType(EXPORT_FOLDER_PATH, exportSampleType);
        importSampleDataUI(EXPORT_SAMPLETYPE_NAME, EXPORT_FOLDER_PATH, SAMPLE_FILES);

        // create an assay in the export folder
        List<PropertyDescriptor> runFields = List.of(
                new FieldDefinition(RUN_TXT_COL, FieldDefinition.ColumnType.String),
                new FieldDefinition(RUN_FILE_COL, ColumnType.File));
        List<PropertyDescriptor> dataFields = List.of(
                new FieldDefinition(RESULT_TXT_COL, FieldDefinition.ColumnType.String),
                new FieldDefinition(RESULT_FILE_COL, ColumnType.File),
                new FieldDefinition(OTHER_RESULT_FILE_COL, ColumnType.File));
        var protocol = makeGeneralAssay(EXPORT_ASSAY_NAME, runFields, dataFields, EXPORT_FOLDER_PATH);
        addRunData(protocol.getProtocolId(), EXPORT_FOLDER_PATH);

        // add a study
        beginAt(EXPORT_FOLDER_PATH + "/project-begin.view");
        clickPortalTab("Study");
        CreateStudyPage createStudyPage = new StudyHelper(this).startCreateStudy();
        createStudyPage.setSubjectNounSingular("Ogre");
        createStudyPage.setSubjectNounPlural("Ogres");
        createStudyPage.setSubjectColumnName("OgreId");
        createStudyPage.setTimepointType(StudyHelper.TimepointType.DATE);
        createStudyPage.createStudy();

        List<PropertyDescriptor> studyDatasetFields = List.of(
                new FieldDefinition("spiteAmount", ColumnType.Decimal),
                new FieldDefinition("file", ColumnType.File));
        CreateDomainCommand cmd = new CreateDomainCommand("StudyDatasetDate", STUDY_DATASET_NAME);
        cmd.getDomainDesign().setFields(studyDatasetFields);
        cmd.execute(this.createDefaultConnection(), EXPORT_FOLDER_PATH);

        // make another assay in a different folder with the same fields, to test via UI
        makeGeneralAssay(SUB_A_ASSAY, runFields, dataFields, SUBFOLDER_A_PATH);
    }

    // list
    @Test
    public void verifyFileDownloadOnClick()
    {
        clickAndWait(Locator.linkWithText(LIST_NAME));
        DataRegionTable testListRegion = new DataRegionTable("query", getDriver()); // Just make sure the DRT is ready

        List<File> downloadTestFiles = List.of(SAMPLE_CSV, SAMPLE_TIF, SAMPLE_TRICKY_PDF);

        for (File testFile : downloadTestFiles)
        {
            int rowIndex = testListRegion.getRowIndex("Name", testFile.getName());
            var downloadLink = testListRegion.link(rowIndex, "File");
            doAndWaitForDownload(()-> downloadLink.click());
        }

        // verify popup/sprite for jpeg
        mouseOver(Locator.tagWithAttribute("img", "title", SAMPLE_JPG.getName()));
        shortWait().until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/span[contains(text(),'" + SAMPLE_JPG.getName() + "')]")));
        mouseOut();
    }

    // sampleType
    /*
        This test exercises file fields but sets up the data by inserting each row individually
     */
    @Test
    public void testSampleFileFields()
    {
        SampleTypeDefinition subASampleType = new SampleTypeDefinition(SUBA_SAMPLETYPE_NAME)
                .setFields(List.of(new FieldDefinition("color", ColumnType.String),
                        new FieldDefinition("file", ColumnType.File)));
        SampleTypeAPIHelper.createEmptySampleType(SUBFOLDER_A_PATH, subASampleType);

        SampleTypeHelper.beginAtSampleTypesList(this, SUBFOLDER_A_PATH);
        clickAndWait(Locator.linkWithText(SUBA_SAMPLETYPE_NAME));
        DataRegionTable samplesRegion = new DataRegionTable("Material", getDriver());
        List<String> fileNames = SAMPLE_FILES.stream().map(File::getName).toList();

        for (File file : SAMPLE_FILES)
        {
            var queryUpdatePage = samplesRegion.clickInsertNewRow();
            queryUpdatePage.setField("Name", file.getName());
            queryUpdatePage.setField("file", file)
                    .submit();
        }
        validateSampleData(SUBA_SAMPLETYPE_NAME, SUBFOLDER_A_PATH, SAMPLE_FILES);

        var fileContentPage = FileContentPage.beginAt(this, SUBFOLDER_A_PATH);
        fileContentPage.fileBrowserHelper().selectFileBrowserItem("/sampletype/csv_sample.csv");
        var files = fileContentPage.fileBrowserHelper().getFileList(true);
        checker().withScreenshot("unexpected_davfile_state")
                .wrapAssertion(()-> Assertions.assertThat(files)
                        .as("expect edited files to be present in dav locaiton")
                        .containsExactlyInAnyOrderElementsOf(fileNames));
    }

    // assay
    @Test
    public void testAssayFileFieldsUI() throws Exception
    {
        String runName = "assay_run.tsv";
        WebDavUploadHelper uploadHelper = new WebDavUploadHelper(SUBFOLDER_A_PATH);

        // upload the files we'll be using as results so they can be resolved
        for (File file : SAMPLE_FILES)
        {
            uploadHelper.uploadFile(file);
        }
        for (File file : OTHER_SAMPLE_FILES)
        {
            uploadHelper.uploadFile(file);
        }

        // generate some resultFiles
        var resultFiles = new ArrayList<File>();


        String runFileContent = """
                resultTxt	resultFile
                result-0	field_file_for_results_domain-0.tsv
                result-1	field_file_for_results_domain-1.tsv
                """;
        File runFileFieldFile = TestFileUtils.writeTempFile("runFileFieldFile.tsv", runFileContent);
        uploadHelper.uploadFile(runFileFieldFile);

        // note: files in otherResultFile column are
        String resultsFile0Content = """
                resultTxt	resultFile	otherResultFile
                result-0	field_file_for_results_domain-0.tsv	csv_sample.csv
                result-1	doc_sample.doc	jpg_sample.jpg
                result-2	7z_sample.7z	pdf_sample_with+%$@+%%+#-+=.pdf
                """;
        String resultsFile1Content = """
                resultTxt	resultFile	otherResultFile
                result-3	java_sample.java	pdf_sample.pdf
                result-4	jar_sample.jar	tif_sample.tif
                """;

        File resultFile0 = TestFileUtils.writeTempFile("field_file_for_results_domain-0.tsv", resultsFile0Content);
        resultFiles.add(resultFile0);
        File resultFile1 = TestFileUtils.writeTempFile("field_file_for_results_domain-1.tsv", resultsFile1Content);
        resultFiles.add(resultFile1);

        for (File file : resultFiles)
            uploadHelper.uploadFile(file);

        beginAt(SUBFOLDER_A_PATH + "/project-begin.view");
        goToModule("Assay");
        clickAndWait(Locator.linkContainingText(SUB_A_ASSAY));
        clickButton("Import Data");
        clickButton("Next");    // batch properties

        // run properties
        setFormElement(Locator.input("name"), runName);
        setFormElement(Locator.input(RUN_TXT_COL), "run text");
        setFormElement(Locator.input(RUN_FILE_COL), runFileFieldFile);
        checkRadioButton(Locator.inputById("Fileupload"));
        int addIndex = 0;
        for (File file : resultFiles)
        {
            if (addIndex > 0)
            {
                sleep(500);
                var plusIcon = Locator.tagWithClass("a", "labkey-file-add-icon-enabled")
                        .waitForElement(getDriver(), 1000);
                scrollIntoView(plusIcon, true);
                shortWait().until(ExpectedConditions.elementToBeClickable(plusIcon));
                plusIcon.click();
                setFormElement(Locator.input("__primaryFile__" + addIndex), file);
            }
            else
            {   // insert the first one, its name doesn't have an index
                setFormElement(Locator.input("__primaryFile__"), file);
            }
            addIndex ++;
        }
        clickButton("Save and Finish");

        List<String> expectedResultTexts = List.of("result-0", "result-1", "result-2", "result-3", "result-4");
        List<String> expectedOtherFiles = List.of("csv_sample.csv", "pdf_sample.pdf", "pdf_sample_with+%$@+%%+#-+=.pdf", "tif_sample.tif", "");
        List<String> expectedResultFiles = List.of("field_file_for_results_domain-0.tsv", "7z_sample.7z", "doc_sample.doc", "jar_sample.jar", "java_sample.java");

        validateAssayRun(SUB_A_ASSAY, SUBFOLDER_A_PATH, runName, runFileFieldFile, expectedResultTexts, expectedResultFiles, expectedOtherFiles);
    }

    // exportImport
    /*
        exports a folder with a list, sampletype, assay, a dataset, and imports them to the import project
        Then, validates expected data in source and destination
     */
    @Test
    public void testExportImportData()
    {
        beginAt(EXPORT_FOLDER_PATH + "/project-begin.view");

        clickPortalTab("Portal");
        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Assay List");

        // validate list data
        log("validate list attachment data prior to export");
        validateListData(LIST_NAME, EXPORT_FOLDER_PATH, SAMPLE_FILES);

        log("validate sample file field data prior to export");
        validateSampleData(EXPORT_SAMPLETYPE_NAME, EXPORT_FOLDER_PATH, SAMPLE_FILES);

        // validate assay data
        goToModule("FileContent");  // get the run file created during class setup
        var fileContentPage = new FileContentPage(getDriver());
        fileContentPage.fileBrowserHelper().selectFileBrowserItem("assaydata/runFile-1.tsv");
        File runFile = fileContentPage.fileBrowserHelper().downloadSelectedFiles();

        List<String> expectedResultTexts = List.of("result-0", "result-1", "result-2", "result-3", "result-4");
        List<String> expectedOtherFiles = List.of("csv_sample.csv", "pdf_sample.pdf",
                "pdf_sample_with+%$@+%%+#-+=.pdf", "tif_sample.tif", "");
        List<String> expectedResultFiles = List.of("java_sample.java", "doc_sample.doc", "zip_sample.zip", "7z_sample.7z",
                "jar_sample.jar");
        validateAssayRun(EXPORT_ASSAY_NAME, EXPORT_FOLDER_PATH, "firstRun", runFile, expectedResultTexts,
                expectedResultFiles, expectedOtherFiles);

        // set up dataset data and verify it is as expected
        beginAt(EXPORT_FOLDER_PATH + "/study-datasets.view");
        clickAndWait(Locator.linkWithText(STUDY_DATASET_NAME));
        DataRegionTable dataSetTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Dataset").waitFor();
        int ogreId = 1;
        for (File file : SAMPLE_FILES)
        {
            var updatePage = dataSetTable.clickInsertNewRow();
            updatePage.setField("OgreId", ogreId);
            updatePage.setField("date", "11-11-2024");
            updatePage.setField("spiteAmount", "34985762");
            updatePage.setField("file", file);
            updatePage.submit();
            ogreId++;
        }
        validateDatasetData(STUDY_DATASET_NAME, EXPORT_FOLDER_PATH, SAMPLE_FILES);


        var exportZip = goToFolderManagement()
                .goToExportTab()
                .includeFiles(true)
                .exportToBrowserAsZipFile();

        beginAt(IMPORT_PROJECT_NAME + "/project-begin.view");
        goToFolderManagement()
                .goToImportTab()
                .selectLocalZipArchive()
                .chooseFile(exportZip)
                .clickImportFolder();
        waitForPipelineJobsToFinish(1);

        log("validate list attachment data in import location");
        validateListData(LIST_NAME, IMPORT_PROJECT_NAME, SAMPLE_FILES);

        log("validate sample file field data in import location");
        validateSampleData(EXPORT_SAMPLETYPE_NAME, IMPORT_PROJECT_NAME, SAMPLE_FILES);

        log("validate assay file field data in import location");
        validateAssayRun(EXPORT_ASSAY_NAME, IMPORT_PROJECT_NAME, "firstRun", runFile, expectedResultTexts,
                expectedResultFiles, expectedOtherFiles);

        log("validate dataset data in import location");
        validateDatasetData(STUDY_DATASET_NAME, IMPORT_PROJECT_NAME, SAMPLE_FILES);
    }

    private void createListWithData(String containerPath)
    {
        beginAt(containerPath + "/project-begin.view");
        clickTab("Portal");

        ListHelper listHelper = new ListHelper(getDriver());
        String LIST_KEY = "TestListId";
        listHelper.createList(getProjectName() + "/" + EXPORT_FOLDER_NAME, LIST_NAME, LIST_KEY,
                new FieldDefinition("Name", ColumnType.String),
                new FieldDefinition("File", ColumnType.Attachment));
        goToManageLists();
        listHelper.click(Locator.linkContainingText(LIST_NAME));

        for (File file : SAMPLE_FILES)
        {
            Map<String, String> fileRow = Map.of("Name", file.getName(), "File", file.getAbsolutePath());
            listHelper.insertNewRow(fileRow, false);
        }
    }

    private void importSampleDataUI(String sampleTypeName, String containerPath, List<File> files)
    {
        var helper = SampleTypeHelper.beginAtSampleTypesList(this, containerPath);
        clickAndWait(Locator.linkWithText(sampleTypeName));

        List<Map<String, String>> sampleFileData = new ArrayList<>();
        for (File file : files)
        {
            sampleFileData.add(Map.of("Name", file.getName(), "Color", "green",
                    "File", file.getName()));
        }
        helper.bulkImport(sampleFileData);
    }

    private Protocol makeGeneralAssay(String assayName, List<PropertyDescriptor> runFields, List<PropertyDescriptor> dataFields,
                                      String folderPath) throws Exception
    {
        var assayDesign =  new GeneralAssayDesign(assayName)
                .setBatchFields(List.of(new FieldDefinition("batchData", FieldDefinition.ColumnType.String)), false)
                .setRunFields(runFields, false)
                .setDataFields(dataFields, false);


        var protocol = assayDesign.createAssay(folderPath, createDefaultConnection());
        var updateProtocol = protocol.setEditableRuns(true).setEditableResults(true);
        return assayDesign.updateProtocol(folderPath, createDefaultConnection(), updateProtocol);
    }

    private void addRunData(Integer protocolId, String folderPath) throws Exception
    {
        var uploadHelper = new WebDavUploadHelper(folderPath);  // put the files in the folder root so assay can resolve them

        String runDataFileContent = """
                runTxt	runFile	resultTxt	resultFile	otherResultFile
                run text	runFile.tsv	result-0	java_sample.java	csv_sample.csv
                run text	runFile.tsv	result-1	doc_sample.doc	jpg_sample.jpg
                run text	runFile.tsv	result-2	zip_sample.zip	pdf_sample_with+%$@+%%+#-+=.pdf
                run text	runFile.tsv	result-3	7z_sample.7z	pdf_sample.pdf
                run text	runFile.tsv	result-4	jar_sample.jar	tif_sample.tif
                """;

        File runFile = TestFileUtils.writeTempFile("runFile.tsv", runDataFileContent);
        uploadHelper.uploadFile(runFile);

        ImportRunCommand importRunCommand = new ImportRunCommand(protocolId, runFile);
        importRunCommand.setName("firstRun");
        importRunCommand.execute(createDefaultConnection(), folderPath);

        // edit the run text and file fields via the UI; the API doesn't support this
        beginAt(EXPORT_FOLDER_PATH + "/project-begin.view");
        goToModule("Assay");
        clickAndWait(Locator.linkContainingText(EXPORT_ASSAY_NAME));
        var runsPage = new AssayRunsPage(getDriver());
        var updatePage = runsPage.getTable().clickEditRow(0);
        updatePage.setField(RUN_TXT_COL, "run text");
        updatePage.setField(RUN_FILE_COL, runFile);
        updatePage.submit();
    }

    private void validateListData(String listName, String folderPath, List<File> expectedFiles)
    {
        beginAt(folderPath + "/project-begin.view");
        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable testListRegion = new DataRegionTable("query", getDriver()); // Just make sure the DRT is ready

        for (File testFile : expectedFiles)
        {
            if (testFile.getName().endsWith(".jpg"))
            {
                // verify popup/sprite for jpeg
                mouseOver(Locator.tagWithAttribute("img", "title", testFile.getName()));
                Locator.tagWithClass("span", "labkey-wp-title").withText(testFile.getName()).waitForElement(getDriver(), 1500);
                mouseOut();
            }
            else
            {
                int rowIndex = testListRegion.getRowIndex("Name", testFile.getName());
                var downloadLink = testListRegion.link(rowIndex, "File");
                doAndWaitForDownload(() -> downloadLink.click());
            }
        }
    }

    private void validateSampleData(String sampleType, String folderPath, List<File> expectedFiles)
    {
        SampleTypeHelper.beginAtSampleTypesList(this, folderPath);
        clickAndWait(Locator.linkWithText(sampleType));
        DataRegionTable samplesRegion = new DataRegionTable("Material", getDriver());
        for (File file : expectedFiles)
        {
            int rowIndex = samplesRegion.getRowIndex("Name", file.getName());
            if (file.getName().endsWith(".jpg")) // jpg don't appear to get name shown as text, just thumbnail
            {
                checker().withScreenshot("unexpected_file_state")
                        .verifyTrue("expect jpg to be visible",
                                Locator.tagWithAttributeContaining("img", "title", file.getName()).existsIn(getDriver()));
                // verify popup/sprite for jpeg
                mouseOver(Locator.tagWithAttributeContaining("img", "title", file.getName()));
                shortWait().until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/span[contains(text(),'" + file.getName() + "')]")));
                mouseOut();
            }
            else
            {
                WebElement fileLinkCell = samplesRegion.findCell(rowIndex, "file");
                Optional<WebElement> optionalFileLink = Locator.tag("a").findOptionalElement(fileLinkCell);
                checker().withScreenshot("unexpected_file_state")
                        .awaiting(Duration.ofSeconds(2),
                                () -> Assertions.assertThat(optionalFileLink.isPresent())
                                        .as("expect file "+file.getName()+" to be present")
                                        .isTrue());
                 if (optionalFileLink.isPresent())
                 {
                     // verify fie download behavior
                     File downloadedFile = doAndWaitForDownload(() -> optionalFileLink.get().click());
                     checker().wrapAssertion(() -> Assertions.assertThat(TestFileUtils.getFileContents(downloadedFile))
                             .as("expect the downloaded file to be the expected file")
                             .isEqualTo(TestFileUtils.getFileContents(file)));   // guard against renames like file2.xyz
                 }
            }
        }
    }

    private void validateAssayRun(String assayName, String folderPath, String runName, File runFile,
                                  List<String> expectedResultTexts, List<String> expectedResultFiles, List<String> otherExpectedFiles)
    {
        beginAt(folderPath + "/project-begin.view");
        goToModule("Assay");
        clickAndWait(Locator.linkWithText(assayName));

        AssayRunsPage runsPage = new AssayRunsPage(getDriver());
        int runRowIndex = runsPage.getTable().getRowIndex("Name", runName);
        WebElement fileLinkCell = runsPage.getTable().findCell(runRowIndex, RUN_FILE_COL);
        Optional<WebElement> optionalFileLink = Locator.tag("a").findOptionalElement(fileLinkCell);
        checker().withScreenshot("unexpected_run_file_state")
                .awaiting(Duration.ofSeconds(2), ()-> Assertions.assertThat(optionalFileLink.isPresent())
                        .as("expect file link for ["+runFile.getName()+"] to be present in the runs grid")
                        .isTrue());
        if (optionalFileLink.isPresent())
        {
            var file = doAndWaitForDownload(()-> optionalFileLink.get().click());
            checker().wrapAssertion(()-> Assertions.assertThat(TestFileUtils.getFileContents(file))
                    .as("expect the downloaded file to have equivalent content")
                    .isEqualTo(TestFileUtils.getFileContents(runFile)));
        }

        var resultsPage = runsPage.clickAssayIdLink(runName);

        var resultTxts = resultsPage.getDataTable().getColumnDataAsText(RESULT_TXT_COL);
        var runTxts = resultsPage.getDataTable().getColumnDataAsText("Run/runTxt");
        var resultFileTexts = resultsPage.getDataTable().getColumnDataAsText(RESULT_FILE_COL);
        var otherResultFileTexts = resultsPage.getDataTable().getColumnDataAsText(OTHER_RESULT_FILE_COL);
        var runFileTexts = resultsPage.getDataTable().getColumnDataAsText("Run/runFile");

        checker().withScreenshot("unexpected_results_texts")
                .wrapAssertion(()-> Assertions.assertThat(resultTxts)
                        .as("expect complete results")
                        .containsExactlyInAnyOrderElementsOf(expectedResultTexts));
        checker().withScreenshot("unexpected_run_texts")
                .wrapAssertion(()-> Assertions.assertThat(runTxts)
                        .as("expect complete run texts in results view")
                        .containsOnly("run text")
                        .hasSize(5));
        checker().withScreenshot("unexpected_results_files")
                .wrapAssertion(()-> Assertions.assertThat(resultFileTexts.stream().map(String::trim).toList())
                        .as("expect complete result files")
                        .containsExactlyInAnyOrderElementsOf(expectedResultFiles));
        checker().withScreenshot("unexpected_other_result_files")
                .wrapAssertion(()-> Assertions.assertThat(otherResultFileTexts.stream().map(String::trim).toList())
                        .as("expect other results files to have resolved")
                        .containsExactlyInAnyOrderElementsOf(otherExpectedFiles));  // empty value is for jpg, which doesn't get text/is rendered inline
        checker().withScreenshot("unexpected_run_file_links")
                .wrapAssertion(()-> Assertions.assertThat(runFileTexts.stream().map(String::trim).toList())
                        .as("expect complete run files")
                        .containsOnly(String.format("assaydata%s%s", File.separatorChar, runFile.getName()))
                        .hasSize(5));
    }

    private void validateDatasetData(String datasetName, String folderPath, List<File> expectedFiles)
    {
        beginAt(folderPath + "/study-datasets.view");
        clickAndWait(Locator.linkWithText(datasetName));
        DataRegionTable dataRegionTable = new DataRegionTable("Dataset", getDriver()); // Just make sure the DRT is ready

        for (File file : expectedFiles)
        {
            if (file.getName().endsWith(".jpg")) // jpg don't appear to get name shown as text, just thumbnail
            {
                checker().withScreenshot("unexpected_file_state")
                        .verifyTrue("expect jpg to be visible",
                                Locator.tagWithAttributeContaining("img", "title", file.getName()).existsIn(dataRegionTable));
                // verify popup/sprite for jpeg
                mouseOver(Locator.tagWithAttributeContaining("img", "title", file.getName()));
                shortWait().until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/span[contains(text(),'" + file.getName() + "')]")));
                mouseOut();
            }
            else
            {
                Optional<WebElement> optionalFileLink = Locator.linkContainingText(String.format("datasetdata%s%s", File.separatorChar, file.getName()))
                        .findOptionalElement(dataRegionTable);
                checker().withScreenshot("unexpected_file_state")
                        .awaiting(Duration.ofSeconds(2),
                                () -> Assertions.assertThat(optionalFileLink.isPresent())
                                        .as("expect file "+file.getName()+" to be present")
                                        .isTrue());
                if (optionalFileLink.isPresent())
                {
                    // verify fie download behavior
                    File downloadedFile = doAndWaitForDownload(() -> optionalFileLink.get().click());
                    checker().wrapAssertion(() -> Assertions.assertThat(TestFileUtils.getFileContents(downloadedFile))
                            .as("expect the downloaded file to be the expected file")
                            .isEqualTo(TestFileUtils.getFileContents(file)));   // guard against renames like file2.xyz
                }
            }
        }
    }

    @Before
    public void preTest()
    {
        beginAt(getProjectName() + "/" + EXPORT_FOLDER_NAME + "/project-begin.view");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("Experiment", "Pipeline", "Portal");
    }
}
