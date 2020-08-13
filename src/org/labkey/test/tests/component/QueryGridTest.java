package org.labkey.test.tests.component;

import org.junit.Before;
import org.junit.BeforeClass;
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
