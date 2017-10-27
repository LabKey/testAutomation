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
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Issues;
import org.labkey.test.components.IssueListDefDataRegion;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.issues.AdminPage;
import org.labkey.test.pages.issues.InsertPage;
import org.labkey.test.pages.issues.ListPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({Issues.class, DailyA.class})
public class IssuesAdminTest extends BaseWebDriverTest
{
    private static final String USER = "admin_user@issuesadmin.test";
    private static final String DEFAULT_NAME = "issues";
    private static final String LIST_NAME = "otherIssues";
    private static final String PROJECT2 = "IssuesAdminWithoutModule";
    private static final String PROJECT3 = "CustomIssueName Project";

    private IssuesHelper _issuesHelper = new IssuesHelper(this);
    private ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(PROJECT2, afterTest);
        _containerHelper.deleteProject(PROJECT3, afterTest);
        _userHelper.deleteUsers(afterTest, USER);
    }

    @BeforeClass
    public static void setupProject()
    {
        IssuesAdminTest init = (IssuesAdminTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(USER);
        _containerHelper.createProject(getProjectName(), null);
        _issuesHelper.createNewIssuesList(LIST_NAME, _containerHelper);
        AdminPage adminPage = AdminPage.beginAt(this, getProjectName(), LIST_NAME);
        adminPage.setIssueAssignmentList(null);
        adminPage.save();
    }

    @Test
    public void testEmptyAssignedToList() throws Exception
    {
        goToProjectHome();
        final String group = "AssignedToGroup";
        _permissionsHelper.setUserPermissions(USER, "FolderAdmin");
        _permissionsHelper.createProjectGroup(group, getProjectName());
        goToModule("Issues");
        OptionSelect assignedTo = new ListPage(getDriver())
                .clickNewIssue()
                .assignedTo();
        assertEquals("", assignedTo.get());
        assertEquals(Collections.singletonList(""), getTexts(assignedTo.getOptions()));
        _permissionsHelper.addUserToProjGroup(USER, getProjectName(), group);
        refresh();
        assignedTo = new InsertPage(getDriver()).assignedTo();
        assertEquals("", assignedTo.get());
        assertEquals(Arrays.asList("", _userHelper.getDisplayNameForEmail(USER)), getTexts(assignedTo.getOptions()));
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
        _issuesHelper.goToAdmin();

        setFormElement(Locator.name("entrySingularName"), singular);
        setFormElement(Locator.name("entryPluralName"), plural);
        clickButton("Save");

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

    // @Test @Ignore //TODO
    public void testProtectedFields() throws Exception
    {
    }

    // @Test @Ignore //TODO
    public void testRelatedIssuesComments() throws Exception
    {
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
