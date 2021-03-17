package org.labkey.remoteapi.workflow;

import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class DeleteJobAttachmentsCommand extends PostCommand<CreateJobResponse>
{
    private final Long _jobId;
    private final String[] _files;

    public DeleteJobAttachmentsCommand(Long jobId, String... files)
    {
        super("samplemanager", "deletejobattachments");
        _jobId = jobId;
        _files = files;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("jobId", _jobId);

        JSONArray fileNames = new JSONArray();
        if (_files != null)
        {
            for (String file : _files)
                fileNames.put(file);

            json.put("names", fileNames);
        }

        return json;
    }

    @Override
    protected CreateJobResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new CreateJobResponse(text, status, contentType, json, this.copy());
    }
}
