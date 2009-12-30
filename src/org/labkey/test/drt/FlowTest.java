/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ExtHelper;

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
        submit();

        beginAt(WebTestHelper.getContextPath() + "/query/" + PROJECT_NAME + "/" + getFolderName() + "/sourceQuery.view?schemaName=flow&query.queryName=DRTQuery1");
        setFormElement("ff_queryText", "SELECT FCSAnalyses.RowId,\n" +
                "FCSAnalyses.Statistic.\"Count\",\n" +
                "FCSAnalyses.Run.FilePathRoot,\n" +
                "FCSAnalyses.FCSFile.Run.WellCount\n" +
                "FROM FCSAnalyses AS FCSAnalyses");
        clickNavButton("View Data");

        clickLinkWithText("Flow Dashboard");
        setFlowPipelineRoot(getLabKeyRoot() + PIPELINE_PATH);
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Browse for FCS files to be loaded");

        waitAndClick(Locator.fileTreeByName("8color"));
        ExtHelper.selectFileBrowserFile(this, "quv-analysis.xml");
        waitAndClickNavButton("Import Multiple Runs");
        // First, just upload the run "8colordata"
        clickNavButton("Clear All", -1); // no nav
        checkCheckbox("ff_path", "8color/8colordata/");
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
        selectOptionByText("selectedRunId", "8colordata");
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
        submit(Locator.dom("document.forms[1]"));

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
        clickLinkWithText("Browse for more FCS files to be loaded");

        waitAndClick(Locator.fileTreeByName("8color"));
        ExtHelper.selectFileBrowserFile(this, "quv-analysis.xml");
        waitAndClickNavButton("Import Multiple Runs");
        assertTextNotPresent("8colordata");
        clickImgButtonNoNav("Select All");
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
        selectOptionByText("ff_compensationMatrixOption", "Matrix: 8colordata comp matrix");
        waitForPageToLoad();

        checkCheckbox(".toggle");
        clickNavButton("Analyze selected runs");
        waitForPipeline(containerPath);

        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("FlowExperiment2");
        clickMenuButton("Query", "Query:DRTQuery1");
        waitForPageToLoad();
        assertTextPresent("File Path Root");

        setSelectedFields(containerPath, "flow", "DRTQuery1", "MostColumns", new String[] {"RowId", "Count","WellCount"});
        setSelectedFields(containerPath, "flow", "DRTQuery1", "AllColumns", new String[] {"RowId", "Count","WellCount", "FilePathRoot"});
        //setWorkingForm("view");
        clickMenuButton("Views", "Views:MostColumns");
        waitForPageToLoad();
        assertTextNotPresent("File Path Root");
        //setWorkingForm("view");
        clickMenuButton("Views", "Views:AllColumns");
        waitForPageToLoad();
        assertTextPresent("File Path Root");

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
        selectOptionByText("ff_dataField", "EXPERIMENT NAME");
        selectOptionByText("ff_dataField", "WELL ID");

        // bug 4625
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("QUV analysis");
        clickLinkWithText("Make a copy of this analysis script");
        setFormElement("name", "QUV analysis");
        submit();
        assertTextPresent("There is already a protocol named 'QUV analysis'");        
    }
}
