package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.FilesWebPart;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.PortalHelper;
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
        UserEditorWithoutDeleteTest init = (UserEditorWithoutDeleteTest) getCurrentTest();
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

        goToProjectHome();
        new PortalHelper(this).addWebPart("Files");
    }

    /*
        Regression coverage for Secure Issue 51500: Editor without Delete users are able to delete files
     */
    @Test
    public void testFileDeleteWithEditorWithoutDeleteUser()
    {
        goToProjectHome();
        impersonate(EDITOR_WITHOUT_DELETE.getEmail());
        FilesWebPart filesWebPart = FilesWebPart.getWebPart(getDriver());
        filesWebPart.fileBrowser().uploadFile(TEST_FILE);
        Assert.assertFalse("Delete action should not be present for " + EDITOR_WITHOUT_DELETE.getEmail(),
                filesWebPart.fileBrowser().isActionPresent(FileBrowserHelper.BrowserAction.DELETE));

        WebDavUploadHelper uploadHelper = new WebDavUploadHelper(WebDavUrlFactory.webDavUrlFactory(getProjectName()),
                WebDavUtils.beginSardine(getCurrentUser()));
        uploadHelper.uploadFile(TEST_FILE);
        uploadHelper.deleteFile(WebDavUtils.buildBaseWebDavUrl(getProjectName()) + TEST_FILE.getName(), true);
        Assert.assertTrue("File should not be deleted by " + EDITOR_WITHOUT_DELETE.getEmail(),
                uploadHelper.fileExists(WebDavUtils.buildBaseWebDavUrl(getProjectName()) + TEST_FILE.getName()));
        stopImpersonating();
    }

    @Override
    protected String getProjectName()
    {
        return "UserEditorWithoutDeleteTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
