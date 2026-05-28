/*
 * Copyright (c) 2024-2026 LabKey Corporation
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

import com.github.sardine.impl.SardineException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.TestUser;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;
import org.labkey.test.util.core.webdav.WebDavUrlFactory;
import org.labkey.test.util.core.webdav.WebDavUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
public class UserEditorWithoutDeleteTest extends BaseWebDriverTest
{
    private static final TestUser EDITOR_WITHOUT_DELETE = new TestUser("editorwithoutdelete@user.test");
    private static final File TEST_FILE = TestFileUtils.getSampleData("studies/Read_Me.txt");

    @BeforeClass
    public static void setupProject()
    {
        UserEditorWithoutDeleteTest init = getCurrentTest();
        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, EDITOR_WITHOUT_DELETE);
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        EDITOR_WITHOUT_DELETE.create(this)
                .setInitialPassword()
                .addPermission("EditorWithoutDelete", getProjectName());
    }

    /*
        Regression coverage for Secure Issue 51500: Editor without Delete users are able to delete files
     */
    @Test
    public void testFileDeleteWithEditorWithoutDeleteUser()
    {
        goToProjectHome();
        goToModule("FileContent");
        impersonate(EDITOR_WITHOUT_DELETE.getEmail());
        _fileBrowserHelper.uploadFile(TEST_FILE);
        Assert.assertFalse("Delete action should not be present for " + EDITOR_WITHOUT_DELETE.getEmail(),
                _fileBrowserHelper.isActionPresent(FileBrowserHelper.BrowserAction.DELETE));

        WebDavUploadHelper uploadHelper = new WebDavUploadHelper(WebDavUrlFactory.webDavUrlFactory(getProjectName()),
                WebDavUtils.beginSardine(getCurrentUser()));
        uploadHelper.uploadFile(TEST_FILE);
        SardineException sardineException = uploadHelper.deleteExpectingError(TEST_FILE.getName());
        Assert.assertEquals("Incorrect response for deletion", 403, sardineException.getStatusCode());
        Assert.assertTrue("File should not be deleted by " + EDITOR_WITHOUT_DELETE.getEmail(),
                uploadHelper.fileExists(TEST_FILE.getName()));

        stopImpersonating();

        log("Verify delete exists for admin");
        goToProjectHome();
        goToModule("FileContent");
        Assert.assertTrue("Delete action should be present for " + getCurrentUser(),
                _fileBrowserHelper.isActionPresent(FileBrowserHelper.BrowserAction.DELETE));
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + TRICKY_CHARACTERS_FOR_PROJECT_NAMES + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("FileContent");
    }
}
