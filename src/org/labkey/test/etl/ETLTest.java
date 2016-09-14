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
import org.labkey.api.util.Pair;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.ETL;
import org.labkey.test.pages.dataintegration.ETLScheduler;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, Data.class, ETL.class})
public class ETLTest extends ETLBaseTest
{
    public static final String ETL_OUT = "etlOut";
    private static final String DATA_INTEGRATION_TAB = "DataIntegration";

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
        final String TRANSFORM_QUERY_ERROR_NAME = "Error Bad Source Query";
        final String TRANSFORM_NOCOL_ERROR = "{simpletest}/SimpleETLCheckerErrorTimestampColumnNonexistent";
        final String TRANSFORM_BADCAST = "{simpletest}/badCast";
        final String TRANSFORM_BADTABLE = "{simpletest}/badTableName";

        final String NO_CHEESEBURGER_TABLE = "Could not find table: vehicle.etl_source_cheeseburger";

        goToProjectHome();
        clickTab(DATA_INTEGRATION_TAB);
        assertTextNotPresent("Should not have loaded invalid transform xml", "Error Missing Source");
        _etlHelper.insertSourceRow("0", "Subject 0", null);

        List<String> errors = new ArrayList<>();
        errors.add("AK_etltarget");
        errors.add("duplicate");
        errors.add("ERROR: Error running executeCopy");
        errors.add("org.labkey.api.pipeline.PipelineJobException: Error running executeCopy");
        _etlHelper.runETLandCheckErrors(TRANSFORM_KEYCONSTRAINT_ERROR, true, false, errors);
        errors.clear();

        errors.add(NO_CHEESEBURGER_TABLE);
        _etlHelper.runETLandCheckErrors(TRANSFORM_QUERY_ERROR, false, true, errors);
        // Verify we're showing error on the DataTransforms webpart
        clickTab(DATA_INTEGRATION_TAB);
        assertTextPresent(NO_CHEESEBURGER_TABLE);

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

        clickTab(DATA_INTEGRATION_TAB);
        _etlHelper.enableScheduledRun(TRANSFORM_QUERY_ERROR_NAME);
        //schedule for job is 15 seconds
        sleep(15000);
        _etlHelper.disableScheduledRun(TRANSFORM_QUERY_ERROR_NAME);
        clickTab("Portal");
        Assert.assertTrue(countText("ConfigurationException: " + NO_CHEESEBURGER_TABLE) > 1);
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
            _etlHelper.incrementExpectedErrorCount(false);
        }

        //Stored proc result set ETL
        _etlHelper.runETL("SProcResultSet");
        _etlHelper.assertInTarget2("Sproc Subject 1", "Sproc Subject 2");
    }

    private static final int DEFAULT_OUTPUT_ROWS = 3;

    @Test
    public void testPipelineFileAnalysisTask() throws Exception
    {
        doSingleFilePipelineFileAnalysis("targetFile", null, DEFAULT_OUTPUT_ROWS);
    }

    /**
     * Test setting an override to default pipeline parameter value via ETL setting.
     * This etl passes -n 2 to the tail command, so the header row should be missing in the output
     *
     */
    @Test
    public void testPipelineTaskParameterOverride() throws Exception
    {
        doSingleFilePipelineFileAnalysis("targetFileParameterOverride", null, 2);
    }

    /**
     * Test queueing one etl from another. Uses pipeline file analysis as that's most relevant for sponsoring client.
     */
    @Test
    public void testQueueAnotherEtl() throws Exception
    {
        doSingleFilePipelineFileAnalysis("targetFileQueueTail", ETL_OUT, DEFAULT_OUTPUT_ROWS);
    }

    private void doSingleFilePipelineFileAnalysis(String etl, @Nullable String outputSubDir, int expectedOutputRows) throws Exception
    {
        insertSingleFileAnalysisSourceData();
        File dir = setupPipelineFileAnalysis(outputSubDir);
        String jobId = _etlHelper.runETL_API(etl).getJobId();
        validatePipelineFileAnalysis(dir, jobId, expectedOutputRows);
    }

    private void validatePipelineFileAnalysis(File dir, String jobId, int expectedOutputRows) throws IOException, CommandException
    {
        Pair<List<String[]>, List<String[]>> fileRows = readFile(dir, jobId, null, true);
        String[] expected = WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL ?
                "rowid,container,created,modified,id,name,transformrun,rowversion,diTransformRunId,diModified".split(",")
                : "rowid,container,created,modified,id,name,transformrun,diTransformRunId,diModified".split(",");
        // Validate the initially written tsv file
        assertArrayEquals("ETL query output file did not contain header", expected, fileRows.first.get(0));
        validateFileRow(fileRows.first, 1, "Subject 2");
        validateFileRow(fileRows.first, 2, "Subject 3");

        // Validate the file output from the pipeline job
        assertEquals("ETL pipeline output file did not have expected number of rows.", expectedOutputRows, fileRows.second.size());
        validateFileRow(fileRows.second, expectedOutputRows - 2, "Subject 2");
        validateFileRow(fileRows.second, expectedOutputRows - 1, "Subject 3");
    }

    @NotNull
    private File setupPipelineFileAnalysis(String outputSubDir) throws IOException
    {
        //file ETL output and external pipeline test
        File dir = TestFileUtils.getTestTempDir();
        FileUtils.deleteDirectory(dir);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        setPipelineRoot(dir.getAbsolutePath());
        if (null != outputSubDir)
        {
            dir = new File(dir, outputSubDir);
        }
        return dir;
    }

    private void insertSingleFileAnalysisSourceData()
    {
        _etlHelper.insertSourceRow("2", "Subject 2", null);
        _etlHelper.insertSourceRow("3", "Subject 3", null);
    }

    private Pair<List<String[]>, List<String[]>> readFile(File dir, String jobId, @Nullable Integer batchNum, boolean expectOutFile) throws IOException, CommandException
    {
        Pair<List<String[]>, List<String[]>> results = new Pair<>(new ArrayList<>(), new ArrayList<>());

        String baseName = "report-" + _diHelper.getTransformRunFieldByJobId(jobId, "transformRunId");
        if (null != batchNum && batchNum > 0)
            baseName = baseName + "-" + batchNum;
        File etlFile = new File(dir, baseName + ".testIn.tsv");
        String fileContents = TestFileUtils.getFileContents(etlFile);

        List<String> rows = Arrays.asList(fileContents.split("[\\n\\r]+"));
        results.first.addAll(rows.stream().map(row -> row.split(",")).collect(Collectors.toList()));

        if (expectOutFile)
        {
            //file created by external pipeline
            File etlFile2 = new File(dir, baseName + ".testOut.tsv");
            fileContents = TestFileUtils.getFileContents(etlFile2);
            rows = Arrays.asList(fileContents.split("[\\n\\r]+"));
            results.second.addAll(rows.stream().map(row -> row.split(",")).collect(Collectors.toList()));
        }
        return results;
    }

    private void validateFileRow(List<String[]> rows, int index, String name)
    {
        // The 6th field in the tsv is the name column; verify it's what we expect
        assertEquals("Row " + index + " was not for name '" + name +"'", name, rows.get(index)[5]);
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
        String SPROC_ERROR_MESSAGE = "Intentional SQL Exception From Inside Proc";
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL)
        {
            FILTER_ERROR_MESSAGE = "violates unique constraint";
        }
        else
        {
            FILTER_ERROR_MESSAGE = "Violation of UNIQUE KEY constraint";
        }

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
        insertSingleFileAnalysisSourceData();
        File dir = setupPipelineFileAnalysis(ETL_OUT);
        _etlHelper.runETLNoNavNoWait(TARGET_FILE_WITH_SLEEP, true, false);
        refresh();
        clickButton("Cancel");
        // 23829 Validate run status is really CANCELLED
        _etlHelper.waitForStatus(TARGET_FILE_WITH_SLEEP, "CANCELLED", 10000);
        clickTab(DATA_INTEGRATION_TAB);
        ETLScheduler scheduler = new ETLScheduler(this);
        assertEquals("Wrong status for " + TARGET_FILE_WITH_SLEEP, "CANCELLED", scheduler.transform(TARGET_FILE_WITH_SLEEP).getLastStatus());
        scheduler.transform(TARGET_FILE_WITH_SLEEP).clickLastStatus();
        _etlHelper.clickRetryButton();
        _etlHelper.waitForEtl();
        scheduler = ETLScheduler.beginAt(this);
        assertEquals("Wrong status for " + TARGET_FILE_WITH_SLEEP, "COMPLETE", scheduler.transform(TARGET_FILE_WITH_SLEEP).getLastStatus());
        scheduler.transform(TARGET_FILE_WITH_SLEEP).clickLastStatus();
        String jobId = _diHelper.getTransformRunFieldByTransformId(TARGET_FILE_WITH_SLEEP, "jobId");
        validatePipelineFileAnalysis(dir, jobId, DEFAULT_OUTPUT_ROWS);
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

    @Test
    public void testBatchingToMultipleFiles() throws Exception
    {
        final String BATCH_FILES = "targetBatchedFiles";
        insertMultipleFilesSourceData();
        File dir = setupPipelineFileAnalysis(ETL_OUT);
        String jobId = _etlHelper.runETL_API(BATCH_FILES).getJobId();

        log("Validating output file count and content");
        Pair<List<String[]>, List<String[]>> fileRows = readFile(dir, jobId, 1, false);
        validateFileRow(fileRows.first, 1, "row 1");
        validateFileRow(fileRows.first, 2, "row 2");
        fileRows = readFile(dir, jobId, 2, false);
        validateFileRow(fileRows.first, 1, "row 3");
        validateFileRow(fileRows.first, 2, "row 4");
        fileRows = readFile(dir, jobId, 3, false);
        validateFileRow(fileRows.first, 1, "row 5");
    }

    @Test
    public void testBatchingMultipleFilesByBatchColumn() throws Exception
    {
        final String BATCH_FILES_WITH_BATCH_COLUMN = "targetBatchedFilesWithBatchColumn";
        insertMultipleFilesSourceData();
        File dir = setupPipelineFileAnalysis(ETL_OUT);
        String jobId = _etlHelper.runETL_API(BATCH_FILES_WITH_BATCH_COLUMN).getJobId();

        log("Validating output file count and content");
        Pair<List<String[]>, List<String[]>> fileRows = readFile(dir, jobId, 1, false);
        validateFileRow(fileRows.first, 1, "row 1");
        validateFileRow(fileRows.first, 2, "row 2");
        validateFileRow(fileRows.first, 3, "row 3");
        validateFileRow(fileRows.first, 4, "row 4");
        fileRows = readFile(dir, jobId, 2, false);
        validateFileRow(fileRows.first, 1, "row 5");
    }

    /**
     * Output to multiple files, and queue a pipeline job (tail the file) for each.
     * Verifies that we implicitly allowing of multiple queuing of etl's that start
     * with an external pipeline task as their first step.
     *
     */
    @Test
    public void testBatchingMultipleFilesQueuingMultipleTails() throws Exception
    {
        final String BATCH_FILES_QUEUE_TAIL = "targetBatchedFilesQueueTail";
        insertMultipleFilesSourceData();
        File dir = setupPipelineFileAnalysis(ETL_OUT);
        String jobId = _etlHelper.runETL_API(BATCH_FILES_QUEUE_TAIL).getJobId();

        // Just validate the final output files, the testBatchingMultipleFilesByBatchColumn test case already validated the intermediate files 
        log("Validating queued pipeline jobs output file count and content");
        Pair<List<String[]>, List<String[]>> fileRows = readFile(dir, jobId, 1, true);
        validateFileRow(fileRows.second, 1, "row 1");
        validateFileRow(fileRows.second, 2, "row 2");
        fileRows = readFile(dir, jobId, 2, true);
        validateFileRow(fileRows.second, 1, "row 3");
        validateFileRow(fileRows.second, 2, "row 4");
        fileRows = readFile(dir, jobId, 3, true);
        validateFileRow(fileRows.second, 1, "row 5");    
    }
    
    private void insertMultipleFilesSourceData()
    {
        _etlHelper.insertSourceRow("1", "row 1", "1");
        _etlHelper.insertSourceRow("2", "row 2", "1");
        _etlHelper.insertSourceRow("3", "row 3", "2");
        _etlHelper.insertSourceRow("4", "row 4", "2");
        _etlHelper.insertSourceRow("5", "row 5", "3");
    }

    @Test
    public void truncateWithoutDataTransfer() throws Exception
    {
        final String NAME = "row 1";
        _etlHelper.insertSourceRow("1", NAME, "1");
        _etlHelper.runETL_API(APPEND);
        _etlHelper.assertInTarget1(NAME);
        _etlHelper.runETL_API("truncateWithoutDataTransfer");
        _etlHelper.assertNotInTarget1(NAME);
    }

    @Test
    public void customContainerFilter() throws Exception
    {
        // This ETL xml uses a CurrentAndSubfolders containerFilter
        final String APPEND_CONTAINER_FILTER = "{simpletest}/appendContainerFilter";
        final String MY_ROW = "own row";
        final String CHILD_FOLDER = "child";
        _etlHelper.insertSourceRow("1", MY_ROW, "1");
        _containerHelper.createSubfolder(getProjectName(), CHILD_FOLDER);
        clickFolder(CHILD_FOLDER);
        new PortalHelper(this).addQueryWebPart("Source", ETLHelper.VEHICLE_SCHEMA, ETLHelper.ETL_SOURCE, null);
        final String CHILD_ROW = "child row";
        _etlHelper.insertSourceRow("2", CHILD_ROW, "1", CHILD_FOLDER);
        _etlHelper.runETL_API(APPEND_CONTAINER_FILTER);
        goToProjectHome();
        log("Validating ETL respects containerFilter.");
        _etlHelper.assertInTarget1(MY_ROW);
        _etlHelper.assertInTarget1(CHILD_ROW);
        final String CHILD_ROW_2 = "child row 2";
        _etlHelper.insertSourceRow("3", CHILD_ROW_2, "1", CHILD_FOLDER);
        _etlHelper.runETL_API(APPEND_CONTAINER_FILTER);
        goToProjectHome();
        log("Validating modifiedSinceFilterStrategy is containerFilter aware.");
        _etlHelper.assertInTarget1(CHILD_ROW_2);
    }

    @Test
    public void testBasicMerge() throws Exception
    {
        final String PREFIX = "Subject for merge test";
        final String MERGE_ETL = "{simpletest}/merge";
        _etlHelper.insertSourceRow("600", PREFIX + "1", null);
        _etlHelper.runETL_API(MERGE_ETL);
        // Check the ETL works at all
        _etlHelper.assertInTarget2(PREFIX + "1");
        _etlHelper.insertSourceRow("610", PREFIX + "2", null);
        final String newNameForRow1 = "newNameForRow1";
        _etlHelper.editSourceRow(0, null, newNameForRow1, null);
        _etlHelper.runETL_API(MERGE_ETL);

        // Check insert of new and update to existing
        _etlHelper.assertInTarget2(PREFIX + "2", newNameForRow1);
        // Check we really did UPDATE and not insert a new one
        _etlHelper.assertNotInTarget2(PREFIX + "1");
    }

    @Test
    public void testManyColumnMerge() throws Exception
    {
        // Mostly identical coverage as the basic case, but with > 100 columns.
        final String MERGE_ETL = "{simpletest}/mergeManyColumns";
        final String firstField5 = "55555";
        final String secondField5 = "66666";
        final String modifiedField5 = "77777";
        final String field180val = "180180";

        _etlHelper.do180columnSetup();
        Map<Integer, String> rowMap = new HashMap<>();
        rowMap.put(5, firstField5);
        _etlHelper.insert180columnsRow(rowMap);
        _etlHelper.runETL_API(MERGE_ETL);
        _etlHelper.assertIn180columnTarget(firstField5);
        rowMap.put(5, secondField5);
        _etlHelper.insert180columnsRow(rowMap);
        rowMap.put(5, modifiedField5);
        rowMap.put(180, field180val);
        _etlHelper.edit180columnsRow(0, rowMap);
        _etlHelper.runETL_API(MERGE_ETL);
        _etlHelper.assertIn180columnTarget(secondField5, modifiedField5, field180val);
        _etlHelper.assertNotIn180columnTarget(firstField5);
    }
}
