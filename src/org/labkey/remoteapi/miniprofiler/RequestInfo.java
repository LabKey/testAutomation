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

    public static final RequestInfo BLANK = new RequestInfo(null, null, null, null, null, null, null);

    private final Long id;
    private final String url;
    private final String date;
    private final Long duration;
    private final Timing root;
    private final JSONObject objects;
    private final String sessionId;

    public RequestInfo(JSONObject json)
    {
        id = json.getLong("id");
        url = json.getString("url");
        date = json.getString("date");
        duration = json.getLong("duration");
        root = new Timing(json.getJSONObject("root"));
        objects = json.getJSONObject("objects");
        sessionId = json.getString("sessionId");
    }

    public RequestInfo(Long id, String url, String date, Long duration, Timing root, JSONObject objects, String sessionId)
    {
        this.id = id;
        this.url = url;
        this.date = date;
        this.duration = duration;
        this.root = root;
        this.objects = objects;
        this.sessionId = sessionId;
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

    public Long getDuration()
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

    public String getSessionId()
    {
        return sessionId;
    }
}
