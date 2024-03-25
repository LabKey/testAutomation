package org.labkey.remoteapi.plate;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class PlateSetResponse extends CommandResponse
{
    private final PlateSetParams _plateSetParams;

    public PlateSetResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);
        _plateSetParams = new PlateSetParams(json.getJSONObject("data"));
    }

    public PlateSetParams getPlateSetParams()
    {
        return _plateSetParams;
    }
}
