/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.WebTestHelper;

import java.io.File;
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
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE1));
        saveAssay();
        sleep(1500);

        // test successful background upload
        importRunForTestLuminexConfig(TEST_ASSAY_LUM_FILE5, Calendar.getInstance(), 0);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToComplete(1, "Assay upload", false);
        clickLinkWithText("COMPLETE");
        assertTextNotPresent("ERROR"); //Issue 14082
        assertTextPresent("Starting assay upload", "Finished assay upload");
        clickButton("Data"); // data button links to the run results
        assertTextPresent(TEST_ASSAY_LUM + " Results");

        // test background upload failure
        uploadPositivityFile("No Fold Change", "1", "", true);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(2);
        clickLinkWithText("ERROR");
        assertTextPresent("An error occurred when running the script (exit code: 1).", 3);
        assertTextPresent("output lines omitted, see full output in log for details]", 2);
        assertTextPresent("Error: No value provided for 'Positivity Fold Change'.", 3);
        checkExpectedErrors(2);
    }
}
