package org.labkey.test.tests.nab;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.AssayImporter;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class, Assays.class})
public class NAbResultCalcTest extends BaseWebDriverTest
{
    protected static final String TEST_ASSAY_NAB = "TestAssayNab";
    protected final File TEST_ASSAY_NAB_FILE = TestFileUtils.getSampleData("Nab/m0902055;4001.xlsx");

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        NAbResultCalcTest init = (NAbResultCalcTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Assay");

        _assayHelper.createAssayDesign("TZM-bl Neutralization (NAb)", TEST_ASSAY_NAB)
                .setPlateTemplate("NAb: 5 specimens in duplicate")
                .clickFinish();
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test // Issue 49036
    public void testPercentNeutralizationCalcsForDilution()
    {
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        new AssayImporter(this, new AssayImportOptions.ImportOptionsBuilder().
                assayId("Method Dilution").
                visitResolver(AssayImportOptions.VisitResolverType.SpecimenID).
                cutoff1("50").
                curveFitMethod("Five Parameter").
                sampleIds(new String[]{"s1", "s2", "s3", "s4", "s5"}).
                initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                runFile(TEST_ASSAY_NAB_FILE).
                build()).doImport();

        clickAndWait(Locator.linkWithText("View Results"));

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("PercentNeutralizationInitialDilution", "Percent Neutralization Initial Dilution");
        _customizeViewsHelper.addColumn("PercentNeutralizationMax", "Percent Neutralization Max");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable region = new DataRegionTable("Data", this);
        Assert.assertEquals("Perc Neut max and initial dilution values should match for this run when method is Dilution",
                region.getColumnDataAsText("PercentNeutralizationInitialDilution"),
                region.getColumnDataAsText("PercentNeutralizationMax")
            );
    }

    @Test // Issue 49036
    public void testPercentNeutralizationCalcsForConcentration()
    {
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        new AssayImporter(this, new AssayImportOptions.ImportOptionsBuilder().
                assayId("Method Concentration").
                visitResolver(AssayImportOptions.VisitResolverType.SpecimenID).
                cutoff1("50").
                curveFitMethod("Five Parameter").
                sampleIds(new String[]{"s1", "s2", "s3", "s4", "s5"}).
                initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                methods(new String[]{"Concentration", "Concentration", "Concentration", "Concentration", "Concentration"}).
                runFile(TEST_ASSAY_NAB_FILE).
                build()).doImport();

        clickAndWait(Locator.linkWithText("View Results"));

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("PercentNeutralizationInitialDilution", "Percent Neutralization Initial Dilution");
        _customizeViewsHelper.addColumn("PercentNeutralizationMax", "Percent Neutralization Max");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable region = new DataRegionTable("Data", this);
        Assert.assertEquals("Perc Neut max and initial dilution values should match for this run when method is Dilution",
                region.getColumnDataAsText("PercentNeutralizationInitialDilution"),
                region.getColumnDataAsText("PercentNeutralizationMax")
            );
    }

    @Override
    protected String getProjectName()
    {
        return "NAbResultCalcTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
