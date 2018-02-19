/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.CustomModules;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Git;
import org.labkey.test.io.Grep;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PipelineStatusTable;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertTrue;

@Category({BVT.class, DailyA.class, DailyB.class, DailyC.class, Git.class, CustomModules.class})
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
        goToAdminConsole().goToAdminConsoleLinksSection();

        clickAndWait(Locator.linkWithText("Check Database"));

        click(Locator.linkWithText("Validate"));

        PipelineStatusTable statusTable = new PipelineStatusTable(this);
        statusTable.clickStatusLink(0);

        waitForTextWithRefresh(300000, "Check complete");
        assertTextPresent("Check complete, 0 errors found");
    }

    @Test
    public void databaseCheckTest()
    {
        goToAdminConsole().goToAdminConsoleLinksSection();
        clickAndWait(Locator.linkWithText("Check Database"));
        clickAndWait(Locator.linkWithText("Do Database Check"), 120000);
        waitForText(60000, "Database Consistency checker complete");
        assertTextNotPresent("ERROR");
    }

    @Test
    public void testTomcatLogs() throws Exception
    {
        File tomcatHome = TestProperties.getTomcatHome();
        assertTrue("Specified tomcat.home does not exist: " + tomcatHome +
                "\nMake sure CATALINA_HOME is set or specify 'tomcat.home' when running tests",
                tomcatHome != null && tomcatHome.exists());
        File logDir = new File(tomcatHome, "logs");
        File[] logs = logDir.listFiles();
        Map<File, Integer> contaminatedLogs = Grep.grep(PasswordUtil.getPassword(), logs);
        Map<String, String> failureFiles = new TreeMap<>();
        contaminatedLogs.keySet().forEach(
                file -> failureFiles.put(file.getName(), "line " + contaminatedLogs.get(file)));

        assertTrue(String.format("These tomcat logs (in %s) contained unwanted text [%s]:\n%s",
                tomcatHome.getAbsolutePath(), PasswordUtil.getPassword(), failureFiles.toString()),
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
