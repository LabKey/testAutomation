/*
 * Copyright (c) 2025-2026 LabKey Corporation
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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.LabkeyErrorPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PostgresOnlyTest;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.labkey.test.util.PermissionsHelper.PROJECT_ADMIN_ROLE;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class PostgresQueriesTest extends AbstractAdminConsoleTest implements PostgresOnlyTest
{

    @Test
    public void testQueries() throws IOException, CommandException
    {
        // Verify activity grid as site admin
        goToAdminConsole().clickPostgresActivity();
        assertTextPresent("pg_stat_activity");
        verifyActivityGrid(true);
        verifyApiAccess(true);

        // Verify project admin gets a 401 for activity grid
        pushLocation();
        goToHome();
        impersonateRole(PROJECT_ADMIN_ROLE);
        popLocation();
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());
        errorPage.assertUnauthorized(checker());
        verifyApiAccess(false);
        stopImpersonating();

        // Verify locks grid as site admin
        goToAdminConsole().clickPostgresLocks();
        verifyLocksGrid();

        // Verify locks grid as site admin
        goToAdminConsole().clickPostgresTableSizes();
        verifyTableSizesGrid();

        // Verify project admin gets a 401 for locks grid
        pushLocation();
        goToHome();
        impersonateRole(PROJECT_ADMIN_ROLE);
        popLocation();
        errorPage = new LabkeyErrorPage(getDriver());
        errorPage.assertUnauthorized(checker());
        stopImpersonating();

        // Log in as appAdmin
        signOut();
        signIn(APP_ADMIN_USER);
        goToAdminConsole().clickPostgresActivity();
        verifyActivityGrid(true);

        goToAdminConsole().clickPostgresLocks();
        verifyLocksGrid();
        verifyApiAccess(true);

        // log in as troubleshooter
        signOut();
        signIn(TROUBLESHOOTER_USER);
        goToAdminConsole().clickPostgresActivity();
        verifyActivityGrid(false);

        goToAdminConsole().clickPostgresLocks();
        verifyLocksGrid();
        verifyApiAccess(true);
    }

    private void verifyApiAccess(boolean expectSuccess) throws IOException, CommandException
    {
        SelectRowsResponse activityResponse = selectRows(expectSuccess, "pg_stat_activity");
        if (expectSuccess)
        {
            assertNotNull("Should have a response from a successful request", activityResponse);
            assertFalse("Should have at least one row", activityResponse.getRows().isEmpty());
            assertNotNull("Should have a pid", activityResponse.getRows().getFirst().get("pid"));
        }
        SelectRowsResponse locksResponse = selectRows(expectSuccess, "pg_locks");
        if (expectSuccess)
        {
            assertNotNull("Should have a response from a successful request", locksResponse);
            assertFalse("Should have at least one row", locksResponse.getRows().isEmpty());
            assertNotNull("Should have a pid", locksResponse.getRows().getFirst().get("locktype"));
        }
    }

    private SelectRowsResponse selectRows(boolean expectSuccess, String queryName) throws IOException, CommandException
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand command = new SelectRowsCommand("postgres", queryName);
        try
        {
            SelectRowsResponse response = command.execute(connection, "/");
            if (!expectSuccess)
            {
                fail("Request should have failed");
            }
            return response;
        }
        catch (CommandException e)
        {
            if (expectSuccess)
            {
                throw e;
            }
            else
            {
                assertEquals("Wrong status code", 404, e.getStatusCode());
            }
        }
        return null;
    }

    private void verifyLocksGrid()
    {
        assertTextPresent("pg_locks");
        DataRegionTable table = new DataRegionTable("query", this);
        List<String> cols = table.getColumnLabels();
        Assertions.assertThat(cols).as("pg_locks columns").contains("Locktype", "Virtualtransaction");
    }

    private void verifyTableSizesGrid()
    {
        assertTextPresent("pg_tablesizes");
        DataRegionTable table = new DataRegionTable("query", this);
        List<String> cols = table.getColumnLabels();
        Assertions.assertThat(cols).as("pg_tablesizes columns").contains("Table Schema", "Table Name", "Table Size", "Index Size", "Total Size");
        // Check a couple of expected tables
        table.setFilter("table_schema", "Equals", "audit");
        assertTextPresent("queryupdateauditdomain", "userauditdomain");
    }

    private void verifyActivityGrid(boolean expectDelete)
    {
        assertTextPresent("pg_stat_activity");
        DataRegionTable table = new DataRegionTable("query", this);
        List<String> cols = table.getColumnLabels();
        Assertions.assertThat(cols).as("pg_stat_activity columns").contains("Query", "Running Time Ms");
        assertTextPresent(
            "GREATEST(EXTRACT(MILLISECONDS FROM AGE(NOW(), query_start)), 0) END AS running_time_ms", 
            "AsyncQueryRequest:");
        assertEquals("Delete button should " + (expectDelete ? "" : "not ") + " be present",
                expectDelete ? 1 : 0,
                table.getHeaderButtons().stream().filter(a -> "Delete".equals(a.getDomAttribute("data-original-title"))).count());
    }
}
