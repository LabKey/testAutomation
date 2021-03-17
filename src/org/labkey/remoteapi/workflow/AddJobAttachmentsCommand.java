package org.labkey.remoteapi.workflow;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.io.File;
import java.net.URI;

public class AddJobAttachmentsCommand extends PostCommand<CreateJobResponse>
{
    private final Long _jobId;
    private final File[] _files;

    public AddJobAttachmentsCommand(Long jobId, File... files)
    {
        super("samplemanager", "addjobattachments");
        _jobId = jobId;
        _files = files;
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addTextBody("rowId", _jobId.toString());
        for (int i = 0; i < _files.length; i++)
        {
            File file = _files[i];
            String name = "file" + (i > 0 ? i : "");
            builder.addBinaryBody(name, file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        }

        HttpPost post = new HttpPost(uri);
        // Setting this header forces the ExtFormResponseWriter to return application/json contentType instead of text/html.
        post.addHeader("X-Requested-With", "XMLHttpRequest");
        post.setEntity(builder.build());
        return post;
    }

    @Override
    protected CreateJobResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new CreateJobResponse(text, status, contentType, json, this.copy());
    }
}
