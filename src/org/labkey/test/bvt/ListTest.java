/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
import static org.labkey.test.util.ListHelper.ListColumnType.*;

import java.io.File;
import java.util.List;
import java.util.Arrays;

/**
 * User: ulberge
 * Date: Jul 13, 2007
 */
public class ListTest extends BaseSeleniumWebTest
{
    protected final static String PROJECT_NAME = "ListVerifyProject";
    private final static String PROJECT_NAME2 = "OtherListVerifyProject";
    private final static String LIST_NAME = "Colors";
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.String;
    private final static String LIST_KEY_NAME = "Key";
    private final static String LIST_KEY_NAME2 = "Color";
    private final static String LIST_DESCRIPTION = "A list of colors and what they are like";
    private final static String FAKE_COL1_NAME = "FakeName";
    private final static String ALIASED_KEY_NAME = "Material";
    private final static String HIDDEN_TEXT = "Hidden";
    private ListColumn _listCol1 = new ListColumn(FAKE_COL1_NAME, FAKE_COL1_NAME, ListHelper.ListColumnType.String, "What the color is like");
    private final ListColumn _listCol2 = new ListColumn("Month", "Month to Wear", ListHelper.ListColumnType.DateTime, "When to wear the color", "M");
    private final ListColumn _listCol3 = new ListColumn("JewelTone", "Jewel Tone", ListHelper.ListColumnType.Boolean, "Am I a jewel tone?");
    private final ListColumn _listCol4 = new ListColumn("Good", "Quality", ListHelper.ListColumnType.Integer, "How nice the color is");
    private final ListColumn _listCol5 = new ListColumn("HiddenColumn", HIDDEN_TEXT, ListHelper.ListColumnType.String, "I should be hidden!");
    private final ListColumn _listCol6 = new ListColumn("AliasedColumn", "Element", ListHelper.ListColumnType.String, "I show aliased data.");
    private final static String[][] TEST_DATA = {
            { "Blue", "Green", "Red", "Yellow" },
            { "Zany", "Robust", "Mellow", "Light"},
            { "true", "false", "true", "false"},
            { "1", "4", "3", "2" },
            { "10", "9", "8", "7"},
            { "Water", "Earth", "Fire", "Air"}};
    private final static String[] CONVERTED_MONTHS = { "2000-01-01", "2000-04-04", "2000-03-03", "2000-02-02" };
    private final static String LIST_ROW1 = TEST_DATA[0][0] + "\t" + TEST_DATA[1][0] + "\t" + TEST_DATA[2][0] + "\t" + CONVERTED_MONTHS[0];
    private final static String LIST_ROW2 = TEST_DATA[0][1] + "\t" + TEST_DATA[1][1] + "\t" + TEST_DATA[2][1] + "\t" + CONVERTED_MONTHS[1];
    private final static String LIST_ROW3 = TEST_DATA[0][2] + "\t" + TEST_DATA[1][2] + "\t" + TEST_DATA[2][2] + "\t" + CONVERTED_MONTHS[2];
    private final String LIST_DATA = LIST_KEY_NAME2 + "\t" + FAKE_COL1_NAME +
            "\t" + _listCol3.getName() + "\t" + _listCol2.getName() + "\n" + LIST_ROW1 + "\n" + LIST_ROW2 + "\n" + LIST_ROW3;
    private final String LIST_DATA2 = 
            LIST_KEY_NAME2 + "\t" + _listCol4.getName() + "\t" + ALIASED_KEY_NAME + "\t" + _listCol5.getName() + "\n" +
            TEST_DATA[0][0] + "\t" + TEST_DATA[4][0] + "\t" + TEST_DATA[5][0] + "\t" + HIDDEN_TEXT + "\n" +
            TEST_DATA[0][1] + "\t" + TEST_DATA[4][1] + "\t" + TEST_DATA[5][1] + "\t" + HIDDEN_TEXT + "\n" +
            TEST_DATA[0][2] + "\t" + TEST_DATA[4][2] + "\t" + TEST_DATA[5][2] + "\t" + HIDDEN_TEXT;
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

    private final String EXCEL_DATA_FILE = getLabKeyRoot() + "/sampledata/dataLoading/excel/fruits.xls";
    private final String TSV_DATA_FILE = getLabKeyRoot() + "/sampledata/dataLoading/excel/fruits.tsv";

    public String getAssociatedModuleDirectory()
    {
        return "list";
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME2); } catch (Throwable t) {}
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doTestSteps()
    {
        log("Setup project and list module");
        createProject(PROJECT_NAME);

//        customizeURLTest();
//        fail();

        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, _listCol1, _listCol2, _listCol3);

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
        assertTextPresent(TEST_DATA[3][2]);
        assertTableCellTextEquals("dataregion_query", 1, 6, "true");
        assertTableCellTextEquals("dataregion_query", 2, 6, "false");
        assertTableCellTextEquals("dataregion_query", 3, 6, "true");

        log("Test check/uncheck of checkboxes");
        // Second row (Green)
        clickLinkWithText("edit", 1);
        setFormElement("quf_" + _listCol2.getName(), CONVERTED_MONTHS[1]);  // Has a funny format -- need to post converted date
        checkCheckbox("quf_JewelTone");
        submit();
        // Third row (Red)
        clickLinkWithText("edit", 2);
        setFormElement("quf_" + _listCol2.getName(), CONVERTED_MONTHS[2]);  // Has a funny format -- need to post converted date
        uncheckCheckbox("quf_JewelTone");
        submit();
        assertTableCellTextEquals("dataregion_query", 1, 6, "true");
        assertTableCellTextEquals("dataregion_query", 2, 6, "true");
        assertTableCellTextEquals("dataregion_query", 3, 6, "false");

        log("Test edit and adding new field with imported data present");
        clickLinkWithText("Lists");
        clickLinkWithText("view design");
        clickLinkWithText("edit fields");
        waitForElement(Locator.id("button_Add Field"), WAIT_FOR_GWT);
        _listCol1 = new ListColumn("Desc", "Description", ListHelper.ListColumnType.String, "What the color is like");
        setFormElement(Locator.id("ff_name0"), _listCol1.getName());
        setFormElement(Locator.id("ff_label0"), _listCol1.getLabel());
        clickNavButton("Add Field", 0);
        setFormElement(Locator.id("ff_name3"), _listCol4.getName());
        setFormElement(Locator.id("ff_label3"), _listCol4.getLabel());
        selectOptionByText("ff_type3", _listCol4.getType().toString());
        setFormElement(Locator.id("propertyDescription"), _listCol4.getDescription());

        // Create "Hidden Field" and remove from all views.
        clickNavButton("Add Field", 0);
        setFormElement(Locator.id("ff_name4"), _listCol5.getName());
        setFormElement(Locator.id("ff_label4"), _listCol5.getLabel());
        selectOptionByText("ff_type4", _listCol5.getType().toString());
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInGrid']/input"));
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInInsert']/input"));
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInUpdate']/input"));
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInDetail']/input"));

        clickNavButton("Add Field", 0);
        setFormElement(Locator.id("ff_name5"), _listCol6.getName());
        setFormElement(Locator.id("ff_label5"), _listCol6.getLabel());
        selectOptionByText("ff_type5", _listCol6.getType().toString());
        setFormElement(Locator.id("importAliases"), ALIASED_KEY_NAME);

        mouseClick(Locator.id("partdown_1").toString());

        clickNavButton("Save", 0);
        waitForPageToLoad();

        log("Check new field was added correctly");
        assertTextPresent(_listCol4.getName());

        log("Set title field of 'Colors' to 'Desc'");
        clickLinkWithText("edit design");
        selectOptionByValue("ff_titleColumn", "Desc");
        submit();

        clickLinkWithText("view data");
        assertTextPresent(TEST_DATA[0][0]);
        assertTextPresent(TEST_DATA[1][1]);
        assertTextPresent(TEST_DATA[3][2]);
        
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from Grid view.
        assertTableCellTextEquals("dataregion_query", 0, 5, _listCol3.getLabel()); // Colummns...
        assertTableCellTextEquals("dataregion_query", 0, 6, _listCol2.getLabel()); // ...swapped.

        log("Add data to existing rows");
        clickLinkWithText("Import Data");
        setFormElement("ff_data", LIST_DATA2);
        submit();

        log("Check that data was added correctly");
        assertTextPresent(TEST_DATA[0][0]);
        assertTextPresent(TEST_DATA[1][1]);
        assertTextPresent(TEST_DATA[3][2]);
        assertTextPresent(TEST_DATA[4][0]);
        assertTextPresent(TEST_DATA[4][1]);
        assertTextPresent(TEST_DATA[4][2]);
        assertTextPresent(TEST_DATA[5][0]);
        assertTextPresent(TEST_DATA[5][1]);
        assertTextPresent(TEST_DATA[5][2]);

        log("Check that hidden column is hidden.");
        clickLinkWithText("details");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        assertTextBefore(_listCol3.getLabel(), _listCol2.getLabel());
        clickNavButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        assertTextBefore(_listCol3.getLabel(), _listCol2.getLabel());
        clickNavButton("Cancel");
        clickNavButton("Show Grid");

        log("Test inserting new row");
        clickNavButton("Insert New");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        assertTextBefore(_listCol3.getLabel(), _listCol2.getLabel());
        String html = selenium.getHtmlSource();
        assertTrue("Description \"" + _listCol1.getDescription() + "\" not present.", html.contains(_listCol1.getDescription()));
        assertTrue("Description \"" + _listCol3.getDescription() + "\" not present.", html.contains(_listCol3.getDescription()));
        setFormElement("quf_" + _listCol1.getName(), TEST_DATA[1][3]);
        setFormElement("quf_" + _listCol2.getName(), "wrong type");
        // Jewel Tone checkbox is left blank -- we'll make sure it's posted as false below
        setFormElement("quf_" + _listCol4.getName(), TEST_DATA[4][3]);
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
        assertTableCellTextEquals("dataregion_query", 4, 5, "false");

        log("Check hidden field is hidden only where specified.");
        clickNavButton("View Design", 0);
        waitForPageToLoad();
        clickLinkWithText("edit fields");
        waitForElement(Locator.id("ff_name4"), WAIT_FOR_GWT);

        setFormElement(Locator.id("ff_name4"), _listCol5.getName()); // Select Hidden field.
        checkCheckbox(Locator.raw("//span[@id='propertyShownInGrid']/input"));
        mouseClick(Locator.id("partdown_1").toString());
        clickNavButton("Save", 0);
        waitForPageToLoad();

        log("Check that hidden column is hidden.");
        clickLinkWithText("view data");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from grid view.
        clickLinkWithText("details");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickNavButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickNavButton("Cancel");
        clickNavButton("Show Grid");
        clickNavButton("Insert New");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickNavButton("Cancel");

        clickNavButton("View Design", 0);
        waitForPageToLoad();
        clickLinkWithText("edit fields");
        waitForElement(Locator.id("ff_name4"), WAIT_FOR_GWT);

        setFormElement(Locator.id("ff_name4"), _listCol5.getName()); // Select Hidden field.
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInGrid']/input"));
        checkCheckbox(Locator.raw("//span[@id='propertyShownInInsert']/input"));
        clickNavButton("Save", 0);
        waitForPageToLoad();

        clickLinkWithText("view data");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        clickLinkWithText("details");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        clickNavButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        clickNavButton("Cancel");
        clickNavButton("Show Grid");
        clickNavButton("Insert New");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from insert view.
        clickNavButton("Cancel");

        clickNavButton("View Design", 0);
        waitForPageToLoad();
        clickLinkWithText("edit fields");

        setFormElement(Locator.id("ff_name4"), _listCol5.getName()); // Select Hidden field.
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInInsert']/input"));
        checkCheckbox(Locator.raw("//span[@id='propertyShownInUpdate']/input"));
        clickNavButton("Save", 0);
        waitForPageToLoad();

        clickLinkWithText("view data");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        clickLinkWithText("details");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        clickNavButton("Edit");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from update view.
        clickNavButton("Cancel");
        clickNavButton("Show Grid");
        clickNavButton("Insert New");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        clickNavButton("Cancel");

        clickNavButton("View Design", 0);
        waitForPageToLoad();
        clickLinkWithText("edit fields");
        waitForElement(Locator.id("ff_name4"), WAIT_FOR_GWT);

        setFormElement(Locator.id("ff_name4"), _listCol5.getName()); // Select Hidden field.
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInUpdate']/input"));
        checkCheckbox(Locator.raw("//span[@id='propertyShownInDetail']/input"));
        clickNavButton("Save", 0);
        waitForPageToLoad();

        clickLinkWithText("view data");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        clickLinkWithText("details");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from details view.
        clickNavButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        clickNavButton("Cancel");
        clickNavButton("Show Grid");
        clickNavButton("Insert New");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        clickNavButton("Cancel");

        log("Test Sort and Filter in Data View");
        setSort("query", _listCol1.getName(), SortDirection.ASC);
        assertTextBefore(TEST_DATA[0][1], TEST_DATA[0][0]);
        setFilter("query", _listCol4.getName(), "Is Greater Than", "7");
        assertTextNotPresent(TEST_DATA[0][3]);

        log("Test Customize View");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);

        removeCustomizeViewColumn(_listCol4.getLabel());
        removeCustomizeViewFilter(_listCol4.getLabel());
        addCustomizeViewFilter(_listCol4.getName(), _listCol4.getLabel(), "Is Less Than", "10");
        removeCustomizeViewSort(_listCol1.getLabel());
        addCustomizeViewSort(_listCol2.getName(), _listCol2.getLabel(), "ASC");
        setFormElement("ff_columnListName", TEST_VIEW);
        clickNavButton("Save");
        
        log("Check Customize View worked");
        assertTextPresent(TEST_DATA[0][3]);
        assertTextPresentInThisOrder(TEST_DATA[0][3], TEST_DATA[0][2], TEST_DATA[0][1]);
        assertTextNotPresent(TEST_DATA[0][0]);
        assertTextNotPresent(_listCol4.getLabel());

        log("4725: Check Customize View can't remove all fields");
        pushLocation();
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        removeCustomizeViewColumn(LIST_KEY_NAME2);
        removeCustomizeViewColumn(_listCol1.getLabel());
        removeCustomizeViewColumn(_listCol2.getLabel());
        removeCustomizeViewColumn(_listCol3.getLabel());
        removeCustomizeViewColumn(_listCol6.getLabel());
        clickNavButton("Save", 0);
        assertAlert("You must select at least one field to display in the grid.");
        popLocation();

        log("Test Export");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        waitForElement(Locator.navButton("Export"), WAIT_FOR_GWT);
        clickExportToText();
        assertTextPresent(TEST_DATA[0][3]);
        assertTextPresentInThisOrder(TEST_DATA[0][3], TEST_DATA[0][2], TEST_DATA[0][1]);
        assertTextNotPresent(TEST_DATA[0][0]);
        assertTextNotPresent(_listCol4.getLabel());
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
        runMenuItemHandler("qwp3:" + _listCol4.getName() + ":filter");
        assertTrue(!isElementPresent(Locator.raw("//option[@value='startswith']")));
        assertTrue(isElementPresent(Locator.raw("//option[@value='isblank']")));
        clickImgButtonNoNav("Cancel");

        log("Test that filters don't affect multiple web parts");
        assertTextPresent(TEST_DATA[1][0], 2);
        setFilter("qwp3", _listCol4.getName(), "Is Less Than", "10");
        assertTextPresent(TEST_DATA[1][0], 1);

        log("Test that sort only affects one web part");
        setSort("qwp2", _listCol4.getName(), SortDirection.ASC);
        String source = selenium.getHtmlSource();
        int index;
        assertTrue(source.indexOf(TEST_DATA[1][2]) < (index = source.indexOf(TEST_DATA[1][1])) &&
                source.indexOf(TEST_DATA[1][1], index) < source.indexOf(TEST_DATA[1][2], index));

        log("Test single list web part");
        addWebPart("Single List");
        setText("title", "This is my single list web part title");
        submit();
        assertTextPresent("Import Data");
        assertTextPresent("View Design");
        clickLinkContainingText("This is my single list web part title");
        assertTextPresent("Colors");
        assertTextPresent("Views");

        log("Create second project");
        createProject(PROJECT_NAME2);

        log("Add List");
        ListHelper.createList(this, PROJECT_NAME2, LIST3_NAME, LIST3_KEY_TYPE, LIST3_KEY_NAME, _list3Col2);
        assertTextPresent("<AUTO> (Owner)");
        clickLinkWithText("edit design");
        clickButton("Update", defaultWaitForPage);
        assertTextPresent("Owner", 5);  // Title plus two "Owners" and two "Owner"

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
        addCustomizeViewColumn(_list2Col1.getName() + "/" +  _listCol4.getName(), _list2Col1.getLabel() + " " + _listCol4.getLabel());
        addCustomizeViewFilter(_list2Col1.getName() + "/" +  _listCol4.getName(), _list2Col1.getLabel() + " " + _listCol4.getLabel(), "Is Less Than", "10");
        addCustomizeViewSort(_list2Col1.getName() + "/" +  _listCol4.getName(), _list2Col1.getLabel() + " " + _listCol4.getLabel(), "ASC");
        click(Locator.id("expand_Owner"));
        addCustomizeViewColumn(_list3Col1.getName() + "/" +  _list3Col1.getName(), _list3Col1.getLabel() + " " +  _list3Col1.getLabel());
        addCustomizeViewColumn(_list3Col1.getName() + "/" +  _list3Col2.getName(), _list3Col1.getLabel() + " " +  _list3Col2.getLabel());
        setFormElement("ff_columnListName", TEST_VIEW);
        clickNavButton("Save");

        log("Check adding referenced fields worked");
        waitForText(_listCol1.getLabel(), WAIT_FOR_GWT);
        assertTextPresent(_listCol1.getLabel());
        assertTextPresent(_listCol2.getLabel());
        assertTextPresent(_listCol4.getLabel());
        assertTextPresent(LIST2_FOREIGN_KEY_OUTSIDE);
        assertTextPresent(LIST3_COL2);
        assertTextNotPresent(LIST2_KEY);
        assertTextBefore(LIST2_KEY3, LIST2_KEY2);
        assertTextNotPresent(LIST2_KEY4);

        log("Test export");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        waitForElement(Locator.navButton("Export"), WAIT_FOR_GWT);
        clickExportToText();
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol1.getName());
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol2.getName());
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol4.getName());
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
        selenium.chooseOkOnNextConfirmation();
        clickButton("Delete", 0);
        assertEquals(selenium.getConfirmation(), "Are you sure you want to delete the selected rows?");
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
        assertTextPresent("List does not exist");

        log("Test exporting a nonexistent list returns a 404");
        selenium.open(WebTestHelper.getBaseURL() + "/query/" + PROJECT_NAME + "/exportRowsTsv.view?schemaName=lists&query.queryName=" + LIST_NAME);
        assertEquals(getResponseCode(), 404);
        assertTextPresent("Query '" + LIST_NAME + "' in schema 'lists' doesn't exist.");

        clickNavButton("Folder");
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "The domain Colors was deleted", 5);
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "An existing list record was deleted", 5);
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "An existing list record was modified", 10);

        doRenameFieldsTest();
        doUploadTest();
        customizeURLTest();
    }

    private void doUploadTest()
    {
        if (!isFileUploadAvailable())
            return;

        log("Infer from excel file, then import data");
        File excelFile = new File(EXCEL_DATA_FILE);
        ListHelper.createListFromFile(this, PROJECT_NAME, "Fruits from Excel", excelFile);
        assertNoLabkeyErrors();
        assertTextPresent("pomegranate");

        log("Infer from a tsv file, then import data");
        File tsvFile = new File(TSV_DATA_FILE);
        ListHelper.createListFromFile(this, PROJECT_NAME, "Fruits from a TSV", tsvFile);
        assertNoLabkeyErrors();
        assertTextPresent("pomegranate");
    }

    private void doRenameFieldsTest()
    {
        log("8329: Test that renaming a field then creating a new field with the old name doesn't result in awful things");
        ListHelper.createList(this, PROJECT_NAME, "new", ListHelper.ListColumnType.AutoInteger, "key", new ListColumn("BarBar", "BarBar", ListHelper.ListColumnType.String, "Some new column"));
        assertTextPresent("BarBar");
        clickLinkWithText("edit fields");
        waitForElement(Locator.navButton("Save"), WAIT_FOR_GWT);
        setFormElement(Locator.id("ff_name0"), "FooFoo");
        clickNavButton("Save");
        assertTextPresent("FooFoo");
        assertTextNotPresent("BarBar");
        clickLinkWithText("edit fields");
        waitForElement(Locator.id("button_Add Field"), WAIT_FOR_GWT);
        clickNavButton("Add Field", 0);
        setFormElement(Locator.id("ff_name1"), "BarBar");
        clickNavButton("Save");
        assertTextPresent("FooFoo");
        assertTextPresent("BarBar");
        assertTextBefore("FooFoo", "BarBar");
    }



    //
    // CUSTOMIZE URL tests
    //

    ListHelper.ListColumn col(String name, ListHelper.ListColumnType type)
    {
        return new ListHelper.ListColumn(name, "", type, "");
    }

    ListHelper.ListColumn col(String name, ListHelper.ListColumnType type, String table)
    {
        return new ListHelper.ListColumn(name, "", type, "", new ListHelper.LookupInfo(null, "lists", table));
    }
    
    ListHelper.ListColumn colURL(String name, ListHelper.ListColumnType type, String url)
    {
        ListColumn c  = new ListHelper.ListColumn(name, "", type, "");
        c.setURL(url);
        return c;
    }

    List<ListColumn> Acolumns = Arrays.asList(
            col("A", Integer),
            colURL("title", String, "/junit/echoForm.view?key=${A}&title=${title}&table=A"),
            col("Bfk", Integer, "B")
    );
    String[][] Adata = new String[][]
    {
        {"1", "one A", "1"},
    };

    List<ListHelper.ListColumn> Bcolumns = Arrays.asList(
            col("B", Integer),
            colURL("title", String, "org.labkey.core.junit.JunitController$EchoFormAction.class?key=${B}&title=${title}&table=B"),
            col("Cfk", Integer, "C")
    );
    String[][] Bdata = new String[][]
    {
        {"1", "one B", "1"},
    };
    
    List<ListHelper.ListColumn> Ccolumns = Arrays.asList(
            col("C", Integer),
            colURL("title", String, "/junit/echoForm.view?key=${C}&title=${title}&table=C")
    );
    String[][] Cdata = new String[][]
    {
        {"1", "one C"},
    };


    String toTSV(List<ListHelper.ListColumn> cols, String[][] data)
    {
        StringBuilder sb = new StringBuilder();
        String tab = "";
        for (ListHelper.ListColumn c : cols)
        {
            sb.append(tab);
            sb.append(c.getName());
            tab = "\t";
        }
        tab = "\n";
        for (String[] row : data)
        {
            for (String cell : row)
            {
                sb.append(tab);
                sb.append(cell);
                tab = "\t";
            }
            tab = "\n";
        }
        sb.append(tab);
        return sb.toString();
    }


    void createList(String name, List<ListHelper.ListColumn> cols, String[][] data)
    {
        log("Add List");
        ListHelper.createList(this, PROJECT_NAME, name, cols.get(0).getType(), cols.get(0).getName(),
                cols.subList(1,cols.size()).toArray(new ListHelper.ListColumn[cols.size()-1]));
        clickLinkWithText("edit design");
        selectOptionByText("ff_titleColumn", cols.get(1).getName());    // Explicitly set to the PK (auto title will pick wealth column)
        clickButton("Update", defaultWaitForPage);
        clickLinkWithText("import data");
        setFormElement("ff_data", toTSV(cols,data));
        submit();
    }


    Locator inputWithValue(String name, String value)
    {
        return Locator.xpath("//input[@name='" + name + "' and @value='" + value + "']");
    }
    

    protected void customizeURLTest()
    {
        this.pushLocation();
        {
            createList("C", Ccolumns, Cdata);
            createList("B", Bcolumns, Bdata);
            createList("A", Acolumns, Adata);

            beginAt("/query/" + PROJECT_NAME + "/executeQuery.view?schemaName=lists&query.queryName=A");

            pushLocation();
            {
                clickLinkWithText("one A");
                assertElementPresent(inputWithValue("table","A"));
                assertElementPresent(inputWithValue("title","one A"));
                assertElementPresent(inputWithValue("key","1"));
            }
            popLocation();

            pushLocation();
            {
                clickLinkWithText("one B");
                assertLinkPresentWithText("one B");
                assertLinkPresentWithText("one C");
            }
            popLocation();

            // show all columns
            clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
            click(Locator.id("expand_Bfk"));
            click(Locator.id("expand_Bfk/Cfk"));
            addCustomizeViewColumn("Bfk/B", "Bfk B");
            addCustomizeViewColumn("Bfk/title", "Bfk Title");
            addCustomizeViewColumn("Bfk/Cfk", "Bfk Cfk");
            addCustomizeViewColumn("Bfk/Cfk/C", "Bfk Cfk C");
            addCustomizeViewColumn("Bfk/Cfk/title", "Bfk Cfk Title");
            setFormElement("ff_columnListName", "allColumns");
            clickNavButton("Save");

            clickLinkWithText("one C", 1);
            assertElementPresent(inputWithValue("key","1"));
            assertElementPresent(inputWithValue("table","C"));
            assertElementPresent(inputWithValue("title","one C"));
            assertTrue(getCurrentRelativeURL().contains("/junit/" + PROJECT_NAME + "/echoForm.view"));
        }        
        popLocation();
    }
}
