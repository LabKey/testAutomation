/*
 * Copyright (c) 2015-2018 LabKey Corporation
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
package org.labkey.test.tests.list;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelper.ListColumn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({DailyA.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class ColumnResizeTest extends BaseWebDriverTest
{
    //Column Names
    private static final String
            LIST_NAME = "ColumnResizeTestList",
            PROJECT_NAME = "ColumnResizeTest",
            LIST_KEY_NAME = "ListKeyName",
            MAX_COLUMN_NAME = "Max",
            GT_COLUMN_NAME = "GreaterThan4K",
            LT_COLUMN_NAME = "LessThan4K",
            FOUR_K_COLUMN_NAME = "FourK",
            MULTI_COLUMN_NAME = "TextArea",
            NUMBER_COLUMN_NAME = "Number";

    //List Designer row indexes
    private static final int
            KEY_ROW = 0,
            MAX_ROW = 1,
            GT_ROW = 2,
            LT_ROW = 3,
            FOUR_ROW = 4,
            MULTI_ROW = 5,
            NUMBER_ROW = 6;

    //Scales used
    private static final int
            LT_SCALE = 10,
            DEFAULT_SCALE = 4000,
            GT_SCALE = 5000;

    private static final String
        OVER_4K = StringUtils.repeat('a', 4001),
        EXACTLY_4K = StringUtils.repeat('b', 4000),
        SHORT_STRING = "ccc";


    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    private void setUpList(String listName)
    {
        ListHelper listHelper = new ListHelper(this);
        ListColumn maxCol = new ListColumn(MAX_COLUMN_NAME, MAX_COLUMN_NAME, ListHelper.ListColumnType.String,null,null,null,null,null,Integer.MAX_VALUE);
        ListColumn gtCol = new ListColumn(GT_COLUMN_NAME, GT_COLUMN_NAME, ListHelper.ListColumnType.String,null,null,null,null,null,GT_SCALE);
        ListColumn ltCol = new ListColumn(LT_COLUMN_NAME, LT_COLUMN_NAME, ListHelper.ListColumnType.String,null,null,null,null,null, LT_SCALE);
        ListColumn fourCol = new ListColumn(FOUR_K_COLUMN_NAME, FOUR_K_COLUMN_NAME, ListHelper.ListColumnType.String,null,null,null,null,null,DEFAULT_SCALE);
        ListColumn textAreaCol = new ListColumn(MULTI_COLUMN_NAME, MULTI_COLUMN_NAME, ListHelper.ListColumnType.MultiLine,null);
        ListColumn numberCol = new ListColumn(NUMBER_COLUMN_NAME, NUMBER_COLUMN_NAME, ListHelper.ListColumnType.Integer,null);

        listHelper.createList(PROJECT_NAME, listName, ListHelper.ListColumnType.String, LIST_KEY_NAME,
                maxCol, gtCol, ltCol, fourCol, textAreaCol, numberCol);
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        ColumnResizeTest init = (ColumnResizeTest)getCurrentTest();
        init._containerHelper.createProject(PROJECT_NAME, null);
    }

    /**
     * Test to verify scale changes for provisioned fields
     * Uses: Lists and the List Designer
     */
    @Test
    public void testColumnResizing()
    {
        setUpList(LIST_NAME);

        PropertiesEditor propertiesEditor = _listHelper.clickEditDesign().listFields();

        log("Validate Scale: Existing fields");
        assertWidgetNotVisible(propertiesEditor, KEY_ROW);    //Check scale widget is not available for key

        //Check unhidden
        assertMaxChecked(propertiesEditor, MAX_ROW);  //Check max checked for field set to max
        assertMaxChecked(propertiesEditor, GT_ROW);   //Check max checked for field set to > 4k
        assertMaxNotChecked(propertiesEditor, LT_ROW);    //Check max isn't checked for LT_ROW
        assertMaxNotChecked(propertiesEditor, FOUR_ROW);  //Check max isn't checked for field set to 4k
        assertMaxNotChecked(propertiesEditor, MULTI_ROW);

        //Check hiding
        assertWidgetNotVisible(propertiesEditor, NUMBER_ROW); //Check widget is not available for number

        //Check value is as expected for field
        assertScaleEquals(propertiesEditor, LT_ROW, LT_SCALE);    //Check arbitrary scale can be set
        assertScaleEquals(propertiesEditor, FOUR_ROW, DEFAULT_SCALE); //Validate 4K is stored
        assertScaleEquals(propertiesEditor, MULTI_ROW, DEFAULT_SCALE);    //Validate that scale not set defaults to 4K

        //Make changes
        log("Validate Scale: Change Scale");
        changeScale(propertiesEditor, LT_ROW, DEFAULT_SCALE, false);  // LT--> LT
        changeScale(propertiesEditor, GT_ROW, LT_SCALE, false);   //Max --> LT
        changeScale(propertiesEditor, FOUR_ROW, GT_SCALE, true); //max checked
        changeScale(propertiesEditor, MULTI_ROW, GT_SCALE, false); //LT --> GT --> Max
        propertiesEditor.selectField(MAX_ROW).setType(FieldDefinition.ColumnType.MultiLine);
        assertMaxChecked(propertiesEditor, MAX_ROW);

        _listHelper.clickSave();
        goToManageLists();
        clickAndWait(Locator.linkWithText("view design"));
        propertiesEditor = _listHelper.clickEditDesign().listFields();  //Open designer so row locator works

        //Verify Changes are retained
        log("Validate Scale: Verify new Scale retained");
        assertMaxChecked(propertiesEditor, MAX_ROW);  //Check max check unchanged for type change
        assertMaxNotChecked(propertiesEditor, GT_ROW);    //Check max unchecked change retained
        assertScaleEquals(propertiesEditor, GT_ROW, LT_SCALE);    //Check arbitrary scale change retained
        assertMaxNotChecked(propertiesEditor, LT_ROW);    //Check scale change retained
        assertScaleEquals(propertiesEditor, LT_ROW, DEFAULT_SCALE);
        assertMaxChecked(propertiesEditor, FOUR_ROW);     //Check max retained
        assertMaxChecked(propertiesEditor, MULTI_ROW);    //Check GT change sets max
    }

    /**
     * Test to verify scale changes for provisioned fields
     * Uses: Lists and the List Designer
     */
    @Test
    public void testResizeColumnWithData()
    {
        //Create List
        String listName = LIST_NAME + "_WithData";
        setUpList(listName);

        log("inserting row");
        Map<String,String> row = new HashMap<>();
        row.put(LIST_KEY_NAME, "0");
        row.put(MAX_COLUMN_NAME, OVER_4K);
        row.put(GT_COLUMN_NAME, EXACTLY_4K);
        row.put(LT_COLUMN_NAME, SHORT_STRING);
        row.put(FOUR_K_COLUMN_NAME, EXACTLY_4K);

        //Insert row into List
        goToManageLists();
        clickAndWait(Locator.linkWithText(listName));
        _listHelper.insertNewRow(row);

        log("Change column with existing larger data");
        //Check changing size with larger existing data
        _extHelper.clickMenuButton("Design");
        PropertiesEditor propertiesEditor = _listHelper.clickEditDesign().listFields();
        changeScale(propertiesEditor, MAX_ROW, LT_SCALE, false);
        clickButton("Save", 0);
        assertAlertContains("A data error occurred");
        checkExpectedErrors(2);

        //Cancel changes
        clickButton("Cancel",0);
        assertAlertContains("discarded");

        log("Change column with existing smaller data");
        //Check changing size with smaller existing data
        propertiesEditor = _listHelper.clickEditDesign().listFields();
        changeScale(propertiesEditor, GT_ROW, DEFAULT_SCALE, false);
        _listHelper.clickSave();
        assertNoLabKeyErrors();
    }

    /**
     * Set value of scale widget
     * @param rowIndex row Index of field to change scale on
     * @param newScale new scale value
     * @param checkMax Override flag to set Max (will not unset Max if newScale >4K)
     */
    private void changeScale(PropertiesEditor propertiesEditor, int rowIndex, int newScale, boolean checkMax)
    {
        PropertiesEditor.FieldPropertyDock.AdvancedTabPane advancedProperties = propertiesEditor
                .selectField(rowIndex).properties()
                .selectAdvancedTab();

        //Uncheck Max if necessary
        advancedProperties.maxTextCheckbox.uncheck();
        advancedProperties.maxTextInput.set(String.valueOf(newScale));
        if (checkMax)
            advancedProperties.maxTextCheckbox.check();
    }

    private void assertScaleEquals(PropertiesEditor propertiesEditor, int rowIndex, Integer expected)
    {
        String scaleStr = propertiesEditor
                .selectField(rowIndex).properties()
                .selectAdvancedTab()
                .maxTextInput.get();
        Integer scale = Integer.valueOf(scaleStr.replace(",",""));

        Assert.assertEquals(String.format("Scale for row [%d] is actually [%d] vs expected [%d]", rowIndex, scale, expected), expected, scale);
    }

    private void assertWidgetNotVisible(PropertiesEditor propertiesEditor, int rowIndex)
    {
        propertiesEditor.selectField(rowIndex).properties().selectAdvancedTab();
        assertElementNotVisible(ListHelper.DesignerLocators.maxCheckbox);
        assertElementNotVisible(ListHelper.DesignerLocators.scaleTextbox);
    }

    private void assertMaxChecked(PropertiesEditor propertiesEditor, int rowIndex)
    {
        propertiesEditor.selectField(rowIndex).properties().selectAdvancedTab();
        assertElementVisible(ListHelper.DesignerLocators.maxCheckbox);
        assertChecked(ListHelper.DesignerLocators.maxCheckbox);
        Assert.assertFalse("Scale textbox is enabled for row [" + rowIndex + "]", ListHelper.DesignerLocators.scaleTextbox.findElement(getDriver()).isEnabled());
    }

    private void assertMaxNotChecked(PropertiesEditor propertiesEditor, int rowIndex)
    {
        propertiesEditor.selectField(rowIndex).properties().selectAdvancedTab();
        assertElementVisible(ListHelper.DesignerLocators.maxCheckbox);
        assertNotChecked(ListHelper.DesignerLocators.maxCheckbox);
        Assert.assertTrue("Scale textbox is not enabled for row [" + rowIndex + "]", ListHelper.DesignerLocators.scaleTextbox.findElement(getDriver()).isEnabled());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
