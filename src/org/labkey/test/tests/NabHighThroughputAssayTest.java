package org.labkey.test.tests;

import bsh.This;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 2/27/13
 */
public class NabHighThroughputAssayTest extends AbstractAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab High Throughput Test Verify Project";            //project for nab test
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";
    private static final String PLATE_TEMPLATE_NAME = "NabHighThroughputAssayTest Template";

    private final static String TEST_ASSAY_FLDR_NAB_RENAME = "Rename" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

    protected static final String TEST_ASSAY_NAB = "TestAssayHighThroughputNab";
    protected static final String TEST_ASSAY_NAB_DESC = "Description for High Throughput NAb assay";

    protected final static String TEST_ASSAY_USR_NAB_READER = "nabreader1@security.test";
    private final static String TEST_ASSAY_GRP_NAB_READER = "Nab Dataset Reader";   //name of Nab Dataset Readers group

    protected final String TEST_ASSAY_NAB_METADATA_FILE = getLabKeyRoot() + "/sampledata/Nab/NVITAL (short) metadata.xlsx";
    protected final String TEST_ASSAY_NAB_DATA_FILE = getLabKeyRoot() + "/sampledata/Nab/NVITAL (short) test data.xlsx";

    @Override
    protected void runUITests() throws Exception
    {
        doCreateSteps();
        doVerifySteps();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        log("Starting High Throughput Nab Assay Test");

        //revert to the admin user
        revertToAdmin();

        log("Testing NAb Assay Designer");

        //create a new test project
        _containerHelper.createProject(TEST_ASSAY_PRJ_NAB, null);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_NAB);

        // create a study so we can test copy-to-study later:
        clickFolder(TEST_ASSAY_PRJ_NAB);
        _containerHelper.createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, null);
        addWebPart("Study Overview");
        clickButton("Create Study");
        clickButton("Create Study");

        //add the Assay List web part so we can create a new nab assay
        _containerHelper.createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_NAB, null);
        clickFolder(TEST_ASSAY_PRJ_NAB);
        addWebPart("Assay List");

        //create a new nab assay
        clickButton("Manage Assays");

        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "TZM-bl Neutralization (NAb), High-throughput (Single Plate Dilution)"));
        clickButton("Next");

        log("Setting up NAb assay");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        getWrapper().type("//input[@id='AssayDesignerName']", TEST_ASSAY_NAB);
        getWrapper().type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_NAB_DESC);

        sleep(1000);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);

        clickAndWait(Locator.linkWithText("configure templates"));
        clickAndWait(Locator.linkWithText("new 384 well (16x24) NAb high-throughput (single plate dilution) template"));

        waitForElement(Locator.xpath("//input[@id='templateName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@id='templateName']"), PLATE_TEMPLATE_NAME);

        clickButton("Save & Close");
        assertTextPresent(PLATE_TEMPLATE_NAME);

        clickFolder(TEST_ASSAY_PRJ_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        clickEditAssayDesign(false);
        waitForElement(Locator.xpath("//select[@id='plateTemplate']"), WAIT_FOR_JAVASCRIPT);
        selectOptionByValue(Locator.xpath("//select[@id='plateTemplate']"), PLATE_TEMPLATE_NAME);

        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        clickFolder(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
        addWebPart("Assay List");

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        if (isFileUploadAvailable())
        {
            log("Uploading NAb Runs");
            clickButton("Import Data");
            clickButton("Next");

            setFormElement("cutoff1", "50");
            setFormElement("cutoff2", "70");
            selectOptionByText("curveFitMethod", "Polynomial");

            File metadata = new File(TEST_ASSAY_NAB_METADATA_FILE);
            setFormElement(Locator.xpath("//input[@type='file' and @name='__sampleMetadataFile']"), metadata);

            File data = new File(TEST_ASSAY_NAB_DATA_FILE);
            setFormElement(Locator.xpath("//input[@type='file' and @name='__primaryFile__']"), data);

            clickButton("Save and Finish");

            // verify expected sample names and virus names
            for (int i=1; i <= 20; i++)
            {
                assertTextPresent("SPECIMEN-" + i);

                // uncomment after 17303 is fixed
                //assertTextPresent("VIRUS-" + i);
            }

            click(Locator.linkContainingText("Change Curve Type"));
            clickAndWait(Locator.menuItem("Four Parameter"));


            click(Locator.linkContainingText("Change Curve Type"));
            clickAndWait(Locator.menuItem("Polynomial"));

            clickAndWait(Locator.linkContainingText("View Results"));

            // verify the correct number of records
            DataRegionTable table = new DataRegionTable("Data", this);
            assert(table.getDataRowCount() == 20);
        }
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_NAB;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        revertToAdmin();
        deleteProject(getProjectName(), afterTest);
        deleteDir(getTestTempDir());
    }
}
