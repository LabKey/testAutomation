/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.tests.issues;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Issues;
import org.labkey.test.components.IssueListDefDataRegion;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.issues.AdminPage;
import org.labkey.test.pages.issues.DetailsPage;
import org.labkey.test.pages.issues.InsertIssueDefPage;
import org.labkey.test.pages.issues.InsertPage;
import org.labkey.test.pages.issues.ListPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.PermissionsHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({Issues.class, DailyA.class})
public class IssueDomainSharingTest extends BaseWebDriverTest
{
    private static final String USER = "project_user@issuessharing.test";
    private static final String USER2 = "shared_user@issuessharing.test";
    private static final String PROJECT2 = "IssuesDomain OtherProject";
    private static final String FOLDER = "IssuedDefSubfolder";
    private static final String SHARED_LIST_DEF = "SharedTestDef";
    private final String FOLDER_PATH = "/" + getProjectName() + "/" + FOLDER;

    private IssuesHelper _issuesHelper = new IssuesHelper(this);
    private ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), false);
        _containerHelper.deleteProject(PROJECT2, false);
        if (_issuesHelper.doesIssueListDefExist("Shared", SHARED_LIST_DEF))
        {
            IssueListDefDataRegion shared = _issuesHelper.goToIssueListDefinitions("Shared");
            shared.deleteListDefs(SHARED_LIST_DEF);
        }
        _userHelper.deleteUsers(false, USER);
    }

    @BeforeClass
    public static void setupProject()
    {
        IssueDomainSharingTest init = (IssueDomainSharingTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(USER);
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("Issues");
        _containerHelper.createSubfolder(getProjectName(), FOLDER);
        _containerHelper.enableModule("Issues");
    }

    @Test
    public void testInheritingDomain() throws Exception
    {
        final String listDef = "InheritedTestDef";

        _issuesHelper.goToIssueListDefinitions(getProjectName())
                .createIssuesListDefinition(listDef);

        Window confirmationWindow = _issuesHelper.goToIssueListDefinitions(FOLDER_PATH)
                .startCreateIssuesListDefinition(listDef);
        assertEquals(String.format("An existing Issue Definition was found in this folder: /%s. " +
                "This existing definition will be shared with your new issue list if created.", getProjectName()),
                confirmationWindow.getBody());
        confirmationWindow.clickButton("Yes");

        AdminPage adminPage = AdminPage.beginAt(this, getProjectName(), listDef);
        String inheritedField = "inheritedfield";
        adminPage.configureFields().addField(new FieldDefinition(inheritedField).setLabel(inheritedField).setType(ColumnType.String));
        adminPage.saveAndClose();

        ListPage issueList = ListPage.beginAt(this, FOLDER_PATH, listDef);
        InsertPage insertPage = issueList.clickNewIssue();
        assertElementPresent(Locator.name(inheritedField));
        assertEquals("Insert button went to wrong container.",
                FOLDER_PATH,
                getCurrentContainerPath());
    }

    @Test
    public void testSharingDomain() throws Exception
    {
        final String projectGroup = "ProjectGroup";
        _permissionsHelper.createProjectGroup(projectGroup, getProjectName());
        _permissionsHelper.addUserToProjGroup(USER, getProjectName(), projectGroup);
        _permissionsHelper.addMemberToRole(projectGroup, "Editor", PermissionsHelper.MemberType.group, getProjectName());

        final String title = "Child Issue";
        final String assignTo = _userHelper.getDisplayNameForEmail(USER);
        final String customValue = "Value for shared domain";
        final String listDef = SHARED_LIST_DEF;
        final String inheritedField = "inheritedfield";

        goToProjectHome("Shared");
        _containerHelper.enableModule("Issues");
        _issuesHelper.goToIssueListDefinitions("Shared")
                .createIssuesListDefinition(listDef);

        Window confirmationWindow = _issuesHelper.goToIssueListDefinitions(getProjectName())
                .startCreateIssuesListDefinition(listDef);
        assertEquals("An existing Issue Definition was found in this folder: /Shared. " +
                "This existing definition will be shared with your new issue list if created.",
                confirmationWindow.getBody());
        confirmationWindow.clickButton("Yes");
        AdminPage adminPage = AdminPage.beginAt(this, getProjectName(), listDef);
        adminPage.setIssueAssignmentList(null);
        adminPage.save();

        adminPage = AdminPage.beginAt(this, "Shared", listDef);
        adminPage.configureFields().addField(new FieldDefinition(inheritedField).setLabel(inheritedField).setType(ColumnType.String));
        adminPage.saveAndClose();

        ListPage issueList = ListPage.beginAt(this, getProjectName(), listDef);
        InsertPage insertPage = issueList.clickNewIssue();
        assertElementPresent(Locator.name(inheritedField));
        assertEquals("Insert button went to wrong container.",
                "/" + getProjectName(),
                getCurrentContainerPath());

        insertPage.title().set(title);
        insertPage.assignedTo().set(assignTo);
        insertPage.fieldWithName(inheritedField).set(customValue);
        String issueId = insertPage.save().getIssueId();

        log("Verify that issues remain after inherited issue list definition is deleted");
        _issuesHelper.goToIssueListDefinitions("Shared").deleteListDefs(listDef);

        DetailsPage detailsPage = DetailsPage.beginAt(this, issueId);
        assertEquals("Unable to find inherited field after deleting inherited issue list def.", customValue, detailsPage.getCustomField(inheritedField).get());
    }

    @Test
    public void testCantShareDomainBetweenProjects() throws Exception
    {
        final String listDef = "UnSharedTestDef";

        _containerHelper.createProject(PROJECT2, null);
        _containerHelper.enableModule("Issues");
        _issuesHelper.goToIssueListDefinitions(PROJECT2)
                .createIssuesListDefinition(listDef);

        Window confirmationWindow = _issuesHelper.goToIssueListDefinitions(getProjectName())
                .startCreateIssuesListDefinition(listDef);
        assertEquals("Wrong issue definition confirmation",
                "A new Issue Definition will be generated in this folder: /" + getProjectName(),
                confirmationWindow.getBody());
        confirmationWindow.clickButton("Yes");

        AdminPage adminPage = AdminPage.beginAt(this, PROJECT2, listDef);
        String inheritedField = "uninheritedfield";
        adminPage.configureFields().addField(new FieldDefinition(inheritedField).setLabel(inheritedField).setType(ColumnType.String));
        adminPage.saveAndClose();

        ListPage issueList = ListPage.beginAt(this, getProjectName(), listDef);
        issueList.clickNewIssue();
        assertElementNotPresent(Locator.name(inheritedField));
        assertEquals("Insert button went to wrong container.",
                "/" + getProjectName(),
                getCurrentContainerPath());
    }

    @Test
    public void testOverlappingDomainInParent() throws Exception
    {
        final String listDef = "OverlappingTestDef";

        _issuesHelper.goToIssueListDefinitions(FOLDER_PATH)
                .createIssuesListDefinition(listDef);

        InsertIssueDefPage.CreateListDefConfirmation confirmationWindow = _issuesHelper.goToIssueListDefinitions(getProjectName())
                .startCreateIssuesListDefinition(listDef);
        assertEquals("Wrong issue definition confirmation",
                "A new Issue Definition will be generated in this folder: /" + getProjectName(),
                confirmationWindow.getBody());

        AdminPage adminPage = confirmationWindow.clickYes();
        String inheritedField = "uninheritedfield";
        adminPage.configureFields().addField(new FieldDefinition(inheritedField).setLabel(inheritedField).setType(ColumnType.String));
        adminPage.saveAndClose();

        ListPage issueList = ListPage.beginAt(this, FOLDER_PATH, listDef);
        issueList.clickNewIssue();
        assertElementNotPresent(Locator.name(inheritedField));
        assertEquals("Insert button went to wrong container.",
                FOLDER_PATH,
                getCurrentContainerPath());
    }

    //@Test @Ignore //TODO
    public void testDeletingInheritedListDef() throws Exception
    {
    }

    //@Test @Ignore //TODO
    public void testProtectedFields() throws Exception
    {
    }

    @Override
    protected WebDriverWrapper.BrowserType bestBrowser()
    {
        return WebDriverWrapper.BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "IssuesDomainSharingTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("issues");
    }
}
