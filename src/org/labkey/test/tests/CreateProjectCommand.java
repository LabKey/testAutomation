package org.labkey.test.tests;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CreateProjectCommand extends PostCommand<CommandResponse>
{
    private final Map<String, Object> _params;

    public CreateProjectCommand(Map<String, Object> params)
    {
        super("admin", "createProject");
        _params = params;
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        // CreateProjectAction is not a real API action, so we POST form data not JSON
        List<BasicNameValuePair> postData = new ArrayList<>();

        _params.forEach((k, v) -> {
            // Expand any collections into multiple individual params
            if (v instanceof Collection<?> col)
                col.forEach(val -> postData.add(new BasicNameValuePair(k, String.valueOf(val))));
            else
                postData.add(new BasicNameValuePair(k, String.valueOf(v)));
        });

        try
        {
            HttpPost request = new HttpPost(uri);
            request.setEntity(new UrlEncodedFormEntity(postData));
            return request;
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
