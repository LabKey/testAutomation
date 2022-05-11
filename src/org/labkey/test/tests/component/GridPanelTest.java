package org.labkey.test.tests.component;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Category({Daily.class})
public class GridPanelTest extends BaseWebDriverTest
{

    @BeforeClass
    public static void setupProject()
    {
        GridPanelTest init = (GridPanelTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    TestDataGenerator sampleSetDataGenerator;

    @Test
    public void testJumpToLastPage() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("grid_paging_samples").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(200);
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "grid_paging_samples");

        assertEquals(1, grid.getGridBar().pager().start());
        assertEquals(20, grid.getGridBar().pager().end());
        grid.getGridBar().jumpToPage("Last Page");
        assertEquals(10, grid.getGridBar().getCurrentPage());
        assertEquals(181, grid.getGridBar().pager().start());
        assertEquals(200, grid.getGridBar().pager().end());

        grid.getGridBar().jumpToPage("First Page");
        assertEquals(1, grid.getGridBar().getCurrentPage());
        assertEquals(1, grid.getGridBar().pager().start());
        assertEquals(20, grid.getGridBar().pager().end());

    }

    @Test
    public void testSelectPageSize() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("grid_page_size_samples").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(200);
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "grid_page_size_samples");

        // assume default page size is 20
        assertEquals(1, grid.getGridBar().getCurrentPage());
        assertEquals(1, grid.getGridBar().pager().start());
        assertEquals(20, grid.getGridBar().pager().end());
        assertEquals(200, grid.getGridBar().pager().total());

        // now set page size to equal 100
        grid.getGridBar().selectPageSize("100");

        // check to see that the pager thinks it's all true
        assertEquals(1, grid.getGridBar().getCurrentPage());
        assertEquals(1, grid.getGridBar().pager().start());
        assertEquals(100, grid.getGridBar().pager().end());
        assertEquals(200, grid.getGridBar().pager().total());
        // but don't take the pager's word for it, count the visible rows in the grid
        assertEquals(100, grid.getRows().size());

        // restore defaults
        grid.getGridBar().selectPageSize("20");
        assertEquals(1, grid.getGridBar().getCurrentPage());
        assertEquals(1, grid.getGridBar().pager().start());
        assertEquals(20, grid.getGridBar().pager().end());
        assertEquals(200, grid.getGridBar().pager().total());

    }

    /**
     * regression coverage for issue 39011
     */
    @Test
    public void testFilterSelections() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("filtered_grid_selection_samples").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(100);
        for (int i=0; i < 16; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(25),
                            "descColumn", "used to test issue 39011",
                            "intColumn", i,
                            "stringColumn", "filtered_x" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "filtered_grid_selection_samples");

        // confirm that selectAll is limited to just the records in a filtered set
        grid.filterColumn("Desc Column", Filter.Operator.EQUAL, "used to test issue 39011");
        grid.selectAllRows();
        grid.removeColumnFilter("Desc Column");
        assertEquals(16, grid.getSelectedRows().size());

        grid.clearAllSelections();
        assertEquals(0, grid.getSelectedRows().size());

        // confirm that search behaves the same way as a filter does
        grid.search("filtered_x");
        grid.selectAllOnPage(true);
        grid.clearSearch();
        assertEquals(16, grid.getSelectedRows().size());

    }

    /**
     * Tests select-all behavior for a sampletype that does not involve paging.
     * The component does not show the same set of controls if paging isn't necessary; this ensures that
     * the test-compontent wrapper handles that pathway appropriately
     */
    @Test
    public void testSelectAllOnSinglePageSet() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("tiny_sampleset").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(5);
        for (int i=0; i < 5; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(25),
                            "descColumn", "used to test single-page filter selection",
                            "intColumn", i,
                            "stringColumn", "filtered_x" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "tiny_sampleset");

        grid.filterColumn("Desc Column", Filter.Operator.EQUAL, "used to test single-page filter selection");
        grid.selectAllRows();

        assertEquals("5 of 5 selected", grid.getSelectionStatusCount());

    }

    @Test
    public void testFilterInMultiPageSelectionSet() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("filtered_grid_multipageselection_samples").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(60);
        for (int i=0; i < 64; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(25),
                            "descColumn", "used to test issue 39011",
                            "intColumn", i,
                            "stringColumn", "filtered_x" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "filtered_grid_multipageselection_samples");

        // confirm that selectAll is limited to just the records in a filtered set
        grid.filterColumn("Desc Column", Filter.Operator.EQUAL, "used to test issue 39011");
        grid.selectAllRows();
        grid.removeColumnFilter("Desc Column");
        assertEquals("64 of 124 selected", grid.getSelectionStatusCount());

        grid.clearAllSelections();
        assertEquals(0, grid.getSelectedRows().size());

        // confirm that search behaves the same way as a filter does
        grid.search("filtered_x");
        grid.selectAllRows();
        grid.clearSearch();
        assertEquals("64 of 124 selected", grid.getSelectionStatusCount());

    }

    @Test
    public void testEmptyNotEmptyFilter() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("empty_filter_test_set").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.addDataSupplier("descColumn", () -> null);
        sampleSetDataGenerator.generateRows(30);
        for (int i=0; i < 5; i++)  // add some rows with a descColumn we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(6),
                            "descColumn", "used to filter non-empty",
                            "stringColumn", "filtered_x" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "empty_filter_test_set");

        grid.filterColumn("Desc Column", Filter.Operator.ISBLANK, null);
        assertEquals("1 - 20 of 30", grid.getGridBar().pager().summary());
        grid.removeColumnFilter("Desc Column");

        grid.filterColumn("String Column", Filter.Operator.CONTAINS,  "filtered_x");
        assertEquals("1 - 5", grid.getGridBar().pager().summary());
        grid.removeColumnFilter("String Column");

        grid.filterColumn("String Column", Filter.Operator.DOES_NOT_CONTAIN, "filtered_x");
        assertEquals("1 - 20 of 30", grid.getGridBar().pager().summary());
        grid.removeColumnFilter("String Column");

        grid.filterColumn("Sample Date", Filter.Operator.NONBLANK, null);
        assertEquals("1 - 20 of 30", grid.getGridBar().pager().summary());
        grid.removeColumnFilter("Sample Date");

        grid.filterColumn("Sample Date", Filter.Operator.ISBLANK, null);
        assertEquals("1 - 5", grid.getGridBar().pager().summary());
        grid.removeColumnFilter("Sample Date");

    }

    @Test
    public void testDeselectOnePageWhenAllAreSelected() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("deselect_one_page_set").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(35);
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "deselect_one_page_set");

        grid.selectAllRows();
        assertEquals("1 - 20 of 35", grid.getGridBar().pager().summary());
        assertEquals("unexpected selection tally after select or filter action" +
                "likely https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=41171",
                "35 of 35 selected", grid.getSelectionStatusCount());

        grid.getGridBar().jumpToPage("Last Page");
        grid.selectAllOnPage(false); // uncheck the checkbox
        assertEquals("20 of 35 selected", grid.getSelectionStatusCount());

        grid.getGridBar().jumpToPage("First Page");
        assertEquals("20 of 35 selected", grid.getSelectionStatusCount());

    }

    @Test
    public void testRemoveFilterWithSelections() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("remove_filters_with_selections_set").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(30);
        for (int i=0; i < 5; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(6),
                            "stringColumn", "filtered_y" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "remove_filters_with_selections_set");

        grid.filterColumn("Int Column", Filter.Operator.NONBLANK);

        grid.selectAllRows();
        assertEquals("1 - 20 of 30", grid.getGridBar().pager().summary());
        assertEquals("30 of 30 selected", grid.getSelectionStatusCount());

        grid.getGridBar().jumpToPage("Last Page");
        grid.selectAllOnPage(false); // uncheck the checkbox
        assertEquals("20 of 30 selected", grid.getSelectionStatusCount());

        grid.getGridBar().jumpToPage("First Page");
        assertEquals("20 of 30 selected", grid.getSelectionStatusCount());

        grid.removeColumnFilter("Int Column");
        assertEquals("20 of 35 selected", grid.getSelectionStatusCount());

    }

    @Test
    public void testDeselectRecordsWithFilter() throws IOException, CommandException
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("deselect_with_filter").setFields(standardTestSampleFields());
        sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(30);
        for (int i=0; i < 5; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(6),
                            "stringColumn", "filtered_y" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getGridPanel("samples", "deselect_with_filter");

        grid.selectAllRows();
        grid.filterColumn("String Column", Filter.Operator.DOES_NOT_CONTAIN, "filtered_y");

        assertEquals("1 - 20 of 30", grid.getGridBar().pager().summary());
        assertEquals("30 of 30 selected", grid.getSelectionStatusCount());

        grid.selectAllOnPage(false);
        assertEquals("1 - 20 of 30", grid.getGridBar().pager().summary());
        assertEquals("10 of 30 selected", grid.getSelectionStatusCount());

        grid.removeColumnFilter("String Column");
        assertEquals("1 - 20 of 35", grid.getGridBar().pager().summary());
        assertEquals("https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=41171",
                "15 of 35 selected", grid.getSelectionStatusCount());

    }

    @After
    public void afterTest() throws IOException, CommandException
    {
        sampleSetDataGenerator.getQueryHelper(createDefaultConnection()).deleteDomain();
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
