package org.labkey.remoteapi;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleFormCommand extends Command<CommandResponse, HttpUriRequest>
{
    private final Map<String, String> _formData;

    public SimpleFormCommand(String controller, String action, Map<String, String> formData)
    {
        super(controller, action);
        _formData = formData;
    }

    public CommandResponse execute(Connection connection) throws IOException, CommandException
    {
        return execute(connection, null);
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        HttpPost post = new HttpPost(uri);

        List<NameValuePair> args = new ArrayList<>();
        for (Map.Entry<String, String> reportVal : _formData.entrySet())
        {
            args.add(new BasicNameValuePair(reportVal.getKey(), reportVal.getValue()));
        }
        post.setEntity(new UrlEncodedFormEntity(args));
        return post;
    }
}
