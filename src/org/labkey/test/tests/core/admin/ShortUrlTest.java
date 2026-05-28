/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.tests.core.admin;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.admin.ShortUrlAdminPage;
import org.labkey.test.pages.admin.UpdateShortUrlPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.labkey.test.WebTestHelper.buildRelativeUrl;

@Category({Daily.class})
public class ShortUrlTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ShortUrlTest Project";
    private static final String URL_PREFIX = "surl_test_";
    private static final String URL_SUFFIX = ".url";
    private static final String USER = "shorturl_user@shorturltest.test";
    private static final String APP_ADMIN = "shorturl_appadmin@shorturltest.test";
    private static int count = 0;

    @Override
    protected void doCleanup(boolean afterTest)
    {
        ShortUrlAdminPage.beginAtFiltered(this, URL_PREFIX).deleteAll();

        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, USER, APP_ADMIN);
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
        _userHelper.createUser(APP_ADMIN);
        new ApiPermissionsHelper(this).addUserAsAppAdmin(APP_ADMIN);
        _containerHelper.createProject(getProjectName());
    }

    /**
     * Verify basic shortUrl functionality
     *  - Creation
     *  - Copy to clipboard grid button
     *  - Usability by other users
     *  - URL target's security is respected
     *  - Updating URL
     *  - Deleting URL
     */
    @Test
    public void testShortUrl() throws Exception
    {
        String shortUrl = nextUrlKey();
        String targetUrl = WebTestHelper.getContextPath() + buildRelativeUrl("project", getProjectName(), "begin");

        ShortUrlAdminPage adminPage = ShortUrlAdminPage.beginAt(this);
        adminPage.createNewShortUrl(shortUrl, targetUrl);

        assertThat(adminPage.getUrlsFromGrid()).as("short Urls").containsEntry(shortUrl, targetUrl);
        String absoluteShortUrl = getAbsoluteShortUrl(shortUrl);

        String urlFromClipboard = adminPage.clickCopyToClipboard(shortUrl);
        assertEquals("Url copied to clipboard", absoluteShortUrl, urlFromClipboard);

        log("Test shortUrl as another user");
        doAsUser(USER, () ->
        {
            beginAt(absoluteShortUrl);

            assertEquals("destination containerPath", "/" + getProjectName(), getCurrentContainerPath());
            assertThat(getDriver().getCurrentUrl()).endsWith(targetUrl);
            assertEquals("Short URL should not avoid container permissions", 403, getResponseCode());
        });

        log("Update shortUrl target");
        String updatedTargetUrl = WebTestHelper.getContextPath() + buildRelativeUrl("project", "home", "begin");

        adminPage = ShortUrlAdminPage.beginAt(this);
        adminPage.createNewShortUrl(shortUrl, updatedTargetUrl);

        log("Test updated shortUrl");
        doAsUser(USER, () ->
        {
            beginAt(absoluteShortUrl);

            assertEquals("destination containerPath", "/home", getCurrentContainerPath());
            assertThat(getDriver().getCurrentUrl()).endsWith(updatedTargetUrl);
            assertEquals("Home project permission", 200, getResponseCode());
        });

        log("Delete shortUrl");
        UpdateShortUrlPage.beginAt(this, shortUrl).clickDeleteAndConfirm();

        log("Verify deleted shortUrl");
        beginAt(absoluteShortUrl);
        assertThat(getDriver().getCurrentUrl()).endsWith(shortUrl + URL_SUFFIX);
        assertEquals("Short URL should not avoid container permissions", 404, getResponseCode());
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
        String targetUrl = WebTestHelper.getContextPath() + getCurrentRelativeURL();

        ShortUrlAdminPage adminPage = ShortUrlAdminPage.beginAt(this);
        adminPage.createNewShortUrl(shortUrl, targetUrl);

        assertThat(adminPage.getUrlsFromGrid().keySet()).as("short Urls").contains(shortUrl);

        beginAt(getAbsoluteShortUrl(shortUrl));

        assertEquals("destination containerPath", "/" + folderPath, getCurrentContainerPath());
        assertTextPresent(trickyValue);
        drt = new DataRegionTable("query", this);
        assertEquals("Row count after navigating to short URL", 1, drt.getDataRowCount());
    }

    @Test
    public void testShortUrlPermissions()
    {
        String shortUrl_a = nextUrlKey();
        String shortUrl_b = nextUrlKey();
        String targetUrl1 = buildRelativeUrl("project", getProjectName(), "begin");
        String targetUrl2 = buildRelativeUrl("project", "shared", "begin");

        log("Create short URL as primary site user");
        ShortUrlAdminPage.beginAt(this)
                .createNewShortUrl(shortUrl_a, targetUrl1);

        doAsUser(APP_ADMIN, () ->
        {
            ShortUrlAdminPage adminPage = ShortUrlAdminPage.beginAt(this);

            log("Create short URL as app admin");
            adminPage.submitShortUrl(shortUrl_b, targetUrl2);

            log("As app admin, update shortUrl created by another user");
            adminPage.submitShortUrl(shortUrl_a, targetUrl2);

            // Issue #52485 "App admins can create and edit shorturls but can't view them" (but now they can!)
            verifyShortUrlsInGrid(Map.of(shortUrl_a, targetUrl2, shortUrl_b, targetUrl2));

            // Edit an existing row, setting it back to the original target
            DataRegionTable table = new DataRegionTable("ShortURL", getDriver());
            table.setFilter("ShortURL", "Equals", shortUrl_a);
            table.clickEditRow(0);
            setFormElement(Locator.name("fullURL"), targetUrl1);
            clickButton("Update");

            // Issue #52485 "App admins can create and edit shorturls but can't view them" (but now they can!)
            verifyShortUrlsInGrid(Map.of(shortUrl_a, targetUrl1, shortUrl_b, targetUrl2));

            // Delete an existing row
            table = new DataRegionTable("ShortURL", getDriver());
            table.setFilter("ShortURL", "Equals", shortUrl_a);
            table.checkCheckbox(0);
            table.deleteSelectedRows();
            assertTextNotPresent(targetUrl1);
        });

        // Double-verify the single entry to reuse the same validation method
        verifyShortUrlsInGrid(Map.of(shortUrl_b, targetUrl2));
    }

    /** Map of short URL to target URLs to expect */
    private void verifyShortUrlsInGrid(Map<String, String> urlMap)
    {
        Assertions.assertThat(ShortUrlAdminPage.beginAt(this).getUrlsFromGrid())
                .as("short URLs")
                .containsAllEntriesOf(urlMap);
    }

    private String nextUrlKey()
    {
        return URL_PREFIX + count++;
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
