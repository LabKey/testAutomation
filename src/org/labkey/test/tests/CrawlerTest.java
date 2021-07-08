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
import org.labkey.test.util.Crawler;
import org.openqa.selenium.UnhandledAlertException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
public class CrawlerTest extends BaseWebDriverTest
{

    private static final String MODULE_NAME = "CrawlerTest";

    @BeforeClass
    public static void setupProject()
    {
        CrawlerTest init = (CrawlerTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    /**
     * Ensure that 'testCrawler' is a valid test
     */
    @Test
    public void testCrawlerTest()
    {
        String safeParam = "OK!";

        log("Verify vulnerable page must be enabled");
        beginAt(getInjectUrl(safeParam));
        assertElementPresent(Locators.labkeyError);
        assertElementNotPresent(Locator.tag("div").withText(safeParam));

        _containerHelper.enableModule(MODULE_NAME);

        log("Verify vulnerable page displays safe parameter");
        beginAt(getInjectUrl(safeParam));
        assertElementPresent(Locator.tag("div").withText(safeParam));

        log("Verify that page is vulnerable");
        try
        {
            beginAt(getInjectUrl(Crawler.injectScriptBlock));
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
            beginAt(getInjectUrl(Crawler.injectAttributeScript));
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

    @Test
    public void testCrawler()
    {
        String safeParam = "OK!";

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
