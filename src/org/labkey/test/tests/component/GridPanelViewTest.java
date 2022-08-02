package org.labkey.test.tests.component;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ui.grids.CustomizeGridDialog;
import org.labkey.test.components.ui.grids.GridFilterModal;
import org.labkey.test.components.ui.grids.QueryGrid;
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
import java.util.List;
import java.util.Map;

@Category({Daily.class})
public class GridPanelViewTest extends GridPanelBaseTest
{

    // Sample type used to validate default views. It is just easier to have a separate sample type where the default views are changed.
    private static final String DEFAULT_VIEW_SAMPLE_TYPE = "Default_View_SampleType";
    private static final int DEFAULT_VIEW_SAMPLE_TYPE_SIZE = 10;
    private static final String DEFAULT_VIEW_SAMPLE_PREFIX = "DFT-";

    // A sample type that will be used to validate shared views.
    private static final String VIEW_SAMPLE_TYPE = "View_SampleType";
    private static final int VIEW_SAMPLE_TYPE_SIZE = 100;
    private static final String VIEW_SAMPLE_PREFIX = "VST-";

    // Column names.
    private static final String COL_NAME = "Name";
    private static final String COL_STRING = "Str";
    private static final String COL_INT = "Int";

    private static final String OTHER_USER = "other_user@grid.panel.test";
    private static final String OTHER_PW = "S0meP@ssW0rd"; // Hardcoded password because it makes it easier to manually debug.

    private static final List<String> stringSetMembers = Arrays.asList("A", "B", "C");
    private static List<String> stringSets = new ArrayList<>();

    private static final String EDITED_ALERT = "EDITED";
    private static final String UPDATED_ALERT = "UPDATED";

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
                new FieldDefinition(COL_STRING, FieldDefinition.ColumnType.String));

        createSampleType(VIEW_SAMPLE_TYPE, VIEW_SAMPLE_PREFIX, VIEW_SAMPLE_TYPE_SIZE, fields);

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

        while (sampleId <= numOfSamples)
        {

            if(allPossibleIndex == stringSets.size())
                allPossibleIndex = 0;

            sampleSetDataGenerator.addCustomRow(
                    Map.of(COL_NAME, String.format("%s%d", samplePrefix, sampleId++),
                            COL_INT, sampleId,
                            COL_STRING, stringSets.get(allPossibleIndex++)));

        }

        sampleSetDataGenerator.insertRows();

        removeFlagColumnFromDefaultView(sampleTypeName);
    }

    private void resetDefaultView(String sampleTypeName, List<String> columns)
    {
        log(String.format("Set the default view for '%s' to have these columns: '%s'", sampleTypeName, columns));

        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText(sampleTypeName));
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        drtSamples.goToView(VIEW_DEFAULT);
        CustomizeView cv = drtSamples.openCustomizeGrid();

        for(String columnName : columns)
        {
            cv.addColumn(columnName);
        }

        cv.saveCustomView("", true);
    }

    @Test
    public void testDefaultViewFromLKS()
    {
        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, Arrays.asList(COL_NAME, COL_STRING, COL_INT));

        goToProjectHome();

        waitAndClickAndWait(Locator.linkWithText(DEFAULT_VIEW_SAMPLE_TYPE));

        log(String.format("In LabKey Server for sample type '%s' remove '%s' column form default view.",
                DEFAULT_VIEW_SAMPLE_TYPE, COL_STRING));

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        drtSamples.goToView(VIEW_DEFAULT);

        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.removeColumn(COL_STRING);

        log(String.format("Don't save the view yet, leave it in an '%s' state.", EDITED_ALERT));

        cv.clickViewGrid();

        log("View the app grid and validate header and columns are as expected.");

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        validateGridHeader(grid, EDITED_ALERT, true);

        checker().screenShotIfNewError("Grid_Alert_Button_Error");

        log("Validate that the menu for the grid has not changed but the columns have.");
        List<String> expectedMenuItems = Arrays.asList(VIEW_DEFAULT, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE);
        List<String> expectedColumns = Arrays.asList(COL_NAME, COL_INT);

        validateViewMenuAndGridColumns(expectedMenuItems, expectedColumns);

        log("Go back to LabKey Server and save the default view but don't make it default for everyone.");

        goToProjectHome();

        waitAndClickAndWait(Locator.linkWithText(DEFAULT_VIEW_SAMPLE_TYPE));

        sampleHelper = new SampleTypeHelper(this);
        drtSamples = sampleHelper.getSamplesDataRegionTable();
        drtSamples.goToView(VIEW_DEFAULT);

        cv = drtSamples.openCustomizeGrid();

        cv.saveCustomView("", false);

        log(String.format("Verify that '%s' is in the views menu and '%s' is not.",
                VIEW_DEFAULT_MODIFIED, VIEW_DEFAULT));

        expectedMenuItems = Arrays.asList(VIEW_DEFAULT_MODIFIED, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE);
        expectedColumns = Arrays.asList(COL_NAME, COL_INT);

        validateViewMenuAndGridColumns(expectedMenuItems, expectedColumns);

        log(String.format("Impersonate '%s' and validate that the 'View' menu and default view have not changed.", OTHER_USER));

        impersonate(OTHER_USER);

        goToProjectHome();

        expectedMenuItems = Arrays.asList(VIEW_DEFAULT, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE);
        expectedColumns = Arrays.asList(COL_NAME, COL_INT, COL_STRING);

        validateViewMenuAndGridColumns(expectedMenuItems, expectedColumns);

        stopImpersonating();

    }

    @Test
    public void testDefaultViewRemoveColumnFromAppGrid()
    {

        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, Arrays.asList(COL_NAME, COL_STRING, COL_INT));

        log(String.format("For sample type '%s' remove the '%s' column using the column header menu.", DEFAULT_VIEW_SAMPLE_TYPE, COL_INT));

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        grid.hideColumn(COL_INT);

        log("Validate panel header is as expected and the column is indeed hidden.");

        validateGridHeader(grid, EDITED_ALERT, true);

        checker().verifyEqualsSorted(String.format("Column '%s' is still visible.", COL_INT),
                Arrays.asList(COL_NAME, COL_STRING), grid.getColumnNames());

        checker().screenShotIfNewError("Hide_Column_Edited_Error");

        log(String.format("Validate that 'Undo' puts the column back and removes the '%s' label as well as the buttons.", EDITED_ALERT));

        grid = grid.clickUndo();

        validateGridHeader(grid, "", false);

        // If the column wasn't replaced cannot really continue with the test.
        checker().fatal()
                .verifyEqualsSorted(String.format("Column '%s' is still not visible. Fatal error", COL_INT),
                        Arrays.asList(COL_NAME, COL_STRING, COL_INT), grid.getColumnNames());

        checker().screenShotIfNewError("Undo_Hide_Column_Edited_Error");

        log(String.format("Hide the '%s' column again and this time save it as default.", COL_INT));

        grid = grid.hideColumn(COL_INT);

        log("Validate the 'Save View' dialog.");
        QueryGrid.SaveViewDialog saveViewDialog = grid.clickSave();

        checker().verifyTrue(String.format("The 'View Name' field should be empty. It contains '%s'.", saveViewDialog.getViewName()),
                saveViewDialog.getViewName().isEmpty());

        checker().verifyFalse("The 'Make default' checkbox should not be checked.",
                saveViewDialog.isMakeDefaultForAllChecked());

        checker().screenShotIfNewError("Save_View_Dialog_Defaults_Error");

        saveViewDialog.setMakeDefaultForAll(true);

        checker().verifyFalse("Setting 'Default for all' should disable the name field, it did not.",
                saveViewDialog.isViewNameEnabled());

        checker().screenShotIfNewError("Save_View_Dialog_Set_Error");

        saveViewDialog.saveView();

        validateGridHeader(grid, UPDATED_ALERT, false);

        log("Verify that view menu and columns are as expected.");
        List<String> expectedMenuItems = Arrays.asList(VIEW_DEFAULT, VIEW_CUSTOMIZE, VIEW_MANAGE, VIEW_SAVE);
        List<String> expectedColumns = Arrays.asList(COL_NAME, COL_STRING);

        validateViewMenuAndGridColumns(expectedMenuItems, expectedColumns);

        log(String.format("Impersonate '%s' and validate the 'View' menu and default view are as expected.", OTHER_USER));

        impersonate(OTHER_USER);

        goToProjectHome();

        validateViewMenuAndGridColumns(expectedMenuItems, expectedColumns);

        stopImpersonating();
    }

    @Test
    public void testDefaultViewFilterColumnFromAppGrid()
    {

        resetDefaultView(DEFAULT_VIEW_SAMPLE_TYPE, Arrays.asList(COL_NAME, COL_STRING, COL_INT));

        log(String.format("For sample type '%s' filter the '%s' then save as default view.", DEFAULT_VIEW_SAMPLE_TYPE, COL_STRING));

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        int rows = grid.getRows().size();

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();
        filterDialog.selectField(COL_STRING);
        FilterFacetedPanel facetedPanel = filterDialog.selectFacetTab();
        facetedPanel.uncheckValues("[All]");
        facetedPanel.checkValues(stringSets.get(0), stringSets.get(2));
        filterDialog.confirm();

        grid.sortColumn(COL_STRING, SortDirection.ASC);

        log("Validate the icons in the column header.");

        checker().verifyTrue(String.format("Column '%s' does not have the filter icon.", COL_STRING),
                grid.hasColumnFilterIcon(COL_STRING));

        checker().verifyTrue(String.format("Column '%s' does not have the sort icon.", COL_STRING),
                grid.hasColumnSortIcon(COL_STRING));

        checker().fatal()
                .verifyTrue("Doesn't look like filtering worked. Row count is not changed as expected. Fatal error.",
                        grid.getRows().size() < rows);

        log("Validate filter pill is shown.");

        checker().verifyTrue("Filter pills shown are not as expected.",
                grid.getFilterStatusValues().size() == 1 &&
                        grid.getFilterStatusValues().get(0).getText().equals("Str Equals One Of A, AB"));

        checker().screenShotIfNewError("Default_View_Column_Filter_Error");

        log("Save as default view (for everyone).");
        grid.getGridBar().doMenuAction("Views", Arrays.asList("Save Grid View"));

        QueryGrid.SaveViewDialog saveViewDialog = new QueryGrid.SaveViewDialog(getDriver());

        saveViewDialog.setMakeDefaultForAll(true);

        log("Validate filter pill is shown after saving as default view.");

        checker().withScreenshot("Filter_Pill_Default_Error")
                .verifyTrue("Filter pills shown are not as expected.",
                        grid.getFilterStatusValues().size() == 1
                                && grid.getFilterStatusValues().get(0).getText().equals("Str Equals One Of A, AB"));

        saveViewDialog.saveView();

        log(String.format("Impersonate '%s' and validate icons in the columns for default view.", OTHER_USER));

        impersonate(OTHER_USER);

        goToProjectHome();

        grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE, false);

        checker().verifyTrue(String.format("Column '%s' does not have the filter icon.", COL_STRING),
                grid.hasColumnFilterIcon(COL_STRING));

        checker().verifyTrue(String.format("Column '%s' does not have the sort icon.", COL_STRING),
                grid.hasColumnSortIcon(COL_STRING));

        checker().verifyTrue("Doesn't look like filtering worked. Row count is not changed as expected.",
                grid.getRows().size() < rows);

        checker().verifyTrue("Filter pills shown are not as expected.",
                grid.getFilterStatusValues().size() == 1 &&
                        grid.getFilterStatusValues().get(0).getText().equals("Str Equals One Of A, AB"));

        checker().screenShotIfNewError("Default_View_Column_Filter_Other_User_Error");

        stopImpersonating();

    }

    private void validateViewMenuAndGridColumns(List<String> expectedMenuItems, List<String> expectedColumns)
    {

        QueryGrid grid = beginAtQueryGrid(DEFAULT_VIEW_SAMPLE_TYPE);

        List<String> actualValues;

        boolean menuError = false;

        if(!expectedMenuItems.isEmpty())
        {
            actualValues = grid.getGridBar().getMenuText("Views");

            if(!checker().verifyEqualsSorted("Items under 'Views' menu not as expected.",
                    expectedMenuItems, actualValues))
            {
                menuError = true;
            }
        }

        if(!expectedColumns.isEmpty())
        {
            actualValues = grid.getColumnNames();

            checker().verifyEqualsSorted("Displayed columns not as expected.",
                    expectedColumns, actualValues);
        }

        if(menuError)
        {
            // Menu items were not as expected so expand Views menu for the screenshot.
            grid.getGridBar().doMenuAction("Views", new ArrayList<>());
        }

        checker().screenShotIfNewError("Views_Menu_Column_Error");
    }

    private void validateGridHeader(QueryGrid grid, String alertText, boolean hasSaveAndUndoButtons)
    {
        String actualText = grid.getEditAlertText();

        if(alertText != null && !alertText.isEmpty())
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

    }

    // Just a temporary test to make sure the changes to the test components are working as expected.
    @Test
    public void testTestComponentChanges()
    {
        goToProjectHome();

        resetDefaultView(VIEW_SAMPLE_TYPE, Arrays.asList(COL_NAME, COL_STRING, COL_INT));

        QueryGrid grid = beginAtQueryGrid(VIEW_SAMPLE_TYPE);

        if(grid.getColumnNames().contains(COL_INT))
        {
            log(String.format("Column '%s' is visible, hide it in the default view so insertion can be tested.", COL_INT));
            grid.hideColumn(COL_INT);
            grid.clickSave().setMakeDefaultForAll(true).saveView();
        }

        CustomizeGridDialog customizeModal = grid.insertColumn();

        log(String.format("Is 'Update' button enabled? %s", customizeModal.isUpdateGridEnabled()));

        log("Available Fields: " + customizeModal.getAvailableFields());
        log("Fields in Grid: " + customizeModal.getShownInGridLabels());
        log("Is 'Show all' checked: " + customizeModal.isShowAllChecked());
        customizeModal.setShowAll(true);
        log("Available Fields with 'Show all' checked: " + customizeModal.getAvailableFields());

        log(String.format("Add field '%s'.", COL_INT));
        customizeModal.addAvailableFieldToGrid(COL_INT);

        String fieldName = "Sample Set";
        log(String.format("Expand field '%s'.", fieldName));
        customizeModal.expandAvailableFields(fieldName);
        log("Available Fields after expanding): " + customizeModal.getAvailableFields());
        log(String.format("Collapse field '%s'.", fieldName));
        customizeModal.collapseAvailableField(fieldName);
        log("Available Fields after collapsing): " + customizeModal.getAvailableFields());

        fieldName = "Email";
        log(String.format("Add nested field '%s'.", fieldName));
        customizeModal.addAvailableFieldToGrid("Sample Set", "Created By", "Created By", fieldName);

        log("'Shown in Grid' before remove: " + customizeModal.getShownInGridLabels());
        customizeModal.removeShownInGridLabel(COL_INT);
        log("'Shown in Grid' after remove: " + customizeModal.getShownInGridLabels());

        customizeModal.addAvailableFieldToGrid(COL_INT);

        String newFieldLabel = String.format("My New Label %s", COL_INT);

        log("'Shown in Grid' before rename: " + customizeModal.getShownInGridLabels());
        customizeModal.updateFieldLabel(COL_INT, newFieldLabel);
        log("'Shown in Grid' after rename: " + customizeModal.getShownInGridLabels());

        log(String.format("Now is 'Update' button enabled? %s", customizeModal.isUpdateGridEnabled()));

        customizeModal.clickUpdateGrid();

        log("Columns: " + grid.getColumnNames());

        log(String.format("Select menu from column '%s' and validate that field label is highlighted in the dialog.", newFieldLabel));

        customizeModal = grid.insertColumn(newFieldLabel);

        log("Active field: " + customizeModal.getSelectedShownInGridLabel());

        log(String.format("Is undo enabled? %s", customizeModal.isUndoEditsEnabled()));

        String nextNewFieldLabel = newFieldLabel + "Something More";

        log(String.format("Update field '%s'.", newFieldLabel));

        customizeModal.updateFieldLabel(newFieldLabel, nextNewFieldLabel);

        log(String.format("Now is undo enabled? %s", customizeModal.isUndoEditsEnabled()));

        log(String.format("Fields before undo: %s", customizeModal.getShownInGridLabels()));

        log("Undo the edits.");
        customizeModal.clickUndoEdits();

        log(String.format("Fields after undo: %s", customizeModal.getShownInGridLabels()));

        log(String.format("Now remove the '%s' field from the grid.", newFieldLabel));

        customizeModal.removeShownInGridLabel(newFieldLabel).clickUpdateGrid();

        log("Columns after removing field: " + grid.getColumnNames());

    }

}
