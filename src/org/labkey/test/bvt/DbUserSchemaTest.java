/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

/**
 * User: kevink
 * Date: Oct 29, 2008 3:52:53 PM
 */
public class DbUserSchemaTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "DataRegionProject";
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
        return "";
    }

    public int insert(String text, int intnotnull)
    {
        beginAt("/dbuserschema/" + PROJECT_NAME + "/insert.view?queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);
        setFormElement("quf_text", text);
        setFormElement("quf_intnotnull", String.valueOf(intnotnull));
        setFormElement("quf_datetimenotnull", "2008-09-25");
        submit();

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + PROJECT_NAME);

        DataRegionTable table = new DataRegionTable("query", this);
        table.setSort("rowid", SortDirection.DESC);

        assertEquals(text, table.getDataAsText(0, table.getColumn("Text")));
//        assertEquals(String.valueOf(intnotnull), table.getDataAsText(0, table.getColumn("Intnotnull")));

        // get newly inserted pk
        String rowidStr = table.getDataAsText(0, table.getColumn("Rowid"));
        return Integer.parseInt(rowidStr);
    }

    public void update(int pk, String text, int intnotnull)
    {
        beginAt("/dbuserschema/" + PROJECT_NAME + "/update.view?queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME + "&pk=" + pk);
        setFormElement("quf_text", text);
        setFormElement("quf_intnotnull", String.valueOf(intnotnull));
        setFormElement("quf_datetimenotnull", "2008-09-25");
        submit();

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + PROJECT_NAME);

        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals(text, table.getDataAsText(String.valueOf(pk), "Text"));
//        assertEquals(String.valueOf(intnotnull), table.getDataAsText(String.valueOf(pk), "Intnotnull"));
    }

    public void delete(int[] pk)
    {
        beginAt("/query/" + PROJECT_NAME + "/executeQuery.view?query.queryName=" + TABLE_NAME + "&schemaName=" + USER_SCHEMA_NAME);
        for (int i = 0; i < pk.length; i++)
            checkCheckbox(Locator.checkboxByNameAndValue(".select", String.valueOf(pk[i]), false));
        clickNavButton("Delete");

        // assume no errors if we end up back on the grid view
        assertTitleEquals(TABLE_NAME + ": /" + PROJECT_NAME);

        DataRegionTable table = new DataRegionTable("query", this);
        for (int i = 0; i < pk.length; i++)
            assertEquals(-1, table.getRow(String.valueOf(pk)));
    }
}
