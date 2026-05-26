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
package org.labkey.remoteapi.miniprofiler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Client-side representation of 'org.labkey.api.miniprofiler.Timing'
 */
public class Timing
{
    /*
      "name" : "\/home\/project-begin.view",
      "id" : "f2cee967-30c7-103d-ac70-019b18afe158",
      "duration" : 784,
      "durationExclusive" : 784,
      "children" : [...],
      "objects" : {...},
      "customTimings" : [...]
     */

    private final String name;
    private final String id;
    private final long duration;
    private final long durationExclusive;
    private final List<Timing> children;
    private final JSONObject objects;
    private final Map<String, List<CustomTiming>> customTimings;

    public Timing(JSONObject json)
    {
        name = json.getString("name");
        id = json.getString("id");
        duration = json.getLong("duration");
        durationExclusive = json.getLong("durationExclusive");
        children = fromArray(json.optJSONArray("children"));
        objects = json.optJSONObject("objects");
        customTimings = extractCustomTimings(json.optJSONObject("customTimings"));
    }

    public String getName()
    {
        return name;
    }

    public String getId()
    {
        return id;
    }

    public Long getDuration()
    {
        return duration;
    }

    public Long getDurationExclusive()
    {
        return durationExclusive;
    }

    public List<Timing> getChildren()
    {
        return children;
    }

    public JSONObject getObjects()
    {
        return objects;
    }

    public Map<String, List<CustomTiming>> getCustomTimings()
    {
        return customTimings;
    }

    private static List<Timing> fromArray(JSONArray array)
    {
        if (array == null)
        {
            return Collections.emptyList();
        }
        else
        {
            return StreamSupport.stream(array.spliterator(), false)
                    .map(o -> new Timing((JSONObject)o)).toList();
        }
    }

    private static Map<String, List<CustomTiming>> extractCustomTimings(JSONObject json)
    {
        if (json == null)
        {
            return Collections.emptyMap();
        }
        else
        {
            Map<String, List<CustomTiming>> customTimingsMap = new HashMap<>();
            for (String key : json.keySet())
            {
                customTimingsMap.put(key, CustomTiming.fromArray(json.getJSONArray(key)));
            }
            return Collections.unmodifiableMap(customTimingsMap);
        }
    }
}
