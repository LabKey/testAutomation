package org.labkey.test.tests;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.remoteapi.ApiKeyCredentialsProvider;
import org.labkey.remoteapi.BasicAuthCredentialsProvider;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsResponse;
import org.labkey.remoteapi.query.GetSchemasCommand;
import org.labkey.remoteapi.query.GetSchemasResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.core.admin.CustomizeSitePage;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyA.class})
public class ApiKeyTest extends BaseWebDriverTest
{
    private static final String APIKEYS_TABLE = "APIKeys";
    private static final String CRYPT_COLUMN = "crypt";
    private static final String API_USERNAME = "apikey";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
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
            List<String> schemaNames = resp.getSchemaNames();
            Set<String> missingSchemas = new CaseInsensitiveHashSet(Arrays.asList("pipeline", "lists", "core"));
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
            GetSchemasResponse resp = cmd.execute(cn, getProjectName());
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
        log("Verify existing active API key with disabled api key setting");
        goToAdminConsole()
                .clickSiteSettings()
                .setAllowApiKeys(false)
                .save();
        verifyValidAPIKey(apiKey);

        // skip testing api key expiration since it's already covered in unit test and 10 seconds expiration option is dev mode only

        log("Verify revoked/deleted api key");
        deleteAPIKeys(_generatedApiKeys);
        verifyInvalidAPIKey(apiKey);
    }

    @Test
    public void testAPIKeysPermissions() throws IOException
    {
        log("Verify " + APIKEYS_TABLE + " table is accessible for admin");
        verifyAPIKeysTablePresence(true);

        log("Verify " + CRYPT_COLUMN + " column is not accessible");
        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        GetQueryDetailsCommand cmdqd = new GetQueryDetailsCommand("core", APIKEYS_TABLE);
        try
        {
            GetQueryDetailsResponse respqd = cmdqd.execute(cn, getProjectName());
            CaseInsensitiveHashSet columnNames = new CaseInsensitiveHashSet();
            respqd.getColumns().forEach(col -> columnNames.add(col.getName()));
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
        Connection cn = createDefaultConnection(false);
        PostCommand generateAPIKeyCommand = new PostCommand("security", "createApiKey");
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
        Connection cn = createDefaultConnection(false);
        PostCommand generateAPIKeyCommand = new PostCommand("security", "createApiKey");
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
        beginAt(getProjectName() + "/query-begin.view?#sbh-ssp-core");
        waitForElement(Locator.tagWithClass("span", "labkey-link").withText("Containers"));
        Locator apiTableLoc = Locator.tagWithClass("span", "labkey-link").withText(APIKEYS_TABLE);
        assertEquals(isAdmin, isElementPresent(apiTableLoc));
    }

    private void deleteAPIKeys(List<Map<String, Object>> _generatedApiKeys) throws IOException
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
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
        goToAPIKeyPage();
        clickButton("Generate session key", "session|");
        return Locator.inputById("session-token").findElement(getDriver()).getAttribute("value");
    }

    private String generateAPIKey(List<Map<String, Object>> _generatedApiKeys) throws IOException
    {
        goToAPIKeyPage();
        clickButton("Generate API key", "apikey|");
        String apiKey = Locator.inputById("apikey-token").findElement(getDriver()).getAttribute("value");
        // get record
        _generatedApiKeys.add(getLastAPIKeyRecord());
        return apiKey;
    }

    protected Map<String, Object> getLastAPIKeyRecord() throws IOException
    {
        // Call the API with admin account (current user)
        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

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
        Integer rowId = (Integer)((JSONObject)record.get(keyField)).get("value");
        newRow.put(keyField, rowId);

        return newRow;
    }


    private void goToAPIKeyPage()
    {
        clickUserMenuItem("API Keys");
        waitForText("API keys are used to authorize");
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