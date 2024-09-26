/*
 * Copyright (c) 2013-2019 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.CustomModules;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Git;
import org.labkey.test.io.Grep;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.util.Maps;
import org.labkey.test.util.Order;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertTrue;

@Category({BVT.class, Daily.class, Git.class, CustomModules.class})
@Order(1)
@BaseWebDriverTest.ClassTimeout(minutes = 20)
public class DatabaseDiagnosticsTest extends BaseWebDriverTest
{
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Test
    public void validateDomainsTest()
    {
        goToAdminConsole().goToSettingsSection();

        clickAndWait(Locator.linkWithText("check database"));

        click(Locator.linkWithText("Validate"));

        PipelineStatusTable statusTable = new PipelineStatusTable(this);
        statusTable.clickStatusLink(0)
                .waitForComplete(300_000)
                .assertLogTextContains("Check complete, 0 errors found");
    }

    @Test
    public void testSiteValidator()
    {
        goToAdminConsole().goToSettingsSection();

        clickAndWait(Locator.linkWithText("site validation"));

        WebElement formEl = Locator.id("form").findElement(getDriver());

        // Enable all validators
        Locator.tagWithAttribute("input", "type", "checkbox")
                .findElements(formEl).forEach(this::checkCheckbox);

        // Validate projects and subfolders
        checkRadioButton(Locator.radioButtonByNameAndValue("includeSubfolders", "true"));

        // Run in background
        checkCheckbox(Locator.id("background"));

        clickAndWait(Locator.lkButton("Validate"));

        new PipelineStatusDetailsPage(getDriver())
                .waitForComplete(300_000)
                .assertLogTextContains("Site validation complete");

        clickAndWait(Locator.lkButton("Data"));

        TextSearcher textSearcher = new TextSearcher(getText(Locators.bodyPanel()));
        assertTextPresent(textSearcher, "Site Level Validation Results", "Folder Validation Results",
                "Module: Core", "Permissions Validator", "Display Format Validator",
                "Module: Pipeline", "Pipeline Validator");
        assertNoLabKeyErrors();
        assertTextNotPresent(textSearcher, "Error");
    }

    @Test
    public void databaseCheckTest()
    {
        // This can take very long depending on what modules are present
        beginAt(WebTestHelper.buildURL("admin", "doCheck"), 600000);
        waitForText(60000, "Database Consistency checker complete");
        assertTextNotPresent("ERROR");
    }

    @Test
    public void testTomcatLogs() throws Exception
    {
        File logDir = TestFileUtils.getServerLogDir();
        assertTrue("Server log directory does not exist: " + logDir, logDir.isDirectory());
        File[] logs = logDir.listFiles();
        Map<File, Integer> contaminatedLogs = Grep.grep(PasswordUtil.getPassword(), logs);
        Map<String, String> failureFiles = new TreeMap<>();
        contaminatedLogs.keySet().forEach(
                file -> failureFiles.put(file.getName(), "line " + contaminatedLogs.get(file)));

        assertTrue(String.format("These tomcat logs (in %s) contained unwanted text [%s]:\n%s",
                logDir.getAbsolutePath(), PasswordUtil.getPassword(), failureFiles.toString()),
                failureFiles.isEmpty());
    }

    @Test
    public void collectActionExceptions()
    {
        beginAt(WebTestHelper.buildURL("admin", "actions", Maps.of("tabId", "exceptions")));
        WebElement exceptionCount = waitForElement(Locator.id("exceptionCount"));
        if (!"no exceptions".equals(exceptionCount.getText()))
            getArtifactCollector().dumpPageSnapshot("exceptions", null);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("admin");
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
