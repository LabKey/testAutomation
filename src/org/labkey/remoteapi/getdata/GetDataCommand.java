package org.labkey.remoteapi.getdata;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.labkey.remoteapi.PostCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Minimal wrapper for VisualizationController.GetDataAction usage. Grabs pre-defined JSON to send as the POST body.
 *
 * User: jeckels
 * Date: 4/16/14
 */
public class GetDataCommand extends PostCommand<GetDataResponse>
{
    private final JSONObject _payload;

    public GetDataCommand(InputStream inputJSON) throws IOException, ParseException
    {
        super("visualization", "getData");
        JSONParser parser = new JSONParser();
        _payload = (JSONObject)parser.parse(new InputStreamReader(inputJSON));
        inputJSON.close();
    }

    @Override
    protected GetDataResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new GetDataResponse(text, status, contentType, json, this);
    }

    @Override
    public JSONObject getJsonObject()
    {
        return _payload;
    }
}
