package org.labkey.test.tests.component;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.FilterStatusValue;
import org.labkey.test.components.ui.grids.GridBar;
import org.labkey.test.components.ui.grids.GridFilterModal;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.components.ui.search.FilterExpressionPanel;
import org.labkey.test.components.ui.search.FilterFacetedPanel;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Category({Daily.class})
public class GridPanelTest extends BaseWebDriverTest
{

    private static final String TEST_SCHEMA = "samples";

    private static final int DEFAULT_PAGE_SIZE = 20;

    // A sample type with a small number of rows. Used to test paging controls (lack of) when results are small and no
    // filters are applied. Also used to test views (since that is a column centric feature).
    private static final String SMALL_SAMPLE_TYPE = "Small_SampleType";
    private static final int SMALL_SAMPLE_TYPE_SIZE = DEFAULT_PAGE_SIZE - 2;
    private static final String SMALL_SAMPLE_PREFIX = "SM-";

    // A sample type with a larger number of fields. Used for filtering and searching.
    private static final String FILTER_SAMPLE_TYPE = "Filter_SampleType";
    private static final int FILTER_SAMPLE_TYPE_SIZE = 300;
    private static final String FILTER_SAMPLE_PREFIX = "FST-";

    // Column names.
    private static final String FILTER_NAME_COL = "Name";
    private static final String FILTER_STRING_COL = "Str";
    private static final String FILTER_INT_COL = "Int";
    private static final String FILTER_EXTEND_CHAR_COL = "\u0106\u00D8\u0139";
    private static final String FILTER_BOOL_COL = "Bool";
    private static final String FILTER_DATE_COL = "Date";

    // Column removed from default view.
    private static final String REMOVED_FLAG_COLUMN = "Flag";

    // Views and columns used in the views. The views are only applied to the small sample type (Small_SampleType).
    private static final String VIEW_DEFAULT = "Default"; // In LKS the default view, even if modified is always named 'Default'.
    private static final String VIEW_DEFAULT_MODIFIED = "My Default"; // If you change the default view the menu item in the grid changes.
    private static final String VIEW_EXTRA_COLUMNS = "Extra_Columns";
    private static final String VIEW_FEWER_COLUMNS = "Fewer_Columns";
    private static final String VIEW_FILTERED_COLUMN = "Filtered_Column";
    private static final List<String> extraColumnsNames = Arrays.asList("IsAliquot", "GenId"); // Special case for adding the columns to the view and calling getRows api.
    private static final List<String> extraColumnsHeaders = Arrays.asList("Is Aliquot", "Gen Id"); // The column headers as they appear in the UI and exported file.
    private static final List<String> removedColumns = Arrays.asList(FILTER_BOOL_COL);

    // Various values used to populate Str field for records. Also used in filtering/searching.
    // Note: Small_SampleType is populated with random data and will have none of these values.
    private static final String EXTEND_RECORD_STRING = "\u01C5 \u01FC";
    private static final String EXTEND_RECORD_OTHER_STRING = "Not an extended value.";

    private static final String ONE_RECORD_STRING = "This will return one row.";

    private static final String FIVE_RECORD_STRING = "This will return five rows.";

    private static final String ONE_PAGE_STRING = "This will return one page of data.";

    private static final String MULTI_PAGE_STRING = "This will return more than one page of data.";
    private static final int MULTI_PAGE_COUNT = 3;

    private static final int NUMBER_FOR_STRING = 1234;
    private static final String NUMBER_STRING = String.format("This string has numbers %d in it.", NUMBER_FOR_STRING);

    private static final List<String> stringSetMembers = Arrays.asList("A", "B", "C", "D");
    private static List<String> stringSets = new ArrayList<>();

    // Number of entries to add that will have the NUMBER_STRING value.
    private static final int NUMBER_STRING_COUNT = 16;

    // Number of entries to add that will be an empty string.
    private static final int EMPTY_STRING_COUNT = 12;

    // Number of entries to add that will have extended characters.
    private static final int EXTEND_RECORD_COUNT = 10;

    // Upper value to put into the Int column.
    private static final int INT_MAX = 15;

    private static final String SELECTED_TEXT_FORMAT = "%d of %d selected";
    private static final String ALL_OPTION = "[All]";
    private static final String BLANK_OPTION = "[blank]";

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        GridPanelTest init = (GridPanelTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), null);

        // Add the 'Sample Types' web part. It is easier when debugging etc...
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.exitAdminMode();

        // Create list of strings that has the various sets of the letters A, B, C & D
        stringSets = getAllSets(stringSetMembers, stringSetMembers.size() - 1);
        // Remove the empty set.
        stringSets.remove(0);

        createSmallSampleType();

        createFilterSampleType();

    }

    // Create a small sample type that has one page of data by default (used in paging testing).
    // This sample type will also have custom saved views.
    // Not concerned about the data in the rows so use some random data.
    private void createSmallSampleType() throws IOException, CommandException
    {
        SampleTypeDefinition props = new SampleTypeDefinition(SMALL_SAMPLE_TYPE)
                .setFields(Arrays.asList(new FieldDefinition(FILTER_INT_COL, FieldDefinition.ColumnType.Integer),
                        new FieldDefinition(FILTER_STRING_COL, FieldDefinition.ColumnType.String),
                        new FieldDefinition(FILTER_DATE_COL, FieldDefinition.ColumnType.DateAndTime),
                        new FieldDefinition(FILTER_BOOL_COL, FieldDefinition.ColumnType.Boolean)));

        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);

        // Add a few rows where the STR column is knowable.
        int setIndex = 0;
        for(int rowCount = 1; rowCount <= SMALL_SAMPLE_TYPE_SIZE; rowCount++)
        {
            if(setIndex == stringSets.size())
                setIndex = 0;

            sampleSetDataGenerator.addCustomRow(
                    Map.of(FILTER_NAME_COL, String.format("%s%d", SMALL_SAMPLE_PREFIX, rowCount),
                            FILTER_INT_COL, sampleSetDataGenerator.randomInt(1, INT_MAX),
                            FILTER_STRING_COL, stringSets.get(setIndex++),
                            FILTER_DATE_COL, sampleSetDataGenerator.randomDateString(DateUtils.addWeeks(new Date(), -25), new Date()),
                            FILTER_BOOL_COL, sampleSetDataGenerator.randomBoolean())
            );
        }

        sampleSetDataGenerator.insertRows();

        // This modifies the default view of the grid. As a result the default view is now labeled "My Default" in the grid menu.
        removeFlagColumnFromDefaultView(SMALL_SAMPLE_TYPE);

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        drtSamples.goToView(VIEW_DEFAULT);

        log(String.format("Create the '%s' view for the '%s' sample type.", VIEW_EXTRA_COLUMNS, SMALL_SAMPLE_TYPE));
        CustomizeView cv = drtSamples.openCustomizeGrid();
        for(String columnName : extraColumnsNames)
        {
            cv.addColumn(columnName);
        }
        cv.saveCustomView(VIEW_EXTRA_COLUMNS);

        // Revert to the default view.
        drtSamples.goToView(VIEW_DEFAULT);

        log(String.format("Create the '%s' view for the '%s' sample type.", VIEW_FEWER_COLUMNS, SMALL_SAMPLE_TYPE));
        cv = drtSamples.openCustomizeGrid();

        for(String columnName : removedColumns)
        {
            cv.removeColumn(columnName);
        }
        cv.saveCustomView(VIEW_FEWER_COLUMNS);

        log(String.format("Finally create a view named '%s' for '%s' that only has a filter.", VIEW_FILTERED_COLUMN, SMALL_SAMPLE_TYPE));
        drtSamples.setFilter(FILTER_STRING_COL, "Contains One Of (example usage: a;b;c)", String.format("%1$s;%1$s%2$s", stringSetMembers.get(0), stringSetMembers.get(1)));
        cv = drtSamples.openCustomizeGrid();
        cv.saveCustomView(VIEW_FILTERED_COLUMN);

        goToProjectHome();

    }

    // Create a larger sample type that can be used for paging and filtering/searching tests.
    private void createFilterSampleType() throws IOException, CommandException
    {
        List<FieldDefinition> fields = new ArrayList<>();
        fields.add(new FieldDefinition(FILTER_STRING_COL, FieldDefinition.ColumnType.String));
        fields.add(new FieldDefinition(FILTER_INT_COL, FieldDefinition.ColumnType.Integer));
        fields.add(new FieldDefinition(FILTER_EXTEND_CHAR_COL, FieldDefinition.ColumnType.String));

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(FILTER_SAMPLE_TYPE).setFields(fields);

        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), sampleTypeDefinition);

        int fiveRecordCount = 0;
        int onePageCount = 0;
        int multiPageCount = 0;
        int emptyCount = 0;
        int extendedCharCount = 0;
        int stringWithNumCount = 0;

        int sampleId = 1;
        int intValue = 1;
        int allPossibleIndex = 0;

        // Sprinkle the values to be searched/filtered for throughout the sample type.
        // Records with the different values could be clumped together but this makes it a little more interesting and
        // is a bit more useful when testing selections based on search/filter results.
        while (sampleId <= FILTER_SAMPLE_TYPE_SIZE)
        {

            String filterColValue;
            String extColValue = EXTEND_RECORD_OTHER_STRING;

            if(sampleId == FILTER_SAMPLE_TYPE_SIZE / 2)
            {
                // Add the single unique row:
                filterColValue = ONE_RECORD_STRING;
            }
            else if(sampleId % 5 == 0 && fiveRecordCount < 5)
            {
                // Add a five row strings.
                filterColValue = FIVE_RECORD_STRING;
                fiveRecordCount++;
            }
            else if(sampleId % 3 == 0 && onePageCount < DEFAULT_PAGE_SIZE)
            {
                // Add a page string.
                filterColValue = ONE_PAGE_STRING;
                onePageCount++;
            }
            else if(sampleId % 2 == 0 && multiPageCount < DEFAULT_PAGE_SIZE * MULTI_PAGE_COUNT)
            {
                // Add a multi-page string.
                filterColValue = MULTI_PAGE_STRING;
                multiPageCount++;
            }
            else if(sampleId % 4 == 0 && emptyCount < EMPTY_STRING_COUNT)
            {
                // Add an empty string record.
                filterColValue = "";
                emptyCount++;
            }
            else if(sampleId % 7 == 0 && stringWithNumCount < NUMBER_STRING_COUNT)
            {
                // Add a number string record.
                filterColValue = NUMBER_STRING;
                stringWithNumCount++;
            }
            else
            {
                if(allPossibleIndex == stringSets.size())
                    allPossibleIndex = 0;

                filterColValue = stringSets.get(allPossibleIndex++);
                if(extendedCharCount < EXTEND_RECORD_COUNT)
                {
                    extColValue = EXTEND_RECORD_STRING;
                    extendedCharCount++;
                }

            }

            if(intValue > INT_MAX)
                intValue = 1;

            sampleSetDataGenerator.addCustomRow(
                    Map.of(FILTER_NAME_COL, String.format("%s%d", FILTER_SAMPLE_PREFIX, sampleId++),
                            FILTER_INT_COL, intValue++,
                            FILTER_STRING_COL, filterColValue,
                            FILTER_EXTEND_CHAR_COL, extColValue));

        }

        sampleSetDataGenerator.insertRows();

        // This modifies the default view of the grid. As a result the default view is now labeled "My Default" in the grid menu.
        removeFlagColumnFromDefaultView(FILTER_SAMPLE_TYPE);

    }

    /**
     * Helper to remove the 'Flag' column from the default view. It just gets in the way for some tests, and is easier
     * to remove it.
     *
     * @param sampleType Name of sample type.
     */
    private void removeFlagColumnFromDefaultView(String sampleType)
    {
        goToProjectHome();

        refresh();

        waitAndClickAndWait(Locator.linkWithText(sampleType));

        log(String.format("Remove '%s' column form default view.", REMOVED_FLAG_COLUMN));

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        drtSamples.goToView(VIEW_DEFAULT);

        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.removeColumn(REMOVED_FLAG_COLUMN);
        cv.saveCustomView("", true);

    }

    // Generate a list of string that has different combinations/sets from the list of strings passed in.
    // For example if values = ['A', 'B', 'C'] this will return the list ['', 'A', 'B', 'C', 'AB', 'AC', 'ABC', 'BC'].
    // This is all the sets of the characters from the list, including the empty set.
    private static List<String> getAllSets(List<String> values, int index)
    {
        List<String> allSets = new ArrayList<>();
        if(index < 0) {
            allSets.add("");
            return allSets;
        }

        allSets = getAllSets(values, index - 1);
        List<String> newSets = new ArrayList<>();

        for (String allCombination : allSets)
        {
            newSets.add(allCombination + values.get(index));
        }

        allSets.addAll(newSets);

        return allSets;
    }

    /**
     * Make sure there are no filters or search values persisted for the given sample type.
     *
     * @param sampleType The sample type used to populate the grid.
     * @return A queryGrid object.
     */
    public QueryGrid beforeTest(String sampleType)
    {
        QueryGrid grid = CoreComponentsTestPage.beginAt(this, getProjectName())
                .getGridPanel(TEST_SCHEMA, sampleType);

        // Selections can persist, clear them.
        grid.clearAllSelections();

        // Searches and filter values shouldn't persist, but clear them just to be safe.
        grid.clearFilters();
        grid.clearSearch();

        return grid;
    }

    /**
     * Validate that using the 'First Page' and 'Last Page' navigation works as expected.
     */
    @Test
    public void testFirstAndLastPageNavigation()
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        log("Check default paging values.");
        checker().verifyEquals("Start row number on first page not as expected.",
                1, grid.getGridBar().pager().start());

        checker().verifyEquals("Last row number on first page size not as expected.",
                DEFAULT_PAGE_SIZE, grid.getGridBar().pager().end());

        checker().screenShotIfNewError("Defaults_First_Page_Error");

        log("Go to the last page.");
        grid.getGridBar().jumpToPage("Last Page");

        checker()
                .withScreenshot("Last_Page_Dropdown_Menu_Visible")
                .verifyFalse("After selecting 'Last Page' the dropdown menu is still visible, it should not be.",
                        grid.getGridBar().pager().isPagingMenuVisible());

        int expectedNumOfPages = FILTER_SAMPLE_TYPE_SIZE / DEFAULT_PAGE_SIZE;
        checker().verifyEquals("Page number not as expected.",
                expectedNumOfPages, grid.getGridBar().getCurrentPage());

        int expectedStart = (FILTER_SAMPLE_TYPE_SIZE - DEFAULT_PAGE_SIZE) + 1;
        checker().verifyEquals("First row number on last page not as expected.",
                expectedStart, grid.getGridBar().pager().start());

        checker().verifyEquals("Last row number on last page size not as expected.",
                FILTER_SAMPLE_TYPE_SIZE, grid.getGridBar().pager().end());

        checker().screenShotIfNewError("Defaults_Last_Page_Error");

        log("Go back to the first page.");
        grid.getGridBar().jumpToPage("First Page");

        checker().verifyFalse("After selecting 'First Page' the dropdown menu is still visible, it should not be.",
                grid.getGridBar().pager().isPagingMenuVisible());

        checker().verifyEquals("Page number not as expected.",
                1, grid.getGridBar().getCurrentPage());
        checker().verifyEquals("Start sample count on first page not as expected.",
                1, grid.getGridBar().pager().start());
        checker().verifyEquals("Last sample count on first page size not as expected.",
                DEFAULT_PAGE_SIZE, grid.getGridBar().pager().end());

        checker().screenShotIfNewError("First_Page_Error");

    }

    /**
     * <p>
     *     Test that changing page size works as expected.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate default page size is as expected.</li>
     *         <li>Change page size to 100 and validate rows in grid update and pagination controls update as expected.</li>
     *         <li>Reset to defaults and make sure everything is right with the world.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testSelectPageSize()
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        log("Validate paging with defaults.");

        checker().verifyEquals("Start page not as expected.",
                1, grid.getGridBar().getCurrentPage());

        checker().verifyEquals("Start sample count on first page not as expected.",
                1, grid.getGridBar().pager().start());

        checker().verifyEquals("Last sample count on first page size not as expected.",
                DEFAULT_PAGE_SIZE, grid.getGridBar().pager().end());

        checker().verifyEquals("Total sample count on first page size not as expected.",
                FILTER_SAMPLE_TYPE_SIZE, grid.getGridBar().pager().total());

        checker().screenShotIfNewError("Defaults_Error");

        int newPageSize = 100;
        log(String.format("Now set page size to %d.", newPageSize));

        grid.getGridBar().selectPageSize(Integer.toString(newPageSize));

        log("Check to see that the pager thinks it's all true.");

        checker()
                .withScreenshot("Larger_Page_Size_Dropdown_Menu_Visible")
                .verifyFalse("After selecting a page size the dropdown menu is still visible, it should not be.",
                        grid.getGridBar().pager().isPagingMenuVisible());

        checker().verifyTrue("After resize there are no paging control, there should be.",
                grid.getGridBar().pager().hasPaginationControls());

        checker().verifyEquals("Start page, after page resize, not as expected.",
                1, grid.getGridBar().getCurrentPage());

        checker().verifyEquals("Start sample count on first page, after page resize, not as expected.",
                1, grid.getGridBar().pager().start());

        checker().verifyEquals("Last sample count on first page, after page resize, not as expected.",
                newPageSize, grid.getGridBar().pager().end());

        checker().verifyEquals("Total sample count on first page, after page resize, not as expected.",
                FILTER_SAMPLE_TYPE_SIZE, grid.getGridBar().pager().total());

        log("But don't take the pager's word for it, count the visible rows in the grid.");

        checker().verifyEquals("Total sample rows on page, after page resize, not as expected.",
                newPageSize, grid.getRows().size());

        checker().screenShotIfNewError("Resize_Error");

        log(String.format("Set page size back to the default size %d.", DEFAULT_PAGE_SIZE));
        grid.getGridBar().selectPageSize(Integer.toString(DEFAULT_PAGE_SIZE));

        checker()
                .withScreenshot("Smaller_page_Size_Dropdown_Menu_Visible")
                .verifyFalse("After selecting a page size the dropdown menu is still visible, it should not be.",
                        grid.getGridBar().pager().isPagingMenuVisible());

        checker().verifyTrue("After resetting there are no paging control, there should be.",
                grid.getGridBar().pager().hasPaginationControls());

        checker().verifyEquals("After reset, current page not as expected.",
                1, grid.getGridBar().getCurrentPage());

        checker().verifyEquals("After reset, first sample index not as expected.",
                1, grid.getGridBar().pager().start());

        checker().verifyEquals("After reset, last index on page not as expected.",
                DEFAULT_PAGE_SIZE, grid.getGridBar().pager().end());

        checker().verifyEquals("After reset, number of rows in grid not as expected.",
                FILTER_SAMPLE_TYPE_SIZE, grid.getGridBar().pager().total());

        checker().screenShotIfNewError("Reset_Error");

    }

    /**
     * <p>
     *     Validate that a single page of unfiltered data works as expected.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Load a small sample type with 20 records.</li>
     *         <li>Validate counts and pagination is not present.</li>
     *         <li>The 'Select All' button is not present.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testSinglePageOfData()
    {
        QueryGrid grid = beforeTest(SMALL_SAMPLE_TYPE);

        String expectedText = String.format("1 - %d", SMALL_SAMPLE_TYPE_SIZE);
        String actualText = grid.getGridBar().pager().summary();
        checker().verifyEquals("Sample count not as expected.", expectedText, actualText);

        checker().verifyFalse("There should be no paging control for a single page of data.",
                grid.getGridBar().pager().hasPaginationControls());

        checker().verifyFalse("There should be no 'Select All' button for a single page of data.",
                grid.hasSelectAllButton());

        grid.selectAllRows();

        expectedText = String.format(SELECTED_TEXT_FORMAT, SMALL_SAMPLE_TYPE_SIZE, SMALL_SAMPLE_TYPE_SIZE);
        actualText = grid.getSelectionStatusCount();

        checker().verifyEquals("Selected text count not as expected.",
                expectedText, actualText);

    }

    /**
     * <p>
     *     Validate the 'Select All' button with filtered grid.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Filter the grid to a result set that has several pages.</li>
     *         <li>Use the 'Select All' button to select all results.</li>
     *         <li>Remove the filter and verify that the selections have not changed.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testSelectAllButtonWithFilteredResults()
    {
        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        log("Validate that the 'Select All' button works as expected.");
        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.EQUAL, MULTI_PAGE_STRING);

        checker().fatal()
                .verifyTrue("The 'Select All' button is not present.", grid.hasSelectAllButton());

        grid.selectAllRows();
        grid.removeColumnFilter(FILTER_STRING_COL);
        checker().withScreenshot("Select_All_Button_Error")
                .verifyEquals("Number of selected rows not as expected after filter removed.",
                        String.format(SELECTED_TEXT_FORMAT, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE, FILTER_SAMPLE_TYPE_SIZE), grid.getSelectionStatusCount());

        grid.clearAllSelections();

    }

    /**
     * <p>
     *     Verify that selecting all on a page with a result set works as expected.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Filter the grid to a result with multiple pages.</li>
     *         <li>Use the check box at the top of the grid to select all samples in the current page.</li>
     *         <li>Remove filter and validate that selections persist.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testSelectAllOnPageWithFilteredResults()
    {
        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        log("Validate the select all check box at the top of the gird works as expected.");
        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.EQUAL, MULTI_PAGE_STRING);
        grid.selectAllOnPage(true);
        grid.removeColumnFilter(FILTER_STRING_COL);
        checker().withScreenshot("Select_All_On_Page_Error")
                .verifyEquals("Using check box at top of grid did not select the expected samples.",
                        String.format(SELECTED_TEXT_FORMAT, DEFAULT_PAGE_SIZE, FILTER_SAMPLE_TYPE_SIZE), grid.getSelectionStatusCount());

        grid.clearAllSelections();

    }

    /**
     * <p>
     *     Test the interaction between the select all on the page option and the 'Select All' button. This covers
     *     issues 39011 and 41171.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Filter the grid to multi-page results.</li>
     *         <li>First use the select all on the page option to select some rows.</li>
     *         <li>Then use the 'Select All' button to all the filtered results.</li>
     *         <li>Remove the filter and validate the selected rows persisted.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testSelectOnPageAndSelectAllButton()
    {
        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        log("Filter grid to multiple pages.");
        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.EQUAL, MULTI_PAGE_STRING);

        log("Use the check box to select all on the page.");
        grid.selectAllOnPage(true);
        checker().withScreenshot("Select_All_On_Page_Error")
                .verifyEquals("Using check box at top of grid did not select the expected samples.",
                        String.format(SELECTED_TEXT_FORMAT, DEFAULT_PAGE_SIZE, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE),
                        grid.getSelectionStatusCount());

        log("Use the 'Select All' button to select all of the filtered rows.");
        checker().fatal()
                .verifyTrue("The 'Select All' button is not present.", grid.hasSelectAllButton());

        grid.selectAllRows();
        checker().withScreenshot("Select_All_Button_With_Some_Selected_Error")
                .verifyEquals("Number of selected rows not as expected.",
                        String.format(SELECTED_TEXT_FORMAT, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE),
                        grid.getSelectionStatusCount());

        log("Remove the filter and validate that the selections persisted.");
        grid.removeColumnFilter(FILTER_STRING_COL);

        checker().withScreenshot("Select_After_Filter_Removed_Error")
                .verifyEquals("Number of selected rows not as expected after filter removed.",
                        String.format(SELECTED_TEXT_FORMAT, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE, FILTER_SAMPLE_TYPE_SIZE),
                        grid.getSelectionStatusCount());

        log("Apply the filter again...");
        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.EQUAL, MULTI_PAGE_STRING);

        checker().withScreenshot("Select_Filter_Reapplied_Error")
                .verifyEquals("Indicated number of selected rows not as expected after filter was reapplied.",
                        String.format(SELECTED_TEXT_FORMAT, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE),
                        grid.getSelectionStatusCount());

        log("Unselect all of the rows on the first page.");
        grid.selectAllOnPage(false);

        checker().withScreenshot("Select_Clear_Page_Error")
                .verifyEquals("Indicated number of selected rows not as expected after first page cleared.",
                        String.format(SELECTED_TEXT_FORMAT, (MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE) - DEFAULT_PAGE_SIZE, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE), grid.getSelectionStatusCount());

        log("Remove the filter, and validate selection count.");
        grid.removeColumnFilter(FILTER_STRING_COL);

        checker().withScreenshot("Select_Remove_Page_Error")
                .verifyEquals("Indicated number of selected rows not as expected after first page cleared and filter removed.",
                        String.format(SELECTED_TEXT_FORMAT, (MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE) - DEFAULT_PAGE_SIZE, FILTER_SAMPLE_TYPE_SIZE), grid.getSelectionStatusCount());

        grid.clearAllSelections();

    }

    /**
     * Validate that filtering on 'Is Blank' and 'Is Not Blank' return the expected results.
     */
    @Test
    public void testIsBlankAndIsNotBlankFilter()
    {
        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        log("Filter grid to only rows with blank entries.");
        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.ISBLANK);

        String expectedCount = String.format("1 - %d", EMPTY_STRING_COUNT);

        checker().withScreenshot("Is_Blank_Error")
                .verifyEquals("Filtering for 'Is Blank' did not return expected number of rows.",
                        expectedCount, grid.getGridBar().pager().summary());

        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.NONBLANK);

        int notBlankCount = FILTER_SAMPLE_TYPE_SIZE - EMPTY_STRING_COUNT;

        expectedCount = String.format("1 - %d of %d", DEFAULT_PAGE_SIZE, notBlankCount);

        checker().withScreenshot("Is_Not_Blank_Error")
                .verifyEquals("Filtering for 'Is Not Blank' did not return expected number of rows.",
                        expectedCount, grid.getGridBar().pager().summary());

        grid.removeColumnFilter(FILTER_STRING_COL);
    }

    /**
     * Apply two filters to the Int column. Filter for values between a low and a high value.
     */
    @Test
    public void testMultipleFiltersOnOneColumn() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        int low = INT_MAX - 3;
        int high = INT_MAX;

        log(String.format("Filter the '%s' for values greater than %d and less than or equal to %d.", FILTER_INT_COL, low, high));

        grid.filterColumn(FILTER_INT_COL, Filter.Operator.GT, low, Filter.Operator.LTE, high);

        // Query the table to get the expected number of rows.
        List<Filter> filters = List.of(
                new Filter(FILTER_INT_COL, low, Filter.Operator.getOperator("GREATER_THAN")),
                new Filter(FILTER_INT_COL, high, Filter.Operator.getOperator("LESS_THAN_OR_EQUAL")));
        int expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        checker().withScreenshot("Multiple_Filters_One_Column_Error")
                .verifyEquals("Number of records returned for filter not as expected.",
                        expectedCount, grid.getRecordCount());

        grid.clearFilters();
    }

    /**
     * Validate that if required fields are not set for filter the dialog shows an error. Also validate that for some
     * field types, like an integer, there is only a Filter tab and not a Values tab in the filter dialog.
     */
    @Test
    public void testFilterErrorAndFilterOnlyTab()
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        log(String.format("Filter on the '%s' field. This should only provide a 'Filter' tab.", FILTER_INT_COL));

        filterDialog.selectField(FILTER_INT_COL);

        checker().verifyFalse(String.format("There should be no 'Choose Values' tab for the '%s' field.", FILTER_INT_COL),
                filterDialog.getTabText().contains("Choose Values"));

        log("Select the 'Filter' tab but only provide one condition when two are expected.");

        FilterExpressionPanel panel = filterDialog.selectExpressionTab();
        panel.setFilters(
                new FilterExpressionPanel.Expression(Filter.Operator.GT, 1),
                new FilterExpressionPanel.Expression(Filter.Operator.LT, "")
        );

        String errorMsg = filterDialog.confirmExpectingError();

        checker().verifyEquals("Expected error message not present.",
                String.format("Missing filter values for: %s.", FILTER_INT_COL), errorMsg);

        checker().screenShotIfNewError("Filter_Dialog_Error");

        log("Cancel the dialog and validate that no filters applied.");

        filterDialog.cancel();

        checker().withScreenshot("Unexpected_Filter_Error")
                .verifyTrue("It looks like there are filters applied after canceling out of the dialog.",
                        grid.getFilterStatusValues(true).isEmpty());

    }

    /**
     * <p>
     *     Test interaction between the 'Filter' and 'Choose Value' tabs.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate that the list of values in the 'Choose Values' tab is, more or less, as expected when no filters are applied.</li>
     *         <li>Apply a filter and validate that the list of values is as expected.</li>
     *         <li>Apply an 'equals' filter and validate that the values is selected in the 'Choose Values' tab.</li>
     *         <li>Select another value from the list and validate that the 'Filter' tab updates to a 'One of' filter.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testInteractionBetweenDialogTabs() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        log(String.format("Select '%s' and get the list of values for the field before filtering.", FILTER_STRING_COL));

        filterDialog.selectField(FILTER_STRING_COL);

        FilterFacetedPanel facetedPanel = filterDialog.selectFacetTab();

        List<String> actualValues = facetedPanel.getAvailableValues();

        List<String> expectedValues = new ArrayList<>();
        expectedValues.add(ALL_OPTION);
        expectedValues.add(BLANK_OPTION);
        expectedValues.add(ONE_PAGE_STRING);
        expectedValues.add(FIVE_RECORD_STRING);
        expectedValues.add(MULTI_PAGE_STRING);
        expectedValues.add(NUMBER_STRING);
        expectedValues.addAll(stringSets);

        checker().fatal()
                .verifyTrue(String.format("List of values to choose from for field '%s' not as expected before filtering. Fatal error.", FILTER_STRING_COL),
                        actualValues.containsAll(expectedValues));

        filterDialog.selectField(FILTER_NAME_COL);

        // This should return samples named FST-10, and FST-100 - FST-109
        String filteredNames = "-10";
        log(String.format("Filter the '%s' to values that contain '%s'.", FILTER_NAME_COL, filteredNames));

        FilterExpressionPanel expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.CONTAINS, filteredNames));

        log("Apply the filter.");
        filterDialog.confirm();

        log(String.format("Open the dialog again and validate that the list of values to select from for '%s' is reduced.", FILTER_STRING_COL));
        filterDialog = grid.getGridBar().openFilterDialog();

        filterDialog.selectField(FILTER_STRING_COL);

        facetedPanel = filterDialog.selectFacetTab();
        actualValues = facetedPanel.getAvailableValues();

        // Hard coding the string combinations (AB, AC, etc...) to make the code more readable.
        expectedValues = Arrays.asList(ALL_OPTION, BLANK_OPTION, "AB", "AC", "BC", "C", NUMBER_STRING, FIVE_RECORD_STRING, MULTI_PAGE_STRING);

        Collections.sort(actualValues);
        Collections.sort(expectedValues);

        checker().withScreenshot("Expected_Faceted_Values_Error")
                .verifyEquals(String.format("List of values to choose from for field '%s' not as expected after the filter was applied.", FILTER_STRING_COL),
                        expectedValues, actualValues);

        log("Clear the filter.");

        filterDialog.cancel();

        grid.clearFilters();

        log("Validate that applying a filter for a fields checks the values in the dialog.");

        filterDialog = grid.getGridBar().openFilterDialog();
        filterDialog.selectField(FILTER_STRING_COL);

        String firstFilterValue = stringSets.get(0);

        log(String.format("Filter '%s' to values equal to '%s'.", FILTER_STRING_COL, firstFilterValue));
        expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.EQUAL, firstFilterValue));

        log(String.format("Go to the 'Choose Value' tab and validate that '%s' is selected.", firstFilterValue));

        facetedPanel = filterDialog.selectFacetTab();
        actualValues = facetedPanel.getSelectedValues();
        expectedValues = Arrays.asList(firstFilterValue);

        checker().withScreenshot("Filtered_Value_Not_Selected")
                .verifyEquals("The selected/filtered value is not as expected.",
                        expectedValues, actualValues);

        String secondFilterValue = stringSets.get(1);
        log(String.format("Now select '%s' from the list and validate that the filter is updated.", secondFilterValue));
        facetedPanel.checkValues(firstFilterValue, secondFilterValue);

        expressionPanel = filterDialog.selectExpressionTab();

        // There are no getters for the filters in the expression tab, and implementing them in the test component
        // would be non-trivial. It is also unlikely that any functional tests outside this one would need to get the
        // filters. Because of those two reasons this test will have some specific code to get various WebElements in the
        // panel (filter values) and validate they have the expected text/values.

        WebElement panelElement = expressionPanel.getComponentElement();
        List<ReactSelect> filterTypes = new ReactSelect.ReactSelectFinder(getDriver()).findAll(panelElement);

        checker().verifyEquals("The first filter expression is not as expected.",
                "Equals One Of", filterTypes.get(0).getValue());

        checker().verifyTrue("The second filter expression should be empty.",
                filterTypes.get(1).getValue().isEmpty());

        List<WebElement> filterValues = Locator.tagWithClass("input", "filter-expression__input").findElements(panelElement);

        checker().verifyEquals("There should only be one filter value text box.",
                1, filterValues.size());

        checker().verifyEquals("The filter value is not as expected.",
                String.format("%s;%s", firstFilterValue, secondFilterValue), getFormElement(filterValues.get(0)));

        checker().screenShotIfNewError("Updated_Filter_Error");

        log("Apply the filter and validate.");
        filterDialog.confirm();

        // Query the table to get the expected number of rows to be returned.
        List<Filter> filters = List.of(
                new Filter(FILTER_STRING_COL, String.format("%s;%s", firstFilterValue, secondFilterValue),
                        Filter.Operator.getOperator("IN")));
        int expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        checker().verifyEquals("Filter did not return the expected number of rows.",
                expectedCount, grid.getRecordCount());

    }

    /**
     * <p>
     *     Validate that using filters with the search box works as expected.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Apply multiple filters to two columns.</li>
     *         <li>Enter a valid search value in the search box (validate count of the records in grid).</li>
     *         <li>Remove the filters, and validate search is still set.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testSearchAndFilter() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        int low = 4;
        int high = INT_MAX - 3;

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        filterDialog.selectField(FILTER_INT_COL);

        log(String.format("Filter '%s' for values between %d and %d.", FILTER_INT_COL, low, high));

        FilterExpressionPanel expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilters(new FilterExpressionPanel.Expression(Filter.Operator.GT, low),
                new FilterExpressionPanel.Expression(Filter.Operator.LT, high));

        filterDialog.selectField(FILTER_STRING_COL);

        filterDialog.selectFacetTab();

        String oneOfFilter = String.format("%s;%s;%s", stringSetMembers.get(0), stringSetMembers.get(1), stringSetMembers.get(2));

        log(String.format("Filter '%s' to have one of '%s'.", FILTER_STRING_COL, oneOfFilter));

        expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.CONTAINS_ONE_OF, oneOfFilter));

        filterDialog.confirm();

        checker().verifyTrue("'Remove all' button should be visible when filters are applied.",
                grid.hasRemoveAllButton());

        // Query the table to get the expected number of rows to be returned. Will modify this list later so make it mutable.
        List<Filter> filters = new ArrayList<>();
        filters.add(new Filter(FILTER_STRING_COL, oneOfFilter, Filter.Operator.getOperator("CONTAINS_ONE_OF")));
        filters.add(new Filter(FILTER_INT_COL, low, Filter.Operator.getOperator("GREATER_THAN")));
        filters.add(new Filter(FILTER_INT_COL, high, Filter.Operator.getOperator("LESS_THAN")));
        int expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        checker().verifyEquals("Number of rows after filter applied not as expected.",
                expectedCount, grid.getRecordCount());

        checker().screenShotIfNewError("Initial_Filter_Error");

        String searchString = String.format("%s%s", stringSetMembers.get(0), stringSetMembers.get(1));

        log(String.format("Set search value to '%s'.", searchString));

        grid.getGridBar().searchFor(searchString);

        // Because the search string is specific to the Str column a query can be used to get the expected count.
        filters.add(new Filter(FILTER_STRING_COL, searchString, Filter.Operator.getOperator("CONTAINS")));
        expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        checker().withScreenshot("Filter_With_Search_Error")
                .verifyEquals("Number of rows after filter applied not as expected.",
                        expectedCount, grid.getRecordCount());

        log("Remove all of the filters.");

        grid = grid.clickRemoveAllButton();

        checker().verifyEquals("Search expression not as expected after clearing filters.",
                searchString, grid.getGridBar().getSearchExpression());

        // Again can use a query to get the expected number of rows from the search.
        filters = new ArrayList<>();
        filters.add(new Filter(FILTER_STRING_COL, searchString, Filter.Operator.getOperator("CONTAINS")));
        expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        checker().verifyEquals("Number of rows after filter were cleared not as expected.",
                expectedCount, grid.getRecordCount());

        checker().screenShotIfNewError("Search_With_Filters_Removed_Error");
    }

    /**
     * <p>
     *     Test the 'filter pills', the oval button with filter info, that are shown above the grid.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate that filtered fields are marked in the dialog.</li>
     *         <li>The pills show the filter selected.</li>
     *         <li>Can remove the filter by clicking the 'x' on the pill.</li>
     *         <li>Validate that clicking on a pill open the filter dialog with the expected values.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testFilterPills() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        int high = INT_MAX - 3;
        int low = 3;

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        filterDialog.selectField(FILTER_INT_COL);

        log(String.format("Filter '%s' for values greater than %d and less than %d.", FILTER_INT_COL, low, high));

        FilterExpressionPanel expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilters(new FilterExpressionPanel.Expression(Filter.Operator.GT, low),
                new FilterExpressionPanel.Expression(Filter.Operator.LT, high));

        filterDialog.selectField(FILTER_STRING_COL);

        String oneOfFilter = String.format("%s;%s;%s", stringSetMembers.get(0), stringSetMembers.get(1), stringSetMembers.get(2));

        log(String.format("Filter '%s' to have one of '%s'.", FILTER_STRING_COL, oneOfFilter));

        expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.CONTAINS_ONE_OF, oneOfFilter));

        List<String> expectedValues = new ArrayList<>();
        expectedValues.add(FILTER_INT_COL);
        expectedValues.add(FILTER_STRING_COL);

        List<String> actualValues = filterDialog.getFilteredFields();

        Collections.sort(expectedValues);
        Collections.sort(actualValues);

        checker().withScreenshot("Filtered_Fields_Not_Marked")
                .verifyEquals(String.format("The '%s' and '%s' fields are not marked as filtered.", FILTER_INT_COL, FILTER_STRING_COL),
                expectedValues, actualValues);

        filterDialog.confirm();

        String pillToRemove = String.format("%s < %d", FILTER_INT_COL, high);

        expectedValues = new ArrayList<>();
        expectedValues.add(pillToRemove);
        expectedValues.add(String.format("%s > %d", FILTER_INT_COL, low));
        expectedValues.add(String.format("%s Contains One Of %s", FILTER_STRING_COL, oneOfFilter.replace(";", ", ")));

        List<FilterStatusValue> filterPills = grid.getFilterStatusValues(false);

        actualValues = filterPills.stream().map(FilterStatusValue::getText).collect(Collectors.toList());

        Collections.sort(expectedValues);
        Collections.sort(actualValues);

        checker().verifyEquals("Filter 'pills' not as expected.",
                expectedValues, actualValues);

        List<Filter> filters = List.of(
                new Filter(FILTER_INT_COL, low, Filter.Operator.getOperator("GREATER_THAN")),
                new Filter(FILTER_INT_COL, high, Filter.Operator.getOperator("LESS_THAN")),
                new Filter(FILTER_STRING_COL, oneOfFilter, Filter.Operator.getOperator("CONTAINS_ONE_OF")));
        int expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        checker().verifyEquals("Rows returned after filters applied not as expected.",
                expectedCount, grid.getRecordCount());

        checker().screenShotIfNewError("Filter_Pill_Error");

        log(String.format("Remove the filter pill '%s'.", pillToRemove));
        for(FilterStatusValue filterPill : filterPills)
        {
            if(filterPill.getText().equals(pillToRemove))
            {
                filterPill.remove();
                break;
            }
        }

        filters = List.of(
                new Filter(FILTER_INT_COL, low, Filter.Operator.getOperator("GREATER_THAN")),
                new Filter(FILTER_STRING_COL, oneOfFilter, Filter.Operator.getOperator("CONTAINS_ONE_OF")));
        expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        checker().verifyEquals("Rows returned after filter pill removed not as expected.",
                expectedCount, grid.getRecordCount());

        checker().screenShotIfNewError("Filter_Pill_Remove_Error");

        // Need to change the focus. After removing the first filter the mouse is in the same position which causes the
        // next pill to get the 'x' icon. This causes the next call to getFilterStatusValues to not recognize the pill as a filter.
        Locator.tagWithClass("input", "grid-panel__search-input").findElement(getDriver()).click();

        FilterStatusValue filterPill = grid.getFilterStatusValues(false).get(0);

        filterPill.getComponentElement().click();

        filterDialog = new GridFilterModal(getDriver(), grid);

        // TODO: Need to figure out how to get the active tab in the dialog.
        /*
        checker().withScreenshot("Default_Tab_Error")
                .verifyEquals("Should have selected the 'Filter' tab.",
                        "Filter", filterDialog.getActiveTab());
        */

        expectedValues = new ArrayList<>();
        expectedValues.add(FILTER_INT_COL);
        expectedValues.add(FILTER_STRING_COL);

        actualValues = filterDialog.getFilteredFields();

        Collections.sort(expectedValues);
        Collections.sort(actualValues);

        checker().verifyEquals(String.format("Both the '%s' and '%s' fields should be marked as filtered.", FILTER_INT_COL, FILTER_STRING_COL),
                expectedValues, actualValues);

        filterDialog.selectField(FILTER_INT_COL);

        expressionPanel = filterDialog.selectExpressionTab();

        WebElement panelElement = expressionPanel.getComponentElement();
        List<ReactSelect> filterTypes = new ReactSelect.ReactSelectFinder(getDriver()).findAll(panelElement);

        checker().verifyEquals(String.format("Filter expression for '%s' is not as expected.", FILTER_INT_COL),
                "Is Greater Than", filterTypes.get(0).getValue());

        WebElement filterValues = Locator.tagWithClass("input", "filter-expression__input").findElement(panelElement);

        checker().verifyEquals(String.format("The filter value for '%s' is not as expected.", FILTER_INT_COL),
                Integer.toString(low), getFormElement(filterValues));

        checker().screenShotIfNewError("Populated_Filter_Int_Field_Error");

        filterDialog.selectField(FILTER_STRING_COL);

        expressionPanel = filterDialog.selectExpressionTab();

        panelElement = expressionPanel.getComponentElement();
        filterTypes = new ReactSelect.ReactSelectFinder(getDriver()).findAll(panelElement);

        checker().verifyEquals(String.format("Filter expression for '%s' is not as expected.", FILTER_STRING_COL),
                "Contains One Of", filterTypes.get(0).getValue());

        filterValues = Locator.tagWithClass("input", "filter-expression__input").findElement(panelElement);

        checker().verifyEquals(String.format("The filter value for '%s' is not as expected.", FILTER_STRING_COL),
                oneOfFilter, getFormElement(filterValues));

        checker().screenShotIfNewError("Populated_Filter_Int_Field_Error");

    }

    /**
     * Validate that the search field is the same as a contains filter.
     */
    @Test
    public void testSearchIsSameAsContainsFilter() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        // Set search string to 'AB'.
        String searchString = stringSetMembers.get(0) + stringSetMembers.get(1);

        log(String.format("Set the search field to '%s'.", searchString));

        grid.getGridBar().searchFor(searchString);

        // Identify how many rows should be returned.
        List<Filter> filters = List.of(
                new Filter(FILTER_STRING_COL, searchString, Filter.Operator.getOperator("CONTAINS")));
        int expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        int actualCount = grid.getRecordCount();

        checker().withScreenshot("Search_Error")
                .verifyEquals("Number of records returned from search not as expected.",
                        expectedCount, actualCount);

        log("Now validate same number returned with a contains filter.");

        grid.getGridBar().clearSearch();

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        filterDialog.selectField(FILTER_STRING_COL);

        FilterExpressionPanel expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.CONTAINS, searchString));
        filterDialog.confirm();

        actualCount = grid.getRecordCount();

        checker().withScreenshot("Filter_Contains_Error")
                .verifyEquals("Number of records returned from filter contains not as expected.",
                        expectedCount, actualCount);

    }

    /**
     * <p>
     *     Validate search woks on multiple columns and only on string columns.
     * </p>
     * <p>
     *     This test will search for a number (12) and should get hits in the Str and Name columns but ignore the Int column.
     * </p>
     * @throws IOException Can be thrown by the call to getRows (used to identify samples that should not be returned).
     * @throws CommandException Can be thrown by the call to getRows (used to identify samples that should not be returned).
     */
    @Test
    public void testSearchAcrossColumns() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        // Something of a "magic number". A search for 12 will find values in sample name and the Str column.
        String searchString = "12";

        log(String.format("Set the search field to '%s'.", searchString));

        grid.getGridBar().searchFor(searchString);

        grid.getGridBar().pager().selectPageSize("40");

        // Have to use a "magical number" for the expected number of rows returned by the search.
        int expectedCount = 29;
        int actualCount = grid.getRecordCount();

        checker().verifyEquals("Number of records returned from search not as expected.",
                expectedCount, actualCount);

        log(String.format("Validate that the result set did not return a row where the search value was in the %s field.", FILTER_INT_COL));

        List<String> actualIds = grid.getColumnDataAsText(FILTER_NAME_COL);

        // Query the table to get a list of samples/rows that should not be in the result from the search.
        List<Filter> filters = List.of(new Filter(FILTER_NAME_COL, searchString, Filter.Operator.getOperator("DOES_NOT_CONTAIN")),
                new Filter(FILTER_STRING_COL, searchString, Filter.Operator.getOperator("DOES_NOT_CONTAIN")),
                new Filter(FILTER_EXTEND_CHAR_COL, searchString, Filter.Operator.getOperator("DOES_NOT_CONTAIN")),
                new Filter(FILTER_INT_COL, searchString, Filter.Operator.getOperator("EQUAL")));
        List<Map<String, Object>> rows = getExpectedResults(FILTER_SAMPLE_TYPE, null, Arrays.asList(FILTER_NAME_COL), filters);

        boolean error = false;
        StringBuilder errorMessage = new StringBuilder();
        for(Map<String, Object> row : rows)
        {
            String id = row.get(FILTER_NAME_COL).toString();
            if(actualIds.contains(id))
            {
                error = true;
                errorMessage.append(String.format("Sample '%s' should not have been returned.\n", id));
            }
        }

        checker().verifyFalse(errorMessage.toString(),
                error);

    }

    /**
     * Test that searching and filtering for extended characters works as expected. Will also validate filtering on a
     * column name with extended characters.
     */
    @Test
    public void testFilteringAndSearchingForExtendedCharacters() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        log(String.format("Search for '%s'.", EXTEND_RECORD_STRING));

        grid.getGridBar().searchFor(EXTEND_RECORD_STRING);

        checker().withScreenshot("Search_Extended_Error")
                .verifyEquals(String.format("Number of records returned when searching for '%s' not as expected.", EXTEND_RECORD_STRING),
                        EXTEND_RECORD_COUNT, grid.getRecordCount());

        grid.getGridBar().clearSearch();

        log(String.format("Filter the '%s' column for value '%s'.", FILTER_EXTEND_CHAR_COL, EXTEND_RECORD_STRING));

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        filterDialog.selectField(FILTER_EXTEND_CHAR_COL);

        FilterExpressionPanel expressionPanel = filterDialog.selectExpressionTab();
        expressionPanel.setFilter(new FilterExpressionPanel.Expression(Filter.Operator.CONTAINS, EXTEND_RECORD_STRING));
        filterDialog.confirm();

        List<Filter> filters = List.of(
                new Filter(FILTER_EXTEND_CHAR_COL, EXTEND_RECORD_STRING, Filter.Operator.getOperator("EQUAL")));
        int expectedCount = getExpectedResults(FILTER_SAMPLE_TYPE, null, null, filters).size();

        checker().withScreenshot("Filter_Extended_Error")
                .verifyEquals(String.format("Number of records returned when filtering column '%s' for '%s' not as expected.",
                                FILTER_EXTEND_CHAR_COL, EXTEND_RECORD_STRING),
                        expectedCount, grid.getRecordCount());

    }

    /**
     * Validate that exporting with a filter applied only exports the filtered rows.
     * Validate that exporting when there are selected rows only exports the selected rows.
     * Validate exporting a view has the appropriate columns.
     */
    @Test
    public void testExport() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        log("Filter the grid and validate only the filtered results are exported.");
        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.EQUAL, MULTI_PAGE_STRING);

        List<String> columns = Arrays.asList(FILTER_NAME_COL, FILTER_STRING_COL, FILTER_INT_COL, FILTER_EXTEND_CHAR_COL);
        List<Filter> filters = List.of(
                new Filter(FILTER_STRING_COL, MULTI_PAGE_STRING, Filter.Operator.getOperator("EQUAL")));
        List<Map<String, Object>> expectedValues = getExpectedResults(FILTER_SAMPLE_TYPE, null, columns, filters);

        File exportedFile = grid.getGridBar().exportData(GridBar.ExportType.CSV);

        validateExportedData(exportedFile, expectedValues, columns);

        log("Validate that if there are selected rows only they are exported.");
        grid.clearFilters();

        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.EQUAL, ONE_PAGE_STRING);

        grid.selectAllRows();

        grid.clearFilters();

        filters = List.of(
                new Filter(FILTER_STRING_COL, ONE_PAGE_STRING, Filter.Operator.getOperator("EQUAL")));
        expectedValues = getExpectedResults(FILTER_SAMPLE_TYPE, null, columns, filters);

        exportedFile = grid.getGridBar().exportData(GridBar.ExportType.CSV);

        validateExportedData(exportedFile, expectedValues, columns);

        log(String.format("Using sample type '%s' validate that if a view is selected the expected columns are exported.", SMALL_SAMPLE_TYPE));

        grid = beforeTest(SMALL_SAMPLE_TYPE);

        log(String.format("Select the '%s' view.", VIEW_EXTRA_COLUMNS));
        grid.selectView(VIEW_EXTRA_COLUMNS);

        exportedFile = grid.getGridBar().exportData(GridBar.ExportType.CSV);

        // The extra column added have spaces in the name so the column header in the exported file, so use the header values.
        columns = new ArrayList<>(extraColumnsHeaders);
        columns.add(FILTER_DATE_COL);
        columns.add(FILTER_NAME_COL);
        columns.add(FILTER_BOOL_COL);
        columns.add(FILTER_STRING_COL);
        columns.add(FILTER_INT_COL);
        validateExportedColumnHeader(exportedFile, columns, new ArrayList<>());

        log(String.format("Now select the '%s' view.", VIEW_FEWER_COLUMNS));
        grid.selectView(VIEW_FEWER_COLUMNS);

        exportedFile = grid.getGridBar().exportData(GridBar.ExportType.CSV);

        columns = new ArrayList<>();
        columns.add(FILTER_DATE_COL);
        columns.add(FILTER_NAME_COL);
        columns.add(FILTER_STRING_COL);
        columns.add(FILTER_INT_COL);
        validateExportedColumnHeader(exportedFile, columns, removedColumns);

    }

    /**
     * <p>
     *     Test the filter dialog with different custom views.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Verify that available fields to filter are as expected when a view has extra fields.</li>
     *         <li>Verify that available fields are as expected if a view has removed fields.</li>
     *         <li>
     *             Verify filtering on a field that is removed when a custom view is selected.
     *             <ul>
     *                 <li>Verify the filter is still applied.</li>
     *                 <li>Verify that the removed field is not shown in the dialog.</li>
     *             </ul>
     *         </li>
     *         <li>
     *             Verify filter dialog with a custom view that is only a filter.
     *             <ul>
     *                 <li>Verify that the filed filtered by the view does not show as filtered in the dialog.</li>
     *                 <li>Verify that the values to choose from are limited to the values filtered by the view.</li>
     *             </ul>
     *         </li>
     *     </ul>
     * </p>
     * @throws IOException Can be thrown by the call to getRows.
     * @throws CommandException Can be thrown by the call to getRows.
     */
    @Test
    public void testFilterDialogWithViews() throws IOException, CommandException
    {

        QueryGrid grid = beforeTest(SMALL_SAMPLE_TYPE);

        log(String.format("For sample type '%s' use view '%s'.", SMALL_SAMPLE_TYPE, VIEW_EXTRA_COLUMNS));

        grid.selectView(VIEW_EXTRA_COLUMNS);

        GridFilterModal filterDialog = grid.getGridBar().openFilterDialog();

        List<String> actualList = filterDialog.getAvailableFields();

        List<String> expectedList = new ArrayList<>(extraColumnsHeaders);
        expectedList.add(FILTER_NAME_COL);
        expectedList.add(FILTER_STRING_COL);
        expectedList.add(FILTER_INT_COL);
        expectedList.add(FILTER_BOOL_COL);
        expectedList.add(FILTER_DATE_COL);

        checker().withScreenshot("View_Extra_Field_Error")
                .verifyTrue(String.format("The fields listed in the dialog do not have expected values '%s'.", expectedList),
                        actualList.containsAll(expectedList));

        filterDialog.cancel();

        log(String.format("Now use view '%s'.", VIEW_FEWER_COLUMNS));

        grid.selectView(VIEW_FEWER_COLUMNS);

        expectedList.removeAll(extraColumnsHeaders);
        expectedList.removeAll(removedColumns);

        filterDialog = grid.getGridBar().openFilterDialog();
        actualList = filterDialog.getAvailableFields();

        checker().verifyTrue(String.format("The fields listed in the dialog are not as expected. Expected '%s'.", expectedList),
                        actualList.containsAll(expectedList));

        for(String removedColumn : removedColumns)
        {
            checker().verifyFalse(String.format("The field '%s' is listed in the dialog, it should not be.", removedColumn),
                    actualList.contains(removedColumn));
        }

        checker().screenShotIfNewError("View_Remove_Field_Error");

        filterDialog.cancel();

        log("Go back to default view. Filter on a column to be removed, then change view that does not include the column.");
        grid.selectView(VIEW_DEFAULT);

        log(String.format("Filter column '%s' to true.", FILTER_BOOL_COL));
        grid.filterColumn(FILTER_BOOL_COL, Filter.Operator.EQUAL, true);

        log(String.format("Change view to '%s' which should remove column '%s' from the grid.", VIEW_FEWER_COLUMNS, FILTER_BOOL_COL));
        grid.selectView(VIEW_FEWER_COLUMNS);

        // Get the expected sample id.
        List<Filter> filters = List.of(new Filter(FILTER_BOOL_COL, true, Filter.Operator.getOperator("EQUAL")));
        int expectedCount = getExpectedResults(SMALL_SAMPLE_TYPE, VIEW_FEWER_COLUMNS, null, filters).size();

        checker().withScreenshot("View_Filtered_Removed_Column_Error")
                .verifyEquals("Row count not as expected after view applied to column that is filtered.",
                        expectedCount, grid.getRecordCount());

        log("Validate the removed, but filtered column, does not appear in the dialog.");

        filterDialog = grid.getGridBar().openFilterDialog();

        actualList = filterDialog.getFilteredFields();

        checker().verifyTrue("No fields should be shown as filtered.",
                actualList.isEmpty());

        expectedList = new ArrayList<>();
        expectedList.add(FILTER_NAME_COL);
        expectedList.add(FILTER_INT_COL);
        expectedList.add(FILTER_STRING_COL);
        expectedList.add(FILTER_DATE_COL);

        actualList = filterDialog.getAvailableFields();

        Collections.sort(expectedList);
        Collections.sort(actualList);

        checker().verifyEquals("Available fields not as expected.",
                expectedList, actualList);

        // Build the list of expected values.
        // Get the expected sample id.
        filters = List.of(new Filter(FILTER_BOOL_COL, true, Filter.Operator.getOperator("EQUAL")));
        List<Map<String, Object>> strValues = getExpectedResults(SMALL_SAMPLE_TYPE, VIEW_FEWER_COLUMNS, List.of(FILTER_STRING_COL), filters);

        expectedList = new ArrayList<>();
        for(Map<String, Object> row : strValues)
        {
            String value = row.get(FILTER_STRING_COL).toString();

            if(!expectedList.contains(value))
            {
                expectedList.add(value);
            }
        }
        expectedList.add(ALL_OPTION);
        expectedList.add(BLANK_OPTION);

        log(String.format("Validate that the list of values for the '%s' is as expected.", FILTER_STRING_COL));
        filterDialog.selectField(FILTER_STRING_COL);
        actualList = filterDialog.selectFacetTab().getAvailableValues();

        Collections.sort(expectedList);
        Collections.sort(actualList);

        checker().verifyEquals("Values to choose from not as expected.",
                expectedList, actualList);

        checker().screenShotIfNewError("View_Filter_Dialog_Error");

        filterDialog.cancel();

        log(String.format("Change the view to '%s' and verify that the filter is still applied.", VIEW_EXTRA_COLUMNS));

        grid.selectView(VIEW_EXTRA_COLUMNS);

        List<FilterStatusValue> filterPills = grid.getFilterStatusValues(false);
        String expectedValue = String.format("%s = true", FILTER_BOOL_COL);
        checker().withScreenshot("View_Filter_Pill_Error").verifyTrue(String.format("Filter pills not as expected. There should only be one with value of '%s'", expectedValue),
                filterPills.size() == 1 && filterPills.get(0).getText().equals(expectedValue));

        log("Validate the fields in the dialog.");

        expectedList = new ArrayList<>(extraColumnsHeaders);
        expectedList.add(FILTER_NAME_COL);
        expectedList.add(FILTER_STRING_COL);
        expectedList.add(FILTER_INT_COL);
        expectedList.add(FILTER_BOOL_COL);
        expectedList.add(FILTER_DATE_COL);

        filterDialog = grid.getGridBar().openFilterDialog();

        actualList = filterDialog.getAvailableFields();

        Collections.sort(expectedList);
        Collections.sort(actualList);

        checker().verifyEquals("List of available fields not as expected.",
                expectedList, actualList);

        expectedList = Arrays.asList(FILTER_BOOL_COL);
        actualList = filterDialog.getFilteredFields();

        checker().verifyEquals(String.format("List of filtered fields with view '%s' not as expected.", FILTER_BOOL_COL),
                expectedList, actualList);

        checker().screenShotIfNewError("View_Filter_With_Extra_Error");

        filterDialog.cancel();

        log("One more check: Select the view that is only a filter.");
        grid.clearFilters();
        grid.selectView(VIEW_FILTERED_COLUMN);

        log("Open the filter dialog.");
        filterDialog = grid.getGridBar().openFilterDialog();

        log("Make sure no fields are shown as filtered.");
        actualList = filterDialog.getFilteredFields();

        checker().verifyTrue(String.format("The fields '%s' are shown as being filtered. They should not be.", actualList),
                actualList.isEmpty());

        log("Make sure the values to filter for are as expected.");
        filterDialog.selectField(FILTER_STRING_COL);
        actualList = filterDialog.selectFacetTab().getAvailableValues();

        // Going to hard code the expected values rather try and be clever and figure them out.
        expectedList = new ArrayList<>(Arrays.asList(ALL_OPTION, BLANK_OPTION, "A", "AB", "ABC", "ABCD", "ABD", "AC", "ACD", "AD"));

        Collections.sort(expectedList);
        Collections.sort(actualList);

        checker().verifyEquals("Values to choose from should only include those values filtered by the custom view.",
                expectedList, actualList);

        checker().screenShotIfNewError("View_With_Filter_Error");

        filterDialog.cancel();

    }

    /**
     * Helper to validate the exported rows.
     *
     * @param exportedFile The exported csv/tsv file.
     * @param expectedValues A list of the expected values in the rows.
     * @param columns The columns to validate against.
     * @throws IOException Can be thrown by the file reader.
     */
    private void validateExportedData(File exportedFile, List<Map<String, Object>> expectedValues, List<String> columns) throws IOException
    {

        try (CSVReader reader = new CSVReader(Readers.getReader(exportedFile), GridBar.ExportType.CSV.getSeparator()))
        {

            List<String[]> allRows = reader.readAll();

            // Use headerRow, the column names, as keys for the map of actual data.
            String[] headerRow = allRows.get(0);

            checker().verifyEquals("Number of rows is not as expected.", expectedValues.size()+1, allRows.size());

            for (int rowIndex = 0; rowIndex < expectedValues.size(); rowIndex++)
            {
                // Get the expected row value.
                Map<String, Object> expectedRowData = expectedValues.get(rowIndex);

                // Create a map of the actual row data returned.
                Map<String, String> actualRowData = new HashMap<>();
                String[] row = allRows.get(rowIndex+1);
                for (int i = 0; i < headerRow.length; i++)
                    actualRowData.put(headerRow[i], row[i]);

                // Check the values from the identified columns (exported vs. expected). All exported values are stings
                // so need to convert the expected values.
                for (String column : columns)
                {
                    checker().verifyEquals(String.format("Value for column '%s' on row %d not as expected.", column, rowIndex + 1),
                            expectedRowData.get(column).toString(), actualRowData.get(column));
                }

            }

        }

    }

    /**
     * Helper to validate the exported rows.
     *
     * @param exportedFile The exported csv/tsv file.
     * @param expectedColumns A list of the columns that should be exported.
     * @param expectedMissingColumns A list of columns that should not be exported.
     * @throws IOException Can be thrown by the file reader.
     */
    private void validateExportedColumnHeader(File exportedFile, List<String> expectedColumns, List<String> expectedMissingColumns) throws IOException
    {

        try (CSVReader reader = new CSVReader(Readers.getReader(exportedFile), GridBar.ExportType.CSV.getSeparator()))
        {

            List<String[]> allRows = reader.readAll();

            List<String> actualHeader = Arrays.asList(allRows.get(0));

            checker().verifyTrue(String.format("Expected column headers '%s' not exported.", expectedColumns),
                    actualHeader.containsAll(expectedColumns));

            for(String missingColumn : expectedMissingColumns)
            {
                checker().verifyFalse(String.format("Column header '%s' was exported and it should not have been.", missingColumn),
                        actualHeader.contains(missingColumn));
            }

        }

    }

    /**
     * Helper to try and avoid using 'magical values' when checking for expected results. This method lends its self to
     * tests that use filtering, it isn't as useful for search tests.
     *
     * @param sampleType Name of the sample type to query.
     * @param viewName If not null apply a view to the query.
     * @param columns The columns to return. If null will get only the Name column.
     * @param filters The list of filters to apply (this is why it is more aligned with filter tests)
     * @return The results from the query.
     * @throws IOException Can be thrown by the call to selectRows api.
     * @throws CommandException Can be thrown by the call to selectRows api.
     */
    private List<Map<String, Object>> getExpectedResults(String sampleType, @Nullable String viewName, @Nullable List<String> columns, @Nullable List<Filter> filters) throws IOException, CommandException
    {

        SelectRowsCommand cmd = new SelectRowsCommand(TEST_SCHEMA, sampleType);

        // If no columns are given get only the Name column.
        if(null == columns)
        {
            columns = Arrays.asList(FILTER_NAME_COL);
        }

        cmd.setColumns(columns);

        if(null != filters)
        {
            cmd.setFilters(new ArrayList<>(filters));
        }

        if(null != viewName)
        {
            cmd.setViewName(viewName);
        }

        SelectRowsResponse selectResponse = cmd.execute(WebTestHelper.getRemoteApiConnection(), getCurrentContainerPath());

        return selectResponse.getRows();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "QueryGridTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
