package org.labkey.test.tests.component;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.glassLibrary.grids.EditableGrid;
import org.labkey.test.pages.test.CoreComponentsTestPage;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Category({DailyB.class})   // until there's a component suite
public class EditableGridTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        EditableGridTest init = (EditableGridTest) getCurrentTest();

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
    public void testTooWideErrorCase() throws Exception
    {
        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        EditableGrid testGrid = testPage.getEditableGrid();
        String wideShape = "Too wide\tACME\tthing\tanother\televen\toff the map\tMoar columns\n";

        testGrid.pasteFromCell(0, "Description", wideShape);
        assertThat("Expect cell error to explain that paste cannot add columns",
                testGrid.getCellPopoverText(0, "Description"),
                is("Unable to paste. Cannot paste columns beyond the columns found in the grid."));
    }

    @Test
    public void testCanAddRowsWithTallShape()
    {
        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        EditableGrid testGrid = testPage.getEditableGrid();
        String tallShape = "42\n" +
                "41\n" +
                "40\n" +
                "39\n" +
                "38";

        int initialRowCount = testGrid.getRowCount();
        assertThat(initialRowCount, is(2));

        testGrid.pasteFromCell(0, "Description", tallShape);
        List<String> pastedColData = testGrid.getColumnData("Description");

        assertThat(pastedColData, hasItems("42", "41", "40", "39", "38"));
        assertThat(testGrid.getRowCount(), is(5));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "EditableGridTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
