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

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.folder.xml.FolderDocument;
import org.labkey.security.xml.GroupsType;
import org.labkey.security.xml.roleAssignment.RoleAssignmentsType;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper.PrincipalType;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.ZipUtil;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 17)
public class FolderExportTest extends BaseWebDriverTest
{
    private final ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);
    protected final APIUserHelper _userHelper = new APIUserHelper(this);

    String[] webParts = {"Study Overview", "Data Pipeline", "Datasets", "Specimens", "Views", "Test wiki", "Study Data Tools", "Lists", "~!@#$%^&*()_+query web part", "Report web part", "Workbooks"};
    File dataDir = TestFileUtils.getSampleData("FolderExport");
    private static final String folderFromZip = "1 Folder From Zip"; // add numbers to folder names to keep ordering for created folders
    private static final String folderFromPipelineZip = "2 Folder From Pipeline Zip";
    private static final String folderFromPipelineExport = "3 Folder From Pipeline Export";
    private static final String folderFromTemplate = "4 Folder From Template";
    private static final String folderWithPermissions = "5 Folder From Zip With Permissions";
    private static final String folderInheritingPermissions = "6 Inheriting";
    private static final String folderArchive = "SampleWithSubfolders.folder";
    private static final String folderZip = folderArchive + ".zip";
    private static final String projectPermsZip = "ProjectWithPerms.folder.zip";
    private static final String projectSubfolderPermsZip = "ProjectWithSubfoldersAndPerms.folder.zip";
    private static final String subfolderPermsZip = "SubfolderWithPerms.folder.zip";
    private static final String inheritedPermsZip = "InheritingSubfolder.folder.zip";

    // These specific usernames are used in the imported folder
    private static final String testUser1 = "testuser1@test.me";
    private static final String testUser2 = "testuser2@test.me";
    private static final String testUser3 = "testuser3@test.me";
    private static final String testUser4 = "testuser4@test.me";
    private static final String submitterGroup = "Submitters";
    private static final String superTesterGroup = "Super Testers";
    private static final String parentGroup = "Parent Group";
    private static final String groupGroup = "Group Group";
    private static final String emptyGroup = "Empty Group";
    private static final String existingGroup = "Existing Group";

    private static final String[] testUsers = new String[]{testUser1, testUser2, testUser3, testUser4};
    private static final String[] importedGroups = new String[]{submitterGroup, superTesterGroup, parentGroup, groupGroup, emptyGroup};
    private static final String[] notImportedGroups = new String[]{};

    private static final String[] importProjects = new String[]{
            "FolderImportTest 1",
            "FolderImportTest 2",
            "FolderImportTest 3",
            "FolderImportTest 4",
            "FolderImportTest 5",
            "FolderImportTest 6"};

    @Override
    protected String getProjectName()
    {
        return "FolderExportTest";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void checkLinks(){} // too many folders

    @Override
    protected Set<String> excludeFromViewCheck()
    {
        Set<String> folders = new HashSet<>();
        folders.add(folderFromTemplate);
        return folders;
    }

    @BeforeClass
    public static void setupProject() throws IOException
    {
        FolderExportTest init = (FolderExportTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws IOException
    {
        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();
        _containerHelper.createProject(getProjectName(), null);

        createUsersAndGroupsWithPermissions();

        new File(dataDir, folderZip).delete();
        ZipUtil zipFolder = new ZipUtil(new File(dataDir, folderArchive));
        zipFolder.zipInto(dataDir);
    }

    @Test
    public void testImport()
    {
        verifyImportFromZip();
        verifyCreateFolderFromTemplate(); // Uses importFromZip as template
    }

    @Test
    public void testExportImportWithSpecialCharactersInFileName()
    {
        String sourceFolder = "Source_Folder~,!@#$%%++_)(&+=[{";
        String targetFolder = "Target_Folder~!@#$%%++_)(&+=]},";
        String dir = "test~!@#$%(%)+-_=+_[]{}";
        String uploadFileName = "pdf_sample_with+%$@+%%+#-+=.pdf";

        goToHome();

        log("Creating " + sourceFolder);
        _containerHelper.createProject(sourceFolder, "Collaboration");

        log("Creating dir " + dir + " in Files webpart");
        goToProjectHome(sourceFolder);
        goToModule("FileContent");
        _fileBrowserHelper.createFolder(dir);

        log("Uploading file '" + uploadFileName + "' in " + dir);
        _fileBrowserHelper.selectFileBrowserItem("/" + dir + "/");
        _fileBrowserHelper.uploadFile(TestFileUtils.getSampleData("fileTypes/" + uploadFileName));

        goToProjectHome(sourceFolder);

        log("Exporting " + sourceFolder);
        File sourceZip = exportFolderAsZip(sourceFolder, false, false, false, true);

        log("Creating " + targetFolder);
        _containerHelper.createProject(targetFolder, "Collaboration");

        log("Importing " + sourceZip.getName() + " to " + targetFolder);
        goToProjectHome(targetFolder);
        importFolderFromZip(sourceZip, false, 1);

        log("Verify presence of " + uploadFileName);
        goToModule("FileContent");
        _fileBrowserHelper.selectFileBrowserItem(dir + "/" + uploadFileName);
        File downloadedFile = _fileBrowserHelper.downloadSelectedFiles();
        assertEquals("Expected file '" + uploadFileName + "' did not get downloaded", uploadFileName, downloadedFile.getName());

        log("Test Drag and Drop zip folder '" + sourceZip.getName() + "'");
        _fileBrowserHelper.dragAndDropFileInDropZone(sourceZip);
    }

    @Test
    public void testImportProjectWithoutUsers()
    {
        //test project import with users that do not exist
        _userHelper.deleteUsers(false, testUsers);
        _containerHelper.createProject(importProjects[0], null);
        importFolder(importProjects[0], projectPermsZip);
        verifyProjectGroups(importProjects[0], notImportedGroups, importedGroups, false);
        verifyRoleAssignments(false, true, false);
    }

    @Test
    public void testImportProjectWithUsersNoGroups()
    {
        // test project import with groups that do not exist
        _containerHelper.createProject(importProjects[1], null);
        createUsers(testUsers);
        importFolder(importProjects[1], projectPermsZip);
        verifyProjectGroups(importProjects[1], notImportedGroups, importedGroups, true);
        verifyRoleAssignments(true, true, false);
    }

    @Test
    public void testImportProjectWithExistingUsersAndGroups()
    {
        // test project import with all users and some groups existing
        createUsers(testUsers);
        _containerHelper.createProject(importProjects[2], null);
        _permissionsHelper.createPermissionsGroup(submitterGroup, testUser4);
        _permissionsHelper.createPermissionsGroup(parentGroup, testUser3);
        importFolder(importProjects[2], projectPermsZip);
        verifyProjectGroups(importProjects[2], notImportedGroups, importedGroups, true);
        verifyRoleAssignments(true, true, false);
        // existing group should be overwritten by imported group
        _permissionsHelper.assertUserNotInGroup(testUser4, submitterGroup, importProjects[2], PrincipalType.USER);
    }

    @Test
    public void testImportProjectToSubfolder()
    {
        // test folder import of project export
        createUsers(testUsers);
        _containerHelper.deleteProject(importProjects[3], false);
        _containerHelper.createProject(importProjects[3], null);
        _containerHelper.createSubfolder(importProjects[3], "Project as Subfolder");
        importFolder("Project as Subfolder", projectSubfolderPermsZip);
        // groups should not be created when importing a subfolder
        for (String group : importedGroups)
        {
            _permissionsHelper.assertGroupDoesNotExist(group, importProjects[3]);
        }
        clickFolder("Project as Subfolder");
        verifyRoleAssignments(true, false, true);
    }

    @Test
    public void testImportSubfolderWithRolesToSubfolder()
    {
        createUsers(testUsers);
        // test folder import of folder export
        _containerHelper.createProject(importProjects[4], null);
        _containerHelper.createSubfolder(importProjects[4], "Subfolder as Subfolder");
        importFolder("Subfolder as Subfolder", subfolderPermsZip);
        clickFolder("Subfolder as Subfolder");
        _permissionsHelper.assertPermissionSetting(testUser1, "Reader");
    }

    @Test
    public void testImportSubfolderWithInheritedRoles()
    {
        _containerHelper.createProject(importProjects[5], null);
        _containerHelper.createSubfolder(importProjects[5], "Inherited Imported Subfolder");
        importFolder("Inherited Imported Subfolder", inheritedPermsZip);
        clickFolder("Inherited Imported Subfolder");
        _permissionsHelper.assertPermissionsInherited();
    }

    private void importFolder(String folderName, String zipFileName)
    {
        clickFolder(folderName);
        importFolderFromZip(new File(dataDir, zipFileName));
        beginAt(getCurrentRelativeURL()); //work around linux issue
        waitForPipelineJobsToComplete(1, "Folder import", false);
    }

    private void verifyRoleAssignments(boolean usersExist, boolean groupsExist, boolean isSubfolder)
    {
        if (groupsExist)
        {
            log("Verifying role assignments to groups");
            _permissionsHelper.assertPermissionSetting(groupGroup, "Reader");
            _permissionsHelper.assertPermissionSetting(superTesterGroup, "Author");
            _permissionsHelper.assertPermissionSetting(groupGroup, "Author");
            _permissionsHelper.assertPermissionSetting(submitterGroup, "Submitter");
            _permissionsHelper.assertPermissionSetting(superTesterGroup, "Editor");
            _permissionsHelper.assertPermissionSetting(parentGroup, "Editor");
        }
        if (usersExist)
        {
            log("Verifying role assignments to users");
            _permissionsHelper.assertPermissionSetting(testUser3, "Author");
            if (!isSubfolder)
                _permissionsHelper.assertPermissionSetting(testUser3, "Project Administrator");
            _permissionsHelper.assertPermissionSetting(testUser2, "Editor");
            _permissionsHelper.assertPermissionSetting(testUser1, "Folder Administrator");
            _permissionsHelper.assertPermissionSetting(testUser3, "Folder Administrator");
        }
    }

    private void verifyProjectGroups(String projectName, @Nullable String[] groupsNotExpected, @Nullable String[] expectedGroups, boolean usersExist)
    {
        if (expectedGroups != null)
        {
            log("Verifying existence of groups");
            for (String group : expectedGroups)
            {
                _permissionsHelper.assertGroupExists(group, projectName);
            }
        }
        if (groupsNotExpected != null)
        {
            log("Verifying absence of groups");
            for (String group : groupsNotExpected)
            {
                _permissionsHelper.assertGroupDoesNotExist(group, projectName);
            }
        }
        if (usersExist)
        {
            log("Verifying existence of users in groups");
            _permissionsHelper.assertUserInGroup(testUser3, parentGroup, projectName, PrincipalType.USER);
            _permissionsHelper.assertUserInGroup(testUser1, submitterGroup, projectName, PrincipalType.USER);
            _permissionsHelper.assertUserInGroup(testUser2, superTesterGroup, projectName, PrincipalType.USER);
        }
        else
        {
            log("Verifying absence of users in groups");
            assertNull("User should not have been created by folder import: " + testUser1, _userHelper.getUserId(testUser1));
            assertNull("User should not have been created by folder import: " + testUser2, _userHelper.getUserId(testUser2));
            assertNull("User should not have been created by folder import: " + testUser3, _userHelper.getUserId(testUser3));
        }
        log("Verifying existence of groups in groups");
        _permissionsHelper.assertUserInGroup(submitterGroup, groupGroup, projectName, PrincipalType.GROUP);
        _permissionsHelper.assertUserInGroup(superTesterGroup, parentGroup, projectName, PrincipalType.GROUP);
        _permissionsHelper.assertUserInGroup(submitterGroup, superTesterGroup, projectName, PrincipalType.GROUP);
    }

    @LogMethod
    private void verifyCreateFolderFromTemplate()
    {
        _containerHelper.createSubFolderFromTemplate(getProjectName(), folderFromTemplate, "/" + getProjectName() + "/" + folderFromZip, new String[]{"Grid Views"});
        verifyExpectedWebPartsPresent();
        verifySubfolderImport(folderFromTemplate, true);
        verifyFolderExportAsExpected(folderFromTemplate);
    }

    @Test
    public void testImportFromPipelineZip()
    {
        verifyImportFromPipeline(folderZip, folderFromPipelineZip);
    }

    @Test
    public void testImportFromPipelineExpanded()
    {
        verifyImportFromPipeline(folderArchive + "/folder.xml", folderFromPipelineExport);
    }

    @LogMethod
    private void verifyImportFromPipeline(String fileImport, String folderName)
    {
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), folderName, "Collaboration", null);
        setPipelineRoot(dataDir.getAbsolutePath());
        importFolderFromPipeline(fileImport);

        setPipelineRootToDefault(); // Export to default location
        clickFolder(folderName);
        verifyFolderImportAsExpected(folderName);
        verifyFolderExportAsExpected(folderName);
    }

    private void createUsers(String...users)
    {
        log("Creating " + users.length + " users");
        _userHelper.ensureUsersExist(Arrays.asList(users));
    }

    @LogMethod
    private void createProjectGroups()
    {
        _permissionsHelper.createPermissionsGroup(submitterGroup, testUser1);
        _permissionsHelper.createPermissionsGroup(superTesterGroup, submitterGroup, testUser2);
        _permissionsHelper.createPermissionsGroup(parentGroup, superTesterGroup, testUser3);
        _permissionsHelper.createPermissionsGroup(emptyGroup);
        _permissionsHelper.createPermissionsGroup(groupGroup, submitterGroup, emptyGroup);
    }

    @LogMethod
    private void setPermissions()
    {
        _permissionsHelper.setUserPermissions(testUser1, "Folder Administrator");
        _permissionsHelper.setUserPermissions(testUser2, "Editor");
        _permissionsHelper.setUserPermissions(testUser3, "Project Administrator");
        _permissionsHelper.setUserPermissions(testUser4, "Author");
        _permissionsHelper.setUserPermissions(testUser3, "Author");

        _permissionsHelper.setPermissions(superTesterGroup, "Author");
        _permissionsHelper.setPermissions(superTesterGroup, "Editor");
        _permissionsHelper.setPermissions(submitterGroup, "Submitter");
        _permissionsHelper.setPermissions(parentGroup, "Editor");
        _permissionsHelper.setPermissions(groupGroup, "Reader");
        _permissionsHelper.setPermissions(groupGroup, "Author");
    }

    @LogMethod
    private void createUsersAndGroupsWithPermissions()
    {
        createUsers(testUsers);
        goToProjectHome();
        createProjectGroups();
        setPermissions();

        _containerHelper.createSubfolder(getProjectName(), folderWithPermissions);
//         stop inheriting permissions from the parent project and set specific permissions in the subfolder

        _permissionsHelper.setUserPermissions(testUser1, "Reader");
        _containerHelper.createSubfolder(getProjectName() + "/" + folderWithPermissions, "Subfolder5", "Collaboration");
        _containerHelper.createSubfolder(getProjectName(), folderInheritingPermissions);
        _permissionsHelper.checkInheritedPermissions();
    }

    @Test
    public void verifyProjectExportWithGroups()
    {
        goToProjectHome();
        verifyFolderExportWithPermissionsAsExpected(null, getExpectedXML("groupsFolder.xml"), true, true, false);
    }

    @Test
    public void verifyProjectExportWithRoleAssignments()
    {
        goToProjectHome();
        verifyFolderExportWithPermissionsAsExpected(null, getExpectedXML("rolesFolder.xml"), true, false, true);
    }

    @Test
    public void verifyProjectExportWithGroupsAndRoleAssignments()
    {
        goToProjectHome();
        verifyFolderExportWithPermissionsAsExpected(null, getExpectedXML("rolesAndGroupsFolder.xml"), true, true, true);
    }

    @Test
    public void verifySubfolderExportWithRoleAssignments()
    {
        goToProjectHome();
        verifyFolderExportWithPermissionsAsExpected(folderWithPermissions, getExpectedXML("subfolderRoleAssignmentsExport.xml"), false, false, true);
    }

    @Test
    public void verifySubfolderExportWithInheritance()
    {
        goToProjectHome();
        verifyFolderExportWithPermissionsAsExpected(folderInheritingPermissions, getExpectedXML("subfolderInheritedAssignments.xml"), false, false, true);
    }

    private String getExpectedXML(String fileName)
    {
        File file = new File(dataDir, fileName);
        String xml = TestFileUtils.getFileContents(file);
        xml = xml.replaceAll("USER_NAME", PasswordUtil.getUsername());
        return xml;
    }

    @LogMethod
    private void verifyImportFromZip()
    {
        _containerHelper.createSubfolder(getProjectName(), folderFromZip);
        // create one of the subfolders, to be imported, to test merge on import of subfolders
        _containerHelper.createSubfolder(getProjectName() + "/" + folderFromZip, "Subfolder1", "Collaboration");

        clickFolder(folderFromZip);
        importFolderFromZip(new File(dataDir, folderZip));
        waitForPipelineJobsToComplete(1, "Folder import", false);
        clickFolder(folderFromZip);
        verifyFolderImportAsExpected(folderFromZip);
        verifyFolderExportAsExpected(folderFromZip);
    }

    @LogMethod
    private void verifyFolderExportAsExpected(String folderName)
    {
        File exportDir = new File(TestFileUtils.getDefaultFileRoot(getProjectName() + "/" + folderName), "export");

        exportDir.delete();

        exportFolderAsIndividualFiles(folderName, false, false, true);

        String[] expectedExportItems = {
                "folder.xml",
                "subfolders/subfolders.xml",
                "subfolders/Subfolder1/folder.xml",
                "subfolders/Subfolder1/subfolders/_hidden/folder.xml",
                "subfolders/Subfolder2/folder.xml"};

        WebDriverWrapper.waitFor(exportDir::exists, "Folder export not present: " + exportDir.getAbsolutePath(), WAIT_FOR_PAGE);
        List<String> missingFiles = new ArrayList<>();
        for (String expectedItem : expectedExportItems)
        {
            File itemFile = new File(exportDir, expectedItem);
            if (!itemFile.exists())
                missingFiles.add(expectedItem);
        }

        Assert.assertTrue("File(s) not found in export of " + folderName + " to " + exportDir.getAbsolutePath() + ":\n[" + StringUtils.join(missingFiles, ", ") +"]", missingFiles.isEmpty());
    }

    @LogMethod
    private void verifyFolderExportWithPermissionsAsExpected(@Nullable String folderName, @Nullable String expectedXml, boolean includeSubfolders, boolean exportGroups, boolean exportAssignments)
    {
        exportFolderAsIndividualFiles(folderName, exportGroups, exportAssignments, includeSubfolders);

        // verify some of the folder export items by selecting them in the file browser
        _fileBrowserHelper.selectFileBrowserItem("export/folder.xml");
        File fileRoot;
        if (folderName != null)
            fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName() + File.separator + folderName);
        else
            fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        File folderXmlFile = new File(fileRoot, "export" + File.separator + "folder.xml");
        FolderDocument exportedFolderDocument;

        XmlOptions defaultParseOptions = new XmlOptions().setLoadLineNumbers();
        try
        {
            exportedFolderDocument = FolderDocument.Factory.parse(folderXmlFile, defaultParseOptions);
        }
        catch (XmlException | IOException e)
        {
            throw new RuntimeException("Problem reading exported folder XML file", e);
        }

        FolderDocument expectedFolderDocument;
        try
        {
            expectedFolderDocument = FolderDocument.Factory.parse(expectedXml, defaultParseOptions);
        }
        catch (XmlException e)
        {
            throw new RuntimeException("Problem parsing string for expected groups string", e);
        }

        if (!exportGroups)
        {
            Assert.assertNull("Exported groups present when not expected", exportedFolderDocument.getFolder().getGroups());
        }
        else
        {
            GroupsType exportedGroups = exportedFolderDocument.getFolder().getGroups();
            Assert.assertNotNull(exportedGroups);
            Assert.assertTrue("Exported role assignments not as expected", expectedFolderDocument.getFolder().getGroups().valueEquals(exportedGroups));
        }

        if (!exportAssignments)
        {
            Assert.assertNull("Exported role assignments present when not expected", exportedFolderDocument.getFolder().getRoleAssignments());
        }
        else
        {
            RoleAssignmentsType exportedAssignments = exportedFolderDocument.getFolder().getRoleAssignments();
            Assert.assertNotNull(exportedAssignments);

            Assert.assertTrue("Exported role assignments not as expected", expectedFolderDocument.getFolder().getRoleAssignments().valueEquals(exportedAssignments));
        }
    }

    private void verifyExpectedWebPartsPresent()
    {
        Locator titleLoc = Locator.css(".labkey-wp-title-text");
        List<WebElement> titlesElements = titleLoc.findElements(getDriver());
        Iterator<WebElement> it = titlesElements.iterator();
        WebElement curEl = it.next();
        for (String expectedTitle : webParts)
        {
            while (!curEl.getText().equals(expectedTitle))
            {
                if (it.hasNext())
                    curEl = it.next();
                else
                {
                    assertElementPresent(titleLoc.withText(expectedTitle));
                    fail("Webpart found out of order: " + expectedTitle);
                }
            }
        }
    }

    @LogMethod
    private void verifyFolderImportAsExpected(String importedFolderName)
    {
        verifyExpectedWebPartsPresent();
        assertElementPresent(Locator.css(".study-properties").withText("Demo Study tracks data in 12 datasets over 26 time points. Data is present for 6 Participants."));
        assertElementPresent(Locator.css(".labkey-wiki").withText("Test wikiTest wikiTest wiki"));

        log("Verify import of list");
        String listName = "safe list";
        assertTextPresent(listName);
        clickAndWait(Locator.linkWithText(listName));
        assertTextPresent("persimmon");
        assertElementPresent(Locator.tagWithClass("td", "labkey-mv-indicator"), 1);
        assertTextNotPresent("grapefruit");//this has been filtered out.  if "grapefruit" is present, the filter wasn't preserved
        goBack();

        log("verify import of query web part");
        assertTextPresent("~!@#$%^&*()_+query web part", "Contains one row per announcement or reply");

        log("verify report present");
        assertTextPresent("pomegranate");

        log("verify search settings as expected");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Search"));
        assertFalse("Folder search settings not imported", isChecked(Locator.checkboxById("searchable")));

        log("verify folder type was overwritten on import");
        clickAndWait(Locator.linkContainingText("Folder Type"));
        assertTrue("Folder type not overwritten on import", isChecked(Locator.radioButtonByNameAndValue("folderType", "None")));

        log("verify notification default settings as expected");
        clickAndWait(Locator.linkWithText("Notifications"));
        waitForText("Default settings");

        String messagesDefault = new ComboBox.ComboBoxFinder(getDriver()).withLabel(MessagesLongTest.MESSAGES_DEFAULT_COMBO).find(getDriver()).getValue();
        assertEquals("Wrong default value for message notification emails.", "All conversations", messagesDefault);

        String fileDefault = new ComboBox.ComboBoxFinder(getDriver()).withLabel(MessagesLongTest.FILES_DEFAULT_COMBO).find(getDriver()).getValue();
        assertEquals("Wrong default value for file notification emails.", "Daily digest", fileDefault);

        verifySubfolderImport(importedFolderName, false);
    }

    @LogMethod
    private void verifySubfolderImport(String importedFolderName, boolean fromTemplate)
    {
        log("verify child containers were imported");

        beginAtSubfolder(importedFolderName, "Subfolder1/_hidden");
        assertTextPresentInThisOrder("Lists", "Hidden Folder List");

        beginAtSubfolder(importedFolderName, "Subfolder2");
        int expectedPtidCount = fromTemplate ? 0 : 2;
        assertElementPresent(Locator.css(".study-properties").withText("Study Label for Subfolder2 tracks data in 1 dataset over 1 visit. Data is present for " + expectedPtidCount + " Monkeys."));

        log("verify container tabs were imported");
        beginAtSubfolder(importedFolderName, "Subfolder1");
        assertTextPresent("My Test Container Tab Query");
        assertElementPresent(Locator.linkWithText("Assay Container"));
        assertElementPresent(Locator.linkWithText("Tab 2"));
        assertElementPresent(Locator.linkWithText("Study Container"));
        assertElementNotPresent(Locator.linkWithText("Tab 1"));
        clickAndWait(Locator.linkWithText("Tab 2"));
        assertTextPresentInThisOrder("A customized web part", "Experiment Runs", "Assay List");
        clickAndWait(Locator.linkWithText("Study Container"));
        assertElementPresent(Locator.css(".study-properties").withText("Study Container Tab Study tracks data in 0 datasets over 0 visits. Data is present for 0 Participants."));
    }

    private void beginAtSubfolder(String importedFolderName, String subfolderName)
    {
        beginAt(WebTestHelper.buildURL("project", getProjectName() + "/" + importedFolderName + "/" + subfolderName, "start"));
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), false);
        for (String importProject : importProjects)
        {
            _containerHelper.deleteProject(importProject, false);
        }
        _userHelper.deleteUsers(false, testUsers);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        // we are using the simpletest module to test Container Tab import/export
        return Arrays.asList("core", "simpletest");
    }

    @Override
    public void validateQueries(boolean validateSubfolders)
    {
        super.validateQueries(false); // too may subfolders
    }
}
