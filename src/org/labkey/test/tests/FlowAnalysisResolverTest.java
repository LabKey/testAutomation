package org.labkey.test.tests;

import junit.framework.Assert;
import org.labkey.test.BaseFlowTest;
import org.labkey.test.BaseFlowTestWD;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 1/10/13
 * Time: 4:40 PM
 * To change this template use File | Settings | File Templates.
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
