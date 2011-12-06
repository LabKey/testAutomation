/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

import org.labkey.test.BaseFlowTest;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class FlowTest extends BaseFlowTest
{
    public static final String SELECT_CHECKBOX_NAME = ".select";
    private static final String QUV_ANALYSIS_SCRIPT = "/sampledata/flow/8color/quv-analysis.xml";
    private static final String JOIN_FIELD_LINK_TEXT = "Define sample description join fields";
    String containerPath = "/" + PROJECT_NAME + "/" + getFolderName();
    private static final String FCS_FILE_1 = "L02-060120-QUV-JS";
    private static final String FCS_FILE_2 = "L04-060120-QUV-JS";

    private void clickButtonWithText(String text)
    {
        click(Locator.raw("//input[@value = '" + text + "']"));
    }

    protected String getProjectName()
    {
        return PROJECT_NAME;
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
    protected void init()
    {
        // fail fast if R is not configured
        // R is needed for the positivity report
        RReportHelper.ensureRConfig(this);

        super.init();
    }

    protected void _doTestSteps()
    {
        containerPath = "/" + PROJECT_NAME + "/" + getFolderName();
        setupQuery();

        importFiles();

        analysisFilterTest();

        configureSampleSetAndMetadata();

        sampleSetAndMetadataTest();

        positivityReportTest();

        copyAnalysisScriptTest();
    }

    protected void setupQuery()
    {
        beginAt("/query" + containerPath + "/begin.view?schemaName=flow");
        createNewQuery("flow");
        setFormElement(Locator.nameOrId("ff_newQueryName"), "DRTQuery1");
        selectOptionByText("identifier=ff_baseTableName", "FCSAnalyses");
        clickButton("Create and Edit Source");

        // Start Query Editing
        setQueryEditorValue("queryText", "SELECT FCSAnalyses.RowId, " +
                "FCSAnalyses.Statistic.\"Count\", " +
                "FCSAnalyses.Run.FilePathRoot, " +
                "FCSAnalyses.FCSFile.Run.WellCount " +
                "FROM FCSAnalyses AS FCSAnalyses");
        clickButton("Save", 0);
        waitForText("Saved", WAIT_FOR_JAVASCRIPT);

        clickButton("Execute Query", 0);
        waitForText("No data to show.", WAIT_FOR_JAVASCRIPT);
    }

    protected void importFiles()
    {
        goToFlowDashboard();
        clickLinkWithText("Browse for FCS files to be imported");

        // Should allow for import all directories containing FCS Files
        ExtHelper.selectFileBrowserItem(this, "8color/");
        ExtHelper.waitForImportDataEnabled(this);
        waitForElement(ExtHelper.locateBrowserFileCheckbox(FCS_FILE_2), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Import Directory of FCS Files");
        assertTextPresent("The following directories within '8color'");
        assertTextPresent(FCS_FILE_1 + " (25 fcs files)");
        assertTextPresent(FCS_FILE_2 + " (14 fcs files)");
        clickNavButton("Cancel"); // go back to file-browser

        // Entering L02-060120-QUV-JS directory should allow import of current directory
        waitForPageToLoad();
        ExtHelper.selectFileBrowserItem(this, "8color/" + FCS_FILE_1 + "/");
        waitForElement(ExtHelper.locateBrowserFileCheckbox("91761.fcs"), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Current directory of 25 FCS Files");
        assertTextPresent("The following directories within '8color" + File.separator + FCS_FILE_1 + "'");
        assertTextPresent("Current Directory (25 fcs files)");
        assertTextNotPresent(FCS_FILE_2);
        clickNavButton("Import Selected Runs");
        waitForPipeline(containerPath);
        clickLinkWithText("Flow Dashboard");
        // Drill into the run, and see that it was uploaded, and keywords were read.
        clickLinkWithText("1 run");
        setSelectedFields(containerPath, "flow", "Runs", null, new String[] { "Flag","Name","ProtocolStep","AnalysisScript","CompensationMatrix","WellCount","Created","RowId","FilePathRoot" } );

        clickLinkWithText("details");
        setSelectedFields(containerPath, "flow", "FCSFiles", null, new String[] { "Keyword/ExperimentName", "Keyword/Stim","Keyword/Comp","Keyword/PLATE NAME","Flag","Name","RowId"});
        assertTextPresent("PerCP-Cy5.5 CD8");

        assertLinkNotPresentWithImage("/flagFCSFile.gif");
        pushLocation();
        clickLinkWithText("91761.fcs");
        //assertTextPresent(FCS_FILE_1); // "experiment name" keyword

        clickNavButton("edit");
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
        /*
        clickLinkWithText("changes");
        assertTextPresent("FlowTest Keyword Plate Name");
        assertTextPresent("FlowTest Comment");
        assertTextPresent(oldPlateNameValue);*/

        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Create a new Analysis script");
        setFormElement("ff_name", "FlowTestAnalysis");
        submit();

        clickLinkWithText("Define compensation calculation from scratch");
        selectOptionByText("selectedRunId", FCS_FILE_1);
        submit();

        selectOptionByText("identifier=positiveKeywordName[3]", "Comp");
        selectOptionByText("identifier=positiveKeywordValue[3]", "FITC CD4");
        submit();
        assertTextPresent("Missing data");
        selectOptionByText("identifier=negativeKeywordName[0]", "WELL ID");
        selectOptionByText("identifier=negativeKeywordValue[0]", "H01");
        clickButtonWithText("Universal");
        submit();
        assertTextPresent("compensation calculation may be edited in a number");

        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Create a new Analysis script");
        setFormElement("ff_name", "QUV analysis");
        submit();
        clickLinkWithText("View Source");
        setLongTextField("script", this.getFileContents(QUV_ANALYSIS_SCRIPT));
        submit();
    }

    protected void analysisFilterTest()
    {
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Other settings");
        clickLinkWithText("Edit FCS Analysis Filter");
        selectOptionByValue(Locator.xpath("//select[@name='ff_field']").index(0),  "Keyword/Stim");
        selectOptionByValue(Locator.xpath("//select[@name='ff_op']").index(0),  "isnonblank");
        selectOptionByValue(Locator.xpath("//select[@name='ff_op']").index(1),  "eq");
        selectOptionByValue(Locator.xpath("//select[@name='ff_op']").index(2),  "eq");
        submit();

        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("QUV analysis");
        clickLinkWithText("Analyze some runs");

        checkCheckbox(".toggle");
        clickNavButton("Analyze selected runs");
        setFormElement("ff_analysisName", "FlowExperiment2");
        submit();
        waitForPipeline(containerPath);
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("FlowExperiment2");
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
        clickLinkWithText("details");

        setSelectedFields(containerPath, "flow", "CompensationControls", null, new String[] {"Name","Flag","Created","Run","FCSFile","RowId"});

        assertLinkPresentWithText("PE Green laser-A+");
        assertLinkNotPresentWithText("91918.fcs");
        clickLinkWithText("PE Green laser-A+");
        pushLocation();
        clickLinkWithText("Keywords from the FCS file");
        assertTextPresent("PE CD8");
        popLocation();
        clickLinkWithText("FlowExperiment2");
        beginAt(urlAnalysis.getFile());


        clickLinkWithText("details");

        clickLinkWithText("91918.fcs");
        clickLinkWithText("More Graphs");
        selectOptionByText("subset", "Singlets/L/Live/3+/4+");
        selectOptionByText("xaxis", "comp-PE Cy7-A IFNg");
        selectOptionByText("yaxis", "comp-PE Green laser-A IL2");
        submit(Locator.dom("document.forms['chooseGraph']")); // UNDONE: v

        // change the name of an analysis
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Other settings");
        clickLinkWithText("Change FCS Analyses Names");
        selectOptionByValue(Locator.xpath("//select[@name='ff_keyword']").index(1), "Keyword/EXPERIMENT NAME");
        submit();

        beginAt(urlAnalysis.getFile());
        clickLinkWithText("details");
        clickLinkWithText("91918.fcs-" + FCS_FILE_1);
        assertTextPresent("91918.fcs-" + FCS_FILE_1);


        // Now, let's add another run:
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Browse for more FCS files to be imported");

        ExtHelper.selectFileBrowserItem(this, "8color/");
        ExtHelper.waitForImportDataEnabled(this);
        waitForElement(ExtHelper.locateBrowserFileCheckbox(FCS_FILE_2), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Import Directory of FCS Files");
        assertTextNotPresent(FCS_FILE_1);
        assertTextPresent(FCS_FILE_2);
        clickNavButton("Import Selected Runs");
        waitForPipeline(containerPath);

        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("QUV analysis");
        clickLinkWithText("Analyze some runs");
        selectOptionByText("ff_targetExperimentId", "<create new>");
        waitForPageToLoad();
        assertEquals(2, countEnabledInputs(SELECT_CHECKBOX_NAME));
        selectOptionByText("ff_targetExperimentId", "FlowExperiment2");
        waitForPageToLoad();

        assertEquals(1, countEnabledInputs(SELECT_CHECKBOX_NAME));
        selectOptionByText("ff_compensationMatrixOption", "Matrix: " + FCS_FILE_1 + " comp matrix");
        waitForPageToLoad();

        checkCheckbox(".toggle");
        clickNavButton("Analyze selected runs");
        waitForPipeline(containerPath);

        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("FlowExperiment2");
        clickMenuButton("Query", "DRTQuery1");
        assertTextPresent("File Path Root");

        setSelectedFields(containerPath, "flow", "DRTQuery1", "MostColumns", new String[] {"RowId", "Count","WellCount"});
        setSelectedFields(containerPath, "flow", "DRTQuery1", "AllColumns", new String[] {"RowId", "Count","WellCount", "FilePathRoot"});
        clickMenuButton("Views", "MostColumns");
        assertTextNotPresent("File Path Root");
        clickMenuButton("Views", "AllColumns");
        assertTextPresent("File Path Root");
    }

    // Test sample set and ICS metadata
    protected void configureSampleSetAndMetadata()
    {
        // upload sample set
        goToFlowDashboard();
        clickLinkWithText("Upload Sample Descriptions");
        setFormElement("data", getFileContents("/sampledata/flow/8color/sample-set.tsv"));
        selectOptionByText("idColumn1", "Exp Name");
        selectOptionByText("idColumn2", "Well Id");
        submit();

        // join with FCSFile keywords
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Define sample description join fields");
        selectOptionByText(Locator.name("ff_samplePropertyURI", 0), "Exp Name");
        selectOptionByText(Locator.name("ff_samplePropertyURI", 1), "Well Id");
        selectOptionByText(Locator.name("ff_dataField", 0), "EXPERIMENT NAME");
        selectOptionByText(Locator.name("ff_dataField", 1), "WELL ID");
        submit();
        assertTextPresent("39 FCS files were linked to samples in this sample set.");

        // add ICS metadata
        clickLinkWithText("Protocol 'Flow'");
        clickLinkWithText("Edit ICS Metadata");

        // specify PTID and Visit columns
        selectOptionByText("ff_participantColumn", "Sample PTID");
        selectOptionByText("ff_visitColumn", "Sample Visit");

        // specify forground-background match columns
        assertFormElementEquals(Locator.name("ff_matchColumn", 0), "Run");
        selectOptionByText(Locator.name("ff_matchColumn", 1), "Sample Sample Order");

        // specify background values
        selectOptionByText(Locator.name("ff_backgroundFilterField", 0), "Sample Stim");
        assertFormElementEquals(Locator.name("ff_backgroundFilterOp", 0), "eq");
        setFormElement(Locator.name("ff_backgroundFilterValue", 0), "Neg Cont");
        submit();
    }

    public void sampleSetAndMetadataTest()
    {
        // verify sample set and background values can be displayed in the FCSAnalysis grid
        goToFlowDashboard();
        clickLinkWithText("29 FCS files");
        clickLinkWithText("Show Graphs");
//            sleep(3000);
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Background/Count");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Background/Singlets:Count");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Background/Singlets:Freq_Of_Parent");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Background/Singlets$SL:Count");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Graph/(<APC-A>)");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Graph/(<Alexa 680-A>)");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Graph/(<FITC-A>)");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Graph/(<PE Cy55-A>)");

        CustomizeViewsHelper.addCustomizeViewColumn(this, "FCSFile/Sample/PTID");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "FCSFile/Sample/Visit");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "FCSFile/Sample/Stim");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Statistic/Singlets$SL$SLive$S3+$S4+$S(IFNg+|IL2+):Freq_Of_Parent");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Background/Singlets$SL$SLive$S3+$S4+$S(IFNg+|IL2+):Freq_Of_Parent");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Statistic/Singlets$SL$SLive$S3+$S8+$S(IFNg+|IL2+):Freq_Of_Parent");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Background/Singlets$SL$SLive$S3+$S8+$S(IFNg+|IL2+):Freq_Of_Parent");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Graph/(FSC-H:FSC-A)");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Graph/Singlets(SSC-A:FSC-A)");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Graph/Singlets$SL$SLive$S3+(<PE Cy55-A>:<FITC-A>)");
        CustomizeViewsHelper.saveCustomView(this);

        // check PTID value from sample set present
        assertTextPresent("P02034");

        // UNDONE: assert background values are correctly calculated

        // check well details page for FCSFile has link to the sample
        clickLinkWithText("91779.fcs-" + FCS_FILE_1);
        clickLinkWithText("91779.fcs");
        assertLinkPresentWithText(FCS_FILE_1 + "-C01");
    }

    public void copyAnalysisScriptTest()
    {
        // bug 4625
        goToFlowDashboard();
        clickLinkWithText("QUV analysis");
        clickLinkWithText("Make a copy of this analysis script");
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
        goToFlowDashboard();
        addWebPart("Flow Reports");

        clickLinkWithText("create positivity report");

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
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@name='filter[4].property_stat']]"), "Count");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@name='filter[4].op']]"), "Is Greater Than or Equal To");

        // NOTE: this filter is set high so we filter out all of the data and produce an error message.
        setFormElement("filter[4].value", "5000");

        clickButton("Save");
    }

    private void updatePositivityReportFilter(String reportName)
    {
        goToFlowDashboard();

        // Should only be one 'manage' menu since we've only created one flow report.
        ExtHelper.clickExtMenuButton(this, true, Locator.xpath("//a/span[text()='manage']"), "Edit");
        setFormElement("filter[4].value", "100");
        clickButton("Save");
    }

    private void executeReport(String reportName)
    {
        goToFlowDashboard();

        clickLinkWithText(reportName);
        clickButton("Execute Report");
        waitForPipeline(containerPath);
    }

    private void verifyReportError(String reportName, String errorText)
    {
        goToFlowDashboard();
        clickLinkContainingText("Show Jobs");
        clickLinkWithText("ERROR");

        assertTitleContains(reportName);
        assertTextPresent(errorText);
        checkExpectedErrors(2);
    }

    private void verifyReport(String reportName)
    {
        beginAt("/flow" + containerPath + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");

        // HACK: need FieldKey.encodePart() in the test module
        String reportNameEscaped = "><$A$S%\\' \"1 positivity report";

        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, new String[] { reportNameEscaped, "Raw P" });
        CustomizeViewsHelper.addCustomizeViewColumn(this, new String[] { reportNameEscaped, "Adjusted P"});
        CustomizeViewsHelper.addCustomizeViewColumn(this, new String[] { reportNameEscaped, "Response"});
        CustomizeViewsHelper.addCustomizeViewFilter(this, new String[] { reportNameEscaped, "Response"}, "Response", "Equals", "1");
        CustomizeViewsHelper.addCustomizeViewSort(this, "Name", "Ascending");
        CustomizeViewsHelper.saveCustomView(this);

        DataRegionTable table = new DataRegionTable("query", this, false);
        assertEquals(4, table.getDataRowCount());
        assertEquals("91926.fcs-" + FCS_FILE_1, table.getDataAsText(0, "Name"));
    }

    private void deleteReport(String reportName)
    {
        goToFlowDashboard();

        // Should only be one 'manage' menu since we've only created one flow report.
        ExtHelper.clickExtMenuButton(this, true, Locator.xpath("//a/span[text()='manage']"), "Delete");
        clickButton("OK");
    }

    private void verifyDeleted(String reportName)
    {
        beginAt("/flow" + containerPath + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        assertTextPresent("Ignoring filter/sort on column '" + reportName + ".Response' because it does not exist.");
    }

}
