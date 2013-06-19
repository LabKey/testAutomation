/*
 * Copyright (c) 2013 LabKey Corporation
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
import org.labkey.test.BaseFlowTest;
import org.labkey.test.BaseFlowTestWD;
import org.labkey.test.Locator;

/**
 * User: elvan
 * Date: 1/10/13
 * Time: 4:40 PM
 */
public class FlowAnalysisResolverTest extends FlowTest
{

    private final String FCS_FILE = "118795.fcs";

    public void _doTestSteps()
    {

        //import set 1

        click(Locator.linkWithText("FlowAnalysisResolverTest"));
        importFCSFiles();

        //import set 2

        //import analsysis
        String analysisZipPath = "/resolve-test/statistics.tsv";

        goToFlowDashboard();
        clickAndWait(Locator.linkContainingText("FCS files to be imported"));
        selectPipelineFileAndImportAction(analysisZipPath, "Import External Analysis");
        importAnalysis_selectFCSFiles(getContainerPath(), SelectFCSFileOption.Previous, null);
        assertTextPresent("Matched 3 of 4 samples");

        //verify the first file doesn't resolve
        Assert.assertEquals("", getMatchedFileForName("selectedSamples.rows[no-resolve01].matchedFile"));

        verifyCantChooseUnmatchedSample();

        //set no-resolve file to a file and proceed with import
        setFormElement(Locator.name("selectedSamples.rows[no-resolve01].matchedFile"), "118795.fcs (microFCS)");


        verifyImportedAllFiles();
    }

    private void verifyImportedAllFiles()
    {

        clickButton("Next");
        clickButton("Next");

        assertTextPresent("All 4 selected", "1 FCS files");
        clickButton("Finish");
        waitForText("Experiment Run Graph");
         assertTextPresent(FCS_FILE, 4);
    }

    private String getMatchedFileForName(String name)
    {
       return getFormElement(Locator.name(name));

    }

    private void verifyCantChooseUnmatchedSample()
    {
        click(Locator.name("selectedSamples.rows[no-resolve01].selected"));
        clickButton("Next");
        assertTextPresent("All selected rows must be matched to a previously imported FCS file.", "Import Analysis: Review Samples");
    }

}
