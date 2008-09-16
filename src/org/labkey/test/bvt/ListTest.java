/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelper.ListColumn;
import org.labkey.test.util.ListHelper.LookupInfo;

/**
 * User: ulberge
 * Date: Jul 13, 2007
 */
public class ListTest extends BaseSeleniumWebTest
{
    private final static String PROJECT_NAME = "ListVerifyProject";
    private final static String PROJECT_NAME2 = "OtherListVerifyProject";
    private final static String LIST_NAME = "Colors";
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.String;
    private final static String LIST_KEY_NAME = "Key";
    private final static String LIST_KEY_NAME2 = "Color";
    private final static String LIST_DESCRIPTION = "A list of colors and what they are like";
    private final static String FAKE_COL1_NAME = "FakeName";
    private ListColumn _listCol1 = new ListColumn(FAKE_COL1_NAME, FAKE_COL1_NAME, ListHelper.ListColumnType.String, "What the color is like");
    private final ListColumn _listCol2 = new ListColumn("Month", "Month to Wear", ListHelper.ListColumnType.DateTime, "When to wear the color", "M");
    private final ListColumn _listCol3 = new ListColumn("Good", "Quality", ListHelper.ListColumnType.Integer, "How nice the color is");
    private final static String[][] TEST_DATA = { { "Blue", "Green", "Red", "Yellow" },
            { "Zany", "Robust", "Mellow", "Light"},
            { "1", "4", "3", "2" },
            { "10", "9", "8", "7"} };
    private final static String[] CONVERTED_MONTHS = { "2000-01-01", "2000-04-04", "2000-03-03", "2000-02-02" };
    private final static String LIST_ROW1 = TEST_DATA[0][0] + "\t" + TEST_DATA[1][0] + "\t" + CONVERTED_MONTHS[0];
    private final static String LIST_ROW2 = TEST_DATA[0][1] + "\t" + TEST_DATA[1][1] + "\t" + CONVERTED_MONTHS[1];
    private final static String LIST_ROW3 = TEST_DATA[0][2] + "\t" + TEST_DATA[1][2] + "\t" + CONVERTED_MONTHS[2];
    private final String LIST_DATA = LIST_KEY_NAME2 + "\t" + FAKE_COL1_NAME +
            "\t" + _listCol2.getName() + "\n" + LIST_ROW1 + "\n" + LIST_ROW2 + "\n" + LIST_ROW3;
    private final String LIST_DATA2 = 
            LIST_KEY_NAME2 + "\t" + _listCol3.getName() + "\n" +
            TEST_DATA[0][0] + "\t" + TEST_DATA[3][0] + "\n" +
            TEST_DATA[0][1] + "\t" + TEST_DATA[3][1] + "\n" +
            TEST_DATA[0][2] + "\t" + TEST_DATA[3][2];
    private final String TEST_FAIL2 = LIST_KEY_NAME2 + "\t" + FAKE_COL1_NAME + "\t" + _listCol2.getName() + "\n" +
            LIST_ROW1 + "\t" + "String";
    private final static String TEST_FAIL = "testfail";
    private final static String TEST_VIEW = "list_view";
    private final static String LIST2_NAME = "Cars";
    private final static ListHelper.ListColumnType LIST2_KEY_TYPE = ListHelper.ListColumnType.String;
    private final static String LIST2_KEY_NAME = "Car";

    private final ListColumn _list2Col1 = new ListColumn(LIST_KEY_NAME2, LIST_KEY_NAME2, LIST2_KEY_TYPE, "The color of the car", new LookupInfo(null, "lists", LIST_NAME));
    private final static String LIST2_KEY = "Car1";
    private final static String LIST2_FOREIGN_KEY = "Blue";
    private final static String LIST2_KEY2 = "Car2";
    private final static String LIST2_FOREIGN_KEY2 = "Green";
    private final static String LIST2_FOREIGN_KEY_OUTSIDE = "Guy";
    private final static String LIST2_KEY3 = "Car3";
    private final static String LIST2_FOREIGN_KEY3 = "Red";
    private final static String LIST2_KEY4 = "Car4";
    private final static String LIST2_FOREIGN_KEY4 = "Brown";
    private final static String LIST3_NAME = "Owners";
    private final static ListHelper.ListColumnType LIST3_KEY_TYPE = ListHelper.ListColumnType.String;
    private final static String LIST3_KEY_NAME = "Owner";
    private final ListColumn _list3Col2 = new ListColumn("Wealth", "Wealth", ListHelper.ListColumnType.String, "");
    private final ListColumn _list3Col1 = new ListColumn(LIST3_KEY_NAME, LIST3_KEY_NAME, LIST3_KEY_TYPE, "Who owns the car", new LookupInfo("/" + PROJECT_NAME2, "lists", LIST3_NAME));
    private final static String LIST3_COL2 = "Rich";
    private final String LIST2_DATA = LIST2_KEY_NAME + "\t" + _list2Col1.getName()  + "\t" + LIST3_KEY_NAME
            + "\n" + LIST2_KEY + "\t" + LIST2_FOREIGN_KEY + "\n" + LIST2_KEY2  + "\t" + LIST2_FOREIGN_KEY2 + "\t" +
            LIST2_FOREIGN_KEY_OUTSIDE + "\n" + LIST2_KEY3  + "\t" + LIST2_FOREIGN_KEY3 + "\n" + LIST2_KEY4  + "\t" +
            LIST2_FOREIGN_KEY4;
    private final String LIST3_DATA = LIST3_KEY_NAME + "\t" + _list3Col2.getName() + "\n" + LIST2_FOREIGN_KEY_OUTSIDE + "\t" +
            LIST3_COL2;
    public static final String LIST_AUDIT_EVENT = "List events";

    public String getAssociatedModuleDirectory()
    {
        return "";
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME2); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        log("Setup project and list module");
        createProject(PROJECT_NAME);

        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, _listCol1, _listCol2);

        log("Add description and test edit");
        clickLinkWithText("edit design");
        setFormElement("ff_description", LIST_DESCRIPTION);
        setFormElement("ff_keyName", LIST_KEY_NAME2);
        clickNavButton("Update");

        log("Check that edit list definition worked");
        assertTextPresent(LIST_KEY_NAME2);
        assertTextPresent(LIST_DESCRIPTION);

        log("Test upload data");
        clickLinkWithText("import data");
        submit();
        assertTextPresent("Form contains no data");
        setFormElement("ff_data", TEST_FAIL);
        submit();
        assertTextPresent(TEST_FAIL);
        assertTextPresent("could not be matched to an existing field");
        setFormElement("ff_data", TEST_FAIL2);
        submit();
        assertTextPresent("could not be matched to an existing field");
        setFormElement("ff_data", LIST_DATA);
        submit();

        log("Check upload worked correctly");
        assertTextPresent(_listCol2.getLabel());
        assertTextPresent(TEST_DATA[0][0]);
        assertTextPresent(TEST_DATA[1][1]);
        assertTextPresent(TEST_DATA[2][2]);

        log("Test edit and adding new field with imported data present");
        clickLinkWithText("Lists");
        clickLinkWithText("view design");
        clickLinkWithText("edit fields");
        waitForElement(Locator.id("button_Add Field"), WAIT_FOR_GWT);
        _listCol1 = new ListColumn("Desc", "Description", ListHelper.ListColumnType.String, "What the color is like");
        setFormElement(Locator.id("ff_name0"), _listCol1.getName());
        setFormElement(Locator.id("ff_label0"), _listCol1.getLabel());
        clickNavButton("Add Field", 0);
        setFormElement(Locator.id("ff_name2"), _listCol3.getName());
        setFormElement(Locator.id("ff_label2"), _listCol3.getLabel());
        selectOptionByText("ff_type2", _listCol3.getType().toString());
        setFormElement(Locator.id("propertyDescription"), _listCol3.getDescription());
        clickNavButton("Save", 0);
        waitForPageToLoad();

        log("Check new field was added correctly");
        assertTextPresent(_listCol3.getName());

        log("Set title field of 'Colors' to 'Desc'");
        clickLinkWithText("edit design");
        selectOptionByValue("ff_titleColumn", "Desc");
        submit();

        clickLinkWithText("view data");
        assertTextPresent(TEST_DATA[0][0]);
        assertTextPresent(TEST_DATA[1][1]);
        assertTextPresent(TEST_DATA[2][2]);

        log("Add data to existing rows");
        clickLinkWithText("Import Data");
        setFormElement("ff_data", LIST_DATA2);
        submit();

        log("Check that data was added correctly");
        assertTextPresent(TEST_DATA[0][0]);
        assertTextPresent(TEST_DATA[1][1]);
        assertTextPresent(TEST_DATA[2][2]);
        assertTextPresent(TEST_DATA[3][0]);
        assertTextPresent(TEST_DATA[3][1]);
        assertTextPresent(TEST_DATA[3][2]);

        log("Test inserting new row");
        clickNavButton("Insert New");
        assertTextPresent(_listCol1.getDescription(), 1, true);     // Field descriptions appear pop-ups -- need to check HTML source
        assertTextPresent(_listCol3.getDescription(), 1, true);
        setFormElement("quf_" + _listCol1.getName(), TEST_DATA[1][3]);
        setFormElement("quf_" + _listCol2.getName(), "wrong type");
        setFormElement("quf_" + _listCol3.getName(), TEST_DATA[3][3]);
        submit();
        assertTextPresent("This field is required");
        setFormElement("quf_" + LIST_KEY_NAME2, TEST_DATA[0][3]);
        submit();
        assertTextPresent("Could not convert");
        setFormElement("quf_" + _listCol2.getName(), CONVERTED_MONTHS[3]);
        submit();

        log("Check new row was added");
        assertTextPresent(TEST_DATA[0][3]);
        assertTextPresent(TEST_DATA[1][3]);
        assertTextPresent(TEST_DATA[2][3]);
        assertTextPresent(TEST_DATA[3][3]);

        log("Test Sort and Filter in Data View");
        setSort("query", _listCol1.getName(), SortDirection.ASC);
        assertTextBefore(TEST_DATA[0][1], TEST_DATA[0][0]);
        setFilter("query", _listCol3.getName(), "Is Greater Than", "7");
        assertTextNotPresent(TEST_DATA[0][3]);

        log("Test Customize View");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);

        removeCustomizeViewColumn(_listCol3.getLabel());
        removeCustomizeViewFilter(_listCol3.getLabel());
        addCustomizeViewFilter(_listCol3.getName(), _listCol3.getLabel(), "Is Less Than", "10");
        removeCustomizeViewSort(_listCol1.getLabel());
        addCustomizeViewSort(_listCol2.getName(), _listCol2.getLabel(), "ASC");
        setFormElement("ff_columnListName", TEST_VIEW);
        clickNavButton("Save");
        
        log("Check Customize View worked");
        assertTextPresent(TEST_DATA[0][3]);
        assertTextBefore(TEST_DATA[0][0], TEST_DATA[0][1]);
        assertTextBefore(TEST_DATA[0][2], TEST_DATA[0][1]);
        assertTextNotPresent(TEST_DATA[0][0]);
        assertTextNotPresent(_listCol3.getLabel());

        log("4725: Check Customize View can't remove all fields");
        pushLocation();
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        removeCustomizeViewColumn(LIST_KEY_NAME2);
        removeCustomizeViewColumn(_listCol1.getLabel());
        removeCustomizeViewColumn(_listCol2.getLabel());
        clickNavButton("Save", 0);
        assertAlert("You must select at least one field to display in the grid.");
        popLocation();

        log("Test Export");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        waitForElement(Locator.navButton("Export"), WAIT_FOR_GWT);
        clickNavButton("Export", 0);
        clickLinkContainingText("Export All to Text");
        assertTextPresent(TEST_DATA[0][3]);
        assertTextBefore(TEST_DATA[0][0], TEST_DATA[0][1]);
        assertTextBefore(TEST_DATA[0][2], TEST_DATA[0][1]);
        assertTextNotPresent(TEST_DATA[0][0]);
        assertTextNotPresent(_listCol3.getLabel());
        popLocation();

        log("Filter Test");
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Query");
        selectOptionByText("schemaName", "lists");
        selenium.click("document.frmCustomize.selectQuery[1]");
        submit();
        addWebPart("Query");
        selectOptionByText("schemaName", "lists");
        selenium.click("document.frmCustomize.selectQuery[1]");
        submit();

        log("Test that the right filters are present for each type");
        runMenuItemHandler("qwp3:" + _listCol3.getName() + ":filter");
        assertTrue(!isElementPresent(Locator.raw("//option[@value='startswith']")));
        assertTrue(isElementPresent(Locator.raw("//option[@value='isblank']")));
        clickImgButtonNoNav("Cancel");

        log("Test that filters don't affect multiple web parts");
        assertTextPresent(TEST_DATA[1][0], 2);
        setFilter("qwp3", _listCol3.getName(), "Is Less Than", "10");
        assertTextPresent(TEST_DATA[1][0], 1);

        log("Test that sort only affects one web part");
        setSort("qwp2", _listCol3.getName(), SortDirection.ASC);
        String source = selenium.getHtmlSource();
        int index;
        assertTrue(source.indexOf(TEST_DATA[1][2]) < (index = source.indexOf(TEST_DATA[1][1])) &&
                source.indexOf(TEST_DATA[1][1], index) < source.indexOf(TEST_DATA[1][2], index));

        log("Create second project");
        createProject(PROJECT_NAME2);

        log("Add List");
        ListHelper.createList(this, PROJECT_NAME2, LIST3_NAME, LIST3_KEY_TYPE, LIST3_KEY_NAME, _list3Col2);

        log("Upload data to second list");
        ListHelper.uploadData(this, PROJECT_NAME2, LIST3_NAME, LIST3_DATA);

        log("Navigate back to first project");
        log("Add second list");
        ListHelper.createList(this, PROJECT_NAME, LIST2_NAME, LIST2_KEY_TYPE, LIST2_KEY_NAME, _list2Col1, _list3Col1);

        log("Upload data to second list");
        ListHelper.uploadData(this, PROJECT_NAME, LIST2_NAME, LIST2_DATA);

        log("Check that upload worked");
        assertTextPresent(LIST2_KEY);
        assertTextPresent(LIST2_KEY2);
        assertTextPresent(LIST2_KEY3);
        assertTextPresent(LIST2_KEY4);

        log("Check that reference worked");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        click(Locator.id("expand_Color"));
        addCustomizeViewColumn(_list2Col1.getName() + "/" +  _listCol1.getName(), _list2Col1.getLabel() + " " +  _listCol1.getLabel());
        addCustomizeViewColumn(_list2Col1.getName() + "/" +  _listCol2.getName(), _list2Col1.getLabel() + " " +  _listCol2.getLabel());
        addCustomizeViewColumn(_list2Col1.getName() + "/" +  _listCol3.getName(), _list2Col1.getLabel() + " " + _listCol3.getLabel());
        addCustomizeViewFilter(_list2Col1.getName() + "/" +  _listCol3.getName(), _list2Col1.getLabel() + " " + _listCol3.getLabel(), "Is Less Than", "10");
        addCustomizeViewSort(_list2Col1.getName() + "/" +  _listCol3.getName(), _list2Col1.getLabel() + " " + _listCol3.getLabel(), "ASC");
        click(Locator.id("expand_Owner"));
        addCustomizeViewColumn(_list3Col1.getName() + "/" +  _list3Col1.getName(), _list3Col1.getLabel() + " " +  _list3Col1.getLabel());
        addCustomizeViewColumn(_list3Col1.getName() + "/" +  _list3Col2.getName(), _list3Col1.getLabel() + " " +  _list3Col2.getLabel());
        setFormElement("ff_columnListName", TEST_VIEW);
        clickNavButton("Save");

        log("Check adding referenced fields worked");
        waitForText(_listCol1.getLabel(), WAIT_FOR_GWT);
        assertTextPresent(_listCol1.getLabel());
        assertTextPresent(_listCol2.getLabel());
        assertTextPresent(_listCol3.getLabel());
        assertTextPresent(LIST2_FOREIGN_KEY_OUTSIDE);
        assertTextPresent(LIST3_COL2);
        assertTextNotPresent(LIST2_KEY);
        assertTextBefore(LIST2_KEY3, LIST2_KEY2);
        assertTextNotPresent(LIST2_KEY4);

        log("Test export");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        waitForElement(Locator.navButton("Export"), WAIT_FOR_GWT);
        clickNavButton("Export", 0);
        clickLinkContainingText("Export All to Text");
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol1.getName());
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol2.getName());
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol3.getName());
        assertTextPresent(LIST2_FOREIGN_KEY_OUTSIDE);
        assertTextPresent(LIST3_COL2);
        assertTextNotPresent(LIST2_KEY);
        assertTextBefore(LIST2_KEY3, LIST2_KEY2);
        assertTextNotPresent(LIST2_KEY4);
        popLocation();

        log("Test edit row");
        if (getTableCellText("dataregion_query", 1, 3).compareTo(LIST2_KEY3) != 0)
            clickLinkWithText(LIST2_KEY_NAME);
        clickLinkWithText("edit");
        selectOptionByText("quf_Color", TEST_DATA[1][1]);
        selectOptionByText("quf_Owner", LIST2_FOREIGN_KEY_OUTSIDE);
        submit();

        clickMenuButton("Views", "Views:default");
        waitForPageToLoad();
        assertTextPresent(TEST_DATA[1][1], 2);

        log("Test deleting rows");
        checkCheckbox(".toggle");
        click(Locator.raw("//input[@value='Delete']"));
        waitForPageToLoad();
        assertTextNotPresent(LIST2_KEY);
        assertTextNotPresent(LIST2_KEY2);
        assertTextNotPresent(LIST2_KEY3);
        assertTextNotPresent(LIST2_KEY4);

        log("Test deleting data");
        clickLinkWithText("Lists");
        click(Locator.raw("//td[contains(text(), '" + LIST_NAME + "')]/../td[3]/a"));
        waitForPageToLoad();
        clickLinkWithText("delete list");
        clickNavButton("OK");

        log("Test that deletion happened");
        assertTextNotPresent(LIST_NAME);
        clickLinkWithText("view data");
        pushLocation();
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        assertElementNotPresent(Locator.id("expand_" + LIST_KEY_NAME2));
        assertElementPresent(Locator.id("expand_" + LIST3_KEY_NAME));
        popLocation();
        clickLinkWithText(PROJECT_NAME);
        assertTextPresent("Query '" + LIST_NAME + "' has errors");

        log("Test exporting a nonexistent list returns a 404");
        selenium.open(WebTestHelper.getBaseURL() + "/query/" + PROJECT_NAME + "/exportRowsTsv.view?schemaName=lists&query.queryName=" + LIST_NAME);
        assertEquals(getResponseCode(), 404);
        assertTextPresent("Query '" + LIST_NAME + "' in schema 'lists' doesn't exist.");

        clickNavButton("Folder");
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "The domain Colors was deleted", 5);
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "An existing list record was deleted", 5);
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "An existing list record was modified", 10);
    }
}
