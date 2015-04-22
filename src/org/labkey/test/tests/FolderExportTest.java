/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.security.PrincipalType;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.UIContainerHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyB.class})
public class FolderExportTest extends BaseWebDriverTest
{

    String[] webParts = {"Study Overview", "Data Pipeline", "Datasets", "Specimens", "Views", "Test wiki", "Study Data Tools", "Lists", "~!@#$%^&*()_+query web part", "Report web part", "Workbooks"};
    File dataDir = new File(TestFileUtils.getSampledataPath(), "FolderExport");
    private final String folderFromZip = "1 Folder From Zip"; // add numbers to folder names to keep ordering for created folders
    private final String folderFromPipelineZip = "2 Folder From Pipeline Zip";
    private final String folderFromPipelineExport = "3 Folder From Pipeline Export";
    private static final String folderFromTemplate = "4 Folder From Template";
    private static final String folderWithPermissions = "5 Folder From Zip With Permissions";
    private static final String folderInheritingPermissions = "6 Inheriting";
    private static final String folderZip = "SampleWithSubfolders.folder.zip";
    private static final String projectPermsZip = "ProjectWithPerms.folder.zip";
    private static final String projectSubfolderPermsZip = "ProjectWithSubfoldersAndPerms.folder.zip";
    private static final String subfolderPermsZip = "SubfolderWithPerms.folder.zip";
    private static final String inheritedPermsZip = "InheritingSubfolder.folder.zip";

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
    private static final String[] importedGroups = new String[]{submitterGroup, superTesterGroup, parentGroup, groupGroup};
    private static final String[] notImportedGroups = new String[]{emptyGroup};

    private static final String importProject = "FolderImportTest";


    public FolderExportTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

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
    protected Set<String> excludeFromViewCheck()
    {
        Set<String> folders = new HashSet<>();
        folders.add(folderFromTemplate);
        return folders;
    }

    @Test
    public void testSteps()
    {
        // we are using the simpletest module to test Container Tab import/export
        goToAdminConsole();
        assertTextPresent("simpletest");

        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();
        _containerHelper.createProject(getProjectName(), null);

        verifyImportFromZip();
        verifyImportFromPipelineZip();
        //Issue 13881
        verifyImportFromPipelineExpanded();
        verifyCreateFolderFromTemplate();

        createUsersAndGroupsWithPermissions();
        verifyProjectExportWithGroups();
        verifyProjectExportWithRoleAssignments();
        verifyProjectExportWithGroupsAndRoleAssignments();
        verifySubfolderExportWithRoleAssignments();
        verifySubfolderExportWithInheritance();

    }

    @Test
    public void testImportProjectWithoutUsers()
    {
//        test project import with users that do not exist
        _containerHelper.deleteProject(importProject, false);
        deleteUsersIfPresent(testUsers);
        _containerHelper.createProject(importProject, null);
        importFolder(importProject, projectPermsZip);
        verifyProjectGroups(importProject, notImportedGroups, importedGroups, false);
        verifyRoleAssignments(false, true, false);
    }

    @Test
    public void testImportProjectWithUsersNoGroups()
    {
        // test project import with groups that do not exist
        _containerHelper.deleteProject(importProject, false);
        _containerHelper.createProject(importProject, null);
        createUsers(testUsers);
        importFolder(importProject, projectPermsZip);
        verifyProjectGroups(importProject, notImportedGroups, importedGroups, true);
        verifyRoleAssignments(true, true, false);
    }

    @Test
    public void testImportProjectWithExistingUsersAndGroups()
    {
        _containerHelper.deleteProject(importProject, false);
        // test project import with all users and some groups existing
        createUsers(testUsers);
        _containerHelper.createProject(importProject, null);
        createProjectGroups(true, importProject);
        importFolder(importProject, projectPermsZip);
        verifyProjectGroups(importProject, notImportedGroups, importedGroups, true);
        verifyRoleAssignments(true, true, false);
        // existing group should be overwritten by imported group
        _permissionsHelper.assertUserNotInGroup(testUser4, submitterGroup, importProject, PrincipalType.USER);
    }

    @Test
    public void testImportProjectToSubfolder()
    {
        _containerHelper.deleteProject(importProject, false);
        // test folder import of project export
        createUsers(testUsers);
        _containerHelper.deleteProject(importProject, false);
        _containerHelper.createProject(importProject, null);
        _containerHelper.createSubfolder(importProject, "Project as Subfolder");
        importFolder("Project as Subfolder", projectSubfolderPermsZip);
        // groups should not be created when importing a subfolder
        for (String group : importedGroups)
        {
            _permissionsHelper.assertGroupDoesNotExist(group, importProject);
        }
        clickFolder("Project as Subfolder");
        verifyRoleAssignments(true, false, true);
    }

    @Test
    public void testImportSubfolderWithRolesToSubfolder()
    {
        _containerHelper.deleteProject(importProject, false);
        createUsers(testUsers);
        // test folder import of folder export
        _containerHelper.createProject(importProject, null);
        _containerHelper.createSubfolder(importProject, "Subfolder as Subfolder");
        importFolder("Subfolder as Subfolder", subfolderPermsZip);
        clickFolder("Subfolder as Subfolder");
        _permissionsHelper.assertPermissionSetting(testUser1, "Reader");
    }

    @Test
    public void testImportSubfolderWithInheritedRoles()
    {
        _containerHelper.deleteProject(importProject, false);
        _containerHelper.createProject(importProject, null);
        _containerHelper.createSubfolder(importProject, "Inherited Imported Subfolder");
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
            _permissionsHelper.assertUserInGroup(testUser3, parentGroup, importProject, PrincipalType.USER);
            _permissionsHelper.assertUserInGroup(testUser1, submitterGroup, importProject, PrincipalType.USER);
            _permissionsHelper.assertUserInGroup(testUser2, superTesterGroup, importProject, PrincipalType.USER);
        }
        else
        {
            log("Verifying absence of users in groups");
            _permissionsHelper.assertUserNotInGroup(testUser3, parentGroup, importProject, PrincipalType.USER);
            _permissionsHelper.assertUserNotInGroup(testUser1, submitterGroup, importProject, PrincipalType.USER);
            _permissionsHelper.assertUserNotInGroup(testUser2, superTesterGroup, importProject, PrincipalType.USER);
        }
        log ("Verifying existence of groups in groups");
        _permissionsHelper.assertUserInGroup(submitterGroup, groupGroup, importProject, PrincipalType.GROUP);
        _permissionsHelper.assertUserInGroup(superTesterGroup, parentGroup, importProject, PrincipalType.GROUP);
        _permissionsHelper.assertUserInGroup(submitterGroup, superTesterGroup, importProject, PrincipalType.GROUP);
    }

    @LogMethod
    private void verifyCreateFolderFromTemplate()
    {
        _containerHelper.createSubFolderFromTemplate(getProjectName(), folderFromTemplate, "/" + getProjectName() + "/" + folderFromZip, new String[]{"Reports"});
        verifyExpectedWebPartsPresent();
        verifySubfolderImport(3, true);
        verifyFolderExportAsExpected(folderFromTemplate);
    }

    private void verifyImportFromPipelineZip()
    {
        verifyImportFromPipeline(folderZip, folderFromPipelineZip, 1);
    }

    private void verifyImportFromPipelineExpanded()
    {
        // test importing the folder archive that we exported from the verifyFolderExportAsExpected method
        verifyImportFromPipeline("export/folder.xml", folderFromPipelineExport, 2);
    }

    @LogMethod
    private void verifyImportFromPipeline(String fileImport, String folderName, int subfolderIndex)
    {
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), folderName, "Collaboration", null);
        setPipelineRoot(dataDir.getAbsolutePath());
        importFolderFromPipeline("" + fileImport);


        clickFolder(folderName);
        verifyFolderImportAsExpected(subfolderIndex);
        verifyFolderExportAsExpected(folderName);
    }

    private void createUsers(String...users)
    {
        log("Creating " + users.length + " users");
        for (String user : users)
        {
            createUser(user, null);
        }
    }

    @LogMethod
    private void createProjectGroups(boolean createSubset, String projectName)
    {
        if (createSubset)
        {
            log("Creating subset of project groups");
            if (!_permissionsHelper.doesGroupExist(submitterGroup, projectName))
                _permissionsHelper.createPermissionsGroup(submitterGroup, testUser4);
            if (!_permissionsHelper.doesGroupExist(parentGroup, projectName))
                _permissionsHelper.createPermissionsGroup(parentGroup, testUser3);
        }
        else
        {
            log("Creating all project groups");
            if (!_permissionsHelper.doesGroupExist(submitterGroup, projectName))
                _permissionsHelper.createPermissionsGroup(submitterGroup, testUser1);
            if (!_permissionsHelper.doesGroupExist(superTesterGroup, projectName))
                _permissionsHelper.createPermissionsGroup(superTesterGroup, submitterGroup, testUser2);
            if (!_permissionsHelper.doesGroupExist(parentGroup, projectName))
                _permissionsHelper.createPermissionsGroup(parentGroup, superTesterGroup, testUser3);
            if (!_permissionsHelper.doesGroupExist(emptyGroup, projectName))
                _permissionsHelper.createPermissionsGroup(emptyGroup);
            if (!_permissionsHelper.doesGroupExist(groupGroup, projectName))
                _permissionsHelper.createPermissionsGroup(groupGroup, submitterGroup, emptyGroup);

        }
        clickButton("Save and Finish");
    }

    @LogMethod
    private void setPermissions()
    {
        log("Creating test users and assigning permissions");

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
        clickButton("Save and Finish");
    }

    @LogMethod
    private void createUsersAndGroupsWithPermissions()
    {
        createUsers(testUsers);
        createProjectGroups(false, getProjectName());
        setPermissions();

        _containerHelper.createSubfolder(getProjectName(), folderWithPermissions);
//         stop inheriting permissions from the parent project and set specific permissions in the subfolder

        _permissionsHelper.uncheckInheritedPermissions(); // this doesn't seem to be necessary for some reason
        _permissionsHelper.setUserPermissions(testUser1, "Reader");
        clickButton("Save and Finish");
        _containerHelper.createSubfolder(getProjectName() + "/" + folderWithPermissions, "Subfolder5", "Collaboration");
        _containerHelper.createSubfolder(getProjectName(), folderInheritingPermissions);
        _permissionsHelper.checkInheritedPermissions();
        clickButton("Save and Finish");
    }

    @LogMethod
    private void verifyProjectExportWithGroups()
    {
        clickFolder(getProjectName());
        String groupXml = getExpectedXML("groupsExport.xml");
        verifyFolderExportWithPermissionsAsExpected(getProjectName(), false, groupXml, null, true);
    }

    @LogMethod
    private void verifyProjectExportWithRoleAssignments()
    {
        clickFolder(getProjectName());
        String assignmentsXml = getExpectedXML("roleAssignmentsExport.xml");
        verifyFolderExportWithPermissionsAsExpected(getProjectName(), false, null, assignmentsXml, true);
    }

    @LogMethod
    private void verifyProjectExportWithGroupsAndRoleAssignments()
    {
        clickFolder(getProjectName());
        String groupXml = getExpectedXML("groupsExport.xml");
        String assignmentsXml = getExpectedXML("roleAssignmentsExport.xml");
        verifyFolderExportWithPermissionsAsExpected(getProjectName(), false, groupXml, assignmentsXml, true);
    }

    @LogMethod
    private void verifySubfolderExportWithRoleAssignments()
    {
        clickFolder(folderWithPermissions);

        String assignmentsXml = getExpectedXML("subfolderRoleAssignmentsExport.xml");
        verifyFolderExportWithPermissionsAsExpected(folderWithPermissions, true, null, assignmentsXml, false);
    }

    private void  verifySubfolderExportWithInheritance()
    {
        clickFolder(folderInheritingPermissions);

        String assignmentsXml = "<roleAssignments inherited=\"true\"/>";
        verifyFolderExportWithPermissionsAsExpected(folderInheritingPermissions, true, null, assignmentsXml, false);
    }

    private String getExpectedXML(String fileName)
    {
        String xml = null;
        File file = new File(dataDir, fileName);
        try
        {
            xml = FileUtils.readFileToString(file);
            xml = xml.replaceAll("USER_NAME", PasswordUtil.getUsername());
        }
        catch (IOException e)
        {
            fail("Unable to read XML file in " + file);
        }
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
        beginAt(getCurrentRelativeURL()); //work around linux issue
        waitForPipelineJobsToComplete(1, "Folder import", false);
        clickFolder(folderFromZip);
        verifyFolderImportAsExpected(0);
        verifyFolderExportAsExpected(folderFromZip);
    }


    @LogMethod
    private void verifyFolderExportAsExpected(String folderName)
    {
        exportFolderAsIndividualFiles(folderName, false, false, true);

        // verify some of the folder export items by selecting them in the file browser
        _fileBrowserHelper.selectFileBrowserItem("export/folder.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/subfolders/subfolders.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/subfolders/Subfolder1/folder.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/subfolders/Subfolder1/subfolders/_hidden/folder.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/subfolders/Subfolder2/folder.xml");
    }


    @LogMethod
    private void verifyFolderExportWithPermissionsAsExpected(@NotNull String folderName, boolean isSubfolder, @Nullable String expectedGroups, @Nullable String expectedRoleAssignments, boolean includeSubfolders)
    {
        exportFolderAsIndividualFiles(folderName, expectedGroups != null, expectedRoleAssignments != null, includeSubfolders);

        // verify some of the folder export items by selecting them in the file browser
        _fileBrowserHelper.selectFileBrowserItem("export/folder.xml");
        File fileRoot;
        if (isSubfolder)
            fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName() + File.separator + folderName);
        else
            fileRoot = TestFileUtils.getDefaultFileRoot(folderName);
        File folderXmlFile = new File(fileRoot, "export" + File.separator + "folder.xml");
        String folderXml = null;
        try
        {
            folderXml = FileUtils.readFileToString(folderXmlFile);
        }
        catch (IOException e)
        {
            fail("Problem reading file: " + folderXmlFile);
        }

        if (expectedGroups == null)
        {
            Assert.assertFalse("Exported groups present when not expected", folderXml.contains("<groups>"));
        }
        else
        {
            Assert.assertTrue("Exported groups XML not as expected", folderXml.contains(expectedGroups));
        }

        if (expectedRoleAssignments == null)
        {
            Assert.assertFalse("Exported role assignments present when not expected", folderXml.contains("<roleAssignments>"));
        }
        else
        {
            Assert.assertTrue("Exported role assignments XML not as expected", folderXml.contains(expectedRoleAssignments));
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
    private void verifyFolderImportAsExpected(int subfolderIndex)
    {
        verifyExpectedWebPartsPresent();
        assertElementPresent(Locator.css(".study-properties").withText("Demo Study tracks data in 12 datasets over 26 time points. Data is present for 6 Participants."));
        assertElementPresent(Locator.css(".labkey-wiki").withText("Test wikiTest wikiTest wiki"));

        log("Verify import of list");
        String listName = "safe list";
        assertTextPresent(listName);
        clickAndWait(Locator.linkWithText(listName));
        assertTextPresent("persimmon");
        assertElementPresent(Locator.imageWithSrc("/labkey/_images/mv_indicator.gif", false));
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
        waitForText("Default Settings");

        _ext4Helper.openComboList(Ext4Helper.Locators.formItemWithLabel(MessagesLongTest.FILES_DEFAULT_COMBO));
        isElementPresent(Locator.xpath("//li[text()='Daily digest' and contains(@class, 'x4-boundlist-selected')]"));
        click(Locator.tagWithText("span", "Default Settings"));

        _ext4Helper.openComboList(Ext4Helper.Locators.formItemWithLabel(MessagesLongTest.MESSAGES_DEFAULT_COMBO));
        isElementPresent(Locator.xpath("//li[text()='All conversations' and contains(@class, 'x4-boundlist-selected')]"));
        click(Locator.tagWithText("span", "Default Settings"));

        verifySubfolderImport(subfolderIndex, false);
    }

    @LogMethod
    private void verifySubfolderImport(int subfolderIndex, boolean fromTemplate)
    {
        log("verify child containers were imported");
        hoverFolderBar();
        expandFolderTree("Subfolder1"); // Will expand to all subfolders with this name
        clickAndWait(Locator.linkWithText("Subfolder1", subfolderIndex));
        assertTextPresent("My Test Container Tab Query");
        hoverFolderBar();
        expandFolderTree("_hidden");
        clickAndWait(Locator.linkWithText("_hidden").index(subfolderIndex));
        assertTextPresentInThisOrder("Lists", "Hidden Folder List");
        hoverFolderBar();
        clickAndWait(Locator.linkWithText("Subfolder2", subfolderIndex));
        if (fromTemplate)
            assertElementPresent(Locator.css("#bodypanel .labkey-wp-body p").withText("This folder does not contain a study."));
        else
            assertElementPresent(Locator.css(".study-properties").withText("Study Label for Subfolder2 tracks data in 1 dataset over 1 visit. Data is present for 2 Monkeys."));

        log("verify container tabs were imported");
        hoverFolderBar();
        clickAndWait(Locator.linkWithText("Subfolder1", subfolderIndex));
        assertElementPresent(Locator.linkWithText("Assay Container"));
        assertElementPresent(Locator.linkWithText("Tab 2"));
        assertElementPresent(Locator.linkWithText("Study Container"));
        assertElementNotPresent(Locator.linkWithText("Tab 1"));
        clickAndWait(Locator.linkWithText("Tab 2"));
        assertTextPresentInThisOrder("A customized web part", "Experiment Runs", "Assay List");
        clickAndWait(Locator.linkWithText("Study Container"));
        if (fromTemplate)
            assertElementPresent(Locator.css("#bodypanel .labkey-wp-body p").withText("This folder does not contain a study."));
        else
            assertElementPresent(Locator.css(".study-properties").withText("Study Container Tab Study tracks data in 0 datasets over 0 visits. Data is present for 0 Participants."));
    }


    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName() + TRICKY_CHARACTERS_FOR_PROJECT_NAMES, false);
        _containerHelper.deleteProject(getProjectName(), false);
        _containerHelper.deleteProject(importProject, false);
        deleteUsersIfPresent(testUsers);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    @Override
    public void validateQueries(boolean validateSubfolders)
    {
        super.validateQueries(false); // too may subfolders
    }
}
