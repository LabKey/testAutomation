/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

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
        deleteUsers(afterTest, R_USER, AUTHOR_USER);
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        DataReportsTest initTest = new DataReportsTest();
        initTest.doCleanup(false);

        // fail fast if R is not configured
        initTest._rReportHelper.ensureRConfig();

        // import study and wait; no specimens needed
        initTest.importStudy();
        initTest.waitForPipelineJobsToComplete(1, "study import", false);

        // need this to turn off the demographic bit in the DEM-1 dataset
        initTest.clickFolder(initTest.getFolderName());
        initTest.setDemographicsBit("DEM-1: Demographics", false);

        currentTest = initTest;
    }

    @Test @Ignore
    public void testSteps(){}

    @Override
    protected void doVerifySteps(){}

    @Override
    protected void doCreateSteps(){}

    @Test
    public void doQueryReportTests()
    {
        log("Create a query report.");

        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();

        clickAddReport("Query Report");
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
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewFilter("MouseId", "Mouse Id", "Equals One Of", "999320533;999320541;999320529;999320518");
        _customizeViewsHelper.saveCustomView(QUERY_REPORT_VIEW_NAME_2);

        goToManageViews();

        clickAddReport("Query Report");
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
        waitForElement(Locator.linkWithText(datasetName));
        scrollIntoView(Locator.linkWithText(datasetName)); // WORKAROUND: Chrome weirdness
        clickAndWait(Locator.linkWithText(datasetName));

        _extHelper.clickMenuButton("Views", QUERY_REPORT_NAME_2);

        DataRegionTable table = new DataRegionTable("Dataset", this);

        Map<String, Integer> counts = new HashMap<>();
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
        assertTrue(counts.get(PTIDS_FOR_CUSTOM_VIEW[0]) == 3);
        assertTrue(counts.get(PTIDS_FOR_CUSTOM_VIEW[1]) == 1);
        assertTrue(counts.get(PTIDS_FOR_CUSTOM_VIEW[2]) == 3);
        assertTrue(counts.get(PTIDS_FOR_CUSTOM_VIEW[3]) == 3);
    }

    @Test
    public void doCrosstabViewTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("DEM-1: Demographics"));

        _extHelper.clickMenuButton("Views", "Create", "Crosstab View");
        selectOptionByValue(Locator.name("rowField"), "DEMsex");
        selectOptionByValue(Locator.name("colField"), "DEMsexor");
        selectOptionByValue(Locator.name("statField"), "SequenceNum");
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
    }

    @Test
    public void doGridViewTest()
    {
        // create new grid view report:
        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        String viewName = "DRT Eligibility Query";
        clickAddReport("Grid View");
        setFormElement(Locator.id("label"), viewName);
        selectOptionByText(Locator.name("params"), "ECI-1 (ECI-1: Eligibility Criteria)");
        clickButton("Create View");
        assertElementPresent(Locator.linkWithText("999320016"));
        assertButtonNotPresent("go");
        clickAndWait(Locator.linkWithText(getStudyLabel()));
        clickTab("Manage");
        deleteReport(viewName);
    }

    @Test
    public void doAdvancedViewTest()
    {
        // create new external report
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("DEM-1: Demographics"));
        _extHelper.clickMenuButton("Views", "Create", "Advanced View");
        selectOptionByText(Locator.name("queryName"), "DEM-1 (DEM-1: Demographics)");
        String java = System.getProperty("java.home") + "/bin/java";
        setFormElement(Locator.name("program"), java);
        setFormElement(Locator.name("arguments"), "-cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE} ${REPORT_FILE}");
        clickButton("Submit");
        assertTextPresent("Female");
        setFormElement(Locator.name("program"), java);
        setFormElement(Locator.name("arguments"), "-cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE}");
        selectOptionByValue(Locator.name("fileExtension"), "tsv");
        clickButton("Submit");
        assertTextPresent("Female");
        setFormElement(Locator.name("label"), "tsv");
        selectOptionByText(Locator.name("showWithDataset"), "DEM-1: Demographics");
        clickButton("Save");
        clickAndWait(Locator.linkWithText(getStudyLabel()));
        clickAndWait(Locator.linkWithText("tsv"));
        assertTextPresent("Female");
    }

    @Test
    public void doRReportsTest()
    {
        log("Create an R Report");

        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        _extHelper.clickMenuButton("Views", "Create", "R View");
        setCodeEditorValue("script-report-editor", " ");

        log("Execute bad scripts");
        clickViewTab();
        assertTextPresent("Empty script, a script must be provided.");
        assertTrue("Script didn't execute as expeced", _rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX) + "\nbadString", R_SCRIPT1_TEXT1));

        // horrible hack to get around single versus double quote difference when running R on Linux or Windows systems.
        assertTextPresent("Error: object ", "badString", R_SCRIPT1_TEXT1, R_SCRIPT1_TEXT2, R_SCRIPT1_PDF);
        assertElementPresent(Locator.xpath("//img[starts-with(@id,'" + R_SCRIPT1_IMG + "')]"));

        log("Execute and save a script");
        assertTrue("Script didn't execute as expeced", _rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1));

        log("Check that the script executed properly");
        assertTextPresent(R_SCRIPT1_TEXT1, R_SCRIPT1_TEXT2, R_SCRIPT1_PDF);
        assertElementPresent(Locator.xpath("//img[starts-with(@id,'" + R_SCRIPT1_IMG + "')]"));

        saveReport(R_SCRIPTS[0]);

        log("Create view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewColumn(R_REMCOL);
        _customizeViewsHelper.addCustomizeViewFilter("DEMhisp", "3.Latino\\a or Hispanic?", "Does Not Equal", "Yes");
        _customizeViewsHelper.addCustomizeViewSort(R_SORT, "2.What is your sex?", "Descending");
        _customizeViewsHelper.saveCustomView("Custom Query View");

        log("Check that customize view worked");
        assertTextNotPresent(R_REMCOL);
        assertTextNotPresent(R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        log("Check that R respects column changes, filters and sorts of data");
        pushLocation();
        _extHelper.clickMenuButton("Views", "Create", "R View");
        setCodeEditorValue("script-report-editor", "labkey.data");
        clickViewTab();
        waitForText(R_SORT1);
        assertTextNotPresent(R_REMCOL);
        assertTextNotPresent(R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        saveReport(R_SCRIPTS[3]);

        popLocation();

        log("Check saved R script");
        _extHelper.clickMenuButton("Views", "default");
        pushLocation();
        //clickButton("Reports >>", 0);
        //clickAndWait(Locator.linkWithText(R_SCRIPTS[0]));
        _extHelper.clickMenuButton("Views", R_SCRIPTS[0]);
        waitForText("Console output", WAIT_FOR_PAGE);
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
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        createRReport(AUTHOR_REPORT, R_SCRIPT2(DATA_BASE_PREFIX, "mouseId"), true, true, new String[0]);
        stopImpersonating();
        popLocation();


        log("Create second R script");
        _extHelper.clickMenuButton("Views", "Create", "R View");
        _rReportHelper.ensureFieldSetExpanded("Shared Scripts");
        _ext4Helper.checkCheckbox(R_SCRIPTS[0]);
        assertTrue("Script didn't execute as expeced", _rReportHelper.executeScript(R_SCRIPT2(DATA_BASE_PREFIX, "mouseid"), R_SCRIPT2_TEXT1));
        clickSourceTab();
        _rReportHelper.selectOption(RReportHelper.ReportOption.shareReport);
        _rReportHelper.selectOption(RReportHelper.ReportOption.runInPipeline);
        clickViewTab();

        log("Check that R script worked");
        assertTextPresent(R_SCRIPT2_TEXT1);
        saveReport(R_SCRIPTS[1]);

        log("Check that background run works");

        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup("Users");
        setFormElement("names", R_USER);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setPermissions("Users", "Editor");
        _permissionsHelper.exitPermissionsUI();


        //create R report with dev
        impersonate(R_USER);

        log("Access shared R script");
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        pushLocation();
        assertElementNotPresent(Locator.xpath("//select[@name='Dataset.viewName']//option[.='" + R_SCRIPTS[0] + "']"));
        _extHelper.clickMenuButton("Views", R_SCRIPTS[1]);
        goBack();
        _extHelper.clickMenuButton("Views", AUTHOR_REPORT);

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
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATA_SET));
        _extHelper.clickMenuButton("Views", "Create", "R View");
        _rReportHelper.ensureFieldSetExpanded("Shared Scripts");
        _ext4Helper.checkCheckbox(R_SCRIPTS[0]);
        _ext4Helper.checkCheckbox(R_SCRIPTS[1]);
        assertTrue("Script didn't execute as expeced", _rReportHelper.executeScript(R_SCRIPT3(DATA_BASE_PREFIX, "mouseid"), R_SCRIPT2_TEXT1));
        saveReport(R_SCRIPTS[2]);

        log("Test editing R scripts");
        signOut();
        signIn();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickReportGridLink(R_SCRIPTS[0]);
        assertTrue("Script didn't execute as expeced", _rReportHelper.executeScript(R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1));
        prepForPageLoad();
        resaveReport();
        waitForPageToLoad();

        log("Check that edit worked");
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickReportGridLink(R_SCRIPTS[1]);

        clickViewTab();
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
        prepForPageLoad();
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

        _extHelper.clickMenuButton("Views", "Create", "R View");
        setCodeEditorValue("script-report-editor", scriptValue);

        // if there are any shared scripts, check the check box so they get included when the report is rendered
        if (sharedScripts != null && sharedScripts.length > 0)
        {
            _rReportHelper.ensureFieldSetExpanded("Shared Scripts");
            for (String script : sharedScripts)
                _ext4Helper.checkCheckbox(script);
        }

        if(share)
        {
            _rReportHelper.selectOption(RReportHelper.ReportOption.shareReport);
            if(shareSource)
                _rReportHelper.selectOption(RReportHelper.ReportOption.showSourceTab);
        }
        saveReport(name);

    }

    @LogMethod
    private void saveReport(String name)
    {
        clickSourceTab();
        _rReportHelper.saveReport(name);
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
        _ext4Helper.clickTabContainingText(name);
        sleep(2000); // TODO
    }
}
