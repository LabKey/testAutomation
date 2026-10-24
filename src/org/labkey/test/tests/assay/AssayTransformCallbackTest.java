/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.tests.assay;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.function.ThrowingRunnable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.assay.Batch;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.remoteapi.assay.Run;
import org.labkey.remoteapi.assay.SaveAssayBatchCommand;
import org.labkey.remoteapi.assay.SaveAssayBatchResponse;
import org.labkey.remoteapi.assay.SaveAssayRunsCommand;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.security.DeleteContainerCommand;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.SiteSettingsHelper;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * GH Issue 1707, GH Issue 1720: an assay import must not hold a database transaction while its transform script runs,
 * or a script that calls back into the server can deadlock with a concurrent folder delete. Files saved before the
 * script runs must still have their audit events recorded in the import's transaction.
 */
@Category({Assays.class, Daily.class})
public class AssayTransformCallbackTest extends AbstractAssayTransformTest
{
    private static final File CALLBACK_SCRIPT = TestFileUtils.getSampleData("qc/assayTransformCallback.R");
    private static final String SYNC_ASSAY = "Sync Callback Assay";
    private static final String BACKGROUND_ASSAY = "Background Callback Assay";
    private static final String SLEEP_SECONDS = "SleepSeconds";
    private static final String CALLBACK_STATUS = "CallbackStatus";
    private static final String TRANSFORM_COMMENT = "set by transform";
    private static final String WORK_DIRS_LIST = "TransformWorkDirs";
    private static final String WORK_DIR = "WorkDir";
    private static final String CALLBACK_DATA = "ParticipantID\tVisitID\nP1\t1\n";
    private static final String SCRIPT_REJECTED = "Transform script rejected the run";
    // The callback script's "edit" status updates the visit of this participant's results
    private static final String EDITED_PARTICIPANT = "EditedByScript";
    private static final String ASSAY_TRANSFORM_SCRIPT_TIMEOUT = "assayTransformScriptTimeout";
    private static final int SCRIPT_SLEEP_SECONDS = 45;
    private static final int REQUEST_TIMEOUT_MS = (int) TimeUnit.MINUTES.toMillis(3);

    // Static so a setting changed in one test method is restored by doCleanup on a later instance
    private static final SiteSettingsHelper SITE_SETTINGS = new SiteSettingsHelper();

    private static int _syncAssayId;
    private static int _backgroundAssayId;

    @Override
    protected void doSetup()
    {
        super.doSetup();
        _syncAssayId = createCallbackAssay(SYNC_ASSAY, false);
        _backgroundAssayId = createCallbackAssay(BACKGROUND_ASSAY, true);
        try
        {
            new IntListDefinition(WORK_DIRS_LIST, "Key")
                    .setFields(List.of(new FieldDefinition(WORK_DIR)))
                    .create(createDefaultConnection(), getProjectName());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        SITE_SETTINGS.restoreSettings();
        super.doCleanup(afterTest);
    }

    @Test
    public void testImportRunCallbackDuringFolderDelete() throws Exception
    {
        assertFolderDeleteDuringCallback("Sync Delete", () -> importRun(_syncAssayId, SCRIPT_SLEEP_SECONDS).getRunId());
    }

    @Test
    public void testSaveBatchCallbackDuringFolderDelete() throws Exception
    {
        assertFolderDeleteDuringCallback("Save Batch Delete", () ->
                saveBatch(batchOf(callbackRun(SCRIPT_SLEEP_SECONDS, "pending", "P1"))).getBatch().getRuns().getFirst().getId());
    }

    @Test
    public void testSaveRunsCallbackDuringFolderDelete() throws Exception
    {
        assertFolderDeleteDuringCallback("Save Runs Delete", () -> {
            SaveAssayRunsCommand command = new SaveAssayRunsByAssayIdCommand(_syncAssayId, List.of(callbackRun(SCRIPT_SLEEP_SECONDS, "pending", "P1")));
            command.setTimeout(REQUEST_TIMEOUT_MS);
            return command.execute(createDefaultConnection(), getProjectName()).getRuns().getFirst().getId();
        });
    }

    @Test
    public void testSaveBatchScriptFailureSavesNothing() throws Exception
    {
        int runsBefore = selectRuns(SYNC_ASSAY, null).getRowCount().intValue();
        int workDirsBefore = selectWorkDirs().size();

        assertSaveRejected(batchOf(callbackRun(0, "report", "P1"), callbackRun(0, "fail", "P2")), SCRIPT_REJECTED);

        assertEquals("Runs after the second run's script failed", runsBefore, selectRuns(SYNC_ASSAY, null).getRowCount().intValue());
        List<String> allWorkDirs = selectWorkDirs();
        List<String> workDirs = allWorkDirs.subList(workDirsBefore, allWorkDirs.size());
        assertEquals("Transform script working directories reported", 2, workDirs.size());
        if (!TestProperties.isServerRemote())
        {
            for (String workDir : workDirs)
                assertFalse("Transform script working directory should be deleted: " + workDir, new File(workDir).exists());
        }
    }

    @Test
    public void testSaveBatchUpdatesExistingRun() throws Exception
    {
        Batch batch = saveBatch(batchOf(callbackRun(0, "pending", "P1"))).getBatch();
        int runId = batch.getRuns().getFirst().getId();

        // A failing script elsewhere in the request must leave the existing run's results alone
        assertSaveRejected(batchOf(batch.getId(), existingRun(runId, "pending", "P2"), callbackRun(0, "fail", "P3")), SCRIPT_REJECTED);
        assertEquals("Results after a failed update", List.of("P1"), selectResultParticipants(runId));

        int workDirsBefore = selectWorkDirs().size();
        saveBatch(batchOf(batch.getId(), existingRun(runId, "report", "P2")));
        assertEquals("Results after the update", List.of("P2"), selectResultParticipants(runId));
        assertEquals("Working directories reported by the update's transform script", workDirsBefore + 1, selectWorkDirs().size());
        SelectRowsResponse runs = selectRuns(SYNC_ASSAY, new Filter("RowId", runId));
        assertEquals("Run property set by the update's transform script", "succeeded", runs.getRows().getFirst().get(CALLBACK_STATUS));
    }

    @Test
    public void testSaveBatchUpdateReusingStoredResults() throws Exception
    {
        Batch batch = saveBatch(batchOf(callbackRun(0, "pending", "P1"))).getBatch();
        int runId = batch.getRuns().getFirst().getId();
        int workDirsBefore = selectWorkDirs().size();

        // The server compares the stored results its script saw with a read inside the transaction; unchanged ones must match
        saveBatch(batchOf(batch.getId(), existingRunWithoutResults(runId, "report")));

        assertEquals("Working directories reported by the update's transform script", workDirsBefore + 1, selectWorkDirs().size());
        assertEquals("Results after the update", List.of("P1"), selectResultParticipants(runId));
    }

    @Test
    public void testSaveBatchRunChangedDuringScript() throws Exception
    {
        Batch batch = saveBatch(batchOf(callbackRun(0, "pending", EDITED_PARTICIPANT))).getBatch();
        int runId = batch.getRuns().getFirst().getId();

        assertSaveRejected(batchOf(batch.getId(), existingRunWithoutResults(runId, "edit")), "changed while its transform scripts ran");

        assertEquals("Visits after the script edited the run's results", List.of(2), selectResultVisits(runId));
    }

    @Test
    public void testSaveDuplicateRunRejected() throws Exception
    {
        Batch batch = saveBatch(batchOf(callbackRun(0, "pending", "P1"))).getBatch();
        int runId = batch.getRuns().getFirst().getId();
        int workDirsBefore = selectWorkDirs().size();
        String duplicateError = "Run " + runId + " appears more than once";

        assertSaveRejected(batchOf(batch.getId(), existingRun(runId, "report", "P2"), existingRun(runId, "report", "P3")), duplicateError);

        SaveAssayRunsCommand command = new SaveAssayRunsByAssayIdCommand(_syncAssayId, List.of(existingRun(runId, "report", "P2"), existingRun(runId, "report", "P3")));
        command.setTimeout(REQUEST_TIMEOUT_MS);
        assertRejected(() -> command.execute(createDefaultConnection(), getProjectName()), duplicateError);

        assertEquals("Results after the rejected saves", List.of("P1"), selectResultParticipants(runId));
        assertEquals("Working directories reported by rejected saves", workDirsBefore, selectWorkDirs().size());
    }

    @Test
    public void testSaveBatchMissingBatchRunsNoScripts() throws Exception
    {
        int runsBefore = selectRuns(SYNC_ASSAY, null).getRowCount().intValue();
        int workDirsBefore = selectWorkDirs().size();

        assertSaveRejected(batchOf(Integer.MAX_VALUE, callbackRun(0, "report", "P1")), "Could not find assay batch");

        assertEquals("Runs after saving to a missing batch", runsBefore, selectRuns(SYNC_ASSAY, null).getRowCount().intValue());
        assertEquals("Working directories reported for a missing batch", workDirsBefore, selectWorkDirs().size());
    }

    @Test
    public void testBackgroundImportCallbackDuringFolderDelete() throws Exception
    {
        String folderPath = createFolderWithDataClass("Background Delete");

        // ImportRunResponse requires a runId, which background imports don't return, so import through the wizard
        beginAt(WebTestHelper.buildURL("assay", getProjectName(), "uploadWizard", Map.of("rowId", _backgroundAssayId)));
        AssayImportPage importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText(SLEEP_SECONDS, String.valueOf(SCRIPT_SLEEP_SECONDS));
        importPage.setNamedInputText(CALLBACK_STATUS, "pending");
        importPage.setDataText(CALLBACK_DATA);
        importPage.clickSaveAndFinish();
        waitFor(() -> isActiveAndStarted(getJobStatus()), "Background import job didn't start", REQUEST_TIMEOUT_MS);

        deleteFolder(folderPath);
        String status = getJobStatus();
        assertTrue("Folder delete should finish while the transform script is still running, but job status is " + status, isActiveAndStarted(status));
        assertEquals("Runs before the background import finished", 0, selectRuns(BACKGROUND_ASSAY, null).getRowCount().intValue());

        goToDataPipeline();
        waitForPipelineJobsToComplete(1, false);
        SelectRowsResponse runs = selectRuns(BACKGROUND_ASSAY, null);
        assertEquals("Background import runs", 1, runs.getRowCount().intValue());
        assertEquals("Run property set by the transform script", "succeeded", runs.getRows().getFirst().get(CALLBACK_STATUS));
        assertEquals("Run comment set by the transform script", TRANSFORM_COMMENT, runs.getRows().getFirst().get("Comments"));
    }

    @Test
    public void testBackgroundImportFromDataRows() throws Exception
    {
        // Own folder so its pipeline job doesn't count against the project's
        _containerHelper.createSubfolder(getProjectName(), "Data Rows");
        String folderPath = getProjectName() + "/Data Rows";
        beginAt(WebTestHelper.buildURL("project", folderPath, "begin"));

        Map<?, ?> rejected = importDataRowsFromBrowser(folderPath, false);
        assertTrue("Unexpected response: " + rejected, String.valueOf(rejected.get("error")).contains("saveDataAsFile=false"));

        Map<?, ?> queued = importDataRowsFromBrowser(folderPath, null);
        assertTrue("Expected a background job: " + queued, queued.get("jobId") != null);

        SelectRowsResponse runs = waitFor(() -> {
            try
            {
                SelectRowsResponse response = selectRuns(BACKGROUND_ASSAY, null, folderPath);
                return response.getRowCount().intValue() > 0 ? response : null;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }, "Background import from dataRows didn't create a run", REQUEST_TIMEOUT_MS);
        assertEquals("Run property set by the transform script", "succeeded", runs.getRows().getFirst().get(CALLBACK_STATUS));
    }

    @Test
    public void testImportRunFileAuditInImportTransaction() throws Exception
    {
        File dataFile = writeDataFile("auditImportRun.tsv");
        importRun(_syncAssayId, dataFile, 0, "pending", null);

        Map<String, Object> audit = selectOnlyFileAudit(dataFile);
        assertEquals("Transaction of the data file's audit event", new AuditLogHelper(this).getLastTransactionId(getProjectName()), audit.get("TransactionId"));
    }

    @Test
    public void testUploadWizardFileAuditInImportTransaction() throws Exception
    {
        File dataFile = writeDataFile("auditUploadWizard.tsv");
        beginAt(WebTestHelper.buildURL("assay", getProjectName(), "uploadWizard", Map.of("rowId", _syncAssayId)));
        AssayImportPage importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText(SLEEP_SECONDS, "0");
        importPage.setNamedInputText(CALLBACK_STATUS, "pending");
        importPage.setDataFile(dataFile);
        importPage.clickSaveAndFinish();

        Map<String, Object> audit = selectOnlyFileAudit(dataFile);
        assertEquals("Transaction of the data file's audit event", new AuditLogHelper(this).getLastTransactionId(getProjectName()), audit.get("TransactionId"));
    }

    @Test
    public void testFailedImportSkipsAuditOfDeletedFile() throws Exception
    {
        File dataFile = writeDataFile("auditFailedImport.tsv");
        assertImportRejected(dataFile, null);

        // The failed import deletes its data file, so an audit event would name a file that no longer exists
        assertEquals("Audit events for the failed import's data file", List.of(), selectFileAudits(dataFile));
    }

    @Test
    public void testFailedReRunAuditsKeptFile() throws Exception
    {
        int runId = importRun(_syncAssayId, writeDataFile("auditOriginalRun.tsv"), 0, "pending", null).getRunId();
        File reRunFile = writeDataFile("auditFailedReRun.tsv");
        assertImportRejected(reRunFile, runId);

        // A failed re-run keeps its data file, but the transaction its audit event was held for rolled back
        Map<String, Object> audit = selectOnlyFileAudit(reRunFile);
        assertNull("Transaction of the kept file's audit event", audit.get("TransactionId"));
        if (!TestProperties.isServerRemote())
        {
            File savedFile = new File((String) audit.get(AuditLogHelper.COL_FILE_AUDIT_DIRECTORY), (String) audit.get(AuditLogHelper.COL_FILE_AUDIT_FILE));
            assertTrue("Failed re-run should keep its data file: " + savedFile, savedFile.exists());
        }
    }

    @Test
    public void testTransformScriptTimeout() throws Exception
    {
        SITE_SETTINGS.setSetting(ASSAY_TRANSFORM_SCRIPT_TIMEOUT, 1);

        try
        {
            assertRejected(() -> importRun(_syncAssayId, 90), "Process killed after exceeding timeout");
        }
        finally
        {
            // Restore before the other tests in this class, whose scripts can run close to a minute
            SITE_SETTINGS.restoreSettings();
        }
    }

    private void assertFolderDeleteDuringCallback(String folderName, Callable<Integer> save) throws Exception
    {
        String folderPath = createFolderWithDataClass(folderName);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            Future<Integer> saveFuture = executor.submit(save);
            sleep(5_000);
            assertStillRunning(saveFuture, "Save finished before the transform script's sleep ended");

            deleteFolder(folderPath);
            assertStillRunning(saveFuture, "Folder delete should finish while the transform script is still running");

            int runId = saveFuture.get(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            SelectRowsResponse runs = selectRuns(SYNC_ASSAY, new Filter("RowId", runId));
            assertEquals("Runs with RowId " + runId, 1, runs.getRowCount().intValue());
            assertEquals("Run property set by the transform script", "succeeded", runs.getRows().getFirst().get(CALLBACK_STATUS));
            assertEquals("Run comment set by the transform script", TRANSFORM_COMMENT, runs.getRows().getFirst().get("Comments"));
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    /** A save that already failed rethrows its own error instead of failing on timing. */
    private static void assertStillRunning(Future<?> future, String message) throws Exception
    {
        if (future.isDone())
            future.get();
        assertFalse(message, future.isDone());
    }

    private int createCallbackAssay(String name, boolean backgroundUpload)
    {
        try
        {
            int protocolId = new GeneralAssayDesign(name)
                    .setBatchFields(List.of(), false)
                    .setRunFields(List.of(new FieldDefinition(SLEEP_SECONDS, FieldDefinition.ColumnType.Integer), new FieldDefinition(CALLBACK_STATUS)), true)
                    .addProtocolTransformer(protocol -> {
                        protocol.setBackgroundUpload(backgroundUpload);
                        protocol.setEditableResults(true);
                    })
                    .createAssay(getProjectName(), createDefaultConnection())
                    .getProtocolId();
            ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolId, "general", "")
                    .addTransformScript(CALLBACK_SCRIPT)
                    .clickSave();
            return protocolId;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private String createFolderWithDataClass(String folderName) throws Exception
    {
        _containerHelper.createSubfolder(getProjectName(), folderName);
        String folderPath = getProjectName() + "/" + folderName;
        new DataClassDefinition("Callback Data Class")
                .setFields(List.of(new FieldDefinition("Comment")))
                .create(createDefaultConnection(), folderPath)
                .addCustomRow(Map.of("Name", "D1", "Comment", "deleted with its folder"))
                .insertRows();
        return folderPath;
    }

    private void deleteFolder(String folderPath) throws Exception
    {
        DeleteContainerCommand command = new DeleteContainerCommand();
        command.setTimeout(REQUEST_TIMEOUT_MS);
        command.execute(createDefaultConnection(), folderPath);
    }

    private ImportRunResponse importRun(int assayId, int sleepSeconds) throws Exception
    {
        return importRun(assayId, writeDataFile("callbackData.tsv"), sleepSeconds, "pending", null);
    }

    private ImportRunResponse importRun(int assayId, File dataFile, int sleepSeconds, String callbackStatus, @Nullable Integer reRunId) throws Exception
    {
        ImportRunCommand command = new ImportRunCommand(assayId, dataFile)
        {
            @Override
            protected Map<String, Object> createParameterMap()
            {
                Map<String, Object> params = super.createParameterMap();
                if (reRunId != null)
                    params.put("reRunId", reRunId);
                return params;
            }
        };
        command.setProperties(Map.of(SLEEP_SECONDS, sleepSeconds, CALLBACK_STATUS, callbackStatus));
        command.setTimeout(REQUEST_TIMEOUT_MS);
        return command.execute(createDefaultConnection(), getProjectName());
    }

    private void assertImportRejected(File dataFile, @Nullable Integer reRunId)
    {
        assertRejected(() -> importRun(_syncAssayId, dataFile, 0, "fail", reRunId), SCRIPT_REJECTED);
    }

    private static void assertRejected(ThrowingRunnable request, String expectedError)
    {
        CommandException e = assertThrows("Request should fail with: " + expectedError, CommandException.class, request);
        assertTrue("Unexpected error: " + e.getMessage(), e.getMessage().contains(expectedError));
    }

    private File writeDataFile(String name) throws Exception
    {
        File dataFile = new File(TestFileUtils.ensureTestTempDir(getClass().getSimpleName()), name);
        TestFileUtils.writeFile(dataFile, CALLBACK_DATA);
        return dataFile;
    }

    private List<Map<String, Object>> selectFileAudits(File providedFile) throws Exception
    {
        return new AuditLogHelper(this).getAuditLogsFromLKS(getProjectName(), AuditLogHelper.AuditEvent.FILE_SYSTEM_EVENT,
                List.of("TransactionId", AuditLogHelper.COL_FILE_AUDIT_FILE, AuditLogHelper.COL_FILE_AUDIT_DIRECTORY),
                List.of(new Filter(AuditLogHelper.COL_FILE_AUDIT_PROVIDED_FILE, providedFile.getName())), null, ContainerFilter.CurrentAndSubfolders).getRows();
    }

    private Map<String, Object> selectOnlyFileAudit(File providedFile) throws Exception
    {
        List<Map<String, Object>> audits = selectFileAudits(providedFile);
        assertEquals("Audit events for " + providedFile.getName(), 1, audits.size());
        return audits.getFirst();
    }

    private static Run callbackRun(int sleepSeconds, String callbackStatus, String participantId)
    {
        Run run = new Run();
        run.setProperties(Map.of(SLEEP_SECONDS, sleepSeconds, CALLBACK_STATUS, callbackStatus));
        run.setResultData(List.of(Map.of("ParticipantID", participantId, "VisitID", 1)));
        return run;
    }

    private static Run existingRun(int runId, String callbackStatus, String participantId)
    {
        Run run = callbackRun(0, callbackStatus, participantId);
        run.setId(runId);
        return run;
    }

    /** Run always posts dataInputs, so the server replaces the run's content using its stored results. */
    private static Run existingRunWithoutResults(int runId, String callbackStatus)
    {
        Run run = new Run();
        run.setId(runId);
        run.setProperties(Map.of(SLEEP_SECONDS, 0, CALLBACK_STATUS, callbackStatus));
        return run;
    }

    private static Batch batchOf(Run... runs)
    {
        return batchOf(null, runs);
    }

    /** A null batchId saves a new batch. */
    private static Batch batchOf(@Nullable Integer batchId, Run... runs)
    {
        Batch batch = new Batch();
        if (batchId != null)
            batch.setId(batchId);
        batch.setRuns(List.of(runs));
        return batch;
    }

    private SaveAssayBatchResponse saveBatch(Batch batch) throws Exception
    {
        SaveAssayBatchCommand command = new SaveAssayBatchCommand(_syncAssayId, batch);
        command.setTimeout(REQUEST_TIMEOUT_MS);
        return command.execute(createDefaultConnection(), getProjectName());
    }

    private void assertSaveRejected(Batch batch, String expectedError)
    {
        assertRejected(() -> saveBatch(batch), expectedError);
    }

    private List<String> selectResultParticipants(int runId) throws Exception
    {
        return selectResultColumn(runId, "ParticipantID", String.class::cast);
    }

    private List<Integer> selectResultVisits(int runId) throws Exception
    {
        return selectResultColumn(runId, "VisitID", value -> ((Number) value).intValue());
    }

    private <T> List<T> selectResultColumn(int runId, String column, Function<Object, T> mapper) throws Exception
    {
        SelectRowsCommand command = new SelectRowsCommand("assay.General." + SYNC_ASSAY, "Data");
        command.setColumns(List.of(column));
        command.addFilter(new Filter("Run/RowId", runId));
        return command.execute(createDefaultConnection(), getProjectName()).getRows().stream()
                .map(row -> mapper.apply(row.get(column)))
                .toList();
    }

    private List<String> selectWorkDirs() throws Exception
    {
        SelectRowsCommand command = new SelectRowsCommand("lists", WORK_DIRS_LIST);
        command.setColumns(List.of(WORK_DIR));
        command.addSort("Key", Sort.Direction.ASCENDING);
        return command.execute(createDefaultConnection(), getProjectName()).getRows().stream()
                .map(row -> (String) row.get(WORK_DIR))
                .toList();
    }

    private Map<?, ?> importDataRowsFromBrowser(String folderPath, Boolean saveDataAsFile)
    {
        return executeAsyncScript("""
                const callback = arguments[arguments.length - 1];
                const config = {
                    assayId: arguments[0],
                    containerPath: arguments[1],
                    properties: { SleepSeconds: 0, CallbackStatus: 'pending' },
                    dataRows: [{ ParticipantID: 'P1', VisitID: 1 }],
                    success: response => callback({ jobId: response.jobId ?? null }),
                    failure: error => callback({ error: error.exception }),
                };
                if (arguments[2] !== null)
                    config.saveDataAsFile = arguments[2];
                LABKEY.Assay.importRun(config);
                """, Map.class, _backgroundAssayId, folderPath, saveDataAsFile);
    }

    private SelectRowsResponse selectRuns(String assayName, Filter filter) throws Exception
    {
        return selectRuns(assayName, filter, getProjectName());
    }

    private SelectRowsResponse selectRuns(String assayName, Filter filter, String folderPath) throws Exception
    {
        SelectRowsCommand command = new SelectRowsCommand("assay.General." + assayName, "Runs");
        command.setColumns(List.of("RowId", CALLBACK_STATUS, "Comments"));
        if (filter != null)
            command.addFilter(filter);
        return command.execute(createDefaultConnection(), folderPath);
    }

    private String getJobStatus()
    {
        try
        {
            SelectRowsCommand command = new SelectRowsCommand("pipeline", "Job");
            command.setColumns(List.of("Status"));
            SelectRowsResponse response = command.execute(createDefaultConnection(), getProjectName());
            assertTrue("Pipeline jobs in " + getProjectName(), response.getRowCount().intValue() <= 1);
            return response.getRows().isEmpty() ? null : (String) response.getRows().getFirst().get("Status");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static boolean isActiveAndStarted(String status)
    {
        return status != null && !status.endsWith("WAITING") && !List.of("COMPLETE", "ERROR", "CANCELLED").contains(status);
    }

    /** SaveAssayRunsCommand only identifies a protocol by name, which the server treats as a non-assay protocol */
    private static class SaveAssayRunsByAssayIdCommand extends SaveAssayRunsCommand
    {
        private final int _assayId;

        SaveAssayRunsByAssayIdCommand(int assayId, List<Run> runs)
        {
            super(null, runs);
            _assayId = assayId;
        }

        @Override
        public JSONObject getJsonObject()
        {
            JSONObject json = super.getJsonObject();
            json.put("assayId", _assayId);
            return json;
        }
    }
}
