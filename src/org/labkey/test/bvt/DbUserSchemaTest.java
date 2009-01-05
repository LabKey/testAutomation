/*
 * Copyright (c) 2008 LabKey Corporation
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
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

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

    protected void doTestSteps() throws Exception
    {
        ensureDbUserSchema();

        int pk1 = insert("A", 3);
        int pk2 = insert("B", 4);

        update(pk1, "AA", 30);
        update(pk2, "BB", 40);

        delete(new int[] { pk1, pk2});
    }

    void ensureDbUserSchema()
    {
        log("Create project: " + PROJECT_NAME);
        createProject(PROJECT_NAME);
        clickNavButton("Done");

        log("Create DbUserSchema: " + USER_SCHEMA_NAME);
        beginAt("/query/" + PROJECT_NAME + "/begin.view");
        clickLinkWithText("Schema Administration");
        if (!isTextPresent("Reload"))
        {
            clickLinkWithText("Define New Schema");
            setFormElement("userSchemaName", USER_SCHEMA_NAME);
            setFormElement("dbSchemaName", DB_SCHEMA_NAME);
            setFormElement("metaData", getFileContents("server/modules/core/src/META-INF/test.xml"));
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

    public int insert(String text, int intNotNull)
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

    public void update(int pk, String text, int intNotNull)
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

    public void delete(int[] pk)
    {
        log("Deleting pks=" + StringUtils.join(Arrays.asList(pk), ",") + "...");
        beginAt("/query/" + PROJECT_NAME + "/executeQuery.view?query.queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);
        for (int i = 0; i < pk.length; i++)
            checkCheckbox(Locator.checkboxByNameAndValue(".select", String.valueOf(pk[i]), false));
        clickNavButton("Delete");

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + PROJECT_NAME);

        DataRegionTable table = new DataRegionTable("query", this);
        for (int i = 0; i < pk.length; i++)
            assertEquals("Expected row '" + pk + "' to be deleted.", -1, table.getRow(String.valueOf(pk)));
    }
}
