/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.tests.filecontent;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.FileBrowserExtendedProperty;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Category({DailyC.class, FileBrowser.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class FilesQueryTest extends BaseWebDriverTest
{
    private static final String EXP_SCHEMA = "exp";
    private static final String CUSTOM_PROPERTY = "CustomProp";
    protected static final String TEST_USER = "user_files@filesquery.test";
    protected static final String TEST_USER_NO_PATHS = "user_files_no_paths@filesquery.test";
    private static final String TEST_GROUP = "FilesQueryTestGroup";
    private static final File PIPELINE_FOLDER = TestFileUtils.getSampleData("studies/ExtraKeyStudy/study");

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
    public static void doSetup()
    {
        FilesQueryTest initTest = (FilesQueryTest)getCurrentTest();

        initTest.doSetupSteps();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(false, TEST_USER, TEST_USER_NO_PATHS);
    }

    private void doSetupSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).doInAdminMode(_portalHelper -> {
            _portalHelper.addWebPart("Files");
            _portalHelper.addQueryWebPart("FileRecords", EXP_SCHEMA, "Files", null);
        });

        DomainDesignerPage designerPage = _fileBrowserHelper.goToEditProperties();
        designerPage.fieldsPanel().addField(CUSTOM_PROPERTY);
        designerPage.clickFinish();

        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.createPermissionsGroup(TEST_GROUP, TEST_USER, TEST_USER_NO_PATHS);
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
        _fileBrowserHelper.selectFileBrowserItem(subFileFolder + "/");
        final String customPropValue2 = "CustomPropValue2";
        uploadFile(testFile2, customPropValue2, "This is another html file");

        impersonate(TEST_USER_NO_PATHS);

        log("Verify exp.files for user without \"See Absolute File Paths\" permission");
        verifyFileRecordsGrid(false, testFile1.getName(), customPropValue1, "/");
        verifyFileRecordsGrid(false, subFileFolder, " ", "/");
        verifyFileRecordsGrid(false, testFile2.getName(), customPropValue2, "/" + subFileFolder);
        String updatedCustomPropValue = "UpdatedCustomPropValue";
        verifyUpdatingCustomFileProps(testFile1.getName(), updatedCustomPropValue);
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).find();
        Assert.assertFalse("Insert data button should be available", table.hasHeaderMenu("Insert data"));
        // update custom prop

        log("Verify exp.files for user with \"See Absolute File Paths\" permission");
        impersonate(TEST_USER);
        verifyFileRecordsGrid(true, testFile1.getName(), updatedCustomPropValue, "/");
        verifyFileRecordsGrid(true, subFileFolder, " ", "/");
        verifyFileRecordsGrid(true, testFile2.getName(), customPropValue2, "/" + subFileFolder);
        updatedCustomPropValue = "UpdatedCustomPropValue2";
        verifyUpdatingCustomFileProps(testFile1.getName(), updatedCustomPropValue);
        Assert.assertTrue("Insert data button should be available", table.hasHeaderMenu("Insert data"));
        stopImpersonating();
    }

    @Test
    public void testNonFileBrowserFileRecords()
    {
        ensureFilesUpToDate();
        log("Set pipeline root override to a file system location");
        setPipelineRoot(PIPELINE_FOLDER.getAbsolutePath());
        goToProjectHome();

        log("Insert a valid file record which might not have been picked up due to delayed sync");
        impersonate(TEST_USER);
        final File fileToInsert = new File(PIPELINE_FOLDER, "datasets/dataset5001.tsv");
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
        String[] allFiles = PIPELINE_FOLDER.list();
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

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
