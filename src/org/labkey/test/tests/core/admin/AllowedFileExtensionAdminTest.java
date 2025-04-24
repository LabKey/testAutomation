package org.labkey.test.tests.core.admin;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Git;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.core.admin.AllowedFileExtensionAdminPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND;

@Category({Git.class})
public class AllowedFileExtensionAdminTest extends BaseWebDriverTest
{
    private final File EXCEL_DATA_FILE = TestFileUtils.getSampleData("dataLoading/excel/fruits.xls");
    private final File TSV_DATA_FILE = TestFileUtils.getSampleData("dataLoading/excel/fruits.tsv");
    private final File TXT_DATA_FILE = TestFileUtils.getSampleData("survey/TestAttachment.txt");

    @BeforeClass
    public static void setupProject()
    {
        AllowedFileExtensionAdminTest init = getCurrentTest();
        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        deleteAllAllowedFileExtension();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);

        goToProjectHome();
        new PortalHelper(getDriver()).addWebPart("Files");
        new PortalHelper(getDriver()).addWebPart("Sample Types");
    }

    @Test
    public void testAllowedFileExtensionsInLists() throws IOException, CommandException
    {
        log("Verify and set allowed file extensions in admin console");
        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();
        Assert.assertEquals("Incorrect error message for invalid extension", "File extension must start with a '.'",
                allowedFileExtensionAdminPage.setExtension("jpg").clickSaveExpectingError());
        allowedFileExtensionAdminPage.setExtension(".xls").clickSaveExtension().clickUpdateExtension();
        Assert.assertEquals("Incorrect error message for duplicate extension", "'.xls' already exists. Duplicate values not allowed.",
                allowedFileExtensionAdminPage.setExtension(".xls").clickSaveExpectingError());

        log("Create a list with attachment field");
        goToProjectHome();
        String listName = "ListWithAttachments";
        new IntListDefinition(listName, "List_key")
                .setFields(List.of(new FieldDefinition("File_Upload_1", FieldDefinition.ColumnType.Attachment)))
                .create(createDefaultConnection(), getProjectName());

        goToManageLists();
        waitAndClickAndWait(Locator.linkWithText(listName));
        _listHelper.insertNewRow(Map.of("File_Upload_1", EXCEL_DATA_FILE.getAbsolutePath()), false);


        //import from file.

    }

    @Test
    public void testAllowedFileExtensionsInSampleType() throws IOException, CommandException
    {
        log("Set allowed file extensions in admin console");
        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();
        allowedFileExtensionAdminPage.setExtension("tsv").clickSaveExtension().clickUpdateExtension();

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", "Sample type Testing");
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("File_Upload_1", FieldDefinition.ColumnType.File)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
    }

    @Test
    public void testAllowedFileExtensionsInFileBrowser()
    {
        log("Set allowed file extensions in admin console");
        deleteAllAllowedFileExtension();
        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();
        allowedFileExtensionAdminPage.setExtension(".txt").clickSaveExtension().clickUpdateExtension();

        log("Verify upload of .txt is successful");
        goToProjectHome();
        _fileBrowserHelper.uploadFile(TXT_DATA_FILE);
        Assert.assertTrue(_fileBrowserHelper.fileIsPresent(TXT_DATA_FILE.getName()));

        log("Verify upload of .tsv fails");
        Window errorWindow = _fileBrowserHelper.uploadFileExpectingError(TSV_DATA_FILE);
        Assert.assertEquals(".tsv is not a valid extension", "This file type [tsv] is not allowed.", errorWindow.getBody());
        click(Ext4Helper.Locators.ext4Button("OK"));

        log("Update the allowed file extension from .txt to .tsv");
        allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();
        allowedFileExtensionAdminPage.updateExtension(".txt", ".tsv").clickUpdateExtension();
        sleep(500);

        log("Verify upload of .tsv is successful");
        goToProjectHome();
        _fileBrowserHelper.uploadFile(TSV_DATA_FILE);
        Assert.assertTrue(_fileBrowserHelper.fileIsPresent(TSV_DATA_FILE.getName()));
    }

    private void deleteAllAllowedFileExtension()
    {
        SimplePostCommand command = new SimplePostCommand("admin", "deleteAllValues");
        Map<String, Object> params = new HashMap<>();
        params.put("type", "FileExtension");
        command.setParameters(params);
        try
        {
            command.execute(createDefaultConnection(), "/");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getProjectName()
    {
        return "AllowedFileExtensionAdminTest Project " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
