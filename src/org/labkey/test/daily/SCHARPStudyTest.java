/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
package org.labkey.test.daily;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: dave
 * Date: Aug 4, 2009
 * Time: 1:59:39 PM
 */
public class SCHARPStudyTest extends BaseSeleniumWebTest
{
    public static final String PROJECT_NAME="SCHARP Study Test";

    private String _labkeyRoot = getLabKeyRoot();
    private String _pipelinePathMain = new File(_labkeyRoot, "/sampledata/study").getPath();
    private String _zipFilePath = new File(_labkeyRoot, "/sampledata/study/studyshell.zip").getPath();

    protected static class StatusChecker implements Checker
    {
        private BaseSeleniumWebTest _test;
        private String _waitForMessage;
        private Locator _loc = Locator.id("vq-status");

        public StatusChecker(String waitForMessage, BaseSeleniumWebTest test)
        {
            _test = test;
            _waitForMessage = waitForMessage;
        }

        public boolean check()
        {
            String curMessage = _test.getText(_loc);
            if (null == curMessage)
                fail("Can't get message in locator " + _loc.toString());
            return (curMessage.startsWith(_waitForMessage));
        }
    }

    protected void doTestSteps() throws Exception
    {
        log("creating project...");
        createProject(PROJECT_NAME, "Study");

        ensureAdminMode();
        clickLinkWithText("Admin Console");
        if (isTextPresent("Microsoft SQL Server"))
        {
            log("NOTE: Database type is SQL Server...skipping test...re-enable this on SQL Server once the following bugs are resolved: 8451, 8452, 8453, 8454, 8455.");
            return;
        }

        log("importing study...");
        setupPipeline();
        importStudy();

        log("Study imported and queries validated successfully.");
    }

    protected void setupPipeline()
    {
        log("Setting pipeline root to " + _pipelinePathMain + "...");
        setPipelineRoot(_pipelinePathMain);
        assertTextPresent("The pipeline root was set");
        clickLinkWithText(PROJECT_NAME);
    }

    protected void importStudy()
    {
        log("Importing study from " + _zipFilePath + "...");
        clickNavButton("Import Study");
        setFormElement("studyZip", _zipFilePath);
        clickNavButton("Import Study From Local Zip Archive");
        assertTextNotPresent("This file does not appear to be a valid .zip file");

        if (isTextPresent("You must select a .zip file to import"))
        {
            setFormElement("studyZip", _zipFilePath);
            clickNavButton("Import Study");
        }

        assertTextPresent("Data Pipeline");

        while(countLinksWithText("COMPLETE") < 1)
        {
            if (countLinksWithText("ERROR") > 0)
            {
                fail("Job in ERROR state found in the list");
            }

            log("Waiting for study to finish loading...");
            sleep(3000);
            refresh();
        }

        clickLinkWithText(PROJECT_NAME);
    }

    protected void doCleanup() throws Exception
    {
        log("Starting cleanup...");
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch(Throwable ignore){}
        log("Cleaned up successfully.");
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }
}
