/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.test.params.perf;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PerfScenario
{
    private final String name;

    // SM perf properties
    private final String type;
    private final String fileName;
    private final String typeName;
    private final Integer average;

    // Bio perf properties
    private final String description;
    private final String container;
    private final String dataGeneratorFile;
    private final Map<String, Integer> measures;

    PerfScenario(JSONObject json)
    {
        name = json.getString("name");

        type = json.optString("type");
        fileName = json.optString("fileName");
        typeName = json.optString("typeName");
        average = json.optInt("average");

        description = json.optString("description");
        container = json.optString("container");
        dataGeneratorFile = json.optString("dataGeneratorFile");
        measures = extractMeasures(json);
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public String getFileName()
    {
        return fileName;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public Integer getAverage()
    {
        return average;
    }

    public String getDescription()
    {
        return description;
    }

    public String getContainer()
    {
        return container;
    }

    public String getDataGeneratorFile()
    {
        return dataGeneratorFile;
    }

    public Map<String, Integer> getMeasures()
    {
        return measures;
    }

    /**
     * Get the map of baseline perf measures
     *
     * @param testInfoObject The json object for a test.
     * @return The names of the sample types or source types or arrays.
     */
    private static Map<String, Integer> extractMeasures(JSONObject testInfoObject)
    {
        Map<String, Integer> measures = new HashMap<>();

        Object measuresObject = testInfoObject.opt("measures");

        if (measuresObject instanceof JSONArray jsonArray)
        {
            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject innerMeasureObj = jsonArray.getJSONObject(i);
                for (String key : innerMeasureObj.keySet())
                {
                    measures.put(key, innerMeasureObj.getInt(key));
                }
            }
        }
        else if (measuresObject instanceof JSONObject jsonObject)
        {
            for (String key : jsonObject.keySet())
            {
                measures.put(key, jsonObject.getInt(key));
            }
        }

        return measures;
    }
}
