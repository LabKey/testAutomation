package org.labkey.test.tests.component;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.FilterStatusValue;
import org.labkey.test.components.ui.grids.CustomizeGridDialog;
import org.labkey.test.components.ui.grids.GridFilterModal;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.components.ui.search.FilterExpressionPanel;
import org.labkey.test.components.ui.search.FilterFacetedPanel;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
public class GridPanelViewTest extends GridPanelBaseTest
{

    // Sample type used to validate default views. It is just easier to have a separate sample type where the default views are changed.
    private static final String DEFAULT_VIEW_SAMPLE_TYPE = "View_SampleType";
    private static final int DEFAULT_VIEW_SAMPLE_TYPE_SIZE = 30;
    private static final String DEFAULT_VIEW_SAMPLE_PREFIX = "VIEW-";

    // A sample type that will be used to validate shared views.
    private static final String VIEW_DIALOG_ST = "Test_View_Dialog_SampleType";
    private static final int VIEW_DIALOG_ST_SIZE = 100;
    private static final String VIEW_DIALOG_ST_PREFIX = "DLG-";

    // Column names.
    private static final String COL_NAME = "Name";
    private static final String COL_STRING1 = "Str1";
    private static final String COL_STRING2 = "Str2";
    private static final String COL_INT = "Int";
    private static final String COL_BOOL = "Bool";

    private static final List<String> DEFAULT_COLUMNS = Arrays.asList(COL_NAME, COL_INT, COL_STRING1, COL_STRING2, COL_BOOL);
    private static Map<String, Integer> defaultColumnState = new HashMap<>();
    private static final int ICONS_NONE = 0;
    private static final int ICONS_SORT = 1;
    private static final int ICONS_FILTER = 2;

    private static final String OTHER_USER = "other_user@grid.panel.test";
    private static final String OTHER_PW = "S0meP@ssW0rd"; // Hardcoded password because it makes it easier to manually debug.

    private static final List<String> stringSetMembers = Arrays.asList("A", "B", "C");
    private static List<String> stringSets = new ArrayList<>();

    private static final String EDITED_ALERT = "EDITED";
    private static final String UPDATED_ALERT = "UPDATED";

    // Keep track of any custom views that may have been created.
    private static List<String> savedViews = new ArrayList<>();


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
        GridPanelViewTest init = (GridPanelViewTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        initProject();

        // Create list of strings that has the various sets of the letters A, B, C & D
        stringSets = getAllSets(stringSetMembers, stringSetMembers.size() - 1);
        // Remove the empty set.
        stringSets.remove(0);

        // Create a sample type that will validate views can be saved and shared. Primarily interested in the views not
        // with complex filtering scenarios.
        List<FieldDefinition> fields = Arrays.asList(new FieldDefinition(COL_INT, FieldDefinition.ColumnType.Integer),
                new FieldDefinition(COL_STRING1, FieldDefinition.ColumnType.String),
                new FieldDefinition(COL_STRING2, FieldDefinition.ColumnType.String),
                new FieldDefinition(COL_BOOL, FieldDefinition.ColumnType.Boolean));

        createSampleType(VIEW_DIALOG_ST, VIEW_DIALOG_ST_PREFIX, VIEW_DIALOG_ST_SIZE, fields);

        createSampleType(DEFAULT_VIEW_SAMPLE_TYPE, DEFAULT_VIEW_SAMPLE_PREFIX, DEFAULT_VIEW_SAMPLE_TYPE_SIZE, fields);

        _userHelper.createUser(OTHER_USER, true,false);
        setInitialPassword(OTHER_USER, OTHER_PW);
        new ApiPermissionsHelper(this).addMemberToRole(OTHER_USER, "Folder Administrator", PermissionsHelper.MemberType.user, getProjectName());

    }

    private void createSampleType(String sampleTypeName, String samplePrefix, int numOfSamples, List<FieldDefinition> fields) throws IOException, CommandException
    {

        SampleTypeDefinition props = new SampleTypeDefinition(sampleTypeName)
                .setFields(fields);

        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);

        int sampleId = 1;
        int allPossibleIndex = 0;
        int memIndex = 0;

        while (sampleId <= numOfSamples)
        {

            if(allPossibleIndex == stringSets.size())
                allPossibleIndex = 0;

            if(memIndex == stringSetMembers.size())
                memIndex = 0;
            sampleSetDataGenerator.addCustomRow(
                    Map.of(COL_NAME, String.format("%s%d", samplePrefix, sampleId),
                            COL_INT, sampleId,
                            COL_STRING1, stringSets.get(allPossibleIndex),
                            COL_STRING2, stringSetMembers.get(memIndex),
                            COL_BOOL, sampleId % 2 == 0));

            allPossibleIndex++;
            memIndex++;
            sampleId++;
        }

        sampleSetDataGenerator.insertRows();

        removeFlagColumnFromDefaultView(sampleTypeName);
    }

    /**
     * Helper function that will reset the default view of a grid. It will also remove any sorts or filters on the columns.
     *
     * @param sampleTypeName Name of the sample type with the default view to change.
     * @param columns The columns to show in the default view. Will be added in the order of the list.
     */
    private void resetDefaultView(String sampleTypeName, List<String> columns)
    {
        log(String.format("Set the default view for '%s' to have these columns: '%s'", sampleTypeName, columns));

        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText(sampleTypeName));
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        drtSamples.goToView(VIEW_DEFAULT);
        CustomizeView cv = drtSamples.openCustomizeGrid();

        // Remove everything but the Name field before adding back.
        for(String columnName : columns)
        {
            if(!columnName.equals(COL_NAME))
                cv.removeColumn(columnName);
        }

        cv.saveCustomView("", true);

        // Adding back in the order of the list sent in.
        cv = drtSamples.openCustomizeGrid();
        for(String columnName : columns)
        {
            cv.addColumn(columnName);
        }

        log("Clear any filters that may have been applied.");
        cv.clearFilters();

        log("Clear any sorts that may have been applied.");
        cv.clearSorts();

        cv.saveCustomView("", true);

        defaultColumnState = new HashMap<>();
        for(String columnName : columns)
        {
            defaultColumnState.put(columnName, ICONS_NONE);
        }

    }

    /**
     * <p>
     *     Simple test to validate the 'My Default' menu option.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Modify the default view for a sample type in LabKey Server.</li>
     *         <li>Save the default view, but do not make it public (default for all).</li>
     *         <li>Validate in the App grid that 'My Default' is shown under the Views menu.</li>
     *         <li>Validate that other users se no change to the default view.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testMyDefaultView()
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

        log("Do not share this default view with everyone.");
        cv.saveCustomView("", false);

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        log(String.format("Verify that '%s' is in the views menu and '%s' is not.",
                VIEW_DEFAULT_MODIFIED, VIEW_DEFAULT));

        List<String> expectedMenuItems = new ArrayList<>();
        expectedMenuItems.addAll(List.of(VIEW_DEFAULT_MODIFIED, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE));
        expectedMenuItems.addAll(savedViews);

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
    public void testRemoveColumnForView()
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

        grid = grid.clickUndo();

        validateGridHeader(screenShotPrefix, grid, "", false);

        // If the grid columns are not as expected after Undo don't continue with the test.
        checker().fatal()
                .verifyTrue("Grid columns were not as expected. Fatal error.",
                        validateGridColumns(screenShotPrefix, grid, defaultColumnState));

        log(String.format("Hide the '%s' column again and this time save it as default.", columnToRemove));

        grid = grid.hideColumn(columnToRemove);

        log("Validate the 'Save View' dialog.");
        QueryGrid.SaveViewDialog saveViewDialog = grid.clickSave(true);

        checker().verifyTrue(String.format("The 'View Name' field should be empty. It contains '%s'.", saveViewDialog.getViewName()),
                saveViewDialog.getViewName().isEmpty());

        checker().verifyFalse("The 'Make default' checkbox should not be checked.",
                saveViewDialog.isMakeDefaultForAllChecked());

        checker().screenShotIfNewError("testDefaultViewRemoveColumn_Save_View_Dialog_Defaults_Error");

        saveViewDialog.setMakeDefaultForAll(true);

        checker().verifyFalse("Setting 'Default for all' should disable the name field, it did not.",
                saveViewDialog.isViewNameEnabled());

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
    public void testColumnHeaderAndFilterPillDefaultView()
    {
        testColumnHeaderAndFilterPill("testColumnHeaderAndFilterPillDefaultView", "");
    }

    /**
     * Validate filter pills and column headers for a saved custom view.
     * @see GridPanelViewTest#testColumnHeaderAndFilterPill(String, String)
     */
    @Test
    public void testColumnHeaderAndFilterPillCustomView()
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
    public void testColumnHeaderAndFilterPill(String testName, String viewName)
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

        QueryGrid.SaveViewDialog saveViewDialog;

        if(viewName.isEmpty())
        {
            log("Save as default view (for everyone).");
            grid.getGridBar().doMenuAction("Views", Arrays.asList("Save Grid View"));
            saveViewDialog = new QueryGrid.SaveViewDialog(getDriver(), grid);
            saveViewDialog.setMakeDefaultForAll(true);
        }
        else
        {
            log(String.format("Save as custom view '%s'.", viewName));
            grid.getGridBar().doMenuAction("Views", Arrays.asList("Save Grid View"));
            saveViewDialog = new QueryGrid.SaveViewDialog(getDriver(), grid);
            saveViewDialog.setViewName(viewName);
            saveViewDialog.setMakeDefaultForAll(false);

            savedViews.add(viewName);
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
            grid.getGridBar().doMenuAction("Views", Arrays.asList(viewName));
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
    public void testEditCustomView()
    {
        testEditView("testEditCustomView", "Edit_Filter_Custom_View");
    }

    /**
     * Test modifying filters used in the default view.
     * @see GridPanelViewTest#testEditView(String, String)
     */
    @Test
    public void testEditDefaultView()
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
    private void testEditView(String testName, String viewName)
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

        QueryGrid.SaveViewDialog saveViewDialog;

        if(viewName.isEmpty())
        {
            log("Save as default view.");
            grid.getGridBar().doMenuAction("Views", List.of("Save Grid View"));
            saveViewDialog = new QueryGrid.SaveViewDialog(getDriver(), grid);
            saveViewDialog.setMakeDefaultForAll(true);
            saveViewDialog.saveView();
        }
        else
        {

            // Issue: 46019 Cannot save a grid view if a non-string column is filtered. Only happens on components page (core-components.view).

            log(String.format("Save view as '%s'.", viewName));
            grid.getGridBar().doMenuAction("Views", List.of("Save Grid View"));
            saveViewDialog = new QueryGrid.SaveViewDialog(getDriver(), grid);
            saveViewDialog.setMakeDefaultForAll(false);
            saveViewDialog.setViewName(viewName);
            saveViewDialog.saveView();

            savedViews.add(viewName);
        }

        int rowCount = grid.getRows().size();

        log("Now that the filters have been save as a view validate filter pills.");
        validateFilterPills(testName, grid, expectedFilterPills);

        log(String.format("Remove the filter '%s' and validate grid is now in '%s' mode.", expectedFilter1Text, EDITED_ALERT));
        grid.removeFilter(expectedFilter1Text);

        validateGridHeader(testName, grid, EDITED_ALERT, true);

        checker().verifyTrue("Filter was removed but number of rows in grid did not increase.",
                grid.getRows().size() > rowCount);

        // Use different screenshot name based on view type.
        checker().screenShotIfNewError(viewName.isEmpty() ?
                String.format("%s_Remove_Filter_Default_Error", testName) :
                String.format("%s_Remove_Filter_Custom_View_Error", testName));

        log("Validate 'Undo' resets as expected.");

        grid.clickUndo();

        log("Validate filter pills go back to expected state.");
        validateFilterPills(testName, grid, expectedFilterPills);

        log("Validate header has no edit status or buttons after 'Undo'.");
        validateGridHeader(testName, grid, "", false);

        checker().verifyEquals("Number of rows after clicking 'Undo' not as expected'.",
                rowCount, grid.getRows().size());

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
            saveViewDialog = grid.clickSave(true);

            log("Verify that view name is empty and 'default checkbox' is checked.");

            checker().verifyTrue(String.format("Value of view name is not empty. Contains '%s'.", saveViewDialog.getViewName()),
                    saveViewDialog.getViewName().isEmpty());

            // The default checkbox is not checked by default. Maybe a bug?
            log("Checking the 'Default for all' checkbox.");
            saveViewDialog.setMakeDefaultForAll(true);

            saveViewDialog.saveView();
        }
        else
        {
            grid.clickSave(false);

            log("Because this is already a saved view verify that no save dialog is shown.");

            sleep(1_000);
            if(!checker().verifyFalse("Looks like a modal dialog was shown after clicking the locked filter pill.",
                    isElementPresent(Locator.tagWithClass("div", "modal-dialog"))))
            {
                saveViewDialog = new QueryGrid.SaveViewDialog(getDriver(), grid);
                checker().screenShotIfNewError(String.format("%s_Unexpected_Save_View_Dialog", testName));

                // If the save view dialog unexpectedly popped up try and save the view.
                if(saveViewDialog.getViewName().isEmpty())
                    saveViewDialog.setViewName(viewName);

                saveViewDialog.setMakeDefaultForAll(false);

                saveViewDialog.saveView();
            }

        }

        log(String.format("Verify that changing the sort order of column '%s' causes the grid view to go to '%s'.",
                sortColumn, EDITED_ALERT));

        grid.sortColumn(sortColumn, SortDirection.DESC);

        validateGridHeader(testName, grid, EDITED_ALERT, true);
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

        List<String> actualColumns = grid.getColumnNames();

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

        checker().screenShotIfNewError(String.format("%s_Views_Menu_Column_Error", screenShotPrefix));

        return checker().errorsSinceMark() == 0;
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

        checker().screenShotIfNewError(String.format("%s_Grid_Header_Error", screenShotPrefix));

        return checker().errorsSinceMark() == 0;
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
    public void testFieldInsertionOrder()
    {
        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        // Removing the column and then testing the removal is redundant with other tests, however it will be used to
        // validate other parts of the dialog that is not covered in other tests.
        String columnToAdd = COL_INT;
        log(String.format("Use the grid menu to hide column '%s'.", columnToAdd));
        grid.hideColumn(columnToAdd);
        grid.clickSave(true).setMakeDefaultForAll(true).saveView();

        List<String> expectedFields = new ArrayList<>(DEFAULT_COLUMNS);
        expectedFields.remove(columnToAdd);

        Map<String, Integer> expectedColumns = new HashMap<>(defaultColumnState);
        expectedColumns.remove(columnToAdd);

        checker().fatal()
                .verifyTrue("Columns not as expected need this to pass for the other test. Fatal error.",
                        validateGridColumns("Dialog_Test", grid, expectedColumns));

        String selectedColumn = COL_STRING2;
        log(String.format("Click the grid menu above column '%s' and validate that insertion happens to the right of the column.", selectedColumn));
        CustomizeGridDialog customizeModal = grid.insertColumn(selectedColumn);

        log("Validate that the 'Available Fields' and 'Shown in Grid' panels are as expected.");

        checker().verifyEquals(String.format("Column '%s' should be selected in the dialog, it is not.", selectedColumn),
                selectedColumn, customizeModal.getSelectedShownInGridLabel());

        checker().verifyEqualsSorted("Field displayed in 'Show in Grid' panel not as expected.",
                expectedFields, customizeModal.getShownInGridLabels());

        for (String field : expectedFields)
        {
            checker().verifyTrue(String.format("Field '%s' is not shown as already added in the 'Available Fields' panel.", field),
                    customizeModal.isAvailableFieldAddedToGrid(field));
        }

        checker().verifyFalse(String.format("Field '%s' should not be shown as already added in the 'Available Fields' panel.", columnToAdd),
                customizeModal.isAvailableFieldAddedToGrid(columnToAdd));

        log(String.format("Add field '%s'.", columnToAdd));
        customizeModal.addAvailableFieldToGrid(columnToAdd);

        checker().verifyTrue(String.format("Field '%s' should now be shown as added in the 'Available Fields' panel, it does not.", columnToAdd),
                customizeModal.isAvailableFieldAddedToGrid(columnToAdd));

        log("Validate that the order of the fields in the 'Shown in Grid' column are as expected.");
        expectedFields = List.of(COL_NAME, COL_STRING1, COL_STRING2, COL_INT, COL_BOOL);
        checker().verifyEquals(String.format("After adding '%s' fields displayed in 'Show in Grid' panel not as expected.", columnToAdd),
                expectedFields, customizeModal.getShownInGridLabels());

        checker().screenShotIfNewError("InsertionOrder_Customize_Dialog_Error");
        customizeModal.clickUpdateGrid();

        log(String.format("Validate that the order of the fields in the grid, specifically that '%s' is after '%s'.", columnToAdd, selectedColumn));
        List<String> columns = grid.getColumnNames();
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
    public void testShowAllLabelEditAndUndo()
    {
        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        grid.getGridBar().doMenuAction("Views", List.of("Customize Grid View"));
        CustomizeGridDialog customizeModal = new CustomizeGridDialog(getDriver(), grid);

        checker().verifyFalse("The 'Update' button is enabled, it should not be.",
                customizeModal.isUpdateGridEnabled());

        checker().verifyFalse("The 'Undo edits' button is enabled, it should not be.",
                customizeModal.isUndoEditsEnabled());

        log("Validate that using the menu to open the dialog results in no fields being selected in the 'Shown in Grid' panel.");
        checker().verifyTrue(String.format("Field '%s' is selected in the 'Shown in Grid' panel, there should be no selected fields.", customizeModal.getSelectedShownInGridLabel()),
                customizeModal.getSelectedShownInGridLabel().isEmpty());

        if (!checker().verifyFalse("The 'Show all system and user-defined fields' should not be checked.",
                customizeModal.isShowAllChecked()))
        {
            log("Uncheck the 'Show all' option.");
            customizeModal.setShowAll(false);
        }

        String materialIDField = "Material Source Id";
        log(String.format("Validate that field '%s' is not visible before checking 'Show all'.", materialIDField));

        List<String> actualFields = customizeModal.getAvailableFields();

        checker().fatal()
                .verifyFalse(String.format("Field '%s' is already present in 'Available Fields' panel. Fatal error.", materialIDField),
                        actualFields.contains(materialIDField));

        log(String.format("Check 'Show all' and validate that '%s' is now listed.", materialIDField));

        customizeModal.setShowAll(true);

        actualFields = customizeModal.getAvailableFields();

        checker().verifyTrue(String.format("Field '%s' is not present in 'Available Fields' panel, it should be.", materialIDField),
                actualFields.contains(materialIDField));

        log(String.format("Expand field '%s'.", materialIDField));
        customizeModal.expandAvailableFields(materialIDField);

        String materialNameField = "Name";
        log(String.format("Select the '%s' field under '%s' and add it to the grid.", materialNameField, materialIDField));
        customizeModal.addAvailableFieldToGrid(materialIDField, materialNameField);

        List<String> expectedFields = new ArrayList<>(DEFAULT_COLUMNS);
        expectedFields.add(materialNameField);

        log("Because no fields should be selected validate this field is added to the end of the list.");
        checker().verifyEquals(String.format("Position of field '%s' is not as expected in the dialog.", materialNameField),
                expectedFields, customizeModal.getShownInGridLabels());

        checker().screenShotIfNewError("ShowAll_Label_Edit_Dialog_Error");

        if(checker().verifyTrue("The 'Undo edits' button is not enabled it should be.", customizeModal.isUndoEditsEnabled()))
        {
            log("Undo the edits.");
            customizeModal.clickUndoEdits();
            expectedFields = new ArrayList<>(DEFAULT_COLUMNS);
            if(checker().verifyEquals("After clicking 'Undo edits' fields in 'Shown in Grid' dialog not as expected.",
                    expectedFields, customizeModal.getShownInGridLabels()))
            {
                log(String.format("Add field '%s / %s' back.", materialIDField, materialNameField));
                customizeModal.addAvailableFieldToGrid(materialIDField, materialNameField);
            }

        }
        else
        {
            checker().screenShotIfNewError("Undo_Edit_Button_Error");
        }

        String newFieldLabel = String.format("My New Label %s", materialNameField);
        log(String.format("Change the label of the field '%s' to '%s'.", materialNameField, newFieldLabel));

        // Adding the 'Material Source Id / Name' field creates two fields with the label 'Name' in the 'Shown in Grid' panel, make sure the expected one is updated.
        customizeModal.updateFieldLabel(materialNameField, 1, newFieldLabel);

        checker().fatal().verifyTrue("'Update' button is not enabled, cannot save changes. Fatal error.",
                customizeModal.isUpdateGridEnabled());

        customizeModal.clickUpdateGrid();

        log("Validate that the grid shows the new field with the updated label.");

        checker().verifyTrue(String.format("Did not find the field labeled '%s' in the grid.", newFieldLabel),
                grid.getColumnNames().contains(newFieldLabel));

    }

    @Test
    public void testSaveViewDialog()
    {
        goToProjectHome();

        resetDefaultView(VIEW_DIALOG_ST, DEFAULT_COLUMNS);

        QueryGrid grid = beginAtQueryGrid(VIEW_DIALOG_ST);

        grid.getGridBar().doMenuAction("Views", List.of("Customize Grid View"));
        CustomizeGridDialog customizeModal = new CustomizeGridDialog(getDriver(), grid);

        String fieldRemoved1 = COL_INT;

        log(String.format("Remove field '%s' using the dialog.", fieldRemoved1));

        customizeModal.removeShownInGridLabel(fieldRemoved1).clickUpdateGrid();

        QueryGrid.SaveViewDialog saveViewDialog = grid.clickSave(true);
        String viewName1 = String.format("No %s", fieldRemoved1);
        saveViewDialog.setViewName(viewName1);
        saveViewDialog.saveView();

        log(String.format("Go back to '%s' and create a new view.", VIEW_DEFAULT));

        grid.getGridBar().doMenuAction("Views", List.of(VIEW_DEFAULT));

        grid.getGridBar().doMenuAction("Views", List.of("Customize Grid View"));
        customizeModal = new CustomizeGridDialog(getDriver(), grid);

        String fieldRemoved2 = COL_STRING2;

        log(String.format("Remove field '%s' using the dialog.", fieldRemoved2));

        customizeModal.removeShownInGridLabel(fieldRemoved2).clickUpdateGrid();

        saveViewDialog = grid.clickSave(true);
        String viewName2 = String.format("No %s", fieldRemoved2);
        saveViewDialog.setViewName(viewName2);
        saveViewDialog.saveView();

    }

}
