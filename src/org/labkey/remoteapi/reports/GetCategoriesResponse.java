package org.labkey.remoteapi.reports;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.reports.model.ViewCategory;
import org.labkey.remoteapi.CommandResponse;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetCategoriesResponse extends CommandResponse
{
    private List<Category> _categoryList;

    public GetCategoriesResponse(String text, int statusCode, String contentType, JSONObject json, GetCategoriesCommand sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
        _categoryList = new ArrayList<>();
        for ( Map<String, Object> categoryMap : (List<Map<String, Object>>)getProperty("categories"))
        {
             _categoryList.add(new Category(categoryMap));
        }
    }

    public List<Category> getCategoryList()
    {
        return _categoryList;
    }

    public Category getCategory(String label)
    {
        for (Category category : getCategoryList())
        {
            if (category.getLabel().equals(label))
                return category;
        }
        return null;
    }

    public Category getCategory(Long rowId)
    {
        for (Category category : getCategoryList())
        {
            if (category.getRowId().equals(rowId))
                return category;
        }
        return null;
    }
}
