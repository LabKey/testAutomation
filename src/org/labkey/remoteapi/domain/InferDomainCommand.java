package org.labkey.remoteapi.domain;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;

import java.io.File;
import java.net.URI;

public class InferDomainCommand extends Command<InferDomainResponse>
{
    private final File _file;
    private final String _domainKindName;

    private int _numLinesToInclude = 4;

    public InferDomainCommand(File file, String domainKindName)
    {
        super("property", "inferDomain");

        _file = file;
        _domainKindName = domainKindName;
    }

    public void setNumLinesToInclude(int numLinesToInclude)
    {
        _numLinesToInclude = numLinesToInclude;
    }

    @Override
    protected InferDomainResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new InferDomainResponse(text, status, contentType, json, this);
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addTextBody("numLinesToInclude", String.valueOf(4));
        builder.addTextBody("domainKindName", _domainKindName);

        if (_file != null)
            builder.addBinaryBody("file", _file, ContentType.APPLICATION_OCTET_STREAM, _file.getName());

        HttpPost post = new HttpPost(uri);
        post.setEntity(builder.build());
        return post;
    }
}
