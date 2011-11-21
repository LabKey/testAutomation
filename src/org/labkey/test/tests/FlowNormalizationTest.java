/*
 * Copyright (c) 2011 LabKey Corporation
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
import org.labkey.test.util.RReportHelper;

import java.util.Arrays;
import java.util.Collections;

/**
 * User: kevink
 * Date: 10/14/11
 */
public class FlowNormalizationTest extends BaseFlowTest
{
    @Override
    protected void init(boolean normalizationEnabled)
    {
        // fail fast if R is not configured
        // R is needed for the positivity report
        RReportHelper.ensureRConfig(this);

        super.init(normalizationEnabled);
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        init(true);
        String containerPath = "/" + PROJECT_NAME + "/" + getFolderName();

        setFlowPipelineRoot(getLabKeyRoot() + PIPELINE_PATH);
        goToFlowDashboard();

        ImportAnalysisOptions options = new ImportAnalysisOptions(
                containerPath,
                "/flowjoquery/miniFCS/mini-fcs.xml",
                "/flowjoquery/miniFCS",
                false,
                "rEngine",
                Arrays.asList("Sample Order 5: Gag1&2"),
                true,
                "118969.fcs",
                "APC-A",
                "RAnalysis",
                false,
                true,
                Collections.<String>emptyList()
        );
        importAnalysis(options);

        goToFlowDashboard();
        clickLinkWithText("Show Jobs");
        clickLinkWithText("COMPLETE");
        assertTextPresent("/flowjoquery/miniFCS/mini-fcs.xml and loading group Sample Order 5: Gag1&2");
        assertTextPresent("finished parsing 2 samples");
        assertTextPresent("finished normalizing 2 samples");
        assertTextPresent("Transaction completed successfully for mini-fcs.xml");
        assertTextPresent("Transaction completed successfully for Normalized mini-fcs.xml");
    }
}
