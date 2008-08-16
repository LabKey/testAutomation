/*
 * Copyright (c) 2008 LabKey Software Foundation
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
package org.labkey.test.bvt;

import org.labkey.test.ms2.MS2PipelineFolder;
import org.labkey.test.ms2.cluster.MS2EmailSuccessParams;
import org.labkey.test.pipeline.PipelineFolder;
import org.labkey.test.pipeline.PipelineTestsBase;
import org.labkey.test.pipeline.PipelineWebTestBase;
import org.labkey.test.pipeline.PipelineTestParams;
import org.labkey.test.Locator;
import org.labkey.test.util.EmailRecordTable;

/**
 * <code>PipelineBvtTest</code>
 */
public class PipelineBvtTest extends PipelineWebTestBase
{
    protected PipelineTestsBase _testSet1 = new PipelineTestsBase(this);

    public PipelineBvtTest()
    {
        super("Pipeline BVT");

        MS2PipelineFolder folder = new MS2PipelineFolder(this, "pipe1",
                getLabKeyRoot() + "/sampledata/xarfiles/ms2pipe");
        folder.setFolderType("None");
        folder.setTabs("Pipeline", "MS1", "MS2", "Dumbster");
        folder.setWebParts("Data Pipeline", "MS1 Runs", "MS2 Runs (Enhanced)", "Mail Record");

        PipelineFolder.MailSettings mail = new PipelineFolder.MailSettings(this);
        mail.setNotifyOnSuccess(true, true, "brother@labkey.org");
        mail.setNotifyOnError(true, true);
        mail.setEscalateUsers("support@labkey.org");
        folder.setMailSettings(mail);

        _testSet1.setFolder(folder);
        _testSet1.addParams(mailParams(new MS2EmailSuccessParams(this, "bov_fract", "test1",
                "CAexample_mini1", "CAexample_mini2"), mail));
        _testSet1.addParams(mailParams(new MS2EmailSuccessParams(this, "bov_fract", "test_fract"), mail));
    }

    MS2EmailSuccessParams mailParams(MS2EmailSuccessParams params, PipelineFolder.MailSettings mail)
    {
        params.setMailSettings(mail);
        return params;
    }

    public String getAssociatedModuleDirectory()
    {
        return "pipeline";  // + ms2 and ms1
    }

    protected void doCleanup() throws Exception
    {
        _testSet1.clean();
        super.doCleanup();
    }

    protected void doTestSteps() throws Exception
    {
        _testSet1.verifyClean();

        setupSite(true);
        _testSet1.setup();

        EmailRecordTable mailTable = new EmailRecordTable(this);
        mailTable.saveRecorderState();
        mailTable.clearAndRecord();
        runProcessing(_testSet1);

        // Repeat to make sure retry works
        mailTable.clearAndRecord();
        runProcessing(_testSet1);
        mailTable.restoreRecorderState();
    }

    private void runProcessing(PipelineTestsBase testSet)
    {
        testSet.runAll();

        // Just wait for everything to complete.
        do
        {
            log("Waiting for tests processing to complete");
            sleep(5000);
            refresh();
        }
        while (testSet.getCompleteParams().length != testSet.getParams().length);

        for (PipelineTestParams tp : testSet.getParams())
        {
            pushLocation();
            tp.validate();
            popLocation();

            // Quit if a test was not valid.
            assertTrue(tp.isValid());
        }
    }
}
