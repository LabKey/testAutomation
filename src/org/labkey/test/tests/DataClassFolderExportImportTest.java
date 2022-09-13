package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.DataClassAPIHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@Category({Daily.class})
public class DataClassFolderExportImportTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "DataClassExportFolderTest";
    private static final String IMPORT_PROJECT_NAME = "DataClassImportFolderTest";

    private final File DATAFILE_DIRECTORY = TestFileUtils.getSampleData("fileTypes");
    private final File SAMPLE_CSV = new File(DATAFILE_DIRECTORY, "csv_sample.csv");
    private final File SAMPLE_JPG = new File(DATAFILE_DIRECTORY, "jpg_sample.jpg");
    private final File SAMPLE_PDF = new File(DATAFILE_DIRECTORY, "pdf_sample.pdf");
    private final File SAMPLE_TIF = new File(DATAFILE_DIRECTORY, "tif_sample.tif");
    private List<File> _attachments = List.of(SAMPLE_CSV, SAMPLE_JPG, SAMPLE_PDF, SAMPLE_TIF);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _containerHelper.deleteProject(IMPORT_PROJECT_NAME, false);
    }

    @BeforeClass
    public static void setupProject()
    {
        DataClassFolderExportImportTest init = (DataClassFolderExportImportTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        // create the import project because we'll need it
        _containerHelper.createProject(IMPORT_PROJECT_NAME);

        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME);

        goToProjectHome(PROJECT_NAME);
        portalHelper.doInAdminMode(ph -> {
            ph.addWebPart("Data Classes");
            ph.addWebPart("Experiment Runs");
        });
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testExportImportSimpleDataClass() throws Exception
    {
        String subfolder = "simpleDataClassExportFolder";
        String subfolderPath = getProjectName() + "/" + subfolder;
        String testDataClass = "testData?<>*/Class";    // having the dataClass with non-file-legal chars in it is intentional,
                                                        // the import/export processes will write temp-files with names derived from
                                                        // the dataClass name.  This ensures that the sanitized name on export can
                                                        // be successfully matched up with the intended dataClass on import
        String importFolder = "simpleDataClassImportFolder";

        // create a subfolder in the import destination project
        _containerHelper.createSubfolder(IMPORT_PROJECT_NAME, importFolder);
        // create a subfolder in the project to hold types we'll export in this test method
        _containerHelper.createSubfolder(getProjectName(), subfolder);

        DataClassDefinition testType = new DataClassDefinition(testDataClass).setFields(DataClassAPIHelper.dataClassTestFields());

        TestDataGenerator testDgen = DataClassAPIHelper.createEmptyDataClass(subfolderPath, testType);

        testDgen.addCustomRow(Map.of("Name", "class1", "intColumn", 1, "decimalColumn", 1.1, "stringColumn", "one"));
        testDgen.addCustomRow(Map.of("Name", "class2", "intColumn", 2, "decimalColumn", 2.2, "stringColumn", "two"));
        testDgen.addCustomRow(Map.of("Name", "class3", "intColumn", 3, "decimalColumn", 3.3, "stringColumn", "three"));
        testDgen.addCustomRow(Map.of("Name", "class4", "intColumn", 4, "decimalColumn", 4.4, "stringColumn", "four"));
        testDgen.addCustomRow(Map.of("Name", "class5", "intColumn", 5, "decimalColumn", 5.5, "stringColumn", "five"));
        testDgen.insertRows();

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.doInAdminMode(ph -> {
            ph.addWebPart("Data Classes");
            ph.addWebPart("Experiment Runs");
        });

        DataRegionTable sourceRunsTable = DataRegionTable.DataRegion(getDriver()).withName("Runs").waitFor();
        List<String> runNames = sourceRunsTable.getColumnDataAsText("Name");

        clickAndWait(Locator.linkWithText(testDataClass));
        DataRegionTable sourceTable = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        for (int i=0; i<_attachments.size(); i++)
        {                           // for the nonce, we cannot add file attachments to an attachment column via remoteAPI
                                    // issue https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=42191 tracks this
                                    // until it is fixed we will have to add attachments via the UI, like this
            sourceTable.clickEditRow(i);
            setFormElement(Locator.input("quf_attachmentColumn"), _attachments.get(i));
            clickButton("Submit");
        }
        List<Map<String, String>> sourceRowData = sourceTable.getTableData();

        // act - export to file and import the file to our designated import directory
        goToFolderManagement()
                .goToExportTab();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        goToProjectFolder(IMPORT_PROJECT_NAME, importFolder);
        importFolderFromZip(exportedFolderFile, false, 1);
        goToProjectFolder(IMPORT_PROJECT_NAME, importFolder);

        List<String> importedRunNames = DataRegionTable.DataRegion(getDriver()).withName("Runs").waitFor()
                .getColumnDataAsText("Name");
        for (String sourceRun : runNames)
        {
            assertThat("expect all runs to come through", importedRunNames, hasItems(sourceRun));
        }

        clickAndWait(Locator.linkWithText(testDataClass));
        DataRegionTable destTable = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();;

        // capture the data in the exported sampleType
        List<Map<String, String>> destRowData = destTable.getTableData();

        // now ensure expected data in the sampleType made it to the destination folder
        for (Map exportedRow : sourceRowData)
        {
            // find the map from the exported project with the same name
            Map<String, String> matchingMap = destRowData.stream().filter(a-> a.get("Name").equals(exportedRow.get("Name")))
                    .findFirst().orElse(null);
            assertNotNull("expect all matching rows to come through", matchingMap);
            assertEquals("Expect imported rows to be equivalent to exported ones", exportedRow, matchingMap);
        }
    }

    @Test
    public void testExportImportMissingValueDataClass() throws Exception
    {
        final String MV_INDICATOR_01 = "Q";
        final String MV_DESCRIPTION_01 = "Data currently under quality control review.";
        final String MV_INDICATOR_02 = "N";
        final String MV_DESCRIPTION_02 = "Required field marked by site as 'data not available'.";
        final String MV_INDICATOR_03 = "X";
        final String MV_DESCRIPTION_03 = "Here is a non system one.";

        List<Map<String, String>> missingValueIndicators = new ArrayList<>();
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_01, "description", MV_DESCRIPTION_01));
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_02, "description", MV_DESCRIPTION_02));
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_03, "description", MV_DESCRIPTION_03));

        final String REQUIRED_FIELD = "requiredField";
        final String MISSING_FIELD = "missingField";
        final String INDICATOR_FIELD = MISSING_FIELD + "MVIndicator";

        String subfolder = "missingValueDataClassExportFolder";
        String subfolderPath = getProjectName() + "/" + subfolder;
        String testDataClass = "missing?Value*Data//Class";     // having the dataClass with non-file-legal chars in it is intentional,
                                                                // the import/export processes will write temp-files with names derived from
                                                                // the dataClass name.  This ensures that the sanitized name on export can
                                                                // be successfully matched up with the intended dataClass on import
        String importFolder = "missingValueDataClassImportFolder";

        // create a subfolder in the import destination project
        _containerHelper.createSubfolder(IMPORT_PROJECT_NAME, importFolder);
        // create a subfolder in the project to hold types we'll export in this test method
        _containerHelper.createSubfolder(getProjectName(), subfolder);

        navigateToFolder(getProjectName(), subfolder);
        setupMVIndicators(missingValueIndicators);

        // put a dataclass in the export subfolder and add test data to it
        List<FieldDefinition> testFields = new ArrayList<>();
        testFields.add(new FieldDefinition("requiredField", FieldDefinition.ColumnType.String)
                .setMvEnabled(false)
                .setRequired(true));
        testFields.add(new FieldDefinition("missingValueField", FieldDefinition.ColumnType.String)
                .setMvEnabled(true)
                .setRequired(false));

        DataClassDefinition testType = new DataClassDefinition(testDataClass).setFields(testFields);
        TestDataGenerator testDgen = DataClassAPIHelper.createEmptyDataClass(subfolderPath, testType);

        testDgen.addCustomRow(Map.of("Name", "firstRow", REQUIRED_FIELD, "first_required",
                MISSING_FIELD, "has value", INDICATOR_FIELD, ""));
        testDgen.addCustomRow(Map.of("Name", "secondRow", REQUIRED_FIELD, "second_required",
                MISSING_FIELD, "", INDICATOR_FIELD, "Q"));
        testDgen.addCustomRow(Map.of("Name", "thirdRow", REQUIRED_FIELD, "third_required",
                MISSING_FIELD, "has value", INDICATOR_FIELD, ""));
        testDgen.addCustomRow(Map.of("Name", "fourthRow", REQUIRED_FIELD, "fourth_required",
                MISSING_FIELD, "", INDICATOR_FIELD, ""));
        testDgen.addCustomRow(Map.of("Name", "fifthRow", REQUIRED_FIELD, "fifth_required",
                MISSING_FIELD, "", INDICATOR_FIELD, "X"));
        testDgen.addCustomRow(Map.of("Name", "sixthRow", REQUIRED_FIELD, "sixth_required",
                MISSING_FIELD, "", INDICATOR_FIELD, "N"));
        testDgen.insertRows();

        navigateToFolder(getProjectName(), subfolder);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.doInAdminMode(ph -> {
            ph.addWebPart("Data Classes");
            ph.addWebPart("Experiment Runs");
        });

        // now add the missing value column to the view on the dataclass's dataregionTable
        clickAndWait(Locator.linkWithText(testDataClass));
        DataRegionTable exportTable = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        addHiddenColumnToDefaultView(exportTable, "missingValueFieldMVIndicator");

        // capture the data before exporting
        List<Map<String, String>> sourceRowData = exportTable.getTableData();

        // act - export to file and import the file to our designated import directory
        goToFolderManagement()
                .goToExportTab();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        goToProjectFolder(IMPORT_PROJECT_NAME, importFolder);
        importFolderFromZip(exportedFolderFile, false, 1);
        goToProjectFolder(IMPORT_PROJECT_NAME, importFolder);

        portalHelper = new PortalHelper(this);
        portalHelper.doInAdminMode(ph -> {
            ph.addWebPart("Data Classes");
            ph.addWebPart("Experiment Runs");
        });

        clickAndWait(Locator.linkWithText(testDataClass));
        DataRegionTable importTable = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        addHiddenColumnToDefaultView(importTable, "missingValueFieldMVIndicator");

        // capture the data after importing
        List<Map<String, String>> destRowData = importTable.getTableData();

        // now ensure expected data in the sampleType made it to the destination folder
        for (Map exportedRow : sourceRowData)
        {
            // find the map from the exported project with the same name
            Map<String, String> matchingMap = destRowData.stream().filter(a-> a.get("Name").equals(exportedRow.get("Name")))
                    .findFirst().orElse(null);
            assertNotNull("expect all matching rows to come through", matchingMap);
            assertEquals("Expect imported rows to be equivalent to exported ones", exportedRow, matchingMap);
        }
    }

    private void addHiddenColumnToDefaultView(DataRegionTable table, String column)
    {
        CustomizeView cv = table.openCustomizeGrid();
        cv.showHiddenItems();
        cv.addColumn(column);
        cv.saveDefaultView();
    }

    @LogMethod
    private void setupMVIndicators(List<Map<String, String>> missingValueIndicators)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Missing Values"));
        uncheckCheckbox(Locator.checkboxById("inherit"));

        // Delete all site-level settings
        for (WebElement deleteButton : Locator.tagWithAttribute("img", "alt", "delete").findElements(getDriver()))
        {
            deleteButton.click();
            shortWait().until(ExpectedConditions.stalenessOf(deleteButton));
        }

        for(int index = 0; index < missingValueIndicators.size(); index++)
        {
            clickButton("Add", 0);
            WebElement mvInd = Locator.css("#mvIndicatorsDiv input[name=mvIndicators]").index(index).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvInd, missingValueIndicators.get(index).get("indicator"));
            WebElement mvLabel = Locator.css("#mvIndicatorsDiv input[name=mvLabels]").index(index).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvLabel, missingValueIndicators.get(index).get("description"));
        }
        clickButton("Save");
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
        return Arrays.asList("experiment");
    }
}
