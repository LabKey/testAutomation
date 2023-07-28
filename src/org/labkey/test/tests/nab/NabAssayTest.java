/*
 * Copyright (c) 2018-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.test.tests.nab;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.PlateGrid;
import org.labkey.test.components.labkey.LabKeyAlert;
import org.labkey.test.pages.admin.PermissionsPage;
import org.labkey.test.pages.assay.RunQCPage;
import org.labkey.test.pages.assay.plate.PlateDesignerPage;
import org.labkey.test.pages.assay.plate.PlateTemplateListPage;
import org.labkey.test.pages.query.NewQueryPage;
import org.labkey.test.pages.query.SourceQueryPage;
import org.labkey.test.tests.AbstractAssayTest;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.AssayImporter;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DilutionAssayHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.QCAssayScriptHelper;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.WikiHelper;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 15)
public class NabAssayTest extends AbstractAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab Test Verify Project";
    private final static String TEST_ASSAY_FLDR_NAB = "NabAssayFolder";
    private final static String TEST_ASSAY_FLDR_NAB_RENAME = "Rename" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

    protected static final String TEST_ASSAY_NAB = "TestAssayNab";
    protected static final String TEST_ASSAY_NAB_DESC = "Description for NAb assay";

    protected final static String TEST_ASSAY_USR_NAB_READER = "nabreader1@security.test";
    private final static String TEST_ASSAY_GRP_NAB_READER = "Nab Dataset Reader";

    private static final String NAB_FILENAME2 = "m0902053;3999.xls";
    protected final File TEST_ASSAY_NAB_FILE1 = TestFileUtils.getSampleData("Nab/m0902051;3997.xls");
    protected final File TEST_ASSAY_NAB_FILE2 = TestFileUtils.getSampleData("Nab/" + NAB_FILENAME2);
    protected final File TEST_ASSAY_NAB_FILE3 = TestFileUtils.getSampleData("Nab/m0902055;4001.xlsx");
    protected final File TEST_ASSAY_NAB_FILE4 = TestFileUtils.getSampleData("Nab/m0902057;4003.xls");
    protected final File TEST_ASSAY_NAB_FILE5 = TestFileUtils.getSampleData("Nab/m0902059;4005.xls");

    private static final String ASSAY_ID_TRANSFORM = "transformed assayId";

    // AUC Column Names.
    private static final String AUC_COL_TITLE = "AUC";
    private static final String CURVE_IC50_COL_TITLE = "Curve IC50";
    private static final String CURVE_IC70_COL_TITLE = "Curve IC70";
    private static final String CURVE_IC80_COL_TITLE = "Curve IC80";
    private static final String AUC_4PL_COL_TITLE = "AUC 4pl";
    private static final String AUC_5PL_COL_TITLE = "AUC 5pl";
    private static final String AUC_POLY_COL_TITLE = "AUC Poly";
    private static final String CURVE_IC50_4PL_COL_TITLE = "Curve IC50 4pl";
    private static final String CURVE_IC50_5PL_COL_TITLE = "Curve IC50 5pl";
    private static final String CURVE_IC50_POLY_COL_TITLE = "Curve IC50 Poly";
    private static final String CURVE_IC70_4PL_COL_TITLE = "Curve IC70 4pl";
    private static final String CURVE_IC70_5PL_COL_TITLE = "Curve IC70 5pl";
    private static final String CURVE_IC70_POLY_COL_TITLE = "Curve IC70 Poly";
    private static final String CURVE_IC80_4PL_COL_TITLE = "Curve IC80 4pl";
    private static final String CURVE_IC80_5PL_COL_TITLE = "Curve IC80 5pl";
    private static final String CURVE_IC80_POLY_COL_TITLE = "Curve IC80 Poly";

    private static final String PLATE_TEMPLATE_NAME = "NabAssayTest Template";
    private static final String TEST_DIV_ID = "testDiv";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("nab");
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_NAB;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        NabAssayTest init = (NabAssayTest)getCurrentTest();
        init.doSetup();
    }

    private boolean isStudyModuleInstalled()
    {
        boolean hasStudyModule = _containerHelper.getAllModules().contains("study");
        if (!hasStudyModule)
            TestLogger.warn("Study module is not installed. Skipping study-related section.");
        return hasStudyModule;
    }

    private void doSetup() throws Exception
    {
        // set up a scripting engine to run a java transform script
        new QCAssayScriptHelper(this).ensureEngineConfig();

        _containerHelper.createProject(getProjectName(), null);

        //setup a pipeline for the project
        setupPipeline(getProjectName());
        if (isStudyModuleInstalled())
        {
            createStudySubfolder();
        }

        //add the Assay List web part so we can create a new nab assay
        _containerHelper.createSubfolder(getProjectName(), TEST_ASSAY_FLDR_NAB);
    }

    private void createStudySubfolder()
    {
        // create a study so we can test link to study later:
        _containerHelper.createSubfolder(getProjectName(), TEST_ASSAY_FLDR_STUDY1);

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Study Overview");

        clickButton("Create Study");
        clickButton("Create Study");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        try
        {
            new QCAssayScriptHelper(this).deleteEngine();
        }
        catch(WebDriverException ignore) {}
    }

    /**
     * Performs Nab designer/upload/publish.
     */
    @Test
    public void runUITests()
    {
        log("Testing NAb Assay Designer");

        goToProjectHome();

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Assay List");

        //create a new nab assay
        _assayHelper.createAssayDesign("TZM-bl Neutralization (NAb)", TEST_ASSAY_NAB)
            .setDescription(TEST_ASSAY_NAB_DESC)
            .setPlateTemplate("NAb: 5 specimens in duplicate")
            .clickFinish();

        PlateTemplateListPage templateListPage = PlateTemplateListPage.beginAt(this);
        templateListPage.clickNewPlate((PlateDesignerPage.PlateDesignerParams
                ._96well()
                .setAssayType("NAb")
                .setTemplateType("single-plate")));

        setFormElement(Locator.inputById("templateName"), PLATE_TEMPLATE_NAME);

        // select the specimen wellgroup tab
        click(Locator.tagWithText("div", "SPECIMEN"));

        // select the first specimen group
        click(Locator.tagWithText("label", "Specimen 1"));
        // set reversed dilution direction to true:
        setFormElement(Locator.inputById("property-ReverseDilutionDirection"), "true");

        // select the second specimen group
        click(Locator.tagWithText("label", "Specimen 2"));
        // set reversed dilution direction to false:
        setFormElement(Locator.inputById("property-ReverseDilutionDirection"), "false");

        // select the third specimen group
        click(Locator.tagWithText("label", "Specimen 3"));
        // set reversed dilution direction to a nonsense value:
        setFormElement(Locator.inputById("property-ReverseDilutionDirection"), "invalid boolean value");

        // note that we're intentionally leaving the fourth and fifth direction specifiers null, which should default to 'false'
        clickButton("Save & Close");

        assertTextPresent(PLATE_TEMPLATE_NAME, "NAb: 5 specimens in duplicate");

        clickProject(TEST_ASSAY_PRJ_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        _assayHelper.clickEditAssayDesign()
                .setPlateTemplate(PLATE_TEMPLATE_NAME)
                .clickFinish();

        _assayHelper.clickEditAssayDesign()
                .goToConfigureTemplates();

        doAndWaitForPageToLoad(() ->                // todo: add page class method of removing first template
        {
            click(Locator.linkWithText("delete"));

            LabKeyAlert modal = new LabKeyAlert(getDriver());
            modal.clickButton("Yes");
        });

        assertTextPresent(PLATE_TEMPLATE_NAME);
        assertTextNotPresent("NAb: 5 specimens in duplicate");

        navigateToFolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_NAB);
        portalHelper.addWebPart("Assay List");

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        log("Uploading NAb Runs");
        new AssayImporter(this, new AssayImportOptions.ImportOptionsBuilder().
                        assayId("ptid + visit").
                        visitResolver(AssayImportOptions.VisitResolverType.ParticipantVisit).
                        cutoff1("50").
                        cutoff2("70").
                        virusName("Nasty Virus").
                        virusId("5433211").
                        curveFitMethod("Polynomial").
                        ptids(new String[]{"ptid 1 A", "ptid 2 A", "ptid 3 A", "ptid 4 A", "ptid 5 A"}).
                        visits(new String[]{"1", "2", "3", "4", "5"}).
                        initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                        dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                        methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                        runFile(TEST_ASSAY_NAB_FILE1).
                        build()).doImport();

        // verify that we catch an invalid date prior to upload
        new AssayImporter(this, new AssayImportOptions.ImportOptionsBuilder().
                        assayId("ptid + date").
                        visitResolver(AssayImportOptions.VisitResolverType.ParticipantDate).
                        cutoff1("50").
                        cutoff2("80").
                        virusName("Nasty Virus").
                        virusId("5433211").
                        curveFitMethod("Five Parameter").
                        ptids(new String[]{"ptid 1 C", "ptid 2 C", "ptid 3 C", "ptid 4 C", "ptid 5 C"}).
                        dates(new String[]{"299999-09-05", "2014/2/28", "2014/2/28", "bad-date", "2014/2/28"}).
                        initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                        dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                        methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                        runFile(TEST_ASSAY_NAB_FILE2).
                        build()).doImport();

        assertElementPresent(Locators.labkeyError.containing("Could not convert value 'bad-date' (String) for Date field 'Date'."), 1);
//        These dates are SQL Server specific
//        assertElementPresent(Locators.labkeyError.containing("Only dates between January 1, 1753 and December 31, 9999 are accepted."), 1);
        assertElementPresent(Locators.labkeyError.containing("Only dates between "), 1);
        clickButton("Cancel");

        // retry the import with a valid date
        new AssayImporter(this, new AssayImportOptions.ImportOptionsBuilder().
                        assayId("ptid + date").
                        visitResolver(AssayImportOptions.VisitResolverType.ParticipantDate).
                        cutoff1("50").
                        cutoff2("80").
                        virusName("Nasty Virus").
                        virusId("5433211").
                        curveFitMethod("Five Parameter").
                        ptids(new String[]{"ptid 1 C", "ptid 2 C", "ptid 3 C", "ptid 4 C", "ptid 5 C"}).
                        dates(new String[]{"2014/2/28", "2014/2/28", "2014/2/28", "2014/2/28", "2014/2/28"}).
                        initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                        dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                        methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                        runFile(TEST_ASSAY_NAB_FILE2).
                        build()).doImport();

        new AssayImporter(this, new AssayImportOptions.ImportOptionsBuilder().
                        assayId("ptid + visit + date").
                        visitResolver(AssayImportOptions.VisitResolverType.ParticipantVisitDate).
                        cutoff1("50").
                        cutoff2("80").
                        virusName("Nasty Virus").
                        virusId("5433211").
                        curveFitMethod("Four Parameter").
                        ptids(new String[]{"ptid 1 B", "ptid 2 B", "ptid 3 B", "ptid 4 B", "ptid 5 B"}).
                        visits(new String[]{"1", "2", "3", "4", "5"}).
                        dates(new String[]{"2014/2/28", "2014/2/28", "2014/2/28", "2014/2/28", "2014/2/28"}).
                        initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                        dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                        methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                        runFile(TEST_ASSAY_NAB_FILE3).
                        build()).doImport();

        new AssayImporter(this, new AssayImportOptions.ImportOptionsBuilder().
                        assayId("ptid + visit + specimenid").
                        visitResolver(AssayImportOptions.VisitResolverType.SpecimenIDParticipantVisit).
                        cutoff1("50").
                        cutoff2("80").
                        virusName("Nasty Virus").
                        virusId("5433211").
                        curveFitMethod("Five Parameter").
                        ptids(new String[]{"ptid 1 D", "ptid 2 D", "ptid 3 D", "ptid 4 D", "ptid 5 D"}).
                        visits(new String[]{"1", "2", "3", "4", "5"}).
                        sampleIds(new String[]{"SPECIMEN-1", "SPECIMEN-2", "SPECIMEN-3", "SPECIMEN-4", "SPECIMEN-5"}).
                        initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                        dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                        methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                        runFile(TEST_ASSAY_NAB_FILE4).
                        build()).doImport();

        verifyRunDetails();
        // Test editing runs
        // Set the design to allow editing
        clickAndWait(Locator.linkWithText("View Runs"));
        DataRegionTable table = new DataRegionTable("Runs", this);
        assertEquals("No rows should be editable", 0, DataRegionTable.updateLinkLocator().findElements(table.getComponentElement()).size());
        _assayHelper.clickEditAssayDesign(true)
                .setEditableRuns(true)
                .clickFinish();

        // Edit the first run
        doAndWaitForPageToLoad(() ->
        {
            table.updateLink(table.getRowIndex("Assay ID", "ptid + visit + specimenid")).click();
        });

        // Make sure that the properties that affect calculations aren't shown
        assertTextNotPresent("Cutoff", "Curve Fit Method");
        setFormElement(Locator.name("quf_Name"), "NameEdited.xlsx");
        setFormElement(Locator.name("quf_HostCell"), "EditedHostCell");
        setFormElement(Locator.name("quf_PlateNumber"), "EditedPlateNumber");
        clickButton("Submit");
        assertElementPresent(Locator.linkWithText("NameEdited.xlsx"));
        assertTextPresent("EditedHostCell", "EditedPlateNumber");

        // Verify that the edit was audited
        goToSchemaBrowser();
        viewQueryData("auditLog", "ExperimentAuditEvent");
        assertTextPresent("Run edited",
                "Plate Number changed from blank to 'EditedPlateNumber'",
                "Host Cell changed from blank to 'EditedHostCell'",
                "Name changed from 'ptid + visit + specimenid' to 'NameEdited.xlsx'");

        // Issue 46796 - try forcing an immediate stats update to prompt a better query plan selection
        startSystemMaintenance("Database");
        waitForSystemMaintenanceCompletion();

        // Return to the run list
        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        // test creating a custom details view via a "magic" named run-level view:
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn("VirusName");
        _customizeViewsHelper.saveCustomView("CustomDetailsView");

        DataRegionTable runsDataRegion = new DataRegionTable("Runs", getDriver());
        clickAndWait(runsDataRegion.detailsLink(1));
        assertNabData();

        clickAndWait(Locator.linkWithText("View Runs"));
        clickAndWait(Locator.linkWithText("View Results"));
        assertAUCColumnsHidden();
        addAUCColumns();

        DataRegionTable region = new DataRegionTable("Data", this);
        region.clearAllFilters();
        assertAliasedAUCCellData();

        region.setFilter("SpecimenLsid/Property/ParticipantID", "Equals", "ptid 1 C");
        assertTextPresent("ptid 1 C");
        String ptid1c_detailsURL = getAttribute(DataRegionTable.detailsLinkLocator(), "href");
        // TODO: Cant get it to scroll to this filter option...
        region.setFilter("SpecimenLsid/Property/ParticipantID", "Equals One Of", "ptid 1 A;ptid 1 B;ptid 2 A;ptid 2 B;ptid 3 A;ptid 3 B;ptid 4 A;ptid 4 B");
        assertTextPresent("ptid 1 A", "ptid 1 B");
        assertTextNotPresent("ptid 1 C", "ptid 5");

        if (isStudyModuleInstalled())
        {
            log("Link to Study");
            region.checkAllOnPage();
            region.clickHeaderButtonAndWait("Link to Study");

            selectOptionByText(Locator.name("targetStudy"), "/" + TEST_ASSAY_PRJ_NAB + "/" + TEST_ASSAY_FLDR_STUDY1 + " (" + TEST_ASSAY_FLDR_STUDY1 + " Study)");
            clickButton("Next", 300_000); // Triggers a query that is, sometimes, very slow on SQL Server

            region = new DataRegionTable("Data", this);
            region.clickHeaderButtonAndWait("Link to Study");
            log("Link to Study complete.");
            assertStudyData(4);

            assertAliasedAUCStudyData();

            clickAndWait(Locator.linkWithText("assay"));
            assertNabData();
        }

        // Delete a single run (regression test for issue 24487)
        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        clickAndWait(Locator.linkWithText("View Runs"));

        region = new DataRegionTable("Runs", this);
        region.checkCheckbox(0);
        region.clickHeaderButton("Delete");
        clickButton("Confirm Delete");

        doSchemaBrowserTest();

        doResolverTypeTest();

        if (isStudyModuleInstalled())
        {
            // create user with read permissions to study and dataset, but no permissions to source assay
            navigateToFolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1);
            pushLocation();  // Save our location because impersonated user won't have permission to project
            PermissionsPage permissionsPage = navBar().goToPermissionsPage();
            permissionsPage.createPermissionsGroup(TEST_ASSAY_GRP_NAB_READER, TEST_ASSAY_USR_NAB_READER);
            setSubfolderSecurity(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, TEST_ASSAY_GRP_NAB_READER, TEST_ASSAY_PERMS_READER);
            setStudyPerms(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, TEST_ASSAY_GRP_NAB_READER, TEST_ASSAY_PERMS_STUDY_READALL);

            // view dataset, click [assay] link, see assay details in subfolder
            impersonate(TEST_ASSAY_USR_NAB_READER);
            popLocation();
            assertTextPresent(TEST_ASSAY_PRJ_NAB);
            assertTextNotPresent(TEST_ASSAY_FLDR_NAB); // assert no read permissions to subfolder
            clickFolder(TEST_ASSAY_FLDR_STUDY1);
            clickAndWait(Locator.linkWithText("Study Navigator"));
            clickAndWait(Locator.linkWithText("2"));
            assertStudyData(1);
            clickAndWait(Locator.linkWithText("assay"));
            assertNabData();

            // no permission to details page for "ptid 1 C"; it wasn't linked to the study
            beginAt(ptid1c_detailsURL);
            assertEquals(403, getResponseCode());

            stopImpersonating();

            doNabApiTest(); // Use NAb study APIs
        }

        runTransformTest();

        moveAssayFolderTest();

        testWellAndDilutionData();

        directBrowserQueryTest();

        runNabQCTest();
    }

    //Issue 17050: UnsupportedOperationException from org.labkey.nab.query.NabProtocolSchema$NabResultsQueryView.createDataView
    private void directBrowserQueryTest()
    {
        beginAt("/query/Nab%20Test%20Verify%20Project/selectRows.api?schemaName=assay&queryName=TestAssayNab%20Data");
        assertTextPresent("metaData");
    }

    /**
     * previously, assays sometimes failed to find their source files after a folder move
     * this test verifies the fix
     */
    @LogMethod
    private void moveAssayFolderTest()
    {
        log("rename assay folder and verify source file still findable");
        _containerHelper.renameFolder(getProjectName(), TEST_ASSAY_FLDR_NAB, TEST_ASSAY_FLDR_NAB_RENAME, false);
        String portalUrl = WebTestHelper.buildURL("project", getProjectName() + "/" + TEST_ASSAY_FLDR_NAB_RENAME, "begin");
        beginAt(portalUrl); // Navigate away from folder that was just renamed
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        clickAndWait(DataRegionTable.detailsLinkLocator());

        assertTextPresent("Description for NAb assay");
    }

    private void doSchemaBrowserTest()
    {
        final String QUERY_NAME = "Data";

        SourceQueryPage queryPage = NewQueryPage.beginAt(this, getProjectName() + "/" + TEST_ASSAY_FLDR_NAB, "assay.NAb.TestAssayNab")
                .setName("New NabQuery")
                .setBaseTable(QUERY_NAME)
                .clickCreate();
        queryPage.setSource(
                "SELECT \n" +
                        QUERY_NAME + ".Properties.AUC As AUC,\n" +
                        QUERY_NAME + ".Properties.CurveIC50_4pl,\n" +
                        QUERY_NAME + ".Properties.CurveIC50_4plOORIndicator,\n" +
                        QUERY_NAME + ".Properties.SpecimenLsid.Property.ParticipantID,\n" +
                        QUERY_NAME + ".WellgroupName\n" +
                        "FROM " + QUERY_NAME + "\n"
        );
        queryPage.clickSaveAndFinish();
        assertTextPresent("AUC", "Curve IC50 4pl", "Curve IC50 4pl OOR Indicator", "Participant ID", "Wellgroup Name",
                          "<20.0", "ptid 1 C", "473.94");
    }

    private void doResolverTypeTest()
    {
        // verify that the participant, visit, and date resolver type is there
        clickFolder(TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        clickButton("Import Data");
        checkRadioButton(Locator.radioButtonByNameAndValue("participantVisitResolver", "ParticipantVisitDate"));
        clickButton("Next");

        // verify that 'Participant ID', 'Visit ID', and 'Date' fields are included
        // in the batch properties as well as data/specimen grid
        assertTextPresent("Participant id, visit id, and date.");
        // assert that both the visit id and date are present.  In other resolver types only one
        // or the other is present
        assertElementPresent(Locator.checkboxById("specimen1_VisitIDCheckBox"));
        assertElementPresent(Locator.checkboxById("specimen1_DateCheckBox"));
        clickButton("Cancel");
    }

    @LogMethod
    private void doNabApiTest()
    {
        final String WIKIPAGE_NAME = "Nab API Wiki";

        WikiHelper wikiHelper = new WikiHelper(this);

        navigateToFolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_NAB);
        goToModule("Wiki");
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), WIKIPAGE_NAME);
        setFormElement(Locator.name("title"), WIKIPAGE_NAME);
        wikiHelper.setWikiBody(TestFileUtils.getFileContents(TestFileUtils.getSampleData("api/nabApiTest.html")));
        wikiHelper.saveWikiPage();
        waitForElement(Locator.id(TEST_DIV_ID));
        waitForElements(Locator.id(TEST_DIV_ID).child(Locator.tagWithText("div", "Success!")), 2, WAIT_FOR_PAGE);
    }

    private void assertStudyData(int ptidCount)
    {
        assertTextPresent("Dataset: " + TEST_ASSAY_NAB);

        if (ptidCount >= 1)
        {
            // reversed dilution direction:
            assertTextPresent("ptid 1 A", "ptid 1 B", "Curve IC50", "493");
            assertTextPresent("Specimen 1", 2);
        }
        if (ptidCount >= 2)
        {
            // standard dilution direction
            assertTextPresent("ptid 2 A", "ptid 2 B", "134");
            assertTextPresent("Specimen 2", 2);
        }
        if (ptidCount >= 3)
        {
            // invalid dilution direction
            assertTextPresent("ptid 3 A", "ptid 3 B", "436");
            assertTextPresent("Specimen 3", 2);
        }
        if (ptidCount >= 4)
        {
            // unspecified dilution direction
            assertTextPresent("ptid 4 A", "ptid 4 B", "277.9");
            assertTextPresent("Specimen 4", 2);
        }
    }

    private void assertNabData()
    {
        assertTextPresent("Cutoff Dilutions", "ptid 1", "ptid 2", "ptid 3", "ptid 4", "ptid 5");
    }

    private void assertAUCColumnsHidden()
    {
        log("Checking for AUC columns");
        // verify that most AUC columns are hidden by default
        assertTextPresent("AUC");
        assertTextNotPresent("AUC 4pl", "AUC 5pl", "AUC Poly", "Curve IC50 4pl", "Curve IC50 5pl", "Curve IC50 Poly",
                "Curve IC70 4pl", "Curve IC70 5pl", "Curve IC70 Poly", "Curve IC80 4pl", "Curve IC80 5pl", "Curve IC80 Poly");
    }

    private void addAUCColumns()
    {
        log("Adding AUC columns to custom view");
        // add AUC columns. ORDER MATTERS!
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("AUC_4pl", "AUC 4pl");
        _customizeViewsHelper.addColumn("AUC_5pl", "AUC 5pl");
        _customizeViewsHelper.addColumn("AUC_Poly", "AUC Poly");
        _customizeViewsHelper.addColumn("Cutoff50/IC_4pl", "Curve IC50 4pl");
        _customizeViewsHelper.addColumn("Cutoff50/IC_5pl", "Curve IC50 5pl");
        _customizeViewsHelper.addColumn("Cutoff50/IC_Poly", "Curve IC50 Poly");
        _customizeViewsHelper.addColumn("Cutoff70/IC_4pl", "Curve IC70 4pl");
        _customizeViewsHelper.addColumn("Cutoff70/IC_5pl", "Curve IC70 5pl");
        _customizeViewsHelper.addColumn("Cutoff70/IC_Poly", "Curve IC70 Poly");
        _customizeViewsHelper.addColumn("Cutoff80/IC_4pl", "Curve IC80 4pl");
        _customizeViewsHelper.addColumn("Cutoff80/IC_5pl", "Curve IC80 5pl");
        _customizeViewsHelper.addColumn("Cutoff80/IC_Poly", "Curve IC80 Poly");
        _customizeViewsHelper.applyCustomView();
    }

    private void assertAliasedAUCCellData()
    {
        log("Checking data in aliased AUC columns");
        // Check that aliased AUC column show data from correct columns.  Any changes in the default location/quantity of columns will require adjustment of column indices.
        DataRegionTable table = new DataRegionTable("Data", this);

        assertEquals(" ", table.getDataAsText(0, CURVE_IC80_COL_TITLE)); //ptid 1 A, Curve IC 80. Should be blank.
        assertEquals(" ", table.getDataAsText(5, CURVE_IC70_COL_TITLE)); //ptid 1 B, Curve IC 70. Should be blank.

        for (int i = 0; i < 5; i++)
        {
            assertEquals(table.getDataAsText(i, AUC_COL_TITLE),        table.getDataAsText(i, AUC_POLY_COL_TITLE));        //AUC = AUC_poly
            assertEquals(table.getDataAsText(i, CURVE_IC50_COL_TITLE), table.getDataAsText(i, CURVE_IC50_POLY_COL_TITLE)); //Curve IC50 = Curve_IC50_poly
            assertEquals(table.getDataAsText(i, CURVE_IC70_COL_TITLE), table.getDataAsText(i, CURVE_IC70_POLY_COL_TITLE)); //Curve IC70 = Curve_IC70_poly
        }
        for (int i = 10; i < 15; i++)
        {
            assertEquals(table.getDataAsText(i, AUC_COL_TITLE),        table.getDataAsText(i, AUC_4PL_COL_TITLE));        //AUC = AUC_4pl
            assertEquals(table.getDataAsText(i, CURVE_IC50_COL_TITLE), table.getDataAsText(i, CURVE_IC50_4PL_COL_TITLE)); //Curve IC50 = Curve_IC50_4pl
            assertEquals(table.getDataAsText(i, CURVE_IC80_COL_TITLE), table.getDataAsText(i, CURVE_IC80_4PL_COL_TITLE)); //Curve IC80 = Curve_IC80_4pl
        }
        for (int i = 5; i < 10; i++)
        {
            assertEquals(table.getDataAsText(i, AUC_COL_TITLE),        table.getDataAsText(i, AUC_5PL_COL_TITLE));        //AUC = AUC_5pl
            assertEquals(table.getDataAsText(i, CURVE_IC50_COL_TITLE), table.getDataAsText(i, CURVE_IC50_5PL_COL_TITLE)); //Curve IC50 = Curve_IC50_5pl
            assertEquals(table.getDataAsText(i, CURVE_IC80_COL_TITLE), table.getDataAsText(i, CURVE_IC80_5PL_COL_TITLE)); //Curve IC80 = Curve_IC80_5pl
        }
    }

    private void assertAliasedAUCStudyData()
    {
        log("Checking data in aliased AUC columns in Study");
        // check copied AUC data.
        DataRegionTable table = new DataRegionTable("Dataset", this);
        table.setSort("ParticipantId", SortDirection.ASC);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("AUC_Poly",        AUC_POLY_COL_TITLE);
        _customizeViewsHelper.addColumn("AUC_4pl",         AUC_4PL_COL_TITLE);
        _customizeViewsHelper.addColumn("AUC_5pl",         AUC_5PL_COL_TITLE);
        _customizeViewsHelper.addColumn("Cutoff50/IC_Poly", CURVE_IC50_POLY_COL_TITLE);
        _customizeViewsHelper.addColumn("Cutoff50/IC_4pl",  CURVE_IC50_4PL_COL_TITLE);
        _customizeViewsHelper.addColumn("Cutoff70/IC_Poly", CURVE_IC70_POLY_COL_TITLE);
        _customizeViewsHelper.addColumn("Cutoff80/IC_4pl",  CURVE_IC80_4PL_COL_TITLE);
        _customizeViewsHelper.saveCustomView();

        table = new DataRegionTable("Dataset", this);
        assertEquals(table.getDataAsText(0, AUC_COL_TITLE),        table.getDataAsText(0, AUC_POLY_COL_TITLE));        //AUC = AUC_poly
        assertEquals(table.getDataAsText(1, AUC_COL_TITLE),        table.getDataAsText(1, AUC_4PL_COL_TITLE));         //AUC = AUC_4pl
        assertEquals(table.getDataAsText(0, CURVE_IC50_COL_TITLE), table.getDataAsText(0, CURVE_IC50_POLY_COL_TITLE)); //CurveIC50 = CurveIC50_poly
        assertEquals(table.getDataAsText(1, CURVE_IC50_COL_TITLE), table.getDataAsText(1, CURVE_IC50_4PL_COL_TITLE));  //CurveIC50 = CurveIC50_4pl
        assertEquals(table.getDataAsText(0, CURVE_IC70_COL_TITLE), table.getDataAsText(0, CURVE_IC70_POLY_COL_TITLE)); //CurveIC70 = CurveIC70_poly
        assertEquals(table.getDataAsText(1, CURVE_IC80_COL_TITLE), table.getDataAsText(1, CURVE_IC80_4PL_COL_TITLE));  //CurveIC80 = CurveIC80_4pl

        assertEquals(" ", table.getDataAsText(0, CURVE_IC80_COL_TITLE)); //IC80 = blank
        assertEquals(" ", table.getDataAsText(1, CURVE_IC70_COL_TITLE)); //IC70 = blank
    }

    @LogMethod
    protected void runTransformTest()
    {
        // add the transform script to the assay
        log("Uploading NAb Runs with a transform script");
        clickProject(TEST_ASSAY_PRJ_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        _assayHelper.clickEditAssayDesign()
                .addTransformScript(TestFileUtils.getSampleData("qc/transform.jar"))
                .clickFinish();

        navigateToFolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        new AssayImporter(this, new AssayImportOptions.ImportOptionsBuilder().
                        assayId(ASSAY_ID_TRANSFORM).
                        visitResolver(AssayImportOptions.VisitResolverType.ParticipantVisit).
                        cutoff1("50").
                        cutoff2("80").
                        curveFitMethod("Polynomial").
                        ptids(new String[]{"ptid 1 E", "ptid 2 E", "ptid 3 E", "ptid 4 E", "ptid 5 E"}).
                        visits(new String[]{"1", "2", "3", "4", "5"}).
                        initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                        dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                        methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                        runFile(TEST_ASSAY_NAB_FILE1).
                        build()).doImport();

        // verify the run property FileID was generated by the transform script
        clickAndWait(Locator.linkWithText("View Runs"));
        assertTextPresent("transformed FileID");

        // verify the fit error was generated by the transform script
        clickAndWait(Locator.linkWithText(ASSAY_ID_TRANSFORM));

        DataRegionTable table = new DataRegionTable("Data", this);
        for (int i = 0; i < 5; i++)
        {
            assertEquals("0.0", table.getDataAsText(i, "Fit Error"));
        }
    }

    private void verifyRunDetails()
    {
        clickAndWait(Locator.linkWithText("View Runs"));
        DilutionAssayHelper assayHelper = new DilutionAssayHelper(this);

        log("verify ptid + visit + date");
        clickAndWait(Locator.linkWithText("ptid + visit + date"));
        clickAndWait(DataRegionTable.detailsLinkLocator());

        assertTextPresent("&lt; 20", 10);

        String nabData = getText(Locator.xpath("//div[contains(@class, 'panel-default')]/parent::div"));
        assertTrue(nabData.contains("461"));      // Four parameter IC50
        assertTrue(nabData.contains("0.043"));    // 4PL AUC/PosAUC
        assertFalse(nabData.contains("561"));      // Five Parameter IC50
        assertFalse(nabData.contains("503"));      // Polynomial IC50
        assertFalse(nabData.contains("0.077"));    // Five PL AUC

        assayHelper.clickDetailsLink("Change Graph Options", "Curve Type", "Five Parameter");
        nabData = getText(Locator.xpath("//div[contains(@class, 'panel-default')]/parent::div"));
        assertTrue(nabData.contains("561"));      // Five Parameter IC50
        assertTrue(nabData.contains("0.077"));    // Five PL AUC
        assertTrue(nabData.contains("0.081"));    // Five PL posAUC
        assertFalse(nabData.contains("503"));      // Polynomial IC50
        assertFalse(nabData.contains("461"));      // Four parameter IC50

        assayHelper.clickDetailsLink("Change Graph Options", "Curve Type", "Polynomial");
        nabData = getText(Locator.xpath("//div[contains(@class, 'panel-default')]/parent::div"));
        assertTrue(nabData.contains("503"));      // Polynomial IC50:
        assertTrue(nabData.contains("0.054"));    // Polynomial AUC:
        assertTrue(nabData.contains("0.055"));    // Polynomial posAUC:
        assertFalse(nabData.contains("561"));      // Five Parameter IC50
        assertFalse(nabData.contains("461"));      // Four parameter IC50
        assertFalse(nabData.contains("0.077"));    // Five PL AUC
        assertFalse(nabData.contains("0.043"));    // 4PL AUC/PosAUC

        log("Verify different graph sizes");
        // Defaults to Small sized graphs
        Number graphHeight = waitForNabGraph().getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Default)", 300, graphHeight);

        assayHelper.clickDetailsLink("Change Graph Options", "Graph Size", "Large");
        graphHeight = waitForNabGraph().getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Large)", 600, graphHeight);

        assayHelper.clickDetailsLink("Change Graph Options", "Graph Size", "Medium");
        graphHeight = waitForNabGraph().getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Medium)", 550, graphHeight);

        assayHelper.clickDetailsLink("Change Graph Options", "Graph Size", "Small");
        graphHeight = waitForNabGraph().getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Small)", 300, graphHeight);

        assayHelper.verifyDataIdentifiers(AssayImportOptions.VisitResolverType.ParticipantVisitDate, "B");

        log("verify ptid + visit");
        clickAndWait(Locator.linkWithText("View Runs"));
        clickAndWait(Locator.linkWithText("ptid + visit"));
        clickAndWait(DataRegionTable.detailsLinkLocator());
        assayHelper.verifyDataIdentifiers(AssayImportOptions.VisitResolverType.ParticipantVisit, "A");

        log("verify ptid + date");
        clickAndWait(Locator.linkWithText("View Runs"));
        clickAndWait(Locator.linkWithText("ptid + date"));
        clickAndWait(DataRegionTable.detailsLinkLocator());
        assayHelper.verifyDataIdentifiers(AssayImportOptions.VisitResolverType.ParticipantDate, "C");

        log("verify ptid + visit + specimenid");
        clickAndWait(Locator.linkWithText("View Runs"));
        clickAndWait(Locator.linkWithText("ptid + visit + specimenid"));
        clickAndWait(DataRegionTable.detailsLinkLocator());
        assayHelper.verifyDataIdentifiers(AssayImportOptions.VisitResolverType.SpecimenIDParticipantVisit, "D");
    }

    private WebElement waitForNabGraph()
    {
        return shortWait().until(ExpectedConditions
                .visibilityOfElementLocated(Locator.tagWithAttribute("img", "alt", "Neutralization Graph")));
    }

    private static final List<String> expectedRow11 = Arrays.asList("ptid + visit", "Specimen 5", "Specimen 5", "1", "12",
            "107916.0", " ", " ", "Specimen 5", "Specimen 5, Replicate 1", "2", "1", " ", "false", "A12");
    private static final List<String> expectedRow12 = Arrays.asList("ptid + visit", " ", "CELL_CONTROL_SAMPLE", "2", "1",
            "993.0", "CELL_CONTROL_SAMPLE", " ", " ", " ", " ", "1", " ", "false", "B1");

    private static final List<String> expectedDilRow10 = Arrays.asList("ptid + visit", "4860.0", "6", "0.06%", "0.022583",
            "107300.0", "110753.0", "109026.5", "2441.6397", "Specimen 2", "Specimen 2, Replicate 3", "Specimen 2", "20.0", "43740.0", "1");
    private static final List<String> expectedDilRow40 = Arrays.asList("ptid + visit", " ", " ", " ", " ",
            "903.0", "1083.0", "974.875", "58.4061", "CELL_CONTROL_SAMPLE", " ", " ", " ", " ", "1");

    protected void testWellAndDilutionData()
    {
        log("Test WellData and DilutionData tables");
        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB_RENAME);
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("assay.NAb.TestAssayNab", "WellData");
        List<String> row11 = table.getRowDataAsText(11);
        assertEquals("Row size did not match", expectedRow11.size(), row11.size());
        assertEquals("WellData row did not match.", expectedRow11, row11.subList(0, expectedRow11.size()));
        List<String> row12 = table.getRowDataAsText(12);
        assertEquals("Row size did not match", expectedRow12.size(), row11.size());
        assertEquals("WellData row did not match.", expectedRow12, row12.subList(0, expectedRow12.size()));

        goToSchemaBrowser();
        table = viewQueryData("assay.NAb.TestAssayNab", "DilutionData");
        List<String> row10 = table.getRowDataAsText(10);
        assertEquals("Row size did not match", expectedDilRow10.size(), row10.size());
        assertEquals("DilutionData row did not match.", expectedDilRow10, row10.subList(0, expectedDilRow10.size()));
        List<String> row40 = table.getRowDataAsText(40);
        assertEquals("Row size did not match", expectedDilRow40.size(), row40.size());
        assertEquals("DilutionData row did not match.", expectedDilRow40, row40.subList(0, expectedDilRow40.size()));

        log("Delete assay data file and test that run details still works");
        File assayFile = new File(TestFileUtils.getTestTempDir(), "assaydata/" + NAB_FILENAME2);
        boolean deleteSuccess = assayFile.delete();
        assertTrue("Failed to delete file: " + NAB_FILENAME2, deleteSuccess);
        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB_RENAME);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        clickAndWait(Locator.linkWithText("ptid + date"));
        clickAndWait(DataRegionTable.detailsLinkLocator());
        DilutionAssayHelper assayHelper = new DilutionAssayHelper(this);
        assayHelper.verifyDataIdentifiers(AssayImportOptions.VisitResolverType.ParticipantDate, "C");

        clickAndWait(Locator.linkWithText("Download Datafile"));
        assertTextPresent("Data file for run ptid + date was not found.");
        clickButton("Back");
    }

    protected void runNabQCTest()
    {
        goToProjectHome();
        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB_RENAME);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        DataRegionTable dataRegionTable = new DataRegionTable("Runs", this);
        clickAndWait(dataRegionTable.detailsLink(0));

        log("Got to the QC page.");
        DilutionAssayHelper detailHelper = new DilutionAssayHelper(this);
        waitForText("View QC");
        detailHelper.clickDetailsLink("View QC", "Review/QC Data");
        RunQCPage runQCPage = new RunQCPage(getDriver());

        log("Select a few values to remove from 'Plate 1'.");
        List<String> valuesToIgnore = new ArrayList<>();
        List<String> allIgnoredValues = new ArrayList<>();
        valuesToIgnore.add("115243");
        valuesToIgnore.add("910");
        runQCPage.selectPlateItemsToIgnore("Plate 1 Controls", valuesToIgnore);
        allIgnoredValues.addAll(valuesToIgnore);

        log("Select data points to remove from 'Specimen 2'.");
        valuesToIgnore = new ArrayList<>();
        valuesToIgnore.add("58510");
        valuesToIgnore.add("25706");
        valuesToIgnore.add("6025");
        runQCPage.selectDilutionItemsToIgnore("Specimen 2", valuesToIgnore);
        allIgnoredValues.addAll(valuesToIgnore);

        log("Remove all of the data points from 'Specimen 1'.");
        valuesToIgnore = new ArrayList<>();
        valuesToIgnore.add("Select all");
        runQCPage.selectDilutionItemsToIgnore("Specimen 1", valuesToIgnore);
        allIgnoredValues.addAll(runQCPage.getValuesFromDilution("Specimen 1"));

        log("Click next.");
        runQCPage.clickNext();

        PlateGrid plateGrid = runQCPage.getPlateGrid("Plate 1");

        log("Check that all of the selected values to be ignored are marked as such in the excluded grid.");
        List<String> excludedValues = plateGrid.getExcludedValues();
        for (String value : allIgnoredValues)
        {
            assertTrue("Did not find value " + value + " in the excluded grid.", excludedValues.contains(value));
        }

        log("Add an excluded comment to one of the values.");
        final String COMMENT = "This is a comment.";
        runQCPage.setExcludedComment("A", "2", COMMENT);

        log("Remove one of the values from exclusion (i.e. add it back).");
        allIgnoredValues.remove(plateGrid.getCellValue("F", "5"));
        runQCPage.removeExclusion("F", "5");

        log("Validated that the excluded grid no longer shows the element as being excluded.");
        excludedValues = plateGrid.getExcludedValues();
        for (String value : allIgnoredValues)
        {
            assertTrue("Did not find value " + value + " in the excluded grid.", excludedValues.contains(value));
        }

        log("Now go back and select a few more values to ignore.");
        runQCPage.clickPrevious();

        log("Select data points to remove from 'Specimen 5'.");
        valuesToIgnore = new ArrayList<>();
        valuesToIgnore.add("961");
        valuesToIgnore.add("11891");
        valuesToIgnore.add("98806");
        runQCPage.selectDilutionItemsToIgnore("Specimen 5", valuesToIgnore);
        allIgnoredValues.addAll(valuesToIgnore);

        log("Click next again and validate that the expected values are excluded.");
        runQCPage.clickNext();

        // Just to be safe get a new reference to the plate.
        plateGrid = runQCPage.getPlateGrid("Plate 1");

        log("Check that all of the previously selected values and the new values to be ignored are marked as such in the excluded grid.");
        excludedValues = plateGrid.getExcludedValues();
        for (String value : allIgnoredValues)
        {
            assertTrue("Did not find value " + value + " in the excluded grid.", excludedValues.contains(value));
        }

        log("Click finish to save the changes.");
        runQCPage.clickFinish();

        log("Now validate that the plate grid shows the expected data.");
        PlateGrid summaryPlateGrid = new PlateGrid(getDriver());
        excludedValues = summaryPlateGrid.getExcludedValues();
        for (String value : allIgnoredValues)
        {
            assertTrue("Did not find value " + value + " in the excluded grid.", excludedValues.contains(value));
        }

        log("Validate that the tooltip is as expected.");
        mouseOver(summaryPlateGrid.getCellElement("A", "2"));
        sleep(500); // Wait for a moment to allow the tool tip to show up.
        String tipText = getText(Locator.xpath("//div[contains(@class, 'x4-tip-body')]//span//div"));
        assertTrue("Tool tip comment not as expected. Expected: '" + COMMENT + "' Found: '" + tipText + "'.", tipText.equals(COMMENT));
    }
}
