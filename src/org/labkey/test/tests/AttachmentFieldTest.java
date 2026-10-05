package org.labkey.test.tests;

import org.apache.hc.core5.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.assay.Batch;
import org.labkey.remoteapi.assay.Data;
import org.labkey.remoteapi.assay.Run;
import org.labkey.remoteapi.assay.SaveAssayBatchCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.admin.FileRootsManagementPage;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.TestDataGenerator;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class AttachmentFieldTest extends BaseWebDriverTest
{
    private static final String RESTRICTED_PROJECT = "AttachmentFieldTest Restricted Project";
    private static final String RESTRICTED_USER = "restrictedreader@attachmentfieldtest.test";
    private static final String OUTSIDE_FILE_CONTENT = "AttachmentFieldTest content outside every file root";
    private final File SAMPLE_FILE = new File(TestFileUtils.getSampleData("fileTypes"), "jpg_sample.jpg");

    @BeforeClass
    public static void setupProject()
    {
        AttachmentFieldTest init = getCurrentTest();
        init.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addBodyWebPart("Sample Types");
        portalHelper.addBodyWebPart("Lists");
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _containerHelper.deleteProject(RESTRICTED_PROJECT, false);
        _userHelper.deleteUsers(false, RESTRICTED_USER);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testFileFieldInSampleType()
    {
        String sampleTypeName = "Sample type with attachment";
        String fieldName = "testFile";
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        log("Create a sample type with attachment field");
        sampleTypeHelper.createSampleType(new SampleTypeDefinition(sampleTypeName)
                .setFields(List.of(
                        new FieldDefinition(fieldName, FieldDefinition.ColumnType.File)
                )));

        log("Inserting samples in sample Type");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleTypeName));

        DataRegionTable.DataRegion(getDriver()).withName("Material")
                .waitFor()
                .clickInsertNewRow()
                .setField("Name", "S1")
                .setField(fieldName, SAMPLE_FILE)
                .submit();

        assertElementPresent(Locator.tagWithAttribute("a", "title", "Download attached file"));

        clickAndWait(Locator.tagWithText("a", "S1"));
        clickAndWait(Locator.tagWithClass("a", "labkey-text-link").withText("edit"));
        waitForElement(Locator.tagContainingText("div", "jpg_sample.jpg"));
        // Issue 53200: Update form incorrectly shows that a file is not available
        assertTextNotPresent("jpg_sample.jpg (unavailable)");
        clickButton("Cancel");

        log("Verifying view in browser works");
        clickAndWait(Locator.tagWithAttributeContaining("img", "title", SAMPLE_FILE.getName()));
        Assertions.assertThat(getDriver().getCurrentUrl()).as("File field view URL.").contains("core-downloadFileLink.view");

        goToProjectHome();
        UpdateSampleTypePage updatePage = sampleTypeHelper.goToEditSampleType(sampleTypeName);
        updatePage.getFieldsPanel().getField(fieldName).expand().setAttachmentBehavior("Download File");
        updatePage.clickSave();

        File downloadedFile = doAndWaitForDownload(() -> Locator.tagWithAttributeContaining("img", "title", SAMPLE_FILE.getName()).findElement(getDriver()).click());
        Assert.assertTrue("Downloaded file is empty", downloadedFile.length() > 0);

        // create a subfolder and set the Project file root to child folder file root, to simulate sample file path not under current file root
        String subFolder = "ChildFolder";
        _containerHelper.createSubfolder(getProjectName(), subFolder);
        clickFolder(subFolder);
        FileRootsManagementPage fileRootsManagementPage = goToFolderManagement().goToFilesTab();
        String childFileRoot = fileRootsManagementPage.getRootPath();
        goToProjectHome();
        fileRootsManagementPage = goToFolderManagement().goToFilesTab();
        fileRootsManagementPage.useCustomFileRoot(childFileRoot).clickSave();

        // verify file path display for files that are present but outside the current file root
        verifyUnavailableFile();

        // reset file root to default
        goToFolderManagement()
                .goToFilesTab()
                .selectFileRootType(FileRootsManagementPage.FileRootOption.siteDefault)
                .clickSave();
        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleTypeName));
        assertElementPresent(Locator.tagWithAttribute("a", "title", "Download attached file"));

        // delete the file and verify the file path that doesn't exist
        goToModule("FileContent");
        _fileBrowserHelper.deleteFile("sampletype");
        verifyUnavailableFile();
    }

    private void verifyUnavailableFile()
    {
        String sampleTypeName = "Sample type with attachment";
        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleTypeName));
        waitForElement(Locator.tagContainingText("td", "jpg_sample.jpg (unavailable)"));
        assertElementNotPresent(Locator.tagWithAttribute("a", "title", "Download attached file"));

        // "(unavailable)" suffix is present in the update view
        clickAndWait(Locator.tagWithText("a", "S1"));
        clickAndWait(Locator.tagWithClass("a", "labkey-text-link").withText("edit"));
        waitForElement(Locator.tagContainingText("div", "jpg_sample.jpg (unavailable)"));
        assertElementNotPresent(Locator.tagWithAttributeContaining("img", "src", "/_icons/image.png"));
    }

    @Test
    public void testAttachmentFieldInLists()
    {
        String listName = TestDataGenerator.randomDomainName("List with attachment field");
        String fieldName = TestDataGenerator.randomFieldName("Test File");
        log("Creating the list");
        _listHelper.createList(getProjectName(), listName, "id");

        log("Adding a attachment field with Show attachment in Browser");
        EditListDefinitionPage editPage = _listHelper.goToEditDesign(listName)
                .addField(new FieldDefinition(fieldName, FieldDefinition.ColumnType.Attachment));
        editPage.getFieldsPanel()
                .getField(fieldName)
                .setAttachmentBehavior("Show Attachment in Browser");
        editPage.clickSave();

        log("Insert row in list");
        _listHelper.beginAtList(getProjectName(), listName);
        new DataRegionTable("query", getDriver())
                .clickInsertNewRow()
                .setField(fieldName, SAMPLE_FILE)
                .submit();

        log("Verify file opened in browser");
        Locator.tagWithAttributeContaining("img", "title", SAMPLE_FILE.getName()).findElement(getDriver()).click();
        switchToWindow(1);
        waitFor(() -> getDriver().getCurrentUrl().startsWith("http"), "Tab failed to load", 5_000);
        Assertions.assertThat(getDriver().getCurrentUrl()).as("Incorrect file displayed").contains(SAMPLE_FILE.getName());
        switchToMainWindow();

        log("Verify file is downloaded");
        editPage = _listHelper.goToEditDesign(listName);
        editPage.getFieldsPanel()
                .getField(fieldName)
                .setAttachmentBehavior("Download Attachment");
        editPage.clickSave();

        File downloadedFile = doAndWaitForDownload(() -> Locator.tagWithAttributeContaining("img", "title", SAMPLE_FILE.getName()).findElement(getDriver()).click());
        Assert.assertTrue("Downloaded file is empty", downloadedFile.length() > 0);
    }

    // Kanban #1924
    @Test
    public void testDownloadFileLinkCrossContainerPermission()
    {
        final String assayName = "CrossContainerAssay";
        final String runFieldName = "runFile";

        log("Create restricted project with Assay folder type to provide a pipeline root for file storage");
        _containerHelper.createProject(RESTRICTED_PROJECT, "Assay");

        log("Create a General assay with a run-level file link field");
        goToProjectHome(RESTRICTED_PROJECT);
        goToManageAssays();
        ReactAssayDesignerPage assayDesigner = _assayHelper.createAssayDesign("General", assayName);
        assayDesigner.setEditableRuns(true);
        assayDesigner.goToRunFields().addField(runFieldName).setType(FieldDefinition.ColumnType.File);
        assayDesigner.clickFinish();

        log("Import a minimal assay run");
        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("Name"), "TestRun");
        setFormElement(Locator.name("TextAreaDataCollector.textArea"),
                "Specimen ID\tParticipant ID\tVisit ID\n100\t1A2B\t1");
        clickButton("Save and Finish");

        log("Edit the run to set the file field");
        clickAndWait(Locator.linkWithText("view runs"));
        new DataRegionTable("Runs", getDriver()).clickEditRow(0);
        setFormElement(Locator.name("quf_" + runFieldName), SAMPLE_FILE);
        clickButton("Submit");
        waitForElement(DataRegionTable.updateLinkLocator());

        log("Hover over the run file thumbnail to reveal the popup and get the objectURI-based downloadFileLink URL");
        mouseOver(Locator.xpath("//img[contains(@title, '" + SAMPLE_FILE.getName() + "')]"));
        longWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#helpDiv")));
        String restrictedDownloadUrl = Locator.xpath("//div[@id='helpDiv']//img[contains(@src, 'downloadFileLink')]")
                .findElement(getDriver()).getAttribute("src");
        Assertions.assertThat(restrictedDownloadUrl).as("Expected downloadFileLink URL with objectURI parameter")
                .contains("downloadFileLink")
                .contains("objectURI");

        // Build a cross-container URL: keep the same objectURI (run LSID) and propertyId but use the main project's
        // container.
        String crossContainerUrl = WebTestHelper.buildURL("core", getProjectName(), "downloadFileLink")
                + "?" + URI.create(restrictedDownloadUrl).getRawQuery();

        log("Create a reader user with access to the main project only, not to the restricted project");
        _userHelper.createUser(RESTRICTED_USER);
        _userHelper.setInitialPassword(RESTRICTED_USER);
        new ApiPermissionsHelper(this).addMemberToRole(RESTRICTED_USER, "Reader", PermissionsHelper.MemberType.user, getProjectName());

        log("Verify cross-container download is rejected with 403 when user lacks read permission on the object's container");
        int status = WebTestHelper.getHttpResponse(crossContainerUrl, RESTRICTED_USER, PasswordUtil.getPassword()).getResponseCode();
        Assert.assertEquals("Expected 403 Forbidden when user lacks read permission on the object's container",
                HttpStatus.SC_FORBIDDEN, status);
    }

    // GH Issue 1463
    @Test
    public void testDownloadFileLinkWithDisabledFileRoot() throws Exception
    {
        Assume.assumeFalse("Test seeds a server-side path from the local file system", TestProperties.isServerRemote());

        final String folderName = "DisabledFileRoot";
        final String folderPath = getProjectName() + "/" + folderName;
        final String sampleTypeName = "FileLinkSamples";
        final String textListName = "TextPathList";
        final String fieldName = "LinkedFile";
        _containerHelper.createSubfolder(getProjectName(), folderName);

        log("Create a sample type with a file field and a list with a same-named text field");
        new SampleTypeDefinition(sampleTypeName)
                .setFields(List.of(new FieldDefinition(fieldName, FieldDefinition.ColumnType.File)))
                .create(createDefaultConnection(), folderPath);
        new IntListDefinition(textListName, "Key")
                .setFields(List.of(new FieldDefinition(fieldName, FieldDefinition.ColumnType.String)))
                .create(createDefaultConnection(), folderPath);

        log("Upload a file while the file root is enabled");
        SampleTypeHelper.beginAtSampleTypesList(this, folderPath).goToSampleType(sampleTypeName);
        DataRegionTable.DataRegion(getDriver()).withName("Material")
                .waitFor()
                .clickInsertNewRow()
                .setField("Name", "S1")
                .setField(fieldName, SAMPLE_FILE)
                .submit();
        String managedFileUrl = Locator.tagWithAttributeContaining("a", "href", "downloadFileLink")
                .findElement(getDriver()).getAttribute("href");
        Matcher propertyIdMatcher = Pattern.compile("propertyId=(\\d+)").matcher(managedFileUrl);
        Assert.assertTrue("No propertyId in download URL: " + managedFileUrl, propertyIdMatcher.find());

        String propertyId = propertyIdMatcher.group(1);

        log("Link a sample to a file under a custom pipeline root that is later replaced");
        clickFolder(folderName);
        File oldPipelineRoot = TestFileUtils.ensureTestTempDir(getClass().getSimpleName(), "oldPipelineRoot");
        File orphanedFile = new File(oldPipelineRoot, SAMPLE_FILE.getName());
        Files.copy(SAMPLE_FILE.toPath(), orphanedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        setPipelineRoot(oldPipelineRoot.getAbsolutePath(), false);
        InsertRowsCommand sampleInsert = new InsertRowsCommand("samples", sampleTypeName);
        sampleInsert.addRow(Map.of("Name", "S2", fieldName, orphanedFile.getAbsolutePath()));
        Object orphanedRowId = sampleInsert.execute(createDefaultConnection(), folderPath).getRows().getFirst().get("RowId");
        String outsideFileUrl = WebTestHelper.buildURL("core", folderPath, "downloadFileLink", Map.of(
                "propertyId", propertyId,
                "schemaName", "samples",
                "queryName", sampleTypeName,
                "pk", String.valueOf(orphanedRowId)));

        log("Pair the file field's propertyId with a same-named text column in a list");
        InsertRowsCommand listInsert = new InsertRowsCommand("lists", textListName);
        listInsert.addRow(Map.of(fieldName, orphanedFile.getAbsolutePath()));
        Object textRowKey = listInsert.execute(createDefaultConnection(), folderPath).getRows().getFirst().get("Key");
        String mismatchedColumnUrl = WebTestHelper.buildURL("core", folderPath, "downloadFileLink", Map.of(
                "propertyId", propertyId,
                "schemaName", "lists",
                "queryName", textListName,
                "pk", String.valueOf(textRowKey)));

        log("Replace the pipeline root and disable the file root, so the assay files root is unresolvable");
        setPipelineRoot(TestFileUtils.ensureTestTempDir(getClass().getSimpleName(), "newPipelineRoot").getAbsolutePath(), false);
        FileRootsManagementPage.beginAt(this, folderPath)
                .selectFileRootType(FileRootsManagementPage.FileRootOption.disable)
                .clickSave();

        Assert.assertEquals("File under the disabled file root should still download",
                HttpStatus.SC_OK, WebTestHelper.getHttpResponse(managedFileUrl).getResponseCode());

        SimpleHttpResponse outsideResponse = WebTestHelper.getHttpResponse(outsideFileUrl);
        Assert.assertEquals("File outside every file root should be rejected",
                HttpStatus.SC_NOT_FOUND, outsideResponse.getResponseCode());
        Assertions.assertThat(outsideResponse.getResponseBody()).as("Rejection reason")
                .contains("under a file root for container");

        Assert.assertEquals("propertyId for a different column should be rejected",
                HttpStatus.SC_BAD_REQUEST, WebTestHelper.getHttpResponse(mismatchedColumnUrl).getResponseCode());

        log("Revert to the default pipeline root, which is unresolvable while the file root is disabled");
        clickFolder(folderName);
        setPipelineRootToDefault();

        Assert.assertEquals("File under the disabled file root should download without a pipeline root",
                HttpStatus.SC_OK, WebTestHelper.getHttpResponse(managedFileUrl).getResponseCode());
        Assert.assertEquals("File outside every file root should be rejected without a pipeline root",
                HttpStatus.SC_NOT_FOUND, WebTestHelper.getHttpResponse(outsideFileUrl).getResponseCode());
    }

    @Test
    public void testExpDataAbsolutePathWithDisabledFileRoot() throws Exception
    {
        Assume.assumeFalse("Test seeds a server-side path from the local file system", TestProperties.isServerRemote());

        final String folderName = "ExpDataAbsolutePath";
        final String folderPath = getProjectName() + "/" + folderName;
        _containerHelper.createSubfolder(getProjectName(), folderName);

        log("Create a derivation run while the default pipeline root is available");
        Data seedData = new Data();
        seedData.setName("seed");
        seedData.setPipelinePath("/");
        Batch savedBatch = saveDerivationRun(folderPath, null, null, seedData);
        Run savedRun = savedBatch.getRuns().getFirst();

        log("Disable the file root, so the folder has no pipeline root");
        FileRootsManagementPage.beginAt(this, folderPath)
                .selectFileRootType(FileRootsManagementPage.FileRootOption.disable)
                .clickSave();

        log("Attach a file outside every file root to the existing run");
        File outsideFile = writeOutsideFile(TestFileUtils.ensureTestTempDir(getClass().getSimpleName(), "outsideRoots"));
        Data outsideData = new Data();
        outsideData.setName(outsideFile.getName());
        outsideData.setAbsolutePath(outsideFile.getAbsolutePath());
        try
        {
            saveDerivationRun(folderPath, savedBatch.getId(), savedRun.getId(), outsideData);
        }
        catch (CommandException expected)
        {
            log("Save rejected the file outside every file root: " + expected.getMessage());
            return;
        }

        SimpleHttpResponse showFileResponse = getShowFileResponse(folderPath, getDataRowId(folderPath, outsideFile.getName()));
        Assert.fail("saveAssayBatch accepted a file outside every file root, and showFile then returned "
                + showFileResponse.getResponseCode()
                + (showFileResponse.getResponseBody().contains(OUTSIDE_FILE_CONTENT) ? " with the file's contents" : ""));
    }

    @Test
    public void testShowFileWithDisabledFileRoot() throws Exception
    {
        Assume.assumeFalse("Test seeds a server-side path from the local file system", TestProperties.isServerRemote());

        final String folderName = "OrphanedExpData";
        final String folderPath = getProjectName() + "/" + folderName;
        _containerHelper.createSubfolder(getProjectName(), folderName);

        log("Create an exp.data file under a custom pipeline root that is later removed");
        File oldPipelineRoot = TestFileUtils.ensureTestTempDir(getClass().getSimpleName(), "oldExpDataPipelineRoot");
        File orphanedFile = writeOutsideFile(oldPipelineRoot);
        clickFolder(folderName);
        setPipelineRoot(oldPipelineRoot.getAbsolutePath(), false);
        Data orphanedData = new Data();
        orphanedData.setName(orphanedFile.getName());
        orphanedData.setAbsolutePath(orphanedFile.getAbsolutePath());
        saveDerivationRun(folderPath, null, null, orphanedData);
        int dataRowId = getDataRowId(folderPath, orphanedFile.getName());

        log("Revert to the default pipeline root");
        clickFolder(folderName);
        setPipelineRootToDefault();
        Assert.assertEquals("File outside the pipeline root should be rejected",
                HttpStatus.SC_FORBIDDEN, getShowFileResponse(folderPath, dataRowId).getResponseCode());

        log("Disable the file root, so the folder has no pipeline root");
        FileRootsManagementPage.beginAt(this, folderPath)
                .selectFileRootType(FileRootsManagementPage.FileRootOption.disable)
                .clickSave();
        Assert.assertEquals("File outside every file root should be rejected without a pipeline root",
                HttpStatus.SC_FORBIDDEN, getShowFileResponse(folderPath, dataRowId).getResponseCode());
    }

    private File writeOutsideFile(File dir) throws IOException
    {
        File file = new File(dir, "outsideRoots.txt");
        Files.writeString(file.toPath(), OUTSIDE_FILE_CONTENT);
        return file;
    }

    private Batch saveDerivationRun(String folderPath, @Nullable Integer batchId, @Nullable Integer runId, Data dataInput) throws IOException, CommandException
    {
        Run run = new Run();
        run.setName("Derivation run");
        if (runId != null)
            run.setId(runId);
        run.setDataInputs(List.of(dataInput));

        Batch batch = new Batch();
        if (batchId != null)
            batch.setId(batchId);
        batch.getRuns().add(run);

        return new SaveAssayBatchCommand(SaveAssayBatchCommand.SAMPLE_DERIVATION_PROTOCOL, batch)
                .execute(createDefaultConnection(), folderPath).getBatch();
    }

    private int getDataRowId(String folderPath, String dataName) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("exp", "Data");
        select.setColumns(List.of("RowId"));
        select.addFilter("Name", dataName, Filter.Operator.EQUAL);
        List<Map<String, Object>> rows = select.execute(createDefaultConnection(), folderPath).getRows();
        Assert.assertEquals("exp.data rows named " + dataName, 1, rows.size());
        return ((Number) rows.getFirst().get("RowId")).intValue();
    }

    private SimpleHttpResponse getShowFileResponse(String folderPath, int dataRowId)
    {
        return WebTestHelper.getHttpResponse(WebTestHelper.buildURL("experiment", folderPath, "showFile",
                Map.of("rowId", String.valueOf(dataRowId))));
    }
}
