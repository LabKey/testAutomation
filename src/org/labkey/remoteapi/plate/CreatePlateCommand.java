package org.labkey.remoteapi.plate;

import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class CreatePlateCommand extends PostCommand<PlateResponse>
{
    private final PlateParams _plateParams;
    public  CreatePlateCommand(PlateParams plateParams)
    {
        super("plate", "createPlate");
        setRequiredVersion(0);
        _plateParams = plateParams;
    }

    @Override
    protected PlateResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new PlateResponse(text, status, contentType, json);
    }

    @Override
    public JSONObject getJsonObject()
    {
        return _plateParams.toJSON();
    }
}
