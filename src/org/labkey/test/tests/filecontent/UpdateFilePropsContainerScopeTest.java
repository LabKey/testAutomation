/*
 * Copyright (c) 2026 LabKey Corporation
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.core.webdav.WebDavUtils;
import org.labkey.test.util.query.QueryApiHelper;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({Daily.class, FileBrowser.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class UpdateFilePropsContainerScopeTest extends BaseWebDriverTest
{
    private static final String FOLDER_A = "FolderA";
    private static final String FOLDER_B = "FolderB";
    private static final String CUSTOM_PROPERTY = "CustomProp";

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
        UpdateFilePropsContainerScopeTest init = getCurrentTest();
        init.doSetupSteps();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    private void doSetupSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_A);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_B);

        // FolderA needs a file properties domain so UpdateFilePropsAction's validation block runs.
        navigateToFolder(getProjectName(), FOLDER_A);
        new PortalHelper(this).doInAdminMode(p -> p.addWebPart("Files"));
        DomainDesignerPage designer = _fileBrowserHelper.goToEditProperties();
        designer.fieldsPanel().addField(CUSTOM_PROPERTY);
        designer.clickFinish();

        navigateToFolder(getProjectName(), FOLDER_B);
        new PortalHelper(this).doInAdminMode(p -> p.addWebPart("Files"));
    }

    @Test
    public void testForeignContainerFileRejected() throws Exception
    {
        final File localFile = TestFileUtils.getSampleData("security/InlineFile.html");
        navigateToFolder(getProjectName(), FOLDER_A);
        _fileBrowserHelper.uploadFile(localFile);
        String localFileUrl = WebDavUtils.buildBaseWebDavUrl(getProjectName() + "/" + FOLDER_A) + localFile.getName();

        goToProjectHome();
        final File foreignFile = TestFileUtils.getSampleData("security/InlineFile2.html");
        navigateToFolder(getProjectName(), FOLDER_B);
        _fileBrowserHelper.uploadFile(foreignFile);
        String foreignFileUrl = WebDavUtils.buildBaseWebDavUrl(getProjectName() + "/" + FOLDER_B) + foreignFile.getName();

        log("Same-container file id should be accepted.");
        updateFileProps(FOLDER_A, localFileUrl, localFile.getName());

        log("Foreign-container file id should be rejected.");
        try
        {
            updateFileProps(FOLDER_A, foreignFileUrl, foreignFile.getName());
            fail("Expected rejection: UpdateFilePropsAction must refuse a file id resolving outside the current folder.");
        }
        catch (CommandException ex)
        {
            assertTrue("Expected 'Invalid file' in error message, got: " + ex.getMessage(),
                    ex.getMessage().contains("Invalid file"));
        }
    }

    private void updateFileProps(String folder, String fileId, String fileName) throws Exception
    {
        JSONObject entry = new JSONObject();
        entry.put("id", fileId);
        entry.put("name", fileName);
        entry.put(CUSTOM_PROPERTY, "value");
        JSONArray files = new JSONArray();
        files.put(entry);
        JSONObject body = new JSONObject();
        body.put("files", files);

        SimplePostCommand cmd = new SimplePostCommand("filecontent", "updateFileProps");
        cmd.setJsonObject(body);
        cmd.execute(WebTestHelper.getRemoteApiConnection(), getProjectName() + "/" + folder);
    }

    @Test
    public void testAbsoluteFilePathMustBeUnderFolderRoot() throws Exception
    {
        final File localFile = TestFileUtils.getSampleData("security/InlineFile_drop.html");
        navigateToFolder(getProjectName(), FOLDER_A);
        _fileBrowserHelper.uploadFile(localFile);
        String localFileUrl = WebDavUtils.buildBaseWebDavUrl(getProjectName() + "/" + FOLDER_A) + localFile.getName();

        goToProjectHome();
        final File foreignFile = TestFileUtils.getSampleData("security/InlineFile.html");
        navigateToFolder(getProjectName(), FOLDER_B);
        _fileBrowserHelper.uploadFile(foreignFile);

        log("A path under the folder's own file root should be accepted.");
        insertFileRow(FOLDER_A, absoluteFilePath(FOLDER_A, localFile.getName()), localFileUrl);

        log("A path outside the folder's file roots should be rejected.");
        assertRejected(FOLDER_A, absoluteFilePath(FOLDER_B, foreignFile.getName()), localFileUrl);

        log("A traversing path that starts with the folder's own root should be rejected.");
        Path localDir = Paths.get(absoluteFilePath(FOLDER_A, localFile.getName())).getParent();
        Path foreignPath = Paths.get(absoluteFilePath(FOLDER_B, foreignFile.getName()));
        String traversing = localDir.resolve(localDir.relativize(foreignPath)).toString();
        assertTrue("Expected a traversing path, got: " + traversing, traversing.contains(".."));
        assertRejected(FOLDER_A, traversing, localFileUrl);
    }

    private void assertRejected(String folder, String absoluteFilePath, String davUrl)
    {
        try
        {
            insertFileRow(folder, absoluteFilePath, davUrl);
            fail("Expected rejection: an absolute path outside the folder's managed file roots must not be stored: " + absoluteFilePath);
        }
        catch (Exception ex)
        {
            assertTrue("Expected 'managed file roots' in error message, got: " + ex.getMessage(),
                    ex.getMessage() != null && ex.getMessage().contains("managed file roots"));
        }
    }

    /** The uploaded file's server-side path, read back from its exp.Data row so the test needn't know the file root layout. */
    private String absoluteFilePath(String folder, String fileName) throws Exception
    {
        SelectRowsResponse response = new QueryApiHelper(WebTestHelper.getRemoteApiConnection(), getProjectName() + "/" + folder, "exp", "Data")
                .selectRows(List.of("Name", "DataFileUrl"), List.of(new Filter("Name", fileName)));
        assertEquals("Expected a single exp.Data row for " + fileName, 1, response.getRows().size());
        return Paths.get(URI.create((String) response.getRows().get(0).get("DataFileUrl"))).toString();
    }

    private void insertFileRow(String folder, String absoluteFilePath, String davUrl) throws Exception
    {
        new QueryApiHelper(WebTestHelper.getRemoteApiConnection(), getProjectName() + "/" + folder, "exp", "Data")
                .insertRows(List.of(Map.of("AbsoluteFilePath", absoluteFilePath, "davUrl", davUrl)));
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
