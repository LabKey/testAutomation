/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: kevink
 * Date: Mar 4, 2008 1:05:38 PM
 */
public class DataRegionTest extends BaseSeleniumWebTest
{
    private static final String FIRST_LINK = "First Page";
    private static final String PREV_LINK = "Previous Page";
    private static final String NEXT_LINK = "Next Page";
    private static final String LAST_LINK = "Last Page";

    private static final String PROJECT_NAME = "DataRegionProject";
    private static final String LIST_NAME = "WebColors" + INJECT_CHARS_1;
    private static final ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.Integer;
    private static final String LIST_KEY_NAME = "Key";

    private static final ListHelper.ListColumn NAME_COLUMN =
            new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Color Name");
    private static final ListHelper.ListColumn HEX_COLUMN =
            new ListHelper.ListColumn("Hex", "Hex", ListHelper.ListColumnType.String, "Hexadecimal");

    private static final String LIST_DATA;
    private static final int TOTAL_ROWS;

    static
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("aqua", "#00FFFF");
        map.put("black", "#000000");
        map.put("blue", "#0000FF");
        map.put("fuchsia", "#FF00FF");
        map.put("green", "#008000");
        map.put("grey", "#808080");
        map.put("lime", "#00FF00");
        map.put("maroon", "#800000");
        map.put("navy", "#000080");
        map.put("olive", "#808000");
        map.put("purple", "#800080");
        map.put("red", "#FF0000");
        map.put("silver", "#C0C0C0");
        map.put("teal", "#008080");
        map.put("white", "#FFFFFF");
        map.put("yellow", "#FFFF00");

        StringBuilder sb = new StringBuilder();
        sb.append("Key\tName\tHex\n");
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            sb.append(i).append("\t");
            sb.append(entry.getKey()).append("\t");
            sb.append(entry.getValue()).append("\n");
            i++;
        }

        LIST_DATA = sb.toString();
        TOTAL_ROWS = map.size();
    }

    DataRegionTable table;

    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup(boolean afterTest) throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps() throws Exception
    {
        createList();

        URL url = getURL();
        dataRegionTest(url, INJECT_CHARS_1);
        dataRegionTest(url, INJECT_CHARS_2);
        exportLoggingTest();
    }

    private void exportLoggingTest()
    {
        exportDataRegion("Script", "R");
        goToAuditLog();
        selectOptionByText("view", "Query events");
        waitForPageToLoad();

        DataRegionTable auditTable =  new DataRegionTable("audit", this);
        String[][] columnAndValues = new String[][] {{"Created By", getDisplayName()},
                {"Project", PROJECT_NAME}, {"Container", PROJECT_NAME}, {"SchemaName", "lists"},
                {"QueryName", LIST_NAME}, {"Comment", "Exported to script type r"}};
        for(String[] columnAndValue : columnAndValues)
        {
            log("Checking column: "+ columnAndValue[0]);
            Assert.assertEquals(columnAndValue[1], auditTable.getDataAsText(0, columnAndValue[0]));
        }
        clickLinkContainingText("details");
        assertTextPresent(LIST_NAME);
    }

    private void createList()
    {
        log("Create project: " + PROJECT_NAME);
        _containerHelper.createProject(PROJECT_NAME, null);

        log("Define list");
        _listHelper.createList(PROJECT_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, NAME_COLUMN, HEX_COLUMN);

        log("Upload data");
        _listHelper.uploadData(PROJECT_NAME, LIST_NAME, LIST_DATA);
    }

    private void dataRegionTest(URL url, String dataRegionName) throws Exception
    {
        log("** Beginning test for dataRegionName: " + dataRegionName);

        // Issue 11392: DataRegion name escaping in button menus.  Append evil dataRegionName parameter.
        try
        {
            String encodedName = EscapeUtil.encode(dataRegionName);
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&dataRegionName=" + encodedName);
        }
        catch (MalformedURLException mue)
        {
            throw new RuntimeException(mue);
        }
        beginAt(url.getFile());

        table = new DataRegionTable(dataRegionName, this, true, true);
        Assert.assertEquals(TOTAL_ROWS, table.getDataRowCount());
        Assert.assertEquals("aqua", table.getDataAsText(0, 3));
        Assert.assertEquals("#FFFF00", table.getDataAsText(15, 4));

        assertMenuButtonPresent("Page Size");
        assertLinkNotPresentWithTitle(PREV_LINK);
        assertLinkNotPresentWithTitle(NEXT_LINK);

        log("Test 3 per page");
        table.setMaxRows(3);
        clickButton("Page Size", 0);
        assertLinkPresentWithText("3 per page");
        assertLinkPresentWithText("40 per page");
        assertLinkPresentWithText("100 per page");
        assertLinkPresentWithText("250 per page");
        assertLinkPresentWithText("1000 per page");
        assertLinkPresentWithText("Show Selected");
        assertLinkPresentWithText("Show Unselected");
        assertLinkPresentWithText("Show All");
        assertTextPresent("1 - 3 of 16");
        Assert.assertEquals(3, table.getDataRowCount());

        log("Test 5 per page");
        table.setMaxRows(5);
        assertTextPresent("1 - 5 of 16");
        Assert.assertEquals(5, table.getDataRowCount());
        Assert.assertEquals("aqua", table.getDataAsText(0, 3));
        assertLinkNotPresentWithTitle(FIRST_LINK);
        assertLinkNotPresentWithTitle(PREV_LINK);
        assertLinkPresentWithTitle(NEXT_LINK);
        assertLinkPresentWithTitle(LAST_LINK);

        log("Next Page");
        table.pageNext();
        assertTextPresent("6 - 10 of 16");
        Assert.assertEquals(5, table.getDataRowCount());
        Assert.assertEquals("grey", table.getDataAsText(0, 3));
        assertLinkNotPresentWithTitle(FIRST_LINK);
        assertLinkPresentWithTitle(PREV_LINK);
        assertLinkPresentWithTitle(NEXT_LINK);
        assertLinkPresentWithTitle(LAST_LINK);

        log("Last Page");
        table.pageLast();
        assertTextPresent("16 - 16 of 16");
        Assert.assertEquals(1, table.getDataRowCount());
        Assert.assertEquals("yellow", table.getDataAsText(0, 3));
        assertLinkPresentWithTitle(FIRST_LINK);
        assertLinkPresentWithTitle(PREV_LINK);
        assertLinkNotPresentWithTitle(NEXT_LINK);
        assertLinkNotPresentWithTitle(LAST_LINK);

        log("Previous Page");
        table.pagePrev();
        assertTextPresent("11 - 15 of 16");
        Assert.assertEquals(5, table.getDataRowCount());
        Assert.assertEquals("purple", table.getDataAsText(0, 3));
        assertLinkPresentWithTitle(FIRST_LINK);
        assertLinkPresentWithTitle(PREV_LINK);
        assertLinkPresentWithTitle(NEXT_LINK);
        assertLinkNotPresentWithTitle(LAST_LINK);

        log("Setting a filter should go back to first page");
        table.setFilter(NAME_COLUMN.getName(), "Does Not Equal", "aqua");
        assertTextPresent("1 - 5 of 15");
        Assert.assertEquals("black", table.getDataAsText(0, 3));

        log("Show Selected");
        table.checkAllOnPage();
        for (int i = 0; i < 10 && !isTextPresent("Selected 5 of 15 rows."); i++)
            Thread.sleep(1000);
        assertTextPresent("Selected 5 of 15 rows.");
        clickButton("Page Size", 0);
        clickLinkWithText("Show Selected");
        Assert.assertEquals(5, table.getDataRowCount());
        assertLinkNotPresentWithTitle(FIRST_LINK);
        assertLinkNotPresentWithTitle(PREV_LINK);
        assertLinkNotPresentWithTitle(NEXT_LINK);
        assertLinkNotPresentWithTitle(LAST_LINK);

        log("Show All");
        clickButton("Page Size", 0);
        clickLinkWithText("Show All");
        Assert.assertEquals(15, table.getDataRowCount());
        assertLinkNotPresentWithTitle(FIRST_LINK);
        assertLinkNotPresentWithTitle(PREV_LINK);
        assertLinkNotPresentWithTitle(NEXT_LINK);
        assertLinkNotPresentWithTitle(LAST_LINK);

        log("Test 1000 per page");
        clickButton("Page Size", 0);
        clickLinkWithText("1000 per page");
        assertLinkNotPresentWithTitle(FIRST_LINK);
        assertLinkNotPresentWithTitle(PREV_LINK);
        assertLinkNotPresentWithTitle(NEXT_LINK);
        assertLinkNotPresentWithTitle(LAST_LINK);

    }

}
