package org.labkey.test.tests.component;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.components.glassLibrary.grids.QueryGrid;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category({InDevelopment.class})
public class QueryGridTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        QueryGridTest init = (QueryGridTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    @Test
    public void testJumpToLastPage() throws Exception
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("grid_paging_samples").setFields(standardTestSampleFields());
        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(200);
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getQueryGrid("samples", "grid_paging_samples");

        assertThat(grid.getGridBar().pager().start(), is(1));
        assertThat(grid.getGridBar().pager().end(), is(20));
        grid.getGridBar().jumpToPage("Last Page");
        assertThat(grid.getGridBar().getCurrentPage(), is(10));
        assertThat(grid.getGridBar().pager().start(), is(181));
        assertThat(grid.getGridBar().pager().end(), is(200));

        grid.getGridBar().jumpToPage("First Page");
        assertThat(grid.getGridBar().getCurrentPage(), is(1));
        assertThat(grid.getGridBar().pager().start(), is(1));
        assertThat(grid.getGridBar().pager().end(), is(20));

        // clean up domain after on success
        sampleSetDataGenerator.deleteDomain(createDefaultConnection());
    }

    @Test
    public void testSelectPageSize() throws Exception
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("grid_page_size_samples").setFields(standardTestSampleFields());
        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(200);
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getQueryGrid("samples", "grid_page_size_samples");

        // assume default page size is 20
        assertThat(grid.getGridBar().getCurrentPage(), is(1));
        assertThat(grid.getGridBar().pager().start(), is(1));
        assertThat(grid.getGridBar().pager().end(), is(20));
        assertThat(grid.getGridBar().pager().total(), is(200));

        // now set page size to equal 100
        grid.getGridBar().selectPageSize("100");

        // check to see that the pager thinks it's all true
        assertThat(grid.getGridBar().getCurrentPage(), is(1));
        assertThat(grid.getGridBar().pager().start(), is(1));
        assertThat(grid.getGridBar().pager().end(), is(100));
        assertThat(grid.getGridBar().pager().total(), is(200));
        // but don't take the pager's word for it, count the visible rows in the grid
        assertThat(grid.getRows().size(), is(100));

        // restore defaults
        grid.getGridBar().selectPageSize("20");
        assertThat(grid.getGridBar().getCurrentPage(), is(1));
        assertThat(grid.getGridBar().pager().start(), is(1));
        assertThat(grid.getGridBar().pager().end(), is(20));
        assertThat(grid.getGridBar().pager().total(), is(200));

        // clean up domain after on success
        sampleSetDataGenerator.deleteDomain(createDefaultConnection());
    }

    /**
     * regression coverage for issue 39011
     * @throws Exception
     */
    @Test
    public void testOmniboxFilterSelections() throws Exception
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("filtered_grid_selection_samples").setFields(standardTestSampleFields());
        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(100);
        for (int i=0; i < 16; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(25),
                            "Description", "used to test issue 39011",
                            "intColumn", i,
                            "stringColumn", "filtered_x" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getQueryGrid("samples", "filtered_grid_selection_samples");

        // confirm that selectAll is limited to just the records in a filtered set
        grid.filterOn("Description", "=", "used to test issue 39011");
        grid.selectAllRows();
        grid.clearSortsAndFilters();
        assertThat(grid.getSelectedRows().size(), is(16));

        grid.clearAllSelections();
        assertThat(grid.getSelectedRows().size(), is(0));

        // confirm that search behaves the same way as a filter does
        grid.search("filtered_x");
        grid.selectAllRows();
        grid.clearSortsAndFilters();
        assertThat(grid.getSelectedRows().size(), is(16));

        // clean up domain after on success
        sampleSetDataGenerator.deleteDomain(createDefaultConnection());
    }

    /**
     * Tests select-all behavior for a sampletype that does not involve paging.
     * The component does not show the same set of controls if paging isn't necessary; this ensures that
     * the test-compontent wrapper handles that pathway appropriately
     * @throws Exception
     */
    @Test
    public void testSelectAllOnSinglePageSet() throws Exception
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("tiny_sampleset").setFields(standardTestSampleFields());
        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(5);
        for (int i=0; i < 5; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(25),
                            "Description", "used to test single-page filter selection",
                            "intColumn", i,
                            "stringColumn", "filtered_x" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getQueryGrid("samples", "tiny_sampleset");

        grid.filterOn("description", "=", "used to test single-page filter selection");
        grid.selectAllRows();

        assertThat(grid.getSelectionStatusCount(), is ("5 of 5 selected"));

        // clean up domain after on success
        sampleSetDataGenerator.deleteDomain(createDefaultConnection());
    }

    @Test
    public void testOmniboxFilterInMultiPageSelectionSet() throws Exception
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("filtered_grid_multipageselection_samples").setFields(standardTestSampleFields());
        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(60);
        for (int i=0; i < 64; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(25),
                            "Description", "used to test issue 39011",
                            "intColumn", i,
                            "stringColumn", "filtered_x" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getQueryGrid("samples", "filtered_grid_multipageselection_samples");

        // confirm that selectAll is limited to just the records in a filtered set
        grid.filterOn("Description", "=", "used to test issue 39011");
        grid.selectAllRows();
        grid.clearSortsAndFilters();
        assertThat(grid.getSelectionStatusCount(), is("64 of 124 selected"));

        grid.clearAllSelections();
        assertThat(grid.getSelectedRows().size(), is(0));

        // confirm that search behaves the same way as a filter does
        grid.search("filtered_x");
        grid.selectAllRows();
        grid.clearSortsAndFilters();
        assertThat(grid.getSelectionStatusCount(), is("64 of 124 selected"));

        // clean up domain after on success
        sampleSetDataGenerator.deleteDomain(createDefaultConnection());
    }

    @Ignore // re-enable or delete after https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=41161 is resolved
    @Test
    public void testSearchOnColumnNotInView() throws Exception
    {
        // create a sampleType domain to use in this case
        SampleTypeDefinition props = new SampleTypeDefinition("search_for_description_expression").setFields(standardTestSampleFields());
        TestDataGenerator sampleSetDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleSetDataGenerator.generateRows(30);
        for (int i=0; i < 5; i++)  // add some rows with a description we can filter on
        {
            sampleSetDataGenerator.addCustomRow(
                    Map.of("Name", sampleSetDataGenerator.randomString(25),
                            "Description", "used to search for off-view data",
                            "intColumn", i,
                            "stringColumn", "filtered_x" + sampleSetDataGenerator.randomString(20)));
        }
        sampleSetDataGenerator.insertRows();

        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        QueryGrid grid = testPage.getQueryGrid("samples", "search_for_description_expression");

        // prove we can filter on this criteria
        grid.filterOn("Description", "contains", "off-view");
        assertThat(grid.getGridBar().pager().summary(), is ("1 - 5"));
        grid.clearSortsAndFilters();

        // search on the same we were able to filter on
        grid.search("off-view");
        assertThat("Search fails to find what filter finds in off-view field " +
                        "https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=41161",
                grid.getGridBar().pager().summary(), is ("1 - 5"));

        sampleSetDataGenerator.deleteDomain(createDefaultConnection());
    }

    protected List<FieldDefinition> standardTestSampleFields()
    {
        return Arrays.asList(
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
