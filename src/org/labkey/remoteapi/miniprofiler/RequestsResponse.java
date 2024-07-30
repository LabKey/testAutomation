package org.labkey.remoteapi.miniprofiler;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.List;
import java.util.stream.StreamSupport;

public class RequestsResponse extends CommandResponse
{
    private final List<RequestInfo> _requestInfos;

    public RequestsResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);
        _requestInfos = StreamSupport.stream(json.getJSONArray("requests").spliterator(), false)
                .map(o -> new RequestInfo((JSONObject)o)).toList();
    }

    public List<RequestInfo> getRequestInfos()
    {
        return _requestInfos;
    }
}
