package org.labkey.test.stress;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.labkey.query.xml.TestCaseType;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

class ApiTestCommand extends Command<CommandResponse, HttpUriRequest>
{
    private final String _url;
    private final String _type;
    private final String _formData;

    public ApiTestCommand(String url, String type, String formData)
    {
        super("", "");
        _url = url.trim();
        _type = type.trim();
        _formData = StringUtils.trimToEmpty(formData);
    }

    public ApiTestCommand(Activity.RequestParams requestParams)
    {
        this(requestParams.getUrl(), requestParams.getType(), requestParams.getFormData());
    }

    public ApiTestCommand(TestCaseType requestParams)
    {
        this(requestParams.getUrl(), requestParams.getType(), requestParams.getFormData());
    }

    public CommandResponse execute(Connection connection) throws IOException, CommandException
    {
        return execute(connection, null);
    }

    @Override
    public CommandResponse execute(Connection connection, String folderPath) throws IOException, CommandException
    {
        // Execute the command. Throws CommandException for error responses.
        try (Response response = _execute(connection, folderPath))
        {
            // For non-streaming Commands, read the entire response body into memory as JSON or a String.
            // The json and responseText will already be parsed when checking for an exception message on small 200 responses.
            JSONObject json = null;
            String responseText = null;
            String contentType = response.getContentType();

            if (null != contentType && contentType.contains(Command.CONTENT_TYPE_JSON))
            {
                // Read entire response body and parse into JSON object
                try (Reader reader = response.getReader())
                {
                    JSONTokener tokener = new JSONTokener(reader);
                    char firstChar = tokener.nextClean();
                    tokener.back();
                    if (firstChar == '{')
                    {
                        json = new JSONObject(tokener);
                    }
                    else if (firstChar == '[')
                    {
                        // Some APIs return JSON that the Java API can't handle (e.g. 'product-menuSections.api')
                        json = new JSONObject();
                        json.put("jsonArray", new JSONArray(tokener));
                    }
                }
                catch (Exception ignore) { }
            }
            if (json == null)
            {
                // Otherwise, read entire response body as text.
                responseText = response.getText();
            }

            return createResponse(responseText, response.getStatusCode(), contentType, json);
        }
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        HttpUriRequest method;
        String requestUrl =  uri.getScheme() + "://" + uri.getAuthority() + '/' + _url;

        if (_type.equals("get"))
        {
            method = new HttpGet(requestUrl);
        }
        else
        {
            method = new HttpPost(requestUrl);
            if (_type.equals("post"))
            {
                method.setEntity(new StringEntity(_formData, ContentType.APPLICATION_JSON));
            }
            else if (_type.equals("post_form"))
            {
                method.setEntity(new StringEntity(_formData, ContentType.APPLICATION_FORM_URLENCODED));
            }
            else
            {
                throw new IllegalArgumentException("Unknown request type: " + _type);
            }
        }
        return method;
    }
}
