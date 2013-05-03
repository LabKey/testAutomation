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

        insertSourceRow("0", "Subject 0");
        runETLAppendJob();
        assertInTarget("Subject 0");
        checkRun(1);

        insertSourceRow("1", "Subject 1");
        deleteSourceRow("0");
        runETLAppendJob();
        checkRun(2);
        assertInTarget("Subject 0", "Subject 1");

        insertSourceRow("2", "Subject 2");
        runETLMergeJob();
        assertInTarget("Subject 0", "Subject 1", "Subject 2");

//        disableModules("simpletest");
    }


    protected void runInitialSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        enableModule("DataIntegration", true);
        enableModule("simpletest", true);
        addQueryWebpart("Source", "vehicle", "etl_source");
        addQueryWebpart("Target", "vehicle", "etl_target2");
    }

    private void addQueryWebpart(String name, String schema, String query)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Query");
        waitForElement(Locator.name("title"));
        setFormElement(Locator.name("title"), name);
        waitForTextToDisappear("Loading...");
        setFormElement(Locator.xpath("//input[@id='schemaName-inputEl']"), schema);
        click(Locator.id("selectQueryContents-inputEl"));
        waitForTextToDisappear("Loading...");
        setFormElement(Locator.xpath("//input[@id='queryName-inputEl']"), query);
        waitForTextToDisappear("Loading...");
        clickButton("Submit");
    }

    private void insertSourceRow(String id, String name)
    {
        goToProjectHome();
        click(Locator.xpath("//span[text()='Source']"));
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForElement(Locator.name("quf_id"));
        setFormElement(Locator.name("quf_id"), id);
        setFormElement(Locator.name("quf_name"), name);
        clickButton("Submit");
        goToProjectHome();
    }

    private void runETLAppendJob()
    {
        goToModule("DataIntegration");
        waitAndClick(Locator.xpath("//tr[contains(@transformid,'append')]/td/a"));
        goToProjectHome();
    }

    private void runETLMergeJob()
    {
        goToModule("DataIntegration");
        waitAndClick(Locator.xpath("//tr[contains(@transformid,'merge')]/td/a"));
        goToProjectHome();
    }

    private void deleteSourceRow(String... ids)
    {
        goToProjectHome();
        click(Locator.xpath("//span[text()='Source']"));
        for(String id : ids)
        {
            click(Locator.xpath("//a[text()='"+id+"']/../../td/input[@type='checkbox']"));
        }
        click(Locator.xpath("//span[text()='Delete']"));
        dismissAlerts();
        goToProjectHome();
    }

    private void assertInTarget(String... targets)
    {
        goToProjectHome();
        click(Locator.xpath("//span[text()='Target']"));
        waitForText("etl_target2");
        for(String target : targets)
        {
            assertTextPresent(target);
        }
    }


    protected void checkRun(int amount)
    {
        goToProjectHome();
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(amount, "ETL Job", false);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

    }

    @Override
    public void checkQueries()
    {
        log("Skipping query check. Some tables used by queries in simpletest module are not created in this test");
        log("Query check from " + SimpleModuleTest.class.getSimpleName() + " should cover anything this would check");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/dataintegration";
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.FIREFOX;
    }
}
