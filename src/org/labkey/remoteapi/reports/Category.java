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

import org.labkey.remoteapi.ResponseObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Category extends ResponseObject
{
    private List<Category> _subcategories;

    public Category(String label)
    {
        this(new HashMap<>(Map.of("label", label)));
    }

    Category(Map<String, Object> map)
    {
        super(map);
        _subcategories=new ArrayList<>();
        List<Map<String, Object>> subCategories = (List<Map<String, Object>>) map.getOrDefault("subCategories", Collections.emptyList());
        for (Map<String, Object> subcategoryMap : subCategories)
        {
            _subcategories.add(new Category(subcategoryMap));
        }
    }

    public void addSubcategories(String... labels)
    {
        for (String label : labels)
        {
            _subcategories.add(new Category(label));
        }
    }

    public void addSubcategories(Category... categories)
    {
        _subcategories.addAll(Arrays.asList(categories));
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

    public Integer getDisplayOrder()
    {
        return (Integer)getAllProperties().get("displayOrder");
    }

    public void setDisplayOrder(Integer displayOrdinal)
    {
        super.getAllProperties().replace("displayOrder", displayOrdinal);
    }

    public Integer getRowId()
    {
        return (Integer)getAllProperties().get("rowid");
    }

    public List<Category> getSubcategories()
    {
        return _subcategories;
    }
}
