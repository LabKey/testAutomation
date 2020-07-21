package org.labkey.test.util;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GuestCredentialsProvider;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({})
public class QuickBootstrapPseudoTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @Override
    public void signIn()
    {
        waitForStartup();
        if (!isInitialUserCreated())
        {
            createInitialUser();
            waitForBootstrap();
        }
        else
        {
            TestLogger.log("Initial user is already created. Nothing to do.");
        }
    }

    @LogMethod
    private void waitForStartup()
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), new GuestCredentialsProvider());
        Command<?> command = new Command<>("admin", "healthCheck");
        Exception lastException = null;

        Timer timer = new Timer(Duration.ofMinutes(5));
        do
        {
            try
            {
                CommandResponse response = command.execute(cn, null);
                if (Boolean.TRUE.equals(response.getParsedData().get("healthy")))
                {
                    return;
                }
            }
            catch (CommandException | IOException e)
            {
                lastException = e;
            }
        } while (!timer.isTimedOut());

        throw new RuntimeException("Server not responsive.", lastException);
    }

    @LogMethod
    private boolean isInitialUserCreated()
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), new GuestCredentialsProvider());
        PostCommand<?> command = new PostCommand<>("admin", "configurationSummary");

        try
        {
            command.execute(cn, null);
            return false; // ConfigurationSummaryAction is accessible by guest only before initial user is created.
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (CommandException e)
        {
            if (e.getStatusCode() == 403)
            {
                return true;
            }
            else
            {
                throw new RuntimeException(e);
            }
        }
    }

    @LogMethod
    private void createInitialUser()
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), new GuestCredentialsProvider());
        PostCommand<?> command = new PostCommand<>("login", "initialUser");
        Map<String, Object> params = new HashMap<>();
        params.put("email", PasswordUtil.getUsername());
        params.put("password", PasswordUtil.getPassword());
        params.put("password2", PasswordUtil.getPassword());
        command.setParameters(params);

        try
        {
            command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to create initial user.", e);
        }
    }

    @LogMethod
    private void waitForBootstrap()
    {
        Connection cn = WebTestHelper.getRemoteApiConnection(false);
        Command<?> command = new Command<>("admin", "startupStatus");
        Exception lastException = null;

        Timer timer = new Timer(Duration.ofMinutes(5));
        do
        {
            try
            {
                CommandResponse response = command.execute(cn, null);
            }
            catch (CommandException | IOException e)
            {
                lastException = e;
            }
        } while (!timer.isTimedOut());

        if (lastException != null)
        {
            throw new RuntimeException("Server didn't finish starting.", lastException);
        }
    }

    @Test
    public void testNothing()
    {
        // Do nothing
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
