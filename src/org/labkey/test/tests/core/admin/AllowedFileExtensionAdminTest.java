package org.labkey.test.tests.core.admin;

import org.junit.Before;
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
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.core.admin.AllowedFileExtensionAdminPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND;

@Category({Git.class})
public class AllowedFileExtensionAdminTest extends BaseWebDriverTest
{
    private final File CSV_FILE = TestFileUtils.getSampleData("fileTypes/csv_sample.csv");
    private final File TSV_FILE = TestFileUtils.getSampleData("fileTypes/tsv_sample.tsv");
    private final File TXT_FILE = TestFileUtils.getSampleData("fileTypes/sample.txt");
    private final File XLS_FILE = TestFileUtils.getSampleData("fileTypes/xls_sample.xls");
    private final File XLSX_FILE = TestFileUtils.getSampleData("fileTypes/xlsx_sample.xlsx");

    private final Map<String, File> fileMap = Map.of(
            ".csv", CSV_FILE,
            ".tsv", TSV_FILE,
            ".txt", TXT_FILE,
            ".xls", XLS_FILE,
            ".xlsx", XLSX_FILE
    );

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
        try
        {
            deleteAllAllowedFileExtension();
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);

        goToProjectHome();
        new PortalHelper(getDriver()).addWebPart("Files");
        new PortalHelper(getDriver()).addWebPart("Sample Types");
        new PortalHelper(getDriver()).addWebPart("Messages List");
    }

    @Before
    public void beforeTest() throws IOException, CommandException
    {
        log("Use API to delete any existing allowed extensions.");
        deleteAllAllowedFileExtension();

        log("Delete any files that have already been uploaded.");
        goToProjectHome();
        _fileBrowserHelper.deleteAll();
    }

    @Test
    public void testAddUpdateAndDelete()
    {

        List<String> allowedTypes = new ArrayList<>();
        List<String> excludedTypes = new ArrayList<>();
        String excludedType = ".xlsx";
        excludedTypes.add(excludedType);

        for (String extension : fileMap.keySet())
        {
            if (!excludedTypes.contains(extension))
                allowedTypes.add(extension);
        }

        log(String.format("Add the following as extensions: %s", allowedTypes));

        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        for (String extension : allowedTypes)
        {
            allowedFileExtensionAdminPage.setExtension(extension);
            allowedFileExtensionAdminPage.clickSaveExtension();
        }

        List<Input> extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted("List of 'Allowed extensions' is not as expected.",
                        allowedTypes, extensions.stream().map(Input::getValue).toList());

        log("Verify upload of allowed file types is successful.");
        goToProjectHome();

        List<String> expectedFiles = new ArrayList<>();
        for (Map.Entry<String, File> entry : fileMap.entrySet())
        {
            if (!excludedTypes.contains(entry.getKey()))
            {
                _fileBrowserHelper.uploadFile(entry.getValue());
                expectedFiles.add(entry.getValue().getName());
            }
        }

        List<String> actualFiles = _fileBrowserHelper.getFileList();

        checker().withScreenshot()
                .verifyEqualsSorted("Uploaded files not as expected.",
                        expectedFiles, actualFiles);

        log(String.format("Verify upload of '%s' fails", excludedType));
        Window<?> errorWindow = _fileBrowserHelper.uploadFileExpectingError(fileMap.get(excludedType));

        checker().withScreenshot()
                .verifyEquals(String.format("Error message for excluded file type '%s' not as expected.", excludedType),
                        String.format("This file type [%s] is not allowed.", excludedType.replace(".", "")), errorWindow.getBody());

        click(Ext4Helper.Locators.ext4Button("OK"));

        allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        allowedFileExtensionAdminPage.deleteExtension(allowedTypes.get(0));
        excludedType = allowedTypes.remove(0);
        excludedTypes.add(excludedType);

        log(String.format("Remove '%s' as an allowed extension.", excludedType));

        extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted(String.format("List of 'Allowed extensions' is not as expected after removing '%s'.", excludedType),
                        allowedTypes, extensions.stream().map(Input::getValue).toList());

        goToProjectHome();
        log(String.format("Remove the '%s' file from the upload.", excludedType));
        _fileBrowserHelper.deleteFile(fileMap.get(excludedType).getName());

        log(String.format("Verify upload of '%s' fails.", excludedType));
        errorWindow = _fileBrowserHelper.uploadFileExpectingError(fileMap.get(excludedType));

        checker().withScreenshot()
                .verifyEquals(String.format("Error message is not as expected after removing type '%s'.", excludedType),
                        String.format("This file type [%s] is not allowed.", excludedType.replace(".", "")), errorWindow.getBody());

        click(Ext4Helper.Locators.ext4Button("OK"));

        allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        log("Click 'Delete All' but cancel out of the confirmation dialog.");

        allowedFileExtensionAdminPage.deleteAllExtensions(false);

        extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted("List of 'Allowed extensions' is not as expected after canceling 'Delete All'.",
                        allowedTypes, extensions.stream().map(Input::getValue).toList());

        log("Now, click 'Delete All' and accept the confirmation dialog.");

        allowedFileExtensionAdminPage.deleteAllExtensions(true);

        extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyTrue("List of 'Allowed extensions' is not as expected after 'Delete All'.",
                        extensions.isEmpty());

        excludedTypes = new ArrayList<>();

        log("Validate 'all' file types can be uploaded.");
        goToProjectHome();
        _fileBrowserHelper.deleteAll();

        expectedFiles = new ArrayList<>();
        for (Map.Entry<String, File> entry : fileMap.entrySet())
        {
            _fileBrowserHelper.uploadFile(entry.getValue());
            expectedFiles.add(entry.getValue().getName());
        }

        actualFiles = _fileBrowserHelper.getFileList();

        checker().withScreenshot()
                .verifyEqualsSorted("Uploaded files not as expected.",
                        expectedFiles, actualFiles);

        _fileBrowserHelper.deleteAll();

        allowedTypes = new ArrayList<>();
        excludedTypes = new ArrayList<>();
        excludedType = ".xlsx";
        excludedTypes.add(excludedType);

        for (String extension : fileMap.keySet())
        {
            if (!excludedTypes.contains(extension))
                allowedTypes.add(extension);
        }

        log(String.format("Add these extensions back: %s", allowedTypes));

        allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        for (String extension : allowedTypes)
        {
            allowedFileExtensionAdminPage.setExtension(extension);
            allowedFileExtensionAdminPage.clickSaveExtension();
        }

        String oldExtension = ".xls";
        String newExtension = excludedType;

        log(String.format("Edit the extension '%s' and change it to '%s'.", oldExtension, newExtension));

        Input editExtension = allowedFileExtensionAdminPage.getAllowedExtension(allowedFileExtensionAdminPage.getAllowedExtensionIndex(oldExtension));

        editExtension.setValue(newExtension);
        allowedFileExtensionAdminPage.clickSaveUpdateExtension();

        allowedTypes.remove(oldExtension);
        allowedTypes.add(newExtension);

        extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted("List of 'Allowed extensions' is not as expected.",
                        allowedTypes, extensions.stream().map(Input::getValue).toList());

        goToProjectHome();
        _fileBrowserHelper.uploadFile(fileMap.get(newExtension));

        actualFiles = _fileBrowserHelper.getFileList();

        checker().withScreenshot()
                .verifyTrue(String.format("File '%s' should have been uploaded.", fileMap.get(newExtension).getName()),
                        actualFiles.contains(fileMap.get(newExtension).getName()));

        log(String.format("Verify file with the old extension '%s' is excluded.", oldExtension));

        errorWindow = _fileBrowserHelper.uploadFileExpectingError(fileMap.get(oldExtension));

        checker().withScreenshot()
                .verifyEquals(String.format("Error message is not as expected after removing type '%s'.", oldExtension),
                        String.format("This file type [%s] is not allowed.", oldExtension.replace(".", "")), errorWindow.getBody());

    }

    @Test
    public void testAllowedFileExtensionsInLists() throws IOException, CommandException
    {
        log("Verify and set allowed file extensions in admin console");
        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for invalid extension",
                        "File extension must start with a '.'", allowedFileExtensionAdminPage.setExtension("jpg").clickSaveExpectingError());
        allowedFileExtensionAdminPage.setExtension(".xls").clickSaveExtension();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for duplicate extension",
                        "'.xls' already exists. Duplicate values not allowed.", allowedFileExtensionAdminPage.setExtension(".xls").clickSaveExpectingError());

        log("Create a list with attachment field");
        goToProjectHome();
        String listName = "ListWithAttachments";
        new IntListDefinition(listName, "List_key")
                .setFields(List.of(new FieldDefinition("File_Upload_1", FieldDefinition.ColumnType.Attachment)))
                .create(createDefaultConnection(), getProjectName());

        goToManageLists();
        waitAndClickAndWait(Locator.linkWithText(listName));
        _listHelper.insertNewRow(Map.of("File_Upload_1", XLS_FILE.getAbsolutePath()), false);

        //import from file.

    }

    @Test
    public void testAllowedFileExtensionsInSampleType() throws IOException, CommandException
    {
        String allowedExtension = ".tsv";
        log("Set allowed file extensions in admin console");
        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();
        allowedFileExtensionAdminPage.setExtension(allowedExtension).clickSaveExtension();

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", "Sample type Testing");
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("File_Upload_1", FieldDefinition.ColumnType.File)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
    }

    private void deleteAllAllowedFileExtension() throws IOException, CommandException
    {
        SimplePostCommand command = new SimplePostCommand("admin", "deleteAllValues");
        Map<String, Object> params = new HashMap<>();
        params.put("type", "FileExtension");
        command.setParameters(params);
        command.execute(createDefaultConnection(), "/");
    }

    @Override
    protected String getProjectName()
    {
        return "Allowed File Extension Admin Test Project " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
