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

import java.util.List;
import java.util.stream.StreamSupport;

public class CustomTiming
{
    /*
      "message" : "-- <QueryServiceImpl.getSelectSQL(Policies)>\n\tSELECT *\n\tFROM (\n\tSELECT \n\tPolicies.resourceid AS resourceid,\n\tPolicies.resourceclass AS resourceclass,\n\tPolicies.container AS container,\n\tPolicies.modified AS modified\n\tFROM core.policies Policies ) x\n\tWHERE (container = ?) AND (resourceid = ?)\n-- <\/QueryServiceImpl.getSelectSQL()>\n",
      "duration" : 3,
      "stackTrace" : null,
      "detailsURL" : "\/admin\/queryStackTraces.view?sqlHashCode=-584866968",
      "startOffset" : 49
     */

    private final String message;
    private final long duration;
    private final String stackTrace;
    private final String detailsURL;
    private final long startOffset;

    public CustomTiming(JSONObject json)
    {
        message = json.optString("message");
        duration = json.getLong("duration");
        stackTrace = json.optString("stackTrace");
        detailsURL = json.optString("detailsURL");
        startOffset = json.getLong("startOffset");
    }

    static List<CustomTiming> fromArray(JSONArray array)
    {
        return StreamSupport.stream(array.spliterator(), false)
                .map(o -> new CustomTiming((JSONObject)o)).toList();
    }

    public String getMessage()
    {
        return message;
    }

    public long getDuration()
    {
        return duration;
    }

    public String getStackTrace()
    {
        return stackTrace;
    }

    public String getDetailsURL()
    {
        return detailsURL;
    }

    public long getStartOffset()
    {
        return startOffset;
    }
}
