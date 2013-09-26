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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.PortalHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * User: Rylan
 * Date: 3/26/13
 * Time: 11:32 AM
 */
@Category({DailyB.class})
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

        //append into empty target
        insertSourceRow("0", "Subject 0", null);
        runETLjob("append");
        assertInTarget1("Subject 0");
        checkRun(1);

        //append into populated target
        insertSourceRow("1", "Subject 1", null);
        deleteSourceRow("0");
        runETLjob("append");
        checkRun(2);
        assertInTarget1("Subject 0", "Subject 1");

        //merge into populated target
        insertSourceRow("2", "Subject 2", null);
        runETLjob("merge");
        assertInTarget1("Subject 0", "Subject 1", "Subject 2");

        //truncate into populated target
        deleteSourceRow("1");
        runETLjob("truncate");
        assertInTarget1("Subject 2");
        assertNotInTarget1("Subject 0", "Subject 1");

        //identify by run into populated target
        insertSourceRow("3", "Subject 3", "42");
        insertTransferRow("42", getDate(), getDate(), "new transfer", "added by test automation", "pending");
        runETLjob("appendIdByRun");
        assertInTarget1("Subject 2", "Subject 3");

        //error logging test, casting error
        runETLjob("badCast");
        assertInLog("java.lang.String cannot be cast to java.util.Date");

        //error logging test, bad run table name
        runETLjob("badTableName");
        assertInLog("Table not found:");
    }


    protected void runInitialSetup()
    {
        log("running setup");
        _containerHelper.createProject(PROJECT_NAME, null);
        enableModule("DataIntegration", true);
        enableModule("simpletest", true);
        addQueryWebpart("Source", "vehicle", "etl_source");
        addQueryWebpart("Target1", "vehicle", "etl_target");
        //addQueryWebpart("Target2", "vehicle", "etl_target2");
        addQueryWebpart("Transfers", "vehicle", "transfer");
        addQueryWebpart("TransformRun", "dataintegration", "TransformRun");
        addQueryWebpart("TransformHistory", "dataintegration", "TransformHistory");
        addQueryWebpart("TransformRun", "dataintegration", "TransformSummary");
    }

    private void addQueryWebpart(String name, String schema, String query)
    {
        log("adding query webpart " + name);
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

    private void insertSourceRow(String id, String name, String RunId)
    {
        log("inserting source row " + name);
        //goToProjectHome();
        clickTab("Portal");
        click(Locator.xpath("//span[text()='Source']"));
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForElement(Locator.name("quf_id"));
        setFormElement(Locator.name("quf_id"), id);
        setFormElement(Locator.name("quf_name"), name);
        if (null != RunId)
        {
            setFormElement(Locator.name("quf_transformrun"), RunId);
        }
        clickButton("Submit");
        log("returning to project home");
        //goToProjectHome();
        clickTab("Portal");
    }

    private void insertTransferRow(String rowId, String transferStart, String transferComplete,  String description, String log, String status)
    {
        log("inserting transfer row rowid " + rowId);
        goToProjectHome();
        click(Locator.xpath("//span[text()='Transfers']"));
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForElement(Locator.name("quf_rowid"));
        setFormElement(Locator.name("quf_rowid"), rowId);
        setFormElement(Locator.name("quf_transferstart"), transferStart);
        setFormElement(Locator.name("quf_transfercomplete"), transferComplete);
        setFormElement(Locator.name("quf_description"), description);
        setFormElement(Locator.name("quf_log"), log);
        setFormElement(Locator.name("quf_status"), status);
        clickButton("Submit");
        log("returning to project home");
        //goToProjectHome();
        clickTab("Portal");
    }

    private void runETLAppendJob()
    {
        log("running append job");
        goToModule("DataIntegration");
        prepForPageLoad();
        waitAndClick(Locator.xpath("//tr[contains(@transformid,'append')]/td/a"));
        newWaitForPageToLoad();
        log("returning to project home");
        goToProjectHome();
    }

    private void runETLMergeJob()
    {
        log("running merge job");
        goToModule("DataIntegration");
        waitAndClick(Locator.xpath("//tr[contains(@transformid,'merge')]/td/a"));
        log("returning to project home");
        goToProjectHome();
    }

    private void runETLTruncateJob()
    {
        log("running truncate job");
        goToModule("DataIntegration");
        waitAndClick(Locator.xpath("//tr[contains(@transformid,'truncate')]/td/a"));
        log("returning to project home");
        goToProjectHome();
    }

    private void runETLAppendByIdJob()
    {
        log("running append by id job");
        goToModule("DataIntegration");
        waitAndClick(Locator.xpath("//tr[contains(@transformid,'appendIdByRun')]/td/a"));
        log("returning to project home");
        goToProjectHome();
    }

    private void runETLjob(String transformId)
    {
        log("running " + transformId + " job");
        goToModule("DataIntegration");
        waitAndClick(Locator.xpath("//tr[contains(@transformid,'" + transformId + "')]/td/a"));
        log("returning to project home");
        goToProjectHome();
        //clickTab("Portal");
    }

    private void deleteSourceRow(String... ids)
    {
        goToProjectHome();
        clickAndWait(Locator.xpath("//span[text()='Source']"));
        for(String id : ids)
        {
            log("deleting source row id " + id);
            click(Locator.xpath("//a[text()='"+id+"']/../../td/input[@type='checkbox']"));
        }
        prepForPageLoad();
        click(Locator.xpath("//span[text()='Delete']"));
        dismissAlerts();
        newWaitForPageToLoad();
        log("returning to project home");
        //goToProjectHome();
        clickTab("Portal");
    }

    private void assertInTarget1(String... targets)
    {
        //goToProjectHome();
        clickTab("Portal");
        click(Locator.xpath("//span[text()='Target1']"));
        waitForText("etl_target");
        for(String target : targets)
        {
            assertTextPresent(target);
        }
    }

    private void assertNotInTarget1(String... targets)
    {
        //goToProjectHome();
        clickTab("Portal");
        click(Locator.xpath("//span[text()='Target1']"));
        waitForText("etl_target");
        for(String target : targets)
        {
            assertTextNotPresent(target);
        }
    }

    private void assertInTarget2(String... targets)
    {
        goToProjectHome();
        click(Locator.xpath("//span[text()='Target2']"));
        waitForText("etl_target2");
        for(String target : targets)
        {
            assertTextPresent(target);
        }
    }

    private void assertNotInTarget2(String... targets)
    {
        goToProjectHome();
        click(Locator.xpath("//span[text()='Target2']"));
        waitForText("etl_target2");
        for(String target : targets)
        {
            assertTextNotPresent(target);
        }
    }

    private void assertInLog(String... targets)
    {
        //goToProjectHome();
        clickTab("Portal");
        click(Locator.xpath("//span[text()='TransformRun']"));
        waitForText("TransformRun");
        for(String target : targets)
        {
            assertTextPresent(target);
        }
    }

    protected void checkRun(int amount)
    {
        //goToProjectHome();
        clickTab("Portal");
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(amount, "ETL Job", false);
    }

    private String getDate()
    {
        Calendar calendar = new GregorianCalendar();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return format.format(calendar.getTime());
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
