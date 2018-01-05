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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This test imports a folder archive that has 2 subfolders (a date based study and a visit based study) which have been
 * trimmed down to contain just the datasets/queries/etc. that are needed for the Time Chart reports in the study folders
 * that are imported (2 in the visit based study and 10 in the date based study).
 *
 * It verifies the contents of the imported time chart reports by checking the SVG content of one or more plots in the
 * report by checking the main title, axis label, axis ranges, and legend text. If the chart has a point click fn, it
 * tests that by clicking on a data point. And it checks the number of rows in the "view data" grid to verify filters
 * on the report.
 *
 * // TODO: add verification for plots with aggregate lines that have error bars
 * // TODO: add verification for plot line with and whether or not the data points are shown
 */
@Category({DailyC.class, Reports.class, Charting.class})
public class TimeChartImportTest extends StudyBaseTest
{
    private static final File MULTI_FOLDER_ZIP = TestFileUtils.getSampleData("studies/TimeChartTesting.folder.zip");
    private static final String EXPORT_TEST_FOLDER = "exportTestFolder";
    private static ArrayList<TimeChartInfo> EXPORTED_CHARTS;
    private static final String DATE_STUDY_FOLDER_NAME = "Date Based Study";
    private static ArrayList<TimeChartInfo> DATE_CHARTS;
    private static final String VISIT_STUDY_FOLDER_NAME = "Visit Based Study";
    private static ArrayList<TimeChartInfo> VISIT_CHARTS;

    @BeforeClass
    public static void doSetup() throws Exception
    {
        TimeChartImportTest initTest = (TimeChartImportTest)getCurrentTest();

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest.importFolderFromZip(MULTI_FOLDER_ZIP);
        initTest._containerHelper.createSubfolder(initTest.getProjectName(), EXPORT_TEST_FOLDER, "Collaboration");
        initTest.populateChartConfigs();
    }

    @Override @Ignore
    public void testSteps(){}

    @Override
    protected void doVerifySteps(){}

    @Override
    protected void doCreateSteps(){}

    private void populateChartConfigs()
    {
        EXPORTED_CHARTS = new ArrayList<>();
        VISIT_CHARTS = new ArrayList<>();
        DATE_CHARTS = new ArrayList<>();

        EXPORTED_CHARTS.add(new TimeChartInfo(
                "One Measure: visit based plot per participant", 17, 47, false,
                new String[]{
                        "Created with Rapha\u00ebl 2.1.0\n1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\nG1: V#2/G2: V#3\nInt. Vis. %{S.1.1} .%{S.2.1}\nInt. Vis. %{S.1.1} .%{S.2.1}\n6 wk Post-V#2/V#3\n32.0\n32.5\n33.0\n33.5\n34.0\n34.5\n35.0\n35.5\n36.0\n36.5\n37.0\nAbbr Phy Exam\n999320016\nVisit Label\nTemperature: body\n999320016"
                }
        ));

        VISIT_CHARTS.add(new TimeChartInfo(
                "One Measure: visit based plot per participant", 17, 47, false,
                new String[]{
                        "1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\nG1: V#2/G2: V#3\nInt. Vis. %{S.1.1} .%{S.2.1}\nInt. Vis. %{S.1.1} .%{S.2.1}\n6 wk Post-V#2/V#3\n32.0\n32.5\n33.0\n33.5\n34.0\n34.5\n35.0\n35.5\n36.0\n36.5\n37.0\nAbbr Phy Exam\n999320016\nVisit Label\nTemperature: body\n999320016",
                        "1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\nG1: V#2/G2: V#3\nInt. Vis. %{S.1.1} .%{S.2.1}\nInt. Vis. %{S.1.1} .%{S.2.1}\n6 wk Post-V#2/V#3\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\nAbbr Phy Exam\n999320518\nVisit Label\nTemperature: body\n999320518"
                }
        ));

        VISIT_CHARTS.add(new TimeChartInfo(
                "Two Measure: group mean with one plot per dimension", 2, 38, false,
                new String[]{
                        "1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\n1 wk Post-V#2/V#3\n2 wk Post-V#2/V#3\n4 wk Post-V#2/V#3\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n1. Weight\nVisit\n1. Weight\nGroup 1\nFemale\nMale",
                        "1 week Post-V#1\nInt. Vis. %{S.1.1} .%{S.2.1}\nGrp1:F/U/Grp2:V#2\nG1: 6wk/G2: 2wk\n6 week Post-V#2\n1 wk Post-V#2/V#3\n2 wk Post-V#2/V#3\n4 wk Post-V#2/V#3\n32\n33\n34\n35\n36\n37\n38\n39\n40\nAPX-1: Abbreviated Physical Exam\n2. Body Temp\nVisit\n2. Body Temp\nGroup 1\nFemale\nMale"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "One Measure: filtered for pregnancy records", 1, 6, false,
                new String[]{
                        "50\n100\n150\n200\n250\n56\n58\n60\n62\n64\n66\n68\n70\n72\n74\n76\n78\nPhysical Exam\nDays Since Start Date\nPulse\n249320489\n249320897"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "Four Measures: one axis with point click fn enabled", 1, 17, true,
                new String[]{
                        "50\n100\n150\n200\n250\n300\n350\n200\n400\n600\n800\n1000\n1200\n1400\nLuminexAssay, Lab Results, GenericAssay, Physical Exam\nDays Since Start Date\nFI, CD4+ (cells/mm3), M1, Weight (kg)\n249318596 ABI-QSTAR\n249318596 CD4+ (cells/mm3)\n249318596 TNF-alpha (40)\n249318596 Weight (kg)"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "Four Measures: one axis with x-axis range and interval changed", 1, 17, true,
                new String[]{
                        "0\n5\n10\n15\n20\n25\n30\n35\n200\n400\n600\n800\n1000\n1200\n1400\nLuminexAssay, Lab Results, GenericAssay, Physical Exam\nMonths\nFI, CD4+ (cells/mm3), M1, Weight (kg)\n249318596 ABI-QSTAR\n249318596 CD4+ (cells/mm3)\n249318596 TNF-alpha (40)\n249318596 Weight (kg)"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "One Measure: FI luminex IL-10 and IL-2 data by Analyte dimension", 2, 30, false,
                new String[]{
                        "0\n50\n100\n150\n200\n80\n800\nLuminex\nIL-10 (23)\nDays Since Start Date\nFI\n249318596\n249320107\n249320127\n249320489\n249320897\n249325717",
                        "0\n50\n100\n150\n200\n60\n600\nLuminex\nIL-2 (3)\nDays Since Start Date\nFI\n249318596\n249320107\n249320127\n249320489\n249320897\n249325717"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "One Measure: FI luminex with thin lines and no data points", 1, 5, false,
                new String[]{
                        "50\n100\n150\n200\n70\n700\nLuminex\nDays Since Start Date\nFI\n249318596 TNF-alpha (40)\n249318596 IL-2 (3)\n249318596 IL-10 (23)"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "One Measure: y-axis log scale and manual range on right side", 1, 33, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nHIV Test Results\nDays Since Start Date\nViral Load Quantified (copies/ml)\n249318596\n249320107\n249320127\n249320489\n249320897"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "One Measure: y-axis log scale and manual range", 1, 33, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nHIV Test Results\nDays Since Start Date\nViral Load Quantified (copies/ml)\n249318596\n249320107\n249320127\n249320489\n249320897"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "Two Measures: cd4 left axis and vl right axis by participant", 3, 23, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n300\n400\n500\n600\n700\n800\n900\n1000\n1100\n1200\n1300\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nPTID\n249318596\nDays Since Start Date\nCD4+ (cells/mm3)\nViral Load Quantified (copies/ml)\n249318596 CD4+(cells/mm3)\n249318596 Viral LoadQuantified (copies/ml)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n300\n400\n500\n600\n700\n800\n900\n1000\n1100\n1200\n1300\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nPTID\n249320127\nDays Since Start Date\nCD4+ (cells/mm3)\nViral Load Quantified (copies/ml)\n249320127 CD4+(cells/mm3)\n249320127 Viral LoadQuantified (copies/ml)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n300\n400\n500\n600\n700\n800\n900\n1000\n1100\n1200\n1300\n10\n100\n1000\n10000\n1e+5\n1e+6\n1e+7\nPTID\n249320897\nDays Since Start Date\nCD4+ (cells/mm3)\nViral Load Quantified (copies/ml)\n249320897 CD4+(cells/mm3)\n249320897 Viral LoadQuantified (copies/ml)"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "Two Measure: all cohorts and groups", 8, 99, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n400\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200\nLab Results\nGroup 1: Accute HIV-1\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\nGroup 1: Accute HIV-1Hemoglobin\nGroup 1: Accute HIV-1Lymphs (cells/mm3)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n200\n400\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200\nLab Results\nGroup 2: HIV-1 Negative\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\nGroup 2: HIV-1 NegativeHemoglobin\nGroup 2: HIV-1 NegativeLymphs (cells/mm3)"
                }
        ));

        DATE_CHARTS.add(new TimeChartInfo(
                "Two Measure: showing both individual lines and aggregate", 3, 50, false,
                new String[]{
                        "0\n50\n100\n150\n200\n250\n300\n350\n20.0\n200.0\nLab Results\nGroup 1: Accute HIV-1\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\n249318596 Hemoglobin\n249318596 Lymphs(cells/mm3)\n249320107 Hemoglobin\n249320107 Lymphs(cells/mm3)\n249320489 Hemoglobin\n249320489 Lymphs(cells/mm3)\nGroup 1: Accute HIV-1Hemoglobin\nGroup 1: Accute HIV-1Lymphs (cells/mm3)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n20.0\n200.0\nLab Results\nFirst ptid\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\n249318596 Hemoglobin\n249318596 Lymphs(cells/mm3)\nFirst ptid Hemoglobin\nFirst ptid Lymphs (cells/mm3)",
                        "0\n50\n100\n150\n200\n250\n300\n350\n20.0\n200.0\n2000.0\nLab Results\nFemale\nDays Since Start Date\nHemoglobin, Lymphs (cells/mm3)\n249320107 Hemoglobin\n249320107 Lymphs(cells/mm3)\n249320127 Hemoglobin\n249320127 Lymphs(cells/mm3)\n249320489 Hemoglobin\n249320489 Lymphs(cells/mm3)\n249320897 Hemoglobin\n249320897 Lymphs(cells/mm3)\nFemale Hemoglobin\nFemale Lymphs (cells/mm3)"
                }
        ));
    }

    @Test
    public void verifyVisitBasedCharts()
    {
        verifyTimeCharts(VISIT_STUDY_FOLDER_NAME, VISIT_CHARTS);
    }

    @Test
    public void verifyDateBasedCharts()
    {
        verifyTimeCharts(DATE_STUDY_FOLDER_NAME, DATE_CHARTS);
    }

    private void verifyTimeCharts(String folderName, List<TimeChartInfo> chartInfos)
    {
        goToProjectHome();
        clickFolder(folderName);
        for (TimeChartInfo chartInfo : chartInfos)
        {
            clickTab("Clinical and Assay Data");
            waitAndClickAndWait(Locator.linkWithText(chartInfo.getName()));
            verifyTimeChartInfo(chartInfo, true);
        }
    }

    private void verifyTimeChartInfo(TimeChartInfo info, boolean isTimeChartWizard)
    {
        log("Verify chart information: " + info.getName());

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

        if (!isTimeChartWizard)
            return;

        // verify that clicking the Export as Script button works
        Assert.assertEquals("Unexpected number of export script icons", info.getCountSVGs(), getExportScriptIconCount("chart-render-div"));
        String exportScript = getExportScript();
        Assert.assertTrue(exportScript != null);

        // verify that there is a PDF export for each plot
        Assert.assertEquals("Unexpected number of export PNG icons", info.getCountSVGs(), getExportPDFIconCount("chart-render-div"));

        // verify that there is a PNG export for each plot
        Assert.assertEquals("Unexpected number of export PNG icons", info.getCountSVGs(), getExportPNGIconCount("chart-render-div"));

        // verify the count of records in the view data grid
        clickButton("View Data", 0);
        waitForElement(Locator.paginationText(info.getGridCount()));
    }

    @Test
    public void verifyExportToScript()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);

        // TODO: this test does not test the d3 renderer for exported charts. We should fix this.
        log("Export Time Chart as Script and paste into Wiki");
        goToProjectHome();
        clickFolder(VISIT_STUDY_FOLDER_NAME);
        clickTab("Clinical and Assay Data");

        TimeChartInfo info = VISIT_CHARTS.get(0);
        waitForElement(Locator.linkWithText(info.getName()));
        clickAndWait(Locator.linkWithText(info.getName()));
        waitForElements(Locator.css("div:not(.thumbnail) > svg"), info.getCountSVGs());

        Assert.assertEquals("Unexpected number of export script icons", info.getCountSVGs(), getExportScriptIconCount("chart-render-div"));
        String exportScript = getExportScript();
        clickFolder(EXPORT_TEST_FOLDER);
        portalHelper.addWebPart("Wiki");
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), "timeChartExportTest");
        wikiHelper.setWikiBody(exportScript);
        wikiHelper.saveWikiPage();

        verifyTimeChartInfo(EXPORTED_CHARTS.get(0), false);
    }

    private String getExportScript()
    {
        clickExportScriptIcon("chart-render-div", 0);
        String exportScript = _extHelper.getCodeMirrorValue("export-script-textarea");
        waitAndClick(Ext4Helper.Locators.ext4Button("Close"));

        return exportScript;
    }

    @Test
    public void verifyMaskedPtidOnPublishStudy()
    {
        // Added for issue 18763
        String publishFolderName = "MaskedPtidStudy";

        log("Get the original mouse Ids from the imported study");
        goToProjectHome();
        clickFolder(VISIT_STUDY_FOLDER_NAME);
        clickTab("Clinical and Assay Data");
        waitAndClickAndWait(Locator.linkWithText("DEM-1: Demographics"));
        DataRegionTable table = new DataRegionTable("Dataset", this);
        List<String> origMouseIds = table.getColumnDataAsText("MouseId");

        log("Created published study from Visit based study, with masked ptids");
        _studyHelper.publishStudy(publishFolderName, 1, "Mouse", "Mice", "Visits", null);

        log("Verify masked ptids in publish study reportInfo");
        clickFolder(publishFolderName);
        for (TimeChartInfo chartInfo : VISIT_CHARTS)
        {
            clickTab("Clinical and Assay Data");
            waitAndClickAndWait(Locator.linkWithText(chartInfo.getName()));
            beginAt("/reports/" + getProjectName() + "/" + VISIT_STUDY_FOLDER_NAME + "/" + publishFolderName + "/reportInfo.view?reportId="
                    + getUrlParam("reportId"));
            waitForText("Report Debug Information");
            for (String origMouseId : origMouseIds)
            {
                assertTextNotPresent(origMouseId);
            }
        }
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    public static class TimeChartInfo
    {
        private String _name;
        private int _countSVGs;
        private int _gridCount;
        private boolean _hasPointClickFn;
        private String[] _svg;

        public TimeChartInfo(String name, int countSVGs, int gridCount, boolean hasPointClickFn, String[] svg)
        {
            _name = name;
            _countSVGs = countSVGs;
            _gridCount = gridCount;
            _hasPointClickFn = hasPointClickFn;
            _svg = svg;
        }

        public String getName()
        {
            return _name;
        }

        public int getGridCount()
        {
            return _gridCount;
        }

        public String[] getSvg()
        {
            return _svg;
        }

        public int getCountSVGs()
        {
            return _countSVGs;
        }

        public boolean hasPointClickFn()
        {
            return _hasPointClickFn;
        }
    }
}
