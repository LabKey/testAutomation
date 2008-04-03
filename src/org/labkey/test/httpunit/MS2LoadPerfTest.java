package org.labkey.test.httpunit;

import org.labkey.test.httpunit.BaseHttpUnitWebTest;

import java.io.IOException;
import java.io.File;

/**
 * User: peter@labkey.com
 * Date: Jan 29, 2007
 *
 */
public class MS2LoadPerfTest extends BaseHttpUnitWebTest
{
    protected static final String PROJECT_NAME = "MS2PerfProject";
    protected static final String FOLDER_NAME = "MS2PerfFolder";
    protected static final String PIPELINE_PATH = "/sampledata/xarfiles/ms2pipe";

    //Save button on the showCustomizeSite form
    String _save_button = "Save";

    String _database = "ipi.HUMAN.fasta.20060111";
    String _search_type = "xtandem";
//    String _search_name = "Good_IPAS_Human_Q3";
//    String _pipeline_subfolder = "AX01";
//    String _xarfileNameBase = "LO_IP0042_AX01_SG25to26";

//    String _pipeline_subfolder = "Test";
//    String _xarfileNameBase = "TestFile";

//    String _expRunNameStartsWith = _pipeline_subfolder + "/" + _xarfileNameBase;


    String _search_name = "Good_IPAS_Human_Q3_Combined";
    String _pipeline_subfolder = "BigSearch";
    String _xarfileNameBase = "all";
    String _expRunNameStartsWith = _pipeline_subfolder;

    String _input_xml_name = "tandem.xml";
    String _input_xml_text1 = "protocol name";
    String _input_xml_text2 = "protocol description";
    String _score_filter = "2";
    String _score = "Unique";
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

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        String ms2RunName = _xarfileNameBase + ".pep.xml (Experiment Import - " + _xarfileNameBase + ".search.xar.xml";
        String ppFileName = _xarfileNameBase + ".pep-prot.xml";

        log("Reload existing run " + ms2RunName );

        clickNavButton("Process and Import Data");
        clickLinkWithText(_pipeline_subfolder);
        clickLinkWithText(_search_type);
        clickLinkWithText(_search_name);

         clickNavButton("Import Experiment");

        log("Verify upload started.");

        int seconds = 0;
        while (getLinkWithTextCount(ms2RunName ) < 1 && seconds++ < MAX_WAIT_SECONDS_LOAD)
        {
            log("Waiting for import task to start ");
            sleep(1000);
            refresh();
        }

        if (getLinkWithTextCount(ms2RunName ) < 1)
            fail("All tasks did not complete.");

        log("get ProteinProphet view of MS2 run ");
        clickImageWithAltText("Experiment");
        beginAt(getCurrentRelativeURL() + "&Runs.Name~startswith=" + _expRunNameStartsWith);
        clickLinkWithText("details");

        log("Go to Protein Prophet details");
        clickLinkWithText("Protein Prophet Scores");

        log("Verify data view.");
        assertTextPresent(ppFileName);
        assertLinkPresentWithText("Download");
        log("Waiting for load to complete");

        int sec = MAX_WAIT_SECONDS_LOAD;
        while (!isLinkPresentWithText("view") && sec-- > 0)
        {
            sleep(2000);
            refresh();
        }
        clickLinkWithText("view");

        log("Waiting for MS2Run view");

        sec = MAX_WAIT_SECONDS_LOAD;
        while (isTextPresent("Run is still loading.") && sec-- > 0)
        {
            sleep(1000);
            refresh();
        }
        
        log("Done load test.");

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
