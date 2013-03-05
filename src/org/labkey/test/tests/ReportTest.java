/*
 * Copyright (c) 2009-2013 LabKey Corporation
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

package org.labkey.test.tests;

import com.sun.istack.internal.NotNull;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.tests.study.DataViewsTester;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.ext4cmp.Ext4FileFieldRef;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: klum
 * Date: Jul 31, 2009
 */
public class ReportTest extends StudyBaseTest
{
    private static final String MICE_A = "Mice A";
    private static final String MICE_B = "Mice B";
    private static final String MICE_C = "Mice C";
    private final RReportHelper _rReportHelper = new RReportHelper(this);
    protected static final String GRID_VIEW = "create_gridView";
    protected static final String R_VIEW = "create_rView";
    protected static final String QUERY_VIEW = "create_query_report";
    private final static String DATA_SET = "DEM-1: Demographics";
    private final static String DATA_BASE_PREFIX = "DEM";
    private final static String R_SCRIPT1_ORIG_FUNC = "length(x)";
    private final static String R_SCRIPT1_EDIT_FUNC = "length(x) * 2";
    protected static final String TEST_GROUP = "firstGroup";
    protected static final String TEST_USER = "report_user1@report.test";
    private static final String TEST_GRID_VIEW = "Test Grid View";
    public static final String AUTHOR_REPORT = "Author report";
    protected static final String DEVELOPER_USER = "developer_user1@report.test";
    protected static final String ATTACHMENT_USER = "attachment_user1@report.test";
    public static final String COHORT_1 = "Group 1";
    public static final String COHORT_2 = "Group 2";

    protected boolean isFileUploadTest()
    {
        return true;
    }

    private String R_SCRIPT1(String function, String database)
    {
        return "data <- labkey.data\n" +
            "dbirth <- data$" + database + "bdt\n" +
            "dbirth\n" +
            "sex <- data$" + database + "sex\n" +
            "sexor <- data$" + database + "sexor\n" +
            R_SCRIPT1_METHOD + " <- function(x)\n" +
            "{\n" + function + "\n}\n" +
            "png(filename=\"${imgout:img1}\")\n" +
            "plot(sex, sexor)\n" +
            "dev.off()\n" +
            "pdf(file=\"${pdfout:study}\")\n" +
            "plot(sex, sexor)\n" +
            "dev.off()";
    }

    private final static String R_SCRIPT1_TEXT1 = "1965-03-06";
    private final static String R_SCRIPT1_TEXT2 = "1980-08-01";
    private final static String R_SCRIPT1_IMG = "resultImage";
    private final static String R_SCRIPT1_PDF = "PDF output file (click to download)";
    private final static String R_FILTERED = "999320565";
    private final static String R_SORT = "DEMsex";
    private final static String R_SORT1 = "Male";
    private final static String R_SORT2 = "Female";
    private final static String R_REMCOL = "DEMsexor";
    private final static String R_SCRIPT2_METHOD = "func2";
    private final static String[] R_SCRIPTS = { "rScript1", "rScript2", "rScript3", "rScript4" };
    private final static String R_SCRIPT1_METHOD = "func1";

    private String R_SCRIPT2(String database, String colName)
    {
        return "source(\"" + R_SCRIPTS[0] + ".R\")\n" +
            R_SCRIPT2_METHOD +
            " <- function(x, y)\n" +
            "{\nn1 <- " + R_SCRIPT1_METHOD +
            "(y)\n" +
            "n2 <- mean(x)\n" +
            "n3 <- n1 + n2\n" +
            "n3\n}\n" +
            "func2(labkey.data$" + colName + ", labkey.data$" + database + "natam)";
    }
    private final static String R_SCRIPT2_TEXT1 = "999320648";
    private final static String R_USER = "r_user@report.test";
    private final static String AUTHOR_USER = "author_user@report.test";
    private String R_SCRIPT3(String database, String colName)
    {
        return "source(\"" + R_SCRIPTS[1] + ".R\")\n" +
            "x <- func2(labkey.data$" + colName + ", labkey.data$" + database + "sex)\n" +
            "x\n";
    }
    private final static String R_SCRIPT2_TEXT2 = "999320672";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsers(false, TEST_USER, R_USER, AUTHOR_USER, DEVELOPER_USER, ATTACHMENT_USER);
        super.doCleanup(afterTest);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        enableEmailRecorder();
        // fail fast if R is not configured
        _rReportHelper.ensureRConfig();

        // import study and wait; no specimens needed
        importStudy();
        startSpecimenImport(2);

        // wait for study and specimens to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);
        waitForSpecimenImport();

        //Need to create participant groups before we flip the demographics bit on DEM-1.
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_ONE, "Mouse", PTIDS_ONE);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_TWO, "Mouse", PTIDS_TWO);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), SPECIMEN_GROUP_ONE, "Mouse", SPEC_PTID_ONE);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), SPECIMEN_GROUP_TWO, "Mouse", SPEC_PTID_TWO);

        // need this to turn off the demographic bit in the DEM-1 dataset
        clickFolder(getFolderName());
        setDemographicsBit("DEM-1: Demographics", false);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        doParticipantGroupCategoriesTest();
        doScatterPlotTests();
        doBoxPlotTests();
        doQueryReportTests();
        doCreateCharts();
        doCreateRReports();
        doReportDiscussionTest();
        doAttachmentReportTest();
        doLinkReportTest();
        doParticipantReportTest();
        doParticipantFilterTests(); // Depends on successful doParticipantReportTest
        doThumbnailChangeTest();

        // additional report and security tests
        setupDatasetSecurity();
        doReportSecurity();
    }

    @LogMethod
    private void doThumbnailChangeTest()
    {
        clickTab("Clinical and Assay Data");
        clickWebpartMenuItem("Data Views", false, "Customize");
        DataViewsTester.clickCustomizeView(AUTHOR_REPORT, this);
        assertTextPresent("Share this report with all users");

        //set change thumbnail
//        setFormElement(Locator.xpath("//input[contains(@id, 'customThumbnail')]"), ATTACHMENT_REPORT2_FILE.toString(), false);

        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT2_FILE.toString());
        clickButtonByIndex("Save", 1, 0);

        //no way to verify, unfortunately
    }

    @LogMethod
    protected void deleteReport(String reportName)
    {
        clickAndWait(Locator.linkWithText("Manage Views"));
        final Locator report = Locator.tagContainingText("div", reportName);

        // select the report and click the delete button
        waitForElement(report, 10000);
        selenium.mouseDown(report.toString());

        String id = _extHelper.getExtElementId("btn_deleteView");
        click(Locator.id(id));

        _extHelper.waitForExtDialog("Delete Views", WAIT_FOR_JAVASCRIPT);

        String btnId = selenium.getEval("this.browserbot.getCurrentWindow().Ext.MessageBox.getDialog().buttons[1].getId();");
        click(Locator.id(btnId));

        // make sure the report is deleted
        waitFor(new Checker()
                {
                    public boolean check()
                    {
                        return !isElementPresent(report);
                    }
                }, "Failed to delete report: " + reportName, WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    protected Locator getReportGridLink(String reportName, String linkText)
    {
        return getReportGridLink(reportName, linkText, true);
    }

    @LogMethod
    protected Locator getReportGridLink(String reportName, String linkText, boolean isAdmin)
    {
        if (isAdmin)
        {
            goToManageViews();
        }
        final Locator report = Locator.tagContainingText("div", reportName);

        waitForElement(report, 10000);

        // click the row to expand it
        Locator expander = Locator.xpath("//div[@id='viewsGrid']//td//div[.='" + reportName + "']");
        selenium.click(expander.toString());

        final Locator link = Locator.xpath("//div[@id='viewsGrid']//td//div[.='" + reportName + "']//..//..//..//td//a[contains(text(),'" + linkText + "')]");

        // make sure the row has expanded
        waitFor(new Checker() {
            public boolean check()
            {
                return isElementPresent(link);
            }
        }, "Unable to click the link: " + linkText + " for report: " + reportName, WAIT_FOR_JAVASCRIPT);

        return link;
    }

    protected void clickReportGridLink(String reportName, String linkText, boolean isAdmin)
    {
        Locator link = getReportGridLink(reportName, linkText, isAdmin);
        clickAndWait(link);
    }

    protected void clickReportGridLink(String reportName, String linkText)
    {
        clickReportGridLink(reportName, linkText, true);
    }

    @LogMethod
    private void doCreateCharts()
    {
        clickAndWait(Locator.linkWithText(getStudyLabel()));
        clickAndWait(Locator.linkWithText("DEM-1: Demographics"));

        clickMenuButton("Views", "Create", "Crosstab View");
        selectOptionByValue("rowField",  "DEMsex");
        selectOptionByValue("colField", "DEMsexor");
        selectOptionByValue("statField", "SequenceNum");
        clickButton("Submit");

        String[] row3 = new String[] {"Male", "2", "9", "3", "14"};
        assertTableRowsEqual("report", 3, new String[][] {row3});

        setFormElement("label", "TestReport");
        clickButton("Save");

        clickAndWait(Locator.linkWithText(getStudyLabel()));
        assertTextPresent("TestReport");
        clickAndWait(Locator.linkWithText("TestReport"));

        assertTableCellTextEquals("report", 2, 0, "Female");

        //Delete the report
        clickAndWait(Locator.linkWithText(getStudyLabel()));
        clickTab("Manage");
        deleteReport("TestReport");

        // create new grid view report:
        String viewName = "DRT Eligibility Query";
        createReport(GRID_VIEW);
        setFormElement("label", viewName);
        selectOptionByText("params", "ECI-1: Eligibility Criteria");
        clickButton("Create View");
        assertLinkPresentWithText("999320016");
        assertNavButtonNotPresent("go");
        clickAndWait(Locator.linkWithText(getStudyLabel()));
        clickTab("Manage");
        deleteReport(viewName);

        // create new external report
        clickAndWait(Locator.linkWithText(getStudyLabel()));
        clickAndWait(Locator.linkWithText("DEM-1: Demographics"));
        clickMenuButton("Views", "Create", "Advanced View");
        selectOptionByText("queryName", "DEM-1: Demographics");
        String java = System.getProperty("java.home") + "/bin/java";
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE} ${REPORT_FILE}");
        clickButton("Submit");
        assertTextPresent("Female");
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE}");
        selectOptionByValue("fileExtension", "tsv");
        clickButton("Submit");
        assertTextPresent("Female");
        setFormElement("label", "tsv");
        selectOptionByText("showWithDataset", "DEM-1: Demographics");
        clickButton("Save");
        clickAndWait(Locator.linkWithText(getStudyLabel()));
        clickAndWait(Locator.linkWithText("tsv"));
        assertTextPresent("Female");
    }

    @Override
    protected String getProjectName()
    {
        return "ReportVerifyProject";  // don't want this test to stomp on StudyVerifyProject
    }

    @LogMethod
    protected void doCreateRReports()
    {
        log("Create an R Report");

        click(Locator.linkWithText("Projects"));
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        clickMenuButton("Views", "Create", "R View");
        setQueryEditorValue("script", "");

        log("Execute bad scripts");
        clickViewTab();
        assertTextPresent("Empty script, a script must be provided.");
        if (!_rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX) + "\nbadString", R_SCRIPT1_TEXT1))
            if (!_rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX.toLowerCase()) + "\nbadString", R_SCRIPT1_TEXT1))
                Assert.fail("There was an error running the script");
        assertTextPresent("Error");//("Error executing command");
//        assertTextPresent("Error: object \"badString\" not found");
        // horrible hack to get around single versus double quote difference when running R on Linux or Windows systems.
        assertTextPresent("Error: object ");
        assertTextPresent("badString");
        assertTextPresent(R_SCRIPT1_TEXT1);
        assertTextPresent(R_SCRIPT1_TEXT2);
        assertElementPresent(Locator.id(R_SCRIPT1_IMG));
        assertTextPresent(R_SCRIPT1_PDF);

        log("Execute and save a script");
        if (!_rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1))
            if (!_rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX.toLowerCase()), R_SCRIPT1_TEXT1))
                Assert.fail("There was an error running the script");
        log("Check that the script executed properly");
        assertTextPresent(R_SCRIPT1_TEXT1);
        assertTextPresent(R_SCRIPT1_TEXT2);
        assertElementPresent(Locator.id(R_SCRIPT1_IMG));
        assertTextPresent(R_SCRIPT1_PDF);

        saveReport(R_SCRIPTS[0]);

        log("Create view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewColumn(R_REMCOL);
        _customizeViewsHelper.addCustomizeViewFilter("DEMhisp", "3.Latino\\a or Hispanic?", "Does Not Equal", "Yes");
        _customizeViewsHelper.addCustomizeViewSort(R_SORT, "2.What is your sex?", "Descending");
        _customizeViewsHelper.saveCustomView(R_VIEW);

        log("Check that customize view worked");
        assertTextNotPresent(R_REMCOL);
        assertTextNotPresent(R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        log("Check that R respects column changes, filters and sorts of data");
        pushLocation();
        clickMenuButton("Views", "Create", "R View");
        setQueryEditorValue("script", "labkey.data");
        clickViewTab();
        waitForText(R_SORT1);
        assertTextNotPresent(R_REMCOL);
        assertTextNotPresent(R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        saveReport(R_SCRIPTS[3]);

        popLocation();

        log("Check saved R script");
        clickMenuButton("Views", "default");
        pushLocation();
        //clickButton("Reports >>", 0);
        //clickAndWait(Locator.linkWithText(R_SCRIPTS[0]));
        clickMenuButton("Views", R_SCRIPTS[0]);
        waitForText("Console output", WAIT_FOR_PAGE);
        assertTextPresent("null device");
        assertTextNotPresent("Error executing command");
        assertTextPresent(R_SCRIPT1_TEXT1);
        assertTextPresent(R_SCRIPT1_TEXT2);
        assertElementPresent(Locator.id(R_SCRIPT1_IMG));
        assertTextPresent(R_SCRIPT1_PDF);
        popLocation();

        log("Test user permissions");
        pushLocation();
        createSiteDeveloper(AUTHOR_USER);
        clickFolder(getProjectName());
        enterPermissionsUI();
        setUserPermissions(AUTHOR_USER, "Author");
        impersonate(AUTHOR_USER);
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        createRReport(AUTHOR_REPORT, R_SCRIPT2(DATA_BASE_PREFIX, "mouseId"), true, true, new String[0]);
        stopImpersonating();
        popLocation();


        log("Create second R script");
        clickMenuButton("Views", "Create", "R View");
        click(Locator.xpath("//td[contains(text(),'" + R_SCRIPTS[0] + "')]/input"));
        if (!_rReportHelper.executeScript(R_SCRIPT2(DATA_BASE_PREFIX, "mouseId"), R_SCRIPT2_TEXT1))
            if (!_rReportHelper.executeScript(R_SCRIPT2(DATA_BASE_PREFIX.toLowerCase(), "mouseid"), R_SCRIPT2_TEXT1))
                Assert.fail("There was an error running the script");
        clickSourceTab();
        checkCheckbox("shareReport");
        checkCheckbox("runInBackground");
        clickViewTab();

        log("Check that R script worked");
        assertTextPresent(R_SCRIPT2_TEXT1);
        saveReport(R_SCRIPTS[1]);

        log("Check that background run works");

        enterPermissionsUI();
        clickManageGroup("Users");
        setFormElement("names", R_USER);
        uncheckCheckbox("sendEmail");
        clickButton("Update Group Membership");
        enterPermissionsUI();
        setPermissions("Users", "Editor");
        exitPermissionsUI();


        //create R report with dev
        impersonate(R_USER);

        log("Access shared R script");
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        pushLocation();
        assertElementNotPresent(Locator.xpath("//select[@name='Dataset.viewName']//option[.='" + R_SCRIPTS[0] + "']"));
        clickMenuButton("Views", R_SCRIPTS[1]);
        goBack();
        clickMenuButton("Views", AUTHOR_REPORT);

        popLocation();
        log("Change user permission");
        stopImpersonating();
        clickFolder(getProjectName());
        if (isTextPresent("Enable Admin"))
            clickAndWait(Locator.linkWithText("Enable Admin"));
        enterPermissionsUI();
        setPermissions("Users", "Project Administrator");
        exitPermissionsUI();

        log("Create a new R script that uses other R scripts");
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        clickMenuButton("Views", "Create", "R View");
        click(Locator.xpath("//td[contains(text(),'" + R_SCRIPTS[0] + "')]/input"));
        click(Locator.xpath("//td[contains(text(),'" + R_SCRIPTS[1] + "')]/input"));
        if (!_rReportHelper.executeScript(R_SCRIPT3(DATA_BASE_PREFIX, "mouseId"), R_SCRIPT2_TEXT1))
            if (!_rReportHelper.executeScript(R_SCRIPT3(DATA_BASE_PREFIX.toLowerCase(), "mouseid"), R_SCRIPT2_TEXT1))
                Assert.fail("There was an error running the script");
        assertTextPresent(R_SCRIPT2_TEXT1);
        resaveReport();
        _extHelper.waitForExtDialog("Save View");

        log("Test editing R scripts");
        signOut();
        signIn();
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickReportGridLink(R_SCRIPTS[0], "edit");
        if (!_rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1))
            if (!_rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX.toLowerCase()), R_SCRIPT1_TEXT1))
                Assert.fail("There was an error running the script");
        resaveReport();
        waitForPageToLoad();

        log("Check that edit worked");
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickReportGridLink(R_SCRIPTS[1], "edit");

        clickViewTab();
        waitForElement(Locator.navButton("Start Job"), WAIT_FOR_JAVASCRIPT);
        clickButton("Start Job", 0);
        waitForText("COMPLETE", WAIT_FOR_PAGE);
        assertTextPresent(R_SCRIPT2_TEXT2);
        assertTextNotPresent(R_SCRIPT2_TEXT1);
        resaveReport();
        waitForPageToLoad();

        log("Clean up R pipeline jobs");
        cleanPipelineItem(R_SCRIPTS[1]);
    }

    /**create an R report from the dataset page
     *
     * @param name name of script
     * @param scriptValue actual script value
     * @param share should this be shared with others?
     * @param shareSource if so, should they be able to see the source (ignored if share is false)
     */
    @LogMethod
    private void createRReport(String name, String scriptValue, boolean share, boolean shareSource, @NotNull String[] sharedScripts)
    {

        clickMenuButton("Views", "Create", "R View");
        setQueryEditorValue("script", scriptValue);

        // if there are any shared scripts, check the check box so they get included when the report is rendered
        if (sharedScripts != null && sharedScripts.length > 0)
        {
            for (String script : sharedScripts)
                click(Locator.xpath("//td[contains(text(),'" + script + "')]/input"));
        }

        if(share)
        {
            checkCheckbox("shareReport");
            if(shareSource)
                checkCheckbox("sourceTabVisible");
        }
        clickButtonContainingText("Save", 0);
        waitForExtMask();

        Locator l = Locator.xpath("//div[span[text()='Please enter a view name:']]/div/input");
        setFormElement(l, name);
        _extHelper.clickExtButton("Save");
        waitForPageToLoad();

    }

    private static final String ATTACHMENT_REPORT_NAME = "Attachment Report1";
    private static final String ATTACHMENT_REPORT_DESCRIPTION = "This attachment report uploads a file";
    private static final File ATTACHMENT_REPORT_FILE = new File(getLabKeyRoot() + "/sampledata/Microarray/", "test1.jpg"); // arbitrary image file

    private static final String ATTACHMENT_REPORT2_NAME = "Attachment Report2";
    private static final String ATTACHMENT_REPORT3_NAME = "Attachment Report3";
    private static final String UPDATE_ATTACHMENT_REPORT = "Update Attachment Report";

    private static final String ATTACHMENT_REPORT2_DESCRIPTION= "This attachment report points at a file on the server.";
    private static final File ATTACHMENT_REPORT2_FILE = new File(getLabKeyRoot() + "/sampledata/Microarray/", "test2.jpg"); // arbitrary image file

    @LogMethod
    private void doAttachmentReportTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        clickMenuButton("Create", "Attachment Report");
        clickButton("Cancel");

        if (isFileUploadAvailable())
        {
            clickMenuButton("Create", "Attachment Report");
            setFormElement("viewName", ATTACHMENT_REPORT_NAME);
            setFormElement("description", ATTACHMENT_REPORT_DESCRIPTION);
            setFormElement("uploadFile", ATTACHMENT_REPORT_FILE.toString());

            Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
            ref.setToFile(ATTACHMENT_REPORT_FILE.toString());
            clickButton("Save");
            // save should return back to manage views page
            waitForText("Manage Views");
        }

        // test creation from Data Views menu option
        clickTab("Clinical and Assay Data");
        clickWebpartMenuItem("Data Views", true, "Add Report", "From File");
        setFormElement("viewName", ATTACHMENT_REPORT2_NAME);
        setFormElement("description", ATTACHMENT_REPORT2_DESCRIPTION);
        click(Locator.xpath("//input[../label[string()='Full file path on server']]"));
        setFormElement("filePath", ATTACHMENT_REPORT2_FILE.toString());
        clickButton("Save");
        // save should return to the Clinical and Assay Data tab
        waitForText("Data Views");

        if (isFileUploadAvailable())
        {
            waitForText(ATTACHMENT_REPORT_NAME);
        }
        waitForText(ATTACHMENT_REPORT2_NAME);

        if (isFileUploadAvailable())
        {
            clickReportGridLink(ATTACHMENT_REPORT_NAME, "view");
            goBack();
        }
        //TODO: Verify reports. Blocked: 13761: Attachment reports can't be viewed
//        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "view");

        // relies on reports created in this function so
        // call from here
        doUpdateAttachmentReportTest();
    }

    @LogMethod
    private void doUpdateAttachmentReportTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());

        //
        // verify edit URL works, share the local attachment report (REPORT)
        //
        if (isFileUploadAvailable())
        {
            clickReportGridLink(ATTACHMENT_REPORT_NAME, "edit");
            click(Locator.xpath("//input[../label[string()='Share this report with all users?']]"));
            clickButton("Save");
            waitForText("Manage Views");
        }

        //
        // verify details edit button works, share the server attachment report (REPORT2)
        //
        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "details");
        clickButton("Edit Report");
        click(Locator.xpath("//input[../label[string()='Share this report with all users?']]"));
        clickButton("Save");
        waitForText("Report Details");

        //
        // verify a non-admin can edit a local attachment report but not a
        // server attachment report
        //
        createUser(ATTACHMENT_USER, null);
        clickFolder(getProjectName());
        enterPermissionsUI();
        setUserPermissions(ATTACHMENT_USER, "Editor");
        impersonate(ATTACHMENT_USER);
        clickFolder(getProjectName());
        clickFolder(getFolderName());

        // can edit local
        if (isFileUploadAvailable())
        {
            clickTab("Clinical and Assay Data");
            clickWebpartMenuItem("Data Views", true, "Manage Views");
            waitForText("Manage Views");
            clickReportGridLink(ATTACHMENT_REPORT_NAME, "details", false /*isAdmin*/);
            waitForText("Report Details");
            Locator.XPathLocator l = getButtonLocator("Edit Report");
            Assert.assertTrue("Expected 'Edit Report' button to be present", l != null);
            clickButton("Edit Report");
            clickButton("Save");
            waitForText("Report Details");
        }

        // cannot edit server
        clickTab("Clinical and Assay Data");
        clickWebpartMenuItem("Data Views", true, "Manage Views");
        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "details", false /*isAdmin*/);
        waitForText("Report Details");
        Locator.XPathLocator l = getButtonLocator("Edit Report");
        Assert.assertTrue("Expected 'Edit Report' button to not be present", l == null);
        stopImpersonating();

        clickFolder(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();

        //
        // verify we can  change a server attachment type to a local attachment type
        //
        if (isFileUploadAvailable())
        {
            // verify we have an edit button on the details page
            clickReportGridLink(ATTACHMENT_REPORT2_NAME, "edit");
            // change this from a server attachment report to a local attachment report
            click(Locator.xpath("//input[../label[string()='Upload file to server']]"));
            Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
            ref.setToFile(ATTACHMENT_REPORT2_FILE.toString());
            clickButton("Save");
            // save should return back to the details page
            waitForText("Manage Views");
        }

        // verify rename
        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "edit");
        setFormElement("viewName", ATTACHMENT_REPORT3_NAME);
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        // verify can rename to same name
        clickReportGridLink(ATTACHMENT_REPORT3_NAME, "edit");
        setFormElement("viewName", ATTACHMENT_REPORT3_NAME);
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        Locator statusElement = Locator.input("status");

        // verify we can set a property
        clickReportGridLink(ATTACHMENT_REPORT3_NAME, "edit");
        waitForText(UPDATE_ATTACHMENT_REPORT);
        Assert.assertFalse("Locked".equals(getFormElement(statusElement)));
        setFormElement("status", "Locked");
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);
        clickReportGridLink(ATTACHMENT_REPORT3_NAME, "edit");
        waitForText(UPDATE_ATTACHMENT_REPORT);
        Assert.assertTrue("Locked".equals(getFormElement(statusElement)));
        clickButton("Cancel");
        waitForText(ATTACHMENT_REPORT3_NAME);
    }

    private static final String LINK_REPORT1_NAME = "Link Report1";
    private static final String LINK_REPORT1_DESCRIPTION= "This link report points links to an internal page.";
    private static final String LINK_REPORT1_URL = "/project/home/begin.view";

    private static final String LINK_REPORT2_NAME = "Link Report2";
    private static final String LINK_REPORT2_DESCRIPTION= "This link report points links to an external page.";

    @LogMethod
    private void doLinkReportTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();

        clickMenuButton("Create", "Link Report");
        setFormElement("viewName", LINK_REPORT1_NAME);
        setFormElement("description", LINK_REPORT1_DESCRIPTION);
        assertTextNotPresent("URL must be absolute");
        setFormElement("linkUrl", "mailto:kevink@example.com");
        assertTextPresent("URL must be absolute");
        setFormElement("linkUrl", getContextPath() + LINK_REPORT1_URL);
        assertTextNotPresent("URL must be absolute");
        Assert.assertTrue("Expected targetNewWindow checkbox to be checked", _extHelper.isChecked("Open link report in new window?"));
        _extHelper.uncheckCheckbox("Open link report in new window?");
        clickButton("Save");
        // save should return back to manage views page
        waitForText("Manage Views");

        // test creation from menu option on Data Views webpart
        clickTab("Clinical and Assay Data");
        clickWebpartMenuItem("Data Views", true, "Add Report", "From Link");
        setFormElement("viewName", LINK_REPORT2_NAME);
        setFormElement("description", LINK_REPORT2_DESCRIPTION);
        setFormElement("linkUrl", getBaseURL() + LINK_REPORT1_URL);
        assertTextNotPresent("URL must be absolute");
        Assert.assertTrue("Expected targetNewWindow checkbox to be checked", _extHelper.isChecked("Open link report in new window?"));
        clickButton("Save");
        // save should return back to Clinical and Assay Data tab
        waitForText("Data Views");

        goToManageViews();
        pushLocation();
        clickReportGridLink(LINK_REPORT1_NAME, "view");
        Assert.assertTrue("Expected link report to go to '" + LINK_REPORT1_URL + "', but was '" + getCurrentRelativeURL() + "'",
                getURL().toString().contains(LINK_REPORT1_URL));
        popLocation();

        // Clicking on LINK_REPORT2_NAME "view" link will open a new browser window.
        // To avoid opening a new browser window, let's just check that the link has the target="_blank" attribute.
        Locator link = getReportGridLink(LINK_REPORT2_NAME, "view");
        String target = getAttribute(link, "target");
        Assert.assertEquals("_blank", target);
    }

    @LogMethod
    private void saveReport(String name)
    {
        clickSourceTab();
        clickButton("Save", 0);

        if (null != name)
        {
            setFormElement(Locator.xpath("//input[@class='ext-mb-input']"), name);
            _extHelper.clickExtButton("Save");
        }
    }

    private void resaveReport()
    {
        saveReport(null);
    }

    private void clickViewTab()
    {
        clickDesignerTab("View");
    }

    private void clickSourceTab()
    {
        clickDesignerTab("Source");
    }

    private void clickDesignerTab(String name)
    {
        _extHelper.clickExtTab(name);
        sleep(2000); // TODO
    }

    @LogMethod
    protected void deleteRReports()
    {
        log("Clean up R Reports");
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        for (String script : R_SCRIPTS)
        {
            while (isTextPresent(script))
            {
                click(Locator.xpath("//a[contains(text(),'" + script + "')]/../../td[3]/a"));
                Assert.assertTrue(selenium.getConfirmation().matches("^Permanently delete the selected view[\\s\\S]$"));
                waitForPageToLoad();
            }
            assertTextNotPresent(script);
        }
    }

    @LogMethod
    protected void cleanPipelineItem(String item)
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Manage Files"));
        if (isTextPresent(item))
        {
            checkCheckbox(Locator.xpath("//td/a[contains(text(), '" + item + "')]/../../td/input"));
            clickButton("Delete");
            assertTextNotPresent(item);
        }
    }

    @LogMethod
    protected void setupDatasetSecurity()
    {
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickFolder(getProjectName());
        clickAndWait(Locator.linkWithText("My Study"));

        // create a test group and give it container read perms
        enterPermissionsUI();

        createPermissionsGroup(TEST_GROUP);

        // add user to the first test group
        clickManageGroup(TEST_GROUP);
        setFormElement("names", TEST_USER);
        uncheckCheckbox("sendEmail");
        clickButton("Update Group Membership");

        enterPermissionsUI();
        setPermissions(TEST_GROUP, "Reader");
        clickButton("Save and Finish");

        // give the test group read access to only the DEM-1 dataset
        clickAndWait(Locator.linkWithText("My Study"));
        enterStudySecurity();

        // enable advanced study security
        selectOptionByValue(Locator.name("securityString"), "ADVANCED_READ");
        waitForPageToLoad(30000);

        click(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//th/input[@value='READOWN']"));
        clickAndWait(Locator.id("groupUpdateButton"));

        selectOptionByText("dataset.1", "Read");
        clickAndWait(Locator.xpath("//form[@id='datasetSecurityForm']//a[@class='labkey-button']/span[text() = 'Save']"));
    }

    @LogMethod
    protected void doReportSecurity()
    {
        // create charts
        clickFolder(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkWithText("APX-1: Abbreviated Physical Exam"));
        clickMenuButton("Charts", "Create Chart View");
        waitForElement(Locator.xpath("//select[@name='columnsX']"), WAIT_FOR_JAVASCRIPT);
        selectOptionByText("columnsX", "1. Weight");
        selectOptionByText("columnsY", "4. Pulse");
        checkCheckbox("participantChart");
        clickButton("Save", 0);
        sleep(2000);

        setFormElement("reportName", "participant chart");
        clickButton("OK", 0);

        waitForElement(Locator.navButton("Views"), WAIT_FOR_JAVASCRIPT);

        clickMenuButton("Views", "default");
        waitForElement(Locator.navButton("Views"), WAIT_FOR_JAVASCRIPT);
        clickMenuButton("Charts", "Create Chart View");
        waitForElement(Locator.xpath("//select[@name='columnsX']"), WAIT_FOR_JAVASCRIPT);

        // create a non-participant chart
        selectOptionByText("columnsX", "1. Weight");
        selectOptionByText("columnsY", "4. Pulse");
        clickButton("Save", 0);
        sleep(2000);

        setFormElement("reportName", "non participant chart");
        setFormElement("description", "a private chart");
        checkCheckbox("shareReport");
        clickButton("OK", 0);

        waitForElement(Locator.navButton("Views"), WAIT_FOR_JAVASCRIPT);

        // create grid view
        clickFolder(getFolderName());
        goToManageViews();

        createReport(GRID_VIEW);
        setFormElement("label", TEST_GRID_VIEW);
        selectOptionByText("datasetSelection", "APX-1: Abbreviated Physical Exam");
        clickButton("Create View");

        // test security
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickFolder(getProjectName());
        clickAndWait(Locator.linkWithText("My Study"));

        clickReportGridLink("participant chart", "permissions");
        selenium.click("useCustom");
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickButton("Save");

        clickReportGridLink(TEST_GRID_VIEW, "permissions");
        selenium.click("useCustom");
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickButton("Save");

        goToAdminConsole();
        impersonate(TEST_USER);
        clickFolder(getProjectName());
        clickAndWait(Locator.linkWithText("My Study"));

        assertLinkNotPresentWithText("APX-1: Abbreviated Physical Exam");
        clickAndWait(Locator.linkWithText("participant chart"));

        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(TEST_GRID_VIEW));
        assertTextPresent("999320016");
        pushLocation();
        clickMenuButton("Views", "default");
        assertTextPresent("User does not have read permission on this dataset.");
/*
        no longer showing the query button by default.
        popLocation();
        clickMenuButton("Query", "Query:APX-1: Abbreviated Physical Exam");
        assertTextPresent("User does not have read permission on this dataset.");
*/
        stopImpersonating();
    }

    private static final String PARTICIPANT_REPORT_NAME = "Test Participant Report";
    private static final String PARTICIPANT_REPORT_DESCRIPTION = "Participant report created by ReportTest";
    private static final String PARTICIPANT_REPORT2_NAME = "Test Participant Report 2";
    private static final String PARTICIPANT_REPORT2_DESCRIPTION = "Another participant report created by ReportTest";
    private static final String ADD_MEASURE_TITLE = "Add Measure";
    private static final String PARTICIPANT_REPORT3_NAME = "Group Filter Report";
    private static final String PARTICIPANT_GROUP_ONE = "TEST_GROUP_1";
    private static final String PARTICIPANT_GROUP_TWO = "TEST_GROUP_2";
    private static final String[] PTIDS_ONE = {"999320016", "999320485", "999320518", "999320529", "999320533", "999320541",
                                               "999320557", "999320565", "999320576", "999320582", "999320590", "999320609"};
    private static final String[] PTIDS_TWO = {"999320613", "999320624", "999320638", "999320646", "999320652", "999320660",
                                               "999320671", "999320687", "999320695", "999320703", "999320719", "999321029",
                                               "999321033"};
    private static final String[] PTIDS = {"999320016", "999320485", "999320518", "999320529", "999320533", "999320541",
                                           "999320557", "999320565", "999320576", "999320582", "999320590", "999320609",
                                           "999320613", "999320624", "999320638", "999320646", "999320652", "999320660",
                                           "999320671", "999320687", "999320695", "999320703", "999320719", "999321029",
                                           "999321033"};
    private static final String PARTICIPANT_REPORT4_NAME = "Specimen Filter Report";
    private static final String SPECIMEN_GROUP_ONE = "SPEC GROUP 1";
    private static final String SPECIMEN_GROUP_TWO = "SPEC GROUP 2";
    private static final String[] SPEC_PTID_ONE = {"999320016"};
    private static final String[] SPEC_PTID_TWO = {"999320518"};
    private static final String PARTICIPANT_REPORT5_NAME = "Demographic Participant Report";
    @LogMethod
    private void doParticipantReportTest()
    {
        log("Testing Participant Report");

        clickFolder(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        clickMenuButton("Create", "Mouse Report");

        // select some measures from a dataset
        waitAndClickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", "cpf-1");
        pressEnter(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//input[contains(@class, 'x4-form-text') and @type='text']");
        waitForElementToDisappear(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//tr[contains(@class, 'x4-grid-row')][18]"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Wrong number of measures visible after filtering.", 17, getXpathCount(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//tr[contains(@class, 'x4-grid-row')]")));

        _extHelper.clickX4GridPanelCheckbox("label", "2a. Creatinine", "measuresGridPanel", true);
        _extHelper.clickX4GridPanelCheckbox("label", "1a.ALT AE Severity Grade", "measuresGridPanel", true);
        _extHelper.clickX4GridPanelCheckbox("label", "1a. ALT (SGPT)", "measuresGridPanel", true);

        clickButton("Select", 0);

        waitForText("Visit Date", 8, WAIT_FOR_JAVASCRIPT);
        assertTextPresent("2a. Creatinine", 19); // 8 mice + 8 grid field tooltips + 1 Report Field list + 2 in hidden add field dialog
        assertTextPresent("1a.ALT AE Severity Grade", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog
        assertTextPresent("1a. ALT (SGPT)", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog

        // select additional measures from another dataset
        clickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", "2a. Creatinine");
        pressEnter(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//input[contains(@class, 'x4-form-text') and @type='text']");
        waitForElementToDisappear(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//tr[contains(@class, 'x4-grid-row')][5]"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Wrong number of measures visible after filtering.", 4, getXpathCount(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//tr[contains(@class, 'x4-grid-row')]")));
        _extHelper.clickX4GridPanelCheckbox("queryName", "CPS-1", "measuresGridPanel", true);
        clickButton("Select", 0);

        // at this point the report should render some content
        waitForText("Creatinine", 37, WAIT_FOR_JAVASCRIPT); // 8 mice (x2 columns + tooltips) + 1 Report Field list + 2 in hidden add field dialog
        assertTextPresent("1a.ALT AE Severity Grade", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog
        assertTextPresent("1a. ALT (SGPT)", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog

        assertTextPresent("Showing partial results while in edit mode.");
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
        waitForText("Showing 8 Results");

        // verify form validation
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Error");
        waitAndClickButton("OK", 0);
        log("assert text prsent in original form");
        assertTextPresentInThisOrder("Visit", "Visit Date", "Screening");
        assertTextPresentInThisOrder("3.5", "45", "1.9");

         clickButton("Transpose", 0);
        log("assert text tranposed");
        assertTextPresentInThisOrder("Screening",  "2 week Post", "Visit Date");
        assertTextPresentInThisOrder("3.5", "1.9", "45");

        // save the report for real
        _extHelper.setExtFormElementByLabel("Report Name", PARTICIPANT_REPORT_NAME);
        _extHelper.setExtFormElementByLabel("Report Description", PARTICIPANT_REPORT_DESCRIPTION);
        clickSaveParticipantReport();

        // verify visiting saved report
        goToManageViews();
        clickReportGridLink(PARTICIPANT_REPORT_NAME, "view");

        waitForText("Creatinine", 34, WAIT_FOR_JAVASCRIPT); // 8 mice (x2 column headers) + 8 mice (x2 column tooltips) + 2 in hidden customize panel
        assertTextPresent(PARTICIPANT_REPORT_NAME);
        assertTextPresent("1a.ALT AE Severity Grade", 17); // 8 mice + 8 grid field tooltips + 1 hidden grid row in customization panel
        assertTextPresent("1a. ALT (SGPT)", 17); // 8 mice + 8 grid field tooltips + 1 hidden grid row in customization panel
        assertTextPresent("Showing 8 Results");
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]")); // Edit panel should be hidden

        // Delete a column and save report
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//img[@data-qtip = 'Delete']")); // Delete 'Creatinine' column.
        clickSaveParticipantReport();

        // Delete a column save a copy of the report (Save As)
        // Not testing column reorder. Ext4 and selenium don't play well together for drag & drop
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//img[@data-qtip = 'Delete']")); // Delete 'Severity Grade' column.
        clickButton("Save As", 0);
        _extHelper.waitForExtDialog("Save As");
        _extHelper.setExtFormElementByLabel("Save As", "Report Name", PARTICIPANT_REPORT2_NAME);
        _extHelper.setExtFormElementByLabel("Save As", "Report Description", PARTICIPANT_REPORT2_DESCRIPTION);
        clickButtonByIndex("Save", 1, 0);
        _ext4Helper.waitForComponentNotDirty("participant-report-panel-1");
        waitForTextToDisappear("Severity Grade");

        // Verify saving with existing report name.
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        clickButton("Save As", 0);
        _extHelper.waitForExtDialog("Save As");
        _extHelper.setExtFormElementByLabel("Save As", "Report Name", PARTICIPANT_REPORT_NAME);
        _extHelper.setExtFormElementByLabel("Save As", "Report Description", PARTICIPANT_REPORT2_DESCRIPTION);
        clickButtonByIndex("Save", 1, 0);
        _extHelper.waitForExtDialog("Failure");
        assertTextPresent("Another report with the same name already exists.");
        waitAndClickButton("OK", 0);
        clickButton("Cancel", 0); // Verify cancel button.
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden


        // verify modified, saved report
        goToManageViews();
        clickReportGridLink(PARTICIPANT_REPORT_NAME, "view");

        waitForText("Creatinine", 17, WAIT_FOR_JAVASCRIPT); // 8 mice + 8 grid field tooltips + 1 in hidden customize panel
        waitForText("Showing 8 Results", 1, WAIT_FOR_JAVASCRIPT); // There should only be 8 results, and it should state that.

        assertTextPresent(PARTICIPANT_REPORT_NAME);
        assertTextPresent("1a.ALT AE Severity Grade", 17); // 8 mice + 8 grid field tooltips + 1 in hidden customize panel
        assertTextPresent("1a. ALT (SGPT)", 17); // 8 mice + 8 grid field tooltips  + 1 in hidden customize panel
        assertTextPresent("Showing 8 Results");
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]")); // Edit panel should be hidden
        log("Verify report name and description.");
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Wrong report description", PARTICIPANT_REPORT_DESCRIPTION, _extHelper.getExtFormElementByLabel("Report Description"));


        // verify modified, saved-as report
        goToManageViews();
        clickReportGridLink(PARTICIPANT_REPORT2_NAME, "view");

        waitForText("Creatinine", 17, WAIT_FOR_JAVASCRIPT); // 8 mice + 8 grid field tooltips + 1 in hidden customize panel
        assertTextPresent(PARTICIPANT_REPORT2_NAME);
        assertTextNotPresent("1a.ALT AE Severity Grade");
        assertTextPresent("1a. ALT (SGPT)", 17); // 8 mice + 8 grid field tooltips + 1 in hidden customize panel
        assertTextPresent("Showing 8 Results");
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]")); // Edit panel should be hidden
        log("Verify report name and description.");
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Wrong report description", PARTICIPANT_REPORT2_DESCRIPTION, _extHelper.getExtFormElementByLabel("Report Description"));

        // Test group filtering
        goToManageViews();
        clickMenuButton("Create", "Mouse Report");
        // select some measures from a dataset
        waitAndClickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        _extHelper.clickX4GridPanelCheckbox("label", "17a. Preg. test result", "measuresGridPanel", true);
        _extHelper.clickX4GridPanelCheckbox("label", "1.Adverse Experience (AE)", "measuresGridPanel", true);

        clickButton("Select", 0);

        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
        waitForText("Showing 25 Results", WAIT_FOR_JAVASCRIPT);

        //Deselect All
        Locator filterExpander = Locator.xpath("(//img[contains(@class, 'x4-tool-expand-right')])[1]");
        click(filterExpander);

        deselectAllFilterGroups();
        waitForText("Showing 0 Results");

        //Mouse down on GROUP 1
        _ext4Helper.checkGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Showing 12 Results");

        //Check if all PTIDs of GROUP 1 are visible.
        List<String> ptid_list2 = Arrays.asList(PTIDS_TWO);
        for(String ptid : PTIDS_ONE)
        {
            assertTextPresent(ptid);

            String base = "//td//a[text()='" + ptid + "']/../../..//td[contains(text(), 'Groups:')]/following-sibling::td[contains(normalize-space(), '";
            waitForElement(Locator.xpath(base + PARTICIPANT_GROUP_ONE + "')]"));

            if (ptid_list2.contains(ptid))
            {
                assertElementPresent(Locator.xpath(base + PARTICIPANT_GROUP_TWO + "')]"));
            }

        }

        _ext4Helper.checkGridRowCheckbox(PARTICIPANT_GROUP_TWO, 0);
        // groups are disjoint
        waitForText("Showing 0 Results");

        _ext4Helper.uncheckGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Showing 13 Results");

        //Check if all PTIDs of GROUP 2 are visible
        for(String ptid : PTIDS_TWO)
        {
            assertLinkPresentWithText(ptid);
        }
        //Make sure none from Group 1 are visible.
        for(String ptid : PTIDS_ONE)
        {
            assertTextNotPresent(ptid);
        }

        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByLabel("Report Name", PARTICIPANT_REPORT3_NAME);
        clickSaveParticipantReport();

        //Participant report with specimen fields.
        goToManageViews();
        clickMenuButton("Create", "Mouse Report");
        // select some measures from a dataset
        waitAndClickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", "primary type vial counts blood");
        pressEnter(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//input[contains(@class, 'x4-form-text') and @type='text']");

        _extHelper.clickX4GridPanelCheckbox("label", "Blood (Whole):VialCount", "measuresGridPanel", true);
        _extHelper.clickX4GridPanelCheckbox("label", "Blood (Whole):AvailableCount", "measuresGridPanel", true);

        clickButton("Select", 0);
        waitForElement(Locator.linkWithText(PTIDS_ONE[0]));

        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
        waitForText("Showing 116 Results", WAIT_FOR_JAVASCRIPT);

        //Deselect All
        click(filterExpander);
        deselectAllFilterGroups();
        waitForText("Showing 0 Results");

        //Mouse down on SPEC GROUP 1
        _ext4Helper.checkGridRowCheckbox(SPECIMEN_GROUP_ONE, 0);
        waitForText("Showing 1 Results");
        Assert.assertEquals(1, getXpathCount(Locator.xpath("//td[text()='Screening']/..//td[3][text()='23']")));
        Assert.assertEquals(1, getXpathCount(Locator.xpath("//td[text()='Screening']/..//td[4][text()='3']")));

        //Add SPEC GROUP 2
        _ext4Helper.checkGridRowCheckbox(SPECIMEN_GROUP_TWO, 0);
        waitForText("Showing 0 Results");
        //Remove SPEC GROUP 1
        _ext4Helper.uncheckGridRowCheckbox(SPECIMEN_GROUP_ONE, 0);
        waitForText("Showing 1 Results");
        Assert.assertEquals(1, getXpathCount(Locator.xpath("//td[text()='Screening']/..//td[3][text()='15']")));
        Assert.assertEquals(1, getXpathCount(Locator.xpath("//td[text()='Screening']/..//td[4][text()='1']")));

        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByLabel("Report Name", PARTICIPANT_REPORT4_NAME);
        clickSaveParticipantReport();

        //Participant report with multiple demographic fields
        clickAndWait(Locator.linkWithText("Manage"));
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("DEM-1: Demographics"));
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.xpath("//input[@name='demographicData']"));
        checkCheckbox(Locator.xpath("//input[@name='demographicData']"));
        clickButton("Save");

        goToManageViews();
        clickMenuButton("Create", "Mouse Report");

        // select some measures from the demographics
        waitAndClickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", "demographic");
        pressEnter(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//input[contains(@class, 'x4-form-text') and @type='text']");

        _extHelper.clickX4GridPanelCheckbox("label", "1.Date of Birth", "measuresGridPanel", true);
        _extHelper.clickX4GridPanelCheckbox("label", "2.What is your sex?", "measuresGridPanel", true);
        _extHelper.clickX4GridPanelCheckbox("label", "5. Sexual orientation", "measuresGridPanel", true);
        clickButton("Select", 0);
        waitForText("Showing partial results while in edit mode.", WAIT_FOR_JAVASCRIPT);

        // verify the data in the report
        waitForText("1.Date of Birth", 27, WAIT_FOR_JAVASCRIPT); // 24 mice + 1 Report Measures list + 2 in hidden add measure dialog
        waitForText("2.What is your sex?", 26, WAIT_FOR_JAVASCRIPT); // 24 mice + 1 Report Measures list + 1 in hidden add measure dialog
        waitForText("5. Sexual orientation", 26, WAIT_FOR_JAVASCRIPT); // 24 mice + 1 Report Measures list + 1 in hidden add measure dialog
        assertTextPresentInThisOrder("1965-03-06", "Female", "heterosexual");

        _extHelper.setExtFormElementByLabel("Report Name", PARTICIPANT_REPORT5_NAME);
        clickSaveParticipantReport();
    }

    private void clickSaveParticipantReport()
    {
        clickButton("Save", 0);
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _extHelper.waitForExtDialogToDisappear("Saved");
        _ext4Helper.waitForComponentNotDirty("participant-report-panel-1");
    }

    @LogMethod
    private void doParticipantFilterTests()
    {
        doParticipantReportFilterTest();
        doParticipantListFilterTest();
    }

    @LogMethod
    private void doParticipantReportFilterTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        waitAndClick(Locator.linkWithText(PARTICIPANT_REPORT5_NAME));
        waitForPageToLoad();

        waitForText("Showing 24 Results");
        waitForElement(Locator.css(".report-filter-window.x4-collapsed"));
        log("Verify report filter window");
        expandReportFilterWindow();
        collapseReportFilterWindow();
        expandReportFilterWindow();
        closeReportFilterWindow();
        openReportFilterWindow();

        deselectAllFilterGroups();
        waitForText("Showing 0 Results");

        selectAllFilterGroups();
        waitForText("Showing 24 Results");

        _ext4Helper.clickParticipantFilterGridRowText("Not in any cohort", 0);
        waitForText("Showing 0 Results");

        _ext4Helper.checkGridRowCheckbox(COHORT_1);
        waitForText("Showing 10 Results");

        _ext4Helper.clickParticipantFilterGridRowText(COHORT_2, 0);
        waitForText("Showing 14 Results");

        // Selecting all or none of an entire category should not filter report
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0); // click group, not category with the same name
        waitForText("Showing 6 Results");
        _ext4Helper.uncheckGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0); // click group, not category with the same name
        waitForText("Showing 14 Results");
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0); // click group, not category with the same name
        waitForText("Showing 6 Results");
        _ext4Helper.clickParticipantFilterCategory(PARTICIPANT_GROUP_ONE); // click category
        waitForText("Showing 14 Results");

        //Check intersection between cohorts and multiple categories
        _ext4Helper.clickParticipantFilterGridRowText(MICE_A, 0);
        waitForText("Showing 3 Results");
        _ext4Helper.clickParticipantFilterGridRowText(SPECIMEN_GROUP_TWO, 0); // click group, not category with the same name
        waitForText("Showing 1 Results");

        selectAllFilterGroups();
        waitForText("Showing 24 Results");

        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[" + Locator.NOT_HIDDEN + "]"), WAIT_FOR_JAVASCRIPT);
        clickSaveParticipantReport();

        //TODO: Test toggling participant/group modes
        //TODO: Blocked: 16110: Participant report filter panel loses state when switching between participant and group modes
    }

    @LogMethod
    private void doParticipantListFilterTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickTab("Mice");
        waitForElement(Locator.css(".participant-filter-panel"));

        waitForText("Showing all 138 mice.");

        deselectAllFilterGroups();
        waitForText("No matching Mice.");

        selectAllFilterGroups();
        waitForText("Found 138 mice of 138.");

        _ext4Helper.clickParticipantFilterGridRowText("Not in any cohort", 0);
        waitForText("Found 113 mice of 138.");

        _ext4Helper.checkGridRowCheckbox(COHORT_1);
        waitForText("Found 123 mice of 138.");

        _ext4Helper.clickParticipantFilterGridRowText(COHORT_2, 0);
        waitForText("Found 15 mice of 138.");

        // Selecting all or none of an entire category should not filter report
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 7 mice of 138.");
        _ext4Helper.uncheckGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 15 mice of 138.");
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 7 mice of 138.");
        _ext4Helper.clickParticipantFilterCategory(PARTICIPANT_GROUP_ONE);
        waitForText("Found 15 mice of 138.");

        //Check intersection between cohorts and multiple categories
        _ext4Helper.clickParticipantFilterGridRowText(MICE_A, 0);
        waitForText("Found 3 mice of 138.");
        _ext4Helper.clickParticipantFilterGridRowText(SPECIMEN_GROUP_TWO, 0);
        waitForText("Found 1 mouse of 138.");

        setFormElement(Locator.id("participantsDiv1.filter"), PTIDS_ONE[0]);
        waitForText("No mouse IDs contain \""+PTIDS_ONE[0]+"\".");
        selectAllFilterGroups();
        waitForText("Found 1 mouse of 138.");
    }

    private void expandReportFilterWindow()
    {
        assertElementPresent(Locator.css(".report-filter-window.x4-collapsed"));
        click(Locator.css(".report-filter-window .x4-tool-expand-right"));
        waitForElement(Locator.css(".report-filter-window .x4-tool-collapse-left"));
        assertElementNotPresent(Locator.css(".report-filter-window.x4-collapsed"));
    }

    private void collapseReportFilterWindow()
    {
        assertElementNotPresent(Locator.css(".report-filter-window.x4-collapsed"));
        assertElementNotPresent(Locator.css(".report-filter-window.x4-hide-offsets"));
        click(Locator.css(".report-filter-window .x4-tool-collapse-left"));
        waitForElement(Locator.css(".report-filter-window.x4-collapsed"));
    }

    private void closeReportFilterWindow()
    {
        assertElementPresent(Locator.css(".report-filter-window"));
        assertElementNotPresent(Locator.css(".report-filter-window.x4-hide-offsets"));
        click(Locator.css(".report-filter-window .x4-tool-close"));
        waitForElement(Locator.css(".report-filter-window.x4-hide-offsets"));
    }

    private void openReportFilterWindow()
    {
        assertElementPresent(Locator.css(".report-filter-window.x4-hide-offsets"));
        clickButton("Filter Report", 0);
        waitForElementToDisappear(Locator.css(".report-filter-window.x4-hide-offsets"), WAIT_FOR_JAVASCRIPT);
    }

    private static final String DISCUSSION_BODY_1 = "Starting a discussion";
    private static final String DISCUSSION_TITLE_1 = "Discussion about R report";
    private static final String DISCUSSION_BODY_2 = "Responding to a discussion";
    private static final String DISCUSSION_BODY_3 = "Editing a discussion response";
    @LogMethod
    private void doReportDiscussionTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickReportGridLink(R_SCRIPTS[0], "edit");

        _extHelper.clickExtDropDownMenu("discussionMenuToggle", "Start new discussion");
        waitForPageToLoad();

        waitForElement(Locator.id("title"), WAIT_FOR_JAVASCRIPT);
        setFormElement("title", DISCUSSION_TITLE_1);
        setFormElement("body", DISCUSSION_BODY_1);
        clickButton("Submit");
        waitForPageToLoad();

        _extHelper.clickExtDropDownMenu("discussionMenuToggle", DISCUSSION_TITLE_1);
        waitForPageToLoad();

        assertTextPresent(DISCUSSION_TITLE_1);
        assertTextPresent(DISCUSSION_BODY_1);

        clickButton("Respond");
        waitForPageToLoad();
        waitForElement(Locator.id("body"));
        setFormElement("body", DISCUSSION_BODY_2);
        clickButton("Submit");
        waitForPageToLoad();

        assertTextPresent(DISCUSSION_BODY_2);

        clickAndWait(Locator.linkContainingText("edit"));
        waitForPageToLoad();
        waitForElement(Locator.id("body"));
        setFormElement("body", DISCUSSION_BODY_3);
        clickButton("Submit");
        waitForPageToLoad();

        assertTextPresent(DISCUSSION_BODY_3);
    }

    private List<String> _boxPlots = new ArrayList<String>();
    private List<String> _boxPlotsDescriptions = new ArrayList<String>();
    @LogMethod
    private void doBoxPlotTests()
    {
        doManageViewsBoxPlotTest();
        doDataRegionBoxPlotTest();
        doQuickChartBoxPlotTest();

        log("Verify saved box plots");
        clickTab("Clinical and Assay Data");
        for(int i = 0; i < _boxPlots.size(); i++)
        {
            Locator loc = Locator.linkWithText(_boxPlots.get(i));
            waitForElement(loc);
            mouseOver(loc);
            waitForText(_boxPlotsDescriptions.get(i));
            mouseOut(loc);
            waitForTextToDisappear(_boxPlotsDescriptions.get(i));
        }
    }

    private static final String BOX_PLOT_MV_1 = "Created with Rapha\u00ebl 2.1.0RCF-1: Reactogenicity-Day 2 - 4c.Induration 1st measureCohortGroup 1Group 24c.Induration 1st measure0.05.010.015.020.025.0";
    private static final String BOX_PLOT_MV_2 = "Created with Rapha\u00ebl 2.1.0Test TitleTestXAxisMice AMice BMice CNot in Cat Mice LetTestYAxis36.537.037.538.038.539.039.540.0";
    private static final String BOX_PLOT_NAME_MV = "ManageViewsBoxPlot";
    private static final String BOX_PLOT_DESC_MV = "This box plot was created through the manage views UI";
    @LogMethod
    private void doManageViewsBoxPlotTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Manage Views"));
        clickMenuButton("Create", "Box Plot");

        _extHelper.waitForExtDialog("Select Chart Query");
        //TODO: weird timing with these combo boxes.
        //Try once bug fixed: 15520: Box Plot - Allows selection of invalid schema/Query combination
//        _extHelper.selectExt4ComboBoxItem(this, "Schema", "assay");
//        _extHelper.selectExt4ComboBoxItem(this, "Query", "AssayList");
//        _extHelper.selectExt4ComboBoxItem(this, "Schema", "study");
        _extHelper.selectExt4ComboBoxItem("Query", "RCF-1: Reactogenicity-Day 2");

        // Todo: put better wait here
        sleep(5000);
        _extHelper.clickExtButton("Select Chart Query", "Save", 0);
        _extHelper.waitForExtDialog("Y Axis");
        waitForText("4c.Induration 1st measure", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[text()='4c.Induration 1st measure']"));
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");

        //Verify box plot
        assertSVG(BOX_PLOT_MV_1);
        log("Set Plot Title");
        click(Locator.css("svg text:contains('4c.Induration 1st measure')"));
        _extHelper.waitForExtDialog("Main Title");
        setFormElement(Locator.name("chart-title-textfield"), "Test Title");
        waitForElement(Locator.css(".revertMainTitle:not(.x4-disabled)"));
        clickDialogButtonAndWaitForMaskToDisappear("Main Title", "OK");
        waitForText("Test Title");

        log("Set Y Axis");
        click(Locator.css("svg text:contains('4c.Induration 1st measure')"));
        _extHelper.waitForExtDialog("Y Axis");
        click(Locator.ext4Radio("log"));
        waitForText("2.Body temperature", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[text()='2.Body temperature']"));
        setFormElement(Locator.name("label"), "TestYAxis");
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");
        waitForText("TestYAxis");

        log("Set X Axis");
        click(Locator.css("svg text:contains('Cohort')"));
        _extHelper.waitForExtDialog("X Axis");
        click(Locator.ext4Radio("log"));
        waitForText("Cat Mice Let", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[text()='Cat Mice Let']"));
        _extHelper.setExtFormElementByLabel("X Axis", "Label:", "TestXAxis");
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");
        waitForText("TestXAxis");

        assertSVG(BOX_PLOT_MV_2);

        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Save Chart");
        //Verify name requirement
        _extHelper.clickExtButton("Save Chart", "Save", 0);
        _extHelper.waitForExtDialog("Error");
        _extHelper.clickExtButton("Error", "OK", 0);
        _extHelper.waitForExtDialogToDisappear("Error");

        //Test cancel button
        _extHelper.setExtFormElementByLabel("Report Name", "TestReportName");
        _extHelper.setExtFormElementByLabel("Report Description", "TestReportDescription");
        clickDialogButtonAndWaitForMaskToDisappear("Save Chart", "Cancel");
        assertTextNotPresent("TestReportName");

        saveBoxPlot(BOX_PLOT_NAME_MV, BOX_PLOT_DESC_MV);
    }

    private static final String BOX_PLOT_DR_1 = "Created with Rapha\u00ebl 2.1.0RCH-1: Reactogenicity-Day 1 - 2.Body temperatureCohortGroup 2Group 12.Body temperature36.636.736.836.937.037.137.2";
    private static final String BOX_PLOT_DR_2 = "Created with Rapha\u00ebl 2.1.0RCH-1: Reactogenicity-Day 1 - 2.Body temperatureCohortGroup 1Group 22.Body temperature36.537.037.538.038.539.039.540.0";
    private static final String BOX_PLOT_NAME_DR = "DataRegionBoxPlot";
    private static final String BOX_PLOT_DESC_DR = "This box plot was created through a data region's 'Views' menu";
    /// Test Box Plot created from a filtered data region.
    @LogMethod
    private void doDataRegionBoxPlotTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("RCH-1: Reactogenicity-Day 1"));
        setFilter("Dataset", "RCHtempc", "Is Less Than", "39");
        clickMenuButton("Charts", "Create Box Plot");

        _extHelper.waitForExtDialog("Y Axis");
        waitForText("2.Body temperature", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[text()='2.Body temperature']"));
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");

        //Verify box plot
        assertSVG(BOX_PLOT_DR_1);

        //Change filter and check box plot again
        clickButton("View Data", 0);
        clearFilter("aqwp3", "RCHtempc", 0);
        waitForText("40.0");
        clickButton("View Chart", 0);
        assertSVG(BOX_PLOT_DR_2);

        //Enable point click function for this box plot
        clickOptionButtonAndWaitForDialog("Developer", "Developer Options");
        clickButton("Enable", 0);
        clickDialogButtonAndWaitForMaskToDisappear("Developer Options", "OK");
        Locator svgCircleLoc = Locator.css("svg a circle");
        waitForElement(svgCircleLoc);
        click(svgCircleLoc);
        _extHelper.waitForExtDialog("Data Point Information");
        assertTextPresentInThisOrder("MouseId/Cohort: Group 1", "RCHtempc:");
        clickButton("OK", 0);

        saveBoxPlot(BOX_PLOT_NAME_DR, BOX_PLOT_DESC_DR);
    }

    private static final String BOX_PLOT_QC = "Created with Rapha\u00ebl 2.1.0Types - DoubleCohortGroup 1Group 2Double0.020000000.040000000.060000000.080000000.0100000000.0120000000.0";
    private static final String BOX_PLOT_NAME_QC = "QuickChartBoxPlot";
    private static final String BOX_PLOT_DESC_QC = "This box plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartBoxPlotTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Types"));

        createQuickChart("Dataset", "dbl");

        //Verify box plot
        assertSVG(BOX_PLOT_QC);

        saveBoxPlot(BOX_PLOT_NAME_QC, BOX_PLOT_DESC_QC);
    }

    @LogMethod
    private void createQuickChart(String regionName, String columnName)
    {
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        Locator quickChart = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":quick-chart"));

        click(header);
        waitAndClick(quickChart);
        waitForPageToLoad();
    }

    private List<String> _scatterPlots = new ArrayList<String>();
    private List<String> _scatterPlotsDescriptions = new ArrayList<String>();
    @LogMethod
    private void doScatterPlotTests()
    {
        doManageViewsScatterPlotTest();
        doDataRegionScatterPlotTest();
        doQuickChartScatterPlotTest();
        doCustomizeScatterPlotTest(); // Uses scatter plot created by doDataRegionScatterPlotTest()
        doPointClickScatterPlotTest(); // Uses scatter plot created by doManageViewsScatterPlotTest()

        log("Verify saved scatter plots");
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        for(int i = 0; i < _scatterPlots.size(); i++)
        {
            Locator loc = Locator.linkWithText(_scatterPlots.get(i));
            waitForElement(loc);
            mouseOver(loc);
            waitForText(_scatterPlotsDescriptions.get(i));
            mouseOut(loc);
            waitForTextToDisappear(_scatterPlotsDescriptions.get(i));
        }
    }

    private static final String SCATTER_PLOT_MV_1 = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200";
    private static final String SCATTER_PLOT_MV_2 = "Created with Rapha\u00ebl 2.1.0Test TitleTestXAxisMice ANot in Cat Mice LetMice BMice CTestYAxis33.034.035.036.037.038.039.040.0";
    private static final String SCATTER_PLOT_NAME_MV = "ManageViewsScatterPlot";
    private static final String SCATTER_PLOT_DESC_MV = "This scatter plot was created through the manage views UI";
    @LogMethod
    private void doManageViewsScatterPlotTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Manage Views"));
        clickMenuButton("Create", "Scatter Plot");

        _extHelper.waitForExtDialog("Select Chart Query");
        //TODO: weird timing with these combo scatteres.
        //Try once bug fixed: 15520: Scatter Plot - Allows selection of invalid schema/Query combination
//        _extHelper.selectExt4ComboScatterItem(this, "Schema", "assay");
//        _extHelper.selectExt4ComboScatterItem(this, "Query", "AssayList");
//        _extHelper.selectExt4ComboScatterItem(this, "Schema", "study");
        _extHelper.selectExt4ComboBoxItem("Query", "APX-1: Abbreviated Physical Exam");

        // Todo: put better wait here
        sleep(5000);
        _extHelper.clickExtButton("Select Chart Query", "Save", 0);
        _extHelper.waitForExtDialog("Y Axis");
        waitForText("1. Weight", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='1. Weight']"));
        _extHelper.clickExtButton("Y Axis", "Ok", 0);
        _extHelper.waitForExtDialog("X Axis");
        waitForText("4. Pulse", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='4. Pulse']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        //Verify scatter plot
        assertSVG(SCATTER_PLOT_MV_1);

        log("Set Plot Title");
        click(Locator.css("svg text:contains('APX-1: Abbreviated Physical Exam')"));
        _extHelper.waitForExtDialog("Main Title");
        setFormElement(Locator.name("chart-title-textfield"), "Test Title");
        waitForElement(Locator.css(".revertMainTitle:not(.x4-disabled)"));
        clickDialogButtonAndWaitForMaskToDisappear("Main Title", "OK");
        waitForText("Test Title");

        log("Set Y Axis");
        click(Locator.css("svg text:contains('1. Weight')"));
        _extHelper.waitForExtDialog("Y Axis");
        click(Locator.ext4Radio("log"));
        waitForText("2. Body Temp", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='2. Body Temp']"));
        setFormElement(Locator.name("label"), "TestYAxis");
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");
        waitForText("TestYAxis");

        log("Set X Axis");
        click(Locator.css("svg text:contains('4. Pulse')"));
        _extHelper.waitForExtDialog("X Axis");
        click(Locator.ext4Radio("log"));
        waitForText("Cat Mice Let", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Cat Mice Let']"));
        _extHelper.setExtFormElementByLabel("X Axis", "Label:", "TestXAxis");
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");
        waitForText("TestXAxis");

        assertSVG(SCATTER_PLOT_MV_2);

        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Save Chart");
        //Verify name requirement
        _extHelper.clickExtButton("Save Chart", "Save", 0);
        _extHelper.waitForExtDialog("Error");
        _extHelper.clickExtButton("Error", "OK", 0);
        _extHelper.waitForExtDialogToDisappear("Error");

        //Test cancel button
        _extHelper.setExtFormElementByLabel("Report Name", "TestReportName");
        _extHelper.setExtFormElementByLabel("Report Description", "TestReportDescription");
        _extHelper.clickExtButton("Save Chart", "Cancel", 0);
        assertTextNotPresent("TestReportName");

        saveScatterPlot(SCATTER_PLOT_NAME_MV, SCATTER_PLOT_DESC_MV);
    }

    private static final String SCATTER_PLOT_DR_1 = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse606570758085901. Weight50556065707580859095100105110";
    private static final String SCATTER_PLOT_DR_2 = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200";
    private static final String SCATTER_PLOT_NAME_DR = "DataRegionScatterPlot";
    private static final String SCATTER_PLOT_DESC_DR = "This scatter plot was created through a data region's 'Views' menu";
    /// Test Scatter Plot created from a filtered data region.
    @LogMethod
    private void doDataRegionScatterPlotTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("APX-1: Abbreviated Physical Exam"));
        setFilter("Dataset", "APXpulse", "Is Less Than", "100");
        clickMenuButton("Charts", "Create Scatter Plot");

        _extHelper.waitForExtDialog("Y Axis");
        waitForText("1. Weight", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='1. Weight']"));
        _extHelper.clickExtButton("Y Axis", "Ok", 0);
        _extHelper.waitForExtDialog("X Axis");
        waitForText("4. Pulse", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='4. Pulse']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        //Verify scatter plot
        assertSVG(SCATTER_PLOT_DR_1);

        //Change filter and check scatter plot again
        clickButton("View Data", 0);
        clearFilter("aqwp3", "APXpulse", 0);
        waitForText("36.0"); // Body temp for filtered out row
        clickButton("View Chart", 0);
        assertSVG(SCATTER_PLOT_DR_2);

        log("Verify point stying");

        saveScatterPlot(SCATTER_PLOT_NAME_DR, SCATTER_PLOT_DESC_DR);
    }

    private static final String SCATTER_PLOT_QC = "Created with Rapha\u00ebl 2.1.0Types - DoubleInteger0.0200000.0400000.0600000.0800000.01000000.01200000.0Double10000000.020000000.030000000.040000000.050000000.060000000.070000000.080000000.090000000.0100000000.0110000000.0120000000.0";
    private static final String SCATTER_PLOT_NAME_QC = "QuickChartScatterPlot";
    private static final String SCATTER_PLOT_DESC_QC = "This scatter plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartScatterPlotTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Types"));

        createQuickChart("Dataset", "dbl");

        log("Set X Axis");
        waitAndClick(Locator.css("svg text:contains('Cohort')"));
        _extHelper.waitForExtDialog("X Axis");
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Integer']"));
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Integer']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        clickOptionButtonAndWaitForDialog("Options", "Plot Options");
        _extHelper.selectExt4ComboBoxItem("Plot Type", "Scatter Plot");
        clickDialogButtonAndWaitForMaskToDisappear("Plot Options", "OK");

        assertSVG(SCATTER_PLOT_QC);

        saveScatterPlot(SCATTER_PLOT_NAME_QC, SCATTER_PLOT_DESC_QC);
    }

    private static final String SCATTER_PLOT_CUSTOMIZED_COLORS = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200 Group 1 Group 2";
    private static final String SCATTER_PLOT_CUSTOMIZED_SHAPES = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200 normal abnormal/insignificant abnormal/significant";
    private static final String SCATTER_PLOT_CUSTOMIZED_BOTH = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200 Group 1 Group 2 normal abnormal/insignificant abnormal/significant";

    @LogMethod
    private void doCustomizeScatterPlotTest()
    {
        clickReportGridLink(SCATTER_PLOT_NAME_DR, "view");
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Locator.button("Grouping"));
        assertElementNotPresent(Locator.button("Save"));
        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Locator.button("Edit"));

        // Verify default styling for point at origin - blue circles
        waitForElement(Locator.css("svg > a > circle"));
        Assert.assertEquals("Scatter points doin't have expected initial color", "#3366ff", getAttribute(Locator.css("svg > a > circle"), "fill"));

        // Enable Grouping - Colors
        log("Group with colors");
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Locator.id("colorCategory-inputEl"));
        click(Locator.ext4Radio("Single shape"));
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        assertSVG(SCATTER_PLOT_CUSTOMIZED_COLORS);
        // Verify custom styling for point at origin (APXpulse: 60, APXwtkg: 48) - pink triangle
        Assert.assertEquals("Point at (60, 48) was an unexpected color", "#fc8d62", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "fill"));
        Assert.assertTrue("Point at (60, 48) was an unexpected shape", isElementPresent(Locator.css("svg > a:nth-of-type(26) > circle")));
        // Verify custom styling for another point (APXpulse: 92, APXwtkg: 89) - teal square
        Assert.assertEquals("Square at (92, 89) was an unexpected color", "#66c2a5", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "fill"));
        Assert.assertTrue("Square at (92, 89) was an unexpected width", isElementPresent(Locator.css("svg > a:nth-of-type(25) > circle")));


        // Enable Grouping - Shapes
        log("Group with shapes");
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Locator.ext4Radio("With a single color"));
        click(Locator.id("shapeCategory-inputEl"));
        _extHelper.selectExt4ComboBoxItem("Point Category:", "16. Evaluation Summary");
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        assertSVG(SCATTER_PLOT_CUSTOMIZED_SHAPES);
        // Verify custom styling for point at origin (APXpulse: 60, APXwtkg: 48) - pink triangle
        Assert.assertEquals("Point at (60, 48) was an unexpected color", "#3366ff", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "fill"));
        Assert.assertEquals("Point at (60, 48) was an unexpected shape", "M75,-45L80,-55L70,-55Z", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "d"));
        // Verify custom styling for another point (APXpulse: 92, APXwtkg: 89) - teal square
        Assert.assertEquals("Square at (92, 89) was an unexpected color", "#3366ff", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "fill"));
        Assert.assertEquals("Square at (92, 89) was an unexpected width", "10", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "width"));
        Assert.assertEquals("Square at (92, 89) was an unexpected height", "10", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "height"));


        // Enable Grouping - Shapes & Colors
        log("Group with both");
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Locator.id("colorCategory-inputEl"));
        click(Locator.id("shapeCategory-inputEl"));
        _extHelper.selectExt4ComboBoxItem("Point Category:", "16. Evaluation Summary");
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        assertSVG(SCATTER_PLOT_CUSTOMIZED_BOTH);
        // Verify custom styling for point at origin (APXpulse: 60, APXwtkg: 48) - pink triangle
        Assert.assertEquals("Point at (60, 48) was an unexpected color", "#fc8d62", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "fill"));
        Assert.assertEquals("Point at (60, 48) was an unexpected shape", "M75,-45L80,-55L70,-55Z", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "d"));
        // Verify custom styling for another point (APXpulse: 92, APXwtkg: 89) - teal square
        Assert.assertEquals("Square at (92, 89) was an unexpected color", "#66c2a5", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "fill"));
        Assert.assertEquals("Square at (92, 89) was an unexpected width", "10", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "width"));
        Assert.assertEquals("Square at (92, 89) was an unexpected height", "10", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "height"));

        saveScatterPlot(SCATTER_PLOT_NAME_DR + " Colored", SCATTER_PLOT_DESC_DR + " Colored");
    }

    private static final String TEST_DATA_API_PATH = "server/test/data/api";

    @LogMethod
    private void doPointClickScatterPlotTest()
    {
        clickReportGridLink(SCATTER_PLOT_NAME_MV, "view");
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Locator.button("Grouping"));
        assertElementNotPresent(Locator.button("Save"));
        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Locator.button("Edit"));

        log("Check Scatter Plot Point Click Function (Developer Only)");
        // open the developer panel and verify that it is disabled by default
        assertElementPresent(Locator.button("Developer"));
        clickOptionButtonAndWaitForDialog("Developer", "Developer Options");
        assertElementPresent(Locator.button("Enable"));
        assertElementNotPresent(Locator.button("Disable"));
        // enable the feature and verify that you can switch tabs
        clickButton("Enable", 0);
        _ext4Helper.clickTabContainingText("Help");
        assertTextPresentInThisOrder("Your code should define a single function", "data:", "measureInfo:", "clickEvent:");
        assertTextPresentInThisOrder("YAxisMeasure:", "XAxisMeasure:", "ColorMeasure:", "PointMeasure:");
        _ext4Helper.clickTabContainingText("Source");
        click(Locator.xpath("//input/../label[contains(text(), 'Toggle editor')]"));
        sleep(1000); // wait for editor to toggle
        Assert.assertTrue("Default point click function not inserted in to editor", getFormElement("point-click-fn-textarea").startsWith("function (data, measureInfo, clickEvent) {"));
        // apply the default point click function
        clickDialogButtonAndWaitForMaskToDisappear("Developer Options", "OK");
        Locator svgCircleLoc = Locator.css("svg a circle");
        waitForElement(svgCircleLoc);
        click(svgCircleLoc);
        _extHelper.waitForExtDialog("Data Point Information");
        clickButton("OK", 0);
        // open developer panel and test JS function validation
        clickOptionButtonAndWaitForDialog("Developer", "Developer Options");
        setFormElement("point-click-fn-textarea", "");
        _extHelper.clickExtButton("Developer Options", "OK", 0);
        assertTextPresent("Error: the value provided does not begin with a function declaration.");
        setFormElement("point-click-fn-textarea", "function(){");
        _extHelper.clickExtButton("Developer Options", "OK", 0);
        assertTextPresent("Error parsing the function:");
        clickButton("Disable", 0);
        _extHelper.waitForExtDialog("Confirmation...");
        _extHelper.clickExtButton("Confirmation...", "Yes", 0);
        assertTextNotPresent("Error");
        // test use-case to navigate to query page on click
        clickButton("Enable", 0);
        String function = getFileContents(TEST_DATA_API_PATH + "/scatterPlotPointClickTestFn.js");
        setFormElement("point-click-fn-textarea", function);
        clickDialogButtonAndWaitForMaskToDisappear("Developer Options", "OK");
        saveScatterPlot(SCATTER_PLOT_NAME_MV + " PointClickFn", SCATTER_PLOT_DESC_MV + " PointClickFn");
        clickAndWait(Locator.css("svg a circle"));
        waitForText("Query Schema Browser");
        assertTextPresent("APX-1: Abbreviated Physical Exam");
        // verify that only developers can see the button to add point click function
        createUser(DEVELOPER_USER, null);
        clickFolder(getProjectName());
        enterPermissionsUI();
        setUserPermissions(DEVELOPER_USER, "Editor");
        impersonate(DEVELOPER_USER);
        clickFolder(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(SCATTER_PLOT_NAME_MV + " PointClickFn"));
        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
        waitForText("Test Title");
        pushLocation();
        assertElementNotPresent(Locator.button("Developer"));
        clickAndWait(Locator.css("svg a circle"));
        waitForText("APX-1: Abbreviated Physical Exam");
        stopImpersonating();
        // give DEVELOPER_USER developer perms and try again
        createSiteDeveloper(DEVELOPER_USER);
        impersonate(DEVELOPER_USER);
        popLocation();
        waitForText("Test Title");
        assertElementPresent(Locator.button("Developer"));
        stopImpersonating();
    }

    private void assertSVG(final String expectedSvgText)
    {
        doesElementAppear(new Checker()
        {
            @Override
            public boolean check()
            {
                return isElementPresent(Locator.css("svg")) &&
                       expectedSvgText.equals(getText(Locator.css("svg")));
            }
        }, WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("SVG did not look as expected", expectedSvgText, getText(Locator.css("svg")));
    }

    private void saveBoxPlot(String name, String description)
    {
        savePlot(name, description);
        _boxPlots.add(name);
        _boxPlotsDescriptions.add(description);
    }

    private void saveScatterPlot(String name, String description)
    {
        savePlot(name, description);
        _scatterPlots.add(name);
        _scatterPlotsDescriptions.add(description);
    }

    @LogMethod
    private void savePlot(String name, String description)
    {
        boolean saveAs = getButtonLocator("Save As") != null;

        clickButton(saveAs ? "Save As" : "Save", 0);
        _extHelper.waitForExtDialog(saveAs ? "Save As" : "Save Chart");
        _extHelper.setExtFormElementByLabel("Report Name", name);
        _extHelper.setExtFormElementByLabel("Report Description", description);

        clickDialogButtonAndWaitForMaskToDisappear(saveAs ? "Save As" : "Save Chart", "Save");
        _extHelper.waitForExtDialogToDisappear("Saved");
        waitForText(name);
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return !Boolean.parseBoolean(selenium.getEval("var p = selenium.browserbot.getCurrentWindow().Ext4.getCmp('generic-report-panel-1'); " +
                        "if (p) p.isDirty(); " +
                        "else false;"));
            }
        },"Page still dirty", WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    private void doParticipantGroupCategoriesTest()
    {
        clickFolder(getProjectName());
        clickFolder(getFolderName());

        setDemographicsBit("DEM-1: Demographics", true);

        // Create category with 3 groups
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_A, "Mouse", "Cat Mice Let", true, true, "999320016,999320518,999320529,999320557");
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_B, "Mouse", "Cat Mice Let", false, true, "999320565,999320576,999320582,999320609");
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_C, "Mouse", "Cat Mice Let", false, true, "999320613,999320671,999320687");

        clickFolder(getFolderName());
        setDemographicsBit("DEM-1: Demographics", false);

        // Check that groups have correct number of members
        clickAndWait(Locator.linkWithText("Mice"));
        waitForText("Cohorts"); // Wait for participant list to appear.
        sleep(500); // Sleep because the list takes a while to populate.

        // no longer an all check box
        deselectAllFilterGroups();
        waitForText("No matching Mice");

        _ext4Helper.checkGridRowCheckbox(MICE_C);
        waitForText("Found 3 mice of 138.");

        _ext4Helper.checkGridRowCheckbox(MICE_B);
        waitForText("Found 7 mice of 138.");

        // Test changing category and changing it back
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Mouse Groups"));
        _extHelper.waitForLoadingMaskToDisappear(10000);
        _studyHelper.editCustomParticipantGroup(MICE_C, "Mouse", "Cat Mice Foo", true, true);
        waitForText("Cat Mice Foo");
        _studyHelper.editCustomParticipantGroup(MICE_C, "Mouse", "Cat Mice Let", false, true);
        waitForTextToDisappear("Cat Mice Foo");

        // Add more participants to a group
        _studyHelper.editCustomParticipantGroup(MICE_C, "Mouse", null, false, true, "999320703,999320719");

        // Check that group has correct number of participants
        clickAndWait(Locator.linkWithText("Mice"));
        waitForElement(Locator.css(".lk-filter-panel-label")); // Wait for participant list to appear.
        deselectAllFilterGroups();
        waitForText("No matching Mice");
        _ext4Helper.checkGridRowCheckbox(MICE_C);
        waitForText("Found 5 mice of 138.");
    }

    @LogMethod
    private void deselectAllFilterGroups()
    {
        _ext4Helper.checkGridRowCheckbox("All");
        _ext4Helper.uncheckGridRowCheckbox("All");
    }

    private void clickDialogButtonAndWaitForMaskToDisappear(String dialogTitle, String btnTxt)
    {
        _extHelper.clickExtButton(dialogTitle, btnTxt, 0);
        _extHelper.waitForExtDialogToDisappear(dialogTitle);
        sleep(500);
        _ext4Helper.waitForMaskToDisappear();
    }

    private void clickOptionButtonAndWaitForDialog(String btnTxt, String dialogTitle)
    {
        clickButton(btnTxt, 0);
        _extHelper.waitForExtDialog(dialogTitle);
    }

    private void selectAllFilterGroups()
    {
        _ext4Helper.checkGridRowCheckbox("All");
    }

    private static final String QUERY_REPORT_NAME = "First Test Query Report";
    private static final String QUERY_REPORT_DESCRIPTION = "Description for the first query report.";
    private static final String QUERY_REPORT_SCHEMA_NAME = "study";
    private static final String QUERY_REPORT_QUERY_NAME = "Mouse";

    private static final String QUERY_REPORT_NAME_2 = "Second Test Query Report";
    private static final String QUERY_REPORT_DESCRIPTION_2 = "Description for the first query report.";
    private static final String QUERY_REPORT_SCHEMA_NAME_2 = "study";
    private static final String QUERY_REPORT_QUERY_NAME_2 = "AE-1:(VTN) AE Log";
    private static final String QUERY_REPORT_VIEW_NAME_2 = "Limited PTIDS";
    private static final String[] PTIDS_FOR_CUSTOM_VIEW = {"999320533", "999320541", "999320529", "999320518"};



    @LogMethod
    private void doQueryReportTests()
    {
        log("Create a query report.");

        clickFolder(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();

        createReport(QUERY_VIEW);

        setFormElement("viewName", QUERY_REPORT_NAME);
        setFormElement("description", QUERY_REPORT_DESCRIPTION);
        _ext4Helper.selectComboBoxItem("Schema:", QUERY_REPORT_SCHEMA_NAME);
        waitForTextToDisappear("loading..."); // Ext4Helper.waitForMaskToDisappear(this) doesn't seem to work.
        _ext4Helper.selectComboBoxItem("Query:", QUERY_REPORT_QUERY_NAME);
        waitForTextToDisappear("loading...");
        setFormElement("selectedQueryName", QUERY_REPORT_QUERY_NAME);

        clickButton("Save");
        waitForText("Manage Views");
        waitForText(QUERY_REPORT_NAME);

        clickReportGridLink(QUERY_REPORT_NAME, "view");
        assertTextPresent(QUERY_REPORT_NAME);
        waitForText("1 - 100 of 138");
        goBack();

        clickFolder(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkWithText("AE-1:(VTN) AE Log"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewFilter("MouseId", "Mouse Id", "Equals One Of", "999320533;999320541;999320529;999320518");
        _customizeViewsHelper.saveCustomView(QUERY_REPORT_VIEW_NAME_2);

        goToManageViews();

        createReport(QUERY_VIEW);

        setFormElement("viewName", QUERY_REPORT_NAME_2);
        setFormElement("description", QUERY_REPORT_DESCRIPTION_2);
        _ext4Helper.selectComboBoxItem("Schema:", QUERY_REPORT_SCHEMA_NAME_2);
        waitForTextToDisappear("loading...");
        _ext4Helper.selectComboBoxItem("Query:", QUERY_REPORT_QUERY_NAME_2);
        waitForTextToDisappear("loading...");
        _ext4Helper.selectComboBoxItem("View:", QUERY_REPORT_VIEW_NAME_2);

        clickButton("Save");
        waitForText("Manage Views");
        waitForText(QUERY_REPORT_NAME_2);

        String datasetName = "AE-1:(VTN) AE Log";

        clickTab("Clinical and Assay Data");
        waitForText(datasetName);
        clickAndWait(Locator.linkWithText(datasetName));

        clickMenuButton("Views", QUERY_REPORT_NAME_2);

        DataRegionTable table = new DataRegionTable("Dataset", this);

        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (String value : table.getColumnDataAsText("MouseId"))
        {
            if (!counts.containsKey(value))
                counts.put(value, 1);
            else
            {
                int count = counts.get(value);
                counts.put(value, count + 1);
            }
        }
        Assert.assertTrue(counts.get(PTIDS_FOR_CUSTOM_VIEW[0]) == 3);
        Assert.assertTrue(counts.get(PTIDS_FOR_CUSTOM_VIEW[1]) == 1);
        Assert.assertTrue(counts.get(PTIDS_FOR_CUSTOM_VIEW[2]) == 3);
        Assert.assertTrue(counts.get(PTIDS_FOR_CUSTOM_VIEW[3]) == 3);
    }
}
