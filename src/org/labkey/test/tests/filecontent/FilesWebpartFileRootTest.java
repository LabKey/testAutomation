/*
 * Copyright (c) 2017-2026 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.files.CustomizeFilesWebPartPage;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class FilesWebpartFileRootTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "FilesWebpartFileRoot Project" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String WORK_FOLDER = "Work Folder" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String WORK_CONTAINER_PATH = PROJECT_NAME + "/" + WORK_FOLDER;
    private static final String GRANDCHILD_CONTAINER = "ChildContainerNotForFileRootSelection";
    PortalHelper portalHelper = new PortalHelper(this);
    FileBrowserHelper fileBrowserHelper = new FileBrowserHelper(this);

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("filecontent");
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void initTest()
    {
        FilesWebpartFileRootTest init = getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), WORK_FOLDER);
        _containerHelper.createSubfolder(WORK_CONTAINER_PATH, GRANDCHILD_CONTAINER);

        goToWorkFolder();
        portalHelper.addWebPart("Files");
    }

    private void goToWorkFolder()
    {
        goToProjectHome(WORK_CONTAINER_PATH);
    }

    @Test
    public void testCustomFileRoot()
    {
        String folderName = "Folder " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
        File testFile = TestFileUtils.getSampleData("fileTypes/sample.txt");

        goToWorkFolder();

        _fileBrowserHelper.createFolder(folderName);
        _fileBrowserHelper.selectFileBrowserItem(folderName + "/");
        _fileBrowserHelper.uploadFile(testFile);

        portalHelper.clickWebpartMenuItem("Files", true, "Customize");
        CustomizeFilesWebPartPage customizePage = new CustomizeFilesWebPartPage(getDriver());
        Assert.assertEquals("Default webpart file root should be set at @files", "@files", customizePage.getFileRoot());
        customizePage.setFileRoot("@files", folderName);
        Assert.assertEquals("File in custom file root", List.of(testFile.getName()), _fileBrowserHelper.getFileList());

        goToWorkFolder();

        portalHelper.clickWebpartMenuItem("Files", true, "Customize");
        customizePage.verifyFileRootNodeNotPresent(GRANDCHILD_CONTAINER); //child container shouldn't show up as file root options
        customizePage.setFileRoot("@files");
        Locator.XPathLocator importDataBtn = Locator.tagWithClass("a", "importDataBtn");
        Assert.assertTrue("Import Data button should be present when file root is @files and no pipeline override exists", isElementPresent(importDataBtn));

        log("Override pipeline root for project");
        setPipelineRoot(TestFileUtils.getSampleData("AssayAPI").getParentFile().getAbsolutePath());
        goToWorkFolder();
        Assert.assertFalse("Import Data button should not be present when file root is @files and pipeline override exists", isElementPresent(importDataBtn));

        log("Set webpart file root to @pipeline");
        portalHelper.clickWebpartMenuItem("Files", true, "Customize");
        customizePage.setFileRoot("@pipeline");
        Assert.assertTrue("Import Data button should be present when file root is set to @pipeline", isElementPresent(importDataBtn));
        String sampleDataSubDir = "AssayAPI";
        Assert.assertTrue("Directory " + sampleDataSubDir + " should be present in files webpart with @pipeline as file root", fileBrowserHelper.fileIsPresent(sampleDataSubDir));
        portalHelper.clickWebpartMenuItem("Files", true, "Customize");
        customizePage.verifyFileRootNodePresent("@pipeline", sampleDataSubDir); //Verify subdirectory of @pipeline is available as file root
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
