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
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.pages.files.FileContentPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataUtils;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
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
    private final String LIST_KEY = "TestListId";
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
    private final List<File> SAMPLE_FILES = List.of(SAMPLE_CSV, SAMPLE_JPG, SAMPLE_TRICKY_PDF, SAMPLE_PDF, SAMPLE_TIF, SAMPLE_ZIP);
    private final String RUN_TXT_COL = "runTxt";
    private final String RUN_FILE_COL = "runFile";
    private final String RESULT_TXT_COL = "resultTxt";
    private final String RESULT_FILE_COL = "resultFile";

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
        _containerHelper.enableModules(Arrays.asList("Experiment", "Pipeline", "Portal"));
        _containerHelper.createProject(getProjectName(), "Custom");
        _containerHelper.createSubfolder(getProjectName(), EXPORT_FOLDER_NAME);
        _containerHelper.enableModules(Arrays.asList("Experiment", "Pipeline", "Portal"));
        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_A);
        _containerHelper.enableModules(Arrays.asList("Experiment", "Pipeline", "Portal"));

        //create list with attachment columns
        createListWithData(EXPORT_FOLDER_PATH);

        //create sample types with file columns
        createSampleTypeWithData(EXPORT_SAMPLETYPE_NAME, EXPORT_FOLDER_PATH);

        // create an assay in the export folder
        List<PropertyDescriptor> runFields = List.of(
                new FieldDefinition(RUN_TXT_COL, FieldDefinition.ColumnType.String),
                new FieldDefinition(RUN_FILE_COL, ColumnType.File));
        List<PropertyDescriptor> dataFields = List.of(
                new FieldDefinition(RESULT_TXT_COL, FieldDefinition.ColumnType.String),
                new FieldDefinition(RESULT_FILE_COL, ColumnType.File));
        var protocol = makeGeneralAssay(EXPORT_ASSAY_NAME, runFields, dataFields, EXPORT_FOLDER_PATH);
        addRunData(protocol.getProtocolId(), EXPORT_FOLDER_PATH);
        // issue 51176, addRunData isn't resolving files

        // make another in a different folder with the same fields
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

     */
    @Test
    public void verifySampleFileFields()
    {
        createSampleTypeWithData(SUBA_SAMPLETYPE_NAME, SUBFOLDER_A_PATH);
        // give the sampleType actual files via editing; for the nonce not all files are resolving on import

        SampleTypeHelper.beginAtSampleTypesList(this, SUBFOLDER_A_PATH);
        clickAndWait(Locator.linkWithText(SUBA_SAMPLETYPE_NAME));
        DataRegionTable samplesRegion = new DataRegionTable("Material", getDriver());
        List<String> fileNames = SAMPLE_FILES.stream().map(File::getName).toList();

        for (File file : SAMPLE_FILES)
        {
            int rowIndex = samplesRegion.getRowIndex("Name", file.getName());
            var fileFieldText = samplesRegion.getRowDataAsText(rowIndex, "File").get(0);

            // due to the current state of file import via file, expect no content in the File field
            checker().withScreenshot("unexpected_file_state")
                    .wrapAssertion(()-> Assertions.assertThat(fileFieldText)
                            .as("expect bulk-imported file to be empty: Issue 51176")
                            .isEqualTo(" "));

            // edit the row to remove the broken/imported file and replace it via row edit
            var queryUpdatePage = samplesRegion.clickEditRow(rowIndex);
            queryUpdatePage.setField("file", file)
                    .submit();
        }

        for (File file : SAMPLE_FILES)
        {
            int rowIndex = samplesRegion.getRowIndex("Name", file.getName());
            //

            if (file == SAMPLE_JPG) // jpg don't appear to get name shown as text, just thumbnail
            {
                checker().withScreenshot("unexpected_file_state")
                        .verifyTrue("expect jpg to be visible",
                                Locator.tagWithAttribute("img", "title",
                                        String.format("sampletype%s%s", File.separatorChar, file.getName())).existsIn(getDriver()));
            }
            else
            {
                WebElement fileLink = samplesRegion.link(rowIndex, "File");
                checker().withScreenshot("unexpected_file_state")
                        .awaiting(Duration.ofSeconds(2),
                                () -> Assertions.assertThat(fileLink.getText())
                                        .as("expect the uploaded file to be fixed")
                                        .endsWith(String.format("sampletype%s%s", File.separatorChar, file.getName())));
            }
        }

        // verify file download behavior for csv, pdf
        doAndWaitForDownload(()->click(Locator.linkContainingText(String.format("sampletype%s%s", File.separatorChar, SAMPLE_CSV.getName()))));
        doAndWaitForDownload(()->click(Locator.linkContainingText(String.format("sampletype%s%s", File.separatorChar, SAMPLE_PDF.getName()))));

        // verify popup/sprite for jpeg
        mouseOver(Locator.tagWithAttribute("img", "title", String.format("sampletype%s%s", File.separatorChar, SAMPLE_JPG.getName())));
        shortWait().until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/span[contains(text(),'" + SAMPLE_JPG.getName() + "')]")));
        mouseOut();

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

        // generate some resultFiles
        var resultFiles = new ArrayList<File>();
        WebDavUploadHelper uploadHelper = new WebDavUploadHelper(SUBFOLDER_A_PATH);
        List<Map<String, Object>> importData = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            String fileName = String.format("field_file_for_results_domain-%d.tsv", i);
            String result = String.format("result-%d", i);
            String fileText = "resultTxt\tresultFile\n"+
                        result+"\t" + fileName;
            var fieldFile = TestFileUtils.writeTempFile(fileName, fileText);
            uploadHelper.uploadFile(fieldFile);
            resultFiles.add(fieldFile);

            importData.add(Map.of(RESULT_TXT_COL, result, RESULT_FILE_COL, fieldFile.getName()));
        }

        // generate a run file, referencing the result files
        String importDataFileContents = TestDataUtils.tsvStringFromRowMaps(importData,
                List.of(RESULT_TXT_COL, RESULT_FILE_COL), true);
        File importFile = TestFileUtils.writeTempFile(runName, importDataFileContents);

        beginAt(SUBFOLDER_A_PATH + "/project-begin.view");
        goToModule("Assay");
        clickAndWait(Locator.linkContainingText(SUB_A_ASSAY));
        clickButton("Import Data");
        clickButton("Next");    // batch properties

        // run properties
        setFormElement(Locator.input("name"), runName);
        setFormElement(Locator.input(RUN_TXT_COL), "run text");
        setFormElement(Locator.input(RUN_FILE_COL), importFile);
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

        // make sure the run file can be downloaded
        AssayRunsPage runsPage = new AssayRunsPage(getDriver());
        var runFileLink = runsPage.getTable().link(0, RUN_FILE_COL);
        doAndWaitForDownload(()-> runFileLink.click());

        var resultsPage = runsPage.clickAssayIdLink(runName);
        var assayHelper = new APIAssayHelper(this);
        Integer assayId = assayHelper.getIdFromAssayName(SUB_A_ASSAY, SUBFOLDER_A_PATH);

        var resultTxts = resultsPage.getDataTable().getColumnDataAsText(RESULT_TXT_COL);
        var runTxts = resultsPage.getDataTable().getColumnDataAsText("Run/runTxt");
        var resultFileTexts = resultsPage.getDataTable().getColumnDataAsText(RESULT_FILE_COL);
        var runFileTexts = resultsPage.getDataTable().getColumnDataAsText("Run/runFile");
        var expectedRunFileLinkTexts = resultFiles.stream().map(File::getName).toList();

        checker().withScreenshot("unexpected_results_texts")
                .wrapAssertion(()-> Assertions.assertThat(resultTxts)
                        .as("expect complete results")
                        .containsExactlyInAnyOrder("result-0", "result-1", "result-2", "result-3", "result-4"));
        checker().withScreenshot("unexpected_run_texts")
                .wrapAssertion(()-> Assertions.assertThat(runTxts)
                        .as("expect complete results")
                        .containsOnly("run text")
                        .hasSize(5));
        checker().withScreenshot("unexpected_results_files")
                .wrapAssertion(()-> Assertions.assertThat(resultFileTexts.stream().map(String::trim).toList())
                        .as("expect complete result files")
                        .containsExactlyInAnyOrderElementsOf(expectedRunFileLinkTexts));
        checker().withScreenshot("unexpected_run_file_links")
                .wrapAssertion(()-> Assertions.assertThat(runFileTexts.stream().map(String::trim).toList())
                        .as("expect complete run files")
                        .containsOnly(String.format("assaydata%s%s", File.separatorChar, importFile.getName()))
                        .hasSize(5));
    }

    // exportImport
    /*
        exports a folder with a list, sampletype, assay, and imports them to the import project
        Then, validates expected data in source and destination
     */
    @Test
    public void testExportImportData()
    {
        beginAt(EXPORT_FOLDER_PATH + "/project-begin.view");
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
        // validate list items

        // samples

        // assay run and result domains
    }

    private void createListWithData(String containerPath)
    {
        beginAt(containerPath + "/project-begin.view");
        clickTab("Portal");

        ListHelper listHelper = new ListHelper(getDriver());
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

    private void createSampleTypeWithData(String sampleTypeName, String containerPath)
    {
        beginAt(containerPath + "/project-begin.view");
        clickTab("Portal");

        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addWebPart("Sample Types");

        log("adding sample type with file column");

        List<Map<String, String>> sampleFileData = new ArrayList<>();
        for (File file : DATAFILE_DIRECTORY.listFiles())
        {
            sampleFileData.add(Map.of("Name", file.getName(), "Color", "green",
                    "File", String.format("\"%s\"",file.getAbsolutePath())));
        }

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName)
                .setFields(List.of(new FieldDefinition("color", ColumnType.String),
                        new FieldDefinition("file", ColumnType.File)));
        sampleHelper.createSampleType(sampleTypeDefinition, sampleFileData);
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
        var resultFiles = new ArrayList<File>();
        List<Map<String, Object>> importData = new ArrayList<>();
        var uploadHelper = new WebDavUploadHelper(folderPath);  // put the files in the folder root so assay can resolve them
        for (int i = 0; i < 5; i++)
        {
            String fileName = String.format("results_file-%d.tsv", i);
            String result = String.format("result-%d", i);
            String fileText = "resultTxt\tresultFile\n"+
                    result+"\t" + fileName;
            var fieldFile = TestFileUtils.writeTempFile(fileName, fileText);
            uploadHelper.uploadFile(fieldFile);
            resultFiles.add(fieldFile);

            importData.add(Map.of(RESULT_TXT_COL, result, RESULT_FILE_COL, fieldFile.getName()));
        }

        // generate a run file, referencing the result files
        String importDataFileContents = TestDataUtils.tsvStringFromRowMaps(importData,
                List.of(RESULT_TXT_COL, RESULT_FILE_COL), true);
        File runFile = TestFileUtils.writeTempFile("runFile.tsv", importDataFileContents);
        uploadHelper.uploadFile(runFile);

        // build the import data
        List<Map<String, Object>> runRecords = new ArrayList<>();
        for (File resultFile : resultFiles)
        {
            runRecords.add(Map.of(RUN_TXT_COL, runFile.getName(), RUN_FILE_COL, runFile,
                    RESULT_TXT_COL, resultFile.getName(), RESULT_FILE_COL, resultFile));
        }

        ImportRunCommand importRunCommand = new ImportRunCommand(protocolId, runRecords);
        importRunCommand.setName("firstRun");
        importRunCommand.execute(createDefaultConnection(), folderPath);
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
