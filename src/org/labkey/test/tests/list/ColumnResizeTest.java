/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.util.ListHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
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
        FieldDefinition maxCol = new FieldDefinition(MAX_COLUMN_NAME, ColumnType.String).setScale(Integer.MAX_VALUE);
        FieldDefinition gtCol = new FieldDefinition(GT_COLUMN_NAME, ColumnType.String).setScale(GT_SCALE);
        FieldDefinition ltCol = new FieldDefinition(LT_COLUMN_NAME, ColumnType.String).setScale(LT_SCALE);
        FieldDefinition fourCol = new FieldDefinition(FOUR_K_COLUMN_NAME, ColumnType.String).setScale(DEFAULT_SCALE);
        FieldDefinition textAreaCol = new FieldDefinition(MULTI_COLUMN_NAME, ColumnType.MultiLine);
        FieldDefinition numberCol = new FieldDefinition(NUMBER_COLUMN_NAME, ColumnType.Integer);

        listHelper.createList(PROJECT_NAME, listName, new FieldDefinition(LIST_KEY_NAME, ColumnType.String),
                maxCol, gtCol, ltCol, fourCol, textAreaCol, numberCol);
    }

    @Override
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
        _listHelper.goToList(LIST_NAME);
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME);
        DomainFormPanel fieldsPanel = listDefinitionPage.getFieldsPanel();

        log("Validate Scale: Existing fields");
        // Issue 39877: max text length options should not be visible for text key field of list
        assertMaxTextLengthNotVisible(fieldsPanel, KEY_ROW);    //Check scale widget is not available for key

        //Check unhidden
        assertMaxChecked(fieldsPanel, MAX_ROW);  //Check max checked for field set to max
        assertMaxChecked(fieldsPanel, GT_ROW);   //Check max checked for field set to > 4k
        assertMaxNotChecked(fieldsPanel, LT_ROW);    //Check max isn't checked for LT_ROW
        assertMaxNotChecked(fieldsPanel, FOUR_ROW);  //Check max isn't checked for field set to 4k
        assertMaxNotChecked(fieldsPanel, MULTI_ROW);

        //Check hiding
        assertMaxTextLengthNotVisible(fieldsPanel, NUMBER_ROW); //Check widget is not available for number

        //Check value is as expected for field
        assertScaleEquals(fieldsPanel, LT_ROW, LT_SCALE);    //Check arbitrary scale can be set
        assertScaleEquals(fieldsPanel, FOUR_ROW, DEFAULT_SCALE); //Validate 4K is stored
        assertScaleEquals(fieldsPanel, MULTI_ROW, DEFAULT_SCALE);    //Validate that scale not set defaults to 4K

        //Make changes
        log("Validate Scale: Change Scale");
        changeScale(fieldsPanel, LT_ROW, DEFAULT_SCALE, false);  // LT--> LT
        changeScale(fieldsPanel, GT_ROW, LT_SCALE, false);   //Max --> LT
        changeScale(fieldsPanel, FOUR_ROW, GT_SCALE, true); //max checked
        changeScale(fieldsPanel, MULTI_ROW, GT_SCALE, false); //LT --> GT --> Max
        fieldsPanel.getField(MAX_ROW).setType(ColumnType.MultiLine);
        assertMaxChecked(fieldsPanel, MAX_ROW);
        listDefinitionPage.clickSave();

        _listHelper.goToList(LIST_NAME);
        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME);
        fieldsPanel = listDefinitionPage.getFieldsPanel();

        //Verify Changes are retained
        log("Validate Scale: Verify new Scale retained");
        assertMaxChecked(fieldsPanel, MAX_ROW);  //Check max check unchanged for type change
        assertMaxNotChecked(fieldsPanel, GT_ROW);    //Check max unchecked change retained
        assertScaleEquals(fieldsPanel, GT_ROW, LT_SCALE);    //Check arbitrary scale change retained
        assertMaxNotChecked(fieldsPanel, LT_ROW);    //Check scale change retained
        assertScaleEquals(fieldsPanel, LT_ROW, DEFAULT_SCALE);
        assertMaxChecked(fieldsPanel, FOUR_ROW);     //Check max retained
        assertMaxChecked(fieldsPanel, MULTI_ROW);    //Check GT change sets max
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
        _listHelper.goToList(listName);
        _listHelper.insertNewRow(row);

        log("Change column with existing larger data");
        //Check changing size with larger existing data
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listName);
        DomainFormPanel fieldsPanel = listDefinitionPage.getFieldsPanel();
        changeScale(fieldsPanel, MAX_ROW, LT_SCALE, false);
        listDefinitionPage.clickSaveExpectingErrors();
        String expectedErrorTxt ="The property \"" + MAX_COLUMN_NAME + "\" cannot be scaled down. It contains existing values exceeding [" + LT_SCALE + "] characters.";
        assertEquals(expectedErrorTxt, fieldsPanel.getPanelErrorText());
        checkExpectedErrors(0);  // Shouldn't log any SQLExceptions - product should detect inability to scale and not issue an alter query

        //Cancel changes
        listDefinitionPage.clickCancel();

        log("Change column with existing smaller data");
        //Check changing size with smaller existing data
        listDefinitionPage = _listHelper.goToEditDesign(listName);
        fieldsPanel = listDefinitionPage.getFieldsPanel();
        changeScale(fieldsPanel, GT_ROW, DEFAULT_SCALE, false);
        listDefinitionPage.clickSave();
        assertNoLabKeyErrors();
    }

    /**
     * Set value of scale widget
     * @param rowIndex row Index of field to change scale on
     * @param newScale new scale value
     * @param checkMax Override flag to set Max (will not unset Max if newScale &gt; 4K)
     */
    private void changeScale(DomainFormPanel fieldsPanel, int rowIndex, int newScale, boolean checkMax)
    {
        DomainFieldRow fieldRow = fieldsPanel.getField(rowIndex);
        if (checkMax)
            fieldRow.allowMaxChar();
        else
            fieldRow.setCharCount(newScale);
    }

    private void assertScaleEquals(DomainFormPanel fieldsPanel, int rowIndex, Integer expected)
    {
        Integer scale = fieldsPanel.getField(rowIndex).getCustomCharCount();
        assertEquals(String.format("Scale for row [%d] is actually [%d] vs expected [%d]", rowIndex, scale, expected), expected, scale);
    }

    private void assertMaxTextLengthNotVisible(DomainFormPanel fieldsPanel, int rowIndex)
    {
        DomainFieldRow fieldRow = fieldsPanel.getField(rowIndex).expand();
        assertFalse("Max text length options should not be visible for field " + rowIndex, fieldRow.isMaxTextLengthPresent(rowIndex));
    }

    private void assertMaxChecked(DomainFormPanel fieldsPanel, int rowIndex)
    {
        DomainFieldRow fieldRow = fieldsPanel.getField(rowIndex).expand();
        assertTrue("Expected max text length options to be visible for field " + rowIndex, fieldRow.isMaxTextLengthPresent(rowIndex));
        assertTrue("Expected max text length 'unlimited' option selected for field " + rowIndex, fieldRow.isMaxCharDefault());
        assertFalse("Expected max text length 'custom' option unselected for field " + rowIndex, fieldRow.isCustomCharSelected());
        assertTrue("Scale textbox is enabled for row [" + rowIndex + "]", fieldRow.isCharCountDisabled());
    }

    private void assertMaxNotChecked(DomainFormPanel fieldsPanel, int rowIndex)
    {
        DomainFieldRow fieldRow = fieldsPanel.getField(rowIndex).expand();
        assertTrue("Expected max text length options to be visible for field " + rowIndex, fieldRow.isMaxTextLengthPresent(rowIndex));
        assertFalse("Expected max text length 'unlimited' option unselected for field " + rowIndex, fieldRow.isMaxCharDefault());
        assertTrue("Expected max text length 'custom' option selected for field " + rowIndex, fieldRow.isCustomCharSelected());
        assertFalse("Scale textbox is disabled for row [" + rowIndex + "]", fieldRow.isCharCountDisabled());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
