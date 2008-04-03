package org.labkey.test.httpunit;

import com.meterware.httpunit.*;

import java.io.IOException;
import java.io.File;

import org.labkey.test.httpunit.BaseHttpUnitWebTest;

/**
 * User: peter@labkey.com
 * Date: Jan 29, 2007
 *
 */
public class MS2AnalyzePerfTest extends BaseHttpUnitWebTest
{
    protected static final String PROJECT_NAME = "MS2PerfProject";
    protected static final String FOLDER_NAME = "MS2PerfFolder";
    protected static final String PIPELINE_PATH = "/sampledata/xarfiles/ms2pipe";

    String _database = "ipi.HUMAN.fasta.20060111";
    String _search_type = "xtandem";
/*
    String _search_name = "Good_IPAS_Human_Q3";
    String _pipeline_subfolder = "AX01";
    String _xarfileNameBase = "LO_IP0042_AX01_SG25to26";
    String _expRunNameStartsWith = _pipeline_subfolder + "/" + _xarfileNameBase;
 */
    String _pipeline_subfolder = "BigSearch";
    String _xarfileNameBase = "all";
    String _search_name = "Good_IPAS_Human_Q3_Combined";
    String _expRunNameStartsWith = _pipeline_subfolder;

    String _ms2ViewName = "MS2PerfTestPPViewWithQ3 (Shared)";

    int MAX_WAIT_SECONDS_LOAD = 60*10;

    public String getAssociatedModuleDirectory()
    {
        return "ms2";
    }

    // override cleanup to avoid deleting target project
    public void cleanup() throws Exception {

    }

    protected void doCleanup() throws IOException {

    }

    protected void doTestSteps()
    {
        String runName = _xarfileNameBase + ".pep.xml (Experiment Import - " + _xarfileNameBase + ".search.xar.xml";
        String ppFileName = _xarfileNameBase + ".pep-prot.xml";

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        log("get ProteinProphet view of MS2 run ");
        clickImageWithAltText("Experiment");
        beginAt(getCurrentRelativeURL() + "&experimentRunFilter=MS2+Searches&MS2SearchRuns.Name~startswith=" + _expRunNameStartsWith);
        clickLinkWithImage("/cpas/MS2/images/runIcon.gif");

        log("View and filter Protein Prophet data");
        assertLinkPresentWithText("Protein Prophet Collapsed");
        clickLinkWithText("Protein Prophet Collapsed");
        selectOption("viewParams", _ms2ViewName);
        clickNavButton("Go");

        log("Sort by unique peptieds in PP view");
        clickLinkWithText("PP Unique");

        // Back to main window
        log("Compare protein prophet across runs.");

/*
        // TODO:  get select All to work on new runs grid
        clickImageWithAltText("Experiment");
        selectOption("experimentRunFilter", "MS2 Searches");
        submit();
        //filter down to fewer runs
        beginAt(getCurrentRelativeURL() + "&MS2SearchRuns.Name~startswith=AX01");

       assertNavButtonPresent("Select All");
        clickNavButton("Select All");

*/
        clickImageWithAltText("MS2");
        clickNavButton("Select All");
        clickNavButton("Compare ProteinProphet");

        assertTextPresent("Compare ProteinProphet Proteins");
        selectOption("viewParams", _ms2ViewName);
        checkCheckbox("light2HeavyRatioMean");
        checkCheckbox("totalPeptides");
        checkCheckbox("uniquePeptides");

        // tricky workaround to failure of the test harness to read in the showCompare.view (always runs out of memory)
        // submit via the getResource method to intercept redirect and send it to the excel export version
        // of the same page, which the webClient doesn't try to parse but needs to be told to discard.
        WebRequest req = getDialog().getForm().getRequest();
        WebClient wc = getDialog().getWebClient();
        WebResponse resp=null;
        try {
            resp = wc.getMainWindow().getResource(req);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        String searchTerm = "/showCompare.view";
        String replaceTerm = "/exportCompareToExcel.view";
        resp = changeRedirectTarget(wc, resp, searchTerm, replaceTerm);
        discardStream(resp);

        log("Done, return to folder root.");

    }


    private void delete(File file) throws IOException
    {
        if (file.isDirectory())
        {
            for (File child : file.listFiles())
            {
                delete(child);
            }
        }
        System.out.println("Deleting " + file.getPath() + "\n");
        file.delete();
    }
}
