package org.labkey.remoteapi.reports;

import org.labkey.remoteapi.ResponseObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Category extends ResponseObject
{
    private List<Category> _subcategories;

    public Category(Map<String, Object> map)
    {
        super(map);
        _subcategories=new ArrayList<>();
        for (Map<String, Object> subcategoryMap : (List<Map<String, Object>>)map.get("subCategories"))
        {
            _subcategories.add(new Category(subcategoryMap));
        }
    }

    public Long getParent()
    {
        return (Long)getAllProperties().get("parent");
    }

    public String getLabel()
    {
        return (String)getAllProperties().get("label");
    }

    public void setLabel(String newLabel)
    {
        super.getAllProperties().replace("label", newLabel);
    }

    public Long getDisplayOrder()
    {
        return (Long)getAllProperties().get("displayOrder");
    }

    public void setDisplayOrder(Long displayOrdinal)
    {
        super.getAllProperties().replace("displayOrder", displayOrdinal);
    }

    public Long getRowId()
    {
        return (Long)getAllProperties().get("rowid");
    }

    public List<Category> getSubcategories()
    {
        return _subcategories;
    }
}
