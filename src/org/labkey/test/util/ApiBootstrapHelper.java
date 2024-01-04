package org.labkey.test.util;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GuestCredentialsProvider;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.ExtraSiteWrapper;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.core.login.SetPasswordForm;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.time.Duration;

/**
 * Bootstrap a server without the initial user validation done by {@link LabKeySiteWrapper#signIn()}
 * Requires that we are able to create the initial user via API.
 */
public class ApiBootstrapHelper extends ExtraSiteWrapper
{
    public ApiBootstrapHelper(WebDriver driver)
    {
        super(driver);
    }

    @Override
    public void signIn()
    {
        waitForStartup();
        if (!isInitialUserCreated())
        {
            createInitialUser();
            waitForBootstrap();
            new APIUserHelper(this).setInjectionDisplayName(PasswordUtil.getUsername());
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
        SimpleGetCommand command = new SimpleGetCommand("admin", "healthCheck");
        Exception lastException = null;

        Timer timer = new Timer(Duration.ofMinutes(5));
        Duration timeOfLastLog = null;
        do
        {
            if (timeOfLastLog == null || timer.elapsed().minus(timeOfLastLog).compareTo(Duration.ofSeconds(10)) > 0)
            {
                timeOfLastLog = timer.elapsed();
                StringBuilder msg = new StringBuilder("Waiting for server to finish starting up.");
                if (lastException != null)
                {
                    msg.append(" [").append(lastException.getMessage()).append("]");
                }
                TestLogger.log(msg.toString());
            }
            try
            {
                CommandResponse response = command.execute(cn, null);
                if ((Boolean) response.getParsedData().getOrDefault("healthy", false))
                {
                    return;
                }
            }
            catch (CommandException | IOException e)
            {
                lastException = e;
            }
            sleep(500);
        } while (!timer.isTimedOut());

        throw new RuntimeException("Server not done starting up.", lastException);
    }

    @LogMethod
    private boolean isInitialUserCreated()
    {
        Connection cn = new Connection(WebTestHelper.getBaseURL(), new GuestCredentialsProvider());
        SimplePostCommand command = new SimplePostCommand("admin", "configurationSummary");

        try
        {
            command.execute(cn, null);
            return false; // ConfigurationSummaryAction is accessible by guest only before initial user is created.
        }
        catch (IOException | CommandException e)
        {
            if (e instanceof CommandException && ((CommandException)e).getStatusCode() == 401)
            {
                return true;
            }
            else
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void createInitialUser()
    {
        beginAt(WebTestHelper.buildURL("login", "initialUser"));
        new SetPasswordForm(getDriver())
                .setEmail(PasswordUtil.getUsername())
                .setNewPassword(PasswordUtil.getPassword())
                .clickSubmit(90_000);
    }

    /**
     * TODO: Make this work so that we don't have to open a browser.
     * The POST just ends up redirecting to the normal initial user view.
     */
    @LogMethod
    private void createInitialUser_API()
    {
        Connection cn = createDefaultConnection();
        SimplePostCommand initialUserCommand = new SimplePostCommand("login", "initialUser");
        JSONObject params = new JSONObject();
        params.put("email", PasswordUtil.getUsername());
        params.put("password", PasswordUtil.getPassword());
        params.put("password2", PasswordUtil.getPassword());
        initialUserCommand.setJsonObject(params);

        try
        {
            initialUserCommand.execute(cn, null);
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
        SimpleGetCommand command = new SimpleGetCommand("admin", "startupStatus");
        Exception lastException = null;

        Timer timer = new Timer(Duration.ofMinutes(5));
        do
        {
            try
            {
                CommandResponse response = command.execute(cn, null);
                if ((Boolean) response.getParsedData().getOrDefault("startupComplete", false))
                {
                    return;
                }
            }
            catch (CommandException | IOException e)
            {
                lastException = e;
            }
        } while (!timer.isTimedOut());

        throw new RuntimeException("Server didn't finish starting.", lastException);
    }

}
