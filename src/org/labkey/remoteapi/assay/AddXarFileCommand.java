package org.labkey.remoteapi.assay;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.labkey.remoteapi.PostCommand;

import java.io.File;
import java.net.URI;

public class AddXarFileCommand extends PostCommand
{
    private final File _file;

    public AddXarFileCommand(File xarFile)
    {
        super("experiment", "assayXarFile");
        this._file = xarFile;
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addBinaryBody("file", _file, ContentType.TEXT_XML, _file.getName());

        HttpPost post = new HttpPost(uri);
        post.setEntity(builder.build());
        return post;
    }
}
