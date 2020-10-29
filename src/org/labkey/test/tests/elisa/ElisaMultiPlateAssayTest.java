package org.labkey.test.tests.elisa;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayDataPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.pages.assay.plate.PlateDesignerPage;
import org.labkey.test.pages.assay.plate.PlateTemplateListPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExperimentalFeaturesHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

@Category({DailyB.class})
public class ElisaMultiPlateAssayTest extends BaseWebDriverTest
{
    public final String EXP_FEATURE = "elisaMultiPlateSupport";
    static final File TEST_ASSAY_ELISA_FILE1 = TestFileUtils.getSampleData("Elisa/biotek_01.xlsx");
    static final File TEST_ASSAY_ELISA_FILE2 = TestFileUtils.getSampleData("Elisa/biotek_02.xls");
    static final File TEST_ASSAY_ELISA_FILE3 = TestFileUtils.getSampleData("Elisa/biotek_03.xls");
    static final File TEST_ASSAY_ELISA_FILE4 = TestFileUtils.getSampleData("Elisa/biotek_04.xls");
    static final File THREE_PLATE_MSD = TestFileUtils.getSampleData("Elisa/3plateMSD.csv");

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

        assertThat("default input formt should be Manual", designerPage.getMetadataInputFormat(), is("Manual"));
    }

    @Test
    public void testMultiPlateHighThroughputAssayWith4ParamCurveFit() throws Exception
    {
        // arrange
        String assayName = "test_high_throughput_assay_with_4_Param";
        String plateTemplateName = "high_speed_fancy";
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);

        createPlateTemplate(16, 24, "high-throughput (multi plate)", plateTemplateName);
        Protocol newAssayProtocol = getProtocolWithPlateTemplate(assayName, "ELISA", plateTemplateName);
        newAssayProtocol.setSelectedMetadataInputFormat("COMBINED");
        saveProtocol(createDefaultConnection(), newAssayProtocol);

        // act
        uploadHighSpeedFile(assayName, "high speed multiplate first run", THREE_PLATE_MSD, "4 Parameter");

        // assert

        // sanity-check to see that we got data in the runs table at least
        goToAssayRunsPage(assayName);
        clickAndWait(Locator.linkWithText("view results"));
        DataRegionTable dataTable = DataRegionTable.DataRegion(getDriver()).withName("Data").waitFor();
        dataTable.assertPaginationText(1, 100, 4608);

        // confirm the new react-based run details view
        DataRegionTable runsTable = goToAssayRunsPage(assayName).getTable();
        runsTable.clickRowDetails(runsTable.getRowIndex("Assay Id", "high speed multiplate first run")); // should be row 0

        // todo: verify relevant/expected on assayRunDetails view
        
    }

    @Test
    public void testMultiPlateHighThroughputAssayWithLinearCurveFit() throws Exception
    {
        String assayName = "test_high_throughput_assay_with_linear_curve";
        String plateTemplateName = "high_speed_regular";
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(), EXP_FEATURE);

        createPlateTemplate(16, 24, "high-throughput (multi plate)", plateTemplateName);
        Protocol newAssayProtocol = getProtocolWithPlateTemplate(assayName, "ELISA", plateTemplateName);
        newAssayProtocol.setSelectedMetadataInputFormat("COMBINED");
        saveProtocol(createDefaultConnection(), newAssayProtocol);

        // act
        uploadHighSpeedFile(assayName, "high speed multiplate linear run", THREE_PLATE_MSD, "Linear");

        // assert

        // todo: verify
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

        // todo: verify view info

        log("foo");
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
