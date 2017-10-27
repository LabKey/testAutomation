/*
 * Copyright (c) 2013-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.Ext4Helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Category({DailyB.class, Reports.class, Charting.class})
public class RaphaelRendererTest extends BaseWebDriverTest
{
    private static final String MULTI_FOLDER_ZIP = "/sampledata/vis/RaphaelRendererTest.folder.zip";
    private static final String DATE_STUDY_FOLDER_NAME = "Date Based Study";
    private static final String VISIT_STUDY_FOLDER_NAME = "Visit Based Study";
    private static final String GENERIC_CHARTS_FOLDER_NAME = "Generic Charts";
    private static ArrayList<TimeChartImportTest.TimeChartInfo> DATE_CHARTS;
    private static ArrayList<TimeChartImportTest.TimeChartInfo> VISIT_CHARTS;

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        RaphaelRendererTest initTest = (RaphaelRendererTest)getCurrentTest();
        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest.importFolderFromZip(new File(TestFileUtils.getLabKeyRoot(), MULTI_FOLDER_ZIP));
        initTest.populateTimeChartConfigs();
    }
    private void populateTimeChartConfigs()
    {
        DATE_CHARTS = new ArrayList<>();
        VISIT_CHARTS = new ArrayList<>();
        VISIT_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "One Measure: visit based plot per participant", 17, 47, false,
                new String[]{
                        "1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\nG1: V#2/G2: V#3\nInt. Vis. %{S.1.1} .%{S.2.1}\nInt. Vis. %{S.1.1} .%{S.2.1}\n6 wk Post-V#2/V#3\n32.0\n32.5\n33.0\n33.5\n34.0\n34.5\n35.0\n35.5\n36.0\n36.5\n37.0\nAbbr Phy Exam\n999320016\nVisit Label\nTemperature: body\n999320016",
                        "1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\nG1: V#2/G2: V#3\nInt. Vis. %{S.1.1} .%{S.2.1}\nInt. Vis. %{S.1.1} .%{S.2.1}\n6 wk Post-V#2/V#3\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\nAbbr Phy Exam\n999320518\nVisit Label\nTemperature: body\n999320518"
                }
        ));

        VISIT_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "Two Measure: group mean with one plot per dimension", 2, 38, false,
                new String[]{
                        "1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\n1 wk Post-V#2/V#3\n2 wk Post-V#2/V#3\n4 wk Post-V#2/V#3\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n1. Weight\nVisit\n1. Weight\nGroup 1\nFemale\nMale",
                        "1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\n1 wk Post-V#2/V#3\n2 wk Post-V#2/V#3\n4 wk Post-V#2/V#3\n32\n33\n34\n35\n36\n37\n38\n39\n40\nAPX-1: Abbreviated Physical Exam\n2. Body Temp\nVisit\n2. Body Temp\nGroup 1\nFemale\nMale"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "One Measure: filtered for pregnancy records", 1, 6, false,
                new String[]{
                        "50\n100\n150\n200\n250\n56\n58\n60\n62\n64\n66\n68\n70\n72\n74\n76\n78\nPhysical Exam\nDays Since Start Date\nPulse\n249320489\n249320897"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "Four Measures: one axis with point click fn enabled", 1, 17, true,
                new String[]{
                        "50\n100\n150\n200\n250\n300\n350\n200\n400\n600\n800\n1000\n1200\n1400\nLuminexAssay, Lab Results, GenericAssay, Physical Exam\nDays Since Start Date\nFI, CD4+ (cells/mm3), M1, Weight (kg)\n249318596 ABI-QSTAR\n249318596 CD4+(cells/mm3)\n249318596 TNF-alpha (40)\n249318596 Weight (kg)"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "Four Measures: one axis with x-axis range and interval changed", 1, 17, true,
                new String[]{
                        "0\n5\n10\n15\n20\n25\n30\n35\n200\n400\n600\n800\n1000\n1200\n1400\nLuminexAssay, Lab Results, GenericAssay, Physical Exam\nMonths\nFI, CD4+ (cells/mm3), M1, Weight (kg)\n249318596 ABI-QSTAR\n249318596 CD4+(cells/mm3)\n249318596 TNF-alpha (40)\n249318596 Weight (kg)"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "One Measure: FI luminex IL-10 and IL-2 data by Analyte dimension", 2, 30, false,
                new String[]{
                        "0\n50\n100\n150\n200\n80\n800\nLuminex\nIL-10 (23)\nDays Since Start Date\nFI\n249318596\n249320107\n249320127\n249320489\n249320897\n249325717",
                        "0\n50\n100\n150\n200\n60\n600\nLuminex\nIL-2 (3)\nDays Since Start Date\nFI\n249318596\n249320107\n249320127\n249320489\n249320897\n249325717"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "One Measure: FI luminex with thin lines and no data points", 1, 5, false,
                new String[]{
                        "50\n100\n150\n200\n70\n700\nLuminex\nDays Since Start Date\nFI\n249318596 TNF-alpha (40)\n249318596 IL-2 (3)\n249318596 IL-10 (23)"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "One Measure: y-axis log scale and manual range on right side", 1, 33, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nHIV Test Results\nDays Since Start Date\nViral Load Quantified (copies/ml)\n249318596\n249320107\n249320127\n249320489\n249320897"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "One Measure: y-axis log scale and manual range", 1, 33, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nHIV Test Results\nDays Since Start Date\nViral Load Quantified (copies/ml)\n249318596\n249320107\n249320127\n249320489\n249320897"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "Two Measures: cd4 left axis and vl right axis by participant", 3, 23, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n300\n400\n500\n600\n700\n800\n900\n1000\n1100\n1200\n1300\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nPTID\n249318596\nDays Since Start Date\nCD4+ (cells/mm3)\nViral Load Quantified (copies/ml)\n249318596 CD4+(cells/mm3)\n249318596 Viral LoadQuantified (copies/ml)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n300\n400\n500\n600\n700\n800\n900\n1000\n1100\n1200\n1300\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nPTID\n249320127\nDays Since Start Date\nCD4+ (cells/mm3)\nViral Load Quantified (copies/ml)\n249320127 CD4+(cells/mm3)\n249320127 Viral LoadQuantified (copies/ml)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n300\n400\n500\n600\n700\n800\n900\n1000\n1100\n1200\n1300\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nPTID\n249320897\nDays Since Start Date\nCD4+ (cells/mm3)\nViral Load Quantified (copies/ml)\n249320897 CD4+(cells/mm3)\n249320897 Viral LoadQuantified (copies/ml)"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "Two Measure: all cohorts and groups", 8, 99, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n400\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200\nLab Results\nGroup 1: Accute HIV-1\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\nGroup 1: Accute HIV-1Hemoglobin\nGroup 1: Accute HIV-1Lymphs (cells/mm3)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n400\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200\nLab Results\nGroup 2: HIV-1 Negative\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\nGroup 2: HIV-1 NegativeHemoglobin\nGroup 2: HIV-1 NegativeLymphs (cells/mm3)"
                }
        ));

        DATE_CHARTS.add(new TimeChartImportTest.TimeChartInfo(
                "Two Measure: showing both individual lines and aggregate", 3, 50, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n20.0\n200.0\nLab Results\nGroup 1: Accute HIV-1\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\n249318596 Hemoglobin\n249318596 Lymphs(cells/mm3)\n249320107 Hemoglobin\n249320107 Lymphs(cells/mm3)\n249320489 Hemoglobin\n249320489 Lymphs(cells/mm3)\nGroup 1: Accute HIV-1Hemoglobin\nGroup 1: Accute HIV-1Lymphs (cells/mm3)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n20.0\n200.0\nLab Results\nFirst ptid\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\n249318596 Hemoglobin\n249318596 Lymphs(cells/mm3)\nFirst ptid Hemoglobin\nFirst ptid Lymphs(cells/mm3)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n20.0\n200.0\n2000.0\nLab Results\nFemale\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\n249320107 Hemoglobin\n249320107 Lymphs(cells/mm3)\n249320127 Hemoglobin\n249320127 Lymphs(cells/mm3)\n249320489 Hemoglobin\n249320489 Lymphs(cells/mm3)\n249320897 Hemoglobin\n249320897 Lymphs(cells/mm3)\nFemale Hemoglobin\nFemale Lymphs (cells/mm3)"
                }
        ));
    }

    @Test
    public void testVisitBasedTimeCharts()
    {
        goToProjectHome();
        clickFolder(VISIT_STUDY_FOLDER_NAME);
        for (TimeChartImportTest.TimeChartInfo chartInfo : VISIT_CHARTS)
        {
            clickTab("Clinical and Assay Data");
            waitForElement(Locator.linkWithText(chartInfo.getName()));
            clickAndWait(Locator.linkWithText(chartInfo.getName()));
            addUrlParameter("useRaphael=true");
            verifyTimeChart(chartInfo);
        }
    }

    @Test
    public void testDateBasedTimeCharts()
    {
        goToProjectHome();
        clickFolder(DATE_STUDY_FOLDER_NAME);
        for (TimeChartImportTest.TimeChartInfo chartInfo : DATE_CHARTS)
        {
            clickTab("Clinical and Assay Data");
            waitForElement(Locator.linkWithText(chartInfo.getName()));
            clickAndWait(Locator.linkWithText(chartInfo.getName()));
            addUrlParameter("useRaphael=true");
            verifyTimeChart(chartInfo);
        }

    }

    private void verifyTimeChart(TimeChartImportTest.TimeChartInfo info)
    {
        waitForElements(Locator.css("div:not(.thumbnail) > svg"), info.getCountSVGs());
        for (int i = 0; i < info.getSvg().length; i++)
        {
            log("Verify SVG for chart index " + i);
            assertSVG(info.getSvg()[i], i);
        }

        // verify that if the plot has a point click fn, the data points are clickable
        if (info.hasPointClickFn())
        {
            click(Locator.css("svg a path"));
            _extHelper.waitForExtDialog("Data Point Information");
            waitAndClick(Ext4Helper.Locators.ext4Button("OK"));
        }
    }

    private static final String SCATTER_DEFAULT_NAME = "Scatter Plot - Default Options";
    private static final String SCATTER_SAME_SHAPE_COLOR_NAME = "Scatter Plot - Same Shape and Color";
    private static final String SCATTER_SEPARATE_SHAPE_COLOR_NAME = "Scatter Plot - Separate Shape and Color";
    private static final String SCATTER_SHAPE_COLOR_POINTCLICKFN_NAME = "Scatter Plot - Shape, Color, PointClickFn";
    private static final String SCATTER_OPACITY_SIZE_COLOR_NAME = "Scatter Plot - Opacity, Size, Color";

    private static final String SCATTER_DEFAULT = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight";
    private static final String SCATTER_SAME_SHAPE_COLOR = "70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\nAPX Main Title\n3. BP diastolic /xxx\n3. BP systolic xxx/\n999320016\n999320485\n999320518\n999320533\n999320541\n999320557\n999320576\n999320582\n999320590\n999320624\n999320646\n999320652\n999320660\n999320695\n999320703\n999320719\n999321029\n999321033";
    private static final String SCATTER_SEPARATE_SHAPE_COLOR = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight\n999320016\n999320485\n999320518\n999320533\n999320541\n999320557\n999320576\n999320582\n999320590\n999320624\n999320646\n999320652\n999320660\n999320695\n999320703\n999320719\n999321029\n999321033\n0\nNormal\nNot Done\nAbnormal";
    private static final String SCATTER_OPACITY_SIZE_COLOR = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight";
    private static final String SCATTER_SHAPE_COLOR_POINTCLICKFN = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight\n0\nAbnormal\nNormal\nNot Done";
    private static final String SCATTER_EXPORTED = "70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\nAPX Main Title\n3. BP diastolic /xxx\n3. BP systolic xxx/\n999320016\n999320485\n999320518\n999320533\n999320541\n999320557\n999320576\n999320582\n999320590\n999320624\n999320646\n999320652\n999320660\n999320695\n999320703\n999320719\n999321029\n999321033";

    @Test
    public void testScatterPlots()
    {
        goToProjectHome();

        verifyGenericChart(SCATTER_DEFAULT_NAME, SCATTER_DEFAULT);
        verifyGenericChart(SCATTER_SAME_SHAPE_COLOR_NAME, SCATTER_SAME_SHAPE_COLOR);
        verifyGenericChart(SCATTER_SEPARATE_SHAPE_COLOR_NAME, SCATTER_SEPARATE_SHAPE_COLOR);
        verifyGenericChart(SCATTER_OPACITY_SIZE_COLOR_NAME, SCATTER_OPACITY_SIZE_COLOR);
        verifyGenericChart(SCATTER_SHAPE_COLOR_POINTCLICKFN_NAME, SCATTER_SHAPE_COLOR_POINTCLICKFN);
        doAndWaitForPageToLoad(() -> fireEvent(Locator.css("svg a path"), SeleniumEvent.click));


        clickFolder(GENERIC_CHARTS_FOLDER_NAME);
        waitForElement(Locator.css("#exportedChart > svg"));
        assertSVG(SCATTER_EXPORTED, 0);
    }

    private static final String BOX_DEFAULT_NAME = "Box Plot - Default Options";
    private static final String BOX_ALL_POINTS_COLORED_NAME = "Box Plot - All Points, Colored";
    private static final String BOX_NO_POINTS_LINE_WIDTH_COLOR_NAME = "Box Plot - No Points, Line Width, Colors";
    private static final String BOX_POINTCLICKFN_NAME = "Box Plot - PointClickFn";

    private static final String BOX_DEFAULT = "Not in Cohort\n36.5\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\n40.0\nRCH-1: Reactogenicity-Day 1 - 2.Body temperature\nCohort\n2.Body temperature";
    private static final String BOX_ALL_POINTS_COLORED = "Mice A\nMice B\nMice C\nNot in Cat Mice Let\n36.5\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\n40.0\nRCH-1: Reactogenicity-Day 1 - 2.Body temperature\nCat Mice Let\n2.Body temperature\nn/a";
    private static final String BOX_NO_POINTS_LINE_WIDTH_COLOR = "Mice A\nMice B\nMice C\nNot in Cat Mice Let\n36.5\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\n40.0\nRCH-1: Reactogenicity-Day 1 - 2.Body temperature\nCat Mice Let\n2.Body temperature";
    private static final String BOX_POINTCLICKFN = "Not in Cohort\n36.5\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\n40.0\nRCH-1: Reactogenicity-Day 1 - 2.Body temperature\nCohort\n2.Body temperature";
    private static final String BOX_EXPORTED = "Not in Cohort\n36.5\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\n40.0\nRCH-1: Reactogenicity-Day 1 - 2.Body temperature\nCohort\n2.Body temperature\n999320557\n999320565\n999320671\n999320719\n999321033";

    @Test
    public void testBoxPlots()
    {
        goToProjectHome();

        verifyGenericChart(BOX_DEFAULT_NAME, BOX_DEFAULT);
        verifyGenericChart(BOX_ALL_POINTS_COLORED_NAME, BOX_ALL_POINTS_COLORED);
        verifyGenericChart(BOX_NO_POINTS_LINE_WIDTH_COLOR_NAME, BOX_NO_POINTS_LINE_WIDTH_COLOR);
        verifyGenericChart(BOX_POINTCLICKFN_NAME, BOX_POINTCLICKFN);
        fireEvent(Locator.css("svg a path"), SeleniumEvent.click);
        _extHelper.waitForExtDialog("Data Point Information");
        waitAndClick(Ext4Helper.Locators.ext4Button("OK"));

        clickFolder(GENERIC_CHARTS_FOLDER_NAME);
        waitForElement(Locator.css("#exportedBoxPlot > svg"));
        assertSVG(BOX_EXPORTED, 1);
        fireEvent(Locator.css("#exportedBoxPlot svg a path"), SeleniumEvent.click);
        _extHelper.waitForExtDialog("Data Point Information");
        waitAndClick(Ext4Helper.Locators.ext4Button("OK"));
    }

    private void verifyGenericChart(String chartName, String expectedSVG)
    {
        clickFolder(GENERIC_CHARTS_FOLDER_NAME);
        waitForText(chartName);
        click(Locator.linkWithText(chartName));
        _ext4Helper.waitForMaskToDisappear();
        addUrlParameter("useRaphael=true");
        waitForElement(Locator.css("div:not(.thumbnail) > svg"));
        assertSVG(expectedSVG);
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "RaphaelRendererTestProject";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        log("Cleaning up");
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("visualization");
    }

}
