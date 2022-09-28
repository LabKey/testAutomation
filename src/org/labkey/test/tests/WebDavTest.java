/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.ConfigureFileSystemAccessPage;
import org.labkey.test.pages.files.WebDavPage;
import org.labkey.test.pages.files.WebFilesHtmlViewPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.core.webdav.WebDavUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class WebDavTest extends BaseWebDriverTest
{
    private static final String TEXT = "Four score and seven years ago our fathers brought forth on this continent a new nation, conceived in liberty, and dedicated to the proposition that all men are created equal.\n"+
    "Now we are engaged in a great civil war, testing whether that nation, or any nation, so conceived and so dedicated, can long endure. We are met on a great battle-field of that war. We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this.\n"+
    "But, in a larger sense, we can not dedicate, we can not consecrate, we can not hallow this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. It is rather for us to be here dedicated to the great task remaining before us-that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion-that we here highly resolve that these dead shall not have died in vain-that this nation, under God, shall have a new birth of freedom-and that government of the people, by the people, for the people, shall not perish from the earth.";

    private final String OTHER_PROJECT = "WebFileCache Project";
    private final String baseWebDavUrl = WebDavUtils.buildBaseWebDavUrl(getProjectName(), "@files/");
    private final String baseWebFilesUrl = WebDavUtils.buildBaseWebfilesUrl(getProjectName());

    @Override
    protected String getProjectName()
    {
        return "WebDavTest Project";
    }

    @BeforeClass
    public static void setupProject()
    {
        WebDavTest init = (WebDavTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    protected String getFirstExpectedFile()
    {
        return "@files";
    }

    protected boolean isCloudTest()
    {
        return false;
    }

    @Before
    public void cleanFiles()
    {
        goToProjectHome();
        goToModule("FileContent");
        _fileBrowserHelper.deleteAll();
    }

    @Test
    public void testWebDav() throws Exception
    {
        final String testURL = baseWebDavUrl;
        final String fileName = "testfile_wd.txt";

        Set<String> expectedFiles = new HashSet<>();
        expectedFiles.add(getFirstExpectedFile());

        beginAt(testURL + "?listing=html");
        assertTextNotPresent("testfile");
        assertElementPresent(Locator.linkWithText("_webdav/"));

        Sardine sardine = WebDavUtils.beginSardine(PasswordUtil.getUsername());
        List<String> names = _listNames(sardine, testURL);
        assertEquals("Initial webdav listing", expectedFiles, new HashSet<>(names));

        sardine.put(testURL + fileName, TEXT.getBytes(StandardCharsets.UTF_8));
        expectedFiles.add(fileName);
        refresh();
        assertTextPresent(fileName);
        names = _listNames(sardine,testURL);
        assertEquals("Webdav listing after upload", expectedFiles, new HashSet<>(names));

        // TODO test search

        waitForIdle();
        deleteFile(sardine, testURL + fileName);
        expectedFiles.remove(fileName);

        refresh();
        assertTextNotPresent(fileName);
        names = _listNames(sardine,testURL);
        assertEquals("Webdav listing after deleting file", expectedFiles, new HashSet<>(names));

        verifyExpected404(sardine, testURL);
    }

    /**
     * Regression Test -- 21936: return SC_BAD_REQUEST instead of IllegalArgumentException for bad request
     */
    @Test
    public void testBadUrlResponse() throws Exception
    {
        SimpleHttpRequest request = new SimpleHttpRequest(baseWebDavUrl + "?uf=%uf");
        SimpleHttpResponse response = request.getResponse();
        assertEquals("Wrong response for unparsable parameter", HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
    }

    @Test
    public void testWebfiles() throws Exception
    {
        Assume.assumeFalse("App admin is unable to enable WebFiles", TestProperties.isPrimaryUserAppAdmin());
        Assume.assumeFalse("Don't test webfiles for S3", isCloudTest());

        setEnableWebfiles(true);

        Set<String> expectedWebFiles = new HashSet<>();
        expectedWebFiles.add(getProjectName());

        String testURL = baseWebFilesUrl;

        WebFilesHtmlViewPage.beginAt(this, getProjectName());
        assertElementPresent(Locator.linkWithText("_webfiles/"));

        Sardine sardine = WebDavUtils.beginSardine(PasswordUtil.getUsername());
        Set<String> names = new HashSet<>(_listNames(sardine, testURL));
        assertEquals("_webfiles for project should be empty (with just a self-reference)", expectedWebFiles, names);

        String projectFileName = "testfile_wf1.txt";
        log("Verify creating file under project");
        sardine.put(testURL + projectFileName, TEXT.getBytes(StandardCharsets.UTF_8));
        expectedWebFiles.add(projectFileName);
        refresh();
        names = new HashSet<>(_listNames(sardine, testURL));
        assertEquals("Content for _webfiles is not as expected", expectedWebFiles, names);
        assertTrue(projectFileName + " file is not present in _webfiles", names.contains(projectFileName));

        String filesRootName = getFirstExpectedFile();
        String conflictFolderName = "childContainerAndDirectory";
        log("Create a subdirectory: \"" + conflictFolderName + "\" under project (which maps to " + filesRootName + ")");
        sardine.createDirectory(testURL + conflictFolderName);
        String childFileName = "testfile_wf2.txt";
        log("Create a file: \"" + childFileName + "\" under subdirectory: \"" + conflictFolderName + "\"");
        sardine.put(testURL + conflictFolderName + "/" + childFileName, TEXT.getBytes(StandardCharsets.UTF_8));
        log("Verify file under subdirectory");
        beginAt(testURL + conflictFolderName + "?listing=html");
        assertTextPresent(childFileName);

        log("Create a child container: \"" + conflictFolderName + "\" under project");
        _containerHelper.createSubfolder(getProjectName(), conflictFolderName);

        log("Verify that subdirectory name that conflicts with child container is decorated correctly for _webfiles");
        beginAt(testURL + "?listing=html");
        String expectedConflictingDirectoryName = conflictFolderName + " (files)";
        expectedWebFiles.add(expectedConflictingDirectoryName);
        expectedWebFiles.add(conflictFolderName);

        names = new HashSet<>(_listNames(sardine, testURL));
        assertEquals("Content for _webfiles is not as expected", expectedWebFiles, new HashSet<>(names));

        log("Verify file and sub directories uploaded to project from _webfiles is present at " + filesRootName + " _webdav node");
        beginAt(baseWebDavUrl + "?listing=html");
        names = new HashSet<>(_listNames(sardine, baseWebDavUrl));
        assertEquals("Content for _webdav/" + filesRootName + " is not as expected", new HashSet<>(Arrays.asList(filesRootName, conflictFolderName, projectFileName)), names);

        goToProjectHome();
        _containerHelper.deleteFolder(getProjectName(), conflictFolderName, WAIT_FOR_PAGE * 3);

        waitForIdle();
        log("Verify deleting file from _webfiles");
        deleteFile(sardine, testURL + projectFileName);
        deleteFile(sardine, testURL + conflictFolderName + "/" + childFileName);
        deleteFile(sardine, testURL + conflictFolderName);
        beginAt(baseWebDavUrl + "?listing=html");
        if (!isCloudTest())
        {
            // In Cloud webdav, for perf we maintain chilren list in all folders, so "outside" delete
            // doesn't appear until cache is refreshed (every 5 minutes)
            assertTextNotPresent(projectFileName);
            names = new HashSet<>(_listNames(sardine, testURL));
            assertEquals(new HashSet<>(Arrays.asList(getProjectName())), names);
        }

        verifyExpected404(sardine, testURL);

        log("Verify _webfiles is not available after disabling it from site settings");
        setEnableWebfiles(false);
        verifyExpected404(sardine, testURL, false);
    }

    @Test // 35730: WebDav: newly created project not showing up in _webfiles
    public void testWebfilesCaching()
    {
        Assume.assumeFalse("App admin is unable to enable WebFiles", TestProperties.isPrimaryUserAppAdmin());

        setEnableWebfiles(true);

        log("Visit WebDav and WebFiles to initialize cache");
        WebDavPage.beginAt(this, "");
        assertTextNotPresent(OTHER_PROJECT);
        WebFilesHtmlViewPage.beginAt(this, "");
        assertTextNotPresent(OTHER_PROJECT);

        _containerHelper.createProject(OTHER_PROJECT, null);

        log("Visit WebDav and WebFiles after project creation");
        WebDavPage.beginAt(this, "");
        assertTextPresent(OTHER_PROJECT);
        WebFilesHtmlViewPage.beginAt(this, "");
        assertTextPresent(OTHER_PROJECT);
    }

    private void verifyExpected404(Sardine sardine, String testURL)
    {
        verifyExpected404(sardine, testURL, true);
    }

    private void verifyExpected404(Sardine sardine, String testURL, boolean useExtra)
    {
        try
        {
            sardine.list(testURL + (useExtra ? "nonexistent/" : ""));
            fail("Expected 404");
        }
        catch (IOException x)
        {
            assertTrue(x.toString().contains("404"));
        }
    }

    private void deleteFile(Sardine sardine, String fileURL)
    {
        for (int retry=0 ; retry<3 ; retry++)
        {
            try
            {
                // give search indexer time to index and release lock
                sleep(100);
                sardine.delete(fileURL);
                break;
            }
            catch (IOException x)
            {
            }
        }
    }

    @LogMethod (quiet = true)
    private void setEnableWebfiles(@LoggedParam boolean enable)
    {
        ConfigureFileSystemAccessPage.beginAt(this)
            .setEnableWebFiles(enable)
            .save();
    }

    private void waitForIdle()
    {
        WebTestHelper.getHttpResponse(WebTestHelper.buildURL("search", "waitForIdle"));
    }

    private List<String> _listNames(Sardine s, String path) throws IOException
    {
        log("Fetching file list: " + path);
        ArrayList<String> names = new ArrayList<>();
        for (DavResource r : s.list(path))
        {
            // ignore .deleted and .upload.log
            if (r.getName().startsWith("."))
                continue;
            names.add(r.getName());
        }
        return names;
    }


    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(OTHER_PROJECT, false);       // Don't fail if not all tests were requested
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
