package org.labkey.remoteapi.getdata;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.query.SelectRowsResponse;

/**
 * User: jeckels
 * Date: 4/16/14
 */
public class GetDataResponse extends SelectRowsResponse
{
    public GetDataResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
    }
}
