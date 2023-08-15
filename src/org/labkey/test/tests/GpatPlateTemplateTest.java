package org.labkey.test.tests;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.plate.PlateDesignerPage;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.QCAssayScriptHelper;
import org.labkey.test.util.UIAssayHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Assays.class, Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class GpatPlateTemplateTest extends BaseWebDriverTest
{
    private static final File TRANSFORM_SCRIPT = TestFileUtils.getSampleData("qc/transformNoop.jar");
    private static final File TEST_PLATE_DATA = TestFileUtils.getSampleData("GPAT/plateData.xlsx");
    // Issue 48470: Conversion error during assay API import with plate metadata
    private static final File TEST_PLATE_METADATA = TestFileUtils.getSampleData("GPAT/plate-metadata-1.json");
    private static final String ASSAY_NAME = "Assay with plate template";
    private static final String templateName = "GPAT";

    @BeforeClass
    public static void setUpProject()
    {
        GpatPlateTemplateTest init = (GpatPlateTemplateTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        new QCAssayScriptHelper(this).ensureEngineConfig();

        _containerHelper.createProject(getProjectName(), "Assay");
        setPipelineRoot(TestFileUtils.getSampleData("GPAT").getAbsolutePath(), false);

        goToProjectHome();
        new UIAssayHelper(this)
                .createAssayDesign("General", ASSAY_NAME)
                .setPlateMetadata(true)
                // Regression check for Issue 48293: Standard Assay with Plate Metadata & Transformation Script throws an error
                .addTransformScript(TRANSFORM_SCRIPT)
                .clickFinish();
        createPlateTemplate(templateName, "blank", "Standard", true);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return "GpatPlateTemplateTest Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testApiWithPlateTemplateAndPlateMetadata() throws Exception
    {
        String runName = "ImportRun API with plate template and plate metadata";
        int assayId = new APIAssayHelper(this).getIdFromAssayName(ASSAY_NAME, getProjectName());
        File plateDataCopy = new File(TestFileUtils.ensureTestTempDir(), "API_" + TEST_PLATE_DATA.getName());

        FileUtils.copyFile(TEST_PLATE_DATA, plateDataCopy);
        ImportRunCommand importRunCommand = new ImportRunCommand(assayId, plateDataCopy);
        importRunCommand.setProperties(Map.of("PlateTemplate", new APIAssayHelper(this).getPlateTemplateLsid(getProjectName(), templateName)));
        importRunCommand.setPlateMetadata(new JSONObject(TestFileUtils.getFileContents(TEST_PLATE_METADATA)));
        importRunCommand.setName(runName);
        ImportRunResponse response = importRunCommand.execute(createDefaultConnection(), getProjectName());

        Assert.assertTrue((Boolean)response.getParsedData().get("success"));
    }

    @Test
    public void testWithPlateTemplateAndPlateMetadata()
    {
        String assayId = "Run Data with plate template and plate metadata";

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        importPlateData(assayId, TEST_PLATE_DATA, templateName, TEST_PLATE_METADATA);

        clickAndWait(Locator.linkWithText(assayId));
        DataRegionTable table = new DataRegionTable("Data", getDriver());

        table.setFilter("Run/PlateTemplate", "Does Not Equal", templateName);
        checker().verifyEquals("Only GPAT should be present", 0, table.getDataRowCount());
        checker().screenShotIfNewError("rowsWithoutPlateTemplate");
        table.clearAllFilters();

        table.setFilter("PlateData/control_well_groups", "Is Not Blank");
        checker().verifyEquals("Control well data is incorrect", Arrays.asList("positive", "negative"),
                table.getColumnDataAsText("PlateData/control_well_groups"));
        checker().verifyEquals("Well location is incorrect", Arrays.asList("A11", "A12"),
                table.getColumnDataAsText("WellLocation"));
        checker().verifyEquals("Dilution is incorrect", Arrays.asList("0.005", "1.01"),
                table.getColumnDataAsText("PlateData/dilution"));
        checker().screenShotIfNewError("rowsInControlWells");
        table.clearAllFilters();

        table.setFilter("PlateData/control_well_groups", "Is Blank");
        checker().verifyEquals("Well Location is incorrect", Arrays.asList("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10"),
                table.getColumnDataAsText("WellLocation"));
        checker().verifyEquals("Sample well group is incorrect", Arrays.asList("SA01", "SA01", "SA01", "SA02", "SA02", "SA02", "SA03", "SA03", "SA04", "SA04"),
                table.getColumnDataAsText("PlateData/sample_well_groups"));
        checker().verifyEquals("Barcode is incorrect", Arrays.asList("BC_111", "BC_111", "BC_111", "BC_222", "BC_222", "BC_222", "BC_333", "BC_333", "BC_444", "BC_444"),
                table.getColumnDataAsText("PlateData/Barcode"));
        checker().verifyEquals("Dilution is incorrect", Arrays.asList("1.01", "1.01", "1.01", "2.01", "2.01", "2.01", "3.01", "3.01", "4.01", "4.01"),
                table.getColumnDataAsText(" PlateData/dilution"));
        checker().screenShotIfNewError("rowsOutsideControlWells");
        table.clearAllFilters();
    }

    @Test
    public void validateImportErrors()
    {
        String assayId = "Run Data with plate template";

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        importPlateData(assayId, TEST_PLATE_DATA, templateName, null);
        checker().verifyEquals("Invalid error message for missing plate metadata", "No data file was uploaded. Please select a file.",
                Locator.byClass("labkey-error").findElement(getDriver()).getText());

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        importPlateData(assayId, TEST_PLATE_DATA, null, TEST_PLATE_METADATA);
        checker().verifyEquals("Invalid error message for missing plate template", "PlateTemplate is required and must be of type Text (String).",
                Locator.byClass("labkey-error").findElement(getDriver()).getText());
    }

    @Test
    public void verifyTemplateOptions()
    {
        goToProjectHome();
        createPlateTemplate("Elisa template", "blank", "ELISA", false);
        createPlateTemplate("ELISpot template", "blank", "ELISpot", false);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        DataRegionTable table = new DataRegionTable("Runs", getDriver());
        table.clickHeaderButtonAndWait("Import Data");

        clickAndWait(Locator.tagWithText("span", "Next"));

        checker().verifyEquals("Only General Template options should be displayed", Arrays.asList("", "GPAT"), getSelectOptions(Locator.name("plateTemplate")));
    }

    private void importPlateData(String name, File plateData, @Nullable String templateName, @Nullable File plateMetadata)
    {
        DataRegionTable table = new DataRegionTable("Runs", getDriver());
        table.clickHeaderButtonAndWait("Import Data");

        clickAndWait(Locator.tagWithText("span", "Next"));

        log("Setting run Properties for " + name);
        setFormElement(Locator.name("name"), name);
        checkRadioButton(Locator.radioButtonById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), plateData);
        if (templateName != null)
            selectOptionByText(Locator.name("plateTemplate"), templateName);
        if (plateMetadata != null)
            setFormElement(Locator.name("__plateMetadataFile__"), TEST_PLATE_METADATA);

        clickAndWait(Locator.tagWithText("span", "Save and Finish"));

    }

    private void createPlateTemplate(String templateName, String templateType, String assayType, boolean configureWells)
    {
        PlateDesignerPage.PlateDesignerParams params = new PlateDesignerPage.PlateDesignerParams(8, 12);
        params.setTemplateType(templateType);
        params.setAssayType(assayType);
        PlateDesignerPage plateDesigner = PlateDesignerPage.beginAt(this, params);

        if (configureWells)
        {
            plateDesigner.createWellGroup("SAMPLE", "SA01");
            plateDesigner.createWellGroup("SAMPLE", "SA02");
            plateDesigner.createWellGroup("SAMPLE", "SA03");
            plateDesigner.createWellGroup("SAMPLE", "SA04");

            // mark the regions on the plate to use the well groups
            plateDesigner.selectWellsForWellgroup("CONTROL", "Positive", "A11", "H11");
            plateDesigner.selectWellsForWellgroup("CONTROL", "Negative", "A12", "H12");

            plateDesigner.selectWellsForWellgroup("SAMPLE", "SA01", "A1", "H3");
            plateDesigner.selectWellsForWellgroup("SAMPLE", "SA02", "A4", "H6");
            plateDesigner.selectWellsForWellgroup("SAMPLE", "SA03", "A7", "H8");
            plateDesigner.selectWellsForWellgroup("SAMPLE", "SA04", "A9", "H10");
        }
        plateDesigner.setName(templateName);
        plateDesigner.saveAndClose();
    }
}
