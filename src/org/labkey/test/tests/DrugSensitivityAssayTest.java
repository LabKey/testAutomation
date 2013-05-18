package org.labkey.test.tests;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 5/18/13
 */
public class DrugSensitivityAssayTest extends AbstractPlateBasedAssayTest
{
    private final static String TEST_ASSAY_PROJECT = "Drug Sensitivity Test Verify Project";
    private static final String PLATE_TEMPLATE_NAME = "DrugSensitivityAssayTest Template";

    protected static final String TEST_ASSAY_NAME = "TestAssayDrugSensitivity";
    protected static final String TEST_ASSAY_DESC = "Description for Drug Sensitivity assay";

    protected final String TEST_ASSAY_FILE1 = getLabKeyRoot() + "/sampledata/DrugSensitivity/1.txt";
    protected final String TEST_ASSAY_FILE2 = getLabKeyRoot() + "/sampledata/DrugSensitivity/2.txt";
    protected final String TEST_ASSAY_FILE3 = getLabKeyRoot() + "/sampledata/DrugSensitivity/3.txt";

    protected final String TEST_ASSAY_DATA_ACQUISITION_FILE1 = getLabKeyRoot() + "/sampledata/DrugSensitivity/acquisition1.xlsx";
    protected final String TEST_ASSAY_DATA_ACQUISITION_FILE2 = getLabKeyRoot() + "/sampledata/DrugSensitivity/acquisition2.xlsx";
    protected final String TEST_ASSAY_DATA_ACQUISITION_FILE3 = getLabKeyRoot() + "/sampledata/DrugSensitivity/acquisition3.xlsx";

    @Override @LogMethod
    protected void runUITests() throws Exception
    {
        log("Starting Drug Sensitivity Assay BVT Test");

        //revert to the admin user
        revertToAdmin();

        log("Testing Drug Sensitivity Assay Designer");

        // set up a scripting engine to run a java transform script
        prepareProgrammaticQC();

        //create a new test project
        _containerHelper.createProject(getProjectName(), null);

        //setup a pipeline for it
        setupPipeline(getProjectName());

        clickProject(getProjectName());
        addWebPart("Assay List");
        createTemplate();

        //create a new assay
        clickProject(getProjectName());
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton("providerName", "Drug Sensitivity");
        clickButton("Next");

        log("Setting up Drug Sensitivity assay");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("AssayDesignerName"), TEST_ASSAY_NAME);

        selectOptionByValue(Locator.xpath("//select[@id='plateTemplate']"), PLATE_TEMPLATE_NAME);
        setFormElement(Locator.id("AssayDesignerDescription"), TEST_ASSAY_DESC);

        clickButton("Save", 0);
        waitForText("Save successful.", 20000);

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAME));

        log("Uploading Drug Sensitivity Runs");
        clickButton("Import Data");
        clickButton("Next");
        uploadFile(TEST_ASSAY_FILE1, null);

        click(Locator.linkContainingText("Import Data"));
        clickButton("Next");
        uploadFile(TEST_ASSAY_FILE2, null);

        click(Locator.linkContainingText("Import Data"));
        clickButton("Next");
        uploadFile(TEST_ASSAY_FILE3, TEST_ASSAY_DATA_ACQUISITION_FILE3);

        // verify details view has a custom sample label
        assertTextPresent("Drug Treatment Information");

        click(Locator.linkContainingText("View Runs"));
        assertTextPresent("assaydata\\acquisition3");
        click(Locator.linkContainingText("3.txt"));

        DataRegionTable table = new DataRegionTable("Data", this);

        assert(table.getDataRowCount() == 3);
    }

    protected void uploadFile(String filePath, String acquisitionFilePath)
    {
        // cutoff values
        setFormElement(Locator.name("cutoff1"), "50");
        setFormElement(Locator.name("cutoff2"), "75");
        setFormElement(Locator.name("cutoff3"), "99");

        setFormElement(Locator.name("mediaType"), "serum");
        setFormElement(Locator.name("mediaFreezerProID"), "77768");
        setFormElement(Locator.name("totalEventPerWell"), "1000");

        setFormElement(Locator.name("experimentPerformer"), "John White");
        selectOptionByText(Locator.name("curveFitMethod"), "Four Parameter");

        // form values
        String[] drugs = {"GSK", "DSM-1", "Quinine"};
        String[] dilution = {"40000", "40000", "20000"};
        String[] factor = {"4", "4", "4"};
        String[] method = {"Concentration", "Concentration", "Concentration"};

        for (int i = 0; i < 3; i++)
        {
            Locator treatmentLocator = Locator.name("drug" + (i+1) + "_TreatmentName");
            Locator concentrationLocator = Locator.name("drug" + (i+1) + "_InitialDilution");
            Locator dilutionFactorLocator = Locator.name("drug" + (i+1) + "_Factor");
            Locator methodLocator = Locator.name("drug" + (i+1) + "_Method");

            setFormElement(treatmentLocator, drugs[i]);
            setFormElement(concentrationLocator, dilution[i]);
            setFormElement(dilutionFactorLocator, factor[i]);
            selectOptionByText(methodLocator, method[i]);
        }

        if (acquisitionFilePath != null)
        {
            File acquisitionFile = new File(acquisitionFilePath);
            setFormElement(Locator.name("dataAcquisitionFile"), acquisitionFile);
        }
        File file1 = new File(filePath);
        setFormElement(Locator.name("__primaryFile__"), file1);

        clickButton("Save and Finish");
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PROJECT;
    }

    @Override
    protected void createTemplate()
    {
        clickButton("Manage Assays");
        clickButton("Configure Plate Templates");
        clickAndWait(Locator.linkWithText("new 96 well (8x12) Drug Sensitivity default template"));
        Locator nameField = Locator.id("templateName");
        waitForElement(nameField, WAIT_FOR_JAVASCRIPT);
        setFormElement(nameField, PLATE_TEMPLATE_NAME);

        clickButton("Save & Close");
        waitForText(PLATE_TEMPLATE_NAME);
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
