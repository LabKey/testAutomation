/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * User: Rylan
 * Date: 3/26/13
 * Time: 11:32 AM
 */
@Category({DailyB.class})
public class ETLTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ETLTestProject";
    private boolean _debugTest = false;

    //
    // expected results for transform UI testing
    //
    private static final String TRANSFORM_APPEND = "{simpletest}/append";
    private static final String TRANSFORM_APPEND_DESC = "Append Test";
    private static final String TRANSFORM_TRUNCATE = "{simpletest}/truncate";
    private static final String TRANSFORM_TRUNCATE_DESC = "Truncate Test";
    private static final String TRANSFORM_BYRUNID = "{simpletest}/appendIdByRun";
    private static final String TRANSFORM_BYRUNID_DESC = "ByRunId";


    private static final String[][] TRANSFORM_SUMMARY_EMPTY = {};
    private static final String[][] TRANSFORM_SUMMARY1 = {
        {TRANSFORM_APPEND, "1", null, "COMPLETE", "2", null}
    };
    private static final String[][] TRANSFORM_SUMMARY2 = {
            {TRANSFORM_APPEND, "1", null, "COMPLETE", "2", null},
            {TRANSFORM_TRUNCATE, "1", null, "COMPLETE", "2", null}
    };
    private static final String[][] TRANSFORM_SUMMARY3 = {
            {TRANSFORM_APPEND, "1", null, "ERROR", null, null},
            {TRANSFORM_BYRUNID, "1", null, "COMPLETE", "2", null},
            {TRANSFORM_TRUNCATE, "1", null, "COMPLETE", "2", null},
    };
    private static final String[][] TRANSFORM_HISTORY1 = {
            {"1", null, "COMPLETE", "2", null}
    };
    private static final String[][] TRANSFORM_HISTORY2 = {
            {"1", null, "COMPLETE", "2", null},
            {"1", null, "COMPLETE", "2", null}
    };
    private static final String[][] TRANSFORM_HISTORY3 = {
            {"1", null, "ERROR", null, null},
            {"1", null, "COMPLETE", "2", null},
            {"1", null, "COMPLETE", "2", null}
    };

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        runInitialSetup();

        // verify we don't show any rows for this newly created
        // project (test container filter)
        verifyTransformSummary(TRANSFORM_SUMMARY_EMPTY);

        //append into empty target
        insertSourceRow("0", "Subject 0", null);
        runETL("append");
        assertInTarget1("Subject 0");
        checkRun(1);

        verifyTransformSummary(TRANSFORM_SUMMARY1);
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC, TRANSFORM_HISTORY1);

        //append into populated target
        insertSourceRow("1", "Subject 1", null);
        runETL("append");
        checkRun(2);
        assertInTarget1("Subject 0", "Subject 1");

        // verify transform summary should only have the most recent entry
        verifyTransformSummary(TRANSFORM_SUMMARY1);
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC, TRANSFORM_HISTORY2);

        // rerun append and verify that no work is done
        runETL_NoWork("append");

        // verify only two pipeline jobs existed since the "no work" one should not
        // have fired off a pipeline job
        checkRun(2);
        // verify transform summary should only have the most recent entry since
        // it should filter out "no work" rows
        verifyTransformSummary(TRANSFORM_SUMMARY1);
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC, TRANSFORM_HISTORY2);


/*
UNDONE: need to fix the merge case

        // merge into populated target, note that "Subject 2" was inserted above to test ERROR case
        // for UI

        insertSourceRow("2", "Subject 2", null);
        runETL("merge");
        assertInTarget1("Subject 0", "Subject 1", "Subject 2");

        remove insert below when merge case is fixed
*/
        insertSourceRow("2", "Subject 2", null);
        //truncate into populated target
        deleteSourceRow("0", "1");
        runETL("truncate");
        assertInTarget1("Subject 2");
        assertNotInTarget1("Subject 0", "Subject 1");
        verifyTransformSummary(TRANSFORM_SUMMARY2);
        // verify 1 history row
        verifyTransformHistory(TRANSFORM_TRUNCATE, TRANSFORM_TRUNCATE_DESC, TRANSFORM_HISTORY1);

        //identify by run into populated target
        insertSourceRow("3", "Subject 3", "42");
        insertTransferRow("42", getDate(), getDate(), "new transfer", "added by test automation", "pending");
        runETL("appendIdByRun");
        assertInTarget1("Subject 2", "Subject 3");

        // intentionally fail transform by running append again after
        // the records were modified above
        runETL("append");
        checkRun(5, true /*expectError*/);
        verifyTransformSummary(TRANSFORM_SUMMARY3);
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC, TRANSFORM_HISTORY3);

        //error logging test, casting error, note that this causes an error in the checker
        // before a pipeline job is even scheduled
        runETL_CheckerError("badCast");
        assertInLog("contains value not castable to a date:");

        //error logging test, bad run table name
        runETL_CheckerError("badTableName");
        assertInLog("Table not found:");

        // be sure to check for all expected errors here so that the test won't fail on exit
        // 1) duplicate key error
        // looks like postgres inserts an "ERROR" word in their error string for the duplicate key
        // but mssql doesn't, hack around that here
        checkExpectedErrors(WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL ? 2 : 1);
    }

    //
    // verify the following:
    // contains expected number of rows (container filter and "no work" filter is there)
    // contains expected number of columns
    // links to file logs work in history and summary UIs
    // links can be traversed from summary -> history -> detail for the given transform id
    //
    private void verifyLogFileLink(String status)
    {
        click(Locator.linkContainingText(status));
        waitForElement(Locator.tag("span").withClass("x4-window-header-text").containing(".etl.log"));
        waitAndClick(Locator.ext4ButtonContainingText("Close"));
    }

    private void verifyTransformSummary(String[][] data)
    {
        gotoQueryWebPart("TransformSummary");
        TransformSummaryVerifier verifier = new TransformSummaryVerifier(this, data);
        verifier.verifyResults();
    }

    private void waitForTransformPage(String linkText, String title)
    {
        waitForTransformPage(linkText, title, "COMPLETE");
    }

    private void waitForTransformPage(String linkText, String title, String status)
    {
        click(Locator.linkContainingText(linkText));
        // verify title
        waitForText(title);
        // wait for data in data region to appear
        waitForText(status);
    }

    private void verifyTransformHistory(String transformId, String transformDesc, String [][] data)
    {
        waitForTransformPage(transformId, "Transform History - " + transformDesc);
        TransformHistoryVerifier verifier = new TransformHistoryVerifier(this, transformId, transformDesc, data);
        verifier.verifyResults();
    }

    protected void runInitialSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        log("running setup");
        _containerHelper.createProject(PROJECT_NAME, null);
        enableModule("DataIntegration", true);
        enableModule("simpletest", true);
        portalHelper.addQueryWebPart("Source", "vehicle", "etl_source", null);
        portalHelper.addQueryWebPart("Target1", "vehicle", "etl_target", null);
        //portalHelper.addQueryWebPart("Target2", "vehicle", "etl_target2", null);
        portalHelper.addQueryWebPart("Transfers", "vehicle", "transfer", null);

        // UNDONE: remove when we finalize casing of table names versus views across pg and mssql
        String transformRun =  (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL) ?
                "transformrun" : "TransformRun";

        portalHelper.addQueryWebPart("TransformRun", "dataintegration", transformRun, null);
        portalHelper.addQueryWebPart("TransformHistory", "dataintegration", "TransformHistory", null);
        portalHelper.addQueryWebPart("TransformSummary", "dataintegration", "TransformSummary", null);
    }

    private void insertSourceRow(String id, String name, String RunId)
    {
        log("inserting source row " + name);
        //goToProjectHome();
        clickTab("Portal");
        click(Locator.xpath("//span[text()='Source']"));
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForElement(Locator.name("quf_id"));
        setFormElement(Locator.name("quf_id"), id);
        setFormElement(Locator.name("quf_name"), name);
        if (null != RunId)
        {
            setFormElement(Locator.name("quf_transformrun"), RunId);
        }
        clickButton("Submit");
        log("returning to project home");
        //goToProjectHome();
        clickTab("Portal");
    }

    private void insertTransferRow(String rowId, String transferStart, String transferComplete,  String description, String log, String status)
    {
        log("inserting transfer row rowid " + rowId);
        goToProjectHome();
        click(Locator.xpath("//span[text()='Transfers']"));
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForElement(Locator.name("quf_rowid"));
        setFormElement(Locator.name("quf_rowid"), rowId);
        setFormElement(Locator.name("quf_transferstart"), transferStart);
        setFormElement(Locator.name("quf_transfercomplete"), transferComplete);
        setFormElement(Locator.name("quf_description"), description);
        setFormElement(Locator.name("quf_log"), log);
        setFormElement(Locator.name("quf_status"), status);
        clickButton("Submit");
        log("returning to project home");
        //goToProjectHome();
        clickTab("Portal");
    }

    private void runETL(String transformId)
    {
        _runETL(transformId, true, false);
    }

    private void runETL_CheckerError(String transformId)
    {
        _runETL(transformId, true, true);
    }

    private void runETL_NoWork(String transformId)
    {
        _runETL(transformId, false, false);
    }

    private void _runETL(String transformId, boolean hasWork, boolean hasCheckerError)
    {
        log("running " + transformId + " job");
        goToModule("DataIntegration");

        if (hasWork && !hasCheckerError)
        {
            // pipeline job will run
            prepForPageLoad();
            waitAndClick(Locator.xpath("//tr[contains(@transformid,'" + transformId + "')]/td/a"));
            newWaitForPageToLoad();
        }
        else
        {
            // pipeline job does not run
            waitAndClick(Locator.xpath("//tr[contains(@transformid,'" + transformId + "')]/td/a"));
            _ext4Helper.clickWindowButton(hasCheckerError ? "Error" : "Success", "OK", 0, 0);
        }

        log("returning to project home");
        goToProjectHome();
    }

    private void deleteSourceRow(String... ids)
    {
        goToProjectHome();
        clickAndWait(Locator.xpath("//span[text()='Source']"));
        for(String id : ids)
        {
            log("deleting source row id " + id);
            click(Locator.xpath("//a[text()='"+id+"']/../../td/input[@type='checkbox']"));
        }
        prepForPageLoad();
        click(Locator.xpath("//span[text()='Delete']"));
        // eat the alert without spewing to the log file
        getAlert();
        newWaitForPageToLoad();
        log("returning to project home");
        //goToProjectHome();
        clickTab("Portal");
    }

    private void assertInTarget1(String... targets)
    {
        assertQueryWebPart("etl_target", "Target1", true, targets);
    }

    private void gotoQueryWebPart(String webpartName)
    {
        gotoQueryWebPart(webpartName, webpartName);
    }

    private void gotoQueryWebPart(String webpartName, String queryName)
    {
        clickTab("Portal");
        click(Locator.xpath("//span[text()='" + queryName + "']"));
        waitForText(webpartName);
    }

    private void assertQueryWebPart(String webpartName, String queryName, boolean assertTextPresent, String ... targets)
    {
        gotoQueryWebPart(webpartName, queryName);
        for(String target : targets)
        {
            if (assertTextPresent)
                assertTextPresent(target);
            else
                assertTextNotPresent(target);
        }
    }

    private void assertNotInTarget1(String... targets)
    {
        assertQueryWebPart("etl_target", "Target1", false, targets);
    }

    private void assertInLog(String... targets)
    {
        assertQueryWebPart("TransformRun", "TransformRun", true, targets);
    }

    protected void checkRun(int amount)
    {
        checkRun(amount, false);
    }

    protected void checkRun(int amount, boolean expectError)
    {
        //goToProjectHome();
        clickTab("Portal");
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(amount, "ETL Job", expectError);
    }

    private String getDate()
    {
        Calendar calendar = new GregorianCalendar();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return format.format(calendar.getTime());
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @Override
    public void checkQueries()
    {
        log("Skipping query check. Some tables used by queries in simpletest module are not created in this test");
        log("Query check from " + SimpleModuleTest.class.getSimpleName() + " should cover anything this would check");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/dataintegration";
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.FIREFOX;
    }

    class BaseTransformVerifier
    {
        // table of results to verify
        protected String[] _columns;
        protected String [][] _data;
        protected ETLTest _test;

        BaseTransformVerifier(ETLTest test, String[] columns, String [][] data)
        {
            _test = test;
            _columns = columns;
            _data = data;
        }

        protected String getDataRegionName()
        {
            return "query";
        }

        //
        // use column names to verify data
        //
        void verifyResults()
        {
            DataRegionTable drt = new DataRegionTable(getDataRegionName(), _test, false /*selectors*/);
            assert(_columns.length == drt.getColumnCount());
            assert(_data.length == drt.getDataRowCount());

            for (int row = 0; row < _data.length; row ++)
            {
                for (int col = 0; col < _columns.length; col++)
                {
                    //
                    // compare data order now using the expected column names
                    // note that this is a bit brittle as the data must be in the
                    // order of the columns
                    //
                    // if the "expected" data is null, then just verify that
                    // the actual data is non-null.  An expected value of "null"
                    // just means that it is note easily comparable (execution times, for example)
                    //
                    String actual = drt.getDataAsText(row, _columns[col]);
                    String expected = _data[row][col];
                    if (null != expected)
                        assert(actual.equalsIgnoreCase(expected));
                }
            }
        }
    }

    class TransformSummaryVerifier extends BaseTransformVerifier
    {
        TransformSummaryVerifier(ETLTest test, String [][] data)
        {
            super(test, new String[]{
                    "Name",
                    "Version",
                    "Last Run",
                    "Last Status",
                    "Records Processed",
                    "Execution Time"}, data);
        }

        @Override
        protected void verifyResults()
        {
            super.verifyResults();

            DataRegionTable drt = new DataRegionTable(getDataRegionName(), _test, false /*selectors*/);

            // just spot check the file log
            for (int row = 0; row < _data.length; row ++)
            {
                String status = drt.getDataAsText(row, "Last Status");
                verifyLogFileLink(status);
                break;
            }
        }
    }

    class TransformHistoryVerifier extends BaseTransformVerifier
    {
        protected String _transformId;
        protected String _transformDesc;

        TransformHistoryVerifier(ETLTest test, String transformId, String transformDesc, String [][] data)
        {
            super(test, new String[]{
                    "Version",
                    "Run",
                    "Status",
                    "Records Processed",
                    "Execution Time"}, data);

            _transformId = transformId;
            _transformDesc = transformDesc;
        }

        @Override
        protected String getDataRegionName()
        {
            return getAttribute(Locator.xpath("//*[starts-with(@id, 'aqwp')]"), "id");
        }

        @Override
        protected void verifyResults()
        {
            super.verifyResults();

            DataRegionTable drt = new DataRegionTable(getDataRegionName(), _test, false /*selectors*/);

            // walk through all the history rows and verify the link to the file log works (just the first one)
            // and the link to the details page works
            for (int row = 0; row < _data.length; row ++)
            {
                String status = drt.getDataAsText(row, "Status");
                if (0 == row)
                {
                    verifyLogFileLink(status);
                }
                String run = drt.getDataAsText(row, "Run");
                verifyTransformDetails(run, status);
                goBack();
                waitForText(run);
            }
        }

        protected void verifyTransformDetails(String run, String status)
        {
            waitForTransformPage(run, "Transform Details - " + _transformDesc, status);
            TransformDetailsVerifier verifier = new TransformDetailsVerifier(_test, _transformId);
            verifier.verifyResults();
        }

    }

    // currently this has the same schema as the TransformRuns table
    class TransformDetailsVerifier extends BaseTransformVerifier
    {
        protected String _transformId;
        TransformDetailsVerifier(ETLTest test, String transformId)
        {

            super(test, new String[]{
                    "Transform Run Id",
                    "Container",
                    "Record Count",
                    "Transform Id",
                    "Transform Version",
                    "Status",
                    "Start Time",
                    "End Time",
                    "Created",
                    "Created By",
                    "Modified",
                    "Modified By",
                    "Exprunid",
                    "Job Id",
                    "Transform Run Log"}, null);

            _transformId = transformId;
        }

        @Override
        protected String getDataRegionName()
        {
            return getAttribute(Locator.xpath("//*[starts-with(@id, 'aqwp')]"), "id");
        }

        // just verify that we have a single row with the transform id we expect
        @Override
        protected void verifyResults()
        {
            DataRegionTable drt = new DataRegionTable(getDataRegionName(), _test, false /*selectors*/);
            assert(_columns.length == drt.getColumnCount());
            assert(1 == drt.getDataRowCount());
            String actual = drt.getDataAsText(0, "Transform Id");
            assert(_transformId.equalsIgnoreCase(actual));
        }
    }
}
