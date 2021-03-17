package org.labkey.remoteapi.workflow;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

public class CreateJobResponse extends CommandResponse
{
    public CreateJobResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
    }

    public Long getJobId()
    {
        return (Long) getProperty("data.rowId");
    }

    public Job getJob()
    {
        return Job.fromMap(getProperty("data"));
    }
}
