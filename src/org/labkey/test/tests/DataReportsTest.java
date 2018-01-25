/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.junit.Assert.assertTrue;

@Category({BVT.class, Reports.class})
public class DataReportsTest extends ReportTest
{
    protected final RReportHelper _rReportHelper = new RReportHelper(this);

    protected static final String AUTHOR_REPORT = "Author report";

    private static final String QUERY_REPORT_NAME = "First Test Query Report";
    private static final String QUERY_REPORT_DESCRIPTION = "Description for the first query report.";
    private static final String QUERY_REPORT_SCHEMA_NAME = "study";
    private static final String QUERY_REPORT_QUERY_NAME = "Mouse";

    private static final String QUERY_REPORT_NAME_2 = "Second Test Query Report";
    private static final String QUERY_REPORT_DESCRIPTION_2 = "Description for the first query report.";
    private static final String QUERY_REPORT_SCHEMA_NAME_2 = "study";
    private static final String QUERY_REPORT_QUERY_NAME_2 = "AE-1 (AE-1:(VTN) AE Log)";
    private static final String QUERY_REPORT_VIEW_NAME_2 = "Limited PTIDS";
    private static final String[] PTIDS_FOR_CUSTOM_VIEW = {"999320533", "999320541", "999320529", "999320518"};

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
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        clickFolder(getFolderName());
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
        DataRegionTable.DataRegion(getDriver()).find().goToReport( "Create Advanced Report");

        log("Verify txt report");
        selectOptionByText(Locator.name("queryName"), "DEM-1 (DEM-1: Demographics)");
        String java = System.getProperty("java.home") + "/bin/java";
        setFormElement(Locator.name("program"), java);
        setFormElement(Locator.name("arguments"), "-cp " + new File(TestFileUtils.getTestBuildDir(), "classes") + " org.labkey.test.util.Echo ${DATA_FILE} ${REPORT_FILE}");
        submit(Locator.tagWithName("form", "reportDesigner"));
        assertElementPresent(Locator.tag("pre").containing("Female"));

        log("Verify tsv report");
        setFormElement(Locator.name("program"), java);
        setFormElement(Locator.name("arguments"), "-cp " + new File(TestFileUtils.getTestBuildDir(), "classes") + " org.labkey.test.util.Echo ${DATA_FILE}");
        selectOptionByValue(Locator.name("fileExtension"), "tsv");
        submit(Locator.tagWithName("form", "reportDesigner"));
        assertElementPresent(Locator.tag("td").withClass("labkey-header").containing("DEMsex"));
        assertElementPresent(Locator.tag("td").containing("Female"));

        log("Verify saved tsv report");
        setFormElement(Locator.name("label"), "tsv");
        selectOptionByText(Locator.name("showWithDataset"), "DEM-1: Demographics");
        submit(Locator.tagWithName("form", "saveReport"));
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
        clickReportTab();
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
        clickReportTab();
        waitForText(R_SORT1);
        assertTextNotPresent(R_REMCOL, R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        saveReport(R_SCRIPTS[3]);
        popLocation();

        log("Check saved R script");
        DataRegionTable.DataRegion(getDriver()).find().goToView("default");
        pushLocation();
        DataRegionTable.DataRegion(getDriver()).find().goToReport(R_SCRIPTS[0]);
        waitForText(WAIT_FOR_PAGE, "Console output");
        assertTextPresent("null device", R_SCRIPT1_TEXT1, R_SCRIPT1_TEXT2, R_SCRIPT1_PDF);
        assertElementPresent(Locator.xpath("//img[starts-with(@id,'" + R_SCRIPT1_IMG + "')]"));
        assertTextNotPresent("Error executing command");
        popLocation();

        log("Test user permissions");
        pushLocation();
        createSiteDeveloper(AUTHOR_USER);
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(AUTHOR_USER, "Author");
        impersonate(AUTHOR_USER);
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        createRReport(AUTHOR_REPORT, R_SCRIPT2(DATA_BASE_PREFIX, "mouseId"), true, true, new String[0]);
        stopImpersonating();
        popLocation();

        log("Create second R script");
        DataRegionTable.DataRegion(getDriver()).find().goToReport("Create R Report");
        _rReportHelper.ensureFieldSetExpanded("Shared Scripts");
        _ext4Helper.checkCheckbox(R_SCRIPTS[0]);
        assertTrue("Script didn't execute as expected", _rReportHelper.executeScript(R_SCRIPT2(DATA_BASE_PREFIX, "mouseid"), R_SCRIPT2_TEXT1));
        clickSourceTab();
        _rReportHelper.selectOption(RReportHelper.ReportOption.shareReport);
        _rReportHelper.selectOption(RReportHelper.ReportOption.runInPipeline);
        clickReportTab();

        log("Check that R script worked");
        assertTextPresent(R_SCRIPT2_TEXT1);
        saveReport(R_SCRIPTS[1]);

        log("Check that background run works");

        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup("Users");
        setFormElement(Locator.name("names"), R_USER);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setPermissions("Users", "Editor");
        _permissionsHelper.exitPermissionsUI();


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
        log("Change user permission");
        stopImpersonating();
        clickProject(getProjectName());
        if (isTextPresent("Enable Admin"))
            clickAndWait(Locator.linkWithText("Enable Admin"));
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setPermissions("Users", "Project Administrator");
        _permissionsHelper.exitPermissionsUI();

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
        signOut();
        signIn();
        navigateToFolder(getProjectName(), getFolderName());
        clickReportGridLink(R_SCRIPTS[0]);
        assertTrue("Script didn't execute as expeced", _rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1));
        resaveReport();

        log("Check that edit worked");
        navigateToFolder(getProjectName(), getFolderName());
        clickReportGridLink(R_SCRIPTS[1]);

        clickReportTab();
        waitForElement(Locator.lkButton("Start Job"), WAIT_FOR_JAVASCRIPT);
        clickButton("Start Job", 0);
        waitForElementToDisappear(Ext4Helper.Locators.window("Start Pipeline Job"));
        goToModule("Pipeline");
        waitForPipelineJobsToFinish(2);
        // go back to the report and confirm it is visible
        clickReportGridLink(R_SCRIPTS[1]);
        waitForText(R_SCRIPT2_TEXT2);
        assertTextPresent(R_SCRIPT2_TEXT2);
        assertTextNotPresent(R_SCRIPT2_TEXT1);
        resaveReport();

        log("Clean up R pipeline jobs");
        cleanPipelineItem(R_SCRIPTS[1]);
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
        goToProjectHome();
        clickFolder(getFolderName());
        scrollIntoView(Locator.linkWithText(DATA_SET_APX1));
        clickAndWait(Locator.linkWithText(DATA_SET_APX1));
        DataRegionTable.DataRegion(getDriver()).find().goToReport( reportName);
        waitForText(WAIT_FOR_PAGE, "Console output");
        clickSourceTab();
        _rReportHelper.clearOption(RReportHelper.ReportOption.showSourceTab);
        resaveReport();

        impersonateRole("Reader");
        clickFolder(getFolderName());
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
            _rReportHelper.selectOption(RReportHelper.ReportOption.shareReport);
            if (shareSource)
                _rReportHelper.selectOption(RReportHelper.ReportOption.showSourceTab);
        }

        saveReport(name);
    }

    @LogMethod
    private void saveReport(String name)
    {
        clickSourceTab();
        waitForElement(Locator.tagWithText("span","Save"));
        _rReportHelper.saveReport(name);
    }

    private void resaveReport()
    {
        doAndWaitForPageToLoad(() -> saveReport(null));
        _ext4Helper.waitForMaskToDisappear();
    }

    private void clickViewTab()
    {
        clickDesignerTab("View");
    }

    private void clickReportTab()
    {
        clickDesignerTab("Report");
    }

    private void clickSourceTab()
    {
        clickDesignerTab("Source");
    }

    private void clickDesignerTab(String name)
    {
        waitAndClick(Ext4Helper.Locators.tab(name));
        sleep(2000); // TODO
    }
}
