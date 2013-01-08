/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.labkey.test.BaseFlowTestWD;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.RReportHelperWD;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class FlowTest extends BaseFlowTestWD

{
    public static final String SELECT_CHECKBOX_NAME = ".select";
    private static final String QUV_ANALYSIS_SCRIPT = "/sampledata/flow/8color/quv-analysis.xml";
    private static final String JOIN_FIELD_LINK_TEXT = "Define sample description join fields";
    private static final String FCS_FILE_1 = "L02-060120-QUV-JS";
    private static final String FCS_FILE_2 = "L04-060120-QUV-JS";

    private void clickButtonWithText(String text)
    {
        click(Locator.xpath("//input[@value = '" + text + "']"));
    }

    public int countEnabledInputs(String name)
    {
        List<Locator> inputs = findAllMatches(Locator.xpath("//input[@name='" + name + "']"));
        int ret = 0;
        for (Locator l : inputs)
        {
            if (!selenium.isEditable(l.toString()))
                continue;
            ret ++;
        }
        return ret;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @Override
    protected void init()
    {
        // fail fast if R is not configured
        // R is needed for the positivity report
        RReportHelperWD _rReportHelper = new RReportHelperWD(this);
        _rReportHelper.ensureRConfig();

        super.init();
    }

    protected void _doTestSteps()
    {
        _doTestStepsSetDepth(false);
    }
    protected void _doTestStepsSetDepth(boolean quickTest)
    {
        setupQuery();

        importFiles();

        analysisFilterTest();

        if(!quickTest)
        {
            configureSampleSetAndMetadata();

            sampleSetAndMetadataTest();

            customGraphQuery();

            positivityReportTest();

            copyAnalysisScriptTest();

            removeAnalysisFilter();
            
            verifyDiscoverableFCSFiles();


        }

    }

    private void removeAnalysisFilter()
    {
        clickTab("Flow Dashboard");
        clickAndWait(Locator.linkContainingText("Other settings"));
        clickAndWait(Locator.linkContainingText("Edit FCS Analysis Filter"));
        selectOptionByValue(Locator.xpath("//select[@name='ff_field']").index(0),  "");
        clickButton("Set filter");

    }

    /**
     * With our new feature, a user doesn't have to individually pick out FCS files, we will
     * find files that match their workspace and suggest the user include them (final curating
     * to be done by the user).  This tests that feature
     */
    private void verifyDiscoverableFCSFiles()     //TODO
    {
        clickFolder(getFolderName());


        importAnalysis_begin( getContainerPath());
        importAnalysis_uploadWorkspace(getContainerPath(), "/8color/workspace.xml");
        clickRadioButtonById("Previous");
        clickButton("Next");

        assertTextPresent("Matched 0 of 59 samples.");

        //TODO:  how many to select?
        selectOptionByText(Locator.name("selectedSamples.rows[0.0.1].matchedFile"),"91745.fcs (L02-060120-QUV-JS)" );
        clickCheckbox("selectedSamples.rows[0.0.1].selected");
        clickButton("Next");
        waitForText("Import Analysis: Analysis Engine");
        clickButton("Next");
        waitForText("Import Analysis: Analysis Folder");
        clickButton("Next");
        waitForText("Import Analysis: Confirm");
        clickButton("Finish", 0);
        sleep(15000);
        waitForText("Ignoring filter");
        assertTextPresent("88436.fcs-050112-8ColorQualitative-ET");
    }

    String query1 =  TRICKY_CHARACTERS_NO_QUOTES + "DRTQuery1";
    String analysisName = "FCSAnalyses";
    protected void setupQuery()
    {
        beginAt("/query" + getContainerPath() + "/begin.view?schemaName=flow");
        createNewQuery("flow");
        setFormElement(Locator.name("ff_newQueryName"), query1);
        selectOptionByText(Locator.name("ff_baseTableName"), analysisName);
        clickButton("Create and Edit Source");

        // Start Query Editing
        setQueryEditorValue("queryText", "SELECT " + analysisName + ".RowId, " +
                analysisName + ".Statistic.\"Count\", " +
                analysisName + ".Run.FilePathRoot, " +
                analysisName + ".FCSFile.Run.WellCount " +
                "FROM " + analysisName + " AS " + analysisName);
        clickButton("Save", 0);
        waitForText("Saved", WAIT_FOR_JAVASCRIPT);

        clickButton("Execute Query", 0);
        waitForText("No data to show.", WAIT_FOR_JAVASCRIPT);
    }

    protected void importFiles()
    {
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Browse for FCS files to be imported"));

        // Should allow for import all directories containing FCS Files
        _extHelper.selectFileBrowserItem("8color/");
        _extHelper.waitForImportDataEnabled();
        waitForElement(_extHelper.locateGridRowCheckbox(FCS_FILE_2), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Import Directory of FCS Files");
        assertTextPresent("The following directories within",  "8color");
        assertTextPresent(FCS_FILE_1 + " (25 fcs files)");
        assertTextPresent(FCS_FILE_2 + " (14 fcs files)");
        clickButton("Cancel"); // go back to file-browser

        // Entering L02-060120-QUV-JS directory should allow import of current directory
        waitForPageToLoad();
        _extHelper.selectFileBrowserItem("8color/" + FCS_FILE_1 + "/");
        waitForElement(_extHelper.locateGridRowCheckbox("91761.fcs"), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Current directory of 25 FCS Files");
        assertTextPresent("The following directories within" ,"'8color",  FCS_FILE_1);
        assertTextPresent("Current Directory (25 fcs files)");
        assertTextNotPresent(FCS_FILE_2);
        clickButton("Import Selected Runs");
        waitForPipeline(getContainerPath());
        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        // Drill into the run, and see that it was uploaded, and keywords were read.
        clickAndWait(Locator.linkWithText("1 run"));
        setSelectedFields(getContainerPath(), "flow", "Runs", null, new String[] { "Flag","Name","ProtocolStep","AnalysisScript","CompensationMatrix","WellCount","Created","RowId","FilePathRoot" } );

        clickAndWait(Locator.linkWithText("details"));
        setSelectedFields(getContainerPath(), "flow", "FCSFiles", null, new String[] { "Keyword/ExperimentName", "Keyword/Stim","Keyword/Comp","Keyword/PLATE NAME","Flag","Name","RowId"});
        assertTextPresent("PerCP-Cy5.5 CD8");

        assertLinkNotPresentWithImage("/flagFCSFile.gif");
        pushLocation();
        clickAndWait(Locator.linkWithText("91761.fcs"));
        assertTextPresent(FCS_FILE_1); // "experiment name" keyword

        clickButton("edit");
        setFormElement("ff_name", "FlowTest New Name");
        setFormElement("ff_comment", "FlowTest Comment");
        Locator locPlateName = Locator.xpath("//td/input[@type='hidden' and @value='PLATE NAME']/../../td/input[@name='ff_keywordValue']");
        setFormElement(locPlateName, "FlowTest Keyword Plate Name");
        submit();
        popLocation();
        assertLinkPresentWithImage("/flagFCSFile.gif");
        assertLinkNotPresentWithText("91761.fcs");
        assertLinkPresentWithText("FlowTest New Name");
        assertTextPresent("FlowTest Keyword Plate Name");

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("Create a new Analysis script"));
        setFormElement("ff_name", "FlowTestAnalysis");
        submit();

        clickAndWait(Locator.linkWithText("Define compensation calculation from scratch"));
        selectOptionByText("selectedRunId", FCS_FILE_1);
        submit();

        selectOptionByText(Locator.name("positiveKeywordName[3]"), "Comp");
        selectOptionByText(Locator.name("positiveKeywordValue[3]"), "FITC CD4");
        submit();
        assertTextPresent("Missing data");
        selectOptionByText(Locator.name("negativeKeywordName[0]"), "WELL ID");
        selectOptionByText(Locator.name("negativeKeywordValue[0]"), "H01");
        clickButtonWithText("Universal");
        submit();
        assertTextPresent("compensation calculation may be edited in a number");

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("Create a new Analysis script"));
        setFormElement("ff_name", "QUV analysis");
        submit();
        clickAndWait(Locator.linkWithText("View Source"));
        setLongTextField("script", this.getFileContents(QUV_ANALYSIS_SCRIPT));
        submit();
    }

    protected void analysisFilterTest()
    {
        setFlowFilter(new String[] {"Keyword/Stim"}, new String[] {"isnonblank"}, new String[] {""});

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("QUV analysis"));
        clickAndWait(Locator.linkWithText("Analyze some runs"));

        checkCheckbox(".toggle");
        clickButton("Analyze selected runs");
        setFormElement("ff_analysisName", "FlowExperiment2");
        submit();
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
        clickAndWait(Locator.linkWithText("details"));

        setSelectedFields(getContainerPath(), "flow", "CompensationControls", null, new String[] {"Name","Flag","Created","Run","FCSFile","RowId"});

        assertLinkPresentWithText("PE Green laser-A+");
        assertLinkNotPresentWithText("91918.fcs");
        clickAndWait(Locator.linkWithText("PE Green laser-A+"));
        pushLocation();
        clickAndWait(Locator.linkWithText("Keywords from the FCS file"));
        assertTextPresent("PE CD8");
        popLocation();
        clickAndWait(Locator.linkWithText("FlowExperiment2"));
        beginAt(urlAnalysis.getFile());


        clickAndWait(Locator.linkWithText("details"));

        clickAndWait(Locator.linkWithText("91918.fcs"));
        clickAndWait(Locator.linkWithText("More Graphs"));
        selectOptionByText("subset", "Singlets/L/Live/3+/4+");
        selectOptionByText("xaxis", "comp-PE Cy7-A IFNg");
        selectOptionByText("yaxis", "comp-PE Green laser-A IL2");
        clickButtonWithText("Show Graph");

        // change the name of an analysis
        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("Other settings"));
        clickAndWait(Locator.linkWithText("Change FCS Analyses Names"));
        selectOptionByValue(Locator.xpath("//select[@name='ff_keyword']").index(1), "Keyword/EXPERIMENT NAME");
        submit();

        beginAt(urlAnalysis.getFile());
        clickAndWait(Locator.linkWithText("details"));
        clickAndWait(Locator.linkWithText("91918.fcs-" + FCS_FILE_1));
        assertTextPresent("91918.fcs-" + FCS_FILE_1);


        // Now, let's add another run:
         clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("Browse for more FCS files to be imported"));

        _extHelper.selectFileBrowserItem("8color/");
        _extHelper.waitForImportDataEnabled();
        waitForElement(_extHelper.locateGridRowCheckbox(FCS_FILE_2), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Import Directory of FCS Files");
        assertTextNotPresent(FCS_FILE_1);
        assertTextPresent(FCS_FILE_2);
        clickButton("Import Selected Runs");
        waitForPipeline(getContainerPath());

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("QUV analysis"));
        click(Locator.linkWithText("Analyze some runs"));
        final Locator.NameLocator ff_targetExperimentId = Locator.name("ff_targetExperimentId");
        waitForElement(ff_targetExperimentId);
        selectOptionByText(ff_targetExperimentId, "<create new>");
//        waitForPageToLoad();
        Assert.assertEquals(2, countEnabledInputs(SELECT_CHECKBOX_NAME));
        selectOptionByText(ff_targetExperimentId, "FlowExperiment2");
//        waitForPageToLoad();

        Assert.assertEquals(1, countEnabledInputs(SELECT_CHECKBOX_NAME));
        selectOptionByText("ff_compensationMatrixOption", "Matrix: " + FCS_FILE_1 + " comp matrix");
        waitForPageToLoad();

        checkCheckbox(".toggle");
        clickButton("Analyze selected runs");
        waitForPipeline(getContainerPath());

        clickAndWait(Locator.linkWithText("Flow Dashboard"));
        clickAndWait(Locator.linkWithText("FlowExperiment2"));
        clickMenuButton("Query", query1);
        assertTextPresent("File Path Root");

        setSelectedFields(getContainerPath(), "flow", query1, "MostColumns", new String[] {"RowId", "Count","WellCount"});
        setSelectedFields(getContainerPath(), "flow", query1, "AllColumns", new String[] {"RowId", "Count","WellCount", "FilePathRoot"});
        clickMenuButton("Views", "MostColumns");
        assertTextNotPresent("File Path Root");
        clickMenuButton("Views", "AllColumns");
        assertTextPresent("File Path Root");
    }

    // Test sample set and ICS metadata
    protected void configureSampleSetAndMetadata()
    {
        uploadSampleDescriptions("/sampledata/flow/8color/sample-set.tsv", new String[] { "Exp Name", "Well Id" }, new String[] { "EXPERIMENT NAME", "WELL ID"});
        setProtocolMetadata("Sample PTID", null, "Sample Visit", true);

        goToFlowDashboard();
        clickAndWait(Locator.linkContainingText("49 sample descriptions"));
        assertTextPresent("49 sample descriptions");
        assertTextPresent("10 samples are not joined");
        assertTextPresent("39 FCS Files", "have been joined");
        assertTextPresent("0 FCS Files", "are not joined");
    }

    public void sampleSetAndMetadataTest()
    {
        // verify sample set and background values can be displayed in the FCSAnalysis grid
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("29 FCS files"));
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='Show Graphs']"), "Inline");
//            sleep(3000);
        assertTextNotPresent("Error generating graph");
        assertTextPresent("No graph for:", "(<APC-A>)");
        windowMaximize();

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewColumn("Background/Count");
        _customizeViewsHelper.removeCustomizeViewColumn("Background/Singlets:Count");
        _customizeViewsHelper.removeCustomizeViewColumn("Background/Singlets:Freq_Of_Parent");
        _customizeViewsHelper.removeCustomizeViewColumn("Background/Singlets$SL:Count");
        _customizeViewsHelper.removeCustomizeViewColumn("Graph/(<APC-A>)");
        _customizeViewsHelper.removeCustomizeViewColumn("Graph/(<Alexa 680-A>)");
        _customizeViewsHelper.removeCustomizeViewColumn("Graph/(<FITC-A>)");
        _customizeViewsHelper.removeCustomizeViewColumn("Graph/(<PE Cy55-A>)");

        _customizeViewsHelper.addCustomizeViewColumn("FCSFile/Sample/PTID");
        _customizeViewsHelper.addCustomizeViewColumn("FCSFile/Sample/Visit");
        _customizeViewsHelper.addCustomizeViewColumn("FCSFile/Sample/Stim");
        _customizeViewsHelper.addCustomizeViewColumn("Statistic/Singlets$SL$SLive$S3+$S4+$S(IFNg+|IL2+):Freq_Of_Parent");
        _customizeViewsHelper.addCustomizeViewColumn("Background/Singlets$SL$SLive$S3+$S4+$S(IFNg+|IL2+):Freq_Of_Parent");
        _customizeViewsHelper.addCustomizeViewColumn("Statistic/Singlets$SL$SLive$S3+$S8+$S(IFNg+|IL2+):Freq_Of_Parent");
        _customizeViewsHelper.addCustomizeViewColumn("Background/Singlets$SL$SLive$S3+$S8+$S(IFNg+|IL2+):Freq_Of_Parent");
        _customizeViewsHelper.addCustomizeViewColumn("Graph/(FSC-H:FSC-A)");
        _customizeViewsHelper.addCustomizeViewColumn("Graph/Singlets(SSC-A:FSC-A)");
        _customizeViewsHelper.addCustomizeViewColumn("Graph/Singlets$SL$SLive$S3+(<PE Cy55-A>:<FITC-A>)");
        _customizeViewsHelper.saveCustomView();

        // check PTID value from sample set present
        assertTextPresent("P02034");

        // check no graph errors are present and the graphs have labels
        assertTextNotPresent("Error generating graph");
        String href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        Assert.assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/showGraph.view"));

        _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='Show Graphs']"), "Thumbnail");
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        Assert.assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/showGraph.view"));

        // UNDONE: assert background values are correctly calculated

        // check well details page for FCSFile has link to the sample
        clickAndWait(Locator.linkWithText("91779.fcs-" + FCS_FILE_1));
        clickAndWait(Locator.linkWithText("91779.fcs"));
        assertLinkPresentWithText(FCS_FILE_1 + "-C01");
    }

    //Issue 16304: query over flow.FCSFiles doesn't copy include URL for Name column
    public void customGraphQuery()
    {
        log("** Creating custom query with Graph columns");
        String graphQuery =
                "SELECT FCSAnalyses.Name,\n" +
                "FCSAnalyses.Graph.\"(FSC-H:FSC-A)\" AS Singlets,\n" +
                "FCSAnalyses.Graph.\"(<APC-A>)\"\n" +
                "FROM FCSAnalyses";
        createQuery(PROJECT_NAME, "GraphQuery", graphQuery, null, true);

        log("** Executing custom query with Graph columns");
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=GraphQuery&query.showGraphs=Inline");

        // verify Issue 16304: query over flow.FCSFiles doesn't include URL for Name column
        DataRegionTable table = new DataRegionTable("query", this);
        String href = table.getHref(0, "Name");
        Assert.assertTrue("Expected 'Name' href to go to showWell.view: " + href, href.contains("/" + getFolderName() + "/showWell.view"));
        
        assertTextNotPresent("Error generating graph");
        assertTextPresent("No graph for:", "(<APC-A>)");
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        Assert.assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/showGraph.view"));

        _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='Show Graphs']"), "Thumbnail");
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        Assert.assertTrue("Expected graph img: " + href, href.contains("/" + getFolderName() + "/showGraph.view"));
    }

    public void copyAnalysisScriptTest()
    {
        // bug 4625
        log("** Check analysis script copy must have unique name");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("QUV analysis"));
        clickAndWait(Locator.linkWithText("Make a copy of this analysis script"));
        setFormElement("name", "QUV analysis");
        submit();
        assertTextPresent("There is already a protocol named 'QUV analysis'");
    }


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

    private void createPositivityReport(String reportName, String description)
    {
        log("** Creating positivity report '" + reportName + "'");
        goToFlowDashboard();
        addWebPart("Flow Reports");

        clickAndWait(Locator.linkWithText("create positivity report"));

        setFormElement("reportName", reportName);
        setFormElement("reportDescription", description);

        Locator l = Locator.name("subset");
        click(l);
        //selenium.typeKeys(l.toString(), "Singlets/L/Live/3+/4+/(IFNg+|IL2+)");
        setFormElement("subset", "Singlets/L/Live/3+/4+/(IFNg+|IL2+)");

        // click on TriggerField trigger image
        click(Locator.xpath("//input[@name='filter[4].property_subset']/../img"));
        // Selenium XPath doesn't support attribute namespaces.
        Locator CD4 = Locator.xpath("//div[contains(@class, 'x-tree-node-el') and @*='Singlets/L/Live/3+/4+']");
        waitForElement(CD4, WAIT_FOR_JAVASCRIPT);
        click(CD4);
        selenium.fireEvent(Locator.name("filter[4].property_subset").toString(), "blur");

//        Issue 12630: add stat/threshold fo TCell filter to positivity report
        _extHelper.selectComboBoxItem(Locator.xpath("//div[./input[@name='filter[4].property_stat']]"), "Count");
        _extHelper.selectComboBoxItem(Locator.xpath("//div[./input[@name='filter[4].op']]"), "Is Greater Than or Equal To");

        // NOTE: this filter is set high so we filter out all of the data and produce an error message.
        setFormElement("filter[4].value", "5000");

        clickButton("Save");
    }

    private void updatePositivityReportFilter(String reportName)
    {
        log("** Updating positivity report filter '" + reportName + "'");
        goToFlowDashboard();

        // Should only be one 'manage' menu since we've only created one flow report.
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='manage']"), "Edit");
        setFormElement("filter[4].value", "100");
        clickButton("Save");
    }

    private void executeReport(String reportName)
    {
        log("** Executing positivity report '" + reportName + "'");
        goToFlowDashboard();

        clickAndWait(Locator.linkWithText(reportName));
        clickButton("Execute Report");
        waitForPipeline(getContainerPath());
    }

    private void verifyReportError(String reportName, String errorText)
    {
        log("** Checking for expected error in report '" + reportName + "'");
        waitForPipeline("/" + getProjectName() + "/" + getFolderName());
        goToFlowDashboard();
        waitForPipeline("/" + getProjectName() + "/" + getFolderName());
        clickAndWait(Locator.linkContainingText("Show Jobs"));
        clickAndWait(Locator.linkWithText("ERROR"));

        assertTitleContains(reportName);
        assertTextPresent(errorText);
        checkExpectedErrors(2);
    }

    private void verifyReport(String reportName)
    {
        log("** Verifying positivity report '" + reportName + "'");
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");

        String reportNameEscaped = EscapeUtil.fieldKeyEncodePart(reportName);

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { reportNameEscaped, "Raw P" });
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { reportNameEscaped, "Adjusted P"});
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { reportNameEscaped, "Response"});
        _customizeViewsHelper.addCustomizeViewFilter(new String[] { reportNameEscaped, "Response"}, "Response", "Equals", "1");
        _customizeViewsHelper.addCustomizeViewSort("Name", "Ascending");
        _customizeViewsHelper.saveCustomView();

        DataRegionTable table = new DataRegionTable("query", this, false);
        Assert.assertEquals(4, table.getDataRowCount());
        Assert.assertEquals("91926.fcs-" + FCS_FILE_1, table.getDataAsText(0, "Name"));
    }

    private void deleteReport(String reportName)
    {
        log("** Deleting positivity report '" + reportName + "'");
        goToFlowDashboard();

        // Should only be one 'manage' menu since we've only created one flow report.
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a/span[text()='manage']"), "Delete");
        clickButton("OK");
    }

    private void verifyDeleted(String reportName)
    {
        log("** Verifying positivity report was deleted '" + reportName + "'");
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        waitForText("Ignoring filter/sort on column");
        assertTextPresent("Ignoring filter/sort on column '" , reportName , ".Response' because it does not exist.");
    }

}
