package org.labkey.remoteapi.miniprofiler;

import org.json.JSONObject;

/**
 * Client-side representation of 'org.labkey.api.miniprofiler.RequestInfo'
 */
public class RequestInfo
{
    /*
        "id" : 1,
        "url" : "\/home\/project-begin.view?",
        "date" : "2024-07-30 11:53:36.509",
        "duration" : 784,
        "root" : {...},
        "objects" : {...},
        "ignored" : false,
        "name" : "project\/begin"
     */

    private final Long id;
    private final String url;
    private final String date;
    private final long duration;
    private final Timing root;
    private final JSONObject objects;

    public RequestInfo(JSONObject json)
    {
        id = json.getLong("id");
        url = json.getString("url");
        date = json.getString("date");
        duration = json.getLong("duration");
        root = new Timing(json.getJSONObject("root"));
        objects = json.getJSONObject("objects");
    }

    public Long getId()
    {
        return id;
    }

    public String getUrl()
    {
        return url;
    }

    public String getDate()
    {
        return date;
    }

    public long getDuration()
    {
        return duration;
    }

    public Timing getRoot()
    {
        return root;
    }

    public JSONObject getObjects()
    {
        return objects;
    }
}
