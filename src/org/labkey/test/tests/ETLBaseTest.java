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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataIntegrationHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.RemoteConnectionHelper;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertTrue;

public abstract class ETLBaseTest extends BaseWebDriverTest
{
    protected static String PROJECT_NAME = "ETLTestProject";
    protected static int _jobsComplete;
    protected static int _expectedErrors;
    protected DataIntegrationHelper _diHelper = new DataIntegrationHelper("/" + PROJECT_NAME);


    //Internal counters

    // holds expected results for the TransformHistory table.  The transform history table
    // shows all the runs for a specific ETL type
    //
    HashMap<String, ArrayList<String[]>> _transformHistories = new HashMap<>();

    //
    // holds expected results for the TransformSummary table.  This will show
    // one row for each different ETL type run.
    //
    ArrayList<String []> _transformSummaries = new ArrayList<>();

    /**
     *  Increment the expected error count to the correct number of occurances in the log file
     *  of the string "ERROR" which correspond to the individual error.
     *  <p/>
     *  This can depend on the source of an expected error, and at the moment all errors generate
     *  at least two occurances anyway.
     *
     * @param dbError true when the expected error is a SQLException from the database
     * @param twoErrors true when a given error generates two occurances of the string "ERROR" in the log.
     */
    protected void incrementExpectedErrorCount(boolean dbError, boolean twoErrors)
    {
        _expectedErrors++;
        if (dbError)
            _expectedErrors = getExpectedErrorCount(_expectedErrors);
        // At the moment, the ETL log files usually have two occurances of the string "ERROR" for every error that occurs.
        if (twoErrors)
            _expectedErrors++;
    }

    protected void incrementExpectedErrorCount(boolean dbError)
    {
        incrementExpectedErrorCount(dbError, true);
    }

    // looks like postgres inserts an "ERROR" word in their error string for the duplicate key
    // but mssql doesn't, hack around that here
    protected int getExpectedErrorCount(int original)
    {
        return (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL) ? (original+ 1) : original;
    }

    //sets 'enabled' checkbox to checked state for given ETL on DataIntegration tab, assumes current tab selected is DataIntegration
    protected void enableScheduledRun(String transformName)
    {
        checkCheckbox(Locator.xpath("//td[.='" + transformName + "']/../td/input[contains(@onchange, 'Enabled')]"));
    }

    protected void disableScheduledRun(String transformName)
    {
        uncheckCheckbox(Locator.xpath("//td[.='" + transformName + "']/../td/input[contains(@onchange, 'Enabled')]"));
    }

    //sets 'verbose' checbox to checked state for given ETL on DataIntegration tab, assumes current tab selected is DataIntegration
    protected void enableVerbose(String transformName)
    {
        checkCheckbox(Locator.xpath("//td[.='" + transformName + "']/../td/input[contains(@onchange, 'Verbose')]"));
    }

    //
    // verify the following:
    // contains expected number of rows (container filter and "no work" filter is there)
    // contains expected number of columns
    // links to file logs work in history and summary UIs
    // links can be traversed from summary -> history -> detail for the given transform id
    //
    protected void verifyLogFileLink(String status)
    {
        click(Locator.linkContainingText(status));
        waitForElement(Locator.tag("span").withClass("x4-window-header-text").containing(".etl.log"));
        waitAndClick(Locator.ext4ButtonContainingText("Close"));
    }

    protected void verifyTransformSummary()
    {
        gotoQueryWebPart("TransformSummary");
        TransformSummaryVerifier verifier = new TransformSummaryVerifier(this, _transformSummaries);
        verifier.verifyResults();
    }

    protected void waitForTransformPage(String linkText, String title, String status)
    {
        log("clicking link with text " + linkText + " and status " + status);
        if(isElementPresent(Locator.xpath("//a[.='" + status + "']/../..//a[.='" + linkText + "']")))
        {
            click(Locator.xpath("//a[.='" + status + "']/../..//a[.='" + linkText + "']"));
        }
        else
        {
            click(Locator.xpath("//a[.='" + status + "']/../..//a/nobr[.='" + linkText + "']"));
        }
        // verify title
        log("waiting for title text " + title);
        waitForText(title);
        // wait for data in data region to appear
        log("waiting for status text " + status);
        waitForText(status);
    }

    protected void verifyTransformHistory(String transformId, String transformDesc)
    {
        verifyTransformHistory(transformId, transformDesc, "COMPLETE");
    }

    protected void verifyTransformHistory(String transformId, String transformDesc, String status)
    {
        waitForTransformPage(transformId, "Transform History - " + transformDesc, status);
        TransformHistoryVerifier verifier = new TransformHistoryVerifier(this, transformId, transformDesc,
                _transformHistories.get(transformId));
        verifier.verifyResults();
    }

    protected void addTransformResult(String transformId, String version, String status, String recordsAffected)
    {
        addTransformSummary(new String[]{transformId, version, null, status, recordsAffected, null});
        addTransformHistory(transformId, new String[]{version, null, status, recordsAffected, null});
    }

    // The summary table should only have one row per transform sorted by transform id so make sure
    // our expectred results match that
    protected void addTransformSummary(String[] newSummary)
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

    protected void addTransformHistory(String transformName, String[] historyRow)
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

    protected void insertDatasetRow(String id, String name)
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

    protected void insertTransferRow(String rowId, String transferStart, String transferComplete,  String description, String log, String status)
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

    protected void runETL(String transformId)
    {
        _runETL(transformId, true, false);
        _jobsComplete++;
    }

    protected void runETL_CheckerError(String transformId)
    {
        _runETL(transformId, true, true);
    }

    protected void runETL_NoWork(String transformId)
    {
        _runETL(transformId, false, false);
    }

    protected void _runETL(String transformId, boolean hasWork, boolean hasCheckerError)
    {
        log("running " + transformId + " job");
        goToModule("DataIntegration");

        if (hasWork && !hasCheckerError)
        {
            // pipeline job will run
            waitAndClickAndWait(Locator.xpath("//tr[contains(@transformid,'" + transformId + "')]/td/a"));
            waitFor(new Checker()
            {
                @Override
                public boolean check()
                {
                    if (isElementPresent(Locator.tag("tr")
                            .withPredicate(Locator.xpath("td").withClass("labkey-form-label").withText("Status"))
                            .withPredicate(Locator.xpath("td").withText("ERROR"))))
                        return true;
                    else if (isElementPresent(Locator.tag("tr")
                            .withPredicate(Locator.xpath("td").withClass("labkey-form-label").withText("Status"))
                            .withPredicate(Locator.xpath("td").withText("COMPLETE"))))
                        return true;
                    else
                        refresh();

                    return false;
                }
            }, "ETL did not finish", WAIT_FOR_JAVASCRIPT);
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

    protected void runETL_NoNav(String transformId, boolean hasWork, boolean hasCheckerError)
    {
        _runETL_NoNav(transformId, hasWork, hasCheckerError);
    }

    protected void _runETL_NoNav(String transformId, boolean hasWork, boolean hasCheckerError)
    {
        log("running " + transformId + " job");
        goToModule("DataIntegration");

        if (hasWork && !hasCheckerError)
        {
            // pipeline job will run
            waitAndClickAndWait(Locator.xpath("//tr[contains(@transformid,'" + transformId + "')]/td/a"));
            waitFor(new Checker()
            {
                @Override
                public boolean check()
                {
                    if (isElementPresent(Locator.tag("tr")
                            .withPredicate(Locator.xpath("td").withClass("labkey-form-label").withText("Status"))
                            .withPredicate(Locator.xpath("td").withText("ERROR"))))
                        return true;
                    else if (isElementPresent(Locator.tag("tr")
                            .withPredicate(Locator.xpath("td").withClass("labkey-form-label").withText("Status"))
                            .withPredicate(Locator.xpath("td").withText("COMPLETE"))))
                        return true;
                    else
                        refresh();

                    return false;
                }
            }, "ETL did not finish", WAIT_FOR_JAVASCRIPT);
        }
        else
        {
            // pipeline job does not run
            waitAndClick(Locator.xpath("//tr[contains(@transformid,'" + transformId + "')]/td/a"));
            _ext4Helper.clickWindowButton(hasCheckerError ? "Error" : "Success", "OK", 0, 0);
        }
    }

    protected RunTransformResponse runETL_API(String projectName, String transformId, boolean hasWork, boolean hasCheckerError) throws Exception
    {
        log("running " + transformId + " job");
        if (!StringUtils.startsWith(transformId, "{"))
        {
            transformId = "{simpletest}/" + transformId;
        }
        return _diHelper.runTransformAndWait(transformId, 30000);
    }

    protected RunTransformResponse runETL_API(String projectName, String transformId) throws Exception
    {
        return runETL_API(projectName, transformId, true, false);
    }

    protected RunTransformResponse runETL_API(String transformId) throws Exception
    {
        return runETL_API(PROJECT_NAME, transformId);
    }

    protected void deleteSourceRow(String... ids)
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

    protected void gotoQueryWebPart(String webpartName)
    {
        gotoQueryWebPart(webpartName, webpartName);
    }

    protected void gotoDataset(String name)
    {
        clickTab("Study");
        clickAndWait(Locator.linkContainingText("2 datasets"));
        clickAndWait(Locator.linkContainingText(name));
    }

    protected void assertInDatasetTarget1(String... targets)
    {
        gotoDataset("ETL Target");
        assertData(true, targets);
    }

    protected void assertNotInDatasetTarget1(String... targets)
    {
        gotoDataset("ETL Target");
        assertData(false, targets);
    }

    protected void gotoQueryWebPart(String webpartName, String queryName)
    {
        clickTab("Portal");
        click(Locator.xpath("//span[text()='" + queryName + "']"));
        waitForText(webpartName);
    }

    protected void assertData(boolean assertTextPresent, String... targets)
    {
        for(String target : targets)
        {
            if (assertTextPresent)
                assertTextPresent(target);
            else
                assertTextNotPresent(target);
        }
    }

    protected void assertQueryWebPart(String webpartName, String queryName, boolean assertTextPresent, String ... targets)
    {
        gotoQueryWebPart(webpartName, queryName);
        assertData(assertTextPresent, targets);
    }

    protected void assertNotInTarget1(String... targets)
    {
        assertQueryWebPart("etl_target", "Target1", false, targets);
    }

    protected void assertInLog(String... targets)
    {
        assertQueryWebPart("TransformRun", "TransformRun", true, targets);
    }

    protected void assertInEtlLogFile(String jobId, String logString) throws Exception
    {

        final String etlLogFile = _diHelper.getEtlLogFile(jobId);
        assertTrue("Log file did not contain: " + logString, StringUtils.containsIgnoreCase(etlLogFile, logString));
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

    protected String getDate()
    {
        Calendar calendar = new GregorianCalendar();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return format.format(calendar.getTime());
    }

    //@Override
    protected void doCleanup(boolean afterTest, String connectionName) throws TestTimeoutException
    {
        // remove the remote connection we created; it's okay to call this if
        // no connection was created
        if (afterTest)
        {
            RemoteConnectionHelper rconnHelper = new RemoteConnectionHelper(this);
            rconnHelper.deleteConnection(connectionName);
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

    protected void verifyErrorLog(String transformName, List<String> errors)
    {
        click(Locator.xpath("//a[.='" + transformName + "']//..//..//a[.='ERROR']"));

        assertTextPresent(errors);
    }

    protected void copyETLfiles(File sourceDir, File destinationDir)
    {
        File[] files = sourceDir.listFiles();
        try
        {
            for(File file : files)
            {
                FileUtils.copyFileToDirectory(file, destinationDir);
            }
        }
        catch(IOException e)
        {
            throw new RuntimeException("Transform xml file copy failed", e);
        }
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    class BaseTransformVerifier
    {
        // table of results to verify
        protected String[] _columns;
        protected ArrayList<String[]> _data;
        protected ETLBaseTest _test;

        BaseTransformVerifier(ETLBaseTest test, String[] columns, ArrayList<String[]> data)
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
                    // just means that it is not easily comparable (execution times, for example)
                    //
                    String actual = drt.getDataAsText(row, _columns[col]);
                    String expected = _data.get(row)[col];
                    if (null != expected)
                        assertTrue("Expected value " + expected + " in row " + String.valueOf(row + 1) + " column " + String.valueOf(col + 1) + " of DataRegion " + getDataRegionName() + " but found " + actual, actual.equalsIgnoreCase(expected));
                }
            }
        }
    }

    class TransformSummaryVerifier extends BaseTransformVerifier
    {
        TransformSummaryVerifier(ETLBaseTest test, ArrayList<String[]> data)
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

        TransformHistoryVerifier(ETLBaseTest test, String transformId, String transformDesc, ArrayList<String[]> data)
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
        TransformDetailsVerifier(ETLBaseTest test, String transformId)
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
            assertTrue("column length mismatch for data region " + getDataRegionName() ,_columns.length == drt.getColumnCount());
            assertTrue(1 == drt.getDataRowCount());
            String actual = drt.getDataAsText(0, "Transform Id");
            assertTrue(_transformId.equalsIgnoreCase(actual));
        }
    }
}
