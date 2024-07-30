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
    private final Long duration;
    private final String stackTrace;
    private final String detailsURL;
    private final Long startOffset;

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
}
