package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({Assays.class, DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class GpatPlateTemplateTest extends BaseWebDriverTest
{
    public static final File TEST_PLATE_DATA = TestFileUtils.getSampleData("GPAT/plateData.xlsx");
    public static final File TEST_PLATE_METADATA = TestFileUtils.getSampleData("GPAT/plate-metadata-1.json");
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
        _containerHelper.createProject(getProjectName(), "Assay");
        setPipelineRoot(TestFileUtils.getSampleData("GPAT").getAbsolutePath(), false);

        goToProjectHome();
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        assayHelper.createAssayWithPlateSupport(ASSAY_NAME);
        assayHelper.createPlateTemplate(templateName, "blank", "GPAT (General)");

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
        table.clearAllFilters();

        table.setFilter("PlateData/control_well_groups", "Is Not Blank");
        checker().verifyEquals("Control well data is incorrect", Arrays.asList("positive", "negative"),
                table.getColumnDataAsText("PlateData/control_well_groups"));
        checker().verifyEquals("Well location is incorrect", Arrays.asList("A11", "A12"),
                table.getColumnDataAsText("WellLocation"));
        checker().verifyEquals("Dilution is incorrect", Arrays.asList("0.005", "1.0"),
                table.getColumnDataAsText(" PlateData/dilution"));
        table.clearAllFilters();

        table.setFilter("PlateData/control_well_groups", "Is Blank");
        checker().verifyEquals("Well Location is incorrect", Arrays.asList("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10"),
                table.getColumnDataAsText("WellLocation"));
        checker().verifyEquals("Sample well group is incorrect", Arrays.asList("SA01", "SA01", "SA01", "SA02", "SA02", "SA02", "SA03", "SA03", "SA04", "SA04"),
                table.getColumnDataAsText("PlateData/sample_well_groups"));
        checker().verifyEquals("Barcode is incorrect", Arrays.asList("BC_111", "BC_111", "BC_111", "BC_222", "BC_222", "BC_222", "BC_333", "BC_333", "BC_444", "BC_444"),
                table.getColumnDataAsText("PlateData/Barcode"));
        checker().verifyEquals("Dilution is incorrect", Arrays.asList("1.0", "1.0", "1.0", "2.0", "2.0", "2.0", "3.0", "3.0", "4.0", "4.0"),
                table.getColumnDataAsText(" PlateData/dilution"));
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
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        assayHelper.createPlateTemplate("Elisa template", "blank", "ELISA");
        assayHelper.createPlateTemplate("ELISpot template", "blank", "ELISpot");

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
}
