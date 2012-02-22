package org.labkey.test.tests;

import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 2/20/12
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class LuminexAsyncImportTest extends LuminexTest
{
    protected void runUITests()
    {
        clickCheckbox("backgroundUpload");
        saveAssay();
        sleep(1500);
        importRunForTestLuminexConfig(TEST_ASSAY_LUM_FILE5, Calendar.getInstance(), 0);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToComplete(1, "Assay upload", false);
        clickLinkWithText("COMPLETE");
//        assertTextNotPresent("ERROR");
    }
}
