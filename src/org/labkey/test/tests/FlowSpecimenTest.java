package org.labkey.test.tests;

import org.junit.Assert;
import org.labkey.test.BaseFlowTestWD;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.Arrays;
import java.util.Collections;

/**
 * This test checks the flow specimen foreign key behavior from flow.FCSFiles and flow.FCSAnalyses.
 */
public class FlowSpecimenTest extends BaseFlowTestWD
{
    public static final String STUDY_FOLDER = "KoStudy";

    public static final String PTID = "P5216";
    public static final String DATE = "2012-09-12";

    public static final String SPECIMEN_DATA =
           "Vial Id\tDraw Date\tParticipant\tVolume\tUnits\tSpecimen Type\tDerivative Type\tAdditive Type\n" +
           "Sample_002\t" + DATE + "\t" + PTID + "\t100\tml\t\t\t\n" +
           "Sample_003\t11/13/12\tP7312\t200\tml\t\t\t";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @Override
    protected void init()
    {
        super.init();
        initializeStudyFolder();
    }

    @LogMethod
    private void initializeStudyFolder()
    {
        log("** Initialize Study Folder");
        createSubfolder(getProjectName(), getProjectName(), STUDY_FOLDER, "Study", null);
        clickButton("Create Study");
        //use date-based study
        click(Locator.xpath("(//input[@name='timepointType'])[1]"));
        setFormElement(Locator.xpath("//input[@name='startDate']"), "2012-01-01");
        clickButton("Create Study");

        log("** Import specimens");
        clickTab("Specimen Data");
        waitForElement(Locator.linkWithText("Import Specimens"));
        clickAndWait(Locator.linkWithText("Import Specimens"));
        setFormElementJS(Locator.id("tsv"), SPECIMEN_DATA);
        clickButton("Submit");
        assertTextPresent("Specimens uploaded successfully");
    }

    @Override
    protected void _doTestSteps() throws Exception
    {
        importFCSFiles();

        verifyFCSFileSpecimenFK();

        importFlowAnalysis();

        // Issue 16945: flow specimen FK doesn't work for 'fake' FCS file wells created during FlowJo import
        //verifyFCSAnalysisSpecimenFK();

        copyFlowResultsToStudy();

        // Issue 16945: flow specimen FK doesn't work for 'fake' FCS file wells created during FlowJo import
        //verifyFlowDatasetSpecimenFK();
    }

    @LogMethod
    protected void importFCSFiles()
    {
        log("** Import microFCS directory, set TargetStudy");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Browse for FCS files to be imported"));
        _extHelper.selectFileBrowserItem("flowjoquery/microFCS");
        _extHelper.waitForImportDataEnabled();
        selectImportDataAction("Import Directory of FCS Files");
        selectOptionByText(Locator.id("targetStudy"), "/" + getProjectName() + "/" + STUDY_FOLDER + " (" + STUDY_FOLDER + " Study)");
        clickButton("Import Selected Runs");
        waitForPipeline(getContainerPath());

        log("** Verify Target Study is set on FCSFile run");
        beginAt("/flow-run/" + getContainerPath() + "/showRuns.view");
        DataRegionTable table = new DataRegionTable("query", this);
        Assert.assertEquals(STUDY_FOLDER + " Study", table.getDataAsText(0, "Target Study"));
        click(Locator.linkWithText("details"));
        assertTextPresent(STUDY_FOLDER);

        log("** Set ICS protocol metadata");
        setProtocolMetadata("Keyword $SRC", null, null, null, false);
    }

    @LogMethod
    protected void importFlowAnalysis()
    {
        log("** Import workspace analysis");
        importAnalysis(getContainerPath(),
                "/flowjoquery/microFCS/microFCS.xml",
                SelectFCSFileOption.Previous,
                null,
                "microFCS",
                false,
                true);
    }

    @LogMethod
    private void copyFlowResultsToStudy()
    {
        // Copy the sample wells to the STUDY_FOLDER
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        clickCheckbox(".toggle");
        clickButton("Copy to Study");
        selectOptionByText("targetStudy", "/" + getProjectName() + "/" + STUDY_FOLDER + " (" + STUDY_FOLDER + " Study)");
        clickButton("Next");
        assertTitleContains("Copy to " + STUDY_FOLDER + " Study: Verify Results");
        // verify specimen information is filled in for '118795.fcs' FCS file
        Assert.assertEquals(PTID, getFormElement(Locator.name("participantId", 0)));
        Assert.assertEquals(DATE, getFormElement(Locator.name("date", 0)));
        clickButton("Copy to Study");

        assertTitleContains("Dataset: Flow");
        Assert.assertTrue("Expected go to STUDY_FOLDER container", getCurrentRelativeURL().contains("/" + STUDY_FOLDER));
        // PTID and Date from specimen vial 'Sample_002' from specimen repository
        assertTextPresent(PTID, DATE);
    }

    @LogMethod
    protected void verifyFCSFileSpecimenFK()
    {
        log("** Verify specimen FK from flow.FCSFile table");
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSFiles");
        verifySpecimenFK("");
    }

    @LogMethod
    protected void verifyFCSAnalysisSpecimenFK()
    {
        log("** Verify specimen FK from flow.FCSAnalysis table");
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        verifySpecimenFK("FCSFile/");
    }

    @LogMethod
    protected void verifyFlowDatasetSpecimenFK()
    {
        log("** Verify specimen FK from flow dataset");
        beginAt("/study/" + getProjectName() + "/" + STUDY_FOLDER + "/dataset.view?datasetId=5001");
        verifySpecimenFK("FCSFile/");
    }

    @LogMethod
    protected void verifySpecimenFK(String lookupPrefix)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn(lookupPrefix + "Specimen");
        _customizeViewsHelper.addCustomizeViewColumn(lookupPrefix + "Specimen/GlobalUniqueId");
        _customizeViewsHelper.addCustomizeViewColumn(lookupPrefix + "Specimen/Volume");
        _customizeViewsHelper.addCustomizeViewColumn(lookupPrefix + "Specimen/Specimen/SequenceNum");
        _customizeViewsHelper.saveCustomView();

        // verify the specimen columns are present
        DataRegionTable table = new DataRegionTable("query", this);
        int row = table.getRow("Name", "118795.fcs");
        Assert.assertEquals("Sample_002", table.getDataAsText(row, "Specimen"));
        Assert.assertEquals("Sample_002", table.getDataAsText(row, "Specimen Global Unique Id"));
        Assert.assertEquals("100.0", table.getDataAsText(row, "Specimen Volume"));
        Assert.assertEquals("20120912", table.getDataAsText(row, "Sequence Num"));
    }

}
