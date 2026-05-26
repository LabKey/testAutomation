/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.test.tests.component;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebTestHelper;
import org.labkey.test.WebTestHelper.DatabaseType;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.ui.FilterStatusValue;
import org.labkey.test.components.ui.grids.FieldSelectionDialog;
import org.labkey.test.components.ui.grids.GridFilterModal;
import org.labkey.test.components.ui.grids.ManageViewsDialog;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.components.ui.grids.SaveViewDialog;
import org.labkey.test.components.ui.search.FilterExpressionPanel;
import org.labkey.test.components.ui.search.FilterFacetedPanel;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldKey;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.data.TestArrayDataUtils;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.labkey.test.util.PermissionsHelper.FOLDER_ADMIN_ROLE;
import static org.labkey.test.util.SampleTypeHelper.beginAtSampleTypesList;
import static org.labkey.test.util.TestDataGenerator.randomTextChoice;

@Category({Daily.class})
public class GridPanelViewTest extends GridPanelBaseTest
{

    // Sample type used to validate views.
    private static final String DEFAULT_VIEW_SAMPLE_TYPE = "View_SampleType";
    private static final int DEFAULT_VIEW_SAMPLE_TYPE_SIZE = 30;
    private static final String DEFAULT_VIEW_SAMPLE_PREFIX = "VIEW-";

    // A sample type that will be used to validate the dialog. Will test adding 'hidden' fields etc...
    private static final String VIEW_DIALOG_ST = "Test_View_Dialog_SampleType";
    private static final int VIEW_DIALOG_ST_SIZE = 100;
    private static final String VIEW_DIALOG_ST_PREFIX = "DLG-";

    // Column names.
    private static final String COL_NAME = "Name";
    private static final String COL_STRING1 = "Str1";
    private static final String COL_STRING2 = "Str2";
    private static final String COL_INT = "Int";
    private static final String COL_BOOL = "Bool";
    // Excluded semicolon due to GitHub Issue 1096
    public static final List<String> TEXT_MULTI_CHOICE_LIST = randomTextChoice(10, ";");
    public static final String COL_MULTITEXTCHOICE = "Multi Choice";

    private static final boolean MULTI_CHOICE_ENABLED = WebTestHelper.getDatabaseType() == DatabaseType.PostgreSQL;
    private static final List<String> DEFAULT_COLUMNS = MULTI_CHOICE_ENABLED
            ? Arrays.asList(COL_NAME, COL_INT, COL_STRING1, COL_STRING2, COL_BOOL, COL_MULTITEXTCHOICE)
            : Arrays.asList(COL_NAME, COL_INT, COL_STRING1, COL_STRING2, COL_BOOL);

    // Will keep track of state of the columns, that is are they filtered, sorted, or have no modifiers.
    private static Map<String, Integer> defaultColumnState = new HashMap<>();
    private static final int ICONS_NONE = 0;
    private static final int ICONS_SORT = 1;
    private static final int ICONS_FILTER = 2;

    private static final String OTHER_USER = "other_user@grid.panel.test";

    private static final List<String> stringSetMembers = Arrays.asList("A", "B", "C");
    private static List<String> stringSets = new ArrayList<>();

    private static final String AUDIT_TEST_VIEW = "Audit_Test_View";

    private static final String EDITED_ALERT = "EDITED";
    private static final String UPDATED_ALERT = "UPDATED";

    // Keep track of any custom views that may have been created.
    private static final List<String> savedViewsForDefaultSampleType = new ArrayList<>();

    // Using the core-components.view adds 'GridPanel - ' to the panel header. Need to take that into account .
    private static final String PANEL_VIEW_NAME_PREFIX = "GridPanel - %s";

    private final AuditLogHelper _auditLogHelper = new AuditLogHelper(this);

    /**
     * Describes the expected state of a saved grid view's filter pill after a field type conversion.
     *
     * @param isDropped          {@code true} if the filter is expected to be silently dropped by the conversion
     *                           (i.e., no filter pill should be visible); {@code false} if the filter should survive.
     * @param expectedFilterText the substring expected to appear in the filter pill after conversion;
     *                           {@code null} when {@code isDropped} is {@code true}.
     */
    private record GridConversionResult(boolean isDropped, @Nullable String expectedFilterText)
    {
        /** The filter survives conversion and its pill should contain {@code filterText}. */
        static GridConversionResult kept(String filterText) { return new GridConversionResult(false, filterText); }
        /** The filter is silently dropped by the conversion; no filter pill should be visible. */
        static GridConversionResult dropped()               { return new GridConversionResult(true, null); }
    }

    /**
     * A test case for verifying that a saved grid view with an MVTC filter survives (or is dropped by)
     * a field type conversion.
     *
     * @param viewName          name of the saved grid view to create and verify.
     * @param op                the MVTC filter operator applied when creating the view.
     * @param filterVals        the filter values; may be empty for unary operators (e.g. IS EMPTY).
     * @param conversionResult  the expected filter state after conversion.
     * @param expectedSampleMap map of sample name → expected MVTC column value for result verification.
     */
    private record GridMVTCCase(String viewName, Filter.Operator op, String[] filterVals,
                                GridConversionResult conversionResult,
                                Map<String, String> expectedSampleMap) {}

    /**
     * A test case for verifying that a saved grid view with a TextChoice filter survives
     * a TC→MVTC field type conversion.
     *
     * @param viewName                  name of the saved grid view to create and verify.
     * @param op                        the TC filter operator applied when creating the view.
     * @param filterVals                the filter values; may be empty for unary operators.
     * @param expectedAfterConversionText substring expected in the filter pill after TC→MVTC conversion.
     * @param expectedSampleMap         map of sample name → expected MVTC column value for result verification.
     */
    private record GridTCCase(String viewName, Filter.Operator op, String[] filterVals,
                              String expectedAfterConversionText,
                              Map<String, String> expectedSampleMap) {}

    // Tests that need to be written:
    // Validate "Save As..." from the grid save button.
    // Validate views that are locked, or in some other way, cannot be updates in the manage views dialog.
    // ???

    @Override
    protected String getProjectName()
    {
        return "QueryGrid_View_Test_Project";
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        new APIUserHelper(this).deleteUser(OTHER_USER);
    }

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        GridPanelViewTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        initProject();

        // Create list of strings that has the various sets of the letters A, B, C & D
        stringSets = getAllSets(stringSetMembers, stringSetMembers.size() - 1);
        // Remove the empty set.
        stringSets.removeFirst();

        // Create a sample type that will validate views can be saved and shared. Primarily interested in the views not
        // with complex filtering scenarios.
        List<FieldDefinition> fields = new ArrayList<>(Arrays.asList(
                new FieldDefinition(COL_INT, FieldDefinition.ColumnType.Integer),
                new FieldDefinition(COL_STRING1, FieldDefinition.ColumnType.String),
                new FieldDefinition(COL_STRING2, FieldDefinition.ColumnType.String),
                new FieldDefinition(COL_BOOL, FieldDefinition.ColumnType.Boolean)));
        if (MULTI_CHOICE_ENABLED)
            fields.add(new FieldDefinition(COL_MULTITEXTCHOICE, FieldDefinition.ColumnType.MultiValueTextChoice)
                    .setMultiChoiceValues(TEXT_MULTI_CHOICE_LIST));

        createSampleType(VIEW_DIALOG_ST, VIEW_DIALOG_ST_PREFIX, VIEW_DIALOG_ST_SIZE, fields);

        createSampleType(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_VIEW_SAMPLE_PREFIX, DEFAULT_VIEW_SAMPLE_TYPE_SIZE, fields);

        _userHelper.createUser(OTHER_USER, true,false);
        new ApiPermissionsHelper(this).addMemberToRole(OTHER_USER, FOLDER_ADMIN_ROLE, PermissionsHelper.MemberType.user, getProjectName());

        removeFlagColumnFromDefaultView(DEFAULT_VIEW_SAMPLE_TYPE);
    }

    private void createSampleType(String sampleTypeName, String samplePrefix, int numOfSamples, List<FieldDefinition> fields) throws IOException, CommandException
    {
        SampleTypeDefinition props = new SampleTypeDefinition(sampleTypeName)
                .setFields(fields);

        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);

        generateSamples(sampleSetDataGenerator, samplePrefix, numOfSamples);

        sampleSetDataGenerator.insertRows();

        removeFlagColumnFromDefaultView(sampleTypeName);
    }

    private void generateSamples(TestDataGenerator sampleSetDataGenerator, String samplePrefix, int numOfSamples) throws IOException
    {
        int sampleId = 1;
        int allPossibleIndex = 0;
        int memIndex = 0;

        while (sampleId <= numOfSamples)
        {

            if(allPossibleIndex == stringSets.size())
                allPossibleIndex = 0;

            if(memIndex == stringSetMembers.size())
                memIndex = 0;
            String name = String.format("%s%d", samplePrefix, sampleId);
            Map<String, Object> rowData = new HashMap<>();
            rowData.put(COL_NAME, name);
            rowData.put(COL_INT, sampleId);
            rowData.put(COL_STRING1, stringSets.get(allPossibleIndex));
            rowData.put(COL_STRING2, stringSetMembers.get(memIndex));
            rowData.put(COL_BOOL, sampleId % 2 == 0);
            if (MULTI_CHOICE_ENABLED)
            {
                rowData.put(COL_MULTITEXTCHOICE, sampleId % 5 == 0
                        ? List.of()
                        : List.of(TEXT_MULTI_CHOICE_LIST.get(Math.abs(name.hashCode()) % TEXT_MULTI_CHOICE_LIST.size())));
            }
            sampleSetDataGenerator.addCustomRow(rowData);

            allPossibleIndex++;
            memIndex++;
            sampleId++;
        }
    }

    /**
     * Helper function that will reset the default view of a grid. It will also remove any sorts or filters on the columns.
     *
     * @param sampleTypeName Name of the sample type with the default view to change.
     * @param columns The columns to show in the default view. Will be added in the order of the list.
     */
    private void resetDefaultView(String sampleTypeName, List<String> columns) throws Exception
    {
        log(String.format("Set the default view for '%s' to have these columns: '%s'", sampleTypeName, columns));

        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText(sampleTypeName));
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        drtSamples.goToView(VIEW_DEFAULT);
        CustomizeView cv = drtSamples.openCustomizeGrid();

        log("Clear any filters that may have been applied.");
        cv.clearFilters();

        log("Clear any sorts that may have been applied.");
        cv.clearSorts();

        log("Remove all the current columns in the view.");
        cv.clearColumns();

        // Column order matters for some tests. Respect column order defined in the columns parameter.
        log("Add the list of columns in the order passed in.");
        cv = drtSamples.openCustomizeGrid();
        for(String columnName : columns)
        {
            cv.addColumn(columnName);
        }

        log("Get baseline audit row ID before saving the shared default view.");
        Integer defaultViewBaselineRowId = _auditLogHelper.getLatestAuditRowId("GridViewAuditEvent");

        // Save as default view for everyone.
        cv.saveCustomView("", true);

        log("Verify audit event for shared default view save.");
        // if the current default view is a private view, 2 events will be generated, one for the creation of the new shared default view, and one for deletion of the existing view.
        // If the current default view is already a shared view, then only 1 event will be generated for the update of the default view. If no current view, a view will be created.
        List<Map<String, Object>> defaultViewAuditRows = getGridViewAuditEvents(defaultViewBaselineRowId);
        checker().verifyTrue("Expected at least 1 audit event for shared default view save.",
                !defaultViewAuditRows.isEmpty());

        if (!defaultViewAuditRows.isEmpty())
        {
            if (defaultViewAuditRows.size() > 1)
            {
                Map<String, Object> deletedPrivateViewEvent = defaultViewAuditRows.getFirst();

                checker().verifyTrue("Comment should contain 'deleted' for private default view that's deleted.",
                        String.valueOf(deletedPrivateViewEvent.get("Comment")).contains("deleted"));

                checker().verifyEquals("ViewName should be 'Default'.", "Default", deletedPrivateViewEvent.get("ViewName"));

                checker().verifyFalse("CustomViewOwner for the deleted private view should not be null.",
                        deletedPrivateViewEvent.get("CustomViewOwner") == null);
            }
            else
            {
                Map<String, Object> defaultViewEvent = defaultViewAuditRows.getFirst();

                checker().verifyTrue("Comment should contain 'created' or 'updated' for default view.",
                        String.valueOf(defaultViewEvent.get("Comment")).contains("created") || String.valueOf(defaultViewEvent.get("Comment")).contains("updated"));

                checker().verifyEquals("ViewName should be 'Default'.", "Default", defaultViewEvent.get("ViewName"));

                checker().verifyTrue("CustomViewOwner should be null for a shared view.",
                        defaultViewEvent.get("CustomViewOwner") == null);
            }

        }

        checker().screenShotIfNewError("Audit_Shared_Default_View_Error");

        // In the default view none of the columns will be filtered or sorted.
        defaultColumnState = new HashMap<>();
        for(String columnName : columns)
        {
            defaultColumnState.put(columnName, ICONS_NONE);
        }

    }

    /**
     * <p>
     *     Simple test to validate 'My Default' view.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Modify the default view for a sample type in LabKey Server.</li>
     *         <li>Save the default view, but do not make it public (default for all).</li>
     *         <li>Validate in the App grid that 'My Default' is shown under the Views menu.</li>
     *         <li>Validate that other users see no change to the default view.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testMyDefaultView() throws Exception
    {
        String screenShotID = "testMyDefaultView";

        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_COLUMNS);

        goToProjectHome();

        waitAndClickAndWait(Locator.linkWithText(DEFAULT_VIEW_SAMPLE_TYPE));

        String columnToRemove = COL_INT;

        log(String.format("In LabKey Server for sample type '%s' remove '%s' column from the default view.",
                DEFAULT_VIEW_SAMPLE_TYPE, columnToRemove));

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        drtSamples.goToView(VIEW_DEFAULT);

        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.removeColumn(columnToRemove);

        log("Get baseline audit row ID before saving personal default view.");
        Integer personalDefaultBaselineRowId = _auditLogHelper.getLatestAuditRowId("GridViewAuditEvent");

        log("Do not share this default view with everyone.");
        cv.saveCustomView("", false);

        log("Verify audit event for personal default view save.");
        List<Map<String, Object>> personalDefaultAuditRows = getGridViewAuditEvents(personalDefaultBaselineRowId);
        checker().verifyTrue("Expected at least 1 audit event for personal default view save.",
                !personalDefaultAuditRows.isEmpty());

        if (!personalDefaultAuditRows.isEmpty())
        {
            Map<String, Object> personalDefaultEvent = personalDefaultAuditRows.getFirst();

            checker().verifyTrue("Comment should contain 'created' or 'updated' for personal default view.",
                    String.valueOf(personalDefaultEvent.get("Comment")).contains("created") ||
                            String.valueOf(personalDefaultEvent.get("Comment")).contains("updated"));

            checker().verifyEquals("ViewName should be 'Default'.", "Default", personalDefaultEvent.get("ViewName"));

            checker().verifyTrue("CustomViewOwner should not be null for a personal view.",
                    personalDefaultEvent.get("CustomViewOwner") != null);
        }

        checker().screenShotIfNewError("Audit_Personal_Default_View_Error");

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        log(String.format("Verify that '%s' is in the views menu and '%s' is not.",
                VIEW_DEFAULT_MODIFIED, VIEW_DEFAULT));

        List<String> expectedMenuItems = new ArrayList<>();
        expectedMenuItems.addAll(List.of(VIEW_DEFAULT_MODIFIED, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE));
        expectedMenuItems.addAll(savedViewsForDefaultSampleType);

        log(String.format("Verify that column '%s' is removed.", columnToRemove));
        Map<String, Integer> expectedColumns = new HashMap<>(defaultColumnState);
        expectedColumns.remove(columnToRemove);

        // Now actually do the verification.
        validateViewMenu(screenShotID, grid, expectedMenuItems);
        validateGridColumns(screenShotID, grid, expectedColumns);

        log(String.format("Impersonate '%s' and validate that the 'View' menu and default view of the grid is not changed.", OTHER_USER));

        impersonate(OTHER_USER);

        goToProjectHome();

        grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);
        expectedMenuItems = Arrays.asList(VIEW_DEFAULT, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE);

        validateViewMenu(screenShotID, grid, expectedMenuItems);
        validateGridColumns(screenShotID, grid, defaultColumnState);

        stopImpersonating();

    }

    /**
     * <p>
     *     Use remove column to validate the view dirty bit is flipped.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>In the default view remove a column, validate that a view change is detected.</li>
     *         <li>Click 'Undo' and validate that the column is returned.</li>
     *         <li>Remove the column from the default view and save as default for everyone.</li>
     *         <li>Impersonate another user and validate default view is missing the expected column.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testRemoveColumnForView() throws Exception
    {

        final String screenShotPrefix = "testRemoveColumnForView";

        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_COLUMNS);

        String columnToRemove = COL_BOOL;
        log(String.format("For sample type '%s' remove the '%s' column using the column header menu.", DEFAULT_VIEW_SAMPLE_TYPE, columnToRemove));

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        grid.hideColumn(columnToRemove);

        log("Validate panel header is as expected and the column is indeed hidden.");

        validateGridHeader(screenShotPrefix, grid, EDITED_ALERT, true);

        Map<String, Integer> expectedColumns = new HashMap<>(defaultColumnState);
        expectedColumns.remove(columnToRemove);

        validateGridColumns(screenShotPrefix, grid, expectedColumns);

        log(String.format("Validate that 'Undo' puts the column back and removes the '%s' label as well as the buttons.", EDITED_ALERT));

        grid = grid.clickUndoButton();

        validateGridHeader(screenShotPrefix, grid, "", false);

        // If the grid columns are not as expected after Undo don't continue with the test.
        checker().fatal()
                .verifyTrue("Grid columns were not as expected. Fatal error.",
                        validateGridColumns(screenShotPrefix, grid, defaultColumnState));

        log(String.format("Hide the '%s' column again and this time save it as default.", columnToRemove));

        grid = grid.hideColumn(columnToRemove);

        log("Validate the 'Save View' dialog.");
        SaveViewDialog saveViewDialog = grid.clickSaveButton(true);

        checker().verifyTrue(String.format("The 'View Name' field should be empty. It contains '%s'.", saveViewDialog.getViewName()),
                saveViewDialog.getViewName().isEmpty());

        checker().verifyFalse("The 'Make default' checkbox should not be checked.",
                saveViewDialog.isMakeDefaultChecked());

        checker().screenShotIfNewError("testDefaultViewRemoveColumn_Save_View_Dialog_Defaults_Error");

        saveViewDialog.setMakeDefault();

        checker().verifyFalse("Setting 'Default for all' should hide the name field, but it did not.",
                saveViewDialog.isViewNameInputDisplayed());

        checker().screenShotIfNewError("testDefaultViewRemoveColumnFromAppGrid_Save_View_Dialog_Set_Error");

        saveViewDialog.saveView();

        validateGridHeader(screenShotPrefix, grid, UPDATED_ALERT, false);

        log("Verify that view menu and columns are as expected.");
        List<String> expectedMenuItems = Arrays.asList(VIEW_DEFAULT, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE);
        validateViewMenu(screenShotPrefix, grid, expectedMenuItems);
        validateGridColumns(screenShotPrefix, grid, expectedColumns);

        log(String.format("Impersonate '%s' and validate the 'View' menu and default view are as expected.", OTHER_USER));

        impersonate(OTHER_USER);

        goToProjectHome();
        grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        validateViewMenu(screenShotPrefix, grid, expectedMenuItems);
        validateGridColumns(screenShotPrefix, grid, expectedColumns);

        stopImpersonating();
    }

    /**
     * Validate filter pills and column headers for the default view.
     * @see GridPanelViewTest#testColumnHeaderAndFilterPill(String, String)
     */
    @Test
    public void testColumnHeaderAndFilterPillDefaultView() throws Exception
    {
        testColumnHeaderAndFilterPill("testColumnHeaderAndFilterPillDefaultView", "");
    }

    /**
     * Validate filter pills and column headers for a saved custom view.
     * @see GridPanelViewTest#testColumnHeaderAndFilterPill(String, String)
     */
    @Test
    public void testColumnHeaderAndFilterPillCustomView() throws Exception
    {
        testColumnHeaderAndFilterPill("testColumnHeaderAndFilterPillCustomView", "Test_Columns_And_Pills_View");
    }

    /**
     * <p>
     *     For a view validate that the column header and filter pills are correct and have the correct icons.
     * </p>
     * <p>
     *     For the view provided this test will:
     *     <ul>
     *         <li>Filter one column and sort another without saving the view.</li>
     *         <li>Validate icons at the top of the grid columns.</li>
     *         <li>Validate filter pills and that they are not locked.</li>
     *         <li>Save the view, and validate filter pills are locked.</li>
     *         <li>Reload the page and validate the filter pills and column headers.</li>
     *         <li>Impersonate another user and validate the Views menu, filter pills and column headers (should not be present for saved view).</li>
     *     </ul>
     * </p>
     * @param testName The name of the test, used for the screenshot prefix.
     * @param viewName What to name the saved view. If empty string will be default view.
     */
    public void testColumnHeaderAndFilterPill(String testName, String viewName) throws Exception
    {

        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        int rows = grid.getRows().size();

        String colToFilter = COL_STRING1;
        String colToSort = COL_STRING2;

        log(String.format("For sample type '%s' filter column '%s' and sort column '%s'.", DEFAULT_VIEW_SAMPLE_TYPE, colToFilter, colToSort));

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        filterDialog.selectField(colToFilter);
        FilterFacetedPanel facetedPanel = filterDialog.selectFacetTab();

        facetedPanel.uncheckValues("[All]");
        facetedPanel.checkValues(stringSets.get(0), stringSets.get(2));
        String expectedPillText = String.format("%s Equals One Of %s, %s", colToFilter, stringSets.get(0), stringSets.get(2));

        filterDialog.confirm();

        grid.sortColumn(colToSort, SortDirection.ASC);

        Map<String, Integer> expectedColumns = new HashMap<>(defaultColumnState);
        expectedColumns.replace(colToFilter, ICONS_FILTER);
        expectedColumns.replace(colToSort, ICONS_SORT);

        log("Validate the icons in the column header.");
        validateGridColumns("validateColumnHeaderAndFilterPill", grid, expectedColumns);

        log("Validate filter pill is shown and is not locked.");
        Map<String, Boolean> expectedFilters = Map.of(expectedPillText, false);

        validateFilterPills(testName, grid, expectedFilters);

        SaveViewDialog saveViewDialog;

        if(viewName.isEmpty())
        {
            log("Save as default view (for everyone).");
            saveViewDialog = grid.saveView();
            saveViewDialog.setMakeDefault();
        }
        else
        {
            log(String.format("Save as custom view '%s'.", viewName));
            saveViewDialog = grid.saveView();
            saveViewDialog.setMakeCustom();
            saveViewDialog.setViewName(viewName);
            saveViewDialog.setMakeShared(false);
            savedViewsForDefaultSampleType.add(viewName);
        }

        saveViewDialog.saveView();

        log("Refresh the page and validate icons from the view.");
        refresh();

        if(viewName.isEmpty())
        {
            // If this is the default view do not clear the filters after refresh, it will just cause complexity.
            grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE, false);
        }
        else
        {
            grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE, true);

            log(String.format("Select the view '%s' from the menu.", viewName));
            grid.selectView(viewName);
        }

        // Pill should now be locked.
        expectedFilters = Map.of(expectedPillText, true);
        validateFilterPills(testName, grid, expectedFilters);

        if(viewName.isEmpty())
        {
            log(String.format("Impersonate '%s' and validate icons in the columns for default view.", OTHER_USER));

            impersonate(OTHER_USER);

            goToProjectHome();

            grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE, false);

            validateGridColumns("validateColumnHeaderAndFilterPill", grid, expectedColumns);

            checker().verifyTrue("Doesn't look like filtering was applied. Row count is not changed as expected.",
                    grid.getRows().size() < rows);

            validateFilterPills(testName, grid, expectedFilters);

            stopImpersonating();
        }
        else
        {
            log(String.format("Impersonate '%s' and validate custom view is not in the menu and default view is unchanged.", OTHER_USER));

            impersonate(OTHER_USER);

            goToProjectHome();

            grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE, false);

            List<String> expectedMenuItems = Arrays.asList(VIEW_DEFAULT, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE);

            validateViewMenu(testName, grid, expectedMenuItems);
            validateGridColumns(testName, grid, defaultColumnState);

            checker().verifyEquals("Row count in default view not as expected.",
                    rows, grid.getRows().size());

            stopImpersonating();
        }

    }

    /**
     * Test modifying filters used to make a custom/named view.
     * @see GridPanelViewTest#testEditView(String, String)
     */
    @Test
    public void testEditCustomView() throws Exception
    {
        testEditView("testEditCustomView", "Edit_Filter_Custom_View");
    }

    /**
     * Test modifying filters used in the default view.
     * @see GridPanelViewTest#testEditView(String, String)
     */
    @Test
    public void testEditDefaultView() throws Exception
    {
        testEditView("testEditDefaultView", "");
    }

    /**
     * <p>
     *     Modify an existing view.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a view with two filtered columns.</li>
     *         <li>Remove one of the filters and validate grid view is in edited mode (dirty bit flipped).</li>
     *         <li>Validate 'Undo'.</li>
     *         <li>Add a new condition, a sort of a third column, to the view.</li>
     *         <li>Change one of the existing filters.</li>
     *         <li>Save the view.</li>
     *         <li>Change the sort order of the third column and verify dirty bit flipped.</li>
     *     </ul>
     * </p>
     * @param testName The name of the test, used for the screenshot prefix.
     * @param viewName What to name the saved view. If empty string will be default view.
     */
    private void testEditView(String testName, String viewName) throws Exception
    {

        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_COLUMNS);

        String filterCol1 = COL_STRING1;
        String filterValue1 = stringSetMembers.get(2);
        String expectedFilter1Text = String.format("%s Contains %s", filterCol1, filterValue1);

        String filterCol2 = COL_BOOL;
        String expectedFilter2Text = String.format("%s = true", filterCol2);

        Map<String, Boolean> expectedFilterPills = new HashMap<>();
        expectedFilterPills.put(expectedFilter1Text, true);
        expectedFilterPills.put(expectedFilter2Text, true);

        log(String.format("For sample type '%s' filter columns '%s' and '%s' column.", DEFAULT_VIEW_SAMPLE_TYPE, filterCol1, filterCol2));

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        log(String.format("Filter field '%s' for value '%s'.", filterCol1, filterValue1));
        filterDialog.selectField(filterCol1);
        FilterExpressionPanel expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.CONTAINS, filterValue1));

        log(String.format("Filter field '%s' for 'true'.", filterCol2));
        filterDialog.selectField(filterCol2);
        expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.EQUAL, true));

        filterDialog.confirm();

        SaveViewDialog saveViewDialog;

        if(viewName.isEmpty())
        {
            log("Save as default view.");
            saveViewDialog = grid.saveView();
            saveViewDialog.setMakeDefault()
                    .saveView();
        }
        else
        {

            // Issue: 46019 Cannot save a grid view if a non-string column is filtered. Only happens on components page (core-components.view).

            log(String.format("Save view as '%s'.", viewName));
            saveViewDialog = grid.saveView();
            saveViewDialog.setMakeCustom()
                    .setViewName(viewName)
                    .setMakeShared(false)
                    .saveView();

            savedViewsForDefaultSampleType.add(viewName);
        }

        int rowCountBefore = grid.getRows().size();

        log("Now that the filters have been save as a view validate filter pills.");
        validateFilterPills(testName, grid, expectedFilterPills);

        log(String.format("Remove the filter '%s' and validate grid is now in '%s' mode.", expectedFilter1Text, EDITED_ALERT));
        grid.removeFilter(expectedFilter1Text);

        // On MSSQL/Windows grid.getRows isn't always updated, pause just a moment to let the test code catch up.
        sleep(500);

        validateGridHeader(testName, grid, EDITED_ALERT, true);

        // Wait until grid.getRows().size() gets an updated count.
        checker().addRecordableErrorType(WebDriverException.class);
        checker().wrapAssertion(()->
                waitFor(()->grid.getRows().size() > rowCountBefore,
                        "Filter was removed but number of rows in grid did not increase.", 5_000));

        // Use different screenshot name based on view type.
        checker().screenShotIfNewError(viewName.isEmpty() ?
                String.format("%s_Remove_Filter_Default_Error", testName) :
                String.format("%s_Remove_Filter_Custom_View_Error", testName));

        log("Validate 'Undo' resets as expected.");

        grid.clickUndoButton();

        sleep(500);

        log("Validate filter pills go back to expected state.");
        validateFilterPills(testName, grid, expectedFilterPills);

        log("Validate header has no edit status or buttons after 'Undo'.");
        validateGridHeader(testName, grid, "", false);

        checker().verifyEquals("Number of rows after clicking 'Undo' not as expected'.",
                rowCountBefore, grid.getRows().size());

        checker().screenShotIfNewError(viewName.isEmpty() ?
                String.format("%s_Undo_Remove_Filter_Default_Error", testName) :
                String.format("%s_Undo_Remove_Filter_Custom_Error", testName));

        log("Modify the view.");

        String sortColumn = COL_INT;
        log(String.format("Add a new condition by sorting the '%s' column.", sortColumn));
        grid.sortColumn(sortColumn, SortDirection.ASC);

        log(String.format("Modify an existing filter. Change '%s' to FALSE.", filterCol2));

        // To update a filter that has been applied by a view the filter must first be removed, then added back with the changes.
        grid.removeFilter(expectedFilter2Text);

        sleep(500);

        filterDialog = grid.getGridBar().openFilterDialog();
        filterDialog.selectField(filterCol2);
        expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.EQUAL, false));
        filterDialog.confirm();

        expectedFilter2Text = String.format("%s = false", filterCol2);

        checker().verifyEqualsSorted("Filter pills not as expected after modifying existing filters.",
                List.of(expectedFilter1Text, expectedFilter2Text), grid.getFilterStatusValuesText());

        validateGridHeader(testName, grid, EDITED_ALERT, true);

        checker().screenShotIfNewError(String.format("%s_Modify_Existing_Error", testName));

        checker().fatal()
                .verifyTrue("Changes did not trigger the dirty bit for the view. Fatal error.",
                        validateGridHeader(testName, grid, EDITED_ALERT, true));

        log("Save the changes.");

        if(viewName.isEmpty())
        {
            saveViewDialog = grid.clickSaveButton(true);

            log("Verify that view name is empty and 'default checkbox' is checked.");

            checker().verifyTrue(String.format("Value of view name is not empty. Contains '%s'.", saveViewDialog.getViewName()),
                    saveViewDialog.getViewName().isEmpty());

            // The default checkbox is not checked by default. Maybe a bug?
            log("Checking the 'Default for all' checkbox.");
            saveViewDialog.setMakeDefault();

            saveViewDialog.saveView();
        }
        else
        {
            grid.clickSaveButton(false);

            log("Because this is already a saved view verify that no save dialog is shown.");

            sleep(1_000);
            if(!checker().verifyFalse("Looks like a modal dialog was shown after clicking the locked filter pill.",
                    isElementPresent(Locator.tagWithClass("div", "modal-dialog"))))
            {
                saveViewDialog = new SaveViewDialog(getDriver(), grid);
                checker().screenShotIfNewError(String.format("%s_Unexpected_Save_View_Dialog", testName));

                // If the save view dialog unexpectedly popped up try and save the view.
                if(saveViewDialog.getViewName().isEmpty())
                    saveViewDialog.setViewName(viewName);

                saveViewDialog.setMakeCustom()
                        .saveView();
            }

        }

        log(String.format("Verify that changing the sort order of column '%s' causes the grid view to go to '%s'.",
                sortColumn, EDITED_ALERT));

        grid.sortColumn(sortColumn, SortDirection.DESC);

        validateGridHeader(testName, grid, EDITED_ALERT, true);

        // Save the view otherwise it will have (edited) as a title.
        log("Validate that using 'Save View' from the menu populates the view name field in the dialog.");
        saveViewDialog = grid.saveView();

        if(viewName.isEmpty())
        {
            checker().withScreenshot(String.format("%s_Default_Dialog_Error", testName))
                    .verifyTrue(String.format("For the default view the view name filed in the save view dialog should be empty, it contains '%s'.", saveViewDialog.getViewName()),
                            saveViewDialog.getViewName().isEmpty());

            // In the core-components.view page the checkbox is not checked by default.
            log("Checking 'Make default for all'.");
            saveViewDialog.setMakeDefault();

        }
        else
        {
            if (!checker().withScreenshot(String.format("%s_Name_Missing_Error", testName))
                    .verifyEquals("The view name filed in the save view dialog was not populated as expected.", viewName, saveViewDialog.getViewName()))
            {
                log("View name was not set int he save dialog, need to set it.");
                saveViewDialog.setViewName(viewName);
            }
        }

        log("Save the view.");
        saveViewDialog.saveView();

    }

    /**
     * <p>
     *     Test using "tricky" characters in a view name.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Save a view with tricky characters (extended characters, quotes, and so on).</li>
     *         <li>Validate view name is displayed correctly in the grid panel header.</li>
     *         <li>Validate view name is listed correctly in the 'Manage Views' dialog.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testSaveViewTrickyName() throws Exception
    {

        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_COLUMNS);

        String hideCol = COL_INT;

        log(String.format("Hide column '%s' in sample type '%s'.", hideCol, DEFAULT_VIEW_SAMPLE_TYPE));
        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);
        grid.hideColumn(hideCol);
        SaveViewDialog saveViewDialog = grid.saveView();

        String trickyViewName = String.format("My View %s", TRICKY_CHARACTERS);
        log(String.format("Save the view name as '%s'.", trickyViewName));

        saveViewDialog.setViewName(trickyViewName)
                .saveView();

        // Add this view name to the list of views.
        savedViewsForDefaultSampleType.add(trickyViewName);

        checker().withScreenshot("Tricky_Name_Error")
                .verifyEquals("View name on grid not as expected.",
                        String.format(PANEL_VIEW_NAME_PREFIX, trickyViewName), grid.getViewName());

        log("Validate the 'Manage Views' dialog.");

        List<String> expectedValues = new ArrayList<>(savedViewsForDefaultSampleType);
        expectedValues.add(String.format("%s View (shared)", VIEW_DEFAULT));
        ManageViewsDialog manageViewsDialog = grid.manageViews();
        List<String> actualValues = manageViewsDialog.getViewNames();

        checker().withScreenshot("Tricky_Name_Manage_Error")
                .verifyEqualsSorted("List of views in manage dialog not as expected.",
                        expectedValues, actualValues);

        manageViewsDialog.dismiss();
    }

    /**
     * <p>
     *     Test that adding a field/column to grid puts it in the expected position.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Add a field to a grid in a specific location.</li>
     *         <li>Validate added icons (checkmarks) in Available Fields panel on the dialog.</li>
     *         <li>Validate expected column is highlighted in the 'Shown in Grid' panel in the dialog.</li>
     *         <li>Adding a field puts the field in the expected location in the dialog and in the grid.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testFieldInsertionOrder() throws Exception
    {
        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        // Removing the column and then testing the removal is redundant with other tests, however it will be used to
        // validate other parts of the dialog that is not covered in other tests.
        String columnToAdd = COL_INT;
        log(String.format("Use the grid menu to hide column '%s'.", columnToAdd));
        grid.hideColumn(columnToAdd);
        grid.clickSaveButton(true)
                .setMakeDefault()
                .saveView();

        List<String> expectedFields = new ArrayList<>(DEFAULT_COLUMNS);
        expectedFields.remove(columnToAdd);

        Map<String, Integer> expectedColumns = new HashMap<>(defaultColumnState);
        expectedColumns.remove(columnToAdd);

        checker().fatal()
                .verifyTrue("Columns not as expected need this to pass for the other test. Fatal error.",
                        validateGridColumns("Dialog_Test", grid, expectedColumns));

        String selectedColumn = COL_STRING2;
        log(String.format("Click the grid menu above column '%s' and validate that insertion happens to the right of the column.", selectedColumn));
        FieldSelectionDialog customizeModal = grid.insertColumn(selectedColumn);

        log("Validate that the 'Available Fields' and 'Shown in Grid' panels are as expected.");

        checker().verifyEquals(String.format("Column '%s' should be selected in the dialog, it is not.", selectedColumn),
                selectedColumn, customizeModal.getActiveSelectedFieldLabel());

        checker().verifyEqualsSorted("Field displayed in 'Show in Grid' panel not as expected.",
                expectedFields, customizeModal.getSelectedFieldLabels());

        for (String field : expectedFields)
        {
            checker().verifyTrue(String.format("Field '%s' is not shown as already added in the 'Available Fields' panel.", field),
                    customizeModal.isAvailableFieldSelected(field));
        }

        checker().verifyFalse(String.format("Field '%s' should not be shown as already added in the 'Available Fields' panel.", columnToAdd),
                customizeModal.isAvailableFieldSelected(columnToAdd));

        log(String.format("Add field '%s'.", columnToAdd));
        customizeModal.selectAvailableField(columnToAdd);

        checker().verifyTrue(String.format("Field '%s' should now be shown as added in the 'Available Fields' panel, it does not.", columnToAdd),
                customizeModal.isAvailableFieldSelected(columnToAdd));

        log("Validate that the order of the fields in the 'Shown in Grid' column are as expected.");
        expectedFields = MULTI_CHOICE_ENABLED
                ? List.of(COL_NAME, COL_STRING1, COL_STRING2, COL_INT, COL_BOOL, COL_MULTITEXTCHOICE)
                : List.of(COL_NAME, COL_STRING1, COL_STRING2, COL_INT, COL_BOOL);
        checker().verifyEquals(String.format("After adding '%s' fields displayed in 'Show in Grid' panel not as expected.", columnToAdd),
                expectedFields, customizeModal.getSelectedFieldLabels());

        checker().screenShotIfNewError("InsertionOrder_Customize_Dialog_Error");
        customizeModal.clickUpdateGrid();

        log(String.format("Validate that the order of the fields in the grid, specifically that '%s' is after '%s'.", columnToAdd, selectedColumn));
        List<String> columns = grid.getColumnLabels();
        checker().verifyTrue("Order of column headers in grid is not as expected.",
                Collections.indexOfSubList(columns, Arrays.asList(selectedColumn, columnToAdd)) >= 0);

    }

    /**
     * <p>
     *     Test the 'Show all' checkbox, editing a label and clicking the 'Undo' button.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate that a hidden field is shown as available after checking the 'Show all' check box.</li>
     *         <li>Will expand and add a sub field from the newly visible field. The field will have the same label, 'Name', as a field already added to the grid.</li>
     *         <li>Validate that clicking the 'Undo' button removes the new field.</li>
     *         <li>Add the field again, change the label.</li>
     *         <li>Validate the updated label is shown in the grid.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testShowAllLabelEditAndUndo() throws Exception
    {
        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        FieldSelectionDialog customizeModal = grid.customizeView();

        checker().verifyFalse("The 'Update' button is enabled, it should not be.",
                customizeModal.isUpdateGridEnabled());

        checker().verifyFalse("The 'Undo edits' button is enabled, it should not be.",
                customizeModal.isUndoEditsEnabled());

        log("Validate that using the menu to open the dialog results in no fields being selected in the 'Shown in Grid' panel.");
        checker().verifyTrue(String.format("Field '%s' is selected in the 'Shown in Grid' panel, there should be no selected fields.", customizeModal.getActiveSelectedFieldLabel()),
                customizeModal.getActiveSelectedFieldLabel().isEmpty());

        if (!checker().verifyFalse("The 'Show all system and user-defined fields' should not be checked.",
                customizeModal.isShowAllChecked()))
        {
            log("Uncheck the 'Show all' option.");
            customizeModal.setShowAll(false);
        }

        String materialIDField = "MaterialSourceId";
        String materialIDFieldName = "Material Source Id";
        log(String.format("Validate that field '%s' is not visible before checking 'Show all'.", materialIDField));

        List<String> actualFields = customizeModal.getAvailableFieldLabels();

        checker().fatal()
                .verifyFalse(String.format("Field '%s' is already present in 'Available Fields' panel. Fatal error.", materialIDField),
                        actualFields.contains(materialIDField));

        log(String.format("Check 'Show all' and validate that '%s' is now listed.", materialIDField));

        customizeModal.setShowAll(true);

        actualFields = customizeModal.getAvailableFieldLabels();

        checker().verifyTrue(String.format("Field '%s' is not present in 'Available Fields' panel, it should be.", materialIDFieldName),
                actualFields.contains(materialIDFieldName));

        String materialNameField = "Name";
        log(String.format("Select the '%s' field under '%s' and add it to the grid.", materialNameField, materialIDField));
        customizeModal.selectAvailableField(materialIDField, materialNameField);

        List<String> expectedFields = new ArrayList<>(DEFAULT_COLUMNS);
        expectedFields.add(materialNameField);

        log("Because no fields should be selected validate this field is added to the end of the list.");
        checker().verifyEquals(String.format("Position of field '%s' is not as expected in the dialog.", materialNameField),
                expectedFields, customizeModal.getSelectedFieldLabels());

        checker().screenShotIfNewError("ShowAll_Label_Edit_Dialog_Error");

        if(checker().verifyTrue("The 'Undo edits' button is not enabled it should be.", customizeModal.isUndoEditsEnabled()))
        {
            log("Undo the edits.");
            customizeModal.clickUndoEdits();
            expectedFields = new ArrayList<>(DEFAULT_COLUMNS);
            if(checker().verifyEquals("After clicking 'Undo edits' fields in 'Shown in Grid' dialog not as expected.",
                    expectedFields, customizeModal.getSelectedFieldLabels()))
            {
                log(String.format("Add field '%s / %s' back.", materialIDField, materialNameField));
                customizeModal.selectAvailableField(materialIDField, materialNameField);
            }

        }
        else
        {
            checker().screenShotIfNewError("Undo_Edit_Button_Error");
        }

        String newFieldLabel = String.format("My New Label %s", materialNameField);
        log(String.format("Change the label of the field '%s' to '%s'.", materialNameField, newFieldLabel));

        // Adding the 'Material Source Id / Name' field creates two fields with the label 'Name' in the 'Shown in Grid' panel, make sure the expected one is updated.
        customizeModal.setFieldLabel(FieldKey.fromParts(materialIDField, materialNameField), newFieldLabel);

        checker().fatal().verifyTrue("'Update' button is not enabled, cannot save changes. Fatal error.",
                customizeModal.isUpdateGridEnabled());

        customizeModal.clickUpdateGrid();

        log("Validate that the grid shows the new field with the updated label.");

        checker().verifyTrue(String.format("Did not find the field labeled '%s' in the grid.", newFieldLabel),
                grid.getColumnLabels().contains(newFieldLabel));

    }

    /**
     * <p>
     *     Test the Manage Views dialog.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate that the list of views in the manage views dialog is as expected.</li>
     *         <li>Validate that all but the default view can be edited or deleted.</li>
     *         <li>Change the current view name.</li>
     *         <li>Delete the current view and verify grid shows the default view.</li>
     *         <li>Save a new view with a duplicate name (case insensitive).</li>
     *         <li>As a different user validate views listed in dialog.</li>
     *         <li>Validate different user can save a view with a name that is the same as a different users view name.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testManageViews() throws Exception
    {
        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        FieldSelectionDialog customizeModal = grid.customizeView();

        String fieldRemoved1 = COL_INT;

        log(String.format("Remove field '%s' using the dialog.", fieldRemoved1));

        customizeModal.removeFieldFromSelected(fieldRemoved1).clickUpdateGrid();

        String viewName1 = String.format("No %s", fieldRemoved1);
        log(String.format("Use the 'Save' button on the grid to save the view as '%s'.", viewName1));
        SaveViewDialog saveViewDialog = grid.clickSaveButton(true);
        saveViewDialog.setViewName(viewName1)
                .saveView();

        log(String.format("Go back to '%s' and create a new view.", VIEW_DEFAULT));
        grid.selectView(VIEW_DEFAULT);

        customizeModal = grid.customizeView();

        String fieldRemoved2 = COL_STRING2;

        log(String.format("Remove field '%s' using the dialog.", fieldRemoved2));

        customizeModal.removeFieldFromSelected(fieldRemoved2).clickUpdateGrid();

        String viewName2 = String.format("No %s", fieldRemoved2);
        log(String.format("Use the menu to save the view as '%s'. This is now the current view.", viewName2));
        saveViewDialog = grid.saveView();
        saveViewDialog.setViewName(viewName2)
                .saveView();

        ManageViewsDialog manageViewsDialog = grid.manageViews();

        List<String> expectedValues = new ArrayList<>(List.of(String.format("%s View (shared)", VIEW_DEFAULT), viewName1, viewName2));
        List<String> actualValues = manageViewsDialog.getViewNames();
        checker().fatal()
                .verifyEqualsSorted("Views names in 'Manage Views' dialog not as expected. Fatal error.",
                        expectedValues, actualValues);

        log("Validate state of views.");
        for(String viewName : expectedValues)
        {
            if(viewName.contains(VIEW_DEFAULT))
            {
                checker().verifyFalse(String.format("View '%s' can be deleted, that should not be.", viewName),
                        manageViewsDialog.canViewBeDeleted(viewName));
                checker().verifyFalse(String.format("View '%s' can be edited, that should not be.", viewName),
                        manageViewsDialog.canViewNameBeChanged(viewName));
            }
            else
            {
                checker().verifyTrue(String.format("View '%s' cannot be deleted, it should be.", viewName),
                        manageViewsDialog.canViewBeDeleted(viewName));
                checker().verifyTrue(String.format("View '%s' cannot be edited, it should be.", viewName),
                        manageViewsDialog.canViewNameBeChanged(viewName));
            }
        }

        checker().screenShotIfNewError("Manage_Views_Default_Error");

        String newViewName2 = String.format("My New View %s", viewName2);
        log(String.format("Change the current view name '%s' to '%s'.", viewName2, newViewName2));

        manageViewsDialog.changeViewName(viewName2, newViewName2);

        manageViewsDialog.dismiss();

        log(String.format("Verify that the view name is now updated to '%s'.", newViewName2));

        // Using the components view adds 'GridPanel - ' to the panel header. Need to take that into account.
        checker().verifyEquals("View name in panel header not as expected.",
                String.format(PANEL_VIEW_NAME_PREFIX, newViewName2), grid.getViewName());

        checker().screenShotIfNewError("Updated_View_Name_Error");

        log(String.format("Delete the current view '%s'.", newViewName2));
        manageViewsDialog = grid.manageViews();
        manageViewsDialog.deleteView(newViewName2);

        checker().withScreenshot("Delete_Confirmation_Text_Error")
                .verifyEquals("Delete confirmation text is not as expected.",
                        "Permanently remove this view?", manageViewsDialog.getDeleteConfirmationText());

        log("Check that clicking 'No' does not change the view names.");
        manageViewsDialog.cancelDelete();
        expectedValues = new ArrayList<>(List.of(String.format("%s View (shared)", VIEW_DEFAULT), viewName1, newViewName2));
        actualValues = manageViewsDialog.getViewNames();
        checker().withScreenshot("Delete_No_Error")
                .verifyEqualsSorted("Clicking 'No' did not reset the displayed view names.",
                        expectedValues, actualValues);

        log(String.format("Again delete the current view '%s' and click 'Yes' button.", newViewName2));
        manageViewsDialog.deleteView(newViewName2);
        manageViewsDialog.confirmDelete();
        expectedValues.remove(newViewName2);
        actualValues = manageViewsDialog.getViewNames();
        checker().withScreenshot("Delete_Yes_Error")
                .verifyEqualsSorted("After clicking 'Yes' list of view names is not as expected.",
                        expectedValues, actualValues);

        log("Dismiss the dialog and validate that the grid view goes back to the default view.");
        manageViewsDialog.dismiss();

        checker().withScreenshot("Delete_Current_View_Title_Error")
                .verifyEquals("View name in panel header does not appear to be the default view.",
                        "GridPanel", grid.getViewName());

        log("Validate column headers.");
        validateGridColumns("Delete_Current_View", grid, defaultColumnState);

        log("Validate saving a view with an existing views name causes an error (case insensitive).");
        grid.hideColumn(COL_BOOL);
        saveViewDialog = grid.saveView();
        saveViewDialog.setViewName(viewName1.toLowerCase());
        String errorMsg = saveViewDialog.saveViewExpectingError();

        checker().withScreenshot("Duplicate_View_Name_Error")
                .verifyEquals("Error message when saving a duplicate name is not as expected.",
                        String.format("There was a problem saving the view for the data grid. A saved view by the name \"%s\" already exists.", viewName1.toLowerCase()),
                        errorMsg);

        String viewName3 = "No Bool";
        log(String.format("Change view name to something new, '%s' and validate save works as expected.", viewName3));
        saveViewDialog.setViewName(viewName3);
        saveViewDialog.saveView();

        checker().withScreenshot("New_View_Name_Error")
                .verifyEquals("View name in panel header not as expected.",
                        String.format(PANEL_VIEW_NAME_PREFIX, viewName3), grid.getViewName());

        log("Validate a manage view with a different user.");
        impersonate(OTHER_USER);

        goToProjectHome();

        grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        manageViewsDialog = grid.manageViews();
        expectedValues = new ArrayList<>(List.of(String.format("%s View (shared)", VIEW_DEFAULT)));
        actualValues = manageViewsDialog.getViewNames();
        checker().withScreenshot("Manage_Views_List_Other_User_Error")
                .verifyEqualsSorted(String.format("List of view in 'Manage Views' dialog not as expected for user '%s'.", OTHER_USER),
                        expectedValues, actualValues);

        manageViewsDialog.dismiss();

        log(String.format("Validate that this user can save a view with the same name '%s' as used by another user.", viewName1));
        grid.hideColumn(fieldRemoved1);
        saveViewDialog = grid.saveView();
        saveViewDialog.setViewName(viewName1);
        saveViewDialog.saveView();

        manageViewsDialog = grid.manageViews();
        expectedValues.add(viewName1);
        actualValues = manageViewsDialog.getViewNames();
        checker().withScreenshot("Manage_Views_Saved_Other_User_Error")
                .verifyEqualsSorted(String.format("List of view in 'Manage Views' dialog not as expected for user '%s' after saving view '%s'.", OTHER_USER, viewName1),
                        expectedValues, actualValues);

        manageViewsDialog.dismiss();

        stopImpersonating();

    }

    @Test
    public void testRemoveAllFields() throws Exception
    {

        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        log("Remove all the fields.");

        FieldSelectionDialog customizeModal = grid.customizeView();
        customizeModal.removeAllSelectedFields();

        checker().verifyTrue("'Undo edits' is not enabled after removing all fields, it should be.",
                customizeModal.isUndoEditsEnabled());

        checker().verifyFalse("'Update Grid' should not be enabled after all fields are updated.",
                        customizeModal.isUpdateGridEnabled());

        checker().screenShotIfNewError("Remove_All_Dialog_Error");

        log("Add a field back and validate.");

        customizeModal.selectAvailableField(COL_STRING1)
                .clickUpdateGrid();

        checker().verifyEquals("Grid columns not as expected after removing all, and adding back a field.",
                List.of(COL_STRING1), grid.getColumnLabels());

    }

    // Issue 52661 Saved grid view with an invalid date filter can't be edited or deleted
    @Test
    public void testWarningOnInvalidDateFilter() throws Exception
    {
        String viewName = "broken view";
        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        log("Save a view, call it 'broken view'");
        grid.saveView()
                .setViewName(viewName)
                .setMakeShared(false)
                .saveView();

        log("add created column so we can filter on it");
        grid.customizeView()
                .selectAvailableField("Created")
                .clickUpdateGrid();

        log("filter on a valid date");
        grid.filterColumn("Created", Filter.Operator.GT, new FilterExpressionPanel.DateString("May 27, 2024"));
        var filterStatuses = grid.getFilterStatusValues();
        checker().withScreenshot("Filter_Texts_Error")
                .wrapAssertion(()-> Assertions.assertThat(filterStatuses)
                        .hasSize(1)
                        .filteredOn(a-> a.getText().equals("Created > 2024-05-27"))
                                .isNotEmpty());
        grid.saveView().saveView();

        log("try to filter on an invalid date");
        grid.filterColumn("Created", Filter.Operator.GT, new FilterExpressionPanel.DateString("05/37/2024"));
        // don't expect the parser to get the invalid date right; current behavior won't do that
        // also add a bogus filter on int column, verify it refuses to do it
        var err = grid.filterColumnExpectingError("Int", Filter.Operator.GT, "XYZ");
        checker().verifyEquals("expect error when trying to configure invalid filter",
                "Missing filter values for: Int.", err);
        grid.saveView().saveView();

        log("ensure the view can be edited after");
        grid.filterColumn("Created", Filter.Operator.LTE, new Date());  // filter on right now
        grid.saveView().saveView();

        grid.manageViews()      // ensure the view can be deleted now
                .revertDefaultView()
                .deleteView(viewName)
                .confirmDelete()
                .dismiss("Done");
    }

    @Test
    public void testCustomGridViewsMVTCtoTC() throws Exception
    {
        Assume.assumeTrue("Multi-choice text fields are only supported on PostgreSQL", MULTI_CHOICE_ENABLED);
        goToProjectHome();
        resetFieldToMVTC();
        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_COLUMNS);

        log("Compute expected sample data for MVTC conversion test.");
        Map<String, String> allSamples = createSampleMVTCMap();
        String filterValue  = TEXT_MULTI_CHOICE_LIST.get(0);
        String filterValue2 = TEXT_MULTI_CHOICE_LIST.get(1);

        Map<String, String> emptyMap          = filterSampleMap(allSamples, v -> v == null);
        Map<String, String> nonemptyMap        = filterSampleMap(allSamples, v -> v != null);
        Map<String, String> containsAnyMap     = filterSampleMap(allSamples, v -> filterValue.equals(v) || filterValue2.equals(v));
        Map<String, String> containsExactMap   = filterSampleMap(allSamples, v -> filterValue.equals(v));
        Map<String, String> containsNotEqMap   = filterSampleMap(allSamples, v -> v == null || !filterValue.equals(v));
        Map<String, String> containsNoneMap    = filterSampleMap(allSamples, v -> v == null || (!filterValue.equals(v) && !filterValue2.equals(v)));

        List<GridMVTCCase> cases = List.of(
                new GridMVTCCase("MVTC→TC Grid - Is Empty", Filter.Operator.ARRAY_ISEMPTY, new String[0],
                        GridConversionResult.kept(Filter.Operator.ISBLANK.getDisplayValue()), emptyMap),
                new GridMVTCCase("MVTC→TC Grid - Contains Any", Filter.Operator.ARRAY_CONTAINS_ANY, new String[]{filterValue, filterValue2},
                        GridConversionResult.kept(Filter.Operator.IN.getDisplayValue()), containsAnyMap),
                new GridMVTCCase("MVTC→TC Grid - Contains All", Filter.Operator.ARRAY_CONTAINS_ALL, new String[]{filterValue, filterValue2},
                        GridConversionResult.dropped(), containsAnyMap),
                new GridMVTCCase("MVTC→TC Grid - Is Not Empty", Filter.Operator.ARRAY_ISNOTEMPTY, new String[0],
                        GridConversionResult.kept(Filter.Operator.NONBLANK.getDisplayValue()), nonemptyMap),
                new GridMVTCCase("MVTC→TC Grid - Contains Not Exact", Filter.Operator.ARRAY_CONTAINS_NOT_EXACT, new String[]{filterValue},
                        GridConversionResult.kept(filterValue), containsNotEqMap),
                new GridMVTCCase("MVTC→TC Grid - Contains Exact", Filter.Operator.ARRAY_CONTAINS_EXACT, new String[]{filterValue},
                        GridConversionResult.kept(filterValue), containsExactMap),
                new GridMVTCCase("MVTC→TC Grid - Contains None", Filter.Operator.ARRAY_CONTAINS_NONE, new String[]{filterValue, filterValue2},
                        GridConversionResult.kept(filterValue), containsNoneMap)
        );

        log("Creating saved grid views with all MVTC operators on column '" + COL_MULTITEXTCHOICE + "'.");
        List<String> viewNames = cases.stream().map(GridMVTCCase::viewName).toList();
        for (GridMVTCCase c : cases)
            createMVTCGridView(c.viewName(), c.op(), c.filterVals());

        log("Converting '" + COL_MULTITEXTCHOICE + "' field of " + DEFAULT_VIEW_SAMPLE_TYPE + " from MVTC to TextChoice.");
        enableMVTCFieldMultiSelect(false);

        log("Verifying saved grid views after MVTC → TC conversion.");
        verifyGridMVTCCases(cases);
        checker().screenShotIfNewError("GridMVTCtoTC_Error");

        log("Cleaning up MVTC→TC test views.");
        cleanupGridViews(beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE), viewNames);
    }

    @Test
    public void testCustomGridViewsTCtoMVTC() throws Exception
    {
        Assume.assumeTrue("Multi-choice text fields are only supported on PostgreSQL", MULTI_CHOICE_ENABLED);
        goToProjectHome();
        resetFieldToMVTC();
        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_COLUMNS);

        log("Converting '" + COL_MULTITEXTCHOICE + "' field of " + DEFAULT_VIEW_SAMPLE_TYPE + " from MVTC to TextChoice for TC→MVTC test setup.");
        enableMVTCFieldMultiSelect(false);

        log("Compute expected sample data for TC conversion test.");
        Map<String, String> allSamples = createSampleMVTCMap();
        String filterValue  = TEXT_MULTI_CHOICE_LIST.get(0);
        String filterValue2 = TEXT_MULTI_CHOICE_LIST.get(1);

        Map<String, String> emptyMap        = filterSampleMap(allSamples, v -> v == null);
        Map<String, String> nonemptyMap      = filterSampleMap(allSamples, v -> v != null);
        Map<String, String> equalsMap        = filterSampleMap(allSamples, v -> filterValue.equals(v));
        Map<String, String> equalsAnyMap     = filterSampleMap(allSamples, v -> filterValue.equals(v) || filterValue2.equals(v));
        Map<String, String> notEqualsMap     = filterSampleMap(allSamples, v -> v == null || !filterValue.equals(v));

        List<GridTCCase> cases = List.of(
                new GridTCCase("TC→MVTC Grid - Is Blank", Filter.Operator.ISBLANK, new String[0],
                        Filter.Operator.ARRAY_ISEMPTY.getDisplayValue(), emptyMap),
                new GridTCCase("TC→MVTC Grid - Is Not Blank", Filter.Operator.NONBLANK, new String[0],
                        Filter.Operator.ARRAY_ISNOTEMPTY.getDisplayValue(), nonemptyMap),
                new GridTCCase("TC→MVTC Grid - Equals", Filter.Operator.EQUAL, new String[]{filterValue},
                        Filter.Operator.ARRAY_CONTAINS_EXACT.getDisplayValue(), equalsMap),
                new GridTCCase("TC→MVTC Grid - Equals One Of", Filter.Operator.IN, new String[]{filterValue, filterValue2},
                        Filter.Operator.ARRAY_CONTAINS_ANY.getDisplayValue(), equalsAnyMap),
                new GridTCCase("TC→MVTC Grid - Does Not Equal", Filter.Operator.NEQ, new String[]{filterValue},
                        filterValue, notEqualsMap),
                new GridTCCase("TC→MVTC Grid - Equals None Of", Filter.Operator.NOT_IN, new String[]{filterValue},
                        filterValue, notEqualsMap)
        );

        log("Creating saved grid views with all TC operators on column '" + COL_MULTITEXTCHOICE + "'.");
        List<String> viewNames = cases.stream().map(GridTCCase::viewName).toList();
        for (GridTCCase c : cases)
            createTCGridView(c.viewName(), c.op(), c.filterVals());

        log("Converting '" + COL_MULTITEXTCHOICE + "' field of " + DEFAULT_VIEW_SAMPLE_TYPE + " back to MultiValueTextChoice.");
        enableMVTCFieldMultiSelect(true);

        log("Verifying saved grid views after TC → MVTC conversion.");
        verifyGridTCCases(cases);
        checker().screenShotIfNewError("GridTCtoMVTC_Error");

        log("Cleaning up TC→MVTC test views.");
        cleanupGridViews(beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE), viewNames);
    }

    @Test
    public void testCustomGridViewsMVTCtoText() throws Exception
    {
        Assume.assumeTrue("Multi-choice text fields are only supported on PostgreSQL", MULTI_CHOICE_ENABLED);
        goToProjectHome();
        resetFieldToMVTC();
        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_COLUMNS);

        log("Create expected sample data for MVTC→Text conversion test.");
        Map<String, String> allSamples = createSampleMVTCMap();
        String filterValue  = TEXT_MULTI_CHOICE_LIST.get(0);
        String filterValue2 = TEXT_MULTI_CHOICE_LIST.get(1);

        Map<String, String> emptyMap          = filterSampleMap(allSamples, v -> v == null);
        Map<String, String> nonemptyMap        = filterSampleMap(allSamples, v -> v != null);
        Map<String, String> containsAnyMap     = filterSampleMap(allSamples, v -> filterValue.equals(v) || filterValue2.equals(v));
        Map<String, String> containsExactMap   = filterSampleMap(allSamples, v -> filterValue.equals(v));
        Map<String, String> containsNotEqMap   = filterSampleMap(allSamples, v -> v == null || !filterValue.equals(v));
        Map<String, String> containsNoneMap    = filterSampleMap(allSamples, v -> v == null || (!filterValue.equals(v) && !filterValue2.equals(v)));

        List<GridMVTCCase> cases = List.of(
                new GridMVTCCase("MVTC→Str Grid - Is Empty", Filter.Operator.ARRAY_ISEMPTY, new String[0],
                        GridConversionResult.kept(Filter.Operator.ISBLANK.getDisplayValue()), emptyMap),
                new GridMVTCCase("MVTC→Str Grid - Contains Any", Filter.Operator.ARRAY_CONTAINS_ANY, new String[]{filterValue, filterValue2},
                        GridConversionResult.kept(Filter.Operator.IN.getDisplayValue()), containsAnyMap),
                new GridMVTCCase("MVTC→Str Grid - Contains All", Filter.Operator.ARRAY_CONTAINS_ALL, new String[]{filterValue, filterValue2},
                        GridConversionResult.dropped(), containsAnyMap),
                new GridMVTCCase("MVTC→Str Grid - Is Not Empty", Filter.Operator.ARRAY_ISNOTEMPTY, new String[0],
                        GridConversionResult.kept(Filter.Operator.NONBLANK.getDisplayValue()), nonemptyMap),
                new GridMVTCCase("MVTC→Str Grid - Contains Not Exact", Filter.Operator.ARRAY_CONTAINS_NOT_EXACT, new String[]{filterValue},
                        GridConversionResult.dropped(), containsNotEqMap),
                new GridMVTCCase("MVTC→Str Grid - Contains Exact", Filter.Operator.ARRAY_CONTAINS_EXACT, new String[]{filterValue},
                        GridConversionResult.dropped(), containsExactMap),
                new GridMVTCCase("MVTC→Str Grid - Contains None", Filter.Operator.ARRAY_CONTAINS_NONE, new String[]{filterValue, filterValue2},
                        GridConversionResult.kept(filterValue), containsNoneMap)
        );

        log("Creating saved grid views with all MVTC operators on column '" + COL_MULTITEXTCHOICE + "'.");
        List<String> viewNames = cases.stream().map(GridMVTCCase::viewName).toList();
        for (GridMVTCCase c : cases)
            createMVTCGridView(c.viewName(), c.op(), c.filterVals());

        log("Converting '" + COL_MULTITEXTCHOICE + "' field of " + DEFAULT_VIEW_SAMPLE_TYPE + " from MVTC to plain String.");
        changeMVTCFieldToText();

        log("Verifying saved grid views after MVTC → String conversion.");
        verifyGridMVTCCases(cases);
        checker().screenShotIfNewError("GridMVTCtoStr_Error");

        log("Cleaning up MVTC→String test views.");
        cleanupGridViews(beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE), viewNames);
    }

    /**
     * Builds a map of sample name → expected MVTC column value for DEFAULT_VIEW_SAMPLE_TYPE.
     * Samples with sampleId % 5 == 0 have a {@code null} (empty) value; others get one
     * value from {@link #TEXT_MULTI_CHOICE_LIST} determined by the hash of the sample name.
     */
    private static Map<String, String> createSampleMVTCMap()
    {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 1; i <= DEFAULT_VIEW_SAMPLE_TYPE_SIZE; i++)
        {
            String name = DEFAULT_VIEW_SAMPLE_PREFIX + i;
            String value = (i % 5 == 0) ? null
                    : TEXT_MULTI_CHOICE_LIST.get(Math.abs(name.hashCode()) % TEXT_MULTI_CHOICE_LIST.size());
            map.put(name, value);
        }
        return map;
    }

    /**
     * Returns a sub-map keeping only entries whose value satisfies {@code valuePredicate}.
     */
    private static Map<String, String> filterSampleMap(Map<String, String> allSamples,
                                                       Predicate<String> valuePredicate)
    {
        // Collectors.toMap rejects null values, so populate the map manually.
        Map<String, String> result = new LinkedHashMap<>();
        allSamples.entrySet().stream()
                .filter(e -> valuePredicate.test(e.getValue()))
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    /**
     * Navigates to DEFAULT_VIEW_SAMPLE_TYPE grid, applies an MVTC-type array filter via the
     * grid filter dialog, and saves the result as a personal named view.
     */
    private void createMVTCGridView(String viewName, Filter.Operator op, String... vals)
    {
        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);
        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();
        filterDialog.selectField(COL_MULTITEXTCHOICE);
        FilterFacetedPanel facetPanel = filterDialog.selectFacetTab();
        facetPanel.selectArrayFilterOperator(op);
        if (vals.length > 0)
            facetPanel.checkValues(vals);
        filterDialog.confirm();
        grid.saveView(viewName);
    }

    /**
     * Navigates to DEFAULT_VIEW_SAMPLE_TYPE grid (with TextChoice field), applies a TC filter,
     * and saves the result as a personal named view.
     * For {@link Filter.Operator#IN}, values are selected via the facet tab (supporting multiple
     * values); all other operators use the expression tab.
     */
    private void createTCGridView(String viewName, Filter.Operator op, String... vals)
    {
        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);
        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();
        filterDialog.selectField(COL_MULTITEXTCHOICE);
        if (op == Filter.Operator.IN)
        {
            FilterFacetedPanel filterFacetedPanel = filterDialog.selectFacetTab();
            filterFacetedPanel.uncheckValues("[All]");
            filterFacetedPanel.checkValues(vals);
        }
        else if (vals.length > 0)
        {
            filterDialog.selectExpressionTab().setFilter(new FilterExpressionPanel.Expression(op, vals[0]));
        }
        else
        {
            filterDialog.selectExpressionTab().setFilter(new FilterExpressionPanel.Expression(op));
        }
        filterDialog.confirm();

        grid.saveView(viewName);
    }

    /**
     * Toggles 'Allow Multiple Selections' on the COL_MULTITEXTCHOICE field of
     * DEFAULT_VIEW_SAMPLE_TYPE. Pass {@code false} for MVTC→TC, {@code true} for TC→MVTC.
     */
    private void enableMVTCFieldMultiSelect(boolean enable)
    {
        UpdateSampleTypePage updatePage = beginAtSampleTypesList(this, getProjectName())
                .goToEditSampleType(DEFAULT_VIEW_SAMPLE_TYPE);
        updatePage.getFieldsPanel()
                .getField(COL_MULTITEXTCHOICE)
                .expand()
                .setAllowMultipleSelections(enable, true);
        updatePage.clickSave();
    }

    /**
     * Changes the COL_MULTITEXTCHOICE field of DEFAULT_VIEW_SAMPLE_TYPE from MVTC
     * to plain String type (MVTC → Text conversion).
     */
    private void changeMVTCFieldToText()
    {
        UpdateSampleTypePage updatePage = beginAtSampleTypesList(this, getProjectName())
                .goToEditSampleType(DEFAULT_VIEW_SAMPLE_TYPE);
        updatePage.getFieldsPanel()
                .getField(COL_MULTITEXTCHOICE)
                .expand()
                .setType(FieldDefinition.ColumnType.String, true);
        updatePage.clickSave();
    }

    /**
     * Resets the COL_MULTITEXTCHOICE field of DEFAULT_VIEW_SAMPLE_TYPE to MultiValueTextChoice
     * if it is not already. Safe to call at the start of each MVTC conversion test to ensure
     * a known initial state.
     */
    private void resetFieldToMVTC()
    {
        UpdateSampleTypePage updatePage = beginAtSampleTypesList(this, getProjectName())
                .goToEditSampleType(DEFAULT_VIEW_SAMPLE_TYPE);
        DomainFieldRow fieldRow = updatePage.getFieldsPanel()
                .getField(COL_MULTITEXTCHOICE)
                .expand();

        if (fieldRow.getType().getLabel() == FieldDefinition.ColumnType.MultiValueTextChoice.getLabel() && fieldRow.getAllowMultipleSelections())
            return;

        fieldRow.setType(FieldDefinition.ColumnType.MultiValueTextChoice, false)
                .setTextChoiceValues(TEXT_MULTI_CHOICE_LIST)
                .setAllowMultipleSelections(true, true);
        updatePage.clickSave();
    }

    /**
     * Verifies all MVTC cases after a type conversion.
     * For kept filters: checks the filter pill text and (when row count fits on one page) data.
     * For dropped filters: checks that no filter pills are shown.
     */
    private void verifyGridMVTCCases(List<GridMVTCCase> cases)
    {
        for (GridMVTCCase c : cases)
        {
            if (c.conversionResult().isDropped())
                verifyDroppedGridView(c.viewName());
            else
            {
                QueryGrid grid = verifyGridViewFilter(c.viewName(), c.conversionResult().expectedFilterText());
                if (c.expectedSampleMap().size() <= DEFAULT_PAGE_SIZE)
                    TestArrayDataUtils.verifyMVTCResults(grid, c.expectedSampleMap(), COL_NAME, COL_MULTITEXTCHOICE, checker());
            }
        }
    }

    /**
     * Verifies all TC→MVTC cases: checks filter pill text and (when small enough) data.
     */
    private void verifyGridTCCases(List<GridTCCase> cases)
    {
        for (GridTCCase c : cases)
        {
            QueryGrid grid = verifyGridViewFilter(c.viewName(), c.expectedAfterConversionText());
            if (c.expectedSampleMap().size() <= DEFAULT_PAGE_SIZE)
                TestArrayDataUtils.verifyMVTCResults(grid, c.expectedSampleMap(), COL_NAME, COL_MULTITEXTCHOICE, checker());
        }
    }

    /**
     * Navigates to the DEFAULT_VIEW_SAMPLE_TYPE grid, selects the named view, and verifies
     * that at least one filter pill text contains {@code expectedFilterText}.
     *
     * @return the QueryGrid showing the named view, for further data assertions.
     */
    private QueryGrid verifyGridViewFilter(String viewName, String expectedFilterText)
    {
        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);
        grid.selectView(viewName);
        List<String> pillTexts = grid.getFilterStatusValuesText();
        checker().withScreenshot().verifyTrue(
                "View '" + viewName + "': filter pill should contain '" + expectedFilterText + "'",
                pillTexts.stream().anyMatch(pill -> pill.contains(expectedFilterText)));
        return grid;
    }

    /**
     * Verifies that a saved view whose filter was dropped after type conversion shows no
     * filter pills (i.e., the grid displays all samples without restriction).
     */
    private void verifyDroppedGridView(String viewName)
    {
        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);
        grid.selectView(viewName);
        checker().withScreenshot().verifyTrue(
                "View '" + viewName + "': dropped filter should not show any filter pills",
                grid.getFilterStatusValues().isEmpty());
    }

    /**
     * Deletes the named views from the DEFAULT_VIEW_SAMPLE_TYPE grid's Manage Views dialog.
     * Views that are no longer present (e.g., already deleted) are silently skipped.
     */
    private void cleanupGridViews(QueryGrid grid, List<String> viewNames)
    {
        ManageViewsDialog dialog = grid.manageViews();
        List<String> existingViews = dialog.getViewNames();
        for (String name : viewNames)
        {
            if (existingViews.stream().anyMatch(v -> v.equals(name)))
                dialog.deleteView(name).confirmDelete();
        }
        dialog.dismiss("Done");
    }

    /**
     * Helper to validate the 'Views' menu.
     *
     * @param screenShotPrefix A string to put at the start of the screenshot name (if taken).
     * @param grid A reference to the QueryGrid.
     * @param expectedMenuItems A list of the expected menu items.
     * @return True if no errors, false otherwise.
     */
    private boolean validateViewMenu(String screenShotPrefix, QueryGrid grid, List<String> expectedMenuItems)
    {

        List<String> actualValues = grid.getGridBar().getMenuText("Views");

        return checker().withScreenshot(String.format("%s_Views_Menu_Error", screenShotPrefix))
                .verifyEqualsSorted("Items under 'Views' menu not as expected.",
                        expectedMenuItems, actualValues);

    }

    /**
     * Helper to validate the columns in the grid.
     *
     * @param screenShotPrefix A string to put at the start of the screenshot name (if taken).
     * @param grid A reference to the QueryGrid.
     * @param expectedColumns A map of the expected columns. Key is the column label. The value indicates which icons, if any, should be present for the column.
     * @return True if no errors, false otherwise.
     */
    private boolean validateGridColumns(String screenShotPrefix, QueryGrid grid, Map<String, Integer> expectedColumns)
    {
        checker().setErrorMark();

        List<String> actualColumns = grid.getColumnLabels();

        checker().verifyEqualsSorted("Grid columns not as expected.",
                expectedColumns.keySet(), actualColumns);

        for(Map.Entry<String, Integer> entry : expectedColumns.entrySet())
        {

            if(actualColumns.contains(entry.getKey()))
            {
                if((entry.getValue() & 1) != 0)
                {
                    checker().verifyTrue(String.format("Column '%s' does not have the sorted icon.", entry.getKey()),
                            grid.hasColumnSortIcon(entry.getKey()));
                }

                if((entry.getValue() & 2) != 0)
                {
                    checker().verifyTrue(String.format("Column '%s' does not have the filtered icon.", entry.getKey()),
                            grid.hasColumnFilterIcon(entry.getKey()));
                }

            }

        }

        // If there was a failure screenShotIfNewError will return true (it took a screenshot), so need to 'not' the return value.
        return !checker().screenShotIfNewError(String.format("%s_Views_Menu_Column_Error", screenShotPrefix));
    }

    /**
     * Helper to validate the view edit controls at the top of the grid panel.
     *
     * @param screenShotPrefix A string to put at the start of the screenshot name (if taken).
     * @param grid A reference to the QueryGrid.
     * @param alertText Expected alert text. Empty string if none is expected.
     * @param hasSaveAndUndoButtons If the buttons are expected.
     * @return True if no errors, false otherwise.
     */
    private boolean validateGridHeader(String screenShotPrefix, QueryGrid grid, String alertText, boolean hasSaveAndUndoButtons)
    {
        checker().setErrorMark();

        String actualText = grid.getEditAlertText();

        if(!alertText.isEmpty())
        {
            checker().verifyEquals(String.format("Grid panel header does not show '%s' edit status.", alertText),
                    alertText, actualText);
        }
        else
        {
            checker().verifyTrue(String.format("Grid panel header has '%s' as the edit status. There should be no status.", actualText),
                    actualText.isEmpty());
        }

        if(hasSaveAndUndoButtons)
        {
            checker().verifyTrue("'Save' button not visible on the grid.",
                    grid.isSaveButtonVisible());

            checker().verifyTrue("'Undo' button is not visible on the grid.",
                    grid.isUndoButtonVisible());
        }
        else
        {
            checker().verifyFalse("'Save' button is visible on the grid, it should not be.",
                    grid.isSaveButtonVisible());

            checker().verifyFalse("'Undo' button is visible on the grid, it should not be.",
                    grid.isUndoButtonVisible());
        }

        // If there was a failure screenShotIfNewError will return true (it took a screenshot), so need to 'not' the return value.
        return !checker().screenShotIfNewError(String.format("%s_Grid_Header_Error", screenShotPrefix));
    }

    /**
     * Helper to validate the filter pills at the top of the grid.
     *
     * @param screenShotPrefix A string to put at the start of the screenshot name (if taken).
     * @param grid A reference to the QueryGrid.
     * @param expectedFiltersAndState A map of the expected filter pills. Key is the text of the expected filter pills. Value is true if the pill is expected to be locked.
     * @return True if no errors, false otherwise.
     */
    private boolean validateFilterPills(String screenShotPrefix, QueryGrid grid, Map<String, Boolean> expectedFiltersAndState)
    {

        checker().setErrorMark();

        List<String> expectedFilters = new ArrayList<>(expectedFiltersAndState.keySet());

        checker().withScreenshot(String.format("%s_Filter_Pill_Default_Error", screenShotPrefix))
                .verifyTrue("Expected filter pills are not present.",
                        grid.getFilterStatusValuesText().containsAll(expectedFilters));

        Map<String, FilterStatusValue> actualFilters = grid.getMapOfFilterStatusValues();

        for(Map.Entry<String, Boolean> entry : expectedFiltersAndState.entrySet())
        {
            if(actualFilters.containsKey(entry.getKey()))
            {

                checker().verifyEquals(String.format("State of filter pill '%s' not as expected. Expected lock state: %s.",
                        entry.getKey(), entry.getValue()), entry.getValue(), actualFilters.get(entry.getKey()).isLocked());

                if(actualFilters.get(entry.getKey()).isLocked())
                {

                    log(String.format("Validate clicking locked filter pill '%s' does not show the filter dialog.", entry.getKey()));

                    actualFilters.get(entry.getKey()).getComponentElement().click();
                    sleep(1_000);

                    // If there is a dialog, call to checker() returns false, then try to remove the dialog.
                    if (!checker().withScreenshot(String.format("%s_Filter_Pill_Dialog_Error", screenShotPrefix))
                            .verifyFalse("Looks like a modal dialog was shown after clicking the locked filter pill.",
                                    isElementPresent(Locator.tagWithClass("div", "modal-dialog"))))
                    {
                        new ModalDialog.ModalDialogFinder(getDriver()).find().dismiss();
                    }

                }
            }
            else
            {
                // Don't need to record an error here. The missing filter pill would have been caught with the checker
                // call that all the filter pills are as expected.
                log(String.format("Filter pill with text '%s' is not present. Cannot verify icon or state.", entry.getKey()));
            }

        }

        return checker().errorsSinceMark() == 0;
    }

    /**
     * <p>
     *     Verify that audit events are logged when grid views are created, updated, and deleted.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a named view and verify a "Grid view created" audit event is logged with correct fields.</li>
     *         <li>Update the view and verify a "Grid view updated" audit event is logged with old and new data maps.</li>
     *         <li>Delete the view and verify a "Grid view deleted" audit event is logged.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testGridViewAuditEvents() throws Exception
    {
        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        // Part A: Verify audit event on Create

        log("Get baseline audit row ID before creating a view.");
        Integer baselineRowId = _auditLogHelper.getLatestAuditRowId("GridViewAuditEvent");

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        log(String.format("Hide column '%s' and save as named view '%s'.", COL_INT, AUDIT_TEST_VIEW));
        grid.hideColumn(COL_INT);
        SaveViewDialog saveViewDialog = grid.clickSaveButton(true);
        saveViewDialog.setViewName(AUDIT_TEST_VIEW)
                .setMakeShared(false)
                .saveView();

        log("Query the GridViewAuditEvent audit log for the create event.");
        List<Map<String, Object>> auditRows = getGridViewAuditEvents(baselineRowId);

        checker().verifyEquals("Expected exactly 1 audit event for view creation.", 1, auditRows.size());

        if (!auditRows.isEmpty())
        {
            Map<String, Object> createEvent = auditRows.getFirst();

            checker().verifyTrue("Comment should contain 'Grid view created: " + AUDIT_TEST_VIEW + "'.",
                    String.valueOf(createEvent.get("Comment")).contains("Grid view created: " + AUDIT_TEST_VIEW));

            checker().verifyEquals("ViewName should match.", AUDIT_TEST_VIEW, createEvent.get("ViewName"));

            checker().verifyEquals("SchemaName should be 'samples'.", "samples", createEvent.get("SchemaName"));

            checker().verifyEquals("QueryName should match the sample type name.", VIEW_DIALOG_ST, createEvent.get("QueryName"));

            checker().verifyTrue("CustomViewOwner should not be null for a personal view.",
                    createEvent.get("CustomViewOwner") != null);

            checker().verifyEquals("Inheritable should be false.", false, createEvent.get("Inheritable"));

            checker().verifyTrue("NewRecordMap should not be null/empty for a create event.",
                    createEvent.get("NewRecordMap") != null && !String.valueOf(createEvent.get("NewRecordMap")).isEmpty());

            checker().verifyTrue("OldRecordMap should be null or empty for a create event.",
                    createEvent.get("OldRecordMap") == null || String.valueOf(createEvent.get("OldRecordMap")).isEmpty());
        }

        checker().screenShotIfNewError("Audit_Create_Event_Error");

        // Part B: Verify audit event on Update (set sort direction on COL_STRING1 to be DESC)

        log("Get new baseline row ID before updating the view with a sort change.");
        Integer sortUpdateBaselineRowId = _auditLogHelper.getLatestAuditRowId("GridViewAuditEvent");

        log(String.format("Update the view '%s' by sorting '%s' descending.", AUDIT_TEST_VIEW, COL_STRING1));
        grid.sortColumn(COL_STRING1, SortDirection.DESC);
        grid.saveView().saveView();

        // Allow time for the save to complete.
        sleep(1_000);

        log("Query the GridViewAuditEvent audit log for the sort update event.");
        auditRows = getGridViewAuditEvents(sortUpdateBaselineRowId);

        checker().verifyEquals("Expected exactly 1 audit event for view sort update.", 1, auditRows.size());

        if (!auditRows.isEmpty())
        {
            Map<String, Object> sortUpdateEvent = auditRows.getFirst();

            checker().verifyTrue("Comment should contain 'Grid view updated: " + AUDIT_TEST_VIEW + "'.",
                    String.valueOf(sortUpdateEvent.get("Comment")).contains("Grid view updated: " + AUDIT_TEST_VIEW));

            checker().verifyEquals("ViewName should match.", AUDIT_TEST_VIEW, sortUpdateEvent.get("ViewName"));

            checker().verifyEquals("SchemaName should be 'samples'.", "samples", sortUpdateEvent.get("SchemaName"));

            checker().verifyEquals("QueryName should match the sample type name.", VIEW_DIALOG_ST, sortUpdateEvent.get("QueryName"));

            checker().verifyTrue("OldRecordMap should not be null/empty for an update event.",
                    sortUpdateEvent.get("OldRecordMap") != null && !String.valueOf(sortUpdateEvent.get("OldRecordMap")).isEmpty());

            checker().verifyTrue("NewRecordMap should not be null/empty for an update event.",
                    sortUpdateEvent.get("NewRecordMap") != null && !String.valueOf(sortUpdateEvent.get("NewRecordMap")).isEmpty());

            checker().verifyFalse("NewRecordMap should differ from OldRecordMap after changing sort direction.",
                    String.valueOf(sortUpdateEvent.get("NewRecordMap")).equals(String.valueOf(sortUpdateEvent.get("OldRecordMap"))));

            checker().verifyTrue("CustomViewOwner should not be null for a personal view.",
                    sortUpdateEvent.get("CustomViewOwner") != null);
        }

        checker().screenShotIfNewError("Audit_Sort_Update_Event_Error");

        // Part C: Verify audit event on Update (make shared, add sort on COL_STRING2)

        log("Get new baseline row ID before updating the view.");
        Integer updateBaselineRowId = _auditLogHelper.getLatestAuditRowId("GridViewAuditEvent");

        log(String.format("Update the view '%s' by adding a sort on '%s' and making it shared.", AUDIT_TEST_VIEW, COL_STRING1));
        grid.sortColumn(COL_STRING2, SortDirection.ASC);
        SaveViewDialog updateSaveDialog = grid.saveView();
        updateSaveDialog.setMakeShared(true);
        updateSaveDialog.saveView();

        // Allow time for the save to complete.
        sleep(1_000);

        log("Query the GridViewAuditEvent audit log for the update event. RowId > " + updateBaselineRowId);
        auditRows = getGridViewAuditEvents(updateBaselineRowId);

        checker().verifyEquals("Expected 2 audit events for view update and changed to shared.", 2, auditRows.size());

        if (auditRows.size() == 2)
        {
            // A shared view is created from the private view
            Map<String, Object> createEvent = auditRows.get(1);

            checker().verifyTrue("Comment should contain 'Grid view created: " + AUDIT_TEST_VIEW + "'.",
                    String.valueOf(createEvent.get("Comment")).contains("Grid view created: " + AUDIT_TEST_VIEW));

            checker().verifyEquals("ViewName should match.", AUDIT_TEST_VIEW, createEvent.get("ViewName"));

            checker().verifyEquals("SchemaName should be 'samples'.", "samples", createEvent.get("SchemaName"));

            checker().verifyEquals("QueryName should match the sample type name.", VIEW_DIALOG_ST, createEvent.get("QueryName"));

            checker().verifyTrue("OldRecordMap should be empty for new private view created from Shared view.",
                    createEvent.get("OldRecordMap") == null || String.valueOf(createEvent.get("OldRecordMap")).isEmpty());

            checker().verifyTrue("NewRecordMap should not be null/empty for an update event.",
                    createEvent.get("NewRecordMap") != null && !String.valueOf(createEvent.get("NewRecordMap")).isEmpty());

            checker().verifyTrue("CustomViewOwner should be null when creating new shared view from existing private view.",
                    createEvent.get("CustomViewOwner") == null);

            // the old private view is deleted
            Map<String, Object> deleteEvent = auditRows.getFirst();

            log(String.valueOf(deleteEvent.get("Comment")));
            checker().verifyTrue("Comment should contain 'Grid view deleted: " + AUDIT_TEST_VIEW + "'.",
                    String.valueOf(deleteEvent.get("Comment")).contains("Grid view deleted: " + AUDIT_TEST_VIEW));

            checker().verifyEquals("ViewName should match.", AUDIT_TEST_VIEW, deleteEvent.get("ViewName"));

            checker().verifyTrue("OldRecordMap should not be null/empty for a delete event.",
                    deleteEvent.get("OldRecordMap") != null && !String.valueOf(deleteEvent.get("OldRecordMap")).isEmpty());

            checker().verifyTrue("NewRecordMap should be null or empty for a delete event.",
                    deleteEvent.get("NewRecordMap") == null || String.valueOf(deleteEvent.get("NewRecordMap")).isEmpty());
        }

        checker().screenShotIfNewError("Audit_Update_Event_Error");

        // Part D: Verify audit event on Delete

        log("Get new baseline row ID before deleting the view.");
        Integer deleteBaselineRowId = _auditLogHelper.getLatestAuditRowId("GridViewAuditEvent");

        log(String.format("Delete the view '%s' via the Manage Views dialog.", AUDIT_TEST_VIEW));
        grid.manageViews()
                .deleteView(AUDIT_TEST_VIEW + " (shared)")  // The view name in the manage views dialog for a shared view is the view name with " (shared)" appended to it.
                .confirmDelete()
                .dismiss();

        log("Query the GridViewAuditEvent audit log for the delete event.");
        auditRows = getGridViewAuditEvents(deleteBaselineRowId);

        checker().verifyEquals("Expected exactly 1 audit event for view deletion.", 1, auditRows.size());

        if (!auditRows.isEmpty())
        {
            Map<String, Object> deleteEvent = auditRows.getFirst();

            checker().verifyTrue("Comment should contain 'Grid view deleted: " + AUDIT_TEST_VIEW + "'.",
                    String.valueOf(deleteEvent.get("Comment")).contains("Grid view deleted: " + AUDIT_TEST_VIEW));

            checker().verifyEquals("ViewName should match.", AUDIT_TEST_VIEW, deleteEvent.get("ViewName"));

            checker().verifyTrue("OldRecordMap should not be null/empty for a delete event.",
                    deleteEvent.get("OldRecordMap") != null && !String.valueOf(deleteEvent.get("OldRecordMap")).isEmpty());

            checker().verifyTrue("NewRecordMap should be null or empty for a delete event.",
                    deleteEvent.get("NewRecordMap") == null || String.valueOf(deleteEvent.get("NewRecordMap")).isEmpty());
        }

        checker().screenShotIfNewError("Audit_Delete_Event_Error");
    }

    /**
     * Helper to query GridViewAuditEvent rows created after the given row ID.
     *
     * @param afterRowId Only return rows with RowId greater than this value.
     * @return List of audit event rows, sorted by RowId descending.
     */
    private List<Map<String, Object>> getGridViewAuditEvents(Integer afterRowId) throws IOException, CommandException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", "GridViewAuditEvent");
        cmd.setColumns(List.of("RowId", "Comment", "ViewName", "SchemaName", "QueryName",
                "CustomViewOwner", "Inheritable", "OldRecordMap", "NewRecordMap"));
        cmd.addFilter("RowId", afterRowId, Filter.Operator.GT);
        cmd.setSorts(List.of(new Sort("RowId", Sort.Direction.DESCENDING)));
        cmd.setContainerFilter(ContainerFilter.CurrentAndSubfolders);

        SelectRowsResponse response = cmd.execute(cn, getProjectName());
        return response.getRows();
    }

}
