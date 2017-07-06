/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

package org.labkey.test.tests.flow;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Flow;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.flow.FlowReportsWebpart;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.flow.reports.QCReportEditorPage;
import org.labkey.test.pages.flow.reports.ReportEditorPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({BVT.class, Flow.class})
public class FlowTest extends BaseFlowTest
{
    private final boolean IS_BOOTSTRAP_LAYOUT_WHITELISTED = setIsBootstrapWhitelisted(false); // set true to whitelist me before constructor time
    public static final String SELECT_CHECKBOX_NAME = ".select";
    private static final String QUV_ANALYSIS_SCRIPT = "/sampledata/flow/8color/quv-analysis.xml";
    private static final String FCS_FILE_1 = "L02-060120-QUV-JS";
    private static final String FCS_FILE_2 = "L04-060120-QUV-JS";
    private static final String QUV_ANALYSIS_NAME = "QUV analysis";

    @BeforeClass
    public static void initR()
    {
        FlowTest init = (FlowTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        // R is needed for the positivity report
        RReportHelper _rReportHelper = new RReportHelper(getCurrentTest());
        _rReportHelper.ensureRConfig();

        goToFlowDashboard();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Flow Reports");

        setupQuery();
        importFiles();
    }

    @Test
    public void _doTestSteps()
    {
        analysisFilterTest();
        testChartMeasurePicker();
        configureSampleSetAndMetadata();
        sampleSetAndMetadataTest();
        customGraphQuery();
        positivityReportTest();
        qcReportTest();
        copyAnalysisScriptTest();
        removeAnalysisFilter();
        verifyDiscoverableFCSFiles();
        verifyExperimentRunGraphLinks();
    }

    String query1 =  TRICKY_CHARACTERS_NO_QUOTES + "DRTQuery1";
    String analysisName = "FCSAnalyses";
    @LogMethod
    protected void setupQuery()
    {
        String querySql = "SELECT " + analysisName + ".RowId, " +
            analysisName + ".Statistic.\"Count\", " +
            analysisName + ".Run.FilePathRoot, " +
            analysisName + ".FCSFile.Run.WellCount " +
            "FROM " + analysisName + " AS " + analysisName;

        createQuery(getContainerPath(), query1, "flow", querySql, null, false);

        clickButton("Execute Query", 0);
        waitForText(WAIT_FOR_JAVASCRIPT, "No data to show.");
    }

    @LogMethod
    protected void importFiles()
    {
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Browse for FCS files to be imported"));

        // Should allow for import all directories containing FCS Files
        _fileBrowserHelper.selectFileBrowserItem("8color/");
        waitForElement(FileBrowserHelper.Locators.gridRowCheckbox(FCS_FILE_2, false), WAIT_FOR_JAVASCRIPT);
        _fileBrowserHelper.selectImportDataAction("Import Directory of FCS Files");

        assertTextPresent(
                "The following directories within",
                "8color",
                FCS_FILE_1 + " (25 fcs files)",
                FCS_FILE_2 + " (14 fcs files)");
        clickButton("Cancel"); // go back to file-browser

        // Entering L02-060120-QUV-JS directory should allow import of current directory
        _fileBrowserHelper.selectFileBrowserItem("8color/" + FCS_FILE_1 + "/");
        waitForElement(FileBrowserHelper.Locators.gridRowCheckbox("91761.fcs", false), WAIT_FOR_JAVASCRIPT);
        _fileBrowserHelper.selectImportDataAction("Current directory of 25 FCS Files");
        assertTextPresent(
                "The following directories within",
                "'8color",
                FCS_FILE_1,
                "Current Directory (25 fcs files)");
        assertTextNotPresent(FCS_FILE_2);
        clickButton("Import Selected Runs");
        waitForPipeline(getContainerPath());
        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        // Drill into the run, and see that it was uploaded, and keywords were read.
        clickAndWait(Locator.linkWithText("1 run"));
        setSelectedFields(getContainerPath(), "flow", "Runs", null, new String[]{"Flag", "Name", "ProtocolStep", "AnalysisScript", "CompensationMatrix", "WellCount", "Created", "RowId", "FilePathRoot"});

        DataRegionTable queryTable = new DataRegionTable("query", getDriver());
        clickAndWait(queryTable.detailsLink(0));
        //clickAndWait(Locator.linkWithText("details"));
        setSelectedFields(getContainerPath(), "flow", "FCSFiles", null, new String[]{"Keyword/ExperimentName", "Keyword/Stim", "Keyword/Comp", "Keyword/PLATE NAME", "Flag", "Name", "RowId"});
        assertTextPresent("PerCP-Cy5.5 CD8");

        assertElementNotPresent(Locator.linkWithImage("/flagFCSFile.gif"));
        pushLocation();
        clickAndWait(Locator.linkWithText("91761.fcs"));
        assertTextPresent(FCS_FILE_1); // "experiment name" keyword

        clickButton("edit");
        setFormElement(Locator.name("ff_name"), "FlowTest New Name");
        setFormElement(Locator.name("ff_comment"), "FlowTest Comment");
        Locator locPlateName = Locator.xpath("//td/input[@type='hidden' and @value='PLATE NAME']/../../td/input[@name='ff_keywordValue']");
        setFormElement(locPlateName, "FlowTest Keyword Plate Name");
        clickButton("update");
        popLocation();
        assertElementPresent(Locator.linkWithImage("/flagFCSFile.gif"));
        assertElementNotPresent(Locator.linkWithText("91761.fcs"));
        assertElementPresent(Locator.linkWithText("FlowTest New Name"));
        assertTextPresent("FlowTest Keyword Plate Name");

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("Create a new Analysis script"));
        setFormElement(Locator.id("ff_name"), "FlowTestAnalysis");
        clickButton("Create Analysis Script");

        clickAndWait(Locator.linkWithText("Define compensation calculation from scratch"));
        selectOptionByText(Locator.name("selectedRunId"), FCS_FILE_1);
        clickButton("Next Step");

        selectOptionByText(Locator.name("positiveKeywordName[3]"), "Comp");
        selectOptionByText(Locator.name("positiveKeywordValue[3]"), "FITC CD4");
        clickAndWait(Locator.tagWithAttribute("input", "value", "Submit"));
        assertTextPresent("Missing data");
        selectOptionByText(Locator.name("negativeKeywordName[0]"), "WELL ID");
        selectOptionByText(Locator.name("negativeKeywordValue[0]"), "H01");
        clickButtonWithText("Universal");
        clickAndWait(Locator.tagWithAttribute("input", "value", "Submit"));
        assertTextPresent("compensation calculation may be edited in a number");

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("Create a new Analysis script"));
        setFormElement(Locator.id("ff_name"), QUV_ANALYSIS_NAME);
        clickButton("Create Analysis Script");
        clickAndWait(Locator.linkWithText("View Source"));
        setFormElement(Locator.name("script"), TestFileUtils.getFileContents(QUV_ANALYSIS_SCRIPT));
        clickAndWait(Locator.tagWithAttribute("input", "value", "Submit"));
    }

    @LogMethod
    protected void analysisFilterTest()
    {
        setFlowFilter(new String[] {"Keyword/Stim"}, new String[] {"isnonblank"}, new String[] {""});

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText(QUV_ANALYSIS_NAME));
        clickAndWait(Locator.linkWithText("Analyze some runs"));

        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickButton("Analyze selected runs");
        setFormElement(Locator.name("ff_analysisName"), "FlowExperiment2");
        clickButton("Analyze runs");
        waitForPipeline(getContainerPath());
        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("FlowExperiment2"));
        URL urlBase = getURL();
        URL urlCompensation;
        URL urlAnalysis;
        try
        {
            urlCompensation = new URL(urlBase.getProtocol(), urlBase.getHost(), urlBase.getPort(), urlBase.getFile() + "&query.ProtocolStep~eq=Compensation");
            urlAnalysis = new URL(urlBase.getProtocol(), urlBase.getHost(), urlBase.getPort(), urlBase.getFile() + "&query.ProtocolStep~eq=Analysis");
        }
        catch (MalformedURLException mue)
        {
            throw new RuntimeException(mue);
        }

        beginAt(urlCompensation.getFile());
        DataRegionTable queryTable = new DataRegionTable("query", getDriver());
        clickAndWait(queryTable.detailsLink(0));

        setSelectedFields(getContainerPath(), "flow", "CompensationControls", null, new String[] {"Name","Flag","Created","Run","FCSFile","RowId"});

        assertElementPresent(Locator.linkWithText("PE Green laser-A+"));
        assertElementNotPresent(Locator.linkWithText("91918.fcs"));
        clickAndWait(Locator.linkWithText("PE Green laser-A+"));
        pushLocation();
        clickAndWait(Locator.linkWithText("Keywords from the FCS file"));
        assertTextPresent("PE CD8");
        popLocation();
        clickAndWait(Locator.linkWithText("FlowExperiment2"));
        beginAt(urlAnalysis.getFile());

        DataRegionTable table = new DataRegionTable("query", getDriver());
        clickAndWait(table.detailsLink(0));

        clickAndWait(Locator.linkWithText("91918.fcs"));
        clickAndWait(Locator.linkWithText("More Graphs"));
        selectOptionByText(Locator.name("subset"), "Singlets/L/Live/3+/4+");
        selectOptionByText(Locator.name("xaxis"), "comp-PE Cy7-A IFNg");
        selectOptionByText(Locator.name("yaxis"), "comp-PE Green laser-A IL2");
        clickButtonWithText("Show Graph");

        // change the name of an analysis
        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("Other settings"));
        clickAndWait(Locator.linkWithText("Change FCS Analyses Names"));
        selectOptionByValue(Locator.xpath("//select[@name='ff_keyword']").index(1), "Keyword/EXPERIMENT NAME");
        clickButton("Set names");

        beginAt(urlAnalysis.getFile());
        table = new DataRegionTable("query", getDriver());
        clickAndWait(table.detailsLink(0));
        clickAndWait(Locator.linkWithText("91918.fcs-" + FCS_FILE_1));
        assertTextPresent("91918.fcs-" + FCS_FILE_1);

        // Now, let's add another run:
         clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("Browse for more FCS files to be imported"));

        _fileBrowserHelper.selectFileBrowserItem("8color/");
        waitForElement(FileBrowserHelper.Locators.gridRowCheckbox(FCS_FILE_2, false), WAIT_FOR_JAVASCRIPT);
        _fileBrowserHelper.selectImportDataAction("Import Directory of FCS Files");
        assertTextNotPresent(FCS_FILE_1);
        assertTextPresent(FCS_FILE_2);
        clickButton("Import Selected Runs");
        waitForPipeline(getContainerPath());

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText(QUV_ANALYSIS_NAME));
        click(Locator.linkWithText("Analyze some runs"));
        final Locator.NameLocator ff_targetExperimentId = Locator.name("ff_targetExperimentId");
        waitForElement(ff_targetExperimentId);
        doAndWaitForPageToLoad(() -> selectOptionByText(ff_targetExperimentId, "<create new>"));
        assertEquals(2, countEnabledInputs(SELECT_CHECKBOX_NAME));
        doAndWaitForPageToLoad(() -> selectOptionByText(ff_targetExperimentId, "FlowExperiment2"));

        assertEquals(1, countEnabledInputs(SELECT_CHECKBOX_NAME));
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("ff_compensationMatrixOption"), "Matrix: " + FCS_FILE_1 + " comp matrix"));

        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickButton("Analyze selected runs");
        waitForPipeline(getContainerPath());

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("FlowExperiment2"));
        table = new DataRegionTable("query", getDriver());
        table.clickHeaderMenu("Query", query1);
        //_extHelper.clickMenuButton("Query", query1);
        assertTextPresent("File Path Root");

        setSelectedFields(getContainerPath(), "flow", query1, "MostColumns", new String[] {"RowId", "Count","WellCount"});
        setSelectedFields(getContainerPath(), "flow", query1, "AllColumns", new String[] {"RowId", "Count","WellCount", "FilePathRoot"});
        if (IS_BOOTSTRAP_LAYOUT)
        {
            table.clickHeaderMenu("Grid views", "MostColumns");
        }else
        {
            _extHelper.clickMenuButton("Grid Views", "MostColumns");
        }
        assertTextNotPresent("File Path Root");
        if (IS_BOOTSTRAP_LAYOUT)
        {
            table.clickHeaderMenu("Grid views", "AllColumns");
        }else
        {
            _extHelper.clickMenuButton("Grid Views", "AllColumns");
        }
        assertTextPresent("File Path Root");
    }

    // Regression test : https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=21160
    // Should be able to chart flow data when chart columns are restricted
    @LogMethod
    private void testChartMeasurePicker()
    {
        goToProjectSettings(getProjectName());
        checkCheckbox(Locator.name("restrictedColumnsEnabled"));
        clickButton("Save");
        clickFolder(getFolderName());

        clickAndWait(Locator.linkWithText(QUV_ANALYSIS_NAME));
        clickAndWait(Locator.linkWithText(FCS_FILE_1 + " analysis"));

        List<String> expectedMeasures = new ArrayList<>(Arrays.asList(
                "Count",
                "Singlets:Count",
                "Singlets:%P",
                "L:Count"
        ));

        DataRegionTable fcsAnalysisTable = new DataRegionTable("query", getDriver());
        List<String> columnHeaders = fcsAnalysisTable.getColumnLabels();
        List<String> actualMeasures = columnHeaders.subList(columnHeaders.size() - 4, columnHeaders.size());
        assertEquals("Expected measure columns are missing", expectedMeasures, actualMeasures);
        fcsAnalysisTable.clickHeaderMenu( IS_BOOTSTRAP_LAYOUT ? "Charts / Reports" : "Charts", "Create Chart");

        // The new plot dialog shows all values from the grid that could be used in any aspect of a plot. So need to add these two values.
        expectedMeasures.add("Compensation Matrix");
        expectedMeasures.add("Run");
        ChartTypeDialog chartTypeDialog = new ChartTypeDialog(getDriver());
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Box);
        List<String> availableMeasures =  chartTypeDialog.getColumnList();
        assertEquals("Wrong measures in picker", new HashSet<>(expectedMeasures), new HashSet<>(availableMeasures));
        log("Validate that the values 'Compensation Matrix' and 'Run' cannot be assigned to the Y axis.");
        chartTypeDialog.clickColumnValue("Compensation Matrix");
        assertTrue("You should not be able to set 'Compensation Matrix' to the y-axis.", chartTypeDialog.getYAxisValue().trim().length() == 0);
        chartTypeDialog.clickColumnValue("Run");
        assertTrue("You should not be able to set 'Run' to the y-axis.", chartTypeDialog.getYAxisValue().trim().length() == 0);
        chartTypeDialog.clickCancel();
    }

    // Test sample set and ICS metadata
    @LogMethod
    protected void configureSampleSetAndMetadata()
    {
        uploadSampleDescriptions("/sampledata/flow/8color/sample-set.tsv", new String[]{"Exp Name", "Well Id"}, new String[]{"EXPERIMENT NAME", "WELL ID"});
        setProtocolMetadata(null, "Sample PTID", null, "Sample Visit", true);

        goToFlowDashboard();
        clickAndWait(Locator.linkContainingText("49 sample descriptions"));
        assertTextPresent(
                "49 sample descriptions",
                "10 samples are not joined",
                "39 FCS Files",
                "have been joined",
                "0 FCS Files",
                "are not joined");
    }

    @LogMethod
    public void sampleSetAndMetadataTest()
    {
        // verify sample set and background values can be displayed in the FCSAnalysis grid
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("29 FCS files"));
        if (IS_BOOTSTRAP_LAYOUT)
        {
            new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("span","lk-menu-drop")
                    .withDescendant(Locator.id("PopupText").withText("Show Graphs")).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT))
                    .clickMenuButton(true, false, "Inline");
        }else
        {
            _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='Show Graphs']"), "Inline");
        }
        waitForElement(Locator.css(".labkey-flow-graph"));
        assertTextNotPresent("Error generating graph");
        assertTextPresent("No graph for:", "(<APC-A>)");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn("Background/Count");
        _customizeViewsHelper.removeColumn("Background/Singlets:Count");
        _customizeViewsHelper.removeColumn("Background/Singlets:Freq_Of_Parent");
        _customizeViewsHelper.removeColumn("Background/Singlets$SL:Count");
        _customizeViewsHelper.removeColumn("Graph/(<APC-A>)");
        _customizeViewsHelper.removeColumn("Graph/(<Alexa 680-A>)");
        _customizeViewsHelper.removeColumn("Graph/(<FITC-A>)");
        _customizeViewsHelper.removeColumn("Graph/(<PE Cy55-A>)");

        _customizeViewsHelper.addColumn("FCSFile/Sample/PTID");
        _customizeViewsHelper.addColumn("FCSFile/Sample/Visit");
        _customizeViewsHelper.addColumn("FCSFile/Sample/Stim");
        _customizeViewsHelper.addColumn("Statistic/Singlets$SL$SLive$S3+$S4+$S(IFNg+|IL2+):Freq_Of_Parent");
        _customizeViewsHelper.addColumn("Background/Singlets$SL$SLive$S3+$S4+$S(IFNg+|IL2+):Freq_Of_Parent");
        _customizeViewsHelper.addColumn("Statistic/Singlets$SL$SLive$S3+$S8+$S(IFNg+|IL2+):Freq_Of_Parent");
        _customizeViewsHelper.addColumn("Background/Singlets$SL$SLive$S3+$S8+$S(IFNg+|IL2+):Freq_Of_Parent");
        _customizeViewsHelper.addColumn("Graph/(FSC-H:FSC-A)");
        _customizeViewsHelper.addColumn("Graph/Singlets(SSC-A:FSC-A)");
        _customizeViewsHelper.addColumn("Graph/Singlets$SL$SLive$S3+(<PE Cy55-A>:<FITC-A>)");
        _customizeViewsHelper.saveCustomView();

        // check PTID value from sample set present
        assertTextPresent("P02034");

        // check no graph errors are present and the graphs have labels
        assertTextNotPresent("Error generating graph");
        String href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/") && href.contains("showGraph.view"));

        if (IS_BOOTSTRAP_LAYOUT)
        {
            new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("span","lk-menu-drop")
                    .withDescendant(Locator.id("PopupText").withText("Show Graphs")).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT))
                    .clickMenuButton(true, false, "Thumbnail");
        }else
        {
            _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='Show Graphs']"), "Thumbnail");
        }
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/") && href.contains("showGraph.view"));

        // UNDONE: assert background values are correctly calculated

        // check well details page for FCSFile has link to the sample
        clickAndWait(Locator.linkWithText("91779.fcs-" + FCS_FILE_1));
        clickAndWait(Locator.linkWithText("91779.fcs"));
        assertElementPresent(Locator.linkWithText(FCS_FILE_1 + "-C01"));
    }

    //Issue 16304: query over flow.FCSFiles doesn't copy include URL for Name column
    @LogMethod
    public void customGraphQuery()
    {
        log("** Creating custom query with Graph columns");
        String graphQuery =
                "SELECT FCSAnalyses.Name,\n" +
                "FCSAnalyses.Graph.\"(FSC-H:FSC-A)\" AS Singlets,\n" +
                "FCSAnalyses.Graph.\"(<APC-A>)\"\n" +
                "FROM FCSAnalyses";
        createQuery(getProjectName(), "GraphQuery", graphQuery, null, true);

        log("** Executing custom query with Graph columns");
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=GraphQuery&query.showGraphs=Inline");

        // verify Issue 16304: query over flow.FCSFiles doesn't include URL for Name column
        DataRegionTable table = new DataRegionTable("query", getDriver());
        String href = table.getHref(0, "Name");
        assertTrue("Expected 'Name' href to go to showWell.view: " + href, href.contains("/" + getFolderName() + "/") && href.contains("showWell.view"));
        
        assertTextNotPresent("Error generating graph");
        assertTextPresent("No graph for:", "(<APC-A>)");
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/") && href.contains("showGraph.view"));

        if (IS_BOOTSTRAP_LAYOUT)
        {
            new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("span","lk-menu-drop")
                    .withDescendant(Locator.id("PopupText").withText("Show Graphs")).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT))
                    .clickMenuButton(true, false, "Thumbnail");
        }else
        {
            _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='Show Graphs']"), "Thumbnail");
        }
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/") && href.contains("showGraph.view"));
    }

    @LogMethod
    public void positivityReportTest()
    {
        String reportName = TRICKY_CHARACTERS + " positivity report";
        String reportDescription = TRICKY_CHARACTERS + " positivity report description";

        createPositivityReport(reportName, reportDescription);
        executeReport(reportName);
        //Issue 12400: Script exception in flow report if all data has been filtered out
        verifyReportError(reportName, "Error: labkey.data is empty");

        updatePositivityReportFilter(reportName);
        executeReport(reportName);
        verifyReport(reportName);

        deleteReport(reportName);
        verifyDeleted(reportName);
    }

    @LogMethod
    public void qcReportTest()
    {
        String reportName = "QC report " + TRICKY_CHARACTERS;

        log("** Creating QC report '" + reportName + "'");
        goToFlowDashboard();

        final QCReportEditorPage qcReport = new FlowReportsWebpart(getDriver()).createQCReport();

        qcReport.setName(reportName);
        qcReport.setSubset("Singlets/L/Live/3+/4+/(IFNg+|IL2+)");
        qcReport.setStatistic(QCReportEditorPage.Stat.Freq_Of_Parent);

        qcReport.addFieldFilter("Name", "Contains", "L02");

        qcReport.save();

        clickAndWait(Locator.linkWithText(reportName));
        final int expectedRows = 15;

        assertEquals("Wrong number of rows in TSV output", expectedRows + 1, Locator.css(".labkey-r-tsvout tr").findElements(getDriver()).size());
        assertEquals("Found links to filtered out run: " + FCS_FILE_2, 0, Locator.linkContainingText("L04").findElements(getDriver()).size());
    }

    @LogMethod
    public void copyAnalysisScriptTest()
    {
        // bug 4625
        log("** Check analysis script copy must have unique name");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText(QUV_ANALYSIS_NAME));
        clickAndWait(Locator.linkWithText("Make a copy of this analysis script"));
        setFormElement(Locator.name("name"), QUV_ANALYSIS_NAME);
        clickAndWait(Locator.tagWithAttribute("input", "value", "Make Copy"));
        assertTextPresent("There is already a protocol named 'QUV analysis'");
    }

    @LogMethod
    private void removeAnalysisFilter()
    {
        clickTab("Flow Dashboard");
        clickAndWait(Locator.linkContainingText("Other settings"));
        clickAndWait(Locator.linkContainingText("Edit FCS Analysis Filter"));
        selectOptionByValue(Locator.xpath("//select[@name='ff_field']").index(0), "");
        clickButton("Set filter");
    }

    /**
     * With our new feature, a user doesn't have to individually pick out FCS files, we will
     * find files that match their workspace and suggest the user include them (final curating
     * to be done by the user).  This tests that feature
     */
    @LogMethod
    private void verifyDiscoverableFCSFiles()
    {
        clickFolder(getFolderName());

        importAnalysis_begin( getContainerPath());
        importAnalysis_uploadWorkspace(getContainerPath(), "/8color/workspace.xml");
        click(Locator.radioButtonById("Previous"));
        clickButton("Next");

        assertTextPresent("Matched 0 of 59 samples.");

        DataRegionTable samplesConfirm = new DataRegionTable("SamplesConfirm", getDriver());
        Locator.css(".labkey-selectors > input[type=checkbox][value]") // Can't use helper. Grid doesn't fire row selection events
                .findElement(samplesConfirm.getComponentElement())
                .click();
        WebElement matchedFileInput = samplesConfirm.findCell(0, "MatchedFile").findElement(By.cssSelector("select"));
        selectOptionByText(matchedFileInput,"91745.fcs (L02-060120-QUV-JS)" );
        clickButton("Next");
        waitForText("Import Analysis: Analysis Engine");
        clickButton("Next");
        waitForText("Import Analysis: Analysis Folder");
        clickButton("Next");
        waitForText("Import Analysis: Confirm");
        clickButton("Finish");
        waitForElement(Locators.labkeyError.containing("Ignoring filter/sort"), defaultWaitForPage);
        DataRegionTable query = new DataRegionTable("query", getDriver());
        List<String> names = query.getColumnDataAsText("Name");
        assertEquals("Wrong name for data row", Arrays.asList("88436.fcs-050112-8ColorQualitative-ET"), names);
    }

    @LogMethod
    private void verifyExperimentRunGraphLinks()
    {
        clickFolder(getFolderName());
        goToSchemaBrowser();
        selectQuery("exp", "Runs");
        waitForText("view data");
        clickAndWait(Locator.linkWithText("view data"));
        Locator.XPathLocator graphLinkLoc = Locator.tagWithAttribute("a", "title", "Experiment run graph");
        int linkCount = graphLinkLoc.findElements(getDriver()).size();
        for (int i = 0; i < linkCount; i++)
        {
            pushLocation();
            clickAndWait(graphLinkLoc.index(i));
            assertTextPresent("Click on a node in the graph below for details.");
            assertTextNotPresent("Error");
            popLocation();
        }
    }

    @LogMethod
    private void createPositivityReport(String reportName, String description)
    {
        log("** Creating positivity report '" + reportName + "'");
        goToFlowDashboard();

        final ReportEditorPage positivityReport = new FlowReportsWebpart(getDriver()).createPositivityReport();

        positivityReport.setName(reportName);
        positivityReport.setDescription(description);
        positivityReport.setSubset("Singlets/L/Live/3+/4+/(IFNg+|IL2+)");

        // NOTE: this filter is set high so we filter out all of the data and produce an error message.
        positivityReport.addStatisticFilter("Singlets/L/Live/3+/4+", "Count", "Is Greater Than or Equal To", "5000");

        positivityReport.save();
    }

    @LogMethod(quiet = true)
    private void updatePositivityReportFilter(String reportName)
    {
        log("** Updating positivity report filter '" + reportName + "'");
        goToFlowDashboard();

        // Should only be one 'manage' menu since we've only created one flow report.
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='manage']"), "Edit");
        setFormElement(Locator.name("filter[2].value"), "100");
        clickButton("Save");
    }

    @LogMethod(quiet = true)
    private void executeReport(String reportName)
    {
        log("** Executing positivity report '" + reportName + "'");
        goToFlowDashboard();

        clickAndWait(Locator.linkWithText(reportName));
        clickButton("Execute Report");
        waitForPipeline(getContainerPath());
    }

    @LogMethod
    private void verifyReportError(@LoggedParam String reportName, String errorText)
    {
        waitForPipeline("/" + getProjectName() + "/" + getFolderName());
        goToFlowDashboard();
        waitForPipeline("/" + getProjectName() + "/" + getFolderName());
        clickAndWait(Locator.linkContainingText("Show Jobs"));
        clickAndWait(Locator.linkWithText("ERROR"));

        assertTitleContains(reportName);
        assertTextPresent(errorText);
        checkExpectedErrors(2);
    }

    @LogMethod
    private void verifyReport(@LoggedParam String reportName)
    {
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");

        String reportNameEscaped = EscapeUtil.fieldKeyEncodePart(reportName);

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(new String[] { reportNameEscaped, "Raw P" });
        _customizeViewsHelper.addColumn(new String[] { reportNameEscaped, "Adjusted P"});
        _customizeViewsHelper.addColumn(new String[] { reportNameEscaped, "Response"});
        _customizeViewsHelper.addFilter(new String[] { reportNameEscaped, "Response"}, "Response", "Equals", "1");
        _customizeViewsHelper.addSort("Name", SortDirection.ASC);
        _customizeViewsHelper.saveCustomView();

        DataRegionTable table = new DataRegionTable("query", getDriver());
        assertEquals(4, table.getDataRowCount());
        assertEquals("91926.fcs-" + FCS_FILE_1, table.getDataAsText(0, "Name"));
    }

    @LogMethod(quiet = true)
    private void deleteReport(@LoggedParam String reportName)
    {
        goToFlowDashboard();

        // Should only be one 'manage' menu since we've only created one flow report.
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='manage']"), "Delete");
        clickButton("OK");
    }

    @LogMethod(quiet = true)
    private void verifyDeleted(@LoggedParam String reportName)
    {
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        DataRegionTable drt = new DataRegionTable("query", getDriver());
        String error = Locators.labkeyError.findElement(drt.getComponentElement()).getText();
        assertEquals("Ignoring filter/sort on column '" + reportName + ".Response' because it does not exist.", error);
    }

    private void clickButtonWithText(String text)
    {
        click(Locator.xpath("//input[@value = '" + text + "']"));
    }

    public int countEnabledInputs(String name)
    {
        List<WebElement> inputs = Locator.xpath("//input[@name='" + name + "']").findElements(getDriver());
        int ret = 0;
        for (WebElement el : inputs)
        {
            if (el.isEnabled())
                ret ++;
        }
        return ret;
    }
}
