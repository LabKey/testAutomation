package org.labkey.test.tests;

/**
 * Created with IntelliJ IDEA.
 * User: RyanS
 * Date: 7/30/14
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */

import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.LogMethod;

import java.io.File;

@Category({DailyA.class, Assays.class})
public class NabMultiVirusPlateTest extends AbstractAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab Multi Virus Test Verify Project";            //project for nab test
    private final static String TEST_ASSAY_FLDR_NAB = "nabmvassay";
    private static final String PLATE_TEMPLATE_NAME = "NabMultiVirusTest Template";
    private static final String TARGET_STUDY = "NAb multi virus study";
    private static final String TARGET_STUDY_DESC = "";

    protected static final String MULTI_VIRUS_ASSAY_NAB = "MultiVirusNab";
    protected static final String MULTI_VIRUS_ASSAY_NAB_DESC = "Description for Multi Virus NAb assay";

    protected final String TEST_ASSAY_NAB_MV_FILE1 = TestFileUtils.getLabKeyRoot() + "/sampledata/Nab/SpectraMax/20140612_0588.txt";
//    protected final String TEST_ASSAY_NAB_MV_FILE2 = getLabKeyRoot() + "/sampledata/Nab/Envision/4 plate data set _001.csv";
//    protected final String TEST_ASSAY_NAB_MV_FILE3 = getLabKeyRoot() + "/sampledata/Nab/Envision/4 plate data set _002.csv";
//    protected final String TEST_ASSAY_NAB_MV_FILE4 = getLabKeyRoot() + "/sampledata/Nab/16AUG11 KK CD3-1-1.8.xls";
    protected final String[] WELLGROUP_NAMES = {"Specimen 1:Virus 2", "Specimen 1:Virus 1", "Specimen 2:Virus 2", "Specimen 3:Virus 1", "Specimen 3:Virus 2", "Specimen 4:Virus 1", "Specimen 5:Virus 1", "Specimen 5:Virus 2", "Specimen 6:Virus 1", "Specimen 6:Virus 2", "Specimen 7:Virus 1", "Specimen 7:Virus 2", "Specimen 8:Virus 1", "Specimen 8:Virus 2", "Specimen 9:Virus 1", "Specimen 9:Virus 2", "Specimen 10:Virus 1", "Specimen 10:Virus 2"};

    @Nullable
    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_NAB;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void runUITests() throws Exception
    {
        doCreateSteps();
        doVerifySteps();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        //create a new test project
        _containerHelper.createProject(TEST_ASSAY_PRJ_NAB, null);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_NAB);

        clickProject(TEST_ASSAY_PRJ_NAB);
//        _containerHelper.createSubfolder(TEST_ASSAY_PRJ_NAB, TARGET_STUDY, null);
//        addWebPart("Study Overview");
//        clickButton("Create Study");
//        //create with default values
//        clickButton("Create Study");

        clickProject(TEST_ASSAY_PRJ_NAB);
        addWebPart("Assay List");

        //clickFolder(TARGET_STUDY);
        //addWebPart("Assay List");
        //create a new nab assay
        clickButton("Manage Assays");

        clickButton("Configure Plate Templates");
        clickAndWait(Locator.linkWithText("new 384 well (16x24) NAb multi-virus plate template"));

        waitForElement(Locator.xpath("//input[@id='templateName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@id='templateName']"), PLATE_TEMPLATE_NAME);

        clickButton("Save & Close");

        clickProject(TEST_ASSAY_PRJ_NAB);
        //clickFolder(TARGET_STUDY);
        //addWebPart("Assay List");

        createAssay(MULTI_VIRUS_ASSAY_NAB, MULTI_VIRUS_ASSAY_NAB_DESC);
        waitAndClick(Locator.linkWithText(MULTI_VIRUS_ASSAY_NAB));
        importPlateData(TEST_ASSAY_NAB_MV_FILE1, "Polynomial");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        log("verifying multi plate NAb assay");
        click(Locator.linkWithText("View Results"));
        verifyWellgroupNamesPresent(WELLGROUP_NAMES);
    }

    private void createAssay(String name, String description)
    {
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "TZM-bl Neutralization (NAb)"));
        clickButton("Next");

        log("Setting up NAb MV assay");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), name);
        setFormElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), description);
        selectOptionByValue(Locator.xpath("//select[@id='plateTemplate']"), PLATE_TEMPLATE_NAME);
        sleep(1000);
        clickButton("Save & Close");
    }

    private void importPlateData(String fileName, String curveFitMethod)
    {
        log("Uploading NAb Runs");
        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("cutoff1"), "50");
        setFormElement(Locator.name("cutoff2"), "70");
        selectOptionByText(Locator.name("curveFitMethod"), curveFitMethod);
        setFormElement(Locator.name("specimen1_InitialDilution"), "5");
        setFormElement(Locator.name("specimen1_Factor"), "42");
        selectOptionByText(Locator.name("specimen1_Method"), "Dilution");
        checkCheckbox(Locator.name("specimen1_InitialDilutionCheckBox"));
        checkCheckbox(Locator.name("specimen1_FactorCheckBox"));
        checkCheckbox(Locator.name("specimen1_MethodCheckBox"));

        File data = new File(fileName);
        setFormElement(Locator.xpath("//input[@type='file' and @name='__primaryFile__']"), data);

        clickButton("Save and Finish");
    }

    private void verifyWellgroupNamesPresent(String... names)
    {
        for(String name : names)
        {
            assertTextPresent(name);
        }
    }
}
