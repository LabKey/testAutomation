package org.labkey.test.tests.core.admin;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.admin.ShortUrlAdminPage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.labkey.test.WebTestHelper.buildRelativeUrl;

@Category({Daily.class})
public class ShortUrlTest extends BaseWebDriverTest
{
    private static final String URL_PREFIX = "surl_test_";
    private static final String URL_SUFFIX = ".url";
    private static final String USER = "shorturl_user@shorturltest.test";
    private static final AtomicInteger count = new AtomicInteger();

    @Override
    protected void doCleanup(boolean afterTest)
    {
        ShortUrlAdminPage.beginAtFiltered(this, URL_PREFIX).deleteAll();

        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, USER);
    }

    @BeforeClass
    public static void setupProject()
    {
        ShortUrlTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(USER);
        _containerHelper.createProject(getProjectName());
    }

    @Test
    public void testShortUrl()
    {
        String shortUrl = getShortUrl();
        String targetUrl = buildRelativeUrl("project", "home", "begin");

        ShortUrlAdminPage adminPage = ShortUrlAdminPage.beginAt(this);
        adminPage.createNewShortUrl(shortUrl, targetUrl);

        Assertions.assertThat(adminPage.getUrlsFromGrid()).as("short Urls").containsEntry(shortUrl, targetUrl);

        beginAtShortUrl(shortUrl);

        Assertions.assertThat(getDriver().getCurrentUrl()).endsWith(targetUrl);
    }

    private String getShortUrl()
    {
        return URL_PREFIX + count.getAndIncrement();
    }

    private void beginAtShortUrl(String shortUrl)
    {
        beginAt(WebTestHelper.getBaseURL() + "/" + shortUrl + URL_SUFFIX);
    }

    @Override
    protected String getProjectName()
    {
        return "ShortUrlTest " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
