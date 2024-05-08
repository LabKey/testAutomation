/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.ApiKeyCredentialsProvider;
import org.labkey.remoteapi.BasicAuthCredentialsProvider;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsResponse;
import org.labkey.remoteapi.query.GetSchemasCommand;
import org.labkey.remoteapi.query.GetSchemasResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.pages.core.admin.CustomizeSitePage;
import org.labkey.test.util.Maps;
import org.labkey.test.util.TestUser;
import org.labkey.test.util.URLBuilder;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class ApiKeyTest extends BaseWebDriverTest
{
    private static final String APIKEYS_TABLE = "APIKeys";
    private static final String CRYPT_COLUMN = "crypt";
    private static final String API_USERNAME = "apikey";
    private static final TestUser EDITOR_USER = new TestUser("editor@apikey.test");

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(false, EDITOR_USER);
    }

    @BeforeClass
    public static void setupProject()
    {
        ApiKeyTest init = (ApiKeyTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);

        EDITOR_USER.create(this)
                .setInitialPassword()
                .addPermission("Editor", getProjectName());
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

        verifyValidAPIKey(apiKey);

        log("Verify session key remains valid if key generation is turned off");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowSessionKeys(false)
                .save();
        verifyValidAPIKey(apiKey);

        signOut();
        log("Verify that logging out invalidates session keys");
        verifyInvalidAPIKey(apiKey);
        simpleSignIn();
        log("Verify that session keys remain invalid after logging back in");
        verifyInvalidAPIKey(apiKey);
    }

    private void verifyValidAPIKey(String apiKey) throws IOException
    {
        verifyValidAPIKey(apiKey, false);
    }

    private void verifyValidAPIKey(String apiKey, boolean basicAuth) throws IOException
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), basicAuth ? new BasicAuthCredentialsProvider(API_USERNAME, apiKey) : new ApiKeyCredentialsProvider(apiKey));
        try
        {
            GetSchemasCommand cmd = new GetSchemasCommand();
            GetSchemasResponse resp = cmd.execute(cn, getProjectName());
            List<String> schemaNames = resp.getSchemaNames().stream().map(String::toLowerCase).collect(Collectors.toList());
            Set<String> missingSchemas = new HashSet<>(Arrays.asList("pipeline", "lists", "core"));
            missingSchemas.removeAll(schemaNames);
            assertTrue("Some expected schemas missing. Schemas missing: " + missingSchemas, missingSchemas.isEmpty());
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Response: " + e.getStatusCode(), e);
        }
    }

    private void verifyInvalidAPIKey(String apiKey) throws IOException
    {
        boolean isSessionKey = !apiKey.startsWith(API_USERNAME);
        Connection cn = new Connection(WebTestHelper.getBaseURL(), new ApiKeyCredentialsProvider(apiKey));
        try
        {
            GetSchemasCommand cmd = new GetSchemasCommand();
            cmd.execute(cn, getProjectName());
            if (isSessionKey)
                fail("Session key didn't invalidate after logout");
            else
                fail("API key should no longer be valid");
        }
        catch(CommandException e)
        {
            assertEquals("Wrong response for invalid " + (isSessionKey ? "session" : "API") + " key", HttpStatus.SC_UNAUTHORIZED, e.getStatusCode());
            log("Success: command failed as expected.");
        }
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
        signIn(EDITOR_USER.getEmail(), EDITOR_USER.getPassword());
        String apiKey = generateAPIKey();
        verifyValidAPIKey(apiKey);

        QueryGrid grid = new QueryGrid.QueryGridFinder(getDriver()).waitFor();
        int beforeDeleteCount = grid.getRecordCount();
        grid = deleteAPIKeyViaUI();
        assertEquals("Number of keys after UI deletion not as expected", beforeDeleteCount-1, grid.getRecordCount());
        verifyInvalidAPIKey(apiKey);
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

        String apiKey = generateAPIKey(_generatedApiKeys);
        log("Verify active API key via api authentication");
        verifyValidAPIKey(apiKey);
        log("Verify active API key via basic authentication");
        verifyValidAPIKey(apiKey, true);

        log("Generate two other keys for use in testing deletion.");
        generateAPIKey();
        generateAPIKey();
        QueryGrid grid = new QueryGrid.QueryGridFinder(getDriver()).waitFor();
        int beforeDeleteCount = grid.getRecordCount();
        grid = deleteAPIKeyViaUI();
        assertEquals("Number of keys after UI deletion not as expected", beforeDeleteCount-1, grid.getRecordCount());

        log("Verify existing active API key with disabled api key setting");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(false)
                .save();
        verifyValidAPIKey(apiKey);

        log("Verify key deletion via UI with disabled api key generation works.");
        grid = deleteAPIKeyViaUI();
        assertEquals("Number of keys after UI deletion not as expected", beforeDeleteCount-2, grid.getRecordCount());

        // skip testing api key expiration since it's already covered in unit test and 10 seconds expiration option is dev mode only

        log("Verify revoked/deleted api key");
        deleteAPIKeys(_generatedApiKeys);
        verifyInvalidAPIKey(apiKey);
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
        generateAPIKey(_generatedApiKeys);
        goToProjectHome();
        impersonate(EDITOR_USER.getEmail());
        goToExternalToolPage();
        List<WebElement> banners = Locator.byClass(BootstrapLocators.BannerType.WARNING.getCss()).findElements(this.getDriver());
        assertEquals("Number of warning banners not as expected", 2, banners.size());
        assertEquals("API key generation warning not as expected", "API key generation is not available while impersonating.", banners.get(0).getText());
        assertEquals("Session key generation warning not as expected", "Session key generation is not available while impersonating.", banners.get(1).getText());
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
        impersonateRoles("Project Administrator");
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
        waitForText("API keys are used to authorize");
        clickButton("Generate Session Key", 0);
        waitForFormElementToNotEqual(Locator.inputByNameContaining("session_token"), "");
        return Locator.inputByNameContaining("session_token").findElement(getDriver()).getAttribute("value");
    }

    private String generateAPIKey()
    {
        goToExternalToolPage();
        clickButton("Generate API Key", 0);
        waitForFormElementToNotEqual(Locator.inputByNameContaining("apikey_token"), "");
        return Locator.inputByNameContaining("apikey_token").findElement(getDriver()).getAttribute("value");
    }

    private String generateAPIKey(List<Map<String, Object>> _generatedApiKeys) throws IOException
    {
        String apiKey = generateAPIKey();
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
        Map<String, Object> record = response.getRows().get(0);
        Map<String, Object> newRow = new HashMap<>();
        Integer rowId = (Integer)((Map<String, Object>)record.get(keyField)).get("value");
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
