package org.labkey.test.tests;

import org.apache.hc.core5.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.net.URI;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class AttachmentFieldTest extends BaseWebDriverTest
{
    private static final String RESTRICTED_PROJECT = "AttachmentFieldTest Restricted Project";
    private static final String RESTRICTED_USER = "restrictedreader@attachmentfieldtest.test";
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

    @Test
    public void testFileFieldInSampleType()
    {
        goToProjectHome();
        String sampleTypeName = "Sample type with attachment";
        String fieldName = "testFile";
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        goToProjectHome();

        log("Create a sample type with attachment field");
        sampleTypeHelper.createSampleType(new SampleTypeDefinition(sampleTypeName)
                .setFields(List.of(
                        new FieldDefinition(fieldName, FieldDefinition.ColumnType.File)
                )));

        log("Inserting samples in sample Type");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleTypeName));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        samplesTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "S1");
        setFormElement(Locator.name("quf_" + fieldName), SAMPLE_FILE);
        clickButton("Submit");

        log("Verifying view in browser works");
        clickAndWait(Locator.tagWithAttributeContaining("img", "title", SAMPLE_FILE.getName()));
        Assertions.assertThat(getDriver().getCurrentUrl()).as("File field view URL.").contains("core-downloadFileLink.view");

        goToProjectHome();
        UpdateSampleTypePage updatePage = sampleTypeHelper.goToEditSampleType(sampleTypeName);
        updatePage.getFieldsPanel().getField(fieldName).expand().setAttachmentBehavior("Download File");
        updatePage.clickSave();

        File downloadedFile = doAndWaitForDownload(() -> Locator.tagWithAttributeContaining("img", "title", SAMPLE_FILE.getName()).findElement(getDriver()).click());
        Assert.assertTrue("Downloaded file is empty", downloadedFile.length() > 0);
    }

    @Test
    public void testAttachmentFieldInLists()
    {
        String listName = TestDataGenerator.randomDomainName("List with attachment field");
        String fieldName = TestDataGenerator.randomFieldName("Test File");
        goToProjectHome();
        log("Creating the list");
        _listHelper.createList(getProjectName(), listName, "id");

        log("Adding a attachment field with Show attachment in Browser");
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel panel = domainDesignerPage.fieldsPanel();
        DomainFieldRow stringRow = panel
                .addField(fieldName)
                .setType(FieldDefinition.ColumnType.Attachment);
        stringRow.setAttachmentBehavior("Show Attachment in Browser");
        domainDesignerPage.clickFinish();

        log("Insert row in list");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());
        listTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_" + fieldName), SAMPLE_FILE);
        clickButton("Submit");

        log("Verify file opened in browser");
        Locator.tagWithAttributeContaining("img", "title", SAMPLE_FILE.getName()).findElement(getDriver()).click();
        switchToWindow(1);
        waitFor(() -> getDriver().getCurrentUrl().startsWith("http"), "Tab failed to load", 5_000);
        Assertions.assertThat(getDriver().getCurrentUrl()).as("Incorrect file displayed").contains(SAMPLE_FILE.getName());
        switchToMainWindow();

        log("Verify file is downloaded");
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        panel = domainDesignerPage.fieldsPanel();
        panel.getField(fieldName).setAttachmentBehavior("Download Attachment");
        domainDesignerPage.clickFinish();

        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
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
        setFormElement(Locator.name("name"), "TestRun");
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
}
