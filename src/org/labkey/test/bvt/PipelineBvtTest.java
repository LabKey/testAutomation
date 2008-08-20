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
import org.labkey.test.ms2.params.MS2EmailSuccessParams;
import org.labkey.test.pipeline.PipelineFolder;
import org.labkey.test.pipeline.PipelineTestsBase;
import org.labkey.test.pipeline.PipelineWebTestBase;
import org.labkey.test.pipeline.PipelineTestParams;
import org.labkey.test.util.EmailRecordTable;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.ms1.params.PepMatchTestParams;
import org.labkey.test.ms1.params.FeaturesTestParams;
import org.apache.commons.lang.StringUtils;

/**
 * <code>PipelineBvtTest</code>
 */
public class PipelineBvtTest extends PipelineWebTestBase
{
    protected static final int MAX_WAIT_SECONDS = 60*5;

    protected PipelineTestsBase _testSetMS2 = new PipelineTestsBase(this);
    protected PipelineTestsBase _testSetMS1 = new PipelineTestsBase(this);

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
        mail.setEscalateUsers("momma@labkey.org");
        folder.setMailSettings(mail);

        String[] sampleNames = new String[] { "CAexample_mini1", "CAexample_mini2" };
        _testSetMS2.setFolder(folder);
        _testSetMS2.addParams(mailParams(new MS2EmailSuccessParams(this, "bov_fract", "test1", sampleNames), mail));
        _testSetMS2.addParams(mailParams(new MS2EmailSuccessParams(this, "bov_fract", "test_fract"), mail));

        _testSetMS1.setFolder(folder);
        _testSetMS1.addParams(mailParams(new FeaturesTestParams(this, "bov_fract", "find_minmax",
                false, sampleNames), mail, true));
        _testSetMS1.addParams(mailParams(new PepMatchTestParams(this, "bov_fract/xtandem/test1", "match1",
                true, sampleNames), mail, true));
    }

    private PipelineTestParams mailParams(PipelineTestParams params, PipelineFolder.MailSettings mail)
    {
        return mailParams(params, mail, false);
    }

    private PipelineTestParams mailParams(PipelineTestParams params, PipelineFolder.MailSettings mail,
                                          boolean expectError)
    {
        params.setMailSettings(mail);
        params.setExpectError(expectError);
        return params;
    }

    public String getAssociatedModuleDirectory()
    {
        return "pipeline";  // + ms2 and ms1
    }

    protected void doCleanup() throws Exception
    {
        _testSetMS2.clean();
        _testSetMS1.clean();
        super.doCleanup();
    }

    protected void doTestSteps() throws Exception
    {
        _testSetMS2.verifyClean();
        _testSetMS1.verifyClean();

        setupSite(true);
        _testSetMS2.setup();

        EmailRecordTable emailTable = new EmailRecordTable(this);
        emailTable.saveRecorderState();
        emailTable.clearAndRecord();
        runProcessing(_testSetMS2);

        // Repeat to make sure retry works
        emailTable.clearAndRecord();
        runProcessing(_testSetMS2);
        checkEmail(emailTable, 2);

        // Make sure there haven't been any errors yet.
        checkErrors();

        // Break the pipeline tools directory setting to cause errors.
        setPipelineToolsDirectory(getLabKeyRoot() + "/external/noexist");
        runProcessing(_testSetMS1);
        checkEmail(emailTable, 4);

        // Make sure the expected errors have been logged.
        checkExpectedErrors();

        // Test pipeline error escalation email.
        _testSetMS1.getParams()[0].validateEmailEscalation(0);
        checkEmail(emailTable, 1);

        // Fix the pipeline tools directory.
        setPipelineToolsDirectory(getLabKeyRoot() + "/external/bin");

        PipelineStatusTable statusTable = new PipelineStatusTable(this, false, true);
        PipelineTestParams tpRetry = _testSetMS1.getParams()[0];
        tpRetry.setExpectError(false);
        for (String sampleExp : tpRetry.getExperimentLinks())
        {
            pushLocation();
            statusTable.clickStatusLink(sampleExp);
            clickNavButton("Retry");
            popLocation();
        }

        waitToComplete(_testSetMS1);

        // Could validate here more, but the final validation should be enough.
        checkEmail(emailTable, 1);

        for (PipelineTestParams tp : _testSetMS1.getParams())
            tp.setExpectError(false);
        runProcessing(_testSetMS1);
        checkEmail(emailTable, 2);

        emailTable.restoreRecorderState();
    }

    public void checkEmail(EmailRecordTable emailTable, int countExpect)
    {
        int count = emailTable.getDataRowCount();
        assertTrue("Expected " + countExpect + " notification emails, found " + count,
                count == countExpect);
        emailTable.clearAndRecord();
    }

    public void checkExpectedErrors()
    {
        // Need to remember our location or the next test could start with a blank page
        pushLocation();
        beginAt("/admin/showErrorsSinceMark.view");

        //IE and Firefox have different notions of empty.
        //IE returns html for all pages even empty text...
        String text = selenium.getHtmlSource();
        if (null == text)
            text = "";
        text = text.trim();
        if ("".equals(text))
        {
            text = selenium.getText("//body");
            if (null == text)
                text = "";
            text = text.trim();
        }

        assertTrue("Expected 4 errors during this run", StringUtils.countMatches(text, "ERROR") == 4);
        log("Expected errors found.");

        // Clear the errors to prevent the test from failing.
        resetErrors();

        popLocation();
    }

    private void runProcessing(PipelineTestsBase testSet)
    {
        testSet.runAll();

        waitToComplete(testSet);
        
        for (PipelineTestParams tp : testSet.getParams())
        {
            pushLocation();
            tp.validate();
            popLocation();

            // Quit if a test was not valid.
            assertTrue(tp.isValid());
        }
    }

    private void waitToComplete(PipelineTestsBase testSet)
    {
        // Just wait for everything to complete.
        int seconds = 0;
        int sleepInterval = 2;
        do
        {
            log("Waiting for tests processing to complete");
            sleep(sleepInterval * 1000);
            seconds += sleepInterval;
            refresh();
        }
        while (testSet.getCompleteParams().length != testSet.getParams().length &&
                seconds < MAX_WAIT_SECONDS);
    }

    private void setPipelineToolsDirectory(String path)
    {
        log("Set tools bin directory to " + path);
        pushLocation();
        clickLinkWithText("Admin Console");
        clickLinkWithText("site settings");
        setFormElement("pipelineToolsDirectory", path);
        clickNavButton("Save");
        popLocation();
    }
}
