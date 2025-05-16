package org.labkey.test.tests.core.admin;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Git;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.FileInput;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.core.admin.AllowedFileExtensionAdminPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.labkey.test.pages.announcements.InsertPage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.DataRegionTable.DataRegion;

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
        new PortalHelper(getDriver()).addWebPart("Lists");
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

    /**
     * <p>
     *     Test the 'Allowed File Extension Admin' page and use the files web part as part of the validation process.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Add several file extensions as allowed extensions, then upload files of that type.</li>
     *         <li>Upload a file that is not allowed and validate it is rejected.</li>
     *         <li>Remove an allowed file type and validate files of that type can not be uploaded.</li>
     *         <li>Click 'Delete All' and cancel out of confirmation, validate no change.</li>
     *         <li>Click 'Delete All' and validate any file type can be uploaded.</li>
     *         <li>Edit an allowed extension, .xls to .xlsx, and validate .xlsx files can be uploaded but .xls can not.</li>
     *         <li>Validate extension value must start with a '.'</li>
     *         <li>Validate duplicate extensions are not allowed.</li>
     *         <li>Validate blank extension type is not allowed.</li>
     *     </ul>
     * </p>
     */
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

        log(String.format("Add the following as allowed extensions: %s", allowedTypes));
        setAllowedExtensions(allowedTypes, allowedTypes);

        log("Verify upload of allowed file types is successful.");
        uploadToFileWebPartAllowed(excludedTypes);

        log(String.format("Verify upload of '%s' fails", excludedType));
        uploadToFileWebPartExcluded(excludedTypes);

        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        allowedFileExtensionAdminPage.deleteExtension(allowedTypes.get(0));
        excludedType = allowedTypes.remove(0);
        excludedTypes.add(excludedType);

        log(String.format("Remove '%s' as an allowed extension.", excludedType));

        List<Input> extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted(String.format("List of 'Allowed extensions' is not as expected after removing '%s'.", excludedType),
                        allowedTypes, extensions.stream().map(Input::getValue).toList());

        log(String.format("Verify upload of '%s' fails.", excludedType));
        uploadToFileWebPartExcluded(excludedTypes);

        allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        log("Click 'Delete All' but cancel out of the confirmation dialog.");

        allowedFileExtensionAdminPage.deleteAllExtensions(false);

        extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted("List of 'Allowed extensions' is not as expected after canceling 'Delete All'.",
                        allowedTypes, extensions.stream().map(Input::getValue).toList());

        // Issue 53039.
        log("Validate that canceling the 'Delete All' dialog does not set the dirty bit.");
        // Using goToProjectHome will validate navigaiton happened.
        goToProjectHome();

        log("Now, click 'Delete All' and accept the confirmation dialog.");

        // Have to navigate back.
        allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        allowedFileExtensionAdminPage.deleteAllExtensions(true);

        extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyTrue("List of 'Allowed extensions' is not as expected after 'Delete All'.",
                        extensions.isEmpty());

        log("Validate 'all' file types can be uploaded.");
        uploadToFileWebPartAllowed(new ArrayList<>());

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
        allowedFileExtensionAdminPage = setAllowedExtensions(allowedTypes, allowedTypes);

        String oldExtension = ".xls";
        String newExtension = excludedType;

        log(String.format("Edit the extension '%s' and change it to '%s'.", oldExtension, newExtension));

        Input editExtension = allowedFileExtensionAdminPage.getAllowedExtension(allowedFileExtensionAdminPage.getAllowedExtensionIndex(oldExtension));

        editExtension.setValue(newExtension);

        // Issue 53039 validate dirty bit warning.
        log("Validate that an alert is shown is the change is not saved.");
        Locator.tagWithClass("a", "brand-logo").findElement(getDriver()).click();
        Alert alert = waitForAlert();

        checker().withScreenshot()
                .verifyTrue("Alert text doesn't have expected text.",
                        alert.getText().contains("Changes you made may not be saved."));

        log("Dismiss the alert.");
        alert.dismiss();

        log("Save the change.");
        allowedFileExtensionAdminPage.clickSaveUpdateExtension();

        allowedTypes.remove(oldExtension);
        allowedTypes.add(newExtension);

        excludedTypes = List.of(oldExtension);
        extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted("List of 'Allowed extensions' is not as expected.",
                        allowedTypes, extensions.stream().map(Input::getValue).toList());

        uploadToFileWebPartAllowed(excludedTypes);

        log(String.format("Verify file with the old extension '%s' is excluded.", oldExtension));
        uploadToFileWebPartExcluded(excludedTypes);

        allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();
        allowedFileExtensionAdminPage.setExtension("not .an extension");
        String expectedValue = "File extension must start with a '.'";
        String actualValue = allowedFileExtensionAdminPage.clickSaveExpectingError();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for invalid extension.",
                        expectedValue, actualValue);

        allowedFileExtensionAdminPage.setExtension(allowedTypes.get(0));
        expectedValue = String.format("'%s' already exists. Duplicate values not allowed.", allowedTypes.get(0));
        actualValue = allowedFileExtensionAdminPage.clickSaveExpectingError();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for duplicate extension.",
                        expectedValue, actualValue);

        allowedFileExtensionAdminPage.setExtension(allowedTypes.get(1).toUpperCase());
        expectedValue = String.format("'%s' already exists. Duplicate values not allowed.", allowedTypes.get(1).toUpperCase());
        actualValue = allowedFileExtensionAdminPage.clickSaveExpectingError();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for duplicate extension but different case.",
                        expectedValue, actualValue);

        allowedFileExtensionAdminPage.setExtension("");
        expectedValue = "File extension must not be blank.";
        actualValue = allowedFileExtensionAdminPage.clickSaveExpectingError();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for blank extension value.",
                        expectedValue, actualValue);

    }

    private void uploadToFileWebPartAllowed(List<String> excludedTypes)
    {
        goToProjectHome();
        _fileBrowserHelper.deleteAll();

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

    }

    private void uploadToFileWebPartExcluded(List<String> excludedTypes)
    {

        goToProjectHome();

        for (String excludedType : excludedTypes)
        {

            if (_fileBrowserHelper.fileIsPresent(fileMap.get(excludedType).getName()))
            {
                log(String.format("Remove the '%s' file from the upload.", excludedType));
                _fileBrowserHelper.deleteFile(fileMap.get(excludedType).getName());
                refresh();
            }

            Window<?> errorWindow = _fileBrowserHelper.uploadFileExpectingError(fileMap.get(excludedType));

            checker().withScreenshot()
                    .verifyEquals(String.format("Error message for excluded file type '%s' not as expected.", excludedType),
                            String.format("This file type [%s] is not allowed.", excludedType.replace(".", "")), errorWindow.getBody());

            click(Ext4Helper.Locators.ext4Button("OK"));
        }

    }

    /**
     * <p>
     *     Using a list validate that attachments work correctly with the allowed files list.
     * </p>
     * <p>
     *     After creating a list that has an auto-index and a attachment field as the only field this test will:
     *     <ul>
     *         <li>Can insert an element into the list with the allowed file type.</li>
     *         <li>Validate that a file of the type not allowed is not allowed.</li>
     *         <li>Remove the bad file and resubmit with a valid file type.</li>
     *     </ul>
     * </p>
     *
     */
    @Test
    public void testAllowedFileExtensionsInLists()
    {

        List<String> allowedTypes = new ArrayList<>();
        String excludedType = ".csv";

        for (String extension : fileMap.keySet())
        {
            if (!extension.equals(excludedType))
                allowedTypes.add(extension);
        }

        log(String.format("Add the following as allowed extensions: %s", allowedTypes));
        setAllowedExtensions(allowedTypes, allowedTypes);

        goToProjectHome();
        String listName = "Test Allowed Attachments";
        String keyField = "Key";
        String attachmentField = "Attachment Field";

        log(String.format("Create a list '%s' with attachment field and auto-increment key.", listName));
        _listHelper.createList(getProjectName(), listName, keyField,
                new FieldDefinition(attachmentField, FieldDefinition.ColumnType.Attachment));

        goToManageLists();
        _listHelper.goToList(listName);

        String [][] expectedData = new String [allowedTypes.size()][1];
        int index = 0;
        for (String allowedType : allowedTypes)
        {
            _listHelper.insertNewRow(Map.of(attachmentField, fileMap.get(allowedType).getAbsolutePath()), false);
            // Add a space before the name to allow for the icon.
            expectedData[index++][0] = String.format(" %s", fileMap.get(allowedType).getName());
        }

        goToManageLists();
        _listHelper.goToList(listName);
        _listHelper.verifyListData(List.of(new FieldDefinition(attachmentField, FieldDefinition.ColumnType.Attachment)), expectedData, checker());

        _listHelper.goToList(listName);
        _listHelper.insertNewRow(Map.of(attachmentField, fileMap.get(excludedType).getAbsolutePath()), false);

        // Not sure why we record two exceptions.
        checkExpectedErrors(2);

        log("Click 'Back' button and select a file type that is allowed.");
        waitForElement(Locator.button("Back"));
        clickButton("Back");

        waitForElement(Locator.name("quf_" + attachmentField));

        // Same as Issue 53026, the fields are cleared after hitting back button. Covewrage for that issue is in
        // testAllowedFileExtensionsInSampleType test.
        log("Clear the file field.");
        FileInput el = FileInput.FileInput(Locator.name("quf_" + attachmentField), getDriver()).findWhenNeeded();
        executeScript("arguments[0].value = '';", el.getComponentElement());

        File fileAgain = fileMap.get(".txt");
        log(String.format("Add the '%s' file to the list again (it is an allowed file).", fileAgain.getName()));
        el.set(fileAgain.getAbsolutePath());

        clickButton("Submit");

        DataRegionTable dataRegion = DataRegion(getDriver()).withName("query").find();
        List<String> actualData = dataRegion.getColumnDataAsText(attachmentField);

        checker().withScreenshot()
                .verifyEquals(String.format("The file '%s' should be in the list twice.", fileAgain.getName()),
                        2, Collections.frequency(actualData, String.format(" %s", fileAgain.getName())));

    }

    /**
     * <p>
     *     Validate sample types work well with the allowed extension list.
     * </p>
     * <p>
     *     This test will set several extension as allowed type, create a sample type with a file field, and then:
     *     <ul>
     *         <li>Validate a row(s) can be inserted with the allowed file types.</li>
     *         <li>Validate a row cannot be inserted with a disallowed file type.</li>
     *         <li>Validate that removing the disallowed type from the file field allows for the row to be inserted.</li>
     *     </ul>
     *     <b>Note:</b> This test does not exercise bulk or import by file. In those scenarios the file would have had
     *     to already been uploaded to the server for it to be referenced in the input or bulk file / data. Uploading to
     *     the server is covered in the testAddUpdateAndDelete test.
     * </p>
     */
    @Test
    public void testAllowedFileExtensionsInSampleType()
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

        log(String.format("Add the following as allowed extensions: %s", allowedTypes));
        setAllowedExtensions(allowedTypes, allowedTypes);

        goToProjectHome();

        String stName = "Allowed File Extension Testing";
        SampleTypeDefinition stDefinition = new SampleTypeDefinition(stName);

        String stFileField = "File Upload Test";
        stDefinition.addField(new FieldDefinition(stFileField, FieldDefinition.ColumnType.File));
        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), stDefinition);

        refresh();
        waitForElement(Locator.linkWithText(stName));

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        sampleTypeHelper.goToSampleType(stName);

        log("Add rows in the sample type with the allowed file types.");
        int i = 1;
        Map<String, String> fieldMap;
        List<String> expectedValues = new ArrayList<>();
        for (String allowedType : allowedTypes)
        {
            fieldMap = Map.of("Name", String.format("S-%d", i), stFileField, fileMap.get(allowedType).getAbsolutePath());
            sampleTypeHelper.insertRow(fieldMap);
            expectedValues.add(String.format(" sampletype/%s", fileMap.get(allowedType).getName()));
            i++;
        }

        // The order of the grid is last one added is at the top, which is opposite of how they were added to the list.
        Collections.reverse(expectedValues);

        List<String> actualValues = sampleTypeHelper.getSamplesDataRegionTable().getColumnDataAsText(stFileField);
        checker().verifyEquals(String.format("Values in the '%s' column not as expected.", stFileField),
                expectedValues, actualValues);

        log("Create a sample that tries to upload a disallowed file type.");
        String sampleId = String.format("S-%d", i);
        String description= "Some text for the description.";
        String amount = "5.00";

        fieldMap = Map.of("Name", sampleId,
                "Description", description,
                stFileField, fileMap.get(excludedType).getAbsolutePath(),
                "StoredAmount", amount);
        sampleTypeHelper.insertRow(fieldMap);

        // Not sure why we record two exceptions.
        checkExpectedErrors(2);

        // Issue 53027
        goToProjectHome();
        sampleTypeHelper = new SampleTypeHelper(this);
        sampleTypeHelper.goToSampleType(stName);
        fieldMap = Map.of("Name", sampleId,
                "Description", description,
                "StoredAmount", amount);
        sampleTypeHelper.insertRow(fieldMap);

        Map<String, String> rowMap = sampleTypeHelper.getSamplesDataRegionTable().getRowDataAsMap(0);

        checker().verifyEquals("'Name' field in grid does not have expected value.",
                sampleId, rowMap.get("Name"));

        checker().verifyEquals("'Amount' field in grid does not have expected value.",
                amount, rowMap.get("Amount"));

        checker().verifyEquals(String.format("'%s' field in grid does not have expected value.", stFileField),
                "", rowMap.get(stFileField));

        checker().screenShotIfNewError("Field_Values_Error");

    }

    /**
     * <p>
     *     Validate that message attachments work correctly with the allowed files list.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a message with several allowed files as attachments.</li>
     *         <li>Verify a message created with a disallowed file type is not allowed.</li>
     *         <li>Remove the disallowed file from the list of attachments and resubmit.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testAllowedFilesInMessages()
    {

        List<String> allowedTypes = new ArrayList<>();
        String excludedType = ".tsv";

        for (String extension : fileMap.keySet())
        {
            if (!extension.equals(excludedType))
                allowedTypes.add(extension);
        }

        log(String.format("Add the following as allowed extensions: %s", allowedTypes));
        setAllowedExtensions(allowedTypes, allowedTypes);

        goToProjectHome();

        String allowedTitle = "Allowed Files Attachment";

        InsertPage page = InsertPage.beginAt(this)
                .setTitle(allowedTitle)
                .setBody("These attachments should be allowed.");

        for (String allowedType : allowedTypes)
        {
            page.addAttachments(fileMap.get(allowedType));
        }

        log(String.format("Create a message with title of '%s' and several allowed files as attachments.", allowedTitle));

        page.submit();

        String notAllowedTitle = "Not Allowed Files Attachment";
        String notAllowedBody = "At least one of these attachments should not be allowed.";

        page = InsertPage.beginAt(this)
                .setTitle(notAllowedTitle)
                .setBody(notAllowedBody);

        for (Map.Entry<String, File> entry : fileMap.entrySet())
        {
            page.addAttachments(entry.getValue());
        }

        List<String> notAllowedAttachments = Locator.tagWithAttributeContaining("span", "id","filename")
                .findElements(getDriver())
                .stream().map(WebElement::getText).toList();

        log(String.format("Try to create a message with title of '%s' with several allowed files as attachments, and one disallowed file type.", allowedTitle));

        page.submit();

        // Not sure why we record two exceptions.
        checkExpectedErrors(2);

    }

    private AllowedFileExtensionAdminPage setAllowedExtensions(List<String> allowedTypes, List<String> expectedTypes)
    {
        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        for (String extension : allowedTypes)
        {
            allowedFileExtensionAdminPage.setExtension(extension);
            allowedFileExtensionAdminPage.clickSaveExtension();
        }

        List<Input> extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted("List of 'Allowed extensions' is not as expected.",
                        expectedTypes, extensions.stream().map(Input::getValue).toList());

        return allowedFileExtensionAdminPage;
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
