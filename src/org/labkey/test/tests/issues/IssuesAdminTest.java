/*
 * Copyright (c) 2016-2018 LabKey Corporation
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
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Issues;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.components.issues.IssueListDefDataRegion;
import org.labkey.test.pages.issues.DetailsPage;
import org.labkey.test.pages.issues.InsertPage;
import org.labkey.test.pages.issues.IssuesAdminPage;
import org.labkey.test.pages.issues.ListPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.util.PasswordUtil.getUsername;

@Category({Issues.class, Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class IssuesAdminTest extends BaseWebDriverTest
{
    private static final String ADMIN_USER = "admin_user@issuesadmin.test";
    private static final String TEST_USER = "testuser_issuetest@issues.test";
    private static final String TEST_USER_DISPLAY_NAME = "testuser issuetest";
    private static final String DEFAULT_NAME = "issues";
    private static final String TEST_GROUP = "testers";
    private static final String ISSUE_LIST_NAME = "otherIssues";
    private static final String PROJECT2 = "IssuesAdminWithoutModule";
    private static final String PROJECT3 = "CustomIssueName Project";

    private IssuesHelper _issuesHelper = new IssuesHelper(this);
    private ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    @BeforeClass
    public static void setupProject()
    {
        IssuesAdminTest init = (IssuesAdminTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(PROJECT2, afterTest);
        _containerHelper.deleteProject(PROJECT3, afterTest);
        _userHelper.deleteUsers(afterTest, ADMIN_USER, TEST_USER);
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _userHelper.createUser(ADMIN_USER);
        _userHelper.createUser(TEST_USER);
        _permissionsHelper.createPermissionsGroup(TEST_GROUP);
        _permissionsHelper.assertPermissionSetting(TEST_GROUP, "No Permissions");
        _permissionsHelper.setPermissions(TEST_GROUP, "Editor");

        _issuesHelper.createNewIssuesList(ISSUE_LIST_NAME, _containerHelper);
        IssuesAdminPage adminPage = IssuesAdminPage.beginAt(this, getProjectName(), ISSUE_LIST_NAME);
        adminPage.setAssignedTo(null); // All Project Users
        adminPage.clickSave();
    }

    @Test
    public void testEmptyAssignedToList() throws Exception
    {
        goToProjectHome();
        final String group = "AssignedToGroup";
        _permissionsHelper.setUserPermissions(ADMIN_USER, "FolderAdmin");
        _permissionsHelper.setUserPermissions(TEST_USER, "FolderAdmin");
        _permissionsHelper.createProjectGroup(group, getProjectName());
        goToModule("Issues");
        OptionSelect assignedTo = new ListPage(getDriver())
                .clickNewIssue()
                .assignedTo();
        assertEquals("", assignedTo.get());
        assertEquals(Collections.singletonList(""), getTexts(assignedTo.getOptions()));
        _permissionsHelper.addUserToProjGroup(ADMIN_USER, getProjectName(), group);
        refresh();
        assignedTo = new InsertPage(getDriver()).assignedTo();
        assertEquals("", assignedTo.get());
        assertEquals(Arrays.asList("", _userHelper.getDisplayNameForEmail(ADMIN_USER)), getTexts(assignedTo.getOptions()));
    }

    @Test
    public void testIssueDefinitionRequiresModule() throws Exception
    {
        _containerHelper.createProject(PROJECT2, null);
        _issuesHelper.goToIssueListDefinitions(PROJECT2)
                .clickInsert()
                .setLabel("noModule")
                .clickSubmitError()
                .clickClose();
        IssueListDefDataRegion listDefDataRegion = _issuesHelper.goToIssueListDefinitions(PROJECT2);
        assertEquals("Issue list definition present with module disabled", 0, listDefDataRegion.getDataRowCount());
    }

    @Test
    public void customIssueNameTest()
    {
        final String singular = "Ticket";
        final String plural = "Tickets";
        final String defaultSingular = "Issue";
        final String defaultPlural = "Issues";

        _containerHelper.createProject(PROJECT3, null);
        _containerHelper.enableModule("Issues");

        _issuesHelper.goToIssueListDefinitions(PROJECT3).createIssuesListDefinition("issues");

        goToModule("Issues");
        _issuesHelper.goToAdmin()
                .setSingularName(singular)
                .setPluralName(plural)
                .clickSave();

        log("Verify issues-list action respects custom noun");
        assertTextPresent(plural + " List", singular + " ID");
        assertTextNotPresent(defaultPlural + " List", defaultSingular + " ID");
        assertElementPresent(Locator.lkButton("New " + singular));

        clickAndWait(Locator.lkButton("Admin"));
        assertTextPresent(plural + " Admin Page");
        clickAndWait(Locator.linkWithText(plural + " List")); // Nav-trail link

        clickProject(PROJECT3);
        log("Verify issues webparts respect custom noun");
        PortalHelper portalHelper = new PortalHelper(_issuesHelper.getDriver());
        portalHelper.addWebPart("Issues Summary");
        clickAndWait(Locator.linkWithText("Submit"));
        assertElementPresent(Locator.lkButton("New " + singular.toLowerCase()));

        portalHelper.addWebPart("Issues List");
        clickAndWait(Locator.linkWithText("Submit"));
        assertEquals("Wrong title for ID column", singular + " ID", new DataRegionTable("issues-issues", getDriver()).getColumnLabels().get(0));
        assertElementPresent(Locator.lkButton("New " + singular));
        assertElementNotPresent(Locator.lkButton("New " + defaultSingular));
    }

    @Test
    public void testProtectedFields() throws Exception
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(ISSUE_LIST_NAME));
        IssuesAdminPage issuesAdminPage = _issuesHelper.goToAdmin();

        log("Checking for the protected field");
        DomainFormPanel domainFormPanel = issuesAdminPage.getFieldsPanel();
        checker().verifyTrue("Title should be protected", domainFormPanel.getField("Title").isFieldProtected());
        checker().verifyTrue("NotifyList should be protected", domainFormPanel.getField("NotifyList").isFieldProtected());
        checker().verifyTrue("AssignedTo should be protected", domainFormPanel.getField("AssignedTo").isFieldProtected());
        checker().verifyTrue("Resolution should be protected", domainFormPanel.getField("Resolution").isFieldProtected());
        checker().verifyFalse("Type should not be protected", domainFormPanel.getField("Type").isFieldProtected());
        checker().verifyFalse("Priority should not be protected", domainFormPanel.getField("Priority").isFieldProtected());
    }

    @Test
    public void testRelatedIssuesComments() throws Exception
    {
        log("Adding the test user to test group to appear in assigned to drop down");
        if (!_permissionsHelper.isUserInGroup(TEST_USER, TEST_GROUP,getProjectName(), PermissionsHelper.PrincipalType.USER))
            _permissionsHelper.addUserToProjGroup(TEST_USER,getProjectName(), TEST_GROUP);

        goToProjectHome();
        String mainTitle = "Main Issue Title";
        String relatedIssueTitle = "Related issue Title";
        clickAndWait(Locator.linkWithText(ISSUE_LIST_NAME));

        DetailsPage detailsPage = _issuesHelper.addIssue(
                Maps.of("title", mainTitle,
                        "assignedTo", TEST_USER_DISPLAY_NAME,
                        "comment", "Main issue Comment"));

        Map<String, String> relatedIssueData = Maps.of("title", relatedIssueTitle,
                "assignedTo", TEST_USER_DISPLAY_NAME,
                "comment", "Related issue Comment");
        InsertPage relatedIssuePage = detailsPage.clickCreateRelatedIssue(getProjectName(), ISSUE_LIST_NAME.toLowerCase());
        for (Map.Entry<String, String> field : relatedIssueData.entrySet())
            relatedIssuePage.fieldWithName(field.getKey()).set(field.getValue());
        relatedIssuePage.save();

        detailsPage = new DetailsPage(getDriver());
        checker().verifyEquals("Incorrect number of related issue rows", 1, detailsPage.getRelatedIssueTable().getDataRowCount());
        checker().verifyEquals("Incorrect title of related issue", mainTitle, detailsPage.getRelatedIssueTable().getDataAsText(0, "Title"));

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ISSUE_LIST_NAME));
        clickAndWait(Locator.linkWithText(mainTitle));
        detailsPage = new DetailsPage(getDriver());
        checker().verifyEquals("Incorrect number of related issue rows", 1, detailsPage.getRelatedIssueTable().getDataRowCount());
        checker().verifyEquals("Incorrect title of related issue", relatedIssueTitle, detailsPage.getRelatedIssueTable().getDataAsText(0, "Title"));

        log("Verifying show related comments");
        detailsPage.clickShowRelatedIssueComment();
        checker().verifyEquals("Incorrect comment display with show related comments", 2, detailsPage.getComments().size());

        log("Verifying hide related comments");
        detailsPage.clickHideRelatedIssueComment();
        checker().verifyEquals("Incorrect comment display with hide related comments", 2, detailsPage.getComments().size());
    }

    @Test
    public void testCommentSortDirection()
    {
        log("Adding the test user to test group to appear in assigned to drop down");
        if (!_permissionsHelper.isUserInGroup(TEST_USER, TEST_GROUP,getProjectName(), PermissionsHelper.PrincipalType.USER))
            _permissionsHelper.addUserToProjGroup(TEST_USER,getProjectName(), TEST_GROUP);


        log("Creating new Issue Definition list");
        String title = "Testing comment sort direction";

        goToProjectHome();
        String issueDefinitionName = "commentSortDirection";
        _issuesHelper.createNewIssuesList(issueDefinitionName, _containerHelper);
        IssuesAdminPage adminPage = IssuesAdminPage.beginAt(this, getProjectName(), issueDefinitionName);
        adminPage.setCommentSortDirection(IssuesAdminPage.SortDirection.OldestFirst)
                .setDefaultUser(getCurrentUserName())
                .clickSave();

        DetailsPage detailsPage = _issuesHelper.addIssue(
                Maps.of("title", title,
                        "comment", "First Comment"));

        log("Updating the issue with first comment");
        detailsPage = detailsPage.clickUpdate()
                .addComment("Second Comment")
                .save();

        log("Updating the issue with second comment");
        detailsPage = detailsPage.clickUpdate()
                .addComment("Third Comment")
                .save();

        log("Verifying the first comment");
        checker().verifyEquals("Incorrect comment order for Oldest first", "First Comment", detailsPage.getComments().get(0).getComment());

        log("Changing the comment direction in admin page");
        adminPage = IssuesAdminPage.beginAt(this, getProjectName(), issueDefinitionName);
        adminPage.setCommentSortDirection(IssuesAdminPage.SortDirection.NewestFirst)
                .clickSave();

        clickAndWait(Locator.linkWithText(title));
        detailsPage = new DetailsPage(getDriver());
        checker().verifyEquals("Incorrect comment order for Newest first", "Third Comment",
                detailsPage.getComments().get(0).getComment());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "IssuesAdminTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("issues");
    }
}
