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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers the audit event recorded by SystemUpgradeAuditProvider when the server comes up on a new version or build.
 * Most assertions hold on any single boot; testVersionChangeRecorded is the reason this is an upgrade test, since a
 * recorded version <i>change</i> only exists once a server has booted twice on two different versions.<br>
 * Reads from /home and builds what it needs inside each test, so it needs no setup phase - and therefore no matching
 * copy on the preceding ESR branch, which the setup phase would otherwise run.
 */
@Category({})
public class SystemUpgradeAuditTest extends BaseUpgradeTest
{
    private static final String AUDIT_QUERY = "SystemUpgradeAuditEvent";
    private static final String AUDIT_LOG_LABEL = "System Upgrade Events";

    private static final String PROJECT_ADMIN_EMAIL = "project_admin@systemupgradeaudit.test";

    /** First release that records SystemUpgradeAuditEvent, so the earliest one that can be a recorded previous version. */
    private static final String FIRST_AUDITED_RELEASE = "26.9";

    /** Every server has one, so no test-owned project is needed to query with a project-scoped container filter. */
    private static final String HOME_PROJECT = "/home";

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

    /** The event is written at startup and each test creates what it needs, so the setup phase has nothing to do. */
    @Override
    protected void doSetup()
    {
    }

    /** This test owns no project, so there is nothing for the framework to clean up. */
    @Override
    protected String getProjectName()
    {
        return null;
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
    public void testVersionChangeRecorded() throws Exception
    {
        Assume.assumeFalse("A version change is only visible after the upgrade", isUpgradeSetupPhase);
        assertNotNull("Set webtest.upgradePreviousVersion or labkeyVersion to verify the recorded change", setupVersion);

        Map<String, Object> latest = getUpgradeEvents(createDefaultConnection()).getFirst();
        String previousReleaseVersion = (String) latest.get("PreviousReleaseVersion");

        // The audit event was added in 26.9, so a setup boot on an earlier release left no event to compare against.
        if (wasSetupWithin(FIRST_AUDITED_RELEASE, null) && wasSetupBefore(getServerReleaseVersion()))
        {
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
        else
        {
            // Redeploying the same build records no second event, and a pre-26.9 setup boot recorded none at all, so
            // either way the newest row is a baseline.
            assertNull("Without a prior " + FIRST_AUDITED_RELEASE + " event there should be no recorded version change",
                previousReleaseVersion);
        }
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
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        TestUser projectAdmin = new TestUser(PROJECT_ADMIN_EMAIL);
        projectAdmin.create(this)
            .setInitialPassword()
            .addPermission(PermissionsHelper.PROJECT_ADMIN_ROLE, HOME_PROJECT);

        try
        {
            Connection connection = projectAdmin.getUserConnection();

            assertFalse("Site admin should see the system upgrade event",
                getUpgradeEvents(createDefaultConnection()).isEmpty());

            assertTrue("Project admin without a site-level audit role should not see the root container event",
                getUpgradeEventsFromProject(connection).isEmpty());

            permissionsHelper.setSiteRoleUserPermissions(projectAdmin.getEmail(), PermissionsHelper.SEE_AUDIT_LOG_SITE_ROLE);
            try
            {
                assertFalse("A user holding the site-level audit role should see the root container event",
                    getUpgradeEventsFromProject(connection).isEmpty());
            }
            finally
            {
                permissionsHelper.removeUserRoleAssignment(projectAdmin.getEmail(), PermissionsHelper.SEE_AUDIT_LOG_SITE_ROLE, "/");
            }
        }
        finally
        {
            projectAdmin.deleteUser();
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
        return executeSelect(connection, HOME_PROJECT, ContainerFilter.AllFolders);
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
