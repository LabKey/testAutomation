/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class, FileBrowser.class})
public class FileContentDownloadTest extends BaseWebDriverTest
{
    @BeforeClass
    public static void doSetup() throws Exception
    {
        FileContentDownloadTest initTest = (FileContentDownloadTest)getCurrentTest();

        initTest.doSetupSteps();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    private void doSetupSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Files");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testMultipleDownloadToZip() throws Exception
    {
        File file1 = getNextSampleFile();
        File file2 = getNextSampleFile();
        Set<String> expectedFiles = new HashSet<>();

        _fileBrowserHelper.uploadFile(file1);
        _fileBrowserHelper.uploadFile(file2);

        _fileBrowserHelper.selectFileBrowserItem(file1.getName());
        expectedFiles.add(file1.getName());
        _fileBrowserHelper.selectFileBrowserItem(file2.getName());
        expectedFiles.add(file2.getName());

        File download = clickAndWaitForDownload(FileBrowserHelper.BrowserAction.DOWNLOAD.button());
        assertEquals(getZipDownloadFileName(), download.getName());

        List<String> filesInZip = TestFileUtils.getFilesInZipArchive(download);
        for(String file : expectedFiles)
        {
            assertTrue(filesInZip.stream().anyMatch((f)-> f.endsWith(file)));
        }
    }

    @Test
    public void testFolderDownload() throws Exception
    {
        File file1 = getNextSampleFile();
        File file2 = getNextSampleFile();
        String folderName = "folderDownload";
        String subfolderName = "subFolder";
        Set<String> expectedFiles = new HashSet<>();

        String folderPath;
        _fileBrowserHelper.createFolder(folderName);
        folderPath = folderName + "/";
        _fileBrowserHelper.selectFileBrowserItem("/" + folderPath);
        _fileBrowserHelper.uploadFile(file1);
        expectedFiles.add(folderPath + file1.getName());

        _fileBrowserHelper.createFolder(subfolderName);
        folderPath = folderPath + subfolderName + "/";
        _fileBrowserHelper.selectFileBrowserItem("/" + folderPath);
        _fileBrowserHelper.uploadFile(file2);
        expectedFiles.add(folderPath + file2.getName());

        _fileBrowserHelper.selectFileBrowserItem("/" + folderName);
        File download = clickAndWaitForDownload(FileBrowserHelper.BrowserAction.DOWNLOAD.button());
        assertEquals(getZipDownloadFileName(), download.getName());

        List<String> filesInZip = TestFileUtils.getFilesInZipArchive(download);
        for(String file : expectedFiles)
        {
            assertTrue(filesInZip.stream().anyMatch((f)-> f.endsWith(file)));
        }
    }

    @Test
    public void testDoubleClickDownload()
    {
        final File file1 = getNextSampleFile();

        _fileBrowserHelper.uploadFile(file1);

        File download = doAndWaitForDownload(() -> doubleClick(FileBrowserHelper.Locators.gridRow(file1.getName())));
        assertEquals(file1.getName(), download.getName());
    }

    @Test
    public void testDoubleClickDoesNotDownloadFolder()
    {
        File file = getNextSampleFile();
        String folderName = "noDownload";

        _fileBrowserHelper.createFolder(folderName);
        _fileBrowserHelper.selectFileBrowserItem("/" + folderName + "/");

        _fileBrowserHelper.uploadFile(file);

        _fileBrowserHelper.selectFileBrowserRoot();

        waitForElement(FileBrowserHelper.Locators.gridRow(folderName));
        doubleClick(FileBrowserHelper.Locators.gridRow(folderName));

        waitForElement(FileBrowserHelper.Locators.gridRow(file.getName()));
    }

    @Test
    public void testDoubleClickDisplaysTxtInWindow()
    {
        File textFile = TestFileUtils.getSampleData("fileTypes/sample.txt");

        _fileBrowserHelper.uploadFile(textFile);

        waitForElement(FileBrowserHelper.Locators.gridRow(textFile.getName()));
        doubleClick(FileBrowserHelper.Locators.gridRow(textFile.getName()));

        switchToWindow(1);
        assertTextPresent("Sample text file");
        getDriver().close();
        switchToMainWindow();
    }

    @Test
    public void testRenderAsRedirect()
    {
        File textFile = TestFileUtils.getSampleData("fileTypes/sample.txt");

        String folderName = "redirectTest";
        _fileBrowserHelper.createFolder(folderName);
        doubleClick(FileBrowserHelper.Locators.gridRow(folderName));
        _fileBrowserHelper.uploadFile(textFile);

        signOut();
        // Test that renderAs can be observed through a login
        log("Test renderAs through login and ensure that page is rendered inside of server UI");
        beginAt("files/" + EscapeUtil.encode(getProjectName()) + "/%40files/" + EscapeUtil.encode(folderName) + "/" + textFile.getName() + "?renderAs=INLINE");
        assertTitleContains("Sign In");

        // If this succeeds, then page has been rendered in frame
        simpleSignIn();

        assertTextPresent("Sample text file");
    }

    private static int zipDownloads = 0;
    private String getZipDownloadFileName()
    {
        String duplicateFileUniquifier = "";
        if(zipDownloads > 0)
        {
            duplicateFileUniquifier = "(" + (zipDownloads) + ")";
            if (getBrowserType() == BrowserType.CHROME)
                duplicateFileUniquifier = " " + duplicateFileUniquifier;
        }
        String suffix = " files" + duplicateFileUniquifier + ".zip";
        zipDownloads++;
        return getProjectName() + suffix;
    }

    private static int sampleFileCounter = 0;
    private File getNextSampleFile()
    {
        return TestFileUtils.getSampleData("Affymetrix/CEL_files/sample_file_" + (++sampleFileCounter) + ".CEL");
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("filecontent");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

}
