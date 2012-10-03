package org.labkey.test.tests;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.AbstractContainerHelper;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.UIAssayHelper;
import org.testng.Assert;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/11/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpecimenProgressReportTest extends BaseSeleniumWebTest
{
    public static final String STUDY_PIPELINE_ROOT = "C:/labkey_base/sampledata/specimenprogressreport";
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
        return "Foo";
    }

    public boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _assayHelper = new UIAssayHelper(this); //todo:  would like to use api, need to see if batch fields are necessary
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), studyFolder, "Study");
        importFolderFromZip(STUDY_PIPELINE_ROOT + "/study.folder.zip");

        createSpecimenFolder();
        waitForText("This assay is unlocked");
        assertTextPresent("30 collections have occurred.",  "48 results from PCR have been uploaded", "46 PCR queries");
        //"5 results from RNA have been uploaded",                                                  , "1 RNA queries"
        assertTextNotPresent("Configuration error:",
                "You must first configure the assay(s) that you want to run reports from. Click on the customize menu for this web part and select the Assays that should be included in this report.");



        sleep(8000);
//        _ext4Helper.selectRadioButtonByText("PCR");
//        sleep(4000);

        Locator.XPathLocator table = Locator.xpath("//table[@id='dataregion_ProgressReport']");//Locator.xpath("//table[contains(@class,'labkey-data-region')]");
//        getSimpleTableCell(table, 4,3);
        Assert.assertTrue(getAttribute(getSimpleTableCell(table, 3, 4), "style").contains("green"));
        Assert.assertTrue(getAttribute(getSimpleTableCell(table, 3,5), "style").contains("red"));
        Assert.assertTrue(getAttribute(getSimpleTableCell(table, 4,11), "style").contains("orange"));

        flagSpecimenForReview();
        sleep(5000);

//        _ext4Helper.selectRadioButtonByText("PCR");
        Assert.assertTrue(getAttribute(getSimpleTableCell(table, 4,12), "style").contains("flagged.png"));


//        clic
//            _customizeViewsHelper.addCustomizeViewColumn("flag", "Flag");
    }

    private void flagSpecimenForReview()
    {
        clickLinkWithText(assay1);
        clickLinkWithText(assay1File);

        click(Locator.tagWithAttribute("img", "title", "Flag for review"));
        clickButton("OK", 0);

        clickLinkWithText(assayFolder);
    }

    private void createSpecimenFolder() throws CommandException, IOException
    {
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), assayFolder, "Assay");
        enableModule(assayFolder, "rho");

        _assayHelper.uploadXarFileAsAssayDesign(STUDY_PIPELINE_ROOT + "/assays/PCR.xar", ++pipelineCount, "PCR");
        assay1File = "PCR Data.tsv";
        _assayHelper.importAssay("PCR", STUDY_PIPELINE_ROOT + "/assays/" + assay1File,  getProjectName() + "/" + assayFolder );
        clickLinkWithText(assayFolder);
        _assayHelper.uploadXarFileAsAssayDesign(STUDY_PIPELINE_ROOT + "/assays/RNA.xar", ++pipelineCount, "RNA");
        _assayHelper.importAssay("RNA", STUDY_PIPELINE_ROOT + "/assays/RNA Data.tsv",  getProjectName() + "/" + assayFolder );


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
        sleep(10000);
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
//        setFormElement(Locator.name("groupingColumn"), "gene");
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
