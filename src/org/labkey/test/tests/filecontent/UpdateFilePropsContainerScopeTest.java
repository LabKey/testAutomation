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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.core.webdav.WebDavUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
