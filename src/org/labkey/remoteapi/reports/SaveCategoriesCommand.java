package org.labkey.remoteapi.reports;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SaveCategoriesCommand extends PostCommand<CommandResponse>
{
    private List<Category> _categories = new ArrayList<>();
    public SaveCategoriesCommand()
    {
        super("reports", "saveCategories");
    }

    public void setCategories(Category... categories)
    {
        _categories = Arrays.asList(categories);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (org.labkey.remoteapi.reports.Category cat : _categories)
        {
            jsonArray.add(cat.getAllProperties());
        }
        jsonObject.put("categories", jsonArray);
        return jsonObject;
    }
}
