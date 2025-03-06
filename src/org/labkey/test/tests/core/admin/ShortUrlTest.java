package org.labkey.test.tests.core.admin;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.pages.admin.ShortUrlAdminPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.labkey.test.WebTestHelper.buildRelativeUrl;

@Category({BVT.class}) // TODO: switch to Daily
public class ShortUrlTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ShortUrlTest Project";
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

    /**
     * Verify basic shortUrl functionality
     */
    @Test
    public void testShortUrl() throws Exception
    {
        String shortUrl = nextUrlKey();
        String targetUrl = buildRelativeUrl("project", getProjectName(), "begin");

        ShortUrlAdminPage adminPage = ShortUrlAdminPage.beginAt(this);
        adminPage.createNewShortUrl(shortUrl, targetUrl);

        assertThat(adminPage.getUrlsFromGrid()).as("short Urls").containsEntry(shortUrl, targetUrl);
        String urlFromClipboard = adminPage.clickCopyToClipboard(shortUrl);
        String absoluteShortUrl = getAbsoluteShortUrl(shortUrl);
        assertEquals("Url copied to clipboard", absoluteShortUrl, urlFromClipboard);

        impersonate(USER);

        beginAt(absoluteShortUrl);

        assertEquals("destination containerPath", "/" + getProjectName(), getCurrentContainerPath());
        assertThat(getDriver().getCurrentUrl()).endsWith(targetUrl);
        assertEquals("Short URL should not avoid container permissions", 403, getResponseCode());
    }

    /**
     * Verify that shortUrls can target urls with tricky container paths and query parameters
     */
    @Test
    public void testTrickyCharacters() throws Exception
    {
        String trickyFolder = "Subfolder " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
        String folderPath = PROJECT_NAME + "/" + trickyFolder;

        _containerHelper.createSubfolder(getProjectName(), trickyFolder);

        String listName = "TrickyList";
        String textField = "TextField";
        String trickyValue = "Value" + TRICKY_CHARACTERS;

        Connection connection = createDefaultConnection();
        new IntListDefinition(listName, "key")
                .addField(new FieldDefinition(textField))
                .create(connection, folderPath)
                .insertRows(connection, List.of(
                        Map.of(textField, trickyValue),
                        Map.of(textField, "boring")
                ));

        DataRegionTable drt = goToManageLists().getGrid().viewListData(listName);
        assertEquals("Row count without filter", 2, drt.getDataRowCount());
        drt.setFilter(textField, "Equals", trickyValue);
        assertEquals("Row count with filter", 1, drt.getDataRowCount());

        String shortUrl = nextUrlKey();
        String targetUrl = getCurrentRelativeURL();

        ShortUrlAdminPage adminPage = ShortUrlAdminPage.beginAt(this);
        adminPage.createNewShortUrl(shortUrl, targetUrl);

        assertThat(adminPage.getUrlsFromGrid().keySet()).as("short Urls").contains(shortUrl);

        beginAt(getAbsoluteShortUrl(shortUrl));

        assertEquals("destination containerPath", "/" + folderPath, getCurrentContainerPath());
        assertTextPresent(trickyValue);
        drt = new DataRegionTable("query", this);
        assertEquals("Row count after navigating to short URL", 1, drt.getDataRowCount());
    }

    private String nextUrlKey()
    {
        return URL_PREFIX + count.getAndIncrement();
    }

    @NotNull
    private static String getAbsoluteShortUrl(String shortUrl)
    {
        return WebTestHelper.getBaseURL() + "/" + shortUrl + URL_SUFFIX;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
