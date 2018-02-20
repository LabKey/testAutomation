/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.pages.files.WebFilesPage;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(BVT.class)
public class WebDavTest extends BaseWebDriverTest
{
    final String PROJECT_NAME="WebDavTest";
    final String TEXT = "Four score and seven years ago our fathers brought forth on this continent a new nation, conceived in liberty, and dedicated to the proposition that all men are created equal.\n"+
    "Now we are engaged in a great civil war, testing whether that nation, or any nation, so conceived and so dedicated, can long endure. We are met on a great battle-field of that war. We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this.\n"+
    "But, in a larger sense, we can not dedicate, we can not consecrate, we can not hallow this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. It is rather for us to be here dedicated to the great task remaining before us-that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion-that we here highly resolve that these dead shall not have died in vain-that this nation, under God, shall have a new birth of freedom-and that government of the people, by the people, for the people, shall not perish from the earth.";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject()
    {
        WebDavTest init = (WebDavTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
    }

    @Test
    public void testWebDav() throws Exception
    {
        // including context Path
        String baseURL = getBaseURL();
        String testDirectory = "/_webdav/" + getProjectName() + "/@files/";
        String testURL = baseURL + testDirectory;

        // make sure the indexer isn't really busy
        waitForIdle();

        beginAt(testDirectory + "?listing=html");
        assertTextNotPresent("testfile1");

        Sardine sardine = SardineFactory.begin(PasswordUtil.getUsername(), PasswordUtil.getPassword());
        List<String> names = _listNames(sardine, testURL);
        assertEquals(1, names.size());
        assertFalse(names.contains("testfile1"));

        sardine.put(testURL + "testfile1.txt", TEXT.getBytes(StandardCharsets.UTF_8));
        refresh();
        assertTextPresent("testfile1.txt");
        names = _listNames(sardine,testURL);
        assertEquals(2, names.size());
        assertTrue(names.contains("testfile1.txt"));

        // TODO test search

        deleteFile(sardine, testURL + "testfile1.txt");

        refresh();
        assertTextNotPresent("testfile1.txt");
        names = _listNames(sardine,testURL);
        assertEquals(1, names.size());
        assertFalse(names.contains("testfile1.txt"));

        verifyExpected404(sardine, testURL);
    }

    /**
     * Regression Test -- 21936: return SC_BAD_REQUEST instead of IllegalArgumentException for bad request
     */
    @Test
    public void testBadUrlResponse() throws Exception
    {
        SimpleHttpRequest request = new SimpleHttpRequest(WebTestHelper.getBaseURL() + "/_webdav?uf=%uf");
        SimpleHttpResponse response = request.getResponse();
        assertEquals("Wrong response for unparsable parameter", HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
    }

    @Test
    public void testWebfiles() throws Exception
    {
        setEnableWebfiles(true);

        // including context Path
        String baseURL = getBaseURL();
        String testDirectory = "/_webfiles/" + getProjectName() + "/";
        String testURL = baseURL + testDirectory;

        // make sure the indexer isn't really busy
        waitForIdle();
        WebFilesPage.beginAt(this, getProjectName());

        Sardine sardine = SardineFactory.begin(PasswordUtil.getUsername(), PasswordUtil.getPassword());
        List<String> names = _listNames(sardine, testURL);
        assertEquals("_webfiles for project should be empty", 1, names.size());

        String projectFileName = "testfile1.txt";
        log("Verify creating file under project");
        sardine.put(testURL + projectFileName, TEXT.getBytes(StandardCharsets.UTF_8));
        refresh();
        names = _listNames(sardine, testURL);
        assertEquals("Content for _webfiles is not as expected", 2, names.size());
        assertTrue(projectFileName + " file is not present in _webfiles", names.contains(projectFileName));

        String conflictFolderName = "childContainerAndDirectory";
        log("Create a subdirectory: \"" + conflictFolderName + "\" under project (which maps to @files of project)");
        sardine.createDirectory(testURL + conflictFolderName);
        String childFileName = "testfile2.txt";
        log("Create a file: \"" + childFileName + "\" under subdirectory: \"" + conflictFolderName + "\"");
        sardine.put(testURL + conflictFolderName + "/" + childFileName, TEXT.getBytes(StandardCharsets.UTF_8));
        log("Verify file under subdirectory");
        beginAt(testDirectory + conflictFolderName + "?listing=html");
        assertTextPresent(childFileName);

        log("Create a child container: \"" + conflictFolderName + "\" under project");
        _containerHelper.createSubfolder(getProjectName(), conflictFolderName);

        log("Verify that subdirectory name that conflicts with child container is decorated correctly for _webfiles");
        beginAt(testDirectory + "?listing=html");
        String expectedConflictingDirectoryName = conflictFolderName + " (files)";
        names = _listNames(sardine, testURL);
        assertEquals("Content for _webfiles is not as expected", 4, names.size());
        log(names.toString());
        assertTrue(projectFileName + " file is not present in _webfiles", names.contains(projectFileName));
        assertTrue(conflictFolderName + " child container is not present in _webfiles", names.contains(conflictFolderName));
        assertTrue(conflictFolderName + " directory is not present in _webfiles", names.contains(expectedConflictingDirectoryName));

        log("Verify file and sub directories uploaded to project from _webfiles is present at @files _webdav node");
        String webDavTestDirectory = "/_webdav/" + getProjectName() + "/@files/";
        String webDavtestURL = baseURL + webDavTestDirectory;
        beginAt(webDavTestDirectory + "?listing=html");
        names = _listNames(sardine,webDavtestURL);
        assertEquals("Content for _webdav/@files is not as expected", 3, names.size());
        assertTrue(projectFileName + " file is not present in _webdav/@files", names.contains(projectFileName));
        assertTrue(conflictFolderName + " directory is not present in _webdav/@files", names.contains(conflictFolderName));

        goToProjectHome();
        _containerHelper.deleteFolder(getProjectName(), conflictFolderName);

        log("Verify deleting file from _webfiles");
        beginAt(testDirectory + "?listing=html");
        deleteFile(sardine, testURL + projectFileName);
        deleteFile(sardine, testURL + conflictFolderName + "/" + childFileName);
        deleteFile(sardine, testURL + conflictFolderName);
        refresh();
        assertTextNotPresent(projectFileName);
        names = _listNames(sardine,testURL);
        assertFalse(names.contains(projectFileName));

        verifyExpected404(sardine, testURL);

        log("Verify _webfiles is not available after disabling it from site settings");
        setEnableWebfiles(false);
        verifyExpected404(sardine, testURL, false);
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

    private void setEnableWebfiles(boolean enable)
    {
        goToProjectHome();
        goToAdminConsole()
            .clickFiles()
            .setEnableWebFiles(enable)
            .save();
    }

    private void waitForIdle()
    {
        // make sure the indexer isn't really busy
        String baseURL = getBaseURL();
        String waitForIdle = baseURL + "/search-waitForIdle.view";
        beginAt(waitForIdle);
    }

    private List<String> _listNames(Sardine s, String path) throws IOException
    {
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
