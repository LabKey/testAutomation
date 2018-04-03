/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.jdbc.LabKeyConnection;
import org.labkey.test.categories.BVT;
import org.labkey.test.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by jeckels on 4/27/14.
 */
@Category(BVT.class)
public class LabKeyJDBCTest
{
    @BeforeClass
    public static void registerDriver() throws ClassNotFoundException
    {
        Class.forName("org.labkey.remoteapi.query.jdbc.LabKeyDriver");
    }

    public Connection _connection;

    @Before
    public void setup() throws SQLException
    {
        _connection = DriverManager.getConnection("jdbc:labkey:" + WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        _connection.setCatalog("/home");
    }

    @Test
    public void testCatalogs() throws SQLException
    {
        assertEquals("Didn't remember catalog", "/home", _connection.getCatalog());
        ResultSet rs = _connection.getMetaData().getCatalogs();
        boolean foundHome = false;
        boolean foundShared = false;

        while (rs.next())
        {
            String catalog = rs.getString("TABLE_CAT");
            foundHome |= "/home".equalsIgnoreCase(catalog);
            foundShared |= "/shared".equalsIgnoreCase(catalog);
        }

        assertTrue("Didn't find /home", foundHome);
        assertTrue("Didn't find /shared", foundShared);
    }

    @Test
    public void testSchemas() throws SQLException
    {
        ResultSet rs = _connection.getMetaData().getSchemas();
        boolean foundCore = false;
        boolean foundPipeline = false;

        while (rs.next())
        {
            String schema = rs.getString("TABLE_SCHEM");
            foundCore |= "core".equalsIgnoreCase(schema);
            foundPipeline |= "pipeline".equalsIgnoreCase(schema);
        }

        assertTrue("Didn't find core schema", foundCore);
        assertTrue("Didn't find pipeline schema", foundPipeline);
    }

    @Test
    public void testQueries() throws SQLException
    {
        ResultSet rs = _connection.getMetaData().getTables("/home", "core", null, null);
        boolean foundContainers = false;
        boolean foundGroups = false;

        while (rs.next())
        {
            assertEquals("Wrong catalog", "/home", rs.getString("TABLE_CAT"));
            assertEquals("Wrong schema", "core", rs.getString("TABLE_SCHEM"));
            String table = rs.getString("TABLE_NAME");
            foundContainers |= "containers".equalsIgnoreCase(table);
            foundGroups |= "groups".equalsIgnoreCase(table);
        }

        assertTrue("Didn't find containers query", foundContainers);
        assertTrue("Didn't find groups query", foundGroups);
    }

    @Test
    public void testSQL() throws SQLException
    {
        ResultSet rs = _connection.createStatement().executeQuery("SELECT * FROM core.Containers");

        assertTrue("Need one row", rs.next());
        // Also test column case-insensitivity
        assertTrue("Should be home container", "home".equalsIgnoreCase(rs.getString("name")));
        assertTrue("Should be home container", "home".equalsIgnoreCase(rs.getString("NAME")));
        assertTrue("Should be home container", "home".equalsIgnoreCase(rs.getString(rs.findColumn("name"))));
        assertTrue("Should be home container", "home".equalsIgnoreCase(rs.getString(rs.findColumn("NAME"))));

        assertTrue("Should have RowId", rs.getInt(rs.findColumn("rowid")) > 0);
        assertTrue("Should have RowId", rs.getInt(rs.findColumn("ROWID")) > 0);
        assertTrue("Should have RowId", rs.getInt("rowid") > 0);
        assertTrue("Should have RowId", rs.getInt("ROWID") > 0);

        assertFalse("Should be a single row", rs.next());
    }

    @Test
    public void testColumns() throws SQLException
    {
        ResultSet rs = _connection.getMetaData().getColumns("/home", "core", "containers", null);
        boolean foundEntityId = false;
        boolean foundRowId = false;
        boolean foundName = false;
        boolean foundCreated = false;
        boolean foundSearchable = false;
        boolean foundInstalledVersion = false;

        while (rs.next())
        {
            assertEquals("Wrong catalog", "/home", rs.getString("TABLE_CAT"));
            assertEquals("Wrong schema", "core", rs.getString("TABLE_SCHEM"));
            assertEquals("Wrong table", "containers", rs.getString("TABLE_NAME"));
            String column = rs.getString("COLUMN_NAME");
            if ("entityid".equalsIgnoreCase(column))
            {
                foundEntityId = true;
                assertEquals("Wrong type for EntityId", Types.VARCHAR, rs.getInt("DATA_TYPE"));
                assertEquals("Wrong auto-increment for EntityId", "NO", rs.getString("IS_AUTOINCREMENT"));
                assertEquals("Wrong auto-generated for EntityId", "YES", rs.getString("IS_GENERATEDCOLUMN"));
            }
            if ("rowid".equalsIgnoreCase(column))
            {
                foundRowId = true;
                assertEquals("Wrong type for RowId", Types.INTEGER, rs.getInt("DATA_TYPE"));
                assertEquals("Wrong auto-increment for RowId", "YES", rs.getString("IS_AUTOINCREMENT"));
                assertEquals("Wrong auto-generated for RowId", "YES", rs.getString("IS_GENERATEDCOLUMN"));
            }
            if ("name".equalsIgnoreCase(column))
            {
                foundName = true;
                assertEquals("Wrong type for Name", Types.VARCHAR, rs.getInt("DATA_TYPE"));
                assertEquals("Wrong auto-increment for Name", "NO", rs.getString("IS_AUTOINCREMENT"));
                assertEquals("Wrong auto-generated for Name", "NO", rs.getString("IS_GENERATEDCOLUMN"));
            }
            if ("created".equalsIgnoreCase(column))
            {
                foundCreated = true;
                assertEquals("Wrong type for Created", Types.TIMESTAMP, rs.getInt("DATA_TYPE"));
            }
            if ("searchable".equalsIgnoreCase(column))
            {
                foundSearchable = true;
                assertEquals("Wrong type for Searchable", Types.BOOLEAN, rs.getInt("DATA_TYPE"));
            }
        }

        rs = _connection.getMetaData().getColumns("/home", "core", "modules", null);
        while (rs.next())
        {
            if ("installedVersion".equalsIgnoreCase(rs.getString("COLUMN_NAME")))
            {
                foundInstalledVersion = true;
                assertEquals("Wrong type for InstalledVersion", Types.DOUBLE, rs.getInt("DATA_TYPE"));
            }
        }

        assertTrue("Didn't find EntityId column", foundEntityId);
        assertTrue("Didn't find Name column", foundName);
        assertTrue("Didn't find RowId column", foundRowId);
        assertTrue("Didn't find Created column", foundCreated);
        assertTrue("Didn't find Searchable column", foundSearchable);
        assertTrue("Didn't find InstalledVersion column", foundInstalledVersion);
    }

    @Test
    public void testRootIsCatalog() throws SQLException
    {
        // With the default rootIsCatalog == false, schemas/tables are visible at the top level
        validateTopLevelQueries(true);
        ((LabKeyConnection)_connection).setRootIsCatalog(true);
        validateTopLevelQueries(false);
    }

    private void validateTopLevelQueries(boolean expectQueries) throws SQLException
    {
        ResultSet rs = _connection.getMetaData().getTables("", null, null, null);
        boolean foundContainers = false;
        while (!foundContainers && rs.next())
        {
            String table = rs.getString("TABLE_NAME");
            foundContainers = "containers".equalsIgnoreCase(table);
        }
        if (expectQueries)
        {
            assertTrue("Didn't find top level containers query", foundContainers);
        }
        else
        {
            assertFalse("Found top level containers query", foundContainers);
        }
    }

    @Test
    public void testConnectionNoOps() throws Exception
    {
        // these are no-ops required by supported client applications, verify that none of them throw
        _connection.setAutoCommit(false);
        _connection.setReadOnly(true);
        _connection.clearWarnings();
        _connection.commit();
    }

    @Test
    public void testStatementResults() throws SQLException
    {
        PreparedStatement statement = _connection.prepareStatement("SELECT * FROM core.modules");
        ResultSet rs = statement.executeQuery();

        rs.next();
        // test all implemented typed value getters. Some of these are allowed casts from the underlying database types
        // If a value is being presented as an incorrect type, the call to a typed getter will fail.
        rs.getString("name");
        rs.getBigDecimal("installedVersion");
        rs.getFloat("installedVersion");
        rs.getDouble("installedVersion");

        // this is an implemented no-op
        statement.cancel();

        statement = _connection.prepareStatement("SELECT * FROM core.containers");
        rs = statement.executeQuery();
        rs.next();
        rs.getBoolean("searchable");
        rs.getTimestamp("created");
        rs.getDate("created");
        rs.getTime("created");
        rs.getString("entityId");
        rs.getInt("rowId");
        rs.getLong("rowId");
        rs.getShort("rowId");
    }
}
