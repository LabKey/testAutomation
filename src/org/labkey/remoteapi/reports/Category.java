/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.remoteapi.reports;

import org.json.JSONObject;
import org.labkey.remoteapi.ResponseObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Category extends ResponseObject
{
    private final List<Category> _subcategories = new ArrayList<>();
    private final Integer _rowId;
    private String _label;
    private Integer _displayOrder;
    private Integer _parent;

    public Category(String label)
    {
        _label = label;
        _rowId = null;
    }

    Category(Map<String, Object> map)
    {
        super(map);
        _rowId = (Integer) map.get("rowId");
        _label = (String) map.get("label");
        _displayOrder = (Integer) map.get("displayOrder");
        _parent = (Integer) map.get("parent");
        List<Map<String, Object>> subCategories = (List<Map<String, Object>>) map.getOrDefault("subCategories", Collections.emptyList());
        for (Map<String, Object> subcategoryMap : subCategories)
        {
            _subcategories.add(new Category(subcategoryMap));
        }
    }

    public JSONObject toJSONObject()
    {
        JSONObject json = new JSONObject();
        json.put("label", _label);
        json.put("rowId", _rowId);
        json.put("displayOrder", _displayOrder);
        json.put("parent", _parent);

        return json;
    }

    public Integer getParent()
    {
        return _parent;
    }

    public Category setParent(Integer parent)
    {
        _parent = parent;
        return this;
    }

    public String getLabel()
    {
        return _label;
    }

    public void setLabel(String newLabel)
    {
        _label = newLabel;
    }

    public Integer getDisplayOrder()
    {
        return _displayOrder;
    }

    public void setDisplayOrder(Integer displayOrdinal)
    {
        _displayOrder = displayOrdinal;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public List<Category> getSubcategories()
    {
        return _subcategories;
    }
}
