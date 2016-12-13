/*
 * Copyright (c) 2011-2016 LabKey Corporation
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

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Flow;
import org.labkey.test.util.RReportHelper;

import java.util.Arrays;
import java.util.Collections;

@Category({/*DailyA.class,*/ Flow.class})
public class FlowNormalizationTest extends BaseFlowTest
{
    @BeforeClass
    public static void initR()
    {
        // fail fast if R is not configured
        // R is needed for the positivity report
        RReportHelper _rReportHelper = new RReportHelper(getCurrentTest());
        String rVersion = _rReportHelper.ensureRConfig();

        Assume.assumeTrue("Wrong version of R: flowNormalization package require 2.15.x", rVersion.startsWith("2.15"));
    }

    @Override
    protected boolean requiresNormalization()
    {
        return true;
    }

    @Test
    public void _doTestSteps()
    {
        ImportAnalysisOptions options = new ImportAnalysisOptions(
                getContainerPath(),
                "/flowjoquery/miniFCS/mini-fcs.xml",
                SelectFCSFileOption.Browse,
                Arrays.asList("/flowjoquery/miniFCS"),
                Arrays.asList("Sample Order 5: Gag1&2"),
                null,
                AnalysisEngine.R,
                true,
                "118969.fcs",
                Arrays.asList("S"),
                Arrays.asList("<APC-A>"),
                "RAnalysis",
                false,
                true,
                Collections.emptyList()
        );
        importAnalysis(options);

        goToFlowDashboard();
        clickAndWait(Locator.linkContainingText("Show Jobs"));
        waitForPipelineJobsToComplete(1, "R Analysis", false);
        clickAndWait(Locator.linkWithText("COMPLETE"));
        assertTextPresent("/flowjoquery/miniFCS/mini-fcs.xml and loading group Sample Order 5: Gag1&2",
                "finished parsing 2 samples",
                "finished normalizing 2 samples",
                "Transaction completed successfully for mini-fcs.xml",
                "Transaction completed successfully for Normalized mini-fcs.xml");
    }
}
