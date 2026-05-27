/*
 * Copyright (c) 2017-2026 LabKey Corporation
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

import org.apache.hc.core5.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.ApiKeyCredentialsProvider;
import org.labkey.remoteapi.BasicAuthCredentialsProvider;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsResponse;
import org.labkey.remoteapi.query.GetSchemasCommand;
import org.labkey.remoteapi.query.ImportDataResponse;
import org.labkey.remoteapi.query.RowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.security.WhoAmICommand;
import org.labkey.remoteapi.security.WhoAmIResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.core.ApiKeyPanel;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.pages.core.admin.CustomizeSitePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.TestUser;
import org.labkey.test.util.URLBuilder;
import org.labkey.test.util.query.QueryApiHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.util.PermissionsHelper.EDITOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.PROJECT_ADMIN_ROLE;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class ApiKeyTest extends BaseWebDriverTest
{
    private static final String APIKEYS_TABLE = "APIKeys";
    private static final String CRYPT_COLUMN = "crypt";
    private static final String API_USERNAME = "apikey";
    private static final String LIST_NAME = "InsertTestList";
    private static final String LIST_VALUE = "value";
    private static final TestUser EDITOR_USER = new TestUser("editor@apikey.test");
    private static final AtomicInteger valueCount = new AtomicInteger();

    @BeforeClass
    public static void setupProject() throws Exception
    {
        ApiKeyTest init = getCurrentTest();

        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(false, EDITOR_USER);
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);

        EDITOR_USER.create(this)
                .setInitialPassword()
                .addPermission(EDITOR_ROLE, getProjectName());

        new IntListDefinition(LIST_NAME, "Key")
            .addField(new FieldDefinition(LIST_VALUE))
            .create(createDefaultConnection(), getProjectName());
    }

    @Test
    public void testSessionKey() throws IOException
    {
        log("Get session key and use it in a command.");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowSessionKeys(true)
                .save();

        String apiKey = generateSessionKey();

        verifyValidAPIKey(createApiKeyConnection(apiKey));
        verifySessionKeyCsrf(createApiKeyConnection(apiKey));

        log("Verify session key remains valid if key generation is turned off");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowSessionKeys(false)
                .save();
        verifyValidAPIKey(createApiKeyConnection(apiKey));
        verifySessionKeyCsrf(createApiKeyConnection(apiKey));

        signOut();
        log("Verify that logging out invalidates session keys");
        verifyInvalidAPIKey(createApiKeyConnection(apiKey), true);
        simpleSignIn();
        log("Verify that session keys remain invalid after logging back in");
        verifyInvalidAPIKey(createApiKeyConnection(apiKey), true);
    }

    @Test
    public void testNonAdminUser() throws IOException
    {
        log("Ensure apiKey generation is enabled.");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(true)
                .setApiKeyExpiration(CustomizeSitePage.KeyExpirationOptions.ONE_WEEK)
                .save();
        signOut();

        log("Log in as non-admin user.");
        signIn(EDITOR_USER.getEmail());
        String keyDescription = "Key for editing";
        String apiKey = generateAPIKey(keyDescription);
        verifyValidAPIKey(createApiKeyConnection(apiKey), EDITOR_USER.getEmail());

        QueryGrid grid = new QueryGrid.QueryGridFinder(getDriver()).waitFor();
        int beforeDeleteCount = grid.getRecordCount();
        assertFalse("Row with description not found", grid.getRowMapByLabel("Description", keyDescription).isEmpty());
        grid = deleteAPIKeyViaUI();
        assertEquals("Number of keys after UI deletion not as expected", beforeDeleteCount - 1, grid.getRecordCount());
        verifyInvalidAPIKey(createApiKeyConnection(apiKey), false);
    }

    @Test
    public void testStandardApiKey() throws IOException
    {
        List<Map<String, Object>> _generatedApiKeys = new ArrayList<>();

        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(true)
                .setApiKeyExpiration(CustomizeSitePage.KeyExpirationOptions.ONE_WEEK)
                .save();

        String apiKey = generateAPIKeyAndRecord(_generatedApiKeys);
        log("Verify active API key via api authentication");
        verifyValidAPIKey(createApiKeyConnection(apiKey));
        log("Verify active API key via basic authentication");
        verifyValidAPIKey(createBasicAuthConnection(apiKey));

        log("Generate two other keys for use in testing deletion.");
        generateAPIKey(null);
        generateAPIKey(null);
        QueryGrid grid = new QueryGrid.QueryGridFinder(getDriver()).waitFor();
        int beforeDeleteCount = grid.getRecordCount();
        grid = deleteAPIKeyViaUI();
        assertEquals("Number of keys after UI deletion not as expected", beforeDeleteCount - 1, grid.getRecordCount());

        log("Verify existing active API key with disabled api key setting");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(false)
                .save();
        verifyValidAPIKey(createApiKeyConnection(apiKey));

        log("Verify key deletion via UI with disabled api key generation works.");
        grid = deleteAPIKeyViaUI();
        assertEquals("Number of keys after UI deletion not as expected", beforeDeleteCount - 2, grid.getRecordCount());

        // skip testing api key expiration since it's already covered in unit test and 10 seconds expiration option is dev mode only

        log("Verify revoked/deleted api key");
        deleteAPIKeys(_generatedApiKeys);
        verifyInvalidAPIKey(createApiKeyConnection(apiKey), false);
    }

    /*
        Regression coverage for Secure Issue 51637: Invalidate sessions when their API key becomes invalid
     */
    @Test
    public void testSessionInvalidatesAfterAPIKeyChange() throws IOException
    {
        List<Map<String, Object>> _generatedApiKeys = new ArrayList<>();

        log("Generating an apikey which expire in one week");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(true)
                .setApiKeyExpiration(CustomizeSitePage.KeyExpirationOptions.ONE_WEEK)
                .save();

        String apiKey1 = generateAPIKeyAndRecord(_generatedApiKeys);
        Connection cn = createApiKeyConnection(apiKey1);
        verifyValidAPIKey(cn);

        log("Deleting the apikey");
        deleteAPIKeys(_generatedApiKeys);

        /*
            Regression coverage for Issue 52004: Session associated with APIKey can used even after APIKey is deleted.
         */
        log("Verifying the session associated with deleted apikey is invalid");
        verifyInvalidAPIKey(cn, false);

        log("Verifying that new connection cannot be created after apikey is deleted");
        verifyInvalidAPIKey(createApiKeyConnection(apiKey1), false);

        log("Generating the apikey which expires in ten seconds");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(true)
                .setApiKeyExpiration(CustomizeSitePage.KeyExpirationOptions.TEN_SECONDS)
                .save();

        log("Verify apikey expiration");
        goToExternalToolPage();
        String apikey2 = ApiKeyPanel.panelFinder(getDriver()).find().generateApiKey();

        log("Verify apikey can be used before expiring");
        verifyValidAPIKey(createApiKeyConnection(apikey2));

        sleep(10000); // Wait for apikey to expire

        log("Verify apikey cannot be used after it has expired");
        verifyInvalidAPIKey(createApiKeyConnection(apikey2), false);
    }

    @Test
    public void testApiKeysImpersonation() throws IOException
    {
        log("Verify key table and generation are not available while impersonating");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(true)
                .setApiKeyExpiration(CustomizeSitePage.KeyExpirationOptions.ONE_WEEK)
                .save()
                .clickSiteSettings()
                .setAllowSessionKeys(true)
                .save();
        List<Map<String, Object>> _generatedApiKeys = new ArrayList<>();
        generateAPIKeyAndRecord(_generatedApiKeys);
        goToProjectHome();
        impersonate(EDITOR_USER.getEmail());
        goToExternalToolPage();
        List<WebElement> banners = Locator.byClass(BootstrapLocators.BannerType.WARNING.getCss()).findElements(this.getDriver());
        assertEquals("Number of warning banners not as expected", 1, banners.size());
        assertEquals("API and session key generation warning not as expected", "API and session key generation is not available while impersonating.", banners.getFirst().getText());
    }

    @Test
    public void testAPIKeysTablePermissions() throws IOException
    {
        log("Verify " + APIKEYS_TABLE + " table is accessible for admin");
        verifyAPIKeysTablePresence(true);

        log("Verify " + CRYPT_COLUMN + " column is not accessible");
        Connection cn = WebTestHelper.getRemoteApiConnection();
        GetQueryDetailsCommand cmdqd = new GetQueryDetailsCommand("core", APIKEYS_TABLE);
        try
        {
            GetQueryDetailsResponse respqd = cmdqd.execute(cn, getProjectName());
            Set<String> columnNames = new HashSet<>();
            respqd.getColumns().forEach(col -> columnNames.add(col.getName().toLowerCase()));
            assertFalse(CRYPT_COLUMN + " column shouldn't be accessible", columnNames.contains(CRYPT_COLUMN));
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Response: " + e.getStatusCode(), e);
        }

        log("Verify " + APIKEYS_TABLE + " table is not accessible for non site-admin");
        goToProjectHome();
        impersonateRoles(PROJECT_ADMIN_ROLE);
        verifyAPIKeysTablePresence(false);
    }

    @Test
    public void testApiKeyDisabled() throws IOException
    {
        log("Verify generating API keys would fail when setting is disabled");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(false)
                .setAllowSessionKeys(true)
                .save();
        Connection cn = createDefaultConnection();
        SimplePostCommand generateAPIKeyCommand = new SimplePostCommand("security", "createApiKey");
        generateAPIKeyCommand.setParameters(new HashMap<>(Maps.of("type", "apikey")));
        try
        {
            generateAPIKeyCommand.execute(cn, "/");
            fail("Shouldn't be able to generate api key when setting is disabled");
        }
        catch (CommandException e)
        {
            log(e.getMessage());
            assertEquals("Wrong response for invalid api generation action", HttpStatus.SC_NOT_FOUND, e.getStatusCode());
            log("Success: command failed as expected.");
        }
    }

    @Test
    public void testSessionKeyDisabled() throws IOException
    {
        log("Verify generating API keys would fail when setting is disabled");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(true)
                .setAllowSessionKeys(false)
                .save();
        Connection cn = createDefaultConnection();
        SimplePostCommand generateAPIKeyCommand = new SimplePostCommand("security", "createApiKey");
        generateAPIKeyCommand.setParameters(new HashMap<>(Maps.of("type", "session")));
        try
        {
            generateAPIKeyCommand.execute(cn, "/");
            fail("Shouldn't be able to generate session key when setting is disabled");
        }
        catch (CommandException e)
        {
            log(e.getMessage());
            assertEquals("Wrong response for invalid api generation action", HttpStatus.SC_NOT_FOUND, e.getStatusCode());
            log("Success: command failed as expected.");
        }
    }

    private void verifyValidAPIKey(Connection connection) throws IOException
    {
        verifyValidAPIKey(connection, PasswordUtil.getUsername());
    }

    private void verifyValidAPIKey(Connection connection, String userEmail) throws IOException
    {
        try
        {
            WhoAmIResponse whoAmI = new WhoAmICommand().execute(connection, null);
            assertEquals("Connection user", userEmail, whoAmI.getEmail());

            QueryApiHelper queryApiHelper = new QueryApiHelper(connection, getProjectName(), "lists", LIST_NAME);

            // ImportData doesn't return auth challenge. Make sure it works
            ImportDataResponse importResponse = queryApiHelper.importData(LIST_VALUE + "\nvalue" + valueCount.get());
            valueCount.incrementAndGet();
            assertEquals("Rows imported", 1, importResponse.getRowCount());

            RowsResponse saveResponse = queryApiHelper.insertRows(List.of(Map.of(LIST_VALUE, "value" + valueCount.get())));
            valueCount.incrementAndGet();
            assertEquals("Rows inserted", 1, saveResponse.getRowsAffected());

            SelectRowsResponse selectResponse = queryApiHelper.selectRows();
            assertEquals("Total rows", valueCount.get(), selectResponse.getRowCount());

            whoAmI = new WhoAmICommand().execute(connection, null);
            assertEquals("Connection user", userEmail, whoAmI.getEmail());
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Response: " + e.getStatusCode(), e);
        }
    }

    private void verifySessionKeyCsrf(Connection connection) throws IOException
    {
        try
        {
            SimplePostCommand cmd = new SimplePostCommand("login", "csrf");
            CommandResponse resp = cmd.execute(connection, getProjectName());
            assertTrue("CSRF success", resp.getProperty("success"));

            WhoAmIResponse whoAmI = new WhoAmICommand().execute(connection, null);
            Assert.assertNotEquals("API CSRF", WebTestHelper.getCookies(PasswordUtil.getUsername()).get(Connection.X_LABKEY_CSRF).getValue(), whoAmI.getCSRF());
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Response: " + e.getStatusCode(), e);
        }
    }

    private Connection createApiKeyConnection(String apiKey)
    {
        return new Connection(WebTestHelper.getBaseURL(), new ApiKeyCredentialsProvider(apiKey));
    }

    private Connection createBasicAuthConnection(String apiKey)
    {
        return new Connection(WebTestHelper.getBaseURL(), new BasicAuthCredentialsProvider(API_USERNAME, apiKey));
    }

    private void verifyInvalidAPIKey(Connection connection, boolean isSessionKey) throws IOException
    {
        try
        {
            GetSchemasCommand cmd = new GetSchemasCommand();
            cmd.execute(connection, getProjectName());
            if (isSessionKey)
                fail("Session key didn't invalidate after logout");
            else
                fail("API key should no longer be valid");
        }
        catch (CommandException e)
        {
            assertEquals("Wrong response for invalid " + (isSessionKey ? "session" : "API") + " key", HttpStatus.SC_UNAUTHORIZED, e.getStatusCode());
            log("Success: command failed as expected.");
        }
    }

    private void verifyAPIKeysTablePresence(boolean isAdmin)
    {
        beginAt(new URLBuilder("query", "begin", getProjectName()).setFragment("sbh-ssp-core").buildURL());
        waitForElement(Locator.tagWithClass("span", "labkey-link").withText("Containers"));
        Locator apiTableLoc = Locator.tagWithClass("span", "labkey-link").withText(APIKEYS_TABLE);
        assertEquals(isAdmin, isElementPresent(apiTableLoc));
    }

    private QueryGrid deleteAPIKeyViaUI()
    {
        goToExternalToolPage();
        waitForText("API keys are used to authorize");
        QueryGrid grid = new QueryGrid.QueryGridFinder(getDriver()).waitFor();
        grid.selectRow(0, true);
        grid.getGridBar().clickButton("Delete");
        ModalDialog dialog = new ModalDialog.ModalDialogFinder(this.getDriver()).find();
        dialog.dismiss("Yes, Delete");
        return new QueryGrid.QueryGridFinder(getDriver()).waitFor();
    }

    private void deleteAPIKeys(List<Map<String, Object>> _generatedApiKeys) throws IOException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        DeleteRowsCommand cmddel = new DeleteRowsCommand("core", APIKEYS_TABLE);
        cmddel.setRows(_generatedApiKeys);
        try
        {
            cmddel.execute(cn, getProjectName());
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Response: " + e.getStatusCode(), e);
        }
    }

    private String generateSessionKey()
    {
        goToExternalToolPage();
        return ApiKeyPanel.panelFinder(getDriver()).find().generateSessionKey();
    }

    private String generateAPIKey(@Nullable String description)
    {
        goToExternalToolPage();
        return ApiKeyPanel.panelFinder(getDriver()).find().generateApiKey(description);
    }

    private String generateAPIKeyAndRecord(List<Map<String, Object>> _generatedApiKeys) throws IOException
    {
        String apiKey = generateAPIKey(null);
        // get record
        _generatedApiKeys.add(getLastAPIKeyRecord());
        return apiKey;
    }

    protected Map<String, Object> getLastAPIKeyRecord() throws IOException
    {
        // Call the API with admin account (current user)
        Connection cn = WebTestHelper.getRemoteApiConnection();

        SelectRowsCommand cmd = new SelectRowsCommand("core", APIKEYS_TABLE);
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("RowId"));
        cmd.setSorts(Arrays.asList(new Sort("RowId", Sort.Direction.DESCENDING)));

        SelectRowsResponse response;
        try
        {
            response = cmd.execute(cn, "/");
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Response: " + e.getStatusCode(), e);
        }

        String keyField = "RowId";
        Map<String, Object> record = response.getRows().getFirst();
        Map<String, Object> newRow = new HashMap<>();
        Integer rowId = (Integer) ((Map<String, Object>) record.get(keyField)).get("value");
        newRow.put(keyField, rowId);

        return newRow;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ApiKeyTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
