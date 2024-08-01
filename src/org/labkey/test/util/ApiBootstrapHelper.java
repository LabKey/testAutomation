package org.labkey.test.util;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GuestCredentialsProvider;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.labkey.test.WebTestHelper.getBaseURL;

/**
 * Bootstrap a server without the initial user validation done by {@link LabKeySiteWrapper#signIn()}
 */
public class ApiBootstrapHelper
{
    public ApiBootstrapHelper() { }

    public void signIn()
    {
        waitForStartup();
        if (!isInitialUserCreated())
        {
            createInitialUser();
            waitForBootstrap();
            new APIUserHelper(this::createDefaultConnection).setInjectionDisplayName(PasswordUtil.getUsername());
        }
        else
        {
            TestLogger.log("Initial user is already created. Nothing to do.");
        }
    }

    @LogMethod
    private void waitForStartup()
    {
        Connection cn = new Connection(getBaseURL(), new GuestCredentialsProvider());
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
            WebDriverWrapper.sleep(500);
        } while (!timer.isTimedOut());

        throw new RuntimeException("Server not done starting up.", lastException);
    }

    @LogMethod
    private boolean isInitialUserCreated()
    {
        Connection cn = new Connection(getBaseURL(), new GuestCredentialsProvider());
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

    @LogMethod
    private void createInitialUser()
    {
        Connection cn = new Connection(getBaseURL(), new GuestCredentialsProvider());
        CreateInitialUserCommand initialUserCommand = new CreateInitialUserCommand();

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
        /*
            Waiting for search service to boot up
            Issue 50601: PDF indexing is slow on first file after server startup on Windows
         */
        SimpleGetCommand searchCmd = new SimpleGetCommand("search", "json");
        searchCmd.setParameters(Map.of("q", "pinging to check server is started", "scope", "All"));
        Exception lastException = null;

        Timer timer = new Timer(Duration.ofMinutes(5));
        do
        {
            try
            {
                CommandResponse response = command.execute(cn, null);
                CommandResponse searchResponse = searchCmd.execute(cn, "/");
                if ((Boolean) response.getParsedData().getOrDefault("startupComplete", false) && searchResponse.getStatusCode() == 200)
                {
                    return;
                }
            }
            catch (CommandException | IOException e)
            {
                lastException = e;
            }
        }
        while (!timer.isTimedOut());

        if (!(lastException.getCause() instanceof CommandException && lastException.getMessage().contains("Not Found")))
            throw new RuntimeException("Server didn't finish starting.", lastException);
    }

    public Connection createDefaultConnection()
    {
        return new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }
}

class CreateInitialUserCommand extends PostCommand<CommandResponse>
{
    public CreateInitialUserCommand()
    {
        super("login", "initialUser");
    }

    protected List<BasicNameValuePair> getPostData()
    {
        List<BasicNameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("email", PasswordUtil.getUsername()));
        postData.add(new BasicNameValuePair("password", PasswordUtil.getPassword()));
        postData.add(new BasicNameValuePair("password2", PasswordUtil.getPassword()));

        return postData;
    }

    @Override
    protected HttpPost createRequest(URI uri)
    {
        // InitialUserAction is not a real API action, so we POST form data instead of JSON
        HttpPost request = new HttpPost(uri);
        request.setEntity(new UrlEncodedFormEntity(getPostData()));
        return request;
    }
}
