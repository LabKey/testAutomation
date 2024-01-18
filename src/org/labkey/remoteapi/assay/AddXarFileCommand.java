package org.labkey.remoteapi.assay;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
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
    protected HttpPost createRequest(URI uri)
    {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addBinaryBody("file", _file, ContentType.TEXT_XML, _file.getName());

        HttpPost post = new HttpPost(uri);
        post.setEntity(builder.build());
        return post;
    }
}
