/*
 * Copyright (c) 2016 LabKey Corporation
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
