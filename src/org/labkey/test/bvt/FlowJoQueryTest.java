package org.labkey.test.bvt;

import org.labkey.test.BaseFlowTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * This test tests uploading a FlowJo workspace that has results calculated in it, but no associated FCS files.
 * It then runs two queries on those results.
 * Then, it uploads another workspace that is in the same directory as 68 FCS files.  These FCS files are much smaller
 * than normal, having been truncated to 1000 events.
 * It then uses LabKey to perform the same analysis on those FCS files.
 * It then runs a query 'Comparison' to ensure than the difference between LabKey's results and FlowJo's is not greater
 * than 25 for any statistic.
 */
public class FlowJoQueryTest extends BaseFlowTest
{
    protected void doTestSteps()
    {
        init();
        clickLinkWithText("Set pipeline root");
        setFormElement("path", getLabKeyRoot() + PIPELINE_PATH);
        submit();
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Upload FlowJo Workspace");
        clickImageWithAltText("Browse the pipeline");
        clickLinkWithText("root");
        clickLinkWithText("flowjoquery");
        clickLinkWithText("Workspaces");
        checkCheckbox("file", "flowjoquery" + File.separator + "Workspaces" + File.separator + "PV1-public.xml", true);
        clickButtonWithImgSrc("Upload%20Workspace");
        setFormElement("ff_newAnalysisName", "FlowJoAnalysis");
        clickButtonWithImgSrc("Upload%20Results", longWaitForPage);
        clickLinkWithText("Customize View");
        clearCustomizeViewColumns();
        addCustomizeViewColumn("Name");
        addCustomizeViewColumn("AnalysisScript", "Analysis Script");
        click(Locator.raw("expand_FCSFile"));
        click(Locator.raw("expand_FCSFile/Keyword"));
        addCustomizeViewColumn("FCSFile/Keyword/Stim", "Stim");
        addCustomizeViewColumn("FCSFile/Keyword/Sample Order", "Sample Order");
        click(Locator.raw("expand_Statistic"));
        addCustomizeViewColumn("Statistic/S$SLv$SL$S3+$S4+:Count", "4+:Count");
        addCustomizeViewColumn("Statistic/S$SLv$SL$S3+$S8+:Count", "8+:Count");
        clickNavButton("Save");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Customize Folder");
        toggleCheckboxByTitle("Query", false);
        toggleCheckboxByTitle("Flow", false);
        createQuery(PROJECT_NAME, "PassFailDetails", getFileContents("/sampledata/flow/flowjoquery/query/PassFailDetails.sql"), getFileContents("/sampledata/flow/flowjoquery/query/PassFailDetails.xml"), true);
        createQuery(PROJECT_NAME, "PassFail", getFileContents("/sampledata/flow/flowjoquery/query/PassFail.sql"), getFileContents("/sampledata/flow/flowjoquery/query/PassFail.xml"), true);
        createQuery(PROJECT_NAME, "DeviationFromMean", getFileContents("/sampledata/flow/flowjoquery/query/DeviationFromMean.sql"), getFileContents("/sampledata/flow/flowjoquery/query/DeviationFromMean.xml"), true);
        createQuery(PROJECT_NAME, "COMP", getFileContents("sampledata/flow/flowjoquery/query/COMP.sql"), "", true);
        createQuery(PROJECT_NAME, "Comparison", getFileContents("sampledata/flow/flowjoquery/query/Comparison.sql"), "", true);
        clickLinkWithText("flowFolder");
        clickLinkWithText("1 run");
        setFormElement("query.queryName", "PassFail");
        waitForPageToLoad();
        assertTextPresent("LO_CD8", 1);
        assertTextPresent("PASS", 4);
//        The DeviationFromMean query does not work on SQL server.
//        "Cannot perform an aggregate function on an expression containing an aggregate or a subquery."
//        setFormElement("query.queryName", "DeviationFromMean");
//        waitForPageToLoad();

        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Upload FlowJo Workspace");
        clickImageWithAltText("Browse the pipeline");
        clickLinkWithText("root");
        clickLinkWithText("flowjoquery");
        clickLinkWithText("miniFCS");
        checkCheckbox("file", "flowjoquery" + File.separator + "miniFCS" + File.separator + "mini-fcs.xml", true);
        clickButtonWithImgSrc("Upload%20Workspace");
        clickButtonWithImgSrc("Upload%20Results", longWaitForPage);
        clickLinkWithText("workspaceScript1");
        clickLinkWithText("Make a copy of this analysis script");
        setFormElement("name", "LabKeyScript");
        checkCheckbox("copyAnalysis");
        submit();
        clickLinkWithText("Analyze some runs");
        setFormElement("ff_targetExperimentId", "");
        waitForPageToLoad();
        checkCheckbox(".toggle");
        clickButtonWithImgSrc("Analyze%20selected%20runs");
        setFormElement("ff_analysisName", "LabKeyAnalysis");
        clickButtonWithImgSrc("Analyze%20runs");
        waitForPipeline("/" + PROJECT_NAME + "/" + FOLDER_NAME);
        clickLinkWithText("Flow Dashboard");
        clickLinkWithText("LabKeyAnalysis");
        setFormElement("query.queryName", "Comparison");
        waitForPageToLoad(longWaitForPage);
        assertTextNotPresent("No data to show");
        setFilterAndWait("query", "AbsDifference", "Is Greater Than or Equal To", "25", longWaitForPage);
        assertTextPresent("No data to show");
    }
}
