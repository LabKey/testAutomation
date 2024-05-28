/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Flow;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.flow.FlowReportsWebpart;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.flow.reports.QCReportEditorPage;
import org.labkey.test.pages.flow.reports.ReportEditorPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.tests.AuditLogTest;
import org.labkey.test.util.DataRegion;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Flow.class})
@BaseWebDriverTest.ClassTimeout(minutes = 18)
public class FlowTest extends BaseFlowTest
{
    public static final String SELECT_CHECKBOX_NAME = ".select";
    private static final File QUV_ANALYSIS_SCRIPT = TestFileUtils.getSampleData("flow/8color/quv-analysis.xml");
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
        RReportHelper _rReportHelper = new RReportHelper(this);
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
        configureSampleTypeAndMetadata();
        sampleTypeAndMetadataTest();
        customGraphQuery();
        positivityReportTest();
        qcReportTest();
        copyAnalysisScriptTest();
        removeAnalysisFilter();
        verifyDiscoverableFCSFiles();
        verifyExperimentRunGraphLinks();
        testBulkKeywordEdit();
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

    public void testBulkKeywordEdit()
    {
        String filename1 = "91745.fcs";
        String filename2 = "91747.fcs";

        goToProjectHome();
        beginAtFCSFileQueryView();

        DataRegionTable result = new DataRegionTable("query", getDriver());
        // confirm the edit keywords button is present and disabled
        WebElement button = result.getHeaderButton("Edit Keywords");
        assertTrue("Edit Keywords button is not disabled",
                button.getAttribute("class").contains("labkey-disabled-button"));
        // select a couple rows, then click the edit keywords button
        result.checkCheckbox(result.getRowIndex("Name", filename1));
        result.checkCheckbox(result.getRowIndex("Name", filename2));
        result = new DataRegionTable("query", getDriver());
        WebElement editKeywordsButton = result.getHeaderButton("Edit Keywords");
        editKeywordsButton.click();
        assertEquals("Expected ","Selected Files:", getTableCellText(Locator.id("keywordTable"),2,0 ));
        assertEquals("Expected ", new HashSet<>(Arrays.asList(filename1, filename2)), new HashSet<>(Arrays.asList(getTableCellText(Locator.id("keywordTable"),2,1 ).split("[ ,]+"))));

        Locator locTubeName = Locator.xpath("//td/input[@type='hidden' and @value='TUBE NAME']/../../td/input[@name='ff_keywordValue']");
        setFormElement(locTubeName, "FlowTest Keyword Tube Name");
        clickButton("update");

        clickAndWait(Locator.linkWithText(filename1));
        assertTextPresent(FCS_FILE_1); // "experiment name" keyword

        clickButton("edit");
        confirmKeywordValue("TUBE NAME", "FlowTest Keyword Tube Name");

        beginAtFCSFileQueryView();

        clickAndWait(Locator.linkWithText(filename2));
        assertTextPresent(FCS_FILE_1); // "experiment name" keyword

        clickButton("edit");
        confirmKeywordValue("TUBE NAME", "FlowTest Keyword Tube Name");

        click(Locator.tagWithClassContaining("a", "add-new-keyword"));

        Locator locNewName = Locator.xpath("//td/input[@type='text' and @value='']");
        String newKeywordName = "new keyword name for 91747";
        String newKeywordValue = "new keyword value for 91747";
        setFormElement(locNewName, newKeywordName);
        Locator locNewValue = Locator.xpath("//td/input[@type='text' and @value='']/../../td/input[@name='ff_keywordValue']");
        setFormElement(locNewValue, newKeywordValue);
        clickButton("update");
        waitForText("FCS File '91747.fcs'");

        String keywordName = newKeywordName ;
        String expectedKeywordValue = newKeywordValue;

        clickButton("edit");
        confirmKeywordValue(keywordName, expectedKeywordValue);

        //Test validation of creating keyword without name
        newKeywordValue="No name";
        beginAtFCSFileQueryView();
        result.checkCheckbox(result.getRowIndex("Name", filename1));
        result.checkCheckbox(result.getRowIndex("Name", filename2));
        editKeywordsButton = result.getHeaderButton("Edit Keywords");
        editKeywordsButton.click();
        click(Locator.linkWithText("Create a new keyword"));
        locNewValue = Locator.xpath("//td/input[@type='text' and @value='' and @name='ff_keywordName']/../../td/input[@name='ff_keywordValue']");
        setFormElement(locNewValue, newKeywordValue);
        clickButton("update");

        assertTextPresent("Missing name for value 'No name'");
        locNewName = Locator.xpath("//td/input[@type='text' and @value='' and @name='ff_keywordName']");
        String newKeywordTestName = "New Name Test";
        setFormElement(locNewName, newKeywordTestName);
        clickButton("update");
        assertTextPresent("FCSFiles");

        //Test validation of creating duplicate keyword
        newKeywordValue="No dup";
        beginAtFCSFileQueryView();
        result.checkCheckbox(result.getRowIndex("Name", filename1));
        result.checkCheckbox(result.getRowIndex("Name", filename2));
        editKeywordsButton = result.getHeaderButton("Edit Keywords");
        editKeywordsButton.click();

        Locator locNewNameTestValue = Locator.xpath("//td/input[@type='hidden' and @value='New Name Test' and @name='ff_keywordName']/../../td/input[@name='ff_keywordValue']");
        setFormElement(locNewNameTestValue, "original");

        click(Locator.linkWithText("Create a new keyword"));
        locNewValue = Locator.xpath("//td/input[@type='text' and @value='' and @name='ff_keywordName']/../../td/input[@name='ff_keywordValue']");
        setFormElement(locNewValue, newKeywordValue);
        locNewName = Locator.xpath("//td/input[@type='text' and @value='' and @name='ff_keywordName']");
        setFormElement(locNewName, newKeywordTestName);
        clickButton("update");

        locNewValue = Locator.xpath("//td/input[@type='text' and @value='New Name Test' and @name='ff_keywordName']");
        String newNameTestDup = "New Name Test Dup";
        setFormElement(locNewValue, newNameTestDup);
        clickButton("update");
        assertTextPresent("FCSFiles");

        //Confirm new keyword added to both files
        beginAtFCSFileQueryView();
        clickAndWait(Locator.linkWithText(filename1));
        assertTextPresent(FCS_FILE_1); // "experiment name" keyword

        clickButton("edit");
        confirmKeywordValue(newNameTestDup, "original");

        beginAtFCSFileQueryView();

        clickAndWait(Locator.linkWithText(filename2));
        assertTextPresent(FCS_FILE_1); // "experiment name" keyword

        clickButton("edit");
        confirmKeywordValue(newNameTestDup, "original");

        String flow_keyword_events = "Flow events";
        AuditLogTest.verifyAuditEvent(this, flow_keyword_events, "File", "91747.fcs", 10);
        AuditLogTest.verifyAuditEvent(this, flow_keyword_events, "OldValue", "Well_002", 10);
        AuditLogTest.verifyAuditEvent(this, flow_keyword_events, "NewValue", "FlowTest Keyword Tube Name", 10);

    }

    private void beginAtFCSFileQueryView()
    {
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSFiles");
    }

    private void confirmKeywordValue(String keywordName, String expectedKeywordValue)
    {
        String keywordValue =  getFormElement(Locator.xpath("//td/input[@type='hidden' and @value='" + keywordName +"']/../../td/input[@name='ff_keywordValue']"));
        assertEquals("Wrong keyword value", expectedKeywordValue, keywordValue);
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
        waitForPipelineComplete();
        goToFlowDashboard();

        // Drill into the run, and see that it was uploaded, and keywords were read.
        clickAndWait(Locator.linkWithText("1 run"));
        setSelectedFields(getContainerPath(), "flow", "Runs", null, new String[]{"Flag", "Name", "ProtocolStep", "AnalysisScript", "CompensationMatrix", "WellCount", "Created", "RowId", "FilePathRoot"});

        DataRegionTable queryTable = new DataRegionTable("query", getDriver());
        clickAndWait(queryTable.detailsLink(0));
        //clickAndWait(Locator.linkWithText("details"));
        setSelectedFields(getContainerPath(), "flow", "FCSFiles", null, new String[]{"Keyword/ExperimentName", "Keyword/Stim", "Keyword/Comp", "Keyword/PLATE NAME", "Flag", "Name", "RowId"});
        assertTextPresent("PerCP-Cy5.5 CD8");

        assertElementNotPresent(Locator.tagWithClass("i", "lk-flag-enabled"));
        pushLocation();
        clickAndWait(Locator.linkWithText("91761.fcs"));
        waitForText(FCS_FILE_1);
        assertTextPresent(FCS_FILE_1); // "experiment name" keyword

        clickButton("edit");
        setFormElement(Locator.name("ff_name"), "FlowTest New Name");
        setFormElement(Locator.name("ff_comment"), "FlowTest Comment");
        Locator locPlateName = Locator.xpath("//td/input[@type='hidden' and @value='PLATE NAME']/../../td/input[@name='ff_keywordValue']");
        setFormElement(locPlateName, "FlowTest Keyword Plate Name");
        clickButton("update");
        popLocation();
        assertElementPresent(Locator.tagWithClass("i", "lk-flag-enabled"));
        assertElementNotPresent(Locator.linkWithText("91761.fcs"));
        assertElementPresent(Locator.linkWithText("FlowTest New Name"));
        assertTextPresent("FlowTest Keyword Plate Name");

        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Create Analysis script"));
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

        goToFlowDashboard();
        clickAndWait(Locator.linkWithText(QUV_ANALYSIS_NAME));
        clickAndWait(Locator.linkWithText("Analyze some runs"));

        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickButton("Analyze selected runs");
        setFormElement(Locator.name("ff_analysisName"), "FlowExperiment2");
        clickButton("Analyze runs");
        waitForPipelineComplete();
        goToFlowDashboard();
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
        goToFlowDashboard();
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
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Browse for more FCS files to be imported"));

        _fileBrowserHelper.selectFileBrowserItem("8color/");
        waitForElement(FileBrowserHelper.Locators.gridRowCheckbox(FCS_FILE_2, false), WAIT_FOR_JAVASCRIPT);
        _fileBrowserHelper.selectImportDataAction("Import Directory of FCS Files");
        assertTextNotPresent(FCS_FILE_1);
        assertTextPresent(FCS_FILE_2);
        clickButton("Import Selected Runs");
        waitForPipelineComplete();

        goToFlowDashboard();
        clickAndWait(Locator.linkWithText(QUV_ANALYSIS_NAME));
        click(Locator.linkWithText("Analyze some runs"));
        final Locator ff_targetExperimentId = Locator.name("ff_targetExperimentId");
        waitForElement(ff_targetExperimentId);
        doAndWaitForPageToLoad(() -> selectOptionByText(ff_targetExperimentId, "<create new>"));
        assertEquals(2, countEnabledInputs(SELECT_CHECKBOX_NAME));
        doAndWaitForPageToLoad(() -> selectOptionByText(ff_targetExperimentId, "FlowExperiment2"));

        assertEquals(1, countEnabledInputs(SELECT_CHECKBOX_NAME));
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("ff_compensationMatrixOption"), "Matrix: " + FCS_FILE_1 + " comp matrix"));

        // Non-standard data-region, can't use `DataRegionTable` component. No enclosing 'lk-region-form'
        doAndWaitForPageSignal(() -> click(Locator.checkboxByName(".toggle")), DataRegion.UPDATE_SIGNAL);
        clickButton("Analyze selected runs");

        // Analyze should auto-navigate to 'flow-run-showRuns.view' after job finishes
        table = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").waitFor();
        table.clickHeaderMenu("Query", query1);
        assertTextPresent("File Path Root");

        setSelectedFields(getContainerPath(), "flow", query1, "MostColumns", new String[] {"RowId", "Count","WellCount"});
        setSelectedFields(getContainerPath(), "flow", query1, "AllColumns", new String[] {"RowId", "Count","WellCount", "FilePathRoot"});
        table.goToView("MostColumns");
        assertTextNotPresent("File Path Root");
        table.goToView("AllColumns");
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
        fcsAnalysisTable.createChart();

        // The new plot dialog shows all values from the grid that could be used in any aspect of a plot. So need to add these two values.
        expectedMeasures.add("Compensation Matrix");
        expectedMeasures.add("Run");
        ChartTypeDialog chartTypeDialog = new ChartTypeDialog(getDriver());
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Box);
        List<String> availableMeasures =  chartTypeDialog.getColumnList();
        assertEquals("Wrong measures in picker", new HashSet<>(expectedMeasures), new HashSet<>(availableMeasures));
        log("Validate that the values 'Compensation Matrix' and 'Run' cannot be assigned to the Y axis.");
        chartTypeDialog.clickColumnValue("Compensation Matrix");
        assertEquals("You should not be able to set 'Compensation Matrix' to the y-axis.", 0, chartTypeDialog.getYAxisValue().trim().length());
        chartTypeDialog.clickColumnValue("Run");
        assertEquals("You should not be able to set 'Run' to the y-axis.", 0, chartTypeDialog.getYAxisValue().trim().length());
        doAndWaitForPageToLoad(chartTypeDialog::clickCancel);
    }

    // Test sample type and ICS metadata
    @LogMethod
    protected void configureSampleTypeAndMetadata()
    {
        Map<String, FieldDefinition.ColumnType> fields = new HashMap<>();
        fields.put("Exp Name", FieldDefinition.ColumnType.String);
        fields.put("Well Id", FieldDefinition.ColumnType.String);
        fields.put("Sample Order", FieldDefinition.ColumnType.Integer);
        fields.put("Sample", FieldDefinition.ColumnType.String);
        fields.put("PTID", FieldDefinition.ColumnType.String);
        fields.put("Visit", FieldDefinition.ColumnType.Integer);
        fields.put("Stim", FieldDefinition.ColumnType.String);
        fields.put("Comp", FieldDefinition.ColumnType.String);
        fields.put("Replicate", FieldDefinition.ColumnType.Integer);
        fields.put("Thaw Date", FieldDefinition.ColumnType.DateAndTime);
        fields.put("Comment", FieldDefinition.ColumnType.String);

        uploadSampleDescriptions(TestFileUtils.getSampleData("flow/8color/sample-set.tsv"), fields, new String[]{"Exp Name", "Well Id"}, new String[]{"EXPERIMENT NAME", "WELL ID"});
        setProtocolMetadata(null, "Sample PTID", null, "Sample Visit", true);

        goToFlowDashboard();
        clickAndWait(Locator.linkContainingText("49 sample descriptions"));

        assertElementContains(Locator.linkWithId("all-samples"), "49 sample descriptions");
        assertElementContains(Locator.linkWithId("linked-fcsfiles"), "39 FCS Files");
        assertElementContains(Locator.linkWithId("unlinked-samples"), "10 samples");
        assertElementContains(Locator.linkWithId("unlinked-fcsfiles"), "0 FCS Files");
    }

    @LogMethod
    public void sampleTypeAndMetadataTest()
    {
        // verify sample type and background values can be displayed in the FCSAnalysis grid
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("39 FCS files"));
        DataRegion dr = DataRegionTable.DataRegion(getDriver()).find();
        dr.clickHeaderMenu("Query", true, analysisName);

        new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("div","lk-menu-drop")
                    .withDescendant(Locator.tag("span").withText("Show Graphs")).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)).clickSubMenu(true, "Inline");
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

        // check PTID value from sample type present
        assertTextPresent("P02034");

        // check no graph errors are present and the graphs have labels
        assertTextNotPresent("Error generating graph");
        String href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/") && href.contains("showGraph.view"));

        new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("div","lk-menu-drop")
                .withDescendant(Locator.tag("span").withText("Show Graphs")).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT))
                .clickSubMenu(true, "Thumbnail");
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

        new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("div","lk-menu-drop")
                    .withDescendant(Locator.tag("span").withText("Show Graphs")).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)).clickSubMenu(true, "Thumbnail");
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/") && href.contains("showGraph.view"));
    }

    @LogMethod
    public void positivityReportTest()
    {
        String reportName = TRICKY_CHARACTERS + " positivity report";
        String reportDescription = TRICKY_CHARACTERS + " positivity report description";

        createPositivityReport(reportName, reportDescription);
        //Issue 12400: Script exception in flow report if all data has been filtered out
        verifyReportError(reportName, "Error: labkey.data is empty");

        updatePositivityReportFilter(reportName);
        executeReport(reportName);
        waitForPipelineComplete();
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
        WebElement reportView = Locator.id("report-view").findElement(getDriver());

        // wait for the ajax report to be rendered
        var tsvOutputLoc = Locator.tagWithClass("table", "labkey-r-tsvout");
        var tsvOutputTable = tsvOutputLoc.waitForElement(reportView, WAIT_FOR_PAGE);

        final int expectedRows = 15;
        assertEquals("Wrong number of rows in TSV output", expectedRows + 1, tsvOutputLoc.descendant("tr").findElements(reportView).size());
        assertEquals("Found links to filtered out run: " + FCS_FILE_2, 0, Locator.linkContainingText("L04").findElements(reportView).size());
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
        goToFlowDashboard();
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

        WebElement container = Locator.tagWithClassContaining("div", "lk-region-ct")
                .parent().findElement(getDriver());
        Locator.css(".labkey-selectors > input[type=checkbox][value]") // Can't use helper. Grid doesn't fire row selection events
                .findElement(container).click();
        WebElement matchedFileInput = Locator.xpath("//select[@name='selectedSamples.rows[0.0.1].matchedFile']").findElement(container);
        selectOptionByText(matchedFileInput,"91745.fcs (L02-060120-QUV-JS)");
        mashButton("Next");
        waitForText("Import Analysis: Analysis Folder");
        clickButton("Next");
        waitForText("Import Analysis: Confirm");
        clickButton("Finish");
        new PipelineStatusDetailsPage(getDriver()).waitForComplete();
        waitForElement(Locator.tagWithClass("div", "alert alert-warning").containing("Ignoring filter/sort"), defaultWaitForPage);
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
            assertTextNotPresent("Unable to display graph", "Error running dot");
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
        new BootstrapMenu(getDriver(), Locator.tagWithClass("div", "lk-menu-drop")
                .withChild(Locator.xpath("//a/span[text()='manage']")).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)).clickSubMenu(true, "Edit");
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
    }

    @LogMethod
    private void verifyReportError(@LoggedParam String reportName, String errorText)
    {
        executeReport(reportName);
        waitForPipelineError(List.of(errorText));

        assertTitleContains(reportName);
        assertTextPresent(errorText);
        checkExpectedErrors(1);

        // Subsequent tests expect there to be no ERRORs in pipeline. Delete errored pipeline job
        PipelineStatusTable pipelineStatusTable = PipelineStatusTable.viewJobsForContainer(this, getContainerPath());
        pipelineStatusTable.checkCheckbox(0);
        pipelineStatusTable.deleteSelectedRows();
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
        new BootstrapMenu(getDriver(), Locator.tagWithClass("div", "lk-menu-drop")
                    .withChild(Locator.xpath("//a/span[text()='manage']")).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)).clickSubMenu(true, "Delete");
        clickButton("OK");
    }

    @LogMethod(quiet = true)
    private void verifyDeleted(@LoggedParam String reportName)
    {
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        DataRegionTable drt = new DataRegionTable("query", getDriver());
        String error = BootstrapLocators.warningBanner.findElement(drt.getComponentElement()).getText();
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
