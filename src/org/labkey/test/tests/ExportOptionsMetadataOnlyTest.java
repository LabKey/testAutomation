package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.pages.admin.ImportFolderPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.DataClassAPIHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ExportOptionsMetadataOnlyTest extends BaseWebDriverTest
{
    private static final String IMPORT_FOLDER = "Folder to import";

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void setupProject()
    {
        ExportOptionsMetadataOnlyTest init = (ExportOptionsMetadataOnlyTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName());
        _containerHelper.createProject(IMPORT_FOLDER);

        goToProjectHome();
        PortalHelper _portalHelper = new PortalHelper(getDriver());
        _portalHelper.addWebPart("Lists");
        _portalHelper.addWebPart("Sample Types");
        _portalHelper.addWebPart("Data Classes");
    }

    @Test
    public void testDataClassExportOptions() throws IOException, CommandException
    {
        String dataClassName = "Export data class";
        goToProjectHome();

        DataClassDefinition testType = new DataClassDefinition(dataClassName).setFields(DataClassAPIHelper.dataClassTestFields());
        TestDataGenerator testDgen = DataClassAPIHelper.createEmptyDataClass(getProjectName(), testType);
        testDgen.addCustomRow(Map.of("Name", "class1", "intColumn", 1, "decimalColumn", 1.1, "stringColumn", "one"));
        testDgen.addCustomRow(Map.of("Name", "class2", "intColumn", 2, "decimalColumn", 2.2, "stringColumn", "two"));
        testDgen.insertRows();

        log("Export data class design only");
        ExportFolderPage exportFolderPage = goToFolderManagement().goToExportTab();
        File exportDataClassArchive = exportFolderPage.includeDataClassDesigns(true).includeDataClassData(false).exportToBrowserAsZipFile();

        importFile(IMPORT_FOLDER, exportDataClassArchive);

        goToProjectHome(IMPORT_FOLDER);
        clickAndWait(Locator.linkWithText(dataClassName));
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").waitFor();
        Assert.assertEquals("Data should not be imported when only design is selected", 0, table.getDataRowCount());

        log("Export data class data + design");
        goToProjectHome();
        exportFolderPage = goToFolderManagement().goToExportTab();
        exportDataClassArchive = exportFolderPage.includeDataClassDesigns(true).includeDataClassData(true).exportToBrowserAsZipFile();

        importFile(IMPORT_FOLDER, exportDataClassArchive);

        goToProjectHome(IMPORT_FOLDER);
        clickAndWait(Locator.linkWithText(dataClassName));
        table = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").waitFor();
        Assert.assertEquals("Data should be imported when only design is selected", 2, table.getDataRowCount());
    }

    @Test
    public void testSampleTypeExportOptions()
    {
        String sampleTypeName = "Export Sample type";
        goToProjectHome();

        List<FieldDefinition> fields = List.of(new FieldDefinition("Fruits", FieldDefinition.ColumnType.String), new FieldDefinition("Count", FieldDefinition.ColumnType.Integer));
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        sampleTypeHelper.createSampleType(new SampleTypeDefinition(sampleTypeName).setFields(fields));
        sampleTypeHelper.goToSampleType(sampleTypeName);
        sampleTypeHelper.insertRow(Map.of("Name", "S-1", "Fruits", "Apple", "Count", "1"));

        log("Export Sample type design only");
        ExportFolderPage exportFolderPage = goToFolderManagement().goToExportTab();
        File exportSampleTypeArchive = exportFolderPage.includeSampleTypeDesigns(true).includeSampleTypeData(false).exportToBrowserAsZipFile();

        importFile(IMPORT_FOLDER, exportSampleTypeArchive);

        goToProjectHome(IMPORT_FOLDER);
        clickAndWait(Locator.linkWithText(sampleTypeName));
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).withName("Material").waitFor();
        Assert.assertEquals("Data should not be imported when only design is selected", 0, table.getDataRowCount());

        log("Export data class data + design");
        goToProjectHome();
        exportFolderPage = goToFolderManagement().goToExportTab();
        exportSampleTypeArchive = exportFolderPage.includeSampleTypeDesigns(true).includeSampleTypeData(true).exportToBrowserAsZipFile();

        importFile(IMPORT_FOLDER, exportSampleTypeArchive);

        goToProjectHome(IMPORT_FOLDER);
        clickAndWait(Locator.linkWithText(sampleTypeName));
        table = new DataRegionTable.DataRegionFinder(getDriver()).withName("Material").waitFor();
        Assert.assertEquals("Data should be imported when only design is selected", 1, table.getDataRowCount());
    }

    @Test
    public void testListsExportOptions()
    {
        String listName = "Export List";
        goToProjectHome();

        _listHelper.createList(getProjectName(), listName, "id", new FieldDefinition("Color", FieldDefinition.ColumnType.String), new FieldDefinition("Shape", FieldDefinition.ColumnType.String));
        _listHelper.beginAtList(getProjectName(), listName);
        _listHelper.insertNewRow(Map.of("Color", "Yellow", "Shape", "Triangle"));

        log("Export List Design only");
        ExportFolderPage exportFolderPage = goToFolderManagement().goToExportTab();
        File exportListArchive = exportFolderPage.includeListDesigns(true).includeListData(false).exportToBrowserAsZipFile();

        importFile(IMPORT_FOLDER, exportListArchive);

        goToProjectHome(IMPORT_FOLDER);
        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").waitFor();
        Assert.assertEquals("Data should not be imported when only design is selected", 0, table.getDataRowCount());

        log("Export Data + Design");
        goToProjectHome();
        exportFolderPage = goToFolderManagement().goToExportTab();
        exportListArchive = exportFolderPage.includeListDesigns(true).includeListData(true).exportToBrowserAsZipFile();

        importFile(IMPORT_FOLDER, exportListArchive);

        goToProjectHome(IMPORT_FOLDER);
        clickAndWait(Locator.linkWithText(listName));
        table = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").waitFor();
        Assert.assertEquals("Data should be imported when only data is selected", 1, table.getDataRowCount());
    }

    @Test
    public void testAssayRunsExportOptions()
    {
        String assayName = "Export Assay";
        File runFile = new File(TestFileUtils.getSampleData("AssayImportExport"), "GenericAssay_Run1.xls");

        goToManageAssays();
        _assayHelper.createAssayDesign("General", assayName).clickSave();
        clickAndWait(Locator.linkWithText(assayName));
        DataRegionTable runTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").waitFor();
        runTable.clickHeaderButton("Import Data");
        clickButton("Next");
        checkRadioButton(Locator.radioButtonById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), runFile);
        sleep(1000);
        clickButton("Save and Finish");

        ExportFolderPage exportFolderPage = goToFolderManagement().goToExportTab();
        File exportAssayArchive = exportFolderPage.includeExperimentsAndRuns(true).includeExperimentRuns(false).exportToBrowserAsZipFile();

        importFile(IMPORT_FOLDER, exportAssayArchive);

        goToProjectHome(IMPORT_FOLDER);
        DataRegionTable table = goToManageAssays().clickAssay(assayName).getTable();
        Assert.assertEquals("Run data should not be imported when runs is not selected", 0, table.getDataRowCount());

        goToProjectHome();
        exportFolderPage = goToFolderManagement().goToExportTab();
        exportAssayArchive = exportFolderPage.includeExperimentsAndRuns(true).includeExperimentRuns(true).exportToBrowserAsZipFile();

        importFile(IMPORT_FOLDER, exportAssayArchive);

        goToProjectHome(IMPORT_FOLDER);
        table = goToManageAssays().clickAssay(assayName).getTable();
        Assert.assertEquals("Run data should not be imported when runs is not selected", 1, table.getDataRowCount());
    }

    private void importFile(String projectName, File exportFile)
    {
        cleanUpPipelineJobs(projectName);
        ImportFolderPage.beginAt(this, projectName).selectLocalZipArchive().chooseFile(exportFile).clickImportFolder();
        waitForPipelineJobsToFinish(1);
    }

    private void cleanUpPipelineJobs(String projectName)
    {
        goToProjectHome(projectName);
        goToModule("Pipeline");
        PipelineStatusTable table = new PipelineStatusTable(this);
        table.deleteAllPipelineJobs();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _containerHelper.deleteProject(IMPORT_FOLDER, false);
    }
}
