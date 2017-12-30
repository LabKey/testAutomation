package org.labkey.test.tests;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.remoteapi.ApiKeyCredentialsProvider;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.GetQueriesCommand;
import org.labkey.remoteapi.query.GetQueriesResponse;
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

        // Logout and using session key, which should fail
        signOut();
        verifyInvalidAPIKey(apiKey, true);
    }

    private void verifyValidAPIKey(String apiKey) throws IOException
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), new ApiKeyCredentialsProvider(apiKey));
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

    private void verifyInvalidAPIKey(String apiKey, boolean isSessionKey) throws IOException
    {
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

        log("Verify API key for admin");
        String adminApiKey = generateAPIKey(_generatedApiKeys);
        verifyValidAPIKey(adminApiKey);
        log("Verify " + APIKEYS_TABLE + " table and columns for admin using api key");
        verifyAPIKeysTablePermissionWithRemoteAPI(adminApiKey);
        verifyAPIKeysTableColumnsWithRemoteAPI(adminApiKey);

        log("Verify API key expiration with keys that expires after 10 seconds");
        goToAdminConsole()
                .clickSiteSettings()
                .setApiKeyExpiration(CustomizeSitePage.KeyExpirationOptions.TEN_SECONDS)
                .save();
        String shortLivedApiKey = generateAPIKey(_generatedApiKeys);
        verifyValidAPIKey(shortLivedApiKey);
        sleep(11000); // wait for api key to expire
        verifyInvalidAPIKey(shortLivedApiKey, false);

        log("Verify revoked/deleted api keys");
        verifyValidAPIKey(adminApiKey);
        deleteAPIKeys(_generatedApiKeys);
        verifyInvalidAPIKey(adminApiKey, false);

        log("Verify " + APIKEYS_TABLE + " table not accessible for non admin");
        goToProjectHome();
        impersonateRoles("Reader");
        beginAt(getProjectName() + "/query-begin.view?#sbh-ssp-core");
        waitForElement(Locator.tagWithClass("span", "labkey-link").withText("Containers"));
        assertElementNotPresent(Locator.tagWithClass("span", "labkey-link").withText(APIKEYS_TABLE));
        stopImpersonating();
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

    private void verifyAPIKeysTableColumnsWithRemoteAPI(String apiKey) throws IOException
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), new ApiKeyCredentialsProvider(apiKey));
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
    }

    private void verifyAPIKeysTablePermissionWithRemoteAPI(String apiKey) throws IOException
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), new ApiKeyCredentialsProvider(apiKey));
        GetQueriesCommand cmdq = new GetQueriesCommand("core");
        try
        {
            GetQueriesResponse respq = cmdq.execute(cn, getProjectName());
            List<String> queryNames = respq.getQueryNames();
            assertTrue(APIKEYS_TABLE + " table should be available for admin user", queryNames.stream().anyMatch(APIKEYS_TABLE::equalsIgnoreCase));
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

        SelectRowsResponse response = null;
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