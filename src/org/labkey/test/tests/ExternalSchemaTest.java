/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.SimpleHttpResponse;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyA.class, Data.class})
public class ExternalSchemaTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ExternalSchemaProject";
    private static final String FOLDER_NAME = "SubFolder";

    private static final String DB_SCHEMA_NAME = "test";
    private static final String USER_SCHEMA_NAME = "Test";
    private static final String TABLE_NAME = "TestTable";

    private static class Row
    {
        public Integer rowid;
        public String text;
        public int intNotNull;
        public Date dateTimeNotNull;

        public Row(String text, int intNotNull, Date dateTimeNotNull)
        {
            this(null, text, intNotNull, dateTimeNotNull);
        }
        
        public Row(Integer rowid, String text, int intNotNull, Date dateTimeNotNull)
        {
            this.rowid = rowid;
            this.text = text;
            this.intNotNull = intNotNull;
            if (dateTimeNotNull != null)
                this.dateTimeNotNull = dateTimeNotNull;
            else
            {
                Calendar c = Calendar.getInstance();
                c.set(2008, Calendar.OCTOBER, 25);
                c.clear(Calendar.MILLISECOND); // milliseconds aren't serialized in JSON
                this.dateTimeNotNull = c.getTime();
            }
        }

        public Row(String test, int intNotNull)
        {
            this(test, intNotNull, null);
        }
        
        public Row(int rowid, String test, int intNotNull)
        {
            this(rowid, test, intNotNull, null);
        }

        public Map<String, Object> toMap()
        {
            HashMap<String, Object> map = new HashMap<>();
            if (rowid != null)
                map.put("Rowid", rowid);
            map.put("Text", text);
            map.put("IntNotNull", intNotNull);
            map.put("DatetimeNotNull", dateTimeNotNull);
            map.put("BitNotNull", Boolean.TRUE);
            return map;
        }

        public static Row fromMap(Map<String, Object> map) throws ParseException
        {
            Number rowid = (Number)map.get("rowid");
            String text = (String)map.get("text");
            Number intNotNull = (Number)map.get("intnotnull");
            String dateStr = (String)map.get("datetimenotnull");
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/d HH:mm:ss");
            Date dateNotNull = fmt.parse(dateStr);
            return new Row(rowid.intValue(), text, intNotNull.intValue(), dateNotNull);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Row row = (Row) o;

            return intNotNull == row.intNotNull &&
                   dateTimeNotNull.equals(row.dateTimeNotNull) &&
                   text.equals(row.text) &&
                   (rowid == null || row.rowid == null || rowid.equals(row.rowid));
        }

        @Override
        public int hashCode()
        {
            int result = text.hashCode();
            result = 31 * result + intNotNull;
            result = 31 * result + dateTimeNotNull.hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            return String.format("{rowId: %s,\n" +
                    "text: '%s',\n" +
                    "intNotNull: %s,\n" +
                    "dateTimeNotNull: %s}", rowid == null ? null : rowid, text, intNotNull, dateTimeNotNull);
        }
    }

    void createProject()
    {
        log("** Create project: " + PROJECT_NAME);
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME);
    }

    void ensureExternalSchema(String containerPath)
    {
        log("** Create ExternalSchema: " + USER_SCHEMA_NAME);
        beginAt("/query/" + containerPath + "/admin.view");

        if (!isTextPresent("reload"))
        {
            clickAndWait(Locator.linkWithText("new external schema"));
            setFormElement(Locator.name("userSchemaName"), USER_SCHEMA_NAME);
            setFormElement(Locator.name("sourceSchemaName"), DB_SCHEMA_NAME);
            setFormElement(Locator.name("metaData"), TestFileUtils.getFileContents("server/modules/core/resources/schemas/test.xml"));
            clickButton("Create");
        }

        assertTextPresent(USER_SCHEMA_NAME);
        assertTextNotPresent("reload all schemas");  // Present only for external schemas > 1
    }

    void setEditable(String containerPath, boolean editable)
    {
        beginAt("/query/" + containerPath + "/admin.view");
        clickAndWait(Locator.linkWithText("edit"));
        if (editable)
            checkCheckbox(Locator.checkboxByName("editable"));
        else
            uncheckCheckbox(Locator.checkboxByName("editable"));
        clickButton("Update");
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps() throws Exception
    {
//        TODO: Test an external schema without applying metadata; verify that table and column names match JDBC meta
//        data casing, which will differ between PostgreSQL and SQL Server. Use code like the below to distinguish.
//
//        // External schemas report JDBC names, so we expect different casing on PostgreSQL vs. Microsoft SQL Server
//        switch (WebTestHelper.getDatabaseType())
//        {
//            case PostgreSQL:
//                TABLE_NAME = "testtable";
//                break;
//            case MicrosoftSQLServer:
//                TABLE_NAME = "TestTable";
//                break;
//            default:
//                throw new IllegalStateException("Unknown database type");
//        }
//
        createProject();
        ensureExternalSchema(PROJECT_NAME);
        doTestContainer();

        setEditable(PROJECT_NAME, false);
        doTestUneditable();

        // set up an additional db user schema in the sub-folder so we can check container perms
        ensureExternalSchema(PROJECT_NAME + "/" + FOLDER_NAME);
        setEditable(PROJECT_NAME, true);
        setEditable(PROJECT_NAME + "/" + FOLDER_NAME, true);

        doTestViaForm();
        doTestViaJavaApi();
    }

    void doTestUneditable()
    {
        log("** Trying to insert via form on uneditable external schema");
        insertViaFormNoPerms(PROJECT_NAME, "Haha!", 3);

        log("** Trying to insert via api on uneditable external schema");
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        Row[] rows = new Row[] {new Row("A", 3), new Row("B", 4) };
        InsertRowsCommand cmd = new InsertRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        for (Row row : rows)
            cmd.addRow(row.toMap());

        try
        {
            SaveRowsResponse resp = cmd.execute(cn, PROJECT_NAME);
            fail("Expected to throw CommandException");
        }
        catch (CommandException ex)
        {
            assertEquals(403, ex.getStatusCode());
        }
        catch (IOException fail)
        {
            throw new RuntimeException(fail);
        }

    }

    void doTestContainer()
    {
        log("** Trying to visit schema in container where it hasn't been configured");
        beginAt("/query/" + PROJECT_NAME + "/" + FOLDER_NAME + "/executeQuery.view?query.queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);
        assertTitleEquals("404: Error Page -- Could not find schema: Test");
    }
    
    void doTestViaForm()
    {
        String containerPath = StringUtils.join(Arrays.asList(PROJECT_NAME, FOLDER_NAME), "/");

        log("** Insert via form");
        int pk1 = insertViaForm(containerPath, "A", 3);
        int pk2 = insertViaForm(containerPath, "B", 4);

        log("** Update via form");
        updateViaForm(containerPath, pk1, "AA", 30);
        updateViaForm(containerPath, pk2, "BB", 40);

        // XXX: update form doesn't render correctly for mis-matched container
//        log("** Trying to update via form from a different container");
//        updateViaFormNoPerms(PROJECT_NAME, pk1, "Haha!", 3);

        log("** Trying to delete via form from a different container");
        deleteFromWrongContainer(PROJECT_NAME, new int[] { pk1 });

        log("** Delete via form");
        deleteViaForm(containerPath, new int[] { pk1, pk2});
    }
    
    void doTestViaJavaApi() throws Exception
    {
        String containerPath = StringUtils.join(Arrays.asList(PROJECT_NAME, FOLDER_NAME), "/");
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        Row[] inserted = new Row[] {new Row("A", 3), new Row("B", 4) };
        int[] pks = insertViaJavaApi(containerPath, cn, inserted);

        Row[] selected = selectViaJavaApi(containerPath, cn, pks);
        for (int i = 0; i < inserted.length; i++)
            assertEquals(inserted[i], selected[i]);
        
        Row[] updated = new Row[] {new Row(pks[0], "AA", 30), new Row(pks[1], "BB", 40)};
        updateViaJavaApi(containerPath, cn, updated);

        try
        {
            log("** Try to update via api from a different container");
            Row[] updateFail = new Row[] {new Row(pks[0], "Should not update", 300)};
            updateViaJavaApi(PROJECT_NAME, cn, updateFail);
            fail("expected exception when trying to update from another container");
        }
        catch (CommandException ex)
        {
            assertEquals(403, ex.getStatusCode());
//            assertEquals("The row is from the wrong container.", ex.getMessage());
//            assertEquals("org.labkey.api.view.UnauthorizedException", ex.getProperties().get("exceptionClass"));
        }
        
        try
        {
            log("** Try to delete via api from a different container");
            deleteViaJavaApi(PROJECT_NAME, cn, pks);
            fail("expected exception when trying to delete from another container");
        }
        catch (CommandException ex)
        {
            assertEquals(403, ex.getStatusCode());
//            assertEquals("The row is from the wrong container.", ex.getMessage());
//            assertEquals("org.labkey.api.view.UnauthorizedException", ex.getProperties().get("exceptionClass"));
        }
        
        deleteViaJavaApi(containerPath, cn, pks);
    }
    
    int[] insertViaJavaApi(String containerPath, Connection cn, Row... rows) throws IOException, CommandException
    {
        log("** Inserting via api...");
        InsertRowsCommand cmd = new InsertRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        for (Row row : rows)
            cmd.addRow(row.toMap());
        
        SaveRowsResponse resp = cmd.execute(cn, containerPath);
        assertEquals("Expected to insert " + rows.length + " rows", rows.length, resp.getRowsAffected().intValue());
        
        int[] pks = new int[rows.length];
        for (int i = 0; i < rows.length; i++)
        {
            Map<String, Object> row = resp.getRows().get(i);
            assertTrue(row.containsKey("rowid"));
            pks[i] = ((Number)row.get("rowid")).intValue();
        }
        
        return pks;
    }
    
    Row[] selectViaJavaApi(String containerPath, Connection cn, int... pks) throws IOException, CommandException
    {
        log("** Select via api: " + join(",", pks) + "...");
        Arrays.sort(pks);
        SelectRowsCommand cmd = new SelectRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        cmd.addFilter("RowId", join(";", pks), Filter.Operator.IN);
        cmd.addSort(new Sort("RowId"));
        SelectRowsResponse resp = cmd.execute(cn, containerPath);
        assertEquals("Expected to select " + pks.length + " rows", pks.length, resp.getRowCount().intValue());

        List<Row> rows = new ArrayList<>(pks.length);
        for (int i = 0; i < pks.length; i++)
        {
            Map<String, Object> row = resp.getRows().get(i);
            Integer rowid = (Integer)row.get("RowId");
            assertEquals("Expected requested rowid and selected rowid to be the same", rowid.intValue(), pks[i]);
            
            String text = (String)row.get("Text");
            int intNotNull = ((Number)row.get("IntNotNull")).intValue();
            Date datetimeNotNull = (Date)row.get("DatetimeNotNull");
            Row r = new Row(rowid, text, intNotNull, datetimeNotNull);
            rows.add(r);
        }
        return rows.toArray(new Row[rows.size()]);
    }
    
    Row[] updateViaJavaApi(String containerPath, Connection cn, Row... rows) throws ParseException, IOException, CommandException
    {
        log("** Updating via api...");
        UpdateRowsCommand cmd = new UpdateRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        for (Row row : rows)
            cmd.addRow(row.toMap());
        SaveRowsResponse resp = cmd.execute(cn, containerPath);
        assertEquals("Expected to update " + rows.length + " rows", rows.length, resp.getRowsAffected().intValue());

        Row[] updated = new Row[rows.length];
        for (int i = 0; i < rows.length; i++)
        {
            Map<String, Object> row = resp.getRows().get(i);
            updated[i] = Row.fromMap(row);
            assertEquals(rows[i], updated[i]);
        }
        return updated;
    }
    
    void deleteViaJavaApi(String containerPath, Connection cn, int... pks) throws IOException, CommandException
    {
        log("** Deleting via api: pks=" + join(",", pks) + "...");
        DeleteRowsCommand cmd = new DeleteRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        for (Integer pk : pks)
            cmd.addRow(Collections.singletonMap("RowId", (Object) pk));
        
        SaveRowsResponse resp = cmd.execute(cn, containerPath);
        assertEquals("Expected to delete " + pks.length + " rows", pks.length, resp.getRowsAffected().intValue());
        
        SelectRowsCommand selectCmd = new SelectRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        selectCmd.addFilter("RowId", join(";", pks), Filter.Operator.IN);
        SelectRowsResponse selectResp = selectCmd.execute(cn, containerPath);
        assertEquals("Expected to select 0 rows", 0, selectResp.getRowCount().intValue());
    }

    String join(String sep, int... pks)
    {
        StringBuilder buf = new StringBuilder(pks.length * 2);
        buf.append(pks[0]);
        for (int i = 1; i < pks.length; i++)
            buf.append(sep).append(pks[i]);
        return buf.toString();
    }

    private void _insertViaForm(String containerPath, String text, int intNotNull)
    {
        log("** Inserting via form: text='" + text + "', intNotNull=" + intNotNull + "...");
        beginAt("/query/" + containerPath + "/insertQueryRow.view?query.queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);
        setFormElement(Locator.name("quf_Text"), text);
        setFormElement(Locator.name("quf_IntNotNull"), String.valueOf(intNotNull));
        setFormElement(Locator.name("quf_DatetimeNotNull"), "2008-09-25");
        submit();
    }

    public void insertViaFormNoPerms(String containerPath, String text, int intNotNull)
    {
        log("** Inserting via form: text='" + text + "', intNotNull=" + intNotNull + "...");
        beginAt("/query/" + containerPath + "/insertQueryRow.view?query.queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);
        assertTextPresent("You do not have permission");
    }

    public int insertViaForm(String containerPath, String text, int intNotNull)
    {
        _insertViaForm(containerPath, text, intNotNull);

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + containerPath);

        DataRegionTable table = new DataRegionTable("query", this);
        table.setSort("RowId", SortDirection.DESC);
        table = new DataRegionTable("query", this);

        assertEquals("Expected 'Text' column to contain '" + text + "' for newly inserted row",
                text, table.getDataAsText(0, table.getColumnIndex("Text")));
        assertEquals("Expected 'IntNotNull' column to contain '" + intNotNull + "' for newly inserted row",
                String.valueOf(intNotNull), table.getDataAsText(0, table.getColumnIndex("IntNotNull")));

        // get newly inserted pk
        String rowidStr = table.getDataAsText(0, table.getColumnIndex("RowId"));
        assertTrue("Expected to find the RowId for the new row instead of '" + rowidStr + "'",
                rowidStr != null && !rowidStr.equals(""));
        return Integer.parseInt(rowidStr);
    }

    private void _updateViaForm(String containerPath, int pk, String text, int intNotNull)
    {
        log("** Updating via form: RowId=" + pk + ", text='" + text + "', intNotNull=" + intNotNull + "...");
        beginAt("/query/" + containerPath + "/updateQueryRow.view?query.queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME + "&RowId=" + pk);
        setFormElement(Locator.name("quf_Text"), text);
        setFormElement(Locator.name("quf_IntNotNull"), String.valueOf(intNotNull));
        setFormElement(Locator.name("quf_DatetimeNotNull"), "2008-09-25");
        submit();
    }

    public void updateViaFormNoPerms(String containerPath, int pk, String text, int intNotNull) throws IOException
    {
        _updateViaForm(containerPath, pk, text, intNotNull);
        assertTitleEquals("401: Error Page -- User does not have permission to perform this operation");
    }

    public void updateViaForm(String containerPath, int pk, String text, int intNotNull)
    {
        _updateViaForm(containerPath, pk, text, intNotNull);

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + containerPath);

        DataRegionTable table = new DataRegionTable("query", this);
        int row = table.getRowIndex(String.valueOf(pk));
        assertTrue("Expected to find row with pk='" + pk + "'", row > -1);

        assertEquals("Expected 'Text' column to contain '" + text + "' for updated row",
                text, table.getDataAsText(row, table.getColumnIndex("Text")));
        assertEquals("Expected 'IntNotNull' column to contain '" + intNotNull + "' for updated row",
                String.valueOf(intNotNull), table.getDataAsText(row, table.getColumnIndex("IntNotNull")));
    }

    private void _deleteViaForm(String containerPath, int[] pk)
    {
        log("** Deleting via form: pks=" + join(",", pk) + "...");
        beginAt("/query/" + containerPath + "/executeQuery.view?query.queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);


        for (int aPk : pk)
            checkCheckbox(Locator.checkboxByNameAndValue(".select", String.valueOf(aPk)));
        doAndWaitForPageToLoad(() ->
        {
            new DataRegionTable("query", getDriver()).clickHeaderButton("Delete");
            assertAlert("Are you sure you want to delete the selected row" + (pk.length == 1 ? "?" : "s?"));
        });
    }

    public void deleteFromWrongContainer(String containerPath, int[] pk)
    {
        // POST directly to the delete URL since ExternalSchemaTables don't allow container filtering
        // and rows in sub-folders won't be available for deleting via the user interface.
        // See Issue 17702: External schema tables disallow folder filter
        StringBuilder deleteUrl = new StringBuilder(WebTestHelper.getBaseURL() + "/query/" + containerPath + "/deleteQueryRows.post?");
        deleteUrl.append("schemaName=").append(USER_SCHEMA_NAME);
        deleteUrl.append("&query.queryName=").append(TABLE_NAME);
        deleteUrl.append("&dataRegionSelectionKey=$").append(USER_SCHEMA_NAME).append("$").append(TABLE_NAME).append("$$query");
        for (int aPk : pk)
            deleteUrl.append("&.select=").append(aPk);
        log("** Deleting via http POST to form action: " + deleteUrl);

        SimpleHttpResponse response = WebTestHelper.getHttpResponse(deleteUrl.toString(), "POST");
        int status = response.getResponseCode();
        String responseBody = response.getResponseBody();

        assertEquals("Wrong response code. Response: " + responseBody, HttpStatus.SC_OK, status);
        assertTrue("Expected error message not present (row in wrong container). Response: " + responseBody, responseBody.contains("The row is from the wrong container."));
    }

    public void deleteViaForm(String containerPath, int[] pk)
    {
        _deleteViaForm(containerPath, pk);

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + containerPath);

        DataRegionTable table = new DataRegionTable("query", this);
        for (int aPk : pk)
            assertEquals("Expected row '" + aPk + "' to be deleted.", -1, table.getRowIndex(String.valueOf(aPk)));
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
