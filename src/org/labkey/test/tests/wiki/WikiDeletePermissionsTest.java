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

package org.labkey.test.tests.wiki;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Wiki;
import org.labkey.test.pages.wiki.EditPage;
import org.labkey.test.util.TestUser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.labkey.test.util.PermissionsHelper.AUTHOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.EDITOR_ROLE;

/**
 * GH Issue 1468: an Author's ownership of a wiki page must not let them delete descendant pages created by others
 * when deleting the page's subtree.
 */
@Category({Daily.class, Wiki.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class WikiDeletePermissionsTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "WikiDeletePermissionsTest";
    private static final TestUser AUTHOR_1 = new TestUser("author1@wikideletepermissions.test");
    private static final TestUser AUTHOR_2 = new TestUser("author2@wikideletepermissions.test");
    private static final TestUser EDITOR = new TestUser("editor@wikideletepermissions.test");
    private static final File ATTACHMENT = TestFileUtils.getSampleData("InlineImages/help.jpg");
    private static final Locator SUBTREE_CHECKBOX = Locator.id("isDeletingSubtree");
    private static final Locator SUBTREE_WARNING = Locator.css("span.labkey-error");

    @BeforeClass
    public static void setupProject()
    {
        WikiDeletePermissionsTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModules(List.of("Wiki"));

        AUTHOR_1.create(this).addPermission(AUTHOR_ROLE, getProjectName());
        AUTHOR_2.create(this).addPermission(AUTHOR_ROLE, getProjectName());
        EDITOR.create(this).addPermission(EDITOR_ROLE, getProjectName());
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, AUTHOR_1, AUTHOR_2, EDITOR);
    }

    @Test
    public void testAuthorCannotDeleteSubtreeWithOthersChildren() throws Exception
    {
        int parentId = createPageAs(AUTHOR_1, "P1", null).getInt("rowId");
        JSONObject child = createPageAs(AUTHOR_2, "C1a", parentId);
        createPageAs(EDITOR, "C1b", parentId);

        // Give C1a version history and an attachment, which a subtree delete would also destroy
        addVersion(child);
        addAttachment("C1a", ATTACHMENT);

        AUTHOR_1.impersonate();
        goToDeleteConfirm("P1");
        Assert.assertFalse("Subtree option should be disabled when the user can't delete every page in the subtree",
                SUBTREE_CHECKBOX.findElement(getDriver()).isEnabled());
        assertTextPresent("You can't delete the entire subtree because you don't have permission to delete the child page");

        // A direct POST, as in the issue's repro, must also be rejected
        assertSubtreeDeleteRejected("P1");
        AUTHOR_1.stopImpersonating();

        assertPagesPresent("P1", "C1a", "C1b");
        Assert.assertEquals("C1a's version history should be intact", 2, getVersionCount("C1a"));
        assertAttachmentPresent("C1a", ATTACHMENT);
    }

    @Test
    public void testAuthorCannotDeleteSubtreeWithOthersGrandchild() throws Exception
    {
        int parentId = createPageAs(AUTHOR_1, "P2", null).getInt("rowId");
        int childId = createPageAs(AUTHOR_1, "C2", parentId).getInt("rowId");
        createPageAs(AUTHOR_2, "G2", childId);

        AUTHOR_1.impersonate();
        String error = assertSubtreeDeleteRejected("P2");
        Assert.assertTrue("Error should name the grandchild that blocks the delete: " + error,
                error.contains("G2"));
        AUTHOR_1.stopImpersonating();

        assertPagesPresent("P2", "C2", "G2");
    }

    @Test
    public void testStaleConfirmPageIsRechecked() throws Exception
    {
        int parentId = createPageAs(AUTHOR_1, "P3", null).getInt("rowId");
        createPageAs(AUTHOR_1, "C3", parentId);

        AUTHOR_1.impersonate();
        goToDeleteConfirm("P3");
        Assert.assertTrue("Subtree option should be enabled while the author owns the entire subtree",
                SUBTREE_CHECKBOX.findElement(getDriver()).isEnabled());

        // While the confirm page is open, another user adds a child. Impersonation is per-session, so switch users via
        // the API without navigating away; the CSRF token lives in a cookie, so the open form stays valid.
        AUTHOR_1.stopImpersonating();
        createPageAs(AUTHOR_2, "C3b", parentId);
        AUTHOR_1.impersonate();

        checkCheckbox(SUBTREE_CHECKBOX);
        clickButton("Delete");
        assertTextPresent("You do not have permissions to delete the child wiki page");
        assertTextPresent("C3b");
        AUTHOR_1.stopImpersonating();

        assertPagesPresent("P3", "C3", "C3b");
    }

    @Test
    public void testAuthorCanDeleteOwnSubtree() throws Exception
    {
        int parentId = createPageAs(AUTHOR_1, "P4", null).getInt("rowId");
        int childId = createPageAs(AUTHOR_1, "C4", parentId).getInt("rowId");
        createPageAs(AUTHOR_1, "G4", childId);

        AUTHOR_1.impersonate();
        deleteViaUi("P4", true);
        AUTHOR_1.stopImpersonating();

        assertPagesAbsent("P4", "C4", "G4");
    }

    @Test
    public void testAuthorCanDeletePageWithoutSubtree() throws Exception
    {
        int parentId = createPageAs(AUTHOR_1, "P5", null).getInt("rowId");
        createPageAs(AUTHOR_2, "C5", parentId);

        AUTHOR_1.impersonate();
        deleteViaUi("P5", false);
        AUTHOR_1.stopImpersonating();

        assertPagesAbsent("P5");
        assertPagesPresent("C5");
    }

    @Test
    public void testEditorCanDeleteSubtreeWithOthersChildren() throws Exception
    {
        int parentId = createPageAs(AUTHOR_1, "P6", null).getInt("rowId");
        createPageAs(AUTHOR_2, "C6", parentId);

        EDITOR.impersonate();
        deleteViaUi("P6", true);
        EDITOR.stopImpersonating();

        assertPagesAbsent("P6", "C6");
    }

    @Test
    public void testAuthorCannotDeleteOthersPage() throws Exception
    {
        createPageAs(AUTHOR_2, "P7", null);

        AUTHOR_1.impersonate();
        goToDeleteConfirm("P7");
        assertTextPresent("You do not have permissions to delete this wiki page");
        assertElementNotPresent(SUBTREE_CHECKBOX);
        AUTHOR_1.stopImpersonating();

        assertPagesPresent("P7");
    }

    @Test
    public void testUndeletableDescendantNameIsEncoded() throws Exception
    {
        String childName = "<b>C8</b>";
        int parentId = createPageAs(AUTHOR_1, "P8", null).getInt("rowId");
        createPageAs(AUTHOR_2, childName, parentId);

        AUTHOR_1.impersonate();
        goToDeleteConfirm("P8");
        String warning = SUBTREE_WARNING.findElement(getDriver()).getText();
        Assert.assertTrue("Warning should show the page name as literal text: " + warning, warning.contains(childName));
        assertElementNotPresent(SUBTREE_WARNING.append(Locator.tag("b")));
        AUTHOR_1.stopImpersonating();
    }

    /** Creates a wiki page as the given user and returns its wikiProps */
    private JSONObject createPageAs(TestUser user, String name, @Nullable Integer parentId) throws IOException, CommandException
    {
        user.impersonate();
        try
        {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("title", name);
            json.put("rendererType", "HTML");
            json.put("body", "<p>Created by " + user.getEmail() + "</p>");
            json.put("pageVersionId", -1);
            if (null != parentId)
                json.put("parent", parentId);

            return saveWiki(json);
        }
        finally
        {
            user.stopImpersonating();
        }
    }

    /** Saves a second version of a page, as the current user, leaving its other properties unchanged */
    private void addVersion(JSONObject wikiProps) throws IOException, CommandException
    {
        JSONObject json = new JSONObject();
        for (String prop : List.of("entityId", "pageVersionId", "name", "title", "rendererType", "parent", "showAttachments", "shouldIndex"))
            json.put(prop, wikiProps.get(prop));
        json.put("body", "<p>Second version</p>");

        saveWiki(json);
    }

    private JSONObject saveWiki(JSONObject json) throws IOException, CommandException
    {
        SimplePostCommand command = new SimplePostCommand("wiki", "saveWiki");
        command.setJsonObject(json);
        var response = command.execute(createDefaultConnection(), getProjectName());
        return new JSONObject(response.getParsedData()).getJSONObject("wikiProps");
    }

    private void addAttachment(String pageName, File file)
    {
        beginAt(WebTestHelper.buildURL("wiki", getProjectName(), "edit", Map.of("name", pageName)));
        new EditPage(getDriver()).addAttachment(file).saveAndClose();
    }

    private void goToDeleteConfirm(String name)
    {
        beginAt(WebTestHelper.buildURL("wiki", getProjectName(), "delete", Map.of("name", name)));
    }

    private void deleteViaUi(String name, boolean deleteSubtree)
    {
        goToDeleteConfirm(name);
        if (deleteSubtree)
            checkCheckbox(SUBTREE_CHECKBOX);
        clickButton("Delete");
    }

    /**
     * POSTs a subtree delete as the current (impersonated) user, asserts that it's rejected as unauthorized, and
     * returns the error response text
     */
    private String assertSubtreeDeleteRejected(String name) throws IOException
    {
        SimplePostCommand command = new SimplePostCommand("wiki", "delete");
        command.setParameters(Map.of("name", name, "isDeletingSubtree", true));
        try
        {
            command.execute(createDefaultConnection(), getProjectName());
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Wrong status for rejected subtree delete", 403, e.getStatusCode());
            return e.getResponseText();
        }
        throw new AssertionError("Subtree delete of '" + name + "' should have been rejected");
    }

    private Set<String> getPageNames() throws IOException, CommandException
    {
        SelectRowsCommand command = new SelectRowsCommand("wiki", "CurrentWikiVersions");
        command.setColumns(List.of("Name"));
        return command.execute(createDefaultConnection(), getProjectName()).getRows().stream()
                .map(row -> (String) row.get("Name"))
                .collect(Collectors.toSet());
    }

    private int getVersionCount(String pageName) throws IOException, CommandException
    {
        SelectRowsCommand command = new SelectRowsCommand("wiki", "AllWikiVersions");
        command.addFilter(new Filter("Name", pageName));
        return command.execute(createDefaultConnection(), getProjectName()).getRowCount().intValue();
    }

    private void assertPagesPresent(String... names) throws IOException, CommandException
    {
        Set<String> pageNames = getPageNames();
        for (String name : names)
            Assert.assertTrue("Wiki page '" + name + "' should still exist. Pages: " + pageNames, pageNames.contains(name));
    }

    private void assertPagesAbsent(String... names) throws IOException, CommandException
    {
        Set<String> pageNames = getPageNames();
        for (String name : names)
            Assert.assertFalse("Wiki page '" + name + "' should have been deleted. Pages: " + pageNames, pageNames.contains(name));
    }

    private void assertAttachmentPresent(String pageName, File file)
    {
        beginAt(WebTestHelper.buildURL("wiki", getProjectName(), "page", Map.of("name", pageName)));
        assertElementPresent(Locator.linkContainingText(file.getName()));
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("wiki");
    }
}
