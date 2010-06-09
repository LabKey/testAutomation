/*
 * Copyright (c) 2009-2010 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.Locator;
import org.labkey.test.drt.StudyBaseTest;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.RReportHelper;

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
    private final static String R_FILTER = "DEMhisp";
    private final static String R_FILTERED = "999320565";
    private final static String R_SORT = "DEMsex";
    private final static String R_SORT1 = "Male";
    private final static String R_SORT2 = "Female";
    private final static String R_REMCOL = "5. Sexual orientation";
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
        // fail fast if R is not configured 
        RReportHelper.ensureRConfig(this);

        // import study and wait; no specimens needed
        importStudy();
        waitForPipelineJobsToComplete(1, "study import");

        // need this to turn off the demographic bit in the DEM-1 dataset
        clickLinkWithText(getFolderName());
        setDemographicsBit("DEM-1: Demographics", false);
    }

    protected void doVerifySteps()
    {
        doCreateCharts();
        doCreateRReports();

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
        clickLinkWithText("Manage Views");
        final Locator report = Locator.tagContainingText("div", reportName);

        waitForElement(report, 10000);

        // click the row to expand it
        //Locator expander = Locator.xpath("//div[@id='viewsGrid']//td//div[.='" + reportName + "']//..//..//div[contains(@class, 'x-grid3-row-expander')]");
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
        //selectOptionByText("showWithDataset", "DEM-1: Demographics");
        clickNavButton("Save");

        clickLinkWithText(getStudyLabel());
        assertTextPresent("TestReport");
        clickLinkWithText("TestReport");

        assertTableCellTextEquals("report", 2, 0, "Female");

        //Delete the report
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        deleteReport("TestReport");

        // create new grid view report:
        String viewName = "DRT Eligibility Query";
        createReport(GRID_VIEW);
        setFormElement("label", viewName);
        selectOptionByText("params", "ECI-1: Eligibility Criteria");
        clickNavButton("Create View");
        assertLinkPresentWithText("999320016");
        //Not sure what we are lookgin for here
        //assertTextPresent("urn:lsid");
        assertNavButtonNotPresent("go");
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
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

    protected void doCreateRReports()
    {
        log("Create an R Report");

        click(Locator.linkWithText("Projects"));
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText(DATA_SET);
        clickMenuButton("Views", "Create", "R View");

        log("Execute bad scripts");
        clickNavButton("Execute Script");
        assertTextPresent("Empty script, a script must be provided.");
        if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX) + "\nbadString", R_SCRIPT1_TEXT1))
            if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX.toLowerCase()) + "\nbadString", R_SCRIPT1_TEXT1))
                fail("There was an error running the script");
        assertTextPresent("Error executing command");
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
        clickLinkWithText("Source");
        clickNavButton("Save View", 0);
        setFormElement("reportName", R_SCRIPTS[0]);
        clickNavButton("Save");

        log("Create view");
        clickMenuButton("Views", CUSTOMIZE_VIEW);
        removeCustomizeViewColumn(R_REMCOL);
        addCustomizeViewFilter(R_FILTER, "3.Latino/a or Hispanic?", "Does Not Equal", "Yes");
        addCustomizeViewSort(R_SORT, "2.What is your sex?", "DESC");
        setFormElement("ff_columnListName", R_VIEW);
        clickNavButton("Save");

        log("Check that customize view worked");
        assertTextNotPresent(R_REMCOL);
        assertTextNotPresent(R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        log("Check that R respects column changes, filters and sorts of data");
        pushLocation();
        clickMenuButton("Views", "Create", "R View");
        setFormElement(Locator.id("script"), "labkey.data");
        clickNavButton("Execute Script");
        assertTextNotPresent(R_REMCOL);
        assertTextNotPresent(R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        clickLinkWithText("Source");
        clickNavButton("Save View", 0);
        setFormElement("reportName", R_SCRIPTS[3]);
        clickNavButton("Save");

        popLocation();

        log("Check saved R script");
        clickMenuButton("Views", "default");
        pushLocation();
        //clickNavButton("Reports >>", 0);
        //clickLinkWithText(R_SCRIPTS[0]);
        clickMenuButton("Views", R_SCRIPTS[0]);
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
        clickLinkWithText("Source");
        checkCheckbox("shareReport");
        checkCheckbox("runInBackground");
        clickNavButton("Execute Script");

        log("Check that R script worked");
        // Add once issue 3738 is fixed
//        assertElementNotPresent(Locator.id(R_SCRIPT1_IMG));
//        assertTextNotPresent(R_SCRIPT1_PDF);
        assertTextPresent(R_SCRIPT2_TEXT1);
        clickLinkWithText("Source");
        //click(Locator.raw("//td[contains(text(),'" + R_SCRIPTS[0] + "')]/input"));
        clickNavButton("Save View", 0);
        setFormElement("reportName", R_SCRIPTS[1]);
        clickNavButton("Save");

        log("Check that background run works");
        //clickNavButton("Reports >>", 0);
        //clickLinkWithText(R_SCRIPTS[1]);
        //selectOptionByText("Dataset.viewName", R_SCRIPTS[1]);
        //goToPipelineItem(R_SCRIPTS[1]);
        //assertTextPresent(R_SCRIPT2_TEXT1);

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
        //clickNavButton("Reports >>", 0);
        //assertTextNotPresent(R_SCRIPTS[0]);
        assertElementNotPresent(Locator.raw("//select[@name='Dataset.viewName']//option[.='" + R_SCRIPTS[0] + "']"));

        clickMenuButton("Views", R_SCRIPTS[1]);
        //goToPipelineItem(R_SCRIPTS[1]);
        //assertTextPresent(R_SCRIPT2_TEXT1);
        popLocation();
        // Exception is logged with the server and creates an error at the end
//        log("Try to create an R Report");
//        clickNavButton("Reports >>", 0);
//        clickLinkWithText("Create R Report");
//        assertTextPresent("401");
//        clickNavButton("Home");

        log("Change user permission");
        stopImpersonating();
        clickLinkWithText(getProjectName());
        if (isTextPresent("Enable Admin"))
            clickLinkWithText("Enable Admin");
        enterPermissionsUI();
        setPermissions("Users", "Project Administrator");
        exitPermissionsUI();

        log("Create a new R script which uses others R scripts");
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
        clickLinkWithText("Source");
        clickNavButton("Save View", 0);

        log("Test editing R scripts");
        signOut();
        signIn();
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickReportGridLink(R_SCRIPTS[0], "source");
        if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1))
            if (!RReportHelper.executeScript(this, R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX.toLowerCase()), R_SCRIPT1_TEXT1))
                fail("There was an error running the script");
        clickLinkWithText("Source");
        clickNavButton("Save View");

        log("Check that edit worked");
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickReportGridLink(R_SCRIPTS[1], "source");

        checkCheckbox(Locator.name("includedReports"));
        clickNavButton("Execute Script");
        clickNavButton("Start Job");
        waitForElement(Locator.navButton("Start Job"), 30000);
        assertTextPresent(R_SCRIPT2_TEXT2);
        assertTextNotPresent(R_SCRIPT2_TEXT1);

        log("Clean up R pipeline jobs");
        cleanPipelineItem(R_SCRIPTS[1]);
    }

    protected void deleteRReports()
    {
        log("Clean up R Reports");
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
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
            checkCheckbox(Locator.raw("//td[contains(text(), '" + item + "')]/../td/input"));
            clickNavButton("Delete");
            assertTextNotPresent(item);
        }
    }

    protected void setupDatasetSecurity()
    {
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickLinkWithText("StudyVerifyProject");
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
        clickNavButton("Study Security");

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
        waitForElement(Locator.linkWithText("Manage Views"), WAIT_FOR_JAVASCRIPT);
        clickLinkWithText("Manage Views");

        createReport(GRID_VIEW);
        setFormElement("label", TEST_GRID_VIEW);
        selectOptionByText("datasetSelection", "APX-1: Abbreviated Physical Exam");
        clickNavButton("Create View");

        // test security
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickLinkWithText("StudyVerifyProject");
        clickLinkWithText("My Study");

        clickReportGridLink("participant chart", "permissions");
        selenium.click("useExplicit");
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickNavButton("Save");

        clickReportGridLink(TEST_GRID_VIEW, "permissions");
        selenium.click("useExplicit");
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickNavButton("Save");

        click(Locator.linkWithText("Manage Site"));
        sleep(3000);
        clickLinkWithText("Admin Console");
        impersonate(TEST_USER);
        clickLinkWithText("StudyVerifyProject");
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
}
