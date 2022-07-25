package org.labkey.remoteapi.admin;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class CreateProjectCommand extends PostCommand<CommandResponse>
{
    private String _name;
    private String _folderType;
    private boolean _assignProjectAdmin = false;
    private String _templateSourceId;
    private boolean _templateIncludeSubfolders = false;
    private List<String> _templateWriterTypes = List.of();

    public CreateProjectCommand()
    {
        super("admin", "createProject");
    }

    public CreateProjectCommand setName(String name)
    {
        _name = name;
        return this;
    }

    public CreateProjectCommand setFolderType(String folderType)
    {
        _folderType = folderType;
        return this;
    }

    public CreateProjectCommand setAssignProjectAdmin(boolean assignProjectAdmin)
    {
        _assignProjectAdmin = assignProjectAdmin;
        return this;
    }

    public CreateProjectCommand setTemplateSourceId(String templateSourceId)
    {
        _templateSourceId = templateSourceId;
        return this;
    }

    public CreateProjectCommand setTemplateIncludeSubfolders(boolean templateIncludeSubfolders)
    {
        _templateIncludeSubfolders = templateIncludeSubfolders;
        return this;
    }

    public CreateProjectCommand setTemplateWriterTypes(String... templateWriterTypes)
    {
        _templateWriterTypes = List.of(templateWriterTypes);
        return this;
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        // CreateProjectAction is not a real API action, so we POST form data instead of JSON

        List<BasicNameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("name", _name));
        postData.add(new BasicNameValuePair("folderType", _folderType));
        postData.add(new BasicNameValuePair("assignProjectAdmin", Boolean.toString(_assignProjectAdmin)));

        if ("Template".equals(_folderType))
        {
            postData.add(new BasicNameValuePair("templateSourceId", _templateSourceId));
            postData.add(new BasicNameValuePair("templateIncludeSubfolders", Boolean.toString(_templateIncludeSubfolders)));
            _templateWriterTypes.forEach(type -> postData.add(new BasicNameValuePair("templateWriterTypes", type)));
        }

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
