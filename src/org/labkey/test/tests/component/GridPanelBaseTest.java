package org.labkey.test.tests.component;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GridPanelBaseTest extends BaseWebDriverTest
{
    protected static final String TEST_SCHEMA = "samples";

    protected static final int DEFAULT_PAGE_SIZE = 20;

    // View menu options.
    protected static final String VIEW_DEFAULT = "Default";
    protected static final String VIEW_DEFAULT_MODIFIED = "My Default"; // If the default view is changed and not shared this will be the menu option.
    protected static final String VIEW_CUSTOMIZE = "Customize Grid View";
    protected static final String VIEW_MANAGE = "Manage Saved Views";
    protected static final String VIEW_SAVE = "Save Grid View";

    // Column removed from default view.
    private static final String REMOVED_FLAG_COLUMN = "Flag";

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "QueryGridBaseTest_Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

    // Generate a list of string that has different combinations/sets from the list of strings passed in.
    // For example if values = ['A', 'B', 'C'] this will return the list ['', 'A', 'B', 'C', 'AB', 'AC', 'ABC', 'BC'].
    // This is all the sets of the characters from the list, including the empty set.
    protected List<String> getAllSets(List<String> values, int index)
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

    protected void initProject()
    {
        _containerHelper.createProject(getProjectName(), null);

        // Add the 'Sample Types' web part. It is easier when debugging etc...
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.exitAdminMode();
    }

    /**
     * Helper to remove the 'Flag' column from the default view. It just gets in the way for some tests, and is easier
     * to remove it.
     *
     * @param sampleType Name of sample type.
     */
    protected void removeFlagColumnFromDefaultView(String sampleType)
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

        // This will save the changes to the default view for everyone. The view menu option will remain as "Default"
        cv.saveCustomView("", true);

    }

    /**
     * Make sure there are no filters or search values persisted for the given sample type.
     *
     * @param sampleType The sample type used to populate the grid.
     * @return A queryGrid object.
     */
    protected QueryGrid initQueryGrid(String sampleType)
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

}
