/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.test;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.util.Pair;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.assay.AssayConstants;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.UIAssayHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.params.FieldDefinition.DOMAIN_TRICKY_CHARACTERS;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class AssayAPITest extends BaseWebDriverTest
{
    protected final static File CREST_FILE = TestFileUtils.getSampleData("InlineImages/crest.png");
    protected final static File SCREENSHOT_FILE = TestFileUtils.getSampleData("InlineImages/screenshot.png");
    protected final static File FOO_XLS_FILE = TestFileUtils.getSampleData("InlineImages/foo.xls");
    protected final static File HELP_ICON_FILE = TestFileUtils.getSampleData("InlineImages/help.jpg");
    protected final static File CREST_2_FILE = TestFileUtils.getSampleData("InlineImages/crest-2.png");
    protected final static String SUBFOLDER_1 = "Sub1";

    @Override
    protected String getProjectName()
    {
        return "Assay API TEST";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        AssayAPITest initTest = getCurrentTest();
        initTest._containerHelper.createProject(initTest.getProjectName(), "Assay");
        initTest._containerHelper.createSubfolder(initTest.getProjectName(), SUBFOLDER_1, "Assay");
        initTest.goToProjectHome();

        initTest.log("upload inline files to the pipeline root");
        initTest.goToModule("FileContent");
        initTest._fileBrowserHelper.uploadFile(CREST_FILE);
        initTest._fileBrowserHelper.uploadFile(SCREENSHOT_FILE);
        initTest._fileBrowserHelper.uploadFile(FOO_XLS_FILE);
    }

    @Test
    public void testImportRun() throws Exception
    {
        goToProjectHome();
        int pipelineCount = 0;
        String runName = "trial01.xls";
        importAssayAndRun(TestFileUtils.getSampleData("AssayAPI/XLS Assay.xar.xml"), ++pipelineCount, "/" + getProjectName(),
                "XLS Assay", TestFileUtils.getSampleData("GPAT/" + runName), runName, new String[]{"K770K3VY-19"});
        // verify images are resolved and rendered properly
        assertElementPresent("Did not find the expected number of icons for images for " + CREST_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_FILE.getName() + "')]"), 100);
        waitForElement(Locator.paginationText(1, 100, 201));

        // Upload from child folder to parent folder
        goToProjectFolder(getProjectName(), SUBFOLDER_1);

        //Issue 16073
        importAssayAndRun(TestFileUtils.getSampleData("AssayAPI/BatchPropRequired.xar"), ++pipelineCount, "/" + getProjectName(),
                "BatchPropRequired", TestFileUtils.getSampleData("GPAT/" + runName), "trial01-1.xls", new String[]{"K770K3VY-19"});
        waitForElement(Locator.paginationText(1, 100, 201));
    }

    protected void importAssayAndRun(File assayPath, int pipelineCount, String container, String assayName, File runPath,
                                     String runName, String[] textToCheck) throws IOException, CommandException
    {
        // Issue 42637: Verify that .xar.xml file can be imported through the UI
        UIAssayHelper _uiAssayHelper = new UIAssayHelper(this);
        _uiAssayHelper.uploadXarFileAsAssayDesign(assayPath, pipelineCount, container);

        APIAssayHelper _apiAssayHelper = new APIAssayHelper(this);
        _apiAssayHelper.importAssay(assayName, runPath, getProjectName(), Collections.singletonMap(AssayConstants.PARTICIPANT_VISIT_RESOLVER_FIELD_NAME, "SampleInfo"));

        log("verify import worked");
        goToProjectHome();
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runName));
        assertTextPresent(textToCheck);
    }

    @Test
    public void testGpatAssayOverAPI() throws Exception
    {
        String assayName = "testGpatAssay";
        String assayDescription = "generated for test purposes over remoteAPI";

        Connection connection = createDefaultConnection();
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand("General");                      // gets a template from the server
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(connection, getCurrentContainerPath());

        Protocol newAssayProtocol = getProtocolResponse.getProtocol();
        newAssayProtocol.setName(assayName)
                .setDescription(assayDescription)
                .setQCEnabled(true)
                .setEditableResults(true)
                .setEditableRuns(true);
        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(newAssayProtocol);
        ProtocolResponse saveProtocolResponse = saveProtocolCommand.execute(connection, getCurrentContainerPath());
        Integer protocolId = saveProtocolResponse.getProtocol().getProtocolId();

        assertEquals(assayDescription, saveProtocolResponse.getProtocol().getDescription());
        assertTrue(saveProtocolResponse.getProtocol().getQcEnabled());
        assertTrue(saveProtocolResponse.getProtocol().getEditableResults());
        assertTrue(saveProtocolResponse.getProtocol().getEditableRuns());

        GetProtocolCommand protocolCommand = new GetProtocolCommand(protocolId);
        ProtocolResponse doubleCheckProtocolResponse = protocolCommand.execute(connection, getCurrentContainerPath());

        assertEquals(assayDescription, doubleCheckProtocolResponse.getProtocol().getDescription());
        assertTrue(doubleCheckProtocolResponse.getProtocol().getQcEnabled());
        assertTrue(doubleCheckProtocolResponse.getProtocol().getEditableResults());
        assertTrue(doubleCheckProtocolResponse.getProtocol().getEditableRuns());
    }

    // Issue 30003: support importing assay data relative to pipeline root
    @Test
    public void testImportRun_serverFilePath() throws Exception
    {
        goToProjectHome();

        String assayName = "GPAT-ImportRunApi" + DOMAIN_TRICKY_CHARACTERS;
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        int assayId = assayHelper.getIdFromAssayName(assayName, getProjectName(), false);
        if (assayId == 0)
            assayId = assayHelper.createAssayDesignWithDefaults(getProjectName(), "General", assayName).getProtocolId();

        // First, simulate file already being uploaded to the server by copying to the pipeline root
        List<String> lines1 = Arrays.asList(
                "ptid\tdate\n",
                "p01\t2017-05-10\n",
                "p02\t2017-05-10\n"
        );
        File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        Path relativePath1 = Paths.get("testImportRunFilePath", "results1.tsv");
        Path pipelinePath1 = createDataFile(fileRoot, relativePath1, lines1);

        // import the file using a relative path
        ImportRunResponse resp = assayHelper.importAssay(assayId, relativePath1.toString(), getProjectName(), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertTextPresent("p01", "p02");

        goToProjectHome();

        List<String> lines2 = Arrays.asList(
                "ptid\tdate\n",
                "p03\t2017-05-10\n",
                "p04\t2017-05-10\n"
        );
        Path relativePath2 = Paths.get("testImportRunFilePath", "results2.tsv");
        Path pipelinePath2 = createDataFile(fileRoot, relativePath2, lines2);

        // import the file using an absolute path
        resp = assayHelper.importAssay(assayId, pipelinePath2.toString(), getProjectName(), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertTextPresent("p03", "p04");

        // attempt to import file outside of pipeline root
        try
        {
            File runFilePath = TestFileUtils.getSampleData("GPAT/trial01.xls");
            assayHelper.importAssay(assayId, runFilePath.toString(), getProjectName(), Collections.emptyMap());
            fail("Expected exception trying to read file outside of pipeline root");
        }
        catch (CommandException ex)
        {
            assertTrue("Expected 'File not found', got: " + ex.getMessage(), ex.getMessage().contains("File not found"));
            assertTrue("Expected 'trial01.xls', got: " + ex.getMessage(), ex.getMessage().contains("trial01.xls"));
        }
    }

    public static Path createDataFile(File fileRoot, Path relativePath, Iterable<String> lines) throws IOException
    {
        Path pipelinePath = fileRoot.toPath().resolve(relativePath);
        if (!Files.isRegularFile(pipelinePath))
        {
            Files.createDirectories(pipelinePath.getParent());
            Files.write(pipelinePath, lines);
            if (!Files.isRegularFile(pipelinePath))
                fail("Failed to create file " + pipelinePath);
        }
        return pipelinePath;
    }

    private void createAssayWithFileFields(String assayName)
    {
        ReactAssayDesignerPage assayDesigner = _assayHelper.createAssayDesign("General", assayName);

        assayDesigner.setEditableRuns(true); // test updateRows.api

        log("Create a 'File' column for the assay run.");
        assayDesigner.goToRunFields()
                .addField("RunFileField")
                .setType(FieldDefinition.ColumnType.File)
                .setLabel("Run File Field");

        log("Create a 'File' column for the assay data.");
        assayDesigner.goToResultsFields()
                .addField("DataFileField")
                .setType(FieldDefinition.ColumnType.File)
                .setLabel("Data File Field");

        assayDesigner.clickFinish();
    }

    // Issue 22632: import runs into GPAT assay using LABKEY.Assay.importRun() API with data rows
    @Test
    public void testImportRun_dataRows() throws Exception
    {
        new ApiPermissionsHelper(this)
                .setSiteRoleUserPermissions(PasswordUtil.getUsername(), "See Absolute File Paths");

        goToProjectHome();

        log("create GPAT assay");
        String assayName = "GPAT-ImportRunApi-dataRows" + DOMAIN_TRICKY_CHARACTERS;
        createAssayWithFileFields(assayName);

        File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        Path fullPath = fileRoot.toPath().resolve("screenshot.png");

        List<Map<String, Object>> dataRows = Arrays.asList(
                Maps.of("ptid", "p01", "date", "2017-05-10", "DataFileField", "crest.png"),
                Maps.of("ptid", "p02", "date", "2017-05-10", "DataFileField", fullPath.toString())
        );

        // import the file using a relative path
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        int assayId = assayHelper.getIdFromAssayName(assayName, getProjectName());
        ImportRunResponse resp = assayHelper.importAssay(assayId, "x", dataRows, getProjectName(), Collections.singletonMap("RunFileField", "foo.xls"), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertTextPresent("p01", "p02");
        DataRegionTable table = new DataRegionTable("Data", this);
        table.clearAllFilters(); // remove run filter

        // verify images are resolved and rendered properly
        assertElementPresent("Did not find the expected number of icons for images for " + CREST_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + SCREENSHOT_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + SCREENSHOT_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + FOO_XLS_FILE.getName() + " from the runs.", Locator.xpath("//a[contains(text(), '" + FOO_XLS_FILE.getName() + "')]"), 2);

        String runName = "file resolution run";

        List<Map<String, Object>> dataRowsInvalidResultFileName = Arrays.asList(
                Maps.of("ptid", "p03", "date", "2017-05-10", "DataFileField", CREST_2_FILE.getName())
        );
        List<Map<String, Object>> dataRowsInvalidResultFileAbsolutePath = Arrays.asList(
                Maps.of("ptid", "p03", "date", "2017-05-10", "DataFileField", CREST_2_FILE.getAbsolutePath())
        );
        List<Map<String, Object>> dataRowsInvalidResultFileDirectory = Arrays.asList(
                Maps.of("ptid", "p03", "date", "2017-05-10", "DataFileField", "../")
        );

        log("verify invalid file path is rejected during import");
        // invalid run file and result file
        assayHelper.importAssay(assayId, runName, dataRowsInvalidResultFileName, getProjectName(), Collections.singletonMap("RunFileField", CREST_2_FILE.getName()), Collections.emptyMap(), "Invalid file path: crest-2.png");
        assayHelper.importAssay(assayId, runName, dataRowsInvalidResultFileName, getProjectName(), Collections.singletonMap("RunFileField", CREST_2_FILE.getAbsolutePath()), Collections.emptyMap(), "Invalid file path: " + CREST_2_FILE.getAbsolutePath());
        assayHelper.importAssay(assayId, runName, dataRowsInvalidResultFileName, getProjectName(), Collections.singletonMap("RunFileField", "../"), Collections.emptyMap(), "Invalid file path: ../");
        // valid run file but invalid result file
        assayHelper.importAssay(assayId, runName, dataRowsInvalidResultFileName, getProjectName(), Collections.singletonMap("RunFileField", CREST_FILE.getName()), Collections.emptyMap(), "DataFileField: Invalid file path: crest-2.png");
        assayHelper.importAssay(assayId, runName, dataRowsInvalidResultFileAbsolutePath, getProjectName(), Collections.singletonMap("RunFileField", CREST_FILE.getName()), Collections.emptyMap(), "DataFileField: Invalid file path: " + CREST_2_FILE.getAbsolutePath());
        assayHelper.importAssay(assayId, runName, dataRowsInvalidResultFileDirectory, getProjectName(), Collections.singletonMap("RunFileField", CREST_FILE.getName()), Collections.emptyMap(), "DataFileField: Invalid file path: ../");

        // valid run file and valid result file
        FileBrowserHelper.FileDetailInfo runFileInfo = FileBrowserHelper.getFileDetailInfo(getProjectName(), CREST_FILE.getName());
        FileBrowserHelper.FileDetailInfo resultFileInfo = FileBrowserHelper.getFileDetailInfo(getProjectName(), SCREENSHOT_FILE.getName());
        List<Pair<String, String>> scenarios = List.of(new Pair<>(CREST_FILE.getName(), SCREENSHOT_FILE.getName()),
                new Pair<>(runFileInfo.absoluteFilePath(), resultFileInfo.absoluteFilePath()),
                new Pair<>(runFileInfo.webDavUrl(), resultFileInfo.webDavUrl()),
                new Pair<>(runFileInfo.dataFileUrl(), resultFileInfo.dataFileUrl()),
                new Pair<>(runFileInfo.webDavUrlRelative(), resultFileInfo.webDavUrlRelative()));
        int count = 3;
        for (Pair<String, String> scenario : scenarios)
        {
            List<Map<String, Object>> dataRowsValid = Arrays.asList(Maps.of("ptid", "p0" + count++, "date", "2017-05-10", "DataFileField", scenario.second));
            assayHelper.importAssay(assayId, "ValidPath" + count, dataRowsValid, getProjectName(), Collections.singletonMap("RunFileField", scenario.first), Collections.emptyMap());
        }

        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText("view runs"));
        assertElementPresent("Did not find the expected number of icons for " + CREST_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_FILE.getName() + "')]"), 5);
        clickAndWait(Locator.linkContainingText("view results"));
        assertElementPresent("Did not find the expected number of icons for " + SCREENSHOT_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + SCREENSHOT_FILE.getName() + "')]"), 6);
    }


    // Issue 21247: Import runs into GPAT assay using LABKEY.Experiment.saveBatch() API
    @Test
    public void testGpatSaveBatch() throws Exception
    {
        new ApiPermissionsHelper(this)
                .setSiteRoleUserPermissions(PasswordUtil.getUsername(), "See Absolute File Paths");

        goToProjectHome();

        log("create GPAT assay");
        String assayName = "GPAT-SaveBatch" + DOMAIN_TRICKY_CHARACTERS;
        createAssayWithFileFields(assayName);

        log("create run via saveBatch");
        String runNameSaved = "created-via-saveBatch";
        List<Map<String, Object>> resultRows = new ArrayList<>();
        resultRows.add(Maps.of("ptid", "188438418", "SpecimenID", "K770K3VY-19", "DataFileField", "crest.png"));
        resultRows.add(Maps.of("ptid", "188487431", "SpecimenID", "A770K4W1-15", "DataFileField", "screenshot.png"));

        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runNameSaved, Collections.singletonMap("RunFileField", "foo.xls"), resultRows, getProjectName(), null);
        Integer savedRunId = getRunId(assayName, runNameSaved);

        log("verify assay saveBatch worked");
        goToManageAssays();
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runNameSaved));
        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals(Arrays.asList("K770K3VY-19", "A770K4W1-15"), table.getColumnDataAsText("SpecimenID"));

        // verify images are resolved and rendered properly
        assertElementPresent("Did not find the expected number of icons for images for " + CREST_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + SCREENSHOT_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + SCREENSHOT_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + FOO_XLS_FILE.getName() + " from the runs.", Locator.xpath("//a[contains(text(), '" + FOO_XLS_FILE.getName() + "')]"), 2);

        String runName = "invalid run file path";
        resultRows.clear();
        resultRows.add(Maps.of("ptid", "188438419", "SpecimenID", "K770K3VY-20", "DataFileField", "help.jpg"));
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runName, Collections.singletonMap("RunFileField", "help.jpg"), resultRows, getProjectName(), "Invalid file path: help.jpg");
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runName, Collections.singletonMap("RunFileField", HELP_ICON_FILE.getAbsolutePath()), resultRows, getProjectName(), "Invalid file path: " + HELP_ICON_FILE.getAbsolutePath());
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runName, Collections.singletonMap("RunFileField", CREST_FILE.getAbsolutePath()), resultRows, getProjectName(), "Invalid file path: " + CREST_FILE.getAbsolutePath());
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runName, Collections.singletonMap("RunFileField", "../"), resultRows, getProjectName(), "Invalid file path: ../");

        // update run file using updateRows
        verifyUpdateRunFileAPIError(assayName, "RunFileField", savedRunId, "help.jpg");
        verifyUpdateRunFileAPIError(assayName, "RunFileField", savedRunId, HELP_ICON_FILE.getAbsolutePath());
        verifyUpdateRunFileAPIError(assayName, "RunFileField", savedRunId, CREST_FILE.getAbsolutePath());
        verifyUpdateRunFileAPIError(assayName, "RunFileField", savedRunId, "../");

        runName = "valid run file path, invalid result file path";
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runName, Collections.singletonMap("RunFileField", CREST_FILE.getName()), resultRows, getProjectName(), "DataFileField: Invalid file path: help.jpg");
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runName, Collections.singletonMap("RunFileField", CREST_FILE.getName()), List.of(Maps.of("ptid", "188438419", "SpecimenID", "K770K3VY-20", "DataFileField", CREST_FILE.getAbsolutePath())), getProjectName(), "DataFileField: Invalid file path: " + CREST_FILE.getAbsolutePath());

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(HELP_ICON_FILE);
        goToManageAssays();
        FileBrowserHelper.FileDetailInfo runFileInfo = FileBrowserHelper.getFileDetailInfo(getProjectName(), "help.jpg");
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, "Valid absolute path", Collections.singletonMap("RunFileField", runFileInfo.absoluteFilePath()), resultRows, getProjectName(), null);
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, "Valid webdav full path", Collections.singletonMap("RunFileField", runFileInfo.webDavUrl()), resultRows, getProjectName(), null);
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, "Valid webdav relative path", Collections.singletonMap("RunFileField", runFileInfo.webDavUrlRelative()), resultRows, getProjectName(), null);
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, "Valid data file url", Collections.singletonMap("RunFileField", runFileInfo.dataFileUrl()), resultRows, getProjectName(), null);

        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText("view runs"));
        assertElementPresent("Did not find the expected number of icons for " + HELP_ICON_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + HELP_ICON_FILE.getName() + "')]"), 4);

        // verify updateRows successful
        verifyUpdateRunFileAPI(assayName, "RunFileField", savedRunId, runFileInfo.absoluteFilePath(), null);
        verifyUpdateRunFileAPI(assayName, "RunFileField", savedRunId, runFileInfo.webDavUrl(), null);
        verifyUpdateRunFileAPI(assayName, "RunFileField", savedRunId, runFileInfo.webDavUrlRelative(), null);
        verifyUpdateRunFileAPI(assayName, "RunFileField", savedRunId, runFileInfo.dataFileUrl(), null);
    }

    protected void executeAndVerifyScript(String script, @Nullable String errorMsg)
    {
        log(script);
        Map<String, Object> result = (Map<String, Object>)executeAsyncScript(script);

        String failureResult = APITestHelper.parseScriptResult(result);

        if (errorMsg == null)
            assertNull(failureResult);
        else
            assertEquals("Unexpected error message", errorMsg, result.get("exception"));
    }

    private void verifyUpdateRunFileAPI(String assayName, String runFileField, int runRowId, String filePath, String errorMsg)
    {
        String updateScript = "LABKEY.Query.updateRows({ schemaName: \"assay.General." + EscapeUtil.fieldKeyEncodePart(assayName) + "\", "+
                "queryName: \"Runs\", " +
                "success: callback," +
                "failure: callback," +
                "rows: [{  \"RowId\": " + runRowId + "," +
                EscapeUtil.toJSONStr(runFileField) + ": " + EscapeUtil.toJSONStr(filePath) +
                "}]" +
                "})";
        executeAndVerifyScript(updateScript, errorMsg);
    }

    private void verifyUpdateRunFileAPIError(String assayName, String runFileField, int runRowId, String filePath)
    {
        verifyUpdateRunFileAPI(assayName, runFileField, runRowId, filePath, "Invalid file path: " + filePath);
    }

    private @Nullable Integer getRunId(String assayName, String runName)
    {
        var rows = executeSelectRowCommand("assay.General." + EscapeUtil.fieldKeyEncodePart(assayName), "Runs").getRows();
        var row = rows.stream().filter(a-> a.get("name").equals(runName)).findFirst().orElse(null);
        if (row == null)
            return null;

        return (Integer) row.get("rowId");
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }
}