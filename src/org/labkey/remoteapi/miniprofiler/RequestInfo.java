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
