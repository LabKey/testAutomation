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
    private final Long duration;
    private final Long durationExclusive;
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
