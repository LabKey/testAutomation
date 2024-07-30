package org.labkey.remoteapi.miniprofiler;

import org.json.JSONObject;
import org.labkey.remoteapi.GetCommand;

import java.util.Map;

public class BaseRequestsCommand extends GetCommand<RequestsResponse>
{
    private final Long _id;

    protected BaseRequestsCommand(String actionName, Long requestId)
    {
        super("mini-profiler", actionName);
        _id = requestId;
    }

    @Override
    protected Map<String, Object> createParameterMap()
    {
        Map<String, Object> parameterMap = super.createParameterMap();
        parameterMap.put("id", _id);
        return parameterMap;
    }

    @Override
    protected RequestsResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new RequestsResponse(text, status, contentType, json);
    }
}
