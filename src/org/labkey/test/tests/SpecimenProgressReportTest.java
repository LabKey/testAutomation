package org.labkey.test.tests;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.AbstractContainerHelper;
import org.labkey.test.util.ListHelper;
import org.junit.Assert;
import org.labkey.test.util.UIAssayHelper;

import java.io.IOException;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/11/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpecimenProgressReportTest extends BaseSeleniumWebTest
{
    public static final String STUDY_PIPELINE_ROOT = getLabKeyRoot() + "/sampledata/specimenprogressreport";
    public static final String assay1 = "PCR";
    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    String studyFolder = "study folder";
    String assayFolder = "assay folder";
    private String specimenFile = "progress_report_test_data.xlsx";
    int pipelineCount = 0;
    private String assay1File = "PCR Data.tsv";

    @Override
    protected String getProjectName()
    {
        //Issue 16247: tricky characters in project name cause alert when trying to add a lookup to a rho query in the folder
        return "Specimen Progress Report Test" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    public boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _assayHelper = new UIAssayHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), studyFolder, "Study");
        importFolderFromZip(STUDY_PIPELINE_ROOT + "/Study.folder.zip");

        createSpecimenFolder();
        waitForText("This assay is unlocked");
        assertTextPresent("30 collections have occurred.",  "48 results from PCR have been uploaded", "46 PCR queries");
        //"5 results from RNA have been uploaded",                                                  , "1 RNA queries"
        assertTextNotPresent("Configuration error:",
                "You must first configure the assay(s) that you want to run reports from. Click on the customize menu for this web part and select the Assays that should be included in this report.");



//        _ext4Helper.selectRadioButtonByText("PCR");
//        sleep(4000);

        Locator.XPathLocator table = Locator.xpath("//table[@id='dataregion_ProgressReport']");//Locator.xpath("//table[contains(@class,'labkey-data-region')]");
        waitForElement(table);
        Assert.assertEquals(2, getXpathCount( Locator.xpath("//td[contains(@style, 'background:green')]")));
        Assert.assertEquals(21, getXpathCount( Locator.xpath("//td[contains(@style, 'background:red')]")));
        Assert.assertEquals(1, getXpathCount( Locator.xpath("//td[contains(@style, 'background:orange')]")));
        Assert.assertEquals(0, getXpathCount(Locator.xpath("//td[contains(@style, 'flagged.png')]")));

        flagSpecimenForReview();

        waitForElement(table);
        Assert.assertEquals(1, getXpathCount(Locator.xpath("//td[contains(@style, 'flagged.png')]")));

        // verify legend text and ordering
        assertTextPresentInThisOrder("specimen collected", "specimen received by lab", "specimen received but invalid", "assay results available", "query");

        // verify setting the PCR additional grouping column
        assertTextPresent("46 PCR queries");
        configureGroupingColumn("PCR", "gene");
        waitForElement(table);
        clickLinkWithText("48 results from PCR have been uploaded.");
        assertTextPresent("Participant Visit not found", 6);
        assertTextPresent("2 duplicates found", 4);
    }

    private void flagSpecimenForReview()
    {
        clickLinkWithText(assay1);
        clickLinkWithText(assay1File);

        click(Locator.tagWithAttribute("img", "title", "Flag for review"));
        clickButton("OK", 0);
        waitForElement(Locator.tagWithAttribute("img", "title", "Flagged for review"));

        clickLinkWithText(assayFolder);
    }

    private void createSpecimenFolder() throws CommandException, IOException
    {
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), assayFolder, "Assay");
        enableModule(assayFolder, "rho");

        _assayHelper.uploadXarFileAsAssayDesign(STUDY_PIPELINE_ROOT + "/assays/PCR.xar", ++pipelineCount, "PCR");
        assay1File = "PCR Data.tsv";
        _assayHelper.importAssay("PCR", STUDY_PIPELINE_ROOT + "/assays/" + assay1File,  getProjectName() + "/" + assayFolder, Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo") );
        clickLinkWithText(assayFolder);
        _assayHelper.uploadXarFileAsAssayDesign(STUDY_PIPELINE_ROOT + "/assays/RNA.xar", ++pipelineCount, "RNA");
        _assayHelper.importAssay("RNA", STUDY_PIPELINE_ROOT + "/assays/RNA Data.tsv",  getProjectName() + "/" + assayFolder, Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo") );


        clickLinkWithText(assayFolder);
        addWebPart("Assay Progress Dashboard");
        addWebPart("Assay Progress Report");
        assertTextPresent("You must first configure the assay(s) that you want to run reports from. Click on the customize menu for this web part and select the Assays that should be included in this report", 2);

        configureAssayProgressDashboard();
        configureAssaySchema("PCR", true);
//        configureAssaySchema("RNA", false);
        clickLinkWithText(assayFolder);
    }

    private void configureAssaySchema(String assayName, boolean showFlag)
    {
        ListHelper.LookupInfo lookupInfo = new ListHelper.LookupInfo("", "rho", assayName + " Query");
        _assayHelper.addAliasedFieldToMetadata(assayName + " Data", "rowid", "qcmessage", lookupInfo);
        clickButton("Save", 0);
        waitForText("Save successful.");
        clickButton("View Data");

        log("Set up data viewto include QC message");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("qcmessage/QueryMessage", "Query Message");
        _customizeViewsHelper.saveCustomView();
    }

    private void configureAssayProgressDashboard()
    {
        clickWebpartMenuItem("Assay Progress Dashboard", true, "Customize");
        _extHelper.checkCheckbox(assay1);
//        _extHelper.checkCheckbox("RNA");
        click(Locator.tagContainingText("label", "Specimen Report Study Folder Study"));
        clickButton("Save");
    }

    private void configureGroupingColumn(String label, String name)
    {
        clickWebpartMenuItem("Assay Progress Dashboard", true, "Customize");
        setFormElement(Locator.xpath("//td/label[contains(text(),'" + label + ":')]/../../td/input[@name='groupingColumn']"), name);
        clickButton("Save");
    }

    @Override
    protected void doCleanup() throws Exception
    {
        deleteProject(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
