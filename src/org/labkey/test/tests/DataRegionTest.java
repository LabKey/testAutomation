/*
 * Copyright (c) 2008-2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelperWD;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * User: kevink
 * Date: Mar 4, 2008 1:05:38 PM
 */
@Category({DailyA.class, Data.class})
public class DataRegionTest extends BaseWebDriverTest
{
    private static final String FIRST_LINK = "First Page";
    private static final String PREV_LINK = "Previous Page";
    private static final String NEXT_LINK = "Next Page";
    private static final String LAST_LINK = "Last Page";

    private static final String PROJECT_NAME = "DataRegionProject";
    private static final String LIST_NAME = "WebColors" + INJECT_CHARS_1;
    private static final ListHelperWD.ListColumnType LIST_KEY_TYPE = ListHelperWD.ListColumnType.Integer;
    private static final String LIST_KEY_NAME = "Key";

    private static final ListHelperWD.ListColumn NAME_COLUMN =
            new ListHelperWD.ListColumn("Name", "Name", ListHelperWD.ListColumnType.String, "Color Name");
    private static final ListHelperWD.ListColumn HEX_COLUMN =
            new ListHelperWD.ListColumn("Hex", "Hex", ListHelperWD.ListColumnType.String, "Hexadecimal");

    private static final String LIST_DATA;
    private static final int TOTAL_ROWS;

    static
    {
        Map<String, String> map = new LinkedHashMap<>();
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

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
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
        prepForPageLoad();
        selectOptionByText(Locator.name("view"), "Query export events");
        newWaitForPageToLoad();

        DataRegionTable auditTable =  new DataRegionTable("query", this);
        String[][] columnAndValues = new String[][] {{"Created By", getDisplayName()},
                {"Project", PROJECT_NAME}, {"Container", PROJECT_NAME}, {"SchemaName", "lists"},
                {"QueryName", LIST_NAME}, {"Comment", "Exported to script type r"}};
        for(String[] columnAndValue : columnAndValues)
        {
            log("Checking column: "+ columnAndValue[0]);
            assertEquals(columnAndValue[1], auditTable.getDataAsText(0, columnAndValue[0]));
        }
        clickAndWait(Locator.linkContainingText("details"));
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
        assertEquals(TOTAL_ROWS, table.getDataRowCount());
        assertEquals("aqua", table.getDataAsText(0, 3));
        assertEquals("#FFFF00", table.getDataAsText(15, 4));

        assertMenuButtonPresent("Page Size");
        assertElementNotPresent(Locator.linkWithText(PREV_LINK));
        assertElementNotPresent(Locator.linkWithText(NEXT_LINK));

        log("Test 3 per page");
        table.setMaxRows(3);
        clickButton("Page Size", 0);
        assertElementPresent(Locator.linkWithText("3 per page"));
        assertElementPresent(Locator.linkWithText("40 per page"));
        assertElementPresent(Locator.linkWithText("100 per page"));
        assertElementPresent(Locator.linkWithText("250 per page"));
        assertElementPresent(Locator.linkWithText("1000 per page"));
        assertElementPresent(Locator.linkWithText("Show Selected"));
        assertElementPresent(Locator.linkWithText("Show Unselected"));
        assertElementPresent(Locator.linkWithText("Show All"));
        assertPaginationText(1, 3, 16);
        assertEquals(3, table.getDataRowCount());

        log("Test 5 per page");
        table.setMaxRows(5);
        assertPaginationText(1, 5, 16);
        assertEquals(5, table.getDataRowCount());
        assertEquals("aqua", table.getDataAsText(0, 3));
        assertElementNotPresent(Locator.linkWithText(FIRST_LINK));
        assertElementNotPresent(Locator.linkWithText(PREV_LINK));
        assertElementPresent(Locator.linkWithText(NEXT_LINK));
        assertElementPresent(Locator.linkWithText(LAST_LINK));

        log("Next Page");
        table.pageNext();
        assertPaginationText(6, 10, 16);
        assertEquals(5, table.getDataRowCount());
        assertEquals("grey", table.getDataAsText(0, 3));
        assertElementNotPresent(Locator.linkWithText(FIRST_LINK));
        assertElementPresent(Locator.linkWithText(PREV_LINK));
        assertElementPresent(Locator.linkWithText(NEXT_LINK));
        assertElementPresent(Locator.linkWithText(LAST_LINK));

        log("Last Page");
        table.pageLast();
        assertPaginationText(16, 16, 16);
        assertEquals(1, table.getDataRowCount());
        assertEquals("yellow", table.getDataAsText(0, 3));
        assertElementPresent(Locator.linkWithText(FIRST_LINK));
        assertElementPresent(Locator.linkWithText(PREV_LINK));
        assertElementNotPresent(Locator.linkWithText(NEXT_LINK));
        assertElementNotPresent(Locator.linkWithText(LAST_LINK));

        log("Previous Page");
        table.pagePrev();
        assertPaginationText(11, 15, 16);
        assertEquals(5, table.getDataRowCount());
        assertEquals("purple", table.getDataAsText(0, 3));
        assertElementPresent(Locator.linkWithText(FIRST_LINK));
        assertElementPresent(Locator.linkWithText(PREV_LINK));
        assertElementPresent(Locator.linkWithText(NEXT_LINK));
        assertElementNotPresent(Locator.linkWithText(LAST_LINK));

        log("Setting a filter should go back to first page");
        table.setFilter(NAME_COLUMN.getName(), "Does Not Equal", "aqua");
        assertPaginationText(1, 5, 15);
        assertEquals("black", table.getDataAsText(0, 3));

        log("Show Selected");
        table.checkAllOnPage();
        for (int i = 0; i < 10 && !isTextPresent("Selected 5 of 15 rows."); i++)
            Thread.sleep(1000);
        assertTextPresent("Selected 5 of 15 rows.");
        clickButton("Page Size", 0);
        clickAndWait(Locator.linkWithText("Show Selected"));
        assertEquals(5, table.getDataRowCount());
        assertElementNotPresent(Locator.linkWithText(FIRST_LINK));
        assertElementNotPresent(Locator.linkWithText(PREV_LINK));
        assertElementNotPresent(Locator.linkWithText(NEXT_LINK));
        assertElementNotPresent(Locator.linkWithText(LAST_LINK));

        log("Show All");
        clickButton("Page Size", 0);
        clickAndWait(Locator.linkWithText("Show All"));
        assertEquals(15, table.getDataRowCount());
        assertElementNotPresent(Locator.linkWithText(FIRST_LINK));
        assertElementNotPresent(Locator.linkWithText(PREV_LINK));
        assertElementNotPresent(Locator.linkWithText(NEXT_LINK));
        assertElementNotPresent(Locator.linkWithText(LAST_LINK));

        log("Test 1000 per page");
        clickButton("Page Size", 0);
        clickAndWait(Locator.linkWithText("1000 per page"));
        assertElementNotPresent(Locator.linkWithText(FIRST_LINK));
        assertElementNotPresent(Locator.linkWithText(PREV_LINK));
        assertElementNotPresent(Locator.linkWithText(NEXT_LINK));
        assertElementNotPresent(Locator.linkWithText(LAST_LINK));

    }

    /**
     * Assert that a Data Region has expected pagination text (e.g. "1 - 3 of 16")
     * @param firstRow position in data of first displayed row
     * @param lastRow position in data of last displayed row
     * @param totalRows total number of rows in data behind the dataregion
     */
    private void assertPaginationText(int firstRow, int lastRow, int totalRows)
    {
        String expected = firstRow + " - " + lastRow + " of " + totalRows;
        String fullPaginationText = Locator.css(".labkey-pagination").findElement(getDriver()).getText();
        Pattern pattern = Pattern.compile("\\d+ - \\d+ of \\d+");
        Matcher matcher = pattern.matcher(fullPaginationText);
        matcher.find();
        String actual = matcher.group(0);
        assertEquals("Wrong pagination text", expected, actual);
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
