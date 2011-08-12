/*
 * Copyright (c) 2010-2011 LabKey Corporation
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

import com.google.common.base.Function;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ListHelper;

/**
 * User: kevink
 * Date: Oct 14, 2010
 * Time: 11:37:20 AM
 */
public class CustomizeViewTest extends BaseSeleniumWebTest
{
    public static final String PROJECT_NAME = "CustomizeViewTest";
    public static final String LIST_NAME = "People" + INJECT_CHARS_1;
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";
    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";

    private final static String LAST_NAME_COLUMN = "LastName" + INJECT_CHARS_2;
    private final static ListHelper.ListColumn[] LIST_COLUMNS = new ListHelper.ListColumn[]
            {
                    new ListHelper.ListColumn("FirstName", "First Name" + INJECT_CHARS_1, ListHelper.ListColumnType.String, "The first name"),
                    new ListHelper.ListColumn(LAST_NAME_COLUMN, "Last Name", ListHelper.ListColumnType.String, "The last name"),
                    new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "The age" + INJECT_CHARS_1)
            };
    static
    {
        LIST_COLUMNS[0].setRequired(true);
        LIST_COLUMNS[1].setRequired(true);
    }

    private final static String[][] TEST_DATA =
            {
                    { "1", "Bill", "Billson", "34" },
                    { "2", "Jane", "Janeson", "42" },
                    { "3", "John", "Johnson", "17" },
                    { "4", "Mandy", "Mandyson", "32" },
                    { "5", "Norbert", "Norbertson", "28" },
                    { "6", "Penny", "Pennyson", "38" },
                    { "7", "Yak", "Yakson", "88" },
            };


    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try { deleteProject(PROJECT_NAME) ; } catch (Throwable t) { }
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        createProject(PROJECT_NAME);
        createList();

        log("** Show only LastName and Age");
        setColumns(LAST_NAME_COLUMN, "Age");
        assertTextPresent("Norbertson");
        assertTextNotPresent("First Name");

        log("** Add filter: LastName starts with 'J'");
        addFilter(LAST_NAME_COLUMN, "Starts With", "J");
        assertTextNotPresent("Norbertson");
        assertTextPresent("Janeson");
        assertTextPresent("Johnson");

        log("** Add another filter: LastName != 'Johnson'");
        addFilter(LAST_NAME_COLUMN, "Does Not Equal", "Johnson");
        assertTextPresent("Janeson");
        assertTextNotPresent("Johnson");

        log("** Remove filter");
        removeFilter(LAST_NAME_COLUMN);
        assertTextPresent("Johnson");
        assertTextPresent("Norbertson");

        log("** Add sort by Age");
        assertTextBefore("Billson", "Johnson");
        addSort("Age", "Ascending");
        assertTextBefore("Johnson", "Billson");

        log("** Remove sort");
        removeSort("Age");
        assertTextBefore("Billson", "Johnson");

        log("** Set column title and SUM aggregate");
        assertTextNotPresent("Oldness Factor");
        setColumnProperties("Age", "Oldness Factor" + INJECT_CHARS_2, "SUM");
        assertTextPresent("Oldness Factor" + INJECT_CHARS_2);
        assertTextPresent("Total:");
        assertTextPresent("279");

        log("** Clear column title and SUM aggregate");
        setColumnProperties("Age", null, null);
        assertTextNotPresent("Oldness Factor");
        assertTextNotPresent("Total:");

        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.saveCustomView(this, "Saved-" + INJECT_CHARS_1);

        // TODO: pin, unpin, move columns/filters/sort, remove single filter clause, save named view, revert, click "Revert|Edit|Save" links,
        saveFilterTest();


        log("** Test HTML/JavaScript escaping");
        Crawler.tryInject(this, new Function<Void, Void>() {
            @Override
            public Void apply(Void v)
            {
                CustomizeViewsHelper.openCustomizeViewPanel(CustomizeViewTest.this);
                CustomizeViewsHelper.saveCustomView(CustomizeViewTest.this, "EVIL: " + Crawler.injectString);
                assertTextBefore("Billson", "Johnson");
                return null;
            }
        }, null);
    }

    //Issue 12577: Save link in view/filter bar doesn't work
    //Issue 12103: Report names appear in random order on Views menu
    private void saveFilterTest()
    {
        String fieldKey = LAST_NAME_COLUMN;
        String op = "Starts With";
        String value = "J";
        String[] viewNames = {TRICKY_CHARACTERS + "view", "AAC", "aaa", "aad", "zzz"};

        for(String name : viewNames)
        {
            CustomizeViewsHelper.openCustomizeViewPanel(this);
            CustomizeViewsHelper.addCustomizeViewFilter(this, new String[] { fieldKey }, fieldKey, op, value);
            CustomizeViewsHelper.saveCustomView(this, name);
        }

        clickMenuButton("Views", "default");
        clickButton("Views", 0);
        assertTextPresentInThisOrder("default", viewNames[0], viewNames[2], viewNames[1], viewNames[3], viewNames[4]);
    }

    private void createList()
    {
        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);

        StringBuilder data = new StringBuilder();
        data.append(LIST_KEY_NAME).append("\t");
        for (int i = 0; i < LIST_COLUMNS.length; i++)
        {
            data.append(LIST_COLUMNS[i].getName());
            data.append(i < LIST_COLUMNS.length - 1 ? "\t" : "\n");
        }
        for (String[] rowData : TEST_DATA)
        {
            for (int col = 0; col < rowData.length; col++)
            {
                data.append(rowData[col]);
                data.append(col < rowData.length - 1 ? "\t" : "\n");
            }
        }

        ListHelper.clickImportData(this);
        ListHelper.submitTsvData(this, data.toString());
        for (String[] rowData : TEST_DATA)
        {
            // check that all the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < rowData.length; col++)
            {
                assertTextPresent(rowData[col]);
            }
        }
    }

    void setColumns(String... fieldKeys)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.clearCustomizeViewColumns(this);
        for (String fieldKey : fieldKeys)
            CustomizeViewsHelper.addCustomizeViewColumn(this, new String[] { fieldKey });
        CustomizeViewsHelper.applyCustomView(this);
    }

    void addFilter(String fieldKey, String op, String value)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewFilter(this, new String[] { fieldKey }, fieldKey, op, value);
        CustomizeViewsHelper.applyCustomView(this);
    }

    void addSort(String fieldKey, String order)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewSort(this, new String[] { fieldKey }, fieldKey, order);
        CustomizeViewsHelper.applyCustomView(this);
    }

    void removeFilter(String fieldKey)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewFilter(this, fieldKey);
        CustomizeViewsHelper.applyCustomView(this);
    }

    void removeSort(String fieldKey)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewSort(this, fieldKey);
        CustomizeViewsHelper.applyCustomView(this);
    }

    void setColumnProperties(String fieldKey, String columnTitle, String aggregate)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.setColumnProperties(this, fieldKey, columnTitle, aggregate);
        CustomizeViewsHelper.applyCustomView(this);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
