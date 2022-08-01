/*
 * Copyright (c) 2013-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.reports.ScriptReportPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Reports.class})
@BaseWebDriverTest.ClassTimeout(minutes = 15)
public class DataReportsTest extends ReportTest
{
    protected final RReportHelper _rReportHelper = new RReportHelper(this);

    protected static final String AUTHOR_REPORT = "Author report";

    private static final String QUERY_REPORT_NAME = BaseWebDriverTest.INJECT_CHARS_1;
    private static final String QUERY_REPORT_DESCRIPTION = BaseWebDriverTest.INJECT_CHARS_1;
    private static final String QUERY_REPORT_SCHEMA_NAME = "study";
    private static final String QUERY_REPORT_QUERY_NAME = "Mouse";

    private static final String QUERY_REPORT_NAME_2 = BaseWebDriverTest.INJECT_CHARS_2;
    private static final String QUERY_REPORT_DESCRIPTION_2 = BaseWebDriverTest.INJECT_CHARS_2;
    private static final String QUERY_REPORT_SCHEMA_NAME_2 = "study";
    private static final String QUERY_REPORT_QUERY_NAME_2 = "AE-1 (AE-1:(VTN) AE Log)";
    private static final String QUERY_REPORT_VIEW_NAME_2 = "Limited PTIDS";
    private static final String[] PTIDS_FOR_CUSTOM_VIEW = {"999320533", "999320541", "999320529", "999320518"};

    private String R_SCRIPT1(String function, String database)
    {
        return "data <- labkey.data\n" +
            "dbirth <- data$" + database + "bdt\n" +
            "dbirth\n" +
            "sex <- as.factor(data$" + database + "sex)\n" +
            "sexor <- as.factor(data$" + database + "sexor)\n" +
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
    private String R_SCRIPT3(String database, String colName)
    {
        return "source(\"" + R_SCRIPTS[1] + ".R\")\n" +
            "x <- func2(labkey.data$" + colName + ", labkey.data$" + database + "sex)\n" +
            "x\n";
    }

    private final static String R_SCRIPT2_TEXT1 = "999320648";

    private final static String DATA_SET = "DEM-1: Demographics";
    private final static String DATA_BASE_PREFIX = "dem";
    private final static String R_SCRIPT1_ORIG_FUNC = "length(x)";
    private final static String R_SCRIPT1_EDIT_FUNC = "length(x) * 2";

    private final static String R_SCRIPT2_TEXT2 = "999320672";

    protected final static String R_USER = "r_editor@report.test";
    protected final static String AUTHOR_USER = "author_user@report.test";

    private final ApiPermissionsHelper _apiPermissionsHelper = new ApiPermissionsHelper(this);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, R_USER, AUTHOR_USER);
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void doSetup()
    {
        DataReportsTest initTest = (DataReportsTest)getCurrentTest();

        // fail fast if R is not configured
        initTest._rReportHelper.ensureRConfig();

        // import study and wait; no specimens needed
        initTest.importStudy();
        initTest.waitForPipelineJobsToComplete(1, "study import", false);

        // need this to turn off the demographic bit in the DEM-1 dataset
        initTest.clickFolder(initTest.getFolderName());
        initTest.setDemographicsBit("DEM-1: Demographics", false);

        // Make sure the Developers group has the Platform Developer role.
        initTest.addDeveloperGroupToPlatformDeveloperRole();
    }

    private void addDeveloperGroupToPlatformDeveloperRole()
    {
        goToHome();

        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);

        apiPermissionsHelper.addMemberToRole("Developers", "Platform Developer", ApiPermissionsHelper.MemberType.siteGroup, "/");
    }

    @Before
    public void preTest()
    {
        navigateToFolder(getProjectName(), getFolderName());
    }

    @Override @Ignore // Mask base StudyTest test method
    public void testSteps(){}

    @Override
    protected void doVerifySteps(){}

    @Override
    protected void doCreateSteps(){}

    @Test
    public void doQueryReportTests()
    {
        goToManageViews().clickAddReport("Query Report");
        waitForElement(Locator.name("viewName"));

        setFormElement(Locator.name("viewName"), QUERY_REPORT_NAME);
        setFormElement(Locator.name("description"), QUERY_REPORT_DESCRIPTION);
        _ext4Helper.selectComboBoxItem("Schema:", QUERY_REPORT_SCHEMA_NAME);
        _ext4Helper.waitForMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _ext4Helper.selectComboBoxItem("Query:", QUERY_REPORT_QUERY_NAME);
        _ext4Helper.waitForMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        clickButton("Save");
        waitForText("Manage Views");
        waitForText(QUERY_REPORT_NAME);

        clickReportGridLink(QUERY_REPORT_NAME);
        assertTextPresent(QUERY_REPORT_NAME);
        waitForElement(Locator.paginationText(26));

        clickFolder(getFolderName());

        clickAndWait(Locator.linkWithText("AE-1:(VTN) AE Log"));
        DataRegionTable dataSetTable = new DataRegionTable("Dataset", getDriver());
        dataSetTable.openCustomizeGrid();
        _customizeViewsHelper.addFilter("MouseId", "Mouse Id", "Equals One Of", String.join(";", PTIDS_FOR_CUSTOM_VIEW));
        _customizeViewsHelper.saveCustomView(QUERY_REPORT_VIEW_NAME_2);

        goToManageViews().clickAddReport("Query Report");
        waitForElement(Locator.name("viewName"));

        setFormElement(Locator.name("viewName"), QUERY_REPORT_NAME_2);
        setFormElement(Locator.name("description"), QUERY_REPORT_DESCRIPTION_2);
        _ext4Helper.selectComboBoxItem("Schema:", QUERY_REPORT_SCHEMA_NAME_2);
        _ext4Helper.waitForMaskToDisappear();
        _ext4Helper.selectComboBoxItem("Query:", QUERY_REPORT_QUERY_NAME_2);
        _ext4Helper.waitForMaskToDisappear();
        _ext4Helper.selectComboBoxItem("View:", QUERY_REPORT_VIEW_NAME_2);

        clickButton("Save");
        waitForText("Manage Views");
        waitForText(QUERY_REPORT_NAME_2);

        String datasetName = "AE-1:(VTN) AE Log";

        clickTab("Clinical and Assay Data");
        WebElement link = waitForElement(Locator.linkWithText(datasetName));
        scrollIntoView(link, true); // WORKAROUND: Chrome weirdness
        clickAndWait(link);

        DataRegionTable.DataRegion(getDriver()).find().goToReport(QUERY_REPORT_NAME_2);

        log("Nested data regions are broken in new UI"); //TODO: Enable following block once fixed
        /*
        DataRegionTable outerDataRegion = DataRegion(getDriver()).withName("Dataset").find();
        DataRegionTable region = DataRegion(getDriver()).find(outerDataRegion);

        Map<String, Integer> counts = new HashMap<>();
        for (String value : region.getColumnDataAsText("MouseId"))
        {
            if (!counts.containsKey(value))
                counts.put(value, 1);
            else
            {
                int count = counts.get(value);
                counts.put(value, count + 1);
            }
        }
        assertEquals("Wrong number of rows for ptid: " + PTIDS_FOR_CUSTOM_VIEW[0], new Integer(3), counts.get(PTIDS_FOR_CUSTOM_VIEW[0]));
        assertEquals("Wrong number of rows for ptid: " + PTIDS_FOR_CUSTOM_VIEW[1], new Integer(1), counts.get(PTIDS_FOR_CUSTOM_VIEW[1]));
        assertEquals("Wrong number of rows for ptid: " + PTIDS_FOR_CUSTOM_VIEW[2], new Integer(3), counts.get(PTIDS_FOR_CUSTOM_VIEW[2]));
        assertEquals("Wrong number of rows for ptid: " + PTIDS_FOR_CUSTOM_VIEW[3], new Integer(3), counts.get(PTIDS_FOR_CUSTOM_VIEW[3]));
        */
    }

    @Test
    public void doCrosstabViewTest()
    {
        String reportName = "TestReport";

        clickAndWait(Locator.linkWithText("DEM-1: Demographics"));

        DataRegionTable.DataRegion(getDriver()).find().goToReport("Create Crosstab Report");
        selectOptionByValue(Locator.name("rowField"), "DEMsex");
        selectOptionByValue(Locator.name("colField"), "DEMsexor");
        selectOptionByText(Locator.name("statField"), "Visit");
        clickButton("Submit");

        String[] row3 = new String[] {"Male", "2", "9", "3", "14"};
        assertTableRowsEqual("report", 3, new String[][] {row3});

        setFormElement(Locator.name("label"), reportName);
        clickButton("Save");

        clickFolder("My Study");
        assertTextPresent(reportName);
        clickAndWait(Locator.linkWithText(reportName));

        assertTableCellTextEquals("report", 2, 0, "Female");

        deleteReport(reportName);
    }

    @Test
    public void doGridViewTest()
    {
        String viewName = "DRT Eligibility Query";

        goToManageViews().clickAddReport("Grid View");
        setFormElement(Locator.id("label"), viewName);
        selectOptionByText(Locator.name("params"), "ECI-1 (ECI-1: Eligibility Criteria)");
        clickButton("Create View");
        assertElementPresent(Locator.linkWithText("999320016"));
        deleteReport(viewName);
    }

    @Test
    public void doAdvancedViewTest()
    {
        clickAndWait(Locator.linkWithText("DEM-1: Demographics"));
        DataRegionTable dataRegion = DataRegionTable.DataRegion(getDriver()).find();
        String create_advanced_report = "Create Advanced Report";

        if (TestProperties.isPrimaryUserAppAdmin())
        {
            BootstrapMenu reportMenu = dataRegion.getReportMenu();
            reportMenu.expand();
            List<String> menuItems = getTexts(reportMenu.findVisibleMenuItems());
            assertThat("App admin shouldn't be able to create an advanced report.", menuItems, not(hasItem(create_advanced_report)));
            assertThat("Sanity check failed. Check menu text for advanced report.", menuItems, hasItem("Create Chart"));
            beginAt(WebTestHelper.buildURL("study-reports", getCurrentContainerPath(), "externalReport"));
            assertEquals("App admin shouldn't be able to create an advanced report.", 403, getResponseCode());
            return; // success
        }

        dataRegion.goToReport(create_advanced_report);

        log("Verify txt report");
        selectOptionByText(Locator.name("queryName"), "DEM-1 (DEM-1: Demographics)");
        String java = System.getProperty("java.home") + "/bin/java";
        setFormElement(Locator.name("program"), java);
        setFormElement(Locator.name("arguments"), "-cp " + new File(TestFileUtils.getTestBuildDir(), "classes/java/uiTest") + " org.labkey.test.util.Echo ${DATA_FILE} ${REPORT_FILE}");
        clickAndWait(Locator.lkButton("Submit"));
        assertElementPresent(Locator.tag("pre").containing("Female"));

        log("Verify tsv report");
        setFormElement(Locator.name("program"), java);
        setFormElement(Locator.name("arguments"), "-cp " + new File(TestFileUtils.getTestBuildDir(), "classes/java/uiTest") + " org.labkey.test.util.Echo ${DATA_FILE}");
        selectOptionByValue(Locator.name("fileExtension"), "tsv");
        clickAndWait(Locator.lkButton("Submit"));
        assertElementPresent(Locator.tag("td").withClass("labkey-header").containing("DEMsex"));
        assertElementPresent(Locator.tag("td").containing("Female"));

        log("Verify saved tsv report");
        setFormElement(Locator.name("label"), "tsv");
        selectOptionByText(Locator.name("showWithDataset"), "DEM-1: Demographics");
        clickAndWait(Locator.lkButton("Save"));
        clickAndWait(Locator.linkWithText(getFolderName()));
        clickAndWait(Locator.linkWithText("tsv"));
        assertElementPresent(Locator.tag("td").withClass("labkey-header").containing("DEMsex"));
        assertElementPresent(Locator.tag("td").containing("Female"));
    }

    @Test
    public void doRReportsTest()
    {
        clickAndWait(Locator.linkWithText(DATA_SET));
        pushLocation();

        DataRegionTable.DataRegion(getDriver()).find().goToReport("Create R Report");
        setCodeEditorValue("script-report-editor", " ");

        log("Execute bad scripts");
        _rReportHelper.clickReportTab();
        assertTextPresent("Empty script, a script must be provided.");
        assertTrue("Script didn't execute as expected", _rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX) + "\nbadString", R_SCRIPT1_TEXT1));

        // horrible hack to get around single versus double quote difference when running R on Linux or Windows systems.
        assertTextPresent("Error: object ", "badString", R_SCRIPT1_TEXT1, R_SCRIPT1_TEXT2, R_SCRIPT1_PDF);
        assertElementPresent(Locator.xpath("//img[starts-with(@id,'" + R_SCRIPT1_IMG + "')]"));

        log("Execute and save a script");
        assertTrue("Script didn't execute as expected", _rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1));

        log("Check that the script executed properly");
        assertTextPresent(R_SCRIPT1_TEXT1, R_SCRIPT1_TEXT2, R_SCRIPT1_PDF);
        assertElementPresent(Locator.xpath("//img[starts-with(@id,'" + R_SCRIPT1_IMG + "')]"));

        saveReport(R_SCRIPTS[0]);
        verifyReportPdfDownload("study", 4500d);
        popLocation();

        log("Create view");
        DataRegionTable dataSetTable = new DataRegionTable("Dataset", getDriver());
        dataSetTable.openCustomizeGrid();
        _customizeViewsHelper.removeColumn(R_REMCOL);
        _customizeViewsHelper.addFilter("DEMhisp", "3.Latino\\a or Hispanic?", "Does Not Equal", "Yes");
        _customizeViewsHelper.addSort(R_SORT, "2.What is your sex?", SortDirection.DESC);
        _customizeViewsHelper.saveCustomView("Custom Query View");

        log("Check that customize view worked");
        assertTextNotPresent(R_REMCOL, R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        log("Check that R respects column changes, filters and sorts of data");
        pushLocation();
        DataRegionTable.DataRegion(getDriver()).find().goToReport("Create R Report");
        setCodeEditorValue("script-report-editor", "labkey.data");
        _rReportHelper.clickReportTab();
        waitForText(R_SORT1);
        assertTextNotPresent(R_REMCOL, R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        saveReport(R_SCRIPTS[3]);
        popLocation();

        log("Check saved R script");
        DataRegionTable.DataRegion(getDriver()).find().goToView("Default");
        pushLocation();
        DataRegionTable.DataRegion(getDriver()).find().goToReport(R_SCRIPTS[0]);
        waitForText(WAIT_FOR_PAGE, "Console output");
        assertTextPresent("null device", R_SCRIPT1_TEXT1, R_SCRIPT1_TEXT2, R_SCRIPT1_PDF);
        assertElementPresent(Locator.xpath("//img[starts-with(@id,'" + R_SCRIPT1_IMG + "')]"));
        assertTextNotPresent("Error executing command");
        verifyReportPdfDownload("study", 4500d);
        popLocation();
        pushLocation();

        if (!TestProperties.isPrimaryUserAppAdmin())
        {
            log("Test user permissions");
            createSiteDeveloper(AUTHOR_USER).addMemberToRole(AUTHOR_USER, "Author", PermissionsHelper.MemberType.user, getProjectName());
            impersonate(AUTHOR_USER);
        }
        else
        {
            log("App Admin can't impersonate site roles. Just create report as primary test user.");
        }
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        createRReport(AUTHOR_REPORT, R_SCRIPT2(DATA_BASE_PREFIX, "mouseId"), true, true, new String[0]);
        if (!TestProperties.isPrimaryUserAppAdmin())
        {
            stopImpersonating();
        }

        popLocation();
        log("Create second R script");
        DataRegionTable.DataRegion(getDriver()).find().goToReport("Create R Report");
        _rReportHelper.ensureFieldSetExpanded("Shared Scripts");
        _ext4Helper.checkCheckbox(R_SCRIPTS[0]);
        assertTrue("Script didn't execute as expected", _rReportHelper.executeScript(R_SCRIPT2(DATA_BASE_PREFIX, "mouseid"), R_SCRIPT2_TEXT1));
        _rReportHelper.clickSourceTab();
        _rReportHelper.selectOption(ScriptReportPage.StandardReportOption.shareReport);
        _rReportHelper.selectOption(ScriptReportPage.StandardReportOption.runInPipeline);
        saveReport(R_SCRIPTS[1]);

        log("Check that R script worked");
        _rReportHelper.clickReportTab();
        assertElementPresent(Locator.lkButton("Start Job"));

        log("Check that background run works");

        _userHelper.createUser(R_USER);
        _apiPermissionsHelper.addUserToProjGroup(R_USER, getProjectName(), "Users");
        _apiPermissionsHelper.addMemberToRole("Users", "Editor", PermissionsHelper.MemberType.group, getProjectName());

        //create R report with dev
        impersonate(R_USER);

        log("Access shared R script");
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        pushLocation();
        assertElementNotPresent(Locator.xpath("//select[@name='Dataset.viewName']//option[.='" + R_SCRIPTS[0] + "']"));
        DataRegionTable.DataRegion(getDriver()).find().goToReport(R_SCRIPTS[1]);
        goBack();
        DataRegionTable.DataRegion(getDriver()).find().goToReport(AUTHOR_REPORT);

        popLocation();
        stopImpersonating();

        log("Change user permission");
        _apiPermissionsHelper.addMemberToRole("Users", "Project Administrator", PermissionsHelper.MemberType.group, getProjectName());

        log("Create a new R script that uses other R scripts");
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        DataRegionTable.DataRegion(getDriver()).find().goToReport("Create R Report");
        _rReportHelper.ensureFieldSetExpanded("Shared Scripts");
        _ext4Helper.checkCheckbox(R_SCRIPTS[0]);
        _ext4Helper.checkCheckbox(R_SCRIPTS[1]);
        assertTrue("Script didn't execute as expeced", _rReportHelper.executeScript(R_SCRIPT3(DATA_BASE_PREFIX, "mouseid"), R_SCRIPT2_TEXT1));
        saveReport(R_SCRIPTS[2]);

        log("Test editing R scripts");
        signIn(); // Reset session to make sure R report isn't cached
        navigateToFolder(getProjectName(), getFolderName());
        clickReportGridLink(R_SCRIPTS[0]);
        assertTrue("Script didn't execute as expeced", _rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1));
        resaveReport();

        log("Check that edit worked");
        navigateToFolder(getProjectName(), getFolderName());
        clickReportGridLink(R_SCRIPTS[1]);
        waitAndClick(Locator.lkButton("Start Job"));

        WebElement pipelineLink = waitForElement(Locator.linkWithText("click here"));
        waitForElement(Locator.byClass("x4-window").containing("Start Pipeline Job").hidden());
        clickAndWait(pipelineLink);
        waitForPipelineJobsToComplete(2, false);
        // go back to the report and confirm it is visible
        clickReportGridLink(R_SCRIPTS[1]);
        waitForElement(Locator.tagWithName("img", "resultImage"));
        assertTextPresent(R_SCRIPT2_TEXT2);
        assertTextNotPresent(R_SCRIPT2_TEXT1);

        // TODO: Issue 36040: Unable to download PDF for R report run as pipeline job
        // verifyReportPdfDownload("study", 4500d);
    }

    private void verifyReportPdfDownload(String schema, double expectedSize)
    {
        File reportPdf = clickAndWaitForDownload(waitForElement(Locator.linkWithText(R_SCRIPT1_PDF)));
        assertTrue("Report PDF has wrong name: " + reportPdf.getName(),
                reportPdf.getName().startsWith(schema) && reportPdf.getName().endsWith(".pdf"));
        assertEquals("Report PDF is the wrong size", expectedSize, reportPdf.length(), expectedSize / 10);
    }

    @Test
    public void testRReportShowSource()
    {
        final String reportName = "Source Tab Test";
        final String R_SCRIPT = "#${tsvout:tsvfile}\nwrite.table(labkey.data, file = \"tsvfile\", sep = \"\\t\", qmethod = \"double\", col.names=NA)";
        final String DATA_SET_APX1 = "APX-1: Abbreviated Physical Exam";

        scrollIntoView(Locator.linkWithText(DATA_SET_APX1));
        clickAndWait(Locator.linkWithText(DATA_SET_APX1));

        log("Test showing the source tab to all users");
        createRReport(reportName, R_SCRIPT, true, true, new String[0]);

        impersonateRole("Reader");
        clickFolder(getFolderName());
        scrollIntoView(Locator.linkWithText(DATA_SET_APX1));
        clickAndWait(Locator.linkWithText(DATA_SET_APX1));
        DataRegionTable.DataRegion(getDriver()).find().goToReport( reportName);
        waitForText(WAIT_FOR_PAGE, "Console output");
        assertElementVisible(Ext4Helper.Locators.tab("Source"));
        stopImpersonating();


        log("Re-save report disabling showing the source tab to all users");
        navigateToFolder(getProjectName(), getFolderName());
        scrollIntoView(Locator.linkWithText(DATA_SET_APX1));
        clickAndWait(Locator.linkWithText(DATA_SET_APX1));
        DataRegionTable.DataRegion(getDriver()).find().goToReport( reportName);
        waitForText(WAIT_FOR_PAGE, "Console output");
        _rReportHelper.clickSourceTab();
        _rReportHelper.clearOption(ScriptReportPage.StandardReportOption.showSourceTab);
        resaveReport();

        impersonateRole("Reader");

        navigateToFolder(getProjectName(), getFolderName());
        scrollIntoView(Locator.linkWithText(DATA_SET_APX1));
        clickAndWait(Locator.linkWithText(DATA_SET_APX1));
        DataRegionTable.DataRegion(getDriver()).find().goToReport( reportName);
        waitForText(WAIT_FOR_PAGE, "Console output");
        assertElementNotVisible(Ext4Helper.Locators.tab("Source"));

        stopImpersonating();

        goToProjectHome();
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
        DataRegionTable.DataRegion(getDriver()).find().goToReport("Create R Report");
        setCodeEditorValue("script-report-editor", scriptValue);

        // if there are any shared scripts, check the check box so they get included when the report is rendered
        if (sharedScripts != null && sharedScripts.length > 0)
        {
            _rReportHelper.ensureFieldSetExpanded("Shared Scripts");
            for (String script : sharedScripts)
                _ext4Helper.checkCheckbox(script);
        }

        if (share)
        {
            _rReportHelper.selectOption(ScriptReportPage.StandardReportOption.shareReport);
            if (shareSource)
                _rReportHelper.selectOption(ScriptReportPage.StandardReportOption.showSourceTab);
        }

        saveReport(name);
    }

    @LogMethod
    private void saveReport(String name)
    {
        _rReportHelper.clickSourceTab();
        waitForElement(Locator.tagWithText("span","Save"));
        _rReportHelper.saveReport(name);
    }

    private void resaveReport()
    {
        doAndWaitForPageToLoad(() -> saveReport(null));
        _ext4Helper.waitForMaskToDisappear();
    }
}
