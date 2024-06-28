package org.labkey.test.stress;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.xmlbeans.XmlException;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.WhoAmICommand;
import org.labkey.test.credentials.Login;
import org.labkey.test.util.APITestHelper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ConcurrentApiTestHelper
{
    private final String _baseUrl;
    private final String _username;
    private final String _password;
    private final Connection _connection;

    public ConcurrentApiTestHelper(String baseUrl, String username, String password)
    {
        _baseUrl = baseUrl;
        _username = username;
        _password = password;
        _connection = new Connection(_baseUrl, _username, _password);
    }

    public ConcurrentApiTestHelper(String baseUrl, Login login)
    {
        this(baseUrl, login.getUsername(), login.getPassword());
    }

    public void start() throws IOException, CommandException
    {
        new WhoAmICommand().execute(_connection, null);
    }

    public static TestCaseType[] parseTests(File testFile)
    {
        try
        {
            List<APITestHelper.ApiTestCase> tests = new ArrayList<>();
            ApiTestsDocument doc = ApiTestsDocument.Factory.parse(testFile);

            return doc.getApiTests().getTestArray();
        }
        catch (IOException | XmlException e)
        {
            throw new RuntimeException("An unexpected error occurred", e);
        }
    }

    public List<Integer> runTests(File testFile)
    {
        List<Integer> results = new ArrayList<>();

        for (TestCaseType testCase : parseTests(testFile))
        {
            results.add(makeRequest(testCase.getUrl(), testCase.getType(), testCase.getFormData()));
        }
        return results;
    }

    private int makeRequest(String url, String type, String formData)
    {
        HttpUriRequest method;
        String requestUrl = _baseUrl + '/' + url.trim();

        if (type.equals("get"))
        {
            method = new HttpGet(requestUrl);
        }
        else
        {
            method = new HttpPost(requestUrl);
            if (type.equals("post"))
            {
                method.setEntity(new StringEntity(formData, ContentType.APPLICATION_JSON));
            }
            else if (type.equals("post_form"))
            {
                method.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            }
            else
            {
                throw new IllegalArgumentException("Unknown request type: " + type);
            }
        }

        Command command = new Command(method);
        try
        {
            CommandResponse response = command.execute(_connection);
            return response.getStatusCode();
        }
        catch (CommandException e)
        {
            return e.getStatusCode();
        }
        catch (IOException e)
        {
            return -1;
        }
    }

}

class Command extends org.labkey.remoteapi.Command<CommandResponse, HttpUriRequest>
{
    final HttpUriRequest _request;

    public Command(HttpUriRequest request)
    {
        super("", "");
        _request = request;
    }

    public CommandResponse execute(Connection connection) throws IOException, CommandException
    {
        return super.execute(connection, null);
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        return _request;
    }
}
