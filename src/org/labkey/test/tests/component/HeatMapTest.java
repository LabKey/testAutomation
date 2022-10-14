package org.labkey.test.tests.component;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ui.heatmap.HeatMap;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
public class HeatMapTest extends BaseWebDriverTest
{
    private static Map<String, Integer> TEST_SAMPLE_TYPES = new HashMap<>();
    /* this query is cribbed from SampleSetHeatMap.sql
    *   At this time, the heatMap in core-components.view is hard-coded to consume exp.SampleSetHeatMap, the plan is to
    *   make that configurable there.  There are other queries in use for this component, some for sampleTypes, others for
    *   assay runs.  Having the query configurable (or perhaps, built-in) will mean we won't need to create it for the test
    *
    *   I did attempt to customize the query to reference the date fields on samples in the sample types created for this test,
    *   for the purpose of back-dating them and having data in cells other than the last/current month, but was unsuccessful.
    *
     */
    private String HEATMAP_QUERY = "SELECT\n" +
            "  -- NOTE: Select SampleSet from TOTAL query so it will be included even if there are no samples within the date range\n" +
            "  TOTAL.SampleSet AS Protocol,\n" +
            "  P.*,\n" +
            "  TOTAL.CompleteCount,\n" +
            "  TOTAL.InRangeCount\n" +
            "FROM\n" +
            "\n" +
            "  (SELECT\n" +
            "     M.SampleSet    AS SampleSet,\n" +
            "     COUNT(M.RowID) AS CompleteCount,\n" +
            "     SUM(CASE WHEN (age_in_months(M.Created, curdate()) < 12) THEN 1 ELSE 0 END)\n" +
            "                    AS InRangeCount\n" +
            "   FROM Materials AS M\n" +
            "   GROUP BY SampleSet\n" +
            "  ) AS TOTAL\n" +
            "\n" +
            "  FULL JOIN\n" +
            "\n" +
            "  (SELECT\n" +
            "     M.SampleSet                                AS _PivotSampleSet @hidden,\n" +
            "     COUNT(M.RowID)                             AS MonthCount,\n" +
            "     CAST(YEAR(M.Created) AS VARCHAR) || '-' || CAST(MONTH(M.Created) AS VARCHAR) AS YearMonth\n" +
            "   FROM exp.Materials AS M\n" +
            "   WHERE (age_in_months(M.Modified, curdate()) < 12)\n" +
            "   GROUP BY M.SampleSet,\n" +
            "     CAST(YEAR(M.Created) AS VARCHAR) || '-' || CAST(MONTH(M.Created) AS VARCHAR)\n" +
            "   PIVOT MonthCount BY YearMonth\n" +
            "  ) AS P\n" +
            "    ON P._PivotSampleSet = TOTAL.SampleSet\n" +
            "\n" +
            "WHERE SampleSet.Name NOT IN ('RawMaterials', 'MixtureBatches')";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        HeatMapTest init = (HeatMapTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        createQuery(getProjectName(), "SampleSetHeatMap", "exp", HEATMAP_QUERY, null, false);

        TEST_SAMPLE_TYPES.put("basic_heatmap_samples 1", 50);
        TEST_SAMPLE_TYPES.put("more_basic_samples", 30);
        TEST_SAMPLE_TYPES.put("filler_samples", 75);
        TEST_SAMPLE_TYPES.put("heatmap_samples", 100);

        for(String sampleTypeName : TEST_SAMPLE_TYPES.keySet())
        {
            makeHeatmapSampletype(sampleTypeName, TEST_SAMPLE_TYPES.get(sampleTypeName));
        }
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testHeatMap()
    {
        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        HeatMap heatMap = testPage.getHeatMap();

        assertTrue("Heatmap should be loaded and have data", heatMap.hasData());
        for(String sampleTypeName : TEST_SAMPLE_TYPES.keySet())
        {
            String expectedSampleCount = Integer.toString(TEST_SAMPLE_TYPES.get(sampleTypeName));
            assertThat("expect ["+expectedSampleCount+"] samples",
                    heatMap.getSummaryLink(sampleTypeName).getText(), is(expectedSampleCount));
        }
        assertThat("Expect all sampleTypes in query exp.SampleSetHeatMap to have made it into the HeatMap",
                heatMap.getRowNames(), hasItems(TEST_SAMPLE_TYPES.keySet().toArray(new String[0])));
    }

    private TestDataGenerator makeHeatmapSampletype(String sampleTypeName, int sampleCount) throws Exception
    {
        SampleTypeDefinition props = new SampleTypeDefinition(sampleTypeName).setFields(standardTestSampleFields());
        TestDataGenerator sampleTypeGenerator = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        sampleTypeGenerator.addDataSupplier("sampleDate", () -> sampleTypeGenerator.randomDateString(getCurrentDateFormatString(),
                DateUtils.addDays(new Date(), -365),
                new Date()));
        sampleTypeGenerator.generateRows(sampleCount);
        sampleTypeGenerator.insertRows();
        return sampleTypeGenerator;
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
        return "HeatMapTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
