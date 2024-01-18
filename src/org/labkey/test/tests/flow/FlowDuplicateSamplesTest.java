package org.labkey.test.tests.flow;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.categories.Flow;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineStatusTable;

import java.util.Arrays;

// Issue 41224: flow: support duplicate sample names in FlowJo workspace
// Issue 41225: flow: import failure for duplicate aliased statistics
@Category({Daily.class, Flow.class, FileBrowser.class})
public class FlowDuplicateSamplesTest extends BaseFlowTest
{

    protected void importFCSFiles()
    {
        waitAndClickAndWait(Locator.linkWithText("Browse for FCS files to be imported"));

        _fileBrowserHelper.selectFileBrowserItem("flowjoquery/microFCS");
        _fileBrowserHelper.selectFileBrowserItem("flowjoquery/miniFCS");
        _fileBrowserHelper.selectImportDataAction("Import Directory of FCS Files");
        assertTextPresent("microFCS (2 fcs files)", "miniFCS (68 fcs files)");
        clickButton("Import Selected Runs", defaultWaitForPage * 2);
        waitForPipelineComplete();

        // verify both miniFCS and microFCS directories were imported
        var dr = new DataRegionTable("query", getDriver());
        dr.setSort("Name", SortDirection.ASC);

        var names = dr.getColumnDataAsText("Name");
        Assert.assertEquals(names, Arrays.asList("microFCS", "miniFCS"));

        var wellCount = dr.getColumnDataAsText("WellCount");
        Assert.assertEquals(wellCount, Arrays.asList("2", "68"));
    }


    @Test
    public void duplicateSamples()
    {
        goToFlowDashboard();
        importFCSFiles();

        importAnalysis(getContainerPath(), "/flowjoquery/Workspaces/duplicate-samples.xml", SelectFCSFileOption.Previous, null, "dupes", false, true);

        // verify both samples "118795.fcs" were imported and associated with the correct FCS file
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addColumn(new String[] { "FCSFile", "Keyword", "$DTOT"});
        _customizeViewsHelper.addColumn(new String[] { "FCSFile", "FilePath"});
        _customizeViewsHelper.addSort("Statistic/Count", SortDirection.ASC);
        _customizeViewsHelper.applyCustomView();

        var dr = new DataRegionTable("query", getDriver());
        var rows = dr.getRows("Name", "Count", "$TOT", "FilePath");
        Assert.assertEquals(2, rows.size());

        var row0 = rows.get(0);
        Assert.assertEquals("118795.fcs", row0.get(0));
        Assert.assertEquals("1,000", row0.get(1));
        Assert.assertEquals("1000", row0.get(2));
        MatcherAssert.assertThat(row0.get(3), CoreMatchers.containsString("miniFCS/118795.fcs"));

        var row1 = rows.get(1);
        Assert.assertEquals("118795.fcs", row1.get(0));
        Assert.assertEquals("10,000", row1.get(1));
        Assert.assertEquals("10000", row1.get(2));
        MatcherAssert.assertThat(row1.get(3), CoreMatchers.containsString("microFCS/118795.fcs"));

        // Issue 41225: flow: import failure for duplicate aliased statistics
        // verify the duplicate alias was detected and ignored
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Show Jobs"));
        PipelineStatusTable statusTable = new PipelineStatusTable(getDriver());
        var status = statusTable.clickStatusLink("Import FlowJo Workspace 'duplicate-samples.xml'");
        status.assertLogTextContains(
                "WARN : Duplicate statistics found for '118795.fcs'",
                "'S/Lv/L/3+/4+/bogus_IFNorIL2:Count' with value 10.0",
                "'S/Lv/L/3+/4+/IFNorIL2:Count' with value 10.0"
        );

    }
}
