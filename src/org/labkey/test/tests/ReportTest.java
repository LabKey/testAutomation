/*
 * Copyright (c) 2009-2012 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.StudyHelper;

import java.io.File;

/**
 * User: klum
 * Date: Jul 31, 2009
 */
public class ReportTest extends StudyBaseTest
{
    protected static final String GRID_VIEW = "create_gridView";
    protected static final String R_VIEW = "create_rView";
    private final static String DATA_SET = "DEM-1: Demographics";
    private final static String DATA_BASE_PREFIX = "DEM";
    private final static String R_SCRIPT1_ORIG_FUNC = "length(x)";
    private final static String R_SCRIPT1_EDIT_FUNC = "length(x) * 2";
    protected static final String TEST_GROUP = "firstGroup";
    protected static final String TEST_USER = "user1@report.test";
    private static final String TEST_GRID_VIEW = "Test Grid View";

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
    private String R_SCRIPT3(String database, String colName)
    {
        return "source(\"" + R_SCRIPTS[1] + ".R\")\n" +
            "x <- func2(labkey.data$" + colName + ", labkey.data$" + database + "sex)\n" +
            "x\n";
    }
    private final static String R_SCRIPT2_TEXT2 = "999320672";

    @Override
    protected void doCleanup() throws Exception
    {
        deleteUser(TEST_USER);
        deleteUser(R_USER);
        super.doCleanup();
    }

    protected void doCreateSteps()
    {
        enableEmailRecorder();
        // fail fast if R is not configured
        RReportHelper.ensureRConfig(this);

        // import study and wait; no specimens needed
        importStudy();
        startSpecimenImport(2);

        // wait for study and specimens to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);
        waitForSpecimenImport();

        //Need to create participant groups before we flip the demographics bit on DEM-1.
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), PARTICIPANT_GROUP_ONE, "Mouse", PTIDS_ONE);
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), PARTICIPANT_GROUP_TWO, "Mouse", PTIDS_TWO);
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), SPECIMEN_GROUP_ONE, "Mouse", SPEC_PTID_ONE);
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), SPECIMEN_GROUP_TWO, "Mouse", SPEC_PTID_TWO);

        // need this to turn off the demographic bit in the DEM-1 dataset
        clickLinkWithText(getFolderName());
        setDemographicsBit("DEM-1: Demographics", false);
    }

    protected void doVerifySteps()
    {
        doCreateCharts();
        doCreateRReports();
        doReportDiscussionTest();
        doAttachmentReportTest();
        doParticipantReportTest();

        // additional report and security tests
        setupDatasetSecurity();
        doReportSecurity();
    }

    protected void deleteReport(String reportName)
    {
        clickLinkWithText("Manage Views");
        final Locator report = Locator.tagContainingText("div", reportName);

        // select the report and click the delete button
        waitForElement(report, 10000);
        selenium.mouseDown(report.toString());

        String id = ExtHelper.getExtElementId(this, "btn_deleteView");
        click(Locator.id(id));

        ExtHelper.waitForExtDialog(this, "Delete Views", WAIT_FOR_JAVASCRIPT);

        String btnId = selenium.getEval("this.browserbot.getCurrentWindow().Ext.MessageBox.getDialog().buttons[1].getId();");
        click(Locator.id(btnId));

        // make sure the report is deleted
        waitFor(new Checker() {
            public boolean check()
            {
                return !isElementPresent(report);
            }
        }, "Failed to delete report: " + reportName, WAIT_FOR_JAVASCRIPT);
    }

    protected void clickReportGridLink(String reportName, String linkText)
    {
        clickTab("Manage");
        clickLinkWithText("Manage Views");
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

        clickAndWait(link);
    }

    private void doCreateCharts()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");

        clickMenuButton("Views", "Create", "Crosstab View");
        selectOptionByValue("rowField",  "DEMsex");
        selectOptionByValue("colField", "DEMsexor");
        selectOptionByValue("statField", "SequenceNum");
        clickNavButton("Submit");

        String[] row3 = new String[] {"Male", "2", "9", "3", "14"};
        assertTableRowsEqual("report", 3, new String[][] {row3});

        setFormElement("label", "TestReport");
        clickNavButton("Save");

        clickLinkWithText(getStudyLabel());
        assertTextPresent("TestReport");
        clickLinkWithText("TestReport");

        assertTableCellTextEquals("report", 2, 0, "Female");

        //Delete the report
        clickLinkWithText(getStudyLabel());
        clickTab("Manage");
        deleteReport("TestReport");

        // create new grid view report:
        String viewName = "DRT Eligibility Query";
        createReport(GRID_VIEW);
        setFormElement("label", viewName);
        selectOptionByText("params", "ECI-1: Eligibility Criteria");
        clickNavButton("Create View");
        assertLinkPresentWithText("999320016");
        assertNavButtonNotPresent("go");
        clickLinkWithText(getStudyLabel());
        clickTab("Manage");
        deleteReport(viewName);

        // create new external report
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");
        clickMenuButton("Views", "Create", "Advanced View");
        selectOptionByText("queryName", "DEM-1: Demographics");
        String java = System.getProperty("java.home") + "/bin/java";
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE} ${REPORT_FILE}");
        clickNavButton("Submit");
        assertTextPresent("Female");
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE}");
        selectOptionByValue("fileExtension", "tsv");
        clickNavButton("Submit");
        assertTextPresent("Female");
        setFormElement("label", "tsv");
        selectOptionByText("showWithDataset", "DEM-1: Demographics");
        clickNavButton("Save");
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("tsv");
        assertTextPresent("Female");
    }

    @Override
    protected String getProjectName()
    {
        return "ReportVerifyProject";  // don't want this test to stomp on StudyVerifyProject
    }

    protected void doCreateRReports()
    {
        log("Create an R Report");

        click(Locator.linkWithText("Projects"));
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText(DATA_SET);
        clickMenuButton("Views", "Create", "R View");
        setQueryEditorValue("script", "");

        log("Execute bad scripts");
        clickViewTab();
        assertTextPresent("Empty script, a script must be provided.");
        if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX) + "\nbadString", R_SCRIPT1_TEXT1))
            if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX.toLowerCase()) + "\nbadString", R_SCRIPT1_TEXT1))
                fail("There was an error running the script");
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
        if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1))
            if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX.toLowerCase()), R_SCRIPT1_TEXT1))
                fail("There was an error running the script");
        log("Check that the script executed properly");
        assertTextPresent(R_SCRIPT1_TEXT1);
        assertTextPresent(R_SCRIPT1_TEXT2);
        assertElementPresent(Locator.id(R_SCRIPT1_IMG));
        assertTextPresent(R_SCRIPT1_PDF);

        saveReport(R_SCRIPTS[0]);

        log("Create view");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewColumn(this, R_REMCOL);
        CustomizeViewsHelper.addCustomizeViewFilter(this, "DEMhisp", "3.Latino\\a or Hispanic?", "Does Not Equal", "Yes");
        CustomizeViewsHelper.addCustomizeViewSort(this, R_SORT, "2.What is your sex?", "Descending");
        CustomizeViewsHelper.saveCustomView(this, R_VIEW);

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
        //clickNavButton("Reports >>", 0);
        //clickLinkWithText(R_SCRIPTS[0]);
        clickMenuButton("Views", R_SCRIPTS[0]);
        waitForText("Console output", WAIT_FOR_PAGE);
        assertTextPresent("null device");
        assertTextNotPresent("Error executing command");
        assertTextPresent(R_SCRIPT1_TEXT1);
        assertTextPresent(R_SCRIPT1_TEXT2);
        assertElementPresent(Locator.id(R_SCRIPT1_IMG));
        assertTextPresent(R_SCRIPT1_PDF);
        popLocation();

        log("Create second R script");
        clickMenuButton("Views", "Create", "R View");
        click(Locator.raw("//td[contains(text(),'" + R_SCRIPTS[0] + "')]/input"));
        if (!RReportHelper.executeScript(this, R_SCRIPT2(DATA_BASE_PREFIX, "mouseId"), R_SCRIPT2_TEXT1))
            if (!RReportHelper.executeScript(this, R_SCRIPT2(DATA_BASE_PREFIX.toLowerCase(), "mouseid"), R_SCRIPT2_TEXT1))
                fail("There was an error running the script");
        clickSourceTab();
        checkCheckbox("shareReport");
        checkCheckbox("runInBackground");
        clickViewTab();

        log("Check that R script worked");
        assertTextPresent(R_SCRIPT2_TEXT1);
        saveReport(R_SCRIPTS[1]);

        log("Check that background run works");
        log("Test user permissions");
        enterPermissionsUI();
        clickManageGroup("Users");
        setFormElement("names", R_USER);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        enterPermissionsUI();
        setPermissions("Users", "Editor");
        exitPermissionsUI();
        impersonate(R_USER);

        log("Access shared R script");
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText(DATA_SET);
        pushLocation();
        assertElementNotPresent(Locator.raw("//select[@name='Dataset.viewName']//option[.='" + R_SCRIPTS[0] + "']"));

        clickMenuButton("Views", R_SCRIPTS[1]);

        popLocation();
        log("Change user permission");
        stopImpersonating();
        clickLinkWithText(getProjectName());
        if (isTextPresent("Enable Admin"))
            clickLinkWithText("Enable Admin");
        enterPermissionsUI();
        setPermissions("Users", "Project Administrator");
        exitPermissionsUI();

        log("Create a new R script that uses other R scripts");
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText(DATA_SET);
        clickMenuButton("Views", "Create", "R View");
        click(Locator.raw("//td[contains(text(),'" + R_SCRIPTS[0] + "')]/input"));
        click(Locator.raw("//td[contains(text(),'" + R_SCRIPTS[1] + "')]/input"));
        if (!RReportHelper.executeScript(this, R_SCRIPT3(DATA_BASE_PREFIX, "mouseId"), R_SCRIPT2_TEXT1))
            if (!RReportHelper.executeScript(this, R_SCRIPT3(DATA_BASE_PREFIX.toLowerCase(), "mouseid"), R_SCRIPT2_TEXT1))
                fail("There was an error running the script");
        assertTextPresent(R_SCRIPT2_TEXT1);
        resaveReport();
        ExtHelper.waitForExtDialog(this, "Save View");

        log("Test editing R scripts");
        signOut();
        signIn();
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickReportGridLink(R_SCRIPTS[0], "edit");
        if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1))
            if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX.toLowerCase()), R_SCRIPT1_TEXT1))
                fail("There was an error running the script");
        resaveReport();
        waitForPageToLoad();

        log("Check that edit worked");
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickReportGridLink(R_SCRIPTS[1], "edit");

        clickViewTab();
        waitForElement(Locator.navButton("Start Job"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Start Job", 0);
        waitForText("COMPLETE", WAIT_FOR_PAGE);
        assertTextPresent(R_SCRIPT2_TEXT2);
        assertTextNotPresent(R_SCRIPT2_TEXT1);
        resaveReport();
        waitForPageToLoad();

        log("Clean up R pipeline jobs");
        cleanPipelineItem(R_SCRIPTS[1]);
    }

    private static final String ATTACHMENT_REPORT_NAME = "Attachment Report1";
    private static final String ATTACHMENT_REPORT_DESCRIPTION = "This attachment report uploads a file";
    private static final File ATTACHMENT_REPORT_FILE = new File(getLabKeyRoot() + "/sampledata/Microarray/", "test1.jpg"); // arbitrary image file
    private static final String ATTACHMENT_REPORT2_NAME = "Attachment Report2";
    private static final String ATTACHMENT_REPORT2_DESCRIPTION= "This attachment report points at a file on the server.";
    private static final File ATTACHMENT_REPORT2_FILE = new File(getLabKeyRoot() + "/sampledata/Microarray/", "test2.jpg"); // arbitrary image file
    private void doAttachmentReportTest()
    {
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Views");
        clickMenuButton("Create", "Attachment Report");
        clickButton("Cancel");

       if(isFileUploadAvailable())
        {
            clickMenuButton("Create", "Attachment Report");
            setFormElement("label", ATTACHMENT_REPORT_NAME);
            setFormElement("description", ATTACHMENT_REPORT_DESCRIPTION);
            setFormElement("uploadFile", ATTACHMENT_REPORT_FILE.toString());
            setFormElement(Locator.xpath("id('uploadFile')/div/input"), ATTACHMENT_REPORT_FILE.toString());
            clickNavButton("Submit");
        }

        clickMenuButton("Create", "Attachment Report");
        setFormElement("label", ATTACHMENT_REPORT2_NAME);
        setFormElement("description", ATTACHMENT_REPORT2_DESCRIPTION);
        click(Locator.xpath("//input[../label[string()='Use a file on server localhost']]"));
        setFormElement("filePath", ATTACHMENT_REPORT2_FILE.toString());
        clickNavButton("Submit");

        clickLinkWithText("Clinical and Assay Data");
        if(isFileUploadAvailable())
        {
            waitForText(ATTACHMENT_REPORT_NAME);
        }
        waitForText(ATTACHMENT_REPORT2_NAME);

        //TODO: Verify reports. Blocked: 13761: Attachment reports can't be viewed
//        if(isFileUploadAvailable())
//        {
//            clickReportGridLink(ATTACHMENT_REPORT_NAME, "view");
//        }
//        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "view");
    }

    private void saveReport(String name)
    {
        clickSourceTab();
        clickNavButton("Save", 0);

        if (null != name)
        {
            setFormElement(Locator.xpath("//input[@class='ext-mb-input']"), name);
            ExtHelper.clickExtButton(this, "Save");
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
        ExtHelper.clickExtTab(this, name);
        sleep(2000); // TODO
    }

    protected void deleteRReports()
    {
        log("Clean up R Reports");
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickTab("Manage");
        clickLinkWithText("Manage Views");
        for (String script : R_SCRIPTS)
        {
            while (isTextPresent(script))
            {
                click(Locator.raw("//a[contains(text(),'" + script + "')]/../../td[3]/a"));
                assertTrue(selenium.getConfirmation().matches("^Permanently delete the selected view[\\s\\S]$"));
                waitForPageToLoad();
            }
            assertTextNotPresent(script);
        }
    }

    protected void cleanPipelineItem(String item)
    {
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Files");
        if (isTextPresent(item))
        {
            checkCheckbox(Locator.raw("//td/a[contains(text(), '" + item + "')]/../../td/input"));
            clickNavButton("Delete");
            assertTextNotPresent(item);
        }
    }

    protected void setupDatasetSecurity()
    {
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickLinkWithText(getProjectName());
        clickLinkWithText("My Study");

        // create a test group and give it container read perms
        enterPermissionsUI();

        createPermissionsGroup(TEST_GROUP);

        // add user to the first test group
        clickManageGroup(TEST_GROUP);
        setFormElement("names", TEST_USER);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");

        enterPermissionsUI();
        setPermissions(TEST_GROUP, "Reader");
        clickNavButton("Save and Finish");

        // give the test group read access to only the DEM-1 dataset
        clickLinkWithText("My Study");
        enterPermissionsUI();
        ExtHelper.clickExtTab(this, "Study Security");
        waitAndClickNavButton("Study Security");

        // enable advanced study security
        selectOptionByValue("securityString", "ADVANCED_READ");
        waitForPageToLoad(30000);

        click(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//th/input[@value='READOWN']"));
        clickAndWait(Locator.id("groupUpdateButton"));

        selectOptionByText("dataset.1", "Read");
        clickAndWait(Locator.xpath("//form[@id='datasetSecurityForm']//a[@class='labkey-button']/span[text() = 'Save']"));
    }

    protected void doReportSecurity()
    {
        // create charts
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());

        clickLinkWithText("APX-1: Abbreviated Physical Exam");
        clickMenuButton("Views", "Create", "Chart View");
        waitForElement(Locator.xpath("//select[@name='columnsX']"), WAIT_FOR_JAVASCRIPT);
        selectOptionByText("columnsX", "1. Weight");
        selectOptionByText("columnsY", "4. Pulse");
        checkCheckbox("participantChart");
        clickNavButton("Save", 0);
        sleep(2000);

        setFormElement("reportName", "participant chart");
        clickNavButton("OK", 0);

        waitForElement(Locator.navButton("Views"), WAIT_FOR_JAVASCRIPT);

        clickMenuButton("Views", "default");
        waitForElement(Locator.navButton("Views"), WAIT_FOR_JAVASCRIPT);
        clickMenuButton("Views", "Create", "Chart View");
        waitForElement(Locator.xpath("//select[@name='columnsX']"), WAIT_FOR_JAVASCRIPT);

        // create a non-participant chart
        selectOptionByText("columnsX", "1. Weight");
        selectOptionByText("columnsY", "4. Pulse");
        clickNavButton("Save", 0);
        sleep(2000);

        setFormElement("reportName", "non participant chart");
        setFormElement("description", "a private chart");
        checkCheckbox("shareReport");
        clickNavButton("OK", 0);

        waitForElement(Locator.navButton("Views"), WAIT_FOR_JAVASCRIPT);

        // create grid view
        clickLinkWithText(getFolderName());
        clickTab("Manage");
        waitForElement(Locator.linkWithText("Manage Views"), WAIT_FOR_JAVASCRIPT);
        clickLinkWithText("Manage Views");

        createReport(GRID_VIEW);
        setFormElement("label", TEST_GRID_VIEW);
        selectOptionByText("datasetSelection", "APX-1: Abbreviated Physical Exam");
        clickNavButton("Create View");

        // test security
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickLinkWithText(getProjectName());
        clickLinkWithText("My Study");

        clickReportGridLink("participant chart", "permissions");
        selenium.click("useCustom");
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickNavButton("Save");

        clickReportGridLink(TEST_GRID_VIEW, "permissions");
        selenium.click("useCustom");
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickNavButton("Save");

        click(Locator.linkWithText("Manage Site"));
        sleep(3000);
        clickLinkWithText("Admin Console");
        impersonate(TEST_USER);
        clickLinkWithText(getProjectName());
        clickLinkWithText("My Study");

        assertLinkNotPresentWithText("APX-1: Abbreviated Physical Exam");
        clickLinkWithText("participant chart");

        clickLinkWithText(getFolderName());
        clickLinkWithText(TEST_GRID_VIEW);
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
    private static final String PARTICIPANT_GROUP_ONE = "TEST GROUP 1";
    private static final String PARTICIPANT_GROUP_TWO = "TEST GROUP 2";
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
    private void doParticipantReportTest()
    {
        log("Testing Participant Report");

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Views");
        clickMenuButton("Create", "Mouse Report");

        // select some measures from a dataset
        waitAndClickNavButton("Choose Measures", 0);
        ExtHelper.waitForExtDialog(this, ADD_MEASURE_TITLE);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        ExtHelper.setExtFormElementByType(this, ADD_MEASURE_TITLE, "text", "cpf-1");
        pressEnter(ExtHelper.getExtDialogXPath(this, ADD_MEASURE_TITLE)+"//input[contains(@class, 'x-form-text') and @type='text']");
        assertEquals("", 17, getXpathCount(Locator.xpath(ExtHelper.getExtDialogXPath(this, ADD_MEASURE_TITLE)+"//div[contains(@class, 'x-grid3-body')]/div[contains(@class, 'x-grid3-row')]")));

        ExtHelper.clickXGridPanelCheckbox(this, "label", "2a. Creatinine", true);
        ExtHelper.clickXGridPanelCheckbox(this, "label", "1a.ALT AE Severity Grade", true);
        ExtHelper.clickXGridPanelCheckbox(this, "label", "1a. ALT (SGPT)", true);

        clickNavButton("Select", 0);

        waitForText("Visit Date", 8, WAIT_FOR_JAVASCRIPT);
        assertTextPresent("2a. Creatinine", 19); // 8 mice + 8 grid field tooltips + 1 Report Field list + 2 in hidden add field dialog
        assertTextPresent("1a.ALT AE Severity Grade", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog
        assertTextPresent("1a. ALT (SGPT)", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog

        // select additional measures from another dataset
        clickNavButton("Choose Measures", 0);
        ExtHelper.waitForExtDialog(this, ADD_MEASURE_TITLE);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        ExtHelper.setExtFormElementByType(this, ADD_MEASURE_TITLE, "text", "2a. Creatinine");
        pressEnter(ExtHelper.getExtDialogXPath(this, ADD_MEASURE_TITLE)+"//input[contains(@class, 'x-form-text') and @type='text']");
        assertEquals("", 4, getXpathCount(Locator.xpath(ExtHelper.getExtDialogXPath(this, ADD_MEASURE_TITLE)+"//div[contains(@class, 'x-grid3-body')]/div[contains(@class, 'x-grid3-row')]")));
        ExtHelper.clickXGridPanelCheckbox(this, "queryName", "CPS-1", true);
        clickNavButton("Select", 0);

        // at this point the report should render some content
        waitForText("Creatinine", 37, WAIT_FOR_JAVASCRIPT); // 8 mice (x2 columns + tooltips) + 1 Report Field list + 2 in hidden add field dialog
        assertTextPresent("1a.ALT AE Severity Grade", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog
        assertTextPresent("1a. ALT (SGPT)", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog
        assertTextPresent("Showing 8 Results");

        // verify form validation
        clickNavButton("Save", 0);
        ExtHelper.waitForExtDialog(this, "Error");
        waitAndClickNavButton("OK", 0);

        // save the report for real
        ExtHelper.setExtFormElementByLabel(this, "Report Name", PARTICIPANT_REPORT_NAME);
        ExtHelper.setExtFormElementByLabel(this, "Report Description", PARTICIPANT_REPORT_DESCRIPTION);
        clickNavButton("Save", 0);
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden

        // verify visiting saved report
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Views");
        clickReportGridLink(PARTICIPANT_REPORT_NAME, "view");

        waitForText("Creatinine", 32, WAIT_FOR_JAVASCRIPT); // 8 mice (x2 columns)
        assertTextPresent(PARTICIPANT_REPORT_NAME);
        assertTextPresent("1a.ALT AE Severity Grade", 16); // 8 mice + 8 grid field tooltips
        assertTextPresent("1a. ALT (SGPT)", 16); // 8 mice + 8 grid field tooltips
        assertTextPresent("Showing 8 Results");
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]")); // Edit panel should be hidden

        // Delete a column and save report
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//img[@data-qtip = 'Delete']")); // Delete 'Creatinine' column.
        clickNavButton("Save", 0);
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div/div[not(contains(@style, 'display: none'))]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden

        // Delete a column save a copy of the report (Save As)
        // Not testing column reorder. Ext4 and selenium don't play well together for drag & drop
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//img[@data-qtip = 'Delete']")); // Delete 'Severity Grade' column.
        clickNavButton("Save As", 0);
        ExtHelper.waitForExtDialog(this, "Save As");
        ExtHelper.setExtFormElementByLabel(this, "Save As", "Report Name", PARTICIPANT_REPORT2_NAME);
        ExtHelper.setExtFormElementByLabel(this, "Save As", "Report Description", PARTICIPANT_REPORT2_DESCRIPTION);
        clickNavButtonByIndex("Save", 1, 0);
        waitForTextToDisappear("Severity Grade");

        // Verify saving with existing report name.
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Save As", 0);
        ExtHelper.waitForExtDialog(this, "Save As");
        ExtHelper.setExtFormElementByLabel(this, "Save As", "Report Name", PARTICIPANT_REPORT_NAME);
        ExtHelper.setExtFormElementByLabel(this, "Save As", "Report Description", PARTICIPANT_REPORT2_DESCRIPTION);
        clickNavButtonByIndex("Save", 1, 0);
        ExtHelper.waitForExtDialog(this, "Failure");
        assertTextPresent("Another report with the same name already exists.");
        waitAndClickNavButton("OK", 0);
        clickNavButton("Cancel", 0); // Verify cancel button.
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div/div[not(contains(@style, 'display: none'))]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden

        
        // verify modified, saved report
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Views");
        clickReportGridLink(PARTICIPANT_REPORT_NAME, "view");

        waitForText("Creatinine", 16, WAIT_FOR_JAVASCRIPT); // 8 mice
        waitForText("Showing 8 Results", 1, WAIT_FOR_JAVASCRIPT); // There should only be 8 results, and it should state that.

        assertTextPresent(PARTICIPANT_REPORT_NAME);
        assertTextPresent("1a.ALT AE Severity Grade", 16); // 8 mice + 8 grid field tooltips
        assertTextPresent("1a. ALT (SGPT)", 16); // 8 mice + 8 grid field tooltips
        assertTextPresent("Showing 8 Results");
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]")); // Edit panel should be hidden
        log("Verify report name and description.");
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Wrong report description", PARTICIPANT_REPORT_DESCRIPTION, ExtHelper.getExtFormElementByLabel(this, "Report Description"));


        // verify modified, saved-as report
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Views");
        clickReportGridLink(PARTICIPANT_REPORT2_NAME, "view");

        waitForText("Creatinine", 16, WAIT_FOR_JAVASCRIPT); // 8 mice + 8 grid field tooltips
        assertTextPresent(PARTICIPANT_REPORT2_NAME);
        assertTextNotPresent("1a.ALT AE Severity Grade");
        assertTextPresent("1a. ALT (SGPT)", 16); // 8 mice + 8 grid field tooltips
        assertTextPresent("Showing 8 Results");
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]")); // Edit panel should be hidden
        log("Verify report name and description.");
        click(Locator.xpath("//a[./img[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Wrong report description", PARTICIPANT_REPORT2_DESCRIPTION, ExtHelper.getExtFormElementByLabel(this, "Report Description"));

        // Test group filtering
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Views");
        clickMenuButton("Create", "Mouse Report");
        // select some measures from a dataset
        waitAndClickNavButton("Choose Measures", 0);
        ExtHelper.waitForExtDialog(this, ADD_MEASURE_TITLE);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);

        ExtHelper.clickXGridPanelCheckbox(this, "label", "17a. Preg. test result", true);
        ExtHelper.clickXGridPanelCheckbox(this, "label", "1.Adverse Experience (AE)", true);

        clickNavButton("Select", 0);

        waitForText("Showing 25 Results", WAIT_FOR_JAVASCRIPT);

        //Deselect All
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), 'All')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Showing 0 Results");

        //Mouse down on GROUP 1
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), '" + PARTICIPANT_GROUP_ONE + "')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Showing 12 Results");

        //Check if all PTIDs of GROUP 1 are visible.
        for(String ptid : PTIDS_ONE)
        {
            assertTextPresent(ptid);
        }

        //Mouse down GROUP 2
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), '" + PARTICIPANT_GROUP_TWO + "')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Showing 25 Results");

        //Check that all PTIDs from GROUP 1 and GROUP 2 are present at the same time.
        for(String ptid : PTIDS)
        {
            assertTextPresent(ptid);
        }

        //Mouse down on GROUP 1 to remove it.
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), '" + PARTICIPANT_GROUP_ONE + "')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Showing 13 Results");
        
        //Check if all PTIDs of GROUP 2 are visible
        for(String ptid : PTIDS_TWO)
        {
            assertTextPresent(ptid);
        }
        //Make sure none from Group 1 are visible.
        for(String ptid : PTIDS_ONE)
        {
            assertTextNotPresent(ptid);
        }

        ExtHelper.setExtFormElementByLabel(this, "Report Name", PARTICIPANT_REPORT3_NAME);
        clickNavButton("Save", 0);
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div/div[not(contains(@style, 'display: none'))]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden

        //Participant report with specimen fields.
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Views");
        clickMenuButton("Create", "Mouse Report");
        // select some measures from a dataset
        waitAndClickNavButton("Choose Measures", 0);
        ExtHelper.waitForExtDialog(this, ADD_MEASURE_TITLE);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        ExtHelper.setExtFormElementByType(this, ADD_MEASURE_TITLE, "text", "primary type vial counts blood");
        pressEnter(ExtHelper.getExtDialogXPath(this, ADD_MEASURE_TITLE)+"//input[contains(@class, 'x-form-text') and @type='text']");

        ExtHelper.clickXGridPanelCheckbox(this, "label", "Blood (Whole):Vial Count", true);
        ExtHelper.clickXGridPanelCheckbox(this, "label", "Blood (Whole):Available Count", true);

        clickNavButton("Select", 0);

        waitForText("Showing 116 Results", WAIT_FOR_JAVASCRIPT);

        //Deselect All
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), 'All')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Showing 0 Results");

        //Mouse down on SPEC GROUP 1
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), '" + SPECIMEN_GROUP_ONE + "')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Showing 1 Results");
        assertEquals(1, getXpathCount(Locator.xpath("//td[text()='Screening']/..//td[3][text()='23']")));
        assertEquals(1, getXpathCount(Locator.xpath("//td[text()='Screening']/..//td[4][text()='3']")));

        //Add SPEC GROUP 2
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), '" + SPECIMEN_GROUP_TWO + "')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Showing 2 Results");
        //Remove SPEC GROUP 1
        mouseDown((Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), '" + SPECIMEN_GROUP_ONE + "')]/../../..//div[contains(@class, 'x4-grid-row-checker')]")));
        waitForText("Showing 1 Results");
        assertEquals(1, getXpathCount(Locator.xpath("//td[text()='Screening']/..//td[3][text()='15']")));
        assertEquals(1, getXpathCount(Locator.xpath("//td[text()='Screening']/..//td[4][text()='1']")));

        ExtHelper.setExtFormElementByLabel(this, "Report Name", PARTICIPANT_REPORT4_NAME);
        clickNavButton("Save", 0);
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div/div[not(contains(@style, 'display: none'))]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
    }

    private static final String DISCUSSION_BODY_1 = "Starting a discussion";
    private static final String DISCUSSION_TITLE_1 = "Discussion about R report";
    private static final String DISCUSSION_BODY_2 = "Responding to a discussion";
    private static final String DISCUSSION_BODY_3 = "Editing a discussion response";
    private void doReportDiscussionTest()
    {
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickReportGridLink(R_SCRIPTS[0], "edit");

        ExtHelper.clickExtDropDownMenu(this, "discussionMenuToggle", "Start new discussion");
        waitForPageToLoad();

        waitForElement(Locator.id("title"), WAIT_FOR_JAVASCRIPT);
        setFormElement("title", DISCUSSION_TITLE_1);
        setFormElement("body", DISCUSSION_BODY_1);
        clickButton("Submit");
        waitForPageToLoad();

        ExtHelper.clickExtDropDownMenu(this, "discussionMenuToggle", DISCUSSION_TITLE_1);
        waitForPageToLoad();

        assertTextPresent(DISCUSSION_TITLE_1);
        assertTextPresent(DISCUSSION_BODY_1);

        clickButton("Respond");
        waitForPageToLoad();
        setFormElement("body", DISCUSSION_BODY_2);
        clickButton("Submit");
        waitForPageToLoad();

        assertTextPresent(DISCUSSION_BODY_2);

        clickLinkContainingText("edit");
        waitForPageToLoad();
        setFormElement("body", DISCUSSION_BODY_3);
        clickButton("Submit");
        waitForPageToLoad();

        assertTextPresent(DISCUSSION_BODY_3);
    }
}
