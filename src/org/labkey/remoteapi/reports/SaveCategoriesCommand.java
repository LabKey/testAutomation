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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public void setCategories(String... categoryLabels)
    {
        _categories = Arrays.stream(categoryLabels).map(label -> new Category(label)).collect(Collectors.toList());
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
