package org.labkey.remoteapi.announcements;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.ArrayList;
import java.util.List;

public class GetDiscussionsResponse extends CommandResponse
{
    private final List<AnnouncementModel> _threads;

    public GetDiscussionsResponse(String text, int statusCode, String contentType, JSONObject json,
                                  GetDiscussionsCommand sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);

        // populate _threads from payload
        _threads = new ArrayList<>();
        JSONArray discussionThreads = json.getJSONArray("data");
        for (Object thread : discussionThreads)
        {
            _threads.add(new AnnouncementModel((JSONObject)thread));
        }
    }

    public List<AnnouncementModel> getThreads()
    {
        return _threads;
    }
}
