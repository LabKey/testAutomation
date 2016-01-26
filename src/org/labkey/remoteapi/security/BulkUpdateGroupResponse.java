package org.labkey.remoteapi.security;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

import java.util.List;
import java.util.Map;

public class BulkUpdateGroupResponse extends CommandResponse
{
    public BulkUpdateGroupResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
    }

    public Number getId()
    {
        return getProperty("id");
    }

    public String getName()
    {
        return getProperty("name");
    }

    public List<Map<String, Object>> getNewUsers()
    {
        return getProperty("newUsers");
    }

    public List<Map<String, Object>> getAddedMembers()
    {
        return getProperty("members.added");
    }

    public List<Map<String, Object>> getRemovedMembers()
    {
        return getProperty("members.removed");
    }

    public Map<String, Object> getErrors()
    {
        return getProperty("errors");
    }
}
