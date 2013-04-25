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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.PortalHelper;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Rylan
 * Date: 3/26/13
 * Time: 11:32 AM
 */
public class ETLTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ETLTestProject";
    private static final String USER = "issueuser@testing.test";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        //Setup Steps
        runInitialSetup();
        //Confirm that if no changes were made, we don't initialize a pipeline job.
        checkRun(1);
        //Add an issue to detect and check that it creates a job
        addIssue("Issue #1", "issueuser", "The first issue.");
        checkRun(2);

        disableModules("simpletest");
    }


    protected void runInitialSetup()
    {
        //This is here to prevent an email send error at the end of the test
        enableEmailRecorder();
        _containerHelper.createProject(PROJECT_NAME, null);
        new PortalHelper(this).addWebPart("Issues List");
        enableModule("DataIntegration", true);
        enableModule("simpletest", true);
        createPermissionsGroup("IssueGroup", USER);

        //Turn on the checker service (should cause a job to appear at the first pipeline check for the user we made)
        goToModule("DataIntegration");
        waitForElement(Locator.xpath("//tr[contains(@transformid,'DemoETL')]"));
        waitForElement(Locator.xpath("//tr[contains(@transformid,'IssuesETL')]"));
        waitForElement(Locator.xpath("//tr[contains(@transformid,'append')]"));

        waitAndClick(Locator.xpath("//tr[contains(@transformid,'DemoETL')]/td/input"));
    }


    protected void checkRun(int amount)
    {
        goToModule("DataIntegration");
        waitAndClick(Locator.xpath("//tr[contains(@transformid,'IssuesETL')]/td/a"));
        goToProjectHome();
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(amount, "ETL Job", false);
    }

    protected void addIssue(String issueName, String assignedTo, String comment)
    {
        goToProjectHome();
        waitForElement(Locator.xpath("//span[text()='New Issue']"));
        clickButton("New Issue");
        waitForElement(Locator.xpath("//input[@name='title']"));
        setFormElement(Locator.xpath("//input[@name='title']"), issueName);
        setFormElement(Locator.xpath("//select[@name='assignedTo']"), assignedTo);
        setFormElement(Locator.xpath("//textarea[@id='comment']"), comment);
        clickButton("Save");
        goToProjectHome();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsers(afterTest, USER);
        super.doCleanup(afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/dataintegration";
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
