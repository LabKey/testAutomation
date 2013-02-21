package org.labkey.test.tests;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ExtHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PerlHelperWD;
import org.labkey.test.util.PipelineHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelperWD;
import org.labkey.test.util.UIAssayHelper;

import java.io.File;
import java.io.IOException;

/**
 * User: tchadick
 * Date: 2/18/13
 * Time: 2:50 PM
 */
public class RISAssayTest extends BaseWebDriverTest
{
    private final File risXarFile = new File(getDownloadDir(), "ris.xar");
    private final File risListArchive = new File(getDownloadDir(), "ris-lists.zip");
    private final File risTransformScript = new File(getDownloadDir(), "kiem_transform.pl");
    private final File risAssayData = new File(getSampledataPath(), "kiem/RIS test.xls");
    
    private final String ASSAY_NAME = "RIS"; // Defined by risXarFile
    private final String ASSAY_ID = risAssayData.getName();

    private final int ASSAY_ROW_COUNT = 719;
    private final int UNIQUEBLATT_ROW_COUNT = 34;

    public RISAssayTest()
    {
        super();
        _assayHelper = new UIAssayHelper(this);
    }

    @Override
    protected String getProjectName()
    {
        return "RISAssayTest Project";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        risXarFile.delete();
        risListArchive.delete();
        risTransformScript.delete();
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupProject();

        createRISAssay();

        verifyRISAssay();
        verifyRISReport();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProject()
    {

        PerlHelperWD perlHelper = new PerlHelperWD(this);
        perlHelper.ensurePerlConfig();

        _containerHelper.createProject(getProjectName(), "Assay");
        enableModule(getProjectName(), "kiem");

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Kiem RIS Dashboard");
        portalHelper.addWebPart("Lists");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void createRISAssay()
    {
        click(Locator.linkWithText("Download Assay Design"));
        click(Locator.linkWithText("Download List Archive"));
        click(Locator.linkWithText("Download Transform Script"));

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return risXarFile.exists();
            }
        }, "failed to download RIS Assay design", WAIT_FOR_JAVASCRIPT);
        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return risListArchive.exists();
            }
        }, "failed to download RIS list archive", WAIT_FOR_JAVASCRIPT);
        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return risTransformScript.exists();
            }
        }, "failed to download RIS Assay transform script", WAIT_FOR_JAVASCRIPT);

        _listHelper.importListArchive(getProjectName(), risListArchive);
        assertElementPresent(Locator.css("#lists tr"), 7);

        PipelineHelper pipelineHelper = new PipelineHelper(this);

        goToModule("FileContent");
        pipelineHelper.uploadFile(risXarFile);
        selectPipelineFileAndImportAction(risXarFile.getName(), "Import Experiment");

        waitForElementWithRefresh(Locator.linkWithText(ASSAY_NAME), WAIT_FOR_JAVASCRIPT);
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        _assayHelper.clickEditAssayDesign();
        _assayHelper.setTransformScript(risTransformScript);
        _assayHelper.saveAssayDesign();

    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyRISAssay() throws IOException, CommandException
    {
        _assayHelper.importAssay(ASSAY_NAME, risAssayData, getProjectName());

        clickFolder(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        log("Add link to Uniqueblatts view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Uniqueblatts");
        _customizeViewsHelper.saveCustomView();

        log("Check transformed RIS data");
        clickAndWait(Locator.linkWithText(ASSAY_ID));
        waitForElement(Locator.paginationText(1, 100, ASSAY_ROW_COUNT));

        log("Check uniqueblatts RIS data");
        clickAndWait(Locator.linkWithText(ASSAY_NAME + " Runs"));
        clickAndWait(Locator.linkWithText(ASSAY_ID).index(1));
        waitForElement(Locator.paginationText(1, UNIQUEBLATT_ROW_COUNT, UNIQUEBLATT_ROW_COUNT));

        goToSchemaBrowser();
        viewQueryData("assay.General.RIS", "uniqueblatts");
        waitForElement(Locator.paginationText(1, UNIQUEBLATT_ROW_COUNT, UNIQUEBLATT_ROW_COUNT));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyRISReport()
    {
        RReportHelperWD rHelper = new RReportHelperWD(this);
        rHelper.ensureRConfig();

        clickFolder(getProjectName());
        clickAndWait(Locator.linkWithText("RIS Report Tool"));

        waitForElement(Locator.linkWithText(ASSAY_ID));
        click(ExtHelperWD.Locators.checkerForGridRowContainingText(ASSAY_ID));
        clickButton("Create Bar Chart", 0);
        waitForElement(Locator.linkWithText("Kiem Chart"));
        //assertTextNotPresent("Error"); //TODO: 17236: RIS Report bar chart is broken

        clickAndWait(Locator.linkWithText("Kiem Chart"));
        waitForElement(Locator.pageHeader("bar-chart"));
        //assertTextNotPresent("Error"); //TODO: 17236: RIS Report bar chart is broken
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/kiem";
    }
}
