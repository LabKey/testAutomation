package org.labkey.remoteapi.workflow;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class CreateJobCommand extends PostCommand<CreateJobResponse>
{
    private final Job _job;

    public CreateJobCommand(Job job)
    {
        super("samplemanager", "createJob");
        _job = job;
    }

    @Override
    public JSONObject getJsonObject()
    {
        return _job.toJSONObject();
    }

    @Override
    public double getRequiredVersion()
    {
        return -1;
    }

    @Override
    protected CreateJobResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new CreateJobResponse(text, status, contentType, json, this.copy());
    }
}
