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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.security.DeleteContainerCommand;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.util.SiteSettingsHelper;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * GH Issue 1707: an assay import must not hold a database transaction while its transform script runs, or a script
 * that calls back into the server can deadlock with a concurrent folder delete.
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
    private static final String CALLBACK_DATA = "ParticipantID\tVisitID\nP1\t1\n";
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
        String folderPath = createFolderWithDataClass("Sync Delete");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            Future<ImportRunResponse> importFuture = executor.submit(() -> importRun(_syncAssayId, SCRIPT_SLEEP_SECONDS));
            sleep(5_000);
            assertFalse("Import finished before the transform script's sleep ended", importFuture.isDone());

            deleteFolder(folderPath);
            assertFalse("Folder delete should finish while the transform script is still running", importFuture.isDone());

            ImportRunResponse response = importFuture.get(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            SelectRowsResponse runs = selectRuns(SYNC_ASSAY, new Filter("RowId", response.getRunId()));
            assertEquals("Runs with RowId " + response.getRunId(), 1, runs.getRowCount().intValue());
            assertEquals("Run property set by the transform script", "succeeded", runs.getRows().getFirst().get(CALLBACK_STATUS));
            assertEquals("Run comment set by the transform script", TRANSFORM_COMMENT, runs.getRows().getFirst().get("Comments"));
        }
        finally
        {
            executor.shutdownNow();
        }
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
    public void testTransformScriptTimeout() throws Exception
    {
        SITE_SETTINGS.setSetting(ASSAY_TRANSFORM_SCRIPT_TIMEOUT, 1);

        try
        {
            importRun(_syncAssayId, 90);
            fail("Import should fail when the transform script exceeds the timeout");
        }
        catch (CommandException e)
        {
            assertTrue("Unexpected error: " + e.getMessage(), e.getMessage().contains("Process killed after exceeding timeout"));
        }
        finally
        {
            // Restore before the other tests in this class, whose scripts can run close to a minute
            SITE_SETTINGS.restoreSettings();
        }
    }

    private int createCallbackAssay(String name, boolean backgroundUpload)
    {
        try
        {
            int protocolId = new GeneralAssayDesign(name)
                    .setBatchFields(List.of(), false)
                    .setRunFields(List.of(new FieldDefinition(SLEEP_SECONDS, FieldDefinition.ColumnType.Integer), new FieldDefinition(CALLBACK_STATUS)), true)
                    .addProtocolTransformer(protocol -> protocol.setBackgroundUpload(backgroundUpload))
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
        File dataFile = new File(TestFileUtils.ensureTestTempDir(getClass().getSimpleName()), "callbackData.tsv");
        TestFileUtils.writeFile(dataFile, CALLBACK_DATA);
        ImportRunCommand command = new ImportRunCommand(assayId, dataFile);
        command.setProperties(Map.of(SLEEP_SECONDS, sleepSeconds, CALLBACK_STATUS, "pending"));
        command.setTimeout(REQUEST_TIMEOUT_MS);
        return command.execute(createDefaultConnection(), getProjectName());
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
}
