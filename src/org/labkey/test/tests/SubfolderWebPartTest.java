package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.FilesWebPart;
import org.labkey.test.components.SubfoldersWebPart;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class SubfolderWebPartTest extends BaseWebDriverTest
{
    public static String otherProjectName = "OtherSubfolderWebPartTest Project";
    public static String FOLDER_ONE_A = "Folder One A";
    public static String FOLDER_ONE_B = "Folder One B";
    public static String FOLDER_TWO_A = "Folder Two A";
    public static String FOLDER_TWO_B = "Folder Two B";
    public static String FOLDER_THREE_A = "Folder Three A";
    public static String FOLDER_WITH_USERS = "Folder With Users";
    public static String TEST_USER = "subfolderwebpartrest@test.testuser";
    private static final File TEST_FILE = TestFileUtils.getSampleData("study/Protocol.txt");

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        SubfolderWebPartTest init = (SubfolderWebPartTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), "Collaboration");
        _userHelper.createUser(TEST_USER);

        if (!_containerHelper.doesContainerExist(otherProjectName))
        {
            _containerHelper.createProject(otherProjectName, "Collaboration");
            _containerHelper.addCreatedProject(otherProjectName);

            // now put a series of folders in otherproject (we will import them to the main project during the test)
            _containerHelper.createSubfolder(otherProjectName, FOLDER_ONE_A, "Collaboration");
            _containerHelper.createSubfolder(otherProjectName, FOLDER_ONE_B, "Collaboration");
            _containerHelper.createSubfolder(otherProjectName + "/" + FOLDER_ONE_A, FOLDER_TWO_A, "Collaboration");
            _containerHelper.createSubfolder(otherProjectName + "/" + FOLDER_ONE_A, FOLDER_TWO_B, "Collaboration");

            _containerHelper.createSubfolder(otherProjectName + "/" + FOLDER_ONE_A + "/" + FOLDER_TWO_A, FOLDER_THREE_A, "Collaboration");
            _containerHelper.createSubfolder(otherProjectName + "/" + FOLDER_ONE_A + "/" + FOLDER_TWO_A, FOLDER_WITH_USERS, "Collaboration");

            projectMenu().navigateToFolder(otherProjectName, FOLDER_THREE_A);
            portalHelper.addWebPart("Files");
            FilesWebPart filesWebPart = FilesWebPart.getWebPart(getDriver());
            filesWebPart.fileBrowser().uploadFile(TEST_FILE);

            // give a user folder-specific role here
            projectMenu().navigateToFolder(otherProjectName, FOLDER_WITH_USERS);
            navBar().goToPermissionsPage().setUserPermissions(TEST_USER, "Editor");
        }

    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    @Test
    public void importAndValidateFolderSet() throws Exception
    {

        // begin in the source project
        goToProjectHome(otherProjectName);

        // confirm expected folders are here using the subfolders webpart
        SubfoldersWebPart subfoldersWebPart = SubfoldersWebPart.getWebPart(getDriver());
        waitForElement(SubfoldersWebPart.Locators.folderLabel.withText(FOLDER_ONE_A));
        List<String> subFolderNames = subfoldersWebPart.GetSubfolderNames();
        assertTrue(subFolderNames.contains(FOLDER_ONE_A.toUpperCase()));
        assertTrue(subFolderNames.contains(FOLDER_ONE_B.toUpperCase()));

        // now export the source folder set to a zip archive
        File exportedFolders = exportFolderAsZip(FOLDER_ONE_A, false, true, true, true);

        goToProjectHome();
        // import the folders to the destination project
        importFolderFromZip(exportedFolders, true, 1, false);

        // confirm we start at the second lavel (the root of the folder we imported was the first level)
        goToProjectHome();
        waitForElement(SubfoldersWebPart.Locators.folderLabel.withText(FOLDER_TWO_A));
        SubfoldersWebPart homeWebPart = SubfoldersWebPart.getWebPart(getDriver());
        List<String> subFolders = homeWebPart.GetSubfolderNames();
        assertTrue(subFolders.contains(FOLDER_TWO_A.toUpperCase()));
        assertTrue(subFolders.contains(FOLDER_TWO_B.toUpperCase()));

        // walk into subfolder with users, confirm the user/role migrated as expected
        homeWebPart.goToSubfolder(FOLDER_TWO_A);
        homeWebPart = SubfoldersWebPart.getWebPart(getDriver());
        homeWebPart.goToSubfolder(FOLDER_WITH_USERS);
        boolean userRoleMigratedAsExpected = navBar().goToPermissionsPage().isUserInRole(TEST_USER, "Editor");
        assertTrue("The user role did not migrate with folder import", userRoleMigratedAsExpected);

        // now validate files
        goToProjectHome();
        projectMenu().navigateToFolder(getProjectName(), FOLDER_THREE_A);
        FilesWebPart filesWebPart = FilesWebPart.getWebPart(getDriver());
        boolean fileMigratedAsExpected = filesWebPart.fileBrowser().fileIsPresent("Protocol.txt");
        assertTrue("the file was not present in the expected folder", fileMigratedAsExpected);
    }

    @Test
    public void createSubdirectoryFromWebPart()
    {
        goToProjectHome();
        SubfoldersWebPart subfoldersWebPart = SubfoldersWebPart.getWebPart(getDriver());
        String newSubFolderName = "From The Webpart";

        subfoldersWebPart.clickCreateSubfolder()
                .selectFolderType("Collaboration")
                .setFolderName(newSubFolderName)
                .clickNext()
                .setMyUserOnly()
                .clickFinish();

        goToProjectHome();
        subfoldersWebPart = SubfoldersWebPart.getWebPart(getDriver());
        subfoldersWebPart.goToSubfolder(newSubFolderName);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "SubfolderWebPartTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}