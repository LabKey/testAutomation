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
import org.labkey.test.util.RemoteConnectionHelperWD;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * User: Rylan
 * Date: 3/26/13
 * Time: 11:32 AM
 */
@Category({DailyB.class})
public class ETLTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ETLTestProject";

    //
    // expected results for transform UI testing
    //
    protected static final String TRANSFORM_APPEND = "{simpletest}/append";
    private static final String TRANSFORM_APPEND_DESC = "Append Test";
    private static final String TRANSFORM_BADCAST = "{simpletest}/badCast";
    private static final String TRANSFORM_BADCAST_DESC = "Bad Cast";
    private static final String TRANSFORM_BADTABLE = "{simpletest}/badTableName";
    private static final String TRANSFORM_BADTABLE_DESC = "BadTableName";
    protected static final String TRANSFORM_TRUNCATE = "{simpletest}/truncate";
    private static final String TRANSFORM_TRUNCATE_DESC = "Truncate Test";
    private static final String TRANSFORM_BYRUNID = "{simpletest}/appendIdByRun";
    private static final String TRANSFORM_REMOTE = "{simpletest}/remote";
    private static final String TRANSFORM_REMOTE_DESC = "Remote Test";
    private static final String TRANSFORM_REMOTE_CONNECTION = "EtlTest_RemoteConnection";
    private static final String TRANSFORM_REMOTE_STUDY = "/sampledata/dataintegration/ETLTestStudy.zip";
    private static final String TRANSFORM_BYRUNID_DESC = "ByRunId";

    //
    // internal counters
    //
    protected static int _jobsComplete;
    private static int _expectedErrors;
    //
    // holds expected results for the TransformHistory table.  The transform history table
    // shows all the runs for a specific ETL type
    //
    HashMap<String, ArrayList<String[]>> _transformHistories = new HashMap<>();

    //
    // holdes expected results for the TransformSummary table.  This will show
    // one row for each different ETL type run.
    //
    ArrayList<String []> _transformSummaries = new ArrayList<>();

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        runInitialSetup(false);

        // verify we don't show any rows for this newly created
        // project (test container filter)
        verifyTransformSummary();

        //append into empty target
        insertSourceRow("0", "Subject 0", null);

        runETL("append");
        addTransformResult(TRANSFORM_APPEND, "1", "COMPLETE", "2");
        assertInTarget1("Subject 0");
        checkRun();
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC);

        //append into populated target
        insertSourceRow("1", "Subject 1", null);
        runETL("append");
        addTransformResult(TRANSFORM_APPEND, "1", "COMPLETE", "2");
        checkRun();
        assertInTarget1("Subject 0", "Subject 1");

        // verify transform summary should only have the most recent entry (i.e., doesn't change)
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC);

        // rerun append and verify that no work is done
        runETL_NoWork("append");

        // verify only two pipeline jobs existed since the "no work" one should not
        // have fired off a pipeline job
        checkRun();

        // verify transform summary should only have the most recent entry since
        // it should filter out "no work" rows
        verifyTransformSummary();
        // summary should be where it was as well
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC);


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
        // add a row for the 'truncate' etl - this should show up in our summary view
        addTransformResult(TRANSFORM_TRUNCATE, "1", "COMPLETE", "2");
        assertInTarget1("Subject 2");
        assertNotInTarget1("Subject 0", "Subject 1");
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_TRUNCATE, TRANSFORM_TRUNCATE_DESC);

        //identify by run into populated target
        insertSourceRow("3", "Subject 3", "42");
        insertTransferRow("42", getDate(), getDate(), "new transfer", "added by test automation", "pending");
        runETL("appendIdByRun");
        addTransformResult(TRANSFORM_BYRUNID, "1", "COMPLETE", "2");
        assertInTarget1("Subject 2", "Subject 3");

        // intentionally fail transform by running append again after
        // the records were modified above
        runETL("append");
        addTransformResult(TRANSFORM_APPEND, "1", "ERROR", null);
        checkRun(true /*expectError*/);
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC);

        //error logging test, casting error, note that this causes an error in the checker
        // before a pipeline job is even scheduled
        runETL_CheckerError("badCast");
        addTransformResult(TRANSFORM_BADCAST, "1", "ERROR", null);
        // verify we log the error regardless of whether the pipeline job runs or not
        verifyTransformSummary();

        assertInLog("contains value not castable to a date:");

        //error logging test, bad run table name
        runETL_CheckerError("badTableName");
        addTransformResult(TRANSFORM_BADTABLE, "1", "ERROR", null);
        _expectedErrors++;
        assertInLog("Table not found:");

        // run tests over remote transform types
        verifyRemoteTransform();

        // be sure to check for all expected errors here so that the test won't fail on exit
        checkExpectedErrors(getExpectedErrorCount(_expectedErrors));
    }

    // looks like postgres inserts an "ERROR" word in their error string for the duplicate key
    // but mssql doesn't, hack around that here
    protected int getExpectedErrorCount(int original)
    {
        return (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL) ? (original+ 1) : original;
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

    private void verifyTransformSummary()
    {
        gotoQueryWebPart("TransformSummary");
        TransformSummaryVerifier verifier = new TransformSummaryVerifier(this, _transformSummaries);
        verifier.verifyResults();
    }

    private void waitForTransformPage(String linkText, String title, String status)
    {
        click(Locator.linkContainingText(linkText));
        // verify title
        waitForText(title);
        // wait for data in data region to appear
        waitForText(status);
    }

    private void verifyTransformHistory(String transformId, String transformDesc)
    {
        verifyTransformHistory(transformId, transformDesc, "COMPLETE");
    }
    private void verifyTransformHistory(String transformId, String transformDesc, String status)
    {
        waitForTransformPage(transformId, "Transform History - " + transformDesc, status);
        TransformHistoryVerifier verifier = new TransformHistoryVerifier(this, transformId, transformDesc,
                _transformHistories.get(transformId));
        verifier.verifyResults();
    }

    //
    // test a "remote" ETL that transfers data into datasets (instead of just base tables)
    //
    private void verifyRemoteTransform()
    {
        //
        // prepare our "remote" source dataset
        //
        gotoDataset("ETL Source");
        for (int i = 1; i < 4; i++)
        {
            insertDatasetRow(String.valueOf(i), "Subject " + String.valueOf(i));
        }
        clickTab("Portal");

        //
        // attempt to run the remote transform without a remote connection
        //
        runETL(TRANSFORM_REMOTE);
        checkRun(true /*expect error*/);
        addTransformResult(TRANSFORM_REMOTE, "1", "ERROR", null);
        assertNotInDatasetTarget1("Subject 1", "Subject 2", "Subject 3");
        _expectedErrors++;

        //
        // create our remote connection
        //
        RemoteConnectionHelperWD rconnHelper = new RemoteConnectionHelperWD(this);
        rconnHelper.createConnection(TRANSFORM_REMOTE_CONNECTION, getBaseURL(), getProjectName());

        //
        // run the remote transform again.  At the end of this we should have one summary entry for the
        // most recently run transform and then two history entries (1 error, 1 complete)
        //
        runETL(TRANSFORM_REMOTE);
        // note we do expect an error in the pipeline log from the above failure
        checkRun(true /*expect error*/);
        addTransformResult(TRANSFORM_REMOTE, "1", "COMPLETE", "3");
        assertInDatasetTarget1("Subject 1", "Subject 2", "Subject 3");
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_REMOTE, TRANSFORM_REMOTE_DESC);
    }

    protected void runInitialSetup(boolean remoteOnly)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        log("running setup");
        _containerHelper.createProject(getProjectName(), null);
        _expectedErrors = 0;
        _jobsComplete = 0;

        enableModule("DataIntegration", true);
        enableModule("simpletest", true);

        if(!remoteOnly)
            enableModule("Study", true);

        portalHelper.addQueryWebPart("Source", "vehicle", "etl_source", null);
        portalHelper.addQueryWebPart("Target1", "vehicle", "etl_target", null);

        if (remoteOnly)
            return;

        portalHelper.addQueryWebPart("Transfers", "vehicle", "transfer", null);

        // UNDONE: remove when we finalize casing of table names versus views across pg and mssql
        String transformRun =  (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL) ?
                "transformrun" : "TransformRun";

        portalHelper.addQueryWebPart("TransformRun", "dataintegration", transformRun, null);
        portalHelper.addQueryWebPart("TransformHistory", "dataintegration", "TransformHistory", null);
        portalHelper.addQueryWebPart("TransformSummary", "dataintegration", "TransformSummary", null);

        //
        // import our study (used by the remote transform tests).  This study is a continuous time point
        // study to use the same type of ETL source and target that one of our customer uses.
        //
        clickTab("Study");
        importStudyFromZip(new File(getLabKeyRoot(), TRANSFORM_REMOTE_STUDY), true /*ignore query validation*/);
        // bump our pipeline job count since we used the pipeline to import the study
        _jobsComplete++;
    }

    private void addTransformResult(String transformId, String version, String status, String recordsAffected)
    {
        addTransformSummary(new String[] {transformId, version, null, status, recordsAffected, null});
        addTransformHistory(transformId, new String[] {version, null, status, recordsAffected, null});
    }

    // The summary table should only have one row per transform sorted by transform id so make sure
    // our expectred results match that
    private void addTransformSummary(String[] newSummary)
    {
        String newTransformId = newSummary[0];
        int insertIdx;
        boolean replace = false;

        for (insertIdx = 0; insertIdx < _transformSummaries.size(); insertIdx++)
        {
            String transformId = _transformSummaries.get(insertIdx)[0];
            int cmp = transformId.compareToIgnoreCase(newTransformId);
            if (cmp >= 0)
            {
                replace = (cmp == 0);
                break;
            }
        }

        if (replace)
            _transformSummaries.remove(insertIdx);

        _transformSummaries.add(insertIdx, newSummary);
    }

    private void addTransformHistory(String transformName, String[] historyRow)
    {
        ArrayList<String[]> rows = null;

        if (_transformHistories.containsKey(transformName))
        {
            rows = _transformHistories.get(transformName);
        }

        if (null == rows)
        {
            rows = new ArrayList<>();
        }

        rows.add(0, historyRow);
        _transformHistories.put(transformName, rows);
    }

    private void insertDatasetRow(String id, String name)
    {
        log("inserting dataset row " + name);
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForElement(Locator.name("quf_ParticipantId"));
        setFormElement(Locator.name("quf_ParticipantId"), name);
        setFormElement(Locator.name("quf_date"), getDate());
        setFormElement(Locator.name("quf_id"), id);
        setFormElement(Locator.name("quf_name"), name);
        clickButton("Submit");
    }

    protected void insertSourceRow(String id, String name, String RunId)
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
        _jobsComplete++;
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

    protected void assertInTarget1(String... targets)
    {
        assertQueryWebPart("etl_target", "Target1", true, targets);
    }

    private void gotoQueryWebPart(String webpartName)
    {
        gotoQueryWebPart(webpartName, webpartName);
    }

    private void gotoDataset(String name)
    {
        clickTab("Study");
        clickAndWait(Locator.linkContainingText("2 datasets"));
        clickAndWait(Locator.linkContainingText(name));
    }

    private void assertInDatasetTarget1(String... targets)
    {
        gotoDataset("ETL Target");
        assertData(true, targets);
    }

    private void assertNotInDatasetTarget1(String... targets)
    {
        gotoDataset("ETL Target");
        assertData(false, targets);
    }

    private void gotoQueryWebPart(String webpartName, String queryName)
    {
        clickTab("Portal");
        click(Locator.xpath("//span[text()='" + queryName + "']"));
        waitForText(webpartName);
    }

    private void assertData(boolean assertTextPresent, String... targets)
    {
        for(String target : targets)
        {
            if (assertTextPresent)
                assertTextPresent(target);
            else
                assertTextNotPresent(target);
        }
    }

    private void assertQueryWebPart(String webpartName, String queryName, boolean assertTextPresent, String ... targets)
    {
        gotoQueryWebPart(webpartName, queryName);
        assertData(assertTextPresent, targets);
    }

    private void assertNotInTarget1(String... targets)
    {
        assertQueryWebPart("etl_target", "Target1", false, targets);
    }

    private void assertInLog(String... targets)
    {
        assertQueryWebPart("TransformRun", "TransformRun", true, targets);
    }

    protected void checkRun()
    {
        checkRun(false);
    }

    protected void checkRun(boolean expectError)
    {
        //goToProjectHome();
        clickTab("Portal");
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(_jobsComplete, "ETL Job", expectError);
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
        // remove the remote connection we created; it's okay to call this if
        // no connection was created
        if (afterTest)
        {
            RemoteConnectionHelperWD rconnHelper = new RemoteConnectionHelperWD(this);
            rconnHelper.deleteConnection(TRANSFORM_REMOTE_CONNECTION);
        }
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
        protected ArrayList<String[]> _data;
        protected ETLTest _test;

        BaseTransformVerifier(ETLTest test, String[] columns, ArrayList<String[]> data)
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
            assertTrue(_columns.length == drt.getColumnCount());
            assertTrue(_data.size() == drt.getDataRowCount());

            for (int row = 0; row < _data.size(); row ++)
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
                    String expected = _data.get(row)[col];
                    if (null != expected)
                        assertTrue(actual.equalsIgnoreCase(expected));
                }
            }
        }
    }

    class TransformSummaryVerifier extends BaseTransformVerifier
    {
        TransformSummaryVerifier(ETLTest test, ArrayList<String[]> data)
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
            for (int row = 0; row < _data.size(); row ++)
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

        TransformHistoryVerifier(ETLTest test, String transformId, String transformDesc, ArrayList<String[]> data)
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
            for (int row = 0; row < _data.size(); row ++)
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
            assertTrue(_columns.length == drt.getColumnCount());
            assertTrue(1 == drt.getDataRowCount());
            String actual = drt.getDataAsText(0, "Transform Id");
            assertTrue(_transformId.equalsIgnoreCase(actual));
        }
    }
}
