package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.PermissionsHelper.MemberType;
import org.openqa.selenium.UnhandledAlertException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class CrawlerTest extends BaseWebDriverTest
{

    private static final String MODULE_NAME = "CrawlerTest";
    private static final String USER = "injectiontester@labkey.injection.test";

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, USER);
    }

    @BeforeClass
    public static void setupProject()
    {
        CrawlerTest init = (CrawlerTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _userHelper.createUser(USER);
        new ApiPermissionsHelper(this).addMemberToRole(USER, "Reader", MemberType.user, getProjectName());
    }

    /**
     * Ensure that 'testCrawler' is a valid test
     */
    @Test
    public void testCrawlerTest() throws Exception
    {
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
            if (!alert.getMessage().contains(Crawler.injectedAlert))
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
            if (!alert.getMessage().contains(Crawler.injectedAlert))
            {
                throw alert; // Wrong alert
            }
        }
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
