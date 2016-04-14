package org.labkey.remoteapi.reports;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;

public class GetCategoriesCommand extends Command<GetCategoriesResponse>
{
    public GetCategoriesCommand()
    {
        super("reports", "getCategories");
    }

    protected GetCategoriesResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new GetCategoriesResponse(text, status, contentType, json, copy());
    }

    @Override
    public GetCategoriesCommand copy()
    {
        return new GetCategoriesCommand();
    }
}
