package org.labkey.remoteapi.plate;

import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class CreatePlateSetCommand extends PostCommand<PlateSetResponse>
{
    private final PlateSetParams _plateSetParams;
    public CreatePlateSetCommand(PlateSetParams params)
    {
        super("plate", "createPlateSet");
        setRequiredVersion(0);
        _plateSetParams = params;
    }

    @Override
    protected PlateSetResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new PlateSetResponse(text, status, contentType, json);
    }

    @Override
    public JSONObject getJsonObject()
    {
        return _plateSetParams.toJSON();
    }
}
