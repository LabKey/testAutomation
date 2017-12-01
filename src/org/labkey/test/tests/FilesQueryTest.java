package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.FileBrowserExtendedProperty;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Category({DailyC.class, FileBrowser.class})
public class FilesQueryTest extends BaseWebDriverTest
{
    private static final String EXP_SCHEMA = "exp";
    private static final String CUSTOM_PROPERTY = "CustomProp";
    protected static final String TEST_USER = "user_files@filesquery.test";
    private static final String TEST_GROUP = "FilesQueryTestGroup";
    private static final String PIPELINE_OVERRIDE_RELATIVE = "studies/ExtraKeyStudy/study/";
    private static final String PIPELINE_OVERRIDE = "sampledata/" + PIPELINE_OVERRIDE_RELATIVE;

    private PortalHelper _portalHelper = new PortalHelper(this);
    private ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + "Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("filecontent");
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        FilesQueryTest initTest = (FilesQueryTest)getCurrentTest();

        initTest.doSetupSteps();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(false, TEST_USER);
    }

    private void doSetupSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        _portalHelper.addWebPart("Files");
        _portalHelper.addQueryWebPart("FileRecords", EXP_SCHEMA, "Files", null);

        _fileBrowserHelper.goToEditProperties();
        waitForElement(Locator.name("ff_name0"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("ff_name0"), CUSTOM_PROPERTY);
        clickButton("Save & Close");

        permissionsHelper.createPermissionsGroup(TEST_GROUP, TEST_USER);
        permissionsHelper.setPermissions(TEST_GROUP, "Project Administrator");
        permissionsHelper.setSiteAdminRoleUserPermissions(TEST_USER, "See Absolute File Paths");
    }

    @Test
    public void testFileRecordsWithCustomProp()
    {
        log("Upload a file to file root from file browser");
        final File testFile1 = TestFileUtils.getSampleData("security/InlineFile.html");
        final String customPropValue1 = "CustomPropValue1";
        uploadFile(testFile1, customPropValue1, "This is an html file");

        log("Create a sub directory and upload a file to the sub directory");
        final File testFile2 = TestFileUtils.getSampleData("security/InlineFile2.html");
        final String subFileFolder = "Sub Folder";
        _fileBrowserHelper.createFolder(subFileFolder);
        doubleClick(FileBrowserHelper.Locators.gridRow(subFileFolder));
        final String customPropValue2 = "CustomPropValue2";
        uploadFile(testFile2, customPropValue2, "This is another html file");

        refresh();

        log("Verify exp.files for user without \"See Absolute File Paths\" permission");
        verifyFileRecordsGrid(false, testFile1.getName(), customPropValue1, "/");
        verifyFileRecordsGrid(false, subFileFolder, " ", "/");
        verifyFileRecordsGrid(false, testFile2.getName(), customPropValue2, "/" + subFileFolder);
        String updatedCustomPropValue = "UpdatedCustomPropValue";
        verifyUpdatingCustomFileProps(testFile1.getName(), updatedCustomPropValue);
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).find();
        Assert.assertEquals("Insert data button should be available", false, table.hasHeaderMenu("Insert data"));
        // update custom prop

        log("Verify exp.files for user with \"See Absolute File Paths\" permission");
        impersonate(TEST_USER);
        verifyFileRecordsGrid(true, testFile1.getName(), updatedCustomPropValue, "/");
        verifyFileRecordsGrid(true, subFileFolder, " ", "/");
        verifyFileRecordsGrid(true, testFile2.getName(), customPropValue2, "/" + subFileFolder);
        updatedCustomPropValue = "UpdatedCustomPropValue2";
        verifyUpdatingCustomFileProps(testFile1.getName(), updatedCustomPropValue);
        Assert.assertEquals("Insert data button should be available", true, table.hasHeaderMenu("Insert data"));
        stopImpersonating();
    }

    @Test
    public void testNonFileBrowserFileRecords()
    {
        ensureFilesUpToDate();
        log("Set pipeline root override to a file system location");
        setPipelineRoot(getPipelinePath());
        goToProjectHome();

        log("Insert a valid file record which might not have been picked up due to delayed sync");
        impersonate(TEST_USER);
        String fileToInsertPath = PIPELINE_OVERRIDE_RELATIVE + "datasets/dataset5001.tsv";
        final File fileToInsert = TestFileUtils.getSampleData(fileToInsertPath);
        final String fileInsertedName = fileToInsert.getName();
        insertFileRecord(fileToInsert.getAbsolutePath(), "InsertedCustomProp");
        stopImpersonating();

        log("Verify inserted file record");
        goToProjectHome();
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).find();
        int rowIndexInserted = table.getRowIndex("Name", fileInsertedName);
        List<String> results = table.getRowDataAsText(rowIndexInserted,"Custom Prop", "Relative Folder");
        Assert.assertEquals("CustomProp is not as expected", "InsertedCustomProp", results.get(0));
        Assert.assertEquals("Relative Folder is not as expected", "/datasets", results.get(1));

        log("Verify all files under pipeline are picked up automatically by exp.files");
        ensureFilesUpToDate();
        final File pipelineFolder = TestFileUtils.getSampleData(PIPELINE_OVERRIDE_RELATIVE);
        String[] allFiles = pipelineFolder.list();
        List<String> filesPickedUp = table.getColumnDataAsText("Name");
        for (String expectedFile : allFiles)
        {
            Assert.assertTrue("File " + expectedFile + " record hasn't been created", filesPickedUp.contains(expectedFile));
        }
    }

    private void ensureFilesUpToDate()
    {
        log("Clear cache so that exp.files will do a sync immediately");
        beginAt("/admin/caches.view?clearCaches=1", 120000);
        goToProjectHome();
    }

    private void insertFileRecord(String absoluteFilePath, String customProp)
    {
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).find();
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_CustomProp"), customProp);
        setFormElement(Locator.name("quf_AbsoluteFilePath"), absoluteFilePath);
        clickButton("Submit");
    }

    private void verifyFileRecordsGrid(boolean canSeeAbsolutePath, String filename, String customPropValue, String relativeFolder)
    {
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).find();
        int rowIndex = table.getRowIndex("Name", filename);
        List<String> results = table.getRowDataAsText(rowIndex, "File Exists", "Custom Prop", "Relative Folder");
        Assert.assertEquals("File should exist", "true", results.get(0));
        Assert.assertEquals("CustomProp is not as expected", customPropValue, results.get(1));
        Assert.assertEquals("Relative Folder is not as expected", relativeFolder, results.get(2));

        if (canSeeAbsolutePath)
        {
            results = table.getRowDataAsText(rowIndex, "Absolute File Path");
            Assert.assertTrue("Absolute File Path is not as expected", results.get(0).indexOf(filename) > 0);
        }
        else
        {
            Assert.assertFalse("Absolute File Path shouldn't be present", table.getColumnLabels().contains("Absolute File Path"));
        }
    }

    private void verifyUpdatingCustomFileProps(String filename, String updatedCustomPropValue)
    {
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).find();
        int rowIndex = table.getRowIndex("Name", filename);
        table.clickEditRow(rowIndex);
        setFormElement(Locator.name("quf_CustomProp"), updatedCustomPropValue);
        clickButton("Submit");

        List<String> results = table.getRowDataAsText(rowIndex, "Custom Prop");
        Assert.assertEquals("Custom Prop is not as expected", updatedCustomPropValue, results.get(0));
    }

    private void uploadFile(File testFile, String customPropValue, String fileDescription)
    {
        List<FileBrowserExtendedProperty> fileProperties = new ArrayList<>();
        fileProperties.add(new FileBrowserExtendedProperty(CUSTOM_PROPERTY, customPropValue, false));
        _fileBrowserHelper.uploadFile(testFile, fileDescription, fileProperties, false);
    }

    public static String getPipelinePath()
    {
        return TestFileUtils.getLabKeyRoot() + "/" + PIPELINE_OVERRIDE;
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
