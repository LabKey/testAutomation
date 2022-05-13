package org.labkey.test.tests.component;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ui.grids.GridFilterModal;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.components.ui.search.FilterExpressionPanel;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Category({Daily.class})
public class GridPanelTest extends BaseWebDriverTest
{

    private static final String TEST_SCHEMA = "samples";

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final String SMALL_SAMPLE_TYPE = "Small_SampleType";
    private static final int SMALL_SAMPLE_TYPE_SIZE = 10;

    private static final String FILTER_SAMPLE_TYPE = "Filter_SampleType";
    private static final int FILTER_SAMPLE_TYPE_SIZE = 300;
    private static final String FILTER_SAMPLE_PREFIX = "FST-";
    private static final String FILTER_STRING_COL = "Str";
    private static final String FILTER_INT_COL = "Int";
    private static final String FILTER_EXTEND_CHAR_COL = "\u0106\u00D8\u0139";

    // Various values used to populate Str filed for records. Also used in filtering/searching.
    private static final String EXTEND_RECORD_STRING = "\u01C5 \u01FC";
    private static final String EXTEND_RECORD_OTHER_STRING = "Not an extended value.";

    private static final String ONE_RECORD_STRING = "This will return one row.";

    private static final String FIVE_RECORD_STRING = "This will return five rows.";

    private static final String ONE_PAGE_STRING = "This will return one page of data.";

    private static final String MULTI_PAGE_STRING = "This will return more than one page of data.";
    private static final int MULTI_PAGE_COUNT = 3;

    private static final int NUMBER_FOR_STRING = 1234;
    private static final String NUMBER_STRING = String.format("This string has numbers %d in it.", NUMBER_FOR_STRING);

    private static final String STARTS_WITH = "This will";
    private static final String ENDS_WITH = "page of data.";

    // Number of entries that have the NUMBER_STRING value.
    private static final int NUMBER_STRING_COUNT = 16;

    // Number of entries that will be an empty string.
    private static final int EMPTY_STRING_COUNT = 12;

    // Number of records that will have extended characters.
    private static final int EXTEND_RECORD_COUNT = 10;

    // Upper value to put into the Int column.
    private static final int INT_MAX = 15;

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

        createSmallSampleType();

        createFilterSampleType();
    }

    // Create a small sample type that has one page of data by default (used in paging testing).
    // Don't care about the data in the rows so use random data.
    private void createSmallSampleType() throws IOException, CommandException
    {
        SampleTypeDefinition props = new SampleTypeDefinition(SMALL_SAMPLE_TYPE).setFields(standardTestSampleFields());
        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(SMALL_SAMPLE_TYPE_SIZE);
        sampleSetDataGenerator.insertRows();
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

        List<String> allPossibleValues = getCombinations("DCBA", 0);
        // Remove the empty string option.
        allPossibleValues.remove(0);

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
                if(allPossibleIndex == allPossibleValues.size())
                    allPossibleIndex = 0;

                filterColValue = allPossibleValues.get(allPossibleIndex++);
                if(extendedCharCount < EXTEND_RECORD_COUNT)
                {
                    extColValue = EXTEND_RECORD_STRING;
                    extendedCharCount++;
                }

            }

            if(intValue > INT_MAX)
                intValue = 1;

            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", String.format("%s%d", FILTER_SAMPLE_PREFIX, sampleId++),
                            FILTER_INT_COL, intValue++,
                            FILTER_STRING_COL, filterColValue,
                            FILTER_EXTEND_CHAR_COL, extColValue));

        }

        sampleSetDataGenerator.insertRows();

    }

    // Generate a list of string that has different combinations from the string value passed in.
    // For example if values = 'ABC' this will return a list ['A', 'B', 'C', 'AB', 'AC', 'ABC', 'BC'].
    // It treats 'BC' and 'CB' as equivalent and only one would be added.
    private static List<String> getCombinations(String values, int index)
    {
        List<String> allCombinations = new ArrayList<>();
        if(index == values.length()) {
            allCombinations.add("");
            return allCombinations;
        }

        allCombinations = getCombinations(values, index + 1);
        List<String> newCombinations = new ArrayList<>();

        for (String allCombination : allCombinations)
        {
            newCombinations.add(allCombination + values.charAt(index));
        }

        allCombinations.addAll(newCombinations);

        return allCombinations;
    }

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

    public boolean isPagingMenuVisible()
    {
        Locator dropMenuLocator = Locator.tagWithClass("ul", "dropdown-menu");
        WebElement gridPanel = Locator.tagWithClass("div", "grid-panel__body").findElement(getDriver());
        WebElement dropDownMenu = dropMenuLocator.refindWhenNeeded(gridPanel);
        return dropDownMenu.isDisplayed();
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
                .withScreenshot("Dropdown_Menu_Visible")
                .verifyFalse("After selecting 'Last Page' the dropdown menu is still visible, it should not be.",
                        isPagingMenuVisible());

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
                isPagingMenuVisible());

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
                .withScreenshot("Dropdown_Menu_Visible")
                .verifyFalse("After selecting a page size the dropdown menu is still visible, it should not be.",
                        isPagingMenuVisible());

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
                .withScreenshot("Dropdown_Menu_Visible")
                .verifyFalse("After selecting a page size the dropdown menu is still visible, it should not be.",
                        isPagingMenuVisible());

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

        expectedText = String.format("%1$d of %1$d selected", SMALL_SAMPLE_TYPE_SIZE);
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
                .verifyEquals("Indicated number of selected rows not as expected after filter removed.",
                        String.format("%d of %d selected", MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE, FILTER_SAMPLE_TYPE_SIZE), grid.getSelectionStatusCount());

        log("Test is done. Clear the selection.");
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
                        String.format("%d of %d selected", DEFAULT_PAGE_SIZE, FILTER_SAMPLE_TYPE_SIZE), grid.getSelectionStatusCount());

        log("Finished. Clear the selection.");
        grid.clearAllSelections();

    }

    /**
     * <p>
     *     Test the interaction between the select all on the page option and the 'Select All' button. This covers issues 39011, 41171.
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
                        String.format("%d of %d selected", DEFAULT_PAGE_SIZE, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE), grid.getSelectionStatusCount());

        log("Use the 'Select All' button to select all of the filtered rows.");
        checker().fatal()
                .verifyTrue("The 'Select All' button is not present.", grid.hasSelectAllButton());

        grid.selectAllRows();
        checker().withScreenshot("Select_All_Button_With_Some_Selected_Error")
                .verifyEquals("Indicated number of selected rows not as expected after filter removed.",
                        String.format("%1$d of %1$d selected", MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE), grid.getSelectionStatusCount());

        log("Remove the filter and validate that the selections persisted.");
        grid.removeColumnFilter(FILTER_STRING_COL);

        checker().withScreenshot("Select_After_Filter_Removed_Error")
                .verifyEquals("Indicated number of selected rows not as expected after filter removed.",
                        String.format("%d of %d selected", MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE, FILTER_SAMPLE_TYPE_SIZE), grid.getSelectionStatusCount());

        log("Apply the filter again...");
        grid.filterColumn(FILTER_STRING_COL, Filter.Operator.EQUAL, MULTI_PAGE_STRING);

        checker().withScreenshot("Select_Filter_Reapplied_Error")
                .verifyEquals("Indicated number of selected rows not as expected after filter was reapplied.",
                        String.format("%1$d of %1$d selected", MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE), grid.getSelectionStatusCount());

        log("Unselect all of the rows on the first page.");
        grid.selectAllOnPage(false);

        checker().withScreenshot("Select_Clear_Page_Error")
                .verifyEquals("Indicated number of selected rows not as expected after first page cleared.",
                        String.format("%d of %d selected", (MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE) - DEFAULT_PAGE_SIZE, MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE), grid.getSelectionStatusCount());

        log("Remove the filter, and validate selection count.");
        grid.removeColumnFilter(FILTER_STRING_COL);

        checker().withScreenshot("Select_Remove_Page_Error")
                .verifyEquals("Indicated number of selected rows not as expected after first page cleared and filter removed.",
                        String.format("%d of %d selected", (MULTI_PAGE_COUNT * DEFAULT_PAGE_SIZE) - DEFAULT_PAGE_SIZE, FILTER_SAMPLE_TYPE_SIZE), grid.getSelectionStatusCount());

        log("Clean up.");
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
    public void testMultipleFiltersOnOneColumn()
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        int low = INT_MAX - 3;
        int high = INT_MAX;

        log(String.format("Filter the '%s' for values greater than %d and less than or equal to %d.", FILTER_INT_COL, low, high));

        grid.filterColumn(FILTER_INT_COL, Filter.Operator.GT, low, Filter.Operator.LTE, high);

        checker().withScreenshot("Multiple_Filters_One_Column_Error")
                .verifyEquals("Number of records returned for filter not as expected.",
                        60, grid.getRecordCount());

        grid.clearFilters();
    }

    /**
     * Validate that if required fields are not set for filter the dialog shows an error.
     */
    @Test
    public void testExpectedTabAndFilterError()
    {

        QueryGrid grid = beforeTest(FILTER_SAMPLE_TYPE);

        GridFilterModal filterDialog = grid.getGridBar().getFilterDialog();

        log(String.format("Filter on the '%s' filed. This should only provide a 'Filter' tab.", FILTER_INT_COL));

        filterDialog.selectField(FILTER_INT_COL);

        try
        {
            filterDialog.selectFacetTab();

            checker().error(String.format("There should be no 'Fields' tab for the '%s' field.", FILTER_INT_COL));
        }
        catch (NoSuchElementException expectedError)
        {
            log("Looks like there is no 'Fields' tab. This is as expected.");
        }

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

    protected List<FieldDefinition> standardTestSampleFields()
    {
        return Arrays.asList(
                new FieldDefinition("descColumn", FieldDefinition.ColumnType.String),
                new FieldDefinition("intColumn", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("stringColumn", FieldDefinition.ColumnType.String),
                new FieldDefinition("sampleDate", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("boolColumn", FieldDefinition.ColumnType.Boolean));
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
