package org.labkey.remoteapi.issues;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.io.File;
import java.net.URI;

public class IssueCommand extends PostCommand<IssueResponse>
{
    private IssueModel _issue;

    public IssueCommand()
    {
        super("issues", "issues");
    }

    public IssueCommand(IssueModel issue)
    {
        this();
        _issue = issue;
    }

    @Override
    protected IssueResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new IssueResponse(text, status, contentType, json, this);
    }

    public void setIssue(IssueModel issue)
    {
        _issue = issue;
    }

    @Override
    public JSONObject getJsonObject()
    {
       throw new IllegalStateException("This command should not use this method for constructing the request");
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        HttpPost request = new HttpPost(uri);

        JSONArray issueArray = new JSONArray();
        issueArray.put(_issue.toJSON());

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addTextBody("issues", issueArray.toString(), ContentType.APPLICATION_JSON);
        for (File attachment : _issue.getAttachments())
        {
            builder.addBinaryBody(attachment.getName(), attachment, ContentType.APPLICATION_OCTET_STREAM, attachment.getName());
        }
        request.setEntity(builder.build());
        return request;
    }
}


