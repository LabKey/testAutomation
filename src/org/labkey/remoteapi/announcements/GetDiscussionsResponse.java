package org.labkey.remoteapi.announcements;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.ArrayList;
import java.util.List;

public class GetDiscussionsResponse extends CommandResponse
{
    private List<TestAnnouncementModel> _threads;

    public GetDiscussionsResponse(String text, int statusCode, String contentType, JSONObject json,
                                  GetDiscussionsCommand sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);

        // populate _threads from payload
        _threads = new ArrayList<>();
        JSONArray discussionThreads =(JSONArray)json.get("data");
        for (int i=0; i< discussionThreads.size(); i++)
        {
            _threads.add(new TestAnnouncementModel((JSONObject)discussionThreads.get(i)));
        }
    }

    public List<TestAnnouncementModel> getThreads()
    {
        return _threads;
    }
}
