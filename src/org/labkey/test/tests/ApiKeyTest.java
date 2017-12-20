package org.labkey.test.tests;

import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.remoteapi.ApiKeyCredentialsProvider;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.GetSchemasCommand;
import org.labkey.remoteapi.query.GetSchemasResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyA.class})
public class ApiKeyTest extends BaseWebDriverTest
{
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
        clickUserMenuItem("API Keys");
        waitForText("API keys are used to authorize");
        clickButton("Generate session key", "session|");
        String apiKey = Locator.inputById("session-token").findElement(getDriver()).getAttribute("value");
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

        // Logout and using session key, which should fail
        signOut();
        cn = new Connection(WebTestHelper.getBaseURL(), new ApiKeyCredentialsProvider(apiKey));
        try
        {
            GetSchemasCommand cmd = new GetSchemasCommand();
            GetSchemasResponse resp = cmd.execute(cn, getProjectName());
            fail("Session key didn't invalidate after logout");
        }
        catch(CommandException e)
        {
            assertEquals("Wrong response for invalid session key", HttpStatus.SC_UNAUTHORIZED, e.getStatusCode());
            log("Success: command failed as expected.");
        }
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