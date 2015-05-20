/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.etl;

import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataIntegrationHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ETLHelper
{
    private static final String VEHICLE_SCHEMA = "vehicle";
    public static final String ETL_SOURCE = "etl_source";
    public static final String ETL_TARGET = "etl_target";
    public static final String ETL_TARGET_2 = "etl_target2";
    public static final String ETL_DELETE = "etl_delete";
    public static final String TRANSFER = "transfer";
    private BaseWebDriverTest _test;

    private int _jobsComplete;
    private int _expectedErrors;

    private DataIntegrationHelper _diHelper;
    private String _projectName;

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

    public ETLHelper(BaseWebDriverTest test, String projectName)
    {
        _test = test;
        _diHelper = new DataIntegrationHelper("/" + projectName);
        _projectName = projectName;
    }

    public DataIntegrationHelper getDiHelper()
    {
        return _diHelper;
    }

    /**
     *  Increment the expected error count to the correct number of occurances in the log file
     *  of the string "ERROR" which correspond to the individual error.
     *  <p/>
     *  This can depend on the source of an expected error, and at the moment all errors generate
     *  at least two occurances anyway.
     *
     * @param twoErrors true when a given error generates two occurances of the string "ERROR" in the log.
     */
    protected void incrementExpectedErrorCount(boolean twoErrors)
    {
        _expectedErrors++;
        if (twoErrors)
            _expectedErrors++;
    }

    protected void incrementExpectedErrorCount()
    {
        // ETL log files usually have two occurances of the string "ERROR" for every error that occurs.
        incrementExpectedErrorCount(true);
    }

    protected int getExpectedErrorCount()
    {
        return _expectedErrors;
    }


    protected void incrementJobsCompleteCount()
    {
        _jobsComplete++;
    }

    protected void resetCounts()
    {
        _expectedErrors = 0;
        _jobsComplete = 0;
    }

    //sets 'enabled' checkbox to checked state for given ETL on DataIntegration tab, assumes current tab selected is DataIntegration
    protected void enableScheduledRun(String transformName)
    {
        _test.checkCheckbox(Locator.xpath("//td[.='" + transformName + "']/../td/input[contains(@onchange, 'Enabled')]"));
    }

    protected void disableScheduledRun(String transformName)
    {
        _test.uncheckCheckbox(Locator.xpath("//td[.='" + transformName + "']/../td/input[contains(@onchange, 'Enabled')]"));
    }

    //sets 'verbose' checbox to checked state for given ETL on DataIntegration tab, assumes current tab selected is DataIntegration
    protected void enableVerbose(String transformName)
    {
        _test.checkCheckbox(Locator.xpath("//td[.='" + transformName + "']/../td/input[contains(@onchange, 'Verbose')]"));
    }

    protected void doBasicSetup()
    {
        _test.log("running setup");
        _test._containerHelper.createProject(_projectName, null);
        _test._containerHelper.enableModules(Arrays.asList("DataIntegration", "simpletest"));
    }

    protected void doSetup()
    {
        doExtendedSetup(true);
    }

    protected void doExtendedSetup(boolean addAllWebparts)
    {
        doBasicSetup();
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.addQueryWebPart("Source", VEHICLE_SCHEMA, ETL_SOURCE, null);
        portalHelper.addQueryWebPart("Target1", VEHICLE_SCHEMA, ETL_TARGET, null);
        portalHelper.addQueryWebPart("Target2", VEHICLE_SCHEMA, ETL_TARGET_2, null);

        if (!addAllWebparts)
            return;

        portalHelper.addQueryWebPart("Delete", VEHICLE_SCHEMA, ETL_DELETE, null);
        portalHelper.addQueryWebPart("Transfers", VEHICLE_SCHEMA, TRANSFER, null);

        // UNDONE: remove when we finalize casing of table names versus views across pg and mssql
        String transformRun =  (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL) ?
                "transformrun" : "TransformRun";

        portalHelper.addQueryWebPart("TransformRun", "dataintegration", transformRun, null);
        portalHelper.addQueryWebPart("TransformHistory", "dataintegration", "TransformHistory", null);
        portalHelper.addQueryWebPart("TransformSummary", "dataintegration", "TransformSummary", null);
        portalHelper.addWebPart("Data Transform Jobs");
        // make sure the webpart has the 'scheduler' button on it
        _test.clickButton("Scheduler");
        _test.waitForText("View Processed Jobs");
        _test.goBack();
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
        _test.click(Locator.linkContainingText(status));
        _test.waitForElement(Locator.tag("span").withClass("x4-window-header-text").containing(".etl.log"));
        _test.waitAndClick(Ext4Helper.Locators.ext4ButtonContainingText("Close"));
    }

    protected void verifyTransformSummary()
    {
        _test.goToProjectHome();
        gotoQueryWebPart("TransformSummary");
        TransformSummaryVerifier verifier = new TransformSummaryVerifier(_transformSummaries);
        verifier.verifyResults();
    }

    protected void waitForTransformLink(String linkText, String goBackText, String ... waitForTexts)
    {
        _test.log("clicking link with text " + linkText + "'");
        _test.click(Locator.linkWithText(linkText));
        for (String waitForText : waitForTexts)
        {
            _test.log("waiting for text '" + waitForText + "'");
            _test.waitForText(waitForText);
        }
        _test.goBack();
        _test.waitForText(goBackText);
    }

    protected void waitForTransformPage(String linkText, String title, String status)
    {
        _test.log("clicking link with text " + linkText + " and status " + status);
        if(_test.isElementPresent(Locator.xpath("//a[.='" + status + "']/../..//a[.='" + linkText + "']")))
        {
            _test.click(Locator.xpath("//a[.='" + status + "']/../..//a[.='" + linkText + "']"));
        }
        else
        {
            _test.click(Locator.xpath("//a[.='" + status + "']/../..//a/nobr[.='" + linkText + "']"));
        }
        // verify title
        _test.log("waiting for title text " + title);
        _test.waitForText(title);
        // wait for data in data region to appear
        _test.log("waiting for status text " + status);
        _test.waitForText(status);
    }

    protected void verifyTransformHistory(String transformId, String transformDesc)
    {
        verifyTransformHistory(transformId, transformDesc, "COMPLETE");
    }

    protected void verifyTransformHistory(String transformId, String transformDesc, String status)
    {
        waitForTransformPage(transformId, "Transform History - " + transformDesc, status);
        TransformHistoryVerifier verifier = new TransformHistoryVerifier(transformId, transformDesc,
                _transformHistories.get(transformId));
        verifier.verifyResults();
    }

    protected void addTransformResult(String transformId, String version, String status, String recordsAffected)
    {
        addTransformSummary(new String[]{transformId, version, null, status, recordsAffected, null});
        addTransformHistory(transformId, new String[]{transformId, version, null, status, recordsAffected, null, "Job Details", "Run Details"});
    }

    // The summary table should only have one row per transform sorted by transform id so make sure
    // our expected results match that
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
        _test.log("inserting dataset row " + name);
        _test.waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        _test.waitForElement(Locator.name("quf_ParticipantId"));
        _test.setFormElement(Locator.name("quf_ParticipantId"), name);
        _test.setFormElement(Locator.name("quf_date"), getDate());
        _test.setFormElement(Locator.name("quf_id"), id);
        _test.setFormElement(Locator.name("quf_name"), name);
        _test.clickButton("Submit");
    }

    protected void insertQueryRow(String id, String name, String RunId, String query)
    {
        _test.log("inserting " + query + " row " + name);
        _test.clickTab("Portal");
        _test.click(new Locator.LinkLocator(StringUtils.capitalize(query)));
        _test.waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        _test.waitForElement(Locator.name("quf_id"));
        _test.setFormElement(Locator.name("quf_id"), id);
        _test.setFormElement(Locator.name("quf_name"), name);
        if (null != RunId)
        {
            _test.setFormElement(Locator.name("quf_transformrun"), RunId);
        }
        _test.clickButton("Submit");
        _test.log("returning to project home");
        _test.clickTab("Portal");
    }

    protected void insertSourceRow(String id, String name, String RunId)
    {
        insertQueryRow(id, name, RunId, "source");
    }

    protected void insertDeleteSourceRow(String id, String name, String RunId)
    {
        insertQueryRow(id, name, RunId, "delete");
    }

    protected void insertTransferRow(String rowId, String transferStart, String transferComplete,  String description, String log, String status)
    {
        _test.log("inserting transfer row rowid " + rowId);
        _test.goToProjectHome();
        _test.click(Locator.xpath("//span[text()='Transfers']"));
        _test.waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        _test.waitForElement(Locator.name("quf_rowid"));
        _test.setFormElement(Locator.name("quf_rowid"), rowId);
        _test.setFormElement(Locator.name("quf_transferstart"), transferStart);
        _test.setFormElement(Locator.name("quf_transfercomplete"), transferComplete);
        _test.setFormElement(Locator.name("quf_description"), description);
        _test.setFormElement(Locator.name("quf_log"), log);
        _test.setFormElement(Locator.name("quf_status"), status);
        _test.clickButton("Submit");
        _test.log("returning to project home");
        _test.clickTab("Portal");
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
        runETLNoNav(transformId, hasWork, hasCheckerError);

        _test.log("returning to project home");
        _test.goToProjectHome();
    }

    protected void runETLNoNav(String transformId, boolean hasWork, boolean hasCheckerError)
    {
        runETLNoNav(transformId, hasWork, hasCheckerError, true);
    }

    protected void runETLNoNavNoWait(String transformId, boolean hasWork, boolean hasCheckerError)
    {
        runETLNoNav(transformId, hasWork, hasCheckerError, false);
    }

    protected void runETLNoNav(String transformId, boolean hasWork, boolean hasCheckerError, boolean wait)
    {
        _test.log("running " + transformId + " job");
        _test.goToModule("DataIntegration");

        if (hasWork && !hasCheckerError)
        {
            // pipeline job will run
            _test.waitAndClickAndWait(findRunNowButton(transformId));
            if (wait)
            {
                waitForEtl();
            }
        }
        else
        {
            // pipeline job does not run
            _test.waitAndClick(findRunNowButton(transformId));
            _test._ext4Helper.clickWindowButton(hasCheckerError ? "Error" : "Success", "OK", 0, 0);
        }
    }

    protected void waitForEtl()
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                if (_test.isElementPresent(Locator.tag("tr")
                        .withPredicate(Locator.xpath("td").withClass("labkey-form-label").withText("Status"))
                        .withPredicate(Locator.xpath("td").withText("ERROR"))))
                    return true;
                else if (_test.isElementPresent(Locator.tag("tr")
                        .withPredicate(Locator.xpath("td").withClass("labkey-form-label").withText("Status"))
                        .withPredicate(Locator.xpath("td").withText("COMPLETE"))))
                    return true;
                else
                    _test.refresh();

                return false;
            }
        }, "ETL did not finish", BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    private Locator.XPathLocator findTransformConfigCell(String transformId, boolean isLink)
    {
        transformId = ensureFullIdString(transformId);
        Locator.XPathLocator baseCell = Locator.xpath("//tr[@transformid='" + transformId + "']/td");
        return (isLink) ? baseCell.child("a") : baseCell;
    }

    private Locator.XPathLocator findRunNowButton(String transformId)
    {
        return findTransformConfigCell(transformId, true).withDescendant(Locator.xpath("span")).withText("run now");
    }

    protected Locator.XPathLocator findLastStatusCell(String transformId, String status, boolean isLink)
    {
        return findTransformConfigCell(transformId, isLink).withText(status);
    }

    protected RunTransformResponse runETL_API(String transformId, boolean wait) throws Exception
    {
        _test.log("running " + transformId + " job");
        transformId = ensureFullIdString(transformId);
        return wait ? _diHelper.runTransformAndWait(transformId, 30000) : _diHelper.runTransform(transformId);
    }

    private String ensureFullIdString(String transformId)
    {
        if (!StringUtils.startsWith(transformId, "{"))
        {
            transformId = "{simpletest}/" + transformId;
        }
        return transformId;
    }

    protected RunTransformResponse runETL_API(String transformId) throws Exception
    {
        return runETL_API(transformId, true);
    }

    protected void clickRetryButton()
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            public boolean check()
            {
                if (null == _test.getButtonLocator("Retry"))
                {
                    _test.refresh();
                    return false;
                }
                return true;
            }
        }, "Retry button did not appear.", BaseWebDriverTest.WAIT_FOR_PAGE);
        _test.clickButton("Retry");
    }

    protected void deleteSourceRow(String... ids)
    {
        _test.goToProjectHome();
        _test.clickAndWait(Locator.xpath("//span[text()='Source']"));
        for(String id : ids)
        {
            _test.log("deleting source row id " + id);
            _test.click(Locator.xpath("//a[text()='" + id + "']/../../td/input[@type='checkbox']"));
        }
        _test.applyAndWaitForPageToLoad(new Function<Void, Void>()
        {
            @Override
            public Void apply(Void aVoid)
            {
                _test.click(Locator.xpath("//span[text()='Delete']"));
                // eat the alert without spewing to the log file
                _test.acceptAlert();
                return null;
            }
        });
        _test.log("returning to project home");
        _test.clickTab("Portal");
    }

    protected void cleanupTestTables() throws Exception
    {
        deleteAllRows(ETL_SOURCE);
        deleteAllRows(ETL_TARGET);
        deleteAllRows(ETL_TARGET_2);
        deleteAllRows(ETL_DELETE);
        deleteAllRows(TRANSFER);

    }
    protected void deleteAllRows(String tableName) throws Exception
    {
        Connection cn = new Connection(_test.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand cmd = new SelectRowsCommand(VEHICLE_SCHEMA, tableName);
        SelectRowsResponse resp = cmd.execute(cn, _projectName);
        if (resp.getRowCount().intValue() > 0)
        {
            _test.log("Deleting rows from " + VEHICLE_SCHEMA + "." + tableName);
            DeleteRowsCommand delete = new DeleteRowsCommand(VEHICLE_SCHEMA, tableName);
            for (Map<String, Object> row : resp.getRows())
            {
                delete.addRow(row);
            }
            delete.execute(cn, _projectName);
        }
    }

    protected void assertInTarget1(String... targets)
    {
        assertQueryWebPart("etl_target", "Target1", true, targets);
    }

    protected void assertInTarget2(String... targets)
    {
        assertQueryWebPart(ETL_TARGET_2, "Target2", true, targets);
    }

    protected void gotoQueryWebPart(String webpartName)
    {
        gotoQueryWebPart(webpartName, webpartName);
    }

    protected void gotoDataset(String name)
    {
        _test.clickTab("Study");
        _test.clickAndWait(Locator.linkContainingText("2 datasets"));
        _test.clickAndWait(Locator.linkContainingText(name));
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
        _test.clickTab("Portal");
        _test.clickAndWait(Locator.xpath("//span[text()='" + queryName + "']"));
        _test.waitForText(webpartName);
    }

    protected void assertData(boolean assertTextPresent, String... targets)
    {
        if (assertTextPresent)
            _test.assertTextPresent(targets);
        else
            _test.assertTextNotPresent(targets);
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

    protected void assertInEtlLogFile(String jobId, String... logStrings) throws Exception
    {
        // Promoted the guts of this method to DataIntegrationHelper
        _diHelper.assertInEtlLogFile(jobId, logStrings);
    }

    protected void checkRun()
    {
        checkRun(false);
    }

    protected void checkRun(boolean expectError)
    {
        _test.clickTab("Portal");
        _test.goToModule("Pipeline");
        _test.waitForPipelineJobsToComplete(_jobsComplete, "ETL Job", expectError);
    }

    protected String getDate()
    {
        Calendar calendar = new GregorianCalendar();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return format.format(calendar.getTime());
    }

    protected void verifyErrorLog(String transformName, List<String> errors)
    {
        _test.click(Locator.xpath("//a[.='" + transformName + "']//..//..//a[.='ERROR']"));

        _test.assertTextPresent(errors);
    }

    protected void runETLandCheckErrors(String ETLName, boolean hasWork, boolean hasCheckerError, List<String> errors)
    {
        runETLNoNav(ETLName, hasWork, hasCheckerError);
        if(!hasCheckerError)
        {
            _test.refresh(); // log webpart may not yet be present
            _test.waitAndClickAndWait(Locator.linkWithText("Show full log file"));
            _test.waitForElement(Locator.linkWithText("Show summary"));
        }
        else
        {
            _test.goToProjectHome();
        }
        _test.assertTextPresentCaseInsensitive(errors);
    }

    class BaseTransformVerifier
    {
        // table of results to verify
        protected String[] _columns;
        protected ArrayList<String[]> _data;

        BaseTransformVerifier(String[] columns, ArrayList<String[]> data)
        {
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
            assertEquals(_columns.length, drt.getColumnCount());
            assertEquals(_data.size(), drt.getDataRowCount());

            for (int row = 0; row < _data.size(); row++)
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
        TransformSummaryVerifier(ArrayList<String[]> data)
        {
            super(new String[]{
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
            String status = drt.getDataAsText(0, "Last Status");
            verifyLogFileLink(status);
        }
    }

    class TransformHistoryVerifier extends BaseTransformVerifier
    {
        protected String _transformId;
        protected String _transformDesc;

        TransformHistoryVerifier(String transformId, String transformDesc, ArrayList<String[]> data)
        {
            super(new String[]{
                    "Name",
                    "Version",
                    "Date Run",
                    "Status",
                    "Records Processed",
                    "Execution Time",
                    "Job Info",
                    "Run Info"
            }, data);

            _transformId = transformId;
            _transformDesc = transformDesc;
        }

        @Override
        protected String getDataRegionName()
        {
            return _test.getAttribute(Locator.xpath("//*[starts-with(@id, 'aqwp')]"), "id");
        }

        @Override
        protected void verifyResults()
        {
            super.verifyResults();

            DataRegionTable drt = new DataRegionTable(getDataRegionName(), _test, false /*selectors*/);

            // walk through all the history rows and verify the link to the file log works (just the first one)
            // and the links work for the transform details page, job details, and run details
            for (int row = 0; row < _data.size(); row ++)
            {
                String status = drt.getDataAsText(row, "Status");
                if (0 == row)
                {
                    verifyLogFileLink(status);
                }
                String run = drt.getDataAsText(row, "Name");
                verifyTransformDetails(run, status);
                _test.goBack();
                // wait for the grid to reload
                _test.waitForText("Run Details");

                // verify job link
                String job = drt.getDataAsText(row, "Job Info");
                waitForTransformLink(job, "Run Details", "Pipeline Jobs", status);

                // verify experiment run link
                String exp = drt.getDataAsText(row, "Run Info");
                waitForTransformLink(exp, "Run Details", "Run Details", run);
            }
        }

        protected void verifyTransformDetails(String run, String status)
        {
            waitForTransformPage(run, "Transform Details - " + _transformDesc, status);
            TransformDetailsVerifier verifier = new TransformDetailsVerifier(_transformId);
            verifier.verifyResults();
        }

    }

    // currently this has the same schema as the TransformRuns table
    class TransformDetailsVerifier extends BaseTransformVerifier
    {
        protected String _transformId;
        TransformDetailsVerifier(String transformId)
        {

            super(new String[]{
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
            return _test.getAttribute(Locator.xpath("//*[starts-with(@id, 'aqwp')]"), "id");
        }

        // just verify that we have a single row with the transform id we expect
        @Override
        protected void verifyResults()
        {
            DataRegionTable drt = new DataRegionTable(getDataRegionName(), _test, false /*selectors*/);
            assertTrue("column length mismatch for data region " + getDataRegionName(), _columns.length == drt.getColumnCount());
            assertEquals(1, drt.getDataRowCount());
            String actual = drt.getDataAsText(0, "Transform Id");
            assertTrue(_transformId.equalsIgnoreCase(actual));
        }
    }
}
