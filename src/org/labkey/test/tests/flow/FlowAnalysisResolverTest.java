/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Flow;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, Flow.class})
public class FlowAnalysisResolverTest extends BaseFlowTest
{
    private final String FCS_FILE = "118795.fcs";

    @Before
    public void preTest()
    {
        goToProjectHome();
        clickFolder(getFolderName());
    }

    @Test
    public void _doTestSteps()
    {
        //import set 1
        clickFolder("FlowAnalysisResolverTest");
        importFCSFiles();

        //import set 2

        //import analysis
        String analysisZipPath = "/resolve-test/statistics.tsv";

        goToFlowDashboard();
        clickAndWait(Locator.linkContainingText("FCS files to be imported"));
        _fileBrowserHelper.importFile(analysisZipPath, "Import External Analysis");
        importAnalysis_selectFCSFiles(getContainerPath(), SelectFCSFileOption.Previous, null);
        assertTextPresent("Matched 3 of 4 samples");

        //verify the first file doesn't resolve
        assertEquals("", getMatchedFileForName("selectedSamples.rows[no-resolve01].matchedFile"));

        verifyCantChooseUnmatchedSample();

        //set no-resolve file to a file and proceed with import
        selectOptionByText(Locator.name("selectedSamples.rows[no-resolve01].matchedFile"), "118795.fcs (microFCS)");

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

        // verify attachments present
        assertTextPresent("readme.txt");
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
