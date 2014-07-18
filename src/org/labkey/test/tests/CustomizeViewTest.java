/*
 * Copyright (c) 2010-2014 LabKey Corporation
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.ListHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({DailyB.class})
public class CustomizeViewTest extends BaseWebDriverTest
{
    public static final String PROJECT_NAME = "CustomizeViewTest";
    public static final String LIST_NAME = "People" + INJECT_CHARS_1;
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";
    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";

    private final static String LAST_NAME_COLUMN = "LastName" + INJECT_CHARS_2;
    private final static String FIRST_NAME = "FirstName";
    private final static ListHelper.ListColumn[] LIST_COLUMNS = new ListHelper.ListColumn[]
            {
                    new ListHelper.ListColumn(FIRST_NAME, FIRST_NAME + INJECT_CHARS_1, ListHelper.ListColumnType.String, "The first name"),
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

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        createList();

        saveAfterApplyingView(null, "CreatedBy", "Created By");
        saveAfterApplyingView("New View", "ModifiedBy", "Modified By");

        log("** Show only LastName and Age");
        setColumns(LAST_NAME_COLUMN, "Age");
        assertTextPresent("Norbertson");
        assertTextNotPresent("First Name");

        log("test js injection attack (Issue 14103) ");
        addFilter(FIRST_NAME, "Starts With", "K");
        removeFilter(FIRST_NAME);

        log("** Add filter: LastName starts with 'J'");
        addFilter(LAST_NAME_COLUMN, "Starts With", "J");
        assertTextNotPresent("Norbertson");
        assertTextPresent("Janeson");
        assertTextPresent("Johnson");

        log("** Add another filter: LastName != 'Johnson'");
        addFilter(LAST_NAME_COLUMN, "Does Not Equal", "Johnson");
        assertTextPresent("Janeson");
        assertElementNotPresent(Locator.tagContainingText("td", "Johnson"));

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

        List<Map<String, String>> aggregates = new ArrayList<>();
        aggregates.add(new HashMap<String, String>(){{put("type", "SUM");}});
        aggregates.add(new HashMap<String, String>(){{put("type", "COUNT");}});
        setColumnProperties("Age", "Oldness Factor" + INJECT_CHARS_2, aggregates);
        assertTextPresent("Oldness Factor" + INJECT_CHARS_2);
        assertTextPresent("Total:");
        assertTextPresent("Count:");
        assertTextNotPresent("Total Age:");
        assertTextPresent("279");

        log("** Set custom aggregate label");
        aggregates.remove(0);
        aggregates.add(new HashMap<String, String>(){{put("type", "SUM");put("label", "Total Age");}});
        setColumnProperties("Age", "Oldness Factor" + INJECT_CHARS_2, aggregates);
        assertTextPresent("Oldness Factor" + INJECT_CHARS_2);

        assertTextNotPresent("Total:");
        assertTextPresent("Total Age:");

        log("** Clear column title and SUM aggregate");
        setColumnProperties("Age", null, null);
        assertTextNotPresent("Oldness Factor");
        assertTextNotPresent("Total Age:");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.saveCustomView("Saved-" + INJECT_CHARS_1);

        // TODO: pin, unpin, move columns/filters/sort, remove single filter clause
        saveFilterTest();
//        saveFilter


        log("** Test HTML/JavaScript escaping");
        Crawler.tryInject(this, new Function<Void, Void>() {
            @Override
            public Void apply(Void v)
            {
                _customizeViewsHelper.openCustomizeViewPanel();
                _customizeViewsHelper.saveCustomView("EVIL: " + Crawler.injectString);
                assertTextBefore("Billson", "Johnson");
                return null;
            }
        }, null);
    }

    //Issue 13099: Unable to save custom view after applying view
    private void saveAfterApplyingView(String name, String newColumnLabel, String newColumnDisplayName)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn(newColumnLabel);
        _customizeViewsHelper.applyCustomView();
        assertTextPresent(newColumnDisplayName);
        assertTextPresent("unsaved");

        _customizeViewsHelper.revertUnsavedViewGridClosed();
        assertTextNotPresent(newColumnDisplayName);

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn(newColumnLabel);
        _customizeViewsHelper.applyCustomView();
        _customizeViewsHelper.saveUnsavedViewGridClosed(name);
        assertTextNotPresent("unsaved");
        assertTextPresent(newColumnDisplayName);
//        assertTextPresent(PasswordUtil.getUsername(),8);
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
            _customizeViewsHelper.openCustomizeViewPanel();
            _customizeViewsHelper.addCustomizeViewFilter(new String[] { fieldKey }, fieldKey, op, value);
            _customizeViewsHelper.saveCustomView(name);
        }

        _extHelper.clickMenuButton("Views", "default");
        clickButton("Views", 0);
        assertTextPresentInThisOrder("default", viewNames[0], viewNames[2], viewNames[1], viewNames[3], viewNames[4]);
    }

    private void createList()
    {
        _listHelper.createList(PROJECT_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);

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

        _listHelper.clickImportData();
        _listHelper.submitTsvData(data.toString());
        for (String[] rowData : TEST_DATA)
        {
            // check that all the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < rowData.length; col++)
            {
                waitForText(rowData[col]);
            }
        }
    }

    void setColumns(String... fieldKeys)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.clearCustomizeViewColumns();
        for (String fieldKey : fieldKeys)
            _customizeViewsHelper.addCustomizeViewColumn(new String[] { fieldKey });
        _customizeViewsHelper.applyCustomView();
    }

    void addFilter(String fieldKey, String op, String value)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewFilter(new String[] { fieldKey }, fieldKey, op, value);
        _customizeViewsHelper.applyCustomView();
    }

    void addSort(String fieldKey, String order)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewSort(new String[] { fieldKey }, fieldKey, order);
        _customizeViewsHelper.applyCustomView();
    }

    void removeFilter(String fieldKey)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewFilter(fieldKey);
        _customizeViewsHelper.applyCustomView();
    }

    void removeSort(String fieldKey)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewSort(fieldKey);
        _customizeViewsHelper.applyCustomView();
    }

    void setColumnProperties(String fieldKey, String columnTitle, List<Map<String, String>> aggregates)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.setColumnProperties(fieldKey, columnTitle, aggregates);
        _customizeViewsHelper.applyCustomView();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
