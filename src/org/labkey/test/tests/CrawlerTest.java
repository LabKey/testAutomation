/*
 * Copyright (c) 2021-2026 LabKey Corporation
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

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.CspLogUtil;
import org.labkey.test.util.PermissionsHelper.MemberType;
import org.labkey.test.util.core.admin.CspConfigHelper;
import org.labkey.test.util.selenium.WebDriverUtils;
import org.openqa.selenium.UnhandledAlertException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class CrawlerTest extends BaseWebDriverTest
{

    private static final String MODULE_NAME = "CrawlerTest";
    private static final String USER = "injectiontester@labkey.injection.test"; // required by 'injectJsp' page

    private final CspConfigHelper _cspConfigHelper = new CspConfigHelper(this);

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, USER);
        _cspConfigHelper.setEnforceCsp(true);
    }

    @BeforeClass
    public static void setupProject()
    {
        CrawlerTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        CspConfigHelper.debugCspWarnings();
        _containerHelper.createProject(getProjectName(), null);
        _userHelper.createUser(USER);
        new ApiPermissionsHelper(this).addMemberToRole(USER, READER_ROLE, MemberType.user, getProjectName());
    }

    /**
     * Ensure that 'testCrawler' is a valid test
     */
    @Test
    public void testCrawlerTest() throws Exception
    {
        _cspConfigHelper.setEnforceCsp(false);

        String safeParam = "OK!";

        log("Verify vulnerable page requires specific user");
        beginAt(getInjectUrl(safeParam));
        assertElementPresent(Locators.labkeyError);
        assertElementNotPresent(Locator.id("crawlerTestDiv"));

        createDefaultConnection().impersonate(USER);

        log("Verify vulnerable page displays safe parameter");
        beginAt(getInjectUrl(safeParam));
        assertElementPresent(Locator.id("crawlerTestDiv").withText(safeParam));

        log("Verify that page is vulnerable");
        try
        {
            beginAt(getInjectUrl(Crawler.injectScriptBlock), 10_000);
            Assert.fail("Expected an injection alert.");
        }
        catch (UnhandledAlertException alert)
        {
            if (!WebDriverUtils.getUnhandledAlertText(alert, getDriver()).contains(Crawler.injectedAlert))
            {
                throw alert; // Wrong alert
            }
        }
        try
        {
            beginAt(getInjectUrl(Crawler.injectAttributeScript), 10_000);
            Assert.fail("Expected an injection alert.");
        }
        catch (UnhandledAlertException alert)
        {
            if (!WebDriverUtils.getUnhandledAlertText(alert, getDriver()).contains(Crawler.injectedAlert))
            {
                throw alert; // Wrong alert
            }
        }
    }

    @Test (expected = CspLogUtil.CspWarningDetectedException.class)
    public void testEnforceCsp() throws Exception
    {
        _cspConfigHelper.setEnforceCsp(true);

        createDefaultConnection().impersonate(USER);

        log("Verify that page is not vulnerable when CSP is enforced");
        beginAt(getInjectUrl(Crawler.injectScriptBlock), 10_000);

        log("Verify that enforced CSP is also reported");
        CspLogUtil.checkNewCspWarnings(getArtifactCollector()); // throws CspWarningDetectedException
    }

    @Test (expected = CspLogUtil.CspWarningDetectedException.class)
    public void testCspWarning() throws Exception
    {
        Assume.assumeFalse("Can't test for CSP report", TestProperties.isCspCheckSkipped());

        _cspConfigHelper.setEnforceCsp(false);

        int initialLength = getCspReportLog().length();

        String cspWarningUrl = WebTestHelper.buildRelativeUrl(MODULE_NAME, getProjectName(), "cspWarning");
        beginAt(cspWarningUrl);

        // 53261: Provide visibility into CSP reports for cloud clients
        Assertions.assertThat(getCspReportLog().substring(initialLength)).as("CSP warning").contains(cspWarningUrl);

        CspLogUtil.checkNewCspWarnings(getArtifactCollector()); // throws CspWarningDetectedException
    }

    // Crawler should flag external links without the correct 'rel' attribute
    // https://www.labkey.org/home/Developer/issues/Secure/issues-details.view?issueId=40708
    @Test
    public void testExternalLink() throws Exception
    {

        Crawler crawler = new Crawler(this, Duration.ofSeconds(30), true);
        try
        {
            String externalLinkPage = WebTestHelper.buildRelativeUrl(MODULE_NAME, getProjectName(), "externalLink");
            crawler.validatePage(externalLinkPage);
            Assert.fail("Crawler should have found bad external link. Crawled:\n" + String.join("\n", crawler.getUrlsVisited()));
        }
        catch (AssertionError expectedError)
        {
            if (!expectedError.getMessage().contains("Bad 'rel' attribute"))
            {
                throw expectedError;
            }
        }
    }

    @Test
    public void testCrawler() throws Exception
    {
        _cspConfigHelper.setEnforceCsp(false);

        String safeParam = "OK!";

        createDefaultConnection().impersonate(USER);
        log("Test crawler against a vulnerable page");
        Crawler crawler = new Crawler(this, Duration.ofSeconds(30), true);
        try
        {
            crawler.validatePage(getInjectUrl(safeParam));
            Assert.fail("Crawler should have triggered a malicious script. Crawled:\n" + String.join("\n", crawler.getUrlsVisited()));
        }
        catch (AssertionError expectedError)
        {
            if (!expectedError.getMessage().equals("Crawler: Malicious script executed"))
            {
                throw expectedError;
            }
        }
    }

    private String getInjectUrl(String injectionParam)
    {
        return WebTestHelper.buildRelativeUrl(MODULE_NAME, getProjectName(), "injectJsp", Map.of("inject", injectionParam));
    }

    @Override
    protected boolean cspFailFast()
    {
        return false;
    }

    public String getCspReportLog() throws Exception
    {
        return new SimpleGetCommand("admin", "showCspReportLog")
            .execute(createDefaultConnection(), null)
            .getText();
    }

    @After
    public void postTest()
    {
        CspLogUtil.resetCspLogMark();
    }

    @Override
    public void checkLinks() { /* Nothing interesting to crawl */ }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "CrawlerTest Project" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
