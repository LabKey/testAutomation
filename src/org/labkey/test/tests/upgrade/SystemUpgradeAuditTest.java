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
package org.labkey.test.tests.upgrade;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.TestUser;
import org.labkey.test.util.Version;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers the audit event recorded by SystemUpgradeAuditProvider when the server comes up on a new version or build.
 * Most assertions hold on any single boot; testVersionChangeRecorded is the reason this is an upgrade test, since a
 * recorded version <i>change</i> only exists once a server has booted twice on two different versions.
 */
@Category({})
public class SystemUpgradeAuditTest extends BaseUpgradeTest
{
    /** The release the provider shipped in. Upgrading from anything earlier leaves no prior event to change from. */
    private static final String FIRST_AUDITED_VERSION = "26.9";

    private static final String AUDIT_QUERY = "SystemUpgradeAuditEvent";
    private static final String AUDIT_LOG_LABEL = "System Upgrade events";

    private static final TestUser PROJECT_ADMIN = new TestUser("project_admin@systemupgradeaudit.test");

    private static final List<String> AUDIT_COLUMNS = List.of(
        "RowId",
        "Created",
        "ChangeType",
        "ReleaseVersion",
        "PreviousReleaseVersion",
        "BuildTime",
        "PreviousBuildTime",
        "HasSchemaUpgrade",
        "Comment"
    );

    @Override
    protected void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        PROJECT_ADMIN.create(this)
            .setInitialPassword()
            .addPermission(PermissionsHelper.PROJECT_ADMIN_ROLE, getProjectName());
    }

    @Before
    public void preTest()
    {
        PROJECT_ADMIN.load(this);
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, PROJECT_ADMIN);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    /** Every server has run at least one boot with this feature, so a baseline event must exist for the running build. */
    @Test
    public void testBaselineEventRecorded() throws Exception
    {
        List<Map<String, Object>> rows = getUpgradeEvents(createDefaultConnection());
        assertFalse("No " + AUDIT_QUERY + " rows were found; the startup listener did not record an event", rows.isEmpty());

        Map<String, Object> latest = rows.getFirst();
        assertEquals("Latest event should record the version the server is running",
            getServerReleaseVersion(), latest.get("ReleaseVersion"));
        assertNotNull("ChangeType should be set", latest.get("ChangeType"));
        assertNotNull("Comment should summarize the change", latest.get("Comment"));
        assertNotNull("HasSchemaUpgrade should be set", latest.get("HasSchemaUpgrade"));
    }

    /**
     * The post-upgrade phase is the only place the interesting case occurs: one server that has booted on two
     * different versions, so the event carries both of them.
     */
    @Test
    @EarliestVersion(FIRST_AUDITED_VERSION)
    public void testVersionChangeRecorded() throws Exception
    {
        Assume.assumeFalse("A version change is only visible after the upgrade", isUpgradeSetupPhase);
        assertNotNull("Set webtest.upgradePreviousVersion or labkeyVersion to verify the recorded change", setupVersion);

        Map<String, Object> latest = getUpgradeEvents(createDefaultConnection()).getFirst();

        String previousReleaseVersion = (String) latest.get("PreviousReleaseVersion");
        assertNotNull("Event should record the version the server upgraded from", previousReleaseVersion);
        assertEquals("Event should record the version the setup phase ran on",
            setupVersion.trim(2), new Version(previousReleaseVersion).trim(2));
        assertEquals("Event should record the version the server upgraded to",
            getServerReleaseVersion(), latest.get("ReleaseVersion"));
        assertEquals("Booting on a newer release should be recorded as an upgrade", "Upgrade", latest.get("ChangeType"));
        assertNotEquals("A new build should have been deployed", latest.get("PreviousBuildTime"), latest.get("BuildTime"));

        // Every release bumps the core module's SchemaVersion, so crossing a release boundary always runs scripts.
        // This is the only phase where the flag can be checked against a known-true expectation.
        assertEquals("Upgrading across releases should have run schema scripts", true, latest.get("HasSchemaUpgrade"));
    }

    /**
     * A restart with no rebuild must not add a second event. Neither phase can restart the server on the same build,
     * so this checks the invariant that survives any boot: each version and build pair appears exactly once.
     */
    @Test
    public void testNoDuplicateEventForCurrentBuild() throws Exception
    {
        List<Map<String, Object>> rows = getUpgradeEvents(createDefaultConnection());
        Map<String, Object> latest = rows.getFirst();

        long matching = rows.stream()
            .filter(row -> Objects.equals(row.get("ReleaseVersion"), latest.get("ReleaseVersion"))
                && Objects.equals(row.get("BuildTime"), latest.get("BuildTime")))
            .count();

        assertEquals("Exactly one event should be recorded per release version and build time", 1, matching);
    }

    @Test
    public void testAdminConsoleGrid()
    {
        DataRegionTable table = new AuditLogHelper(this).goToAuditEventView(AUDIT_LOG_LABEL);

        assertTrue("Admin Console audit grid should show the system upgrade event", table.getDataRowCount() > 0);
        assertTrue("Grid should show the running release version",
            table.getColumnDataAsText("ReleaseVersion").contains(getServerReleaseVersion()));
    }

    /**
     * The event lives in the root container, so only users holding CanSeeAuditLogPermission at root can read it -
     * a project-scoped admin cannot, even with an allFolders container filter. See the impl plan's section 3.1.
     */
    @Test
    public void testRootContainerPermissions() throws Exception
    {
        Connection projectAdmin = PROJECT_ADMIN.getUserConnection();
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);

        assertFalse("Site admin should see the system upgrade event",
            getUpgradeEvents(createDefaultConnection()).isEmpty());

        assertTrue("Project admin without a site-level audit role should not see the root container event",
            getUpgradeEventsFromProject(projectAdmin).isEmpty());

        permissionsHelper.setSiteRoleUserPermissions(PROJECT_ADMIN.getEmail(), PermissionsHelper.SEE_AUDIT_LOG_SITE_ROLE);
        try
        {
            assertFalse("A user holding the site-level audit role should see the root container event",
                getUpgradeEventsFromProject(projectAdmin).isEmpty());
        }
        finally
        {
            permissionsHelper.removeUserRoleAssignment(PROJECT_ADMIN.getEmail(), PermissionsHelper.SEE_AUDIT_LOG_SITE_ROLE, "/");
        }
    }

    /** Newest first, read directly from the root container */
    private List<Map<String, Object>> getUpgradeEvents(Connection connection) throws IOException, CommandException
    {
        return executeSelect(connection, "/", null);
    }

    /** What the app grids do: query from a project with an allFolders container filter */
    private List<Map<String, Object>> getUpgradeEventsFromProject(Connection connection) throws IOException, CommandException
    {
        return executeSelect(connection, getProjectName(), ContainerFilter.AllFolders);
    }

    private List<Map<String, Object>> executeSelect(Connection connection, String containerPath, ContainerFilter containerFilter) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", AUDIT_QUERY);
        cmd.setColumns(AUDIT_COLUMNS);
        cmd.setSorts(List.of(new Sort("Created", Sort.Direction.DESCENDING), new Sort("RowId", Sort.Direction.DESCENDING)));
        if (containerFilter != null)
            cmd.setContainerFilter(containerFilter);

        SelectRowsResponse response = cmd.execute(connection, containerPath);
        return response.getRows();
    }

    private String getServerReleaseVersion()
    {
        // Same value the audit event records: AppProps.getReleaseVersion()
        return (String) executeScript("return LABKEY.versionString;");
    }
}
