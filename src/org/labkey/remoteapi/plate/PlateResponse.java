package org.labkey.remoteapi.plate;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class PlateResponse extends CommandResponse
{
    private final PlateParams _plateParams;

    public PlateResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);
        _plateParams = new PlateParams(json.getJSONObject("data"));
    }

    public PlateParams getPlateParams()
    {
        return _plateParams;
    }
}
