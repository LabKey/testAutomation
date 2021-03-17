package org.labkey.remoteapi.workflow;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

public class DeleteJobsCommand extends PostCommand<CommandResponse>
{
    private final long _jobId;

    public DeleteJobsCommand(long jobId)
    {
        super("samplemanager", "deleteJobs");
        _jobId = jobId;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("singleObjectRowId", _jobId);
        return json;
    }

    @Override
    public double getRequiredVersion()
    {
        return -1;
    }
}
