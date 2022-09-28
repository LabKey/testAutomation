package org.labkey.remoteapi.issues;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.json.old.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.io.File;
import java.net.URI;
import java.util.List;

public class IssuesCommand extends PostCommand<IssueResponse>
{
    private List<IssueModel> _issues;

    public IssuesCommand()
    {
        super("issues", "issues");
    }

    public IssuesCommand(List<IssueModel> issues)
    {
        this();
        _issues = issues;
    }

    @Override
    protected IssueResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new IssueResponse(text, status, contentType, json, this);
    }

    public void setIssues(List<IssueModel> issues)
    {
        _issues = issues;
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

        JSONArray issuesArray = new JSONArray();
        for (IssueModel issue: _issues)
        {
            issuesArray.put(issue.toJSON());
        }

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.LEGACY);
        builder.addTextBody("issues", issuesArray.toString(), ContentType.APPLICATION_JSON);

        for(IssueModel issue: _issues)
        {
            for (File attachment : issue.getAttachments())
            {
                builder.addBinaryBody(attachment.getName(), attachment, ContentType.APPLICATION_OCTET_STREAM, attachment.getName());
            }
            request.setEntity(builder.build());
        }
        return request;
    }
}


