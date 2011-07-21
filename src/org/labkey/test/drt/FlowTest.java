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

package org.labkey.test.drt;

import org.labkey.test.BaseFlowTest;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class FlowTest extends BaseFlowTest
{
    public static final String SELECT_CHECKBOX_NAME = ".select";
    private static final String QUV_ANALYSIS_SCRIPT = "/sampledata/flow/8color/quv-analysis.xml";

    private void clickButtonWithText(String text)
    {
        click(Locator.raw("//input[@value = '" + text + "']"));
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

    protected void doTestSteps()
    {
        init();
        String containerPath = "/" + PROJECT_NAME + "/" + getFolderName();
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

        clickLinkWithText("Flow Dashboard");
        setFlowPipelineRoot(getLabKeyRoot() + PIPELINE_PATH);
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Browse for FCS files to be imported");

        // Should allow for import all directories containing FCS Files
        waitForPageToLoad();
        waitAndClick(Locator.fileTreeByName("8color"));
        ExtHelper.waitForImportDataEnabled(this);
        waitForElement(ExtHelper.locateBrowserFileCheckbox("L04-060120-QUV-JS"), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Import Directory of FCS Files");
        assertTextPresent("The following directories within '8color'");
        assertTextPresent("L02-060120-QUV-JS (25 fcs files)");
        assertTextPresent("L04-060120-QUV-JS (14 fcs files)");
        clickNavButton("Cancel"); // go back to file-browser

        // Entering L02-060120-QUV-JS directory should allow import of current directory
        waitForPageToLoad();
        waitAndClick(Locator.fileTreeByName("8color"));
        waitAndClick(Locator.fileTreeByName("L02-060120-QUV-JS"));
        waitForElement(ExtHelper.locateBrowserFileCheckbox("91761.fcs"), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Current directory of 25 FCS Files");
        assertTextPresent("The following directories within '8color" + File.separator + "L02-060120-QUV-JS'");
        assertTextPresent("Current Directory (25 fcs files)");
        assertTextNotPresent("L04-060120-QUV-JS");
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
        //assertTextPresent("L02-060120-QUV-JS"); // "experiment name" keyword

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
        selectOptionByText("selectedRunId", "L02-060120-QUV-JS");
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
        clickLinkWithText("91918.fcs-L02-060120-QUV-JS");
        assertTextPresent("91918.fcs-L02-060120-QUV-JS");


        // Now, let's add another run:
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Browse for more FCS files to be imported");

        waitForPageToLoad();
        waitAndClick(Locator.fileTreeByName("6color")); // try to avoid intermittent bug in file browser
        waitAndClick(Locator.fileTreeByName("8color"));
        ExtHelper.waitForImportDataEnabled(this);
        waitForElement(ExtHelper.locateBrowserFileCheckbox("L04-060120-QUV-JS"), WAIT_FOR_JAVASCRIPT);
        selectImportDataAction("Import Directory of FCS Files");
        assertTextNotPresent("L02-060120-QUV-JS");
        assertTextPresent("L04-060120-QUV-JS");
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
        selectOptionByText("ff_compensationMatrixOption", "Matrix: L02-060120-QUV-JS comp matrix");
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

        // Test sample set and ICS metadata
        {
            // upload sample set
            clickLinkWithText("Flow Dashboard");
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

            // verify sample set and background values can be displayed in the FCSAnalysis grid
            clickLinkWithText("Flow Dashboard");
            clickLinkWithText("29 FCS files");
            clickLinkWithText("Show Graphs");
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
            clickLinkWithText("91779.fcs-L02-060120-QUV-JS");
            clickLinkWithText("91779.fcs");
            assertLinkPresentWithText("L02-060120-QUV-JS-C01");
        }

        // bug 4625
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("QUV analysis");
        clickLinkWithText("Make a copy of this analysis script");
        setFormElement("name", "QUV analysis");
        submit();
        assertTextPresent("There is already a protocol named 'QUV analysis'");        
    }
}
