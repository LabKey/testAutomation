package org.labkey.test.tests.elisa;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayDataPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.pages.assay.elisa.ElisaRunDetailsPage;
import org.labkey.test.pages.assay.plate.PlateDesignerPage;
import org.labkey.test.pages.assay.plate.PlateTemplateListPage;
import org.labkey.test.util.ExperimentalFeaturesHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Assays.class})
public class ElisaMultiPlateAssayTest extends BaseWebDriverTest
{
    public final String EXP_FEATURE = "elisaMultiPlateSupport";
    static final File TEST_ASSAY_ELISA_FILE1 = TestFileUtils.getSampleData("Elisa/biotek_01.xlsx");
    static final File THREE_PLATE_MSD = TestFileUtils.getSampleData("Elisa/3plateMSD.csv");

    @Override
    protected void doCleanup(boolean afterTest)
    {
        // Need an extra-long timeout for deleting project
        // Issue 42163: Deleting experiment properties is slow on SQL server
        _containerHelper.deleteProject(getProjectName(), afterTest, 6 * 60_000);
    }

    @BeforeClass
    public static void setupProject()
    {
        ElisaMultiPlateAssayTest init = (ElisaMultiPlateAssayTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        goToProjectHome();
        new PortalHelper(this).addBodyWebPart("Assay List");
    }



    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testHighSpeedPlateTemplateIsNotAvailableWithoutExpFlagEnabled()
    {
        String highSpeedTemplateName = "new 384 well (16x24) ELISA high-throughput (multi plate) template";
        ExperimentalFeaturesHelper.disableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);
        PlateTemplateListPage listPage = PlateTemplateListPage.beginAt(this, getProjectName());
        assertThat("don't expect to see high-throughput ELISA options if exp feature is disabled",
                listPage.getTemplateOptions(), not(hasItem(highSpeedTemplateName)));

        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);
        listPage = PlateTemplateListPage.beginAt(this, getProjectName());
        assertThat("expect to see high-throughput ELISA option now with exp feature enabled",
                listPage.getTemplateOptions(), hasItem(highSpeedTemplateName));
    }

    @Test
    public void testExpFlagChangesAssayOptions() throws Exception
    {
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);
        String assayName = "test_assay_for_input_format_options";
        String plateName = "high_speed_384";
        createPlateTemplate(16, 24, "high-throughput (multi plate)", plateName );

        Protocol newAssay = getProtocolWithPlateTemplate(assayName, "ELISA", plateName);
        saveProtocol(createDefaultConnection(), newAssay);
        ExperimentalFeaturesHelper.disableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(assayName));

        ReactAssayDesignerPage designerPage = _assayHelper.clickEditAssayDesign();
        assertFalse("metadata input format is not present if exp flag is not set",
                designerPage.isMetadataInputFormatSelectPresent());

        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);
        refresh();
        designerPage = new ReactAssayDesignerPage(getDriver());
        assertTrue("metadata input format should be present if exp flag is set",
                designerPage.isMetadataInputFormatSelectPresent());

        assertThat("default input format should be Manual",
                designerPage.getMetadataInputFormat(), is("Manual"));
    }

    @Test
    public void testDetailsViewResiliencyWithRenamedFields() throws Exception
    {
        // arrange
        String assayName = "test_AssayWithDifferentFieldNames";
        String plateTemplateName = "high_speed_tangent";
        String runId = "has different spot and plate";
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);

        createPlateTemplate(16, 24, "high-throughput (multi plate)", plateTemplateName);
        Protocol newAssayProtocol = getProtocolWithPlateTemplate(assayName, "ELISA", plateTemplateName);

        // rename PlateName and Spot fields and ensure it still works
        Domain resultsDomain = newAssayProtocol.getDomains().stream()
                .filter(a -> a.getName().equals("Data Fields")).findFirst().orElse(null);
        assertNotNull("expect data fields to be on the assay", resultsDomain);
        List<org.labkey.remoteapi.domain.PropertyDescriptor> fields = resultsDomain.getFields();
        for(org.labkey.remoteapi.domain.PropertyDescriptor field : fields)
        {
            if(field.getName().equals("PlateName"))
                field.setName("PlateMoniker");
            if(field.getName().equals("Spot"))
                field.setName("Puff");
        }

        newAssayProtocol.setSelectedMetadataInputFormat("COMBINED");
        saveProtocol(createDefaultConnection(), newAssayProtocol);

        // act
        uploadHighSpeedFile(assayName, runId, THREE_PLATE_MSD, "4 Parameter");

        goToAssayRunsPage(assayName)
                .getTable().clickRowDetails(0);
        ElisaRunDetailsPage detailsPage = new ElisaRunDetailsPage(getDriver());

        assertFalse("don't expect plate filter to be present", detailsPage.isPlateSelectPresent());
        assertFalse("don't expect spot filter to be present", detailsPage.isSpotSelectPresent());

        assertTrue(detailsPage.getAlertWarning().isPresent());
        assertThat(detailsPage.getAlertWarning().get().getText(),
                is("Warning: the assay design is missing the following fields which may affect the various plotting features and display of this page: PlateName, Spot."));
    }

    @Test
    public void testMultiPlateHighThroughputAssayWith4ParamCurveFit() throws Exception
    {
        // arrange
        String assayName = "test_high_throughput_assay_with_4_Param";
        String plateTemplateName = "high_speed_fancy";
        String runId = "high-speed multiplate 4Param";
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);

        createPlateTemplate(16, 24, "high-throughput (multi plate)", plateTemplateName);
        Protocol newAssayProtocol = getProtocolWithPlateTemplate(assayName, "ELISA", plateTemplateName);
        newAssayProtocol.setSelectedMetadataInputFormat("COMBINED");
        saveProtocol(createDefaultConnection(), newAssayProtocol);

        // act
        uploadHighSpeedFile(assayName, runId, THREE_PLATE_MSD, "4 Parameter");

        // assert
        goToAssayRunsPage(assayName).clickViewResults().getDataTable()
                .assertPaginationText(1, 100, 4608);

        goToAssayRunsPage(assayName)
                .getTable().clickRowDetails(0);
        ElisaRunDetailsPage detailsPage = new ElisaRunDetailsPage(getDriver());

        // ensure that expected options are there, at least
        assertThat(detailsPage.getAvailableSamples(), hasItems("Sample 1", "Sample 2",
                "Sample 3", "Sample 4","Sample 5","Sample 6","Sample 7","Sample 8", "Sample 9",
                "Sample 10", "Sample 11", "Sample 12", "Sample 13", "Sample 14", "Sample 15",
                "Sample 16", "Sample 17", "Sample 18", "Sample 19", "Sample 20", "SARS-COV2-POS-3"));

        // verify some selected option values
        detailsPage.selectPlate("Plate_1LK05A1071")
                .selectSpot("1")
                .setSelectedSamples(List.of("Sample 11", "Sample 13"))
                .setSelectedControls(List.of("Blank (Diluent)"));

        // now go to the results view and expect the filters above to be in effect
        detailsPage.clickViewResultsGrid().getDataTable()
                .assertPaginationText(1, 35, 35);
        closeExtraWindows();
    }

    @Test
    public void testMultiPlateHighThroughputAssayWithLinearCurveFit() throws Exception
    {
        String assayName = "test_high_throughput_assay_with_linear_curve";
        String plateTemplateName = "high_speed_regular";
        String runId = "high-speed multiplate Linear";
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);

        createPlateTemplate(16, 24, "high-throughput (multi plate)", plateTemplateName);
        Protocol newAssayProtocol = getProtocolWithPlateTemplate(assayName, "ELISA", plateTemplateName);
        newAssayProtocol.setSelectedMetadataInputFormat("COMBINED");
        saveProtocol(createDefaultConnection(), newAssayProtocol);

        // act
        uploadHighSpeedFile(assayName, runId, THREE_PLATE_MSD, "Linear");

        // assert
        goToAssayRunsPage(assayName)
                .getTable().clickRowDetails(0);

        ElisaRunDetailsPage detailsPage = new ElisaRunDetailsPage(getDriver());
        detailsPage.selectPlate("Plate_1LK05AG072")
                .selectSpot("3")
                .setShowCurveFitLineCheckboxSelected(true);

        // confirm that selected spot/plate combinations show expected curve fit params
        List<Map<String, Object>> curveFitRows = executeSelectRowCommand(
                "assay.ELISA.test_high_throughput_assay_with_linear_curve", "CurveFit" ).getRows();
        Map<String, Object> fitParamsRow = curveFitRows.stream()
                .filter(a-> "Plate_1LK05AG072".equals(a.get("PlateName")) && a.get("Spot").equals(3))
                .findFirst().get();
        assertNotNull(fitParamsRow);
        String expectedFitParams = fitParamsRow.get("FitParameters").toString().replace(" ", "");

        assertTrue("Expect curve fit details to be selected by default", detailsPage.getShowCurveFitLineCheckboxSelected());
        assertTrue("Expect rSquared element to be visible", detailsPage.getRSquaredShown());
        assertTrue("Expect fitParameters to be visible", detailsPage.getCurveFitParamsShown());
        assertThat("Expect selection to reflect expected curve fit params",
                detailsPage.getCurveFitParams(), is(expectedFitParams));
    }

    @Test
    public void testRegular96WellStillWorks() throws Exception
    {
        String assayName = "test_regular_assay_with_linear_curve";
        String plateTemplateName = "new 96 well template";
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);

        createPlateTemplate(8, 12, "default", plateTemplateName);
        Protocol newAssayProtocol = getProtocolWithPlateTemplate(assayName, "ELISA", plateTemplateName);
        newAssayProtocol.setSelectedMetadataInputFormat("MANUAL");
        saveProtocol(createDefaultConnection(), newAssayProtocol);

        // act
        goToAssayRunsPage(assayName);
        AssayRunsPage runsPage = uploadFile(assayName, "manual linear run", TEST_ASSAY_ELISA_FILE1, "Linear",
                "A", 1, 5);

        AssayDataPage dataPage = runsPage.clickViewResults();
        dataPage.getDataTable().assertPaginationText(1, 92, 92);

        goToAssayRunsPage(assayName)
                .getTable().clickRowDetails(0);
        ElisaRunDetailsPage detailsPage = new ElisaRunDetailsPage(getDriver());

        assertFalse("Plate select should not be shown for single-plate assay", detailsPage.isPlateSelectPresent());
        assertFalse("Spot select should not be shown for single-plate assay", detailsPage.isSpotSelectPresent());

        assertThat("Expect these specimens to appear in the sample filter box",
                detailsPage.getAvailableSamples(),
                hasItems("specimen 1 A", "specimen 2 A", "specimen 3 A", "specimen 4 A", "specimen 5 A"));
        assertThat("expect just 1 control", detailsPage.getAvailableControls(), hasItems("Standard"));

        detailsPage.clickViewResultsGrid()
                .getDataTable()
                .assertPaginationText(1, 12, 12);

        closeExtraWindows();
    }

    private AssayRunsPage uploadHighSpeedFile(String assayName, String runName, File file, String curveFitMethod)
    {
        goToAssayRunsPage(assayName);
        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("curveFitMethod"), curveFitMethod);
        setFormElement(Locator.name("__primaryFile__"), file);
        clickButton("Save and Finish", 180000); // 3 minutes wait if need
        return new AssayRunsPage(getDriver());
    }

    private AssayRunsPage uploadFile(String assayName, String runName, File file, String curveFitMethod, String uniqueifier, int startSpecimen, int lastSpecimen)
    {
        goToAssayRunsPage(assayName);
        clickButton("Import Data");
        clickButton("Next");

        for (int i = startSpecimen; i <= lastSpecimen; i++)
        {
            Locator specimenLocator = Locator.name("specimen" + (i) + "_SpecimenID");
            Locator participantLocator = Locator.name("specimen" + (i) + "_ParticipantID");

            setFormElement(specimenLocator, "specimen " + (i) + " " + uniqueifier);
            setFormElement(participantLocator, "ptid " + (i) + " " + uniqueifier);

            setFormElement(Locator.name("specimen" + (i) + "_VisitID"), "" + (i));
        }

        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("curveFitMethod"), curveFitMethod);
        setFormElement(Locator.name("__primaryFile__"), file);
        clickButton("Next");

        String[] letters = {"A","B","C","D","E","F","G","H"};
        for (int i = 0; i <= 5; i++)
        {
            setFormElement(Locator.name(letters[i].toLowerCase()+"1"+letters[i]+"2_Concentration"), "" + (i + 1));
        }

        clickButton("Save and Finish");
        return new AssayRunsPage(getDriver());
    }

    private AssayRunsPage goToAssayRunsPage(String assayName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(assayName));
        return new AssayRunsPage(getDriver());
    }

    private Protocol getProtocolWithPlateTemplate(String assayName, String providerName, String plateTemplate) throws Exception
    {
        // get the template from the server
        Connection cn = createDefaultConnection();
        String containerPath = getProjectName();
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand(providerName);
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(cn, containerPath);

        // modify the protocol according to our needs
        Protocol newAssayProtocol = getProtocolResponse.getProtocol();
        newAssayProtocol.setName(assayName);
        newAssayProtocol.setSelectedPlateTemplate(plateTemplate);

        return newAssayProtocol;
    }

    private Protocol saveProtocol(Connection cn, Protocol protocol) throws Exception
    {
        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(protocol);
        ProtocolResponse savedProtocolResponse = saveProtocolCommand.execute(cn, getProjectName());
        return savedProtocolResponse.getProtocol();
    }

    private void createPlateTemplate(int rowCount, int colCount, String templateType, String name)
    {
        PlateDesignerPage.PlateDesignerParams params = new PlateDesignerPage.PlateDesignerParams(rowCount, colCount);
        params.setTemplateType(templateType);
        params.setAssayType("ELISA");
        PlateTemplateListPage listPage = PlateTemplateListPage.beginAt(this, getProjectName());
        PlateDesignerPage designerPage = listPage.clickNewPlate(params);

        designerPage.setName(name);
        designerPage.saveAndClose();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ElisaMultiPlateAssayTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("Elisa");
    }
}
