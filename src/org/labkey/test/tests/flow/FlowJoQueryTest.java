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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ExecuteSqlCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Flow;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test tests uploading a FlowJo workspace that has results calculated in it, but no associated FCS files.
 * It then runs two queries on those results.
 * Then, it uploads another workspace that is in the same directory as 68 FCS files.  These FCS files are much smaller
 * than normal, having been truncated to 1000 events.
 * It then uses LabKey to perform the same analysis on those FCS files.
 * It then runs a query 'Comparison' to ensure than the difference between LabKey's results and FlowJo's is not greater
 * than 25 for any statistic.
 */
@Category({Daily.class, Flow.class})
@BaseWebDriverTest.ClassTimeout(minutes = 15)
public class FlowJoQueryTest extends BaseFlowTest
{
    @Test
    public void _doTestSteps()
    {
        verifyQueryTest();

        verifyTableMethods();

        verifyNestedBooleans();

        verifyWSPImport();

        verifyFilterOnImport();
    }

    protected void verifyQueryTest()
    {
        importAnalysis(getContainerPath(), "/flowjoquery/Workspaces/PV1-public.xml", SelectFCSFileOption.None, null, "FlowJoAnalysis", false, true);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.clearColumns();
        _customizeViewsHelper.addColumn("Name");
        _customizeViewsHelper.addColumn("AnalysisScript", "Analysis Script");
        _customizeViewsHelper.addColumn("FCSFile/Keyword/Comp", "Comp");
        _customizeViewsHelper.addColumn("FCSFile/Keyword/Stim", "Stim");
        _customizeViewsHelper.addColumn("FCSFile/Keyword/Sample Order", "Sample Order");
        _customizeViewsHelper.addColumn("Statistic/S$SLv$SL$S3+$S4+:Count", "4+:Count");
        _customizeViewsHelper.addColumn("Statistic/S$SLv$SL$S3+$S8+:Count", "8+:Count");
        _customizeViewsHelper.applyCustomView();

        clickProject(getProjectName());
        _containerHelper.enableModules(Arrays.asList("Query", "Flow"));

        File sampledataDir = TestFileUtils.getSampleData("flow/flowjoquery/query");
        createQuery(getProjectName(), "PassFailDetails", TestFileUtils.getFileContents(new File(sampledataDir, "PassFailDetails.sql")), TestFileUtils.getFileContents(new File(sampledataDir, "PassFailDetails.xml")), true);
        createQuery(getProjectName(), "PassFail", TestFileUtils.getFileContents(new File(sampledataDir, "PassFail.sql")), TestFileUtils.getFileContents(new File(sampledataDir, "PassFail.xml")), true);
        //createQuery(getProjectName(), "DeviationFromMean", getFileContents(new File(sampledataDir, "DeviationFromMean.sql")), getFileContents(new File(sampledataDir, "DeviationFromMean.xml")), true);
        createQuery(getProjectName(), "COMP", TestFileUtils.getFileContents(new File(sampledataDir, "COMP.sql")), TestFileUtils.getFileContents(new File(sampledataDir, "COMP.xml")), true);
        createQuery(getProjectName(), "Comparison", TestFileUtils.getFileContents(new File(sampledataDir, "Comparison.sql")), TestFileUtils.getFileContents(new File(sampledataDir, "Comparison.xml")), true);

        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("1 run"));
        new DataRegionTable("query", getDriver()).clickHeaderMenu("Query", "PassFail");

        assertTextPresent("LO_CD8", 1);
        assertTextPresent("PASS", 4);

        goToFlowDashboard();
        importAnalysis(getContainerPath(), "/flowjoquery/miniFCS/mini-fcs.xml", SelectFCSFileOption.Browse, Arrays.asList("/flowjoquery/miniFCS"), "FlowJoAnalysis", true, false);

        int runId = -1;
        String currentURL = getCurrentRelativeURL();
        Pattern p = Pattern.compile(".*runId=([0-9]+).*$");
        Matcher m = p.matcher(currentURL);
        if (m.matches())
        {
            String runIdStr = m.group(1);
            runId = Integer.parseInt(runIdStr);
            log("mini-fcs.xml runId = " + runId);
        }
        else
        {
            fail("Failed to match runId pattern for url: " + currentURL);
        }
        assertTrue("Failed to find runId of mini-fcs.xml run", runId > 0);

        // Copy the generated 'workspaceScript1' from one of the sample wells (not one of the comp wells)
        (new DataRegionTable("query", this)).setFilter("Name", "Equals", "118795.fcs");
        clickAndWait(Locator.linkContainingText("workspaceScript"));
        clickAndWait(Locator.linkWithText("Make a copy of this analysis script"));
        setFormElement(Locator.name("name"), "LabKeyScript");
        checkCheckbox(Locator.name("copyAnalysis"));
        submit(Locator.tag("form").withAttributeContaining("action", "flow-editscript-copy.view"));    //

        // Only run LabKeyScript on sample wells
        // NOTE: we use 'Contains' since it is case-insensitive. Some values are Non-comp and other are Non-Comp.
        clickAndWait(Locator.linkWithText("Edit Settings"));
        selectOptionByText(Locator.name("ff_filter_field"), "Comp");
        selectOptionByText(Locator.name("ff_filter_op"), "Contains");
        setFormElement(Locator.name("ff_filter_value"), "non-comp"); clickButton("Update");

        clickAndWait(Locator.linkWithText("Analyze some runs"));
        doAndWaitForPageToLoad(() -> selectOptionByValue(Locator.name("ff_targetExperimentId"), ""));
        // select mini-fcs.xml Analysis run
        checkCheckbox(Locator.checkboxByNameAndValue(".select", String.valueOf(runId)));
        clickButton("Analyze selected runs");
        setFormElement(Locator.name("ff_analysisName"), "LabKeyAnalysis");
        clickButton("Analyze runs");
        waitForPipelineComplete();
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("LabKeyAnalysis"));
        new DataRegionTable("query", getDriver()).clickHeaderMenu("Query", "Comparison");
        // Custom queries are filtered by analysis folder (Issue 18332)
        assertTextPresent("No data to show");

        new DataRegionTable("query", getDriver()).clickHeaderMenu("Analysis Folder", "All Analysis Folders");
        assertTextNotPresent("No data to show");
        DataRegionTable region = new DataRegionTable("query", this);
        region.setFilter("AbsDifference", "Is Greater Than or Equal To", "2", longWaitForPage);
        region.setFilter("PercentDifference", "Is Greater Than or Equal To", "2.5", longWaitForPage);

        // NOTE: see https://github.com/LabKey/commonAssays/pull/476
        // As part of this PR there were various places we removed range constraints on data values to improve plot consistency.
        // This causes our analysis to be _less_ consistent.  However, as no one uses this feature (all customers use
        // external flowjo analysis), this is not a priority.  The follow on fix is probably to use constraints only
        // when loading FCS data and not after compensation.
        // assertTextPresent("No data to show");
    }

    @LogMethod
    private void verifyTableMethods()
    {
        String sql = "SELECT\n" +
                "  cm.Name,\n" +
                "  cm.Value('Spill(APC-A:Alexa 680-A)') AS E1\n" +
                "FROM CompensationMatrices cm";
        ExecuteSqlCommand cmd = new ExecuteSqlCommand("flow", sql);

        Connection conn = createDefaultConnection();
        SelectRowsResponse r;
        try
        {
            r = cmd.execute(conn, getContainerPath());
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        assertTrue("Expected some rows", r.getRowCount().intValue() > 0);
    }

    private void verifyWSPImport()
    {
        importAnalysis(getContainerPath(), "/advanced/advanced-v7.6.5.wsp", SelectFCSFileOption.Browse, Arrays.asList("/advanced"), "Windows File", false, true);
        assertTextPresent("931115-B02- Sample 01.fcs");
    }

    private void verifyNestedBooleans()
    {
        //verify workspaces with booleans-within-booleans
        importAnalysis(getContainerPath(), "/flowjoquery/Workspaces/boolean-sub-populations.xml", SelectFCSFileOption.Previous, Arrays.asList("miniFCS"), "BooleanOfBooleanAnalysis", false, true);
        clickAndWait(Locator.linkWithText("118795.fcs"));
        sleep(2000);
        waitForElement(Locator.xpath("//table/tbody/tr/td/a/span[text()='A&B']"), defaultWaitForPage);
        assertElementPresent(Locator.xpath("//tbody/tr/td/a/span[text()='C|D']"));
    }


    private void verifyFilterOnImport()
    {
        setFlowFilter(new String[] {"Name", "Keyword/Comp"}, new String[] { "startswith","eq"}, new String[] {"118", "PE CD8"});
        importAnalysis(getContainerPath(), "/flowjoquery/miniFCS/mini-fcs.xml", SelectFCSFileOption.Previous, Arrays.asList("miniFCS"), "FilterAnalysis", false, true);
        DataRegionTable queryTable = new DataRegionTable("query", this);
        assertEquals(1, queryTable.getDataRowCount());
    }

}
