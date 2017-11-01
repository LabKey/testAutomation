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
import org.labkey.test.categories.BVT;
import org.labkey.test.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
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
        }

        assertTrue("Didn't find EntityId column", foundEntityId);
        assertTrue("Didn't find Name column", foundName);
        assertTrue("Didn't find RowId column", foundRowId);
    }
}
