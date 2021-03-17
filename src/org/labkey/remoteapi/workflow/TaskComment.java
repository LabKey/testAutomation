package org.labkey.remoteapi.workflow;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

public class TaskComment implements JSONAware
{
    Long userId;
    String comment;
    String commentDate;

    public TaskComment(Long userId, String comment, String commentDate)
    {
        this.userId = userId;
        this.comment = comment;
        this.commentDate = commentDate;
    }

    public JSONObject toJSONObject()
    {
        JSONObject json = new JSONObject();
        json.put("userId", userId);
        json.put("comment", comment);
        json.put("commentDate", commentDate);

        return json;
    }

    @Override
    public String toJSONString()
    {
        return toJSONObject().toJSONString();
    }
}
