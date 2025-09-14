package org.labkey.test.tests;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.admin.FileRootsManagementPage;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class AttachmentFieldTest extends BaseWebDriverTest
{
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
        waitForElement(Locator.tagContainingText("div", "sampletype/jpg_sample.jpg"));
        // Issue 53200: Update form incorrectly shows that a file is not available
        assertTextNotPresent("sampletype/jpg_sample.jpg (unavailable)");
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
}
