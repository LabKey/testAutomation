package org.labkey.remoteapi.domain;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

import java.util.List;
import java.util.Map;

public class DomainResponse extends CommandResponse
{
    /**
     * Constructs a new CommandResponse, initialized with the provided
     * response text and status code.
     *
     * @param text          The response text
     * @param statusCode    The HTTP status code
     * @param contentType   The response content type
     * @param json          The parsed JSONObject (or null if JSON was not returned).
     * @param sourceCommand A copy of the command that created this response
     */
    public DomainResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
    }

    public List<Map<String, Object>> getColumns()
    {
        return (List<Map<String, Object>>) getParsedData().get("fields");
    }

    public Long getDomainId()
    {
        return (Long)getParsedData().get("domainId");
    }

    public String getDomainURI()
    {
        return (String)getParsedData().get("domainURI");
    }
}
