/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.ETL;
import org.labkey.test.pages.dataintegration.ETLScheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, Data.class, ETL.class})
public class ETLTest extends ETLBaseTest
{
    public static final String ETL_OUT = "etlOut";

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "ETLTestProject";
    }

    @BeforeClass
    public static void setupProject()
    {
        ETLTest init = (ETLTest) getCurrentTest();

        init.doSetup();
    }

    @Before
    public void preTest() throws Exception
    {
        _etlHelper.resetCounts();
        resetErrors();
        _etlHelper.cleanupTestTables();
        goToProjectHome();
    }

    @After
    public void postTest()
    {
        if (!_testFailed)
            checkExpectedErrors(_etlHelper.getExpectedErrorCount());
    }

    @Test
    public void testDeleteRows() throws Exception
    {

        final String PREFIX = "Subject For Delete Test ";
        _etlHelper.insertSourceRow("500", PREFIX + "1", null);
        _etlHelper.insertSourceRow("501", PREFIX + "2", null);
        _etlHelper.runETL(APPEND_WITH_ROWVERSION);
        _etlHelper.assertInTarget1(PREFIX + "1", PREFIX + "2");
        // Add one of them to the deleted rows source query
        _etlHelper.insertDeleteSourceRow("500", PREFIX + "1", null);
        // Filter column for the append is a rowversion; for the delete is a datetime
        _etlHelper.runETL(APPEND_WITH_ROWVERSION);
        _etlHelper.assertNotInTarget1(PREFIX + "1");
        _etlHelper.assertInTarget1(PREFIX + "2");

        // Now flip which column is which datatype. Filter column for append is a datetime, delete is a rowversion
        _etlHelper.cleanupTestTables();
        _etlHelper.insertSourceRow("502", PREFIX + "3", null);
        _etlHelper.insertSourceRow("503", PREFIX + "4", null);
        _etlHelper.runETL(APPEND);
        _etlHelper.assertInTarget1(PREFIX + "3", PREFIX + "4");
        // Add one of them to the deleted rows source query
        _etlHelper.insertDeleteSourceRow("503", PREFIX + "4", null);
        _etlHelper.runETL(APPEND);
        _etlHelper.assertNotInTarget1(PREFIX + "4");
        _etlHelper.assertInTarget1(PREFIX + "3");
    }

    @Test
    public void testRowversionIncremental()
    {
        // Test using a rowversion column as incremental filter
        final String PREFIX = "Subject By Rowversion ";
        _etlHelper.insertSourceRow("400", PREFIX + "1", null);
        _etlHelper.runETL(APPEND_WITH_ROWVERSION);
        _etlHelper.assertInTarget1(PREFIX + "1");
        _etlHelper.insertSourceRow("401", PREFIX + "2", null);
        // This will throw a constraint error if the filter doesn't work
        _etlHelper.runETL(APPEND_WITH_ROWVERSION);
        _etlHelper.assertInTarget1(PREFIX + "2");
    }

    @Test
    public void testMultipleTransactions() throws Exception // Migrated from ETLMultipleTransactionsTest
    {
        final String ETL = "{simpletest}/multipleTransactions";
        String jobId = _etlHelper.runETL_API(ETL).getJobId();
        _etlHelper.incrementExpectedErrorCount();

        if (WebTestHelper.getDatabaseType().equals(WebTestHelper.DatabaseType.PostgreSQL))
        {
            // See issue 22213; we don't support specifying a transaction size on Postgres when source
            // and target are the same datasource. Make sure the error message happens.
            // Note: it will actually work for small datasets like that used in this test, but larger datasets will give
            // errors from pg and leave the target in a bad state.
            _etlHelper.assertInEtlLogFile(jobId, "not supported on Postgres");
        }
        else
        {
            _etlHelper.assertInEtlLogFile(jobId, "Target transactions will be committed every 2 rows", "Could not convert 'uhoh' for field rowid");
            goToProjectHome();
            _etlHelper.assertInTarget2("xact1 1", "xact1 2");
        }
    }

    @Test
    public void testErrors() // Migrated from ETLErrorTest
    {
        final String TRANSFORM_KEYCONSTRAINT_ERROR = "{simpletest}/SimpleETLCausesKeyConstraintViolation";
        final String TRANSFORM_QUERY_ERROR = "{simpletest}/SimpleETLqueryDoesNotExist";
        final String TRANSFORM_NOCOL_ERROR = "{simpletest}/SimpleETLCheckerErrorTimestampColumnNonexistent";
        final String TRANSFORM_BADCAST = "{simpletest}/badCast";
        final String TRANSFORM_BADTABLE = "{simpletest}/badTableName";

        goToProjectHome();
        clickTab("DataIntegration");
        assertTextNotPresent("Should not have loaded invalid transform xml", "Error Missing Source");
        _etlHelper.insertSourceRow("0", "Subject 0", null);

        List<String> errors = new ArrayList<>();
        errors.add("AK_etltarget");
        errors.add("duplicate");
        errors.add("ERROR: Error running executeCopy");
        errors.add("org.labkey.api.pipeline.PipelineJobException: Error running executeCopy");
        _etlHelper.runETLandCheckErrors(TRANSFORM_KEYCONSTRAINT_ERROR, true, false, errors);
        errors.clear();

        errors.add("Could not find table: vehicle.etl_source_cheeseburger");
        _etlHelper.runETLandCheckErrors(TRANSFORM_QUERY_ERROR, false, true, errors);

        _etlHelper.runETLNoNav(TRANSFORM_NOCOL_ERROR, false, true);
        assertTextPresent("Column not found: etl_source.monkeys");
        errors.clear();

        errors.add("contains value not castable");
        _etlHelper.runETLandCheckErrors(TRANSFORM_BADCAST, false, true, errors);
        errors.clear();

        errors.add("Table not found:");
        waitForElement(Locator.xpath(".//*[@id='bodypanel']"));
        _etlHelper.runETLandCheckErrors(TRANSFORM_BADTABLE, false, true, errors);
        errors.clear();

        clickTab("DataIntegration");
        _etlHelper.enableScheduledRun("Error Bad Source Schema");
        //schedule for job is 15 seconds
        sleep(15000);
        _etlHelper.disableScheduledRun("Error Bad Source Schema");
        clickTab("Portal");
        Assert.assertTrue(countText("java.lang.IllegalArgumentException: Could not find table: vehicle.etl_source_cheeseburger") > 1);
        //no way of knowing error count due to scheduled job running unknown number of times
        pushLocation();
        resetErrors();
        popLocation();
    }

    @Test
    public void testStoredProcTransforms() throws Exception // Migrated from ETLStoredProcedureTest
    {
        final String TRANSFORM_NORMAL_OPERATION_SP = "{simpletest}/SProcNormalOperation";
        final String TRANSFORM_BAD_NON_ZERO_RETURN_CODE_SP = "{simpletest}/SProcBadNonZeroReturnCode";
        final String TRANSFORM_BAD_PROCEDURE_NAME_SP = "{simpletest}/SProcBadProcedureName";
        final String TRANSFORM_PERSISTED_PARAMETER_SP = "{simpletest}/SProcPersistedParameter";
        final String TRANSFORM_OVERRIDE_PERSISTED_PARAMETER_SP = "{simpletest}/SProcOverridePersistedParameter";
        final String TRANSFORM_RUN_FILTER_SP = "{simpletest}/SProcRunFilter";
        final String TRANSFORM_MODIFIED_FILTER_NO_SOURCE_SP = "{simpletest}/SProcModifiedSinceNoSource";
        final String TRANSFORM_MODIFIED_FILTER_WITH_SOURCE_SP = "{simpletest}/SProcModifiedSinceWithSource";
        final String TRANSFORM_BAD_MODIFIED_FILTER_WITH_BAD_SOURCE_SP = "{simpletest}/SProcBadModifiedSinceWithBadSource";

        /*
        Test modes as mapped in the proc etlTest
        1	normal operation
        2	return code > 0
        3	raise error
        4	input/output parameter persistence
        5	override of persisted input/output parameter
        6	Run filter strategy, require @filterRunId
        7   Modified since filter strategy, no source, require @filterStartTimeStamp & @filterEndTimeStamp,
            populated from output of previous run
        8	Modified since filter strategy with source, require @filterStartTimeStamp & @filterEndTimeStamp
            populated from the filter strategy IncrementalStartTime & IncrementalEndTime
        */

        // All tests use the API
        RunTransformResponse rtr;

        // test mode 1, Normal operation
        rtr = _etlHelper.runETL_API(TRANSFORM_NORMAL_OPERATION_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));
        _etlHelper.assertInEtlLogFile(rtr.getJobId(), "Test print statement logging", "Test returnMsg logging");

        // test mode 2, return code > 0, should be an error
        rtr = _etlHelper.runETL_API(TRANSFORM_BAD_NON_ZERO_RETURN_CODE_SP);
        assertEquals("ERROR", _diHelper.getTransformStatus(rtr.getJobId()));
        _etlHelper.incrementExpectedErrorCount();
        // That should have an error ERROR: Error: Sproc exited with return code 1

        // try to run a non-existent proc
        rtr = _etlHelper.runETL_API(TRANSFORM_BAD_PROCEDURE_NAME_SP);
        assertEquals("ERROR", _diHelper.getTransformStatus(rtr.getJobId()));
        _etlHelper.incrementExpectedErrorCount(false);
        // Error on missing procedure; over api that didn't create a job

        // test mode 3, raise an error inside the sproc
        rtr = _etlHelper.runETL_API(TRANSFORM_BAD_THROW_ERROR_SP);
        assertEquals("ERROR", _diHelper.getTransformStatus(rtr.getJobId()));
        _etlHelper.incrementExpectedErrorCount(false);

        // test mode 4, parameter persistance. Run twice. First time, the value of the in/out parameter supplied in the xml file
        // is changed in the sproc, which should be persisted into the transformConfiguration.state variable map.
        // The second time, the proc doublechecks the changed value was persisted and passed in; errors if not.
        _etlHelper.runETL_API(TRANSFORM_PERSISTED_PARAMETER_SP);
        rtr = _etlHelper.runETL_API(TRANSFORM_PERSISTED_PARAMETER_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));
        // verify no error

        // test mode 5, override of persisted parameter. Run twice. First time, the value of the in/out parameter supplied in the xml file
        // is changed in the sproc, which should be persisted into the transformConfiguration.state variable map.
        // However, the parameter is set to override=true in the xml, so on the second run the persisted value should be ignored
        // and original value used instead. The proc verifies this and errors if value has changed.
        _etlHelper.runETL_API(TRANSFORM_OVERRIDE_PERSISTED_PARAMETER_SP);
        rtr = _etlHelper.runETL_API(TRANSFORM_OVERRIDE_PERSISTED_PARAMETER_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));
        // verify no error

        // test mode 6, RunFilterStrategy specified in the xml. Require filterRunId input parameter to be populated
        _etlHelper.insertTransferRow("142", _etlHelper.getDate(), _etlHelper.getDate(), "new transfer", "added by test automation", "pending");
        rtr = _etlHelper.runETL_API(TRANSFORM_RUN_FILTER_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));
        // run again, should be No Work. response status should be 'No Work'
        rtr = _etlHelper.runETL_API(TRANSFORM_RUN_FILTER_SP);
        assertEquals("No work", rtr.getStatus());
        // insert new transfer and run again to test filterRunId was persisted properly for next run
        _etlHelper.insertTransferRow("143", _etlHelper.getDate(), _etlHelper.getDate(), "new transfer", "added by test automation", "pending");
        rtr = _etlHelper.runETL_API(TRANSFORM_RUN_FILTER_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));

        // test mode 7, ModifiedSinceFilterStrategy specified in the xml. Require filterStart & End timestamp input parameters to be populated
        rtr = _etlHelper.runETL_API(TRANSFORM_MODIFIED_FILTER_NO_SOURCE_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));
        // run again to test filterStartTimeStamp & filterEndTimeStamp was persisted properly for next run
        rtr = _etlHelper.runETL_API(TRANSFORM_MODIFIED_FILTER_NO_SOURCE_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));

        // Now test Modified where source is specified
        _etlHelper.insertSourceRow("111", "Sproc Subject 1", "142");
        rtr = _etlHelper.runETL_API(TRANSFORM_MODIFIED_FILTER_WITH_SOURCE_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));
        // run again, should be no work
        rtr = _etlHelper.runETL_API(TRANSFORM_MODIFIED_FILTER_WITH_SOURCE_SP);
        assertEquals("No work", rtr.getStatus());
        // insert another source row, should have work
        _etlHelper.insertSourceRow("112", "Sproc Subject 2", "143");
        rtr = _etlHelper.runETL_API(TRANSFORM_MODIFIED_FILTER_WITH_SOURCE_SP);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));

        // Bad source name
        try
        {
            _etlHelper.runETL_API(TRANSFORM_BAD_MODIFIED_FILTER_WITH_BAD_SOURCE_SP);
        }
        catch (CommandException e)
        {
            assertTrue("Incorrect exception message on bad source: ", e.getMessage().startsWith("Could not find table"));
            assertEquals("ERROR", _diHelper.getTransformStatusByTransformId(TRANSFORM_BAD_MODIFIED_FILTER_WITH_BAD_SOURCE_SP));
        }

        //Stored proc result set ETL
        _etlHelper.runETL("SProcResultSet");
        _etlHelper.assertInTarget2("Sproc Subject 1", "Sproc Subject 2");
    }

    @Test
    public void testPipelineFileAnalysisTask() throws Exception
    {
        doPipelineFileAnalysis("targetFile", null);
    }

    /**
     * Test queueing one etl from another. Uses pipeline file analysis as that's most relevant for sponsoring client.
     */
    @Test
    public void testQueueAnotherEtl() throws Exception
    {
        doPipelineFileAnalysis("targetFileQueueTail", ETL_OUT);
    }

    private void doPipelineFileAnalysis(String etl, @Nullable String outputSubDir) throws Exception
    {
        File dir = setupPipelineFileAnalysis();
        if (null != outputSubDir)
        {
            dir = new File(dir, outputSubDir);
        }
        String jobId = _etlHelper.runETL_API(etl).getJobId();
        validatePipelineFileAnalysis(dir, jobId);
    }

    private void validatePipelineFileAnalysis(File dir, String jobId) throws IOException, CommandException
    {
        String baseName = "report-" + _diHelper.getTransformRunFieldByJobId(jobId, "transformRunId");
        File etlFile = new File(dir, baseName + ".testIn.tsv");
        String fileContents = TestFileUtils.getFileContents(etlFile);
        String[] rows = fileContents.split("[\\n\\r]+");
        String expected = WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL ?
                "rowid,container,created,modified,id,name,transformrun,rowversion,diTransformRunId,diModified"
                : "rowid,container,created,modified,id,name,transformrun,diTransformRunId,diModified";
        assertEquals("ETL output file did not contain header", expected, rows[0]);
        assertEquals("First row was not for 'Subject 2'", "Subject 2", rows[1].split(",")[5]);
        assertEquals("Second row was not for 'Subject 3'", "Subject 3", rows[2].split(",")[5]);
        //file created by external pipeline
        File etlFile2 = new File(dir, baseName + ".testOut.tsv");
        fileContents = TestFileUtils.getFileContents(etlFile2);
        rows = fileContents.split("[\\n\\r]+");
        assertEquals("First row was not for 'Subject 2'", "Subject 2", rows[1].split(",")[5]);
        assertEquals("Second row was not for 'Subject 3'", "Subject 3", rows[2].split(",")[5]);
    }

    @NotNull
    private File setupPipelineFileAnalysis() throws IOException
    {
        _etlHelper.insertSourceRow("2", "Subject 2", null);
        _etlHelper.insertSourceRow("3", "Subject 3", null);
        //file ETL output and external pipeline test
        File dir = TestFileUtils.getTestTempDir();
        FileUtils.deleteDirectory(dir);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        setPipelineRoot(dir.getAbsolutePath());
        return dir;
    }

    /**
     * Test that jobs are serialized correctly so they can be requeued. Using "retry" as standin for requeue on server
     * restart, as it is the same mechanism. For each case, deliberately create an error condition (key violation, etc),
     * and then retry the job. If the same error occurs, the job requeued successfully. (Check the initial error now appears twice
     * in log.)
     */
    @Test
    public void testRequeueJobs() throws Exception
    {
        String FILTER_ERROR_MESSAGE;
        String SPROC_ERROR_MESSAGE = "ERROR: ";
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL)
        {
            FILTER_ERROR_MESSAGE = "violates unique constraint";
            SPROC_ERROR_MESSAGE += "ERROR: ";
        }
        else
            FILTER_ERROR_MESSAGE = "Violation of UNIQUE KEY constraint";
        SPROC_ERROR_MESSAGE += "Intentional SQL Exception From Inside Proc";

        _etlHelper.insertSourceRow("2", "Subject 2", "1042");

        // SelectAllFilter
        _etlHelper.runETL_API(APPEND_SELECT_ALL);
        tryAndRetry(APPEND_SELECT_ALL, FILTER_ERROR_MESSAGE, true);
        // 23833 Validate that a successful retry gives a status of COMPLETE
        _etlHelper.deleteAllRows(ETLHelper.ETL_TARGET);
        _etlHelper.clickRetryButton();
        assertEquals("Wrong transform status for retried ETL.", ETLHelper.COMPLETE, _diHelper.getTransformStatusByTransformId(_etlHelper.ensureFullIdString(APPEND_SELECT_ALL)));
        ETLScheduler scheduler = ETLScheduler.beginAt(this);
        assertEquals("Error status didn't clear on successful retry for " + APPEND_SELECT_ALL, "COMPLETE", scheduler.transform(APPEND_SELECT_ALL).getLastStatus());

        // ModifiedSinceFilter
        tryAndRetry(APPEND, FILTER_ERROR_MESSAGE, true);

        //RunFilter
        _etlHelper.insertTransferRow("1042", _etlHelper.getDate(), _etlHelper.getDate(), "new transfer", "added by test automation", "pending");
        tryAndRetry(TRANSFORM_BYRUNID, FILTER_ERROR_MESSAGE, true);

        //StoredProc
        tryAndRetry(TRANSFORM_BAD_THROW_ERROR_SP, SPROC_ERROR_MESSAGE, false);

        // Remote Source
        tryAndRetry("remoteInvalidDestinationSchemaName", "ERROR: Target schema not found: study_buddy", true);

        // TaskRef (serialization failure only occurred when taskref task had not yet started)
        tryAndRetry("appendAndTaskRefTask", FILTER_ERROR_MESSAGE, true);
    }

    /**
     * Test requeuing an etl job with a pipeline task in it. Doing this one a little differently than other requeue tests.
     * Cancel the job while it's running, and then on retry verify the job ran to completion.
     *
     */
    @Test
    public void testRetryCancelledPipelineTask() throws IOException, CommandException
    {
        final String TARGET_FILE_WITH_SLEEP = _etlHelper.ensureFullIdString("targetFileWithSleep");
        File dir = new File(setupPipelineFileAnalysis(), ETL_OUT);
        _etlHelper.runETLNoNavNoWait(TARGET_FILE_WITH_SLEEP, true, false);
        refresh();
        clickButton("Cancel");
        // 23829 Validate run status is really CANCELLED
        _etlHelper.waitForStatus(TARGET_FILE_WITH_SLEEP, "CANCELLED", 10000);
        clickTab("DataIntegration");
        ETLScheduler scheduler = new ETLScheduler(this);
        assertEquals("Wrong status for " + TARGET_FILE_WITH_SLEEP, "CANCELLED", scheduler.transform(TARGET_FILE_WITH_SLEEP).getLastStatus());
        scheduler.transform(TARGET_FILE_WITH_SLEEP).clickLastStatus();
        _etlHelper.clickRetryButton();
        _etlHelper.waitForEtl();
        scheduler = ETLScheduler.beginAt(this);
        assertEquals("Wrong status for " + TARGET_FILE_WITH_SLEEP, "COMPLETE", scheduler.transform(TARGET_FILE_WITH_SLEEP).getLastStatus());
        scheduler.transform(TARGET_FILE_WITH_SLEEP).clickLastStatus();
        String jobId = _diHelper.getTransformRunFieldByTransformId(TARGET_FILE_WITH_SLEEP, "jobId");
        validatePipelineFileAnalysis(dir, jobId);
    }

    @Test
    public void testColumnMapping()
    {
        _etlHelper.insertSourceRow("id1", "name1", null);
        _etlHelper.runETL("appendMappedColumns");
        Map<String, Object> result = executeSelectRowCommand("vehicle", "etl_target").getRows().get(0);
        // The id and name fields should have been swapped by the mapping in the etl
        assertEquals("Wrong mapped value for id field", "name1", result.get("id"));
        assertEquals("Wrong mapped value for name field", "id1", result.get("name"));
    }

    @Test
    public void testSaveStateMidJob() throws Exception
    {
        final String ETL = _etlHelper.ensureFullIdString("saveStateMidJobWithSleep");
        _etlHelper.runETL_API(ETL, false);
        _etlHelper.waitForStatus(ETL, "PENDING", 3000);
        String state = _diHelper.getTransformState(ETL);
        assertTrue("Midjob state not saved", state.contains("after"));
        assertFalse("Midjob state shouldn't have later step parameters", state.contains("setting1"));
        _etlHelper.waitForStatus(ETL, ETLHelper.COMPLETE, 12000);
        state = _diHelper.getTransformState(ETL);
        assertTrue("State not persisted after job", state.contains("after"));
        assertTrue("State not persisted after job", state.contains("\"setting1\":\"test\""));
    }

    /**
     *  Test persisting global in/out parameters, and chaining global input parameters.
     *
     */
    @Test
    public void testStoredProcGlobalParams() throws Exception
    {
        RunTransformResponse rtr = _etlHelper.runETL_API("SprocGlobalParameters");
        assertEquals("Wrong transform status from using stored proc global parameters.", ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));
    }

    @Test
    public void testGatingStoredProc() throws Exception
    {
        final String SPROC_GATE = "SProcGate";
        RunTransformResponse rtr = _etlHelper.runETL_API(SPROC_GATE);
        assertEquals(ETLHelper.COMPLETE, _diHelper.getTransformStatus(rtr.getJobId()));
        rtr = _etlHelper.runETL_API(SPROC_GATE);
        assertEquals("Stored proc failed to gate job.", "NO WORK", rtr.getStatus().toUpperCase());

    }

    @Test
    public void testTruncateAndReset() throws Exception
    {
        _etlHelper.insertSourceRow("trunc111", "Truncate me 1", null);
        _etlHelper.insertSourceRow("trunc222", "Truncate me 2", null);
        _etlHelper.runETL_API(APPEND_SELECT_ALL);
        ETLScheduler scheduler = ETLScheduler.beginAt(this);
        scheduler.transform(APPEND_SELECT_ALL)
                .truncateAndReset()
                .confirmYes();

        _etlHelper.assertNotInTarget1("trunc111", "trunc222", "Truncate me");
    }

    private void tryAndRetry(String transformId, String expectedError, boolean normalErrorCount)
    {
        _etlHelper.runETLNoNav(transformId, true, false);
        _etlHelper.incrementExpectedErrorCount(normalErrorCount);
        refresh();
        assertTextPresent(expectedError);
        _etlHelper.clickRetryButton();
        _etlHelper.incrementExpectedErrorCount(normalErrorCount);
        refresh();
        assertTextPresent(expectedError, 2);
    }
}
