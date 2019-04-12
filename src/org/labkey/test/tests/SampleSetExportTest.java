package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper.ColumnHeaderType;
import org.labkey.test.util.SampleSetHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class SampleSetExportTest extends AbstractExportTest
{
    private static final String SAMPLE_SET_NAME = SampleSetExportTest.class.getSimpleName();

    private static final String SAMPLE_DATA = "Name\tBarcode\n" +
            "Q\tabc123\n" +
            "A\tabc123\n" +
            "D\tabc123\n" +
            "C\tabc123\n" +
            "Z\tabc123\n" +
            "E\tabc123\n";

    @BeforeClass
    public static void doSetup() throws Exception
    {
        SampleSetExportTest initTest = (SampleSetExportTest)getCurrentTest();

        initTest._containerHelper.createProject(initTest.getProjectName(), null);

        initTest.beginAt("/" + initTest.getProjectName() + "/experiment-listMaterialSources.view");
        SampleSetHelper sampleHelper = new SampleSetHelper(initTest);
        sampleHelper.createSampleSet(SAMPLE_SET_NAME, null, Map.of("Barcode", FieldDefinition.ColumnType.String), SAMPLE_DATA);
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected String getProjectName()
    {
        return "SampleSet Download Test";
    }

    @Override
    protected boolean hasSelectors()
    {
        return true;
    }

    @Override
    protected String getTestColumnTitle()
    {
        return "Name";
    }

    @Override
    protected int getTestColumnIndex()
    {
        return 0;
    }

    @Override
    protected String getExportedXlsTestColumnHeader(ColumnHeaderType exportType)
    {
        return "Name";
    }

    @Override
    protected String getExportedTsvTestColumnHeader(ColumnHeaderType exportType)
    {
        return "Name";
    }

    @Override
    protected String getDataRegionColumnName()
    {
        return "Name";
    }

    @Override
    protected String getDataRegionSchemaName()
    {
        return "samples";
    }

    @Override
    protected String getDataRegionQueryName()
    {
        return SAMPLE_SET_NAME;
    }

    @Override
    protected String getExportedFilePrefixRegex()
    {
        return SAMPLE_SET_NAME;
    }

    @Override
    protected String getDataRegionId()
    {
        return "Material";
    }

    @Override
    protected void goToDataRegionPage()
    {
        beginAt("/" + getProjectName() + "/experiment-listMaterialSources.view");
        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));
    }


    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }
}
