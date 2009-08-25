/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PasswordUtil;
import org.labkey.remoteapi.query.*;
import org.labkey.remoteapi.Connection;

import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * User: kevink
 * Date: Oct 29, 2008 3:52:53 PM
 */
public class DbUserSchemaTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "DbUserSchemaProject";
    private static final String DB_SCHEMA_NAME = "test";
    private static final String USER_SCHEMA_NAME = "Test";
    private static final String TABLE_NAME = "testtable";

    private static class Row
    {
        public Integer rowid;
        public String text;
        public int intNotNull;
        public boolean bitNotNull;
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
                c.set(2008, 9, 25);
                c.clear(Calendar.MILLISECOND);
                this.dateTimeNotNull = c.getTime();
            }
        }
        
        public Map<String, Object> toMap()
        {
            HashMap<String, Object> map = new HashMap<String, Object>();
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
            SimpleDateFormat fmt = new SimpleDateFormat("d MMM yyyy HH:mm:ss Z");
            Date dateNotNull = fmt.parse(dateStr);
            return new Row(rowid.intValue(), text, intNotNull.intValue(), dateNotNull);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Row row = (Row) o;

            if (intNotNull != row.intNotNull) return false;
            if (!dateTimeNotNull.equals(row.dateTimeNotNull)) return false;
            if (!text.equals(row.text)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = text.hashCode();
            result = 31 * result + intNotNull;
            result = 31 * result + dateTimeNotNull.hashCode();
            return result;
        }

    }
    
    static Row Row(String test, int intNotNull) { return new Row(test, intNotNull, null); }
    static Row Row(int rowid, String test, int intNotNull) { return new Row(rowid, test, intNotNull, null); }
    static void assertEquals(Row row1, Row row2)
    {
        if (row1.rowid != null)
            assertEquals(row1.rowid, row2.rowid);
        assertEquals(row1.intNotNull, row2.intNotNull);
        assertEquals(row1.dateTimeNotNull, row2.dateTimeNotNull);
        assertEquals(row1.text, row2.text);
    }

    void ensureDbUserSchema()
    {
        log("Create project: " + PROJECT_NAME);
        createProject(PROJECT_NAME);

        log("Create DbUserSchema: " + USER_SCHEMA_NAME);
        beginAt("/query/" + PROJECT_NAME + "/begin.view");
        clickLinkWithText("Schema Administration");
        if (!isTextPresent("reload"))
        {
            clickLinkWithText("define new schema");
            setFormElement("userSchemaName", USER_SCHEMA_NAME);
            setFormElement("dbSchemaName", DB_SCHEMA_NAME);
            setFormElement("metaData", getFileContents("server/modules/core/resources/schemas/test.xml"));
            checkCheckbox("editable");
            clickNavButton("Create");
        }
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return "query";
    }

    protected void doTestSteps() throws Exception
    {
        ensureDbUserSchema();

        doTestViaForm();
        doTestViaJavaApi();
    }
    
    void doTestViaForm()
    {
        int pk1 = insertViaForm("A", 3);
        int pk2 = insertViaForm("B", 4);

        updateViaForm(pk1, "AA", 30);
        updateViaForm(pk2, "BB", 40);

        deleteViaForm(new int[] { pk1, pk2});
    }
    
    void doTestViaJavaApi() throws Exception
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        Row[] inserted = new Row[] { Row("A", 3), Row("B", 4) };
        int[] pks = insertViaJavaApi(cn, inserted);
        
        Row[] selected = selectViaJavaApi(cn, pks);
        for (int i = 0; i < inserted.length; i++)
            assertEquals(inserted[i], selected[i]);
        
        Row[] updated = new Row[] { Row(pks[0], "AA", 30), Row(pks[1], "BB", 40) };
        updateViaJavaApi(cn, updated);
        
        deleteViaJavaApi(cn, pks);
    }
    
    int[] insertViaJavaApi(Connection cn, Row... rows) throws Exception
    {
        InsertRowsCommand cmd = new InsertRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        for (Row row : rows)
            cmd.addRow(row.toMap());
        
        SaveRowsResponse resp = cmd.execute(cn, PROJECT_NAME);
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
    
    Row[] selectViaJavaApi(Connection cn, int... pks) throws Exception
    {
        SelectRowsCommand cmd = new SelectRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        cmd.addFilter("RowId", join(";", pks), Filter.Operator.IN);
        SelectRowsResponse resp = cmd.execute(cn, PROJECT_NAME);
        assertEquals("Expectd to select " + pks.length + " rows", pks.length, resp.getRowCount().intValue());

        List<Row> rows = new ArrayList<Row>(pks.length);
        for (int i = 0; i < pks.length; i++)
        {
            Map<String, Object> row = resp.getRows().get(i);
            Long rowid = (Long)row.get("RowId");
            assertEquals(rowid.intValue(), pks[i]);
            
            String text = (String)row.get("Text");
            int intNotNull = ((Number)row.get("IntNotNull")).intValue();
            Date datetimeNotNull = (Date)row.get("DatetimeNotNull");
            Row r = new Row(rowid.intValue(), text, intNotNull, datetimeNotNull);
            rows.add(r);
        }
        return rows.toArray(new Row[rows.size()]);
    }
    
    Row[] updateViaJavaApi(Connection cn, Row... rows) throws Exception
    {
        UpdateRowsCommand cmd = new UpdateRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        for (Row row : rows)
            cmd.addRow(row.toMap());
        SaveRowsResponse resp = cmd.execute(cn, PROJECT_NAME);
        assertEquals("Expectd to update " + rows.length + " rows", rows.length, resp.getRowsAffected().intValue());

        Row[] updated = new Row[rows.length];
        for (int i = 0; i < rows.length; i++)
        {
            Map<String, Object> row = resp.getRows().get(i);
            updated[i] = Row.fromMap(row);
            assertEquals(rows[i], updated[i]);
        }
        return updated;
    }
    
    void deleteViaJavaApi(Connection cn, int... pks) throws Exception
    {
        DeleteRowsCommand cmd = new DeleteRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        for (Integer pk : pks)
            cmd.addRow(Collections.singletonMap("RowId", (Object) pk));
        
        SaveRowsResponse resp = cmd.execute(cn, PROJECT_NAME);
        assertEquals("Expectd to delete " + pks.length + " rows", pks.length, resp.getRowsAffected().intValue());
        
        SelectRowsCommand selectCmd = new SelectRowsCommand(USER_SCHEMA_NAME, TABLE_NAME);
        selectCmd.addFilter("RowId", join(";", pks), Filter.Operator.IN);
        SelectRowsResponse selectResp = selectCmd.execute(cn, PROJECT_NAME);
        assertEquals("Expected to select 0 rows", 0, selectResp.getRowCount().intValue());
    }

    String join(String sep, int... pks)
    {
        StringBuffer buf = new StringBuffer(pks.length * 2);
        buf.append(pks[0]);
        for (int i = 1; i < pks.length; i++)
            buf.append(sep).append(pks[i]);
        return buf.toString();
    }
    
    public int insertViaForm(String text, int intNotNull)
    {
        log("Inserting text='" + text + "', intNotNull=" + intNotNull + "...");
        beginAt("/dbuserschema/" + PROJECT_NAME + "/insert.view?queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);
        setFormElement("quf_Text", text);
        setFormElement("quf_IntNotNull", String.valueOf(intNotNull));
        setFormElement("quf_DatetimeNotNull", "2008-09-25");
        submit();

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + PROJECT_NAME);

        DataRegionTable table = new DataRegionTable("query", this);
        table.setSort("RowId", SortDirection.DESC);

        assertEquals("Expected 'Text' column to contain '" + text + "' for newly inserted row",
                text, table.getDataAsText(0, table.getColumn("Text")));
        assertEquals("Expected 'IntNotNull' column to contain '" + intNotNull + "' for newly inserted row",
                String.valueOf(intNotNull), table.getDataAsText(0, table.getColumn("IntNotNull")));

        // get newly inserted pk
        String rowidStr = table.getDataAsText(0, table.getColumn("RowId"));
        assertTrue("Expected to find the RowId for the new row instead of '" + rowidStr + "'",
                rowidStr != null && !rowidStr.equals(""));
        return Integer.parseInt(rowidStr);
    }

    public void updateViaForm(int pk, String text, int intNotNull)
    {
        log("Updating pk=" + pk + ", text='" + text + "', intNotNull=" + intNotNull + "...");
        beginAt("/dbuserschema/" + PROJECT_NAME + "/update.view?queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME + "&pk=" + pk);
        setFormElement("quf_Text", text);
        setFormElement("quf_IntNotNull", String.valueOf(intNotNull));
        setFormElement("quf_DatetimeNotNull", "2008-09-25");
        submit();

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + PROJECT_NAME);

        DataRegionTable table = new DataRegionTable("query", this);
        int row = table.getRow(String.valueOf(pk));
        assertTrue("Expected to find row with pk='" + pk + "'", row > -1);
        
        assertEquals("Expected 'Text' column to contain '" + text + "' for updated row",
                text, table.getDataAsText(row, table.getColumn("Text")));
        assertEquals("Expected 'IntNotNull' column to contain '" + intNotNull + "' for updated row",
                String.valueOf(intNotNull), table.getDataAsText(row, table.getColumn("IntNotNull")));
    }

    public void deleteViaForm(int[] pk)
    {
        log("Deleting pks=" + join(",", pk) + "...");
        beginAt("/query/" + PROJECT_NAME + "/executeQuery.view?query.queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);
        for (int i = 0; i < pk.length; i++)
            checkCheckbox(Locator.checkboxByNameAndValue(".select", String.valueOf(pk[i])));
        selenium.chooseOkOnNextConfirmation();
        clickButton("Delete", 0);
        assertEquals(selenium.getConfirmation(), "Are you sure you want to delete the selected rows?");
        waitForPageToLoad();

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + PROJECT_NAME);

        DataRegionTable table = new DataRegionTable("query", this);
        for (int i = 0; i < pk.length; i++)
            assertEquals("Expected row '" + pk + "' to be deleted.", -1, table.getRow(String.valueOf(pk)));
    }
}
